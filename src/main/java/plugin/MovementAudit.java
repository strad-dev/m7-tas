package plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-tick airborne movement printer — a SUPER-verbose dev tool that traces a player's trajectory tick by tick
 * after a launch (spring boots, lava jump, bonzo-staff / jerry-chine knockback, a scripted jump) for speedrun
 * physics analysis. Inert unless verbose mode is SUPER. Real-player driven off its own 1-tick task (the original
 * was driven by the removed fake-player ticker; that NMS version lives in git history on {@code main}).
 */
public class MovementAudit {
	// Hard cap so a trace that never cleanly lands (velocity stuck, weird geometry) can't print forever.
	private static final int MAX_TRACE_TICKS = 200;

	private static final class State {
		final String source;
		int ticks;
		boolean leftGround;
		double lastX, lastY, lastZ;
		double peakY;
		State(String source, Location at) {
			this.source = source;
			this.lastX = at.getX();
			this.lastY = at.getY();
			this.lastZ = at.getZ();
			this.peakY = at.getY();
		}
	}

	private static final Map<UUID, State> audits = new ConcurrentHashMap<>();
	private static BukkitTask task;

	public static boolean hasAirborneAudit(UUID id) {
		return audits.containsKey(id);
	}

	/** Begin tracing {@code p}'s airborne trajectory (SUPER verbose only — a no-op otherwise). Prints a START line
	 *  and drives a per-tick trace until the player lands or the audit is cancelled. */
	public static void startAirborneAudit(Player p, String source) {
		if(!Utils.isSuperVerbose()) return;
		Location at = p.getLocation();
		audits.put(p.getUniqueId(), new State(source, at));
		Utils.debug(Utils.DebugType.SERVER, p.getName() + " [" + source + "] airborne trace START at Y="
				+ Utils.round(at.getY(), 5) + " vel=" + fmtVec(p.getVelocity()));
		ensureTask();
	}

	public static void cancelAirborneAudit(UUID id) {
		audits.remove(id);
		if(audits.isEmpty()) stopTask();
	}

	public static void cancelAll() {
		audits.clear();
		stopTask();
	}

	private static void ensureTask() {
		if(task != null && !task.isCancelled()) return;
		task = Bukkit.getScheduler().runTaskTimer(M7tas.getInstance(), MovementAudit::tick, 1L, 1L);
	}

	private static void stopTask() {
		if(task != null) {
			task.cancel();
			task = null;
		}
	}

	private static void tick() {
		if(audits.isEmpty()) { stopTask(); return; }
		// Drop the whole trace if the operator left SUPER verbose mid-flight.
		if(!Utils.isSuperVerbose()) { audits.clear(); stopTask(); return; }
		for(Iterator<Map.Entry<UUID, State>> it = audits.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, State> entry = it.next();
			Player p = Bukkit.getPlayer(entry.getKey());
			if(p == null || !p.isOnline()) { it.remove(); continue; }
			State st = entry.getValue();
			st.ticks++;
			Location cur = p.getLocation();
			double dx = cur.getX() - st.lastX, dy = cur.getY() - st.lastY, dz = cur.getZ() - st.lastZ;
			st.lastX = cur.getX();
			st.lastY = cur.getY();
			st.lastZ = cur.getZ();
			if(cur.getY() > st.peakY) st.peakY = cur.getY();
			boolean onGround = ((org.bukkit.craftbukkit.entity.CraftPlayer) p).getHandle().onGround(); // server-side flag; Player#isOnGround() is deprecated
			if(!onGround) st.leftGround = true;
			Utils.debug(Utils.DebugType.SERVER, p.getName() + " [" + st.source + "] t+" + st.ticks
					+ " Y=" + Utils.round(cur.getY(), 5)
					+ " dY=" + Utils.round(dy, 5)
					+ " dXZ=" + Utils.round(Math.hypot(dx, dz), 5)
					+ " vel=" + fmtVec(p.getVelocity())
					+ (onGround ? " [GROUND]" : ""));
			// End once the player has actually left the ground and come back down (so the launch tick, which can
			// still read grounded, doesn't end it immediately), or if the trace overruns the safety cap.
			if((onGround && st.leftGround) || st.ticks >= MAX_TRACE_TICKS) {
				Utils.debug(Utils.DebugType.SERVER, p.getName() + " [" + st.source + "] airborne trace END after "
						+ st.ticks + "t, peak Y=" + Utils.round(st.peakY, 5));
				it.remove();
			}
		}
		if(audits.isEmpty()) stopTask();
	}

	private static String fmtVec(Vector v) {
		return Utils.round(v.getX(), 4) + "," + Utils.round(v.getY(), 4) + "," + Utils.round(v.getZ(), 4);
	}
}
