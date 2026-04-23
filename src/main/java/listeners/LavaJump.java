package listeners;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import plugin.M7tas;
import plugin.MovementAudit;
import plugin.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LavaJump {
	private static final double LAUNCH_VELOCITY = 3.5D;
	private static final int RELAUNCH_COOLDOWN_TICKS = 10;

	// Goldor boss arena bounds: -8 254 -8 to 134 0 147
	private static final double MIN_X = -8, MAX_X = 134;
	private static final double MIN_Y = 0, MAX_Y = 254;
	private static final double MIN_Z = -8, MAX_Z = 147;

	private static final Map<UUID, Integer> lastLaunchTick = new HashMap<>();
	private static BukkitTask poller;

	public static void start() {
		if(poller != null) return;
		poller = new BukkitRunnable() {
			@Override
			public void run() {
				tick();
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	public static void stop() {
		if(poller != null) {
			poller.cancel();
			poller = null;
		}
		lastLaunchTick.clear();
	}

	private static void tick() {
		for(Player p : Bukkit.getOnlinePlayers()) {
			check(p);
		}
	}

	private static void check(Player p) {
		if(p.getGameMode() == GameMode.SPECTATOR) return;
		Location loc = p.getLocation();
		UUID id = p.getUniqueId();

		boolean inBounds = loc.getX() >= MIN_X && loc.getX() <= MAX_X
				&& loc.getY() >= MIN_Y && loc.getY() <= MAX_Y
				&& loc.getZ() >= MIN_Z && loc.getZ() <= MAX_Z;
		boolean touchingLava = inBounds && loc.getBlock().getType() == Material.LAVA;

		if(!touchingLava) {
			lastLaunchTick.remove(id);
			return;
		}

		int now = MinecraftServer.currentTick;
		Integer last = lastLaunchTick.get(id);
		if(last != null && now - last < RELAUNCH_COOLDOWN_TICKS) return;
		lastLaunchTick.put(id, now);

		Utils.debug(Utils.DebugType.SERVER, p.getName() + (last == null ? " lava contact" : " still-in-lava rebounce") + " detected at Y=" + Utils.round(loc.getY(), 5));
		Utils.playLocalSound(p, Sound.ENTITY_PLAYER_HURT, 1.0F, 1.0F);

		ServerPlayer npc = ((CraftPlayer) p).getHandle();
		Utils.scheduleTask(() -> {
			Vec3 m = npc.getDeltaMovement();
			npc.setDeltaMovement(new Vec3(m.x(), LAUNCH_VELOCITY, m.z()));
			npc.hurtMarked = true;
			MovementAudit.startAirborneAudit(p, "lavajump");

			// One tick after launch, the stored Y delta reflects lava's drag multiplier (if still in lava):
			//   shallow lava (0.8x): ≈ 2.80 → "big"
			//   deep lava   (0.5x): ≈ 1.75 → "small"
			Utils.scheduleTask(() -> {
				double postY = npc.getDeltaMovement().y();
				String kind = postY > (LAUNCH_VELOCITY * 0.65D) ? "big" : "small";
				Utils.debug(Utils.DebugType.SERVER, p.getName() + " lava launched with upwards velocity " + postY + " classified " + kind);
			}, 1);
		}, 1);
	}
}
