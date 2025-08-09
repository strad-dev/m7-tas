package plugin;

import instructions.bosses.CustomBossBar;
import instructions.bosses.Watcher;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.craftbukkit.v1_21_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftWither;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

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
			ServerGamePacketListenerImpl conn = ((CraftPlayer) joiningPlayer).getHandle().connection;              // PlayerConnection

			// Re-send each fake NPC’s “add + spawn” packets just to this connection:
			for(Player fake : M7tas.getFakePlayers()) {
				// 1) NMS handles
				ServerPlayer npc = ((CraftPlayer) fake).getHandle();
				ServerLevel world = ((CraftWorld) Objects.requireNonNull(fake.getWorld())).getHandle();

				ServerEntity entry = new ServerEntity(world, npc, 0, false, packet -> {
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
}