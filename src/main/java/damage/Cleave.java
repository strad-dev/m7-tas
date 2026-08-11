package damage;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;

/**
 * Cleave, the sweep mechanic (DAMAGE_PLAN.md §7).
 * <p>
 * <b>The radius is measured from the HIT ENEMY, not from the player.</b>  That settles the old "radius origin"
 * question and is what makes a Berserk's reach matter twice over: its swing range extends both the reach attribute
 * and this radius, so the two move together (3.0 -> 8.0 and 4.8 -> 9.8).
 *
 * <table>
 *   <caption>Cleave by class</caption>
 *   <tr><th>Class</th><th>Damage to nearby mobs</th><th>Radius from the hit enemy</th></tr>
 *   <tr><td>Everyone except Berserk</td><td>30% of the main hit</td><td>4.8 blocks</td></tr>
 *   <tr><td>Berserk</td><td>100% of the main hit</td><td>4.8 + swing range = 9.8 (10.3 solo)</td></tr>
 * </table>
 *
 * Cleave is a sword enchantment, so a bow never sweeps (§7's bow exclusion list).  A Cleave hit must <b>never</b>
 * generate its own Cleave - one level of propagation, always - which {@link Damage#dealSecondary} enforces by
 * marking the instance non-primary.
 * <p>
 * <b>Nor does the MAGE BEAM sweep</b>, even though {@link DamagePath#isMelee()} is true for it.  That flag answers
 * "does the sword enchantment list apply?", which for the beam is yes - it is a melee attack for Sharpness, Smite,
 * First Strike and the rest.  Cleave is a different question: the beam is a single-target ranged hit that goes where
 * the crosshair points, so <b>only the entity it hits takes damage</b>.  Testing {@code isMelee()} here quietly gave
 * every Mage a free 30% sweep on every beam, which is why the test below is now the exact path rather than the
 * category.
 * <p>
 * I-frames do not swallow it: mobs have zero i-frames, so a Cleave hit lands in full alongside the main hit.
 */
public final class Cleave {
	private Cleave() {}

	/** The base radius, before a Berserk's swing range is added. */
	private static final double BASE_RADIUS = 4.8;
	private static final double SHARE = 0.30;
	private static final double BERSERK_SHARE = 1.00;

	/** Spread a primary melee hit to everything else within the radius of the target it landed on. */
	public static void spread(Player attacker, LivingEntity hit, double sbDamage, DamagePath path) {
		if(attacker == null || hit == null || sbDamage <= 0) return;
		if(path != DamagePath.MELEE) return;                         // an actual SWING - not a bow, not the mage beam

		DungeonClass clazz = DungeonClass.of(attacker);
		boolean berserk = clazz == DungeonClass.BERSERK;
		boolean solo = DungeonClass.isSoloOnClass(attacker);
		double radius = BASE_RADIUS + ClassBonuses.swingRange(clazz, solo);
		double share = berserk ? BERSERK_SHARE : SHARE;
		double each = sbDamage * share;

		for(Entity e : hit.getNearbyEntities(radius, radius, radius)) {
			if(!(e instanceof LivingEntity other) || other instanceof Player || other.equals(hit)) continue;
			if(other.isDead() || other.getHealth() <= 0) continue;
			// An armoured wither takes nothing, the same rule the direct hit paths use.
			if(other instanceof Wither w && w.getInvulnerableTicks() != 0) continue;
			Damage.dealSecondary(other, each, DamageKind.CLEAVE, attacker);
		}
	}
}
