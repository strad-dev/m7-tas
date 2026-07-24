package listeners;

import instructions.clear.ClearManager;
import instructions.clear.PuzzleQuiz;
import instructions.clear.Rooms;
import instructions.clear.Secret;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
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
		if(instructions.Server.isCleanupInProgress()) return; // cleanup purges must not count as kills
		LivingEntity ent = e.getEntity();
		Location loc = ent.getLocation();
		Player killer = ent.getKiller() != null ? ent.getKiller() : ClearManager.nearestRealPlayer(loc);

		if(ent.getScoreboardTags().contains("ClearMiniboss")) {
			ClearManager.minibossKilled(Rooms.roomAt(loc), killer);
		}
		if(ent.getScoreboardTags().contains(ClearManager.TAG_BAT)) {
			ClearManager.noteBatKill();
			ClearManager.secretFound(killer, ClearManager.findSecretByEntity(ent.getUniqueId()));
			ent.remove(); // drop the hitbox immediately instead of leaving the dying-bat corpse
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
		// Secret chests (right-click, no GUI) and essence skulls (right-click to collect).
		Secret s = ClearManager.findSecretAtBlock(b.getX(), b.getY(), b.getZ());
		if(s != null) {
			e.setCancelled(true); // ALWAYS cancel so an already-opened chest never shows the vanilla GUI
			if(!s.found) {
				if(s.isChest()) ClearManager.openChest(e.getPlayer(), s);
				else ClearManager.secretFound(e.getPlayer(), s); // essence
			}
		}
	}

	// Wizard crystal hand-in via RIGHT-click. NOT ignoreCancelled: MiscListener cancels villager right-clicks
	// at LOWEST to block the trade GUI, and we still want the hand-in to fire.
	@EventHandler(priority = EventPriority.NORMAL)
	public void onRightClickEntity(PlayerInteractEntityEvent e) {
		if(!ClearManager.isActive()) return;
		if(e.getRightClicked() instanceof Villager v && ClearManager.hasCrystal() && isWizard(v)) {
			ClearManager.handInCrystal(e.getPlayer());
		}
	}

	// Wizard crystal hand-in via LEFT-click (attack). MiscListener makes villagers invulnerable, but the
	// hand-in should still register. NOT ignoreCancelled for the same reason as the right-click path.
	@EventHandler(priority = EventPriority.NORMAL)
	public void onLeftClickEntity(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
		if(!ClearManager.isActive()) return;
		if(!(e.getEntity() instanceof Villager v)) return;
		if(!(e.getDamager() instanceof Player p)) return;
		if(ClearManager.hasCrystal() && isWizard(v)) ClearManager.handInCrystal(p);
	}

	/** The Wizard is the villager in the Wizard room (name-independent, so it survives map re-labels). */
	private static boolean isWizard(Villager v) {
		String name = Utils.plain(v.customName());
		return Rooms.roomAt(v.getLocation()) == Rooms.WIZARD || name.contains("Wizard");
	}
}
