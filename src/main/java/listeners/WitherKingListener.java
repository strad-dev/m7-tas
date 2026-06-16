package listeners;

import instructions.bosses.witherking.WitherKing;
import org.bukkit.block.Block;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Drives the Wither-King summon phase. Pickup fires when a player right-clicks a relic's Interaction entity;
 * placement fires when a player right-clicks the matching altar block (Y 6/7) while holding that relic. Both
 * paths work for real players (practice) and — once the player choreography is wired — fake players.
 */
public class WitherKingListener implements Listener {

	/** Right-click a relic's Interaction entity → pick it up. */
	@EventHandler
	public void onPickup(PlayerInteractAtEntityEvent e) {
		if(!(e.getRightClicked() instanceof Interaction interaction)) return;
		String color = WitherKing.relicColorForInteraction(interaction);
		if(color == null) return;
		e.setCancelled(true);
		WitherKing.pickUpRelic(e.getPlayer(), color);
	}

	/** Right-click an altar block (Y 6/7) while holding the matching relic → place it. */
	@EventHandler
	public void onPlace(PlayerInteractEvent e) {
		if(e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		Block b = e.getClickedBlock();
		if(b == null || (b.getY() != 6 && b.getY() != 7)) return;
		String altarColor = WitherKing.altarColorAt(b.getX(), b.getZ());
		if(altarColor == null) return;

		Player p = e.getPlayer();
		ItemStack held = p.getInventory().getItemInMainHand();
		String heldColor = WitherKing.relicColorOfItem(held);
		if(!altarColor.equals(heldColor)) return; // wrong-color (or no) relic — do nothing

		e.setCancelled(true);
		WitherKing.placeRelic(p, altarColor);
	}
}
