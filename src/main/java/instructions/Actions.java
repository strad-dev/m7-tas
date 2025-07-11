package instructions;

import net.minecraft.network.protocol.game.PacketPlayOutEntityTeleport;
import net.minecraft.network.protocol.game.PacketPlayOutHeldItemSlot;
import net.minecraft.network.protocol.game.PacketPlayOutPosition;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
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
import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
public class Actions {
	/**
	 * Moves a Player in this Direction for t ticks.  The Vector referrs to the number of blocks per tick.
	 * The Y component of the vector is ignored.  Vertical motion is left to gravity or other methods.
	 *
	 * @param p             The Player
	 * @param perTick       The Distance to be moved per tick
	 * @param durationTicks The total number of Ticks to move
	 */
	public static void move(Player p, Vector perTick, int durationTicks) {
		// only handle CraftPlayer/NMS and positive duration
		if(!(p instanceof CraftPlayer cp) || durationTicks <= 0) return;

		EntityPlayer npc = cp.getHandle();
		// convert Bukkit Vector to NMS Vec3D
		Vec3D motion = new Vec3D(perTick.getX(), 0, perTick.getZ());

		new BukkitRunnable() {
			int ticks = 0;

			@Override
			public void run() {
				if(ticks++ >= durationTicks) {
					cancel();
					return;
				}

				// Retain current Y motion (from gravity, jump, etc.)
				Vec3D currentMotion = npc.dy(); // Get current velocity
				Vec3D combined = new Vec3D(motion.d, currentMotion.e, motion.f);

				// 1) Apply vanilla movement & collision (handles stairs/slabs, walls, etc.)
				npc.a(EnumMoveType.a, combined);
				// 2) Apply gravity, friction, fall damage checks, etc.
				npc.h();

				// 3) Package up the new position + rotation
				PositionMoveRotation pmr = PositionMoveRotation.a(npc);

				// 4) Create teleport packet with NO relative flags
				PacketPlayOutEntityTeleport tp = PacketPlayOutEntityTeleport.a(npc.ar(), pmr, EnumSet.noneOf(Relative.class), npc.aJ());

				// 5) Broadcast to all online players (including spectators)
				Utils.broadcastPacket(tp);

				// Send position to all spectators
				List<Player> spectators = M7tas.getSpectatingPlayer(p); // p is the fake player
				if(!spectators.isEmpty()) {
					PacketPlayOutPosition snapCam = new PacketPlayOutPosition(0, pmr, EnumSet.noneOf(Relative.class));

					for(Player spectator : spectators) {
						if(spectator instanceof CraftPlayer craftSpectator) {
							EntityPlayer nmsSpectator = craftSpectator.getHandle();
							PlayerConnection conn = nmsSpectator.f;
							conn.b(snapCam);
						}
					}
				}
			}
			// schedule at 0 tick delay, repeating every tick
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	/**
	 * Makes a Player entity perform a jump if it is currently on the ground.
	 *
	 * @param p The Player entity that is to perform the jump. Requires the player to be on the ground.
	 */
	public static void jump(Player p) {
		if(!(p instanceof CraftPlayer cp)) return;

		EntityPlayer npc = cp.getHandle();

		if(npc.aJ()) { // onGround check
			Vec3D motion = npc.dy();
			Vec3D newMotion = new Vec3D(motion.d, 0.42D, motion.f);

			// Apply jump physics
			npc.j(newMotion);
			npc.a(EnumMoveType.a, newMotion);
			npc.h();

			// Send packets using unified method
			PositionMoveRotation pmr = PositionMoveRotation.a(npc);
			PacketPlayOutEntityTeleport tp = PacketPlayOutEntityTeleport.a(npc.ar(), pmr, EnumSet.noneOf(Relative.class), npc.aJ());

			Utils.broadcastPacket(tp);
			Utils.updateSpectators(p, pmr); // Use the unified method
		}
	}

	/**
	 * Change which hotbar slot (0–8) the fake player is “holding”,
	 * and broadcast that slot change.
	 *
	 * @param p           The player whose hotbar slot is to be changed.
	 * @param hotbarIndex The index of the new hotbar slot (0–8).
	 */
	public static void setFakePlayerHotbarSlot(Player p, int hotbarIndex) {
		if(!(p instanceof CraftPlayer cp)) return;
		EntityPlayer npc = cp.getHandle();

		// Update the NMS held-slot index
		npc.gi().j = hotbarIndex;

		// Tell that player's client "your held slot is now hotbarIndex"
		PacketPlayOutHeldItemSlot heldPkt = new PacketPlayOutHeldItemSlot(hotbarIndex);
		cp.getHandle().f.b(heldPkt);

		// Broadcast the new main-hand item to all viewers AND sync to spectators
		Utils.syncFakePlayerHand(p); // This now handles both!
	}

	/**
	 * Swap two inventory slots in the fake player’s inventory and
	 * immediately broadcast both slot changes.
	 *
	 * @param slotA any slot index (0–8 hotbar, 9–35 main inv, 36–39 armor, 40 offhand)
	 * @param slotB same range
	 */
	public static void swapFakePlayerInventorySlots(Player p, int slotA, int slotB) {
		if(!(p instanceof CraftPlayer cp)) return;
		EntityPlayer npc = cp.getHandle();
		PlayerInventory inv = npc.gi();

		// Swap internally
		net.minecraft.world.item.ItemStack a = inv.a(slotA);
		net.minecraft.world.item.ItemStack b = inv.a(slotB);
		inv.a(slotA, b);
		inv.a(slotB, a);

		// Re-send the entire inventory window to fake player AND spectators
		Utils.syncInventoryToSpectators(p); // This updates spectators

		// If either swapped slot was in the hotbar, update the hand-item too
		if(slotA < 9 || slotB < 9) {
			Utils.syncFakePlayerHand(p); // This now handles both viewers and spectators
		}
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
		Location to = p.getLocation();
		to.setYaw(yaw);
		to.setPitch(pitch);

		Utils.teleportWithSpectators(p, to);
	}

	/**
	 * Simulates the teleportation of a Player to a specified location while preserving
	 * their orientation (yaw and pitch). This method resets the player's velocity,
	 * adjusts their yaw and pitch to match the origin location, and teleports any spectator
	 * viewers who are spectating the player to the same destination. A teleportation sound
	 * effect is played at the player's location.
	 *
	 * @param p  The player to be teleported and simulated.
	 * @param to The target location to which the player will be teleported.
	 */
	public static void simulateEtherwarp(Player p, Location to) {
		Location from = p.getLocation();
		to.setYaw(from.getYaw());
		to.setPitch(from.getPitch());

		p.setVelocity(new Vector(0, 0, 0));
		Utils.teleportWithSpectators(p, to);

		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, 1, 0.50F);
	}

	/**
	 * Simulates the teleportation of a Player to a specified location while preserving
	 * their orientation (yaw and pitch). Additionally, resets the player's velocity
	 * and teleports any spectator viewers following the player to the same location.
	 * A teleportation sound effect is played at the player's location.
	 * <p>
	 * This function is also used when an Ender Pearl lands.
	 *
	 * @param p  The player to be teleported and simulated.
	 * @param to The target location to which the player will be teleported.
	 */
	public static void simulateAOTV(Player p, Location to) {
		Location from = p.getLocation();
		to.setYaw(from.getYaw());
		to.setPitch(from.getPitch());

		p.setVelocity(new Vector(0, 0, 0));
		Utils.teleportWithSpectators(p, to);

		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
	}

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
		BlockData blockdata = b.getBlockData();
		b.setType(Material.AIR);
		Utils.scheduleTask(() -> {
			b.setType(material);
			b.setBlockData(blockdata);
		}, 5);
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
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2 + " " + z2 + " minecraft:air replace minecraft:cracked_stone_bricks");
		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2.0F, 1.0F);
		Utils.scheduleTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fill " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2 + " " + z2 + " minecraft:cracked_stone_bricks replace minecraft:air"), 21);
	}

	/**
	 * Simulates a "Blow Up Crypt" action within a specified cuboid area. The method performs
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
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "clone " + x1 + " " + 0 + " " + z1 + " " + x2 + " " + Math.abs(y2 - y1) + " " + z2 + " " + Math.min(x1, x2) + " " + Math.min(y1, y2) + " " + Math.min(z1, z2));
			try {
				zombie.remove();
			} catch(Exception exception) {
				// nothing here
			}
		}, 21);
	}

	public static void simulateLeap(Player p, Player target) {
		Location targetLoc = target.getLocation();
		p.setVelocity(new Vector(0, 0, 0));
		Utils.teleportWithSpectators(p, targetLoc);

		p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
	}

	public static void mimicChest(Player p, Block b) {
		simulateStonking(p, b);

		Zombie zombie = (Zombie) p.getWorld().spawnEntity(b.getLocation().add(0.5, 0, 0.5), EntityType.ZOMBIE);
		zombie.setCustomName("Mimic " + ChatColor.RESET + ChatColor.RED + "❤ " + ChatColor.YELLOW + 2 + "/" + 2);
		zombie.setCustomNameVisible(true);
		zombie.setAI(false);
		zombie.setSilent(true);
		zombie.setBaby();
		Objects.requireNonNull(zombie.getAttribute(Attribute.ARMOR)).setBaseValue(-2);
		Objects.requireNonNull(zombie.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(2);
		zombie.setHealth(2);

		Utils.scheduleTask(() -> {
			try {
				zombie.remove();
			} catch(Exception exception) {
				// nothing here
			}
		}, 21);
	}

	/**
	 * Simulates the action of a player throwing a pearl in the direction they are facing.
	 * This method is typically used for replicating the behavior of an ender pearl throw,
	 * including potentially initiating movement or teleportation.
	 *
	 * @param p The player for whom the pearl throw is being simulated.
	 */
	public static void simulatePearlThrow(Player p) {
		EnderPearl pearl = (EnderPearl) p.getWorld().spawnEntity(p.getEyeLocation(), EntityType.ENDER_PEARL);
		pearl.setGravity(true);
		pearl.setShooter(null);
		pearl.setVelocity(p.getLocation().getDirection().normalize().multiply(3)); // Normal pearl speed is ~3.0

		Actions.simulateLeftClickAir(p);
		p.getInventory().getItemInMainHand().setAmount(p.getInventory().getItemInMainHand().getAmount() - 1);
	}

	public static void simulateRagAxe(Player p) {
		p.getWorld().playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
		Utils.scheduleTask(() -> p.getWorld().playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F), 20);
		Utils.scheduleTask(() -> p.getWorld().playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F), 40);
		Utils.scheduleTask(() -> {
			p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WOLF_DEATH, 1.0F, 1.0F);
			p.addScoreboardTag("RagBuff");
		}, 60);
		Utils.scheduleTask(() -> p.removeScoreboardTag("RagBuff"), 260);
	}

	/**
	 * Simulate a left‐click (attack) in the air.  Your other plugin
	 * will see a PlayerInteractEvent with Action.LEFT_CLICK_AIR.
	 */
	@SuppressWarnings("ConstantConditions")
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
	@SuppressWarnings("ConstantConditions")
	public static void simulateRightClickAir(Player p) {
		PlayerInteractEvent ev = new PlayerInteractEvent(p, Action.RIGHT_CLICK_AIR, p.getInventory().getItemInMainHand(), null, null);
		Bukkit.getPluginManager().callEvent(ev);
	}
}