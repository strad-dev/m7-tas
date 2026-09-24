package items.util;

import damage.Rarity;
import damage.ReforgeId;
import items.Item;
import items.ItemFactory;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** No lore ID, no stats: a movement toy. */
public final class PitchinRod implements Item {
	public static final PitchinRod INSTANCE = new PitchinRod();

	private PitchinRod() {}

	@Override
	public Material material() {
		return Material.FISHING_ROD;
	}

	@Override
	public String baseName() {
		return "Pitchin' Rod of the Sea";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.LEGENDARY;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.NONE;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.item(material(), colouredName(reforge), loreId(), "ROD_OF_THE_SEA");
	}
}
