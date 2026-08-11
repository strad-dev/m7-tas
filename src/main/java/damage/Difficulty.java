package damage;

import instructions.clear.Blessing;
import instructions.clear.ClearManager;
import plugin.Utils;

import java.util.Map;

/**
 * Classic vs realistic mode (DAMAGE_PLAN.md §0).
 * <p>
 * <b>This is a flag on inputs, not a second damage path.</b> Every one of the five things that differ is already
 * modelled as a target state or a stat input - never as a constant folded into a formula - so classic mode is
 * "the lookups answer yes unconditionally" and realistic mode is "the lookups read live state".  Keep it that way:
 * if any of the five gets baked into a hit calculation as a literal, realistic mode becomes a rewrite.
 *
 * <table>
 *   <caption>What the two modes change</caption>
 *   <tr><th>Input</th><th>Classic</th><th>Realistic</th></tr>
 *   <tr><td>Last Breath (up to x0.5 defense)</td><td>all 5 stacks</td><td>built by landing arrows</td></tr>
 *   <tr><td>Lethality (x0.91^4 defense)</td><td>all 4 stacks</td><td>built by hitting</td></tr>
 *   <tr><td>Ice Spray debuff (x1.1)</td><td>always</td><td>cast, 8 blocks, 5s</td></tr>
 *   <tr><td>Twilight Arrow Poison (x1.1)</td><td>always</td><td>bow hit, 20s</td></tr>
 *   <tr><td>Blessings</td><td>maxed</td><td>the chests the party actually opened</td></tr>
 * </table>
 *
 * Classic exists so a practising player can concentrate on movement and routing without also maintaining four
 * debuffs and a blessing count.  It is the mode the TAS was reasoned about in, and it is the default.
 * <p>
 * <b>Times from the two modes are not comparable</b>, so anything that records a run has to carry the mode with
 * it - see {@code plugin/RunResult} and the network plugin's leaderboard key.
 */
public enum Difficulty {
	CLASSIC, REALISTIC;

	// Maxed blessing levels, i.e. the classic table (§0).  These are TOTAL levels, the sum over every blessing of
	// that type the party collected, which is what the formulas below read.
	private static final int MAX_POWER = 29;
	private static final int MAX_TIME = 5;
	private static final int MAX_WISDOM = 14;
	private static final int MAX_STONE = 9;

	/** Blessing of Stone's contribution: a FLAT {@code +10.89} base Damage per level, so +98.01 at the maxed 9. */
	private static final double STONE_DAMAGE_PER_LEVEL = 10.89;

	/**
	 * A blessing's multiplicative bonus at a given total level: {@code 1 + 3.63% per level}, the same formula
	 * {@code Utils.broadcastBlessing} announces.  At the maxed levels that is Power x2.0527, Time x1.1815 and
	 * Wisdom x1.5082, matching §1.13.
	 * <p>
	 * The generic flat half of a blessing (+7.26 per level) is still not modelled - §1.13's base tables do not list
	 * it, and the worked aggregate in §1.10 reproduces exactly without it.  <b>Blessing of Stone is the exception</b>
	 * and is modelled, as {@link #stoneDamage()}: it is a real, measured source of base Damage rather than a
	 * bookkeeping detail, and leaving it out understated every hit.
	 */
	private static double blessingMultiplier(int totalLevel) {
		return 1.0 + 0.0363 * totalLevel;
	}

	private static Difficulty current = CLASSIC;

	public static Difficulty current() {
		return current;
	}

	public static void set(Difficulty d) {
		current = d;
	}

	/** Flip the mode and return the new one. */
	public static Difficulty toggle() {
		current = current == CLASSIC ? REALISTIC : CLASSIC;
		return current;
	}

	/** Parse "classic" / "realistic" (any case), or null. */
	public static Difficulty parse(String s) {
		if(s == null) return null;
		for(Difficulty d : values()) if(d.name().equalsIgnoreCase(s)) return d;
		return null;
	}

	/** Lower-case id, for the run payload and the leaderboard key. */
	public String id() {
		return name().toLowerCase(java.util.Locale.ROOT);
	}

	/** True while the debuff and defense-reduction lookups should answer "applied" without checking anything. */
	public static boolean debuffsAssumed() {
		return current == CLASSIC;
	}

	/**
	 * The multiplier a blessing type contributes to its stats right now.
	 * <p>
	 * In realistic mode this reads the run's actual chest history from {@code ClearManager}'s tally.  <b>If the
	 * clear phase is not part of the practice session, max blessings are assumed even in realistic mode</b> (§0) -
	 * there is no chest history to read, so the input falls back to the classic table.
	 */
	public static double blessing(Utils.BlessingType type) {
		return blessingMultiplier(blessingLevel(type));
	}

	/**
	 * Blessing of Stone's flat base-Damage contribution right now: {@code +10.89} per level, +98.01 at the maxed 9.
	 * <p>
	 * Flat, so it belongs to the base sum in {@link Profile#base()} and NOT to the multiplicative bucket the other
	 * blessings use.  Getting that wrong is the §1.13 mistake the stat pipeline's own doc warns about - a base source
	 * and a multiplier on the same stat are not interchangeable.
	 */
	public static double stoneDamage() {
		return STONE_DAMAGE_PER_LEVEL * blessingLevel(Utils.BlessingType.STONE);
	}

	/**
	 * The total collected level of one blessing type.  Classic answers from the maxed table; realistic reads the
	 * run's actual chest history, and falls back to the maxed table when this session has no clear phase to read.
	 */
	private static int blessingLevel(Utils.BlessingType type) {
		if(current == REALISTIC && clearPhaseInThisSession()) {
			int level = 0;
			for(Map.Entry<Blessing, Integer> e : ClearManager.blessingTally().entrySet()) {
				if(e.getKey().type() == type) level += e.getKey().level() * e.getValue();
			}
			return level;
		}
		return switch(type) {
			case POWER -> MAX_POWER;
			case TIME -> MAX_TIME;
			case WISDOM -> MAX_WISDOM;
			case STONE -> MAX_STONE;
			default -> 0;
		};
	}

	/**
	 * Whether this session has a clear phase to read blessings from.  The clear being live covers a run in
	 * progress; a non-empty tally covers the stretch after the clear has handed off to the boss chain.
	 */
	private static boolean clearPhaseInThisSession() {
		return ClearManager.isActive() || !ClearManager.blessingTally().isEmpty();
	}
}
