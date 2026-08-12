package instructions.bosses;

import commands.Spectate;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftWither;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import plugin.BossScheduler;
import plugin.FakePlayerManager;
import plugin.M7tas;

import java.util.*;

@SuppressWarnings("unused")
public class WitherActions {

	// Aggro movers run in BossScheduler's MOVEMENT lane (BossScheduler.addMovementTicker), driven from the
	// fake-player ticker after fake aiStep, so the boss moves at the same point in the tick as the fakes
	// ("where movement normally happens") and not at the start of the tick.  The value is the registered
	// mover handle, used to unregister in clearWitherAggro.
	private static final Map<UUID, Runnable> witherAggroTasks = new HashMap<>();
	private static BukkitTask armorTask = null;

	// Server tick on which each wither last had its armor DROPPED (became vulnerable).  Read by
	// wasMadeVulnerableThisTick so a Terminator/Last Breath arrow can register on a one-tick window that opened
	// then re-closed within the same tick.  See that method and WithersNotImmuneToArrows.
	private static final Map<UUID, Integer> lastVulnerableTick = new HashMap<>();

	// --- Aggro target: bosses chase whoever last damaged them (fake or real).  Among players that hit on the same
	// tick, the alphabetically-first name wins.  The target persists until a later tick's hit. ---
	private static volatile boolean practiceMode = false;
	private static volatile Player lastDamager = null;
	private static volatile int lastDamageTick = Integer.MIN_VALUE;

	/** Toggle practice mode (set by {@code TAS.runPractice}/{@code TAS.runTAS}). Resets the aggro damager. */
	public static void setPracticeMode(boolean on) {
		practiceMode = on;
		lastDamager = null;
		lastDamageTick = Integer.MIN_VALUE;
	}

	public static boolean isPracticeMode() { return practiceMode; }

	/**
	 * Announce that a /m7practice run just finished as a plain Bukkit event ({@link plugin.RunCompleteEvent}).
	 * M7 TAS depends on nothing external.  The event fires into the void when nothing listens, so the plugin
	 * stays fully standalone, and an optional glue plugin may listen to return players to spectator and free a
	 * network slot.  Only fires in practice mode.  Every section calls it as soon as its ending is DECIDED - at
	 * {@code chainNext(false)}, at clear completion, or, for a Wither-King run, with the scoreboard rather than at
	 * the end of the death dialogue that follows it (see {@code WitherKing.deathSequence}).  The event says the run
	 * is OVER, not that the room is quiet: a listener that wants to leave an ending on screen owns that delay
	 * itself, which is exactly what the network plugin does.
	 * <br>
	 * Overload for a run that was actually WON. Storm's all-pillars-gone failure path fires
	 * {@link #signalRunComplete(boolean)} with {@code false} instead, so a listener can tell the two apart, and
	 * the event used to be indistinguishable between a win and that one loss.
	 */
	public static void signalRunComplete() {
		signalRunComplete(true);
	}

	/**
	 * @param success false only for a run that ended in failure (see {@code Storm.endFailedRun}).  A failed run
	 *                still fires the event, because the network plugin needs to free its slot either way, but it
	 *                carries {@code success=false} so it is never recorded to a leaderboard.
	 */
	public static void signalRunComplete(boolean success) {
		if (!practiceMode) return;
		// Prefer a snapshot taken earlier by captureRunResult().  See that method for why the capture instant and
		// the signal instant have to be allowed to differ.
		plugin.RunResult result = (success && pendingResult != null)
				? pendingResult
				: plugin.RunResult.capture(runSection, success);
		pendingResult = null;
		Bukkit.getPluginManager().callEvent(new plugin.RunCompleteEvent(result));
	}

	/** A result snapshotted before the run-complete signal is due to fire. Null when nothing has snapshotted yet. */
	private static volatile plugin.RunResult pendingResult = null;

	/**
	 * Snapshot the run's result NOW, for a {@link #signalRunComplete()} that will come later.
	 * <br>
	 * <b>Why this exists.</b> A result records whoever is still in the run ({@code ClearManager.realPlayers()}), so
	 * the instant it's captured matters.  The Wither King used to hold its signal until the death dialogue had played
	 * out in full, and during those ~9 seconds a player could walk out - capturing then found an empty roster and
	 * silently dropped the entire run from the leaderboards.  The signal has since moved forward to the scoreboard,
	 * so the two now land on the same tick, but the split stays: the capture is pinned to the moment the numbers are
	 * PRINTED, so it keeps agreeing with them no matter where the signal is fired from later.  Bosses that signal
	 * the moment they finish don't need this; they can capture at signal time.
	 */
	public static void captureRunResult() {
		if (!practiceMode) return;
		pendingResult = plugin.RunResult.capture(runSection, true);
	}

