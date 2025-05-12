package instructions;

import net.minecraft.network.protocol.game.PacketPlayOutEntityVelocity;
import net.minecraft.network.protocol.game.PacketPlayOutHeldItemSlot;
import net.minecraft.network.protocol.game.PacketPlayOutSetSlot;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import plugin.M7tas;
import plugin.Utils;

public class Actions {
	/**
	 * Moves a Player in this Direction for t ticks.  The Vector referrs to the number of blocks per tick.
	 *
	 * @param p             The Player
	 * @param perTick       The Distance to be moved per tick
	 * @param durationTicks The total number of Ticks to move
	 */
	public static void move(Player p, Vector perTick, int durationTicks) {
		if(!(p instanceof CraftPlayer cp) || durationTicks <= 0) return;
		EntityPlayer nms = cp.getHandle();
		Vec3D motion = new Vec3D(perTick.getX(), perTick.getY(), perTick.getZ());

		new BukkitRunnable() {
			int ticks = 0;

			@Override
			public void run() {
				if(ticks++ >= durationTicks) {
					cancel();
					return;
				}
				// 1) set the server‐side motion
				nms.i(motion);
				nms.a(EnumMoveType.a, motion);
				nms.h();

				// 2) let each viewer animate that motion
				PacketPlayOutEntityVelocity vel = new PacketPlayOutEntityVelocity(nms);
				for(Player viewer : Bukkit.getOnlinePlayers()) {
					((CraftPlayer) viewer).getHandle().f.b(vel);
				}
				System.out.println(p.getLocation());
			}
		}.runTaskTimer(M7tas.getInstance(), 0, 1);
	}

	/**
	 * 1) Change which hotbar slot (0–8) the fake player is “holding”,
	 * and broadcast that slot change.
	 */
	public static void setFakePlayerHotbarSlot(Player p, int hotbarIndex) {
		EntityPlayer npc = ((CraftPlayer) p).getHandle();
		// 1a) mutate the server‐side pointer
		npc.gi().j = hotbarIndex;
		// 1b) tell clients “this player now has slot X selected”
		PacketPlayOutHeldItemSlot pkt = new PacketPlayOutHeldItemSlot(hotbarIndex);
		Utils.broadcastPacket(pkt);
	}

	/**
	 * 2) Swap two inventory slots in the fake player’s inventory and
	 * immediately broadcast both slot changes.
	 *
	 * @param slotA any slot index (0–8 hotbar, 9–35 main inv, 36–39 armor, 40 offhand)
	 * @param slotB same range
	 */
	public static void swapFakePlayerInventorySlots(Player p, int slotA, int slotB) {
		EntityPlayer npc = ((CraftPlayer) p).getHandle();
		PlayerInventory inv = npc.gi();

		// 2a) swap in the server‐side inventory
		ItemStack a = inv.a(slotA);
		ItemStack b = inv.a(slotB);
		inv.a(slotA, b);
		inv.a(slotB, a);

		// 2b) broadcast the two slot updates
		Container open = npc.cd;  // usually id 0 for player‐inv
		int windowId = open.l;
		int stateId = open.j();

		PacketPlayOutSetSlot pktA = new PacketPlayOutSetSlot(windowId, stateId, slotA, b);
		PacketPlayOutSetSlot pktB = new PacketPlayOutSetSlot(windowId, stateId, slotB, a);
		Utils.broadcastPacket(pktA);
		Utils.broadcastPacket(pktB);
	}

	/**
	 * Simulate a left‐click (attack) in the air.  Your other plugin
	 * will see a PlayerInteractEvent with Action.LEFT_CLICK_AIR.
	 */
	public static void simulateLeftClickAir(Player p) {
		// 1) do the swing animation
		p.swingMainHand();

		// 2) build & fire the same event Bukkit would normally fire
		PlayerInteractEvent ev = new PlayerInteractEvent(
				p,
				Action.LEFT_CLICK_AIR,
				p.getInventory().getItemInMainHand(),
				/* no block */ null,
				/* face unused */ null
		);
		Bukkit.getPluginManager().callEvent(ev);
	}

	/**
	 * Simulate a right‐click (use) in the air.  Your other plugin
	 * will see a PlayerInteractEvent with Action.RIGHT_CLICK_AIR.
	 */
	public static void simulateRightClickAir(Player p) {
		p.swingMainHand();

		PlayerInteractEvent ev = new PlayerInteractEvent(
				p,
				Action.RIGHT_CLICK_AIR,
				p.getInventory().getItemInMainHand(),
				null,
				null
		);
		Bukkit.getPluginManager().callEvent(ev);
	}

	/**
	 * Simulate a left‐click on an entity (attack interact).  Your other plugin
	 * will see a PlayerInteractEntityEvent.
	 */
	public static void simulateLeftClickEntity(Player p, Entity target) {
		p.swingMainHand();

		PlayerInteractEntityEvent ev = new PlayerInteractEntityEvent(
				p,
				target,
				EquipmentSlot.HAND  // MAIN_HAND
		);
		Bukkit.getPluginManager().callEvent(ev);
	}

	/**
	 * Simulate a right‐click on an entity (use/interact).  Your other plugin
	 * will see a PlayerInteractEntityEvent.
	 */
	public static void simulateRightClickEntity(Player p, Entity target) {
		p.swingMainHand();

		PlayerInteractEntityEvent ev = new PlayerInteractEntityEvent(
				p,
				target,
				EquipmentSlot.HAND
		);
		Bukkit.getPluginManager().callEvent(ev);
	}

	/**
	 * If you do want to simulate a block‐click, use the block/face variants:
	 */
	public static void simulateLeftClickBlock(Player p, Block block, BlockFace face) {
		p.swingMainHand();
		PlayerInteractEvent ev = new PlayerInteractEvent(
				p,
				Action.LEFT_CLICK_BLOCK,
				p.getInventory().getItemInMainHand(),
				block,
				face
		);
		Bukkit.getPluginManager().callEvent(ev);
	}

	public static void simulateRightClickBlock(Player p, Block block, BlockFace face) {
		p.swingMainHand();
		PlayerInteractEvent ev = new PlayerInteractEvent(
				p,
				Action.RIGHT_CLICK_BLOCK,
				p.getInventory().getItemInMainHand(),
				block,
				face
		);
		Bukkit.getPluginManager().callEvent(ev);
	}
}