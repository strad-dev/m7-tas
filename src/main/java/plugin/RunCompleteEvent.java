package plugin;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired the moment a /practice run actually finishes — the boss is defeated (for Wither-King runs, only
 * AFTER the death dialogue ends). M7 TAS fires this unconditionally in practice mode and depends on
 * nothing external — it fires into the void when nothing listens, so the plugin stays fully standalone.
 * An optional glue plugin may listen to it (e.g. to return players to spectator and free a network slot).
 */
public class RunCompleteEvent extends Event {
	private static final HandlerList HANDLERS = new HandlerList();

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}

	public static @NotNull HandlerList getHandlerList() {
		return HANDLERS;
	}
}
