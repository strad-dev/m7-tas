package listeners;

import instructions.bosses.PadAndPillar;
import instructions.bosses.Storm;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Iterator;

/**
 * Filters the {@link EntityExplodeEvent} from Storm's crush explosion so that
 * only diorite/polished_diorite below y196 is destroyed. Keeps the pillar
 * anchors at y196 intact so {@code restoreStormPillars} can rebuild from them
 * on the next {@code /tas}.
 */
public class StormCrushExplosion implements Listener {

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onCrushExplode(EntityExplodeEvent event) {
		// Only filter explosions sourced from Storm's wither entity.
		if(Storm.INSTANCE.getBoss() == null) return;
		if(!Storm.INSTANCE.getBoss().equals(event.getEntity())) return;

		Iterator<Block> it = event.blockList().iterator();
		while(it.hasNext()) {
			Block b = it.next();
			Material type = b.getType();
			boolean isDiorite = type == Material.DIORITE || type == Material.POLISHED_DIORITE;
			boolean belowAnchor = b.getY() < PadAndPillar.PILLAR_ANCHOR_Y;
			if(!isDiorite || !belowAnchor) {
				it.remove();
			}
		}
	}
}
