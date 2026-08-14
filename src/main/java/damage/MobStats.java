package damage;

import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wither;
import instructions.clear.Room;
import instructions.clear.Rooms;

import java.util.EnumSet;
import java.util.Set;

/**
 * HP, defense, mob types and the inherent boss resistance for every target the damage system computes against
 * (MAP.md §5).
 * <p>
 * <b>The difficulty curve lives in the defense, not the HP.</b> The Withered Dragon has less HP than Necron but
 * 2500 defense makes it 84% as tough per point of damage.  On top of defense, every boss and mini-boss also takes
 * a flat x0.1, which is what keeps the whole chain in a sane range - without it every hit is an order of magnitude
 * too strong.
 * <p>
 * The plugin covers the whole floor rather than only the five bosses: the Watcher's adds, Crypt Undeads, Princes,
 * Wither Miners, Angry Archaeologists and Shadow Assassins all get real stats too.  What stays out is only the
 * non-combat entities - the Mort and Wizard villager NPCs, and the clear-phase props.
 */
public final class MobStats {
	private MobStats() {}

	/**
	 * One mob's stat block.
	 *
	 * @param id           a readable name, for debug output
	 * @param displayHealth the SkyBlock HP as the game displays it (800M, 1.4B, ...).  Internal health is this
	 *                      divided by {@link Scale#SB_PER_MC_HP}, so the displayed number is no longer a
	 *                      hand-picked constant that has to be kept in step with it.
	 * @param defense      the real SkyBlock defense, BEFORE Lethality and Last Breath reduce it
	 * @param bossResistance whether this target carries the inherent x0.1 every boss and mini-boss has
	 * @param elite        whether the Elite attribute's +30% applies (Bosses and Mini-Bosses)
	 * @param types        every SkyBlock type it carries; all matching buffs stack
	 */
	public record MobStat(String id, double displayHealth, double defense, boolean bossResistance, boolean elite,
			Set<MobType> types) {

		/** Minecraft health for this mob: the SkyBlock figure at the {@code /1e6} scale. */
		public double internalHealth() {
			return displayHealth / Scale.SB_PER_MC_HP;
		}

		/** The same block with its HP scaled by room depth (§5's +10% per tier). */
		MobStat atDepth(int depth) {
			double m = depthMultiplier(depth);
			return m == 1.0 ? this : new MobStat(id, displayHealth * m, defense, bossResistance, elite, types);
		}

		/** The same block scaled by the depth of the room at a location, for a mob being spawned there. */
		public MobStat atDepthOf(org.bukkit.Location location) {
			Room room = Rooms.roomAt(location);
			return atDepth(room == null ? 1 : Math.max(1, room.level));
		}
	}

	private static Set<MobType> types(MobType... t) {
		return t.length == 0 ? EnumSet.noneOf(MobType.class) : EnumSet.copyOf(java.util.List.of(t));
	}

	// ===================== the four Wither Lords and the Withered Dragons =====================
	// §7 calls the Wither Lords "Arcane, Wither" and then lists Undead Ruler +39% among the buffs that match them,
	// which only works if they count as Undead too - there is no Wither Ruler, so a Wither-type mob pays out
	// through Smite and the Hyperion instead.  UNDEAD is in the set for exactly that reason; between them the
	// three types reproduce §7's list: Smite +50%, Arcane Ruler +39%, Undead Ruler +39%, Elite +30%, Hyperion x1.5.
	public static final MobStat MAXOR = new MobStat("Maxor", 800_000_000d, 1000, true, true,
			types(MobType.ARCANE, MobType.WITHER, MobType.UNDEAD));
	public static final MobStat STORM = new MobStat("Storm", 1_000_000_000d, 1200, true, true,
			types(MobType.ARCANE, MobType.WITHER, MobType.UNDEAD));
	public static final MobStat GOLDOR = new MobStat("Goldor", 1_200_000_000d, 1800, true, true,
			types(MobType.ARCANE, MobType.WITHER, MobType.UNDEAD));
	public static final MobStat NECRON = new MobStat("Necron", 1_400_000_000d, 2100, true, true,
			types(MobType.ARCANE, MobType.WITHER, MobType.UNDEAD));
	/**
	 * Arcane + Ender + Airborne, and NOT Wither - which is why the Hyperion's x1.5 does not apply to them and the
	 * Dark Claymore wins on this target.  Three Rulers, Ender Slayer, Gravity, the Draconic Artifact and an Ender
	 * Dragon pet all stack here, roughly +490% of additive the Withers never see.
	 */
	public static final MobStat WITHERED_DRAGON = new MobStat("Withered Dragon", 1_000_000_000d, 2500, true, true,
			types(MobType.ARCANE, MobType.ENDER, MobType.AIRBORNE));

