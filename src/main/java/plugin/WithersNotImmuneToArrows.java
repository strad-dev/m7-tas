package plugin;

import org.bukkit.Bukkit;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("DataFlowIssue")
public class WithersNotImmuneToArrows implements Listener {
	private static final Set<UUID> processingArrows = new HashSet<>();

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onProjectileHitWither(ProjectileHitEvent event) {
		// Only handle arrows hitting withers
		if(!(event.getEntity() instanceof Arrow arrow)) return;
		if(!(event.getHitEntity() instanceof Wither wither)) return;

		// Prevent double processing
		UUID arrowUUID = arrow.getUniqueId();
		if(processingArrows.contains(arrowUUID)) {
			return;
		}

		// Check if wither would normally be immune (health <= 50%)
		if(wither.getHealth() > wither.getAttribute(Attribute.MAX_HEALTH).getValue() / 2.0f) {
			return; // Wither isn't in armor phase, let vanilla handle it
		}

		// Mark arrow as being processed
		processingArrows.add(arrowUUID);

		try {
			// Create and fire the damage event
			Bukkit.getPluginManager().callEvent(new EntityDamageByEntityEvent(arrow, wither, EntityDamageByEntityEvent.DamageCause.KILL, DamageSource.builder(DamageType.GENERIC_KILL).build(), 0));

			// Cancel the original event to prevent the bounce
			event.setCancelled(true);
		} finally {
			// Clean up tracking
			processingArrows.remove(arrowUUID);
		}
	}
}