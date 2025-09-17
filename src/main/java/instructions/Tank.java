package instructions;

import instructions.bosses.Goldor;
import instructions.bosses.Storm;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.util.Vector;
import plugin.Utils;

import java.util.List;
import java.util.Objects;

public class Tank {
	private static Player tank;
	@SuppressWarnings("FieldCanBeLocal")
	private static World world;

	public static void tankInstructions(Player p, String section) {
		tank = p;
		world = Tank.tank.getWorld();
		tank.setGameMode(GameMode.SURVIVAL);
		tank.setFlying(false);
		Objects.requireNonNull(tank.getInventory().getItem(4)).addUnsafeEnchantment(Enchantment.POWER, 2);

		switch(section) {
			case "all", "clear" -> {
				Actions.teleport(Tank.tank, new Location(world, -196.5, 68, -222.5, 0f, 5.6f));
				Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(tank, 2, 29), 60);
				Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 2), 61);
				Utils.scheduleTask(() -> Actions.rightClickWithSpectators(tank), 101);
				Utils.scheduleTask(() -> {
					Actions.setFakePlayerHotbarSlot(Tank.tank, 1);
					Actions.move(tank, new Vector(-0.8634, 0, 0), 5);
				}, 102);
				Utils.scheduleTask(() -> {
					Actions.teleport(tank, new Location(tank.getWorld(), -120.5, 75, -220.5));
					Actions.swapFakePlayerInventorySlots(Tank.tank, 2, 29);
				}, 141);
				Utils.scheduleTask(() -> clear(section.equals("all")), 162);
			}
			case "maxor", "boss" -> {
				Actions.teleport(tank, new Location(world, 73.5, 221, 13.5, 0f, 0f));
				Actions.swapFakePlayerInventorySlots(tank, 1, 28);
				Actions.swapFakePlayerInventorySlots(tank, 3, 30);
				Actions.swapFakePlayerInventorySlots(tank, 4, 31);
				Actions.swapFakePlayerInventorySlots(tank, 5, 32);
				Actions.swapFakePlayerInventorySlots(tank, 6, 33);
				Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(tank, 7, 33), 1);
				if(section.equals("maxor")) {
					Utils.scheduleTask(() -> maxor(false), 60);
				} else {
					Utils.scheduleTask(() -> maxor(true), 60);
				}
			}
			case "storm" -> {
				Actions.teleport(tank, new Location(world, 35.043, 170, 92.054, 46.9f, 25f));
				Actions.swapFakePlayerInventorySlots(tank, 1, 28);
				Actions.swapFakePlayerInventorySlots(tank, 3, 30);
				Actions.swapFakePlayerInventorySlots(tank, 5, 32);
				Actions.swapFakePlayerInventorySlots(tank, 6, 33);
				Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(tank, 7, 33), 1);
				Utils.scheduleTask(() -> storm(false), 60);
			}
			case "goldor" -> {
				Actions.teleport(tank, new Location(world, 107.736, 120, 89.242, -54.5f, 2f));
				Actions.swapFakePlayerInventorySlots(tank, 1, 28);
				Actions.swapFakePlayerInventorySlots(tank, 3, 30);
				Actions.swapFakePlayerInventorySlots(tank, 5, 32);
				Actions.swapFakePlayerInventorySlots(tank, 6, 33);
				Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(tank, 7, 33), 1);
				Utils.scheduleTask(() -> goldor(false), 60);
			}
			case "necron" -> {
				Actions.teleport(tank, new Location(world, 54.529, 65, 83.688, 180f, -5f));
				Actions.swapFakePlayerInventorySlots(tank, 1, 28);
				Actions.swapFakePlayerInventorySlots(tank, 3, 30);
				Actions.swapFakePlayerInventorySlots(tank, 5, 32);
				Actions.swapFakePlayerInventorySlots(tank, 6, 33);
				Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(tank, 7, 33), 1);
				Utils.scheduleTask(() -> necron(false), 60);
			}
		}
	}

	private static void clear(boolean doContinue) {
		/*
		 * ████████╗██████╗  █████╗ ██████╗
		 * ╚══██╔══╝██╔══██╗██╔══██╗██╔══██╗
		 *    ██║   ██████╔╝███████║██████╔╝
		 *    ██║   ██╔══██╗██╔══██║██╔═══╝
		 *    ██║   ██║  ██║██║  ██║██║
		 *    ╚═╝   ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝
		 */
		// Tick 162 (clear tick 2, delay = 0)
		Actions.etherwarp(tank, new Location(world, -196.5, 69, -216.5));
		Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Purple Flags Insta-Cleared");
		Utils.scheduleTask(() -> Actions.turnHead(tank, 90f, 1.8f), 1);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -229.5, 69, -216.5)), 2);

		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 196f, 15f);
			Actions.setFakePlayerHotbarSlot(tank, 7);
		}, 3);
		Utils.scheduleTask(() -> Actions.throwPearl(tank), 4);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 12f, -23f), 5);
		Utils.scheduleTask(() -> Actions.AOTV(tank, new Location(world, -226.42, 66, -227.16)), 13);
		Utils.scheduleTask(() -> Actions.throwPearl(tank), 14);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -87f, -3f), 15);
		Utils.scheduleTask(() -> Actions.throwPearl(tank), 16);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -88.8f, 32.8f), 17);
		Utils.scheduleTask(() -> Actions.AOTV(tank, new Location(world, -214.971, 67, -226.596)), 25);
		Utils.scheduleTask(() -> {
			Actions.swingHand(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Trap 1/3 (Opened Chest)");
			world.playSound(tank.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 26);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -118f, -79.5f);
			Actions.AOTV(tank, new Location(world, -229.7, 71, -211.391));
		}, 27);
		Utils.scheduleTask(() -> Actions.throwPearl(tank), 28);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -102f, -8.9f), 29);
		Utils.scheduleTask(() -> Actions.AOTV(tank, new Location(world, -226.71, 87.93905, -212.7)), 41);
		Utils.scheduleTask(() -> Actions.throwPearl(tank), 42);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -5.3f, -23.8f);
			Actions.setFakePlayerHotbarSlot(tank, 0);
		}, 43);
		Utils.scheduleTask(() -> Actions.AOTV(tank, new Location(world, -222.8, 89.5, -213.3)), 46);
		Utils.scheduleTask(() -> {
			Actions.swingHand(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Trap 2/3 (Killed Bat)");
			world.playSound(tank.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0F, 1.0F);
		}, 47);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -78.5f, 67.3f);
			Actions.setFakePlayerHotbarSlot(tank, 5);
		}, 48);
		Utils.scheduleTask(() -> Actions.stonk(tank, world.getBlockAt(-223, 89, -214)), 49);
		Utils.scheduleTask(() -> {
			Actions.move(tank, new Vector(0.8634, 0, 0), 1);
			Actions.turnHead(tank, -57f, 42.5f);
			Actions.setFakePlayerHotbarSlot(tank, 7);
		}, 50);
		Utils.scheduleTask(() -> Actions.throwPearl(tank), 51);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 21.6f, 48.48f), 52);
		Utils.scheduleTask(() -> Actions.AOTV(tank, new Location(world, -215.217, 80, -208.649)), 60);
		Utils.scheduleTask(() -> Actions.throwPearl(tank), 61);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -103.27f, -88.35f), 62);
		Utils.scheduleTask(() -> Actions.AOTV(tank, new Location(world, -216.323, 78, -205.898)), 65);
		Utils.scheduleTask(() -> Actions.throwPearl(tank), 66);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -84.80f, 19.01f), 67);
		Utils.scheduleTask(() -> Actions.throwPearl(tank), 73);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -90f, -50.1f);
			Actions.AOTV(tank, new Location(world, -216.3, 88.68032, -205.7));
		}, 74);
		Utils.scheduleTask(() -> {
			Actions.swingHand(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Trap 3/3 (Opened Chest)");
			world.playSound(tank.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 75);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -90f, 33.3f);
			Actions.setFakePlayerHotbarSlot(tank, 5);
			Actions.AOTV(tank, new Location(world, -212.5, 79, -205.5));
		}, 76);
		// Stone through wall from tick 77-84
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.8634, 0, 0), 9), 77);
		Utils.scheduleTask(() -> Actions.stonk(tank, world.getBlockAt(-212, 80, -206)), 77);
		Utils.scheduleTask(() -> Actions.stonk(tank, world.getBlockAt(-212, 79, -206)), 78);
		Utils.scheduleTask(() -> Actions.stonk(tank, world.getBlockAt(-211, 80, -206)), 79);
		Utils.scheduleTask(() -> Actions.stonk(tank, world.getBlockAt(-211, 79, -206)), 80);
		Utils.scheduleTask(() -> Actions.stonk(tank, world.getBlockAt(-210, 80, -206)), 81);
		Utils.scheduleTask(() -> Actions.stonk(tank, world.getBlockAt(-210, 79, -206)), 82);
		Utils.scheduleTask(() -> Actions.stonk(tank, world.getBlockAt(-209, 80, -206)), 83);
		Utils.scheduleTask(() -> Actions.stonk(tank, world.getBlockAt(-209, 79, -206)), 84);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -149.42f, 32f);
			Actions.setFakePlayerHotbarSlot(tank, 7);
		}, 85);
		Utils.scheduleTask(() -> Actions.throwPearl(tank), 86);
		Utils.scheduleTask(() -> Actions.AOTV(tank, new Location(world, -202.631, 69, -216.015)), 97);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -98.5f, 4f);
			Actions.setFakePlayerHotbarSlot(tank, 1);
			Actions.move(tank, new Vector(1.11009, 0, -0.1659), 3);
		}, 98);

		/*
		 * ██████╗ ██╗   ██╗██████╗ ██████╗ ██╗     ███████╗    ███████╗██╗      █████╗  ██████╗ ███████╗
		 * ██╔══██╗██║   ██║██╔══██╗██╔══██╗██║     ██╔════╝    ██╔════╝██║     ██╔══██╗██╔════╝ ██╔════╝
		 * ██████╔╝██║   ██║██████╔╝██████╔╝██║     █████╗      █████╗  ██║     ███████║██║  ███╗███████╗
		 * ██╔═══╝ ██║   ██║██╔══██╗██╔═══╝ ██║     ██╔══╝      ██╔══╝  ██║     ██╔══██║██║   ██║╚════██║
		 * ██║     ╚██████╔╝██║  ██║██║     ███████╗███████╗    ██║     ███████╗██║  ██║╚██████╔╝███████║
		 * ╚═╝      ╚═════╝ ╚═╝  ╚═╝╚═╝     ╚══════╝╚══════╝    ╚═╝     ╚══════╝╚═╝  ╚═╝ ╚═════╝ ╚══════╝
		 */
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -190.5, 70, -217.5)), 101);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -99.2f, 8.9f), 102);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -167.5, 68, -221.5)), 103);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -180f, 30f);
			Actions.setFakePlayerHotbarSlot(tank, 5);
		}, 104);
		Utils.scheduleTask(() -> Actions.stonk(tank, world.getBlockAt(-168, 69, -223)), 105);
		Utils.scheduleTask(() -> Actions.stonk(tank, world.getBlockAt(-168, 68, -223)), 106);
		Utils.scheduleTask(() -> Actions.stonk(tank, world.getBlockAt(-168, 68, -224)), 107);
		Utils.scheduleTask(() -> {
			Actions.swingHand(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Purple Flags 1/5 (Opened Chest)");
			world.playSound(tank.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 108);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 36.3f, 29.9f);
			Actions.setFakePlayerHotbarSlot(tank, 1);
		}, 109);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -178.5, 60, -206.5)), 110);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 86.5f, 0.4f), 111);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -195.5, 62, -205.5)), 112);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -15.6f, 70.2f), 113);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -194.5, 57, -203.5)), 114);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -90f, 53.2f), 115);
		Utils.scheduleTask(() -> {
			Actions.swingHand(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Purple Flags 2/5 (Obtained Wither Essence)");
			world.playSound(tank.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		}, 116);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -126.9f, 30.4f), 117);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -186.5, 53, -209.5)), 118);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -54.6f, 38.2f), 119);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -180.5, 49, -205.5)), 120);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -90f, 12.2f);
			Actions.setFakePlayerHotbarSlot(tank, 3);
		}, 121);
		Utils.scheduleTask(() -> Actions.superboom(tank, -180, 49, -206, -180, 53, -208), 122);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 1), 123);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -172.5, 49, -205.5)), 124);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -94.8f, 7.7f);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Purple Flags 3/5 (Obtained Item)");
			world.playSound(tank.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
		}, 125);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -160.5, 49, -206.5)), 126);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 0f, 15f), 127);
		Utils.scheduleTask(() -> {
			Actions.swingHand(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Purple Flags 4/5 (Opened Chest)");
			world.playSound(tank.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
			Actions.move(tank, new Vector(0, 0, -0.8634), 1);
		}, 128);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -96.7f, 7.7f), 129);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -151.5, 49, -208.5)), 130);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -90f, 35f);
			Actions.setFakePlayerHotbarSlot(tank, 5);
		}, 131);
		Utils.scheduleTask(() -> {
			Actions.move(tank, new Vector(1.12242, 0, 0), 7);
			Actions.stonk(tank, world.getBlockAt(-151, 50, -209));
		}, 132);
		Utils.scheduleTask(() -> Actions.stonk(tank, world.getBlockAt(-151, 49, -209)), 133);
		Utils.scheduleTask(() -> Actions.stonk(tank, world.getBlockAt(-150, 50, -209)), 134);
		Utils.scheduleTask(() -> Actions.stonk(tank, world.getBlockAt(-150, 49, -209)), 135);
		Utils.scheduleTask(() -> Actions.stonk(tank, world.getBlockAt(-149, 50, -209)), 136);
		Utils.scheduleTask(() -> Actions.stonk(tank, world.getBlockAt(-149, 49, -209)), 137);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -83.2f, 19.7f);
			Actions.setFakePlayerHotbarSlot(tank, 1);
		}, 138);
		Utils.scheduleTask(() -> Actions.AOTV(tank, new Location(world, -143.5, 49, -207.5)), 139);
		Utils.scheduleTask(() -> {
			Actions.swingHand(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Purple Flags 5/5 (Opened Chest)");
			world.playSound(tank.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 140);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 58.1f, -59.9f), 141);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -146.5, 58, -205.5)), 142);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 91.7f, -30.8f), 143);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -162.5, 69, -205.5)), 144);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 90f, 4.5f), 145);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -184.5, 69, -205.5)), 146);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 0f, 5.3f), 147);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -184.5, 69, -187.5)), 148);

		/*
		 * ██████╗ ███████╗██████╗     ██████╗ ██╗     ██╗   ██╗███████╗
		 * ██╔══██╗██╔════╝██╔══██╗    ██╔══██╗██║     ██║   ██║██╔════╝
		 * ██████╔╝█████╗  ██║  ██║    ██████╔╝██║     ██║   ██║█████╗
		 * ██╔══██╗██╔══╝  ██║  ██║    ██╔══██╗██║     ██║   ██║██╔══╝
		 * ██║  ██║███████╗██████╔╝    ██████╔╝███████╗╚██████╔╝███████╗
		 * ╚═╝  ╚═╝╚══════╝╚═════╝     ╚═════╝ ╚══════╝ ╚═════╝ ╚══════╝
		 */
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 82f, -10f);
			Actions.setFakePlayerHotbarSlot(tank, 7);
		}, 149);
		Utils.scheduleTask(() -> Actions.throwPearl(tank), 150);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -43.3f, -75.5f);
			Actions.setFakePlayerHotbarSlot(tank, 1);
		}, 151);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -182.5, 83, -185.5)), 152);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 65.5f, 30.3f), 153);
		Utils.scheduleTask(() -> {
			Actions.swingHand(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Red Blue 1/4 (Obtained Wither Essence)");
			world.playSound(tank.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		}, 154);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -95.7f, -0.1f), 155);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -156.5, 85, -188.5)), 156);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 111.7f, -9.3f), 157);
		Utils.scheduleTask(() -> Actions.AOTV(tank, new Location(world, -164.5, 86, -191.5)), 158);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -97.1f, -2.7f);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Red Blue 2/4 (Obtained Item)");
			world.playSound(tank.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
		}, 159);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -148.5, 89, -193.5)), 160);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -120f, 0.7f);
			Actions.setFakePlayerHotbarSlot(tank, 5);
			Actions.move(tank, new Vector(0.22432, 0, -0.12951), 2);
		}, 161);
		Utils.scheduleTask(() -> Actions.stonk(tank, world.getBlockAt(-146, 90, -195)), 162);
		Utils.scheduleTask(() -> Actions.stonk(tank, world.getBlockAt(-146, 90, -196)), 163);
		Utils.scheduleTask(() -> {
			Actions.swingHand(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Red Blue 3/4 (Opened Chest)");
			world.playSound(tank.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 164);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -6.4f, 67.2f);
			Actions.setFakePlayerHotbarSlot(tank, 1);
		}, 165);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -146.5, 69, -184.5)), 166);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 55.1f, 6.4f), 167);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -158.5, 69, -176.5)), 168);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 66.3f, 1f);
			Actions.setFakePlayerHotbarSlot(tank, 5);
		}, 169);
		Utils.scheduleTask(() -> Actions.stonk(tank, world.getBlockAt(-161, 70, -176)), 170);
		Utils.scheduleTask(() -> {
			Actions.swingHand(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Red Blue 4/4 (Opened Chest)");
			world.playSound(tank.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 171);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 102.7f, 4f);
			Actions.setFakePlayerHotbarSlot(tank, 4);
			Actions.AOTV(tank, new Location(world, -212.058, 69, -183.541));
		}, 172);

		/*
		 * ███████╗██████╗ ██╗██████╗ ███████╗██████╗
		 * ██╔════╝██╔══██╗██║██╔══██╗██╔════╝██╔══██╗
		 * ███████╗██████╔╝██║██║  ██║█████╗  ██████╔╝
		 * ╚════██║██╔═══╝ ██║██║  ██║██╔══╝  ██╔══██╗
		 * ███████║██║     ██║██████╔╝███████╗██║  ██║
		 * ╚══════╝╚═╝     ╚═╝╚═════╝ ╚══════╝╚═╝  ╚═╝
		 */
		Utils.scheduleTask(() -> Actions.rightClick(tank), 173);
		Utils.scheduleTask(() -> Actions.swingHand(tank), 177);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 178);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -31.6f, 16.6f);
			Actions.setFakePlayerHotbarSlot(tank, 1);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Spider Cleared");
		}, 179);
		Utils.scheduleTask(() -> Actions.AOTV(tank, new Location(world, -206.027, 65.57762, -173.747)), 180);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 68.8f, 63.6f), 181);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -211.5, 55, -171.5)), 182);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 83f, 12.7f), 183);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -218.5, 55, -170.5)), 184);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -44.9f, 49.6f);
			Actions.setFakePlayerHotbarSlot(tank, 7);
		}, 185);
		Utils.scheduleTask(() -> Actions.throwPearl(tank), 186);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -165.1f, 16.3f);
			Actions.setFakePlayerHotbarSlot(tank, 1);
		}, 187);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -212.5, 50, -193.5)), 188);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 180f, 0f);
			Actions.move(tank, new Vector(0, 0, -1.12242), 3);
		}, 189);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -81.5f, 18.9f), 191);
		Utils.scheduleTask(() -> Actions.AOTV(tank, new Location(world, -207.5, 50, -196.5)), 192);
		Utils.scheduleTask(() -> {
			Actions.swingHand(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Spider 2/9 (Opened Chest)");
			world.playSound(tank.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 193);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 52.2f, 6.8f);
			Actions.AOTV(tank, new Location(world, -213.969, 47.5, -165.769));
		}, 194);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -222.5, 48, -159.5)), 195);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -40.5f, 4.5f);
			Actions.setFakePlayerHotbarSlot(tank, 7);
		}, 196);
		Utils.scheduleTask(() -> Actions.throwPearl(tank), 197);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 90f, 65f);
			Actions.setFakePlayerHotbarSlot(tank, 1);
		}, 198);
		Utils.scheduleTask(() -> Actions.AOTV(tank, new Location(world, -223.5, 47, -159.5)), 199);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 90f, 90f);
			Actions.move(tank, new Vector(-1.12242, 0, 0), 1);
		}, 200);
		Utils.scheduleTask(() -> Actions.AOTV(tank, new Location(world, -224.5, 36, -159.5)), 201);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -90f, 35.3f), 202);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -210.5, 28, -159.5)), 203);
		Utils.scheduleTask(() -> {
			Actions.swingHand(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Spider 4/9 (Opened Chest)");
			world.playSound(tank.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 204);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -92.7f, -21.4f);
			Actions.AOTV(tank, new Location(world, -215.275, 47, -152.695));
		}, 205);
		Utils.scheduleTask(() -> {
			Actions.etherwarp(tank, new Location(world, -177.5, 64, -154.5));
			Actions.stonk(null, world.getBlockAt(-183, 84, -164)); // pass null to avoid hand swinging - this is so that the teleport packet in tick 210 works
		}, 206);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 149.5f, -61f), 207);
		Utils.scheduleTask(() -> Actions.etherwarp(tank, new Location(world, -182.5, 84, -163.5)), 208);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 90f, -90f);
			Actions.setFakePlayerHotbarSlot(tank, 0);
		}, 209);
		Utils.scheduleTask(() -> {
			Actions.witherImpact(tank, new Location(world, -182.5, 84, -163.5));
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Spider 6/9 (Killed Bat)");
			world.playSound(tank.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0F, 1.0F);
		}, 210);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 90f, 90f), 211);
		Utils.scheduleTask(() -> {
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Spider 8/9 (Opened Chest)");
			world.playSound(tank.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Clear Finished in 214 Ticks (10.70 seconds)");
		}, 212);
		Utils.scheduleTask(() -> {
			Actions.swapFakePlayerInventorySlots(tank, 1, 28);
			Actions.swapFakePlayerInventorySlots(tank, 3, 30);
			Actions.swapFakePlayerInventorySlots(tank, 4, 31);
			Actions.swapFakePlayerInventorySlots(tank, 5, 32);
			Actions.swapFakePlayerInventorySlots(tank, 6, 33);
			Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(tank, 7, 33), 1);
		}, 213);
		if(doContinue) {
			Utils.scheduleTask(() -> {
				Actions.teleport(tank, new Location(world, 73.5, 221, 13.5));
				maxor(true);
			}, 1025);
		}
	}

	public static void maxor(boolean doContinue) {
		Actions.setFakePlayerHotbarSlot(tank, 4);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0, 0, 1.12242), 57), 1);
		Utils.scheduleTask(() -> Actions.jump(tank), 48);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -180f, -5f), 58);
		Utils.scheduleTask(() -> Actions.AOTS(tank), 59);
		Utils.scheduleTask(() -> Actions.AOTS(tank), 71);
		Utils.scheduleTask(() -> Actions.AOTS(tank), 83);
		Utils.scheduleTask(() -> Actions.AOTS(tank), 95);
		Utils.scheduleTask(() -> Actions.AOTS(tank), 107);
		Utils.scheduleTask(() -> Actions.AOTS(tank), 119);
		Utils.scheduleTask(() -> Actions.AOTS(tank), 131);
		Utils.scheduleTask(() -> Actions.AOTS(tank), 143);
		Utils.scheduleTask(() -> Actions.AOTS(tank), 155);
		Utils.scheduleTask(() -> Actions.AOTS(tank), 167);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 6), 168);
		Utils.scheduleTask(() -> Actions.lastBreath(tank, 29), 169);
		Utils.scheduleTask(() -> Actions.lastBreath(tank, 4), 198);
		Utils.scheduleTask(() -> Actions.lastBreath(tank, 4), 202);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 2), 203);
		Utils.scheduleTask(() -> Actions.iceSpray(tank), 204);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 5), 205);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 206);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 218);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 230);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 242);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 254);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 266);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 278);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 290);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 302);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 314);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 326);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 338);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 350);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 362);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 374);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 386);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 398);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 3), 399);
		Utils.scheduleTask(() -> Actions.leap(tank, Berserk.get()), 400);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 28.7f, 0f);
			Actions.setFakePlayerHotbarSlot(tank, 1);
		}, 401);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.539, 0, 0.9845), 28), 402);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 28.7f, 82f), 429);
		Utils.scheduleTask(() -> Actions.bonzo(tank, new Vector(-0.7326, 0.5, 1.338)), 430);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.539, 0, 0.9845), 6), 441);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 46.9f, 25f), 448);
		Utils.scheduleTask(() -> {
			Actions.setFakePlayerHotbarSlot(tank, 7);
			Actions.swapFakePlayerInventorySlots(tank, 4, 31);
		}, 449);
		if(doContinue) {
			Utils.scheduleTask(() -> storm(true), 499);
		}
	}

	public static void storm(boolean doContinue) {
		Storm.prepadYellow();
		Actions.setFakePlayerHotbarSlot(tank, 6);
		Utils.scheduleTask(() -> Actions.gyro(tank, new Location(world, 32.5, 170, 94.5)), 1);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 46.9f, 0f);
			Actions.setFakePlayerHotbarSlot(tank, 4);
		}, 2);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 3);
		Utils.scheduleTask(() -> Actions.salvation(tank), 4);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 8);
		Utils.scheduleTask(() -> Actions.salvation(tank), 9);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 13);
		Utils.scheduleTask(() -> Actions.salvation(tank), 14);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 18);
		Utils.scheduleTask(() -> Actions.salvation(tank), 19);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 23);
		Utils.scheduleTask(() -> Actions.salvation(tank), 24);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 28);
		Utils.scheduleTask(() -> Actions.salvation(tank), 29);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 90f, 0f), 30);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-1.12242, 0, 0), 5), 31);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -180f, 9f), 36);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 37);
		Utils.scheduleTask(() -> Actions.salvation(tank), 38);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 42);
		Utils.scheduleTask(() -> Actions.salvation(tank), 43);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 47);
		Utils.scheduleTask(() -> Actions.salvation(tank), 48);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -180f, 6f), 49);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 52);
		Utils.scheduleTask(() -> Actions.salvation(tank), 53);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 57);
		Utils.scheduleTask(() -> Actions.salvation(tank), 58);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 62);
		Utils.scheduleTask(() -> Actions.salvation(tank), 63);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -180f, 3f), 64);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 67);
		Utils.scheduleTask(() -> Actions.salvation(tank), 68);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 72);
		Utils.scheduleTask(() -> Actions.salvation(tank), 73);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 77);
		Utils.scheduleTask(() -> Actions.salvation(tank), 78);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -90f, 0f), 79);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(1.12242, 0, 0), 5), 80);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -73f, -2f), 85);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 86);
		Utils.scheduleTask(() -> Actions.salvation(tank), 87);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 91);
		Utils.scheduleTask(() -> Actions.salvation(tank), 92);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -80f, 9f), 93);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 96);
		Utils.scheduleTask(() -> Actions.salvation(tank), 97);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 101);
		Utils.scheduleTask(() -> Actions.salvation(tank), 102);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -88f, 9f), 103);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 106);
		Utils.scheduleTask(() -> Actions.salvation(tank), 107);
		Utils.scheduleTask(() -> Actions.rightClick(tank), 111);
		Utils.scheduleTask(() -> Actions.salvation(tank), 112);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -156.7f, 0f), 173);
		Utils.scheduleTask(() -> {
			Actions.setFakePlayerHotbarSlot(tank, 1);
			Actions.move(tank, new Vector(0.444, 0, -1.031), 6);
		}, 174);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -156.7f, 82f), 179);
		Utils.scheduleTask(() -> Actions.bonzo(tank, new Vector(0.6034, 0.5, -1.401)), 180);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -156.7f, 0f), 181);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.444, 0, -1.031), 9), 195);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 4), 201);
		for(int tick = 205; tick <= 545; tick += 5) {
			Utils.scheduleTask(() -> {
				List<Entity> nearbyEntities = tank.getNearbyEntities(6, 6, 6);

				for(Entity entity : nearbyEntities) {
					if(entity instanceof WitherSkeleton) {
						Location healerLoc = tank.getLocation();
						Location witherLoc = entity.getLocation();

						double deltaX = witherLoc.getX() - healerLoc.getX();
						double deltaY = witherLoc.getY() - healerLoc.getY();
						double deltaZ = witherLoc.getZ() - healerLoc.getZ();

						float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;
						float pitch = (float) -(Math.atan2(deltaY, Math.sqrt(deltaX * deltaX + deltaZ * deltaZ)) * 180.0 / Math.PI);

						Actions.turnHead(tank, yaw, pitch);

						Utils.scheduleTask(() -> Actions.rightClick(tank), 1);

						break;
					}
				}
			}, tick);
		}
		Utils.scheduleTask(() -> Actions.turnHead(tank, -90f, -5.2f), 546);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -23.3f, 0f), 687);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.444, 0, 1.031), 12), 688);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -23.3f, 82f), 699);
		Utils.scheduleTask(() -> Actions.bonzo(tank, new Vector(-0.6034, 0.5, 1.401)), 700);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -23.3f, 0f), 701);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.444, 0, 1.031), 3), 711);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 3), 712);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.444, 0, 1.031), 2), 758);
		Utils.scheduleTask(Storm::crushYellow, 760);
		Utils.scheduleTask(() -> Actions.leap(tank, Healer.get()), 770);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 173f, 0f), 771);
		Utils.scheduleTask(() -> {
			Actions.move(tank, new Vector(-0.1368, 0, -1.114), 1);
			Actions.jump(tank);
			Actions.setFakePlayerHotbarSlot(tank, 5);
		}, 772);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.0342, 0, -0.2785), 4), 773);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -54.5f, 2f), 777);
		if(doContinue) {
			Utils.scheduleTask(() -> goldor(true), 890);
		}
	}

	private static void goldor(boolean doContinue) {
		/*
		 *  ██╗
		 * ███║
		 * ╚██║
		 *  ██║
		 *  ██║
		 *  ╚═╝
		 */
		Actions.setFakePlayerHotbarSlot(tank, 5);
		Utils.scheduleTask(() -> Actions.swingHand(tank), 1);
		Utils.scheduleTask(() -> Actions.swingHand(tank), 2);
		Utils.scheduleTask(() -> Actions.swingHand(tank), 3);
		Utils.scheduleTask(() -> Actions.swingHand(tank), 4);
		Utils.scheduleTask(() -> Actions.swingHand(tank), 5);
		Utils.scheduleTask(() -> Actions.swingHand(tank), 6);
		Utils.scheduleTask(() -> Actions.swingHand(tank), 7);
		Utils.scheduleTask(() -> Actions.swingHand(tank), 8);
		Utils.scheduleTask(() -> Actions.swingHand(tank), 9);
		Utils.scheduleTask(() -> Actions.swingHand(tank), 10);
		Utils.scheduleTask(() -> Actions.swingHand(tank), 11);
		Utils.scheduleTask(() -> Actions.swingHand(tank), 12);
		Utils.scheduleTask(() -> Actions.swingHand(tank), 13);
		Utils.scheduleTask(() -> Actions.swingHand(tank), 14);
		Utils.scheduleTask(() -> Actions.swingHand(tank), 15);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 179f, 0f), 16);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.0245, 0, -1.4028), 6), 17);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.0049, 0, -0.2805), 2), 23);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -101f, 17.2f), 24);
		Utils.scheduleTask(() -> Actions.swingHand(tank), 25);
		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(tank, "terminal", 2, 7), 26);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -160f, 0f), 27);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.48, 0, -1.3184), 1), 28);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.096, 0, -0.2637), 4), 29);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -125f, 67.6f), 33);
		Utils.scheduleTask(() -> Actions.swingHand(tank), 38);
		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(tank, "terminal", 4, 7), 39);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 3), 40);
		Utils.scheduleTask(() -> Actions.leap(tank, Archer.get()), 41);

		/*
		 * ██████╗
		 * ╚════██╗
		 *  █████╔╝
		 * ██╔═══╝
		 * ███████╗
		 * ╚══════╝
		 */
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -126.6f, 82f);
			Actions.setFakePlayerHotbarSlot(tank, 1);
		}, 42);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(1.1264, 0, -0.837), 1), 43);
		Utils.scheduleTask(() -> Actions.bonzo(tank, new Vector(1.2247, 0.5, -0.91)), 44);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -126.6f, 0f), 45);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -180f, 90f), 57);
		Utils.scheduleTask(() -> Actions.swingHand(tank), 58);
		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(tank, "terminal", 1, 8), 59);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 53.5f, 82f), 60);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-1.1278, 0, 0.8345), 1), 61);
		Utils.scheduleTask(() -> Actions.bonzo(tank, new Vector(-1.2263, 0.5, 0.907)), 62);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 53.5f, 0f), 63);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-1.1278, 0, 0.8345), 2), 76);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 53.5f, 82f), 77);
		Utils.scheduleTask(() -> Actions.bonzo(tank, new Vector(-1.2263, 0.5, 0.907)), 78);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 53.5f, 0f), 79);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 89f, 0f), 91);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-1.4028, 0, 0.0245), 4), 92);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 0f, 27f), 95);
		Utils.scheduleTask(() -> Actions.swingHand(tank), 96);
		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(tank, "terminal", 4, 8), 97);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 90f, 0f), 98);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-1.403, 0, 0), 2), 99);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.2806, 0, 0), 6), 101);
		Utils.scheduleTask(() -> Actions.lavaJump(tank, false), 108);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -15.5f, 0f), 109);
		Utils.scheduleTask(() -> Actions.bonzo(tank, new Vector(-0.4077, 0.5, -1.47)), 120);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 164.5f, 35f), 121);
		Utils.scheduleTask(() -> {
			Actions.rightClickLever(tank);
			Goldor.broadcastTerminalComplete(tank, "lever", 7, 8);
		}, 131);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 90f, 0f), 133);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-1.403, 0, 0), 2), 134);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.2806, 0, 0), 15), 136);
		Utils.scheduleTask(() -> {
			for(int i = 0; i < 21; i += 5) {
				Utils.scheduleTask(() -> {
					world.playSound(tank.getLocation(), Sound.BLOCK_CROP_BREAK, 2.0f, 1.0f);
					world.playSound(tank.getLocation(), Sound.BLOCK_GRASS_BREAK, 2.0f, 1.0f);
				}, i);
			}
			Goldor.broadcastTerminalComplete(tank, "gate", 2, 3);
		}, 137);

		/*
		 * ██████╗1
		 * ╚════██╗
		 *  █████╔╝
		 *  ╚═══██╗
		 * ██████╔╝
		 * ╚═════╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(tank, 140f, 0f), 150);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.9018, 0, -1.0748), 5), 151);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 140f, 82f), 155);
		Utils.scheduleTask(() -> Actions.bonzo(tank, new Vector(-0.9806, 0.5, -1.169)), 156);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 140f, 0f), 157);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 70f, 16.2f), 178);
		Utils.scheduleTask(() -> Actions.swingHand(tank), 179);
		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(tank, "terminal", 5, 7), 180);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 3), 181);
		Utils.scheduleTask(() -> Actions.leap(tank, Mage.get()), 182);

		/*
		 * ██╗  ██╗
		 * ██║  ██║
		 * ███████║
		 * ╚════██║
		 *      ██║
		 *      ╚═╝
		 */
		Utils.scheduleTask(() -> Actions.forceMove(tank, new Vector(0, 0, -1.403), 3), 183); // forceMove to get over the carpet
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -90f, 0f);
			Actions.setFakePlayerHotbarSlot(tank, 1);
		}, 185);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(1.403, 0, 0), 3), 186);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -90f, 82f), 188);
		Utils.scheduleTask(() -> Actions.bonzo(tank, new Vector(1.52552, 0.5, 0)), 189);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -90f, 0f), 190);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -56.1f, 19.8f), 203);
		Utils.scheduleTask(() -> Actions.swingHand(tank), 204);
		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(tank, "terminal", 3, 7), 205);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -90f, 0f), 206);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(1.403, 0, 0), 2), 207);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.2806, 0, 0), 15), 209);
		Utils.scheduleTask(() -> Actions.lavaJump(tank, true), 223);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -108.2f, -32.3f), 224);
		Utils.scheduleTask(() -> {
			Actions.rightClickLever(tank);
			Goldor.broadcastTerminalComplete(tank, "lever", 5, 7);
		}, 229);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 2), 230);
		Utils.scheduleTask(() -> Actions.leap(tank, Mage.get()), 231);

		/*
		 * ███████╗██╗ ██████╗ ██╗  ██╗████████╗
		 * ██╔════╝██║██╔════╝ ██║  ██║╚══██╔══╝
		 * █████╗  ██║██║  ███╗███████║   ██║
		 * ██╔══╝  ██║██║   ██║██╔══██║   ██║
		 * ██║     ██║╚██████╔╝██║  ██║   ██║
		 * ╚═╝     ╚═╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝
		 */
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 6), 232);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0, 0, -1.403), 11), 256);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -82.5f, -10f), 266);
		// tick 267: switch to baby yeti
		Utils.scheduleTask(() -> Actions.lastBreath(tank, 10), 268);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 26.6f, 0f), 278);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.5026, 0, 1.004), 4), 279);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.1256, 0, 0.251), 15), 283);
		Utils.scheduleTask(() -> Actions.lavaJump(tank, true), 298);
		Utils.scheduleTask(() -> {
			Utils.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_PLING, 2f, 0.5f);
			Bukkit.broadcastMessage(ChatColor.RED + " ☠ " + ChatColor.GOLD + "cookiethebald" + ChatColor.GRAY + " burned to death and became a ghost.");
			tank.setGameMode(GameMode.SPECTATOR);
			tank.setFlying(true);
		}, 314);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 3), 315);
		Utils.scheduleTask(() -> Actions.leap(tank, Healer.get()), 316);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 176f, 0f);
			Actions.move(tank, new Vector(0, 0.42, 0), 4);
		}, 317);
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(-0.07534, 0.001, -1.0774), 26), 321);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 180f, -10f), 347);
		Utils.scheduleTask(() -> {
			Bukkit.broadcastMessage(ChatColor.GREEN + " ❣ " + ChatColor.GOLD + "cookiethebald" + ChatColor.GREEN + " was revived by " + ChatColor.GOLD + "cookiethebald" + ChatColor.GREEN + "!");
			tank.setGameMode(GameMode.SURVIVAL);
			tank.setFlying(false);
		}, 414);
		if(doContinue) {
			Utils.scheduleTask(() -> necron(true), 350);
		}
	}

	private static void necron(boolean doContinue) {
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 6), 71);
		Utils.scheduleTask(() -> Actions.lastBreath(tank, 39), 120);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 2), 160);
		Utils.scheduleTask(() -> Actions.iceSpray(tank), 161);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 6), 162);
		Utils.scheduleTask(() -> Actions.lastBreath(tank, 19), 163);
		Utils.scheduleTask(() -> Actions.lastBreath(tank, 19), 183);
		Utils.scheduleTask(() -> Actions.lastBreath(tank, 19), 203);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 2), 223);
		Utils.scheduleTask(() -> Actions.iceSpray(tank), 224);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 5), 225);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 226);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 238);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 250);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 262);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 274);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 2), 277);
		Utils.scheduleTask(() -> Actions.iceSpray(tank), 278);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 5), 279);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 286);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 298);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 310);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 322);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 334);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 2), 337);
		Utils.scheduleTask(() -> Actions.iceSpray(tank), 338);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 5), 339);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 346);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 358);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 370);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 6), 371);
		Utils.scheduleTask(() -> Actions.lastBreath(tank, 19), 372);
		Utils.scheduleTask(() -> Actions.lastBreath(tank, 19), 392);
		Utils.scheduleTask(() -> Actions.lastBreath(tank, 19), 412);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 2), 413);
		Utils.scheduleTask(() -> Actions.iceSpray(tank), 414);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 5), 415);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 416);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 428);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 440);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 452);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 464);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 2), 467);
		Utils.scheduleTask(() -> Actions.iceSpray(tank), 468);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 5), 469);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 476);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 488);
		Utils.scheduleTask(() -> Actions.flamingFlay(tank), 500);
	}

	@SuppressWarnings("unused")
	public static Player get() {
		return tank;
	}
}