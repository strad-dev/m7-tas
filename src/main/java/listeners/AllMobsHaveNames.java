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

	/** Strips legacy § codes, which MiniMessage rejects, from a foreign mob's name (a /summon'd "§fMort"). */
	private static String sanitize(String name) {
		return name == null ? "" : name.replaceAll("(?i)§[0-9A-FK-OR]", "");
	}

	@EventHandler
	public void onEntitySpawn(EntitySpawnEvent e) {
		if(e.getEntity() instanceof LivingEntity entity) {
			// Mobs get ZERO i-frames (MAP.md §7): invulnerableTime stays 0, vanilla's "> 10" branch and lastHurt are
			// never reached, so every hit lands in full. Otherwise vanilla swallows a Cleave hit on the main target
			// (it's smaller than the main hit) and every other hit at a 4-5 tick cadence. Covers any damage path.
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
