package loadout;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import plugin.Catalog;
import plugin.Menus;
import plugin.Utils;

import java.util.*;

/**
 * {@code /m7loadout} - the editor for the player's CURRENTLY SELECTED class loadout.
 *
 * <p><b>The player's OWN inventory is the loadout's four inventory rows.</b>  A 41-slot kit is 36 inventory
 * slots plus armour and an off-hand, and the old editor drew those 36 inside the chest, where three rows of a
 * chest and a chest's own hotbar row read as "some GUI" rather than as your inventory - which is what people
 * kept misreading.  So on open we SNAPSHOT the real inventory, write the loadout into it, and let the player
 * arrange it exactly as they would in a run; on close we save what is in those 36 slots and put their own items
 * back.  Everything else lives in the chest:
 * <pre>
 *   rows 1-4 (0-35) : item palette, one page (click to copy to the cursor, shift-click to drop it in a free slot)
 *   row 5    (36)   : previous page          (37-43): the labelled seam          (44): next page
 *   row 6    (45-48): helmet, chestplate, leggings, boots
 *            (52)   : trash - click with an item on the cursor to delete it
 *            (53)   : load the class's default kit
 * </pre>
 * <b>There is no off-hand slot.</b>  M7 does not allow an off-hand item, so array slot 40 is never editable and
 * is written back EMPTY on save, which also clears one saved before that rule existed.
 * <p>
 * An empty armour slot shows a white pane named for what belongs there, so the row is readable when
 * the kit is bare.  Those panes are placeholders, never contents: {@link #isPlaceholder} is what keeps one out of
 * a saved loadout.
 *
 * <p><b>Nothing may escape, and nothing may be lost.</b>  Two hazards, and they pull in opposite directions: the
 * palette hands out COPIES, so any copy that reaches the world is a free item spawn; and the player's real
 * inventory is sitting in a field, so any path that ends the editor without restoring it eats their items.  Hence
 * {@link #finish}, the ONE way an editor ends - save, restore, clear the cursor - reached from the close, the
 * quit, the death and the plugin-disable paths alike, and idempotent because it removes the snapshot first.
 * Clicks are an ALLOWLIST with a default of cancel, so an action Bukkit adds later fails closed.
 *
 * <p>The palette comes straight from {@link Catalog#palette()} in memory, so it is always current and works
 * before the catalog has ever been exported.
 *
 * <p>NOTE: twin of the network plugin's {@code loadout/LoadoutEditor.java} - keep in sync, especially the
 * containment layers and {@link #finish}.
 */
public class LoadoutEditor implements CommandExecutor, Listener {
	private static final int PALETTE_START = 0, PALETTE_COUNT = 36;   // rows 1-4 of the chest
	/** Row 5 is the seam: the page arrows at each end, a labelled bar between them. */
	private static final int PREV_SLOT = 36, DIVIDER_START = 37, DIVIDER_END = 43, NEXT_SLOT = 44;
	private static final int HELMET_SLOT = 45, CHEST_SLOT = 46, LEGS_SLOT = 47, BOOTS_SLOT = 48;
	private static final int TRASH_SLOT = 52, RESET_SLOT = 53;
	/**
	 * The pet you START the run with, in the dead space before the buttons.
	 * <p>
	 * It belongs here rather than only in {@code /pets} because it is part of what you take in, like the kit
	 * itself - and unlike the rest of this window it is NOT saved into the loadout: it writes straight through to
	 * {@code pets/<uuid>.json}.
	 * <p>
	 * <b>Its own member there, NOT the pet that is currently out.</b>  Pointing this at {@code equipped} looked
	 * tidier - one value, not two - and was wrong: {@code /pets} and every autopet rule move that field during a
	 * run, so the button showed whatever the last run left out and appeared to change on its own.
	 * {@code Pets.applyStartingPets} is what turns the preference into the pet you actually begin with.
	 */
	private static final int PET_SLOT = 51;
	/**
	 * The last gear slot in the bottom row; everything between it and the trash is dead space.
	 * <b>There is no off-hand slot: M7 does not allow an off-hand item</b>, so array slot 40 is never editable and
	 * is written back EMPTY on every save rather than merely left alone - see {@code finish}.
	 */
	private static final int GEAR_END = BOOTS_SLOT;
	/** The 36 loadout slots the player's own inventory stands in for: [0..8] hotbar, [9..35] storage. */
	private static final int MAIN_SLOTS = 36;
	// Hotbar slot 9 (loadout index 8) is RESERVED for the SkyBlock Menu: every class kit puts it there, so the
	// editor pins it, refuses to let it be edited, and writes it back on save regardless of what was saved before.
	private static final int MENU_ARR_INDEX = 8;

