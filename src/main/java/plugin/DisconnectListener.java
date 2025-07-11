package plugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public class DisconnectListener implements Listener {

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		// If they were spectating, clean up
		if (M7tas.getSpectatorMap().containsKey(player)) {
			Player fakePlayer = M7tas.getSpectatorMap().remove(player);
			if (fakePlayer != null) {
				List<Player> spectators = M7tas.getReverseSpectatorMap().get(fakePlayer);
				if (spectators != null) {
					spectators.remove(player);
					if (spectators.isEmpty()) {
						M7tas.getReverseSpectatorMap().remove(fakePlayer);
					}
				}
			}
		}

		// Clean up their inventory backup (no need to restore since they're leaving)
		M7tas.originalInventories.remove(player);
	}
}