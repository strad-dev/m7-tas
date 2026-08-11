package plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import instructions.bosses.WitherActions;
import instructions.clear.ClearManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A snapshot of everything measurable about a finished /practice run, attached to {@link RunCompleteEvent}.
 * <br>
 * This is M7 TAS reporting FACTS about the run: ticks, score, who was there.  It deliberately knows nothing
 * about leaderboards, categories or group sizes: deciding which boards a run qualifies for is the listening
 * plugin's business. M7 TAS stays standalone (nothing here reaches outside the plugin), and a listener that
 * doesn't want to compile against this class can read {@link #toJson()} through one reflective call.
 * <br>
 * All ticks are server ticks (20/s). "Overall" ticks are relative to the run's t=0
 * ({@link Utils#runTick()}); phase durations are relative to their own boss's start ({@link Utils#phaseTick()}).
 * A {@code null} Integer means "not reached this run", e.g. every clear milestone is null for a boss-only
 * practice, and {@code witherKing} is null for a run that stopped at Necron.
 */
public final class RunResult {
	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

	/** The section /practice was invoked with: all, clear, boss, maxor, storm, goldor, necron, witherking. */
	public String section;

	/** Unique id for this run, identical across every report it makes (see {@link WitherActions#runId()}). */
	public String runId;

	/** False only for a run that ended in failure (enraged Storm with no pillars left). */
	public boolean success;

	/**
	 * The damage difficulty this run was set under: {@code classic} or {@code realistic} (DAMAGE_PLAN.md §0).
	 * <p>
	 * <b>Times from the two modes are not comparable</b> - a realistic run pays for maintaining four debuffs and
	 * for however many blessings the party actually collected - so anything recording this run has to key on it.
	 * On the network that means the leaderboard key gains a third component,
	 * {@code category|groupSize|difficulty}, with legacy 2-part keys migrated to {@code classic}: every run
	 * recorded before the split was set under the hand-tuned damage that classic mode reproduces.
	 */
	public String difficulty;

	/** Total run length: {@link Utils#runTick()} at the moment the run completed. */
	public int runTicks;

	/** Overall tick the clear phase ended (boss portal entered, or blood done on a clear-only run); null if no clear. */
	public Integer clearEndTick;

	/** Overall tick blood finished; null if blood was never finished. */
	public Integer bloodDoneTick;

	/** Overall tick the team first reached 300 score; null if it never did. */
	public Integer score300Tick;

	/** Overall tick the run became a full clear (max score AND blood done); null otherwise. */
	public Integer fullClearTick;

	/** Final team score, or null if this run had no clear phase to score. */
	public Integer teamScore;

	/** Letter grade for {@link #teamScore} (S+, S, A, …), or null if there was no clear phase. */
	public String grade;

	/** Phase-relative duration of each completed boss phase: Maxor, Storm, Goldor, Necron, WitherKing. */
	public Map<String, Integer> phaseDurations;

	/** Overall tick at which each section finished: Clear, Maxor, Storm, Terminals, Goldor, Necron, WitherKing. */
	public Map<String, Integer> splitEnds;

	/** Who was actually in the run at the moment it completed. */
	public List<Participant> participants = new ArrayList<>();

	public static final class Participant {
		public String uuid;
		public String name;
		/** True only if they never left Adventure mode all run (the practice scoreboard's golden-name check). */
		public boolean stayedAdventure;

		Participant(Player p) {
			this.uuid = p.getUniqueId().toString();
			this.name = p.getName();
			this.stayedAdventure = WitherActions.stayedAdventure(p);
		}
	}

	private RunResult() {}

	/**
	 * Snapshot the current run. Must be called at completion time, while the participants are still online and
	 * still in Adventure, i.e. before anything forces them to spectator.  {@code /practice end} moves everyone
	 * to spectator, which would empty {@link ClearManager#realPlayers()}.
	 */
	public static RunResult capture(String section, boolean success) {
		RunResult r = new RunResult();
		r.section = section;
		r.runId = WitherActions.runId();
		r.success = success;
		r.difficulty = damage.Difficulty.current().id();
		r.runTicks = Utils.runTick();
		r.phaseDurations = WitherActions.phaseDurations();
		r.splitEnds = WitherActions.splitEnds();
		r.clearEndTick = WitherActions.getSplitEnd("Clear");

		// Score and the clear milestones only mean anything if a clear phase actually ran. On a boss-only
		// practice ClearManager was never started, and teamScore() would report a meaningless ~120 from an
		// unexplored map, so report null rather than a number that would poison a leaderboard.
		if (ClearManager.isActive()) {
			r.teamScore = ClearManager.teamScore();
			r.grade = ClearManager.grade();
			r.bloodDoneTick = nullIfUnset(ClearManager.bloodDoneTick());
			r.score300Tick = nullIfUnset(ClearManager.score300Tick());
			r.fullClearTick = nullIfUnset(ClearManager.fullClearTick());
		}

		for (Player p : ClearManager.realPlayers()) r.participants.add(new Participant(p));
		return r;
	}

	private static Integer nullIfUnset(int tick) {
		return tick < 0 ? null : tick;
	}

	/** Compact JSON, so a listener can read the whole result without compiling against this class. */
	public String toJson() {
		return GSON.toJson(this);
	}
}
