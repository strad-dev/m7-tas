package items.armor;

import damage.Rarity;
import damage.ReforgeId;
import items.ItemFactory;
import items.Wearable;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Renowned Spring Boots. Epic recombobulated, so dark purple (it was wrongly an Epic BASE, gold, until colour was
 * derived from rarity).
 * <p>
 * The only wearable with a lore ID: {@code listeners/SpringBoots} identifies the boots by it. {@code Item} suffix
 * avoids colliding with that listener.
 */
public final class SpringBootsItem implements Wearable {
	public static final SpringBootsItem INSTANCE = new SpringBootsItem();

	private SpringBootsItem() {}

	@Override
	public String loreId() {
		return "skyblock/combat/spring_boots";
	}

	@Override
	public Material material() {
		return Material.CHAINMAIL_BOOTS;
	}

	@Override
	public String baseName() {
		return "Spring Boots";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.RARE;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.RENOWNED;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.item(material(), colouredName(reforge), loreId(), "SPRING_BOOTS");
	}

	@Override
	public EquipmentSlot slot() {
		return EquipmentSlot.FEET;
	}
}
