package commands;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
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
import plugin.Utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/*
 * Eq (/eq), a real Hypixel command.
 * Opens a 1-row chest GUI showing the player's worn armor (helmet/chestplate/leggings/boots in slots 0-3)
 * and a sugar cane in slot 8 whose stack size is the player's current speed floored to the lower 100
 * (e.g. 480 → a stack of 4) with the exact speed on its tooltip. Clicking an armor piece in your own
 * inventory while the menu is open swaps it onto your body (and into the matching GUI slot).
 *
 * Registered both as the /eq executor and as an event listener, as two stateless instances, since the GUI is
 * identified by its {@link EqHolder}, not by instance state).
 */
public class Eq implements CommandExecutor, Listener {

	private static final Component TITLE = Utils.msg("<dark_gray>Equipment");
	private static final int SPEED_SLOT = 8;
	/** Movement-speed modifiers {@link #currentSpeed} resolves the attribute WITHOUT: vanilla mechanics that
	 *  aren't part of the SkyBlock speed stat.  Add a key here and it stops counting; Speed/Soul Speed and the
	 *  plugin's own modifiers are deliberately absent, since those do belong in the stat. */
	private static final Set<NamespacedKey> IGNORED_SPEED_MODIFIERS = Set.of(NamespacedKey.minecraft("sprinting"));
	/** Last server tick a swap ran per player, which collapses a double-click's burst of events into one swap. */
	private static final Map<UUID, Integer> lastSwapTick = new HashMap<>();

	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command cmd, @NonNull String label, String @NonNull [] args) {
		if(!(sender instanceof Player p)) {
			sender.sendMessage(Utils.msg("Only players can run this"));
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
		applySpeedCane(p); // must run after the menu exists, since it writes the cane via NMS (see below)
	}

	/** Mirror the player's worn armor into slots 0-3, and the aggregate stats into 6-7.  Slot 8's speed cane is
	 *  set separately via NMS. */
	private static void refresh(Player p, Inventory gui) {
		for(int i = 0; i < 4; i++) {
			ItemStack worn = getArmor(p, i);
			gui.setItem(i, worn == null ? null : worn.clone());
		}
		gui.setItem(MELEE_STATS_SLOT, meleeStatsItem(p));
		gui.setItem(MAGIC_STATS_SLOT, magicStatsItem(p));
	}

	// =================== Aggregate stat readout (MAP.md §7b) ===================
	// Item lore is per-item, so it can only show that item's contribution.  These two slots show the thing lore
	// cannot: the player's whole aggregate, equipment, Accessory Power, tunings, profile sources and class bonus
	// included.  Generated from the stat layer, never authored, for the same reason lore is.

	private static final int MELEE_STATS_SLOT = 6;
	private static final int MAGIC_STATS_SLOT = 7;

	/** Slot 6: Damage, Strength and Crit Damage - the melee/beam half of the aggregate. */
	private static ItemStack meleeStatsItem(Player p) {
		return statsItem(p, Material.DIAMOND_SWORD, "<red>Offensive Stats",
				damage.Stat.DAMAGE, damage.Stat.STRENGTH, damage.Stat.CRIT_DAMAGE);
	}

	/** Slot 7: Intelligence and Ability Damage - what the beam multiplier and the ability formula read. */
	private static ItemStack magicStatsItem(Player p) {
		return statsItem(p, Material.POTION, "<aqua>Magic Stats",
				damage.Stat.INTELLIGENCE, damage.Stat.ABILITY_DAMAGE);
	}

	private static ItemStack statsItem(Player p, Material material, String title, damage.Stat... stats) {
		// The aggregate is path-resolved, so the readout has to pick a path and say which: a Mage's ability-path
		// Intelligence is a different number entirely from their beam-path one.
		damage.DungeonClass clazz = damage.DungeonClass.of(p);
		damage.DamagePath path = clazz.primaryPath();
		damage.StatBlock block = damage.Stats.of(p, path);

		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if(meta == null) return item;
		meta.displayName(Utils.mm(title));
		java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
		for(damage.Stat stat : stats) {
			// No SkyBlock glyph and no leading "+": this is an aggregate TOTAL, not a bonus something else adds to,
			// so a plus sign reads as "+3729.6 on top of what?".  Item lore still shows a "+", because there the
			// number really is that item's contribution.
			lore.add(Utils.mm("<gray>" + stat.display() + ": " + stat.colour()
					+ Utils.roundCommas(block.get(stat), 1)));
		}
		lore.add(net.kyori.adventure.text.Component.empty());
		lore.add(Utils.mm("<dark_gray>" + clazz + ", " + path.name().toLowerCase(java.util.Locale.ROOT) + " path"));
		meta.lore(lore);
		item.setItemMeta(meta);
		// The Magic Stats slot is a POTION, so vanilla appends its potion-effect lines under our lore.  This was
		// ItemFlag.HIDE_ADDITIONAL_TOOLTIP, deprecated in favour of naming the component to hide: the flag was one
		// switch covering a dozen unrelated "extra tooltip" sources, and the component form says which one is meant.
		// Set AFTER setItemMeta, since that writes the whole component patch.  Harmless on the Offensive Stats sword,
		// which has no potion contents to hide in the first place.
		item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
				.addHiddenComponents(DataComponentTypes.POTION_CONTENTS));
		return item;
	}

	/**
	 * Write the speed cane into the open /eq menu's slot 8 via NMS. The cane's count is speed/10 (e.g. 650 -> 65),
	 * since the client renders a stack count clamped to the item's max_stack_size and that component is itself
	 * hard-capped at 99, so a literal 3-digit speed can't be a count.  The exact speed stays on the tooltip.
	 * We still raise MAX_STACK_SIZE to the count because sugar cane's default cap (64) would otherwise clamp
	 * counts of 65..99. Done via NMS because the Bukkit ItemStack path clamps the count to the item's max stack.
	 */
	private static void applySpeedCane(Player p) {
		ServerPlayer sp = ((CraftPlayer) p).getHandle();
		AbstractContainerMenu menu = sp.containerMenu;
		if(SPEED_SLOT >= menu.slots.size()) return;
		int amount = Math.clamp(currentSpeed(p) / 10, 1, 99); // speed/10, within the 99 stack-count ceiling
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
			meta.displayName(Utils.mm("<aqua>Speed: <white>" + speed));
			cane.setItemMeta(meta);
		}
		return cane;
	}

	/**
	 * Player's current movement speed on the 100-based scale (100 = vanilla default).
	 * <p>
	 * Resolves the attribute here rather than calling {@code getValue()}, so that
	 * {@link #IGNORED_SPEED_MODIFIERS} can be left out of the sum outright instead of undone afterwards.
	 * {@code LivingEntity.setSprinting} adds {@code minecraft:sprinting} (+0.3 ADD_MULTIPLIED_TOTAL) as a
	 * TRANSIENT modifier for as long as you sprint, and {@code getValue()} resolves it like every other one,
	 * which is why a sprinting player read 30% high (a real 400 showed as 520).  Transient modifiers DO appear
	 * in {@code getModifiers()}, so none of this needs NMS.
	 * <p>
	 * The three passes mirror {@code AttributeInstance.calculateValue}: base plus every ADD_NUMBER, then each
	 * ADD_SCALAR against that summed base, then each MULTIPLY_SCALAR_1 in turn.  Each pass is internally
	 * order-independent (two sums and a product), so walking one unordered collection three times matches
	 * vanilla exactly.  Negatives are clamped like vanilla's {@code sanitizeValue}, whose floor for
	 * movement_speed is 0; the ceiling is a Spigot config value and far out of reach here, so it is ignored.
	 */
	private static int currentSpeed(Player p) {
		var attr = p.getAttribute(Attribute.MOVEMENT_SPEED);
		if(attr == null || attr.getBaseValue() == 0) return 100;
		double vanillaBase = attr.getBaseValue(); // the 100-point scale is relative to this, so keep it separate
		Collection<AttributeModifier> mods = attr.getModifiers();

		double base = vanillaBase;
		for(AttributeModifier mod : mods) {
			if(counts(mod, AttributeModifier.Operation.ADD_NUMBER)) base += mod.getAmount();
		}
		double value = base;
		for(AttributeModifier mod : mods) {
			if(counts(mod, AttributeModifier.Operation.ADD_SCALAR)) value += base * mod.getAmount();
		}
		for(AttributeModifier mod : mods) {
			if(counts(mod, AttributeModifier.Operation.MULTIPLY_SCALAR_1)) value *= 1 + mod.getAmount();
		}
		return (int) Math.round(Math.max(0, value) / vanillaBase * 100);
	}

	/** Whether a modifier belongs in {@code op}'s pass of {@link #currentSpeed}'s sum. */
	private static boolean counts(AttributeModifier mod, AttributeModifier.Operation op) {
		return mod.getOperation() == op && !IGNORED_SPEED_MODIFIERS.contains(mod.getKey());
	}

	// =================== Click handling: swap armor from the player's inventory ===================

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		if(!(e.getView().getTopInventory().getHolder() instanceof EqHolder)) return;
		e.setCancelled(true); // the GUI is fully controlled; only the armor swap below mutates anything
		if(e.getClick() == ClickType.DOUBLE_CLICK) return; // collect-to-cursor, not a swap
		if(!(e.getWhoClicked() instanceof Player p)) return;
		Inventory clicked = e.getClickedInventory();
		if(clicked == null || !clicked.equals(p.getInventory())) return; // only bottom-inventory clicks act

		ItemStack item = e.getCurrentItem();
		if(item == null || item.getType() == Material.AIR) return;
		int idx = armorSlotIndex(item.getType());
		if(idx < 0) return;

		// A double-click fires several events in the same tick, so perform at most one swap per player per tick.
		int now = MinecraftServer.currentTick;
		if(lastSwapTick.getOrDefault(p.getUniqueId(), -1) == now) return;
		lastSwapTick.put(p.getUniqueId(), now);

		// Swap: equip the clicked piece, returning the previously-worn piece to the clicked slot.
		ItemStack worn = getArmor(p, idx);
		setArmor(p, idx, item.clone());
		e.setCurrentItem(worn); // null clears the slot if nothing was worn
		Inventory gui = e.getView().getTopInventory();
		gui.setItem(idx, item.clone());
		// A helmet swap changes speed next tick (HelmetSpeedSync poll), so refresh the cane afterwards - and it
		// changes the aggregate too, by thousands of Intelligence in the Storm's-helmet case, so redraw both.
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
			if(p.getOpenInventory().getTopInventory().getHolder() instanceof EqHolder) {
				gui.setItem(MELEE_STATS_SLOT, meleeStatsItem(p));
				gui.setItem(MAGIC_STATS_SLOT, magicStatsItem(p));
				applySpeedCane(p);
			}
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
		@Override public @NonNull Inventory getInventory() { return inv; }
	}
}