	// ===================== the rest of the floor =====================
	// The wiki publishes no mob type for Sadan's Giants or for Bonzo, so they take no Ruler and no type enchant.
	public static final MobStat DIAMANTE_GIANT = new MobStat("Diamante Giant", 400_000_000d, 0, true, true, types());
	public static final MobStat BONZO = new MobStat("Bonzo", 300_000_000d, 0, true, true, types());
	/** The Watcher's other adds.  No types are published for any of them; Undead is the plan's own presumption. */
	public static final MobStat WATCHER_UNDEAD = new MobStat("Watcher Undead", 6_000_000d, 2000, true, false,
			types(MobType.UNDEAD));
	/** Undead + Subterranean, so Smite +50, Undead Ruler +39 AND Subterranean Ruler +39 all land. */
	public static final MobStat CRYPT_UNDEAD = new MobStat("Crypt Undead", 9_000_000d, 0, false, false,
			types(MobType.UNDEAD, MobType.SUBTERRANEAN));
	public static final MobStat PRINCE = new MobStat("Prince", 1_000_000d, 0, false, false,
			types(MobType.UNDEAD, MobType.SUBTERRANEAN));
	/**
	 * 300M, confirmed.  Wither + Undead, so a Hyperion hits it for x1.5 on top of Smite and Undead Ruler.  It does
	 * NOT get Skeletal Ruler: Skeletal is Normal-mode only and this is Master Mode.
	 * <p>
	 * <b>Zero defense, and no x0.1</b> - it is a regular mob rather than a boss or mini-boss.  This block covers the
	 * Wither Guard, Wither Husk and Apostle too (see {@code of}), and none of them have defense either.
	 * <p>
	 * It used to carry <b>1200</b> defense, guessed off "the wiki's F7 Master Mode row" while §5 left the real figure
	 * [TBD].  That guess was wrong and it was expensive: 1200 defense is a /13 divisor, so it was quietly throwing
	 * away 92% of every hit on the most-hit trash mob on the floor, and made RCM read as roughly a tenth of its real
	 * SkyBlock damage.  <b>Do not reintroduce a defense figure here without measuring it.</b>
	 */
	public static final MobStat WITHER_MINER = new MobStat("Wither Miner", 300_000_000d, 0, false, false,
			types(MobType.WITHER, MobType.UNDEAD));
	/**
	 * Humanoid + Subterranean, and a Mini-Boss, so Elite applies.  Its HP scales with room depth.
	 * <p>
	 * <b>1200 defense, confirmed</b> - it really is the one non-boss on the floor that has any.  It shares a figure
	 * with nothing else now: the Wither trash and both Shadow Assassins were guessed off this same row and are 0.
	 */
	public static final MobStat ANGRY_ARCHAEOLOGIST = new MobStat("Angry Archaeologist", 12_000_000d, 1200, true, true,
			types(MobType.HUMANOID, MobType.SUBTERRANEAN));
	/**
	 * The Shadow Assassins in the BOSS FIGHT (Storm's four pad corners): a flat <b>145M</b>, with no room depth to
	 * scale by - the boss arena is not on the room grid.
	 * <p>
	 * Humanoid + Arcane, so it takes those two Rulers and <b>no Smite at all</b> - the softest-looking mob on the
	 * floor is the one that resists the whole undead package.
	 * <p>
	 * <b>Zero defense</b>, measured.  The only thing softening it is the x0.1 mini-boss resistance, which it does
	 * keep.  It used to carry 1200, guessed off the Archaeologist's row purely because both are mini-bosses - and
	 * that guess was a /13 divisor on a mob four players burn down under a timer.
	 */
	public static final MobStat SHADOW_ASSASSIN = new MobStat("Shadow Assassin", 145_000_000d, 0, true, true,
			types(MobType.HUMANOID, MobType.ARCANE));
	/**
	 * The Shadow Assassin in the CLEAR phase's Yellow room, which is a different mob from the boss-fight ones:
	 * <b>140M base</b>, and Yellow is depth II, so <b>154M</b> in play.  Zero defense and the x0.1, as above.
	 */
	public static final MobStat YELLOW_SHADOW_ASSASSIN = new MobStat("Shadow Assassin", 140_000_000d, 0, true, true,
			types(MobType.HUMANOID, MobType.ARCANE));

