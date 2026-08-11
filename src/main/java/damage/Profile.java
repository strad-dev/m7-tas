package damage;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import plugin.Utils;

/**
 * Everything outside the item / equipment / power layers (DAMAGE_PLAN.md §1.13): skills, slayers, potions,
 * essence-shop perks, individual accessories' own stats, blessings and pets.
 * <p>
 * <b>The stat pipeline.</b> Each stat has its own additive and multiplicative sources.  All additive sources sum,
 * then the multiplicative ones apply one after another:
 * <pre>
 * statTotal = ( sum of ALL base sources )        // items + armour + equipment + power + tunings + profile
 *           x ( 1 + sum of additive % )
 *           x product of multiplicative factors
 * </pre>
 * <b>This is a different system from the damage-level buckets in §7.</b>  These multiply a STAT; those multiply a
 * HIT.  A Power 29 blessing raises your Strength number; Fabled's x1.15 raises the damage a hit deals with that
 * Strength.  Applying either at the other's point is the easiest way to be wrong by a factor of two.
 * <p>
 * <b>Scope, ruled: a stat's multipliers affect that ONE stat, and all of it.</b>  The Strength sources scale the
 * whole summed Strength - weapon, armour, equipment, power AND profile - and touch nothing else.  Applying a
 * multiplier to only part of a stat is worth 17x on time-to-kill, so this is not a detail.
 */
public final class Profile {
	private Profile() {}

	/**
	 * Unconditional base sources.  Conditional ones (marked {@code !} in §1.13) are added by the caller:
	 * the pet's own stats through {@link Pet#ownStats()}, and the Ragnarock buff through
	 * {@link Stats#ragnarockStrength} - which is COMPUTED from the axe's own Strength (§1.7), not authored, so the
	 * plan's "+939" never appears as a constant.
	 * <p>
	 * The player's inherent +5 Damage is deliberately NOT here: it is the {@code 5 +} term of the formula
	 * (§1.0.4), so it belongs to {@link Scale#PLAYER_BASE_DAMAGE} and must not be double-counted as a stat.
	 */
	public static StatBlock base() {
		return StatBlock.EMPTY
				// ----- Damage -----
				.plus(Stat.DAMAGE, 4)            // Blazetekk Ham Radio (Bluertooth Ring)

				// ----- Strength -----
				.plus(Stat.STRENGTH, 124)        // SkyBlock Level, +1 per 5 levels (max 620)
				.plus(Stat.STRENGTH, 100)        // Foraging Skill
				.plus(Stat.STRENGTH, 78.75)      // Strength VIII potion, 75 x 1.05
				.plus(Stat.STRENGTH, 65)         // Attributes
				.plus(Stat.STRENGTH, 50)         // Strength Essence
				.plus(Stat.STRENGTH, 5)          // Forbidden Strength
				.plus(Stat.STRENGTH, 6)          // Blessing of Time (essence shop)
				.plus(Stat.STRENGTH, 3)          // Inferno Demonlord Levels
				.plus(Stat.STRENGTH, 10)         // Sunshine/Moonlight Crystal
				.plus(Stat.STRENGTH, 14)         // Blood God Crest
				.plus(Stat.STRENGTH, 10)         // Shark Tooth Necklace
				.plus(Stat.STRENGTH, 10)         // Pandora's Box
				.plus(Stat.STRENGTH, 8)          // Relic of Power
				.plus(Stat.STRENGTH, 2)          // Burststopper Artifact

				// ----- Crit Damage -----
				.plus(Stat.CRIT_DAMAGE, 50)      // base stat
				.plus(Stat.CRIT_DAMAGE, 40)      // Critical IV potion
				.plus(Stat.CRIT_DAMAGE, 40)      // Spirit IV potion
				.plus(Stat.CRIT_DAMAGE, 60)      // Enrichments, non-dungeon talismans
				.plus(Stat.CRIT_DAMAGE, 35.46)   // Enrichments, the 6 cata-buffed ones (no stars)
				.plus(Stat.CRIT_DAMAGE, 50)      // Critical Essence
				.plus(Stat.CRIT_DAMAGE, 16)      // Spider Slayer
				.plus(Stat.CRIT_DAMAGE, 12)      // Beacon Effect
				.plus(Stat.CRIT_DAMAGE, 6)       // Relic of Power
				.plus(Stat.CRIT_DAMAGE, 5)       // Vial of Venom
				.plus(Stat.CRIT_DAMAGE, 5)       // Red Claw Artifact
				.plus(Stat.CRIT_DAMAGE, 3)       // Wolf Slayer
				.plus(Stat.CRIT_DAMAGE, 1)       // Tiny Dancer

				// ----- Intelligence -----
				.plus(Stat.INTELLIGENCE, 300)    // Bottle of Jyrre
				.plus(Stat.INTELLIGENCE, 106)    // Enchanting Skill
				.plus(Stat.INTELLIGENCE, 100)    // Pocket Expresso Machine
				.plus(Stat.INTELLIGENCE, 86)     // Alchemy Skill
				.plus(Stat.INTELLIGENCE, 75)     // Intelligence Essence
				.plus(Stat.INTELLIGENCE, 65)     // Attributes
				.plus(Stat.INTELLIGENCE, 28)     // Harp Completions
				.plus(Stat.INTELLIGENCE, 15)     // Relic of Power
				.plus(Stat.INTELLIGENCE, 13)     // Enderman Slayer
				.plus(Stat.INTELLIGENCE, 10)     // Defuse Kit
				.plus(Stat.INTELLIGENCE, 10)     // Forbidden Intelligence
				.plus(Stat.INTELLIGENCE, 6)      // Blessing of Time (essence shop)
				.plus(Stat.INTELLIGENCE, 6)      // Melody's Hair
				.plus(Stat.INTELLIGENCE, 5)      // Century Cake
				.plus(Stat.INTELLIGENCE, 5)      // Aged Like Fine Jyrre
				.plus(Stat.INTELLIGENCE, 3)      // Bat Artifact
				.plus(Stat.INTELLIGENCE, 3)      // Beastmaster Crest
				.plus(Stat.INTELLIGENCE, 1)      // Big Brain Talisman
				.plus(Stat.INTELLIGENCE, 1)      // Necron's Ladder
				.plus(Stat.INTELLIGENCE, -2)     // Bluertooth Ring
				.plus(Stat.INTELLIGENCE, -4)     // Golden Jerry Artifact

				// ----- Ability Damage -----
				.plus(Stat.ABILITY_DAMAGE, 30);  // Enchanting Skill
	}

