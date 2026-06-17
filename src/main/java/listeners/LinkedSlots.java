package listeners;

import net.minecraft.server.MinecraftServer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import plugin.FakePlayerInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Two inventory conveniences for real players:
 *
 * <ul>
 *   <li><b>Linked slots</b> — in the normal player inventory (the one opened with E), shift+left-clicking an item
 *       in a backpack slot (9-35) swaps it with the hotbar slot in the same column. The 9th column (slots 17/26/35)
 *       maps to hotbar slot 7 instead of 8, since hotbar slot 8 is reserved for the SkyBlock menu.</li>
 *   <li><b>SkyBlock-menu lock</b> — the SkyBlock menu can't be moved out of hotbar slot 8 by any inventory action
 *       (click, number-key swap, drag, or drop). It only leaves when the game programmatically replaces it with the
 *       Energy Crystal or a Wither-King relic (those use direct inventory writes, not player click events).</li>
 * </ul>
 */
public class LinkedSlots implements Listener {

	private static final int MENU_SLOT = 8;
	/** Last server tick a linked swap ran per player — collapses a double-click's burst of events into one swap. */
	private static final Map<UUID, Integer> lastSwapTick = new HashMap<>();

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		if(!(e.getWhoClicked() instanceof Player p)) return;
		PlayerInventory inv = p.getInventory();

		// --- SkyBlock-menu lock: block any click that would move the menu out of slot 8 ---
		if(FakePlayerInventory.isSkyblockMenu(inv.getItem(MENU_SLOT))) {
			boolean clicksMenuSlot = e.getClickedInventory() != null && e.getClickedInventory().equals(inv) && e.getSlot() == MENU_SLOT;
			boolean numberKeyToMenu = e.getClick() == ClickType.NUMBER_KEY && e.getHotbarButton() == MENU_SLOT;
			boolean swapOffhandMenu = e.getClick() == ClickType.SWAP_OFFHAND && e.getClickedInventory() != null
					&& e.getClickedInventory().equals(inv) && e.getSlot() == MENU_SLOT;
			if(clicksMenuSlot || numberKeyToMenu || swapOffhandMenu) {
				e.setCancelled(true);
				return;
			}
		}

		// A shift+double-click fires a trailing DOUBLE_CLICK after our SHIFT_LEFT swap. If we just swapped
		// this tick, swallow it so vanilla doesn't gather/move the items we already placed.
		if(e.getClick() == ClickType.DOUBLE_CLICK
				&& e.getView().getTopInventory().getType() == InventoryType.CRAFTING
				&& lastSwapTick.getOrDefault(p.getUniqueId(), -1) == MinecraftServer.currentTick) {
			e.setCancelled(true);
			return;
		}

		// --- Linked slots: shift+left-click a backpack slot swaps with its hotbar column ---
		// The top row (slots 9-17) is ignored; only the middle/bottom rows (18-35) link.
		if(e.getClick() != ClickType.SHIFT_LEFT) return;
		if(e.getView().getTopInventory().getType() != InventoryType.CRAFTING) return; // only the E inventory
		if(e.getClickedInventory() == null || !e.getClickedInventory().equals(inv)) return;
		int slot = e.getSlot();
		if(slot < 18 || slot > 35) return; // middle + bottom backpack rows only (top row excluded) → vanilla otherwise
		int hotbar = Math.min((slot - 9) % 9, 7); // column, but the 9th column maps to 7 (slot 8 is the menu)

		ItemStack back = inv.getItem(slot);
		ItemStack bar = inv.getItem(hotbar);
		// Only swap two real items — if either slot is empty, let vanilla handle the click instead.
		if(isEmpty(back) || isEmpty(bar)) return;

		e.setCancelled(true); // we own this gesture now — suppress the vanilla shift-move
		// Double-click guard: its burst of events lands in one tick, so swap at most once per player per tick.
		int now = MinecraftServer.currentTick;
		if(lastSwapTick.getOrDefault(p.getUniqueId(), -1) == now) return;
		lastSwapTick.put(p.getUniqueId(), now);

		inv.setItem(slot, bar);
		inv.setItem(hotbar, back);
		p.updateInventory();
	}

	private static boolean isEmpty(ItemStack item) {
		return item == null || item.getType() == Material.AIR;
	}

	@EventHandler
	public void onInventoryDrag(InventoryDragEvent e) {
		if(!(e.getWhoClicked() instanceof Player p)) return;
		if(!FakePlayerInventory.isSkyblockMenu(p.getInventory().getItem(MENU_SLOT))) return;
		for(int raw : e.getRawSlots()) {
			Inventory at = e.getView().getInventory(raw);
			if(at != null && at.equals(p.getInventory()) && e.getView().convertSlot(raw) == MENU_SLOT) {
				e.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler
	public void onDrop(PlayerDropItemEvent e) {
		if(FakePlayerInventory.isSkyblockMenu(e.getItemDrop().getItemStack())) {
			e.setCancelled(true);
		}
	}

	// Pressing F with no inventory open fires this (not an InventoryClickEvent) — block swapping the menu to offhand.
	@EventHandler
	public void onSwapHands(PlayerSwapHandItemsEvent e) {
		if(FakePlayerInventory.isSkyblockMenu(e.getMainHandItem()) || FakePlayerInventory.isSkyblockMenu(e.getOffHandItem())) {
			e.setCancelled(true);
		}
	}
}
