package listeners;

import instructions.bosses.CustomBossBar;
import instructions.bosses.Watcher;
import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import nms.PlayerPacketInterceptor;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.craftbukkit.v1_21_R7.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftWither;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import plugin.FakePlayerManager;
import plugin.Utils;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class JoinListener implements Listener {
	/**
	 * Handles the player login event and updates the server with fake players' data.
	 * When a player logs in, this method retrieves and broadcasts data for all
	 * fake players, ensuring they appear correctly to all clients.
	 *
	 * @param ev the PlayerLoginEvent that is triggered when a player logs in
	 */
	@EventHandler
	public void onJoin(PlayerJoinEvent ev) {
		Utils.scheduleTask(() -> {
			Player joiningPlayer = ev.getPlayer();

			if (!FakePlayerManager.getFakePlayers().containsValue(joiningPlayer)) {
				try {
					Channel ch = getChannel(joiningPlayer);
					if (ch.pipeline().get("tas_interceptor") == null)
						ch.pipeline().addBefore("packet_handler", "tas_interceptor",
								new PlayerPacketInterceptor(joiningPlayer));
				} catch (Exception ex) {
					Bukkit.getLogger().warning("[M7TAS] Could not inject interceptor for "
							+ joiningPlayer.getName() + ": " + ex.getMessage());
				}
			}

			ServerGamePacketListenerImpl conn = ((CraftPlayer) joiningPlayer).getHandle().connection;              // PlayerConnection

			// Re-send each fake NPC’s “add + spawn” packets just to this connection:
			for(Player fake : FakePlayerManager.getFakePlayers().values()) {
				// 1) NMS handles
				ServerPlayer npc = ((CraftPlayer) fake).getHandle();
				ServerLevel world = ((CraftWorld) Objects.requireNonNull(fake.getWorld())).getHandle();

				ServerEntity entry = new ServerEntity(world, npc, 0, false,
						new ServerEntity.Synchronizer() {
							@Override
							public void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> packet) {
								// No-op for fake players
							}

							@Override
							public void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> packet) {
								// No-op for fake players
							}

							@Override
							public void sendToTrackingPlayersFiltered(Packet<? super ClientGamePacketListener> packet,
																	  Predicate<ServerPlayer> filter) {
								// No-op for fake players
							}

							@Override
							public void sendToTrackingPlayersFilteredAndSelf(Packet<? super ClientGamePacketListener> packet,
																			 Predicate<ServerPlayer> filter) {
								// No-op for fake players
							}
						}, new HashSet<>());

				// 2) Build the “ADD_PLAYER” info packet
				EnumSet<ClientboundPlayerInfoUpdatePacket.Action> addAction = EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER);
				ClientboundPlayerInfoUpdatePacket addPkt = new ClientboundPlayerInfoUpdatePacket(addAction, List.of(npc));

				// 3) Build the spawn packet (uses the same entry you used at creation)
				ClientboundAddEntityPacket spawnPkt = new ClientboundAddEntityPacket(npc, entry);

				// 4) Send JUST to the joining player
				conn.send(addPkt);
				conn.send(spawnPkt);
			}

			BossBar activeBossBar = CustomBossBar.getActiveBossBar();
			Wither activeWither = CustomBossBar.getActiveWither();

			if(activeBossBar != null && activeWither != null) {
				// Add the joining player to the custom boss bar
				activeBossBar.addPlayer(joiningPlayer);

				// Disable vanilla wither boss bar for this player
				if(activeWither instanceof CraftWither) {
					try {
						WitherBoss nmsWither = ((CraftWither) activeWither).getHandle();
						ServerPlayer nmsPlayer = ((CraftPlayer) joiningPlayer).getHandle();

						// Remove the joining player from vanilla boss bar
						nmsWither.bossEvent.removePlayer(nmsPlayer);
					} catch(Exception e) {
						Bukkit.getLogger().warning("Failed to remove vanilla wither bossbar for joining player: " + e.getMessage());
					}
				}
			}

			// Also check for active Watcher boss bar
			BossBar watcherBossBar = Watcher.getActiveBossBar();
			if(watcherBossBar != null) {
				watcherBossBar.addPlayer(joiningPlayer);
			}
		}, 1);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent ev) {
		Player p = ev.getPlayer();
		if (FakePlayerManager.getFakePlayers().containsValue(p)) return;
		try {
			Channel ch = getChannel(p);
			if (ch.pipeline().get("tas_interceptor") != null)
				ch.pipeline().remove("tas_interceptor");
		} catch (Exception ignored) { /* channel already closed */ }
	}

	private static Channel getChannel(Player player) throws Exception {
		ServerPlayer nms = ((CraftPlayer) player).getHandle();
		Field connField = ServerCommonPacketListenerImpl.class.getDeclaredField("connection");
		connField.setAccessible(true);
		Connection conn = (Connection) connField.get(nms.connection);
		Field channelField = Connection.class.getDeclaredField("channel");
		channelField.setAccessible(true);
		return (Channel) channelField.get(conn);
	}
}