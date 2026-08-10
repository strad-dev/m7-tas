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
import instructions.clear.DungeonMap;
import plugin.FakePlayerInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Two inventory conveniences for real players:
 *
 * <ul>
 *   <li><b>Linked slots</b>: in the normal player inventory (the one opened with E), shift+left-clicking an item
 *       in a backpack slot (9-35) swaps it with the hotbar slot in the same column.  The 9th column (slots
 *       17/26/35) maps to hotbar slot 7 instead of 8, since hotbar slot 8 is reserved for the SkyBlock menu.</li>
 *   <li><b>SkyBlock-menu lock</b>: the SkyBlock menu can't be moved out of hotbar slot 8 by any inventory action,
 *       whether click, number-key swap, drag, or drop.  It only leaves when the game programmatically replaces it
 *       with the Energy Crystal or a Wither-King relic, which use direct inventory writes, not click events.</li>
 * </ul>
 */
public class LinkedSlots implements Listener {

	private static final int MENU_SLOT = 8;
	/** Raw slot of the offhand in the survival/E inventory view, where the clear-phase dungeon map is locked. */
	private static final int OFFHAND_SLOT = 40;
	/** A double-click's pickup-all event can trail its first click by up to the click window (~5 ticks). */
	private static final int DOUBLE_CLICK_WINDOW = 10;
	/** Last server tick a linked swap ran per player, which collapses a double-click's burst of events into one swap. */
	private static final Map<UUID, Integer> lastSwapTick = new HashMap<>();
	/** Hotbar slot the most recent linked swap moved an item INTO.  A trailing double-click on it is ignored. */
	private static final Map<UUID, Integer> lastSwapHotbar = new HashMap<>();

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

		// --- offhand dungeon-map lock: the clear-phase map can't be moved out of the offhand by any click ---
		if(DungeonMap.isDungeonMap(inv.getItemInOffHand())) {
			boolean clicksOffhand = e.getClickedInventory() != null && e.getClickedInventory().equals(inv) && e.getSlot() == OFFHAND_SLOT;
			// SWAP_OFFHAND (F while hovering any slot) would pull the map out regardless of which slot is hovered.
			boolean swapOffhand = e.getClick() == ClickType.SWAP_OFFHAND;
			if(clicksOffhand || swapOffhand) {
				e.setCancelled(true);
				return;
			}
		}

		// A shift+double-click's second physical click trails ~5 ticks later as another SHIFT_LEFT, but landing
		// on the hotbar slot I just swapped the item INTO (slotType QUICKBAR).  Vanilla would shift-move that
		// item back out and undo the swap, so swallow a trailing quickbar shift or double-click on that slot.
		if((e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.DOUBLE_CLICK)
				&& e.getSlotType() == InventoryType.SlotType.QUICKBAR) {
			Integer swapTick = lastSwapTick.get(p.getUniqueId());
			Integer swapHotbar = lastSwapHotbar.get(p.getUniqueId());
			if(swapTick != null && swapHotbar != null && swapHotbar == e.getSlot()
					&& MinecraftServer.currentTick - swapTick <= DOUBLE_CLICK_WINDOW) {
				e.setCancelled(true);
				return;
			}
		}

		// --- Linked slots: shift+left-click a backpack slot swaps with its hotbar column ---
		// The top row (slots 9-17) is ignored; only the middle/bottom rows (18-35) link.
		if(e.getClick() != ClickType.SHIFT_LEFT) return;
		// Only the E inventory (CRAFTING).  Note that creative's inventory is client-authoritative: its clicks arrive
		// as ServerboundSetCreativeModeSlotPacket and never as an InventoryClickEvent, so this is survival/adventure only.
		if(e.getView().getTopInventory().getType() != InventoryType.CRAFTING) return;
		if(e.getClickedInventory() == null || !e.getClickedInventory().equals(inv)) return;
		int slot = e.getSlot();
		if(slot < 18 || slot > 35) return; // middle + bottom backpack rows only (top row excluded) → vanilla otherwise
		int hotbar = Math.min((slot - 9) % 9, 7); // column, but the 9th column maps to 7 (slot 8 is the menu)

		ItemStack back = inv.getItem(slot);
		ItemStack bar = inv.getItem(hotbar);
		// Only swap two real items.  If either slot is empty, let vanilla handle the click instead.
		if(isEmpty(back) || isEmpty(bar)) return;

		e.setCancelled(true); // I own this gesture now, so suppress the vanilla shift-move
		// Double-click guard: its burst of events lands in one tick, so swap at most once per player per tick.
		int now = MinecraftServer.currentTick;
		if(lastSwapTick.getOrDefault(p.getUniqueId(), -1) == now) return;
		lastSwapTick.put(p.getUniqueId(), now);
		lastSwapHotbar.put(p.getUniqueId(), hotbar);

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
		ItemStack dropped = e.getItemDrop().getItemStack();
		// The menu and the auto-managed offhand dungeon map are both undroppable.
		if(FakePlayerInventory.isSkyblockMenu(dropped) || DungeonMap.isDungeonMap(dropped)) {
			e.setCancelled(true);
		}
	}

	// Pressing F with no inventory open fires this instead of an InventoryClickEvent, so block swapping the menu,
	// or the clear-phase dungeon map that the clear tick keeps in the offhand, out of its slot.
	@EventHandler
	public void onSwapHands(PlayerSwapHandItemsEvent e) {
		if(FakePlayerInventory.isSkyblockMenu(e.getMainHandItem()) || FakePlayerInventory.isSkyblockMenu(e.getOffHandItem())
				|| DungeonMap.isDungeonMap(e.getMainHandItem()) || DungeonMap.isDungeonMap(e.getOffHandItem())) {
			e.setCancelled(true);
		}
	}
}
