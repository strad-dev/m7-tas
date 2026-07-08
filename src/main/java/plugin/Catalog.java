package plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Exports the M7 item catalog to the shared data folder ({@code ../data/m7-item-catalog.json}) on
 * plugin enable, so the network plugin's lobby loadout editor can load the real M7 items (palette)
 * and each class's default kit. M7 is the SOLE writer of this file - it is the single source of
 * truth for item definitions; the network plugin only reads it.
 *
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
			LinkedHashSet<String> palette = new LinkedHashSet<>();

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
					if (b64 != null) palette.add(b64); // dedupe identical items across classes
				}
				f.defaults.put(role, ser);
			}
			f.palette = new ArrayList<>(palette);

			Path file = dataDir().resolve("m7-item-catalog.json");
			save(file, f);
			M7tas.getInstance().getLogger().info("Exported M7 item catalog (" + f.palette.size()
					+ " palette items) to " + file);
		} catch (Exception e) {
			M7tas.getInstance().getLogger().warning("Failed to export M7 item catalog: " + e);
		}
	}

	/** Terminator Power per class, matching the TAS / GetCustomItems (Archer 66, Berserk/Healer/Tank 16, Mage none). */
	private static int terminatorPower(String role) {
		return switch (role) {
			case "Archer" -> 66;
			case "Berserk", "Healer", "Tank" -> 16;
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
