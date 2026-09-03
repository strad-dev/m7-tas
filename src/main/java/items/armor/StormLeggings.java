package items.armor;

import damage.Rarity;
import damage.ReforgeId;
import items.ItemFactory;
import items.Wearable;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import plugin.*;

import java.util.*;

/**
 * Storm's Leggings.  Ships in TWO reforges: the Ancient piece the Mage class wears, and the
 * alternate-reforge ("RCM") Necrotic one, which trades Ancient's Strength and Crit Damage
 * for a much larger Intelligence block and runs Sapphire in both slots.
 * <p>
 * They are one item here rather than two, which is the whole point of reforge being a parameter: the colours
 * and the dye are identical, because the reforge is the ONLY difference between them.
 */
public final class StormLeggings implements Wearable {
	public static final StormLeggings INSTANCE = new StormLeggings();

	private StormLeggings() {}

	@Override
	public Material material() {
		return Material.LEATHER_LEGGINGS;
	}

	@Override
	public String baseName() {
		return "Storm's Leggings";
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
		return List.of(ReforgeId.ANCIENT, ReforgeId.NECROTIC);
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.leather(material(), Color.fromRGB(23, 168, 196), colouredName(reforge));
	}

	@Override
	public EquipmentSlot slot() {
		return EquipmentSlot.LEGS;
	}
}