	// --- Identity of the current run (/m7practice <section>), for the run-result payload ---
	private static volatile String runSection = "all";
	private static volatile String runId = "";

	/**
	 * Start tracking a new run: records which section /m7practice was invoked with and mints a fresh run id.
	 * Called once by {@code TAS.runPractice}.
	 * <br>
	 * The run id lets a consumer recognise two reports from the SAME run. That matters because a run reports
	 * more than once: the 300-score milestone is signalled the instant it's reached (so it counts even if the
	 * team then resets), and the run-complete payload later repeats that same milestone. A listener keyed on the
	 * run id can drop the duplicate instead of recording the run twice.
	 */
	public static void startRunTracking(String section) {
		runSection = section == null ? "all" : section;
		runId = java.util.UUID.randomUUID().toString();
		pendingResult = null; // never let a previous run's snapshot leak into this one
	}

	/** The section the current run was started with. */
	public static String runSection() { return runSection; }

	/** Unique id for the current run, stable across every report this run makes. */
	public static String runId() { return runId; }

	/**
	 * Announce that the team just hit a score milestone, as {@link plugin.ScoreMilestoneEvent}. Fired the moment
	 * it happens rather than at run end, so the time stands even if the team resets straight afterwards. Same
	 * standalone contract as {@link #signalRunComplete()}: practice-only, and it fires into the void unlistened.
	 */
	public static void signalScoreMilestone(int score) {
		if (!practiceMode) return;
		Bukkit.getPluginManager().callEvent(
				new plugin.ScoreMilestoneEvent(score, plugin.RunResult.capture(runSection, true)));
	}

	/** Whether {@code p} can aggro a boss in the current mode.  During a TAS (non-practice) the withers ignore all
	*  real players, since they are not part of the run, so only fake players are eligible.  In practice mode the
	*  real players ARE the runners and no fakes are spawned, so the opposite holds. */
	private static boolean isAggroEligible(Player p) {
		if(p.getGameMode() == GameMode.SPECTATOR || Spectate.isSpectating(p)) return true; // spectators never aggro
		boolean fake = FakePlayerManager.getFakePlayers().containsValue(p);
		return practiceMode == fake;
	}

	// --- Live section splits (for the Wither-King practice scoreboard) ---
	// Overall tick (Utils.runTick()) recorded at each section's finish. Populated as the boss chain progresses;
	// the WitherKing practice scoreboard reads these to show the real per-section times from a /m7practice run.
	private static final Map<String, Integer> splitEnds = new java.util.LinkedHashMap<>();

	/** Record the overall tick at which the named section finished. */
	public static void recordSplit(String section, int overallTick) { splitEnds.put(section, overallTick); }

	/** Overall tick at which the named section finished, or null if it wasn't run this session. */
	public static Integer getSplitEnd(String section) { return splitEnds.get(section); }

	/** Every recorded split, in the order the sections finished. */
	public static Map<String, Integer> splitEnds() { return new java.util.LinkedHashMap<>(splitEnds); }

	// --- Per-phase durations (for the leaderboards) ---
	// PHASE-RELATIVE tick (Utils.phaseTick(), i.e. the boss's own clock) at the moment the phase ENDED.  That is the
	// tick the death dialogue finishes and the boss chains to the next phase ("<Boss> finished in ..."), NOT the
	// killing blow ("<Boss> killed in ..."), which is 80-200 ticks earlier.  This is distinct from splitEnds above,
	// which is overall-run-relative and is stamped by the NEXT section's start, meaning a standalone
	// "/m7practice maxor" never lands a Maxor split at all.  Each boss stamps its own duration here at the end of its
	// own death sequence, so an individual-phase practice is timed exactly like a phase inside a full run.
	private static final Map<String, Integer> phaseDurations = new java.util.LinkedHashMap<>();

	/** Record the duration (phase-relative ticks) of the named boss phase, stamped when the phase ends. */
	public static void recordPhaseDuration(String phase, int phaseTicks) { phaseDurations.put(phase, phaseTicks); }

