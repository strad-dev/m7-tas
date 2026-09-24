package items.armor;

import damage.Rarity;
import damage.ReforgeId;
import items.ItemFactory;
import items.Wearable;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Renowned Thermodynamic piece. No stats; 4/4 is a RATE bonus: the Terminator's 5-tick cooldown becomes 4. The old
 * 97.5% damage penalty is deleted.
 */
public final class ThermodynamicLeggings implements Wearable {
	public static final ThermodynamicLeggings INSTANCE = new ThermodynamicLeggings();

	private ThermodynamicLeggings() {}

	@Override
	public Material material() {
		return Material.LEATHER_LEGGINGS;
	}

	@Override
	public String baseName() {
		return "Thermodynamic Leggings";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.LEGENDARY;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.RENOWNED;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.leather(material(), Color.fromRGB(255, 112, 10), colouredName(reforge));
	}

	@Override
	public EquipmentSlot slot() {
		return EquipmentSlot.LEGS;
	}

	@Override
	public String setId() {
		return ThermodynamicHelmet.THERMODYNAMIC;
	}
}