	/**
	 * Room depth multiplier: {@code base x (1 + 0.10 x (depth - 1))}, counted from the FIRST room rather than from
	 * zero, so depth I is x1.00 and depth V is x1.40 (§5).
	 * <p>
	 * {@code Room.level} IS the depth - the numerals in {@code Rooms} line up exactly with the observed values
	 * (Deathmite at level 2 gives the observed 13.2M), so no graph work is needed.  A room with no depth at all
	 * would read as depth 0 and give a NEGATIVE buff, so an unset level is treated as depth I.
	 */
	public static double depthMultiplier(int depth) {
		return 1.0 + 0.10 * (Math.max(1, depth) - 1);
	}

	/**
	 * The stat block for a target, already depth-scaled where that applies, or null if it is not modelled.
	 * <p>
	 * Bosses are matched on their scoreboard tag; the rest of the floor on its display name, which is the only
	 * identity those mobs have ever carried (they are plain Zombies and Wither Skeletons distinguished by their
	 * custom name).  The name is matched with {@code contains} because {@code Utils.changeName} rewrites the
	 * health suffix on every hit, so only the leading part is stable.
	 */
	public static MobStat of(LivingEntity entity) {
		if(entity == null) return null;
		// Memoised for the current tick.  Every hit asks four times (defense, resistance, types, elite) and Cleave
		// plus a Terminator volley makes that dozens of lookups a tick, each of which would otherwise re-resolve a
		// name and a room-grid cell.
		int now = MinecraftServer.currentTick;
		if(now != lookupTick) {
			lookupTick = now;
			lookupCache.clear();
		}
		java.util.UUID id = entity.getUniqueId();
		if(lookupCache.containsKey(id)) return lookupCache.get(id);
		MobStat resolved = resolve(entity);
		lookupCache.put(id, resolved);
		return resolved;
	}

	private static int lookupTick = -1;
	private static final java.util.Map<java.util.UUID, MobStat> lookupCache = new java.util.HashMap<>();

	private static MobStat resolve(LivingEntity entity) {
		Set<String> tags = entity.getScoreboardTags();
		if(tags.contains("TASMaxor")) return MAXOR;
		if(tags.contains("TASStorm")) return STORM;
		if(tags.contains("TASGoldor")) return GOLDOR;
		if(tags.contains("TASNecron")) return NECRON;
		if(tags.contains("WitherKingDragon")) return WITHERED_DRAGON;

		String name = displayName(entity);
		if(tags.contains("WatcherMob")) {
			if(name.contains("Diamante") || name.contains("Giant")) return DIAMANTE_GIANT;
			if(name.contains("Bonzo")) return BONZO;
			return WATCHER_UNDEAD;
		}
		if(tags.contains("SecretPrince")) return PRINCE.atDepth(depthAt(entity));
		if(tags.contains(instructions.clear.ClearManager.TAG_CRYPT)) return CRYPT_UNDEAD.atDepth(depthAt(entity));
		if(name.contains("Angry Archaeologist")) return ANGRY_ARCHAEOLOGIST.atDepth(depthAt(entity));
		if(name.contains("Shadow Assassin")) {
			// Two different mobs share the name.  The clear phase's Yellow-room one is a room miniboss (tagged
			// as such, and depth-scaled at 140M base); Storm's four pad-corner ones are flat 145M and stand in
			// the boss arena, which is not on the room grid at all.
			return tags.contains("ClearMiniboss")
					? YELLOW_SHADOW_ASSASSIN.atDepth(depthAt(entity))
					: SHADOW_ASSASSIN;
		}
		// The wither-class trash that spawns in the Maxor and Storm phases.  §5 leaves the Guard's, Husk's and
		// Apostle's own HP [TBD], so they share the Wither Miner's block - they are the same kind of mob.
		if(name.contains("Wither Miner") || name.contains("Wither Guard") || name.contains("Wither Husk")
				|| name.contains("Apostle")) {
			return WITHER_MINER;
		}
		return null;
	}

