package plugin;

import net.minecraft.network.protocol.Packet;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class Utils {

	/**
	 * Sends a packet to every player on the server.
	 *
	 * @param pkt Packet to send
	 */
	public static void broadcastPacket(Packet<?> pkt) {
		for (Player p : Bukkit.getOnlinePlayers()) {
			((CraftPlayer)p).getHandle().f.b(pkt);
		}
	}

	/**
	 * Plays a sound for every player on the server
	 *
	 * @param s The sound to play
	 */
	public static void playGlobalSound(Sound s) {
		Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, s, 1.0F, 1.0F));
	}

	/**
	 * Plays a sound for every player on the server
	 *
	 * @param s      The sound to play
	 * @param volume The volume of the sound
	 * @param pitch  The pitch of the sound
	 */
	public static void playGlobalSound(Sound s, float volume, float pitch) {
		Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, s, volume, pitch));
	}
}