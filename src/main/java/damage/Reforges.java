package damage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The reforge table, keyed {@code (reforgeId, category, rarity)} exactly as Hypixel computes it (DAMAGE_PLAN.md
 * §2.1) - <b>not</b> a per-item constant.  Correcting one cell here fixes every item that uses it.
 * <p>
 * The Heroic / Suspicious / Precise rows are the wiki's full tables.  They reproduce §1's per-item reforge terms
 * exactly (Heroic @ Mythic = the Hyperion's +50/+125, Heroic @ Legendary = the Ice Spray's +40/+100, Precise @
 * Mythic = the Terminator's and Last Breath's +34/+70), which is the check that the split is right.
 * <p>
 * The other reforges are pinned by §1 at ONE rarity each, because that is the only rarity anything wears them at.
 * Those rows are authored at that rarity only; asking for another logs once rather than silently answering zero,
 * so an unauthored cell shows up as a warning instead of as a quietly wrong number.
 * <p>
 * Attack Speed and the reforges' Crit Chance are deliberately not modelled beyond Crit Chance being stored: §7
 * rules every hit a crit, and the attack-speed cap comes from the Thermodynamic set (§1.10), not from a reforge.
 */
public final class Reforges {
	private Reforges() {}

	private record Key(ReforgeId reforge, ItemCategory category, Rarity rarity) {}

	private static final Map<Key, StatBlock> TABLE = new HashMap<>();
	/** Cells already warned about, so a missing row costs one line, not one per hit. */
	private static final Set<Key> WARNED = new HashSet<>();

	private static void put(ReforgeId r, ItemCategory c, Rarity rarity, StatBlock stats) {
		TABLE.put(new Key(r, c, rarity), stats);
	}

