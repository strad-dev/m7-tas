package plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class MiscListener implements Listener {
	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent e) {
		String player = e.getPlayer().getName();
		String message = e.getMessage();
		String sentMessage = "";
		if(player.equals("Beethoven_")) {
			sentMessage += ChatColor.BLUE;
		} else {
			sentMessage += ChatColor.GREEN;
		}
		sentMessage += player + ChatColor.WHITE + ": " + message;
		Bukkit.broadcastMessage(sentMessage);
		e.setCancelled(true);
	}

	@EventHandler
	public void onProjectileHit(ProjectileHitEvent e) {
		// Handle arrows hitting blocks - remove Terminator arrows
		if (e.getHitBlock() != null) {
			if (e.getEntity() instanceof Arrow arrow && arrow.getScoreboardTags().contains("TerminatorArrow")) {
				arrow.remove();
			}
		} else {
			// Handle arrows hitting entities - prevent self-hits
			if (!(e.getEntity() instanceof Arrow arrow) ||
					!(e.getHitEntity() instanceof Player player) ||
					!(arrow.getShooter() instanceof Player shooter) ||
					!player.equals(shooter)) {
				return;
			}
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onDragonArrowHit(EntityDamageByEntityEvent e) {
		// Check if this is an arrow hitting a dragon or dragon part
		if (!(e.getDamager() instanceof Arrow arrow)) {
			return;
		}

		Entity damaged = e.getEntity();

		// Check if we hit a dragon or dragon part
		boolean isDragonHit = (damaged instanceof EnderDragon) || (damaged instanceof EnderDragonPart);

		if (isDragonHit) {
			// Consume all pierce levels so the arrow stops after hitting the first part
			arrow.setPierceLevel(0);
			arrow.remove();

			// Optional: You could also remove the arrow entirely after a short delay
			// to ensure it doesn't continue flying
			// Bukkit.getScheduler().runTaskLater(plugin, arrow::remove, 1L);
		}
	}
}