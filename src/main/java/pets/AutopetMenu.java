package pets;

import loadout.SpectatorGuiAccess;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import plugin.M7tas;
import plugin.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * The Autopet Settings window behind slot 46 of {@link PetMenu}: one button per {@link Autopet.Trigger}, whose
 * lore lists what it fires on, what it summons and what it will not interrupt.
 * <p>
 * <b>Both clicks open a picker</b> ({@link PetPicker}): left click the one that chooses the pet the rule
 * summons, right click the one that toggles its exceptions.  This used to step the value on each click, the
 * shape {@code commands/SettingsMenu} uses for the three dungeon settings - which is fine for a setting with
 * three values and became six clicks to get back where you started once there were five pets plus "off", with
 * no way to see what the other values were.  A setting with a handful of values steps; a setting with a roster
 * behind it opens onto the roster.
 * <p>
 * <b>Rod Swap is the odd one out, as everywhere else.</b>  It holds an ordered cycle rather than a single pet,
 * so there is no "the pet this rule equips" to pick and nothing to except: both clicks open the cycle editor,
 * which is the same picker in its third mode.
 * <p>
 * Every click is cancelled, like the menu it hangs off: this is a click target, not an inventory.
 * <p>
 * <b>This class is already registered as a listener</b> ({@code M7tas.onEnable} does it via
 * {@code petMenu.autopetMenu()}) and it forwards the three inventory events on to the picker it owns, so
 * {@link PetPicker} needs no registration line of its own.
 */
public final class AutopetMenu implements Listener {

	private static final int SIZE = 27;
	/** One button per trigger, spaced out along the middle row. */
	private static final int[] TRIGGER_SLOTS = {10, 12, 14, 16};
	private static final int HEADER_SLOT = 4, BACK_SLOT = 22;

	/** The window this one hangs off, so Back is a return rather than a close. */
	private final PetMenu parent;

	/**
	 * The picker every rule click opens.  Owned here, like this menu is owned by {@link PetMenu}, so the two can
	 * point at each other without a static handle - and so it rides this class's listener registration.
	 */
	private final PetPicker picker = new PetPicker(this);

	AutopetMenu(PetMenu parent) {
		this.parent = parent;
	}

	void open(Player p) {
		Holder h = new Holder();
		Inventory gui = Bukkit.createInventory(h, SIZE, Utils.msg("<dark_gray>Autopet Settings"));
		h.inv = gui;
		draw(p, gui);
		p.openInventory(gui);
		SpectatorGuiAccess.install(p);
	}

	private void draw(Player p, Inventory gui) {
		for(int slot = 0; slot < SIZE; slot++) gui.setItem(slot, PetMenu.button(Material.BLACK_STAINED_GLASS_PANE, " ", List.of()));
		gui.setItem(HEADER_SLOT, PetMenu.button(Material.BOOK, "<red>Autopet Settings",
				List.of("<gray>Summon a pet for you when", "<gray>something happens in the run.")));
		gui.setItem(BACK_SLOT, PetMenu.button(Material.ARROW, "<yellow>Back to Pets", List.of()));

		Autopet.Trigger[] triggers = Autopet.Trigger.values();
		for(int i = 0; i < triggers.length && i < TRIGGER_SLOTS.length; i++) {
			Autopet.Trigger t = triggers[i];
			gui.setItem(TRIGGER_SLOTS[i], PetMenu.button(t.icon(), "<gold>" + t.displayName(), lore(p, t)));
		}
	}

	private List<String> lore(Player p, Autopet.Trigger t) {
		List<String> out = new ArrayList<>();
		out.add("");
		if(t.isCycle()) {
			out.add("<gray>Cycle:");
			out.addAll(PetPicker.cycleLines(Pets.rodCycle(p)));
			out.add("");
			out.add("<yellow>Click to edit the cycle");
			return out;
		}
		Autopet.Rule rule = Pets.rule(p, t);
		PetType pet = rule == null ? null : rule.pet();
		// Order is settled by Autopet.Rule's constructor, so this prints the same sequence as the picker.
		List<PetType> except = rule == null ? List.of() : rule.exceptions();
		out.add("<gray>Summons: " + (pet == null ? "<dark_gray>Off" : pet.colouredName()));
		if(except.isEmpty()) {
			out.add("<gray>Unless out: <dark_gray>Nothing");
		} else {
			out.add("<gray>Unless out:");
			for(PetType e : except) out.add("<gray>  " + e.colouredName());
		}
		out.add("");
		out.add("<yellow>Left click to pick the pet");
		out.add("<yellow>Right click to pick the exceptions");
		return out;
	}

	@EventHandler
	public void onClick(InventoryClickEvent e) {
		InventoryHolder top = e.getView().getTopInventory().getHolder();
		// The picker rides this registration rather than having one of its own; it cancels the click itself, on
		// the same "before anything is read" rule as below.
		if(top instanceof PetPicker.Holder ph) {
			picker.onClick(e, ph);
			return;
		}
		if(!(top instanceof Holder)) return;
		e.setCancelled(true); // a click target, not an inventory - see PetMenu
		if(!(e.getWhoClicked() instanceof Player p)) return;
		if(e.getClickedInventory() != e.getView().getTopInventory()) return;
		// No mode check here, deliberately.  The window can only be REACHED in realistic mode, and a window left
		// open across a mode change is still editing a preference that is the player's either way - a gate here
		// would only make the buttons silently stop working under them.

		int slot = e.getRawSlot();
		if(slot == BACK_SLOT) {
			// Deferred a tick, like every other view swap here: the close for this window has to land first.
			Bukkit.getScheduler().runTask(M7tas.getInstance(), () -> parent.open(p, false));
			return;
		}
		for(int i = 0; i < TRIGGER_SLOTS.length && i < Autopet.Trigger.values().length; i++) {
			if(TRIGGER_SLOTS[i] != slot) continue;
			Autopet.Trigger t = Autopet.Trigger.values()[i];
			// Rod Swap has no single pet and nothing to except, so BOTH clicks land on its cycle editor.
			PetPicker.Mode mode = t.isCycle() ? PetPicker.Mode.CYCLE
					: e.isRightClick() ? PetPicker.Mode.EXCEPTIONS : PetPicker.Mode.PET;
			// Deferred a tick, like every other view swap here: the close for this window has to land first.
			Bukkit.getScheduler().runTask(M7tas.getInstance(), () -> picker.open(p, t, mode));
			return;
		}
	}

	@EventHandler
	public void onDrag(InventoryDragEvent e) {
		InventoryHolder top = e.getView().getTopInventory().getHolder();
		if(top instanceof PetPicker.Holder) picker.onDrag(e);
		else if(top instanceof Holder) e.setCancelled(true);
	}

	/**
	 * Nothing is ever on the cursor in this window, so there is no session to end - but the spectator bypass was
	 * armed on open and has to come back off, or it stays on the connection for the rest of the session.
	 */
	@EventHandler
	public void onClose(InventoryCloseEvent e) {
		InventoryHolder top = e.getView().getTopInventory().getHolder();
		if(top instanceof PetPicker.Holder) {
			picker.onClose(e); // it uninstalls too, and reopens this window unless it is the one swapping it out
			return;
		}
		if(!(top instanceof Holder)) return;
		if(e.getPlayer() instanceof Player p) SpectatorGuiAccess.uninstall(p);
	}

	/** Marker holder; the window has no state of its own, since every value is read off the player's profile. */
	public static final class Holder implements InventoryHolder {
		Inventory inv;

		@Override
		public @NotNull Inventory getInventory() {
			return inv;
		}
	}
}
