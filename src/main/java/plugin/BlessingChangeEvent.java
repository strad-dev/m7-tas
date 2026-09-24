package plugin;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired whenever the blessing tally MOVES: an award, or the clear at section setup. A listener never has to poll.
 * <br>
 * Same standalone contract as {@link RunCompleteEvent}: fires into the void, {@link #json()} is the reflection
 * door. A consumer that (re)loads mid-run reads {@link BlessingState#currentJson()}.
 * <br>
 * The CLEAR is reported too, or a display keeps the previous party's blessings for the whole next run.
 */
public class BlessingChangeEvent extends Event {
	private static final HandlerList HANDLERS = new HandlerList();

	private final BlessingState state;

	public BlessingChangeEvent(BlessingState state) {
		this.state = state;
	}

	public BlessingState state() {
		return state;
	}

	/** Compact JSON; see {@link RunCompleteEvent#json()}. */
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
