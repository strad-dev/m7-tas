package instructions;

import net.minecraft.network.protocol.game.PacketPlayOutEntityVelocity;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import plugin.M7tas;

public class Actions {
	/**
	 * Moves a Player in this Direction for t ticks.  The Vector referrs to the number of blocks per tick.
	 *
	 * @param p The Player
	 * @param perTick The Distance to be moved per tick
	 * @param durationTicks The total number of Ticks to move
	 */
	public static void move(Player p, Vector perTick, int durationTicks) {
		if (!(p instanceof CraftPlayer cp) || durationTicks <= 0) return;
		EntityPlayer nms = cp.getHandle();
		Vec3D motion = new Vec3D(perTick.getX(), perTick.getY(), perTick.getZ());

		new BukkitRunnable() {
			int ticks = 0;
			@Override
			public void run() {
				if (ticks++ >= durationTicks) {
					cancel();
					return;
				}
				// 1) set the server‐side motion
				nms.i(motion);
				nms.a(EnumMoveType.a, motion);
				nms.h();

				// 2) let each viewer animate that motion
				PacketPlayOutEntityVelocity vel = new PacketPlayOutEntityVelocity(nms);
				for (Player viewer : Bukkit.getOnlinePlayers()) {
					((CraftPlayer)viewer).getHandle().f.b(vel);
				}
				System.out.println(p.getLocation());
			}
		}.runTaskTimer(M7tas.getInstance(), 0, 1);
	}
}