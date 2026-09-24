package plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import damage.Blessings;
import damage.Difficulty;
import damage.Mayor;
import damage.Stat;
import instructions.bosses.WitherActions;
import instructions.clear.ClearManager;

import java.util.ArrayList;
import java.util.List;

/**
 * The run's blessings RIGHT NOW: collected, and what the damage pipeline uses. On {@link BlessingChangeEvent} and
 * via {@link #currentJson()}. Same facts-only contract as {@link RunResult}; {@link #toJson()} is one reflective call.
 * <br>
 * Two real numbers that often disagree: {@link Entry#level} is what the party picked up, {@link Entry#effectiveLevel}
 * what {@code damage/Difficulty} feeds the formulas. Classic (default) uses the maxed table regardless, and a live
 * run with no clear phase falls back to it too. {@link #assumedMax} says which, so a boss-only run doesn't read as
 * having collected nothing.
 */
public final class BlessingState {
	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

	/** {@link WitherActions#runId()}, matching the run's other reports. */
	public String runId;

	/** {@code Difficulty.id()}: {@code classic}, {@code perfect_rng} or {@code rta}. */
	public String difficulty;

	/**
	 * {@code damage/Mayor}: {@code paul}, {@code derpy} or {@code other}. Here because Paul's Benediction is a term of
	 * {@link Blessings#effectIncrease()} (1.815, 1.452 without), so {@link Entry#multiplier} and
	 * {@link Entry#flatDamage} differ at the same {@link Entry#effectiveLevel}.
	 */
	public String mayor;

	/**
	 * True while a practice run is live ({@code WitherActions.isPracticeMode}). Tells stale from live: a finished
	 * run's last publish is its teardown, so without this a HUD keeps the numbers up on an idle server.
	 */
	public boolean runActive;

	/** True while the clear phase is live. */
	public boolean clearActive;

	/** True when {@link Entry#level} describes THIS session: the clear is live or collected something. */
	public boolean hasClearData;

	/** True when damage uses the maxed table instead of {@link Entry#level}. */
	public boolean assumedMax;

	/** One per type, damage-relevance order: Power, Wisdom, Time, Stone, Life. */
	public List<Entry> blessings = new ArrayList<>();

	/**
	 * {@link Blessings#effectIncrease()}: 1.815 with Paul, 1.452 without. Explains why one {@link Entry#effectiveLevel}
	 * reports different {@link Entry#multiplier}s on two runs.
	 */
	public double effectIncrease;

	/** Emit order, so a display can render the list as it arrives. */
	private static final Utils.BlessingType[] ORDER = {
			Utils.BlessingType.POWER, Utils.BlessingType.WISDOM, Utils.BlessingType.TIME,
			Utils.BlessingType.STONE, Utils.BlessingType.LIFE
	};

	public static final class Entry {
		/** {@code Utils.BlessingType} name. */
		public String type;
		/** Total level COLLECTED this run, over every blessing of this type (a Power V is 5). */
		public int level;
		/** Blessings of this type found. */
		public int count;
		/** Level the formulas use: {@link #level} or the maxed table ({@link BlessingState#assumedMax}). */
		public int effectiveLevel;
		/** Multiplier at {@link #effectiveLevel}; null if the type grants no percent. */
		public Double multiplier;
		/** Stone's flat base Damage; null for every other type. */
		public Double flatDamage;

		Entry(Utils.BlessingType type) {
			this.type = type.name();
			this.level = ClearManager.collectedLevel(type);
			this.count = ClearManager.collectedCount(type);
			this.effectiveLevel = Difficulty.blessingLevel(type);
			// Both from damage/Blessings, omitted when there's nothing to say, since a 0 or 1.0 reads as a real
			// figure. Every type has a multiplier now (Stone's is Defense, Life's Health); only Stone grants flat DAMAGE.
			double m = Blessings.multiplier(type);
			if(m != 1.0) this.multiplier = m;
			double d = Blessings.flat(type, Stat.DAMAGE);
			if(d != 0.0) this.flatDamage = d;
		}
	}

	private BlessingState() {}

	public static BlessingState capture() {
		BlessingState s = new BlessingState();
		s.runId = WitherActions.runId();
		s.difficulty = Difficulty.current().id();
		s.mayor = Mayor.current().id();
		s.effectIncrease = Blessings.effectIncrease();
		s.runActive = WitherActions.isPracticeMode();
		s.clearActive = ClearManager.isActive();
		s.hasClearData = ClearManager.hasBlessingData();
		s.assumedMax = Difficulty.blessingsAssumedMax();
		for(Utils.BlessingType t : ORDER) s.blessings.add(new Entry(t));
		return s;
	}

	/** Current blessings as compact JSON: the whole API for a consumer that doesn't compile against this. */
	public static String currentJson() {
		return capture().toJson();
	}

	public String toJson() {
		return GSON.toJson(this);
	}
}
