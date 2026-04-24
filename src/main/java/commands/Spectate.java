package commands;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.util.Mth;
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
	private static final Map<Player, Set<Player>> hiddenFakePlayers = new HashMap<>();

	// Maps a real Player to the player they are spectating
	private static final Map<Player, Player> spectatorMap = new HashMap<>();

	// Previous fake-player position per spectator — used to compute observed delta for velocity injection
	private static final Map<Player, Vec3> prevFakePositions = new HashMap<>();

	// Last yaw/pitch we snapped the spectator to — used to detect rotation changes without relying on
	// nmsSpectator.getYRot() which stays stale when noPhysics=true blocks server position updates
	private static final Map<Player, float[]> lastSentRotations = new HashMap<>();

	// Maps a fake Player to the Player(s) that are spectating them
	private static final HashMap<Player, Set<Player>> reverseSpectatorMap = new HashMap<>();
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

			// NEW: Backup the player's inventory before spectating
			PlayerInventoryBackup.backup(p);

			Location fakeLocation = fakePlayer.getLocation();
			p.teleport(fakeLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);

			preventPlayerCollision(p, fakePlayer);

			// Sync inventory when starting to spectate
			PlayerInventoryBackup.syncInventory(fakePlayer);
			Utils.scheduleTask(() -> hideFakePlayerFromSpectator(p, fakePlayer), 1);

			if(spectatorSyncTask == null) {
				startSpectatorSync();
			}

			p.sendMessage("You are now spectating " + role);
			return true;
		} else if(cmd.getName().equalsIgnoreCase("unspectate")) {
			if(spectatorMap.containsKey(p)) {
				Player fakePlayer = spectatorMap.remove(p);
				if(fakePlayer != null) {
					Set<Player> spectators = reverseSpectatorMap.get(fakePlayer);
					if(spectators != null) {
						spectators.remove(p);
						if(spectators.isEmpty()) {
							reverseSpectatorMap.remove(fakePlayer);
						}
					}
				}

				prevFakePositions.remove(p);
				lastSentRotations.remove(p);
				// NEW: Restore the player's original inventory
				PlayerInventoryBackup.restoreAndRemove(p);
				PlayerCollision.removeFromNoCollisionTeam(p);
				p.setCollidable(false);
				if (p instanceof CraftPlayer craftPlayer) {
					craftPlayer.getHandle().setNoGravity(false);
					craftPlayer.getHandle().noPhysics = false;
				}
				p.removePotionEffect(PotionEffectType.INVISIBILITY);

				if(fakePlayer != null) {
					showFakePlayerToSpectator(p, fakePlayer);
				}

				Set<Player> hidden = hiddenFakePlayers.remove(p);
				if(hidden != null) {
					for(Player hiddenFake : hidden) {
						showFakePlayerToSpectator(p, hiddenFake);
					}
				}

				p.sendMessage("You are no longer spectating a class");
				return true;
			}
			p.sendMessage(ChatColor.RED + "You are not spectating a class");
			return true;
		}
		return true;
	}

	/**
	 * Retrieves the player that the given player is currently spectating.
	 *
	 * @param player the player whose spectating target is being retrieved
	 * @return the player being spectated by the given player, or null if the player is not spectating anyone
	 */
	public static Set<Player> getSpectatingPlayers(Player player) {
		return reverseSpectatorMap.getOrDefault(player, new HashSet<>());
	}

	public static void updateLastSentRotation(Player spectator, float yaw, float pitch) {
		lastSentRotations.put(spectator, new float[]{yaw, pitch});
	}

	public static void snapToFake(Player spectator) {
		Player fakePlayer = spectatorMap.get(spectator);
		if(fakePlayer == null || !(spectator instanceof CraftPlayer cs) || !(fakePlayer instanceof CraftPlayer cf)) return;
		ServerPlayer nmsSpectator = cs.getHandle();
		ServerPlayer nmsFake = cf.getHandle();
		nmsSpectator.connection.send(new ClientboundPlayerPositionPacket(nmsSpectator.getId(), PositionMoveRotation.of(nmsFake), Set.of()));
		nmsSpectator.absSnapTo(nmsFake.getX(), nmsFake.getY(), nmsFake.getZ(), nmsFake.getYRot(), nmsFake.getXRot());
	}

	/**
	 * @return the map of which real Player is spectating which fake Player.
	 */
	public static Map<Player, Player> getSpectatorMap() {
		return spectatorMap;
	}

	/**
	 * @return the map of which real Players are spectating each fake Player
	 */
	public static Map<Player, Set<Player>> getReverseSpectatorMap() {
		return reverseSpectatorMap;
	}

	public static void preventPlayerCollision(Player realPlayer, Player fakePlayer) {
		// Add both players to the no-collision team
		PlayerCollision.addToNoCollisionTeam(realPlayer);
		PlayerCollision.addToNoCollisionTeam(fakePlayer);

		// Also make the real player unable to be hit by projectiles while spectating
		realPlayer.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 0, true, false));
		realPlayer.setCollidable(false);

		if (realPlayer instanceof CraftPlayer craftPlayer) {
			craftPlayer.getHandle().noPhysics = true;
		}
	}

	public static void hideFakePlayerFromSpectator(Player spectator, Player fakePlayer) {
		if(spectator instanceof CraftPlayer craftSpectator && fakePlayer instanceof CraftPlayer craftFake) {
			ServerPlayer nmsSpectator = craftSpectator.getHandle();
			ServerPlayer nmsFake = craftFake.getHandle();

			// Send destroy packet to hide the fake player from this spectator
			ClientboundRemoveEntitiesPacket destroyPacket = new ClientboundRemoveEntitiesPacket(nmsFake.getId());
			nmsSpectator.connection.send(destroyPacket);
		}
	}


	private static void showFakePlayerToSpectator(Player spectator, Player fakePlayer) {
		if(spectator instanceof CraftPlayer craftSpectator && fakePlayer instanceof CraftPlayer craftFake) {
			ServerPlayer nmsSpectator = craftSpectator.getHandle();
			ServerPlayer nmsFake = craftFake.getHandle();

			// Re-send spawn packet to show the fake player again
			ServerEntity entry = new ServerEntity(nmsFake.level(), nmsFake, 0, false, new ServerEntity.Synchronizer() {
				@Override
				public void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> packet) {
					// No-op for fake players
				}

				@Override
				public void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> packet) {
					// No-op for fake players
				}

				@Override
				public void sendToTrackingPlayersFiltered(Packet<? super ClientGamePacketListener> packet, Predicate<ServerPlayer> filter) {
					// No-op for fake players
				}

				@Override
				public void sendToTrackingPlayersFilteredAndSelf(Packet<? super ClientGamePacketListener> packet, Predicate<ServerPlayer> filter) {
					// No-op for fake players
				}
			}, new HashSet<>());
			ClientboundAddEntityPacket spawn = new ClientboundAddEntityPacket(nmsFake, entry);
			nmsSpectator.connection.send(spawn);

			// Also send metadata and equipment
			SynchedEntityData synchedEntityData = nmsFake.getEntityData();
			ClientboundSetEntityDataPacket metadataPacket = new ClientboundSetEntityDataPacket(nmsFake.getId(), synchedEntityData.getNonDefaultValues());
			nmsSpectator.connection.send(metadataPacket);

			PlayerInventory inventory = fakePlayer.getInventory();

			net.minecraft.world.item.ItemStack helmet = CraftItemStack.asNMSCopy(inventory.getHelmet());
			net.minecraft.world.item.ItemStack chestplate = CraftItemStack.asNMSCopy(inventory.getChestplate());
			net.minecraft.world.item.ItemStack leggings = CraftItemStack.asNMSCopy(inventory.getLeggings());
			net.minecraft.world.item.ItemStack boots = CraftItemStack.asNMSCopy(inventory.getBoots());
			net.minecraft.world.item.ItemStack mainHand = CraftItemStack.asNMSCopy(inventory.getItemInMainHand());
			net.minecraft.world.item.ItemStack offHand = CraftItemStack.asNMSCopy(inventory.getItemInOffHand());

			// Create equipment list
			List<Pair<EquipmentSlot, ItemStack>> equipment = List.of(Pair.of(EquipmentSlot.HEAD, helmet),      // HEAD
					Pair.of(EquipmentSlot.CHEST, chestplate),  // CHEST
					Pair.of(EquipmentSlot.LEGS, leggings),    // LEGS
					Pair.of(EquipmentSlot.FEET, boots),       // FEET
					Pair.of(EquipmentSlot.MAINHAND, mainHand),    // MAINHAND
					Pair.of(EquipmentSlot.OFFHAND, offHand)      // OFFHAND
			);

			// Send equipment packet to the specific spectator
			ClientboundSetEquipmentPacket equipmentPacket = new ClientboundSetEquipmentPacket(nmsFake.getId(), equipment);
			nmsSpectator.connection.send(equipmentPacket);
		}
	}

	public static void startSpectatorSync() {
		if(spectatorSyncTask != null) {
			spectatorSyncTask.cancel();
		}

		spectatorSyncTask = new BukkitRunnable() {
			@Override
			public void run() {
				Map<Player, Player> spectatorMap = Spectate.getSpectatorMap();
				// Iterate through all spectator relationships
				for(Player spectator : spectatorMap.keySet()) {
					Player fakePlayer = spectatorMap.get(spectator);

					// Get the fake player's current position and update spectators
					if(fakePlayer instanceof CraftPlayer craftFake && spectator instanceof CraftPlayer craftSpectator) {
						ServerPlayer nmsFake = craftFake.getHandle();
						ServerPlayer nmsSpectator = craftSpectator.getHandle();
						Vec3 prevPos = prevFakePositions.getOrDefault(spectator, nmsFake.position());
						Vec3 delta = nmsFake.position().subtract(prevPos);

						float fakeYaw = nmsFake.getYRot();
						float fakePitch = nmsFake.getXRot();
						float[] lastRot = lastSentRotations.get(spectator);
						boolean firstTick = lastRot == null;
						float yawDiff = firstTick ? 180f : Math.abs(Mth.wrapDegrees(lastRot[0] - fakeYaw));
						float pitchDiff = firstTick ? 180f : Math.abs(lastRot[1] - fakePitch);
						double spectatorDesyncSq = nmsSpectator.position().distanceToSqr(nmsFake.position());
						boolean teleport = spectatorDesyncSq > 10.0;
						boolean posSnap = firstTick || teleport;
						boolean rotSnap = yawDiff > 0.1f || pitchDiff > 0.1f;

						nmsSpectator.setPose(nmsFake.getPose());
						List<SynchedEntityData.DataValue<?>> dirtyData = nmsSpectator.getEntityData().getNonDefaultValues();
						if(dirtyData != null && !dirtyData.isEmpty()) {
							nmsSpectator.connection.send(new ClientboundSetEntityDataPacket(nmsSpectator.getId(), dirtyData));
						}

						if(posSnap) {
							// Position only — handleMovePlayer fires AFTER Entity.tick(), so sending
							// velocity on the same tick is redundant (it applies next tick anyway) and
							// the gap is already collapsed by setOldPosAndRot(). Keep them exclusive.
							nmsSpectator.connection.send(new ClientboundPlayerPositionPacket(nmsSpectator.getId(), PositionMoveRotation.of(nmsFake), Set.of()));
							lastSentRotations.put(spectator, new float[]{fakeYaw, fakePitch});
						} else if(rotSnap) {
							nmsSpectator.connection.send(new ClientboundPlayerRotationPacket(fakeYaw, false, fakePitch, false));
							lastSentRotations.put(spectator, new float[]{fakeYaw, fakePitch});
						} else {
							nmsSpectator.connection.send(new ClientboundPlayerRotationPacket(fakeYaw, false, fakePitch, false));
							// Velocity only — Entity.tick() provides the natural xo/x gap for smooth rendering.
							// The client applies the injected delta as exact displacement (no gravity subtraction).
							nmsSpectator.connection.send(new ClientboundSetEntityMotionPacket(nmsSpectator.getId(), new Vec3(delta.x, delta.y, delta.z)));
						}
						prevFakePositions.put(spectator, nmsFake.position());
						nmsSpectator.absSnapTo(nmsFake.getX(), nmsFake.getY(), nmsFake.getZ(), fakeYaw, fakePitch);

						// Keep fake player hidden
						nmsSpectator.connection.send(new ClientboundRemoveEntitiesPacket(nmsFake.getId()));
						updateFakePlayerVisibility();
					}
				}
			}
		};

		// Run every tick for smooth camera movement
		spectatorSyncTask.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	private static void updateFakePlayerVisibility() {
		Map<Player, Player> spectatorMap = Spectate.getSpectatorMap();
		for(Player spectator : spectatorMap.keySet()) {
			Player spectatedFake = spectatorMap.get(spectator);
			Location spectatorLocation = spectatedFake.getLocation(); // Use fake player's location

			Set<Player> currentlyHidden = hiddenFakePlayers.getOrDefault(spectator, new HashSet<>());
			Set<Player> shouldBeHidden = new HashSet<>();

			// Check all other fake players
			for(Player otherFake : FakePlayerManager.getFakePlayers().values()) {
				if(otherFake.equals(spectatedFake)) continue; // Skip the one being spectated

				double distance = spectatorLocation.distanceSquared(otherFake.getLocation());

				if(distance <= 3) {
					shouldBeHidden.add(otherFake);
					Spectate.hideFakePlayerFromSpectator(spectator, otherFake);
				} else {
					// Show if currently hidden
					if(currentlyHidden.contains(otherFake)) {
						showFakePlayerToSpectator(spectator, otherFake);
					}
				}
			}

			hiddenFakePlayers.put(spectator, shouldBeHidden);
		}
	}

	public static void stopSpectatorSync() {
		if(spectatorSyncTask != null) {
			spectatorSyncTask.cancel();
			spectatorSyncTask = null;
		}
	}

	public static void clearSpectatorMaps() {
		Spectate.stopSpectatorSync();

		for(Player spectator : new ArrayList<>(Spectate.getSpectatorMap().keySet())) {
			PlayerInventoryBackup.restoreAndRemove(spectator);
			spectator.removePotionEffect(PotionEffectType.INVISIBILITY);
			if(spectator instanceof CraftPlayer craftPlayer) {
				craftPlayer.getHandle().setNoGravity(false);
				craftPlayer.getHandle().noPhysics = false;
			}
		}

		Spectate.getSpectatorMap().clear();
		reverseSpectatorMap.clear();
		hiddenFakePlayers.clear();
		prevFakePositions.clear();
		lastSentRotations.clear();
	}
}