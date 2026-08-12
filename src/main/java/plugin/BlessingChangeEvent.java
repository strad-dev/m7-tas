package plugin;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired whenever the run's blessing tally MOVES: a blessing was awarded, or the tally was cleared as a section
 * was set up.  A listener that only ever reacts to this therefore always holds the current state, and never has
 * to poll.
 * <br>
 * Same standalone contract as {@link RunCompleteEvent} and {@link ScoreMilestoneEvent}: it fires into the void
 * when nothing listens, and {@link #json()} is the reflection-friendly door.  A consumer that starts up (or
 * reloads) mid-run and so missed the awards can read the same payload from
 * {@link BlessingState#currentJson()} instead.
 * <br>
 * The CLEAR is reported as well as the awards, which is the half that's easy to forget: a display fed only by
 * awards keeps showing the previous party's blessings for the whole of the next run.
 */
public class BlessingChangeEvent extends Event {
	private static final HandlerList HANDLERS = new HandlerList();

	private final BlessingState state;

	public BlessingChangeEvent(BlessingState state) {
		this.state = state;
	}

	/** The blessings as they stand at this instant. */
	public BlessingState state() {
		return state;
	}

	/** The state as compact JSON; see {@link RunCompleteEvent#json()}. */
	public String json() {
		return state.toJson();
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}

	public static @NotNull HandlerList getHandlerList() {
		return HANDLERS;
	}
}
