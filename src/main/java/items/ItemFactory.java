package items;

import net.minecraft.nbt.CompoundTag;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import plugin.FakePlayerInventory;
import plugin.Utils;

import java.util.function.Consumer;

/**
 * The three ways an {@code Item} builds its stack, in one place so an item class reads as its own definition
 * rather than as plumbing.
 * <p>
 * Every method here DELEGATES to the existing builder rather than reimplementing it, deliberately: those
 * builders are still called directly from outside {@code items/} (the Maxor Energy Crystal, the SkyBlock Menu
 * that {@code WitherKing} and {@code ClearManager} hand back, the Shadow Assassin Boots), and a second copy of
 * the NBT and lore assembly is exactly the kind of drift that would show up as a saved loadout quietly failing
 * to match its template.
 * <p>
 * <b>Names arrive as MiniMessage and are built the same way each family always was.</b>  The two paths construct
 * subtly different name Components ({@code Utils.mm} wraps in {@code <!italic>}; the leather path round-trips
 * through legacy and sets non-italic explicitly), so they are kept separate on purpose: switching one family to
 * the other would rewrite every existing saved copy of those items on the next refresh.  Harmless, since
 * {@code paletteKey} strips styling, but pointless churn.
 */
public final class ItemFactory {
	private ItemFactory() {}

	/** A custom item with {@code loreId} on lore line 0 and a SkyBlock {@code id} in NBT. */
	public static ItemStack item(Material material, String name, String loreId, String skyblockId) {
		return FakePlayerInventory.getSkyBlockItem(material, name, loreId, skyblockId);
	}

	/** The same, for the few items that need more NBT than a bare {@code id} (the Aspect of the Void). */
	public static ItemStack item(Material material, String name, String loreId, Consumer<CompoundTag> nbt) {
		return FakePlayerInventory.getSkyBlockItem(material, name, loreId, nbt);
	}

	/**
	 * A textured player head.  {@code identifier} seeds the profile UUID and must be STABLE: a random one made
	 * every copy of a head a byte-different stack, which listed the shared heads once per class in the palette.
	 */
	public static ItemStack head(String name, String identifier, String texture, String signature) {
		return FakePlayerInventory.getCustomHead(name, identifier, texture, signature);
	}

	/** A dyed leather armour piece.  Carries no lore ID, so {@code StatLore} leaves lore line 0 blank. */
	public static ItemStack leather(Material material, Color colour, String name) {
		return Utils.createLeatherArmor(material, colour, Utils.mmLegacy(name));
	}
}