	/** Duration (phase-relative ticks) of the named boss phase, or null if it wasn't completed this run. */
	public static Integer getPhaseDuration(String phase) { return phaseDurations.get(phase); }

	/** Every recorded phase duration, in completion order. */
	public static Map<String, Integer> phaseDurations() { return new java.util.LinkedHashMap<>(phaseDurations); }

	/** Clear all recorded splits and phase durations.  Called at the start of every /tas and /m7practice run. */
	public static void clearSplits() {
		splitEnds.clear();
		phaseDurations.clear();
	}

	// --- Game-mode tracking for the practice scoreboard's golden-name anti-cheat ---
	// Players who changed game mode at any point during the current run; their scoreboard name shows white, not gold.
	private static final Set<UUID> gameModeChanged = new HashSet<>();

	/** Clear recorded game-mode changes.  Called at the start of every /tas and /m7practice run. */
	public static void clearGameModeChanges() { gameModeChanged.clear(); }

	/** Record that a player changed game mode during the run (disqualifies their golden name). */
	public static void noteGameModeChange(UUID id) { gameModeChanged.add(id); }

	/** True only if the player has stayed in Adventure Mode for the entire run (never changed game mode). */
	public static boolean stayedAdventure(Player p) {
		return p.getGameMode() == GameMode.ADVENTURE && !gameModeChanged.contains(p.getUniqueId());
	}

	/** Record a player that just damaged a boss, which makes them the aggro target.  Same-tick ties go to the
	*  alphabetically-first name; a later tick's hit always takes over. */
	public static void noteDamager(Player p) {
		if(p == null) return;
		// During a TAS the run is driven entirely by the fake players, so a real player hitting a boss must never
		// steal its aggro.  In practice mode it's the reverse: only real players count.
		if(isAggroEligible(p)) return;
		int now = MinecraftServer.currentTick;
		if(now > lastDamageTick) {
			lastDamager = p;
			lastDamageTick = now;
		} else if(now == lastDamageTick && (lastDamager == null || p.getName().compareTo(lastDamager.getName()) < 0)) {
			lastDamager = p;
		}
	}

	/** Hard cap on per-tick vertical displacement so a large goalY-wy gap doesn't snap-teleport. */
	private static final double AGGRO_SPEED_VERTICAL_MAX = 0.2;

