package death;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
 * <b>Two clocks, deliberately.</b> Cooldowns run on the absolute server tick ({@code Utils.serverTick()}),
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
		SPIRIT("Spirit", 600, 60, "<light_purple>",
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
	/** Absolute server tick each player stops being immune.  Absent = not immune. */
	private static final Map<UUID, Integer> immuneUntil = new HashMap<>();

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
		if(now < immuneUntil.getOrDefault(p.getUniqueId(), Integer.MIN_VALUE)) return true;

		Saver used = pick(p, now);
		if(used == null) return false;

		readyAt.computeIfAbsent(p.getUniqueId(), k -> new EnumMap<>(Saver.class))
				.put(used, now + used.cooldownTicks);
		immuneUntil.put(p.getUniqueId(), now + used.immuneTicks);
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
			if(s.isWorn() && !isWorn(s, helmet)) continue;
			return s;
		}
		return null;
	}

	private static boolean onCooldown(Player p, Saver s, int now) {
		EnumMap<Saver, Integer> mine = readyAt.get(p.getUniqueId());
		return mine != null && now < mine.getOrDefault(s, Integer.MIN_VALUE);
	}

	/** True if {@code helmet} is the item behind {@code s}.  Both masks are identified by display name, as everywhere else. */
	private static boolean isWorn(Saver s, ItemStack helmet) {
		return switch(s) {
			case BONZO -> FakePlayerInventory.isBonzoMask(helmet);
			case SPIRIT -> FakePlayerInventory.isSpiritMask(helmet);
			case PHOENIX -> false;
		};
	}

	/** Ticks left on a saver's cooldown, 0 when it is ready. */
	private static int remaining(Player p, Saver s, int now) {
		EnumMap<Saver, Integer> mine = readyAt.get(p.getUniqueId());
		if(mine == null) return 0;
		return Math.max(0, mine.getOrDefault(s, Integer.MIN_VALUE) - now);
	}

	// ==================== the durability bar ====================

	/**
	 * Push each worn saver's remaining cooldown onto its item's durability bar.  Called once every 20 ticks by
	 * {@link Deaths}, which is all the resolution a bar needs, and also the moment a proc lands so the bar empties
	 * on the same tick the save happens.
	 * <p>
	 * <b>{@code max_damage} only renders on an unstackable item</b>, and these are player heads, which stack. So the
	 * write sets {@code max_stack_size} to 1 as well and {@link #clearDurability} removes both again - leave the
	 * stack size behind and the mask stays a one-per-slot item for the rest of the session.  The damage is clamped
	 * one below the maximum so a fresh cooldown never reads as a broken item.
	 */
	static void refreshDurability(Player p) {
		int now = Utils.serverTick();
		for(Saver s : Saver.values()) {
			if(!s.isWorn()) continue;
			int left = remaining(p, s, now);
			if(left > 0) writeDurability(p, s, left);
			else clearDurability(p, s);
		}
	}

	private static void writeDurability(Player p, Saver s, int remainingTicks) {
		ItemStack helmet = p.getInventory().getHelmet();
		if(!isWorn(s, helmet)) return;
		// The damage component lives on Damageable, not ItemMeta.  Every CraftMetaItem is one, so this is a cast in
		// practice, but the pattern keeps a future meta type that isn't from silently NPEing the bar.
		if(!(helmet.getItemMeta() instanceof Damageable m)) return;
		m.setMaxStackSize(1);
		m.setMaxDamage(s.cooldownTicks);
		m.setDamage(Math.min(remainingTicks, s.cooldownTicks - 1));
		helmet.setItemMeta(m);
	}

	/** Take the bar back off, so a ready mask looks exactly like a mask that never procced. */
	private static void clearDurability(Player p, Saver s) {
		ItemStack helmet = p.getInventory().getHelmet();
		if(!isWorn(s, helmet)) return;
		if(!(helmet.getItemMeta() instanceof Damageable m)) return;
		if(!m.hasMaxDamage() && !m.hasDamage()) return; // nothing to undo, so don't churn the stat cache every 20t
		m.resetDamage();
		m.setMaxDamage(null);
		m.setMaxStackSize(null);
		helmet.setItemMeta(m);
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

	private static String segments(Player p, boolean leadingSeparator) {
		if(!damage.Difficulty.deathsEnabled()) return "";
		int now = Utils.serverTick();
		StringBuilder sb = new StringBuilder();
		for(Saver s : Saver.values()) {
			int left = remaining(p, s, now);
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
		immuneUntil.clear();
	}
}
