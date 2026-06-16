package instructions.bosses;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftWither;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wither;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import plugin.M7tas;
import plugin.FakePlayerManager;
import plugin.BossScheduler;
import commands.Spectate;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
public class WitherActions {

	// Aggro movers run in BossScheduler's MOVEMENT lane (BossScheduler.addMovementTicker), driven from the
	// fake-player ticker after fake aiStep — so the boss moves at the same point in the tick as the fakes
	// ("where movement normally happens"), not at the start of the tick. The value is the registered mover
	// handle, used to unregister in clearWitherAggro.
	private static final Map<UUID, Runnable> witherAggroTasks = new HashMap<>();
	private static BukkitTask armorTask = null;

	// Server tick on which each wither last had its armor DROPPED (became vulnerable). Read by
	// wasMadeVulnerableThisTick so a Terminator/Last Breath arrow can register on a one-tick window that opened
	// then re-closed within the same tick — see that method and WithersNotImmuneToArrows.
	private static final Map<UUID, Integer> lastVulnerableTick = new HashMap<>();

	// --- Practice mode: bosses chase the real player who last hit (or the closest), not the fake actors. ---
	private static volatile boolean practiceMode = false;
	private static volatile Player practiceLastDamager = null;

	/** Toggle practice mode (set by {@code TAS.runPractice}/{@code TAS.runTAS}). Clears the last-damager on exit. */
	public static void setPracticeMode(boolean on) {
		practiceMode = on;
		if(!on) practiceLastDamager = null;
	}

	public static boolean isPracticeMode() { return practiceMode; }

	/** Record the real player who most recently damaged a boss (used as the practice aggro target). */
	public static void notePracticeDamager(Player p) { practiceLastDamager = p; }

	/** Hard cap on per-tick vertical displacement so a large goalY-wy gap doesn't snap-teleport. */
	private static final double AGGRO_SPEED_VERTICAL_MAX = 0.5;

