package damage;

import instructions.clear.ClearManager;
import plugin.Utils;

/**
 * The run mode: classic vs Perfect RNG vs realistic (MAP.md §0).
 * <p>
 * <b>This is a flag on inputs, not a second damage path.</b> Every one of the five damage things that differ is
 * already modelled as a target state or a stat input - never as a constant folded into a formula - so classic mode
 * is "the lookups answer yes unconditionally" and the other two are "the lookups read live state".  Keep it that
 * way: if any of the five gets baked into a hit calculation as a literal, the live modes become a rewrite.
 *
 * <table>
 *   <caption>What the modes change</caption>
 *   <tr><th>Input</th><th>Classic</th><th>Perfect RNG and realistic</th></tr>
 *   <tr><td>Last Breath (up to x0.5 defense)</td><td>all 5 stacks</td><td>built by landing arrows</td></tr>
 *   <tr><td>Lethality (x0.91^4 defense)</td><td>all 4 stacks</td><td>built by hitting</td></tr>
 *   <tr><td>Ice Spray debuff (x1.1)</td><td>always</td><td>cast, 8 blocks, 5s</td></tr>
 *   <tr><td>Twilight Arrow Poison (x1.1)</td><td>always</td><td>bow hit, 20s</td></tr>
 *   <tr><td>Blessing LEVELS</td><td>maxed</td><td>the chests the party actually opened</td></tr>
 *   <tr><td>Death</td><td>impossible</td><td>the storm, Goldor and relic instakills all fire</td></tr>
 * </table>
 *
 * Classic exists so a practising player can concentrate on movement and routing without also maintaining four
 * debuffs and a blessing count.  It is the mode the TAS was reasoned about in, and it is the default.
 * <p>
 * <b>Perfect RNG and realistic differ only in what a player has to do BY HAND</b>, never in damage.  Perfect RNG
 * is the run where the dungeon always rolls in your favour: a terminal opens on a board that has already fallen
 * out the easy way, Sharp Shooter walks its targets in the one order a player can learn, the dragons take their
 * set colours, and the pet you need is always the pet you have.  Realistic takes that away - {@link #realPuzzles()}
 * turns on the generated terminal puzzles, the real Simon Says / Arrow Align, Sharp Shooter's rolled order and
 * the rolled dragon order, and {@link #manualPets()} makes the player carry their own pet menu.  Neither touches
 * a formula.
 * <p>
 * Ask "are the inputs live?" with {@link #liveInputs()}, never with {@code == PERFECT_RNG}: that comparison
 * silently drops realistic back to the classic tables.
 * <p>
 * <b>Times from the three modes are not comparable</b>, so anything that records a run has to carry the mode with
 * it - see {@code plugin/RunResult} and the network plugin's leaderboard key.
 * <p>
 * The mode is the FIRST of the dungeon settings {@code /dungeonsettings} owns; the other is {@link Mayor}, which
 * is independent of it and stacks with any of the three.
 */
public enum Difficulty {
	/** The TAS calculation style: all four debuffs applied, blessings maxed, nobody can die.  The default. */
	CLASSIC("classic", "Classic"),
	/**
	 * Live damage inputs and death, but every roll the dungeon makes goes your way: stand-in terminal boards, the
	 * Sharp Shooter and dragon orders fixed, and the assumed pet table.  <b>This is what used to be called
	 * "Realistic"</b>, and the old "Ultra Realistic" folded into it - see the network plugin's {@code Leaderboards}
	 * migration, and it is where the stand-in terminal boards come from.
	 */
	PERFECT_RNG("perfect_rng", "Perfect RNG"),
	/**
	 * Perfect RNG plus everything a real run makes you do by hand: generated terminal puzzles, working devices and
	 * a pet menu you manage yourself.
	 * <p>
	 * <b>The id is {@code rta}, not {@code realistic}.</b>  {@code realistic} is already on disk as the id of
	 * every run recorded under what is now Perfect RNG, and a board key is permanent; reusing the string would
	 * make an old file and a new one mean different things with no way to tell them apart.  The mode was called
	 * "RTA Mode" while it was being designed, which is where the id comes from.
	 */
	REALISTIC("rta", "Realistic");

	// Maxed blessing levels, i.e. the classic table (§0).  These are TOTAL levels, the sum over every blessing of
	// that type the party collected, which is what the formulas below read.
	private static final int MAX_POWER = 29;
	private static final int MAX_TIME = 5;
	private static final int MAX_WISDOM = 14;
	private static final int MAX_STONE = 9;

	private static Difficulty current = CLASSIC;

	private final String id;
	private final String displayName;

	Difficulty(String id, String displayName) {
		this.id = id;
		this.displayName = displayName;
	}

	public static Difficulty current() {
		return current;
	}

	public static void set(Difficulty d) {
		if(d != null) current = d;
	}

	/** Step to the next mode in declaration order, wrapping, and return it. */
	public static Difficulty toggle() {
		Difficulty[] all = values();
		current = all[(current.ordinal() + 1) % all.length];
		return current;
	}

	/** Step BACK one mode, wrapping, and return it.  A right-click on the menu button that left-clicks forwards. */
	public static Difficulty toggleBack() {
		Difficulty[] all = values();
		current = all[(current.ordinal() + all.length - 1) % all.length];
		return current;
	}

	/**
	 * Parse a mode name (any case), or null.  Matches {@link #id()} first, so the network's ids go straight
	 * through, then the enum name, then {@link #ALIASES} for what a person actually types.
	 */
	public static Difficulty parse(String s) {
		if(s == null) return null;
		for(Difficulty d : values()) if(d.id.equalsIgnoreCase(s)) return d;
		for(Difficulty d : values()) if(d.name().equalsIgnoreCase(s)) return d;
		return ALIASES.get(s.toLowerCase(java.util.Locale.ROOT));
	}

