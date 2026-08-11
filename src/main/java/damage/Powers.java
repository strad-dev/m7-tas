package damage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Accessory Powers and tuning points (DAMAGE_PLAN.md §1.12).  Like equipment, accessories are <b>assumed, never
 * items</b>; what matters is the selected Power, whose stats scale off total Magical Power.
 * <p>
 * Everything here derives from ONE input, the accessory list.  Magical Power, the power multiplier and the tuning
 * count all follow from it, so changing an accessory is a one-line edit - §1.12 is explicit that hardcoding 2259
 * or 238 loses exactly that.
 * <p>
 * None of it is dungeon-scaled: accessories are not dungeon items, so no x6.66 and no x1.81.
 */
public final class Powers {
	private Powers() {}

	/**
	 * Total Accessory Power from the profile's accessory bag OUTSIDE a dungeon.  It already includes one copy of
	 * each dungeon accessory below; what those add in a dungeon is a SECOND copy, not a doubling of this figure.
	 */
	private static final double OVERWORLD_ACCESSORY_MP = 2121;

	/**
	 * The dungeon accessories held, with their own Accessory Power.  These are exactly the standard per-rarity
	 * values (Mythic 22, Legendary 16, Epic 12), which is what confirms the reading: "dungeon accessories give
	 * double Accessory Power inside Dungeons" means each contributes its MP again.
	 * <p>
	 * The doubling must be a property of the ACCESSORY, never a global x2 on the total - a blanket x2 would give
	 * 4242 and overshoot by 88%.
	 */
	private static final Map<String, Double> DUNGEON_ACCESSORIES = new LinkedHashMap<>();

	static {
		DUNGEON_ACCESSORIES.put("Auto Recombobulator", 22.0);
		DUNGEON_ACCESSORIES.put("Master Skull Tier VII", 22.0);
		DUNGEON_ACCESSORIES.put("Scarf's Grimoire", 22.0);
		DUNGEON_ACCESSORIES.put("Treasure Artifact", 22.0);
		DUNGEON_ACCESSORIES.put("Wither Relic", 22.0);
		DUNGEON_ACCESSORIES.put("Catacombs Expert Ring", 16.0);
		DUNGEON_ACCESSORIES.put("General's Medallion", 12.0);
	}

	/**
	 * Magical Power in M7.  This plugin only ever runs M7, so the dungeon doubling is ALWAYS active - there is no
	 * overworld value to compute.
	 */
	public static double magicalPower() {
		double bonus = 0;
		for(double mp : DUNGEON_ACCESSORIES.values()) bonus += mp;
		return OVERWORLD_ACCESSORY_MP + bonus;
	}

	/**
	 * The wiki's Accessory Power curve.  Reproduces the published 250 AP row for all three powers and both stats
	 * of Silky's 2000 AP row exactly, which is what recovered the weights below from published totals.
	 */
	public static double multiplier(double magicalPower) {
		return 29.97 * Math.pow(Math.log(0.0019 * magicalPower + 1.0), 1.2);
	}

	/**
	 * An accessory Power.  {@code weights} are per-unit-of-multiplier; {@code unique} is the power's flat bonus,
	 * which <b>does not scale</b> - confirmed against the Eccentric Painting's per-AP table, where Crit Damage,
	 * Intelligence and Strength move with Accessory Power and Ability Damage stays at +5 on every row.
	 */
	public enum Power {
		/** Magma Urchin.  Archer, Berserk, Healer, Tank. */
		HURTFUL(StatBlock.of(Stat.STRENGTH, 4.8, Stat.CRIT_DAMAGE, 19.2), StatBlock.EMPTY),
		/** Luxurious Spool.  The Mage's BEAM power.  Its 0.6 Speed weight is not modelled - nothing reads Speed. */
		SILKY(StatBlock.of(Stat.CRIT_DAMAGE, 22.8), StatBlock.EMPTY),
		/**
		 * Eccentric Painting.  The Mage's ABILITY power, and a real trade rather than a free win: it costs
		 * Strength and Crit Damage to buy Intelligence, which is exactly why it is only assigned to the path that
		 * cannot use those two at all.
		 */
		BIZARRE(StatBlock.of(Stat.INTELLIGENCE, 43.2, Stat.CRIT_DAMAGE, -2.4, Stat.STRENGTH, -2.4),
				StatBlock.of(Stat.ABILITY_DAMAGE, 5));

		private final StatBlock weights;
		private final StatBlock unique;

		Power(StatBlock weights, StatBlock unique) {
			this.weights = weights;
			this.unique = unique;
		}

		/** This power's contribution at the given Magical Power. */
		public StatBlock stats(double magicalPower) {
			return weights.times(multiplier(magicalPower)).plus(unique);
		}
	}

	/**
	 * Which Power a class runs.  The Mage's is path-dependent like its gloves - Silky on the beam, Bizarre on the
	 * ability - which is the second independent reason the stat cache is keyed {@code (player, path)}.
	 */
	public static Power powerFor(DungeonClass clazz, DamagePath path) {
		if(clazz != DungeonClass.MAGE) return Power.HURTFUL;
		return path == DamagePath.ABILITY ? Power.BIZARRE : Power.SILKY;
	}

	/**
	 * Tuning points, DERIVED from Magical Power rather than authored: {@code floor(MP/10) + 13}, the +13 coming
	 * from an attribute.  Feeding MP in is what makes it 238 rather than 225 - the dungeon-accessory bonus grants
	 * its own tuning points, and the extra 138 MP is worth 13 more of them.
	 */
	public static int tuningPoints(double magicalPower) {
		return (int) Math.floor(magicalPower / 10.0) + 13;
	}

	/**
	 * What the tuning points are spent on: all of them on Crit Damage, at 1 point = +1 Crit Damage.  Flat, not
	 * dungeon-scaled, same reasoning as the powers.  Per-point rates for any other stat only matter if points ever
	 * move off Crit Damage, which they do not.
	 */
	public static StatBlock tunings(double magicalPower) {
		return StatBlock.of(Stat.CRIT_DAMAGE, tuningPoints(magicalPower));
	}

	/** The full assumed-accessory contribution for a class on a path: its Power plus its tunings. */
	public static StatBlock forClass(DungeonClass clazz, DamagePath path) {
		double mp = magicalPower();
		return powerFor(clazz, path).stats(mp).plus(tunings(mp));
	}
}
