package plugin;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Workspace rule: a custom menu IGNORES double-clicks. Every menu click handler calls {@link #ignoreDoubleClick}
 * first; no per-menu note.
 * <p>
 * {@code ClickType.DOUBLE_CLICK} isn't a second click: with an empty cursor the first press arrives as {@code LEFT},
 * then the client sends a SECOND event for the same gesture (collect-to-cursor). A menu acting on both does one
 * action twice (found via a Same Color pane stepping two colours).
 * <p>
 * {@code isLeftClick()} can't be the test: Bukkit counts {@code DOUBLE_CLICK} as a left click.
 */
public final class Menus {
	private Menus() {}

	/**
	 * Call FIRST in every menu click handler, right AFTER the holder check, or it cancels collects in a real chest.
	 *
	 * @return true if the caller should return; the event is already cancelled.
	 */
	public static boolean ignoreDoubleClick(InventoryClickEvent e) {
		if (e.getClick() != ClickType.DOUBLE_CLICK) return false;
		e.setCancelled(true);
		return true;
	}
}
