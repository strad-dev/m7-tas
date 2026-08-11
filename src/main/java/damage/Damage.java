package damage;

import instructions.bosses.WitherActions;
import instructions.bosses.WitherLord;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wither;
import org.bukkit.inventory.ItemStack;
import plugin.Utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The damage formulas and the single application boundary (DAMAGE_PLAN.md §7).
 *
 * <pre>
 * melee   = (5 + Damage) x (1 + Strength/100) x (1 + CritDamage/100)      // ALWAYS crits
 *         x (1 + sumAdditive/100) x product(Multiplicative_i)
 *
 * bow     = the same shape, x chargeFraction; a non-full draw ALSO drops the crit term entirely
 *
 * ability = BaseDamage x (1 + (Intelligence/100) x AbilityScaling) x (1 + AbilityDamage/100)
 *         x (1 + sumAdditive/100) x product(Multiplicative_i)             // no Strength, no Crit Damage
 *
 * mageBeam = melee x (0.30 + 0.0009 x Intelligence)                       // the Mage Staff passive
 * </pre>
 *
 * <b>There is no {@code Strength/5} term</b> - SkyBlock removed it years ago, and carrying it inflates every melee
 * and beam figure by x2.69 at these stat levels.  Strength enters only through {@code (1 + Strength/100)}.
 * <p>
 * <b>All math is in real SkyBlock units</b> (doubles into the billions).  There is exactly one conversion to
 * Minecraft health, in {@link #deal}:
 * <pre>
 * mcDamage = ( sbDamage x bossResistance / (1 + defense/100) ) / 1e6
 * </pre>
 * For Necron those two factors together are a /82 before the number touches vanilla health.
 * <p>
 * <b>One damage path.</b>  Three used to exist - {@code hurtServer(genericKill)}, {@code setHealth} for dragons and
 * {@code wither.damage()} for arrows-on-withers - with different i-frame, armor, event and aggro behaviour, and
 * the split was already visible as workarounds in the code.  Everything now goes through {@link #deal}, which
 * applies damage by reading health, subtracting and setting it: vanilla is not a participant in this model, and
 * every place it tried to be had already been suppressed by hand.
 */
public final class Damage {
	private Damage() {}

	// ===================== §7 damage-level additive sources =====================
	private static final double SHARPNESS_VII = 50;
	private static final double POWER_VII = 65;
	private static final double SNIPE_IV_PER_10_BLOCKS = 4;
	private static final double ARCHERY_IV_POTION = 80;
	private static final double SMITE_VII = 50;
	private static final double BANE_OF_ARTHROPODS_VII = 50;
	private static final double ENDER_SLAYER_VII = 50;
	private static final double PYROCLASM_VI = 80;
	private static final double CUBISM_VI = 40;
	private static final double GRAVITY_VI = 40;   // the renamed Dragon Hunter: +40% to Airborne
	private static final double IMPALING_V = 30;
	private static final double SMOLDERING_V = 30;
	private static final double FIRST_STRIKE_V = 125;
	private static final double TRIPLE_STRIKE_V = 50;
	private static final double GIANT_KILLER_VII = 65;
	private static final double TITAN_KILLER_PER_100_DEFENSE = 20;
	private static final double TITAN_KILLER_CAP = 80;
	private static final double EXECUTE_VI_PER_PERCENT_MISSING = 1.25;
	private static final double PROSECUTE_VI_PER_PERCENT_REMAINING = 1.0;
	private static final double PRECISE_HEADSHOT = 10;
	private static final double RULER = 39;
	private static final double WARRIOR = 25;
	private static final double SKELETOR = 25;
	private static final double ELITE = 30;
	private static final double DOMINANCE = 15;
	private static final double COMBAT_60 = 210;
	private static final double RING_OF_LOVE = 100;
	private static final double DRACONIC_ARTIFACT = 5;

	// ===================== §7 damage-level multiplicative sources =====================
	private static final double HYPERION_VS_WITHER = 1.5;
	private static final double OVERLOAD = 1.5;
	private static final double BOOK_OF_PROGRESSION = 1.05;
	private static final double TARANTULA_RING = 1.15;

	// ===================== §7 mage beam =====================
	/** The Mage Staff passive: 30% base plus 0.09% per Intelligence, ADDED to the 30%, not multiplied by it. */
	private static final double BEAM_BASE = 0.30;
	private static final double BEAM_PER_INTELLIGENCE = 0.0009;

	/**
	 * The beam's three range tiers (§7), each doubling the previous total.  Full damage to the cutoff, then a
	 * linear decrement to zero at max range.  Note what mostly grows is the CUTOFF: in the boss arena the beam
	 * deals full damage out to 35 blocks and only fades over the last 15.
	 */
	public record BeamRange(double cutoff, double maxRange) {
		public static final BeamRange DEFAULT = new BeamRange(10, 25);
		public static final BeamRange BOSS_ARENA = new BeamRange(35, 50);
		public static final BeamRange WITHER_KING = new BeamRange(70, 100);

		/** The share of full damage a beam does at this distance. */
		public double falloff(double distance) {
			if(distance <= cutoff) return 1.0;
			if(distance >= maxRange) return 0.0;
			return (maxRange - distance) / (maxRange - cutoff);
		}
	}

	/**
	 * Which tier applies where the player is standing.  Detection needs a PHASE check, not a location check:
	 * {@code LavaJump.isInBossArena} is one box that already contains the Wither King arena, so a third positional
	 * tier is impossible.
	 */
	public static BeamRange beamRange(Player p) {
		if(MobStats.witherKingPhaseActive()) return BeamRange.WITHER_KING;
		return listeners.LavaJump.isInBossArena(p.getLocation()) ? BeamRange.BOSS_ARENA : BeamRange.DEFAULT;
	}

	// ===================== computing a hit =====================

	/** A melee swing.  The weapon is whatever is in the main hand at the time. */
	public static double melee(Player p, LivingEntity target, ItemStack weapon) {
		ItemDef def = Items.of(weapon);
		applyOnHitDebuffs(p, target, DamagePath.MELEE, def);
		return finish(p, target, DamagePath.MELEE, def, statCore(p, DamagePath.MELEE, true), null);
	}

	/**
	 * A mage beam: the melee hit rescaled by the Mage Staff passive, then faded by distance.  It is not a separate
	 * damage path in the enchantment sense - it counts as a melee attack and takes the whole sword list.
	 */
	public static double beam(Player p, LivingEntity target, ItemStack weapon, double distance) {
		ItemDef def = Items.of(weapon);
		applyOnHitDebuffs(p, target, DamagePath.BEAM, def);
		StatBlock stats = Stats.of(p, DamagePath.BEAM);
		double core = statCore(p, DamagePath.BEAM, true);
		double beamMultiplier = BEAM_BASE + BEAM_PER_INTELLIGENCE * stats.get(Stat.INTELLIGENCE);
		BeamRange range = beamRange(p);
		return finish(p, target, DamagePath.BEAM, def, core * beamMultiplier * range.falloff(distance), null);
	}

	/**
	 * The STAT half of a melee hit, with no target involved.  Exists for the thrown-axe abilities, which decide
	 * their damage as a share of the wielder's melee at THROW time and only learn their target later.
	 */
	public static double meleeCore(Player p) {
		return statCore(p, DamagePath.MELEE, true);
	}

	/** The TARGET-dependent half of a melee hit, applied when a thrown axe actually connects. */
	public static double meleeFinish(Player p, LivingEntity target, ItemStack weapon, double core) {
		ItemDef def = Items.of(weapon);
		applyOnHitDebuffs(p, target, DamagePath.MELEE, def);
		return finish(p, target, DamagePath.MELEE, def, core, null);
	}

	/**
	 * The STAT half of a bow shot, which {@link Arrows} stamps on the projectile at fire time so a mid-flight
	 * weapon swap cannot change what the arrow hits for (§1.0.5).
	 *
	 * @param crit false only for a partially drawn bow, which loses the whole crit term rather than a fraction
	 */
	public static double bowCore(Player p, boolean crit) {
		return statCore(p, DamagePath.BOW, crit);
	}

	/**
	 * The TARGET-dependent half of a bow shot, applied when the arrow lands.  None of it is knowable at fire time.
	 *
	 * @param blocksTravelled how far the arrow flew, for Snipe IV's +4% per 10 blocks
	 */
	public static double bowFinish(Player p, LivingEntity target, ItemDef weapon, double core,
			double blocksTravelled, boolean headshot) {
		return finish(p, target, DamagePath.BOW, weapon, core, new BowContext(blocksTravelled, headshot));
	}

	/**
	 * A whole bow shot in one call, for the paths that resolve at hit time anyway (the Terminator's Salvation
	 * beam, which is not a bow shot and so is never draw-scaled).
	 */
	public static double bow(Player p, LivingEntity target, ItemStack weapon, double chargeFraction,
			double blocksTravelled, boolean headshot) {
		ItemDef def = Items.of(weapon);
		applyOnHitDebuffs(p, target, DamagePath.BOW, def);
		boolean full = chargeFraction >= 1.0;
		double core = bowCore(p, full) * Math.max(0, Math.min(chargeFraction, 1.0));
		return bowFinish(p, target, def, core, blocksTravelled, headshot);
	}

	/**
	 * A right-click ability.  Abilities get neither Strength nor Crit Damage, which is the whole reason they
	 * behave so differently from the melee/beam path - and why they come out at roughly 1/414 of a beam.  That is
	 * intended: an option for anyone who wants to try them, not a damage strategy.
	 */
	public static double ability(Player p, LivingEntity target, ItemStack weapon) {
		ItemDef def = Items.of(weapon);
		if(def == null || def.ability() == null) return 0;
		applyOnHitDebuffs(p, target, DamagePath.ABILITY, def);
		StatBlock stats = Stats.of(p, DamagePath.ABILITY);
		double core = def.ability().baseDamage()
				* (1.0 + (stats.get(Stat.INTELLIGENCE) / 100.0) * def.ability().intelligenceScaling())
				* (1.0 + stats.get(Stat.ABILITY_DAMAGE) / 100.0);
		return finish(p, target, DamagePath.ABILITY, def, core, null);
	}

	/** Extra inputs only the bow path has. */
	private record BowContext(double blocksTravelled, boolean headshot) {}

	/**
	 * The stat half of the melee/bow shape.  {@code crit} is true for everything except a partially drawn bow -
	 * §7 rules every hit a crit, deliberately, because a random roll would make two identical runs incomparable.
	 */
	private static double statCore(Player p, DamagePath path, boolean crit) {
		StatBlock stats = Stats.of(p, path);
		double hit = (Scale.PLAYER_BASE_DAMAGE + stats.get(Stat.DAMAGE))
				* (1.0 + stats.get(Stat.STRENGTH) / 100.0);
		if(crit) hit *= 1.0 + stats.get(Stat.CRIT_DAMAGE) / 100.0;
		return hit;
	}

	/** The damage-level stage: one additive factor, then the multiplicative product. */
	private static double finish(Player p, LivingEntity target, DamagePath path, ItemDef weapon, double core,
			BowContext bow) {
		if(target == null || core <= 0) return 0;
		double additive = additivePercent(p, target, path, weapon, bow);
		double multiplicative = multiplicative(p, target, path, weapon);
		return core * (1.0 + additive / 100.0) * multiplicative;
	}

	// ===================== additive =====================

	/**
	 * The whole {@code sumAdditive} for one hit, as a percentage.
	 * <p>
	 * <b>The mage beam counts as a melee attack</b>, so it takes every melee row.  Bows lose most of the damage
	 * enchantments (Execute, Prosecute, First Strike, Triple Strike, Giant Killer, Titan Killer and Sharpness are
	 * all sword-only), so an Archer's arrows miss Prosecute's +100% and Titan Killer's +80% entirely.
	 */
	private static double additivePercent(Player p, LivingEntity target, DamagePath path, ItemDef weapon,
			BowContext bow) {
		Set<MobType> types = MobStats.typesOf(target);
		boolean sword = path.isMelee() || path == DamagePath.ABILITY;
		double sum = 0;

		// --- always on ---
		sum += COMBAT_60;
		sum += RING_OF_LOVE;                       // assumed always procs
		sum += DOMINANCE;                          // assumed always (full health)
		if(MobStats.isElite(target)) sum += ELITE;
		sum += CombatState.comboAdditive(p);
		for(MobType t : types) if(t.hasRuler()) sum += RULER;
		if(target instanceof EnderDragon) sum += DRACONIC_ARTIFACT;
		Pet pet = Pet.forPlayer(p, path);
		sum += pet.damageAdditive(types);

		// --- mob-type enchantments ---
		// Cubism, Gravity, Impaling and Smoldering are on swords AND bows; the rest are sword-only.
		if(types.contains(MobType.CUBIC)) sum += CUBISM_VI;
		if(types.contains(MobType.AIRBORNE)) sum += GRAVITY_VI;
		if(types.contains(MobType.AQUATIC)) sum += IMPALING_V;
		if(types.contains(MobType.INFERNAL)) sum += SMOLDERING_V;
		if(sword) {
			if(types.contains(MobType.SKELETAL) || types.contains(MobType.WITHER) || types.contains(MobType.UNDEAD)) {
				sum += SMITE_VII;
			}
			if(types.contains(MobType.ARTHROPOD)) sum += BANE_OF_ARTHROPODS_VII;
			if(types.contains(MobType.ENDER)) sum += ENDER_SLAYER_VII;
			if(types.contains(MobType.MAGMATIC)) sum += PYROCLASM_VI;
		}

		// --- either/or pairs: evaluate both and take the larger, NEVER sum them ---
		if(sword) {
			sum += Math.max(giantKiller(), titanKiller(target));
			sum += Math.max(execute(target), prosecute(target));
		}
		if(path.isMelee()) {
			// First Strike and Triple Strike are melee-only, and are an either/or pair with each other.
			double firstStrike = CombatState.isFirstHitOn(p, target.getUniqueId()) ? FIRST_STRIKE_V : 0;
			double tripleStrike = CombatState.isTripleStrikeHitOn(p, target.getUniqueId()) ? TRIPLE_STRIKE_V : 0;
			sum += Math.max(firstStrike, tripleStrike);
			sum += SHARPNESS_VII;
			sum += WARRIOR;
		}

		// --- bow-only ---
		if(path == DamagePath.BOW) {
			sum += POWER_VII;
			sum += ARCHERY_IV_POTION;
			sum += SKELETOR;
			if(bow != null) {
				sum += SNIPE_IV_PER_10_BLOCKS * (bow.blocksTravelled() / 10.0);
				if(bow.headshot() && weapon != null && weapon.reforge() == ReforgeId.PRECISE) sum += PRECISE_HEADSHOT;
			}
		}

		// --- class bonuses (§1.14).  Berserk's repeated-hit stack is the largest additive source in the plan. ---
		DungeonClass clazz = DungeonClass.of(p);
		sum += ClassBonuses.damageAdditive(p, clazz, path, target.getUniqueId(), DungeonClass.isSoloOnClass(p));
		return sum;
	}

	private static double giantKiller() {
		// Giant Killer VII: +65%, since the target's health is assumed always far larger than the player's.
		return GIANT_KILLER_VII;
	}

	/** Titan Killer VII: +20% per 100 of the target's defense, capped at +80%.  Zero against a 0-defense mob. */
	private static double titanKiller(LivingEntity target) {
		double defense = MobStats.defenseOf(target);
		return Math.min(TITAN_KILLER_CAP, TITAN_KILLER_PER_100_DEFENSE * defense / 100.0);
	}

	/** Execute VI: +1.25% per 1% of the target's MISSING health.  Overtakes Prosecute below ~44% health. */
	private static double execute(LivingEntity target) {
		return EXECUTE_VI_PER_PERCENT_MISSING * (100.0 - healthPercent(target));
	}

	/** Prosecute VI: +1% per 1% of the target's REMAINING health, so +100% at full health. */
	private static double prosecute(LivingEntity target) {
		return PROSECUTE_VI_PER_PERCENT_REMAINING * healthPercent(target);
	}

	private static double healthPercent(LivingEntity target) {
		var attr = target.getAttribute(Attribute.MAX_HEALTH);
		double max = attr == null ? 0 : attr.getValue();
		if(max <= 0) return 100;
		return Math.clamp(target.getHealth() / max, 0.0, 1.0) * 100.0;
	}

	// ===================== multiplicative =====================

	/** The {@code product(Multiplicative_i)} for one hit.  Ordering does not matter. */
	private static double multiplicative(Player p, LivingEntity target, DamagePath path, ItemDef def) {
		double product = BOOK_OF_PROGRESSION;

		// The Hyperion's x1.5 against EVERY Wither-type mob: the four Wither Lords, the Wither Miners and the
		// wither-class trash - but NOT the Withered Dragons, which are Arcane + Ender + Airborne.  This is the same
		// mechanic the code used to write inside out as "-33% against anything that isn't a wither" (1/1.5 = 0.667);
		// only one of the two survives, and it is this one.
		if(def != null && "skyblock/combat/scylla".equals(def.loreId())
				&& MobStats.typesOf(target).contains(MobType.WITHER)) {
			product *= HYPERION_VS_WITHER;
		}
		if(def != null && path.isMelee()) product *= def.reforge().meleeMultiplier();   // Fabled x1.15
		if(path == DamagePath.BOW) product *= OVERLOAD;                                  // assumed always procs
		if(path == DamagePath.ABILITY) product *= lovingMultiplier(p);
		if(path.isMelee() && CombatState.isTarantulaHit(p)) product *= TARANTULA_RING;

		DungeonClass clazz = DungeonClass.of(p);
		product *= ClassBonuses.damageMultiplier(p, clazz, path, DungeonClass.isSoloOnClass(p));

		// The two x1.1 target debuffs.  They help every attacker, since they live on the target.
		product *= TargetDebuffs.damageMultiplier(target);
		return product;
	}

	/** The Loving reforge's x1.05, which is abilities-only and comes off whatever armour is worn. */
	private static double lovingMultiplier(Player p) {
		var inv = p.getInventory();
		for(ItemStack piece : new ItemStack[]{inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots()}) {
			ItemDef def = Items.of(piece);
			if(def != null && def.reforge() == ReforgeId.LOVING) return def.reforge().abilityMultiplier();
		}
		return 1.0;
	}

	// ===================== debuffs the hit itself applies =====================

	/**
	 * Apply the debuffs this hit carries, BEFORE its own damage is computed, so the hit benefits from its own
	 * debuff (§7's ordering rule).  That is the opposite of the obvious implementation, which is why it is
	 * explicit.
	 * <p>
	 * This is also called on its own from the paths that skip damage, because <b>stacks land even when the damage
	 * does not</b>: a mage beam on an invulnerable boss still builds Lethality and ramps Venomous, so the moment
	 * the boss opens up the debuffs are already there.
	 */
	public static void applyOnHitDebuffs(Player p, LivingEntity target, DamagePath path, ItemDef weapon) {
		applyOnHitDebuffs(p, target, path, weapon, true);
	}

	/**
	 * As above, with {@code buildsLastBreath} false for an arrow that came off a Last Breath but must not stack
	 * it - the Archer's two bonus arrows.  See {@link Arrows#stamp} for the full rule.
	 */
	public static void applyOnHitDebuffs(Player p, LivingEntity target, DamagePath path, ItemDef weapon,
			boolean buildsLastBreath) {
		if(target == null) return;
		// Lethality is a sword enchantment, so a bow never builds its stacks.
		if(path.isMelee()) TargetDebuffs.applyLethality(target);
		if(path == DamagePath.BOW) {
			TargetDebuffs.applyTwilightPoison(target);
			TargetDebuffs.applyDuplexFire(target);
			if(buildsLastBreath && weapon != null && "skyblock/combat/last_breath".equals(weapon.loreId())) {
				TargetDebuffs.applyLastBreath(target);
			}
		}
	}

	/** As above, for a call site that has the held stack rather than its definition. */
	public static void applyOnHitDebuffs(Player p, LivingEntity target, DamagePath path, ItemStack weapon) {
		applyOnHitDebuffs(p, target, path, Items.of(weapon));
	}

	// ===================== the single application boundary =====================

	/** Deal a computed SkyBlock-scale hit.  Notes aggro, and counts as the primary instance for procs and Cleave. */
	public static void deal(LivingEntity target, double sbDamage, DamageKind kind, Player attacker, DamagePath path) {
		deal(target, sbDamage, kind, attacker, path, true, true);
	}

	/**
	 * As {@link #deal}, but without noting the attacker for boss aggro.  Arrows deliberately do NOT set the aggro
	 * target - only melee and mage-beam hits do.
	 */
	public static void dealNoAggro(LivingEntity target, double sbDamage, DamageKind kind, Player attacker,
			DamagePath path) {
		deal(target, sbDamage, kind, attacker, path, true, false);
	}

	/**
	 * A secondary instance - a Cleave hit or a proc.  It goes through the same boundary as the main hit, but does
	 * not itself generate Cleave or procs: one level of propagation, always.
	 */
	public static void dealSecondary(LivingEntity target, double sbDamage, DamageKind kind, Player attacker) {
		deal(target, sbDamage, kind, attacker, DamagePath.MELEE, false, false);
	}

	private static void deal(LivingEntity target, double sbDamage, DamageKind kind, Player attacker, DamagePath path,
			boolean primary, boolean aggro) {
		if(target == null || sbDamage <= 0) return;

		// Targets that must never be touched at all, checked before anything else.
		// Villager NPCs (Mort / the Wizard) never take plugin-dealt damage.  Blocking it here rather than only in
		// MiscListener matters: this used to hit with genericKill, the exact source vanilla's /kill uses, so the
		// two were indistinguishable once they reached the damage event.  Keeping ability damage away from
		// villagers at the source is what lets a KILL-cause event on a villager mean a real /kill.
		if(target instanceof Villager) return;
		// The Watcher cannot be damaged at all; the fight is won by killing its Undeads.
		if(target.getScoreboardTags().contains("TASWatcher")) return;
		// A blood mob is shielded for its first ~2 ticks so a spawn-tick arrow can't kill it before it registers
		// toward progress.  This used to be enforced only in MiscListener.onWatcherDamage, i.e. on the vanilla
		// damage event; nothing on this path fires one, so the guard has to live here too.
		if(target.getScoreboardTags().contains("WatcherMobSpawning")) return;
		// Aggro is noted BEFORE the immunity returns below: a boss should aggro whoever was hitting it through an
		// armoured window, the moment that window ends.
		if(aggro && attacker != null && target instanceof Wither
				&& target.getScoreboardTags().contains("TASWither")) {
			WitherActions.noteDamager(attacker);
		}

		// An armoured wither takes nothing.  Every call site already checks this, but it belongs at the single
		// boundary as well so a Cleave hit or a proc can't slip past one.  WithersNotImmuneToArrows' deliberate
		// "vulnerable then re-armoured on the same tick" exception clears the counter itself before calling in,
		// so it is unaffected.
		if(target instanceof Wither armoured && armoured.getInvulnerableTicks() != 0) return;

		// The Wither King is immune to all direct player damage.  Its HP is driven solely by dragon kills.  Aggro
		// is still noted above, and the debuffs the hit carried have already landed at the call site.
		if(target.getScoreboardTags().contains("TASWitherKing")) return;

		double defense = TargetDebuffs.reducedDefense(target, MobStats.defenseOf(target));
		double resistance = MobStats.resistanceOf(target);
		double mcDamage = sbDamage * resistance / Scale.defenseDivisor(defense) / Scale.SB_PER_MC_HP;

		// The hurt sound is judged on the PRE-clamp damage, i.e. "did this hit do anything?".  Otherwise a hit
		// clamped to 0 - once Maxor's 75% or Storm's 55% stun cap is reached - would silently go quiet.
		witherHurtSound(target, attacker, mcDamage);

		// The bosses' own clamps (Maxor's 75% stun cap, Storm's 55% crush cap, Necron's thresholds, Goldor's
		// patrol immunity, every dying state).  Called explicitly, since no EntityDamageEvent fires for our damage.
		if(target instanceof Wither wither) {
			WitherLord lord = WitherLord.activeFor(wither);
			if(lord != null) mcDamage = lord.clampDamage(mcDamage);
		}

		double healthBefore = target.getHealth();
		if(mcDamage > 0) {
			// Belt and braces.  With direct health manipulation vanilla's invulnerability window is not consulted
			// at all, but a mob that took vanilla damage a tick earlier would otherwise still be carrying one.
			target.setNoDamageTicks(0);
			target.setHealth(Math.max(0, healthBefore - mcDamage));
			// setHealth bypasses the vanilla damage path, so the red hurt flash never plays.  Send it ourselves.
			Utils.broadcastPacket(new ClientboundHurtAnimationPacket(((CraftLivingEntity) target).getHandle()));
			Utils.changeName(target);
		}

		// Kill chokepoints.  No event fires on this path, so the deaths that other systems watch for are detected
		// here.  Gated on the target having been ALIVE before this hit, so a Cleave hit or a proc landing on a
		// corpse cannot re-arm the post-kill buff or inflate the combo; both handlers below are idempotent anyway.
		if(healthBefore > 0 && target.getHealth() <= 0) {
			if(target.getScoreboardTags().contains("WatcherMob")) {
				instructions.bosses.Watcher.INSTANCE.registerMobKill(target);
			}
			if(target instanceof org.bukkit.entity.EnderDragon dragon
					&& dragon.getScoreboardTags().contains("WitherKingDragon")) {
				instructions.bosses.witherking.WitherKing.handleDragonKilled(dragon);
			}
			if(attacker != null) CombatState.noteKill(attacker);
			TargetDebuffs.forget(target);
		}

		if(attacker != null && primary) {
			// Only PRIMARY instances go into the rolling history.  Its consumers all ask for a best HIT (Berserk's
			// axe throw, Explosive Shot, Rapid Fire, Venomous's DPS term), and feeding a proc's own output back in
			// would compound: a Venomous tick would raise the history, raising the next tick, and so on.
			CombatState.recordDamage(attacker, sbDamage);
			CombatState.noteHit(attacker, target.getUniqueId(), path);
			CombatState.spendPostKillBuff(attacker);
		}

		DamageNumbers.show(target, mcDamage * Scale.SB_PER_MC_HP, kind, attacker);
		verbose(attacker, target, sbDamage, mcDamage, defense, resistance, kind);
		if(primary) {
			Procs.onHit(attacker, target, sbDamage, path);
			Cleave.spread(attacker, target, sbDamage, path);
		}
	}

	/**
	 * A boss wither's hurt noise.
	 * <p>
	 * This used to hang off {@code EntityDamageEvent} in {@code MiscListener.onWitherHurtSound}, which the
	 * unified path stopped firing - so it moved here, to the one place every hit passes through.  Three rules
	 * carried over verbatim:
	 * <ul>
	 *   <li>judged on the PRE-clamp damage, so a hit clamped to 0 by a stun cap still sounds;</li>
	 *   <li>silent while the boss is dying, when only the death noise plays;</li>
	 *   <li>silent for a mage beam, which routes its own constant-volume sound to the beamer, so an at-location
	 *       copy would double up and be distance-attenuated.</li>
	 * </ul>
	 */
	private static void witherHurtSound(LivingEntity target, Player attacker, double preClampDamage) {
		if(!(target instanceof Wither wither) || preClampDamage <= 0) return;
		WitherLord lord = WitherLord.activeFor(wither);
		if(lord != null && lord.isDying()) return;
		if(listeners.CustomItems.beamDamageInProgress) return;

		org.bukkit.Location loc = wither.getLocation();
		wither.getWorld().playSound(loc, org.bukkit.Sound.ENTITY_WITHER_HURT, 1.0f, 1.0f);
		if(attacker != null) attacker.playSound(loc, org.bukkit.Sound.ENTITY_WITHER_HURT, 1.0f, 1.0f);
	}

	// ===================== §7a verbose breakdowns =====================

	private static void verbose(Player attacker, LivingEntity target, double sbDamage, double mcDamage,
			double defense, double resistance, DamageKind kind) {
		if(Utils.getVerboseLevel().ordinal() < Utils.VerboseLevel.ON.ordinal()) return;
		double postDefense = mcDamage * Scale.SB_PER_MC_HP;
		if(!Utils.isSuperVerbose()) {
			// `on`: the final full damage, then the damage after defense.  Two numbers, nothing else.
			Utils.debug(Utils.DebugType.BOSS, integer(sbDamage) + " -> " + integer(postDefense) + " after defense");
			return;
		}
		// `super`: the entire calculation.  Built ONLY at this level - a full breakdown per hit would be thousands
		// of string concatenations a second at Terminator fire rates.
		StringBuilder sb = new StringBuilder();
		sb.append(kind).append(' ').append(attacker == null ? "?" : Utils.getRealName(attacker))
				.append(" -> ").append(target.getName());
		sb.append("\n  full ").append(integer(sbDamage));
		sb.append("\n  x resistance ").append(Utils.round(resistance, 2));
		sb.append("\n  defense ").append(Utils.round(MobStats.defenseOf(target), 1))
				.append(" -> ").append(Utils.round(defense, 2))
				.append(" (/").append(Utils.round(Scale.defenseDivisor(defense), 4)).append(')');
		sb.append("\n  = ").append(integer(postDefense)).append(" SB, ").append(Utils.round(mcDamage, 4)).append(" HP");
		Utils.debug(Utils.DebugType.BOSS, sb.toString());
	}

	/** A full integer with every digit, never abbreviated - the same rule the floating numbers follow (§7a). */
	public static String integer(double value) {
		return String.valueOf((long) Math.floor(value));
	}

	/**
	 * The full itemised stat breakdown for {@code /verbose super} and {@code /eq}.  Separate from the per-hit
	 * breakdown above because it is per player, not per hit.
	 */
	public static Map<String, StatBlock> statBreakdown(Player p, DamagePath path) {
		return new LinkedHashMap<>(Stats.breakdown(p, path));
	}
}
