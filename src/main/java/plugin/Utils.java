package plugin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.inventory.Container;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_21_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.function.Consumer;

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

	/**
	 * Forces a complete resynchronization of the specified player's data, including
	 * metadata, hand items, rotation, and open inventory window. This method allows
	 * applying custom modifications to the player's NMS (server-side) entity before
	 * triggering the sync actions.
	 *
	 * @param bukkit    The Bukkit player whose data will be resynchronized.
	 * @param nmsChange A consumer for applying custom modifications to the player's
	 *                  NMS entity (EntityPlayer) before the synchronization is performed.
	 */
	public static void forceFullSync(Player bukkit, Consumer<EntityPlayer> nmsChange) {
		if(!(bukkit instanceof CraftPlayer)) return;
		EntityPlayer nms = ((CraftPlayer) bukkit).getHandle();

		// 1) apply your NMS mutation
		nmsChange.accept(nms);

		// 2) push out all four sync channels:
		syncFakePlayerHand(bukkit);
		forceRotationSync(bukkit);
		forceFullInventorySync(bukkit);

		PacketPlayOutHeldItemSlot held = new PacketPlayOutHeldItemSlot(nms.gi().j);
		((CraftPlayer) bukkit).getHandle().f.b(held);
	}

	/**
	 * Force a full resync of the given player's open inventory window.
	 */
	public static void forceFullInventorySync(Player player) {
		CraftPlayer cp = (CraftPlayer) player;
		var handle = cp.getHandle();                       // NMS EntityPlayer
		Container menu = handle.cd; // the open window (0 = player inv)

		// Build the packet with the current contents
		PacketPlayOutWindowItems pkt = new PacketPlayOutWindowItems(menu.l, menu.j(), menu.c(), menu.g());

		// Send it only to this player's connection
		handle.f.b(pkt);
	}

	/**
	 * Updates the held item in an NPC's hand
	 */
	public static void syncFakePlayerHand(Player fake) {
		EntityPlayer npc = ((CraftPlayer) fake).getHandle();
		// get whatever ItemStack is in their selected slot
		net.minecraft.world.item.ItemStack handStack = npc.gi().a(npc.gi().j);

		// build an equipment packet for MAIN_HAND
		PacketPlayOutEntityEquipment equipPkt = new PacketPlayOutEntityEquipment(npc.ar(), Collections.singletonList(Pair.of(EnumItemSlot.a, handStack)));

		// send it to every real viewer
		broadcastPacket(equipPkt);
	}
	
	/*
	 * Forces a metadata synchronization for the specified entity player.
	 * This method ensures that all metadata entries are marked and sent to all online players.
	 *
	 * @param nms the server-side player entity for which metadata needs to be synchronized
	 */
	/* public static void forceMetadataSync(EntityPlayer nms) {
		// 1) grab the data-watcher
		DataWatcher dw = nms.au();

		byte oldFlags = dw.a(SHARED_FLAGS);
		byte newFlags = nms.cd()
				? (byte)(oldFlags | 0x02)
				: (byte)(oldFlags & ~0x02);
		dw.a(SHARED_FLAGS, newFlags, true);

		// 2) pull only the *dirty* entries via packDirty() -> aks.b()
		List<DataWatcher.c<?>> entries = dw.b();

		// 3) if nothing changed, bail out
		assert entries != null;
		if (entries.isEmpty()) return;

		// 4) build & broadcast the metadata packet
		PacketPlayOutEntityMetadata pkt =
				new PacketPlayOutEntityMetadata(nms.ar(), entries);
		broadcastPacket(pkt);

		// 5) mark these entries as “sent” so they’re no longer dirty -> assignValues() -> aks.a()
		dw.a(entries);
	} */

	/**
	 * Forces a synchronization of the player's rotation across all online players.
	 * This method sends a teleport-style packet that updates the yaw and pitch of the specified player.
	 *
	 * @param bukkit The Bukkit player whose rotation will be synchronized.
	 */
	public static void forceRotationSync(Player bukkit) {
		CraftPlayer cp = (CraftPlayer) bukkit;
		EntityPlayer nms = cp.getHandle();

		// build a teleport‐style packet carrying the new yaw/pitch (and position)
		PositionMoveRotation pmr = PositionMoveRotation.a(nms);
		PacketPlayOutEntityTeleport tp = PacketPlayOutEntityTeleport.a(nms.ar(), pmr, EnumSet.noneOf(Relative.class), nms.aJ());

		// send to only the real connections
		broadcastPacket(tp);
	}

	public static void forceInstantTeleport(Player bukkit, Location to) {
		CraftPlayer p = (CraftPlayer) bukkit;
		EntityPlayer nms = p.getHandle();
		WorldServer world = ((CraftWorld) bukkit.getWorld()).getHandle();

		// 1) Update the server‐side EntityPlayer position & rotation immediately
		nms.teleportTo(world, to.getX(), to.getY(), to.getZ(), EnumSet.noneOf(Relative.class), to.getYaw(), to.getPitch(), false, PlayerTeleportEvent.TeleportCause.PLUGIN);
		nms.v(to.getYaw());
		nms.w(to.getPitch());
		nms.aZ = to.getYaw();
		nms.aX = to.getYaw();

		PositionMoveRotation pmr = PositionMoveRotation.a(nms);

		PacketPlayOutEntityTeleport tp = PacketPlayOutEntityTeleport.a(nms.ar(), pmr, EnumSet.noneOf(Relative.class), nms.aJ());

		broadcastPacket(tp);

		PacketPlayOutPosition snapCam = new PacketPlayOutPosition(0, pmr, EnumSet.noneOf(Relative.class));

		for(Player viewer : Bukkit.getOnlinePlayers()) {
			if(viewer.getGameMode() == GameMode.SPECTATOR && Objects.equals(viewer.getSpectatorTarget(), bukkit)) {
				EntityPlayer nmsViewer = ((CraftPlayer) viewer).getHandle();
				PlayerConnection conn = nmsViewer.f;
				conn.b(snapCam);
			}
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