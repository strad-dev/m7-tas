package items.tools;

import damage.Rarity;
import damage.ReforgeId;
import items.AbilityItem;
import items.ItemFactory;
import items.ItemUtils;
import items.Tool;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import plugin.*;

import java.util.*;

/**
 * The Dungeonbreaker.  Not an ability item at all: its whole behaviour is that a block it breaks is removed
 * TEMPORARILY and without physics, and restored 200 ticks later ({@code ItemUtils.stonk}).
 * <p>
 * Three plumbing rules follow from it being a pickaxe rather than a weapon: its right-click is not cancelled
 * (so it still interacts with the world), it may still right-click an entity, and it is the one item that keeps
 * working inside the Trap room, where the clear phase disables right-click abilities.
 * <p>
 * <b>Its lore ID is {@code skyblock/combat/stonk}</b>, not {@code .../dungeonbreaker}.  That second ID appeared
 * in two of the old exemption lists and no item ever carried it; it is dropped here rather than carried over.
 */
public final class Dungeonbreaker implements Tool, AbilityItem {
	public static final Dungeonbreaker INSTANCE = new Dungeonbreaker();

	private Dungeonbreaker() {}

	@Override
	public String loreId() {
		return "skyblock/combat/stonk";
	}

	@Override
	public Material material() {
		return Material.DIAMOND_PICKAXE;
	}

	@Override
	public String baseName() {
		return "Dungeonbreaker";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.SPECIAL;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.NONE;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return buildPickaxe(colouredName(reforge));
	}

	@Override
	public boolean cancelsInteract() {
		return false;
	}

	@Override
	public boolean allowsEntityInteract() {
		return true;
	}

	@Override
	public boolean usableInTrapRoom() {
		return true;
	}

	@Override
	public boolean onBreak(Player p, Block block) {
		ItemUtils.stonk(p, block); // no-physics removal, restored after 200 ticks
		return true;
	}

	/**
	 * Efficiency 255 plus a flat +1024 block-break speed, so a stonk is instant, and the can-break-anything
	 * stamp LAST, since that mutates the NMS copy directly and must follow every {@code setItemMeta}.
	 */
	private static ItemStack buildPickaxe(String name) {
		ItemStack pickaxe = ItemFactory.item(Material.DIAMOND_PICKAXE, name, "skyblock/combat/stonk", "DUNGEONBREAKER");
		pickaxe.addUnsafeEnchantment(Enchantment.EFFICIENCY, 255);
		ItemMeta meta = pickaxe.getItemMeta();
		meta.addAttributeModifier(Attribute.BLOCK_BREAK_SPEED, new AttributeModifier(
				new NamespacedKey(M7tas.getInstance(), "stonk"), 1024,
				AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
		pickaxe.setItemMeta(meta);
		return Utils.breakAnyBlockInAdventure(pickaxe);
	}

}
