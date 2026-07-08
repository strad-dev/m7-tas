package listeners;

import instructions.bosses.CustomBossBar;
import instructions.bosses.Watcher;
import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import nms.PlayerPacketInterceptor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BossBar;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.entity.CraftWither;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import plugin.FakePlayerManager;
import plugin.Utils;

import java.lang.reflect.Field;

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

			// Remove the vanilla attack cooldown for every joining player (instant re-attack).
			var attackSpeed = joiningPlayer.getAttribute(Attribute.ATTACK_SPEED);
			if(attackSpeed != null) attackSpeed.setBaseValue(100);

			// Full knockback resistance for every joining player (so no armor needs to grant it).
			var knockback = joiningPlayer.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
			if(knockback != null) knockback.setBaseValue(1);

			// Full EXPLOSION knockback resistance too — explosion knockback (e.g. Necron's fireballs) is governed by
			// this separate attribute, NOT KNOCKBACK_RESISTANCE, so without it players still get blasted by blasts.
			var explosionKb = joiningPlayer.getAttribute(Attribute.EXPLOSION_KNOCKBACK_RESISTANCE);
			if(explosionKb != null) explosionKb.setBaseValue(1);

			// Large safe-fall distance so players don't take fall damage during practice.
			var safeFall = joiningPlayer.getAttribute(Attribute.SAFE_FALL_DISTANCE);
			if(safeFall != null) safeFall.setBaseValue(1024);

			// Put every player in the no-collision team so real players don't push each other (or the fakes).
			plugin.PlayerCollision.addToNoCollisionTeam(joiningPlayer);

			// Default real players to 400 speed, bumped if their helmet entitles them (Cow Hat 550 / Racing 650).
			// Fakes are script-managed, so leave their speed alone.
			if(!FakePlayerManager.getFakePlayers().containsValue(joiningPlayer)) {
				plugin.HelmetSpeedSync.initSpeed(joiningPlayer);
			}

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

			// TAS-only: re-send fake-player spawn packets to joiners — disabled in the practice fork (no fakes).
//			ServerGamePacketListenerImpl conn = ((CraftPlayer) joiningPlayer).getHandle().connection;              // PlayerConnection
//
//			// Re-send each fake NPC’s “add + spawn” packets just to this connection:
//			for(Player fake : FakePlayerManager.getFakePlayers().values()) {
//				// 1) NMS handles
//				ServerPlayer npc = ((CraftPlayer) fake).getHandle();
//				ServerLevel world = ((CraftWorld) Objects.requireNonNull(fake.getWorld())).getHandle();
//
//				ServerEntity entry = new ServerEntity(world, npc, 0, false,
//						new ServerEntity.Synchronizer() {
//							@Override
//							public void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> packet) {
//								// No-op for fake players
//							}
//
//							@Override
//							public void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> packet) {
//								// No-op for fake players
//							}
//
//							@Override
//							public void sendToTrackingPlayersFiltered(Packet<? super ClientGamePacketListener> packet,
//																	  Predicate<ServerPlayer> filter) {
//								// No-op for fake players
//							}
//
//							@Override
//							public void sendToTrackingPlayersFilteredAndSelf(Packet<? super ClientGamePacketListener> packet,
//																			 Predicate<ServerPlayer> filter) {
//								// No-op for fake players
//							}
//						}, new HashSet<>());
//
//				// 2) Build the “ADD_PLAYER” info packet
//				EnumSet<ClientboundPlayerInfoUpdatePacket.Action> addAction = EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER);
//				ClientboundPlayerInfoUpdatePacket addPkt = new ClientboundPlayerInfoUpdatePacket(addAction, List.of(npc));
//
//				// 3) Build the spawn packet (uses the same entry you used at creation)
//				ClientboundAddEntityPacket spawnPkt = new ClientboundAddEntityPacket(npc, entry);
//
//				// 4) Send JUST to the joining player
//				conn.send(addPkt);
//				conn.send(spawnPkt);
//			}

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

	// Force every real player to the dungeon-entrance spawn on join so they stop appearing above the
	// map. LOWEST priority so that if something runs /practice on the same join (e.g. the network
	// plugin sending a practicer in), that teleport runs afterwards and still wins.
	@EventHandler(priority = EventPriority.LOWEST)
	public void onJoinSpawn(PlayerJoinEvent ev) {
		Player jp = ev.getPlayer();
		if (FakePlayerManager.getFakePlayers().containsValue(jp)) return; // never the fakes
		if (jp.getGameMode() == GameMode.SPECTATOR) return;              // don't yank spectators
		World w = Bukkit.getWorld("world");
		if (w != null) jp.teleport(new Location(w, -120.5, 71, -183.5, 0.0f, 0.0f));
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent ev) {
		Player p = ev.getPlayer();
		if (FakePlayerManager.getFakePlayers().containsValue(p)) return;
		// Drop cached helmet-speed / relic-debuff transition state so a relog re-evaluates cleanly.
		plugin.HelmetSpeedSync.forget(p.getUniqueId());
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