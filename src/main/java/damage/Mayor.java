package damage;

import java.util.Locale;

/**
 * SkyBlock mayor in office: second dungeon setting, alongside {@link Difficulty} (MAP.md §0).
 * <p>
 * M7 was authored against Paul with Jerry: EZPZ's flat +10 bonus score, a boost to every blessing, Jerry's +10%
 * stats and ordinary Master Mode mob health. Every real mayor collapses into one of five cases:
 *
 * <table>
 *   <caption>What each mayor changes</caption>
 *   <tr><th>Mayor</th><th>Bonus score</th><th>Blessing effect increase</th><th>Jerry +10%</th><th>Mob health</th><th>Board</th></tr>
 *   <tr><td>{@link #JERRY_PAUL} (default)</td><td>+10 (EZPZ)</td><td>x1.815</td><td>yes</td><td>x1</td><td>Paul</td></tr>
 *   <tr><td>{@link #PAUL}</td><td>+10 (EZPZ)</td><td>x1.815</td><td>no</td><td>x1</td><td>Paul</td></tr>
 *   <tr><td>{@link #JERRY}</td><td>none</td><td>x1.452</td><td>yes</td><td>x1</td><td>Other</td></tr>
 *   <tr><td>{@link #OTHER}</td><td>none</td><td>x1.452</td><td>no</td><td>x1</td><td>Other</td></tr>
 *   <tr><td>{@link #DERPY}</td><td>none</td><td>x1.452</td><td>no</td><td><b>x2</b></td><td>Derpy</td></tr>
 * </table>
 *
 * Paul's {@link #PAUL_BLESSING_BOOST} is one of four terms of {@link Blessings#effectIncrease()}, scaling every
 * blessing figure, percent and flat; the only one not assumed maxed. {@link #OTHER} = no dungeon perks, the honest
 * baseline; {@link #DERPY} = that plus double health.
 * <p>
 * <b>Like {@link Difficulty}, a flag on inputs, never a second path.</b> All four effects are read live where the
 * numbers already live: {@code ClearManager.bonus}, {@link Blessings#effectIncrease()},
 * {@code Profile.additivePercent}, {@link MobStats.MobStat#internalHealth}. A fifth effect belongs in whichever
 * class owns that number.
 * <p>
 * Server-wide global, read live; only HP is latched at spawn, so a mid-run change leaves existing mobs on old HP.
 * Hence {@code /m7practice} sets it before the run arms and the network always sends a value instead of inheriting.
 * <p>
 * {@code plugin/RunResult} carries the id. The network folds the five onto three boards (last column); that
 * mapping lives there, not here.
 */
public enum Mayor {
	/** Default: Paul's perks plus Jerry's +10% stats. */
	JERRY_PAUL(true, true, false),
	/** EZPZ +10 bonus score and boosted blessings, no Jerry. */
	PAUL(true, false, false),
	/** Jerry's +10% stats, no Paul perks. */
	JERRY(false, true, false),
	/** No dungeon perks: no +10, unboosted blessings, no Jerry, ordinary HP. */
	OTHER(false, false, false),
	/** No perks, and every mob has x2 HP. */
	DERPY(false, false, true);

	private final boolean paul, jerry, doubleHealth;

	Mayor(boolean paul, boolean jerry, boolean doubleHealth) {
		this.paul = paul;
		this.jerry = jerry;
		this.doubleHealth = doubleHealth;
	}

	/** EZPZ: flat +10 bonus score, why a perfect run scores 319. */
	public static final int PAUL_SCORE_BONUS = 10;

	/** The one effect here that isn't just a lost perk. */
	public static final double DERPY_HEALTH_MULTIPLIER = 2.0;

	/**
	 * Paul's Benediction, "blessings are 25% stronger". Effect increase {@code 1.25 x 1.10 x 1.20 x 1.10 = 1.815}
	 * with it, {@code 1.452} without; scales percent and flat alike.
	 */
	public static final double PAUL_BLESSING_BOOST = 1.25;

	/** Jerry's +10% Strength, Crit Damage, Intelligence and Ability Damage, additive. */
	public static final double JERRY_STAT_BONUS = 10;

	private static Mayor current = JERRY_PAUL;

	public static Mayor current() {
		return current;
	}

	/** Drops the stat cache, or cached aggregates keep the old mayor's blessings for the cache window. */
	public static void set(Mayor m) {
		if(m == null) return;
		current = m;
		Stats.invalidateAll();
	}

	/** Next mayor in declaration order, wrapping. */
	public static Mayor toggle() {
		Mayor[] all = values();
		set(all[(current.ordinal() + 1) % all.length]);
		return current;
	}

	/** Previous mayor, wrapping (menu right-click). Goes through {@link #set}, so drops the stat cache too. */
	public static Mayor toggleBack() {
		Mayor[] all = values();
		set(all[(current.ordinal() + all.length - 1) % all.length]);
		return current;
	}

	/**
	 * Any case, {@code -} for {@code _} ({@code jerry-paul}), or null; network ids go straight through.
	 * {@code /dungeonsettings mayor} and {@code /m7practice} both parse here so they can't disagree.
	 */
	public static Mayor parse(String s) {
		if(s == null) return null;
		String n = s.replace('-', '_');
		for(Mayor m : values()) if(m.name().equalsIgnoreCase(n)) return m;
		// "none" = no dungeon perks = OTHER.
		return s.equalsIgnoreCase("none") ? OTHER : null;
	}

	/** For run payload and network setting. Must match the network's ids. */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** 2 under Derpy, else 1. */
	public static double healthMultiplier() {
		return current.doubleHealth ? DERPY_HEALTH_MULTIPLIER : 1.0;
	}

	/** {@link #PAUL_SCORE_BONUS} with Paul, else 0. */
	public static int scoreBonus() {
		return current.paul ? PAUL_SCORE_BONUS : 0;
	}

	/** {@link #JERRY_STAT_BONUS} with Jerry, else 0. */
	public static double jerryBonus() {
		return current.jerry ? JERRY_STAT_BONUS : 0;
	}

	/** Blessings get Paul's boost, multiplier and flat both. Ask this, never compare {@code current}. */
	public static boolean blessingsBoosted() {
		return current.paul;
	}
}
