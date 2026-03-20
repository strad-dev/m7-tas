package commands;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PositionMoveRotation;
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
import plugin.M7tas;
import plugin.Utils;

import java.util.*;
import java.util.function.Predicate;

public class Spectate implements CommandExecutor {
	private static final Map<Player, Set<Player>> hiddenFakePlayers = new HashMap<>();

	// Maps a real Player to the player they are spectating
	private static final Map<Player, Player> spectatorMap = new HashMap<>();

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

			Map<String, Player> fakePlayers = M7tas.getFakePlayers();
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
			Utils.backupInventory(p);

			Location fakeLocation = fakePlayer.getLocation();
			p.teleport(fakeLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);

			preventPlayerCollision(p, fakePlayer);

			// Sync inventory when starting to spectate
			Utils.syncInventory(fakePlayer);
			Utils.scheduleTask(() -> hideFakePlayerFromSpectator(p, fakePlayer), 1);

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

				// NEW: Restore the player's original inventory
				Utils.restoreInventory(p);
				M7tas.removeFromNoCollisionTeam(p);
				p.setCollidable(false);
				if (p instanceof CraftPlayer craftPlayer) {
					craftPlayer.getHandle().noPhysics = true;
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
		M7tas.addToNoCollisionTeam(realPlayer);
		M7tas.addToNoCollisionTeam(fakePlayer);

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
						// Match exact pose
						nmsSpectator.setPose(nmsFake.getPose());

						// Send pose metadata to spectator's client
						SynchedEntityData entityData = nmsSpectator.getEntityData();
						List<SynchedEntityData.DataValue<?>> dirtyData = entityData.getNonDefaultValues();
						if(dirtyData != null && !dirtyData.isEmpty()) {
							nmsSpectator.connection.send(new ClientboundSetEntityDataPacket(nmsSpectator.getId(), dirtyData));
						}

						// Send position and rotation
						nmsSpectator.connection.send(new ClientboundPlayerPositionPacket(nmsSpectator.getId(), PositionMoveRotation.of(nmsFake), Set.of()));

						// Sync velocity so client predicts movement correctly between position snaps
						nmsSpectator.setDeltaMovement(nmsFake.getDeltaMovement());
						nmsSpectator.connection.send(new ClientboundSetEntityMotionPacket(nmsSpectator.getId(), nmsFake.getDeltaMovement()));

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
			for(Player otherFake : M7tas.getFakePlayers().values()) {
				if(otherFake.equals(spectatedFake)) continue; // Skip the one being spectated

				double distance = spectatorLocation.distanceSquared(otherFake.getLocation());

				if(distance <= 3) {
					shouldBeHidden.add(otherFake);

					// Hide if not already hidden
					if(!currentlyHidden.contains(otherFake)) {
						Spectate.hideFakePlayerFromSpectator(spectator, otherFake);
					}
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
			Utils.restoreInventory(spectator);
			spectator.removePotionEffect(PotionEffectType.INVISIBILITY);
		}

		Spectate.getSpectatorMap().clear();
		reverseSpectatorMap.clear();
		hiddenFakePlayers.clear();
	}
}