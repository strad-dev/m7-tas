package listeners;

import instructions.Actions;
import instructions.bosses.goldor.Goldor;
import instructions.bosses.goldor.GoldorLever;
import instructions.bosses.goldor.GoldorSection;
import instructions.bosses.goldor.GoldorTerminal;
import instructions.bosses.goldor.GoldorTerminalGui;
import net.minecraft.server.MinecraftServer;
import org.bukkit.*;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import plugin.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GoldorListener implements Listener {

	/** Single registered instance (see M7tas.onEnable). Lets static reset paths reach instance state. */
	public static GoldorListener INSTANCE;

	public GoldorListener() {
		INSTANCE = this;
		// Java zero-fills sharpHitTick, and 0 is a real tick value: give every device a defined starting state
		// rather than relying on the first /setup to have run.
		resetSharpHits();
	}

	// ------ Per-device runtime state (cleared on each phase via Goldor's resetState by reference) ------

	// Simon Says: cumulative GLOBAL click count (15 activates, no time limit).  This is NOT per-player: any
	// players' clicks accumulate toward 15.  Reset on device completion and on serverSetup (see resetSimon).
	private int simonClicks = 0;
	// Last server tick each player registered a Simon Says click on.  A single physical right-click can surface as
	// TWO PlayerInteractEvents on the same tick: the off-hand event while sneaking, and/or vanilla re-firing after
	// PlayerPacketInterceptor resets its interact-dedupe.  One click cannot span two ticks, so I collapse any
	// repeat from the same player within the same tick.  That fixes the same-tick double-count without
	// rate-limiting genuine clicks, which land on different ticks.  Stale entries self-expire via the comparison.
	private final Map<UUID, Integer> lastSimonClickTick = new HashMap<>();
	// The last toggle of each S2 "Lights" lever, keyed by its "x_y_z".  In ADVENTURE a single physical click emits
	// BOTH a right-click and a phantom left-click on the SAME lever a tick or two apart, and toggling on each
	// flip-flops it into an undo.  I swallow ONLY that phantom: an OPPOSITE-type click on the same lever within a
	// couple of ticks.  Same-button spam is never throttled, because people click fast, and different levers are
	// independent, so fast solving still registers every lever.
	private record LeverToggle(int tick, boolean wasRight) {}
	private final Map<String, LeverToggle> lastS2LeverToggle = new HashMap<>();
	private static final int S2_PHANTOM_WINDOW_TICKS = 2;
	// Sharp Shooter: hit state on the 9 target blocks
	private final boolean[][] sharpHits = new boolean[3][3]; // [xIdx 0..2 → 68/66/64][yIdx 0..2 → 130/128/126]
	private int sharpHitCount = 0;
	/**
	 * ULTRA-REALISTIC ONLY: which target the emerald block is on, as a sequence index 0..8, or -1 before the plate
	 * has started the device and 9 once it has run off the end.
	 * <p>
	 * The sequence IS the array order: {@code seq = yIdx * 3 + xIdx}, so it walks -X across a row and then -Y down
	 * to the next one - {@code TARGET_XS} and {@code TARGET_YS} are both already listed descending. First is
	 * (68, 130), last is (64, 126), and each of the nine is the active block at most once; it never loops.
	 */
	private int sharpCursor = -1;
	/**
	 * ULTRA-REALISTIC ONLY: the server tick an arrow last struck each target, or {@code Integer.MIN_VALUE} for never.
	 * <p>
	 * <b>Scratch for one tick, not a bank.</b>  A hit on anything that is not the active target does not count - it
	 * only leaves this stamp, and the stamp is worth something solely while the tick it names is still the current
	 * one.  See {@link #registerSequentialHit}.
	 */
	private final int[][] sharpHitTick = new int[3][3];
	/** World the sequential device was armed in, so {@link #pollSharpPlate} can reset it without guessing. */
	private World sharpWorld;
	/**
	 * Server tick the plate was FIRST seen empty on while the device was running, or -1 while it is occupied.
	 * <p>
	 * This is the one tick of grace that makes a solve beat a reset - see {@link #pollSharpPlate}.
	 */
	private int sharpPlateEmptySince = -1;

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
	// The redstone lamps sit one block behind the levers, on the blocks the levers are mounted on.  The device is
	// only solved when EVERY lamp in this grid is lit.
	private static final int LIGHTS_LAMP_Z = 143;

	/**
	 * True if {@code p} must not be able to progress a Goldor device (the shared {@link Utils#isSpectator}
	 * predicate, named for what it means here): a spectator, which is the idle state on m7
	 * where they are watching rather than running the phase, or a player spectating a fake.  This is not redundant
	 * with vanilla's own spectator gating: CraftBukkit still fires the interact events for a spectator's clicks, and the raw
	 * {@code ServerboundUseItemOnPacket} path in {@code PlayerPacketInterceptor} → {@link #tryRegisterSimonClick}
	 * bypasses vanilla entirely, so without this a spectator could click a terminal or spam Simon Says.
	 * <p>
	 * Checked at every entry point AND again in the {@code process*} solvers, which can run a tick later off
	 * {@link #runWhenPhaseActive}.  That is the same "re-check all state on the deferred path" rule the rest of
	 * this class uses.
	 */
	private static boolean cannotSolve(Player p) {
		return Utils.isSpectator(p);
	}

	// =================== Terminal click (right-click) ===================
	@EventHandler(priority = EventPriority.LOW)
	public void onInteractAt(PlayerInteractAtEntityEvent e) {
		if(cannotSolve(e.getPlayer())) return;
		tryActivateTerminal(e.getRightClicked(), e.getPlayer());
	}

	// =================== Terminal click (left-click) ===================
	@EventHandler(priority = EventPriority.LOW)
	public void onLeftClickTerminal(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
		if(!(e.getDamager() instanceof Player p)) return;
		if(cannotSolve(p)) return;
		if(tryActivateTerminal(e.getEntity(), p)) e.setCancelled(true);
	}

	/** Returns true if the entity was a terminal Interaction belonging to the current section
	*  and the activation was accepted, or was already pending.  Either way the click is "consumed". */
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
			// Ultra-realistic: the click opens the terminal's puzzle and solving it is what activates.  The pending
			// flag now spans the whole time the GUI is open, which is what makes a terminal one player's at a time.
			if(damage.Difficulty.deathsEnabled()) {
				GoldorTerminalGui.open(p, term);
				return true;
			}
			Utils.scheduleTask(() -> {
				if(term.isActivated()) return;
				term.markActivated();
				Goldor.INSTANCE.onActivation(p, sec, "terminal");
			}, 1L);
			return true;
		}
		return false;
	}

	// =================== Terminal puzzle GUI (ultra-realistic only) ===================

	/**
	 * Every click inside a terminal puzzle, in either inventory, is cancelled first and only then read as a possible
	 * solve.  The puzzles are click targets, not inventories, so nothing may be picked up, moved, dropped,
	 * shift-clicked in or number-keyed out - see {@link GoldorTerminalGui}.
	 * <p>
	 * A solve is credited to the tick the click landed on, not a tick later: there is no deferral to do here, unlike
	 * the one-click path, because the phase is unquestionably active by the time a GUI is open.
	 */
	@EventHandler(priority = EventPriority.LOW)
	public void onTerminalGuiClick(InventoryClickEvent e) {
		if(!(e.getView().getTopInventory().getHolder() instanceof GoldorTerminalGui gui)) return;
		e.setCancelled(true);
		if(!(e.getWhoClicked() instanceof Player p)) return;
		if(cannotSolve(p)) return;
		// Only clicks in the puzzle itself count; a click down in the player's own inventory is just cancelled.
		if(e.getClickedInventory() != e.getView().getTopInventory()) return;
		if(!gui.onClick(e.getSlot(), e.getClick())) return;

		GoldorTerminal term = gui.terminal();
		GoldorSection sec = Goldor.INSTANCE.getSection(term.sectionIdx);
		// Activate NOW, on the click's own tick, so the terminal's time is the time the player earned.  The close is
		// deferred a tick: closing a view from inside its own click event is the one thing Bukkit asks you not to do,
		// and a second click in the meantime is harmless - the GUI latches "solved" and answers no.
		if(sec != null && !term.isActivated()) {
			term.markActivated();
			Goldor.INSTANCE.onActivation(p, sec, "terminal");
		}
		Bukkit.getScheduler().runTask(plugin.M7tas.getInstance(), () -> p.closeInventory());
	}

	/** Dragging is another way to move items, so it is refused wholesale. */
	@EventHandler(priority = EventPriority.LOW)
	public void onTerminalGuiDrag(InventoryDragEvent e) {
		if(e.getView().getTopInventory().getHolder() instanceof GoldorTerminalGui) e.setCancelled(true);
	}

	/**
	 * Closing a puzzle without solving it hands the terminal back: the pending flag goes, so anyone (including the
	 * same player) can open it again, and the progress is gone with the view - a half-finished Melody starts over.
	 * A solved puzzle closes itself from the click handler above, and must NOT clear the flag, or the pending state
	 * would outlive the activation it belongs to.
	 */
	@EventHandler
	public void onTerminalGuiClose(InventoryCloseEvent e) {
		if(!(e.getView().getTopInventory().getHolder() instanceof GoldorTerminalGui gui)) return;
		if(gui.isSolved()) return;
		gui.terminal().clearPending();
	}

	// =================== Lever flip + Simon Says button + Lights levers ===================
	@EventHandler(priority = EventPriority.LOW)
	public void onInteract(PlayerInteractEvent e) {
		boolean rightClick = e.getAction() == Action.RIGHT_CLICK_BLOCK;
		boolean leftClick = e.getAction() == Action.LEFT_CLICK_BLOCK;
		if(!rightClick && !leftClick) return;
		// Only the main hand.  A sneaking right-click fires a second, off-hand event for the same click,
		// which would otherwise double-count the Simon Says button.
		if(e.getHand() != EquipmentSlot.HAND) return;
		Block b = e.getClickedBlock();
		if(b == null) return;
		int bx = b.getX(), by = b.getY(), bz = b.getZ();
		Player p = e.getPlayer();
		// Spectators can't solve anything, so return BEFORE the Lights branch and we also don't cancel their event
		// or own the lever toggle on their behalf.  Vanilla ignores a spectator's click by itself.
		if(cannotSolve(p)) return;

		// Simon Says button (S1 device), right-click only since it's a button.  Defer if the phase hasn't spun up yet
		// so a click in a chained full run counts: players are scheduled on start, but Goldor is only active when
		// Storm dies.
		if(bx == SIMON_BX && by == SIMON_BY && bz == SIMON_BZ) {
			if(rightClick) tryRegisterSimonClick(p, bx, by, bz);
			return;
		}

		// Lights levers (S2 device): the puzzle reads the lever's physical toggled state.  I OWN the toggle for both
		// clicks: cancel the event so vanilla never flips the lever, then flip it myself exactly once per tick.
		// Relying on vanilla's right-click toggle made the device behave differently per gamemode (adventure vs
		// survival/creative) and could double-toggle from the duplicate UseItemOn packets one click sends (flip →
		// unflip → "undo"); owning it makes the flip deterministic and identical in every gamemode. Deduped per tick
		// because one physical click can surface as several same-tick interact events.
		if(b.getType() == Material.LEVER
				&& bx >= LIGHTS_X1 && bx <= LIGHTS_X2
				&& by >= LIGHTS_Y1 && by <= LIGHTS_Y2
				&& bz == LIGHTS_Z) {
			e.setCancelled(true); // don't let vanilla toggle or break the lever; I do it myself
			int now = MinecraftServer.currentTick;
			String leverKey = bx + "_" + by + "_" + bz;
			LeverToggle last = lastS2LeverToggle.get(leverKey);
			// Phantom = an opposite-type click on this lever within a couple ticks of the last one (the adventure
			// right→left echo). Real repeated clicks are the same type (or land later), so they always toggle.
			boolean phantom = last != null && last.wasRight() != rightClick && now - last.tick() <= S2_PHANTOM_WINDOW_TICKS;
			if(!phantom) {
				lastS2LeverToggle.put(leverKey, new LeverToggle(now, rightClick));
				toggleLever(b);
			}
			runWhenPhaseActive(deferred -> processLightsClick(p));
			return;
		}

		// Section levers are section-gated, so they only matter once the phase is active.
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

	/**
	 * Register a Simon Says button click from {@code p} when (bx,by,bz) is the button, deduped to once per player
	 * per server tick.  A single physical click can surface twice on the same tick: the off-hand event while
	 * sneaking, and/or vanilla re-firing the interact after {@code PlayerPacketInterceptor} resets its dedupe.
	 * One click cannot span two ticks, so any same-tick repeat from a player is dropped.
	 *
	 * <p>Called from two places, both main-thread and both funneling through this single guard:
	 * <ul>
	 *   <li>the vanilla {@link #onInteract} PlayerInteractEvent, the path fake players take via simulated packets;
	 *   <li>the raw {@code ServerboundUseItemOnPacket} in {@code PlayerPacketInterceptor}, for real players, which
	 *       bypasses vanilla's interact-event suppression so rapid clicks aren't throttled to a few per second.
	 * </ul>
	 */
	public static void tryRegisterSimonClick(Player p, int bx, int by, int bz) {
		if(INSTANCE == null) return;
		if(cannotSolve(p)) return; // the interceptor path skips vanilla's spectator gating entirely
		if(bx != SIMON_BX || by != SIMON_BY || bz != SIMON_BZ) return;
		int now = MinecraftServer.currentTick;
		if(INSTANCE.lastSimonClickTick.getOrDefault(p.getUniqueId(), -1) == now) return;
		INSTANCE.lastSimonClickTick.put(p.getUniqueId(), now);
		INSTANCE.runWhenPhaseActive(deferred -> INSTANCE.processSimonClick(p, deferred));
	}

	/** Register one Simon Says click on the global counter.  Safe to call from the deferred path, since it re-checks state. */
	private void processSimonClick(Player p, boolean wasDeferred) {
		if(cannotSolve(p)) return;
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

	/** Activate the S2 Lights device, but ONLY once every redstone lamp is lit.  The clicked lever hasn't toggled
	*  yet (vanilla flips it AFTER this PlayerInteractEvent) and its lamp won't relight until the resulting block
	*  update settles, so the lamp grid is read on the NEXT tick; a flip that doesn't complete the puzzle is a no-op.
	*  The activation is credited to the completing click's own tick: the lamp lit this tick and I just observe it
	*  one tick later, hence wasDeferred=true. */
	private void processLightsClick(Player p) {
		if(cannotSolve(p)) return;
		GoldorSection s2 = Goldor.INSTANCE.getSection(1);
		if(s2 == null || s2.device.isActivated()) return;
		World w = p.getWorld();
		Utils.scheduleTask(() -> {
			if(s2.device.isActivated() || cannotSolve(p)) return;
			if(!allLightsLit(w)) return;
			s2.device.markActivated();
			Goldor.INSTANCE.onActivation(p, s2, "device", true);
		}, 1L);
	}

	/** Flip an S2 Lights lever's powered state (physics on, so the redstone lamp behind it re-lights/unlights).
	 *  Used to make a LEFT-click behave like a right-click: vanilla only toggles a lever on right-click, so a
	 *  left-click must flip it explicitly. */
	private static void toggleLever(Block b) {
		if(b.getBlockData() instanceof org.bukkit.block.data.Powerable pw) {
			pw.setPowered(!pw.isPowered());
			b.setBlockData(pw, true); // physics=true: updates the lever's own neighbours → lights the lamp directly behind it
			// Vanilla LeverBlock.pull does TWO neighbour updates: the lever's own block AND the block the lever is
			// mounted on. That second update is what lets the strongly-powered mount lamp re-light the lamps around it
			// (a redstone lamp adjacent to a strongly-powered solid block lights up too → the vanilla cross section).
			// Bukkit's setBlockData only did the lever's own update, so replicate the mount-block update here; without
			// it only the single lamp directly behind the lever would light.
			net.minecraft.server.level.ServerLevel level = ((org.bukkit.craftbukkit.CraftWorld) b.getWorld()).getHandle();
			net.minecraft.core.BlockPos mountPos = new net.minecraft.core.BlockPos(b.getX(), b.getY(), b.getZ() + 1); // lamp is at LIGHTS_LAMP_Z = lever z + 1
			level.updateNeighborsAt(mountPos, net.minecraft.world.level.block.Blocks.LEVER, null);
		}
	}

	/** True only if EVERY redstone lamp of the S2 Lights device (the lever mount blocks at z=143) is lit. */
	private static boolean allLightsLit(World w) {
		for(int x = LIGHTS_X1; x <= LIGHTS_X2; x++) {
			for(int y = LIGHTS_Y1; y <= LIGHTS_Y2; y++) {
				Block b = w.getBlockAt(x, y, LIGHTS_LAMP_Z);
				if(b.getType() != Material.REDSTONE_LAMP) return false;
				if(!(b.getBlockData() instanceof org.bukkit.block.data.Lightable lamp) || !lamp.isLit()) return false;
			}
		}
		return true;
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
		if(Goldor.INSTANCE.isProtectedFrame(frame)) return; // frames outside S3 behave normally
		ItemFrame arrow = Goldor.INSTANCE.getArrowAlignFrame();
		if(frame.equals(arrow)) {
			// processArrowFrame rotates explicitly; cancel so vanilla doesn't ALSO rotate it (double-turn)
			// when the held item happens to be exempt from CustomItems' interaction cancel.
			if(processArrowFrame(frame, p, false)) e.setCancelled(true);
			return;
		}
		if(p.getGameMode() == GameMode.CREATIVE) return; // creative bypass
		e.setCancelled(true);
	}

	/** Solve the S3 Arrow Align device.  Returns true if this call activated it, in which case the caller
	*  suppresses vanilla's rotation.  Safe to call from the deferred path, since it re-checks all state. */
	private boolean processArrowFrame(ItemFrame frame, Player p, boolean wasDeferred) {
		if(cannotSolve(p)) return false; // guarded here, not in onInteractEntity, since frame PROTECTION still applies to spectators
		if(Goldor.INSTANCE.isPhaseInactive()) return false;
		ItemFrame arrow = Goldor.INSTANCE.getArrowAlignFrame();
		if(!frame.equals(arrow)) return false;
		GoldorSection s3 = Goldor.INSTANCE.getSection(2);
		if(s3 == null || s3.device.isActivated()) return false;
		s3.device.markActivated();
		Goldor.INSTANCE.onActivation(p, s3, "device", wasDeferred);
		// CustomItems cancels this interaction when the fake player holds a non-exempt custom item, which skips
		// vanilla's rotation, so replicate the normal one-step turn here.
		frame.setRotation(frame.getRotation().rotateClockwise());
		return true;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onInteractAtFrame(PlayerInteractAtEntityEvent e) {
		if(Goldor.INSTANCE.isPhaseInactive()) return;
		if(!(e.getRightClicked() instanceof ItemFrame frame)) return;
		if(Goldor.INSTANCE.isProtectedFrame(frame)) return;
		ItemFrame arrow = Goldor.INSTANCE.getArrowAlignFrame();
		if(frame.equals(arrow)) return;
		if(e.getPlayer().getGameMode() == GameMode.CREATIVE) return; // creative bypass
		e.setCancelled(true);
	}

	// =================== Punching items out of S3 frames: cancelled (creative bypass) ===================
	// Phase-independent (isInS3FrameRegion, not isProtectedFrame) so the items can never be knocked out, whether
	// in prep, between phases, or mid-phase.
	@EventHandler(priority = EventPriority.LOWEST)
	public void onFramePunch(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
		if(e.getEntity() instanceof ItemFrame frame && Goldor.INSTANCE.isInS3FrameRegion(frame)) {
			if(e.getDamager() instanceof Player p && p.getGameMode() == GameMode.CREATIVE) return; // creative bypass
			e.setCancelled(true);
		}
	}

	// Broader EntityDamageEvent fallback (non-entity damage sources). No player → no creative bypass. Phase-independent.
	@EventHandler(priority = EventPriority.LOWEST)
	public void onFrameDamage(org.bukkit.event.entity.EntityDamageEvent e) {
		if(e instanceof org.bukkit.event.entity.EntityDamageByEntityEvent) return; // handled by onFramePunch
		if(e.getEntity() instanceof ItemFrame frame && Goldor.INSTANCE.isInS3FrameRegion(frame)) {
			e.setCancelled(true);
		}
	}

	// =================== Sharp Shooter: the plate starts it (ultra-realistic only) ===================

	/**
	 * Stepping on the gold pressure plate BEGINS the S4 device: the emerald block appears on the first target and
	 * the player shoots their way along the sequence.  Ultra-realistic only - the other two modes keep the original
	 * device, where the nine targets are hit in any order and the plate has to be held for each hit.
	 * <p>
	 * {@code Action.PHYSICAL} is the pressure-plate event, which is why this is its own handler rather than a branch
	 * in {@link #onInteract} - that one returns early for anything that is not a left or right click, and its
	 * main-hand guard does not apply to a step.
	 * <p>
	 * <b>The plate must be HELD.</b>  Stepping on it begins the device; stepping off resets it outright - see
	 * {@link #pollSharpPlate}.  Arrows that land BEFORE it is stepped on do nothing at all, since the device has not
	 * begun, so the wall cannot be pre-fired at.
	 */
	@EventHandler(priority = EventPriority.LOW)
	public void onPlateStep(PlayerInteractEvent e) {
		if(e.getAction() != Action.PHYSICAL) return;
		Block b = e.getClickedBlock();
		if(b == null || b.getX() != PLATE_X || b.getY() != PLATE_Y || b.getZ() != PLATE_Z) return;
		if(cannotSolve(e.getPlayer())) return;
		if(!damage.Difficulty.deathsEnabled()) return;
		if(Goldor.INSTANCE.isPhaseInactive()) return;
		if(sharpCursor >= 0) return; // already running; a second step is not a restart

		GoldorSection s4 = Goldor.INSTANCE.getSection(3);
		if(s4 == null || s4.device.isActivated()) return;

		sharpCursor = 0;
		sharpWorld = b.getWorld();
		renderSharpTargets(sharpWorld);
	}

	/**
	 * Reset the sequential device the moment nobody is on the plate any more.
	 * <p>
	 * <b>The plate has to be HELD after all.</b>  Stepping on it begins the device, and stepping off throws the whole
	 * thing away - every target back to blue, every hit forgotten, the emerald gone - so it has to be started again
	 * from the first target.  Progress is not banked, which is the point: the run is "stay on the plate and clear all
	 * nine", not "chip away at it".
	 * <p>
	 * Polled per tick rather than driven by an event, because there is no "left the pressure plate" event to hook -
	 * {@code Action.PHYSICAL} only fires on the way ON.  It reuses {@link #isPlayerOnPlate}, the same predicate the
	 * classic device gates each hit on, so the two devices can never disagree about who counts as standing there
	 * (spectators included: a hovering ghost has never held this plate down).
	 * <p>
	 * <b>A SOLVE BEATS A RESET on the same tick</b>, which is why the reset lands a tick after the plate is first
	 * seen empty rather than immediately.  The two race: a scheduler task cannot see an arrow that has not landed
	 * yet, so resetting on sight would wipe the hit state out from under a ninth arrow arriving later in the very
	 * same tick, and {@link #registerSharpHit} would then reject it for the device not running.  One tick of grace
	 * settles it in the player's favour - a completing hit clears {@code sharpCursor} and this poll goes quiet on
	 * its own - and costs a genuine step-off one tick of latency that nobody can see.
	 */
	private static void pollSharpPlate() {
		if(INSTANCE == null || INSTANCE.sharpCursor < 0) return; // not running
		if(Goldor.INSTANCE.isPhaseInactive()) return;            // the next /setup resets it
		if(INSTANCE.isPlayerOnPlate()) {
			INSTANCE.sharpPlateEmptySince = -1;
			return;
		}
		int now = plugin.Utils.serverTick();
		if(INSTANCE.sharpPlateEmptySince < 0) {
			INSTANCE.sharpPlateEmptySince = now; // first tick empty: give this tick's arrows their chance
			return;
		}
		if(now == INSTANCE.sharpPlateEmptySince) return;
		World w = INSTANCE.sharpWorld;
		if(w != null) INSTANCE.resetSharpShooter(w);
		else INSTANCE.resetSharpHits();
	}

	/**
	 * Start the per-tick plate watch.  Registered from {@code M7tas.onEnable} as a raw repeating task, the same
	 * shape as {@code OutOfBounds.start} - untracked, so a boss teardown flushing the scheduler cannot silently
	 * stop it and leave a device that no longer resets.
	 */
	public static void startSharpPlatePoll() {
		if(sharpPlateTask != null) return;
		sharpPlateTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(
				plugin.M7tas.getInstance(), GoldorListener::pollSharpPlate, 1L, 1L);
	}

	public static void stopSharpPlatePoll() {
		if(sharpPlateTask != null) {
			sharpPlateTask.cancel();
			sharpPlateTask = null;
		}
	}

	private static org.bukkit.scheduler.BukkitTask sharpPlateTask;

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

	/**
	 * Register a single Sharp Shooter target hit (idempotent per target).  Completes the S4 device on the ninth
	 * distinct hit either way.  Re-checks all gates itself so it is safe to call from a deferred (next-tick) task.
	 * <p>
	 * <b>Two devices behind one hit.</b>  Classic and realistic keep the original: any order, checked against the
	 * plate per hit.  Ultra-realistic is sequential - {@link #onPlateStep} begins it and an emerald block marks the
	 * target to shoot - and the plate is watched per tick instead, so stepping off resets the whole device.
	 * <p>
	 * <b>Only the ACTIVE target counts</b> in the sequential device.  An arrow anywhere else on the wall does
	 * nothing at all - it is not banked for later, and the nine cannot be picked off out of order.
	 * <p>
	 * <b>The one exception is the SAME TICK</b>, and it exists only to make the ordering inside a tick irrelevant.
	 * {@code ProjectileHitEvent} fires once per arrow, so a volley arrives as several calls in one tick: hit the
	 * emerald and the next target together and both should complete, whichever of the two the server happens to
	 * process first.  See {@link #registerSequentialHit}.  Arrows landing a tick apart need no exception - by then
	 * the cursor has moved on and the second target IS the active one.
	 */
	private void registerSharpHit(World world, int xIdx, int yIdx, Player shooter, boolean wasDeferred) {
		if(Goldor.INSTANCE.isPhaseInactive()) return;
		GoldorSection s4 = Goldor.INSTANCE.getSection(3);
		if(s4 == null || s4.device.isActivated()) return;
		boolean sequential = damage.Difficulty.deathsEnabled();
		// The plate is a per-hit requirement in the old device and a one-off start in the sequential one.
		if(sequential ? sharpCursor < 0 : !isPlayerOnPlate()) return;
		if(sharpHits[xIdx][yIdx]) return;

		if(sequential) {
			if(!registerSequentialHit(world, xIdx, yIdx)) return; // not the active target: nothing completed
		} else {
			sharpHits[xIdx][yIdx] = true;
			sharpHitCount++;
			setTargetBlock(world, xIdx, yIdx, TARGET_HIT);
		}
		if(sharpHitCount >= 9) {
			if(shooter == null || cannotSolve(shooter)) {
				shooter = null;
				for(Player pl : Bukkit.getOnlinePlayers()) { if(!cannotSolve(pl)) { shooter = pl; break; } }
			}
			s4.device.markActivated();
			if(shooter != null) Goldor.INSTANCE.onActivation(shooter, s4, "device", wasDeferred);
			resetSharpShooter(world);
		}
	}

	/**
	 * Take one arrow in the sequential device.
	 * <p>
	 * Every hit leaves a {@link #sharpHitTick} stamp, and then <b>only a hit on the ACTIVE target does anything</b>.
	 * When one does, the emerald steps forward over every target stamped with THIS SAME TICK - which is the whole of
	 * the same-tick exception, and the reason it is expressed as "stamped now" rather than "struck at some point".
	 * <p>
	 * Written this way so the order arrows are processed in within a tick cannot matter.  Emerald first: it advances,
	 * finds the next target not yet stamped, stops - and the second arrow then lands on what is now the active
	 * target and advances it.  Next target first: its stamp is set but nothing completes, and the emerald's arrow
	 * then advances over both.  Either way two arrows complete two targets.  <b>Do not turn the stamp into a plain
	 * boolean</b>: that is what let the whole wall be picked off out of order.
	 *
	 * @return true if at least one target was completed, i.e. whether the caller should check for the ninth.
	 */
	private boolean registerSequentialHit(World world, int xIdx, int yIdx) {
		int now = Utils.serverTick();
		sharpHitTick[xIdx][yIdx] = now;
		if(yIdx * 3 + xIdx != sharpCursor) return false;
		while(sharpCursor < 9 && sharpHitTick[sharpCursor % 3][sharpCursor / 3] == now) {
			sharpHits[sharpCursor % 3][sharpCursor / 3] = true;
			sharpHitCount++;
			sharpCursor++;
		}
		renderSharpTargets(world);
		return true;
	}

	/**
	 * Redraw all nine targets from state: struck ones red, the emerald on the active one, the rest blue.
	 * <p>
	 * Drawn from state rather than patched per event, so a skipped target can never be left showing the wrong
	 * colour - which is exactly what a per-hit {@code setTargetBlock} would do to a target the emerald jumped over.
	 */
	private void renderSharpTargets(World world) {
		for(int xIdx = 0; xIdx < 3; xIdx++) {
			for(int yIdx = 0; yIdx < 3; yIdx++) {
				Material mat = sharpHits[xIdx][yIdx] ? TARGET_HIT
						: (yIdx * 3 + xIdx == sharpCursor ? TARGET_ACTIVE : TARGET_RESTING);
				setTargetBlock(world, xIdx, yIdx, mat);
			}
		}
	}

	private boolean isPlayerOnPlate() {
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(cannotSolve(p)) continue; // a spectator hovering over the plate must not hold it down
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
		for(int i = 0; i < 3; i++) for(int j = 0; j < 3; j++) {
			sharpHits[i][j] = false;
			sharpHitTick[i][j] = Integer.MIN_VALUE;
		}
		sharpHitCount = 0;
		sharpCursor = -1; // back to "not started": the plate has to begin it again
		sharpWorld = null;
		sharpPlateEmptySince = -1;
	}

	/** Reset the Simon Says global click counter. Invoked on server reset ({@link instructions.Server#serverSetup})
	 *  so a new run never inherits clicks from a previous (possibly aborted) run. */
	public void resetSimon() {
		simonClicks = 0;
	}

	// Sharp Shooter target block materials: blue = resting/solved, red = arrow-hit, and in ultra-realistic an
	// emerald block marks the ONE target currently being asked for.
	private static final Material TARGET_RESTING = Material.BLUE_TERRACOTTA;
	private static final Material TARGET_HIT = Material.RED_TERRACOTTA;
	private static final Material TARGET_ACTIVE = Material.EMERALD_BLOCK;

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
	// Phase-independent (isInS3FrameRegion) so the frames are unbreakable in EVERY phase.  This also cancels the
	// PHYSICS cause, which is what fires when the frame's support block is broken out from behind it, so the frame
	// stays put even with its support gone.
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onHangingBreak(HangingBreakEvent e) {
		if(!(e.getEntity() instanceof ItemFrame frame) || !Goldor.INSTANCE.isInS3FrameRegion(frame)) return;
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
