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

	// -100 63 -111 is the Wizard's crystal ball: right-click to pick up, with ±1 block tolerance.
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

	/**
	 * True if {@code p} must not be able to progress the clear: a spectator, which is the idle state on m7 where
	 * they are watching someone else's run rather than in it, or a player spectating a fake.  The predicate is
	 * shared ({@link Utils#isSpectator}); this name is what it means HERE, the way {@code GoldorListener.cannotSolve}
	 * is for its devices.
	 * <p>
	 * <b>Not redundant with vanilla's own spectator gating, and the guard's PLACEMENT matters.</b>
	 * {@code ServerPlayerGameMode.useItemOn} decides a spectator's click is a no-op only when the block has no
	 * {@code MenuProvider} - a CHEST has one, because opening containers is a spectator feature - so a secret
	 * chest fires {@link PlayerInteractEvent} with the block use still ALLOWED, and vanilla opens the GUI unless
	 * something denies it.  Every other clear block (buttons, skulls, the crystal ball) arrives pre-cancelled,
	 * but this handler doesn't {@code ignoreCancelled}, so those clicks reached the progress calls anyway.  So a
	 * spectator could open the chest, take the secret's credit, collect essence and answer the Quiz for the party.
	 * Hence: cancel FIRST (that's what shuts the chest GUI), then check this and return without progressing.
	 */
	private static boolean cannotInteract(Player p) {
		return Utils.isSpectator(p);
	}

	@EventHandler
	public void onRightClickBlock(PlayerInteractEvent e) {
		if(!ClearManager.isActive()) return;
		if(e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		Block b = e.getClickedBlock();
		if(b == null) return;
		Player p = e.getPlayer();

		// Quiz answer buttons
		int btn = PuzzleQuiz.buttonIndex(b);
		if(btn >= 0) {
			e.setCancelled(true);
			if(cannotInteract(p)) return;
			PuzzleQuiz.answer(p, btn);
			return;
		}
		// Crystal ball (±1 block)
		if(Math.abs(b.getX() - CRYSTAL[0]) <= 1 && Math.abs(b.getY() - CRYSTAL[1]) <= 1 && Math.abs(b.getZ() - CRYSTAL[2]) <= 1) {
			e.setCancelled(true);
			if(cannotInteract(p)) return;
			ClearManager.pickUpCrystal(p);
			return;
		}
		// Secret chests (right-click, no GUI) and essence skulls (right-click to collect).
		Secret s = ClearManager.findSecretAtBlock(b.getX(), b.getY(), b.getZ());
		if(s != null) {
			// ALWAYS cancel: so an already-opened chest never shows the vanilla GUI, and so a SPECTATOR never gets
			// it either (their click arrives here uncancelled - see cannotInteract).
			e.setCancelled(true);
			if(cannotInteract(p)) return;
			if(!s.found) {
				if(s.isChest()) ClearManager.openChest(p, s);
				else ClearManager.secretFound(p, s); // essence
			}
		}
	}

	// Wizard crystal hand-in via RIGHT-click. NOT ignoreCancelled: MiscListener cancels villager right-clicks
	// at LOWEST to block the trade GUI, and we still want the hand-in to fire.
	@EventHandler(priority = EventPriority.NORMAL)
	public void onRightClickEntity(PlayerInteractEntityEvent e) {
		if(!ClearManager.isActive()) return;
		if(cannotInteract(e.getPlayer())) return; // a spectator can't hand the crystal in either
		if(e.getRightClicked() instanceof Villager v && ClearManager.hasCrystal() && isWizard(v)) {
			ClearManager.handInCrystal(e.getPlayer());
		}
	}

	// Wizard crystal hand-in via LEFT-click (attack). Uses PrePlayerAttackEntityEvent, which fires on the
	// attack itself, so it works even though the Wizard villager takes no damage, being invulnerable or cancelled.
	@EventHandler(priority = EventPriority.NORMAL)
	public void onLeftClickEntity(io.papermc.paper.event.player.PrePlayerAttackEntityEvent e) {
		if(!ClearManager.isActive()) return;
		if(cannotInteract(e.getPlayer())) return;
		if(e.getAttacked() instanceof Villager v && ClearManager.hasCrystal() && isWizard(v)) {
			ClearManager.handInCrystal(e.getPlayer());
		}
	}

	/** The Wizard is the villager in the Wizard room (name-independent, so it survives map re-labels). */
	private static boolean isWizard(Villager v) {
		String name = Utils.plain(v.customName());
		return Rooms.roomAt(v.getLocation()) == Rooms.WIZARD || name.contains("Wizard");
	}
}
