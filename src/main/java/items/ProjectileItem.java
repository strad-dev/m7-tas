package items;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;

/**
 * Launches a projectile it handles again on impact: Jerry-chine snowball, Explosive Bow arrows.
 * <p>
 * Not the Bonzo Staff: its wind charge is vanilla and the plugin only ages it ({@code BonzoStaff.bonzoFireTick}).
 * Matched by SCOREBOARD TAG, not held item, since the shooter may have swapped by impact.
 */
public interface ProjectileItem extends Item {

	/** Stamped on its projectiles; the registry matches impacts on it. */
	String projectileTag();

	/** @param shooter null if not a Player */
	void onProjectileHit(ProjectileHitEvent e, Player shooter);
}
