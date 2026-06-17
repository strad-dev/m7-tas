package commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R7.inventory.CraftItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;
import plugin.M7tas;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/*
 * Eq (/eq) — a real Hypixel command.
 * Opens a 1-row chest GUI showing the player's worn armor (helmet/chestplate/leggings/boots in slots 0-3)
 * and a sugar cane in slot 8 whose stack size is the player's current speed floored to the lower 100
 * (e.g. 480 → a stack of 4) with the exact speed on its tooltip. Clicking an armor piece in your own
 * inventory while the menu is open swaps it onto your body (and into the matching GUI slot).
 *
 * Registered both as the /eq executor and as an event listener (two stateless instances — the GUI is
 * identified by its {@link EqHolder}, not by instance state).
 */
public class Eq implements CommandExecutor, Listener {

	private static final String TITLE = ChatColor.DARK_GRAY + "Equipment";
	private static final int SPEED_SLOT = 8;
	/** Last server tick a swap ran per player — collapses a double-click's burst of events into one swap. */
	private static final Map<UUID, Integer> lastSwapTick = new HashMap<>();

	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command cmd, @NonNull String label, String @NonNull [] args) {
		if(!(sender instanceof Player p)) {
			sender.sendMessage("Only players can run this");
			return true;
		}
		open(p);
		return true;
	}

	private static void open(Player p) {
		EqHolder holder = new EqHolder();
		Inventory gui = Bukkit.createInventory(holder, 9, TITLE);
		holder.setInventory(gui);
		refresh(p, gui);
		p.openInventory(gui);
		applySpeedCane(p); // must run after the menu exists — writes the cane via NMS (see below)
	}

	/** Mirror the player's worn armor into slots 0-3. The speed cane (slot 8) is set separately via NMS. */
	private static void refresh(Player p, Inventory gui) {
		for(int i = 0; i < 4; i++) {
			ItemStack worn = getArmor(p, i);
			gui.setItem(i, worn == null ? null : worn.clone());
		}
	}

	/**
	 * Write the speed cane into the open /eq menu's slot 8 via NMS. The cane's count is speed/10 (e.g. 650 -> 65),
	 * since the client renders a stack count clamped to the item's max_stack_size and that component is itself
	 * hard-capped at 99 — so a literal 3-digit speed can't be a count. The exact speed stays on the tooltip.
	 * We still raise MAX_STACK_SIZE to the count because sugar cane's default cap (64) would otherwise clamp
	 * counts of 65..99. Done via NMS because the Bukkit ItemStack path clamps the count to the item's max stack.
	 */
	private static void applySpeedCane(Player p) {
		ServerPlayer sp = ((CraftPlayer) p).getHandle();
		AbstractContainerMenu menu = sp.containerMenu;
		if(menu == null || SPEED_SLOT >= menu.slots.size()) return;
		int amount = Math.max(1, Math.min(99, currentSpeed(p) / 10)); // speed/10, within the 99 stack-count ceiling
		net.minecraft.world.item.ItemStack nms = CraftItemStack.asNMSCopy(speedItem(p));
		nms.set(DataComponents.MAX_STACK_SIZE, amount); // raise the cap so the client renders counts of 65..99
		nms.setCount(amount);
		menu.getSlot(SPEED_SLOT).set(nms);
		sp.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), SPEED_SLOT, nms));
	}

	private static ItemStack speedItem(Player p) {
		int speed = currentSpeed(p);
		ItemStack cane = new ItemStack(Material.SUGAR_CANE);
		ItemMeta meta = cane.getItemMeta();
		if(meta != null) {
			meta.setDisplayName(ChatColor.AQUA + "Speed: " + ChatColor.WHITE + speed);
			cane.setItemMeta(meta);
		}
		return cane;
	}

	/** Player's current movement speed on the 100-based scale (100 = vanilla default), all modifiers included. */
	private static int currentSpeed(Player p) {
		var attr = p.getAttribute(Attribute.MOVEMENT_SPEED);
		if(attr == null || attr.getBaseValue() == 0) return 100;
		return (int) Math.round(attr.getValue() / attr.getBaseValue() * 100);
	}

	// =================== Click handling: swap armor from the player's inventory ===================

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		if(!(e.getView().getTopInventory().getHolder() instanceof EqHolder)) return;
		e.setCancelled(true); // the GUI is fully controlled — only the armor swap below mutates anything
		if(e.getClick() == ClickType.DOUBLE_CLICK) return; // collect-to-cursor, not a swap
		if(!(e.getWhoClicked() instanceof Player p)) return;
		Inventory clicked = e.getClickedInventory();
		if(clicked == null || !clicked.equals(p.getInventory())) return; // only bottom-inventory clicks act

		ItemStack item = e.getCurrentItem();
		if(item == null || item.getType() == Material.AIR) return;
		int idx = armorSlotIndex(item.getType());
		if(idx < 0) return;

		// A double-click fires several events in the same tick — perform at most one swap per player per tick.
		int now = MinecraftServer.currentTick;
		if(lastSwapTick.getOrDefault(p.getUniqueId(), -1) == now) return;
		lastSwapTick.put(p.getUniqueId(), now);

		// Swap: equip the clicked piece, returning the previously-worn piece to the clicked slot.
		ItemStack worn = getArmor(p, idx);
		setArmor(p, idx, item.clone());
		e.setCurrentItem(worn); // null clears the slot if nothing was worn
		Inventory gui = e.getView().getTopInventory();
		gui.setItem(idx, item.clone());
		// A helmet swap changes speed next tick (HelmetSpeedSync poll), so refresh the cane afterwards.
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
			if(p.getOpenInventory().getTopInventory().getHolder() instanceof EqHolder) applySpeedCane(p);
		}, 2L);
	}

	@EventHandler
	public void onInventoryDrag(InventoryDragEvent e) {
		if(!(e.getView().getTopInventory().getHolder() instanceof EqHolder)) return;
		int topSize = e.getView().getTopInventory().getSize();
		for(int slot : e.getRawSlots()) {
			if(slot < topSize) { // any drag touching the GUI is blocked
				e.setCancelled(true);
				return;
			}
		}
	}

	// =================== Armor helpers ===================

	/** GUI/equipment slot for an armor material: 0 helmet, 1 chestplate, 2 leggings, 3 boots, else -1. */
	private static int armorSlotIndex(Material m) {
		String n = m.name();
		if(n.endsWith("_HELMET") || m == Material.PLAYER_HEAD || m == Material.CARVED_PUMPKIN) return 0;
		if(n.endsWith("_CHESTPLATE") || m == Material.ELYTRA) return 1;
		if(n.endsWith("_LEGGINGS")) return 2;
		if(n.endsWith("_BOOTS")) return 3;
		return -1;
	}

	private static ItemStack getArmor(Player p, int idx) {
		PlayerInventory inv = p.getInventory();
		return switch(idx) {
			case 0 -> inv.getHelmet();
			case 1 -> inv.getChestplate();
			case 2 -> inv.getLeggings();
			case 3 -> inv.getBoots();
			default -> null;
		};
	}

	private static void setArmor(Player p, int idx, ItemStack item) {
		PlayerInventory inv = p.getInventory();
		switch(idx) {
			case 0 -> inv.setHelmet(item);
			case 1 -> inv.setChestplate(item);
			case 2 -> inv.setLeggings(item);
			case 3 -> inv.setBoots(item);
		}
	}

	/** Marker holder identifying the /eq GUI in the click/drag handlers. */
	public static final class EqHolder implements InventoryHolder {
		private Inventory inv;
		void setInventory(Inventory inv) { this.inv = inv; }
		@Override public Inventory getInventory() { return inv; }
	}
}
