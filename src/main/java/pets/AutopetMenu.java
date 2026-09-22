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
 * <b>Left click changes the pet, right click changes the exception</b>, both stepping through the same list -
 * every pet, then "off" / "no exception", then round again.  That is the shape {@code commands/SettingsMenu}
 * already uses for the dungeon settings: one button per setting, every value reachable by clicking, and the one
 * in force written into the lore rather than hidden in a title.
 * <p>
 * <b>Rod Swap is the exception to the exception.</b>  It holds an ordered cycle rather than a single pet, so
 * there is no "the pet this rule equips" to except: left click appends the next pet not yet in the cycle (and
 * clears it once every pet is in), right click drops the last one.  The button's own lore says so, since it is
 * the one place in this window where the two clicks do not mean what they mean everywhere else.
 * <p>
 * Every click is cancelled, like the menu it hangs off: this is a click target, not an inventory.
 */
public final class AutopetMenu implements Listener {

	private static final int SIZE = 27;
	/** One button per trigger, spaced out along the middle row. */
	private static final int[] TRIGGER_SLOTS = {10, 12, 14, 16};
	private static final int HEADER_SLOT = 4, BACK_SLOT = 22;

	/** The window this one hangs off, so Back is a return rather than a close. */
	private final PetMenu parent;

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
		for(String line : t.description()) out.add("<gray>" + line);
		out.add("");
		if(t.isCycle()) {
			List<PetType> cycle = Pets.rodCycle(p);
			out.add("<gray>Cycle:");
			if(cycle.isEmpty()) {
				out.add("<dark_gray>  empty (off)");
			} else {
				for(int i = 0; i < cycle.size(); i++) out.add("<gray>  " + (i + 1) + ". " + cycle.get(i).colouredName());
			}
			out.add("");
			out.add("<yellow>Left click to add the next pet");
			out.add("<yellow>Right click to drop the last one");
			return out;
		}
		Autopet.Rule rule = Pets.rule(p, t);
		PetType pet = rule == null ? null : rule.pet();
		PetType except = rule == null ? null : rule.exception();
		out.add("<gray>Summons: " + (pet == null ? "<dark_gray>Off" : pet.colouredName()));
		out.add("<gray>Unless out: " + (except == null ? "<dark_gray>Nothing" : except.colouredName()));
		out.add("");
		out.add("<yellow>Left click to change the pet");
		out.add("<yellow>Right click to change the exception");
		return out;
	}

	@EventHandler
	public void onClick(InventoryClickEvent e) {
		if(!(e.getView().getTopInventory().getHolder() instanceof Holder holder)) return;
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
			if(t.isCycle()) stepCycle(p, e.isRightClick());
			else stepRule(p, t, e.isRightClick());
			draw(p, holder.inv);
			return;
		}
	}

	/** Step one half of a normal rule to the next value, wrapping through "unset". */
	private static void stepRule(Player p, Autopet.Trigger t, boolean exception) {
		Autopet.Rule rule = Pets.rule(p, t);
		PetType pet = rule == null ? null : rule.pet();
		PetType except = rule == null ? null : rule.exception();
		if(exception) except = next(except);
		else pet = next(pet);
		Pets.setRule(p, t, new Autopet.Rule(pet, except));
	}

	/**
	 * The next pet after {@code current} in declaration order, with null on both ends of the list.
	 * <p>
	 * Null is "off" for a pet and "no exception" for an exception, and it has to be IN the cycle rather than
	 * only the starting value, or a rule could never be turned back off once it was set.
	 */
	private static PetType next(PetType current) {
		PetType[] all = PetType.values();
		if(current == null) return all[0];
		int at = current.ordinal() + 1;
		return at >= all.length ? null : all[at];
	}

	/**
	 * Rod Swap's two clicks: append the next pet not already in the cycle, or drop the last one.
	 * <p>
	 * Appending wraps to CLEARING once every pet is in, for the same reason null sits in {@link #next}'s list:
	 * without it there is no click that turns the rule off again.
	 */
	private static void stepCycle(Player p, boolean remove) {
		List<PetType> cycle = new ArrayList<>(Pets.rodCycle(p));
		if(remove) {
			if(!cycle.isEmpty()) cycle.removeLast();
		} else {
			PetType add = null;
			for(PetType t : PetType.values()) {
				if(!cycle.contains(t)) {
					add = t;
					break;
				}
			}
			if(add == null) cycle.clear();
			else cycle.add(add);
		}
		Pets.setRodCycle(p, cycle);
	}

	@EventHandler
	public void onDrag(InventoryDragEvent e) {
		if(e.getView().getTopInventory().getHolder() instanceof Holder) e.setCancelled(true);
	}

	/**
	 * Nothing is ever on the cursor in this window, so there is no session to end - but the spectator bypass was
	 * armed on open and has to come back off, or it stays on the connection for the rest of the session.
	 */
	@EventHandler
	public void onClose(InventoryCloseEvent e) {
		if(!(e.getView().getTopInventory().getHolder() instanceof Holder)) return;
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
