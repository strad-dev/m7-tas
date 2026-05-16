package instructions.bosses;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftWither;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wither;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import plugin.M7tas;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
public class WitherActions {

	private static final Map<UUID, BukkitTask> witherAggroTasks = new HashMap<>();
	private static BukkitTask armorTask = null;

	/** Max horizontal displacement per tick along the XZ wither-to-target vector. */
	private static final double AGGRO_SPEED_HORIZONTAL = 0.6;

	/** Max vertical displacement per tick, applied independently of the horizontal step. */
	private static final double AGGRO_SPEED_VERTICAL = 0.2;

	/**
	 * Makes a Wither chase a target using a per-tick "linear-toward-target" model:
	 * each tick the target point is recomputed (the closest spot at {@code stopDistance}
	 * horizontally from the target's position, with {@code yOffset} vertical offset),
	 * and the wither steps toward it at {@link #AGGRO_SPEED} blocks/tick. If the wither
	 * is already within one step of the target, it snaps exactly to the target.
	 * Also enables {@code noPhysics} so the wither phases through walls while chasing.
	 *
	 * @param wither       The Wither. Must have setAI(false).
	 * @param target       LivingEntity to chase.
	 * @param stopDistance Horizontal distance (blocks) at which the wither stops chasing.
	 * @param yOffset      Vertical offset above the target the wither hovers at.
	 */
	public static void setWitherAggro(Wither wither, LivingEntity target, double stopDistance, double yOffset) {
		clearWitherAggro(wither);

		net.minecraft.world.entity.boss.wither.WitherBoss w = ((CraftWither) wither).getHandle();
		net.minecraft.world.entity.LivingEntity t = ((CraftLivingEntity) target).getHandle();
		w.noPhysics = true;

		BukkitTask task = new BukkitRunnable() {
			@Override
			public void run() {
				if(w.isRemoved() || t.isRemoved() || !t.isAlive()) {
					witherAggroTasks.remove(wither.getUniqueId());
					w.noPhysics = false;
					cancel();
					return;
				}

				double wx = w.getX(), wy = w.getY(), wz = w.getZ();
				double tx = t.getX(), tz = t.getZ();

				// Horizontal target: closest point on the (radius=stopDistance) ring around the
				// player, projected from the wither's current horizontal position. If the wither
				// is exactly above the player, fall back to keeping its current direction.
				double dx = wx - tx;
				double dz = wz - tz;
				double horiz = Math.sqrt(dx * dx + dz * dz);
				double goalX, goalZ;
				if(horiz < 1e-6) {
					goalX = tx + stopDistance;
					goalZ = tz;
				} else {
					double scale = stopDistance / horiz;
					goalX = tx + dx * scale;
					goalZ = tz + dz * scale;
				}
				double goalY = t.getY() + yOffset;

				// Horizontal step: cap at AGGRO_SPEED_HORIZONTAL along the XZ vector to the goal.
				double mx = goalX - wx;
				double mz = goalZ - wz;
				double horizMove = Math.sqrt(mx * mx + mz * mz);
				double vx, vz;
				if(horizMove <= AGGRO_SPEED_HORIZONTAL || horizMove < 1e-9) {
					vx = mx;
					vz = mz;
				} else {
					double k = AGGRO_SPEED_HORIZONTAL / horizMove;
					vx = mx * k;
					vz = mz * k;
				}

				// Vertical step: independent of horizontal — cap at AGGRO_SPEED_VERTICAL along Y only.
				// Decoupling fixes the "diagonal slow-down" where a small vertical error combined
				// with a large horizontal error would make Y barely change tick-to-tick.
				double my = goalY - wy;
				double vy;
				if(Math.abs(my) <= AGGRO_SPEED_VERTICAL) {
					vy = my;
				} else {
					vy = Math.signum(my) * AGGRO_SPEED_VERTICAL;
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
				w.lookAt(EntityAnchorArgument.Anchor.EYES, t.getEyePosition());
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);

		witherAggroTasks.put(wither.getUniqueId(), task);
	}

	/**
	 * Cancels any active aggro task on the given Wither and restores block collision.
	 * Remaining delta movement is left as-is; vanilla aiStep will continue to dampen it.
	 */
	public static void clearWitherAggro(Wither wither) {
		BukkitTask prior = witherAggroTasks.remove(wither.getUniqueId());
		if(prior != null && !prior.isCancelled()) {
			prior.cancel();
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
		}
	}
}
