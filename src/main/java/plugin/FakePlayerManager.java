package plugin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.ProfileResult;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import nms.TASGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.v1_21_R7.CraftServer;
import org.bukkit.craftbukkit.v1_21_R7.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FakePlayerManager {
	private static final Map<String, Player> fakePlayers = new HashMap<>();
	private static boolean fakeTickerStarted = false;

	private static final Map<String, String> SKIN_DATA = Map.of("Archer", "0b0fa6bc-69ee-4f6c-a4f8-7cac79f1871a", "Mage3", "f0b1d2e5-dfd2-4d33-9768-4d2b259a4743", "Mage4", "6715b245-be6e-496c-87eb-1d2c19066403", "Mage1", "cdb9e9c6-c096-4f58-9c49-35395d7b897c", "Mage2", "5d142c3a-bdf1-418b-b907-797bbaaed188");

	public static Map<String, Player> getFakePlayers() {
		return fakePlayers;
	}

	public static void spawnAllFakes(World world) {
		stopCustomConnection();
		kickAllFakes();
		fakePlayers.clear();

		for(var entry : SKIN_DATA.entrySet()) {
			String role = entry.getKey();
			String skin = entry.getValue();

			Player fake = spawnFakePlayer(world, role, UUID.fromString(skin));
			fakePlayers.put(role, fake);
		}
		startFakePlayerTicker();
	}

	public static Player spawnFakePlayer(World world, String fakePlayerName, UUID skinOwner) {
		MinecraftServer nmsServer = ((CraftServer) M7tas.getInstance().getServer()).getServer();
		ServerLevel nmsWorld = ((CraftWorld) world).getHandle();

		GameProfile profile = buildFakeProfileWithSkin(UUID.randomUUID(), fakePlayerName, skinOwner);

		ClientInformation clientInfo = ClientInformation.createDefault();
		ServerPlayer nmsPlayer = new ServerPlayer(nmsServer, nmsWorld, profile, clientInfo);

		Connection nm = new Connection(PacketFlow.SERVERBOUND) {
			{
				this.channel = new EmbeddedChannel();
				this.address = new InetSocketAddress("127.0.0.1", 0);
			}

			@Override
			public void send(Packet<?> packet) {

			}

			@Override
			public boolean isConnected() {
				return true;
			}
		};
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
		TASGamePacketListenerImpl connection = new TASGamePacketListenerImpl(nmsServer, nm, nmsPlayer, cookie);
		nmsPlayer.connection = connection;
		forceCustomConnection(nmsPlayer, connection);

		nmsPlayer.setPos(-120.5, 71, -183.5);
		nmsPlayer.setYRot(0.0f);
		nmsPlayer.setXRot(0.0f);
		nmsPlayer.setNoGravity(false);

		nmsServer.getPlayerList().placeNewPlayer(nm, nmsPlayer, cookie);

		nmsPlayer.getEntityData().set(net.minecraft.world.entity.player.Player.DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7F);

		ClientboundSetEntityDataPacket entityMetadataPacket = new ClientboundSetEntityDataPacket(nmsPlayer.getId(), nmsPlayer.getEntityData().getNonDefaultValues());

		Utils.broadcastPacket(entityMetadataPacket);

		Player bukkitPlayer = nmsPlayer.getBukkitEntity();

		PlayerCollision.addToNoCollisionTeam(bukkitPlayer);
		Utils.setSpeed(bukkitPlayer, 400);

		return bukkitPlayer;
	}

	public static GameProfile buildFakeProfileWithSkin(UUID fakeUuid, String fakeName, UUID skinOwnerUuid) {
		MinecraftServer nms = ((CraftServer) Bukkit.getServer()).getServer();
		ProfileResult result = nms.services().sessionService().fetchProfile(skinOwnerUuid, true);

		if(result == null) {
			throw new IllegalStateException("Failed to fetch profile for " + skinOwnerUuid);
		}

		GameProfile populated = result.profile();
		Multimap<String, Property> properties = HashMultimap.create();
		for(Property tex : populated.properties().get("textures")) {
			properties.put("textures", tex);
		}

		PropertyMap propertyMap = new PropertyMap(properties);
		return new GameProfile(fakeUuid, fakeName, propertyMap);
	}

	private static void kickAllFakes() {
		fakePlayers.values().forEach(p -> {
			if(p.isOnline()) {
				p.kickPlayer("");
			}
		});
		fakePlayers.clear();
	}

	private static void startFakePlayerTicker() {
		if(fakeTickerStarted) {
			return;
		}
		fakeTickerStarted = true;

		Method updatePlayerPose;
		try {
			updatePlayerPose = net.minecraft.world.entity.player.Player.class
					.getDeclaredMethod("updatePlayerPose");
			updatePlayerPose.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Failed to find updatePlayerPose", e);
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				for(Player fake : fakePlayers.values()) {
					if(!(fake instanceof CraftPlayer)) continue;
					ServerPlayer npc = ((CraftPlayer) fake).getHandle();
					npc.setNoGravity(false);
					try {
						updatePlayerPose.invoke(npc);
					} catch (Exception e) {
						e.printStackTrace();
					}
					npc.aiStep();
				}
			}
		}.runTaskTimer(M7tas.getInstance(), 0, 1);
	}

	private static final Map<ServerPlayer, BukkitRunnable> customConnectionTask = new HashMap<>();

	private static void forceCustomConnection(ServerPlayer serverPlayer, TASGamePacketListenerImpl connection) {
		customConnectionTask.put(serverPlayer, new BukkitRunnable() {
			@Override
			public void run() {
				serverPlayer.connection = connection;
			}
		});
		customConnectionTask.get(serverPlayer).runTaskTimer(M7tas.getInstance(), 0, 1);
	}

	public static void stopCustomConnection() {
		for(ServerPlayer serverPlayer : customConnectionTask.keySet()) {
			customConnectionTask.get(serverPlayer).cancel();
			MinecraftServer nmsServer = ((CraftServer) M7tas.getInstance().getServer()).getServer();
			Connection nm = new Connection(PacketFlow.SERVERBOUND) {
				{
					this.channel = new EmbeddedChannel();
					this.address = new InetSocketAddress("127.0.0.1", 0);
				}

				@Override
				public void send(Packet<?> packet) {

				}

				@Override
				public boolean isConnected() {
					return true;
				}
			};
			CommonListenerCookie cookie = CommonListenerCookie.createInitial(serverPlayer.getGameProfile(), false);
			serverPlayer.connection = new ServerGamePacketListenerImpl(nmsServer, nm, serverPlayer, cookie);
		}
	}
}
