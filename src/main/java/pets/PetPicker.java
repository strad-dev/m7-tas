package pets;

import loadout.SpectatorGuiAccess;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import plugin.M7tas;
import plugin.Menus;
import plugin.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The window a rule in {@link AutopetMenu} opens onto: a wall of pet heads you click.
 * <p>
 * It replaced a click-to-step button, which with five pets plus "off" took six clicks to get back where you
 * started and gave no way to see what the other values even were.  <b>One class in three {@link Mode}s</b> rather
 * than three near-identical menus, because the furniture, the containment rules, the spectator bypass and the
 * return-to-the-menu ordering are the same in all three and a second copy of them would drift:
 * <ul>
 *   <li>{@link Mode#PET} - single select, "which pet does this rule summon".  It carries an explicit <b>Off</b>
 *       button, since a rule with no pet fires nothing and there has to be a way back to that;</li>
 *   <li>{@link Mode#EXCEPTIONS} - multi select, toggling pets in and out of the rule's exception list;</li>
 *   <li>{@link Mode#CYCLE} - Rod Swap's ordered rotation: a click APPENDS, so any pet may appear at any
 *       position and as often as the player likes.</li>
 * </ul>
 *
 * <h2>Nothing here is an inventory</h2>
 * <b>Every click in the view is cancelled, in both inventories, before anything else is read</b> - the same rule
 * and the same reason as {@link PetMenu} and {@code goldor/GoldorTerminalGui}.  These are click targets: nothing
 * may be picked up, dragged, shift-clicked in from the player's own inventory or number-keyed out.  Nothing ever
 * reaches the cursor either, so unlike the arranging half of {@link PetMenu} there is no session to unwind.
 *
 * <h2>Not a listener of its own</h2>
 * {@link AutopetMenu} owns the one instance and forwards the three inventory events to it, so this class needs no
 * line in {@code M7tas.onEnable} - the menu it hangs off is already registered there
 * ({@code petMenu.autopetMenu()}).  One registration for the whole {@code /pets} tree is also what keeps the
 * open/close ordering below honest: the same handler sees both windows' closes.
 *
 * <h2>The close always goes back</h2>
 * Escape out of a picker and you land in the autopet menu, not in nothing: a half-made choice is not a reason to
 * throw the player out of the settings they were editing.  The reopen is deferred a tick, for the ordering reason
 * {@link PetMenu} already documents - the close for the old view fires before the new one can open, so opening
 * from inside a close (or a click) puts the two in the wrong order.  {@link #swapping} is how the deliberate
 * swaps avoid reopening the menu twice.
 */
public final class PetPicker {

	/** What a click in this window means. */
	public enum Mode {
		/** Pick the one pet a rule summons.  Choosing closes the picker. */
		PET,
		/** Toggle pets in and out of a rule's exception list.  Stays open. */
		EXCEPTIONS,
		/** Append to Rod Swap's ordered cycle.  Stays open. */
		CYCLE
	}

	private static final int SIZE = 27;
	private static final int HEADER_SLOT = 4;
	/** The middle row, which the pet heads are centred along.  Nine squares for five pets today. */
	private static final int ROW_START = 9, ROW_WIDTH = 9;
	/** The bottom row's three buttons.  What the two side ones do is per mode; the middle one always goes back. */
	private static final int LEFT_SLOT = 20, BACK_SLOT = 22, RIGHT_SLOT = 24;

	/** The window this one hangs off, so every exit is a return to it. */
	private final AutopetMenu parent;

	/**
	 * Players whose picker is being swapped out by the menu itself (a Back, or a pet chosen in {@link Mode#PET}).
	 * The reopen is already scheduled for them, so {@link #onClose} must not schedule a second one and make the
	 * autopet menu open, close and open again a tick later.
	 */
	private final Set<UUID> swapping = new HashSet<>();

	PetPicker(AutopetMenu parent) {
		this.parent = parent;
	}

	void open(Player p, Autopet.Trigger t, Mode mode) {
		Holder h = new Holder(t, mode);
		Inventory gui = Bukkit.createInventory(h, SIZE, Utils.msg("<dark_gray>" + title(t, mode)));
		h.inv = gui;
		draw(p, h);
		p.openInventory(gui);
		// Idle players on m7 sit in spectator and vanilla refuses them container clicks; armed after the open and
		// taken off again in onClose, exactly as PetMenu and AutopetMenu do it.
		SpectatorGuiAccess.install(p);
	}

	private static String title(Autopet.Trigger t, Mode mode) {
		return switch(mode) {
			case PET -> "Summon: " + t.displayName();
			case EXCEPTIONS -> "Except: " + t.displayName();
			case CYCLE -> "Rod Swap Cycle";
		};
	}

	// ==================== drawing ====================

	private void draw(Player p, Holder h) {
		Inventory gui = h.inv;
		for(int slot = 0; slot < SIZE; slot++) gui.setItem(slot, PetMenu.button(Material.BLACK_STAINED_GLASS_PANE, " ", List.of()));
		gui.setItem(HEADER_SLOT, header(p, h));

		PetType[] pets = PetType.values();
		for(int i = 0; i < pets.length && i < ROW_WIDTH; i++) gui.setItem(slotOf(i), entry(p, h, pets[i]));

		switch(h.mode) {
			case PET -> {
				gui.setItem(LEFT_SLOT, PetMenu.button(Material.BARRIER, "<red>Off",
						List.of("<gray>Fire nothing on this trigger.", "", "<yellow>Click to turn the rule off")));
				gui.setItem(BACK_SLOT, PetMenu.button(Material.ARROW, "<yellow>Back",
						List.of("<gray>Leave the rule as it is.")));
			}
			case EXCEPTIONS -> gui.setItem(BACK_SLOT, PetMenu.button(Material.ARROW, "<yellow>Done", List.of()));
			case CYCLE -> {
				gui.setItem(LEFT_SLOT, PetMenu.button(Material.SHEARS, "<yellow>Remove Last",
						List.of("<gray>Drop the last pet off the end", "<gray>of the cycle.")));
				gui.setItem(RIGHT_SLOT, PetMenu.button(Material.LAVA_BUCKET, "<red>Clear Cycle",
						List.of("<gray>Empty the cycle, which turns", "<gray>the Rod Swap rule off.")));
				gui.setItem(BACK_SLOT, PetMenu.button(Material.ARROW, "<yellow>Done", List.of()));
			}
		}
	}

	/** Where the i-th pet sits: the heads are centred in the middle row, so five of nine start at slot 11. */
	private static int slotOf(int i) {
		return ROW_START + Math.max(0, (ROW_WIDTH - Math.min(PetType.values().length, ROW_WIDTH)) / 2) + i;
	}

	/** The pet drawn on a slot, or null if that slot is not one of the heads. */
	private static PetType petAt(int slot) {
		PetType[] pets = PetType.values();
		for(int i = 0; i < pets.length && i < ROW_WIDTH; i++) if(slotOf(i) == slot) return pets[i];
		return null;
	}

	private ItemStack header(Player p, Holder h) {
		List<String> lore = new ArrayList<>();
		switch(h.mode) {
			case PET -> {
				PetType pet = rulePet(p, h.trigger);
				lore.add("<gray>Summons: " + (pet == null ? "<dark_gray>Off" : pet.colouredName()));
				lore.add("");
				lore.add("<gray>Pick the pet this trigger");
				lore.add("<gray>summons.  One only.");
			}
			case EXCEPTIONS -> {
				List<PetType> except = exceptions(p, h.trigger);
				lore.add("<gray>The rule will NOT fire while");
				lore.add("<gray>one of these is already out:");
				if(except.isEmpty()) lore.add("<dark_gray>  nothing");
				else for(PetType e : except) lore.add("<gray>  " + e.colouredName());
			}
			case CYCLE -> {
				lore.add("<gray>Each throw of your Pitchin' Rod");
				lore.add("<gray>steps one place along:");
				lore.addAll(cycleLines(Pets.rodCycle(p)));
			}
		}
		Material mat = h.mode == Mode.CYCLE ? Material.FISHING_ROD : h.trigger.icon();
		return PetMenu.button(mat, "<gold>" + title(h.trigger, h.mode), lore);
	}

	/**
	 * One pet's head, with the last lore line swapped for what a click here does.
	 * <p>
	 * <b>{@link PetType#icon} builds it.</b>  That is the one place the head texture and a pet's tooltip are
	 * assembled and it is not this class's to fork - a second copy of the NBT assembly is what CLAUDE.md warns
	 * makes saved stacks stop matching.  So the stack is built there and only its trailing action line, the
	 * "Click to summon!" the summoning menu wants, is rewritten.  The glint comes out right for free: {@code icon}
	 * sets the override from the same flag this passes as "selected".
	 */
	private ItemStack entry(Player p, Holder h, PetType pet) {
		List<Integer> at = positions(Pets.rodCycle(p), pet); // cycle mode only, but cheap and needed twice there
		// A switch EXPRESSION over the enum, so a fourth mode is a compile error here rather than a head that
		// silently never glints.
		boolean selected = switch(h.mode) {
			case PET -> pet == rulePet(p, h.trigger);
			case EXCEPTIONS -> exceptions(p, h.trigger).contains(pet);
			case CYCLE -> !at.isEmpty();
		};

		List<String> trailing = new ArrayList<>();
		switch(h.mode) {
			case PET -> trailing.add(selected ? "<green><bold>THIS RULE'S PET" : "<yellow>Click to summon this on the trigger");
			case EXCEPTIONS -> {
				if(selected) {
					trailing.add("<green><bold>AN EXCEPTION");
					trailing.add("<yellow>Click to stop excepting it");
				} else {
					trailing.add("<yellow>Click to except it");
				}
			}
			case CYCLE -> {
				if(selected) {
					StringBuilder sb = new StringBuilder("<gray>In the cycle at: <white>");
					for(int i = 0; i < at.size(); i++) sb.append(i == 0 ? "" : ", ").append(at.get(i));
					trailing.add(sb.toString());
				}
				trailing.add("<yellow>Click to add it to the end");
			}
		}

		ItemStack it = pet.icon(selected, false);
		ItemMeta m = it.getItemMeta();
		if(m == null) return it;
		List<Component> lore = m.lore();
		List<Component> out = lore == null ? new ArrayList<>() : new ArrayList<>(lore);
		if(!out.isEmpty()) out.removeLast(); // the action line icon() wrote, which is the summoning menu's
		for(String line : trailing) out.add(Utils.mm(line));
		m.lore(out);
		it.setItemMeta(m);
		// The stack number IS the repeat count in cycle mode: a pet listed three times reads as x3 at a glance,
		// and which positions it actually occupies is in the lore, where the order can be spelled out.
		if(h.mode == Mode.CYCLE) it.setAmount(Math.max(1, Math.min(64, at.size())));
		return it;
	}

	/**
	 * The cycle written out, one numbered line per throw.
	 * <p>
	 * <b>Capped.</b>  The editor appends, so a cycle has no length limit any more and a tooltip taller than the
	 * screen shows the player less than a short one does.  Shared with {@link AutopetMenu}, which draws the same
	 * list on the Rod Swap button and would otherwise cap it differently or not at all.
	 */
	static List<String> cycleLines(List<PetType> cycle) {
		List<String> out = new ArrayList<>();
		if(cycle.isEmpty()) {
			out.add("<dark_gray>  empty (off)");
			return out;
		}
		int shown = Math.min(cycle.size(), CYCLE_LINES);
		for(int i = 0; i < shown; i++) out.add("<gray>  " + (i + 1) + ". " + cycle.get(i).colouredName());
		if(cycle.size() > shown) out.add("<dark_gray>  ... and " + (cycle.size() - shown) + " more");
		return out;
	}

	/** How many cycle entries a tooltip lists before it summarises the rest. */
	private static final int CYCLE_LINES = 12;

	/** Every 1-based position {@code pet} occupies in the cycle.  More than one is legal - repeats are allowed. */
	private static List<Integer> positions(List<PetType> cycle, PetType pet) {
		List<Integer> out = new ArrayList<>();
		for(int i = 0; i < cycle.size(); i++) if(cycle.get(i) == pet) out.add(i + 1);
		return out;
	}

	// ==================== clicks ====================

	/** Called by {@link AutopetMenu}'s registered handler once it has recognised this window's holder. */
	void onClick(InventoryClickEvent e, Holder h) {
		if(Menus.ignoreDoubleClick(e)) return;
		e.setCancelled(true); // every slot, both inventories, before anything is read
		if(!(e.getWhoClicked() instanceof Player p)) return;
		if(e.getClickedInventory() != e.getView().getTopInventory()) return;
		// No mode check, for the reason AutopetMenu gives: this edits a preference that is the player's whichever
		// difficulty is loaded, and a gate here would only make the buttons stop working under them.

		int slot = e.getRawSlot();
		if(slot == BACK_SLOT) {
			back(p);
			return;
		}
		PetType pet = petAt(slot);
		switch(h.mode) {
			case PET -> {
				if(slot == LEFT_SLOT) {
					setPet(p, h.trigger, null);
					back(p);
				} else if(pet != null) {
					setPet(p, h.trigger, pet);
					back(p); // single select: the choice IS the exit
				}
			}
			case EXCEPTIONS -> {
				if(pet == null) return;
				toggleException(p, h.trigger, pet);
				draw(p, h);
			}
			case CYCLE -> {
				if(slot == LEFT_SLOT) removeLast(p);
				else if(slot == RIGHT_SLOT) setCycle(p, List.of());
				else if(pet != null) append(p, pet);
				else return; // the header or a pane
				draw(p, h);
			}
		}
	}

	/** Dragging is another way to move an item, so it is refused wholesale - as in every other window here. */
	void onDrag(InventoryDragEvent e) {
		e.setCancelled(true);
	}

	/**
	 * Every close, deliberate or not.  The spectator bypass was armed on open and has to come back off, or it
	 * stays on the connection for the rest of the session.
	 * <p>
	 * An Escape then puts the player back in the autopet menu rather than nowhere.  Three guards on that: the
	 * quit path fires a close for a player who is already gone, a shutdown fires one while the scheduler refuses
	 * new tasks, and something else may have opened a window in the tick we waited - {@code /pets}, a Goldor
	 * terminal - which a reopen would yank them straight back out of.
	 */
	void onClose(InventoryCloseEvent e) {
		if(!(e.getPlayer() instanceof Player p)) return;
		SpectatorGuiAccess.uninstall(p);
		if(swapping.remove(p.getUniqueId())) return; // a reopen is already in flight
		if(!M7tas.getInstance().isEnabled()) return;
		Bukkit.getScheduler().runTask(M7tas.getInstance(), () -> {
			if(!p.isOnline()) return;
			// CRAFTING (or CREATIVE) is Bukkit's "no container open": anything else means a window arrived while
			// we were waiting and it is not ours to close.
			InventoryType open = p.getOpenInventory().getType();
			if(open != InventoryType.CRAFTING && open != InventoryType.CREATIVE) return;
			parent.open(p);
		});
	}

	/** Leave the picker for the menu it came from.  Deferred a tick: this view's close has to land first. */
	private void back(Player p) {
		swapping.add(p.getUniqueId());
		Bukkit.getScheduler().runTask(M7tas.getInstance(), () -> {
			if(p.isOnline()) parent.open(p);
			else swapping.remove(p.getUniqueId()); // they left before the reopen; nothing will consume the flag
		});
	}

	// ==================== the edits ====================

	private static PetType rulePet(Player p, Autopet.Trigger t) {
		Autopet.Rule rule = Pets.rule(p, t);
		return rule == null ? null : rule.pet();
	}

	private static List<PetType> exceptions(Player p, Autopet.Trigger t) {
		Autopet.Rule rule = Pets.rule(p, t);
		return rule == null ? List.of() : rule.exceptions();
	}

	/** Set the pet half, keeping the exceptions: the two halves are set in either order and neither clears the other. */
	private static void setPet(Player p, Autopet.Trigger t, PetType pet) {
		Pets.setRule(p, t, new Autopet.Rule(pet, exceptions(p, t)));
	}

	private static void toggleException(Player p, Autopet.Trigger t, PetType pet) {
		List<PetType> now = new ArrayList<>(exceptions(p, t));
		if(!now.remove(pet)) now.add(pet);
		Pets.setRule(p, t, new Autopet.Rule(rulePet(p, t), now));
	}

	private static void append(Player p, PetType pet) {
		List<PetType> cycle = new ArrayList<>(Pets.rodCycle(p));
		cycle.add(pet); // no contains() check: the same pet twice in a rotation is the point
		setCycle(p, cycle);
	}

	private static void removeLast(Player p) {
		List<PetType> cycle = new ArrayList<>(Pets.rodCycle(p));
		if(cycle.isEmpty()) return;
		cycle.removeLast();
		setCycle(p, cycle);
	}

	/**
	 * The one write for the cycle, so the cursor can never be left pointing into a list that has moved under it.
	 * The copy is not optional: {@code Pets.rodCycle} hands back the live list and {@code setRodCycle} clears it
	 * before refilling.
	 */
	private static void setCycle(Player p, List<PetType> cycle) {
		Pets.setRodCycle(p, cycle);
		Autopet.clearRodCursor(p);
	}

	/** Marker holder carrying which rule is being edited and in which mode.  The values live on the profile. */
	public static final class Holder implements InventoryHolder {
		final Autopet.Trigger trigger;
		final Mode mode;
		Inventory inv;

		Holder(Autopet.Trigger trigger, Mode mode) {
			this.trigger = trigger;
			this.mode = mode;
		}

		@Override
		public @NotNull Inventory getInventory() {
			return inv;
		}
	}
}
