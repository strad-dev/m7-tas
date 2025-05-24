package plugin;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.a;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.server.level.*;
import org.bukkit.craftbukkit.v1_21_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

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
	public void onLogin(PlayerLoginEvent ev) {
		List<Player> fakePlayers = M7tas.getFakePlayers();
		for(Player p : fakePlayers) {
			WorldServer nmsWorld = ((CraftWorld) Objects.requireNonNull(p.getWorld())).getHandle();
			EntityPlayer nmsPlayer = ((CraftPlayer) p).getHandle();

			ChunkProviderServer provider = nmsWorld.m();
			provider.a.a(nmsPlayer);

			int id = nmsPlayer.ar(); // in your mappings this is nmsEntity.ar()
			PlayerChunkMap.EntityTracker wrapper = provider.a.K.get(id);

			EntityTrackerEntry entry = wrapper.b;

			EnumSet<ClientboundPlayerInfoUpdatePacket.a> addAction = EnumSet.of(a.a);
			ClientboundPlayerInfoUpdatePacket add = new ClientboundPlayerInfoUpdatePacket(addAction, List.of(nmsPlayer));
			PacketPlayOutSpawnEntity spawn = new PacketPlayOutSpawnEntity(nmsPlayer, entry);

			Utils.broadcastPacket(add);
			Utils.broadcastPacket(spawn);
		}
	}
}