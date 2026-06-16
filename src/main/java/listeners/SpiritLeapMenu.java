package listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import plugin.FakePlayerManager;

import java.util.*;

/**
 * Practice-mode Spirit Leap GUI: a 5-row chest that lets a player teleport to up to four teammates. Teammates are
 * placed in the four corners (top-left, top-right, bottom-left, bottom-right), ordered by class alphabetically then
 * by name. Each teammate "owns" a quadrant filled with their class-colored stained glass (Archer green, Berserk red,
 * Healer yellow, Mage light blue, Tank gray) with their head in the corner; clicking anywhere in a quadrant leaps to
 * that teammate. The middle row + middle column are white stained glass dividers that do nothing.
 *
 * <p>The instance is the inventory's {@link InventoryHolder}, carrying the slot→teammate map so the click handler
 * ({@link SpiritLeapListener}) can resolve the target.
 */
public class SpiritLeapMenu implements InventoryHolder {
	private static final int SIZE = 45; // 5 rows × 9

	// Quadrant slot groups (cols 0–3 / 5–8, rows 0–1 / 3–4) and each quadrant's outer corner.
	private static final int[][] QUADRANTS = {
			{0, 1, 2, 3, 9, 10, 11, 12},        // top-left
			{5, 6, 7, 8, 14, 15, 16, 17},       // top-right
			{27, 28, 29, 30, 36, 37, 38, 39},   // bottom-left
			{32, 33, 34, 35, 41, 42, 43, 44}    // bottom-right
	};
	private static final int[] CORNERS = {0, 8, 36, 44};
	// Middle row (18–26) + middle column (4,13,22,31,40) — plain glass dividers, no action.
	private static final int[] CROSS = {4, 13, 18, 19, 20, 21, 22, 23, 24, 25, 26, 31, 40};

	private final Inventory inv;
	private final Map<Integer, Player> slotTargets = new HashMap<>();

	private SpiritLeapMenu(Player viewer) {
		inv = Bukkit.createInventory(this, SIZE, ChatColor.DARK_GRAY + "Spirit Leap");
		build(viewer);
	}

	public static void open(Player viewer) {
		viewer.openInventory(new SpiritLeapMenu(viewer).inv);
	}

	/** True if there's at least one teammate the viewer could leap to. */
	public static boolean hasCandidates(Player viewer) {
		return !candidates(viewer).isEmpty();
	}

	@Override
	public Inventory getInventory() {
		return inv;
	}

	/** Teammate a given menu slot leaps to, or null for dividers / empty quadrants / the player's own inventory. */
	public Player targetForSlot(int slot) {
		return slotTargets.get(slot);
	}

	private void build(Player viewer) {
		ItemStack divider = pane(Material.WHITE_STAINED_GLASS_PANE, " ");
		for(int s : CROSS) inv.setItem(s, divider);

		List<Player> targets = candidates(viewer);
		for(int q = 0; q < QUADRANTS.length && q < targets.size(); q++) {
			Player t = targets.get(q);
			String cls = resolveClass(t);
			ItemStack glass = pane(classPane(cls), classColor(cls) + "Leap to " + t.getName());
			for(int s : QUADRANTS[q]) {
				inv.setItem(s, glass);
				slotTargets.put(s, t);
			}
			inv.setItem(CORNERS[q], head(t, cls)); // corner shows the teammate's head (still leaps on click)
		}
	}

	/** Online, non-spectating, non-fake players (excluding the viewer), ordered by class then name, capped at 4. */
	private static List<Player> candidates(Player viewer) {
		List<Player> list = new ArrayList<>();
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(p.equals(viewer)) continue;
			if(p.getGameMode() == GameMode.SPECTATOR) continue;
			if(FakePlayerManager.getFakePlayers().containsValue(p)) continue;
			list.add(p);
		}
		list.sort(Comparator.comparing(SpiritLeapMenu::resolveClass).thenComparing(Player::getName));
		return list.size() > 4 ? new ArrayList<>(list.subList(0, 4)) : list;
	}

	/** Class from the player's scoreboard tag (set by /getcustomitems), falling back to the fake-player name. */
	private static String resolveClass(Player p) {
		for(String c : new String[]{"Archer", "Berserk", "Healer", "Mage", "Tank"}) {
			if(p.getScoreboardTags().contains(c)) return c;
		}
		String n = p.getName();
		if(n.equals("Archer")) return "Archer";
		if(n.equals("Mage2")) return "Tank";
		if(n.equals("Mage3")) return "Berserk";
		if(n.equals("Mage4")) return "Healer";
		return "Mage";
	}

	private static Material classPane(String cls) {
		return switch(cls) {
			case "Archer" -> Material.GREEN_STAINED_GLASS_PANE;
			case "Berserk" -> Material.RED_STAINED_GLASS_PANE;
			case "Healer" -> Material.YELLOW_STAINED_GLASS_PANE;
			case "Tank" -> Material.GRAY_STAINED_GLASS_PANE;
			default -> Material.LIGHT_BLUE_STAINED_GLASS_PANE; // Mage
		};
	}

	private static ChatColor classColor(String cls) {
		return switch(cls) {
			case "Archer" -> ChatColor.GREEN;
			case "Berserk" -> ChatColor.RED;
			case "Healer" -> ChatColor.YELLOW;
			case "Tank" -> ChatColor.GRAY;
			default -> ChatColor.AQUA; // Mage (light blue)
		};
	}

	private static ItemStack pane(Material mat, String name) {
		ItemStack it = new ItemStack(mat);
		ItemMeta meta = it.getItemMeta();
		meta.setDisplayName(name);
		it.setItemMeta(meta);
		return it;
	}

	private static ItemStack head(Player target, String cls) {
		ItemStack it = new ItemStack(Material.PLAYER_HEAD);
		SkullMeta meta = (SkullMeta) it.getItemMeta();
		meta.setOwningPlayer(target);
		meta.setDisplayName(classColor(cls) + target.getName());
		it.setItemMeta(meta);
		return it;
	}
}
