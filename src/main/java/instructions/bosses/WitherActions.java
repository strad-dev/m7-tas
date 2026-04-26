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

	/**
	 * Makes a Wither deterministically chase a target using vanilla-equivalent
	 * aiStep chase math, independent of its actual HP / isPowered() state.
	 * Also enables noPhysics so the Wither phases through walls while chasing.
	 *
	 * @param wither       The Wither. Must have setAI(false); the RNG-bearing goals and
	 *                     customServerAiStep path are skipped under noAi.
	 * @param target       LivingEntity to chase.
	 * @param stopDistance Horizontal distance (blocks) at which the wither stops chasing.
	 * @param yOffset      Vertical offset above the target the wither hovers at.
	 */
	public static void setWitherAggro(Wither wither, LivingEntity target, double stopDistance, double yOffset) {
		final double stopDistSq = stopDistance * stopDistance;
		clearWitherAggro(wither);

		net.minecraft.world.entity.boss.wither.WitherBoss w = ((CraftWither) wither).getHandle();
		net.minecraft.world.entity.LivingEntity t = ((CraftLivingEntity) target).getHandle();
		w.noPhysics = true;

		BukkitTask task = new BukkitRunnable() {
			// Own velocity state — vanilla aiStep mutates deltaMovement between our ticks
			// (its multiply(1,0.6,1) y-damp and the !isEffectiveAi 0.98 scale still run even
			// with setAI(false)), so we can't rely on deltaMovement to carry state for us.
			double vx = 0, vy = 0, vz = 0;

			@Override
			public void run() {
				if(w.isRemoved() || t.isRemoved() || !t.isAlive()) {
					witherAggroTasks.remove(wither.getUniqueId());
					w.noPhysics = false;
					cancel();
					return;
				}

				// Soft proportional vertical control — desired vy is proportional to the
				// Y error, capped in [-0.2, 0.5]. Replaces the vanilla below/above toggle
				// which was flipping every tick near the target Y and causing the bounce.
				double targetY = t.getY() + yOffset;
				double desiredVy = Math.max(-0.2, Math.min(0.5, (targetY - w.getY()) * 0.3));
				vy += (desiredVy - vy) * 0.4;

				// Horizontal chase (mirrors Wither.aiStep:173-177) — only when > stopDistance blocks away.
				// Within stopDistance, zero horizontal velocity BEFORE move() so the wither hard-stops
				// on this tick rather than coasting one extra tick of leftover momentum.
				double dx = t.getX() - w.getX();
				double dz = t.getZ() - w.getZ();
				double horizSq = dx * dx + dz * dz;
				boolean chasing = horizSq > stopDistSq;
				if(chasing) {
					double len = Math.sqrt(horizSq);
					double dirx = dx / len;
					double dirz = dz / len;
					vx += dirx * 0.3 - vx * 0.6;
					vz += dirz * 0.3 - vz * 0.6;
				} else {
					vx = 0;
					vz = 0;
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

				// Vanilla in-air friction for next tick (only matters while still chasing).
				if(chasing) {
					vx *= 0.91;
					vz *= 0.91;
				}
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
