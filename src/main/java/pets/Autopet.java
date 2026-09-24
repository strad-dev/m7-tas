package pets;

import damage.Difficulty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import plugin.Utils;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Autopet: swap the player's pet for them when something happens.
 * <p>
 * A rule is <b>(trigger, the pet to equip, any number of exception pets)</b>, and an exception means "fire this
 * rule UNLESS that pet is the one currently out" - which is how a player keeps a deliberate choice from being
 * undone by the next trigger.  Rules live on the player's profile ({@code Pets}) and are edited in
 * {@link AutopetMenu}.
 * <p>
 * <b>Everything here is a no-op outside {@link Difficulty#manualPets()}</b>, checked at each entry point rather
 * than at the listener registration, because the mode is a server-wide global that {@code /m7practice} moves
 * between runs.
 * <p>
 * <b>Three of the four triggers are not events this class can see.</b>  They are moments the plugin already
 * knows about and nothing else was watching, so each needs one call from the file that owns the moment - see the
 * {@code onXxx} methods below for which line goes where.  Only Rod Swap is a Bukkit event, and it is handled
 * here.
 */
public final class Autopet implements Listener {

	/**
	 * What autopet says when it moves a pet.  {@code Pets.equip} finishes the line with the pet's own name.
	 * <p>
	 * <b>The "o" is a Greek omicron (U+03BF), on purpose.</b>  SkyBlock client mods hide Hypixel's real
	 * "Autopet equipped your ..." line by matching its text, and they ate ours too.  Written as an escape so it
	 * survives any source encoding and nobody "fixes" it back.
	 */
	private static final String ANNOUNCEMENT = "<green>Autοpet<gray> equipped your ";

	/**
	 * The four things that can move a pet.
	 * <p>
	 * The display data sits here rather than in {@link AutopetMenu} so a new trigger is one constant and appears
	 * in the menu for free, the same reason {@code Difficulty} carries its own {@code displayName}.
	 */
	public enum Trigger {
		/** The run going live: the countdown ending and the start door opening. */
		RUN_START("On Run Start", Material.OAK_DOOR),
		/** The first hit of a fight, after a lull.  See {@link #onCombatHit}. */
		ENTER_COMBAT("On Enter Combat", Material.IRON_SWORD),
		/** Maxor's phase starting. */
		MAXOR_SPAWN("When Maxor Spawns", Material.WITHER_SKELETON_SKULL),
		/**
		 * Throwing a fishing rod.  <b>The odd one out</b>: it holds an ordered CYCLE rather than one pet, so each
		 * throw steps to the next pet in the list.  See {@link #advanceRodCycle}.
		 */
		ROD_SWAP("Rod Swap", Material.FISHING_ROD);

		private final String displayName;
		private final Material icon;

		Trigger(String displayName, Material icon) {
			this.displayName = displayName;
			this.icon = icon;
		}

		public String displayName() {
			return displayName;
		}

		public Material icon() {
			return icon;
		}

		/** True for the trigger that holds a cycle instead of a single pet. */
		public boolean isCycle() {
			return this == ROD_SWAP;
		}

		static Trigger parse(String name) {
			if(name == null) return null;
			for(Trigger t : values()) if(t.name().equals(name)) return t;
			return null;
		}
	}

	/**
	 * One rule: what to equip, and what not to interrupt.
	 * <p>
	 * A null {@code pet} is "off" and fires nothing; an empty {@code exceptions} is "always fire".  A rule with
	 * exceptions but no pet is kept rather than discarded, so a player can set the two halves in either order
	 * without the first one vanishing - that is the whole job of {@link #isOff()}.
	 * <p>
	 * <b>{@code exceptions} is an IMMUTABLE list, normalised into {@link PetType} declaration order.</b>  A list
	 * and not an {@code EnumSet} because a record component has to be immutable for the record to keep its value
	 * semantics, and {@code List.copyOf} gives that in one call where an EnumSet would need a defensive copy at
	 * every read.  Normalising in the canonical constructor rather than at the menu means the order is settled
	 * once: the rule's lore, the exception picker and the saved file all render the same sequence without anyone
	 * sorting.  Duplicates fold out on the way in, since "except this pet twice" means nothing.
	 */
	public record Rule(PetType pet, List<PetType> exceptions) {
		public Rule {
			exceptions = normalise(exceptions);
		}

		/** Nothing set at all.  {@code Pets.setRule} DROPS such a rule rather than storing an empty one. */
		public boolean isOff() {
			return pet == null && exceptions.isEmpty();
		}

		/** Does the pet currently out veto this rule? */
		public boolean excepts(PetType out) {
			return out != null && exceptions.contains(out);
		}

		/** Dedupe into declaration order, tolerating a null list and nulls inside it.  EnumSet iterates by ordinal. */
		private static List<PetType> normalise(List<PetType> pets) {
			if(pets == null || pets.isEmpty()) return List.of();
			EnumSet<PetType> set = EnumSet.noneOf(PetType.class);
			for(PetType t : pets) if(t != null) set.add(t);
			return List.copyOf(set);
		}
	}

	// ==================== the triggers ====================

	/**
	 * The run going live.
	 * <p>
	 * <b>Needs one line in {@code instructions/Server.java}</b>, in {@code startSection}, directly under
	 * {@code runStarted = true;}: {@code pets.Autopet.onRunStart();}.  That is the one place every section's
	 * start funnels through (the clear via the countdown, a boss via the load grace), so it is the only spot that
	 * means "the run is now live" for a party that started at the door AND for one warped straight to Goldor.
	 */
	public static void onRunStart() {
		fireForEveryone(Trigger.RUN_START);
	}

	/**
	 * Maxor's phase starting.
	 * <p>
	 * <b>Needs one line in {@code instructions/bosses/maxor/Maxor.java}</b>, in {@code maxorInstructions},
	 * directly under {@code INSTANCE.start(world, doContinue);}: {@code pets.Autopet.onMaxorSpawn();}.  The boss
	 * entity exists by then ({@code WitherLord.start} spawns it), and that facade is what both the boss chain and
	 * {@code /m7practice maxor} go through.
	 */
	public static void onMaxorSpawn() {
		fireForEveryone(Trigger.MAXOR_SPAWN);
	}

	/**
	 * A hit landing, which is how this class learns a fight has started.
	 * <p>
	 * <b>Needs one line in {@code damage/CombatState.java}</b>, at the top of {@code noteHit}, after its null
	 * guard: {@code pets.Autopet.onCombatHit(p);}.  {@code noteHit} is the plugin's existing "a primary hit just
	 * landed" chokepoint, and there is no in-combat flag anywhere to hook instead.
	 * <p>
	 * <b>The transition is worked out HERE, not at the call site</b>, so that line can be an unconditional
	 * one-liner: a hit more than {@link #COMBAT_LAPSE_TICKS} after the last one is an entry into combat, anything
	 * sooner is the same fight continuing.  Nothing has to tick to expire that, because the test is made at the
	 * next hit and not in between.
	 */
	public static void onCombatHit(Player p) {
		if(p == null || !Difficulty.manualPets()) return;
		int now = Utils.serverTick();
		Integer last = lastHitTick.put(p.getUniqueId(), now);
		if(last != null && now - last <= COMBAT_LAPSE_TICKS) return; // still the same fight
		fire(p, Trigger.ENTER_COMBAT);
	}

	/**
	 * How long without landing a hit before the next one counts as entering combat again.  10s, long enough to
	 * cover walking between two mobs in a room and short enough that the next section is a new fight.
	 */
	private static final int COMBAT_LAPSE_TICKS = 200;

	/** Absolute server tick each player last landed a primary hit on.  Absent = not in combat. */
	private static final Map<UUID, Integer> lastHitTick = new HashMap<>();

	/**
	 * <b>The throw, not the reel.</b>  {@code PlayerFishEvent} fires several times for one cast and only
	 * {@code FISHING} is the bobber leaving the hand; hooking anything else would swap the pet again on the way
	 * back, so a rod used for movement would step the cycle twice per throw.
	 * <p>
	 * Gated on the Pitchin' Rod by name rather than on the material, because the Flaming Flay is a fishing rod
	 * too and its right-click ability throws a bobber like any other - stepping the cycle every time a Mage cast
	 * one would make the rule unusable.
	 * <p>
	 * There was no {@code PlayerFishEvent} handler anywhere in the plugin to extend, so this is the first one.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onFish(PlayerFishEvent e) {
		if(e.getState() != PlayerFishEvent.State.FISHING) return;
		if(!Difficulty.manualPets()) return;
		Player p = e.getPlayer();
		if(!items.util.PitchinRod.INSTANCE.matches(p.getInventory().getItemInMainHand())) return;
		fire(p, Trigger.ROD_SWAP);
	}

	/**
	 * Where in the Rod Swap cycle each player last landed.  <b>In memory only, never persisted</b>: it is a
	 * position in one session's rotation, not a preference, and a stale one read off disk would start a run
	 * mid-cycle.  Cleared by {@link #reset} and by {@link #clearRodCursor} whenever the cycle itself is edited.
	 * <p>
	 * <b>This map exists because the cycle may hold the same pet twice, and {@code indexOf} cannot cope.</b>  A
	 * cycle of [A, B, A, C] with A out has two answers to "where am I", and {@code indexOf} always gives the
	 * first, so advancing from it walks A, B, A, B and C is unreachable.  Do not simplify this back to a lookup.
	 */
	private static final Map<UUID, Integer> rodCursor = new HashMap<>();

	/**
	 * Forget a player's place in the cycle.  Called by {@link PetPicker}'s cycle editor on every edit: once the
	 * list has changed under them the old index points at a different throw, and starting over at the front is
	 * the only answer that is not arbitrary.
	 */
	public static void clearRodCursor(Player p) {
		if(p != null) rodCursor.remove(p.getUniqueId());
	}

	/**
	 * Step the Rod Swap cycle one place and summon what it lands on.
	 * <p>
	 * <b>A pet outside the cycle jumps to the front of it rather than advancing.</b>  "Advance" needs a position
	 * to advance from, and a pet that is not in the list has none; treating that as "start at the first" means a
	 * player who summoned something by hand gets the cycle back in one throw instead of an arbitrary place in it.
	 * <p>
	 * Otherwise the step is off {@link #rodCursor}, and the cursor is only believed while it still POINTS at what
	 * is out.  A cold start (first throw of the session), a hand summon from {@code /pets} and a cycle edited to
	 * a different length all leave it disagreeing with the equipped pet, and the first occurrence of that pet is
	 * the best guess left - which is exactly what the old {@code indexOf} did, kept for the case it is right for.
	 */
	private static void advanceRodCycle(Player p) {
		List<PetType> cycle = Pets.rodCycle(p);
		if(cycle.isEmpty()) return;
		PetType out = Pets.equipped(p);
		int at;
		if(!cycle.contains(out)) {
			at = 0;
		} else {
			Integer cursor = rodCursor.get(p.getUniqueId());
			int from = cursor != null && cursor < cycle.size() && cycle.get(cursor) == out ? cursor : cycle.indexOf(out);
			at = (from + 1) % cycle.size();
		}
		rodCursor.put(p.getUniqueId(), at);
		// The cursor moves even when the pet does not: a cycle may list the same pet twice in a row, and
		// Pets.equip stays quiet on a no-op, so the throw still costs a place in the rotation and says nothing.
		Pets.equip(p, cycle.get(at), ANNOUNCEMENT);
	}

	// ==================== firing ====================

	/** Run one trigger for every real player online.  The two whole-run triggers are not about one player. */
	private static void fireForEveryone(Trigger t) {
		if(!Difficulty.manualPets()) {
			Utils.debug(Utils.DebugType.BOSS, "Autopet " + t.name() + ": skipped, mode is "
					+ Difficulty.current().displayName() + " and autopet is Realistic-only");
			return;
		}
		for(Player p : Bukkit.getOnlinePlayers()) fire(p, t);
	}

	/**
	 * Apply one player's rule for a trigger, if they have one and none of its exceptions vetoes it.
	 * <p>
	 * Spectators are skipped: an idle m7 player sits in spectator and is not in the run at all, so moving their
	 * pet would only spam them a line per boss.
	 */
	private static void fire(Player p, Trigger t) {
		if(!Difficulty.manualPets() || p == null) return;
		// EVERY skip below says why, at BOSS verbosity.  A rule that does not fire and a rule that fires onto the
		// pet you already had out look identical from the chat - both are silent - so "autopet did nothing" is a
		// report with five possible causes and no way to tell them apart. These lines are how you tell.
		if(Utils.isSpectator(p)) {
			Utils.debug(Utils.DebugType.BOSS, "Autopet " + t.name() + ": " + Utils.getRealName(p) + " is a spectator");
			return;
		}
		if(t.isCycle()) {
			advanceRodCycle(p);
			return;
		}
		Rule rule = Pets.rule(p, t);
		if(rule == null || rule.pet() == null) {
			Utils.debug(Utils.DebugType.BOSS, "Autopet " + t.name() + ": " + Utils.getRealName(p) + " has no rule for it");
			return;
		}
		if(rule.excepts(Pets.equipped(p))) {
			Utils.debug(Utils.DebugType.BOSS, "Autopet " + t.name() + ": vetoed for " + Utils.getRealName(p)
					+ " - " + Pets.equipped(p).name() + " is an exception");
			return;
		}
		if(!Pets.equip(p, rule.pet(), ANNOUNCEMENT)) {
			Utils.debug(Utils.DebugType.BOSS, "Autopet " + t.name() + ": nothing to do for " + Utils.getRealName(p)
					+ " - " + rule.pet().name() + " was already out");
		}
	}

	/** Forget who was in combat and where they were in the rod cycle.  Both are per-session, neither is saved. */
	public static void reset() {
		lastHitTick.clear();
		rodCursor.clear();
	}
}