	/**
	 * Makes a Wither chase a target using a vanilla-style PD-with-friction velocity controller:
	 * each tick the goal is recomputed (closest spot at {@code stopDistance} horizontally from
	 * the target, with {@code yOffset} vertical offset), and the wither's horizontal velocity
	 * is integrated via the same shape as vanilla {@code Wither.aiStep} — {@code v += dir*A - v*0.6}
	 * then a {@code *= 0.91} friction. The acceleration term {@code A} is scaled so the
	 * steady-state per-tick displacement equals {@code maxSpeed} (vanilla's hard-coded
	 * {@code A=0.3} gives ~0.4717 blocks/tick; we use {@code A = maxSpeed * 0.636}). When the
	 * PD step would overshoot the goal, the wither snaps exactly to the goal and the velocity
	 * state is zeroed — no ramp-down.
	 * <br>
	 * Also enables {@code noPhysics} so the wither phases through walls while chasing.
	 *
	 * @param wither       The Wither. Must have setAI(false).
	 * @param target       LivingEntity to chase.
	 * @param stopDistance Horizontal distance (blocks) at which the wither stops chasing.
	 * @param yOffset      Vertical offset above the target the wither hovers at.
	 * @param maxSpeed     Steady-state horizontal displacement per tick along the XZ wither-to-target vector.
	 */
	public static void setWitherAggro(Wither wither, LivingEntity target, double stopDistance, double yOffset, double maxSpeed) {
		clearWitherAggro(wither);

		net.minecraft.world.entity.boss.wither.WitherBoss w = ((CraftWither) wither).getHandle();
		net.minecraft.world.entity.LivingEntity t = target == null ? null : ((CraftLivingEntity) target).getHandle();
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
				// In practice mode chase the real player who last hit the boss (or the closest one) instead of
				// the fixed (fake) target — the fakes are kicked by /practice, so `t` may be a stale/removed
				// reference. Re-resolve each tick.
				net.minecraft.world.entity.LivingEntity active = practiceMode ? resolvePracticeTarget(wither) : t;

				if(w.isRemoved() || active == null || active.isRemoved() || !active.isAlive()) {
					// A momentarily-absent practice target shouldn't end the chase — hold this tick instead of
					// cancelling (only the boss disappearing tears the task down).
					if(practiceMode && !w.isRemoved()) return;
					witherAggroTasks.remove(wither.getUniqueId());
					w.noPhysics = false;
					BossScheduler.removeMovementTicker(handle[0]);
					return;
				}

				double wx = w.getX(), wy = w.getY(), wz = w.getZ();
				double tx = active.getX(), tz = active.getZ();

				// Horizontal target: closest point on the (radius=stopDistance) ring around the
				// player, projected from the wither's current horizontal position. If the wither
				// is already inside the ring, hold position — never back away from the player.
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
				// for overshoot and snap if reached. Velocity state persists across ticks so the
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
						// Would reach or overshoot the goal this tick — snap exactly, no ramp-down.
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
				// which is true for a Mob under setAI(false). Move manually — noPhysics makes
				// this a pure setPos.
				w.move(MoverType.SELF, v);

				// Use vanilla's LivingEntity.lookAt — this is the exact math LookControl
				// uses to aim a mob at a target, so the formula and sign conventions are
				// guaranteed to match rendering. It sets xRot, yRot, yHeadRot in one call.
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

	public static void setWitherArmor(Wither wither, boolean showArmor) {

		// Cancel any existing task
		if(armorTask != null && !armorTask.isCancelled()) {
			armorTask.cancel();
			armorTask = null;
		}

		if(showArmor) {
			// Start the armor maintenance task
			armorTask = new BukkitRunnable() {
				@Override
				public void run() {
					// Reapply invulnerability ticks
					wither.setInvulnerabilityTicks(3);
				}
			}.runTaskTimer(M7tas.getInstance(), 0L, 1L); // Start immediately, repeat every 20 ticks (1 second)
		} else {
			// Remove armor immediately
			wither.setInvulnerabilityTicks(0);
			// Record the tick the window opened so a same-tick re-arm (e.g. a stun whose cap-enrage fires in the
			// same tick's damage handler) still lets a Terminator/Last Breath arrow through this tick.
			lastVulnerableTick.put(wither.getUniqueId(), MinecraftServer.currentTick);
		}
	}

	/**
	 * True if {@code wither} was made vulnerable (armor dropped) on the CURRENT server tick — even if it has since
	 * been re-armored within the same tick.
	 *
	 * <p>The arrow damage gate ({@link listeners.WithersNotImmuneToArrows}) normally reads the live invulnerability
	 * counter, but a Terminator/Last Breath arrow's hit resolves in the entity-physics lane, AFTER the start-of-tick
	 * boss scans (see CLAUDE.md "Boss Tick Ordering"). So a one-tick vulnerability window that opens and re-closes
	 * within a single tick is invisible to that arrow — the counter already reads "shielded" by the time the hit
	 * lands, even though a same-tick mage beam (player lane) would connect. This captures the heartbeat-time intent
	 * so the arrow honors it too.
	 */
	public static boolean wasMadeVulnerableThisTick(Wither wither) {
		return lastVulnerableTick.getOrDefault(wither.getUniqueId(), Integer.MIN_VALUE) == MinecraftServer.currentTick;
	}

	// --- Practice-mode target resolution ---

	/** NMS handle of the practice target (last damager, else closest valid real player), or null if none. */
	private static net.minecraft.world.entity.LivingEntity resolvePracticeTarget(Wither bukkitWither) {
		Player p = pickPracticePlayer(bukkitWither);
		return p == null ? null : ((CraftLivingEntity) p).getHandle();
	}

	private static Player pickPracticePlayer(Wither bukkitWither) {
		World world = bukkitWither.getWorld();
		Player last = practiceLastDamager;
		if(isValidPracticeTarget(last, world)) return last;
		// Fall back to the closest valid real player.
		Player closest = null;
		double best = Double.MAX_VALUE;
		Location bl = bukkitWither.getLocation();
		for(Player pl : Bukkit.getOnlinePlayers()) {
			if(!isValidPracticeTarget(pl, world)) continue;
			double d = pl.getLocation().distanceSquared(bl);
			if(d < best) { best = d; closest = pl; }
		}
		return closest;
	}

	/** A valid practice target is an online, alive, same-world, non-spectating REAL player (not a fake actor). */
	private static boolean isValidPracticeTarget(Player p, World world) {
		return p != null && p.isOnline() && !p.isDead() && p.getWorld() == world
				&& p.getGameMode() != GameMode.SPECTATOR && !Spectate.isSpectating(p)
				&& !FakePlayerManager.getFakePlayers().containsValue(p);
	}
}
