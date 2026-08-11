package damage;

/**
 * The reforge table's category axis (§2.1): Hypixel keys a reforge's values on
 * {@code (reforgeId, item category, rarity)}, so the sword, ranged, armour and equipment tables can hold different
 * numbers for the same reforge name without colliding.
 */
public enum ItemCategory {
	/** Swords, and everything else that hits in melee (the Ragnarock Axe, the Flaming Flay, the wands). */
	SWORD,
	/** Bows.  Note a bow runs Duplex rather than Chimera, and misses most of the sword damage enchantments (§7). */
	RANGED,
	ARMOR,
	/** Necklace / cloak / belt / gloves.  Never modelled as items; see §1.11 and {@link Equipment}. */
	EQUIPMENT
}
