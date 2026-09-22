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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Autopet: swap the player's pet for them when something happens.
 * <p>
 * A rule is <b>(trigger, the pet to equip, an optional exception pet)</b>, and the exception means "fire this
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

	/** What autopet says when it moves a pet.  {@code Pets.equip} finishes the line with the pet's own name. */
	private static final String ANNOUNCEMENT = "<green>Autopet equipped your ";

	/**
	 * The four things that can move a pet.
	 * <p>
	 * The display data sits here rather than in {@link AutopetMenu} so a new trigger is one constant and appears
	 * in the menu for free, the same reason {@code Difficulty} carries its own {@code displayName}.
	 */
	public enum Trigger {
		/** The run going live: the countdown ending and the start door opening. */
		RUN_START("On Run Start", Material.OAK_DOOR, List.of("When the run starts and the", "first door opens.")),
		/** The first hit of a fight, after a lull.  See {@link #onCombatHit}. */
		ENTER_COMBAT("On Enter Combat", Material.IRON_SWORD, List.of("The first hit you land after", "a lull in the fighting.")),
		/** Maxor's phase starting. */
		MAXOR_SPAWN("When Maxor Spawns", Material.WITHER_SKELETON_SKULL, List.of("When the Maxor fight begins.")),
		/**
		 * Throwing a fishing rod.  <b>The odd one out</b>: it holds an ordered CYCLE rather than one pet, so each
		 * throw steps to the next pet in the list.  See {@link #advanceRodCycle}.
		 */
		ROD_SWAP("Rod Swap", Material.FISHING_ROD, List.of("Each throw of your Pitchin' Rod", "steps to the next pet in the cycle."));

		private final String displayName;
		private final Material icon;
		private final List<String> description;

		Trigger(String displayName, Material icon, List<String> description) {
			this.displayName = displayName;
			this.icon = icon;
			this.description = description;
		}

		public String displayName() {
			return displayName;
		}

		public Material icon() {
			return icon;
		}

		public List<String> description() {
			return description;
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
	 * A null {@code pet} is "off" and fires nothing; a null {@code exception} is "always fire".  A rule with an
	 * exception but no pet is kept rather than discarded, so a player can set the two halves in either order
	 * without the first one vanishing.
	 */
	public record Rule(PetType pet, PetType exception) {
		public boolean isOff() {
			return pet == null && exception == null;
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
	 * Step the Rod Swap cycle one place and summon what it lands on.
	 * <p>
	 * <b>A pet outside the cycle jumps to the front of it rather than advancing.</b>  "Advance" needs a position
	 * to advance from, and a pet that is not in the list has none; treating that as "start at the first" means a
	 * player who summoned something by hand gets the cycle back in one throw instead of an arbitrary place in it.
	 */
	private static void advanceRodCycle(Player p) {
		List<PetType> cycle = Pets.rodCycle(p);
		if(cycle.isEmpty()) return;
		int at = cycle.indexOf(Pets.equipped(p));
		PetType next = at < 0 ? cycle.getFirst() : cycle.get((at + 1) % cycle.size());
		Pets.equip(p, next, ANNOUNCEMENT);
	}

	// ==================== firing ====================

	/** Run one trigger for every real player online.  The two whole-run triggers are not about one player. */
	private static void fireForEveryone(Trigger t) {
		if(!Difficulty.manualPets()) return;
		for(Player p : Bukkit.getOnlinePlayers()) fire(p, t);
	}

	/**
	 * Apply one player's rule for a trigger, if they have one and the exception does not veto it.
	 * <p>
	 * Spectators are skipped: an idle m7 player sits in spectator and is not in the run at all, so moving their
	 * pet would only spam them a line per boss.
	 */
	private static void fire(Player p, Trigger t) {
		if(!Difficulty.manualPets() || p == null || Utils.isSpectator(p)) return;
		if(t.isCycle()) {
			advanceRodCycle(p);
			return;
		}
		Rule rule = Pets.rule(p, t);
		if(rule == null || rule.pet() == null) return;
		if(rule.exception() != null && Pets.equipped(p) == rule.exception()) return;
		Pets.equip(p, rule.pet(), ANNOUNCEMENT);
	}

	/** Forget who was in combat.  Nothing else here is per-run state. */
	public static void reset() {
		lastHitTick.clear();
	}
}