	/**
	 * Makes a Wither chase a target using a vanilla-style PD-with-friction velocity controller:
	 * each tick the goal is recomputed (closest spot at {@code stopDistance} horizontally from
	 * the target, with {@code yOffset} vertical offset), and the wither's horizontal velocity
	 * is integrated via the same shape as vanilla {@code Wither.aiStep}, i.e. {@code v += dir*A - v*0.6}
	 * then a {@code *= 0.91} friction.  The acceleration term {@code A} is scaled so the
	 * steady-state per-tick displacement equals {@code maxSpeed} (vanilla's hard-coded
	 * {@code A=0.3} gives ~0.4717 blocks/tick; I use {@code A = maxSpeed * 0.636}).  When the
	 * PD step would overshoot the goal, the wither snaps exactly to the goal and the velocity
	 * state is zeroed, with no ramp-down.
	 * <br>
	 * Also enables {@code noPhysics} so the wither phases through walls while chasing.
	 *
	 * <p>The chase target is whoever last damaged the boss (see {@link #noteDamager}); the boss holds position until
	 * something hits it. Works identically for fake actors (TAS) and real players (practice).
	 *
	 * @param wither       The Wither. Must have setAI(false).
	 * @param stopDistance Horizontal distance (blocks) at which the wither stops chasing.
	 * @param yOffset      Vertical offset above the target the wither hovers at.
	 * @param maxSpeed     Steady-state horizontal displacement per tick along the XZ wither-to-target vector.
	 */
	public static void setWitherAggro(Wither wither, double stopDistance, double yOffset, double maxSpeed) {
		clearWitherAggro(wither);

		net.minecraft.world.entity.boss.wither.WitherBoss w = ((CraftWither) wither).getHandle();
		w.noPhysics = true;

		// Scale vanilla's A=0.3 so steady-state move-per-tick equals maxSpeed instead of ~0.4717.
		// See class doc above for derivation: move_steady = A * 1.5723 → A = maxSpeed / 1.5723.
		final double A = maxSpeed * 0.636;

		// Self-reference so the ticker can unregister itself from the boss heartbeat on teardown.
		final Runnable[] handle = new Runnable[1];
		Runnable task = new Runnable() {
			// Persistent velocity state across ticks (the PD's "v"). Reset to 0 on every snap-to-goal.
			double vxState = 0, vzState = 0;

			@Override
			public void run() {
				// Chase whoever last beam/melee-damaged the boss (NOT arrows); until someone does, auto-aggro the
				// closest player so the boss actively pursues instead of sitting at spawn. Re-resolved each tick.
				// A damager who went offline/died/changed world OR slipped into spectator falls back to auto-aggro.
				Player damager = lastDamager;
				if(damager == null || !damager.isOnline() || damager.isDead() || damager.getWorld() != wither.getWorld()
						|| damager.getGameMode() == GameMode.SPECTATOR || Spectate.isSpectating(damager)) {
					damager = closestPlayer(wither);
				}
				net.minecraft.world.entity.LivingEntity active = damager == null ? null : ((CraftLivingEntity) damager).getHandle();

				if(w.isRemoved() || active == null || active.isRemoved() || !active.isAlive()) {
					// No valid damager yet, or it momentarily went away, so hold position this tick.  Only the
					// boss itself disappearing tears the chase task down.
					if(!w.isRemoved()) return;
					witherAggroTasks.remove(wither.getUniqueId());
					w.noPhysics = false;
					BossScheduler.removeMovementTicker(handle[0]);
					return;
				}

				double wx = w.getX(), wy = w.getY(), wz = w.getZ();
				double tx = active.getX(), tz = active.getZ();

				// Horizontal target: closest point on the (radius=stopDistance) ring around the
				// player, projected from the wither's current horizontal position.  If the wither
				// is already inside the ring, hold position and never back away from the player.
				double dx = wx - tx;
				double dz = wz - tz;
				double horiz = Math.sqrt(dx * dx + dz * dz);
				double goalX, goalZ;
				if(horiz <= stopDistance) {
					goalX = wx;
					goalZ = wz;
				} else {
					double scale = stopDistance / horiz;
					goalX = tx + dx * scale;
					goalZ = tz + dz * scale;
				}
				double goalY = active.getY() + yOffset;

				// Horizontal step: vanilla-shape PD (vxState += dir*A - vxState*0.6), then check
				// for overshoot and snap if reached.  Velocity state persists across ticks so the
				// wither accelerates smoothly from rest like a vanilla Wither.
				double mx = goalX - wx;
				double mz = goalZ - wz;
				double horizMove = Math.sqrt(mx * mx + mz * mz);
				double vx, vz;
				if(horizMove < 1e-9) {
					vxState = 0;
					vzState = 0;
					vx = 0;
					vz = 0;
				} else {
					double dirx = mx / horizMove;
					double dirz = mz / horizMove;
					vxState += dirx * A - vxState * 0.6;
					vzState += dirz * A - vzState * 0.6;
					double stepLen = Math.sqrt(vxState * vxState + vzState * vzState);
					if(stepLen >= horizMove) {
						// Would reach or overshoot the goal this tick, so snap exactly, with no ramp-down.
						vx = mx;
						vz = mz;
						vxState = 0;
						vzState = 0;
					} else {
						vx = vxState;
						vz = vzState;
					}
				}

				// Vertical step: synced to horizontal arrival time at maxSpeed so a stationary
				// target is reached at the same tick on Y as on XZ, then clamped to
				// {@link #AGGRO_SPEED_VERTICAL_MAX} so a huge goalY-wy gap doesn't snap-teleport.
				double my = goalY - wy;
				double vy;
				if(horizMove < 1e-9) {
					vy = my;
				} else {
					double ticksToArrive = Math.max(1.0, horizMove / maxSpeed);
					vy = my / ticksToArrive;
				}
				if(Math.abs(vy) > AGGRO_SPEED_VERTICAL_MAX) {
					vy = Math.signum(vy) * AGGRO_SPEED_VERTICAL_MAX;
				}

				Vec3 v = new Vec3(vx, vy, vz);
				w.setDeltaMovement(v);
				w.hurtMarked = true;

				// Vanilla LivingEntity.travel() no-ops when !isControlledByLocalInstance(),
				// which is true for a Mob under setAI(false).  Move manually, and noPhysics
				// makes this a pure setPos.
				w.move(MoverType.SELF, v);

				// Use vanilla's LivingEntity.lookAt.  This is the exact math LookControl
				// uses to aim a mob at a target, so the formula and sign conventions are
				// guaranteed to match rendering.  It sets xRot, yRot, yHeadRot in one call.
				w.lookAt(EntityAnchorArgument.Anchor.EYES, active.getEyePosition());

				// Vanilla air-drag for next tick: matches Wither.aiStep's `*= 0.91` after the move.
				// Combined with the per-tick `- vxState * 0.6` PD damping, this gives the same
				// ramp curve shape vanilla uses; the time constant is independent of maxSpeed.
				vxState *= 0.91;
				vzState *= 0.91;
			}
		};

		handle[0] = task;
		BossScheduler.addMovementTicker(task);
		witherAggroTasks.put(wither.getUniqueId(), task);
	}

