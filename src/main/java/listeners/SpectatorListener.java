package listeners;

import commands.Spectate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import plugin.M7tas;
import plugin.PlayerCollision;
import plugin.PlayerInventoryBackup;

public class SpectatorListener implements Listener {

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		if (Spectate.getSpectatorMap().containsKey(player)) {
			Spectate.removeSpectator(player);
		} else {
			PlayerCollision.removeFromNoCollisionTeam(player);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getWhoClicked() instanceof Player player) {
			if (Spectate.getSpectatorMap().containsKey(player)) {
				// Allow viewing but prevent actual changes
				event.setCancelled(true);

				// Re-sync inventory to make sure it stays correct
				Player fakePlayer = Spectate.getSpectatorMap().get(player);
				if (fakePlayer != null) {
					Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> PlayerInventoryBackup.syncInventory(fakePlayer), 1L);
				}
			}
		}
	}

	// NEW: Prevent item drops for spectators
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		if (Spectate.getSpectatorMap().containsKey(event.getPlayer())) {
			event.setCancelled(true);
		}
	}
}