package listeners;

import instructions.bosses.witherking.WitherKing;
import org.bukkit.block.Block;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Drives the Wither-King summon phase. Pickup fires when a player right-clicks a relic's Interaction entity;
 * placement fires when a player right-clicks the matching altar block (Y 6/7) while holding that relic. A held
 * relic is locked into the hotbar — it can't be moved, dropped, or placed anywhere except its correct altar.
 * Both paths work for real players (practice) and — once the player choreography is wired — fake players.
 */
public class WitherKingListener implements Listener {

	/** Right-click a relic's Interaction entity → pick it up (unless already carrying one). */
	@EventHandler
	public void onPickup(PlayerInteractAtEntityEvent e) {
		if(!(e.getRightClicked() instanceof Interaction interaction)) return;
		String color = WitherKing.relicColorForInteraction(interaction);
		if(color == null) return;
		e.setCancelled(true);
		if(WitherKing.isHoldingRelic(e.getPlayer())) return; // one relic at a time
		WitherKing.pickUpRelic(e.getPlayer(), color);
	}

	/** Right-click a block while holding a relic: the wool is never placed as a real block — only a click on the
	 *  matching altar (Y 6/7) places the relic. Anything else is silently cancelled (prevents misplacement). */
	@EventHandler
	public void onPlace(PlayerInteractEvent e) {
		if(e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		Player p = e.getPlayer();
		String heldColor = WitherKing.relicColorOfItem(p.getInventory().getItemInMainHand());
		if(heldColor == null) return; // not holding a relic — ignore
		e.setCancelled(true); // never let the wool be placed as a block

		Block b = e.getClickedBlock();
		if(b == null || (b.getY() != 6 && b.getY() != 7)) return;
		String altarColor = WitherKing.altarColorAt(b.getX(), b.getZ());
		if(altarColor == null || !altarColor.equals(heldColor)) return; // wrong altar — do nothing

		WitherKing.placeRelic(p, altarColor);
	}

	// --- Lock a held relic into the hotbar until it's placed on its altar ---

	/** Block moving a relic within any inventory (click, shift-click, hotbar number-key swap). */
	@EventHandler
	public void onRelicClick(InventoryClickEvent e) {
		if(WitherKing.relicColorOfItem(e.getCurrentItem()) != null || WitherKing.relicColorOfItem(e.getCursor()) != null) {
			e.setCancelled(true);
			return;
		}
		if(e.getClick() == ClickType.NUMBER_KEY && e.getWhoClicked() instanceof Player p) {
			ItemStack swapTarget = p.getInventory().getItem(e.getHotbarButton());
			if(WitherKing.relicColorOfItem(swapTarget) != null) e.setCancelled(true);
		}
	}

	/** Block dragging a relic across slots. */
	@EventHandler
	public void onRelicDrag(InventoryDragEvent e) {
		if(WitherKing.relicColorOfItem(e.getOldCursor()) != null) {
			e.setCancelled(true);
			return;
		}
		for(ItemStack it : e.getNewItems().values()) {
			if(WitherKing.relicColorOfItem(it) != null) {
				e.setCancelled(true);
				return;
			}
		}
	}

	/** Block dropping a relic. */
	@EventHandler
	public void onRelicDrop(PlayerDropItemEvent e) {
		if(WitherKing.relicColorOfItem(e.getItemDrop().getItemStack()) != null) e.setCancelled(true);
	}

	/** Belt-and-suspenders: never let a relic wool be placed as a real block. */
	@EventHandler
	public void onRelicBlockPlace(BlockPlaceEvent e) {
		if(WitherKing.relicColorOfItem(e.getItemInHand()) != null) e.setCancelled(true);
	}
}
