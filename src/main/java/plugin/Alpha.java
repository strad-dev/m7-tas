package plugin;

import java.util.Locale;

/**
 * <b>Alpha timings</b>: the third dungeon setting, alongside {@code damage/Difficulty} and {@code damage/Mayor}
 * (MAP.md §0).  <b>Temporary</b>, and meant to be deleted once the timings it holds either land for everyone or
 * are thrown away.
 * <p>
 * Alpha is a flag on <b>TIMINGS ONLY</b>: every dialogue delay, animation length and spawn schedule it moves is
 * still authored in exactly one place, and that place asks {@link #ticks(int, int)} for the number to use.  It
 * changes no damage, no HP, no score and no room layout, so it stacks with any difficulty and any mayor.  Keep it
 * that way: a second behaviour branched on {@link #enabled()} is how this stops being deletable.
 * <p>
 * <b>An alpha run is NEVER eligible for a leaderboard.</b>  The whole point is that the fight is shorter, so its
 * times are not comparable with anything, and unlike the difficulty this is not a board axis that could hold them
 * honestly - the timings move whenever the experiment moves.  {@code plugin/RunResult.alpha} carries the flag and
 * the network's {@code Leaderboards.submit} drops the run outright.
 * <p>
 * What moves, per boss, is listed in MAP.md § Alpha timings.
 */
public enum Alpha {
	/** The authored Hypixel timings.  The default, and what every recorded time is set under. */
	OFF,
	/** The experimental short timings.  Times set under it are thrown away by the leaderboards. */
	ON;

	private static Alpha current = OFF;

	public static Alpha current() {
		return current;
	}

	public static void set(Alpha a) {
		if(a != null) current = a;
	}

	/**
	 * True while the alpha timings are in force.  <b>Ask this, never {@code current == ON}</b>, and only ever to
	 * pick a NUMBER: see {@link #ticks(int, int)}.
	 */
	public static boolean enabled() {
		return current == ON;
	}

	/**
	 * The tick count in force: {@code alpha} while alpha timings are on, {@code normal} otherwise.
	 * <p>
	 * <b>This is the only shape an alpha difference may take.</b>  Writing it as one call at the site that already
	 * owns the number keeps both timings visible side by side, and keeps deleting alpha down to deleting the
	 * second argument.
	 */
	public static int ticks(int normal, int alpha) {
		return enabled() ? alpha : normal;
	}

	/** {@link #ticks(int, int)} for a speed or an acceleration rather than a tick count. */
	public static double value(double normal, double alpha) {
		return enabled() ? alpha : normal;
	}

	/**
	 * {@link #ticks(int, int)} for a COUNT rather than a tick count: how many rows, cells or repetitions a puzzle
	 * asks for.
	 * <p>
	 * <b>These are the exception to "alpha is a flag on timings only"</b>, and they are deliberate: the Goldor
	 * puzzles are work, not schedule, so the only way to shorten them is to ask for less.  Three sites today -
	 * Melody's row count, Click In Order's width and Simon Says' target sequence length - and they still obey the
	 * shape rule, one call at the site that owns the number, so deleting the experiment is still deleting the
	 * second argument.  Kept separate from {@link #ticks} so a reader can tell at a glance which kind of
	 * difference they are looking at.
	 */
	public static int count(int normal, int alpha) {
		return enabled() ? alpha : normal;
	}

	/** Step to the next value, wrapping, and return it. */
	public static Alpha toggle() {
		Alpha[] all = values();
		set(all[(current.ordinal() + 1) % all.length]);
		return current;
	}

	/** Step BACK one value, wrapping.  A right-click on the menu button that left-clicks forwards. */
	public static Alpha toggleBack() {
		Alpha[] all = values();
		set(all[(current.ordinal() + all.length - 1) % all.length]);
		return current;
	}

	/**
	 * Parse a value (any case), or null.  Matches {@link #id()} first, so the network's ids go straight through,
	 * then the spellings a player types by hand.
	 */
	public static Alpha parse(String s) {
		if(s == null) return null;
		return switch(s.toLowerCase(Locale.ROOT)) {
			case "on", "alpha", "true", "enabled", "yes" -> ON;
			case "off", "normal", "false", "disabled", "no" -> OFF;
			default -> null;
		};
	}

	/** Lower-case id, for the run payload and the network's stored setting.  Must keep matching the network's. */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}
}
