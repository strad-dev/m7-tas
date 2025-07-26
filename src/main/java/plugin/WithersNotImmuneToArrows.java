package plugin;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

@SuppressWarnings("DataFlowIssue")
public class WithersNotImmuneToArrows implements Listener {
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onProjectileHitWither(ProjectileHitEvent event) {
		// Only handle arrows hitting withers
		if(!(event.getEntity() instanceof Arrow arrow)) return;
		if(!(event.getHitEntity() instanceof Wither wither)) return;

		// Check if wither would normally be immune (health <= 50%)
		if(wither.getHealth() > wither.getAttribute(Attribute.MAX_HEALTH).getValue() / 2.0f) {
			return; // Wither isn't in armor phase, let vanilla handle it
		}

		// Get the damage directly from the arrow
		double damage = arrow.getDamage();

		// Apply damage manually
		Bukkit.getPluginManager().callEvent(new EntityDamageByEntityEvent(arrow, wither, EntityDamageByEntityEvent.DamageCause.KILL, DamageSource.builder(DamageType.GENERIC_KILL).build(), damage));

		// Add hit effects
		wither.getWorld().playSound(arrow.getLocation(), Sound.ENTITY_ARROW_HIT, 1.0f, 1.0f);

		// Cancel the event to prevent the bounce
		event.setCancelled(true);
	}
}