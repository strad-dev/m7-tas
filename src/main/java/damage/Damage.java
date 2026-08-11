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
		Breakdown b = Breakdown.begin();
		return finish(p, target, DamagePath.MELEE, def, statCore(p, DamagePath.MELEE, true, b), null, b);
	}

	/**
	 * A mage beam: the melee hit rescaled by the Mage Staff passive, then faded by distance.  It is not a separate
	 * damage path in the enchantment sense - it counts as a melee attack and takes the whole sword list.
	 */
	public static double beam(Player p, LivingEntity target, ItemStack weapon, double distance) {
		ItemDef def = Items.of(weapon);
		applyOnHitDebuffs(p, target, DamagePath.BEAM, def);
		StatBlock stats = Stats.of(p, DamagePath.BEAM);
		Breakdown b = Breakdown.begin();
		double core = statCore(p, DamagePath.BEAM, true, b);
		double beamMultiplier = BEAM_BASE + BEAM_PER_INTELLIGENCE * stats.get(Stat.INTELLIGENCE);
		BeamRange range = beamRange(p);
		double falloff = range.falloff(distance);
		if(b != null) {
			// The beam multiplier is labelled "Intelligence" because that is what it reads as: 0.30 + 0.09% per point,
			// so at these Intelligence levels the 0.30 is a rounding error and the line is effectively the Int term.
			b.factor("Intelligence", beamMultiplier);
			if(falloff < 1.0) b.factor("Distance falloff", falloff);
		}
		return finish(p, target, DamagePath.BEAM, def, core * beamMultiplier * falloff, null, b);
	}

	/**
	 * The STAT half of a melee hit, with no target involved.  Exists for the thrown-axe abilities, which decide
	 * their damage as a share of the wielder's melee at THROW time and only learn their target later.
	 */
	public static double meleeCore(Player p) {
		return statCore(p, DamagePath.MELEE, true, null);
	}

	/** The TARGET-dependent half of a melee hit, applied when a thrown axe actually connects. */
	public static double meleeFinish(Player p, LivingEntity target, ItemStack weapon, double core) {
		ItemDef def = Items.of(weapon);
		applyOnHitDebuffs(p, target, DamagePath.MELEE, def);
		Breakdown b = Breakdown.begin();
		// The stat half was settled at throw time, on an earlier tick, so the breakdown can only show it as the one
		// number it already is - hence "Stat core" rather than a Base/Strength/Crit Damage decomposition.
		if(b != null) b.base("Stat core", core);
		return finish(p, target, DamagePath.MELEE, def, core, null, b);
	}

	/**
	 * The STAT half of a bow shot, which {@link Arrows} stamps on the projectile at fire time so a mid-flight
	 * weapon swap cannot change what the arrow hits for (§1.0.5).
	 *
	 * @param crit false only for a partially drawn bow, which loses the whole crit term rather than a fraction
	 */
	public static double bowCore(Player p, boolean crit) {
		return statCore(p, DamagePath.BOW, crit, null);
	}

	/**
	 * The TARGET-dependent half of a bow shot, applied when the arrow lands.  None of it is knowable at fire time.
	 *
	 * @param blocksTravelled how far the arrow flew, for Snipe IV's +4% per 10 blocks
	 */
	public static double bowFinish(Player p, LivingEntity target, ItemDef weapon, double core,
			double blocksTravelled, boolean headshot) {
		Breakdown b = Breakdown.begin();
		// Stamped on the arrow at fire time, so - as with a thrown axe - it can only be shown as one figure.
		if(b != null) b.base("Stat core", core);
		return finish(p, target, DamagePath.BOW, weapon, core, new BowContext(blocksTravelled, headshot), b);
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
		double charge = Math.max(0, Math.min(chargeFraction, 1.0));
		// Deliberately NOT bowCore + bowFinish, even though the numbers are identical.  Both halves resolve in this
		// one call, so this path CAN show a full /verbose super breakdown, where bowFinish can only ever report the
		// stat core as one pre-decided figure - it is normally reached a tick or more after the arrow was stamped.
		Breakdown b = Breakdown.begin();
		double core = statCore(p, DamagePath.BOW, full, b) * charge;
		if(b != null && charge < 1.0) b.factor("Draw", charge);
		return finish(p, target, DamagePath.BOW, def, core, new BowContext(blocksTravelled, headshot), b);
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
		double intelligence = 1.0 + (stats.get(Stat.INTELLIGENCE) / 100.0) * def.ability().intelligenceScaling();
		double abilityDamage = 1.0 + stats.get(Stat.ABILITY_DAMAGE) / 100.0;
		double core = def.ability().baseDamage() * intelligence * abilityDamage;
		Breakdown b = Breakdown.begin();
		if(b != null) {
			// No Strength and no Crit Damage rows here, because the ability formula genuinely has neither - printing
			// them at x1 would suggest they were considered and came out neutral.
			b.base("Base Damage", def.ability().baseDamage());
			b.factor("Intelligence", intelligence);
			b.factor("Ability Damage", abilityDamage);
		}
		return finish(p, target, DamagePath.ABILITY, def, core, null, b);
	}

	/** Extra inputs only the bow path has. */
	private record BowContext(double blocksTravelled, boolean headshot) {}

	/**
	 * The granularity health is actually moved by, in Minecraft health: <b>thousandths of a health point</b>.
	 * <p>
	 * One health point is a million SkyBlock damage, so a thousandth is a thousand - fine enough that nothing at this
	 * scale notices the rounding, coarse enough that health stops being a 15-significant-digit double nobody can
	 * reason about.  Tenths and hundredths were the alternatives; thousandths loses the least.
	 * <p>
	 * This affects the STORED HEALTH only.  What the floating number and {@code /verbose} report is the unrounded
	 * figure, so the two can differ by up to half a step - deliberately, because one is a storage decision and the
	 * other is the answer to "how hard did I just hit that".
	 */
	private static final double HP_STEP = 0.001;

	/** Quantise one hit to {@link #HP_STEP}.  Cheap on purpose - this runs on every instance at Terminator rates. */
	private static double roundHp(double mcDamage) {
		return Math.round(mcDamage / HP_STEP) * HP_STEP;
	}

	/**
	 * The stat half of the melee/bow shape.  {@code crit} is true for everything except a partially drawn bow -
	 * §7 rules every hit a crit, deliberately, because a random roll would make two identical runs incomparable.
	 */
	private static double statCore(Player p, DamagePath path, boolean crit, Breakdown b) {
		StatBlock stats = Stats.of(p, path);
		double base = Scale.PLAYER_BASE_DAMAGE + stats.get(Stat.DAMAGE);
		double strength = 1.0 + stats.get(Stat.STRENGTH) / 100.0;
		double critDamage = crit ? 1.0 + stats.get(Stat.CRIT_DAMAGE) / 100.0 : 1.0;
		if(b != null) {
			b.base("Base Damage", base);
			b.factor("Strength", strength);
			// A partly drawn bow loses the crit term entirely rather than scaling it, so there is no row at all -
			// which is the point worth seeing in the breakdown.
			if(crit) b.factor("Crit Damage", critDamage);
		}
		return base * strength * critDamage;
	}

	/** The damage-level stage: one additive factor, then the multiplicative product. */
	private static double finish(Player p, LivingEntity target, DamagePath path, ItemDef weapon, double core,
			BowContext bow, Breakdown b) {
		if(target == null || core <= 0) return 0;
		double additive = additivePercent(p, target, path, weapon, bow);
		double multiplicative = multiplicative(p, target, path, weapon);
		double total = core * (1.0 + additive / 100.0) * multiplicative;
		if(b != null) {
			b.factor("Additive Damage", 1.0 + additive / 100.0);
			b.factor("Multiplicative Damage", multiplicative);
			b.complete(total);
		}
		return total;
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

	/**
	 * Deal a computed SkyBlock-scale hit.  Counts as the primary instance for procs and Cleave.
	 * <p>
	 * <b>Aggro is pulled only if the hit did real damage.</b>  Melee, beam, bow and ability all follow the same rule:
	 * a hit that took health off a boss makes you its target, and one that took nothing does not - not on an armoured
	 * Maxor, and not on Goldor mid-terminals or Necron mid-interlude, where the hit is deliberately feedback-only and
	 * clamped away.  The three abilities that may aggro a <i>fully invulnerable</i> wither anyway (the mage beam, the
	 * thrown-axe projectiles and the Flaming Flay arc) do it at their own armour checks, before calling in here -
	 * which is the only place that state is visible as more than "the damage was zero".
	 * <p>
	 * There used to be a {@code dealNoAggro} for arrows, on the rule "arrows deliberately do NOT set the aggro target
	 * - only melee and mage-beam hits do".  That rule is gone; arrows and melee are the same case now, so the two
	 * methods collapsed back into this one.
	 */
	public static void deal(LivingEntity target, double sbDamage, DamageKind kind, Player attacker, DamagePath path) {
		deal(target, sbDamage, kind, attacker, path, true, true);
	}

	/**
	 * A secondary instance - a Cleave hit or a proc.  It goes through the same boundary as the main hit, but does
	 * not itself generate Cleave or procs: one level of propagation, always.  Never pulls aggro, which
	 * {@link DamageKind#pullsAggro} enforces as well.
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
		// Aggro used to be noted HERE, ahead of the immunity returns below, so a boss chased whoever was hitting it
		// through an armoured window the moment that window ended.  It is now noted further down, inside the branch
		// where health actually moved: a hit that deals nothing does not pull aggro.  The three abilities that DO
		// aggro through a full shield note it themselves, at the armour check they already have.

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
		double preClamp = mcDamage;

		// The hurt sound is judged on the PRE-clamp damage, i.e. "did this hit do anything?".  Otherwise a hit
		// clamped to 0 - once Maxor's 75% or Storm's 55% stun cap is reached - would silently go quiet.
		witherHurtSound(target, attacker, mcDamage, kind);

		// The bosses' own clamps (Maxor's 75% stun cap, Storm's 55% crush cap, Necron's thresholds, Goldor's
		// patrol immunity, every dying state).  Called explicitly, since no EntityDamageEvent fires for our damage.
		//
		// A boss in a FEEDBACK-ONLY window (Goldor on patrol during the terminals, Necron mid-frenzy or
		// mid-fireballs) is a deliberate exception to "the number shown is what the target actually lost": it takes
		// nothing, but the hit still displays the figure it would have done, so a player working the terminals or
		// waiting out an interlude can still read their own damage.  Every other clamp - the stun caps, the
		// thresholds, the dying states - keeps showing the clamped number, because there the health bar really did
		// move by that much.
		boolean showPreClamp = false;
		if(target instanceof Wither wither) {
			WitherLord lord = WitherLord.activeFor(wither);
			if(lord != null) {
				showPreClamp = lord.showsUnclampedDamage();
				mcDamage = lord.clampDamage(mcDamage);
			}
		}

		// What HEALTH moves by is quantised to HP_STEP, after the clamps, so a boss's HP stays a number you can
		// reason about rather than a 15-significant-digit double.  mcDamage itself is left ALONE, because the
		// floating number and /verbose report the TRUE figure - reading your real damage is the whole point of them,
		// and rounding it first would be reporting the storage format instead of the hit.
		double applied = roundHp(mcDamage);

		double healthBefore = target.getHealth();
		if(applied > 0) {
			// Belt and braces.  With direct health manipulation vanilla's invulnerability window is not consulted
			// at all, but a mob that took vanilla damage a tick earlier would otherwise still be carrying one.
			target.setNoDamageTicks(0);
			target.setHealth(Math.max(0, healthBefore - applied));
			// setHealth bypasses the vanilla damage path, so the red hurt flash never plays.  Send it ourselves.
			Utils.broadcastPacket(new ClientboundHurtAnimationPacket(((CraftLivingEntity) target).getHandle()));
			Utils.changeName(target);
			// Aggro, from inside the "health actually moved" branch - that IS the rule, for every path.  A hit worth
			// zero, whether clamped by a stun cap or swallowed by a feedback-only window, does not redirect the fight.
			if(aggro) noteAggro(target, attacker, kind);
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

		// What SkyBlock would show: the hit AFTER resistance, the defense divisor and the boss clamp - i.e. what the
		// target actually lost.  What it must NOT be is quantised: `applied` is the HP_STEP-rounded figure health
		// moves by, and reporting that put a number ending in three zeros in the air on every single hit.  So the
		// unrounded `mcDamage` feeds the display, and `applied` feeds setHealth, and they are not the same variable.
		DamageNumbers.show(target, (showPreClamp ? preClamp : mcDamage) * Scale.SB_PER_MC_HP, kind, attacker);
		verbose(attacker, target, sbDamage, mcDamage, defense, resistance, kind);
		if(primary) {
			Procs.onHit(attacker, target, sbDamage, path);
			Cleave.spread(attacker, target, sbDamage, path);
		}
	}

	/**
	 * Make {@code attacker} the boss's aggro target, if this hit is allowed to.
	 * <p>
	 * Called only from inside the "health actually moved" branch, so <b>a hit worth zero never reaches it</b>.  The
	 * per-KIND gate lives here too: <b>only a direct hit pulls aggro</b>, so Fire Aspect, Venomous, Thunderlord and a
	 * Cleave sweep never do, whatever the caller passed.  Only the four boss withers have an aggro target at all,
	 * hence the TASWither check.
	 */
	private static void noteAggro(LivingEntity target, Player attacker, DamageKind kind) {
		if(attacker == null || !kind.pullsAggro()) return;
		if(!(target instanceof Wither) || !target.getScoreboardTags().contains("TASWither")) return;
		WitherActions.noteDamager(attacker);
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
	 * And one rule that is new: <b>only a DIRECT hit sounds</b>.  The damage-over-time kinds each fire five
	 * instances off one swing, so letting them ring turned a single melee hit into six overlapping hurt noises and a
	 * Terminator volley into a wall of them.  See {@link DamageKind#playsHurtSound}.
	 */
	private static void witherHurtSound(LivingEntity target, Player attacker, double preClampDamage, DamageKind kind) {
		if(!(target instanceof Wither wither) || preClampDamage <= 0) return;
		if(!kind.playsHurtSound()) return;
		WitherLord lord = WitherLord.activeFor(wither);
		if(lord != null && lord.isDying()) return;
		if(listeners.CustomItems.beamDamageInProgress) return;

		org.bukkit.Location loc = wither.getLocation();
		wither.getWorld().playSound(loc, org.bukkit.Sound.ENTITY_WITHER_HURT, 1.0f, 1.0f);
		if(attacker != null) attacker.playSound(loc, org.bukkit.Sound.ENTITY_WITHER_HURT, 1.0f, 1.0f);
	}

	// ===================== §7a verbose breakdowns =====================

	/**
	 * The factored breakdown of one hit: the formula's own terms, in the order they multiply, rather than only its
	 * answer.
	 * <p>
	 * It is <b>threaded explicitly</b> through the formula methods rather than reconstructed afterwards, because
	 * {@link #deal} is handed a single finished double and there is no way to decompose that back into base x
	 * Strength x Crit Damage x additive x multiplicative.
	 * <p>
	 * {@link #begin()} returns <b>null</b> unless {@code /verbose super} is on, and every producer is null-guarded, so
	 * the normal path allocates nothing and formats nothing - a per-hit breakdown at Terminator fire rates would be
	 * thousands of string concatenations a second.
	 * <p>
	 * {@link #complete} parks the finished object in {@link #lastBreakdown} for {@link #verbose} to pick up, since the
	 * formula call and the {@code deal} call are separate.  Main-thread only, and {@code verbose} both CONSUMES it and
	 * checks the total matches the hit in front of it - which is what stops a Cleave hit or a Venomous tick, neither
	 * of which runs a formula at all, from printing the previous hit's rows as its own.
	 */
	private static final class Breakdown {
		private String baseLabel = "Base Damage";
		private double baseValue;
		private final java.util.List<String> rows = new java.util.ArrayList<>();
		private double total;

		static Breakdown begin() {
			return Utils.isSuperVerbose() ? new Breakdown() : null;
		}

		void base(String label, double value) {
			baseLabel = label;
			baseValue = value;
		}

		void factor(String label, double value) {
			rows.add(label + ": " + factorText(value));
		}

		void complete(double total) {
			this.total = total;
			lastBreakdown = this;
		}
	}

	private static Breakdown lastBreakdown;

	private static void verbose(Player attacker, LivingEntity target, double sbDamage, double mcDamage,
			double defense, double resistance, DamageKind kind) {
		// Consumed unconditionally, whatever the verbose level: leaving it parked would let the NEXT hit that runs no
		// formula of its own - a proc, a Cleave sweep - inherit these rows.
		Breakdown b = lastBreakdown;
		lastBreakdown = null;
		if(Utils.getVerboseLevel().ordinal() < Utils.VerboseLevel.ON.ordinal()) return;

		double finalDamage = mcDamage * Scale.SB_PER_MC_HP;
		double defenseFactor = 1.0 / Scale.defenseDivisor(defense);

		if(!Utils.isSuperVerbose()) {
			// `on`: the total, the two target-side reductions AS ONE factor, and the result.  Three lines.
			Utils.debug(Utils.DebugType.BOSS, "Total Damage: " + integer(sbDamage)
					+ "\n  Defense & Boss Multiplier: " + factorText(defenseFactor * resistance)
					+ "\n  Final Damage: " + integer(finalDamage));
			return;
		}

		// `super`: every term. The player-side rows come from the Breakdown, so a path that genuinely has no Strength
		// or Crit Damage term (an ability) simply has no such row - rather than a misleading x1.
		StringBuilder sb = new StringBuilder();
		sb.append(kind).append(' ').append(attacker == null ? "?" : Utils.getRealName(attacker))
				.append(" -> ").append(target.getName());
		if(b != null && Math.abs(b.total - sbDamage) <= 1e-6) {
			sb.append("\n  ").append(b.baseLabel).append(": +").append(trimZeros(Utils.roundCommas(b.baseValue, 2)));
			for(String row : b.rows) sb.append("\n  ").append(row);
		}
		sb.append("\n  Total Damage: ").append(integer(sbDamage));
		sb.append("\n  Defense (").append(defenseText(target, defense)).append("): ").append(factorText(defenseFactor));
		sb.append("\n  Boss Multiplier: ").append(factorText(resistance));
		sb.append("\n  Final Damage: ").append(integer(finalDamage));
		Utils.debug(Utils.DebugType.BOSS, sb.toString());
	}

	/**
	 * The defense figure for the {@code Defense (...)} row: the EFFECTIVE value, or {@code raw -> effective} when
	 * Lethality and Last Breath have actually reduced it, since which of the two is being read is the whole question
	 * when a defense number looks wrong.
	 */
	private static String defenseText(LivingEntity target, double effective) {
		double raw = MobStats.defenseOf(target);
		String shown = trimZeros(Utils.roundCommas(effective, 2));
		if(Math.abs(raw - effective) <= 1e-6) return shown;
		return trimZeros(Utils.roundCommas(raw, 2)) + " -> " + shown;
	}

	/** One multiplier as it reads in the breakdown: {@code x100}, {@code x1.05}, {@code x0.0769}. */
	private static String factorText(double value) {
		return "x" + trimZeros(Utils.roundCommas(value, 4));
	}

	/** Drop a trailing {@code .0000} / {@code .10} so a round factor reads as {@code x100}, not {@code x100.0000}. */
	private static String trimZeros(String s) {
		if(s.indexOf('.') < 0) return s;
		int end = s.length();
		while(end > 0 && s.charAt(end - 1) == '0') end--;
		if(end > 0 && s.charAt(end - 1) == '.') end--;
		return s.substring(0, end);
	}

	/**
	 * A full integer with every digit, never abbreviated - the same rule the floating numbers follow (§7a) - and
	 * thousands-separated, because {@code 726525143} is unreadable at a glance and {@code 726,525,143} is not.
	 * {@code Locale.ROOT} so the separator is a comma on every host, not a dot or a space.
	 */
	public static String integer(double value) {
		return String.format(java.util.Locale.ROOT, "%,d", (long) Math.floor(value));
	}

	/**
	 * The full itemised stat breakdown for {@code /verbose super} and {@code /eq}.  Separate from the per-hit
	 * breakdown above because it is per player, not per hit.
	 */
	public static Map<String, StatBlock> statBreakdown(Player p, DamagePath path) {
		return new LinkedHashMap<>(Stats.breakdown(p, path));
	}
}
