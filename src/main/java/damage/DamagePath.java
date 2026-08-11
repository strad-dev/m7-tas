package damage;

/**
 * Which of the four damage formulas a hit is going through (DAMAGE_PLAN.md §7).
 * <p>
 * The path is also half the stat cache key, not just a formula selector: a Mage's equipment and Accessory Power
 * are path-dependent (§1.11, §1.12), so the same Mage has different Strength, Crit Damage, Intelligence AND
 * Ability Damage depending on whether they are computing a beam or a cast.
 */
public enum DamagePath {
	MELEE,
	/**
	 * The Mage class's "Mage Staff" passive: every melee attack becomes ranged and deals a rescaled share of the
	 * melee number.  It is <b>not</b> a separate damage path in the enchantment sense - the beam counts as a melee
	 * attack and takes the whole sword list (§7).
	 */
	BEAM,
	BOW,
	ABILITY;

	/** True for the two paths the sword enchantment list applies to.  The beam is melee (§7). */
	public boolean isMelee() {
		return this == MELEE || this == BEAM;
	}
}
