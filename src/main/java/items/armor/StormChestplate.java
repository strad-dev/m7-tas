package items.armor;

import damage.Rarity;
import damage.ReforgeId;
import items.ItemFactory;
import items.Wearable;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Storm's Chestplate, in TWO reforges: Ancient (Mage kit) and the alternate ("RCM") Loving, which trades Ancient's Strength
 * and Crit Damage for much more Intelligence and runs Sapphire in both slots. One item, since the reforge is the
 * ONLY difference.
 */
public final class StormChestplate implements Wearable {
	public static final StormChestplate INSTANCE = new StormChestplate();

	private StormChestplate() {}

	@Override
	public Material material() {
		return Material.LEATHER_CHESTPLATE;
	}

	@Override
	public String baseName() {
		return "Storm's Chestplate";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.LEGENDARY;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.ANCIENT;
	}

	@Override
	public List<ReforgeId> reforges() {
		return List.of(ReforgeId.ANCIENT, ReforgeId.LOVING);
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.leather(material(), Color.fromRGB(23, 147, 196), colouredName(reforge));
	}

	@Override
	public EquipmentSlot slot() {
		return EquipmentSlot.CHEST;
	}
}
