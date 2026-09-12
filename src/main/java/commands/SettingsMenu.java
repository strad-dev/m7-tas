package commands;

import damage.Difficulty;
import damage.Mayor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jspecify.annotations.NonNull;
import plugin.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The one-row <b>M7 Settings</b> menu behind a bare {@code /dungeonsettings}: the difficulty and the mayor, each
 * as one button whose lore lists every value with the one in force in bold, and each click stepping to the next.
 *
 * <p><b>Standalone only.</b>  On the network those two are PARTY settings - they ride the practice request so a
 * party can't be mixed-mode - and the lobby has its own copy of this menu that writes them there.  This one flips
 * the server-wide globals, which on a shared instance would silently change what somebody else's run is scored
 * under, so {@link #suppressed()} refuses to open it whenever the network plugin is installed and
 * {@code /dungeonsettings} falls back to its text output. The command's text form still works either way, which
 * is what the network gates to admins (see {@code M7Bridge}).
 */
public final class SettingsMenu implements Listener {
	private static final int DIFFICULTY_SLOT = 3, MAYOR_SLOT = 5;

	/** One line, on both buttons, so the right-click half is never a secret. */
	private static final String CYCLE_HINT = "<yellow>Click to change <dark_gray>(right-click to go back)";

	/** The Megakloon head Hypixel's mayor menu uses, as profile id + texture value + signature. */
	private static final UUID MAYOR_HEAD_ID = UUID.fromString("ff8b48bd-20ef-33c2-9a57-0df4860ebdc6");
	private static final String MAYOR_HEAD_VALUE = "ewogICJ0aW1lc3RhbXAiIDogMTU5Nzc4MDk0OTYzOCwKICAicHJvZmlsZUlkIiA6ICI0MWQzYWJjMmQ3NDk0MDBjOTA5MGQ1NDM0ZDAzODMxYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJNZWdha2xvb24iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWI1OWM0M2Q4ZGJjY2ZkN2VjNmU2Mzk0YjYzMDRiNzBkNGVkMzE1YWRkMDQ5NGVlNzdjNzMzZjQxODE4YzczYSIKICAgIH0KICB9Cn0=";
	private static final String MAYOR_HEAD_SIGNATURE = "UeEf0Ir81BLTBE0PfVeEtU/6mHUw3xSP7XGcQYN18qlYS6J7qd+bQskaSJQbEHXr+axq2+5aPm/AfGrjNnl9zQn1EucBIwhdRBHnjuJeRY6x9VKEHAtX2gnpnNelU/oP6MKPZ1dUad4iAHQg8BmJR/oQpedvOJDuqdUZowe8WTVFC5qctJQWRIZCX0BWYK1O1xxJx4FZ9LzF++7qWZsVqO+qmOR8R7Xr4jFkF8cdNIRyezgcfhmw3BCqiDDheOzuJzo0l7y9kHR82reHus/JBLGyTy/iqMqlZFgNePEoaOGRgIvROw9oIS4R/19+UABIe0MDD6CSGgsE7VfgWeyCVw3qxmCSAZDHnYuYyH0zpnGQRsmQrx9aTjFXHf6g551MEpx7KGGTMaOm9b7ygOGuVGB/52UXR2W9UU+YtBZoUzDPMeVcM1NQno/fY3rLoSf5PfuaaEpZMlczYpH0DBvApEQ6FBm/XTOPeS5w3a+7UXh/wrjXu5b62rXG4SNZaBZRT76eseX3wZoCGiLYEh+IXfAJxXSKOqmlHmRmv6FGRzMcZpHzr63GqD0jJwpLuUZ54uuMgUtjx/liEZG1pesdUuf0ObRVf+xDxk/iLAiNKRvd6BoP7wVm0CkebDlyPObdKW0Ss+tAhB0y1o4das+n+UjpHUuPM2D/eLZFA01umyw=";

	/**
	 * True while this menu must not open: the network plugin is installed, so the settings belong to the party and
	 * the lobby's own menu owns them.  Presence only - M7 TAS keeps no compile-time dependency on it.
	 */
	public static boolean suppressed() {
		return Bukkit.getPluginManager().getPlugin("StradNetworkPlugin") != null;
	}

	public void open(Player p) {
		Holder h = new Holder();
		Inventory inv = Bukkit.createInventory(h, 9, Utils.msg("<dark_gray>M7 Settings"));
		h.inv = inv;

		List<String> diffLore = new ArrayList<>();
		for(Difficulty d : Difficulty.values()) diffLore.add(option(d == Difficulty.current(), colour(d), label(d)));
		diffLore.add("");
		diffLore.add(CYCLE_HINT);
		inv.setItem(DIFFICULTY_SLOT, button(Material.WITHER_SKELETON_SKULL, "<gold>Difficulty", diffLore));

		List<String> mayorLore = new ArrayList<>();
		for(Mayor m : Mayor.values()) mayorLore.add(option(m == Mayor.current(), colour(m), label(m)));
		mayorLore.add("");
		mayorLore.add(CYCLE_HINT);
		inv.setItem(MAYOR_SLOT, mayorHead(mayorLore));

		p.openInventory(inv);
	}

	@EventHandler
	public void onClick(InventoryClickEvent e) {
		if(!(e.getView().getTopInventory().getHolder() instanceof Holder)) return;
		e.setCancelled(true); // read-only row: nothing here is ever picked up
		if(!(e.getWhoClicked() instanceof Player p)) return;
		if(e.getClickedInventory() != e.getView().getTopInventory()) return;

		// Both settings broadcast, exactly as the text command does: they are server-wide, so everybody online
		// needs to know they moved.  A right-click steps BACK, so three values are reachable in one click either way.
		boolean back = e.isRightClick();
		if(e.getRawSlot() == DIFFICULTY_SLOT) {
			DungeonSettings.applyDifficulty(back ? Difficulty.toggleBack() : Difficulty.toggle());
		} else if(e.getRawSlot() == MAYOR_SLOT) {
			DungeonSettings.applyMayor(back ? Mayor.toggleBack() : Mayor.toggle());
		} else {
			return;
		}
		open(p); // re-render in place, so the new value is under the cursor
	}

	@EventHandler
	public void onDrag(InventoryDragEvent e) {
		if(e.getView().getTopInventory().getHolder() instanceof Holder) e.setCancelled(true);
	}

	// ===== helpers =====
	/** One line of a settings item's option list: the value in force is bold and keeps its colour. */
	private static String option(boolean selected, String colour, String displayName) {
		return selected ? colour + "<bold>" + displayName : "<dark_gray>" + displayName;
	}

	private static String colour(Difficulty d) {
		return switch(d) {
			case CLASSIC -> "<aqua>";
			case REALISTIC -> "<light_purple>";
			case ULTRA_REALISTIC -> "<red>";
		};
	}

	private static String colour(Mayor m) {
		return switch(m) {
			case PAUL -> "<green>";
			case DERPY -> "<light_purple>";
			case OTHER -> "<gray>";
		};
	}

	private static String label(Difficulty d) {
		return switch(d) {
			case CLASSIC -> "Classic";
			case REALISTIC -> "Realistic";
			case ULTRA_REALISTIC -> "Ultra Realistic";
		};
	}

	private static String label(Mayor m) {
		return switch(m) {
			case PAUL -> "Paul";
			case DERPY -> "Derpy";
			case OTHER -> "Other";
		};
	}

	private static ItemStack mayorHead(List<String> lore) {
		ItemStack it = button(Material.PLAYER_HEAD, "<gold>Mayor", lore);
		if(it.getItemMeta() instanceof SkullMeta sm) {
			com.destroystokyo.paper.profile.PlayerProfile profile = Bukkit.createProfile(MAYOR_HEAD_ID, "Megakloon");
			profile.setProperty(new com.destroystokyo.paper.profile.ProfileProperty(
					"textures", MAYOR_HEAD_VALUE, MAYOR_HEAD_SIGNATURE));
			sm.setPlayerProfile(profile);
			it.setItemMeta(sm);
		}
		return it;
	}

	private static ItemStack button(Material mat, String name, List<String> lore) {
		ItemStack it = new ItemStack(mat);
		ItemMeta m = it.getItemMeta();
		if(m != null) {
			m.displayName(Utils.msg(name).decoration(TextDecoration.ITALIC, false));
			List<Component> rendered = new ArrayList<>();
			for(String line : lore) rendered.add(Utils.msg(line).decoration(TextDecoration.ITALIC, false));
			m.lore(rendered);
			it.setItemMeta(m);
		}
		return it;
	}

	/** Marker holder; the menu has no state of its own, since both values are server-wide globals. */
	private static final class Holder implements InventoryHolder {
		Inventory inv;

		@Override
		public @NonNull Inventory getInventory() {
			return inv;
		}
	}
}
