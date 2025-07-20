package plugin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.inventory.Container;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class Utils {
	/**
	 * Wrapper for Bukkit.getScheduler().runTaskLater(Plugin, Runnable, long)
	 *
	 * @param task  The task to run later.
	 * @param delay In how many ticks this task should be run.
	 */
	public static void scheduleTask(Runnable task, long delay) {
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), task, delay);
	}

	public static void teleportWithSpectators(Player p, Location to) {
		if(!(p instanceof CraftPlayer cp)) return;
		EntityPlayer npc = cp.getHandle();

		// Update NMS position
		npc.a_(to.getX(), to.getY(), to.getZ());
		npc.v(to.getYaw());
		npc.w(to.getPitch());
		npc.aZ = to.getYaw();
		npc.aX = to.getYaw();

		// Send packets
		PositionMoveRotation pmr = PositionMoveRotation.a(npc);
		PacketPlayOutEntityTeleport tp = PacketPlayOutEntityTeleport.a(npc.ar(), pmr, EnumSet.noneOf(Relative.class), npc.aJ());

		broadcastPacket(tp);
		updateSpectators(p, pmr);
	}

	public static void updateSpectators(Player fakePlayer, PositionMoveRotation pmr) {
		List<Player> spectators = M7tas.getSpectatingPlayers(fakePlayer);
		if (!spectators.isEmpty()) {
			PacketPlayOutPosition snapCam = new PacketPlayOutPosition(0, pmr, EnumSet.noneOf(Relative.class));

			for (Player spectator : spectators) {
				if (spectator instanceof CraftPlayer craftSpectator) {
					EntityPlayer nmsSpectator = craftSpectator.getHandle();
					PlayerConnection conn = nmsSpectator.f;
					conn.b(snapCam);
				}
			}
		}
	}

	public static void updateSpectators(Player fakePlayer, Location newLocation) {
		List<Player> spectators = M7tas.getSpectatingPlayers(fakePlayer);
		if (!spectators.isEmpty()) {
			for (Player spectator : spectators) {
				spectator.teleport(newLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
			}
		}
	}

	public static void syncSpectatorPosition(Player spectator, Player fakePlayer) {
		if (spectator instanceof CraftPlayer craftSpectator && fakePlayer instanceof CraftPlayer craftFake) {
			EntityPlayer nmsSpectator = craftSpectator.getHandle();
			EntityPlayer nmsFake = craftFake.getHandle();

			// Sync position and rotation exactly
			nmsSpectator.a_(nmsFake.dA(), nmsFake.dC(), nmsFake.dG());
			nmsSpectator.v(nmsFake.dL());
			nmsSpectator.w(nmsFake.dN());

			// Sync velocity for smooth movement
			nmsSpectator.j(nmsFake.dy());

			// Sync fall distance
			nmsSpectator.Z = nmsFake.Z;
		}
	}

	public static void updateSpectatorsWithPhysics(Player fakePlayer, PositionMoveRotation pmr) {
		List<Player> spectators = M7tas.getSpectatingPlayers(fakePlayer);
		if (!spectators.isEmpty()) {
			for (Player spectator : spectators) {
				// Use the new physics sync method
				syncSpectatorPosition(spectator, fakePlayer);

				// Send position packet
				if (spectator instanceof CraftPlayer craftSpectator) {
					EntityPlayer nmsSpectator = craftSpectator.getHandle();
					PlayerConnection conn = nmsSpectator.f;
					PacketPlayOutPosition snapCam = new PacketPlayOutPosition(0, pmr, EnumSet.noneOf(Relative.class));
					conn.b(snapCam);
				}
			}
		}
	}

	/**
	 * Syncs fake player's inventory to spectators using packet-based approach
	 */
	public static void syncInventoryToSpectators(Player fakePlayer) {
		List<Player> spectators = M7tas.getSpectatingPlayers(fakePlayer);
		if (!spectators.isEmpty()) {
			// Method 1: Use packet-based sync (more efficient)
			forceFullInventorySync(fakePlayer, spectators);

			// Method 2: Also sync held item slot specifically
			for (Player spectator : spectators) {
				spectator.getInventory().setHeldItemSlot(fakePlayer.getInventory().getHeldItemSlot());
			}
		}
	}

	/**
	 * Force a full resync of the given player's inventory to specified targets.
	 */
	public static void forceFullInventorySync(Player sourcePlayer, List<Player> targets) {
		CraftPlayer cp = (CraftPlayer) sourcePlayer;
		var handle = cp.getHandle();
		Container menu = handle.cd; // the open window (0 = player inv)

		// Build the packet with the current contents
		PacketPlayOutWindowItems pkt = new PacketPlayOutWindowItems(menu.l, menu.j(), menu.c(), menu.g());

		// Send to specified targets
		for (Player target : targets) {
			if (target instanceof CraftPlayer craftTarget) {
				craftTarget.getHandle().f.b(pkt);
			}
		}
	}

	/**
	 * Updates the held item in an NPC's hand for all viewers AND spectators
	 */
	public static void syncFakePlayerHand(Player fake) {
		EntityPlayer npc = ((CraftPlayer) fake).getHandle();
		// get whatever ItemStack is in their selected slot
		net.minecraft.world.item.ItemStack handStack = npc.gi().a(npc.gi().j);

		// build an equipment packet for MAIN_HAND
		PacketPlayOutEntityEquipment equipPkt = new PacketPlayOutEntityEquipment(npc.ar(), Collections.singletonList(Pair.of(EnumItemSlot.a, handStack)));

		// send it to every real viewer
		broadcastPacket(equipPkt);

		// Also update spectators' held items
		List<Player> spectators = M7tas.getSpectatingPlayers(fake);
		for (Player spectator : spectators) {
			spectator.getInventory().setHeldItemSlot(fake.getInventory().getHeldItemSlot());
			spectator.getInventory().setItemInMainHand(fake.getInventory().getItemInMainHand());
			spectator.updateInventory();
		}
	}

	/**
	 * Sends a packet to every player on the server.
	 *
	 * @param pkt Packet to send
	 */
	public static void broadcastPacket(Packet<?> pkt) {
		for(Player p : Bukkit.getOnlinePlayers()) {
			((CraftPlayer) p).getHandle().f.b(pkt);
		}
	}

	/**
	 * Creates a leather armor item with the specified material, color, and name.
	 *
	 * @param material The material type of the leather armor.
	 * @param color    The color to apply to the leather armor.
	 * @param name     The display name to set for the leather armor.
	 * @return A new ItemStack representing the customized leather armor.
	 */
	public static ItemStack createLeatherArmor(Material material, Color color, String name) {
		ItemStack item = new ItemStack(material);
		LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
		assert meta != null;
		meta.setColor(color);
		meta.setDisplayName(name);
		item.setItemMeta(meta);
		return item;
	}

	public static void backupPlayerInventory(Player player) {
		M7tas.originalInventories.put(player, new M7tas.PlayerInventoryBackup(player));
	}

	public static void restorePlayerInventory(Player player) {
		M7tas.PlayerInventoryBackup backup = M7tas.originalInventories.remove(player);
		if (backup != null) {
			backup.restore(player);
		}
	}

	private static BukkitTask inventorySyncTask;

	public static void startInventorySync() {
		if (inventorySyncTask != null) {
			inventorySyncTask.cancel();
		}

		inventorySyncTask = Bukkit.getScheduler().runTaskTimer(M7tas.getInstance(), () -> {
			// Re-sync all spectators' inventories every 10 ticks
			for (Player spectator : M7tas.getSpectatorMap().keySet()) {
				Player fakePlayer = M7tas.getSpectatorMap().get(spectator);
				if (fakePlayer != null) {
					syncInventoryToSpectators(fakePlayer);
				}
			}
		}, 0L, 1L);
	}

	public static void stopInventorySync() {
		if (inventorySyncTask != null) {
			inventorySyncTask.cancel();
			inventorySyncTask = null;
		}
	}

	/**
	 * Plays a sound for every player on the server
	 *
	 * @param s The sound to play
	 */
	@SuppressWarnings("unused")
	public static void playGlobalSound(Sound s) {
		Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, s, 1.0F, 1.0F));
	}

	/**
	 * Plays a sound for every player on the server
	 *
	 * @param s      The sound to play
	 * @param volume The volume of the sound
	 * @param pitch  The pitch of the sound
	 */
	public static void playGlobalSound(Sound s, float volume, float pitch) {
		Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player, s, volume, pitch));
	}
}