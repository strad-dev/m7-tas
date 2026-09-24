package plugin;

import java.util.Locale;

/**
 * <b>Alpha timings</b>: third dungeon setting next to {@code damage/Difficulty} and {@code damage/Mayor} (MAP.md §0).
 * TEMPORARY: delete once its timings land for everyone or are thrown away.
 * <p>
 * A flag on TIMINGS ONLY: each number it moves is authored in one place, which asks {@link #ticks(int, int)}. No
 * damage, HP, score or layout changes, so it stacks with any difficulty and mayor. A behaviour branched on
 * {@link #enabled()} is how this stops being deletable.
 * <p>
 * An alpha run NEVER reaches a leaderboard: the fight is shorter and the timings move with the experiment, so it
 * can't be a board axis. {@code plugin/RunResult.alpha} carries the flag; the network's {@code Leaderboards.submit}
 * drops the run. What moves, per boss: MAP.md § Alpha timings.
 */
public enum Alpha {
	/** Authored Hypixel timings. Default; every recorded time is set under it. */
	OFF,
	/** Experimental short timings. Leaderboards discard its times. */
	ON;

	private static Alpha current = OFF;

	public static Alpha current() {
		return current;
	}

	public static void set(Alpha a) {
		if(a != null) current = a;
	}

	/** Ask this, never {@code current == ON}, and only to pick a NUMBER ({@link #ticks(int, int)}). */
	public static boolean enabled() {
		return current == ON;
	}

	/**
	 * {@code alpha} while alpha is on, else {@code normal}. The ONLY shape an alpha difference may take: one call at
	 * the site owning the number, so both are visible and deleting alpha is deleting the second argument.
	 */
	public static int ticks(int normal, int alpha) {
		return enabled() ? alpha : normal;
	}

	/** {@link #ticks(int, int)} for a speed or acceleration. */
	public static double value(double normal, double alpha) {
		return enabled() ? alpha : normal;
	}

	/**
	 * {@link #ticks(int, int)} for a COUNT (rows, cells, repetitions). The deliberate exception to "timings only":
	 * Goldor puzzles are work, not schedule, so shortening them means asking for less. Three sites: Melody rows,
	 * Click In Order width, Simon Says length. Same shape rule; separate from {@link #ticks} so the kind is obvious.
	 */
	public static int count(int normal, int alpha) {
		return enabled() ? alpha : normal;
	}

	/** Next value, wrapping. */
	public static Alpha toggle() {
		Alpha[] all = values();
		set(all[(current.ordinal() + 1) % all.length]);
		return current;
	}

	/** Previous value, wrapping: a right-click on the menu button. */
	public static Alpha toggleBack() {
		Alpha[] all = values();
		set(all[(current.ordinal() + all.length - 1) % all.length]);
		return current;
	}

	/** Parse any case, or null. Accepts {@link #id()} (the network's ids) and hand-typed spellings. */
	public static Alpha parse(String s) {
		if(s == null) return null;
		return switch(s.toLowerCase(Locale.ROOT)) {
			case "on", "alpha", "true", "enabled", "yes" -> ON;
			case "off", "normal", "false", "disabled", "no" -> OFF;
			default -> null;
		};
	}

	/** Lower-case id for the run payload and the network's stored setting. Must match the network's. */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}
}
