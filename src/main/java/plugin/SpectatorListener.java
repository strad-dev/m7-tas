package plugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class SpectatorListener implements Listener {

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		M7tas.removeFromNoCollisionTeam(player);

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

		Utils.restoreInventory(player);
		M7tas.removeFromNoCollisionTeam(player);
		player.removePotionEffect(PotionEffectType.INVISIBILITY);
		M7tas.originalInventories.remove(player);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getWhoClicked() instanceof Player player) {
			if (M7tas.getSpectatorMap().containsKey(player)) {
				// Allow viewing but prevent actual changes
				event.setCancelled(true);

				// Re-sync inventory to make sure it stays correct
				Player fakePlayer = M7tas.getSpectatorMap().get(player);
				if (fakePlayer != null) {
					Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> Utils.syncInventory(fakePlayer), 1L);
				}
			}
		}
	}

	// NEW: Prevent item drops for spectators
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		if (M7tas.getSpectatorMap().containsKey(event.getPlayer())) {
			event.setCancelled(true);
		}
	}
}