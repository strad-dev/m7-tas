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
import java.util.stream.Collectors;

/**
 * Exports the M7 item catalog to the shared data folder ({@code ../data/m7-item-catalog.json}) on
 * plugin enable, so the network plugin's lobby loadout editor can load the real M7 items (palette)
 * and each class's default kit.  M7 is the SOLE writer of this file and the single source of
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

	/**
	 * One class's default 41-slot kit, freshly BUILT from the item factories, so it always carries whatever lore,
	 * attributes or NBT those factories currently produce.  The per-class Terminator Power is baked in, matching
	 * the TAS.
	 * <br>
	 * This and {@link #palette()} are the in-memory catalog.  {@link #export()} is only a serializer of them, and
	 * M7's own loadout editor reads them directly, so the editor and the exported JSON cannot disagree, and the
	 * editor works before the first export, or if the shared folder is unwritable.
	 */
	public static ItemStack[] defaultFor(String role) {
		ItemStack[] arr = FakePlayerInventory.classLoadoutContents(role);
		int power = terminatorPower(role);
		if (power > 0 && arr[4] != null && arr[4].getType() == Material.BOW) {
			arr[4].addUnsafeEnchantment(Enchantment.POWER, power);
		}
		return arr;
	}

	/**
	 * Every item the loadout editor offers, in {@link #PALETTE_ORDER}. Fresh copies from the item factories, same as
	 * {@link #defaultFor}.
	 * <br>
	 * Logical key -> the first copy seen for it, so the palette lists each ITEM once. Deduping on the raw serialized
	 * bytes was not enough: two copies of the same item can differ byte-for-byte (the per-class Terminator Power,
	 * and previously every custom head's random profile id), which listed them twice in the editor. Sorting is
	 * stable, so an item missing from PALETTE_ORDER keeps its discovery order in the tail (ROLES x slot index, then
	 * extraPaletteItems).
	 */
	public static List<ItemStack> palette() {
		LinkedHashMap<String, ItemStack> byKey = new LinkedHashMap<>();
		for (String role : ROLES) {
			for (ItemStack it : defaultFor(role)) {
				if (it != null && !it.getType().isAir() && !hiddenFromPalette(it)) byKey.putIfAbsent(paletteKey(it), it);
			}
		}
		// Items nobody's default kit carries still belong in the palette.
		for (ItemStack it : extraPaletteItems()) {
			if (it != null && !it.getType().isAir()) byKey.putIfAbsent(paletteKey(it), it);
		}
		return byKey.values().stream()
				.sorted(Comparator.comparingInt(Catalog::paletteRank))
				.collect(Collectors.toCollection(ArrayList::new));
	}

	public static void export() {
		try {
			CatalogFile f = new CatalogFile();
			f.defaults = new LinkedHashMap<>();
			for (String role : ROLES) f.defaults.put(role, toSer(defaultFor(role)));
			f.palette = new ArrayList<>();
			for (ItemStack it : palette()) {
				String b64 = ItemSerial.toB64(it);
				if (b64 != null) f.palette.add(b64);
			}

			Path file = dataDir().resolve("m7-item-catalog.json");
			save(file, f);
			M7tas.getInstance().getLogger().info("Exported M7 item catalog (" + f.palette.size()
					+ " palette items) to " + file);
		} catch (Exception e) {
			M7tas.getInstance().getLogger().warning("Failed to export M7 item catalog: " + e);
		}
	}

	/** A 41-slot array as the on-disk list: one entry per slot, null for an empty one. */
	private static List<String> toSer(ItemStack[] arr) {
		List<String> ser = new ArrayList<>(41);
		for (int i = 0; i < 41; i++) ser.add(ItemSerial.toB64(arr != null && i < arr.length ? arr[i] : null));
		return ser;
	}

	/**
	 * Items that stay in the default kits but must NOT be offered in the editor palette. Just the SkyBlock Menu: it
	 * has no ability, and the network plugin's editor pins it to the last hotbar slot itself, so there is nothing
	 * to pick it for.  The Rapid Bonemerang was removed from the Archer kit outright rather than hidden here, so it
	 * no longer reaches the palette at all; it had no ability behind it.
	 */
	private static boolean hiddenFromPalette(ItemStack it) {
		return FakePlayerInventory.isSkyblockMenu(it);
	}

	/** Custom items that are in no class's default kit but should still be offered by the loadout editor. */
	static List<ItemStack> extraPaletteItems() {
		return List.of(
				FakePlayerInventory.getGolemSword(),
				FakePlayerInventory.getWitherGoggles(),
				FakePlayerInventory.getLovingStormChestplate(),
				FakePlayerInventory.getNecroticStormLeggings(),
				FakePlayerInventory.getNecroticStormBoots());
	}

	/**
	 * The exported palette's order, hand-picked rather than derived from a category. The network plugin's loadout
	 * editor renders the palette in list order, 9 per page (its {@code LoadoutEditor.PALETTE_COUNT} = the whole top
	 * row), so <b>each block of 9 below is literally one page</b>.  Keep the blocks nine long or the pages shift.
	 * <br>
	 * Entries are {@link #orderName}: the PLAIN display name, or the material name for the one nameless stack (the
	 * ender pearls). Renaming an item here without renaming it in {@link FakePlayerInventory} silently drops it to
	 * the tail, so change both together.
	 * <br>
	 * Anything not listed sorts to the end in discovery order, so a newly added item shows up at the back of the
	 * palette instead of vanishing. The list currently covers every palette item exactly, so that tail is empty.
	 * <br>
	 * Names here are DISPLAY names, and the Fabled reforge is displayed as {@code Withered} on purpose (see
	 * DAMAGE_PLAN.md §1.0.6), so Hyperion, Dark Claymore, Flaming Flay and Ragnarock Axe all read "Withered".
	 */
	private static final List<String> PALETTE_ORDER = List.of(
			// Page 1: the core damage kit.
			"Heroic Hyperion",
			"Withered Hyperion",
			"Precise Terminator",
			"Withered Dark Claymore",
			"Warped Aspect of the Void",
			"Dungeonbreaker",
			"Heroic Bonzo Staff",
			"Infinityboom TNT",
			"Infinileap",
			// Page 2: situational weapons and the pearls.
			"Heroic Ice Spray Wand",
			"Precise Last Breath",
			"Withered Ragnarock Axe",
			"Suspicious Axe of the Shredded",
			"Withered Flaming Flay",
			"Heroic Jerry-chine Gun",
			"Suspicious Golem Sword",
			"Precise Explosive Bow",
			"ENDER_PEARL",
			// Page 3: utility, the cosmetic heads, then the Necron set.
			"Gyrokinetic Wand",
			"Tactical Insertion",
			"Pitchin' Rod of the Sea",
			"Ancient Spirit Mask",
			"Ancient Bonzo's Mask",
			"Ancient Diamond Necron Head",
			"Ancient Necron's Chestplate",
			"Ancient Necron's Leggings",
			"Ancient Necron's Boots",
			// Page 4: the Storm set, then its alternate-reforge pieces.
			"Ancient Storm's Helmet",
			"Ancient Storm's Chestplate",
			"Ancient Storm's Leggings",
			"Ancient Storm's Boots",
			"Necrotic Wither Goggles",
			"Loving Storm's Chestplate",
			"Necrotic Storm's Leggings",
			"Necrotic Storm's Boots",
			"Renowned Cow Hat",
			// Page 5: the rest of the Renowned wearables.
			"Renowned Spring Boots",
			"Renowned Racing Helmet",
			"Renowned Thermodynamic Helmet",
			"Renowned Thermodynamic Chestplate",
			"Renowned Thermodynamic Leggings",
			"Renowned Thermodynamic Boots");

	/** Sort key for one palette item: its index in {@link #PALETTE_ORDER}, or the end of the list if unlisted. */
	private static int paletteRank(ItemStack it) {
		int i = PALETTE_ORDER.indexOf(orderName(it));
		return i < 0 ? PALETTE_ORDER.size() : i;
	}

	/** An item's {@link #PALETTE_ORDER} entry: plain display name, falling back to the material for a nameless stack. */
	private static String orderName(ItemStack it) {
		ItemMeta meta = it.hasItemMeta() ? it.getItemMeta() : null;
		String name = meta == null ? "" : Utils.plain(meta.displayName());
		return name.isEmpty() ? it.getType().name() : name;
	}

	/**
	 * An item's LOGICAL identity for palette dedupe: material + display name + custom-item lore ID, all as PLAIN
	 * text so a colour/format change can't split one item into two entries. Two stacks that agree on all three are
	 * the same item to a player picking one out of the editor, however their NBT differs (enchant levels, profile
	 * ids, amounts), and only the first is kept.  Items that share an ID but not a name (Heroic vs Withered
	 * Hyperion) stay separate entries, which is what you want in a picker.
	 * <br>
	 * Both loadout/ItemRefresh copies (M7's own and the network plugin's) match saved loadout items against the
	 * catalog with this exact rule, so keep all three in sync.
	 * <br>
	 * <b>Only the FIRST lore line is read.</b> That is what lets an item grow lore freely, with stat lines, ability
	 * text, rarity and everything DAMAGE_PLAN.md will hang on these items, without breaking the match: a saved copy
	 * still keys to the same string and gets silently replaced by the new definition on the next refresh.  Keep the
	 * item ID on lore line 0 (which {@code CustomItems.getID()} already requires) and item changes need no
	 * migration; move it, or prepend a line above it, and every saved loadout in the network quietly stops updating.
	 */
	public static String paletteKey(ItemStack it) {
		ItemMeta meta = it.hasItemMeta() ? it.getItemMeta() : null;
		if (meta == null) return it.getType().name() + "||";
		return it.getType().name() + "|" + Utils.plain(meta.displayName()) + "|" + Utils.firstLorePlain(meta);
	}

	/** Terminator Power per class, matching the TAS (Archer 70, Berserk/Healer/Tank 17, Mage none). */
	private static int terminatorPower(String role) {
		return switch (role) {
			case "Archer" -> 70;
			case "Berserk", "Healer", "Tank" -> 17;
			default -> 0;
		};
	}

	/** Shared data folder, resolved as {@code <server>/../data} (mirrors the network plugin's Config default). */
	public static Path dataDir() throws Exception {
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
