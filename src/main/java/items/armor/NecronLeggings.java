package items.armor;

import damage.Rarity;
import damage.ReforgeId;
import items.ItemFactory;
import items.Wearable;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Necron's Leggings, a dyed leather piece of the Necron set (§1.10). */
public final class NecronLeggings implements Wearable {
	public static final NecronLeggings INSTANCE = new NecronLeggings();

	private NecronLeggings() {}

	@Override
	public Material material() {
		return Material.LEATHER_LEGGINGS;
	}

	@Override
	public String baseName() {
		return "Necron's Leggings";
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
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.leather(material(), Color.fromRGB(231, 92, 60), colouredName(reforge));
	}

	@Override
	public EquipmentSlot slot() {
		return EquipmentSlot.LEGS;
	}
}
