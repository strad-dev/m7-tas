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

	// Real players: a per-tick sampling task (their position is driven by inbound move packets, not by us).
	private static final Map<UUID, BukkitRunnable> airborneAudits = new HashMap<>();
	// Fake players: a lightweight session driven by the fake-player ticker (see auditMove). The ticker hands us
	// the exact per-tick aiStep displacement, so the first launch tick is reported cleanly — no separate task
	// whose timing vs. aiStep would drop sample-0 or merge it into the next tick.
	private static final Map<UUID, AirborneSession> fakeAirborne = new HashMap<>();
	private static boolean silentCancel = false;

	private static final class AirborneSession {
		final String source;
		final Vec3 startPos;
		int tick = 0;

		AirborneSession(String source, Vec3 startPos) {
			this.source = source;
			this.startPos = startPos;
		}
	}

	public static boolean hasAirborneAudit(UUID id) {
		return airborneAudits.containsKey(id) || fakeAirborne.containsKey(id);
	}

	public static void startAirborneAudit(Player p, String source) {
		if(!Utils.isVerbose()) return;

		UUID id = p.getUniqueId();
		ServerPlayer npc = ((CraftPlayer) p).getHandle();

		// Fake players: drive the audit from the fake ticker's aiStep (auditMove), which is SUPER-only.
		if(FakePlayerManager.getFakePlayers().containsValue(p)) {
			if(!Utils.isSuperVerbose()) return;
			endFakeSession(p, npc, fakeAirborne.get(id), true); // supersede a prior arc → report it as landed
			fakeAirborne.put(id, new AirborneSession(source, npc.position()));
			return;
		}

		BukkitRunnable existing = airborneAudits.get(id);
		if(existing != null) existing.cancel();

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

				// Entering flight mode ends the ballistic arc this audit tracks — stop silently (no "landed").
				if(p.isFlying()) {
					silentCancel = true;
					try {
						cancel();
					} finally {
						silentCancel = false;
					}
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

				// No tick>0 guard: sample-0 is the launch tick's real displacement (the full impulse), so report it.
				if(Utils.isSuperVerbose()) {
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

	/** Ends a fake player's airborne session, optionally printing the "landed" line. */
	private static void endFakeSession(Player p, ServerPlayer npc, AirborneSession s, boolean printLanded) {
		if(s == null) return;
		if(printLanded && !silentCancel) {
			Vec3 landPos = npc.position();
			double distanceTraveled = landPos.distanceTo(s.startPos);
			Utils.debug(Utils.DebugType.CLIENT, p.getName() + " " + s.source + " landed after " + s.tick + " ticks at " + Utils.round(landPos.x, 2) + " " + Utils.round(landPos.y, 2) + " " + Utils.round(landPos.z, 2) + ", distance traveled " + Utils.round(distanceTraveled, 2) + " blocks");
		}
		fakeAirborne.remove(p.getUniqueId());
	}

	/** Minecraft zeroes a horizontal velocity component once it falls below this (LivingEntity#aiStep),
	 *  so below it there is no residual slide left to report. */
	private static final double RESIDUAL_EPSILON = 0.003;

	public static void auditMove(Player p, ServerPlayer npc, double dx, double dy, double dz) {
		if(!Utils.isSuperVerbose()) return;

		UUID id = p.getUniqueId();

		// Entering flight mode ends the arc / suppresses residual spam; resumes once flight ends.
		if(p.isFlying()) {
			fakeAirborne.remove(id); // drop the airborne session silently — flight isn't a landing
			return;
		}

		// Airborne (launch/jump arc): the dx/dy/dz handed in is this tick's actual post-aiStep displacement,
		// so the FIRST call (s.tick == 0) reports the full launch tick — nothing hidden or merged.
		AirborneSession s = fakeAirborne.get(id);
		if(s != null) {
			if(npc.onGround() && s.tick > 0) {
				endFakeSession(p, npc, s, true);
				return;
			}
			Vec3 pos = npc.position();
			String input = Actions.getActiveInput(id);
			String sprintSneak = input.isEmpty() ? "" : (npc.isSprinting() ? " sprinting" : npc.isShiftKeyDown() ? " sneaking" : "");
			Utils.debug(Utils.DebugType.CLIENT, p.getName() + " " + s.source + "ed " + Utils.round(dx, 4) + " " + Utils.round(dy, 4) + " " + Utils.round(dz, 4) + sprintSneak + " @ " + Utils.round(pos.x, 3) + " " + Utils.round(pos.y, 5) + " " + Utils.round(pos.z, 3));
			s.tick++;
			return;
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
		fakeAirborne.remove(id); // fake session: silent drop

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
		fakeAirborne.clear();

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
