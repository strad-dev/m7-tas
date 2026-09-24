package pets;

import damage.Difficulty;
import items.ItemFactory;
import loadout.SpectatorGuiAccess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import plugin.M7tas;
import plugin.Menus;
import plugin.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code /pets} and {@code /petloadout} - one 54-slot window in two modes.
 * <p>
 * {@code /pets} is the summoning menu: click a head and that pet comes out.  {@code /petloadout} is the same
 * window in ARRANGING mode: pick a pet up and drop it in another slot, swapping with whatever is there.  One
 * class rather than two because the layout, the furniture and the containment rules are identical and a second
 * copy of them would drift.
 *
 * <h2>Nothing here is an inventory</h2>
 * <b>Every click in the view is cancelled, in both inventories, before anything else happens</b> - the same rule
 * (and the same reason) as {@code goldor/GoldorTerminalGui}: these are click targets, so nothing may be picked
 * up, moved, dropped, shift-clicked in from the player's own inventory, number-keyed out or dragged.  A click
 * that means something is cancelled too and then handled by hand.
 *
 * <h2>A pet may never leave the menu</h2>
 * Arranging mode is the only thing here that ever puts a stack on the cursor, and that stack is a freshly built
 * COPY of a pet head: one that reached the world would be a free item spawn, and one still on the cursor when
 * the view closes is the same thing a tick later.  So {@link #finish} is the ONE way an arranging session ends -
 * put the carried pet back, save the layout, clear the cursor - reached from the close, the quit, the death and
 * the plugin-disable paths alike, and idempotent because it removes the session first.  That is
 * {@code loadout/LoadoutEditor.finish}'s structure, for the reason its own comment gives: missing one of those
 * four paths is how items disappear.
 *
 * <h2>Realistic only</h2>
 * Both commands refuse outright in any other mode and say why.  In classic and Perfect RNG the pet is ASSUMED
 * from what the player is doing and wearing ({@code damage/Pet.forPlayer}), so a menu there would be offering a
 * choice nothing reads.
 */
public final class PetMenu implements CommandExecutor, Listener {

	private static final int SIZE = 54;

	/** The black panes.  This list is the ONLY place the window's shape is written down. */
	private static final int[] FILLER_SLOTS = {0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 47, 48, 50, 51, 53};

	/** The four buttons. */
	private static final int HEADER_SLOT = 4, AUTOPET_SLOT = 46, CLOSE_SLOT = 49, RESET_SLOT = 52;

	/** Hypixel's Autopet head texture, taken off the real item.  See {@link #autopetButton()}. */
	private static final String AUTOPET_TEXTURE =
			"eyJ0aW1lc3RhbXAiOjE1MTgyODU5Njg3NjMsInByb2ZpbGVJZCI6ImIwZDczMmZlMDBmNzQwN2U5ZTdmNzQ2MzAxY2Q5OGNhIiwicHJvZmlsZU5hbWUiOiJPUHBscyIsInNpZ25hdHVyZVJlcXVpcmVkIjp0cnVlLCJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzMxNmE4Yjk2Y2I5YWVlYTUxNDBkZDkzNmM2ZWJiOTY0ZWZjYmZkNzFhZTIxN2Q5ZjcwODg4ZWZiMTRlN2MwIn19fQ==";

	/**
	 * Every slot a pet may sit in: whatever is left once the panes and the four buttons are taken out.
	 * <p>
	 * <b>Derived, never listed twice.</b>  It comes out as 10-16, 19-25, 28-34 and 37-43, which is 28 slots, and
	 * that is worth checking against if the shape above ever moves - but writing those four runs out as a second
	 * constant is how a pane and a pet slot end up claiming the same square.
	 */
	private static final List<Integer> PET_SLOTS = derivePetSlots();

	private static List<Integer> derivePetSlots() {
		boolean[] taken = new boolean[SIZE];
		for(int slot : FILLER_SLOTS) taken[slot] = true;
		taken[HEADER_SLOT] = true;
		taken[AUTOPET_SLOT] = true;
		taken[CLOSE_SLOT] = true;
		taken[RESET_SLOT] = true;
		List<Integer> out = new ArrayList<>();
		for(int slot = 0; slot < SIZE; slot++) if(!taken[slot]) out.add(slot);
		return List.copyOf(out);
	}

	/** The pet slots in ascending order.  {@code Pets} reads this for its default layout and its repair path. */
	public static List<Integer> petSlots() {
		return PET_SLOTS;
	}

	/**
	 * Live arranging sessions: player -> the layout they are editing.  Present == a session is live, which is
	 * what makes {@link #finish} idempotent.  The profile is only written when the session ends, so an abandoned
	 * arrange is still saved (the pets are all in the window either way) but a half-done swap is never written
	 * twice.
	 */
	private final Map<UUID, Map<Integer, PetType>> sessions = new HashMap<>();

	/** The pet on a player's cursor, per live session.  Absent = the cursor is empty. */
	private final Map<UUID, PetType> carried = new HashMap<>();

	/**
	 * The settings window behind slot 46.  Owned here rather than built by the bootstrap so the two can point at
	 * each other (its Back button reopens this one) without either needing a static handle to the other; it still
	 * has to be REGISTERED as a listener, which is what {@link #autopetMenu()} is for.
	 */
	private final AutopetMenu autopet = new AutopetMenu(this);

	/** The settings window, for {@code M7tas.onEnable} to register as a listener. */
	public AutopetMenu autopetMenu() {
		return autopet;
	}

	// ==================== commands ====================

	@Override
	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
		if(!(sender instanceof Player p)) {
			sender.sendMessage(Utils.msg("<red>Only players have pets"));
			return true;
		}
		if(!Difficulty.manualPets()) {
			// Plainly, and naming both the mode they are in and the one this needs: the menu opening onto an
			// assumed pet would be worse than not opening at all.
			sender.sendMessage(Utils.msg("<red>Pets are yours to pick only in <white>Realistic</white> mode."));
			sender.sendMessage(Utils.msg("<gray>The mode is <white>" + Difficulty.current().displayName()
					+ "</white>, where your pet follows from what you are doing."));
			return true;
		}
		open(p, command.getName().equalsIgnoreCase("petloadout"));
		return true;
	}

	/** Package-private so {@link AutopetMenu}'s Back button can reopen the summoning half. */
	void open(Player p, boolean arranging) {
		Holder holder = new Holder(arranging);
		Inventory gui = Bukkit.createInventory(holder, SIZE,
				Utils.msg(arranging ? "<dark_gray>Pets: Arrange" : "<dark_gray>Pets"));
		holder.inv = gui;
		if(arranging) sessions.put(p.getUniqueId(), new LinkedHashMap<>(Pets.layout(p)));
		draw(p, holder);
		p.openInventory(gui);
		// Idle players on m7 sit in spectator, and vanilla refuses them container clicks - arm the bypass so they
		// can use the menu without leaving spectator mode.  No-op for anyone not in spectator.
		SpectatorGuiAccess.install(p);
	}

	/** (Re)draw the whole window from the layout this mode is showing. */
	private void draw(Player p, Holder holder) {
		Inventory gui = holder.inv;
		for(int slot : FILLER_SLOTS) gui.setItem(slot, filler());

		PetType out = Pets.equipped(p);
		gui.setItem(HEADER_SLOT, button(Material.BONE, "<green>Pets",
				List.of("<gray>Currently summoned:", out.colouredName())));
		gui.setItem(AUTOPET_SLOT, autopetButton());
		gui.setItem(CLOSE_SLOT, button(Material.BARRIER, "<red>Close", List.of()));
		gui.setItem(RESET_SLOT, button(Material.LAVA_BUCKET, "<yellow>Reset to Default",
				List.of("<gray>Put your pets back on the first", "<gray>four slots of the menu.")));

		Map<Integer, PetType> layout = layoutFor(p, holder);
		for(int slot : PET_SLOTS) {
			PetType pet = layout.get(slot);
			gui.setItem(slot, pet == null ? null : pet.icon(pet == out, holder.arranging));
		}
	}

	/** Arranging edits a working copy; summoning reads the saved one straight off the profile. */
	private Map<Integer, PetType> layoutFor(Player p, Holder holder) {
		if(!holder.arranging) return Pets.layout(p);
		return sessions.getOrDefault(p.getUniqueId(), Pets.layout(p));
	}

	// ==================== clicks ====================

	@EventHandler
	public void onClick(InventoryClickEvent e) {
		if(!(e.getView().getTopInventory().getHolder() instanceof Holder holder)) return;
		if(Menus.ignoreDoubleClick(e)) return;
		e.setCancelled(true); // every slot, both inventories, before anything is read
		if(!(e.getWhoClicked() instanceof Player p)) return;

		if(e.getClickedInventory() != e.getView().getTopInventory()) {
			// A click down in the player's own inventory. Cancelled above, and while a pet is on the cursor it is
			// also an attempt to put the pet somewhere it may never go, so say so rather than doing nothing.
			if(carried.containsKey(p.getUniqueId())) refuse(p);
			return;
		}

		int slot = e.getRawSlot();
		boolean holding = carried.containsKey(p.getUniqueId());
		if(PET_SLOTS.contains(slot)) {
			if(holder.arranging) arrangeClick(p, holder, slot);
			else summonClick(p, holder, slot);
			return;
		}
		// Furniture. Carrying a pet, every one of them is a place onto something that is not a pet slot - refused,
		// including the buttons, so a pet can never be lost behind an action that swaps the window out.  Escape
		// still closes, and finish() puts the carried pet back.
		if(holding) {
			refuse(p);
			return;
		}
		switch(slot) {
			case CLOSE_SLOT -> {
				// Deferred a tick: closing a view from inside its own click event is the one thing Bukkit asks you
				// not to do (the same deferral GoldorListener uses on a solved terminal).
				Bukkit.getScheduler().runTask(M7tas.getInstance(), () -> p.closeInventory());
			}
			case AUTOPET_SLOT -> {
				// Also deferred, and that ordering is load-bearing in arranging mode: the close event for this
				// window fires first, so finish() has saved the layout before the settings menu opens.
				Bukkit.getScheduler().runTask(M7tas.getInstance(), () -> autopet.open(p));
			}
			case RESET_SLOT -> {
				Pets.resetLayout(p);
				if(holder.arranging) sessions.put(p.getUniqueId(), new LinkedHashMap<>(Pets.layout(p)));
				draw(p, holder);
				p.sendMessage(Utils.msg("<yellow>Put your pets back on the default slots"));
			}
			default -> { } // the header and the panes do nothing
		}
	}

	/**
	 * {@code /pets}: a click on a head summons it, and <b>the window closes</b>.
	 * <p>
	 * Summoning is the whole job of this mode, so there is nothing left to do once it has happened - leaving the
	 * window open only made a second click the way out.  The redraw still runs: it costs nothing, and it means
	 * the glint is on the right head for the tick the window has left.  <b>Deferred a tick</b>, like every other
	 * close here, because closing a view from inside its own click event is the one thing Bukkit asks you not to
	 * do.
	 * <p>
	 * A click that changes nothing - an empty slot, or the pet already out - leaves the window alone.  It is not
	 * an action, so it should not read as one.
	 */
	private void summonClick(Player p, Holder holder, int slot) {
		PetType pet = Pets.layout(p).get(slot);
		if(pet == null) return;
		if(Pets.equip(p, pet, "<green>You summoned your ")) { // the pet name closes the line
			draw(p, holder);
			Bukkit.getScheduler().runTask(M7tas.getInstance(), () -> p.closeInventory());
		}
	}

	/**
	 * {@code /petloadout}: a click on a pet slot picks a pet up, puts one down, or swaps the two.
	 * <p>
	 * The cursor is the real one, so the window reads exactly like moving an item - which is also why the carried
	 * pet is tracked here as well: the ItemStack is only a picture, and {@link #finish} needs to know which pet
	 * to put back without parsing it out of a head.
	 */
	private void arrangeClick(Player p, Holder holder, int slot) {
		Map<Integer, PetType> layout = sessions.get(p.getUniqueId());
		if(layout == null) return; // no live session: a stale view, so leave it inert
		UUID id = p.getUniqueId();
		PetType held = carried.get(id);
		if(held == null) {
			PetType pet = layout.remove(slot);
			if(pet == null) return; // an empty slot with an empty cursor
			carried.put(id, pet);
			p.setItemOnCursor(pet.icon(pet == Pets.equipped(p), true));
		} else {
			PetType under = layout.put(slot, held);
			if(under == null) {
				carried.remove(id);
				p.setItemOnCursor(null);
			} else {
				carried.put(id, under);
				p.setItemOnCursor(under.icon(under == Pets.equipped(p), true));
			}
		}
		draw(p, holder);
		p.updateInventory();
	}

	private static void refuse(Player p) {
		p.sendMessage(Utils.msg("<red>A pet can only go in another pet slot"));
	}

	/** Dragging is another way to move an item, so it is refused wholesale. */
	@EventHandler
	public void onDrag(InventoryDragEvent e) {
		if(e.getView().getTopInventory().getHolder() instanceof Holder) e.setCancelled(true);
	}

	/**
	 * Backstop for every drop path, whatever the click event classified it as: while this menu is open, nothing
	 * the player does puts an item on the ground.  Cheaper than keeping a click allowlist exhaustive forever, and
	 * the item at risk here is a pet head the world has no business holding.
	 */
	@EventHandler(ignoreCancelled = true)
	public void onDropItem(PlayerDropItemEvent e) {
		if(e.getPlayer().getOpenInventory().getTopInventory().getHolder() instanceof Holder) e.setCancelled(true);
	}

	// ==================== the four ends ====================

	@EventHandler
	public void onClose(InventoryCloseEvent e) {
		if(!(e.getView().getTopInventory().getHolder() instanceof Holder)) return;
		if(e.getPlayer() instanceof Player p) finish(p);
	}

	/**
	 * A quit with the menu still open.  Whichever of this and {@link #onClose} the server fires first ends the
	 * session; {@link #finish} is idempotent, so the other is a no-op.
	 */
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		finish(e.getPlayer());
		Pets.unload(e.getPlayer().getUniqueId());
	}

	/**
	 * Dying with the menu open.  The carried pet goes back into the layout like any other ending, and the head
	 * the server has already listed as a drop is struck off: it is a menu COPY, so dropping it would spawn a pet
	 * head into the world that no player ever owned.
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onDeath(PlayerDeathEvent e) {
		Player p = e.getEntity();
		if(!sessions.containsKey(p.getUniqueId())) return;
		finish(p);
		e.getDrops().removeIf(PetMenu::isPetIcon);
	}

	/**
	 * Load a joining player's pet profile, so the damage path never reads the disk, and put them on their starting
	 * pet if no run is live.  Lives here rather than in a listener of its own because this class is already the
	 * package's registered listener.
	 */
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Pets.preload(e.getPlayer().getUniqueId());
		// Arriving before the run is live (a party member landing mid-countdown, or idle) means starting it on
		// your starting pet.  Mid-run, the pet is whatever the run has made it.
		if(!instructions.Server.isRunStarted()) Pets.applyStartingPet(e.getPlayer());
	}

	/** Shut every open arranging session down, saving as we go.  Called from {@code M7tas.onDisable}. */
	public void restoreAll() {
		for(UUID id : new ArrayList<>(sessions.keySet())) {
			Player p = Bukkit.getPlayer(id);
			if(p == null) {
				sessions.remove(id); // offline with a session left: nothing to close it into
				carried.remove(id);
				continue;
			}
			finish(p);
			p.closeInventory();
		}
		Pets.clearCache();
	}

	/**
	 * The ONE way an arranging session ends: put the carried pet back, save the layout, clear the cursor.
	 * Idempotent - the session is removed first, so a second caller does nothing.
	 * <p>
	 * A no-op for {@code /pets}, which never opens a session because it never puts anything on the cursor.
	 */
	private void finish(Player p) {
		UUID id = p.getUniqueId();
		Map<Integer, PetType> layout = sessions.remove(id);
		PetType held = carried.remove(id);
		if(layout == null) return;
		// The cleanup runs in a finally for the same reason the loadout editor's does: the spectator bypass
		// rewrites EVERY container click from this player, so if the save throws it must still come off, and the
		// cursor must be cleared whatever happened or a pet head rides out of the menu with them.
		try {
			// A pet is never allowed to end up nowhere. There are 28 slots and five pets, so a free one exists.
			if(held != null) Pets.placeInFirstFree(layout, held);
			Pets.setLayout(p, layout);
		} finally {
			SpectatorGuiAccess.uninstall(p);
			p.setItemOnCursor(null);
			p.updateInventory();
		}
	}

	/**
	 * Is this stack one of the menu's pet heads?  Matched on material plus plain display name, the same shape as
	 * {@code Catalog.paletteKey}.  A false positive is not reachable: a pet head never leaves this window, so a
	 * player's own inventory cannot legitimately hold one.
	 */
	private static boolean isPetIcon(ItemStack it) {
		if(it == null || it.getType() != Material.PLAYER_HEAD || !it.hasItemMeta()) return false;
		ItemMeta meta = it.getItemMeta();
		if(meta == null) return false;
		String name = Utils.plain(meta.displayName());
		for(PetType t : PetType.values()) if(Utils.plain(Utils.mm(t.colouredName())).equals(name)) return true;
		return false;
	}

	// ==================== helpers ====================

	private static ItemStack filler() {
		return button(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
	}

	/**
	 * Hypixel's own Autopet head, rather than the book this used to be.
	 * <p>
	 * Built through {@code ItemFactory.head} like every other custom head here, then stripped of the Protection 5
	 * that builder adds for a WEARABLE head - the same two-step {@link PetType#icon} does and for the same reason:
	 * one copy of the profile/NBT assembly, per the CLAUDE.md warning, and no enchantment line on a menu button.
	 * <p>
	 * The real item carries a "Rules used: N/28" counter.  It is left off rather than faked: this menu has four
	 * triggers, not 28, and a counter over a different denominator would read as Hypixel's own and be wrong.
	 */
	private static ItemStack autopetButton() {
		ItemStack head = ItemFactory.head("<red>Autopet", "petsAutopet", AUTOPET_TEXTURE, null);
		for(Enchantment e : new ArrayList<>(head.getEnchantments().keySet())) head.removeEnchantment(e);
		ItemMeta m = head.getItemMeta();
		if(m != null) {
			m.displayName(Utils.msg("<red>Autopet").decoration(TextDecoration.ITALIC, false));
			List<Component> lore = new ArrayList<>();
			for(String line : List.of("<gray>Define custom <red>rules <gray>to automatically",
					"<gray>equip your pets.", "", "<yellow>Click to setup autopet!")) {
				lore.add(Utils.msg(line).decoration(TextDecoration.ITALIC, false));
			}
			m.lore(lore);
			head.setItemMeta(m);
		}
		return head;
	}

	static ItemStack button(Material mat, String name, List<String> lore) {
		ItemStack it = new ItemStack(mat);
		ItemMeta m = it.getItemMeta();
		if(m != null) {
			m.displayName(Utils.msg(name).decoration(TextDecoration.ITALIC, false));
			List<Component> rendered = new ArrayList<>(lore.size());
			for(String line : lore) rendered.add(Utils.msg(line).decoration(TextDecoration.ITALIC, false));
			m.lore(rendered);
			it.setItemMeta(m);
		}
		return it;
	}

	/** Marker holder carrying which of the two modes this window is. */
	public static final class Holder implements InventoryHolder {
		final boolean arranging;
		Inventory inv;

		Holder(boolean arranging) {
			this.arranging = arranging;
		}

		@Override
		public @NotNull Inventory getInventory() {
			return inv;
		}
	}
}
