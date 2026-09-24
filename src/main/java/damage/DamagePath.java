package damage;

/**
 * Which of the four damage formulas a hit uses (MAP.md §7). Also half the stat cache key: a Mage's equipment and
 * Accessory Power are path-dependent (§1.11, §1.12), so Strength, Crit Damage, Int AND Ability Damage differ between
 * beam and cast.
 */
public enum DamagePath {
	MELEE,
	/** Mage Staff passive: melee becomes ranged at a rescaled share. Still melee for enchants, full sword list (§7). */
	BEAM,
	BOW,
	ABILITY;

	/** The two paths the sword enchant list applies to. */
	public boolean isMelee() {
		return this == MELEE || this == BEAM;
	}
}
