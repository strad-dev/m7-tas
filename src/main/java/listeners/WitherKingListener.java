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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Drives the Wither-King summon phase. Pickup fires when a player right-clicks a relic's Interaction entity;
 * placement fires when a player right-clicks the matching altar block (Y 6/7) while holding that relic. A held
 * relic is locked into the hotbar: it can't be moved, dropped, or placed anywhere except its correct altar.
 * Both paths work for real players in practice and, once the player choreography is wired, fake players.
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

	/**
	 * Right-click a block while holding a relic: the wool is never placed as a real block, and only a click on the
	 * matching altar (Y 6/7) places the relic. Anything else is silently cancelled (prevents misplacement).
	 * <p>
	 * <b>The WRONG cauldron sends the relic back to its statue</b>, in every mode, and in ultra-realistic it kills
	 * you too ({@code death/Deaths}).  The relic can never be destroyed or kept: the summon needs all five, so
	 * losing one to a mistake would strand the phase.  <b>The return has to come first</b> - {@code Deaths}
	 * snapshots the inventory for the revival, so a relic still in hand at that moment would be handed straight
	 * back on revival while the statue held a second copy.
	 * <p>
	 * Clicking anything that is not an altar at all is still just cancelled: the mistake is putting a relic in the
	 * wrong cauldron, not missing one.  <b>Placing and returning are exclusive branches</b> - a relic that goes onto
	 * its own altar is never also sent home, and {@code returnRelicToStatue} re-checks that itself.
	 */
	@EventHandler
	public void onPlace(PlayerInteractEvent e) {
		if(e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		// Main hand only.  Cancelling the main-hand attempt makes vanilla try the off hand, which fires a SECOND
		// event for the same physical click; the same reason GoldorListener gates on this.  Both would read the main
		// hand and re-run whichever branch below already ran.
		if(e.getHand() != EquipmentSlot.HAND) return;
		Player p = e.getPlayer();
		String heldColor = WitherKing.relicColorOfItem(p.getInventory().getItemInMainHand());
		if(heldColor == null) return; // not holding a relic, so ignore
		e.setCancelled(true); // never let the wool be placed as a block

		Block b = e.getClickedBlock();
		if(b == null || (b.getY() != 6 && b.getY() != 7)) return;
		String altarColor = WitherKing.altarColorAt(b.getX(), b.getZ());
		if(altarColor == null) return; // not an altar, so do nothing

		if(altarColor.equals(heldColor)) {
			WitherKing.placeRelic(p, altarColor);
		} else {
			WitherKing.returnRelicToStatue(p, heldColor);
			death.Deaths.kill(p, "Wither King"); // no-op outside ultra-realistic
		}
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
