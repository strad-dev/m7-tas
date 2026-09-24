package plugin;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when the team reaches a clear-score milestone (only 300). Lands MID-RUN on purpose: the time counts even if
 * the team resets right after, so a listener records it now.
 * <br>
 * Same standalone contract as {@link RunCompleteEvent}; {@link #json()} is the reflection door. {@code runId}
 * matches the later {@link RunCompleteEvent}'s, so the milestone isn't double-counted.
 */
public class ScoreMilestoneEvent extends Event {
	private static final HandlerList HANDLERS = new HandlerList();

	private final int score;
	private final RunResult result;

	public ScoreMilestoneEvent(int score, RunResult result) {
		this.score = score;
		this.result = result;
	}

	/** Milestone reached (300). */
	public int score() {
		return score;
	}

	/** The run so far; {@code score300Tick} is the milestone's time. */
	public RunResult result() {
		return result;
	}

	/** Compact JSON; see {@link RunCompleteEvent#json()}. */
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
