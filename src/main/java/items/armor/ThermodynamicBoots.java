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
 * A Renowned Thermodynamic piece.  The set grants no stats; its 4/4 bonus is a RATE one - it raises the
 * attack-speed cap, which in this plugin is the Terminator's 5-tick cooldown becoming 4.  Deliberately not a
 * per-hit multiplier: the old 97.5% damage penalty that used to accompany it is deleted.
 */
public final class ThermodynamicBoots implements Wearable {
	public static final ThermodynamicBoots INSTANCE = new ThermodynamicBoots();

	private ThermodynamicBoots() {}

	@Override
	public Material material() {
		return Material.LEATHER_BOOTS;
	}

	@Override
	public String baseName() {
		return "Thermodynamic Boots";
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
		return EquipmentSlot.FEET;
	}

	@Override
	public String setId() {
		return ThermodynamicHelmet.THERMODYNAMIC;
	}
}