	static {
		// ===== Wiki tables: Reforging/Sword and Fishing Rod, Reforging/Ranged Weapon (§2.1) =====
		put(ReforgeId.HEROIC, ItemCategory.SWORD, Rarity.RARE, StatBlock.of(Stat.STRENGTH, 25, Stat.INTELLIGENCE, 65));
		put(ReforgeId.HEROIC, ItemCategory.SWORD, Rarity.EPIC, StatBlock.of(Stat.STRENGTH, 32, Stat.INTELLIGENCE, 80));
		put(ReforgeId.HEROIC, ItemCategory.SWORD, Rarity.LEGENDARY, StatBlock.of(Stat.STRENGTH, 40, Stat.INTELLIGENCE, 100));
		put(ReforgeId.HEROIC, ItemCategory.SWORD, Rarity.MYTHIC, StatBlock.of(Stat.STRENGTH, 50, Stat.INTELLIGENCE, 125));

		// Suspicious additionally grants a flat +15 Damage at EVERY rarity, and it lands in the Damage stat rather
		// than as a multiplier (§1.9).
		put(ReforgeId.SUSPICIOUS, ItemCategory.SWORD, Rarity.RARE, StatBlock.of(Stat.CRIT_DAMAGE, 50, Stat.CRIT_CHANCE, 3, Stat.DAMAGE, 15));
		put(ReforgeId.SUSPICIOUS, ItemCategory.SWORD, Rarity.EPIC, StatBlock.of(Stat.CRIT_DAMAGE, 65, Stat.CRIT_CHANCE, 5, Stat.DAMAGE, 15));
		put(ReforgeId.SUSPICIOUS, ItemCategory.SWORD, Rarity.LEGENDARY, StatBlock.of(Stat.CRIT_DAMAGE, 85, Stat.CRIT_CHANCE, 7, Stat.DAMAGE, 15));
		put(ReforgeId.SUSPICIOUS, ItemCategory.SWORD, Rarity.MYTHIC, StatBlock.of(Stat.CRIT_DAMAGE, 110, Stat.CRIT_CHANCE, 10, Stat.DAMAGE, 15));

		put(ReforgeId.PRECISE, ItemCategory.RANGED, Rarity.EPIC, StatBlock.of(Stat.STRENGTH, 18, Stat.CRIT_DAMAGE, 32, Stat.CRIT_CHANCE, 11));
		put(ReforgeId.PRECISE, ItemCategory.RANGED, Rarity.LEGENDARY, StatBlock.of(Stat.STRENGTH, 25, Stat.CRIT_DAMAGE, 50, Stat.CRIT_CHANCE, 13));
		put(ReforgeId.PRECISE, ItemCategory.RANGED, Rarity.MYTHIC, StatBlock.of(Stat.STRENGTH, 34, Stat.CRIT_DAMAGE, 70, Stat.CRIT_CHANCE, 15));

		// ===== Pinned by §1 at one rarity each =====
		// Fabled @ Mythic: the Hyperion's, Dark Claymore's and Flaming Flay's +75 Strength / +50 Crit Damage.
		put(ReforgeId.FABLED, ItemCategory.SWORD, Rarity.MYTHIC, StatBlock.of(Stat.STRENGTH, 75, Stat.CRIT_DAMAGE, 50));
		// Withered @ Epic: the Ragnarock Axe's +160 Strength.  A DIFFERENT reforge from the Fabled alias (§1.0.6).
		put(ReforgeId.WITHERED, ItemCategory.SWORD, Rarity.EPIC, StatBlock.of(Stat.STRENGTH, 160));
		// Warped @ Legendary: the Aspect of the Void's +165 Damage / +165 Strength / +150 Intelligence.
		put(ReforgeId.WARPED, ItemCategory.SWORD, Rarity.LEGENDARY,
				StatBlock.of(Stat.DAMAGE, 165, Stat.STRENGTH, 165, Stat.INTELLIGENCE, 150));

		// Armour reforges.  Ancient is rarity-keyed like everything else, which is what explains the Spirit Mask
		// (Mythic) getting 35/50/25 where Bonzo's Mask (Epic) gets 18/50/16 - see §1.10.
		put(ReforgeId.ANCIENT, ItemCategory.ARMOR, Rarity.MYTHIC,
				StatBlock.of(Stat.STRENGTH, 35, Stat.CRIT_DAMAGE, 50, Stat.INTELLIGENCE, 25));
		put(ReforgeId.ANCIENT, ItemCategory.ARMOR, Rarity.EPIC,
				StatBlock.of(Stat.STRENGTH, 18, Stat.CRIT_DAMAGE, 50, Stat.INTELLIGENCE, 16));
		put(ReforgeId.NECROTIC, ItemCategory.ARMOR, Rarity.LEGENDARY, StatBlock.of(Stat.INTELLIGENCE, 150));
		put(ReforgeId.NECROTIC, ItemCategory.ARMOR, Rarity.MYTHIC, StatBlock.of(Stat.INTELLIGENCE, 200));
		put(ReforgeId.LOVING, ItemCategory.ARMOR, Rarity.MYTHIC, StatBlock.of(Stat.INTELLIGENCE, 120));
		// Renowned grants nothing here on purpose: §1.10 rules the cosmetics' and Thermodynamic's own stats to
		// zero at this scale, and Renowned's real buff is the +1% additive per piece worn, applied in Profile.
		for(Rarity r : Rarity.values()) put(ReforgeId.RENOWNED, ItemCategory.ARMOR, r, StatBlock.EMPTY);

		for(Rarity r : Rarity.values()) put(ReforgeId.NONE, ItemCategory.SWORD, r, StatBlock.EMPTY);
		for(Rarity r : Rarity.values()) put(ReforgeId.NONE, ItemCategory.RANGED, r, StatBlock.EMPTY);
		for(Rarity r : Rarity.values()) put(ReforgeId.NONE, ItemCategory.ARMOR, r, StatBlock.EMPTY);
		for(Rarity r : Rarity.values()) put(ReforgeId.NONE, ItemCategory.EQUIPMENT, r, StatBlock.EMPTY);
	}

	/**
	 * The stats a reforge grants at a given category and rarity.  Read this at the item's EFFECTIVE (recombed)
	 * rarity, never its base - §1.0.9, and {@link ItemDef#rarity()} already does that for you.
	 */
	public static StatBlock stats(ReforgeId reforge, ItemCategory category, Rarity rarity) {
		Key key = new Key(reforge, category, rarity);
		StatBlock found = TABLE.get(key);
		if(found != null) return found;
		if(WARNED.add(key)) {
			plugin.M7tas.getInstance().getLogger().warning(
					"No reforge row for " + reforge + " / " + category + " / " + rarity
							+ " - treating it as no stats.  Add the row to damage/Reforges if this combination is real.");
		}
		return StatBlock.EMPTY;
	}
}
