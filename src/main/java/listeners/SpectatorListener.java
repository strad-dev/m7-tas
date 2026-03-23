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
import org.bukkit.potion.PotionEffectType;
import plugin.M7tas;
import plugin.PlayerCollision;
import plugin.PlayerInventoryBackup;
import plugin.Utils;

import java.util.Set;

public class SpectatorListener implements Listener {

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		PlayerCollision.removeFromNoCollisionTeam(player);

		// If they were spectating, clean up
		if (Spectate.getSpectatorMap().containsKey(player)) {
			Player fakePlayer = Spectate.getSpectatorMap().remove(player);
			if (fakePlayer != null) {
				Set<Player> spectators = Spectate.getReverseSpectatorMap().get(fakePlayer);
				if (spectators != null) {
					spectators.remove(player);
					if (spectators.isEmpty()) {
						Spectate.getReverseSpectatorMap().remove(fakePlayer);
					}
				}
			}
		}

		PlayerInventoryBackup.restoreAndRemove(player);
		PlayerCollision.removeFromNoCollisionTeam(player);
		player.removePotionEffect(PotionEffectType.INVISIBILITY);
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