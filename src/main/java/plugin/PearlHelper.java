package plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;

public class PearlHelper implements Listener {
	@EventHandler
	public void onPearlThrow(ProjectileLaunchEvent e) {
		if (!(e.getEntity() instanceof EnderPearl pearl)) return;
		if (!(e.getEntity().getShooter() instanceof Player p)) return;

		Vector direction = p.getLocation().getDirection();
		pearl.setVelocity(direction.multiply(1.5));

		Utils.scheduleTask(() -> p.setCooldown(Material.ENDER_PEARL, 0), 1);
	}

	@EventHandler
	public void onPearlLand(ProjectileHitEvent e) {
		if(e.getEntity() instanceof EnderPearl pearl && pearl.getShooter() instanceof Player p) {
			e.setCancelled(true);
			Location l = pearl.getLocation();
			l.setX(Math.floor(l.getX()) + 0.5);
			l.setY(Math.ceil(l.getY()));
			l.setZ(Math.floor(l.getZ()) + 0.5);
			l.setYaw(p.getLocation().getYaw());
			l.setPitch(p.getLocation().getPitch());
			Bukkit.broadcastMessage(ChatColor.GREEN + "[sim] Ender Pearl from " + p.getName() + " landed in " + pearl.getTicksLived() + " ticks.");
			Bukkit.broadcastMessage(ChatColor.GREEN + "[sim] Landed at " + l.getX() + " " + l.getY() + " " + l.getZ());
			pearl.remove();
			p.teleport(l);
		}
	}
}