package plugin;

import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public class PearlHelper implements Listener {
	@EventHandler
	public void onPearlThrow(ProjectileLaunchEvent event) {
		if (!(event.getEntity() instanceof EnderPearl pearl)) return;
		if (!(event.getEntity().getShooter() instanceof Player)) return;

		pearl.setVelocity(pearl.getVelocity().multiply(2.0));
	}
}