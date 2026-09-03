package items;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;

/**
 * An item that launches a projectile it has to handle again on impact: the Jerry-chine Gun's snowball and the
 * Explosive Bow's arrows.
 * <p>
 * The Bonzo Staff is deliberately NOT one, even though it fires a wind charge: that charge is vanilla, and what
 * the plugin does with it is age it ({@code BonzoStaff.bonzoFireTick}) from {@code MiscListener} and
 * {@code Actions}, not handle its impact.
 * <p>
 * Projectiles are matched by SCOREBOARD TAG rather than by the shooter's held item, on purpose: the shot has
 * already left, and by the time it lands the shooter may well be holding something else.
 */
public interface ProjectileItem extends Item {

	/** The scoreboard tag this item stamps on its projectiles, and what the registry matches an impact on. */
	String projectileTag();

	/**
	 * One of this item's projectiles hit something.
	 *
	 * @param shooter the shooter, already resolved to a Player, or null if it was not one
	 */
	void onProjectileHit(ProjectileHitEvent e, Player shooter);
}
