package plugin;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Shared rules for this plugin's custom menus.
 *
 * <p><b>The workspace rule: a custom menu IGNORES double-clicks.</b>  This is not a per-menu decision and it is
 * not written down at each window any more - every menu click handler calls {@link #ignoreDoubleClick} first and
 * that is the whole of it.
 *
 * <p>The reason, once: {@code ClickType.DOUBLE_CLICK} is not a second click the player made.  With an empty
 * cursor the first press arrives as an ordinary {@code LEFT} and picks the stack up; the client then sends a
 * SECOND event for the same physical double-click, the vanilla collect-to-cursor.  A menu that acts on both does
 * one action twice - a Same Color pane stepping two colours off one gesture is how this was found - and a menu
 * whose slots are click targets rather than storage has nothing for a collect to mean anyway.
 *
 * <p><b>{@code isLeftClick()} cannot be the test</b>: Bukkit counts {@code DOUBLE_CLICK} as a left click.
 */
public final class Menus {
	private Menus() {}

	/**
	 * Eat a double-click.  Call it FIRST in every menu click handler, right after the holder check that says the
	 * window is ours - before it, and this would cancel collects in a real chest, which players use.
	 *
	 * @return true if the caller should return immediately; the event is already cancelled.
	 */
	public static boolean ignoreDoubleClick(InventoryClickEvent e) {
		if (e.getClick() != ClickType.DOUBLE_CLICK) return false;
		e.setCancelled(true);
		return true;
	}
}
