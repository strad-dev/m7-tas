package items;

import damage.ItemCategory;

/**
 * A melee-capable item: the swords, the axes and the wands.  Everything here takes the SWORD reforge table and
 * can be swung, which is the one melee damage path ({@code CustomItems.meleeAttack}).
 */
public interface Weapon extends Item {

	/**
	 * The reforge table's category axis, NOT the Bukkit material: the Jerry-chine Gun is a horse-armour "gun" and
	 * still reads the sword table, and so do all the wands.
	 */
	default ItemCategory category() {
		return ItemCategory.SWORD;
	}

	/**
	 * Whether a MAGE left-clicking this fires the mage beam instead of swinging.  The class gate is the whole
	 * point: the same weapon in a Berserk's hand is a melee weapon.
	 * <p>
	 * This replaced a material test ({@code IRON_SWORD || STONE_SWORD}) plus a four-entry set of lore IDs, so the
	 * beam is now a property each weapon declares.  One behaviour change comes with that, and it is intended: an
	 * UNREGISTERED iron or stone sword in a Mage's hand no longer beams.
	 */
	default boolean mageBeams() {
		return false;
	}

	/**
	 * Whether an ordinary attack packet with this in hand lands a melee hit.  False only where the left click is
	 * an ability instead, which {@code AbilityItem.suppressesBlockBreak} already says for the items that have one.
	 */
	default boolean swingsMelee() {
		return true;
	}
}
