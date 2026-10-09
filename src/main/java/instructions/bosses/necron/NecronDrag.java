package instructions.bosses.necron;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import plugin.BossScheduler;
import plugin.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Goldor -> Necron drag: 20t after Necron spawns from Goldor every live player is sat on an invisible, invulnerable pig that
 * carries them toward the goal, jittered per pig by up to {@value #JITTER_XZ} on X/Z and {@value #JITTER_Y} on Y;
 * 60t after, they are dropped. They can't dismount in between.
 * Horizontal: at most {@value #MAX_HORIZONTAL_STEP} block per tick. Vertical: a fixed step per pig, set at mount so
 * the full height change takes {@value #VERTICAL_TICKS}t.
 */
public final class NecronDrag implements Listener {
	private NecronDrag() {}

	public static final NecronDrag LISTENER = new NecronDrag();

	public static final String PIG_TAG = "NecronDragPig";

	private static final int MOUNT_TICK = 20;
	private static final int RELEASE_TICK = 60;
	private static final double GOAL_X = 54.5, GOAL_Y = 67, GOAL_Z = 114.5;
	private static final double MAX_HORIZONTAL_STEP = 1.25;
	private static final int VERTICAL_TICKS = 60;
	private static final double JITTER_XZ = 5, JITTER_Y = 2;

	/** One pig's destination and its fixed vertical step per tick. */
	private record Ride(double x, double y, double z, double yStep) {}

	private static final Map<Pig, Ride> rides = new HashMap<>();
	private static Runnable mover;
	// Our own dismount, not the player's: the listener lets it through.
	private static boolean releasing;

	/** From {@code Goldor.chainNext}, the tick Necron spawns. Never on a direct Necron practice. */
	public static void start(World world) {
		Utils.scheduleTask(() -> mount(world), MOUNT_TICK);
		Utils.scheduleTask(NecronDrag::release, RELEASE_TICK);
	}

	private static void mount(World world) {
		for(Player p : world.getPlayers()) {
			if(Utils.isSpectator(p) || p.isDead()) continue;
			if(p.isInsideVehicle()) p.leaveVehicle();
			Location at = p.getLocation();
			Pig pig = world.spawn(at, Pig.class, pg -> {
				pg.setAI(false);
				pg.setGravity(false);
				pg.setInvulnerable(true);
				pg.setInvisible(true);
				pg.setSilent(true);
				pg.setCollidable(false);
				pg.setAdult();
				pg.addScoreboardTag(PIG_TAG);
			});
			// AllMobsHaveNames names every spawn.
			pig.customName(null);
			pig.setCustomNameVisible(false);
			pig.addPassenger(p);
			ThreadLocalRandom rng = ThreadLocalRandom.current();
			double gy = GOAL_Y + rng.nextDouble(-JITTER_Y, JITTER_Y);
			rides.put(pig, new Ride(GOAL_X + rng.nextDouble(-JITTER_XZ, JITTER_XZ), gy,
					GOAL_Z + rng.nextDouble(-JITTER_XZ, JITTER_XZ), (gy - at.getY()) / VERTICAL_TICKS));
		}
		if(rides.isEmpty()) return;
		mover = NecronDrag::step;
		BossScheduler.addTicker(mover);
	}

	private static void step() {
		for(Map.Entry<Pig, Ride> r : rides.entrySet()) {
			Pig pig = r.getKey();
			Ride ride = r.getValue();
			if(!pig.isValid()) continue;
			Location cur = pig.getLocation();
			double dx = ride.x() - cur.getX(), dz = ride.z() - cur.getZ();
			double horizontal = Math.sqrt(dx * dx + dz * dz);
			if(horizontal > MAX_HORIZONTAL_STEP) {
				dx *= MAX_HORIZONTAL_STEP / horizontal;
				dz *= MAX_HORIZONTAL_STEP / horizontal;
			}
			double dy = ride.y() - cur.getY();
			if(Math.abs(dy) > Math.abs(ride.yStep())) dy = ride.yStep();
			pig.teleport(cur.add(dx, dy, dz));
		}
	}

	/** Drops everyone and removes the pigs. At {@link #RELEASE_TICK} and from {@code Necron.resetState}. */
	static void release() {
		if(mover != null) BossScheduler.removeTicker(mover);
		mover = null;
		releasing = true;
		try {
			for(Pig pig : rides.keySet()) {
				pig.eject();
				pig.remove();
			}
		} finally {
			releasing = false;
		}
		rides.clear();
	}

	@EventHandler
	public void onDismount(EntityDismountEvent e) {
		if(releasing || !e.isCancellable()) return;
		Entity vehicle = e.getDismounted();
		if(!(vehicle instanceof Pig pig) || !rides.containsKey(pig)) return;
		if(!(e.getEntity() instanceof Player p) || Utils.isSpectator(p) || p.isDead()) return;
		e.setCancelled(true);
	}
}
