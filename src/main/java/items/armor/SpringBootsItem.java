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
 * The Renowned Spring Boots.  Epic once recombobulated, hence dark purple - it was registered as an Epic BASE
 * (so Legendary, gold) before the colour started being derived from the rarity, which is how that got noticed.
 * <p>
 * The only wearable that carries a lore ID, because the charge-and-launch mechanic in
 * {@code listeners/SpringBoots} identifies the boots by it.  Class name has the {@code Item} suffix purely to
 * avoid colliding with that listener.
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
