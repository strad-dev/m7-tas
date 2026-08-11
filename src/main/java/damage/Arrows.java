package damage;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import plugin.M7tas;

/**
 * Arrow damage (DAMAGE_PLAN.md §1.0.5).
 * <p>
 * <b>Arrows carry their damage value with them.</b>  The STAT half of a shot is resolved at fire time and stamped
 * on the projectile, so swapping to a stronger weapon while an arrow is in flight cannot change what it hits for.
 * Real Hypixel computes the whole thing later, which is what the arrow-damage tech exploits; we deliberately do not
 * reproduce that.
 * <p>
 * What is <b>not</b> stamped is the target-dependent half - the Rulers, Titan Killer, Prosecute, the type
 * enchantments, the target's own debuffs - because none of that is knowable at fire time.  Those resolve at
 * {@link #resolve} against whatever the arrow actually hit.
 * <p>
 * Also stamped: the weapon's name, so the reforge that fired it still decides the Precise headshot bonus, and the
 * origin, so Snipe IV's +4% per 10 blocks reads real flight distance rather than an estimate.
 */
public final class Arrows {
	private Arrows() {}

	private static final NamespacedKey CORE = key("arrow_core");
	private static final NamespacedKey WEAPON = key("arrow_weapon");
	private static final NamespacedKey ORIGIN = key("arrow_origin");
	private static final NamespacedKey HITS = key("arrow_hits");
	/** 1 if this arrow may build a Last Breath stack, 0 if not.  See {@link #stamp}. */
	private static final NamespacedKey BUILDS_LAST_BREATH = key("arrow_last_breath");
	/** 1 if this arrow carries an ALREADY-FINISHED figure rather than a stat core.  See {@link #stampFlat}. */
	private static final NamespacedKey DERIVED = key("arrow_derived");

	/** Piercing I: an arrow passes through, and each mob AFTER the first takes 25% of its damage (§7). */
	private static final double PIERCING_SHARE = 0.25;

	private static NamespacedKey key(String name) {
		return new NamespacedKey(M7tas.getInstance(), name);
	}

	/**
	 * Stamp a freshly-fired arrow with its damage.
	 *
	 * @param chargeFraction vanilla's charge, {@code min(useTicks/20, 1)}.  1.0 for a shortbow (the Terminator is
	 *                       never drawn, so every shot is full damage and a crit) or a full draw.  A partial draw
	 *                       scales the damage AND loses the crit term entirely, which is what makes it much worse
	 *                       than the fraction alone suggests.
	 * @param share          a fraction of a normal arrow, for Duplex's extra arrow at x0.2
	 */
	public static void stamp(AbstractArrow arrow, Player shooter, ItemStack weapon, double chargeFraction,
			double share) {
		stamp(arrow, shooter, weapon, chargeFraction, share, true);
	}

	/**
	 * As above, with explicit control over whether this arrow may build a <b>Last Breath stack</b>.
	 * <p>
	 * Which arrows count is not simply "was it fired from the Last Breath":
	 * <ul>
	 *   <li>the Last Breath's own shot - <b>yes</b>;</li>
	 *   <li>its <b>Duplex</b> arrow - <b>yes</b>;</li>
	 *   <li>the Archer's two <b>bonus</b> arrows - <b>no</b>, which is the only case this flag exists for;</li>
	 *   <li>Explosive Shot and Rapid Fire arrows fired <b>while a Last Breath is held</b> - <b>yes</b>, and that
	 *       falls out on its own, because those abilities stamp whatever weapon is in the main hand.</li>
	 * </ul>
	 * The weapon still has to BE a Last Breath either way; this only ever takes the answer away.
	 */
	public static void stamp(AbstractArrow arrow, Player shooter, ItemStack weapon, double chargeFraction,
			double share, boolean buildsLastBreath) {
		if(arrow == null || shooter == null) return;
		boolean full = chargeFraction >= 1.0;
		double core = Damage.bowCore(shooter, full) * Math.max(0, Math.min(chargeFraction, 1.0)) * share;
		var pdc = arrow.getPersistentDataContainer();
		pdc.set(CORE, PersistentDataType.DOUBLE, core);
		ItemDef def = Items.of(weapon);
		pdc.set(WEAPON, PersistentDataType.STRING, def == null ? "" : def.displayName());
		Location o = arrow.getLocation();
		pdc.set(ORIGIN, PersistentDataType.STRING, o.getX() + "," + o.getY() + "," + o.getZ());
		pdc.set(HITS, PersistentDataType.INTEGER, 0);
		pdc.set(BUILDS_LAST_BREATH, PersistentDataType.INTEGER, buildsLastBreath ? 1 : 0);
		// Keep the vanilla field roughly in step so anything that reads it (a hit path we do not intercept) sees a
		// sane number rather than the default 2.0.  Our own paths never read it.
		arrow.setDamage(0);
	}

