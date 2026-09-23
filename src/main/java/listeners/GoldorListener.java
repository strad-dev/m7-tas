package listeners;

import instructions.Actions;
import instructions.bosses.goldor.*;
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
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
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

	// Simon Says STAND-IN (classic + Perfect RNG): cumulative GLOBAL click count, 15 activates, no time limit.
	// This is NOT per-player: any players' clicks accumulate toward 15.  Reset on device completion and on
	// serverSetup (see resetSimon).  The REAL device is GoldorSimonSays, realistic mode only; the two live side by
	// side behind one entry point, the same way the Sharp Shooter keeps two devices behind onPlateStep.
	private int simonClicks = 0;
	// Last server tick each player registered a Simon Says click on - the START button and, in realistic mode, the
	// 16 grid buttons.  A single physical right-click can surface as TWO PlayerInteractEvents on the same tick: the
	// off-hand event while sneaking, and/or vanilla re-firing after PlayerPacketInterceptor resets its
	// interact-dedupe.  One click cannot span two ticks, so I collapse any repeat from the same player within the
	// same tick.  That fixes the same-tick double-count without rate-limiting genuine clicks, which land on
	// different ticks.  Stale entries self-expire via the comparison.
	//
	// THE SPAM WINDOW MAKES THIS LOAD-BEARING IN A NEW WAY.  It is no longer only cosmetic double-counting: a
	// duplicate event inside the real device's 10-tick window would push a player over a skip threshold they never
	// earned, and a skip is worth a whole phase.  The flip side is that it caps one player at one click per tick,
	// so a single person tops out at 10 clicks (3 skips) in the window and the 15- and 21-click thresholds are a
	// team's to reach - which is the intended reading of "sum across players".
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
	 * LIVE MODES ONLY: how far along {@link #sharpOrder} the emerald block is, 0..8, or -1 before the plate has
	 * started the device and 9 once it has run off the end.
	 * <p>
	 * <b>An index into the permutation, not a target.</b>  The target it is currently on is
	 * {@code sharpOrder[sharpCursor]}, itself encoded {@code seq = yIdx * 3 + xIdx} over {@code TARGET_XS} /
	 * {@code TARGET_YS}.  It used to BE that encoding, which made the walk the fixed -X-then-Y order; realistic
	 * rolls the order per arming now, so nothing may go back to treating the cursor as a position on the wall -
	 * not even for Perfect RNG, whose order is that fixed walk again but still reached through the permutation.
	 * Each of the nine is the active block at most once either way; it never loops.
	 */
	private int sharpCursor = -1;
	/**
	 * LIVE MODES ONLY: the order the emerald walks the nine targets, as a permutation of the nine
	 * {@code seq = yIdx * 3 + xIdx} encodings, or null while the device is not armed.
	 * <p>
	 * <b>Realistic rolls it fresh every time the plate arms the device</b> ({@link #onPlateStep}), so stepping off
	 * and back on is a new wall rather than the same one again; <b>Perfect RNG always gets
	 * {@link #SEQUENTIAL_SHARP_ORDER}</b>, the fixed walk, because that mode is the one where the dungeon rolls in
	 * your favour and a known wall is what that means here.  Dropped by {@link #resetSharpHits} either way.
	 */
	private int[] sharpOrder;
	/**
	 * LIVE MODES ONLY: the server tick an arrow last struck each target, or {@code Integer.MIN_VALUE} for never.
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

	// Simon Says START button coord - the one button the device actually has in the world today.  The 16 grid
	// buttons the real device needs are NOT here: their coords derive from GoldorSimonSays' one grid table.
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
	 * <p>
	 * Public because {@link GoldorSimonSays} asks it too - the real S1 device credits a short-circuited solve to a
	 * player it picks itself, and the one it picks has to pass the same gate every click does.
	 */
	public static boolean cannotSolve(Player p) {
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
			// Both live modes: the click opens the terminal's puzzle and solving it is what activates.  The pending
			// flag now spans the whole time the GUI is open, which is what makes a terminal one player's at a time.
			// deathsEnabled(), not realPuzzles(): Perfect RNG opens a terminal too, on the short stand-in board -
			// GoldorTerminalGui picks which board - and only classic keeps the one-click terminal.
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

	// =================== Terminal puzzle GUI (live modes only) ===================

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
		if(plugin.Menus.ignoreDoubleClick(e)) return;
		e.setCancelled(true);
		if(!(e.getWhoClicked() instanceof Player p)) return;
		if(cannotSolve(p)) return;
		// Only clicks in the puzzle itself count; a click down in the player's own inventory is just cancelled.
		if(e.getClickedInventory() != e.getView().getTopInventory()) return;
		if(!gui.onClick(p, e.getSlot(), e.getClick())) return;

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
	 * <p>
	 * <b>{@code onClosed()} runs BEFORE that early return</b>, so it covers the abandoned puzzle too.  It is the
	 * GUI's teardown and Melody's mover is a repeating task, so the failure mode is a task still painting panes
	 * into an inventory nobody has open - it would keep going for the rest of the run.  Idempotent, which is why
	 * a solved Melody (which already stopped its own mover) can go through the same call.
	 */
	@EventHandler
	public void onTerminalGuiClose(InventoryCloseEvent e) {
		if(!(e.getView().getTopInventory().getHolder() instanceof GoldorTerminalGui gui)) return;
		gui.onClosed();
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

		// Simon Says buttons (S1 device), right-click only since they're buttons: the start button in every mode,
		// and in realistic the 16 grid buttons of the real device as well.  Defer if the phase hasn't spun up yet
		// so a click in a chained full run counts: players are scheduled on start, but Goldor is only active when
		// Storm dies.
		if(isSimonInput(p, bx, by, bz)) {
			// CANCEL FIRST, before anything that can touch the block.  MiscListener.onStoneButtonInArena also
			// cancels arena button presses, but at NORMAL priority and by re-reading getClickedBlock().getType()
			// from the world - and we are at LOW.  A cell click that completes a phase makes GoldorSimonSays pull
			// all 16 buttons in this very call, so that later guard would read AIR, decide the block is not a
			// button and let the event through; vanilla then presses the button it captured BEFORE the click and
			// writes it straight back into the slot we just cleared.  That is why the clicked button, and only
			// the clicked button, used to survive the teardown.  The device owns this click outright, the same
			// way it owns the S2 lever below.
			e.setCancelled(true);
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
	 * True if (bx,by,bz) is a block the S1 device takes clicks on: the start button, in every mode, and in
	 * realistic the 16 grid buttons too.  Lets {@link #onInteract} consume the click without duplicating the
	 * lookup {@link #tryRegisterSimonClick} does.
	 */
	private static boolean isSimonInput(Player p, int bx, int by, int bz) {
		if(bx == SIMON_BX && by == SIMON_BY && bz == SIMON_BZ) return true;
		return simonCellAt(p, bx, by, bz) >= 0;
	}

	/**
	 * The Simon Says grid cell whose button sits at (bx,by,bz), or -1 for "not one of ours".
	 * <p>
	 * <b>The block has to really BE a button.</b>  The 16 grid buttons exist only while the device is taking an
	 * answer - {@link GoldorSimonSays} puts them up when a playback ends and takes them down when the answer
	 * lands - so this is what makes a click outside an input window a non-event.  Coordinates alone would also
	 * make the "i1" sign at {@code 110 121 93} a Simon Says input, since it sits in one of the 16 slots.
	 */
	private static int simonCellAt(Player p, int bx, int by, int bz) {
		if(!damage.Difficulty.realPuzzles()) return -1;
		int idx = GoldorSimonSays.cellAtButton(bx, by, bz);
		if(idx < 0) return -1;
		return Tag.BUTTONS.isTagged(p.getWorld().getBlockAt(bx, by, bz).getType()) ? idx : -1;
	}

	/**
	 * Register a Simon Says click from {@code p} when (bx,by,bz) is one of the device's buttons - the start button
	 * in every mode, or a grid button in realistic - deduped to once per player per server tick.  A single physical
	 * click can surface twice on the same tick: the off-hand event while sneaking, and/or vanilla re-firing the
	 * interact after {@code PlayerPacketInterceptor} resets its dedupe.  One click cannot span two ticks, so any
	 * same-tick repeat from a player is dropped.
	 *
	 * <p><b>The dedupe is load-bearing now, not just tidy.</b>  The real device's spam window turns start clicks
	 * into skips, and a skip is worth a whole phase, so one physical click surfacing twice would hand a player a
	 * threshold they never earned.  It is what makes one click count once toward a skip.
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
		boolean start = bx == SIMON_BX && by == SIMON_BY && bz == SIMON_BZ;
		final int cellIdx = start ? -1 : simonCellAt(p, bx, by, bz);
		if(!start && cellIdx < 0) return;
		int now = MinecraftServer.currentTick;
		if(INSTANCE.lastSimonClickTick.getOrDefault(p.getUniqueId(), -1) == now) return;
		INSTANCE.lastSimonClickTick.put(p.getUniqueId(), now);
		if(start) INSTANCE.runWhenPhaseActive(deferred -> INSTANCE.processSimonClick(p, deferred));
		else INSTANCE.runWhenPhaseActive(deferred -> INSTANCE.processSimonCell(p, cellIdx, deferred));
	}

	/**
	 * A start-button click, once the phase is live.  Safe to call from the deferred path, since it re-checks state.
	 * <p>
	 * <b>Two devices behind one entry point</b>, the same shape {@link #registerSharpHit} has always had: realistic
	 * hands the click to the real {@link GoldorSimonSays}, and classic and Perfect RNG keep the 15-click stand-in
	 * below untouched.
	 */
	private void processSimonClick(Player p, boolean wasDeferred) {
		if(cannotSolve(p)) return;
		if(damage.Difficulty.realPuzzles()) {
			GoldorSimonSays.INSTANCE.onStartClick(p, wasDeferred);
			return;
		}
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

	/** A grid-button click, once the phase is live.  Realistic only - the stand-in has no grid.  Re-checks the mode
	*  as well as the spectator gate, since a deferred click can land after either has changed. */
	private void processSimonCell(Player p, int cellIdx, boolean wasDeferred) {
		if(cannotSolve(p)) return;
		if(!damage.Difficulty.realPuzzles()) return;
		GoldorSimonSays.INSTANCE.onCellClick(p, cellIdx, wasDeferred);
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

	// =================== Item frame rotation: ONLY the device's frames may be touched ===================
	// PHASE-INDEPENDENT, matching the punch and break guards below.  This used to gate the whole thing on an active
	// phase and simply return otherwise, so before Goldor spun up - in prep, and between phases - every frame in
	// the wall could be freely rotated, which is not recoverable: nothing re-deals the grid mid-run.
	//
	// Which frames those are is Goldor.isTurnableArrowFrame - STATIC, and answered off the frame's own position and
	// item - precisely so this can answer while the phase is still inactive, before any frame has been scanned for.
	// It is MODE-DEPENDENT: all nine in realistic, and outside it only the stand-in's one unfinished frame, since
	// the other eight already read the answer there.  Every other frame in the wall stays untouchable in every
	// phase and every mode.
	@EventHandler(priority = EventPriority.LOWEST)
	public void onInteractEntity(PlayerInteractEntityEvent e) {
		if(!(e.getRightClicked() instanceof ItemFrame frame)) return;
		Player p = e.getPlayer();
		if(!Goldor.INSTANCE.isInS3FrameRegion(frame)) return; // frames outside S3 behave normally
		if(Goldor.isTurnableArrowFrame(frame)) {
			if(Goldor.INSTANCE.isPhaseInactive()) {
				// Defer a solve that lands before the phase spins up (full-run chain timing) and cancel the vanilla
				// rotation now - processArrowFrame turns the frame itself when it fires, so letting vanilla turn it
				// too made an early click worth two steps.
				runWhenPhaseActive(deferred -> processArrowFrame(frame, p, deferred));
				e.setCancelled(true);
				return;
			}
			// processArrowFrame rotates explicitly; cancel so vanilla doesn't ALSO rotate it (double-turn)
			// when the held item happens to be exempt from CustomItems' interaction cancel.
			if(processArrowFrame(frame, p, false)) e.setCancelled(true);
			return;
		}
		if(p.getGameMode() == GameMode.CREATIVE) return; // creative bypass
		e.setCancelled(true);
	}

	/**
	 * Turn one Arrow Align frame and judge the device.
	 * <p>
	 * <b>Returns true whenever the click was taken</b>, not only when it solved, because the handler owns the turn:
	 * CustomItems cancels this interaction when the player holds a non-exempt custom item, which skips vanilla's
	 * rotation, so the one-step turn is done here - and the caller must then cancel the event so a click vanilla
	 * DID reach isn't worth two steps.
	 * <p>
	 * In realistic all nine frames have to read ordinal 1 at once; in classic and Perfect RNG only the bottom-left
	 * frame can get here at all ({@code Goldor.isTurnableArrowFrame}) and turning it activates, which is the
	 * one-frame stand-in the device has always had in those modes.  Safe to call from the deferred path, since it
	 * re-checks all state, the mode included.
	 */
	private boolean processArrowFrame(ItemFrame frame, Player p, boolean wasDeferred) {
		if(cannotSolve(p)) return false; // guarded here, not in onInteractEntity, since frame PROTECTION still applies to spectators
		if(Goldor.INSTANCE.isPhaseInactive()) return false;
		if(!Goldor.isTurnableArrowFrame(frame)) return false;
		GoldorSection s3 = Goldor.INSTANCE.getSection(2);
		if(s3 == null || s3.device.isActivated()) return false;
		frame.setRotation(frame.getRotation().rotateClockwise());
		// Judged AFTER the turn, off the world, so the frame this click moved counts toward the answer.
		if(damage.Difficulty.realPuzzles() && !Goldor.arrowFramesAligned(frame.getWorld())) return true;
		s3.device.markActivated();
		Goldor.INSTANCE.onActivation(p, s3, "device", wasDeferred);
		return true;
	}

	/**
	 * The same rule on {@code PlayerInteractAtEntityEvent}, which has its own handler list and so is NOT delivered
	 * to the handler above.  Phase-independent for the same reason; the solve itself rides the other event.
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onInteractAtFrame(PlayerInteractAtEntityEvent e) {
		if(!(e.getRightClicked() instanceof ItemFrame frame)) return;
		if(!Goldor.INSTANCE.isInS3FrameRegion(frame)) return;
		if(Goldor.isTurnableArrowFrame(frame)) return;
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

	// =================== Sharp Shooter: the plate starts it (live modes only) ===================

	/**
	 * Stepping on the gold pressure plate BEGINS the S4 device: the emerald block appears on the first target of
	 * the order and the player shoots their way along it.  <b>Both live modes</b> - classic keeps the original
	 * device, where the nine targets are hit in any order and the plate has to be held for each hit.
	 * <p>
	 * <b>Which order is the mode's whole difference here.</b>  Realistic rolls a fresh permutation per arming;
	 * Perfect RNG gets {@link #SEQUENTIAL_SHARP_ORDER}, the fixed -X-then-Y walk this device had when it was
	 * ultra-realistic mode's, so a player can learn the wall and shoot it blind.
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

		// The order is decided HERE, per arming, so stepping off and back on deals a new wall rather than the same
		// one again.  resetSharpHits drops it, and until then sharpOrder is what the cursor indexes.  Perfect RNG's
		// copy is defensive: nothing mutates it today, and one shared array would be a trap for whatever does.
		sharpOrder = damage.Difficulty.realPuzzles()
				? randomSharpOrder()
				: SEQUENTIAL_SHARP_ORDER.clone();
		sharpCursor = 0;
		sharpWorld = b.getWorld();
		renderSharpTargets(sharpWorld);
	}

	/**
	 * Perfect RNG's fixed walk: the nine {@code seq = yIdx * 3 + xIdx} encodings in their own order, which reads
	 * -X across a row and then -Y down to the next one, from (68, 130) to (64, 126).
	 * <p>
	 * It is written out rather than generated because it is a piece of the mode's content - the wall a player
	 * learns - and not an identity permutation that happens to be lying around.
	 */
	private static final int[] SEQUENTIAL_SHARP_ORDER = {0, 1, 2, 3, 4, 5, 6, 7, 8};

	/** A fresh permutation of the nine {@code seq = yIdx * 3 + xIdx} target encodings (Fisher-Yates). */
	private static int[] randomSharpOrder() {
		int[] order = new int[9];
		for(int i = 0; i < order.length; i++) order[i] = i;
		java.util.concurrent.ThreadLocalRandom rng = java.util.concurrent.ThreadLocalRandom.current();
		for(int i = order.length - 1; i > 0; i--) {
			int j = rng.nextInt(i + 1);
			int tmp = order[i];
			order[i] = order[j];
			order[j] = tmp;
		}
		return order;
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
	 * <b>Two devices behind one hit.</b>  Classic keeps the original: any order, checked against the plate per hit.
	 * Both live modes are sequential - {@link #onPlateStep} begins it, settles the order and an emerald block marks
	 * the target to shoot - and the plate is watched per tick instead, so stepping off resets the whole device.
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
	 * <p>
	 * <b>"Next" means next in {@link #sharpOrder}, not next on the wall.</b>  The cursor indexes the permutation, so
	 * both the active-target test and the walk read {@code sharpOrder[sharpCursor]} and decode it to a target; the
	 * stamps stay on the wall's own (x, y) grid, which is what the arrow knows. Nothing else about the mechanism
	 * changes - the emerald can now jump anywhere on the wall, and that is exactly why the stamp still has to be a
	 * tick and not a boolean.
	 *
	 * @return true if at least one target was completed, i.e. whether the caller should check for the ninth.
	 */
	private boolean registerSequentialHit(World world, int xIdx, int yIdx) {
		int now = Utils.serverTick();
		sharpHitTick[xIdx][yIdx] = now;
		if(sharpOrder == null || sharpCursor < 0 || sharpCursor >= sharpOrder.length) return false;
		if(sharpOrder[sharpCursor] != yIdx * 3 + xIdx) return false;
		while(sharpCursor < sharpOrder.length
				&& sharpHitTick[sharpOrder[sharpCursor] % 3][sharpOrder[sharpCursor] / 3] == now) {
			int seq = sharpOrder[sharpCursor];
			sharpHits[seq % 3][seq / 3] = true;
			sharpHitCount++;
			sharpCursor++;
		}
		renderSharpTargets(world);
		return true;
	}

	/** True if (xIdx, yIdx) is the target the emerald is currently on, i.e. the one the cursor points at through
	*  {@link #sharpOrder}.  False whenever the device is not armed, so the wall reads all blue. */
	private boolean isActiveSharpTarget(int xIdx, int yIdx) {
		if(sharpOrder == null || sharpCursor < 0 || sharpCursor >= sharpOrder.length) return false;
		return sharpOrder[sharpCursor] == yIdx * 3 + xIdx;
	}

	/**
	 * Redraw all nine targets from state: struck ones red, the emerald on the active one, the rest blue.
	 * <p>
	 * Drawn from state rather than patched per event, so a skipped target can never be left showing the wrong
	 * colour - which is exactly what a per-hit {@code setTargetBlock} would do to a target the emerald jumped over.
	 * Under realistic's random order the emerald's jumps are bigger, not different in kind, so this is still the
	 * answer.
	 */
	private void renderSharpTargets(World world) {
		for(int xIdx = 0; xIdx < 3; xIdx++) {
			for(int yIdx = 0; yIdx < 3; yIdx++) {
				Material mat = sharpHits[xIdx][yIdx] ? TARGET_HIT
						: (isActiveSharpTarget(xIdx, yIdx) ? TARGET_ACTIVE : TARGET_RESTING);
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
		sharpOrder = null; // and the next arming rolls its own order
		sharpWorld = null;
		sharpPlateEmptySince = -1;
	}

	/** Reset BOTH S1 devices: the stand-in's global click counter and the real one's run (which also puts any lit
	 *  sea lantern back to obsidian).  Invoked on server reset ({@link instructions.Server#serverSetup}) so a new
	 *  run never inherits clicks, a half-played sequence or a flash in flight from a previous one. */
	public void resetSimon() {
		simonClicks = 0;
		GoldorSimonSays.INSTANCE.cleanup();
	}

	// Sharp Shooter target block materials: blue = resting/solved, red = arrow-hit, and in the live modes an
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
		unpowerPlate(world);
	}

	/**
	 * Force the S4 plate at {@code 63 127 35} back to power 0.
	 * <p>
	 * <b>A plate left powered bricks the whole device, silently.</b>  Vanilla only presses a plate from
	 * {@code BasePressurePlateBlock.entityInside}, and that reads
	 * {@code if (getSignalForState(state) == 0) checkPressed(...)} - so a plate that is ALREADY powered never calls
	 * {@code checkPressed}, never fires {@code PlayerInteractEvent} with {@code Action.PHYSICAL}, and
	 * {@link #onPlateStep} is never reached.  Standing on it does nothing at all, with no error and nothing in the
	 * log, and in either live mode that means S4 can never be started and the phase can never be completed.
	 * <p>
	 * It gets stuck because the release is a SCHEDULED BLOCK TICK: a plate presses on contact and un-presses from a
	 * tick it queues for itself. Anything that writes the block without that tick pending - a teardown mid-press, a
	 * {@code clone}/{@code fill} out of a region that was captured while somebody was standing on it - leaves the
	 * powered state with nothing scheduled to clear it, and it stays that way forever.
	 * <p>
	 * So it is forced back on both edges of a run: here (reached from {@code Server.serverSetup}, i.e. before the
	 * run) and from {@code TAS.endPractice} (after it).  Written with {@code applyPhysics = false} like every other
	 * block write in this plugin - nothing is wired to this plate, it is a puzzle prop.  Harmless if a player is
	 * standing on it: the signal reads 0 again, so the next {@code entityInside} simply presses it back.
	 */
	public static void unpowerPlate(World world) {
		if(world == null) return;
		Block b = world.getBlockAt(PLATE_X, PLATE_Y, PLATE_Z);
		org.bukkit.block.data.BlockData data = b.getBlockData();
		// Gold is a LIGHT-weighted plate, so its state is an analogue power 0-15 rather than a boolean. The
		// Powerable branch is there so swapping the plate's material can never quietly un-fix this.
		if(data instanceof org.bukkit.block.data.AnaloguePowerable ap) {
			if(ap.getPower() == 0) return;
			ap.setPower(0);
			b.setBlockData(ap, false);
		} else if(data instanceof org.bukkit.block.data.Powerable pw) {
			if(!pw.isPowered()) return;
			pw.setPowered(false);
			b.setBlockData(pw, false);
		}
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
