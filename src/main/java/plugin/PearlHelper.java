package plugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public class PearlHelper implements Listener {
	@EventHandler
	public void onPearlThrow(ProjectileLaunchEvent e) {
		if (!(e.getEntity() instanceof EnderPearl pearl)) return;
		if (!(e.getEntity().getShooter() instanceof Player p)) return;

		pearl.setVelocity(pearl.getVelocity().multiply(2.0));
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
			pearl.remove();
			p.teleport(l);
		}
	}
}