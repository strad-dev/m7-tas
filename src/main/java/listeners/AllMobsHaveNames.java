package listeners;

import org.bukkit.ChatColor;
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
			if(temp instanceof LivingEntity entity && entity.getCustomName() == null) {
				double health = entity.getHealth() + entity.getAbsorptionAmount();
				if(entity.getScoreboardTags().contains("TASWitherKing") || entity.getScoreboardTags().contains("TASWatcher")) {
					entity.setCustomName(ChatColor.AQUA + entity.getName() + " " + ChatColor.RED + "❤" + ChatColor.YELLOW + health);
				} else {
					entity.setCustomName(ChatColor.AQUA + entity.getName() + " " + ChatColor.RED + "❤" + ChatColor.YELLOW + Utils.formatHealthM(entity));
				}
				entity.setCustomNameVisible(true);
			}
		}
	}

	@EventHandler
	public void onEntitySpawn(EntitySpawnEvent e) {
		if(e.getEntity() instanceof LivingEntity entity) {
			double health = entity.getHealth() + entity.getAbsorptionAmount();
			String name = ChatColor.AQUA + entity.getName();
			if(!name.contains("❤")) {
				if(entity.getScoreboardTags().contains("TASWitherKing") || entity.getScoreboardTags().contains("TASWatcher")) {
					name += " " + ChatColor.RED + "❤" + ChatColor.YELLOW + health;
				} else {
					name += " " + ChatColor.RED + "❤" + ChatColor.YELLOW + Utils.formatHealthM(entity);
				}
			}
			entity.setCustomName(name);
			entity.setCustomNameVisible(true);
		}
	}
}
