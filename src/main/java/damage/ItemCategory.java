package damage;

/**
 * Reforge table's category axis (§2.1): Hypixel keys reforges on {@code (reforgeId, category, rarity)}, so one
 * reforge name can hold different numbers per category.
 */
public enum ItemCategory {
	/** Anything that hits in melee (Ragnarock Axe, Flaming Flay, wands). */
	SWORD,
	/** Bows run Duplex rather than Chimera and miss most sword damage enchants (§7). */
	RANGED,
	ARMOR,
	/** Necklace / cloak / belt / gloves. Never items; see §1.11 and {@link Equipment}. */
	EQUIPMENT
}