	/**
	 * Cancels any active aggro task on the given Wither and restores block collision.
	 * Remaining delta movement is left as-is; vanilla aiStep will continue to dampen it.
	 */
	public static void clearWitherAggro(Wither wither) {
		Runnable prior = witherAggroTasks.remove(wither.getUniqueId());
		if(prior != null) {
			BossScheduler.removeMovementTicker(prior);
		}
		net.minecraft.world.entity.boss.wither.WitherBoss w = ((CraftWither) wither).getHandle();
		w.noPhysics = false;
	}

	/** Closest valid player to the boss, the auto-aggro fallback used until someone beam or melee damages it.
	*  Only fakes are considered during a TAS, only real players in practice (see {@link #isAggroEligible}).
	*  Spectators and dead or cross-world players are excluded. */
	private static Player closestPlayer(Wither wither) {
		Player closest = null;
		double best = Double.MAX_VALUE;
		Location loc = wither.getLocation();
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(isAggroEligible(p)) continue;
			if(p.getGameMode() == GameMode.SPECTATOR || Spectate.isSpectating(p)) continue;
			if(p.isDead() || p.getWorld() != wither.getWorld()) continue;
			double d = p.getLocation().distanceSquared(loc);
			if(d < best) { best = d; closest = p; }
		}
		return closest;
	}

	public static void setWitherArmor(Wither wither, boolean showArmor) {

		// Cancel any existing task
		if(armorTask != null && !armorTask.isCancelled()) {
			armorTask.cancel();
			armorTask = null;
		}

		if(showArmor) {
			// Assert the shield THIS tick.  The maintenance task's first run is only next tick, since a 0-tick delay
			// means the next scheduler pass, so without this an enrage mid-tick would leave the boss unshielded for the
			// rest of the tick and let same-tick beams or arrows land after it re-armored, which is over-DPS.
			wither.setInvulnerableTicks(3);
			// Start the armor maintenance task
			armorTask = new BukkitRunnable() {
				@Override
				public void run() {
					// Reapply invulnerability ticks
					wither.setInvulnerableTicks(3);
				}
			}.runTaskTimer(M7tas.getInstance(), 0L, 1L); // Start immediately, repeat every 20 ticks (1 second)
		} else {
			// Remove armor immediately
			wither.setInvulnerableTicks(0);
			// Record the tick the window opened so a same-tick re-arm (e.g. a stun whose cap-enrage fires in the
			// same tick's damage handler) still lets a Terminator/Last Breath arrow through this tick.
			lastVulnerableTick.put(wither.getUniqueId(), MinecraftServer.currentTick);
		}
	}

	/**
	 * True if {@code wither} was made vulnerable (armor dropped) on the CURRENT server tick, even if it has since
	 * been re-armored within the same tick.
	 *
	 * <p>The arrow damage gate ({@link listeners.WithersNotImmuneToArrows}) normally reads the live invulnerability
	 * counter, but a Terminator/Last Breath arrow's hit resolves in the entity-physics lane, AFTER the start-of-tick
	 * boss scans (see CLAUDE.md "Boss Tick Ordering"). So a one-tick vulnerability window that opens and re-closes
	 * within a single tick is invisible to that arrow, because the counter already reads "shielded" by the time the
	 * hit lands, even though a same-tick mage beam in the player lane would connect.  This captures the
	 * heartbeat-time intent so the arrow honors it too.
	 */
	public static boolean wasMadeVulnerableThisTick(Wither wither) {
		return lastVulnerableTick.getOrDefault(wither.getUniqueId(), Integer.MIN_VALUE) == MinecraftServer.currentTick;
	}

}
