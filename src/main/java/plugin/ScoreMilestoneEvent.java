package plugin;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired the moment the team reaches a clear-score milestone (currently only 300). Unlike
 * {@link RunCompleteEvent} this lands MID-RUN, which is the point: the milestone's time is a real achievement
 * whether or not the run is then finished, so a listener can record it immediately instead of waiting for an end
 * that may never come (the team can reset right after hitting 300).
 * <br>
 * Same standalone contract as every other outbound signal here — M7 TAS depends on nothing external and this
 * fires into the void when nothing listens. {@link #json()} is the reflection-friendly door.
 * <br>
 * The payload's {@code runId} matches the one on this run's later {@link RunCompleteEvent}, so a consumer can
 * tell that the two reports describe the same run and not double-count the milestone.
 */
public class ScoreMilestoneEvent extends Event {
	private static final HandlerList HANDLERS = new HandlerList();

	private final int score;
	private final RunResult result;

	public ScoreMilestoneEvent(int score, RunResult result) {
		this.score = score;
		this.result = result;
	}

	/** The milestone that was reached (300). */
	public int score() {
		return score;
	}

	/** The run as it stands at this instant; {@code score300Tick} is the milestone's time. */
	public RunResult result() {
		return result;
	}

	/** The result as compact JSON — see {@link RunCompleteEvent#json()}. */
	public String json() {
		return result.toJson();
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}

	public static @NotNull HandlerList getHandlerList() {
		return HANDLERS;
	}
}
