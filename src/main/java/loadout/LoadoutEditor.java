package loadout;

import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import plugin.Catalog;
import plugin.Utils;

import java.util.List;

/**
 * {@code /m7loadout} - a 54-slot chest editor for the player's CURRENTLY SELECTED class loadout, laid out like a
 * real inventory:
 * <pre>
 *   row 1 (0-8)   : item palette (full row)
 *   row 2 (9-12)  : helmet, chestplate, leggings, boots | (13) glass | (14) trash | (15) prev | (16) next | (17) default
 *   row 3 (18-26) : inventory storage row 1
 *   row 4 (27-35) : inventory storage row 2
 *   row 5 (36-44) : inventory storage row 3
 *   row 6 (45-53) : hotbar
 * </pre>
 * GUI slots map to the 41-slot loadout array via {@link #arrIndex(int)}. On close the editable slots are serialized
 * and saved for the selected class. {@code /m7loadout reset} re-seeds from the default kit.
 * <p>
 * The palette comes straight from {@link Catalog#palette()} in memory, so it is always current and works before the
 * catalog has ever been exported.
 * <p>
 * NOTE: twin of the network plugin's {@code loadout/LoadoutEditor.java} - keep in sync, ESPECIALLY the three
 * containment layers in {@link #onClick}/{@link #onDropItem}/{@link #onClose} (slot contents are palette COPIES, so
 * anything that escapes the GUI is a free item spawn).
 */
public class LoadoutEditor implements CommandExecutor, Listener {
	private static final int PALETTE_START = 0, PALETTE_COUNT = 9;          // slots 0..8 (entire first row)
	private static final int TRASH_SLOT = 14, PREV_SLOT = 15, NEXT_SLOT = 16, RESET_SLOT = 17;
	// Last hotbar slot (loadout index 8) is RESERVED for the SkyBlock Menu: every class kit puts it there, so the
	// editor pins it, refuses to let it be edited, and writes it back on save regardless of what was saved before.
	private static final int MENU_GUI_SLOT = 53, MENU_ARR_INDEX = 8;

	/** GUI slot -> loadout array index (0-35 main, 36 helmet, 37 chest, 38 legs, 39 boots, 40 offhand), or -1. */
	private static int arrIndex(int gui) {
		switch(gui) {
			case 9: return 36;  // helmet
			case 10: return 37; // chestplate
			case 11: return 38; // leggings
			case 12: return 39; // boots
			case MENU_GUI_SLOT: return -1; // reserved: SkyBlock Menu, pinned in open() and written back in onClose()
			default: // fall through to the inventory rows
		}
		if(gui >= 18 && gui <= 26) return 9 + (gui - 18);   // storage row 1 -> arr 9..17
		if(gui >= 27 && gui <= 35) return 18 + (gui - 27);  // storage row 2 -> arr 18..26
		if(gui >= 36 && gui <= 44) return 27 + (gui - 36);  // storage row 3 -> arr 27..35
		if(gui >= 45 && gui <= 53) return gui - 45;         // hotbar       -> arr 0..8
		return -1;
	}

	private static boolean isPalette(int gui) {
		return gui >= PALETTE_START && gui < PALETTE_START + PALETTE_COUNT;
	}

	@Override
	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
		if(!(sender instanceof Player p)) {
			sender.sendMessage(Utils.msg("<red>Only players can edit a loadout"));
			return true;
		}
		String role = Loadouts.getSelectedClass(p.getUniqueId());
		if(role == null) {
			p.sendMessage(Utils.msg("<red>Pick a class first with <white>/class <name>"));
			return true;
		}
		Loadouts.seedIfAbsent(p.getUniqueId(), role);
		// Saved loadouts hold frozen item copies; bring them up to the CURRENT item definitions before we show or
		// reset anything, so the editor never displays (or re-saves) a stale item. See ItemRefresh.
		ItemRefresh.refreshSaved(p.getUniqueId());

		if(args.length >= 1 && args[0].equalsIgnoreCase("reset")) {
			Loadouts.setContents(p.getUniqueId(), role, Catalog.defaultFor(role));
			p.sendMessage(Utils.msg("<yellow>Reset your <white><role></white> loadout to the default kit",
					Placeholder.unparsed("role", role)));
			return true;
		}

