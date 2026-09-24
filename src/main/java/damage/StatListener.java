package damage;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;

/**
 * Keeps the stat cache honest. §7: invalidation must cover EQUIPMENT changes too; masks and hats get worn mid-fight
 * and a helmet swap is worth thousands of Int. Each event is a way an item moves on or off a player.
 * <p>
 * {@link Stats} also self-expires after a few ticks since Legion and the Ragnarock buff move with no inventory event;
 * these listeners make a deliberate swap show immediately.
 */
public final class StatListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void onHeldItemChange(PlayerItemHeldEvent e) {
		refresh(e.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onInventoryClick(InventoryClickEvent e) {
		if(e.getWhoClicked() instanceof Player p) refresh(p);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onInventoryDrag(InventoryDragEvent e) {
		if(e.getWhoClicked() instanceof Player p) refresh(p);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onInventoryClose(InventoryCloseEvent e) {
		if(e.getPlayer() instanceof Player p) refresh(p);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onSwapHands(PlayerSwapHandItemsEvent e) {
		refresh(e.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onDrop(PlayerDropItemEvent e) {
		refresh(e.getPlayer());
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		refresh(e.getPlayer());
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		// Changes everyone's Legion stacks and maybe who is solo on a class.
		Stats.invalidateAll();
	}

	/**
	 * Drop the cache and re-render Chimera lore. A hat forces the Black Cat in assumed modes, changing what Chimera
	 * copies. {@code StatLore.refreshChimeraLore} defers a tick and no-ops if the pet didn't move, so this is cheap.
	 * Not on {@link #onQuit}: that invalidates everyone for Legion, but nobody's pet changed.
	 */
	private static void refresh(Player p) {
		Stats.invalidate(p);
		StatLore.refreshChimeraLore(p);
	}

	/** Housekeeping only; the kill is counted in {@code Damage.deal}, which knows who landed it. */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onDeath(EntityDeathEvent e) {
		TargetDebuffs.forget(e.getEntity());
	}
}