	/**
	 * Spellings a player types by hand.  The canonical form is always {@link #id()}; these only feed
	 * {@link #parse}, and <b>none of them may ever be persisted</b>.
	 * <p>
	 * <b>{@code realistic} means the mode CALLED Realistic, which is {@link #REALISTIC}</b> - not the id
	 * {@code realistic} sitting in old leaderboard keys, which meant what is now {@link #PERFECT_RNG}.  Those old
	 * keys are rewritten once, by raw string, in the network plugin's {@code Leaderboards} migration; they never
	 * come through here.  The old {@code ultra*} spellings point at Perfect RNG, since that is where
	 * ultra-realistic folded.
	 */
	private static final java.util.Map<String, Difficulty> ALIASES = java.util.Map.ofEntries(
			java.util.Map.entry("realistic", REALISTIC),
			java.util.Map.entry("rta_mode", REALISTIC),
			java.util.Map.entry("rtamode", REALISTIC),
			java.util.Map.entry("real", REALISTIC),
			java.util.Map.entry("perfectrng", PERFECT_RNG),
			java.util.Map.entry("perfect-rng", PERFECT_RNG),
			java.util.Map.entry("prng", PERFECT_RNG),
			java.util.Map.entry("rng", PERFECT_RNG),
			java.util.Map.entry("ultra_realistic", PERFECT_RNG),
			java.util.Map.entry("ultrarealistic", PERFECT_RNG),
			java.util.Map.entry("ultra-realistic", PERFECT_RNG),
			java.util.Map.entry("ultra", PERFECT_RNG));

	/** Lower-case id, for the run payload and the leaderboard key.  Must keep matching the network's {@code m7.lb.Difficulty} ids. */
	public String id() {
		return id;
	}

	/** What a player is shown, e.g. "Perfect RNG".  Must keep matching the network's {@code displayName}. */
	public String displayName() {
		return displayName;
	}

	/**
	 * The spelling a player types and tab-completes, which is <b>not always {@link #id()}</b>: Realistic is typed
	 * {@code realistic}, while the id it is stored under is {@code rta}.
	 * <p>
	 * <b>Every usage string, error message and tab completion uses this; every persisted value uses {@code id()}.</b>
	 * The two only differ for Realistic, and they differ because the id is a permanent storage key while the typed
	 * name is what the mode is actually called - a player told to type {@code rta} for a mode the FAQ and every
	 * settings line call "Realistic" is being asked to know an implementation detail.  Both spellings parse
	 * ({@code realistic} is in {@link #ALIASES}, {@code rta} is the id), so this only decides which one we teach.
	 */
	public String commandName() {
		return this == REALISTIC ? "realistic" : id;
	}

	/** True while the debuff and defense-reduction lookups should answer "applied" without checking anything. */
	public static boolean debuffsAssumed() {
		return current == CLASSIC;
	}

	/**
	 * True while the debuff and blessing lookups read live state rather than the maxed tables, i.e. in EITHER of
	 * the two live modes.  <b>Every such test goes through here</b>, never through {@code == PERFECT_RNG}, or
	 * realistic silently gets classic's inputs.
	 */
	public static boolean liveInputs() {
		return current != CLASSIC;
	}

	/**
	 * True while a player can actually die - the gate on every instakill in {@code death/Deaths}, and on Goldor's
	 * death ticks.  Both live modes; only classic models death as impossible.
	 * <p>
	 * Read live at each kill site rather than latched at run start, exactly like the damage lookups: the mode is a
	 * server-wide global that {@code /m7practice} sets before the run arms, so there is one place it can change.
	 */
	public static boolean deathsEnabled() {
		return current != CLASSIC;
	}

	/**
	 * True while the dungeon's content is the real, generated version rather than the short stand-in: the generated
	 * terminal boards of {@code goldor/GoldorTerminalGui}, the working Simon Says and Arrow Align, and every order
	 * the dungeon rolls - Sharp Shooter's nine targets and the Wither King's five dragon colours.
	 * <p>
	 * <b>Realistic only.</b>  Perfect RNG is the mode where the dungeon rolls in your favour, so its boards open
	 * nearly solved and its orders are the fixed ones a player can learn.  This changes no damage and no timing
	 * anywhere - it only decides how much work a section costs - so nothing in {@code damage/} may ever read it.
	 */
	public static boolean realPuzzles() {
		return current == REALISTIC;
	}

	/**
	 * True while the player owns their pet instead of being assumed to have the right one out ({@code pets/}).
	 * <p>
	 * <b>Realistic only</b>, and the one place {@code damage/Pet.forPlayer} stops answering from its table: in
	 * every other mode the pet still follows from what the player is doing and wearing.
	 */
	public static boolean manualPets() {
		return current == REALISTIC;
	}

	// What a blessing is WORTH - the per-stat percent and flat tables, and the effect increase the mayor is one
	// term of - is damage/Blessings.  This class owns only what LEVEL is in force, which is the part the mode
	// decides; Blessings reads blessingLevel below and nothing here needs to know the figures.

	/**
	 * The total level of one blessing type that the formulas are actually using.  Classic answers from the maxed
	 * table; the live modes read the run's actual chest history, and fall back to the maxed table when this
	 * session has no clear phase to read.
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
	 * collected - always in classic mode, and in a live mode too when the session has no clear phase behind it.
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
