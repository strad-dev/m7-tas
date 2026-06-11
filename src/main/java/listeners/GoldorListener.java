package listeners;

import instructions.Actions;
import instructions.bosses.goldor.Goldor;
import instructions.bosses.goldor.GoldorLever;
import instructions.bosses.goldor.GoldorSection;
import instructions.bosses.goldor.GoldorTerminal;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import plugin.Utils;

public class GoldorListener implements Listener {

	/** Single registered instance (see M7tas.onEnable). Lets static reset paths reach instance state. */
	public static GoldorListener INSTANCE;

	public GoldorListener() {
		INSTANCE = this;
	}

	// ------ Per-device runtime state (cleared on each phase via Goldor's resetState by reference) ------

	// Simon Says: cumulative GLOBAL click count (15 activates, no time limit). NOT per-player — any players'
	// clicks accumulate toward 15. Reset on device completion and on serverSetup (see resetSimon).
	private int simonClicks = 0;
	// Sharp Shooter: hit state on the 9 target blocks
	private final boolean[][] sharpHits = new boolean[3][3]; // [xIdx 0..2 → 68/66/64][yIdx 0..2 → 130/128/126]
	private int sharpHitCount = 0;

	// Simon Says button coord
	private static final int SIMON_BX = 110, SIMON_BY = 121, SIMON_BZ = 91;
	// Sharp Shooter plate coord
	private static final int PLATE_X = 63, PLATE_Y = 127, PLATE_Z = 35;
	// Sharp Shooter target Z
	private static final int TARGET_Z = 50;
	// Sharp Shooter target X values
	private static final int[] TARGET_XS = {68, 66, 64};
	// Sharp Shooter target Y values
	private static final int[] TARGET_YS = {130, 128, 126};
	// Lights levers bounding box (S2 device)
	private static final int LIGHTS_X1 = 58, LIGHTS_X2 = 62;
	private static final int LIGHTS_Y1 = 133, LIGHTS_Y2 = 136;
	private static final int LIGHTS_Z = 142;

	// =================== Terminal click (right-click) ===================
	@EventHandler(priority = EventPriority.LOW)
	public void onInteractAt(PlayerInteractAtEntityEvent e) {
		tryActivateTerminal(e.getRightClicked(), e.getPlayer());
	}

	// =================== Terminal click (left-click) ===================
	@EventHandler(priority = EventPriority.LOW)
	public void onLeftClickTerminal(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
		if(!(e.getDamager() instanceof Player p)) return;
		if(tryActivateTerminal(e.getEntity(), p)) e.setCancelled(true);
	}

	/** Returns true if the entity was a terminal Interaction belonging to the current section
	 *  and the activation was accepted (or already pending — either way, the click is "consumed"). */
	private boolean tryActivateTerminal(Entity ent, Player p) {
		if(Goldor.INSTANCE.isPhaseInactive()) return false;
		String tagPrefix = GoldorTerminal.TAG_PREFIX;
		for(String tag : ent.getScoreboardTags()) {
			if(!tag.startsWith(tagPrefix)) continue;
			int[] idx = GoldorTerminal.parseTag(tag);
			if(idx == null) return false;
			GoldorSection sec = Goldor.INSTANCE.getSection(idx[0]);
			if(sec == null) return false;
			if(idx[0] != Goldor.INSTANCE.getCurrentSectionIdx()) return false; // section-gated
			if(idx[1] < 0 || idx[1] >= sec.terminals.size()) return false;
			GoldorTerminal term = sec.terminals.get(idx[1]);
			if(term.isActivated() || term.isPending()) return true;

			term.setPending();
			Actions.clearMovementInput(p);
			Utils.scheduleTask(() -> {
				if(term.isActivated()) return;
				term.markActivated();
				Goldor.INSTANCE.onActivation(p, sec, "terminal");
			}, 1L);
			return true;
		}
		return false;
	}

