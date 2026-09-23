package listeners;

import instructions.Actions;
import instructions.bosses.WitherActions;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import plugin.FakePlayerManager;
import plugin.Menus;
import plugin.Utils;

/**
 * Opens and drives the practice-mode Spirit Leap menu ({@link SpiritLeapMenu}). Right-clicking the Infinileap opens
 * the menu in practice; during a TAS it errors. Clicking a teammate's quadrant leaps to them.
 */
public class SpiritLeapListener implements Listener {

	@EventHandler
	public void onLeapItemRightClick(PlayerInteractEvent e) {
		if(e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		// Resolved through the registry rather than by comparing a lore ID, so the menu it opens is the item's
		// own (items.MenuItem.open) and this listener only owns the GATES.
		if(!(items.ItemRegistry.of(e.getItem()) instanceof items.MenuItem menu)) return;
		Player p = e.getPlayer();
		if(FakePlayerManager.getFakePlayers().containsValue(p)) return; // fakes leap via Actions.leap, never the menu
		e.setCancelled(true);
		p.setCooldown(Material.ENDER_PEARL, 0); // right-clicking the leap item refreshes the ender pearl cooldown

		if(!WitherActions.isPracticeMode()) {
			p.sendMessage(Utils.msg("<red>This item can't be used right now!"));
			return;
		}
		if(!SpiritLeapMenu.hasCandidates(p)) {
			p.sendMessage(Utils.msg("<red>Found no one to leap to!"));
			return;
		}
		menu.open(p);
	}

	@EventHandler
	public void onMenuClick(InventoryClickEvent e) {
		if(!(e.getInventory().getHolder() instanceof SpiritLeapMenu menu)) return;
		if(Menus.ignoreDoubleClick(e)) return;
		e.setCancelled(true); // lock the menu; never move items
		if(!(e.getWhoClicked() instanceof Player p)) return;
		Player target = menu.targetForSlot(e.getRawSlot());
		if(target == null || !target.isOnline()) return;
		p.closeInventory();
		Actions.leap(p, target); // viewer is still holding the Infinileap that opened the menu
	}
}
