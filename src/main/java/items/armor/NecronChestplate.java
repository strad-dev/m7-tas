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

/** Necron's Chestplate, a dyed leather piece of the Necron set (§1.10). */
public final class NecronChestplate implements Wearable {
	public static final NecronChestplate INSTANCE = new NecronChestplate();

	private NecronChestplate() {}

	@Override
	public Material material() {
		return Material.LEATHER_CHESTPLATE;
	}

	@Override
	public String baseName() {
		return "Necron's Chestplate";
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
		return ItemFactory.leather(material(), Color.fromRGB(231, 65, 80), colouredName(reforge));
	}

	@Override
	public EquipmentSlot slot() {
		return EquipmentSlot.CHEST;
	}
}
