package plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import damage.Difficulty;
import instructions.bosses.WitherActions;
import instructions.clear.ClearManager;

import java.util.ArrayList;
import java.util.List;

/**
 * The run's blessings as they stand RIGHT NOW: what the party has collected, and what the damage pipeline is
 * actually using.  Attached to {@link BlessingChangeEvent}, and readable at any moment through
 * {@link #currentJson()}.
 * <br>
 * Same contract as {@link RunResult}: this is M7 TAS reporting facts and nothing else.  It fires into the void
 * when nothing listens, and a consumer that doesn't want to compile against this class can read
 * {@link #toJson()} through one reflective call.
 * <br>
 * <b>Two different numbers, both real, and they routinely disagree.</b>  {@link Entry#level} is what the party
 * has actually picked up; {@link Entry#effectiveLevel} is what {@code damage/Difficulty} feeds the formulas.
 * In classic mode - the default - the second is the maxed table whatever the first says, and even in realistic
 * mode a run with no clear phase has no chest history to read and so falls back to the same table
 * ({@link #assumedMax} says which of the two you are looking at, so a display can't accidentally claim a
 * boss-only run collected nothing).
 */
public final class BlessingState {
	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

	/** The run these blessings belong to, matching every other report it makes ({@link WitherActions#runId()}). */
	public String runId;

	/** The damage difficulty in force: {@code classic} or {@code realistic}. */
	public String difficulty;

	/**
	 * True while a practice run is live at all ({@code WitherActions.isPracticeMode}).
	 * <p>
	 * This is what tells a stale snapshot from a live one, and a display wants it: the last thing a finished run
	 * publishes is its own teardown ({@code /m7practice end} flips this off, then the section setup clears the
	 * tally), so without it a HUD keeps the numbers up forever on an idle server.
	 */
	public boolean runActive;

	/** True while the clear phase is live. */
	public boolean clearActive;

	/** True when {@link Entry#level} describes THIS session, i.e. the clear is live or it collected something. */
	public boolean hasClearData;

	/** True when the damage pipeline is using the maxed table instead of {@link Entry#level} (see the class doc). */
	public boolean assumedMax;

	/** One entry per blessing type, in damage-relevance order: Power, Wisdom, Time, Stone, Life. */
	public List<Entry> blessings = new ArrayList<>();

	/** The order the entries are emitted in, so a display can render the list as it arrives. */
	private static final Utils.BlessingType[] ORDER = {
			Utils.BlessingType.POWER, Utils.BlessingType.WISDOM, Utils.BlessingType.TIME,
			Utils.BlessingType.STONE, Utils.BlessingType.LIFE
	};

	public static final class Entry {
		/** The {@code Utils.BlessingType} name: POWER, WISDOM, TIME, STONE, LIFE. */
		public String type;
		/** Total level COLLECTED this run, summed over every blessing of this type (a Power V is 5 of it). */
		public int level;
		/** How many separate blessings of this type were found. */
		public int count;
		/** The total level the damage formulas are using - {@link #level}, or the maxed table (see {@link BlessingState#assumedMax}). */
		public int effectiveLevel;
		/** The multiplicative bonus at {@link #effectiveLevel}; null for the two types that don't have one. */
		public Double multiplier;
		/** Blessing of Stone's flat base-Damage contribution; null for every other type (only Stone is flat). */
		public Double flatDamage;

		Entry(Utils.BlessingType type) {
			this.type = type.name();
			this.level = ClearManager.collectedLevel(type);
			this.count = ClearManager.collectedCount(type);
			this.effectiveLevel = Difficulty.blessingLevel(type);
			// Only the three multiplicative blessings get a multiplier, and only Stone gets a flat figure. Stone's
			// own multiplicative half (Defense) and Life entirely are unmodelled, and reporting a 1 + 3.63%/level
			// for them would be inventing a number the damage pipeline never applies.
			switch(type) {
				case POWER, WISDOM, TIME -> this.multiplier = Difficulty.blessing(type);
				case STONE -> this.flatDamage = Difficulty.stoneDamage();
				default -> {
				}
			}
		}
	}

	private BlessingState() {}

	/** Snapshot the blessings as they stand right now. */
	public static BlessingState capture() {
		BlessingState s = new BlessingState();
		s.runId = WitherActions.runId();
		s.difficulty = Difficulty.current().id();
		s.runActive = WitherActions.isPracticeMode();
		s.clearActive = ClearManager.isActive();
		s.hasClearData = ClearManager.hasBlessingData();
		s.assumedMax = Difficulty.blessingsAssumedMax();
		for(Utils.BlessingType t : ORDER) s.blessings.add(new Entry(t));
		return s;
	}

	/** The current blessings as compact JSON - the whole API for a consumer that never compiles against us. */
	public static String currentJson() {
		return capture().toJson();
	}

	public String toJson() {
		return GSON.toJson(this);
	}
}
