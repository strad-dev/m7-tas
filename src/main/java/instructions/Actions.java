package instructions;

import net.minecraft.network.protocol.game.PacketPlayOutEntityTeleport;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import plugin.M7tas;
import plugin.Utils;

import java.util.EnumSet;
import java.util.Objects;

@SuppressWarnings("unused")
public class Actions {
	/**
	 * Moves a Player in this Direction for t ticks.  The Vector referrs to the number of blocks per tick.
	 *
	 * @param p             The Player
	 * @param perTick       The Distance to be moved per tick
	 * @param durationTicks The total number of Ticks to move
	 */
	public static void move(Player p, Vector perTick, int durationTicks) {
		// only handle CraftPlayer/NMS and positive duration
		if (!(p instanceof CraftPlayer cp) || durationTicks <= 0) return;

		EntityPlayer npc = cp.getHandle();
		// convert Bukkit Vector to NMS Vec3D
		Vec3D motion = new Vec3D(perTick.getX(), perTick.getY(), perTick.getZ());

		new BukkitRunnable() {
			int ticks = 0;
			@Override
			public void run() {
				if (ticks++ >= durationTicks) {
					cancel();
					return;
				}

				// 1) Apply vanilla movement & collision (handles stairs/slabs, walls, etc.)
				npc.a(EnumMoveType.a, motion);
				// 2) Apply gravity, friction, fall damage checks, etc.
				npc.h();

				// 3) Package up the new position + rotation
				PositionMoveRotation pmr = PositionMoveRotation.a(npc);

				// 4) Create teleport packet with NO relative flags
				PacketPlayOutEntityTeleport tp = PacketPlayOutEntityTeleport.a(
						npc.ar(),
						pmr,
						EnumSet.noneOf(Relative.class),
						npc.aJ()  // head yaw/pitch
				);

				// 5) Broadcast to all online players (including spectators)
				Utils.broadcastPacket(tp);
			}
			// schedule at 0 tick delay, repeating every tick
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}


	/**
	 * Change which hotbar slot (0–8) the fake player is “holding”,
	 * and broadcast that slot change.
	 *
	 * @param p           The player whose hotbar slot is to be changed.
	 * @param hotbarIndex The index of the new hotbar slot (0–8).
	 */
	public static void setFakePlayerHotbarSlot(Player p, int hotbarIndex) {
		Utils.forceFullSync(p, ep -> ep.gi().j = hotbarIndex);
	}

	/**
	 * Swap two inventory slots in the fake player’s inventory and
	 * immediately broadcast both slot changes.
	 *
	 * @param slotA any slot index (0–8 hotbar, 9–35 main inv, 36–39 armor, 40 offhand)
	 * @param slotB same range
	 */
	public static void swapFakePlayerInventorySlots(Player p, int slotA, int slotB) {
		Utils.forceFullSync(p, ep -> {
			EntityPlayer npc = ((CraftPlayer) p).getHandle();
			PlayerInventory inv = npc.gi();
			ItemStack a = inv.a(slotA);
			ItemStack b = inv.a(slotB);
			inv.a(slotA, b);
			inv.a(slotB, a);
		});
	}

	/**
	 * Rotates a player’s body & head to the given yaw/pitch
	 * and broadcasts that new orientation to every real client.
	 * <p>
	 * Works on both real and fake Players (CraftPlayer instances).
	 *
	 * @param p     the Bukkit Player (real or NPC)
	 * @param yaw   body + head yaw, in degrees (0 = south, 90 = west…)
	 * @param pitch look pitch, in degrees (–90 = straight up, +90 = straight down)
	 */
	public static void turnHead(Player p, float yaw, float pitch) {
		Utils.forceFullSync(p, ep -> {
			ep.v(yaw);
			ep.aZ = yaw;
			ep.w(pitch);
		});
	}

	/**
	 * Simulates an etherwarp for the specified player. The player is teleported to the
	 * provided location, their velocity is reset, and a sound effect is played.
	 *
	 * @param p  The player to simulate the etherwarp for.
	 * @param to The target location to which the player will be teleported.
	 */
	public static void simulateEtherwarp(Player p, Location to) {
		Location from = p.getLocation();
		to.setYaw(from.getYaw());
		to.setPitch(from.getPitch());

		p.setVelocity(new Vector(0, 0, 0));
		Utils.forceInstantTeleport(p, to);

		for (Player viewer : Bukkit.getOnlinePlayers()) {
			if (viewer.getGameMode() == GameMode.SPECTATOR
					&& viewer.getSpectatorTarget() != null
					&& viewer.getSpectatorTarget().equals(p)) {

				Utils.forceInstantTeleport(viewer, to);
			}
		}

		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, 1, 0.50F);
	}

