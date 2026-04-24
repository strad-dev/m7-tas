package commands;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R7.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jspecify.annotations.NonNull;
import plugin.*;

import java.util.*;
import java.util.function.Predicate;

public class Spectate implements CommandExecutor {

	private static class SpectatorState {
		Vec3 prevFakePosition;
		float[] lastSentRotation;
		Vec3 lastKnownClientPosition;
		final Set<Player> hiddenFakePlayers = new HashSet<>();
	}

	// Maps a real Player to the fake Player they are spectating
	private static final Map<Player, Player> spectatorMap = new HashMap<>();

	// Per-spectator sync state
	private static final Map<Player, SpectatorState> spectatorStates = new HashMap<>();

	// Maps a fake Player to the Player(s) that are spectating them
	private static final Map<Player, Set<Player>> reverseSpectatorMap = new HashMap<>();

	private static BukkitRunnable spectatorSyncTask;

	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command cmd, @NonNull String label, String @NonNull [] args) {
		if(!(sender instanceof Player p)) {
			sender.sendMessage("Only players can run this");
			return true;
		}

		if(cmd.getName().equalsIgnoreCase("spectate")) {
			if(args.length < 1) {
				p.sendMessage(ChatColor.RED + "Please specify a class to spectate");
				return true;
			}

			Map<String, Player> fakePlayers = FakePlayerManager.getFakePlayers();
			if(fakePlayers.isEmpty()) {
				p.sendMessage(ChatColor.RED + "No classes to spectate!  Try running /setup first");
				return true;
			}

			String role = args[0];
			role = Character.toUpperCase(role.charAt(0)) + role.substring(1).toLowerCase();
			if(!fakePlayers.containsKey(role)) {
				p.sendMessage(ChatColor.RED + "Invalid class specified");
				return true;
			}

			if(spectatorMap.containsKey(p)) {
				p.sendMessage(ChatColor.RED + "You are already spectating a class.  Use /unspectate first");
				return true;
			}

			Player fakePlayer = fakePlayers.get(role);
			spectatorMap.put(p, fakePlayer);
			reverseSpectatorMap.computeIfAbsent(fakePlayer, k -> new HashSet<>()).add(p);
			spectatorStates.put(p, new SpectatorState());

			PlayerInventoryBackup.backup(p);

			Location fakeLocation = fakePlayer.getLocation();
			p.teleport(fakeLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);

			preventPlayerCollision(p, fakePlayer);

			PlayerInventoryBackup.syncInventory(fakePlayer);
			Utils.scheduleTask(() -> hideFakePlayerFromSpectator(p, fakePlayer), 1);

			if(spectatorSyncTask == null) {
				startSpectatorSync();
			}

			p.sendMessage("You are now spectating " + role);
			return true;
		} else if(cmd.getName().equalsIgnoreCase("unspectate")) {
			if(spectatorMap.containsKey(p)) {
				removeSpectator(p);
				p.sendMessage("You are no longer spectating a class");
				return true;
			}
			p.sendMessage(ChatColor.RED + "You are not spectating a class");
			return true;
		}
		return true;
	}

	private static void removeSpectator(Player p) {
		SpectatorState state = spectatorStates.remove(p);
		Player fakePlayer = spectatorMap.remove(p);

		if(fakePlayer != null) {
			Set<Player> spectators = reverseSpectatorMap.get(fakePlayer);
			if(spectators != null) {
				spectators.remove(p);
				if(spectators.isEmpty()) reverseSpectatorMap.remove(fakePlayer);
			}
			showFakePlayerToSpectator(p, fakePlayer);
		}

		if(state != null) {
			for(Player hiddenFake : state.hiddenFakePlayers) {
				showFakePlayerToSpectator(p, hiddenFake);
			}
		}

		PlayerInventoryBackup.restoreAndRemove(p);
		restorePlayerCollision(p);
	}

	public static Set<Player> getSpectatingPlayers(Player player) {
		return reverseSpectatorMap.getOrDefault(player, new HashSet<>());
	}

	public static void updateLastSentRotation(Player spectator, float yaw, float pitch) {
		SpectatorState state = spectatorStates.get(spectator);
		if(state != null) state.lastSentRotation = new float[]{yaw, pitch};
	}

	public static void updateClientPosition(Player spectator, double x, double y, double z) {
		SpectatorState state = spectatorStates.get(spectator);
		if(state != null) state.lastKnownClientPosition = new Vec3(x, y, z);
	}

	public static Map<Player, Player> getSpectatorMap() {
		return spectatorMap;
	}

	public static Map<Player, Set<Player>> getReverseSpectatorMap() {
		return reverseSpectatorMap;
	}

	public static void preventPlayerCollision(Player realPlayer, Player fakePlayer) {
		PlayerCollision.addToNoCollisionTeam(realPlayer);
		PlayerCollision.addToNoCollisionTeam(fakePlayer);
		realPlayer.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 0, true, false));
		realPlayer.setCollidable(false);
		if(realPlayer instanceof CraftPlayer craftPlayer) {
			craftPlayer.getHandle().noPhysics = true;
		}
	}

	private static void restorePlayerCollision(Player player) {
		PlayerCollision.removeFromNoCollisionTeam(player);
		player.setCollidable(true);
		player.removePotionEffect(PotionEffectType.INVISIBILITY);
		if(player instanceof CraftPlayer craftPlayer) {
			craftPlayer.getHandle().setNoGravity(false);
			craftPlayer.getHandle().noPhysics = false;
		}
	}

	public static void hideFakePlayerFromSpectator(Player spectator, Player fakePlayer) {
		if(spectator instanceof CraftPlayer craftSpectator && fakePlayer instanceof CraftPlayer craftFake) {
			ServerPlayer nmsSpectator = craftSpectator.getHandle();
			ServerPlayer nmsFake = craftFake.getHandle();
			nmsSpectator.connection.send(new ClientboundRemoveEntitiesPacket(nmsFake.getId()));
		}
	}

	private static void showFakePlayerToSpectator(Player spectator, Player fakePlayer) {
		if(spectator instanceof CraftPlayer craftSpectator && fakePlayer instanceof CraftPlayer craftFake) {
			ServerPlayer nmsSpectator = craftSpectator.getHandle();
			ServerPlayer nmsFake = craftFake.getHandle();

			ServerEntity entry = new ServerEntity(nmsFake.level(), nmsFake, 0, false, new ServerEntity.Synchronizer() {
				@Override public void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> packet) {}
				@Override public void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> packet) {}
				@Override public void sendToTrackingPlayersFiltered(Packet<? super ClientGamePacketListener> packet, Predicate<ServerPlayer> filter) {}
				@Override public void sendToTrackingPlayersFilteredAndSelf(Packet<? super ClientGamePacketListener> packet, Predicate<ServerPlayer> filter) {}
			}, new HashSet<>());
			nmsSpectator.connection.send(new ClientboundAddEntityPacket(nmsFake, entry));
			nmsSpectator.connection.send(new ClientboundSetEntityDataPacket(nmsFake.getId(), nmsFake.getEntityData().getNonDefaultValues()));

			PlayerInventory inventory = fakePlayer.getInventory();
			nmsSpectator.connection.send(new ClientboundSetEquipmentPacket(nmsFake.getId(), List.of(
				Pair.of(EquipmentSlot.HEAD,     CraftItemStack.asNMSCopy(inventory.getHelmet())),
				Pair.of(EquipmentSlot.CHEST,    CraftItemStack.asNMSCopy(inventory.getChestplate())),
				Pair.of(EquipmentSlot.LEGS,     CraftItemStack.asNMSCopy(inventory.getLeggings())),
				Pair.of(EquipmentSlot.FEET,     CraftItemStack.asNMSCopy(inventory.getBoots())),
				Pair.of(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(inventory.getItemInMainHand())),
				Pair.of(EquipmentSlot.OFFHAND,  CraftItemStack.asNMSCopy(inventory.getItemInOffHand()))
			)));
		}
	}

	public static void startSpectatorSync() {
		if(spectatorSyncTask != null) {
			spectatorSyncTask.cancel();
		}

		spectatorSyncTask = new BukkitRunnable() {
			@Override
			public void run() {
				for(Map.Entry<Player, Player> entry : spectatorMap.entrySet()) {
					Player spectator = entry.getKey();
					Player fakePlayer = entry.getValue();
					SpectatorState state = spectatorStates.get(spectator);

					if(state == null || !(fakePlayer instanceof CraftPlayer craftFake) || !(spectator instanceof CraftPlayer craftSpectator)) continue;

					ServerPlayer nmsFake = craftFake.getHandle();
					ServerPlayer nmsSpectator = craftSpectator.getHandle();

					Vec3 prevPos = state.prevFakePosition != null ? state.prevFakePosition : nmsFake.position();
					Vec3 delta = nmsFake.position().subtract(prevPos);
					Vec3 clientPos = state.lastKnownClientPosition != null ? state.lastKnownClientPosition : nmsFake.position();

					float fakeYaw = nmsFake.getYRot();
					float fakePitch = nmsFake.getXRot();
					boolean firstTick = state.prevFakePosition == null;
					boolean posSnap = firstTick || clientPos.distanceToSqr(nmsFake.position()) > 0.01;

					nmsSpectator.setPose(nmsFake.getPose());
					List<SynchedEntityData.DataValue<?>> dirtyData = nmsSpectator.getEntityData().getNonDefaultValues();
					if(dirtyData != null && !dirtyData.isEmpty()) {
						nmsSpectator.connection.send(new ClientboundSetEntityDataPacket(nmsSpectator.getId(), dirtyData));
					}

					if(posSnap) {
						nmsSpectator.connection.send(new ClientboundPlayerPositionPacket(nmsSpectator.getId(), PositionMoveRotation.of(nmsFake), Set.of()));
						nmsSpectator.absSnapTo(nmsFake.getX(), nmsFake.getY(), nmsFake.getZ(), fakeYaw, fakePitch);
						state.lastKnownClientPosition = nmsFake.position();
						state.lastSentRotation = new float[]{fakeYaw, fakePitch};
					} else {
						nmsSpectator.connection.send(new ClientboundPlayerRotationPacket(fakeYaw, false, fakePitch, false));
						nmsSpectator.connection.send(new ClientboundSetEntityMotionPacket(nmsSpectator.getId(), new Vec3(delta.x, delta.y, delta.z)));
					}
					state.prevFakePosition = nmsFake.position();

					nmsSpectator.connection.send(new ClientboundRemoveEntitiesPacket(nmsFake.getId()));
				}
				updateFakePlayerVisibility();
			}
		};

		spectatorSyncTask.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	private static void updateFakePlayerVisibility() {
		for(Map.Entry<Player, Player> entry : spectatorMap.entrySet()) {
			Player spectator = entry.getKey();
			Player spectatedFake = entry.getValue();
			SpectatorState state = spectatorStates.get(spectator);
			if(state == null) continue;

			Location spectatorLocation = spectatedFake.getLocation();
			Set<Player> shouldBeHidden = new HashSet<>();

			for(Player otherFake : FakePlayerManager.getFakePlayers().values()) {
				if(otherFake.equals(spectatedFake)) continue;
				double distance = spectatorLocation.distanceSquared(otherFake.getLocation());
				if(distance <= 3) {
					shouldBeHidden.add(otherFake);
					hideFakePlayerFromSpectator(spectator, otherFake);
				} else if(state.hiddenFakePlayers.contains(otherFake)) {
					showFakePlayerToSpectator(spectator, otherFake);
				}
			}

			state.hiddenFakePlayers.clear();
			state.hiddenFakePlayers.addAll(shouldBeHidden);
		}
	}

	public static void stopSpectatorSync() {
		if(spectatorSyncTask != null) {
			spectatorSyncTask.cancel();
			spectatorSyncTask = null;
		}
	}

	public static void clearSpectatorMaps() {
		stopSpectatorSync();
		for(Player spectator : new ArrayList<>(spectatorMap.keySet())) {
			removeSpectator(spectator);
		}
		spectatorMap.clear();
		reverseSpectatorMap.clear();
		spectatorStates.clear();
	}
}
