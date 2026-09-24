package damage;

import java.util.Locale;

/**
 * A reforge, decoupled from the item (MAP.md §2): {@link ItemDef} stores only this id and numbers come from
 * {@link Reforges}. Non-stat effects (§2.4) live here: {@link #meleeMultiplier()}, {@link #abilityMultiplier()}.
 */
public enum ReforgeId {
	NONE,
	HEROIC,
	PRECISE,
	SUSPICIOUS,
	/**
	 * Real Fabled, assumed to always land its max x1.15. DISPLAYED as "Withered" on purpose (§1.0.6): owner doesn't
	 * want "Fabled" on items. Keep id {@code FABLED}, translate only in {@link #displayName()}. {@link #WITHERED} is a
	 * different reforge sharing the name.
	 */
	FABLED,
	/** Real Withered, on the Ragnarock Axe. Not the {@link #FABLED} alias, so no x1.15 (§1.0.6). */
	WITHERED,
	WARPED,
	ANCIENT,
	NECROTIC,
	LOVING,
	/** Cosmetics and Thermodynamic set. No stats; its buff is +1% additive per piece in {@link Profile} (§1.10, §1.13). */
	RENOWNED,
	/** Was the Rapid Bonemerang's, now gone (§1.9); kept so the name resolves if it returns. */
	RAPID,
	STRENGTHENED,
	BLOODSHOT,
	BRILLIANT,
	MENACING;

	/** Name prefix. Literal except {@link #FABLED} -> "Withered", the one alias (§1.0.6). */
	public String displayName() {
		if(this == NONE) return "";
		if(this == FABLED) return "Withered";
		String n = name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(n.charAt(0)) + n.substring(1);
	}

	/** MELEE hit factor (§7). Fabled's x1.15 assumed to always max-proc, why it was picked. */
	public double meleeMultiplier() {
		return this == FABLED ? 1.15 : 1.0;
	}

	/** ABILITY factor (§1.10, §7). Ability loadout wears the Loving chestplate, so effectively always on. */
	public double abilityMultiplier() {
		return this == LOVING ? 1.05 : 1.0;
	}
}
