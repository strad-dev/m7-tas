package items;

import damage.ItemCategory;

/**
 * Swords, axes, wands. SWORD reforge table; swung through the one melee path ({@code CustomItems.meleeAttack}).
 */
public interface Weapon extends Item {

	/** Reforge category, NOT material: the Jerry-chine Gun (horse armour) and all wands read the sword table. */
	default ItemCategory category() {
		return ItemCategory.SWORD;
	}

	/**
	 * A MAGE left-click beams instead of swinging; in a Berserk's hand it's melee.
	 * <p>
	 * Replaced a material test ({@code IRON_SWORD || STONE_SWORD}) plus four lore IDs. Intended change: an
	 * UNREGISTERED iron or stone sword no longer beams.
	 */
	default boolean mageBeams() {
		return false;
	}

	/** False only where the left click is an ability ({@code AbilityItem.suppressesBlockBreak}). */
	default boolean swingsMelee() {
		return true;
	}
}
