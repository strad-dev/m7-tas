package damage;

/**
 * SkyBlock mob type. A mob can carry several and every matching buff stacks (MAP.md §7).
 * <p>
 * The thirteen Ruler types (+39% each) come first; the rest exist because enchants key on them. No Wither Ruler:
 * Wither pays out through Smite and Hyperion x1.5. Skeletal is Normal-mode only and never matches here, but the
 * Ruler exists so it stays.
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
	/** No Ruler; matched by Smite and Hyperion x1.5 (§7). */
	WITHER(false),
	CUBIC(false),
	AQUATIC(false);

	private final boolean hasRuler;

	MobType(boolean hasRuler) {
		this.hasRuler = hasRuler;
	}

	/** A Ruler attribute matches this type, +39% additive (§7). */
	public boolean hasRuler() {
		return hasRuler;
	}
}