	/**
	 * Simulates an instant teleportation similar to an ability or action performed
	 * with an "Aspect of the Void" (AOTV) item. The method resets the player's
	 * velocity, teleports them to the specified location, and plays a teleport
	 * sound effect.
	 */
	public static void simulateAOTV(Player p, Location to) {
		Location from = p.getLocation();
		to.setYaw(from.getYaw());
		to.setPitch(from.getPitch());

		p.setVelocity(new Vector(0, 0, 0));
		Utils.forceInstantTeleport(p, to);

		for (Player viewer : Bukkit.getOnlinePlayers()) {
			if (viewer.getGameMode() == GameMode.SPECTATOR
					&& viewer.getSpectatorTarget() != null
					&& viewer.getSpectatorTarget().equals(p)) {

				Utils.forceInstantTeleport(viewer, to);
			}
		}

		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
	}

	/* private static void wipeNmsVelocity(Player p) {
		EntityPlayer ep = ((CraftPlayer) p).getHandle();

		// a) zero server‐side velocity
		ep.i(Vec3D.c);   // (0,0,0)

		// b) clear fall distance so no “bounce”
		ep.Z = 0;

		// c) tell all clients to drop their velocity too
		PacketPlayOutEntityVelocity vel = new PacketPlayOutEntityVelocity(ep);
		Utils.broadcastPacket(vel);
	} */

	/**
	 * Simulates the "stonking" action on a given block. This involves temporarily
	 * changing the block to an AIR block and then resetting it to its original
	 * material type after a short delay.
	 *
	 * @param b The Block to simulate the stonking action on.
	 */
	public static void simulateStonking(Player p, Block b) {
		p.swingMainHand();
		p.getWorld().playSound(p.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0F, 1.0F);
		Material material = b.getType();
		b.setType(Material.AIR);
		Utils.scheduleTask(() -> b.setType(material), 4);
	}