	/** An entity's plain custom name, falling back to its type name. */
	private static String displayName(LivingEntity entity) {
		return entity.customName() == null ? entity.getName() : plugin.Utils.plain(entity.customName());
	}

	/**
	 * Give a freshly-spawned mob its real HP, and clear the vanilla armour attributes.
	 * <p>
	 * These mobs used to be flat-kill targets carrying a hand-picked handful of health and a negative
	 * {@code minecraft:armor} to claw damage back out of vanilla's reduction.  Neither is wanted now:
	 * internal HP is {@code SB/1e6}, and SkyBlock defense is applied by {@link Damage} at the boundary, so
	 * {@code minecraft:armor} stays 0 on every mob (§5).
	 */
	public static void apply(LivingEntity mob, MobStat stat) {
		if(mob == null || stat == null) return;
		var maxHealth = mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
		if(maxHealth != null) {
			maxHealth.setBaseValue(stat.internalHealth());
			mob.setHealth(stat.internalHealth());
		}
		var armor = mob.getAttribute(org.bukkit.attribute.Attribute.ARMOR);
		if(armor != null) armor.setBaseValue(0);
		var toughness = mob.getAttribute(org.bukkit.attribute.Attribute.ARMOR_TOUGHNESS);
		if(toughness != null) toughness.setBaseValue(0);
		// Mobs get zero i-frames (§7), so every computed hit lands in full.
		mob.setMaximumNoDamageTicks(0);
		mob.setNoDamageTicks(0);
	}

	/** The room depth where an entity stands, or 1 if it is not inside a mapped room. */
	private static int depthAt(Entity entity) {
		Room room = Rooms.roomAt(entity.getLocation());
		return room == null ? 1 : Math.max(1, room.level);
	}

	/** The defense to apply to a target, before Lethality and Last Breath.  Unmodelled targets have none. */
	public static double defenseOf(LivingEntity entity) {
		MobStat stat = of(entity);
		return stat == null ? 0 : stat.defense();
	}

	/** The inherent x0.1 every boss and mini-boss carries, or 1.0 for anything else. */
	public static double resistanceOf(LivingEntity entity) {
		MobStat stat = of(entity);
		return stat != null && stat.bossResistance() ? Scale.BOSS_RESISTANCE : 1.0;
	}

	/** Every SkyBlock type a target carries.  Empty for an unmodelled target, so no type buff matches it. */
	public static Set<MobType> typesOf(LivingEntity entity) {
		MobStat stat = of(entity);
		return stat == null ? java.util.Set.of() : stat.types();
	}

	/** True if the Elite attribute's +30% applies to this target (Bosses and Mini-Bosses). */
	public static boolean isElite(LivingEntity entity) {
		MobStat stat = of(entity);
		return stat != null && stat.elite();
	}

	// ===================== Wither King phase detection =====================
	// Cached per tick: the mage beam's range tier asks for this on every shot, and it is a world entity scan.

	private static int wkCheckedTick = -1;
	private static boolean wkActive = false;

	/**
	 * True while the Wither King fight is up.  This is a PHASE check, not a location check, and it has to be:
	 * {@code LavaJump.isInBossArena} is one box that already contains the Wither King arena, so no third
	 * positional tier is possible.  The {@code TASWitherKing} tag is the more precise of the two available
	 * signals, since it only exists while the boss is actually alive (§7).
	 */
	public static boolean witherKingPhaseActive() {
		int now = MinecraftServer.currentTick;
		if(now == wkCheckedTick) return wkActive;
		wkCheckedTick = now;
		wkActive = false;
		for(World w : Bukkit.getWorlds()) {
			for(Wither wither : w.getEntitiesByClass(Wither.class)) {
				if(wither.getScoreboardTags().contains("TASWitherKing")) {
					wkActive = true;
					return true;
				}
			}
		}
		return false;
	}
}
