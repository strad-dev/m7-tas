package listeners;

import instructions.clear.ClearManager;
import instructions.clear.PuzzleQuiz;
import instructions.clear.Rooms;
import instructions.clear.Secret;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import plugin.Utils;

/**
 * All clear-phase interactions for real players: opening chests, collecting essence, answering the Quiz,
 * the Wizard crystal-ball hand-in, and routing miniboss / bat / crypt / mimic deaths into {@link ClearManager}.
 * Every handler is gated on {@link ClearManager#isActive()} so it never interferes with the boss phases.
 */
public class ClearListener implements Listener {

	// -100 63 -111 — the Wizard's crystal ball (right-click to pick up; ±1 block tolerance).
	private static final int[] CRYSTAL = {-100, 63, -111};

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onDeath(EntityDeathEvent e) {
		if(!ClearManager.isActive()) return;
		LivingEntity ent = e.getEntity();
		Location loc = ent.getLocation();
		Player killer = ent.getKiller() != null ? ent.getKiller() : ClearManager.nearestRealPlayer(loc);

		if(ent.getScoreboardTags().contains("ClearMiniboss")) {
			ClearManager.minibossKilled(Rooms.roomAt(loc), killer);
		}
		if(ent.getScoreboardTags().contains(ClearManager.TAG_BAT)) {
			ClearManager.noteBatKill();
			ClearManager.secretFound(killer, ClearManager.findSecretByEntity(ent.getUniqueId()));
		}
		if(ent.getScoreboardTags().contains(ClearManager.TAG_CRYPT)) {
			ClearManager.cryptKilled(ent.getScoreboardTags().contains("SecretPrince"));
		}
		if(ent.getScoreboardTags().contains(ClearManager.TAG_MIMIC)) {
			ClearManager.mimicKilledEvent(killer, ClearManager.findSecretByEntity(ent.getUniqueId()));
		}
	}

	@EventHandler
	public void onRightClickBlock(PlayerInteractEvent e) {
		if(!ClearManager.isActive()) return;
		if(e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		Block b = e.getClickedBlock();
		if(b == null) return;

		// Quiz answer buttons
		int btn = PuzzleQuiz.buttonIndex(b);
		if(btn >= 0) {
			e.setCancelled(true);
			PuzzleQuiz.answer(e.getPlayer(), btn);
			return;
		}
		// Crystal ball (±1 block)
		if(Math.abs(b.getX() - CRYSTAL[0]) <= 1 && Math.abs(b.getY() - CRYSTAL[1]) <= 1 && Math.abs(b.getZ() - CRYSTAL[2]) <= 1) {
			e.setCancelled(true);
			ClearManager.pickUpCrystal(e.getPlayer());
			return;
		}
		// Secret chests
		if(b.getType() == Material.CHEST) {
			Secret s = ClearManager.findChestSecret(b.getX(), b.getY(), b.getZ());
			if(s != null) {
				e.setCancelled(true); // no vanilla container GUI
				ClearManager.openChest(e.getPlayer(), s);
			}
		}
	}

	// Essence (Interaction hitbox) + Wizard crystal hand-in. NOT ignoreCancelled: MiscListener cancels
	// villager right-clicks at LOWEST to block the trade GUI, and we still want the hand-in to fire.
	@EventHandler(priority = EventPriority.NORMAL)
	public void onRightClickEntity(PlayerInteractEntityEvent e) {
		if(!ClearManager.isActive()) return;
		Entity ent = e.getRightClicked();

		if(ent instanceof Interaction && ent.getScoreboardTags().contains(ClearManager.TAG_ESSENCE)) {
			Secret s = ClearManager.findSecretByEntity(ent.getUniqueId());
			if(s != null && !s.found) {
				e.setCancelled(true);
				ClearManager.secretFound(e.getPlayer(), s);
			}
			return;
		}
		if(ent instanceof Villager && ClearManager.hasCrystal()) {
			// The Wizard is the villager in the Wizard room (name-independent, so it survives map re-labels).
			String name = Utils.plain(ent.customName());
			boolean isWizard = Rooms.roomAt(ent.getLocation()) == Rooms.WIZARD || (name != null && name.contains("Wizard"));
			if(isWizard) ClearManager.handInCrystal(e.getPlayer());
		}
	}
}
