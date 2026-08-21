package death;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import plugin.FakePlayerInventory;
import plugin.Utils;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The three things that stop an ultra-realistic instakill: Bonzo's Mask, the Spirit Mask and the Phoenix pet.
 * <p>
 * <b>One entry point.</b> {@link #tryProc(Player)} is called by {@link Deaths#kill} and by nothing else, so every
 * kill site in the plugin inherits the same order of precedence and the same immunity window without knowing any of
 * this exists.  It answers "this death was cheated", not "an item was consumed": an already-immune player answers
 * true without spending anything, which is what makes a burst of hits in one window cost a single proc.
 * <p>
 * <b>Precedence.</b> A worn mask wins over the pet, and the two masks can never compete because both are helmets
 * and a player wears one hat.  The pet is the fallback: the player is ASSUMED to have a Phoenix out whenever no
 * mask is available, so there is no item to look for and nothing to show a durability bar on.
 * <p>
 * <b>A proc starts two clocks.</b> The immunity window, which is how long the next hit is free, and the item's
 * cooldown.  The action bar shows them on ONE segment in that order - the immunity counting down first, then the
 * cooldown - see {@link #segmentTicks}.
 * <p>
 * <b>Both run on the absolute server tick</b> ({@code Utils.serverTick()}),
 * not on the run-relative {@code Utils.runTick()}, because a cooldown has to survive a phase change and must not
 * jump when the run clock is re-anchored.  {@link Deaths#reset()} is what clears them between runs.
 */
public final class CheatDeath {
	private CheatDeath() {}

	/**
	 * One life-saver: how long it goes on cooldown for, how long it makes its owner immune, and how its action-bar
	 * segment is coloured.
	 * <p>
	 * Declaration order IS the precedence order in {@link #pick} and the segment order in
	 * {@link #actionBarSuffix}, so the bar's segments never swap places under a player as their timers run out -
	 * the same reasoning as Storm's armed-pillar segments.
	 */
	public enum Saver {
		/** Bonzo's Mask, worn: 180s cooldown, 3s immune. */
		BONZO("Bonzo", 3600, 60, "<blue>",
				"<green>Your <blue>Bonzo's Mask</blue> saved your life!"),
		/** Spirit Mask, worn: 30s cooldown, 3s immune. */
		SPIRIT("Spirit", 600, 60, "<dark_purple>",
				"<gold>Second Wind Activated<green>!  Your Spirit Mask saved your life!"),
		/** The Phoenix pet, assumed out: 60s cooldown, 4s immune, and NO item, so no durability bar. */
		PHOENIX("Phoenix", 1200, 80, "<gold>",
				"<yellow>Your <red>Phoenix Pet</red> saved you from certain death!");

		/** Action-bar label, kept short so it sits next to a boss HUD's own segments. */
		public final String label;
		public final int cooldownTicks;
		public final int immuneTicks;
		/** MiniMessage colour of the action-bar label; the countdown itself is always white. */
		public final String colour;
		/**
		 * The whole chat line the saved player reads, verbatim from Hypixel's own wording.
		 * <p>
		 * A full line per saver rather than a template with the item's name slotted in, because the three do not
		 * share a shape - Spirit Mask leads with "Second Wind Activated!" and Phoenix says "certain death".
		 */
		public final String chatLine;

		Saver(String label, int cooldownTicks, int immuneTicks, String colour, String chatLine) {
			this.label = label;
			this.cooldownTicks = cooldownTicks;
			this.immuneTicks = immuneTicks;
			this.colour = colour;
			this.chatLine = chatLine;
		}

		/** True if this saver is a worn item, i.e. one that carries the cooldown on its own durability bar. */
		boolean isWorn() {
			return this != PHOENIX;
		}
	}

	/** Absolute server tick each player's saver comes off cooldown.  Absent = ready. */
	private static final Map<UUID, EnumMap<Saver, Integer>> readyAt = new HashMap<>();

	/**
	 * A live immunity window: which saver opened it, and the absolute server tick it shuts on.
	 * <p>
	 * <b>The saver is carried, not just the tick</b>, because the action bar counts the immunity down on that
	 * saver's own segment before switching to its cooldown - see {@link #segmentTicks}.  Only one window can be open
	 * at a time (a proc inside one is refused outright), so one entry per player is the whole story.
	 */
	private record Immunity(Saver from, int untilTick) {}

	/** The open immunity window per player.  Absent = not immune. */
	private static final Map<UUID, Immunity> immunity = new HashMap<>();

	/**
	 * Spend whatever would keep {@code p} alive right now.
	 *
	 * @return true if this death was cheated - either because a proc landed or because {@code p} is still inside an
	 *         earlier proc's immunity window.  False means nothing was available and the player dies.
	 */
	public static boolean tryProc(Player p) {
		int now = Utils.serverTick();
		// Inside a live immunity window: the death is refused and NOTHING is spent.  This is what makes a second
		// storm bolt or a same-tick pair of triggers cost one proc rather than one each.
		if(liveImmunity(p, now) != null) return true;

		Saver used = pick(p, now);
		if(used == null) return false;

		readyAt.computeIfAbsent(p.getUniqueId(), k -> new EnumMap<>(Saver.class))
				.put(used, now + used.cooldownTicks);
		immunity.put(p.getUniqueId(), new Immunity(used, now + used.immuneTicks));
		if(used.isWorn()) writeDurability(p, used, used.cooldownTicks);

		// To the saved player, not the party: this is their item and their cooldown, and Hypixel words it in the
		// second person.  playLocalSound plays it at them (and, for a fake player, at whoever is spectating them).
		p.sendMessage(Utils.msg(used.chatLine));
		Utils.playLocalSound(p, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 2.0f);
		Utils.debug(Utils.DebugType.BOSS, Utils.getRealName(p) + " cheated death with " + used.label);
		return true;
	}

	/** The first saver in precedence order that {@code p} actually has available, or null if none. */
	private static Saver pick(Player p, int now) {
		ItemStack helmet = p.getInventory().getHelmet();
		for(Saver s : Saver.values()) {
			if(onCooldown(p, s, now)) continue;
			// A worn saver has to actually be on the head.  The pet has no item, so it is always "held".
			if(s.isWorn() && !isSaverItem(s, helmet)) continue;
			return s;
		}
		return null;
	}

	/**
	 * Whether {@code s} is still cooling down for {@code p}.
	 * <p>
	 * Reads {@link #remaining} rather than looking the tick up itself, so <b>one</b> place in this class knows how an
	 * absent entry is answered.  This was a second lookup with a {@code MIN_VALUE} sentinel - harmless as a bare
	 * comparison, but it is the idiom that overflowed in {@link #remaining}, and two of them invites the bug back.
	 */
	private static boolean onCooldown(Player p, Saver s, int now) {
		return remaining(p, s, now) > 0;
	}

	/**
	 * True if {@code stack} IS the item behind {@code s}, wherever it happens to be.  Both masks are identified by
	 * display name, as everywhere else in the plugin.
	 * <p>
	 * Asked of the helmet when deciding whether a saver is available (it has to be on the head to proc) and of every
	 * slot when taking a cooldown bar back off (the item can have been moved by then).
	 */
	private static boolean isSaverItem(Saver s, ItemStack stack) {
		return switch(s) {
			case BONZO -> FakePlayerInventory.isBonzoMask(stack);
			case SPIRIT -> FakePlayerInventory.isSpiritMask(stack);
			case PHOENIX -> false;
		};
	}

	/** The player's open immunity window, or null if it has shut (or never opened). */
	private static Immunity liveImmunity(Player p, int now) {
		Immunity live = immunity.get(p.getUniqueId());
		return live != null && now < live.untilTick() ? live : null;
	}

	/**
	 * What one saver's action-bar segment counts down right now: <b>its immunity window first, then its cooldown</b>.
	 * <p>
	 * A proc opens both clocks at once, and the immunity is the one that matters in the moment - it is how long the
	 * next hit is still free.  So the segment shows that until it runs out and then falls back to the cooldown,
	 * which is why the number JUMPS UP at the changeover (Bonzo's 60t immunity gives way to ~3540t of cooldown).
	 * Only the saver that opened the window is affected; the other two show their cooldowns throughout.
	 * <p>
	 * Every immunity is far shorter than its own cooldown, so a segment can never show an immunity for a saver that
	 * is already ready - but the fallback here is the cooldown, so it would read correctly even if that changed.
	 *
	 * @return 0 when there is nothing to show for this saver.
	 */
	private static int segmentTicks(Player p, Saver s, int now) {
		Immunity live = liveImmunity(p, now);
		if(live != null && live.from() == s) return live.untilTick() - now;
		return remaining(p, s, now);
	}

	/**
	 * Ticks left on a saver's cooldown, 0 when it is ready.
	 * <p>
	 * <b>No sentinel.</b>  An absent entry means "never used", and it has to be answered by the null check, not by
	 * subtracting {@code now} from some floor value: {@code Integer.MIN_VALUE - now} OVERFLOWS to a large positive,
	 * so a sentinel here made every saver a player had not used report a huge remaining - all three showing in the
	 * action bar the moment one of them procced, and an empty durability bar drawn on a mask that was ready.
	 */
	private static int remaining(Player p, Saver s, int now) {
		EnumMap<Saver, Integer> mine = readyAt.get(p.getUniqueId());
		if(mine == null) return 0;
		Integer readyTick = mine.get(s);
		return readyTick == null ? 0 : Math.max(0, readyTick - now);
	}

	// ==================== the durability bar ====================

	/**
	 * Keep each worn saver's durability bar honest.  Called EVERY tick by {@link Deaths}; the drawing is throttled to
	 * a 20-tick grid inside, which is all the resolution a bar needs.
	 * <p>
	 * <b>The CLEAR is not throttled</b>, and that is the point: it fires on the exact tick the cooldown runs out.
	 * On the grid, a cooldown expiring at tick 613 would keep a part-empty bar until 620 - the mask reads as still
	 * cooling down while it is in fact ready, which is the one thing the bar exists to tell you.  The entry is then
	 * dropped, so the (whole-inventory) clear runs once per cooldown rather than every tick afterwards.
	 * <p>
	 * <b>{@code max_damage} only renders on an unstackable item</b>, and these are player heads, which stack. So the
	 * write sets {@code max_stack_size} to 1 as well and {@link #clearDurability} removes both again - leave the
	 * stack size behind and the mask stays a one-per-slot item for the rest of the session.  The damage is clamped
	 * one below the maximum so a fresh cooldown never reads as a broken item.
	 */
	static void refreshDurability(Player p) {
		EnumMap<Saver, Integer> mine = readyAt.get(p.getUniqueId());
		if(mine == null) return;
		int now = Utils.serverTick();
		for(Saver s : Saver.values()) {
			if(!s.isWorn()) continue;
			Integer readyTick = mine.get(s);
			if(readyTick == null) continue; // never used, or already cleared
			int left = readyTick - now;
			if(left > 0) {
				if(now % DRAW_INTERVAL_TICKS == 0) writeDurability(p, s, left);
			} else {
				clearDurability(p, s);
				mine.remove(s); // done with: stop scanning for it every tick
			}
		}
	}

	/** How often a running cooldown's bar is redrawn.  The CLEAR ignores this - see {@link #refreshDurability}. */
	private static final int DRAW_INTERVAL_TICKS = 20;

	private static void writeDurability(Player p, Saver s, int remainingTicks) {
		ItemStack helmet = p.getInventory().getHelmet();
		if(!isSaverItem(s, helmet)) return;
		// The damage component lives on Damageable, not ItemMeta.  Every CraftMetaItem is one, so this is a cast in
		// practice, but the pattern keeps a future meta type that isn't from silently NPEing the bar.
		if(!(helmet.getItemMeta() instanceof Damageable m)) return;
		m.setMaxStackSize(1);
		m.setMaxDamage(s.cooldownTicks);
		m.setDamage(Math.min(remainingTicks, s.cooldownTicks - 1));
		helmet.setItemMeta(m);
	}

	/**
	 * Take the bar back off, so a ready mask looks exactly like a mask that never procced.
	 * <p>
	 * <b>Searches the WHOLE inventory, not the helmet.</b>  The bar lives on an item and the item moves: take the
	 * mask off mid-cooldown and a helmet-only clear never runs, so the cooldown expires with nothing watching and
	 * the mask keeps a half-empty bar - for the rest of the session if the run ends before it goes back on, since
	 * {@link #reset} wipes the cooldown that would have triggered the clear.
	 * <p>
	 * Slots are read through {@code getStorageContents} / {@code getArmorContents} / the offhand and written back
	 * explicitly, rather than indexed 0..40 or mutated through a mirror: which slots a player inventory's
	 * {@code getContents} spans has moved between versions, and these three accessors have not.
	 */
	private static void clearDurability(Player p, Saver s) {
		PlayerInventory inv = p.getInventory();
		ItemStack[] storage = inv.getStorageContents();
		if(stripBar(s, storage)) inv.setStorageContents(storage);
		ItemStack[] armour = inv.getArmorContents();
		if(stripBar(s, armour)) inv.setArmorContents(armour);
		ItemStack[] offhand = {inv.getItemInOffHand()};
		if(stripBar(s, offhand)) inv.setItemInOffHand(offhand[0]);
	}

	/** Strip {@code s}'s cooldown bar off every copy of its item in {@code items}.  @return whether anything changed. */
	private static boolean stripBar(Saver s, ItemStack[] items) {
		boolean changed = false;
		for(ItemStack it : items) {
			if(!isSaverItem(s, it)) continue;
			if(!(it.getItemMeta() instanceof Damageable m)) continue;
			if(!m.hasMaxDamage() && !m.hasDamage()) continue; // nothing to undo on this copy
			m.resetDamage();
			m.setMaxDamage(null);
			m.setMaxStackSize(null);
			it.setItemMeta(m);
			changed = true;
		}
		return changed;
	}

	// ==================== the action bar ====================

	/** What separates one action-bar segment from the next, matching the boss HUDs. */
	private static final String SEPARATOR = " <dark_gray>| ";

	/**
	 * The cooldown segments to hang off the END of somebody else's action bar, each
	 * {@code " | <colour>Label <white>Nt"}, in declaration order.  Empty when everything is ready.
	 * <p>
	 * Per player, since cooldowns are per player.  Appended by {@code Utils.sendActionBar}, which is the one place
	 * that does the appending, so every HUD in the plugin picks it up without knowing about it.  Empty outside
	 * ultra-realistic, so no other mode has to know about it either.
	 */
	public static String actionBarSuffix(Player p) {
		return segments(p, true);
	}

	/**
	 * The same segments as their own whole action bar, i.e. with no leading separator.  Used when nothing else owns
	 * the bar this tick - the Goldor phase has no HUD of its own, and a cooldown still has to be readable there.
	 */
	public static String actionBarOnly(Player p) {
		return segments(p, false);
	}

	/**
	 * True if {@code p} has any saver on cooldown, i.e. whether {@link #actionBarSuffix} would produce anything.
	 * <p>
	 * A predicate rather than "is the string empty", so {@code Deaths}' per-tick fallback can decide whether to write
	 * the bar at all without building a string it would then throw away.
	 */
	public static boolean hasCooldowns(Player p) {
		if(!damage.Difficulty.deathsEnabled()) return false;
		int now = Utils.serverTick();
		for(Saver s : Saver.values()) if(segmentTicks(p, s, now) > 0) return true;
		return false;
	}

	private static String segments(Player p, boolean leadingSeparator) {
		if(!damage.Difficulty.deathsEnabled()) return "";
		int now = Utils.serverTick();
		StringBuilder sb = new StringBuilder();
		for(Saver s : Saver.values()) {
			int left = segmentTicks(p, s, now);
			if(left <= 0) continue;
			if(leadingSeparator || !sb.isEmpty()) sb.append(SEPARATOR);
			sb.append(s.colour).append(s.label).append(" <white>").append(left).append("t");
		}
		return sb.toString();
	}

	// ==================== lifecycle ====================

	/** Forget every cooldown and immunity window, and take the durability bars back off.  Run start and run end. */
	static void reset() {
		for(Player p : Bukkit.getOnlinePlayers()) {
			for(Saver s : Saver.values()) if(s.isWorn()) clearDurability(p, s);
		}
		readyAt.clear();
		immunity.clear();
	}
}
