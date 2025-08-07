package plugin;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.craftbukkit.v1_21_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
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
			ServerGamePacketListenerImpl conn = ((CraftPlayer) ev.getPlayer()).getHandle().connection;              // PlayerConnection

			// Re-send each fake NPC’s “add + spawn” packets just to this connection:
			for(Player fake : M7tas.getFakePlayers()) {
				// 1) NMS handles
				ServerPlayer npc = ((CraftPlayer) fake).getHandle();
				ServerLevel world = ((CraftWorld) Objects.requireNonNull(fake.getWorld())).getHandle();

				ServerEntity entry = new ServerEntity(world, npc, 0, false, packet -> {}, new HashSet<>());

				// 2) Build the “ADD_PLAYER” info packet
				EnumSet<ClientboundPlayerInfoUpdatePacket.Action> addAction = EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER);
				ClientboundPlayerInfoUpdatePacket addPkt = new ClientboundPlayerInfoUpdatePacket(addAction, List.of(npc));

				// 3) Build the spawn packet (uses the same entry you used at creation)
				ClientboundAddEntityPacket spawnPkt = new ClientboundAddEntityPacket(npc, entry);

				// 4) Send JUST to the joining player
				conn.send(addPkt);
				conn.send(spawnPkt);
			}
		}, 1);
	}
}