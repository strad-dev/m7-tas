package damage;

import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

/**
 * The equipped pet.  Pets are assumed rather than owned (MAP.md §1.13): a player never picks one, it
 * follows from what they are doing and what they are wearing.
 * <p>
 * A pet contributes in three separate places, and they are NOT the same numbers:
 * <ul>
 *   <li>{@link #ownStats()} - profile-level base stats.  Not cata-scaled, because a pet is not a dungeon item.</li>
 *   <li>{@link #statAdditive(Stat)} - the pet's additive % on a stat, e.g. the Golden Dragon's +5% Strength.</li>
 *   <li>{@link #damageAdditive} - its damage-level additive %, e.g. the Golden Dragon's +250% (§7).</li>
 * </ul>
 * <b>Chimera stacks with the pet, it does not replace it</b> (§1.13): Chimera V copies a pet's BASE stats at 100%,
 * so a Golden Dragon gives +300 Strength and the weapon's Chimera copies another +300 - 600 in total.  It does not
 * copy the additive %, so there is no second +5%.  Because Chimera's copy lands on the WEAPON it is cata-scaled
 * (x6.66) while the pet's own stats are not, which is the whole reason the Mage wants the Golden Dragon and the
 * Archer (whose bow runs Duplex, not Chimera) is nearly indifferent - see §7's pet cross-check.
 */
public enum Pet {
	/** The default (§1.13). */
	GOLDEN_DRAGON(StatBlock.of(Stat.STRENGTH, 300)),
	/** The ability pet.  Note this is why neither dragon's damage additive is live on a cast (§7). */
	CROW(StatBlock.of(Stat.INTELLIGENCE, 225, Stat.ABILITY_DAMAGE, 30)),
	/** Archer / Berserk during the Wither King phase. */
	ENDER_DRAGON(StatBlock.of(Stat.STRENGTH, 50, Stat.CRIT_DAMAGE, 60)),
	/** Worn with a Racing Helmet or Cow Hat.  Nothing damage-relevant, which is now the entire cost of a hat. */
	BLACK_CAT(StatBlock.EMPTY);

	private final StatBlock ownStats;

	Pet(StatBlock ownStats) {
		this.ownStats = ownStats;
	}

	/** The pet's own base stats, at profile level, so NOT cata-scaled. */
	public StatBlock ownStats() {
		return ownStats;
	}

	/** What Chimera V copies: the pet's base stats at 100%, landing on the weapon (§1.13, §2). */
	public StatBlock chimeraCopy() {
		return ownStats;
	}

	/** This pet's additive % on one STAT (§1.13's additive tables), e.g. +5 meaning +5%. */
	public double statAdditive(Stat stat) {
		return switch(this) {
			case GOLDEN_DRAGON -> stat == Stat.STRENGTH ? 5.0 : 0.0;
			case ENDER_DRAGON -> (stat == Stat.STRENGTH || stat == Stat.CRIT_DAMAGE) ? 10.0 : 0.0;
			default -> 0.0;
		};
	}

	/**
	 * This pet's additive % on a HIT (§7's misc table).  The Ender Dragon's +200% is Ender-only, so it needs the
	 * target; the Golden Dragon's +250% is unconditional.
	 */
	public double damageAdditive(java.util.Set<MobType> targetTypes) {
		return switch(this) {
			case GOLDEN_DRAGON -> 250.0;
			case ENDER_DRAGON -> targetTypes.contains(MobType.ENDER) ? 200.0 : 0.0;
			default -> 0.0;
		};
	}

	/**
	 * Which pet a player is assumed to have out right now, per §1.13's table.  Evaluated live rather than stored,
	 * because every input to it (the damage path, the phase, the worn helmet) changes mid-run.
	 * <p>
	 * Order matters: a hat overrides everything (it is the reason the Black Cat is out at all), then a cast, then
	 * the Wither King phase for the two classes that swap, then the default.
	 */
	public static Pet forPlayer(Player p, DamagePath path) {
		PlayerInventory inv = p.getInventory();
		if(plugin.FakePlayerInventory.isRacingHelmet(inv.getHelmet()) || plugin.FakePlayerInventory.isCowHat(inv.getHelmet())) {
			return BLACK_CAT;
		}
		if(path == DamagePath.ABILITY) return CROW;
		DungeonClass clazz = DungeonClass.of(p);
		if((clazz == DungeonClass.ARCHER || clazz == DungeonClass.BERSERK) && MobStats.witherKingPhaseActive()) {
			return ENDER_DRAGON;
		}
		return GOLDEN_DRAGON;
	}
}
