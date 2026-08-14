package damage;

/**
 * A mob's SkyBlock type.  A mob can carry several, and <b>every matching damage buff applies - they all stack</b>
 * (MAP.md §7).
 * <p>
 * The thirteen types with a Ruler attribute (+39% each) are listed first; WITHER and CUBIC/AQUATIC etc. exist
 * because enchantments key on them even where no Ruler does.  There is deliberately no Wither Ruler: a Wither-type
 * mob pays out through Smite and the Hyperion's x1.5 instead.  Skeletal is Normal-mode only and so never matches
 * anything on this floor, but it stays in the list because the Ruler exists.
 */
public enum MobType {
	SKELETAL(true),
	UNDEAD(true),
	ARTHROPOD(true),
	ENDER(true),
	MAGMATIC(true),
	FROZEN(true),
	AIRBORNE(true),
	ARCANE(true),
	SUBTERRANEAN(true),
	ANIMAL(true),
	CONSTRUCT(true),
	INFERNAL(true),
	HUMANOID(true),
	/** No Ruler attribute exists for Wither; it is matched by Smite and by the Hyperion's x1.5 (§7). */
	WITHER(false),
	CUBIC(false),
	AQUATIC(false);

	private final boolean hasRuler;

	MobType(boolean hasRuler) {
		this.hasRuler = hasRuler;
	}

	/** True if one of the thirteen Ruler attributes matches this type, each worth +39% additive (§7). */
	public boolean hasRuler() {
		return hasRuler;
	}
}
