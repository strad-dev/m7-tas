package plugin;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired the moment a /practice run actually finishes, i.e. the boss is defeated (for Wither-King runs, only
 * AFTER the death dialogue ends). M7 TAS fires this unconditionally in practice mode and depends on
 * nothing external: it fires into the void when nothing listens, so the plugin stays fully standalone.
 * An optional glue plugin may listen to it (e.g. to return players to spectator and free a network slot).
 * <br>
 * Carries a {@link RunResult} describing the run (ticks, splits, score, participants). Note the event fires for
 * a FAILED run too, because the listener still needs to free its slot, so check {@link RunResult#success} before
 * treating it as a completion. A listener that doesn't compile against M7 TAS can read {@link #json()}.
 */
public class RunCompleteEvent extends Event {
	private static final HandlerList HANDLERS = new HandlerList();

	private final RunResult result;

	public RunCompleteEvent(RunResult result) {
		this.result = result;
	}

	/** Everything measurable about the run that just finished. */
	public RunResult result() {
		return result;
	}

	/**
	 * The result as compact JSON, the reflection-friendly door.  The network plugin listens for this event
	 * reflectively (it doesn't depend on M7 TAS), so it calls this one no-arg String method and parses it,
	 * rather than walking {@link RunResult}'s fields.
	 */
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
