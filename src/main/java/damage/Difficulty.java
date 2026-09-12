package damage;

import instructions.clear.ClearManager;
import plugin.Utils;

/**
 * Classic vs realistic vs ultra-realistic mode (MAP.md §0).
 * <p>
 * <b>This is a flag on inputs, not a second damage path.</b> Every one of the five things that differ is already
 * modelled as a target state or a stat input - never as a constant folded into a formula - so classic mode is
 * "the lookups answer yes unconditionally" and realistic mode is "the lookups read live state".  Keep it that way:
 * if any of the five gets baked into a hit calculation as a literal, realistic mode becomes a rewrite.
 *
 * <table>
 *   <caption>What the modes change</caption>
 *   <tr><th>Input</th><th>Classic</th><th>Realistic and ultra-realistic</th></tr>
 *   <tr><td>Last Breath (up to x0.5 defense)</td><td>all 5 stacks</td><td>built by landing arrows</td></tr>
 *   <tr><td>Lethality (x0.91^4 defense)</td><td>all 4 stacks</td><td>built by hitting</td></tr>
 *   <tr><td>Ice Spray debuff (x1.1)</td><td>always</td><td>cast, 8 blocks, 5s</td></tr>
 *   <tr><td>Twilight Arrow Poison (x1.1)</td><td>always</td><td>bow hit, 20s</td></tr>
 *   <tr><td>Blessing LEVELS</td><td>maxed</td><td>the chests the party actually opened</td></tr>
 * </table>
 *
 * Classic exists so a practising player can concentrate on movement and routing without also maintaining four
 * debuffs and a blessing count.  It is the mode the TAS was reasoned about in, and it is the default.
 * <p>
 * <b>Ultra-realistic takes realistic's damage inputs unchanged and adds death</b>, the one thing the rest of this
 * plugin models as impossible: see {@code death/Deaths}.  It deliberately shares every lookup below, so nothing in
 * {@code damage/} needs to know a third mode exists - {@link #deathsEnabled()} is the whole difference, and it is
 * only read outside this package.  Ask "are the inputs live?" with {@link #liveInputs()}, never with
 * {@code == REALISTIC}: that comparison silently drops ultra-realistic back to the classic tables.
 * <p>
 * <b>Times from the three modes are not comparable</b>, so anything that records a run has to carry the mode with
 * it - see {@code plugin/RunResult} and the network plugin's leaderboard key.
 * <p>
 * The difficulty is the FIRST of the dungeon settings {@code /dungeonsettings} owns; the other is {@link Mayor},
 * which is independent of it and stacks with any of the three modes.
 */
public enum Difficulty {
	CLASSIC, REALISTIC, ULTRA_REALISTIC;

	// Maxed blessing levels, i.e. the classic table (§0).  These are TOTAL levels, the sum over every blessing of
	// that type the party collected, which is what the formulas below read.
	private static final int MAX_POWER = 29;
	private static final int MAX_TIME = 5;
	private static final int MAX_WISDOM = 14;
	private static final int MAX_STONE = 9;

	private static Difficulty current = CLASSIC;

	public static Difficulty current() {
		return current;
	}

	public static void set(Difficulty d) {
		current = d;
	}

	/** Step to the next mode in declaration order, wrapping, and return it. */
	public static Difficulty toggle() {
		Difficulty[] all = values();
		current = all[(current.ordinal() + 1) % all.length];
		return current;
	}

	/**
	 * Parse a mode name (any case), or null.  Matches the enum name, so the network's ids go straight through
	 * ({@code ultra_realistic} = {@code ULTRA_REALISTIC}), plus {@link #ALIASES} for what a person actually types.
	 */
	public static Difficulty parse(String s) {
		if(s == null) return null;
		for(Difficulty d : values()) if(d.name().equalsIgnoreCase(s)) return d;
		return ALIASES.get(s.toLowerCase(java.util.Locale.ROOT));
	}

	/** Spellings a player types by hand.  The canonical form is always {@link #id()}; these only feed {@link #parse}. */
	private static final java.util.Map<String, Difficulty> ALIASES = java.util.Map.of(
			"ultrarealistic", ULTRA_REALISTIC,
			"ultra-realistic", ULTRA_REALISTIC,
			"ultra", ULTRA_REALISTIC);

	/** Lower-case id, for the run payload and the leaderboard key.  Must keep matching the network's {@code m7.lb.Difficulty} ids. */
	public String id() {
		return name().toLowerCase(java.util.Locale.ROOT);
	}

	/** True while the debuff and defense-reduction lookups should answer "applied" without checking anything. */
	public static boolean debuffsAssumed() {
		return current == CLASSIC;
	}

	/**
	 * True while the debuff and blessing lookups read live state rather than the maxed tables, i.e. in EITHER
	 * realistic mode.  <b>Every such test goes through here</b>, never through {@code == REALISTIC}, or
	 * ultra-realistic silently gets classic's inputs.
	 */
	public static boolean liveInputs() {
		return current != CLASSIC;
	}

	/**
	 * True while a player can actually die (ultra-realistic only) - the gate on every instakill in
	 * {@code death/Deaths}.
	 * <p>
	 * Read live at each kill site rather than latched at run start, exactly like the damage lookups: the mode is a
	 * server-wide global that {@code /m7practice} sets before the run arms, so there is one place it can change.
	 */
	public static boolean deathsEnabled() {
		return current == ULTRA_REALISTIC;
	}

	// What a blessing is WORTH - the per-stat percent and flat tables, and the effect increase the mayor is one
	// term of - is damage/Blessings.  This class owns only what LEVEL is in force, which is the part the difficulty
	// decides; Blessings reads blessingLevel below and nothing here needs to know the figures.

	/**
	 * The total level of one blessing type that the formulas are actually using.  Classic answers from the maxed
	 * table; realistic reads the run's actual chest history, and falls back to the maxed table when this session
	 * has no clear phase to read.
	 * <p>
	 * Public because it is half of what {@code plugin/BlessingState} publishes to other plugins: what the party
	 * collected and what the damage pipeline used are different numbers whenever {@link #blessingsAssumedMax()}
	 * is true, and a display showing only one of them misreports every classic run.
	 */
	public static int blessingLevel(Utils.BlessingType type) {
		if(liveInputs() && clearPhaseInThisSession()) return ClearManager.collectedLevel(type);
		return switch(type) {
			case POWER -> MAX_POWER;
			case TIME -> MAX_TIME;
			case WISDOM -> MAX_WISDOM;
			case STONE -> MAX_STONE;
			default -> 0;
		};
	}

	/**
	 * True while {@link #blessingLevel} is answering from the maxed table rather than from what the party actually
	 * collected - always in classic mode, and in realistic mode too when the session has no clear phase behind it.
	 * <p>
	 * Published on {@code plugin/BlessingState} so a display can say which of the two it is showing.
	 */
	public static boolean blessingsAssumedMax() {
		return !(liveInputs() && clearPhaseInThisSession());
	}

	/**
	 * Whether this session has a clear phase to read blessings from.  The clear being live covers a run in
	 * progress; a non-empty tally covers the stretch after the clear has handed off to the boss chain.
	 */
	private static boolean clearPhaseInThisSession() {
		return ClearManager.hasBlessingData();
	}
}
