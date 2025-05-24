package plugin;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.a;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.server.level.ChunkProviderServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.EntityTrackerEntry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.PlayerConnection;
import org.bukkit.craftbukkit.v1_21_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.EnumSet;
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


			PlayerConnection conn = ((CraftPlayer) ev.getPlayer()).getHandle()     // EntityPlayer
					.f;              // PlayerConnection

			// Re-send each fake NPC’s “add + spawn” packets just to this connection:
			for(Player fake : M7tas.getFakePlayers()) {
				// 1) NMS handles
				EntityPlayer npc = ((CraftPlayer) fake).getHandle();
				WorldServer world = ((CraftWorld) Objects.requireNonNull(fake.getWorld())).getHandle();
				ChunkProviderServer provider = world.m();

				// ensure the chunk-provider is tracking our NPC
				provider.a.a(npc);

				// fetch the EntityTrackerEntry so we can spawn with all watchers
				int id = npc.ar();
				EntityTrackerEntry entry = provider.a.K.get(id).b;

				// 2) Build the “ADD_PLAYER” info packet
				EnumSet<a> addAction = EnumSet.of(a.a);
				ClientboundPlayerInfoUpdatePacket addPkt = new ClientboundPlayerInfoUpdatePacket(addAction, List.of(npc));

				// 3) Build the spawn packet (uses the same entry you used at creation)
				PacketPlayOutSpawnEntity spawnPkt = new PacketPlayOutSpawnEntity(npc, entry);

				// 4) Send JUST to the joining player
				conn.b(addPkt);
				conn.b(spawnPkt);
			}
		}, 1);
	}
}