	/**
	 * The sum of every additive % on one stat, as a percentage (so 22.35 means +22.35%).
	 * <p>
	 * Legion counts OTHER players within 30 blocks, not yourself, so a full party is 4 stacks.  Renowned counts
	 * the Renowned armour pieces actually worn - which is the only thing a Cow Hat or Spring Boots now contributes
	 * to damage at all, the old x0.70/x0.80 penalties having been deleted outright (§1.10, §8).
	 */
	public static double additivePercent(Player p, Stat stat, Pet pet) {
		double sum = 0;
		if(stat == Stat.STRENGTH || stat == Stat.CRIT_DAMAGE || stat == Stat.INTELLIGENCE
				|| stat == Stat.ABILITY_DAMAGE) {
			sum += 10;                                   // Jerry
			sum += 1.4 * legionStacks(p);
			sum += 1.0 * renownedPiecesWorn(p);
		}
		switch(stat) {
			case STRENGTH -> sum += 1.75;                // Unlimited Power Attribute
			case CRIT_DAMAGE -> sum += 1.75;             // Unlimited Energy Attribute
			case INTELLIGENCE -> sum += 2.0 + 1.75;      // 2 IQ Points Talisman, Unlimited Torment Attribute
			default -> { }                               // Ability Damage has no attribute of its own
		}
		if(pet != null) sum += pet.statAdditive(stat);
		return sum;
	}

	/**
	 * The product of every multiplicative factor on one stat.  These are the blessings and the Master Skull, and
	 * they are read through {@link Difficulty} so realistic mode can supply the party's real blessing count
	 * without any of this becoming a second code path.
	 */
	public static double multiplicative(Stat stat) {
		return switch(stat) {
			case STRENGTH -> Difficulty.blessing(Utils.BlessingType.POWER)
					* Difficulty.blessing(Utils.BlessingType.TIME)
					* 1.1;                                // Master Skull, Tier 7
			case CRIT_DAMAGE -> Difficulty.blessing(Utils.BlessingType.POWER);
			case INTELLIGENCE -> Difficulty.blessing(Utils.BlessingType.WISDOM)
					* Difficulty.blessing(Utils.BlessingType.TIME);
			default -> 1.0;                               // Damage and Ability Damage have no multiplicative source
		};
	}

	/** Legion stacks: other non-spectating players within 30 blocks.  Never yourself, so a full party is 4. */
	public static int legionStacks(Player p) {
		if(p == null) return 0;
		int n = 0;
		for(Player other : Bukkit.getOnlinePlayers()) {
			if(other == p || other.getGameMode() == GameMode.SPECTATOR) continue;
			if(!other.getWorld().equals(p.getWorld())) continue;
			if(other.getLocation().distanceSquared(p.getLocation()) <= 30 * 30) n++;
		}
		return n;
	}

	/** Renowned armour pieces currently worn, each worth +1% additive on every stat it applies to. */
	public static int renownedPiecesWorn(Player p) {
		if(p == null) return 0;
		PlayerInventory inv = p.getInventory();
		int n = 0;
		for(ItemStack piece : new ItemStack[]{inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()}) {
			ItemDef def = Items.of(piece);
			if(def != null && def.reforge() == ReforgeId.RENOWNED) n++;
		}
		return n;
	}
}