	/**
	 * Stamp an arrow whose damage is NOT the shooter's stat core but an already-decided figure - Rapid Fire's 75%
	 * of the player's best recent arrow (§1.14).
	 * <p>
	 * <b>A figure copied out of the rolling history is a FINISHED hit, so the target half must not run on it again.</b>
	 * The best-arrow-in-the-last-minute it copied already had the Rulers, Titan Killer, Snipe, Power, the class
	 * multiplier and everything else in it; putting the stamped number back through {@link Damage#bowFinish} charges
	 * for all of that a second time, which is a x4-x10 on the ability before anything else goes wrong.  What DOES
	 * still happen at {@link #resolve} is the debuffs the arrow applies (Rapid Fire arrows off a Last Breath build
	 * its stacks, §1.14) and Piercing's 25% share, because those are properties of the projectile, not of the
	 * damage formula.
	 * <p>
	 * Combined with {@code deal}'s history feedback, the second charge was also compounding: each arrow re-read the
	 * history, multiplied it by the target half, landed several times the figure it read and recorded THAT, so 50
	 * arrows over 200 ticks walked the number up until it overflowed.  Both halves of that are fixed, and both
	 * halves are needed - either one alone still leaves the ability several times too strong.
	 */
	public static void stampFlat(AbstractArrow arrow, Player shooter, ItemStack weapon, double core) {
		if(arrow == null || shooter == null) return;
		var pdc = arrow.getPersistentDataContainer();
		pdc.set(CORE, PersistentDataType.DOUBLE, core);
		ItemDef def = Items.of(weapon);
		pdc.set(WEAPON, PersistentDataType.STRING, def == null ? "" : def.displayName());
		Location o = arrow.getLocation();
		pdc.set(ORIGIN, PersistentDataType.STRING, o.getX() + "," + o.getY() + "," + o.getZ());
		pdc.set(HITS, PersistentDataType.INTEGER, 0);
		// Explosive Shot and Rapid Fire stamp the HELD weapon, so if that is a Last Breath these count as Last
		// Breath arrows, which is exactly the rule.
		pdc.set(BUILDS_LAST_BREATH, PersistentDataType.INTEGER, 1);
		pdc.set(DERIVED, PersistentDataType.INTEGER, 1);
		arrow.setDamage(0);
	}

	/** True if this arrow was stamped by us, i.e. {@link #resolve} can answer for it. */
	public static boolean isStamped(AbstractArrow arrow) {
		return arrow != null && arrow.getPersistentDataContainer().has(CORE, PersistentDataType.DOUBLE);
	}

	/**
	 * True if this arrow's damage came out of the rolling damage history rather than the shooter's stats, i.e. the
	 * hit it lands must go through {@link Damage#dealDerived} and not feed that history back.
	 */
	public static boolean isDerived(AbstractArrow arrow) {
		return arrow != null
				&& arrow.getPersistentDataContainer().getOrDefault(DERIVED, PersistentDataType.INTEGER, 0) == 1;
	}

