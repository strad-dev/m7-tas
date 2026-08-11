package listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import plugin.Utils;

public class AllMobsHaveNames implements Listener {
	@EventHandler
	public void onEntitiesLoad(EntitiesLoadEvent e) {
		for(Entity temp : e.getEntities()) {
			if(temp instanceof LivingEntity entity) entity.setMaximumNoDamageTicks(0); // see onEntitySpawn
			if(temp instanceof LivingEntity entity && entity.customName() == null) {
				double health = entity.getHealth() + entity.getAbsorptionAmount();
				if(entity.getScoreboardTags().contains("TASWitherKing") || entity.getScoreboardTags().contains("TASWatcher")) {
					entity.customName(Utils.msg("<aqua>" + sanitize(entity.getName()) + " <yellow>" + health + "<red>❤"));
				} else {
					entity.customName(Utils.msg("<aqua>" + sanitize(entity.getName()) + " <yellow>" + Utils.formatHealthM(entity) + "<red>❤"));
				}
				entity.setCustomNameVisible(true);
			}
		}
	}

	/** Strips legacy §-color codes so a foreign mob's §-coded name (e.g. a /summon'd "§fMort") doesn't
	 *  break MiniMessage, which rejects § codes. */
	private static String sanitize(String name) {
		return name == null ? "" : name.replaceAll("(?i)§[0-9A-FK-OR]", "");
	}

	@EventHandler
	public void onEntitySpawn(EntitySpawnEvent e) {
		if(e.getEntity() instanceof LivingEntity entity) {
			// Mobs get ZERO invulnerability frames (DAMAGE_PLAN.md §7).  With the duration at 0 a hit sets
			// invulnerableTime = 0, vanilla's "> 10" branch is never reached and lastHurt is never consulted, so
			// every computed hit lands in full.  Left alone, vanilla's rule would swallow a Cleave hit on the
			// directly-hit target whole (it is smaller than the main hit) and make every other hit at a 4-5 tick
			// cadence deal nothing.  Declarative and permanent, and it also covers damage arriving by any path
			// other than damage/Damage.
			entity.setMaximumNoDamageTicks(0);
			entity.setNoDamageTicks(0);
			double health = entity.getHealth() + entity.getAbsorptionAmount();
			String name = "<aqua>" + sanitize(entity.getName());
			if(!name.contains("❤")) {
				if(entity.getScoreboardTags().contains("TASWitherKing") || entity.getScoreboardTags().contains("TASWatcher")) {
					name += " <yellow>" + health + "<red>❤";
				} else {
					name += " <yellow>" + Utils.formatHealthM(entity) + "<red>❤";
				}
			}
			entity.customName(Utils.msg(name));
			entity.setCustomNameVisible(true);
		}
	}
}
