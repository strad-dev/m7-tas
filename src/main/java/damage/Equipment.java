package damage;

/**
 * Equipment (necklace / cloak / belt / gloves) - <b>assumed, never an item</b> (DAMAGE_PLAN.md §1.11).  There are
 * no ItemStacks, no inventory slots and no palette entries for these; each class is simply taken to be wearing the
 * right ones and their stats are added to the aggregate as a flat per-class source.
 * <p>
 * The Mage's set is <b>path-dependent</b>, which is one of the two reasons the stat cache is keyed
 * {@code (player, path)} rather than {@code player}: the Soulweaver Gloves (beam) and the Manticore Claw (ability)
 * are the same slot and can never both apply, and the Balloon Snake changes reforge with them.
 * <p>
 * Values here are the plain SkyBlock numbers; the x6.66 is applied as a stage, exactly as for items.  The one
 * exception is the Manticore Claw, which is not a dungeon item, so its 20 / 37.5 / 3 stay flat - and its Ability
 * Damage is not starred either, so it does not take the x1.81 either.
 * <p>
 * Per-piece reforge terms are authored inline rather than looked up.  §2.1 rules the equipment reforge table
 * "only needed if equipment reforges ever change", because §1.11 already publishes the resolved per-piece values -
 * they differ per piece (a Strengthened Bone Necklace grants +6 Strength where a Strengthened cloak grants +7)
 * since the pieces differ in rarity, and those rarities are not published.  Each number still appears exactly once.
 */
public final class Equipment {
	private Equipment() {}

	/**
	 * The equipment stat block for a class on a given damage path, already scaled.
	 * <p>
	 * The Adaptive Belt is worn by EVERY class including the Mage, and its ability grants a different stat per
	 * class, which is why the belt is built per class rather than shared.
	 */
	public static StatBlock forClass(DungeonClass clazz, DamagePath path) {
		return clazz == DungeonClass.MAGE ? mage(path) : martial(clazz);
	}

	/** Archer / Berserk / Healer / Tank: one shared set, differing only in what the Adaptive Belt grants. */
	private static StatBlock martial(DungeonClass clazz) {
		StatBlock scaled = StatBlock.EMPTY
				// Bone Necklace (Strengthened): 18 The One + 6 reforge
				.plus(Stat.STRENGTH, 18 + 6)
				// Shadow Assassin Cloak (Strengthened): 25 base + 7 reforge
				.plus(Stat.STRENGTH, 25 + 7)
				// Adaptive Belt (Bloodshot): 10 base + 5 reforge, plus the class ability below
				.plus(Stat.STRENGTH, 10 + 5)
				// Soulweaver Gloves (Strengthened): 10 base + 6 reforge Strength, 10 base Crit Damage
				.plus(Stat.STRENGTH, 10 + 6)
				.plus(Stat.CRIT_DAMAGE, 10);
		scaled = switch(clazz) {
			case BERSERK -> scaled.plus(Stat.STRENGTH, 10);      // belt ability
			case ARCHER -> scaled.plus(Stat.CRIT_DAMAGE, 5);     // belt ability
			default -> scaled;                                   // Healer and Tank: nothing damage-relevant
		};
		return scaled.scaled(Scale.SB_CATA_MULT, Scale.SB_STAR_MULT);
	}

	/**
	 * The Mage's two sets.  The beam set trades the ability set's Intelligence for Crit Damage, which is exactly
	 * what the beam formula wants and the ability formula cannot use at all (§7).
	 */
	private static StatBlock mage(DamagePath path) {
		boolean ability = path == DamagePath.ABILITY;
		StatBlock scaled = StatBlock.EMPTY
				// Shadow Assassin Cloak (Brilliant), same on both paths: 25 base Strength, 15 reforge Intelligence
				.plus(Stat.STRENGTH, 25)
				.plus(Stat.INTELLIGENCE, 15)
				// Adaptive Belt (Bloodshot): 10 base + 5 reforge Strength, and its Mage ability is +25 Intelligence
				.plus(Stat.STRENGTH, 10 + 5)
				.plus(Stat.INTELLIGENCE, 25)
				// Balloon Snake: 18 The One Strength either way, then the reforge splits the two sets
				.plus(Stat.STRENGTH, 18);
		if(ability) {
			// Brilliant snake: 10 base + 9 reforge Intelligence
			scaled = scaled.plus(Stat.INTELLIGENCE, 10 + 9);
		} else {
			// Menacing snake: 10 base Intelligence, 4 reforge Crit Damage
			scaled = scaled.plus(Stat.INTELLIGENCE, 10).plus(Stat.CRIT_DAMAGE, 4);
			// Soulweaver Gloves (Brilliant), the BEAM half of the shared gloves slot
			scaled = scaled.plus(Stat.STRENGTH, 10).plus(Stat.CRIT_DAMAGE, 10).plus(Stat.INTELLIGENCE, 12);
		}
		StatBlock out = scaled.scaled(Scale.SB_CATA_MULT, Scale.SB_STAR_MULT);
		if(ability) {
			// Manticore Claw (Brilliant), the ABILITY half of the gloves slot.  NOT a dungeon item, so these
			// three numbers are flat: 20 Strength, 22.5 ability + 15 reforge Intelligence, 3 Ability Damage.
			out = out.plus(Stat.STRENGTH, 20)
					.plus(Stat.INTELLIGENCE, 22.5 + 15)
					.plus(Stat.ABILITY_DAMAGE, 3);
		}
		return out;
	}
}
