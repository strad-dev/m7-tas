package plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Exports the M7 item catalog to the shared data folder ({@code ../data/m7-item-catalog.json}) on
 * plugin enable, so the network plugin's lobby loadout editor can load the real M7 items (palette)
 * and each class's default kit. M7 is the SOLE writer of this file - it is the single source of
 * truth for item definitions; the network plugin only reads it.
 * <br>
 * Format (matches the network plugin's reader):
 *   { "palette": [ "&lt;base64 item&gt;", ... ],
 *     "defaults": { "Archer": [ &lt;41 base64-or-null&gt; ], "Mage": [...], ... } }
 *
 * The 41-slot array layout is: [0..35] main inventory slots, [36] helmet, [37] chestplate,
 * [38] leggings, [39] boots, [40] off-hand (see FakePlayerInventory#classLoadoutContents).
 */
public final class Catalog {
	private static final String[] ROLES = {"Archer", "Berserk", "Healer", "Mage", "Tank"};

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private Catalog() {}

	public static void export() {
		try {
			CatalogFile f = new CatalogFile();
			f.defaults = new LinkedHashMap<>();
			// Logical key -> the first b64 seen for it, so the palette lists each ITEM once. Deduping on the raw
			// b64 was not enough: two copies of the same item can differ byte-for-byte (the per-class Terminator
			// Power, and previously every custom head's random profile id), which listed them twice in the editor.
			LinkedHashMap<String, String> palette = new LinkedHashMap<>();

			for (String role : ROLES) {
				ItemStack[] arr = FakePlayerInventory.classLoadoutContents(role);
				// Bake the per-class Terminator Power into the default kit, matching /getcustomitems.
				int power = terminatorPower(role);
				if (power > 0 && arr[4] != null && arr[4].getType() == Material.BOW) {
					arr[4].addUnsafeEnchantment(Enchantment.POWER, power);
				}
				List<String> ser = new ArrayList<>(41);
				for (ItemStack it : arr) {
					String b64 = ItemSerial.toB64(it);
					ser.add(b64);
					if (b64 != null && !hiddenFromPalette(it)) palette.putIfAbsent(paletteKey(it), b64);
				}
				f.defaults.put(role, ser);
			}
			// Items nobody's default kit carries still belong in the palette.
			for (ItemStack it : extraPaletteItems()) {
				String b64 = ItemSerial.toB64(it);
				if (b64 != null) palette.putIfAbsent(paletteKey(it), b64);
			}
			f.palette = new ArrayList<>(palette.values());

			Path file = dataDir().resolve("m7-item-catalog.json");
			save(file, f);
			M7tas.getInstance().getLogger().info("Exported M7 item catalog (" + f.palette.size()
					+ " palette items) to " + file);
		} catch (Exception e) {
			M7tas.getInstance().getLogger().warning("Failed to export M7 item catalog: " + e);
		}
	}

	/**
	 * Items that stay in the default kits but must NOT be offered in the editor palette. Just the SkyBlock Menu: it
	 * has no ability, and the network plugin's editor pins it to the last hotbar slot itself, so there is nothing
	 * to pick it for.
	 */
	private static boolean hiddenFromPalette(ItemStack it) {
		return FakePlayerInventory.isSkyblockMenu(it);
	}

	/** Custom items that are in no class's default kit but should still be offered by the loadout editor. */
	static List<ItemStack> extraPaletteItems() {
		return List.of(FakePlayerInventory.getGolemSword());
	}

	/**
	 * An item's LOGICAL identity for palette dedupe: material + display name + custom-item lore ID, all as PLAIN
	 * text so a colour/format change can't split one item into two entries. Two stacks that agree on all three are
	 * the same item to a player picking one out of the editor, however their NBT differs (enchant levels, profile
	 * ids, amounts) - only the first is kept. Items that share an ID but not a name (Heroic vs Withered Hyperion)
	 * stay separate entries, which is what you want in a picker.
	 * <br>
	 * The network plugin's {@code loadout/ItemRefresh.key} matches saved loadout items against the exported catalog
	 * with this exact rule - keep the two in sync.
	 */
	private static String paletteKey(ItemStack it) {
		ItemMeta meta = it.hasItemMeta() ? it.getItemMeta() : null;
		if (meta == null) return it.getType().name() + "||";
		return it.getType().name() + "|" + Utils.plain(meta.displayName()) + "|" + Utils.firstLorePlain(meta);
	}

	/** Terminator Power per class, matching the TAS / GetCustomItems (Archer 70, Berserk/Healer/Tank 17, Mage none). */
	private static int terminatorPower(String role) {
		return switch (role) {
			case "Archer" -> 70;
			case "Berserk", "Healer", "Tank" -> 17;
			default -> 0;
		};
	}

	/** Shared data folder, resolved as {@code <server>/../data} (mirrors the network plugin's Config default). */
	private static Path dataDir() throws Exception {
		Path dir = M7tas.getInstance().getServer().getWorldContainer().toPath().resolve("../data").normalize();
		Files.createDirectories(dir);
		return dir;
	}

	private static void save(Path file, Object value) throws Exception {
		Path parent = file.getParent();
		if (parent != null) Files.createDirectories(parent);
		Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
		try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
			GSON.toJson(value, w);
		}
		try {
			Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (AtomicMoveNotSupportedException ex) {
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/** On-disk shape (field names must match the network plugin's reader). */
	public static class CatalogFile {
		public List<String> palette = new ArrayList<>();
		public Map<String, List<String>> defaults = new LinkedHashMap<>();
	}
}