	/**
	 * Resolve this arrow against {@code target} <b>and deal it</b>.  Every arrow hit goes through here so the
	 * derived-arrow rule can't be forgotten at a call site: a Rapid Fire arrow carries a figure read out of the
	 * damage history, so it must not feed that history back (see {@link Damage#dealDerived}).
	 *
	 * @return the reported hit, as {@link Damage#deal} defines it
	 */
	public static double hit(AbstractArrow arrow, Player shooter, LivingEntity target) {
		return hit(arrow, shooter, target, true);
	}

	/** As above, with {@code countPierce} as {@link #resolve(AbstractArrow, Player, LivingEntity, boolean)} means it. */
	public static double hit(AbstractArrow arrow, Player shooter, LivingEntity target, boolean countPierce) {
		double sbDamage = resolve(arrow, shooter, target, countPierce);
		return isDerived(arrow)
				? Damage.dealDerived(target, sbDamage, DamageKind.NORMAL, shooter, DamagePath.BOW)
				: Damage.deal(target, sbDamage, DamageKind.NORMAL, shooter, DamagePath.BOW);
	}

	/**
	 * The SkyBlock damage this arrow does to {@code target}, applying the debuffs it carries first (§7's ordering
	 * rule) and taking Piercing's 25% for every mob after the first.
	 * <p>
	 * <b>Private on purpose</b>: the figure and the way it must be dealt are one decision, so everything goes
	 * through {@link #hit} rather than resolving here and picking a {@code deal} of its own.
	 */
	private static double resolve(AbstractArrow arrow, Player shooter, LivingEntity target) {
		return resolve(arrow, shooter, target, true);
	}

	/**
	 * As above, with {@code countPierce} false for an ability that hits several mobs from ONE arrow and gives them
	 * all the same damage - the Explosive Bow's 3-block blast, where §1.9 says every mob in range takes the
	 * weapon's <b>full</b> damage.  Piercing is about an arrow passing THROUGH successive mobs, which is a
	 * different thing.
	 */
	private static double resolve(AbstractArrow arrow, Player shooter, LivingEntity target, boolean countPierce) {
		if(arrow == null || shooter == null || target == null) return 0;
		var pdc = arrow.getPersistentDataContainer();
		Double core = pdc.get(CORE, PersistentDataType.DOUBLE);
		if(core == null) return 0;

		ItemDef def = Items.byName(pdc.getOrDefault(WEAPON, PersistentDataType.STRING, ""));
		boolean buildsLastBreath = pdc.getOrDefault(BUILDS_LAST_BREATH, PersistentDataType.INTEGER, 1) == 1;
		Damage.applyOnHitDebuffs(shooter, target, DamagePath.BOW, def, buildsLastBreath);

		double piercing = 1.0;
		if(countPierce) {
			int priorHits = pdc.getOrDefault(HITS, PersistentDataType.INTEGER, 0);
			pdc.set(HITS, PersistentDataType.INTEGER, priorHits + 1);
			if(priorHits > 0) piercing = PIERCING_SHARE;
		}

		// A derived arrow's core is a finished hit copied out of the damage history, not a stat core, so the target
		// half is already inside it and running bowFinish here would charge for it twice.  See stampFlat.
		if(isDerived(arrow)) return core * piercing;

		return Damage.bowFinish(shooter, target, def, core, blocksTravelled(arrow), isHeadshot(arrow, target))
				* piercing;
	}

	/** How far this arrow has flown, for Snipe IV.  Zero if it was never stamped with an origin. */
	private static double blocksTravelled(AbstractArrow arrow) {
		String origin = arrow.getPersistentDataContainer().getOrDefault(ORIGIN, PersistentDataType.STRING, "");
		String[] parts = origin.split(",");
		if(parts.length != 3) return 0;
		try {
			Vector from = new Vector(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]),
					Double.parseDouble(parts[2]));
			return from.distance(arrow.getLocation().toVector());
		} catch(NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * Whether the arrow landed on the target's head, for the Precise reforge's +10%.  Approximated from the
	 * arrow's own position against the target's eye height, since a projectile hit reports no precise contact
	 * point.
	 */
	private static boolean isHeadshot(AbstractArrow arrow, LivingEntity target) {
		return arrow.getLocation().getY() >= target.getEyeLocation().getY() - 0.3;
	}
}
