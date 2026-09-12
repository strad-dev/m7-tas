package damage;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import plugin.M7tas;

/**
 * Damage carried by a <b>guided carrier</b> - the Spirit Sceptre's bat and the Mage's Guided Sheep.
 * <p>
 * <b>The same rule arrows follow (§1.0.5): the carrier carries its damage with it.</b>  The cast is finished the
 * moment the animal is spawned - the figure is worked out, written into the entity's PDC, and the click is over.
 * The flight ({@code items.ItemUtils.launchGuided}) knows nothing about damage, and the blast a second or two later
 * just reads what is stamped.  So turning, swapping weapons, losing a buff or dying mid-flight cannot change what
 * the carrier hits for, exactly as with a bow shot.
 * <p>
 * <b>Two stamp forms, and picking the wrong one is a real bug</b>, which is why {@link #hit} is the only way to
 * apply one:
 * <ul>
 *   <li>{@link #stamp} - an ITEM ability's stat core, still owed the target half.  Finished at
 *       {@link Damage#abilityFinish} and dealt as a primary hit.</li>
 *   <li>{@link #stampFlat} - an already-FINISHED figure, i.e. the Guided Sheep's flat class-level damage.  It goes
 *       through {@link Damage#dealDerived}: running the target half on it would charge for the Rulers, Smite and
 *       the class multiplier it never had, and letting it feed the rolling damage history would seed what
 *       Explosive Shot and Rapid Fire read.</li>
 * </ul>
 * The parallel with {@code damage/Arrows} is deliberate and goes as far as the key names; the two are separate
 * because an arrow is an {@code AbstractArrow} on the bow path and a carrier is a living animal on the ability path,
 * with none of the draw, pierce or Last Breath machinery.
 */
public final class GuidedCarriers {
	private GuidedCarriers() {}

	/** The stamped figure: a stat core, or a finished hit if {@link #DERIVED} is set. */
	private static final NamespacedKey CORE = key("guided_core");
	/** The weapon's plain display name, so the target half resolves against the item that actually fired it. */
	private static final NamespacedKey WEAPON = key("guided_weapon");
	/** 1 if {@link #CORE} is an ALREADY-FINISHED figure rather than a stat core.  See {@link #stampFlat}. */
	private static final NamespacedKey DERIVED = key("guided_derived");
	/** The ability's display name, for the "hit N enemies" line the blast prints.  Travels with the carrier too. */
	private static final NamespacedKey ABILITY = key("guided_ability");

	private static NamespacedKey key(String name) {
		return new NamespacedKey(M7tas.getInstance(), name);
	}

	/**
	 * Stamp a carrier with an ITEM ability's stat core, as {@link Damage#abilityCore} computed it at fire time.
	 *
	 * @param ability the ability's display name, for the blast's "hit N enemies" line
	 * @param weapon  the item that fired it, recorded by display name so the finish reads the right definition
	 */
	public static void stamp(LivingEntity carrier, String ability, ItemStack weapon, double core) {
		if(carrier == null) return;
		var pdc = carrier.getPersistentDataContainer();
		pdc.set(CORE, PersistentDataType.DOUBLE, core);
		ItemDef def = Items.of(weapon);
		pdc.set(WEAPON, PersistentDataType.STRING, def == null ? "" : def.displayName());
		pdc.set(DERIVED, PersistentDataType.INTEGER, 0);
		pdc.set(ABILITY, PersistentDataType.STRING, ability);
	}

	/** Stamp a carrier with an already-finished figure, which {@link #hit} deals derived.  See the class javadoc. */
	public static void stampFlat(LivingEntity carrier, String ability, double sbDamage) {
		if(carrier == null) return;
		var pdc = carrier.getPersistentDataContainer();
		pdc.set(CORE, PersistentDataType.DOUBLE, sbDamage);
		pdc.set(DERIVED, PersistentDataType.INTEGER, 1);
		pdc.set(ABILITY, PersistentDataType.STRING, ability);
	}

	/** The stamped ability name, or {@code ""} for a carrier nobody stamped. */
	public static String abilityName(LivingEntity carrier) {
		if(carrier == null) return "";
		return carrier.getPersistentDataContainer().getOrDefault(ABILITY, PersistentDataType.STRING, "");
	}

	/**
	 * Resolve a carrier's blast against one mob <b>and deal it</b>.  Everything goes through here rather than
	 * reading the stamp and picking a {@code deal} of its own, for the same reason {@code Arrows.hit} is the only
	 * entry point there: the figure and the way it must be dealt are one decision.
	 *
	 * @return the reported hit, as {@link Damage#deal} defines it, or 0 if nothing landed
	 */
	public static double hit(LivingEntity carrier, Player shooter, LivingEntity target) {
		if(carrier == null || shooter == null || target == null) return 0;
		var pdc = carrier.getPersistentDataContainer();
		Double core = pdc.get(CORE, PersistentDataType.DOUBLE);
		if(core == null || core <= 0) return 0;
		if(pdc.getOrDefault(DERIVED, PersistentDataType.INTEGER, 0) == 1) {
			return Damage.dealDerived(target, core, DamageKind.MAGIC, shooter, DamagePath.ABILITY);
		}
		ItemDef def = Items.byName(pdc.getOrDefault(WEAPON, PersistentDataType.STRING, ""));
		double sbDamage = Damage.abilityFinish(shooter, target, def, core);
		return Damage.deal(target, sbDamage, DamageKind.MAGIC, shooter, DamagePath.ABILITY);
	}
}
