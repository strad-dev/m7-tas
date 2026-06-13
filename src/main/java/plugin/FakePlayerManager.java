package plugin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;
import nms.TASGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_21_R7.CraftServer;
import org.bukkit.craftbukkit.v1_21_R7.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FakePlayerManager {
	private static final Map<String, Player> fakePlayers = new HashMap<>();
	private static boolean fakeTickerStarted = false;

	// Launch impulses (e.g. Bonzo's Staff) queued from entity-tick events, applied at the top of the fake
	// player's NEXT aiStep. Setting deltaMovement directly in the firing event is too early: the fake ticker's
	// aiStep already ran this tick, and by the next one ground/jump/input re-assertion has clobbered the impulse,
	// so the full first-tick displacement is lost. Applying it immediately before aiStep guarantees the launch
	// integrates as a clean in-air first tick, matching real (client-physics) behaviour.
	private static final Map<UUID, net.minecraft.world.phys.Vec3> pendingLaunches = new HashMap<>();

	/** Queue a launch impulse to be applied to {@code fake}'s deltaMovement at the start of its next aiStep.
	 *  Only meaningful for fake players (those driven by {@link #startFakePlayerTicker()}). */
	public static void launch(Player fake, org.bukkit.util.Vector velocity) {
		pendingLaunches.put(fake.getUniqueId(), new net.minecraft.world.phys.Vec3(velocity.getX(), velocity.getY(), velocity.getZ()));
	}

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

		// View distance 0 — ChunkMap.getPlayerViewDistance clamps to min 2 internally,
		// but this still drops the per-fake chunk load from 17x17=289 to 5x5=25.
		ClientInformation clientInfo = new ClientInformation("en_us", 0, ChatVisiblity.FULL, true, 0, HumanoidArm.RIGHT, false, false, ParticleStatus.MINIMAL);
		ServerPlayer nmsPlayer = new ServerPlayer(nmsServer, nmsWorld, profile, clientInfo);
		try {
			java.lang.reflect.Field firstTick = net.minecraft.world.entity.Entity.class.getDeclaredField("firstTick");
			firstTick.setAccessible(true);
			firstTick.set(nmsPlayer, false);
		} catch (Exception e) {
			throw new RuntimeException("Failed to clear firstTick on fake player", e);
		}

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

	private static final long SKIN_CACHE_TTL_MS = 24L * 60L * 60L * 1000L;
	private static Map<UUID, CachedSkin> skinCache = null;

	private record CachedSkin(String value, String signature, long fetchedAt) {
		boolean isFresh() {
			return System.currentTimeMillis() - fetchedAt < SKIN_CACHE_TTL_MS;
		}
	}

	public static GameProfile buildFakeProfileWithSkin(UUID fakeUuid, String fakeName, UUID skinOwnerUuid) {
		if(skinCache == null) {
			skinCache = loadSkinCache();
		}

		CachedSkin cached = skinCache.get(skinOwnerUuid);
		Property texture;
		if(cached != null && cached.isFresh()) {
			texture = new Property("textures", cached.value(), cached.signature());
		} else {
			MinecraftServer nms = ((CraftServer) Bukkit.getServer()).getServer();
			ProfileResult result = nms.services().sessionService().fetchProfile(skinOwnerUuid, true);
			if(result == null) {
				throw new IllegalStateException("Failed to fetch profile for " + skinOwnerUuid);
			}
			var fetched = result.profile().properties().get("textures").iterator();
			if(!fetched.hasNext()) {
				throw new IllegalStateException("Profile for " + skinOwnerUuid + " has no textures property");
			}
			texture = fetched.next();
			skinCache.put(skinOwnerUuid, new CachedSkin(texture.value(), texture.signature(), System.currentTimeMillis()));
			saveSkinCache(skinCache);
		}

		Multimap<String, Property> properties = HashMultimap.create();
		properties.put("textures", texture);
		return new GameProfile(fakeUuid, fakeName, new PropertyMap(properties));
	}

	private static File getSkinCacheFile() {
		File folder = M7tas.getInstance().getDataFolder();
		if(!folder.exists()) {
			folder.mkdirs();
		}
		return new File(folder, "skins.json");
	}

	private static Map<UUID, CachedSkin> loadSkinCache() {
		Map<UUID, CachedSkin> out = new HashMap<>();
		File file = getSkinCacheFile();
		if(!file.exists()) {
			return out;
		}
		try {
			String raw = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
			JsonObject root = JsonParser.parseString(raw).getAsJsonObject();
			for(var entry : root.entrySet()) {
				JsonObject obj = entry.getValue().getAsJsonObject();
				out.put(UUID.fromString(entry.getKey()), new CachedSkin(obj.get("value").getAsString(), obj.has("signature") && !obj.get("signature").isJsonNull() ? obj.get("signature").getAsString() : null, obj.get("fetchedAt").getAsLong()));
			}
		} catch (Exception e) {
			M7tas.getInstance().getLogger().warning("Failed to load skin cache, starting empty: " + e.getMessage());
		}
		return out;
	}

	private static void saveSkinCache(Map<UUID, CachedSkin> cache) {
		JsonObject root = new JsonObject();
		for(var entry : cache.entrySet()) {
			JsonObject obj = new JsonObject();
			obj.addProperty("value", entry.getValue().value());
			obj.addProperty("signature", entry.getValue().signature());
			obj.addProperty("fetchedAt", entry.getValue().fetchedAt());
			root.add(entry.getKey().toString(), obj);
		}
		try {
			Files.write(getSkinCacheFile().toPath(), root.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			M7tas.getInstance().getLogger().warning("Failed to save skin cache: " + e.getMessage());
		}
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

		Field useItemRemainingField;
		try {
			useItemRemainingField = net.minecraft.world.entity.LivingEntity.class
					.getDeclaredField("useItemRemaining");
			useItemRemainingField.setAccessible(true);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Failed to find useItemRemaining", e);
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				for(Player fake : fakePlayers.values()) {
					if(!(fake instanceof CraftPlayer)) continue;
					ServerPlayer npc = ((CraftPlayer) fake).getHandle();
					TASGamePacketListenerImpl conn = fakeConnections.get(npc);
					if(conn != null) npc.connection = conn;
					npc.setNoGravity(false);
					try {
						updatePlayerPose.invoke(npc);
					} catch (Exception e) {
						e.printStackTrace();
					}
					// Re-assert movement fields before aiStep() — zza/xxa decay by 0.98x/tick otherwise
					String input = instructions.Actions.getActiveInput(fake.getUniqueId());
					if(!input.isEmpty()) {
						npc.xxa = (input.contains("A") && !input.contains("D")) ? 1.0F : (!input.contains("A") && input.contains("D")) ? -1.0F : 0.0F;
						npc.zza = (input.contains("W") && !input.contains("S")) ? 1.0F : (!input.contains("W") && input.contains("S")) ? -1.0F : 0.0F;
						if(input.contains("N")) { npc.xxa *= 0.3F; npc.zza *= 0.3F; }
					}
					if(input.contains("P") && npc.zza > 0 && !npc.isShiftKeyDown()) {
						npc.setSprinting(true);
					}
					if(npc.isUsingItem()) {
						try {
							int remaining = useItemRemainingField.getInt(npc);
							if(remaining > 1) {
								useItemRemainingField.setInt(npc, remaining - 1);
							} else {
								npc.stopUsingItem();
							}
						} catch(IllegalAccessException e) {
							e.printStackTrace();
						}
					}
					npc.updateFluidHeightAndDoFluidPushing(net.minecraft.tags.FluidTags.WATER, 0.014);
					npc.updateFluidHeightAndDoFluidPushing(net.minecraft.tags.FluidTags.LAVA, 0.007);
					// Apply any queued launch impulse RIGHT before aiStep so it's the deltaMovement this tick's
					// move() integrates — onGround/jumping cleared so a ground recompute or jumpFromGround can't
					// overwrite the upward component before the move happens.
					net.minecraft.world.phys.Vec3 launch = pendingLaunches.remove(fake.getUniqueId());
					if(launch != null) {
						npc.setOnGround(false);
						npc.setJumping(false);
						npc.setDeltaMovement(launch);
						npc.hurtMarked = true;
					}
					if(Utils.isSuperVerbose()) {
						net.minecraft.world.phys.Vec3 before = npc.position();
						npc.aiStep();
						net.minecraft.world.phys.Vec3 after = npc.position();
						MovementAudit.auditMove(fake, npc, after.x - before.x, after.y - before.y, after.z - before.z);
					} else {
						npc.aiStep();
					}
				}
			}
		}.runTaskTimer(M7tas.getInstance(), 0, 1);
	}

	private static final Map<ServerPlayer, TASGamePacketListenerImpl> fakeConnections = new HashMap<>();

	private static void forceCustomConnection(ServerPlayer serverPlayer, TASGamePacketListenerImpl connection) {
		fakeConnections.put(serverPlayer, connection);
	}

	public static void stopCustomConnection() {
		for(ServerPlayer serverPlayer : fakeConnections.keySet()) {
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
		fakeConnections.clear();
	}
}
