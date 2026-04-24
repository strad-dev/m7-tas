package instructions.bosses;

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

	public enum WitherAggroHeight { LOW, HIGH }

	private static final Map<UUID, BukkitTask> witherAggroTasks = new HashMap<>();
	private static BukkitTask armorTask = null;

	/**
	 * Makes a Wither deterministically chase a target using vanilla-equivalent
	 * aiStep chase math, independent of its actual HP / isPowered() state.
	 * Also enables noPhysics so the Wither phases through walls while chasing.
	 *
	 * @param wither The Wither. Must have setAI(false); the RNG-bearing goals and
	 *               customServerAiStep path are skipped under noAi.
	 * @param target LivingEntity to chase.
	 * @param height HIGH = hover ~5 blocks above target (Storm-style, !isPowered vanilla),
	 *               LOW  = hover at target's Y (Maxor-style, isPowered vanilla).
	 */
	public static void setWitherAggro(Wither wither, LivingEntity target, WitherAggroHeight height) {
		clearWitherAggro(wither);

		net.minecraft.world.entity.boss.wither.WitherBoss w = ((CraftWither) wither).getHandle();
		net.minecraft.world.entity.LivingEntity t = ((CraftLivingEntity) target).getHandle();
		double yOffset = (height == WitherAggroHeight.HIGH) ? 5.0 : 0.0;
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

				Vec3 v = w.getDeltaMovement();
				double dy = v.y;
				if(w.getY() < t.getY() + yOffset) {
					dy = Math.max(0.0, dy);
					dy += 0.3 - dy * 0.6;
				}
				v = new Vec3(v.x, dy, v.z);

				Vec3 flat = new Vec3(t.getX() - w.getX(), 0.0, t.getZ() - w.getZ());
				if(flat.horizontalDistanceSqr() > 9.0) {
					Vec3 dir = flat.normalize();
					v = v.add(dir.x * 0.3 - v.x * 0.6, 0.0, dir.z * 0.3 - v.z * 0.6);
				}

				w.setDeltaMovement(v);
				if(v.horizontalDistanceSqr() > 0.05) {
					w.setYRot((float) (net.minecraft.util.Mth.atan2(v.z, v.x) * (180.0 / Math.PI) - 90.0));
				}
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);

		witherAggroTasks.put(wither.getUniqueId(), task);
	}

	/**
	 * Cancels any active aggro task on the given Wither. The Wither will coast
	 * to a stop via vanilla travel() friction. Also restores block collision.
	 */
	public static void clearWitherAggro(Wither wither) {
		BukkitTask prior = witherAggroTasks.remove(wither.getUniqueId());
		if(prior != null && !prior.isCancelled()) {
			prior.cancel();
		}
		((CraftWither) wither).getHandle().noPhysics = false;
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
