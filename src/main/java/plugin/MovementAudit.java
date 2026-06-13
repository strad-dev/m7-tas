package plugin;

import instructions.Actions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MovementAudit {

	private static final Map<UUID, BukkitRunnable> airborneAudits = new HashMap<>();
	private static boolean silentCancel = false;

	public static boolean hasAirborneAudit(UUID id) {
		return airborneAudits.containsKey(id);
	}

	public static void startAirborneAudit(Player p, String source) {
		if(!Utils.isVerbose()) return;

		UUID id = p.getUniqueId();
		BukkitRunnable existing = airborneAudits.get(id);
		if(existing != null) existing.cancel();

		ServerPlayer npc = ((CraftPlayer) p).getHandle();
		Vec3 startPos = npc.position();

		BukkitRunnable runnable = new BukkitRunnable() {
			int tick = 0;
			Vec3 prev = npc.position();

			@Override
			public void run() {
				if(!p.isValid() || npc.isRemoved()) {
					airborneAudits.remove(id);
					cancel();
					return;
				}

				Vec3 pos = npc.position();
				double dx = pos.x - prev.x;
				double dy = pos.y - prev.y;
				double dz = pos.z - prev.z;
				prev = pos;

				String input = Actions.getActiveInput(id);
				String sprintSneak = "";
				if(!input.isEmpty()) {
					sprintSneak = npc.isSprinting() ? " sprinting" : npc.isShiftKeyDown() ? " sneaking" : "";
				}

				if(npc.onGround() && tick > 0) {
					cancel();
					return;
				}

				if(tick > 0 && Utils.isSuperVerbose()) {
					Utils.debug(Utils.DebugType.CLIENT, p.getName() + " " + source + "ed " + Utils.round(dx, 4) + " " + Utils.round(dy, 4) + " " + Utils.round(dz, 4) + sprintSneak + " @ " + Utils.round(pos.x, 3) + " " + Utils.round(pos.y, 5) + " " + Utils.round(pos.z, 3));
				}
				tick++;
			}

			@Override
			public void cancel() {
				if(!silentCancel) {
					net.minecraft.world.phys.Vec3 landPos = npc.position();
					double distanceTraveled = landPos.distanceTo(startPos);
					Utils.debug(Utils.DebugType.CLIENT, p.getName() + " " + source + " landed after " + tick + " ticks at " + Utils.round(landPos.x, 2) + " " + Utils.round(landPos.y, 2) + " " + Utils.round(landPos.z, 2) + ", distance traveled " + Utils.round(distanceTraveled, 2) + " blocks");
				}
				airborneAudits.remove(id);
				super.cancel();
			}
		};
		runnable.runTaskTimer(M7tas.getInstance(), 0L, 1L);
		airborneAudits.put(id, runnable);
	}

	/** Minecraft zeroes a horizontal velocity component once it falls below this (LivingEntity#aiStep),
	 *  so below it there is no residual slide left to report. */
	private static final double RESIDUAL_EPSILON = 0.003;

	public static void auditMove(Player p, ServerPlayer npc, double dx, double dy, double dz) {
		if(!Utils.isSuperVerbose()) return;

		UUID id = p.getUniqueId();

		if(hasAirborneAudit(id)) {
			return; // the airborne audit reports its own per-tick movement
		}

		boolean hasInput = !Actions.getActiveInput(id).isEmpty();
		// With active input, always report. Without input, keep reporting the residual slide that momentum
		// carries after a move()/bonzo jump ends — until friction decays it below Minecraft's zeroing threshold.
		if(!hasInput && Math.abs(dx) < RESIDUAL_EPSILON && Math.abs(dz) < RESIDUAL_EPSILON) return;

		Vec3 pos = npc.position();
		String verb = hasInput ? "moved" : "drifted";
		Utils.debug(Utils.DebugType.CLIENT, p.getName() + " " + verb + " " + Utils.round(dx, 4) + " " + Utils.round(dy, 4) + " " + Utils.round(dz, 4) + (npc.isSprinting() ? " sprinting" : npc.isShiftKeyDown() ? " sneaking" : "") + (npc.onGround() ? " on ground" : "") + " @ " + Utils.round(pos.x, 3) + " " + Utils.round(pos.y, 5) + " " + Utils.round(pos.z, 3));
	}

	/** Cancels a single player's airborne audit without printing a "landed" line — used when the player
	 *  is teleported mid-flight (e.g. a leap), so the audit shouldn't report the teleport as movement. */
	public static void cancelAirborneAudit(UUID id) {
		BukkitRunnable runnable = airborneAudits.get(id);
		if(runnable == null) return;
		silentCancel = true;
		try {
			runnable.cancel(); // the overridden cancel() removes it from airborneAudits
		} finally {
			silentCancel = false;
		}
	}

	/** Cancels every active airborne audit without printing "landed" lines — the players didn't land,
	 *  the run was restarted/reset out from under them. */
	public static void cancelAll() {
		silentCancel = true;
		try {
			for(BukkitRunnable runnable : new java.util.ArrayList<>(airborneAudits.values())) {
				runnable.cancel();
			}
		} finally {
			silentCancel = false;
		}
		airborneAudits.clear();
	}
}