	// =================== Lever flip + Simon Says button + Lights levers ===================
	@EventHandler(priority = EventPriority.LOW)
	public void onInteract(PlayerInteractEvent e) {
		if(e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		// Only the main hand — a sneaking right-click fires a second (off-hand) event for the same click,
		// which would otherwise double-count the Simon Says button.
		if(e.getHand() != EquipmentSlot.HAND) return;
		Block b = e.getClickedBlock();
		if(b == null) return;
		int bx = b.getX(), by = b.getY(), bz = b.getZ();
		Player p = e.getPlayer();

		// Simon Says button (S1 device) — not section-gated. Defer if the phase hasn't spun up yet so a
		// click in a chained full run (players scheduled on start, Goldor only active when Storm dies) counts.
		if(bx == SIMON_BX && by == SIMON_BY && bz == SIMON_BZ) {
			runWhenPhaseActive(deferred -> processSimonClick(p, deferred));
			return;
		}

		// Lights levers (S2 device) — not section-gated; defer too.
		if(b.getType() == Material.LEVER
				&& bx >= LIGHTS_X1 && bx <= LIGHTS_X2
				&& by >= LIGHTS_Y1 && by <= LIGHTS_Y2
				&& bz == LIGHTS_Z) {
			runWhenPhaseActive(deferred -> processLightsClick(p, deferred));
			return;
		}

		// Section levers — section-gated, so they only matter once the phase is active.
		if(Goldor.INSTANCE.isPhaseInactive()) return;
		if(b.getType() == Material.LEVER) {
			for(GoldorSection sec : new GoldorSection[]{
					Goldor.INSTANCE.getSection(0),
					Goldor.INSTANCE.getSection(1),
					Goldor.INSTANCE.getSection(2),
					Goldor.INSTANCE.getSection(3)}) {
				if(sec == null) continue;
				for(GoldorLever lev : sec.levers) {
					if(lev.isLeverBlock(bx, by, bz)) {
						if(sec.idx != Goldor.INSTANCE.getCurrentSectionIdx()) return; // section-gated
						if(lev.isActivated()) return;
						lev.markActivated();
						Goldor.INSTANCE.onActivation(p, sec, "lever");
						return;
					}
				}
			}
		}
	}

	/** Register one Simon Says click (global counter). Safe to call from the deferred path — re-checks state. */
	private void processSimonClick(Player p, boolean wasDeferred) {
		GoldorSection s1 = Goldor.INSTANCE.getSection(0);
		if(s1 == null || s1.device.isActivated()) return;
		simonClicks++;
		Utils.debug(Utils.DebugType.BOSS, "Button clicked by " + Utils.getRealName(p) + " " + simonClicks + "/15");
		if(simonClicks >= 15) {
			s1.device.markActivated();
			Goldor.INSTANCE.onActivation(p, s1, "device", wasDeferred);
			simonClicks = 0;
		}
	}

	/** Activate the S2 Lights device. Safe to call from the deferred path — re-checks state. */
	private void processLightsClick(Player p, boolean wasDeferred) {
		GoldorSection s2 = Goldor.INSTANCE.getSection(1);
		if(s2 == null || s2.device.isActivated()) return;
		s2.device.markActivated();
		Goldor.INSTANCE.onActivation(p, s2, "device", wasDeferred);
	}

	/** Run the action now if the Goldor phase is active, else give it a one-tick grace and retry once. The only
	 *  legitimate race is sub-tick ordering: an interaction can be processed the same tick the phase activates
	 *  but before the activation task runs that tick, so it's active by the next tick. A larger window would
	 *  just mask genuine mistimings, so if it's still inactive next tick the interaction is dropped. */
	private void runWhenPhaseActive(java.util.function.Consumer<Boolean> action) {
		if(!Goldor.INSTANCE.isPhaseInactive()) { action.accept(false); return; }
		// Deferred: ran a tick late, so the action is told it was deferred (it credits the click's true tick).
		Utils.scheduleTask(() -> { if(!Goldor.INSTANCE.isPhaseInactive()) action.accept(true); }, 1L);
	}

	// =================== Item frame rotation: only S3 frames affected (creative players bypass) ===================
	@EventHandler(priority = EventPriority.LOWEST)
	public void onInteractEntity(PlayerInteractEntityEvent e) {
		if(!(e.getRightClicked() instanceof ItemFrame frame)) return;
		Player p = e.getPlayer();
		if(Goldor.INSTANCE.isPhaseInactive()) {
			// Defer an arrow-frame solve that lands before the phase spins up (full-run chain timing). Use a
			// phase-independent bounds check since isProtectedFrame/getArrowAlignFrame need an active phase.
			if(Goldor.INSTANCE.isInS3FrameRegion(frame)) runWhenPhaseActive(deferred -> processArrowFrame(frame, p, deferred));
			return;
		}
		if(!Goldor.INSTANCE.isProtectedFrame(frame)) return; // frames outside S3 behave normally
		ItemFrame arrow = Goldor.INSTANCE.getArrowAlignFrame();
		if(arrow != null && frame.equals(arrow)) {
			// processArrowFrame rotates explicitly; cancel so vanilla doesn't ALSO rotate it (double-turn)
			// when the held item happens to be exempt from CustomItems' interaction cancel.
			if(processArrowFrame(frame, p, false)) e.setCancelled(true);
			return;
		}
		if(p.getGameMode() == GameMode.CREATIVE) return; // creative bypass
		e.setCancelled(true);
	}

	/** Solve the S3 Arrow Align device. Returns true if this call activated it (caller suppresses vanilla's
	 *  rotation). Safe to call from the deferred path — re-checks all state. */
	private boolean processArrowFrame(ItemFrame frame, Player p, boolean wasDeferred) {
		if(Goldor.INSTANCE.isPhaseInactive()) return false;
		ItemFrame arrow = Goldor.INSTANCE.getArrowAlignFrame();
		if(arrow == null || !frame.equals(arrow)) return false;
		GoldorSection s3 = Goldor.INSTANCE.getSection(2);
		if(s3 == null || s3.device.isActivated()) return false;
		s3.device.markActivated();
		Goldor.INSTANCE.onActivation(p, s3, "device", wasDeferred);
		// CustomItems cancels this interaction when the fake player holds a non-exempt custom item, which skips
		// vanilla's rotation — replicate the normal one-step turn here.
		frame.setRotation(frame.getRotation().rotateClockwise());
		return true;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onInteractAtFrame(PlayerInteractAtEntityEvent e) {
		if(Goldor.INSTANCE.isPhaseInactive()) return;
		if(!(e.getRightClicked() instanceof ItemFrame frame)) return;
		if(!Goldor.INSTANCE.isProtectedFrame(frame)) return;
		ItemFrame arrow = Goldor.INSTANCE.getArrowAlignFrame();
		if(arrow != null && frame.equals(arrow)) return;
		if(e.getPlayer().getGameMode() == GameMode.CREATIVE) return; // creative bypass
		e.setCancelled(true);
	}

	// =================== Punching items out of S3 frames: cancelled (creative bypass) ===================
	@EventHandler(priority = EventPriority.LOWEST)
	public void onFramePunch(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
		if(Goldor.INSTANCE.isPhaseInactive()) return;
		if(e.getEntity() instanceof ItemFrame frame && Goldor.INSTANCE.isProtectedFrame(frame)) {
			if(e.getDamager() instanceof Player p && p.getGameMode() == GameMode.CREATIVE) return; // creative bypass
			e.setCancelled(true);
		}
	}

	// Broader EntityDamageEvent fallback (non-entity damage sources). No player → no creative bypass.
	@EventHandler(priority = EventPriority.LOWEST)
	public void onFrameDamage(org.bukkit.event.entity.EntityDamageEvent e) {
		if(Goldor.INSTANCE.isPhaseInactive()) return;
		if(e instanceof org.bukkit.event.entity.EntityDamageByEntityEvent) return; // handled by onFramePunch
		if(e.getEntity() instanceof ItemFrame frame && Goldor.INSTANCE.isProtectedFrame(frame)) {
			e.setCancelled(true);
		}
	}

	// =================== Sharp Shooter arrows ===================
	@EventHandler(priority = EventPriority.LOW)
	public void onProjectileHit(ProjectileHitEvent e) {
		Block hit = e.getHitBlock();
		if(hit == null) return;
		if(hit.getZ() != TARGET_Z) return;

		int xIdx = -1, yIdx = -1;
		for(int i = 0; i < TARGET_XS.length; i++) if(hit.getX() == TARGET_XS[i]) { xIdx = i; break; }
		for(int i = 0; i < TARGET_YS.length; i++) if(hit.getY() == TARGET_YS[i]) { yIdx = i; break; }
		if(xIdx < 0 || yIdx < 0) return;

		World world = hit.getWorld();
		Player shooter = (e.getEntity().getShooter() instanceof Player pl) ? pl : null;

		// Boundary case: a pre-fired arrow can land before the Goldor phase spins up, so isPhaseInactive()
		// is still true here and the hit (and the arrow, removed by MiscListener) would be lost. Defer the
		// registration until the phase is active instead of dropping it.
		if(Goldor.INSTANCE.isPhaseInactive()) {
			deferSharpHit(world, xIdx, yIdx, shooter);
			return;
		}
		registerSharpHit(world, xIdx, yIdx, shooter, false);
	}

	/** A pre-fired arrow can land the same tick the phase activates but before the activation task runs that
	 *  tick. Give it a one-tick grace and retry the registration once; if still inactive, drop it (so a real
	 *  mistiming surfaces rather than being masked). */
	private void deferSharpHit(World world, int xIdx, int yIdx, Player shooter) {
		Utils.scheduleTask(() -> {
			if(!Goldor.INSTANCE.isPhaseInactive()) registerSharpHit(world, xIdx, yIdx, shooter, true);
		}, 1L);
	}

	/** Register a single Sharp Shooter target hit (idempotent per target). Completes the S4 device on the
	 *  ninth distinct hit. Re-checks all gates itself so it is safe to call from a deferred (next-tick) task. */
	private void registerSharpHit(World world, int xIdx, int yIdx, Player shooter, boolean wasDeferred) {
		if(Goldor.INSTANCE.isPhaseInactive()) return;
		GoldorSection s4 = Goldor.INSTANCE.getSection(3);
		if(s4 == null || s4.device.isActivated()) return;
		if(!isPlayerOnPlate()) return;
		if(sharpHits[xIdx][yIdx]) return;

		sharpHits[xIdx][yIdx] = true;
		sharpHitCount++;
		setTargetBlock(world, xIdx, yIdx, TARGET_HIT);
		if(sharpHitCount >= 9) {
			if(shooter == null) {
				for(Player pl : Bukkit.getOnlinePlayers()) { shooter = pl; break; }
			}
			s4.device.markActivated();
			if(shooter != null) Goldor.INSTANCE.onActivation(shooter, s4, "device", wasDeferred);
			resetSharpShooter(world);
		}
	}

	private boolean isPlayerOnPlate() {
		Location plate = new Location(Bukkit.getWorlds().getFirst(), PLATE_X + 0.5, PLATE_Y, PLATE_Z + 0.5);
		for(Player p : Bukkit.getOnlinePlayers()) {
			Location pl = p.getLocation();
			if(Math.abs(pl.getX() - (PLATE_X + 0.5)) <= 0.6
					&& Math.abs(pl.getZ() - (PLATE_Z + 0.5)) <= 0.6
					&& Math.abs(pl.getY() - PLATE_Y) <= 1.5) {
				return true;
			}
		}
		return false;
	}

	private void resetSharpHits() {
		for(int i = 0; i < 3; i++) for(int j = 0; j < 3; j++) sharpHits[i][j] = false;
		sharpHitCount = 0;
	}

	/** Reset the Simon Says global click counter. Invoked on server reset ({@link instructions.Server#serverSetup})
	 *  so a new run never inherits clicks from a previous (possibly aborted) run. */
	public void resetSimon() {
		simonClicks = 0;
	}

	// Sharp Shooter target block materials: blue = resting/solved, red = arrow-hit.
	private static final Material TARGET_RESTING = Material.BLUE_TERRACOTTA;
	private static final Material TARGET_HIT = Material.RED_TERRACOTTA;

	/** Set the (xIdx, yIdx) Sharp Shooter target block to the given material (physics suppressed). */
	private void setTargetBlock(World world, int xIdx, int yIdx, Material mat) {
		world.getBlockAt(TARGET_XS[xIdx], TARGET_YS[yIdx], TARGET_Z).setType(mat, false);
	}

	/** Revert all nine Sharp Shooter targets to blue and clear hit state.
	 *  Invoked on device completion and on server reset ({@link instructions.Server#serverSetup}). */
	public void resetSharpShooter(World world) {
		for(int i = 0; i < TARGET_XS.length; i++) {
			for(int j = 0; j < TARGET_YS.length; j++) {
				setTargetBlock(world, i, j, TARGET_RESTING);
			}
		}
		resetSharpHits();
	}

	// =================== Item-frame indestructibility (S3 only, creative bypass) ===================
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onHangingBreak(HangingBreakEvent e) {
		if(Goldor.INSTANCE.isPhaseInactive()) return;
		if(!(e.getEntity() instanceof ItemFrame frame) || !Goldor.INSTANCE.isProtectedFrame(frame)) return;
		if(e instanceof org.bukkit.event.hanging.HangingBreakByEntityEvent be
				&& be.getRemover() instanceof Player p
				&& p.getGameMode() == GameMode.CREATIVE) return;
		e.setCancelled(true);
	}

	// =================== Gate explosion (real EntityExplodeEvent fallback) ===================
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent e) {
		if(Goldor.INSTANCE.isPhaseInactive()) return;
		Location loc = e.getEntity().getLocation();
		Goldor.INSTANCE.notifyExplosionAt(loc);
	}
}
