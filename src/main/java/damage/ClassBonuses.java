package damage;

import org.bukkit.entity.Player;

/**
 * Dungeon class bonuses (MAP.md §1.14), all at class level 50; sub-50 isn't modelled, like maxed gear elsewhere.
 * <p>
 * Each bonus has a normal and a solo value (only player on that class), evaluated live via
 * {@link DungeonClass#isSoloOnClass}. Three buckets, don't conflate them:
 * <ul>
 *   <li>{@link #stats} - BASE stat sources, multiplied by the stat stage.</li>
 *   <li>{@link #damageMultiplier} - §7 multiplicative (Berserk melee x1.775, Archer x3.3 / x0.75).</li>
 *   <li>{@link #damageAdditive} - §7 additive (Berserk post-kill bonus and repeated-hit stack).</li>
 * </ul>
 */
public final class ClassBonuses {
	private ClassBonuses() {}

	/** Berserk's repeated-hit stack: +165% per consecutive hit on the same monster, +180% solo. */
	public static final double BERSERK_REPEAT_PER_HIT = 165.0;
	public static final double BERSERK_REPEAT_PER_HIT_SOLO = 180.0;
	/** Capped at +950% (+1200% solo), i.e. the 6th consecutive hit (7th solo). */
	public static final double BERSERK_REPEAT_CAP = 950.0;
	public static final double BERSERK_REPEAT_CAP_SOLO = 1200.0;
	/** Berserk's first hit after killing a monster: +57.5% (+77.5% solo), one hit only. */
	public static final double BERSERK_POST_KILL = 57.5;
	public static final double BERSERK_POST_KILL_SOLO = 77.5;
	/** Expires 5s after the kill if unspent. Separate timer from the combo counter's 3s (§1.14). */
	public static final int BERSERK_POST_KILL_TICKS = 100;
	/** Berserk's ultimate ({@code drop}): x1.5 melee for 15s, 60s cooldown. */
	public static final double BERSERK_ULTIMATE = 1.5;
	public static final int BERSERK_ULTIMATE_TICKS = 300;
	/** Berserk's extra swing range, which also extends its Cleave radius (§7). */
	public static final double BERSERK_SWING_RANGE = 5.0;
	public static final double BERSERK_SWING_RANGE_SOLO = 5.5;

	/** BASE stats. Mage only: +500 Int, +15 Ability Damage (+750 / +20 solo). The +500 is ~+891 after the Int product. */
	public static StatBlock stats(DungeonClass clazz, boolean solo) {
		if(clazz != DungeonClass.MAGE) return StatBlock.EMPTY;
		return StatBlock.of(Stat.INTELLIGENCE, solo ? 750 : 500, Stat.ABILITY_DAMAGE, solo ? 20 : 15);
	}

	/** §7 multiplicative factor by path, including Berserk's {@code drop} ultimate as a second melee multiplier. */
	public static double damageMultiplier(Player p, DungeonClass clazz, DamagePath path, boolean solo) {
		double m = switch(clazz) {
			// Beam counts as melee, but moot: a Berserk has no beam.
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
	 * §7 ADDITIVE percent for this hit. Berserk only: post-kill bonus (per player) and repeated-hit stack (per
	 * target), both in {@link CombatState}. Pure; {@link Damage} advances the counters, primary hits only.
	 * Largest additive source: at the cap it's about all of §7 again, then x1.775 on top.
	 */
	public static double damageAdditive(Player p, DungeonClass clazz, DamagePath path,
			java.util.UUID target, boolean solo) {
		if(clazz != DungeonClass.BERSERK) return 0;
		double sum = 0;
		// Post-kill bonus: first hit after a kill, melee OR ranged.
		if(CombatState.hasPostKillBuff(p)) sum += solo ? BERSERK_POST_KILL_SOLO : BERSERK_POST_KILL;
		if(path.isMelee() || path == DamagePath.BOW) {
			int repeats = CombatState.repeatHits(p, target);
			double per = solo ? BERSERK_REPEAT_PER_HIT_SOLO : BERSERK_REPEAT_PER_HIT;
			double cap = solo ? BERSERK_REPEAT_CAP_SOLO : BERSERK_REPEAT_CAP;
			sum += Math.min(repeats * per, cap);
		}
		return sum;
	}

	/** Berserk's swing range; extends both reach and Cleave radius. */
	public static double swingRange(DungeonClass clazz, boolean solo) {
		if(clazz != DungeonClass.BERSERK) return 0;
		return solo ? BERSERK_SWING_RANGE_SOLO : BERSERK_SWING_RANGE;
	}
}
