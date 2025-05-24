package plugin;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;

/**
 * Drops *all* incoming movement packets for N ticks by
 * temporarily disabling autoRead on the channel.
 */
public class MovementDropper implements Listener {
	private static final Plugin PLUGIN = M7tas.getInstance();

	/**
	 * Call this *after* you teleport your real player.
	 * It will pause reading *any* incoming flying/pos/look packets
	 * for exactly `ticks` server ticks (50 ms each).
	 */
	public static void pauseMovementReads(Player p, int ticks) {
		Channel ch = getChannel(p);
		if(ch == null) return;

		// disable reads
		ch.config().setAutoRead(false);

		// re‐enable in `ticks`
		Bukkit.getScheduler()
				.runTaskLater(PLUGIN, () -> {
					if(ch.isOpen()) ch.config().setAutoRead(true);
				}, ticks);
	}

	/**
	 * reflection helper to pull the netty Channel out of a Player
	 */
	private static Channel getChannel(Player p) {
		try {
			PlayerConnection conn = ((CraftPlayer) p).getHandle().f;
			Field nmField = ServerCommonPacketListenerImpl.class
					.getDeclaredField("e"); // that's the NetworkManager
			nmField.setAccessible(true);
			NetworkManager nm = (NetworkManager) nmField.get(conn);
			return nm.n; // the actual Netty Channel
		} catch(Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * We inject a no-op handler into the pipeline just so that
	 * the pipeline is *touched* (and thus initialized) before you ever
	 * call pauseMovementReads().  We do this on PlayerLoginEvent,
	 * delayed by one tick.
	 */
	@EventHandler
	public void onLogin(PlayerLoginEvent ev) {
		/*Bukkit.getScheduler().runTaskLater(PLUGIN, () -> {
			Channel ch = getChannel(ev.getPlayer());
			if(ch == null) return;
			ChannelPipeline pipe = ch.pipeline();
			if(pipe.get("m7tas_stall_tester") == null) {
				pipe.addBefore("packet_handler", "m7tas_stall_tester",
						new ChannelDuplexHandler() {
						});
			}
		}, 1);*/
	}
}