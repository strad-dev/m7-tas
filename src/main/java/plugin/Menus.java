package plugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Workspace rule: a custom menu IGNORES double-clicks. Every menu click handler calls {@link #ignoreDoubleClick}
 * first; no per-menu note.
 * <p>
 * {@code ClickType.DOUBLE_CLICK} isn't a second click: with an empty cursor the first press arrives as {@code LEFT},
 * then the client sends a SECOND event for the same gesture (collect-to-cursor). A menu acting on both does one
 * action twice (found via a Same Color pane stepping two colours).
 * <p>
 * {@code isLeftClick()} can't be the test: Bukkit counts {@code DOUBLE_CLICK} as a left click.
 * A SHIFT double-click sends no {@code DOUBLE_CLICK} at all, only extra {@code SHIFT_LEFT}s, so those are caught
 * by timing ({@link #isShiftDoubleClick}).
 */
public final class Menus {
	private Menus() {}

	/**
	 * Call FIRST in every menu click handler, right AFTER the holder check, or it cancels collects in a real chest.
	 *
	 * @return true if the caller should return; the event is already cancelled.
	 */
	public static boolean ignoreDoubleClick(InventoryClickEvent e) {
		if (e.getClick() != ClickType.DOUBLE_CLICK && !isShiftDoubleClick(e)) return false;
		e.setCancelled(true);
		return true;
	}

	private static final long DOUBLE_CLICK_MS = 300;
	private static final long REPLAY_WAIT_MS = 500;

	private static final class ShiftState {
		int slot = -1;
		long at;
		long replayUntil;
		int replayTick = -1;
	}

	private static final Map<HumanEntity, ShiftState> shiftStates = new WeakHashMap<>();

	private static boolean isShiftDoubleClick(InventoryClickEvent e) {
		if (e.getClick() != ClickType.SHIFT_LEFT) return false;
		ShiftState state = shiftStates.computeIfAbsent(e.getWhoClicked(), k -> new ShiftState());
		long now = System.currentTimeMillis();
		int tick = Bukkit.getCurrentTick();
		if (state.replayTick == tick) return true;
		if (now < state.replayUntil) {
			state.replayUntil = 0;
			state.replayTick = tick;
			return true;
		}
		if (state.slot == e.getRawSlot() && now - state.at < DOUBLE_CLICK_MS) {
			state.slot = -1;
			state.replayUntil = now + REPLAY_WAIT_MS;
			return false;
		}
		state.slot = e.getRawSlot();
		state.at = now;
		return false;
	}
}