	/**
	 * Simulates a "Superboom" explosion action within a specified cuboid area.
	 * The method triggers a left-click air interaction, clears the specified area by
	 * filling it with air, plays an explosion sound for the player, and then fills
	 * the area with cracked stone bricks after a short delay.
	 *
	 * @param p  The player for whom the superboom simulation is performed.
	 * @param x1 The x-coordinate of the first corner of the cuboid area.
	 * @param y1 The y-coordinate of the first corner of the cuboid area.
	 * @param z1 The z-coordinate of the first corner of the cuboid area.
	 * @param x2 The x-coordinate of the opposite corner of the cuboid area.
	 * @param y2 The y-coordinate of the opposite corner of the cuboid area.
	 * @param z2 The z-coordinate of the opposite corner of the cuboid area.
	 */
	public static void simulateSuperboom(Player p, int x1, int y1, int z1, int x2, int y2, int z2) {
		Actions.simulateLeftClickAir(p);
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2 + " " + z2 + " minecraft:air");
		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 1.0F);
		Utils.scheduleTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2 + " " + z2 + " minecraft:cracked_stone_bricks"), 20);
	}

	/**
	 * Simulates a "Crypt" action within a specified cuboid area. The method performs
	 * a sequence of operations: simulating a left-click air interaction, clearing
	 * the specified area by filling it with air, playing a sound effect, and then
	 * cloning a specified structure after a short delay.
	 *
	 * @param p  The player for whom the crypt simulation is performed.
	 * @param x1 The x-coordinate of the first corner of the cuboid area.
	 * @param y1 The y-coordinate of the first corner of the cuboid area.
	 * @param z1 The z-coordinate of the first corner of the cuboid area.
	 * @param x2 The x-coordinate of the opposite corner of the cuboid area.
	 * @param y2 The y-coordinate of the opposite corner of the cuboid area.
	 * @param z2 The z-coordinate of the opposite corner of the cuboid area.
	 */
	public static void simulateCrypt(Player p, int x1, int y1, int z1, int x2, int y2, int z2) {
		Actions.simulateLeftClickAir(p);
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2 + " " + z2 + " minecraft:air");
		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 1.0F);
		Zombie zombie = (Zombie) p.getWorld().spawnEntity(new Location(p.getWorld(), Math.min(x1, x2), y1, Math.min(z1, z2)), EntityType.ZOMBIE);
		zombie.setCustomName("Crypt Undead " + ChatColor.RESET + ChatColor.RED + "❤ " + ChatColor.YELLOW + 1 + "/" + 1);
		zombie.setCustomNameVisible(true);
		zombie.setAI(false);
		zombie.setSilent(true);
		zombie.setAdult();
		Objects.requireNonNull(zombie.getAttribute(Attribute.ARMOR)).setBaseValue(-2);
		Objects.requireNonNull(zombie.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(1);
		zombie.setHealth(1);

		assert zombie.getEquipment() != null;
		zombie.getEquipment().setItemInMainHand(new org.bukkit.inventory.ItemStack(Material.BONE));
		Utils.scheduleTask(() -> {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "clone " + x1 + " " + 0 + " " + z1 + " " + x2 + " " + (y2 - y1) + " " + z2 + " " + Math.min(x1, x2) + " " + Math.min(y1, y2) + " " + Math.min(z1, z2));
			try {
				zombie.remove();
			} catch(Exception exception) {
				// nothing here
			}
		}, 20);
	}

	/**
	 * Simulates the action of a player throwing a pearl in the direction they are facing.
	 * This method is typically used for replicating the behavior of an ender pearl throw,
	 * including potentially initiating movement or teleportation.
	 *
	 * @param p       The player for whom the pearl throw is being simulated.
	 */
	public static void simulatePearlThrow(Player p) {
		EnderPearl pearl = (EnderPearl) p.getWorld().spawnEntity(p.getEyeLocation(), EntityType.ENDER_PEARL);
		pearl.setGravity(false);
		pearl.setShooter(null);
		pearl.setVelocity(p.getLocation().getDirection().normalize().multiply(1.5)); // Normal pearl speed is ~3.0
	}

	/**
	 * Simulate a left‐click (attack) in the air.  Your other plugin
	 * will see a PlayerInteractEvent with Action.LEFT_CLICK_AIR.
	 */
	public static void simulateLeftClickAir(Player p) {
		// 1) do the swing animation
		p.swingMainHand();

		// 2) build & fire the same event Bukkit would normally fire
		PlayerInteractEvent ev = new PlayerInteractEvent(p, Action.LEFT_CLICK_AIR, p.getInventory().getItemInMainHand(),
				/* no block */ null,
				/* face unused */ null);
		Bukkit.getPluginManager().callEvent(ev);
	}

	/**
	 * Simulate a right‐click (use) in the air.  Your other plugin
	 * will see a PlayerInteractEvent with Action.RIGHT_CLICK_AIR.
	 */
	public static void simulateRightClickAir(Player p) {
		PlayerInteractEvent ev = new PlayerInteractEvent(p, Action.RIGHT_CLICK_AIR, p.getInventory().getItemInMainHand(), null, null);
		Bukkit.getPluginManager().callEvent(ev);
	}
}