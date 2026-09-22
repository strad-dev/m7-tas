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
 * Keeps the stat cache honest.
 * <p>
 * MAP.md §7 is explicit that invalidation has to cover <b>equipment</b> changes and not just inventory
 * ones: the masks and hats are hotbar items that get worn mid-fight, and a helmet swap is worth thousands of
 * Intelligence.  Every event here is one of the ways an item can move on or off a player.
 * <p>
 * {@link Stats} also expires entries on its own after a few ticks, because several inputs move with no inventory
 * event at all - Legion counts players within 30 blocks, and the Ragnarock buff comes and goes on a timer.  These
 * listeners are what make a deliberate swap show up immediately rather than up to that window later.
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
		// A player leaving changes everyone else's Legion stacks and can change who is solo on a class.
		Stats.invalidateAll();
	}

	/**
	 * One player's stats have moved: drop the cache, and re-render the lore on their Chimera weapons.
	 * <p>
	 * <b>The same events cover both.</b>  A helmet swap is the case that needs it - a Racing Helmet or Cow Hat
	 * forces the Black Cat in the assumed modes, which changes what Chimera copies, so the weapon's tooltip is
	 * wrong the moment the hat goes on.  {@code StatLore.refreshChimeraLore} defers itself a tick and does
	 * nothing when the pet has not actually moved, so hanging it off every one of these is cheap.
	 * <p>
	 * Not on {@link #onQuit}: that invalidates EVERYONE (Legion), and nobody else's pet changed.
	 */
	private static void refresh(Player p) {
		Stats.invalidate(p);
		StatLore.refreshChimeraLore(p);
	}

	/**
	 * Drop a dead mob's debuff state.  Housekeeping only - the kill itself is counted in {@code Damage.deal}'s own
	 * chokepoint, which is the one that knows who landed it.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onDeath(EntityDeathEvent e) {
		TargetDebuffs.forget(e.getEntity());
	}
}
