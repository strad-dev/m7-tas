package items.tools;

import damage.Rarity;
import damage.ReforgeId;
import items.AbilityItem;
import items.ItemFactory;
import items.ItemUtils;
import items.Tool;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import plugin.M7tas;
import plugin.Utils;

/**
 * No ability: a broken block is removed TEMPORARILY without physics and restored 200 ticks later
 * ({@code ItemUtils.stonk}). As a pickaxe its right-click isn't cancelled, it may right-click entities, and it's the
 * one item that works in the Trap room.
 * <p>
 * Lore ID is {@code skyblock/combat/stonk}. The {@code .../dungeonbreaker} ID in two old exemption lists was never
 * carried by any item and is dropped.
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
	 * Efficiency 255 + flat +1024 break speed, so stonks are instant. Can-break stamp LAST: it mutates the NMS copy
	 * and must follow every {@code setItemMeta}.
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
