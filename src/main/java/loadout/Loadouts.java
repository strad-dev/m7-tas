package loadout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import plugin.Catalog;
import plugin.ItemSerial;
import plugin.M7tas;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player, per-class M7 loadout storage: {@code <data>/loadouts/&lt;uuid&gt;.json} holds the player's selected
 * class plus one saved 41-slot inventory per class.
 * <p>
 * <b>This is the SAME file the network plugin's {@code loadout/Loadouts} reads and writes.</b> That is the point:
 * a player edits their kit in the lobby and it is the kit they practice with here. The shared folder is resolved
 * by {@link Catalog#dataDir()} ({@code <server>/../data}), which M7 creates itself, so this works with or without
 * the network plugin installed.
 * <p>
 * Two plugins writing one path is safe here because the file is PER PLAYER and a player is on exactly one server
 * at a time, so there is never a second writer for a given file; writes are temp+rename anyway. Do not widen this
 * into a shared multi-player file without revisiting that.
 * <p>
 * 41-slot layout (the convention the editor, {@code M7Bridge} and {@code FakePlayerInventory} all use):
 * [0..35] main inventory, [36] helmet, [37] chestplate, [38] leggings, [39] boots, [40] off-hand.
 * <p>
 * NOTE: a near-identical copy lives in the network plugin ({@code loadout/Loadouts.java}) - keep in sync.
 */
public final class Loadouts {
	/** Canonical class names (must match {@code Catalog.ROLES} and the network plugin's list). */
	public static final List<String> CLASSES = List.of("Archer", "Mage", "Tank", "Berserk", "Healer");

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private Loadouts() {}

	/** Map free user input ("archer", "MAGE", ...) to a canonical class name, or null if invalid. */
	public static String normalize(String input) {
		if(input == null) return null;
		for(String c : CLASSES) if(c.equalsIgnoreCase(input)) return c;
		return null;
	}

	// ===== file I/O =====
	private static Path file(UUID uuid) throws Exception {
		return Catalog.dataDir().resolve("loadouts/" + uuid + ".json");
	}

	public static LoadoutFile load(UUID uuid) {
		LoadoutFile f = null;
		try {
			Path p = file(uuid);
			if(Files.exists(p)) {
				try(Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
					f = GSON.fromJson(r, LoadoutFile.class);
				}
			}
		} catch(Exception e) {
			M7tas.getInstance().getLogger().warning("Failed to read loadout for " + uuid + ": " + e);
		}
		if(f == null) f = new LoadoutFile();
		if(f.perClass == null) f.perClass = new LinkedHashMap<>();
		return f;
	}

	public static void save(UUID uuid, LoadoutFile f) {
		try {
			Path file = file(uuid);
			Path parent = file.getParent();
			if(parent != null) Files.createDirectories(parent);
			Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
			try(Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				GSON.toJson(f, w);
			}
			try {
				Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch(AtomicMoveNotSupportedException ex) {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch(Exception e) {
			M7tas.getInstance().getLogger().warning("Failed to save loadout for " + uuid + ": " + e);
		}
	}

	// ===== selected class =====
	public static String getSelectedClass(UUID uuid) {
		return normalize(load(uuid).selectedClass);
	}

	public static void setSelectedClass(UUID uuid, String role) {
		LoadoutFile f = load(uuid);
		f.selectedClass = role;
		save(uuid, f);
	}

	// ===== per-class contents =====
	/** This player's saved 41-slot loadout for a class, or null if they've never saved/seeded one. */
	public static ItemStack[] getContents(UUID uuid, String role) {
		List<String> ser = load(uuid).perClass.get(role);
		return ser == null ? null : fromSer(ser);
	}

	public static void setContents(UUID uuid, String role, ItemStack[] arr) {
		LoadoutFile f = load(uuid);
		f.perClass.put(role, toSer(arr));
		save(uuid, f);
	}

	/** Seed this class's loadout from the current default kit if the player has none yet. */
	public static void seedIfAbsent(UUID uuid, String role) {
		LoadoutFile f = load(uuid);
		if(!f.perClass.containsKey(role)) {
			f.perClass.put(role, toSer(Catalog.defaultFor(role)));
			save(uuid, f);
		}
	}

	// ===== array <-> json =====
	public static List<String> toSer(ItemStack[] arr) {
		List<String> out = new ArrayList<>(41);
		for(int i = 0; i < 41; i++) out.add(ItemSerial.toB64(arr != null && i < arr.length ? arr[i] : null));
		return out;
	}

	public static ItemStack[] fromSer(List<String> ser) {
		ItemStack[] arr = new ItemStack[41];
		if(ser != null) for(int i = 0; i < 41 && i < ser.size(); i++) arr[i] = ItemSerial.fromB64(ser.get(i));
		return arr;
	}

	// ===== apply to a real player =====
	/** Equip {@code p} with a 41-slot loadout array, replacing their inventory. */
	public static void apply(Player p, ItemStack[] arr) {
		PlayerInventory inv = p.getInventory();
		for(int i = 0; i < 36; i++) inv.setItem(i, arr[i]);
		inv.setHelmet(arr[36]);
		inv.setChestplate(arr[37]);
		inv.setLeggings(arr[38]);
		inv.setBoots(arr[39]);
		inv.setItemInOffHand(arr[40] == null ? new ItemStack(Material.AIR) : arr[40]);
		p.updateInventory();
	}

	/**
	 * Equip {@code p} with their saved kit for their selected class, refreshed to the CURRENT item definitions, and
	 * apply the matching class scoreboard tag. Returns the class applied, or null if they have not picked one.
	 * <p>
	 * This is the one entry point that should be used to hand a player their loadout - it is what guarantees the
	 * items they receive carry today's lore/attributes and not whatever was frozen into their file months ago.
	 */
	public static String applyFor(Player p) {
		UUID id = p.getUniqueId();
		String role = getSelectedClass(id);
		if(role == null) return null;
		seedIfAbsent(id, role);
		ItemRefresh.refreshSaved(id);
		ItemStack[] arr = getContents(id, role);
		if(arr == null) return null;
		apply(p, arr);
		applyClassTag(p, role);
		return role;
	}

	/**
	 * Set the class scoreboard tag (removing any other), so class-gated behaviour - the mage beam, the per-class
	 * damage paths - treats this player as that class. Mirrors the network plugin's {@code M7Bridge.applyLoadout}.
	 */
	public static void applyClassTag(Player p, String role) {
		for(String tag : CLASSES) p.removeScoreboardTag(tag);
		p.addScoreboardTag(role);
		applySwingRange(p);
		damage.Stats.invalidate(p);
	}

	/**
	 * A Berserk's extra swing range (MAP.md §1.14): +5, or +5.5 when it is the only Berserk in the party.
	 * The same figure extends its Cleave radius (§7), so the two move together - {@code entity_interaction_range}
	 * 3.0 to 8.0, Cleave radius 4.8 to 9.8 - which is why both read it from
	 * {@code damage.ClassBonuses.swingRange} rather than each carrying its own number.
	 * <p>
	 * Applied as a named modifier so re-applying it is idempotent and switching class removes it.
	 */
	public static void applySwingRange(Player p) {
		var attr = p.getAttribute(org.bukkit.attribute.Attribute.ENTITY_INTERACTION_RANGE);
		if(attr == null) return;
		org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(M7tas.getInstance(), "swing_range");
		attr.removeModifier(new org.bukkit.attribute.AttributeModifier(key, 0,
				org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,
				org.bukkit.inventory.EquipmentSlotGroup.ANY));
		double bonus = damage.ClassBonuses.swingRange(damage.DungeonClass.of(p), damage.DungeonClass.isSoloOnClass(p));
		if(bonus > 0) {
			attr.addModifier(new org.bukkit.attribute.AttributeModifier(key, bonus,
					org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,
					org.bukkit.inventory.EquipmentSlotGroup.ANY));
		}
	}

	/** On-disk shape - MUST match the network plugin's {@code Loadouts.LoadoutFile} (UUID is the filename). */
	public static class LoadoutFile {
		public String selectedClass;
		public Map<String, List<String>> perClass = new LinkedHashMap<>();
	}
}
