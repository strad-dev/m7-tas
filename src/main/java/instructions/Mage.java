package instructions;

import instructions.bosses.Maxor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import plugin.Utils;

import java.util.ArrayList;
import java.util.Objects;

public class Mage {
	private static Player mage;
	private static World world;

	public static void mageInstructions(Player p, String section) {
		mage = p;
		world = Mage.mage.getWorld();

		if(section.equals("all") || section.equals("clear")) {
			Actions.teleport(Mage.mage, new Location(world, -132.5, 69, -76.5, -180f, 0f));
			Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(mage, 2, 29), 60);
			Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 2), 61);
			Utils.scheduleTask(() -> Actions.simulateRightClickAirWithSpectators(mage), 101);
			Utils.scheduleTask(() -> Actions.move(mage, new Vector(0, 0, 0.8634), 5), 102);
			Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 4), 121);
			Utils.scheduleTask(() -> {
				Actions.teleport(mage, new Location(mage.getWorld(), -120.5, 75, -220.5));
				Actions.swapFakePlayerInventorySlots(mage, 2, 29);
			}, 141);
			// Tick 160 (clear tick 0: run begins)
			// Tick 161 (clear tick 1: teleport back) - watcher sequence begins
			Utils.scheduleTask(() -> clear(section.equals("all")), 162);
		} else if(section.equals("maxor") || section.equals("boss")) {
			Actions.teleport(mage, new Location(world, 73.5, 221, 13.5));
			Actions.swapFakePlayerInventorySlots(mage, 9, 39);
			Actions.swapFakePlayerInventorySlots(mage, 10, 36);
			Actions.swapFakePlayerInventorySlots(mage, 1, 28);
			Actions.swapFakePlayerInventorySlots(mage, 3, 30);
			Actions.swapFakePlayerInventorySlots(mage, 5, 32);
			if(section.equals("maxor")) {
				Utils.scheduleTask(() -> maxor(false), 60);
			} else {
				Utils.scheduleTask(() -> maxor(true), 60);
			}
		}
	}

	private static void clear(boolean doContinue) {
		/*
		 * ██████╗ ██╗      ██████╗  ██████╗ ██████╗     ██████╗ ██╗   ██╗███████╗██╗  ██╗
		 * ██╔══██╗██║     ██╔═══██╗██╔═══██╗██╔══██╗    ██╔══██╗██║   ██║██╔════╝██║  ██║
		 * ██████╔╝██║     ██║   ██║██║   ██║██║  ██║    ██████╔╝██║   ██║███████╗███████║
		 * ██╔══██╗██║     ██║   ██║██║   ██║██║  ██║    ██╔══██╗██║   ██║╚════██║██╔══██║
		 * ██████╔╝███████╗╚██████╔╝╚██████╔╝██████╔╝    ██║  ██║╚██████╔╝███████║██║  ██║
		 * ╚═════╝ ╚══════╝ ╚═════╝  ╚═════╝ ╚═════╝     ╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚═╝  ╚═╝
		 */
		// Tick 162 (clear tick 5, delay = 3)
		Utils.scheduleTask(() -> Actions.simulateLeap(mage, Archer.getArcher()), 3);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 3.4f, -3.4f);
			Actions.setFakePlayerHotbarSlot(mage, 1);
		}, 4);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(mage, new Location(world, -120.5, 74, -154.5)), 5);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 0f, 13.4f), 6);
		Utils.scheduleTask(() -> Actions.simulateAOTV(mage, new Location(world, -120.5, 71.21667, -142.83)), 7);
		Utils.scheduleTask(() -> Actions.simulateAOTV(mage, new Location(world, -120.5, 71, -138.5)), 8);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -6.9f, 8.6f);
			Actions.simulateLeftClickAir(mage);
			Server.openWitherDoor();
		}, 9);
		// tick 10: open inventory
		Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(mage, 4, 31), 11);
		// tick 12: close inventory
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(mage, new Location(world, -119.5, 69, -127.5)), 29);

		/*
		 * ██████╗ ███████╗ █████╗ ████████╗██╗  ██╗███╗   ███╗██╗████████╗███████╗
		 * ██╔══██╗██╔════╝██╔══██╗╚══██╔══╝██║  ██║████╗ ████║██║╚══██╔══╝██╔════╝
		 * ██║  ██║█████╗  ███████║   ██║   ███████║██╔████╔██║██║   ██║   █████╗
		 * ██║  ██║██╔══╝  ██╔══██║   ██║   ██╔══██║██║╚██╔╝██║██║   ██║   ██╔══╝
		 * ██████╔╝███████╗██║  ██║   ██║   ██║  ██║██║ ╚═╝ ██║██║   ██║   ███████╗
		 * ╚═════╝ ╚══════╝╚═╝  ╚═╝   ╚═╝   ╚═╝  ╚═╝╚═╝     ╚═╝╚═╝   ╚═╝   ╚══════╝
		 */
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -31, 15);
			Actions.setFakePlayerHotbarSlot(mage, 7);
		}, 30);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(mage), 31);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 45.1f, 6f);
			Actions.setFakePlayerHotbarSlot(mage, 1);
		}, 32);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(mage, new Location(world, -130.5, 69, -116.5)), 33);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 0f, 0f);
			Actions.setFakePlayerHotbarSlot(mage, 3);
		}, 34);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(mage, -131, 69, -116, -130, 72, -115), 35);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 1), 36);
		Utils.scheduleTask(() -> Actions.simulateAOTV(mage, new Location(world, -130.5, 69, -111.5)), 37);
		Utils.scheduleTask(() -> {
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Deathmite 1/6 (Obtained Item)");
			world.playSound(mage.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
		}, 38);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -124f, -78f);
			Actions.setFakePlayerHotbarSlot(mage, 7);
			Actions.simulateAOTV(mage, new Location(world, -114.535, 67, -119.218));
		}, 39);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(mage), 40);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -15.3f, -5.2f);
			Actions.setFakePlayerHotbarSlot(mage, 1);
		}, 41);
		Utils.scheduleTask(() -> Actions.simulateAOTV(mage, new Location(world, -111.387, 69, -107.693)), 42);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, 76.4f), 43);
		Utils.scheduleTask(() -> Actions.simulateAOTV(mage, new Location(world, -109.5, 60, -107.5)), 44);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 171f, 18f), 45);
		Utils.scheduleTask(() -> Actions.simulateAOTV(mage, new Location(world, -110.5, 60, -112.5)), 46);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(mage);
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Deathmite 2/6 (Opened Chest)");
			world.playSound(mage.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 47);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 90f, -90f), 48);
		Utils.scheduleTask(() -> Actions.simulateAOTV(mage, new Location(world, -112.3, 78.2, -120.7)), 50);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(mage, new Location(world, -112.5, 81, -120.5)), 51);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, 0f), 52);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(mage);
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Deathmite 3/6 (Opened Chest)");
			world.playSound(mage.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 53);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 100.8f, 3f), 54);
		Utils.scheduleTask(() -> Actions.simulateAOTV(mage, new Location(world, -124.5, 82, -122.5)), 55);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -164.2f, 12.9f), 56);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(mage, new Location(world, -122.5, 82, -129.5)), 57);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -90f, 4f);
			Actions.setFakePlayerHotbarSlot(mage, 7);
		}, 58);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(mage), 59);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 124.1f, -63.5f);
			Actions.setFakePlayerHotbarSlot(mage, 1);
		}, 60);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(mage, new Location(world, -125.5, 92, -131.5)), 61);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -133.7f, 39.3f), 62);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(mage);
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Deathmite 4/6 (Opened Chest)");
			world.playSound(mage.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 63);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -90f, 60f);
			Actions.setFakePlayerHotbarSlot(mage, 0);
		}, 64);
		Utils.scheduleTask(() -> {
			Actions.simulateWitherImpact(mage, new Location(world, -124.5, 92, -131.5));
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Deathmte 5/6 (Killed Bat)");
			world.playSound(mage.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0F, 1.0F);
		}, 65);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -84.6f, -4.9f);
			Actions.setFakePlayerHotbarSlot(mage, 1);
			Actions.simulateAOTV(mage, new Location(world, -113.923, 82, -129.565));
		}, 66);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(mage, new Location(world, -92.5, 86, -127.5)), 67);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -96.4f, 6.7f), 68);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(mage, new Location(world, -78.5, 86, -129.5)), 69);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 171f, 18f);
			Actions.setFakePlayerHotbarSlot(mage, 4);
		}, 70);
		Utils.scheduleTask(() -> {
			Actions.simulateCrypt(mage, -81, 87, -133, -83, 86, -130);
			Actions.simulateCrypt(mage, -76, 87, -133, -78, 86, -130);
		}, 71);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 124f, 0f);
			Actions.setFakePlayerHotbarSlot(mage, 3);
		}, 72);
		Utils.scheduleTask(() -> {
			simulateBeam();
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Crypt 1/5");
		}, 73);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -135f, 0f), 74);
		Utils.scheduleTask(() -> {
			simulateBeam();
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Crypt 2/5");
		}, 78);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 0f, 17.8f);
			Actions.setFakePlayerHotbarSlot(mage, 1);
		}, 79);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(mage, new Location(world, -78.5, 86, -124.5)), 80);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 45f, 39.4f), 81);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(mage, new Location(world, -94.5, 69, -108.5)), 82);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 126.5f, 3.7f), 83);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(mage, new Location(world, -119.5, 69, -127.5)), 84);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 2.3f, 4.4f), 85);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(mage, new Location(world, -120.5, 69, -106.5)), 86);
		// tick 87: open inventory
		Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(mage, 5, 32), 88);
		// tick 89: close inventory
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 5), 90);

		/*
		 * ██████╗ ██╗      ██████╗  ██████╗ ██████╗      ██████╗ █████╗ ███╗   ███╗██████╗
		 * ██╔══██╗██║     ██╔═══██╗██╔═══██╗██╔══██╗    ██╔════╝██╔══██╗████╗ ████║██╔══██╗
		 * ██████╔╝██║     ██║   ██║██║   ██║██║  ██║    ██║     ███████║██╔████╔██║██████╔╝
		 * ██╔══██╗██║     ██║   ██║██║   ██║██║  ██║    ██║     ██╔══██║██║╚██╔╝██║██╔═══╝
		 * ██████╔╝███████╗╚██████╔╝╚██████╔╝██████╔╝    ╚██████╗██║  ██║██║ ╚═╝ ██║██║
		 * ╚═════╝ ╚══════╝ ╚═════╝  ╚═════╝ ╚═════╝      ╚═════╝╚═╝  ╚═╝╚═╝     ╚═╝╚═╝
		 */
		Utils.scheduleTask(() -> Actions.simulateRagAxe(mage), 374);
		Utils.scheduleTask(Server::openBloodDoor, 415);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 0f, -12f), 416);
		// rag axe activates on tick 434
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 3), 435);
		Utils.scheduleTask(() -> {
			simulateBeam();
			Actions.move(mage, new Vector(0, 0, 1.12242), 10);
		}, 436);
		Utils.scheduleTask(() -> snapHead("Bonzo"), 437);
		Utils.scheduleTask(Mage::simulateBeam, 441);
		Utils.scheduleTask(() -> snapHead("Meepy_"), 442);
		Utils.scheduleTask(Mage::simulateBeam, 446);
		Utils.scheduleTask(() -> snapHead("Mallyanke"), 447);
		Utils.scheduleTask(Mage::simulateBeam, 451);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 0f, -35f), 452);
		Utils.scheduleTask(Mage::simulateBeam, 493);
		Utils.scheduleTask(Mage::simulateBeam, 526);
		Utils.scheduleTask(Mage::simulateBeam, 567);
		Utils.scheduleTask(Mage::simulateBeam, 597);
		Utils.scheduleTask(Mage::simulateBeam, 630);
		Utils.scheduleTask(Mage::simulateBeam, 659);
		Utils.scheduleTask(Mage::simulateBeam, 688);
		Utils.scheduleTask(Mage::simulateBeam, 717);
		Utils.scheduleTask(Mage::simulateBeam, 750);
		Utils.scheduleTask(Mage::simulateBeam, 782);
		Utils.scheduleTask(Mage::simulateBeam, 823);
		Utils.scheduleTask(Mage::simulateBeam, 853);
		Utils.scheduleTask(Mage::simulateBeam, 886);
		Utils.scheduleTask(Mage::simulateBeam, 915);
		Utils.scheduleTask(() -> {
			simulateBeam();
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Blood Camp Finished in 947 Ticks (47.35 seconds)");
		}, 945);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 0f, 4.5f);
			Actions.setFakePlayerHotbarSlot(mage, 1);
		}, 946);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(mage, new Location(world, -120.5, 69, -74.5)), 947);
		Utils.scheduleTask(() -> {
			Actions.swapFakePlayerInventorySlots(mage, 9, 39);
			Actions.swapFakePlayerInventorySlots(mage, 10, 36);
			Actions.swapFakePlayerInventorySlots(mage, 1, 28);
			Actions.swapFakePlayerInventorySlots(mage, 3, 30);
			Actions.swapFakePlayerInventorySlots(mage, 4, 31);
		}, 948);
		Utils.scheduleTask(() -> {
			Bukkit.broadcastMessage(ChatColor.AQUA + "Mage: Entered Boss in 1027 Ticks (51.35 seconds)");
			if(doContinue) {
				Actions.teleport(mage, new Location(world, 73.5, 221, 13.5));
				maxor(true);
			}
		}, 1025);
	}

	public static void maxor(boolean doContinue) {
		Actions.setFakePlayerHotbarSlot(mage, 5);
		Actions.move(mage, new Vector(0.22, 0, 1.1), 28);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -11.31f, 0f), 1);
		Utils.scheduleTask(() -> {
			Actions.move(mage, new Vector(0.051, 0, 0.255), 16);
			Actions.simulateSpringBoots(mage);
		}, 28);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -33f, 0f), 45);
		Utils.scheduleTask(() -> {
			Maxor.pickUpCrystal(mage);
			Bukkit.broadcastMessage(ChatColor.GOLD + "Beethoven_" + ChatColor.GREEN + " picked up an " + ChatColor.AQUA + "Energy Crystal" + ChatColor.GREEN + "!");
		}, 56);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.1403, 0, 0.243), 2), 57);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -90f, 0f), 58);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.2806, 0, 0), 16), 59);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -131.2925f, 0f), 75);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.8433, 0, -0.7401), 1), 79);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.2108, 0, -0.1852), 19), 81);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.8433, 0, -0.7401), 1), 100);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.1954, 0, -0.1716), 1), 101);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 48.7075f, 0f), 102);
		Utils.scheduleTask(() -> {
			Maxor.placeCrystal(mage);
			Actions.move(mage, new Vector(-0.1954, 0, 0.1716), 16);
			Actions.simulateSpringBoots(mage);
		}, 160);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(-0.2108, 0, 0.1852), 27), 177);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(-0.0488, 0, 0.0429), 1), 206);
		Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(mage, 10, 36), 207);
		Utils.scheduleTask(() -> {
			Maxor.pickUpCrystal(mage);
			Bukkit.broadcastMessage(ChatColor.GOLD + "Beethoven_" + ChatColor.GREEN + " picked up an " + ChatColor.AQUA + "Energy Crystal" + ChatColor.GREEN + "!");
		}, 239);
		Utils.scheduleTask(() -> Actions.turnHead(mage, -131.2925f, 0f), 240);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.8433, 0, -0.7401), 1), 241);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.2108, 0, -0.1852), 19), 242);
		Utils.scheduleTask(() -> Actions.move(mage, new Vector(0.8433, 0, -0.7401), 1), 261);
		Utils.scheduleTask(() -> Maxor.placeCrystal(mage), 262);
		Utils.scheduleTask(() -> Actions.simulateRagAxe(mage), 263);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 4), 324);
		Utils.scheduleTask(() -> Actions.simulateLeap(mage, Archer.getArcher()), 325);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 3), 326);
		Utils.scheduleTask(Mage::simulateBeam, 399);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 4), 400);
		Utils.scheduleTask(() -> Actions.simulateLeap(mage, Berserk.getBerserk()), 401);
		if(doContinue) {
			Utils.scheduleTask(() -> storm(true), 499);
		}
	}

	public static void storm(boolean doContinue) {

	}

	private static void snapHead(String target) {
		// Find the nearest mob with the target name
		Entity nearestMob = null;
		double nearestDistance = Double.MAX_VALUE;

		// Search through all nearby entities
		for(Entity entity : mage.getNearbyEntities(32, 32, 32)) { // 50 block search radius
			// Check if entity is a living entity (mob)
			if(entity instanceof LivingEntity) {
				// Check if the entity has a custom name containing the target
				if(entity.getCustomName() != null && entity.getCustomName().toLowerCase().contains(target.toLowerCase())) {
					double distance = mage.getLocation().distance(entity.getLocation());
					if(distance < nearestDistance) {
						nearestDistance = distance;
						nearestMob = entity;
					}
				}
			}
		}

		// If we found a target, turn the player's head to face it
		if(nearestMob != null) {
			Location playerLoc = mage.getEyeLocation();
			Location targetLoc = nearestMob.getLocation().add(0, nearestMob.getHeight() / 2, 0); // Aim at center of mob

			// Calculate the direction vector
			Vector direction = targetLoc.subtract(playerLoc).toVector().normalize();

			// Convert to yaw and pitch
			float yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
			float pitch = (float) Math.toDegrees(Math.asin(-direction.getY()));

			// Turn the player's head
			Actions.turnHead(mage, yaw, pitch);
		}
	}

	private static void simulateBeam() {
		Actions.simulateLeftClickAir(mage);

		Location l = mage.getLocation();
		l.add(0, 1.62, 0);
		Vector v = l.getDirection();
		v.setX(v.getX() / 5);
		v.setY(v.getY() / 5);
		v.setZ(v.getZ() / 5);
		for(int i = 0; i < 100; i++) {
			if(l.getBlock().getType().isSolid()) {
				break;
			}
			boolean shouldBreak = false;
			ArrayList<Entity> entities = (ArrayList<Entity>) world.getNearbyEntities(l, 1, 1, 1);
			for(Entity entity : entities) {
				//noinspection DataFlowIssue
				if(entity instanceof LivingEntity temp && !temp.equals(mage) && !(temp instanceof Player) && !entity.isDead() && !entity.isInvulnerable() && !(temp.hasPotionEffect(PotionEffectType.RESISTANCE) && temp.getPotionEffect(PotionEffectType.RESISTANCE).getAmplifier() == 255)) {
					double damage = mage.getScoreboardTags().contains("RagBuff") ? (temp instanceof Wither ? 145 : 85) : (temp instanceof Wither ? 120 : 70);
					Bukkit.getPluginManager().callEvent(new EntityDamageByEntityEvent(mage, temp, EntityDamageByEntityEvent.DamageCause.KILL, DamageSource.builder(DamageType.GENERIC_KILL).build(), damage));
					if(temp.getHurtSound() != null) {
						world.playSound(l, temp.getHurtSound(), 1.0F, 1.0F);
					}
					shouldBreak = true;
					break;
				}
			}
			spawnParticle(l);
			l.add(v);
			if(shouldBreak) {
				spawnParticle(l);
				l.add(v);
				spawnParticle(l);
				l.add(v);
				spawnParticle(l);
				l.add(v);
				spawnParticle(l);
				l.add(v);
				spawnParticle(l);
				l.add(v);
				spawnParticle(l);
				break;
			}
		}
	}

	private static void spawnParticle(Location l) {
		ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(ParticleTypes.FIREWORK,  // ParticleParam
				false,                   // overrideLimiter
				false,                   // longDistance
				l.getX(),        // x position
				l.getY(),        // y position
				l.getZ(),        // z position
				0.0f,                   // xDist (no spread)
				0.0f,                   // yDist (no spread)
				0.0f,                   // zDist (no spread)
				0.0f,                   // speed (no velocity)
				1                       // count (single particle)
		);

		for(Player player : Objects.requireNonNull(l.getWorld()).getPlayers()) {
			if(player instanceof CraftPlayer craftPlayer) {
				ServerPlayer nmsPlayer = craftPlayer.getHandle();
				nmsPlayer.connection.send(packet);
			}
		}
	}

	@SuppressWarnings("unused")
	public static Player getMage() {
		return mage;
	}
}
