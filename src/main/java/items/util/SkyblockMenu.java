package items.util;

import damage.Rarity;
import damage.ReforgeId;
import items.Item;
import items.ItemFactory;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * The SkyBlock Menu, pinned to hotbar slot 8 in every kit.  A plain {@link Item}: no ability, no stats, no lore
 * ID, and nothing happens when it is clicked.  It is registered anyway so the four places that used to identify
 * it by comparing a display-name constant can ask the registry instead.
 * <p>
 * Withheld from the loadout palette ({@code Catalog.hiddenFromPalette}), since the editor pins it itself.
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
