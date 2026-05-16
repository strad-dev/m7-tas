package listeners;

import instructions.bosses.PadAndPillar;
import instructions.bosses.Storm;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityKnockbackByEntityEvent;
import org.bukkit.event.entity.EntityKnockbackEvent;

import java.util.Iterator;

/**
 * Storm's crush explosion is purely cosmetic for entities — its job is to clear
 * the pillar's diorite. This listener:
 * <ol>
 *   <li>filters {@link EntityExplodeEvent#blockList()} so only the active pillar's
 *       diorite/polished_diorite at y&lt;196 is destroyed (no collateral),</li>
 *   <li>cancels any {@link EntityDamageByEntityEvent} attributed to Storm's
 *       wither with cause {@code ENTITY_EXPLOSION}, so entities take no damage
 *       from the blast,</li>
 *   <li>cancels any {@link EntityKnockbackEvent} originating from Storm's wither
 *       so the blast doesn't push entities around.</li>
 * </ol>
 */
public class StormCrushExplosion implements Listener {

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onCrushExplode(EntityExplodeEvent event) {
		if(!isStormCrush(event.getEntity())) return;

		// Scope the destruction to only the pillar Storm is currently crushing —
		// no collateral damage to other pillars or arena blocks.
		PadAndPillar pillar = Storm.INSTANCE.getCurrentCrushPillar();

		Iterator<Block> it = event.blockList().iterator();
		while(it.hasNext()) {
			Block b = it.next();
			Material type = b.getType();
			boolean isDiorite = type == Material.DIORITE || type == Material.POLISHED_DIORITE;
			boolean belowAnchor = b.getY() < PadAndPillar.PILLAR_ANCHOR_Y;
			boolean inActivePillar = pillar != null
					&& b.getX() >= pillar.pillarX1() && b.getX() <= pillar.pillarX2()
					&& b.getZ() >= pillar.pillarZ1() && b.getZ() <= pillar.pillarZ2();
			if(!isDiorite || !belowAnchor || !inActivePillar) {
				it.remove();
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onCrushDamage(EntityDamageByEntityEvent event) {
		if(event.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) return;
		if(!isStormCrush(event.getDamager())) return;
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onCrushKnockback(EntityKnockbackByEntityEvent event) {
		if(!isStormCrush(event.getSourceEntity())) return;
		event.setCancelled(true);
	}

	/** Generic-source knockback fallback for explosion physics paths that don't carry the source entity. */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onAnyKnockback(EntityKnockbackEvent event) {
		if(event.getCause() != EntityKnockbackEvent.KnockbackCause.EXPLOSION) return;
		if(!Storm.INSTANCE.isCrushExplosionActive()) return;
		event.setCancelled(true);
	}

	private static boolean isStormCrush(Entity source) {
		if(source == null) return false;
		if(Storm.INSTANCE.getBoss() == null) return false;
		return Storm.INSTANCE.getBoss().equals(source);
	}
}
