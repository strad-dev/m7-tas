package damage;

import java.util.Locale;

/**
 * Which SkyBlock mayor is in office: the second dungeon setting, alongside {@link Difficulty} (MAP.md §0).
 * <p>
 * The mayor is a global the whole SkyBlock lobby shares, and the one M7 was authored against is <b>Paul</b>.  Paul
 * gives dungeons two things this plugin models - the EZPZ perk's flat <b>+10 bonus score</b>, and a boost to every
 * blessing's per-level bonus - and mobs are on their ordinary Master Mode health.  Only three cases matter here, so
 * every real mayor collapses into one of them:
 *
 * <table>
 *   <caption>What each mayor changes</caption>
 *   <tr><th>Mayor</th><th>Bonus score</th><th>Blessing effect increase</th><th>Mob health</th></tr>
 *   <tr><td>{@link #PAUL} (default)</td><td>+10 (EZPZ)</td><td>x1.815</td><td>x1</td></tr>
 *   <tr><td>{@link #DERPY}</td><td>none</td><td>x1.452</td><td><b>x2</b></td></tr>
 *   <tr><td>{@link #OTHER}</td><td>none</td><td>x1.452</td><td>x1</td></tr>
 * </table>
 *
 * Paul's blessing perk ({@link #PAUL_BLESSING_BOOST}) is one of the four terms of
 * {@link Blessings#effectIncrease()}, so it scales <b>every</b> blessing figure - percent and flat, on every stat.
 * It is the only one of the four this plugin does not assume maxed.
 *
 * {@link #OTHER} is every mayor who does nothing to dungeons at all - the honest baseline, i.e. Paul's perks gone
 * and nothing put in their place. {@link #DERPY} is that plus the double health.
 * <p>
 * <b>Like {@link Difficulty}, this is a flag on inputs, never a second path.</b>  All three effects are read live
 * from the three places that already own those numbers - {@code ClearManager.bonus}, {@link Difficulty}'s blessing
 * multiplier and {@link MobStats.MobStat#internalHealth} - so nothing else in the plugin has to know the mayor can
 * change.  Keep it that way: a fourth effect belongs in whichever class already owns that number.
 * <p>
 * <b>The setting is a server-wide global, read live, and only the HP is latched.</b>  A mob's health is written once
 * when it spawns, so changing mayor mid-run leaves everything already on the floor on its old HP - which is why
 * {@code /m7practice} sets this before the run arms, exactly as it does the difficulty, and why the network always
 * sends a value rather than inheriting the last party's.  The score and blessing terms have no such problem; they
 * are recomputed on every read.
 * <p>
 * <b>Times under different mayors are not comparable</b> - Derpy is strictly harder and its perfect clear tops out
 * at 309 rather than 319 - so {@code plugin/RunResult} carries the mayor for the same reason it carries the
 * difficulty.  The network's leaderboards do not currently split on it.
 */
public enum Mayor {
	/** The mayor M7 was authored under, and the default: EZPZ's +10 bonus score and boosted blessings. */
	PAUL,
	/** No Paul perks, and <b>every mob carries twice its HP</b>. */
	DERPY,
	/** Any mayor with no dungeon perks at all: no +10, unboosted blessings, ordinary mob health. */
	OTHER;

	/** Mayor Paul's EZPZ perk: a flat +10 on the bonus score, and the whole reason a perfect run scores 319. */
	public static final int PAUL_SCORE_BONUS = 10;

	/** Mobs carry twice their HP under Derpy - the one effect here that is not simply a lost Paul perk. */
	public static final double DERPY_HEALTH_MULTIPLIER = 2.0;

	/**
	 * Mayor Paul's <b>Benediction</b> perk - "blessings are 25% stronger" - and the only term of
	 * {@link Blessings#effectIncrease()} that is not assumed maxed.
	 * <p>
	 * With it the effect increase is {@code 1.25 x 1.10 x 1.20 x 1.10 = 1.815}; without, {@code 1.452}.  It scales
	 * a blessing's percent and flat halves alike, so it moves every stat every blessing grants.
	 */
	public static final double PAUL_BLESSING_BOOST = 1.25;

	private static Mayor current = PAUL;

	public static Mayor current() {
		return current;
	}

	/**
	 * Put a mayor in office.  Drops the stat cache, since the blessing multiplier is one of its inputs and a
	 * cached aggregate would otherwise keep the old mayor's blessings for the rest of the cache window.
	 */
	public static void set(Mayor m) {
		if(m == null) return;
		current = m;
		Stats.invalidateAll();
	}

	/** Step to the next mayor in declaration order, wrapping, and return it. */
	public static Mayor toggle() {
		Mayor[] all = values();
		set(all[(current.ordinal() + 1) % all.length]);
		return current;
	}

	/**
	 * Parse a mayor name (any case), or null.  Matches the enum name, so the network's ids go straight through.
	 * Both {@code /dungeonsettings mayor} and {@code /m7practice}'s content-matched arg list go through here, so
	 * the two can never disagree about what a mayor is called.
	 */
	public static Mayor parse(String s) {
		if(s == null) return null;
		for(Mayor m : values()) if(m.name().equalsIgnoreCase(s)) return m;
		// "none" reads as OTHER: a player asking for "no mayor" means "no dungeon perks", which is what OTHER is.
		return s.equalsIgnoreCase("none") ? OTHER : null;
	}

	/** Lower-case id, for the run payload and the network's stored setting.  Must keep matching the network's ids. */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** The factor every mob's HP is scaled by right now: 2 under Derpy, 1 otherwise. */
	public static double healthMultiplier() {
		return current == DERPY ? DERPY_HEALTH_MULTIPLIER : 1.0;
	}

	/** The flat bonus-score term the mayor contributes right now: {@link #PAUL_SCORE_BONUS} under Paul, else 0. */
	public static int scoreBonus() {
		return current == PAUL ? PAUL_SCORE_BONUS : 0;
	}

	/**
	 * True while blessings get Paul's boosted per-level figures - <b>both</b> the multiplier and the flat half.
	 * Ask this, never {@code current == PAUL}.
	 */
	public static boolean blessingsBoosted() {
		return current == PAUL;
	}
}
