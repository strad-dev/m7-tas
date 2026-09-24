package items.util;

import damage.Rarity;
import damage.ReforgeId;
import items.Item;
import items.ItemFactory;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Hotbar slot 8 in every kit. No ability, stats or lore ID; registered so four places can ask the registry instead
 * of comparing display names. Hidden from the palette ({@code Catalog.hiddenFromPalette}); the editor pins it.
 */
public final class SkyblockMenu implements Item {
	public static final SkyblockMenu INSTANCE = new SkyblockMenu();

	private SkyblockMenu() {}

	@Override
	public Material material() {
		return Material.NETHER_STAR;
	}

	@Override
	public String baseName() {
		return "SkyBlock Menu (Click)";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.COMMON;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.NONE;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.item(material(), colouredName(reforge), loreId(), "SKYBLOCK_MENU");
	}
}
