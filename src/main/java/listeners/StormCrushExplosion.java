package listeners;

import instructions.bosses.storm.PadAndPillar;
import instructions.bosses.storm.Storm;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;

import java.util.Iterator;

/**
 * Storm's crush explosion only clears the pillar's diorite; it's cosmetic for entities. This:
 * <ol>
 *   <li>filters {@link EntityExplodeEvent#blockList()} to the active pillar's diorite/polished_diorite at y&lt;196;</li>
 *   <li>cancels {@link EntityDamageByEntityEvent} from Storm's wither with cause {@code ENTITY_EXPLOSION};</li>
 *   <li>cancels {@link EntityKnockbackEvent} from Storm's wither;</li>
 *   <li>cancels {@link HangingBreakEvent} from the crush, so item frames (Goldor terminals) survive: vanilla removes
 *       hanging entities on a separate path the damage/knockback cancels don't cover.</li>
 * </ol>
 */
public class StormCrushExplosion implements Listener {

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onCrushExplode(EntityExplodeEvent event) {
		if(!isStormCrush(event.getEntity())) return;

		// Only the pillar Storm is crushing; no collateral.
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
			boolean inRedPillar = b.getX() >= PadAndPillar.RED_PILLAR_X1 && b.getX() <= PadAndPillar.RED_PILLAR_X2
					&& b.getZ() >= PadAndPillar.RED_PILLAR_Z1 && b.getZ() <= PadAndPillar.RED_PILLAR_Z2;
			if(!isDiorite || !belowAnchor || !inActivePillar || inRedPillar) {
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

	// 26.2: Paper's unified EntityKnockbackEvent, one handler. The crush source arrives as
	// EntityPushedByEntityAttackEvent (subclass, same HandlerList); explosion physics on the base event with
	// Cause.EXPLOSION.
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onCrushKnockback(EntityKnockbackEvent event) {
		// Crush source as a pushed-by-entity knockback.
		if(event instanceof EntityPushedByEntityAttackEvent pushed && isStormCrush(pushed.getPushedBy())) {
			event.setCancelled(true);
			return;
		}
		// Explosion-physics knockback during a crush (no source entity).
		if(event.getCause() == EntityKnockbackEvent.Cause.EXPLOSION && !Storm.INSTANCE.crushExplosionNotActive()) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onCrushHangingBreak(HangingBreakByEntityEvent event) {
		if(event.getCause() != HangingBreakEvent.RemoveCause.EXPLOSION) return;
		if(!isStormCrush(event.getRemover())) return;
		event.setCancelled(true);
	}

	/** Fallback for explosion paths that fire the parent event without a remover entity. */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onAnyHangingBreak(HangingBreakEvent event) {
		if(event.getCause() != HangingBreakEvent.RemoveCause.EXPLOSION) return;
		if(Storm.INSTANCE.crushExplosionNotActive()) return;
		event.setCancelled(true);
	}

	private static boolean isStormCrush(Entity source) {
		if(source == null) return false;
		if(Storm.INSTANCE.getBoss() == null) return false;
		return Storm.INSTANCE.getBoss().equals(source);
	}
}
