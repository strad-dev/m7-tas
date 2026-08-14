package damage;

import java.util.Locale;

/**
 * A reforge.  MAP.md §2 requires the reforge to be decoupled from the item: an {@link ItemDef} stores this
 * id and nothing else, and the numbers come from {@link Reforges} at lookup time, so re-reforging an item is a
 * one-word change and correcting a table cell fixes every item that uses it at once.
 * <p>
 * A reforge may also grant a NON-stat effect (§2.4), which is why {@link #meleeMultiplier()} and
 * {@link #abilityMultiplier()} live here alongside the stat table.
 */
public enum ReforgeId {
	NONE,
	HEROIC,
	PRECISE,
	SUSPICIOUS,
	/**
	 * The real Fabled reforge - chosen because you can assume it always lands its max x1.15 proc.
	 * <p>
	 * <b>It is DISPLAYED as "Withered"</b> (§1.0.6), which is a deliberate naming choice and not an oversight:
	 * the owner does not want "Fabled" on the items.  Keep the id {@code FABLED} everywhere and translate only at
	 * the display layer, via {@link #displayName()}.  {@link #WITHERED} below is a genuinely different reforge that
	 * happens to share the alias's name.
	 */
	FABLED,
	/**
	 * The real Withered reforge, which the Ragnarock Axe carries.  <b>Not</b> the {@link #FABLED} alias, so it does
	 * NOT get Fabled's x1.15 (§1.0.6).
	 */
	WITHERED,
	WARPED,
	ANCIENT,
	NECROTIC,
	LOVING,
	/** Worn on the cosmetics and the Thermodynamic set.  Grants no stats here; its buff is the +1% additive per
	 *  piece worn, applied at the stat level in {@link Profile} (§1.10, §1.13). */
	RENOWNED,
	/** Was the Rapid Bonemerang's.  That item is gone (§1.9); kept only so the name resolves if it returns. */
	RAPID,
	STRENGTHENED,
	BLOODSHOT,
	BRILLIANT,
	MENACING;

	/**
	 * The word that appears in front of the item's name.  Every reforge is displayed literally except
	 * {@link #FABLED}, which is displayed as "Withered" - the one alias (§1.0.6).
	 */
	public String displayName() {
		if(this == NONE) return "";
		if(this == FABLED) return "Withered";
		String n = name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(n.charAt(0)) + n.substring(1);
	}

	/**
	 * Multiplicative factor this reforge puts on a MELEE hit (§7).  Fabled's x1.15 is assumed to always land its
	 * max proc, which is the whole reason that reforge was picked.
	 */
	public double meleeMultiplier() {
		return this == FABLED ? 1.15 : 1.0;
	}

	/**
	 * Multiplicative factor this reforge puts on an ABILITY (§1.10, §7).  Loving is the first reforge here to grant
	 * a non-stat effect, and the ability loadout wears the Loving chestplate, so in practice it is always on.
	 */
	public double abilityMultiplier() {
		return this == LOVING ? 1.05 : 1.0;
	}
}
