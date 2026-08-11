package damage;

import org.bukkit.entity.Player;

/**
 * Dungeon class bonuses (DAMAGE_PLAN.md §1.14).  Every figure is at <b>class level 50</b>; sub-50 scaling is
 * deliberately not modelled at all - level 50 is the practice assumption, like maxed gear everywhere else.  If a
 * class level ever needs to matter it becomes a new source here, not a rewrite.
 * <p>
 * Every bonus comes in a normal and a <b>solo</b> value, the solo one applying when the player is the only one in
 * the party on that class.  That is party composition, so it is evaluated live via
 * {@link DungeonClass#isSoloOnClass}, never authored per class.
 * <p>
 * The bonuses land in three different buckets and must not be conflated:
 * <ul>
 *   <li>{@link #stats} - BASE stat sources, so they go in before the stat stage and get multiplied by it.</li>
 *   <li>{@link #damageMultiplier} - §7 multiplicative (Berserk's melee x1.775, the Archer's x3.3 / x0.75).</li>
 *   <li>{@link #damageAdditive} - §7 additive (Berserk's post-kill bonus and repeated-hit stack).</li>
 * </ul>
 */
public final class ClassBonuses {
	private ClassBonuses() {}

	/** Berserk's repeated-hit stack: +165% per consecutive hit on the same monster, +180% solo. */
	public static final double BERSERK_REPEAT_PER_HIT = 165.0;
	public static final double BERSERK_REPEAT_PER_HIT_SOLO = 180.0;
	/** ...capped at +950% (+1200% solo), i.e. the 6th consecutive hit (7th when solo). */
	public static final double BERSERK_REPEAT_CAP = 950.0;
	public static final double BERSERK_REPEAT_CAP_SOLO = 1200.0;
	/** Berserk's first hit after killing a monster: +57.5% (+77.5% solo), one hit only. */
	public static final double BERSERK_POST_KILL = 57.5;
	public static final double BERSERK_POST_KILL_SOLO = 77.5;
	/** ...and it EXPIRES 5s after the kill if unspent.  A separate timer from the combo counter's 3s (§1.14). */
	public static final int BERSERK_POST_KILL_TICKS = 100;
	/** Berserk's ultimate ({@code drop}): x1.5 melee for 15s, 60s cooldown. */
	public static final double BERSERK_ULTIMATE = 1.5;
	public static final int BERSERK_ULTIMATE_TICKS = 300;
	/** Berserk's extra swing range, the same attribute that extends its Cleave radius (§7). */
	public static final double BERSERK_SWING_RANGE = 5.0;
	public static final double BERSERK_SWING_RANGE_SOLO = 5.5;

	/**
	 * The class's BASE stat contribution.  Only the Mage has one: +500 Intelligence and +15 Ability Damage
	 * (+750 / +20 solo).  Being base sources they land before the stat stage, so the +500 is worth ~+891 after
	 * the Intelligence product.
	 */
	public static StatBlock stats(DungeonClass clazz, boolean solo) {
		if(clazz != DungeonClass.MAGE) return StatBlock.EMPTY;
		return StatBlock.of(Stat.INTELLIGENCE, solo ? 750 : 500, Stat.ABILITY_DAMAGE, solo ? 20 : 15);
	}

	/**
	 * The class's §7 multiplicative factor on a hit, by path.  Includes Berserk's {@code drop} ultimate, which is
	 * a second melee multiplier on top of the class one while it is running.
	 */
	public static double damageMultiplier(Player p, DungeonClass clazz, DamagePath path, boolean solo) {
		double m = switch(clazz) {
			// The mage beam counts as melee, so a Berserk beaming would take this too - which is moot, since a
			// Berserk is not a Mage and has no beam.
			case BERSERK -> path.isMelee() ? (solo ? 2.175 : 1.775) : 1.0;
			case ARCHER -> switch(path) {
				case BOW -> solo ? 3.8 : 3.3;
				case MELEE, BEAM -> 0.75;
				case ABILITY -> 1.0;
			};
			default -> 1.0;
		};
		if(clazz == DungeonClass.BERSERK && path.isMelee() && CombatState.berserkUltimateActive(p)) {
			m *= BERSERK_ULTIMATE;
		}
		return m;
	}

	/**
	 * The class's §7 ADDITIVE contribution for this hit, as a percentage.  Berserk only: its post-kill bonus and
	 * its repeated-hit stack, both of which live in {@link CombatState} because they are per-player and
	 * per-target respectively.  Pure - {@link Damage} is what advances the counters, and only for a primary hit.
	 * <p>
	 * This is the largest additive source in the plan - at the cap a Berserk carries roughly as much again as
	 * everything in §7 combined, and then multiplies by 1.775 on top.
	 */
	public static double damageAdditive(Player p, DungeonClass clazz, DamagePath path,
			java.util.UUID target, boolean solo) {
		if(clazz != DungeonClass.BERSERK) return 0;
		double sum = 0;
		// The post-kill bonus applies to the first hit after a kill, melee OR ranged.
		if(CombatState.hasPostKillBuff(p)) sum += solo ? BERSERK_POST_KILL_SOLO : BERSERK_POST_KILL;
		if(path.isMelee() || path == DamagePath.BOW) {
			int repeats = CombatState.repeatHits(p, target);
			double per = solo ? BERSERK_REPEAT_PER_HIT_SOLO : BERSERK_REPEAT_PER_HIT;
			double cap = solo ? BERSERK_REPEAT_CAP_SOLO : BERSERK_REPEAT_CAP;
			sum += Math.min(repeats * per, cap);
		}
		return sum;
	}

	/** Berserk's swing range, which extends both its reach attribute and its Cleave radius. */
	public static double swingRange(DungeonClass clazz, boolean solo) {
		if(clazz != DungeonClass.BERSERK) return 0;
		return solo ? BERSERK_SWING_RANGE_SOLO : BERSERK_SWING_RANGE;
	}
}
