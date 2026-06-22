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
			if(temp instanceof LivingEntity entity && entity.customName() == null) {
				double health = entity.getHealth() + entity.getAbsorptionAmount();
				if(entity.getScoreboardTags().contains("TASWitherKing") || entity.getScoreboardTags().contains("TASWatcher")) {
					entity.customName(Utils.msg("<aqua>" + entity.getName() + " <yellow>" + health + "<red>❤"));
				} else {
					entity.customName(Utils.msg("<aqua>" + entity.getName() + " <yellow>" + Utils.formatHealthM(entity) + "<red>❤"));
				}
				entity.setCustomNameVisible(true);
			}
		}
	}

	@EventHandler
	public void onEntitySpawn(EntitySpawnEvent e) {
		if(e.getEntity() instanceof LivingEntity entity) {
			double health = entity.getHealth() + entity.getAbsorptionAmount();
			String name = "<aqua>" + entity.getName();
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