	/** Open editors: player -> the 36 inventory slots we took from them.  Present == an editor is live. */
	private final Map<UUID, ItemStack[]> snapshots = new HashMap<>();

	/** Chest slot -> loadout array index, for the armour and off-hand row only (the 36 are in the player's own). */
	private static int arrIndex(int gui) {
		return switch(gui) {
			case HELMET_SLOT -> 36;
			case CHEST_SLOT -> 37;
			case LEGS_SLOT -> 38;
			case BOOTS_SLOT -> 39;
			default -> -1; // no off-hand: M7 does not allow one, so array slot 40 is never editable
		};
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
		if(snapshots.containsKey(p.getUniqueId())) return; // already editing; a second open would snapshot the copies

		EditorHolder holder = new EditorHolder(role, Catalog.palette());
		Inventory gui = Bukkit.createInventory(holder, 54,
				Utils.msg("<dark_gray>Loadout: <white><role>", Placeholder.unparsed("role", role)));
		holder.inv = gui;

		ItemStack[] arr = Loadouts.getContents(p.getUniqueId(), role); // non-null after seedIfAbsent
		// Take the player's own inventory BEFORE anything is written into it: this array is the only copy.
		snapshots.put(p.getUniqueId(), takeInventory(p));
		writeMain(p, arr);
		pinMenu(p, role, arr);
		for(int g = HELMET_SLOT; g <= GEAR_END; g++) gui.setItem(g, orPlaceholder(arr[arrIndex(g)], g));
		for(int g = GEAR_END + 1; g < TRASH_SLOT; g++) {
			gui.setItem(g, g == PET_SLOT ? petButton(p) : filler()); // dead space before the buttons
		}
		drawDivider(gui);
		gui.setItem(TRASH_SLOT, button(Material.LAVA_BUCKET, "<red>Trash (click with an item to delete it)"));
		refreshPalette(gui, holder);

		p.openInventory(gui);
		// Idle players on m7 sit in spectator, and vanilla refuses them container clicks - arm the bypass so they
		// can edit without leaving spectator mode. No-op for anyone not in spectator.
		SpectatorGuiAccess.install(p);
		p.sendMessage(Utils.msg("<gray>Editing <white><role>",
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
		// A missing arrow falls back to the seam pane, not a plain filler, so row 5 reads as one unbroken bar.
		gui.setItem(PREV_SLOT, holder.page > 0 ? button(Material.ARROW, "<yellow>Previous page") : divider());
		gui.setItem(NEXT_SLOT, holder.page < pages - 1 ? button(Material.ARROW, "<yellow>Next page") : divider());
		gui.setItem(RESET_SLOT, button(Material.BARRIER, "<red>Load default kit"));
	}

	// ===== events =====
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		if(!(e.getView().getTopInventory().getHolder() instanceof EditorHolder holder)) return;
		if(Menus.ignoreDoubleClick(e)) return;
		if(!(e.getWhoClicked() instanceof Player p)) return;
		int raw = e.getRawSlot();

		if(raw >= e.getView().getTopInventory().getSize()) {
			// The player's own inventory, which IS rows 1-4 of the loadout right now. Everything that keeps the
			// items inside these 36 slots is allowed; anything that could move one OUT (shift into the chest, a
			// drop, a double-click collect that would also vacuum palette copies) is denied, default-cancel.
			int slot = e.getSlot();
			if(slot == MENU_ARR_INDEX || e.getHotbarButton() == MENU_ARR_INDEX) {
				e.setCancelled(true); // the pinned SkyBlock Menu, whichever end of the swap it is on
				return;
			}
			switch(e.getAction()) {
				case NOTHING, PICKUP_ALL, PICKUP_SOME, PICKUP_HALF, PICKUP_ONE,
						PLACE_ALL, PLACE_SOME, PLACE_ONE, SWAP_WITH_CURSOR,
						HOTBAR_SWAP, HOTBAR_MOVE_AND_READD,
						PICKUP_FROM_BUNDLE, PICKUP_ALL_INTO_BUNDLE, PICKUP_SOME_INTO_BUNDLE,
						PLACE_FROM_BUNDLE, PLACE_ALL_INTO_BUNDLE, PLACE_SOME_INTO_BUNDLE -> { }
				default -> e.setCancelled(true);
			}
			return;
		}

		e.setCancelled(true); // every chest slot is driven by hand below, so nothing vanilla moves anything here
		if(arrIndex(raw) >= 0) {
			swapArmour(p, holder.inv, raw);
			return;
		}
		if(isPalette(raw)) {
			ItemStack tmpl = e.getCurrentItem();
			if(tmpl == null || tmpl.getType().isAir()) return;
			if(e.isShiftClick()) {
				giveToLoadout(p, tmpl.clone()); // straight into the first free inventory slot
				return;
			}
			ItemStack cursor = e.getCursor();
			if(cursor == null || cursor.getType().isAir()) p.setItemOnCursor(tmpl.clone());
			return;
		}
		if(raw == PREV_SLOT) { holder.page--; refreshPalette(holder.inv, holder); return; }
		if(raw == NEXT_SLOT) { holder.page++; refreshPalette(holder.inv, holder); return; }
		if(raw == RESET_SLOT) {
			ItemStack[] def = Catalog.defaultFor(holder.role);
			writeMain(p, def);
			pinMenu(p, holder.role, def);
			for(int g = HELMET_SLOT; g <= GEAR_END; g++) holder.inv.setItem(g, orPlaceholder(def[arrIndex(g)], g));
			return;
		}
		if(raw == PET_SLOT) {
			// Left steps forward, right steps back - the cycling-button convention the rest of the network's
			// menus use.  A roster of five is short enough to step; the moment it is not, this wants the
			// pets/PetPicker treatment instead, which cannot live here because opening another window would
			// end the editing session and hand the inventory back.
			cyclePet(p, e.isRightClick());
			holder.inv.setItem(PET_SLOT, petButton(p));
			return;
		}
		if(raw == TRASH_SLOT) {
			ItemStack held = p.getItemOnCursor();
			if(held != null && !held.getType().isAir()) p.setItemOnCursor(null); // delete the held item
		}
	}

	/**
	 * One armour / off-hand slot, done by hand so the slot is never left blank: taking an item back puts the
	 * placeholder pane there, and placing one swaps out whatever was underneath (a placeholder swaps out as
	 * nothing).  Doing this through the vanilla click would leave the pane in the loadout or the slot empty.
	 */
	private void swapArmour(Player p, Inventory gui, int slot) {
		ItemStack current = gui.getItem(slot);
		ItemStack held = isPlaceholder(current, slot) ? null : current;
		ItemStack cursor = p.getItemOnCursor();
		if(cursor == null || cursor.getType().isAir()) {
			if(held == null) return;
			p.setItemOnCursor(held);
			gui.setItem(slot, placeholder(slot));
		} else {
			gui.setItem(slot, cursor.clone());
			p.setItemOnCursor(held);
		}
		p.updateInventory();
	}

	@EventHandler
	public void onDrag(InventoryDragEvent e) {
		if(!(e.getView().getTopInventory().getHolder() instanceof EditorHolder)) return;
		int topSize = e.getView().getTopInventory().getSize();
		for(int raw : e.getRawSlots()) {
			// Chest slots are all hand-driven, and the pinned menu slot is off limits: cancel the whole drag.
			if(raw < topSize || e.getView().convertSlot(raw) == MENU_ARR_INDEX) {
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
		finish(p, holder, true);
	}

	/**
	 * A quit with the editor still open.  Whichever of this and {@link #onClose} the server fires first ends the
	 * editor; {@link #finish} is idempotent, so the other one is a no-op.  Without this, a disconnect that skipped
	 * the close event would leave the player's real inventory in {@link #snapshots} and gone.
	 */
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		finish(e.getPlayer(), holderOf(e.getPlayer()), true);
	}

	/**
	 * Dying with the editor open.  The drop list the server just built is made of PALETTE COPIES, so it is thrown
	 * away and refilled from the snapshot: the player drops the items they actually had, exactly as they would
	 * have without the editor open.  LOWEST so the list is still ours to replace.
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onDeath(PlayerDeathEvent e) {
		Player p = e.getEntity();
		ItemStack[] snap = snapshots.get(p.getUniqueId());
		if(snap == null) return;
		finish(p, holderOf(p), true);
		e.getDrops().clear();
		for(ItemStack it : snap) if(it != null && !it.getType().isAir()) e.getDrops().add(it);
	}

	/** Shut every open editor down, saving as we go. Called from {@code M7tas.onDisable}, before players are dropped. */
	public void restoreAll() {
		for(UUID id : new ArrayList<>(snapshots.keySet())) {
			Player p = Bukkit.getPlayer(id);
			if(p == null) {
				snapshots.remove(id); // offline with a snapshot left: nothing to restore it into
				continue;
			}
			finish(p, holderOf(p), true);
			p.closeInventory();
		}
	}

	/**
	 * The ONE way an editor ends: save the 41 slots, hand the player their own inventory back, drop the cursor.
	 * Idempotent - the snapshot is removed first, so the second caller does nothing.
	 *
	 * @param holder the open editor, or null when the caller only has the player (quit / death / disable); the
	 *               armour row is then read off whatever chest is still on top, and skipped if there isn't one.
	 */
	private void finish(Player p, EditorHolder holder, boolean save) {
		ItemStack[] snap = snapshots.remove(p.getUniqueId());
		if(snap == null) return;
		// The cleanup runs in a finally: the spectator bypass rewrites EVERY container click from this player as
		// non-spectator, so if the save throws it must still come off - otherwise it stays armed on the connection
		// for the rest of the session. Same for the inventory, which would otherwise keep the palette copies.
		try {
			if(save && holder != null) {
				ItemStack[] arr = new ItemStack[41];
				for(int i = 0; i < MAIN_SLOTS; i++) {
					ItemStack it = p.getInventory().getItem(i);
					arr[i] = it == null ? null : it.clone(); // a live mirror, and we are about to overwrite the slot
				}
				for(int g = HELMET_SLOT; g <= GEAR_END; g++) {
					ItemStack it = holder.inv.getItem(g);
					arr[arrIndex(g)] = isPlaceholder(it, g) ? null : it;
				}
				arr[40] = null; // no off-hand in M7, and a kit saved before that rule must not keep one
				// The reserved slot always holds the class default's menu, whatever the player left there.
				ItemStack menu = Catalog.defaultFor(holder.role)[MENU_ARR_INDEX];
				if(menu != null) arr[MENU_ARR_INDEX] = menu;
				Loadouts.setContents(p.getUniqueId(), holder.role, arr);
				p.sendMessage(Utils.msg("<green>Saved your <white><role></white> loadout",
						Placeholder.unparsed("role", holder.role)));
			}
		} finally {
			writeMain(p, snap);
			SpectatorGuiAccess.uninstall(p);
			p.setItemOnCursor(null); // don't let a held palette copy leak into the player's inventory
			p.updateInventory();
		}
	}

	/** The editor this player has open, or null. */
	private static EditorHolder holderOf(Player p) {
		return p.getOpenInventory().getTopInventory().getHolder() instanceof EditorHolder h ? h : null;
	}

	// ===== the player's inventory as the loadout's 36 slots =====
	/** A deep copy of the player's 36 inventory slots. The ONLY copy of their real items while an editor is open. */
	private static ItemStack[] takeInventory(Player p) {
		ItemStack[] out = new ItemStack[MAIN_SLOTS];
		for(int i = 0; i < MAIN_SLOTS; i++) {
			ItemStack it = p.getInventory().getItem(i);
			out[i] = it == null ? null : it.clone();
		}
		return out;
	}

	/** Write the first 36 entries of a 41-slot array (or a bare 36-slot snapshot) into the player's inventory. */
	private static void writeMain(Player p, ItemStack[] arr) {
		for(int i = 0; i < MAIN_SLOTS; i++) p.getInventory().setItem(i, arr != null && i < arr.length ? arr[i] : null);
		p.updateInventory();
	}

	/**
	 * Put the class default's SkyBlock Menu in the reserved hotbar slot, so a loadout saved before the slot was
	 * reserved (or one whose menu was trashed) gets it back.
	 */
	private static void pinMenu(Player p, String role, ItemStack[] arr) {
		ItemStack menu = Catalog.defaultFor(role)[MENU_ARR_INDEX];
		p.getInventory().setItem(MENU_ARR_INDEX, menu != null ? menu : arr[MENU_ARR_INDEX]);
		p.updateInventory();
	}

	/** Shift-click from the palette: a copy into the first free inventory slot, never over the pinned menu. */
	private static void giveToLoadout(Player p, ItemStack copy) {
		for(int i = 0; i < MAIN_SLOTS; i++) {
			if(i == MENU_ARR_INDEX) continue;
			ItemStack at = p.getInventory().getItem(i);
			if(at == null || at.getType().isAir()) {
				p.getInventory().setItem(i, copy);
				p.updateInventory();
				return;
			}
		}
	}

	// ===== helpers =====
	/** The white pane that stands in for an empty armour / off-hand slot, named for what belongs there. */
	private static ItemStack placeholder(int gui) {
		return button(Material.WHITE_STAINED_GLASS_PANE, "<gray>" + slotName(gui));
	}

	private static ItemStack orPlaceholder(ItemStack it, int gui) {
		return it == null || it.getType().isAir() ? placeholder(gui) : it;
	}

	/**
	 * Is this the placeholder pane for that slot rather than a real item?  Built the same way every time, so an
	 * exact match is the test - and a player cannot forge one, because the palette holds no white pane.
	 */
	private static boolean isPlaceholder(ItemStack it, int gui) {
		return it != null && it.isSimilar(placeholder(gui));
	}

	private static String slotName(int gui) {
		return switch(gui) {
			case HELMET_SLOT -> "Helmet";
			case CHEST_SLOT -> "Chestplate";
			case LEGS_SLOT -> "Leggings";
			case BOOTS_SLOT -> "Boots";
			default -> " ";
		};
	}

	/**
	 * Row 5: the seam between the palette and the loadout.  It exists because the two halves of this window look
	 * identical and mean opposite things - above is a catalogue you copy FROM, below is the kit you are building -
	 * and a labelled bar between them is cheaper than explaining it in chat every time.
	 * <p>
	 * <b>Only the first line can be the NAME.</b>  An item display name is a single tooltip line and a newline in
	 * it does nothing, so the rule and the second label are lore - in the name's own colour, so the three still
	 * read as one block.
	 */
	private static void drawDivider(Inventory gui) {
		for(int g = DIVIDER_START; g <= DIVIDER_END; g++) gui.setItem(g, divider());
	}

	private static ItemStack divider() {
		return button(Material.GRAY_STAINED_GLASS_PANE, "<gray>▲ Item Palette", List.of("<gray>--------------------------", "<gray>▼ Your Armor & Inventory"));
	}

	/**
	 * The starting-pet button: the pet itself, with its whole tooltip, and the action line rewritten.
	 * <p>
	 * <b>{@code PetType.icon} builds it</b>, as everywhere else that draws a pet - a second copy of the head
	 * assembly is what makes saved stacks stop matching.  Only the trailing action line, the one the summoning
	 * menu wants, is swapped for what a click HERE does.
	 * <p>
	 * <b>There is always a pet to show.</b>  {@code Pets.startingPet} never returns null: no file, an unreadable
	 * file and a file naming a deleted pet all come back as {@code Pets.DEFAULT_PET}, the Golden Dragon.
	 */
	private static ItemStack petButton(Player p) {
		pets.PetType pet = pets.Pets.startingPet(p);
		ItemStack it = pet.icon(true, false);
		ItemMeta m = it.getItemMeta();
		if(m == null) return it;
		List<Component> lore = m.lore();
		List<Component> out = lore == null ? new ArrayList<>() : new ArrayList<>(lore);
		if(!out.isEmpty()) out.removeLast(); // the "CURRENTLY SUMMONED" line icon() wrote, which is /pets'
		out.add(Utils.msg("<gray>The pet every run of yours begins with.").decoration(TextDecoration.ITALIC, false));
		out.add(Utils.msg("<dark_gray>Applies in Realistic mode.").decoration(TextDecoration.ITALIC, false));
		out.add(Component.empty());
		out.add(Utils.msg("<yellow>Click to change <dark_gray>(right-click to go back)")
				.decoration(TextDecoration.ITALIC, false));
		m.lore(out);
		it.setItemMeta(m);
		return it;
	}

	/** Step the starting pet one place through {@code PetType}, wrapping.  Writes the PREFERENCE, not the pet out. */
	private static void cyclePet(Player p, boolean back) {
		pets.PetType[] all = pets.PetType.values();
		if(all.length == 0) return;
		int at = 0;
		pets.PetType now = pets.Pets.startingPet(p);
		for(int i = 0; i < all.length; i++) if(all[i] == now) at = i;
		pets.Pets.setStartingPet(p, all[Math.floorMod(at + (back ? -1 : 1), all.length)]);
	}

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

	/**
	 * A button with lore.  Only the seam uses it, but it lives beside {@link #button(Material, String)} so both
	 * styles of item in this window are built the same way.
	 */
	private static ItemStack button(Material mat, String name, List<String> lore) {
		ItemStack it = button(mat, name);
		ItemMeta m = it.getItemMeta();
		if(m != null) {
			List<Component> rendered = new ArrayList<>();
			for(String line : lore) rendered.add(Utils.msg(line).decoration(TextDecoration.ITALIC, false));
			m.lore(rendered);
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
