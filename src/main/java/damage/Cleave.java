package damage;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;

/**
 * Cleave, the sweep mechanic (MAP.md §7).
 * <p>
 * Radius is measured from the HIT ENEMY, not the player. Berserk swing range extends both reach and this radius,
 * so they move together (3.0 -> 8.0 and 4.8 -> 9.8).
 *
 * <table>
 *   <caption>Cleave by class</caption>
 *   <tr><th>Class</th><th>Damage to nearby mobs</th><th>Radius from the hit enemy</th></tr>
 *   <tr><td>Everyone except Berserk</td><td>30% of the main hit</td><td>4.8 blocks</td></tr>
 *   <tr><td>Berserk</td><td>100% of the main hit</td><td>4.8 + swing range = 9.8 (10.3 solo)</td></tr>
 * </table>
 *
 * Sword enchant, so a bow never sweeps (§7). A Cleave hit never makes its own Cleave; {@link Damage#dealSecondary}
 * enforces that by marking it non-primary.
 * <p>
 * The MAGE BEAM doesn't sweep either, though {@link DamagePath#isMelee()} is true for it. That flag means "sword
 * enchants apply"; the beam is a single-target ranged hit. Testing {@code isMelee()} here gave every Mage a free 30%
 * sweep per beam, so the test is the exact path.
 * <p>
 * Mobs have zero i-frames, so a Cleave hit lands in full alongside the main hit.
 */
public final class Cleave {
	private Cleave() {}

	/** Before a Berserk's swing range is added. */
	private static final double BASE_RADIUS = 4.8;
	private static final double SHARE = 0.30;
	private static final double BERSERK_SHARE = 1.00;

	/** Spread a primary melee hit to everything within the radius of its target. */
	public static void spread(Player attacker, LivingEntity hit, double sbDamage, DamagePath path) {
		if(attacker == null || hit == null || sbDamage <= 0) return;
		if(path != DamagePath.MELEE) return;                         // a real SWING, not bow or mage beam

		DungeonClass clazz = DungeonClass.of(attacker);
		boolean berserk = clazz == DungeonClass.BERSERK;
		boolean solo = DungeonClass.isSoloOnClass(attacker);
		double radius = BASE_RADIUS + ClassBonuses.swingRange(clazz, solo);
		double share = berserk ? BERSERK_SHARE : SHARE;
		double each = sbDamage * share;

		for(Entity e : hit.getNearbyEntities(radius, radius, radius)) {
			if(!(e instanceof LivingEntity other) || other instanceof Player || other.equals(hit)) continue;
			if(other.isDead() || other.getHealth() <= 0) continue;
			// Armoured wither takes nothing, same as direct hits.
			if(other instanceof Wither w && w.getInvulnerableTicks() != 0) continue;
			Damage.dealSecondary(other, each, DamageKind.CLEAVE, attacker);
		}
	}
}