		open(p, role);
		return true;
	}

	private void open(Player p, String role) {
		EditorHolder holder = new EditorHolder(role, Catalog.palette());
		Inventory gui = Bukkit.createInventory(holder, 54,
				Utils.msg("<dark_gray>Loadout: <white><role>", Placeholder.unparsed("role", role)));
		holder.inv = gui;

		ItemStack[] arr = Loadouts.getContents(p.getUniqueId(), role); // non-null after seedIfAbsent
		for(int g = 0; g < 54; g++) {
			int idx = arrIndex(g);
			if(idx >= 0) gui.setItem(g, arr[idx]);
		}
		// Pin the SkyBlock Menu: always the class default's copy, so a loadout saved before this slot was reserved
		// (or one whose menu was trashed) gets it back.
		ItemStack menu = Catalog.defaultFor(role)[MENU_ARR_INDEX];
		gui.setItem(MENU_GUI_SLOT, menu != null ? menu : arr[MENU_ARR_INDEX]);
		gui.setItem(13, filler());
		gui.setItem(TRASH_SLOT, button(Material.LAVA_BUCKET, "<red>Trash (click with an item to delete it)"));
		refreshPalette(gui, holder);

		p.openInventory(gui);
		// Idle players on m7 sit in spectator, and vanilla refuses them container clicks - arm the bypass so they
		// can edit without leaving spectator mode. No-op for anyone not in spectator.
		SpectatorGuiAccess.install(p);
		p.sendMessage(Utils.msg("<gray>Editing <white><role></white>. Row 1 = item palette (click to copy); row 2 = armor + page/reset buttons; rows 3-5 = inventory; bottom = hotbar. Close to save.",
				Placeholder.unparsed("role", role)));
	}

	/** (Re)draw the palette items + page/reset buttons for the holder's current page. */
	private void refreshPalette(Inventory gui, EditorHolder holder) {
		List<ItemStack> pal = holder.palette;
		int pages = Math.max(1, (pal.size() + PALETTE_COUNT - 1) / PALETTE_COUNT);
		if(holder.page < 0) holder.page = 0;
		if(holder.page >= pages) holder.page = pages - 1;
		for(int i = 0; i < PALETTE_COUNT; i++) {
			int idx = holder.page * PALETTE_COUNT + i;
			gui.setItem(PALETTE_START + i, idx < pal.size() ? pal.get(idx).clone() : null);
		}
		gui.setItem(PREV_SLOT, holder.page > 0 ? button(Material.ARROW, "<yellow>Previous page") : filler());
		gui.setItem(NEXT_SLOT, holder.page < pages - 1 ? button(Material.ARROW, "<yellow>Next page") : filler());
		gui.setItem(RESET_SLOT, button(Material.BARRIER, "<red>Load default kit"));
	}

	// ===== events =====
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		if(!(e.getView().getTopInventory().getHolder() instanceof EditorHolder holder)) return;
		int raw = e.getRawSlot();
		boolean top = raw < e.getView().getTopInventory().getSize();

		if(!top) {
			e.setCancelled(true); // lock the player's own inventory - nothing can be moved out of the editor into it
			return;
		}
		if(arrIndex(raw) >= 0) {
			// Editable slot: these are palette COPIES, not real items, so anything that escapes the editor is a
			// free item spawn. Allowlist, not a blocklist - only the actions that move a stack between the cursor
			// and this slot are permitted; everything else (shift-click, hotbar keys, Q, creative clone, and
			// anything Bukkit couldn't classify) is denied, so a new or unrecognised action fails CLOSED.
			switch(e.getAction()) {
				case NOTHING, PICKUP_ALL, PICKUP_SOME, PICKUP_HALF, PICKUP_ONE,
						PLACE_ALL, PLACE_SOME, PLACE_ONE, SWAP_WITH_CURSOR,
						// Bundle ops are cursor<->slot too, so they can't move anything out either.
						PICKUP_FROM_BUNDLE, PICKUP_ALL_INTO_BUNDLE, PICKUP_SOME_INTO_BUNDLE,
						PLACE_FROM_BUNDLE, PLACE_ALL_INTO_BUNDLE, PLACE_SOME_INTO_BUNDLE -> { }
				default -> e.setCancelled(true);
			}
			return;
		}

		e.setCancelled(true);
		if(isPalette(raw)) {
			ItemStack tmpl = e.getCurrentItem();
			ItemStack cursor = e.getCursor();
			if(tmpl != null && !tmpl.getType().isAir() && (cursor == null || cursor.getType().isAir())
					&& e.getWhoClicked() instanceof Player p) {
				p.setItemOnCursor(tmpl.clone());
			}
			return;
		}
		if(raw == PREV_SLOT) { holder.page--; refreshPalette(holder.inv, holder); return; }
		if(raw == NEXT_SLOT) { holder.page++; refreshPalette(holder.inv, holder); return; }
		if(raw == RESET_SLOT) {
			ItemStack[] def = Catalog.defaultFor(holder.role);
			for(int g = 0; g < 54; g++) {
				int idx = arrIndex(g);
				if(idx >= 0) holder.inv.setItem(g, def[idx]);
			}
		}
		if(raw == TRASH_SLOT && e.getWhoClicked() instanceof Player tp) {
			ItemStack held = tp.getItemOnCursor();
			if(held != null && !held.getType().isAir()) tp.setItemOnCursor(null); // delete the held item
		}
		// other top slots (filler 13, page buttons) stay cancelled.
	}

	@EventHandler
	public void onDrag(InventoryDragEvent e) {
		if(!(e.getView().getTopInventory().getHolder() instanceof EditorHolder)) return;
		int topSize = e.getView().getTopInventory().getSize();
		for(int raw : e.getRawSlots()) {
			if(raw >= topSize || arrIndex(raw) < 0) { // bottom inventory, or a non-editable top slot
				e.setCancelled(true);
				return;
			}
		}
	}

	/**
	 * Backstop for every drop path, whatever {@link InventoryClickEvent} classified the click as: while an editor
	 * is open on top, nothing this player does can put an item on the ground. Cheaper to reason about than keeping
	 * the click allowlist exhaustive forever.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onDropItem(PlayerDropItemEvent e) {
		if(e.getPlayer().getOpenInventory().getTopInventory().getHolder() instanceof EditorHolder) e.setCancelled(true);
	}

	@EventHandler
	public void onClose(InventoryCloseEvent e) {
		if(!(e.getView().getTopInventory().getHolder() instanceof EditorHolder holder)) return;
		if(!(e.getPlayer() instanceof Player p)) return;
		// The cleanup runs in a finally: the spectator bypass rewrites EVERY container click from this player as
		// non-spectator, so if the save throws it must still come off - otherwise it stays armed on the connection
		// for the rest of the session. Same for the cursor, which would otherwise hold a live palette copy.
		try {
			ItemStack[] arr = new ItemStack[41];
			for(int g = 0; g < 54; g++) {
				int idx = arrIndex(g);
				if(idx >= 0) arr[idx] = holder.inv.getItem(g);
			}
			arr[MENU_ARR_INDEX] = holder.inv.getItem(MENU_GUI_SLOT); // reserved slot: arrIndex skips it, so save it here
			Loadouts.setContents(p.getUniqueId(), holder.role, arr);
			p.sendMessage(Utils.msg("<green>Saved your <white><role></white> loadout",
					Placeholder.unparsed("role", holder.role)));
		} finally {
			SpectatorGuiAccess.uninstall(p);
			p.setItemOnCursor(null); // don't let a held palette copy leak into the player's inventory
		}
	}

	// ===== helpers =====
	private static ItemStack filler() {
		return button(Material.GRAY_STAINED_GLASS_PANE, " ");
	}

	private static ItemStack button(Material mat, String name) {
		ItemStack it = new ItemStack(mat);
		ItemMeta m = it.getItemMeta();
		if(m != null) {
			m.displayName(Utils.msg(name).decoration(TextDecoration.ITALIC, false));
			it.setItemMeta(m);
		}
		return it;
	}

	/** Marker holder carrying the editor's class + palette paging state. */
	public static final class EditorHolder implements InventoryHolder {
		final String role;
		final List<ItemStack> palette;
		int page;
		Inventory inv;

		EditorHolder(String role, List<ItemStack> palette) {
			this.role = role;
			this.palette = palette;
		}

		@Override
		public @NotNull Inventory getInventory() {
			return inv;
		}
	}
}
