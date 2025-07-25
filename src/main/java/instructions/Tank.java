package instructions;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import plugin.Utils;

import java.util.Objects;

public class Tank {
	private static Player tank;
	@SuppressWarnings("FieldCanBeLocal")
	private static World world;

	public static void tankInstructions(Player p, String section) {
		tank = p;
		world = Tank.tank.getWorld();
		Objects.requireNonNull(tank.getInventory().getItem(4)).addUnsafeEnchantment(Enchantment.POWER, 2);

		if(section.equals("all") || section.equals("clear")) {
			Actions.teleport(Tank.tank, new Location(world, -196.5, 68, -222.5, 0f, 5.6f));
			Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(tank, 2, 29), 60);
			Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 2), 61);
			Utils.scheduleTask(() -> Actions.simulateRightClickAirWithSpectators(tank), 101);
			Utils.scheduleTask(() -> {
				Actions.setFakePlayerHotbarSlot(Tank.tank, 1);
				Actions.move(tank, new Vector(-0.8634, 0, 0), 5);
			}, 102);
			Utils.scheduleTask(() -> {
				Actions.teleport(tank, new Location(tank.getWorld(), -120.5, 75, -220.5));
				Actions.swapFakePlayerInventorySlots(Tank.tank, 2, 29);
			}, 141);
			// Tick 160 (clear tick 0: run begins)
			// Tick 161 (clear tick 1: teleport back
			Utils.scheduleTask(() -> clear(section.equals("all")), 162);
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
		Actions.simulateEtherwarp(tank, new Location(world, -196.5, 69, -216.5));
		Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Purple Flags Insta-Cleared");
		Utils.scheduleTask(() -> Actions.turnHead(tank, 90f, 1.8f), 1);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -229.5, 69, -216.5)), 2);

		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 196f, 15f);
			Actions.setFakePlayerHotbarSlot(tank, 7);
		}, 3);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(tank), 4);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 12f, -23f), 5);
		Utils.scheduleTask(() -> Actions.simulateAOTV(tank, new Location(world, -226.42, 66, -227.16)), 13);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(tank), 14);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -87f, -3f), 15);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(tank), 16);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -88.8f, 32.8f), 17);
		Utils.scheduleTask(() -> Actions.simulateAOTV(tank, new Location(world, -214.971, 67, -226.596)), 25);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Trap 1/3 (Opened Chest)");
			world.playSound(tank.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 26);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -118f, -79.5f);
			Actions.simulateAOTV(tank, new Location(world, -229.7, 71, -211.391));
		}, 27);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(tank), 28);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -102f, -8.9f), 29);
		Utils.scheduleTask(() -> Actions.simulateAOTV(tank, new Location(world, -226.71, 87.93905, -212.7)), 41);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(tank), 42);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -5.3f, -23.8f);
			Actions.setFakePlayerHotbarSlot(tank, 0);
		}, 43);
		Utils.scheduleTask(() -> Actions.simulateAOTV(tank, new Location(world, -222.8, 89.5, -213.3)), 46);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Trap 2/3 (Killed Bat)");
			world.playSound(tank.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0F, 1.0F);
		}, 47);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -78.5f, 67.3f);
			Actions.setFakePlayerHotbarSlot(tank, 5);
		}, 48);
		Utils.scheduleTask(() -> Actions.simulateStonking(tank, world.getBlockAt(-223, 89, -214)), 49);
		Utils.scheduleTask(() -> {
			Actions.move(tank, new Vector(0.8634, 0, 0), 1);
			Actions.turnHead(tank, -57f, 42.5f);
			Actions.setFakePlayerHotbarSlot(tank, 7);
		}, 50);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(tank), 51);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 21.6f, 48.48f), 52);
		Utils.scheduleTask(() -> Actions.simulateAOTV(tank, new Location(world, -215.217, 80, -208.649)), 60);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(tank), 61);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -103.27f, -88.35f), 62);
		Utils.scheduleTask(() -> Actions.simulateAOTV(tank, new Location(world, -216.323, 78, -205.898)), 65);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(tank), 66);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -84.80f, 19.01f), 67);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(tank), 73);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -90f, -50.1f);
			Actions.simulateAOTV(tank, new Location(world, -216.3, 88.68032, -205.7));
		}, 74);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Trap 3/3 (Opened Chest)");
			world.playSound(tank.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 75);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -90f, 33.3f);
			Actions.setFakePlayerHotbarSlot(tank, 5);
			Actions.simulateAOTV(tank, new Location(world, -212.5, 79, -205.5));
		}, 76);
		// Stone through wall from tick 77-84
		Utils.scheduleTask(() -> Actions.move(tank, new Vector(0.8634, 0, 0), 9), 77);
		Utils.scheduleTask(() -> Actions.simulateStonking(tank, world.getBlockAt(-212, 80, -206)), 77);
		Utils.scheduleTask(() -> Actions.simulateStonking(tank, world.getBlockAt(-212, 79, -206)), 78);
		Utils.scheduleTask(() -> Actions.simulateStonking(tank, world.getBlockAt(-211, 80, -206)), 79);
		Utils.scheduleTask(() -> Actions.simulateStonking(tank, world.getBlockAt(-211, 79, -206)), 80);
		Utils.scheduleTask(() -> Actions.simulateStonking(tank, world.getBlockAt(-210, 80, -206)), 81);
		Utils.scheduleTask(() -> Actions.simulateStonking(tank, world.getBlockAt(-210, 79, -206)), 82);
		Utils.scheduleTask(() -> Actions.simulateStonking(tank, world.getBlockAt(-209, 80, -206)), 83);
		Utils.scheduleTask(() -> Actions.simulateStonking(tank, world.getBlockAt(-209, 79, -206)), 84);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -149.42f, 32f);
			Actions.setFakePlayerHotbarSlot(tank, 7);
		}, 85);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(tank), 86);
		Utils.scheduleTask(() -> Actions.simulateAOTV(tank, new Location(world, -202.631, 69, -216.015)), 97);
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
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -190.5, 70, -217.5)), 101);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -99.2f, 8.9f), 102);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -167.5, 68, -221.5)), 103);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -180f, 30f);
			Actions.setFakePlayerHotbarSlot(tank, 5);
		}, 104);
		Utils.scheduleTask(() -> Actions.simulateStonking(tank, world.getBlockAt(-168, 69, -223)), 105);
		Utils.scheduleTask(() -> Actions.simulateStonking(tank, world.getBlockAt(-168, 68, -223)), 106);
		Utils.scheduleTask(() -> Actions.simulateStonking(tank, world.getBlockAt(-168, 68, -224)), 107);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Purple Flags 1/5 (Opened Chest)");
			world.playSound(tank.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 108);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 36.3f, 29.9f);
			Actions.setFakePlayerHotbarSlot(tank, 1);
		}, 109);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -178.5, 60, -206.5)), 110);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 86.5f, 0.4f), 111);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -195.5, 62, -205.5)), 112);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -15.6f, 70.2f), 113);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -194.5, 57, -203.5)), 114);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -90f, 53.2f), 115);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Purple Flags 2/5 (Obtained Wither Essence)");
			world.playSound(tank.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		}, 116);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -126.9f, 30.4f), 117);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -186.5, 53, -209.5)), 118);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -54.6f, 38.2f), 119);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -180.5, 49, -205.5)), 120);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -90f, 12.2f);
			Actions.setFakePlayerHotbarSlot(tank, 3);
		}, 121);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(tank, -180, 49, -206, -180, 53, -208), 122);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(tank, 1), 123);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -172.5, 49, -205.5)), 124);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -94.8f, 7.7f);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Purple Flags 3/5 (Obtained Item)");
			world.playSound(tank.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
		}, 125);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -160.5, 49, -206.5)), 126);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 0f, 15f), 127);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Purple Flags 4/5 (Opened Chest)");
			world.playSound(tank.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
			Actions.move(tank, new Vector(0, 0, -0.8634), 1);
		}, 128);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -96.7f, 7.7f), 129);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -151.5, 49, -208.5)), 130);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -90f, 35f);
			Actions.setFakePlayerHotbarSlot(tank, 5);
		}, 131);
		Utils.scheduleTask(() -> {
			Actions.move(tank, new Vector(1.12242, 0, 0), 7);
			Actions.simulateStonking(tank, world.getBlockAt(-151, 50, -209));
		}, 132);
		Utils.scheduleTask(() -> Actions.simulateStonking(tank, world.getBlockAt(-151, 49, -209)), 133);
		Utils.scheduleTask(() -> Actions.simulateStonking(tank, world.getBlockAt(-150, 50, -209)), 134);
		Utils.scheduleTask(() -> Actions.simulateStonking(tank, world.getBlockAt(-150, 49, -209)), 135);
		Utils.scheduleTask(() -> Actions.simulateStonking(tank, world.getBlockAt(-149, 50, -209)), 136);
		Utils.scheduleTask(() -> Actions.simulateStonking(tank, world.getBlockAt(-149, 49, -209)), 137);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -83.2f, 19.7f);
			Actions.setFakePlayerHotbarSlot(tank, 1);
		}, 138);
		Utils.scheduleTask(() -> Actions.simulateAOTV(tank, new Location(world, -143.5, 49, -207.5)), 139);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Purple Flags 5/5 (Opened Chest)");
			world.playSound(tank.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 140);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 58.1f, -59.9f), 141);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -146.5, 58, -205.5)), 142);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 91.7f, -30.8f), 143);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -162.5, 69, -205.5)), 144);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 90f, 4.5f), 145);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -184.5, 69, -205.5)), 146);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 0f, 5.3f), 147);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -184.5, 69, -187.5)), 148);

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
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(tank), 150);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -43.3f, -75.5f);
			Actions.setFakePlayerHotbarSlot(tank, 1);
		}, 151);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -182.5, 83, -185.5)), 152);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 65.5f, 30.3f), 153);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Red Blue 1/4 (Obtained Wither Essence)");
			world.playSound(tank.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		}, 154);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -95.7f, -0.1f), 155);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -156.5, 85, -188.5)), 156);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 111.7f, -9.3f), 157);
		Utils.scheduleTask(() -> Actions.simulateAOTV(tank, new Location(world, -164.5, 86, -191.5)), 158);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -97.1f, -2.7f);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Red Blue 2/4 (Obtained Item)");
			world.playSound(tank.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
		}, 159);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -148.5, 89, -193.5)), 160);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -120f, 0.7f);
			Actions.setFakePlayerHotbarSlot(tank, 5);
			Actions.move(tank, new Vector(0.22432, 0, -0.12951), 2);
		}, 161);
		Utils.scheduleTask(() -> Actions.simulateStonking(tank, world.getBlockAt(-146, 90, -195)), 162);
		Utils.scheduleTask(() -> Actions.simulateStonking(tank, world.getBlockAt(-146, 90, -196)), 163);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Red Blue 3/4 (Opened Chest)");
			world.playSound(tank.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 164);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -6.4f, 67.2f);
			Actions.setFakePlayerHotbarSlot(tank, 1);
		}, 165);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -146.5, 69, -184.5)), 166);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 55.1f, 6.4f), 167);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -158.5, 69, -176.5)), 168);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 66.3f, 1f);
			Actions.setFakePlayerHotbarSlot(tank, 5);
		}, 169);
		Utils.scheduleTask(() -> Actions.simulateStonking(tank, world.getBlockAt(-161, 70, -176)), 170);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Red Blue 4/4 (Opened Chest)");
			world.playSound(tank.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 171);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 102.7f, 4f);
			Actions.setFakePlayerHotbarSlot(tank, 4);
			Actions.simulateAOTV(tank, new Location(world, -212.058, 69, -183.541));
		}, 172);

		/*
		 * ███████╗██████╗ ██╗██████╗ ███████╗██████╗
		 * ██╔════╝██╔══██╗██║██╔══██╗██╔════╝██╔══██╗
		 * ███████╗██████╔╝██║██║  ██║█████╗  ██████╔╝
		 * ╚════██║██╔═══╝ ██║██║  ██║██╔══╝  ██╔══██╗
		 * ███████║██║     ██║██████╔╝███████╗██║  ██║
		 * ╚══════╝╚═╝     ╚═╝╚═════╝ ╚══════╝╚═╝  ╚═╝
		 */
		Utils.scheduleTask(() -> Actions.simulateRightClickAir(tank), 173);
		Utils.scheduleTask(() -> Actions.simulateLeftClickAir(tank), 177);
		Utils.scheduleTask(() -> Actions.simulateRightClickAir(tank), 178);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -31.6f, 16.6f);
			Actions.setFakePlayerHotbarSlot(tank, 1);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Spider Cleared");
		}, 179);
		Utils.scheduleTask(() -> Actions.simulateAOTV(tank, new Location(world, -206.027, 65.57762, -173.747)), 180);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 68.8f, 63.6f), 181);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -211.5, 55, -171.5)), 182);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 83f, 12.7f), 183);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -218.5, 55, -170.5)), 184);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -44.9f, 49.6f);
			Actions.setFakePlayerHotbarSlot(tank, 7);
		}, 185);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(tank), 186);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -165.1f, 16.3f);
			Actions.setFakePlayerHotbarSlot(tank, 1);
		}, 187);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -212.5, 50, -193.5)), 188);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 180f, 0f);
			Actions.move(tank, new Vector(0, 0, -1.12242), 3);
		}, 189);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -81.5f, 18.9f), 191);
		Utils.scheduleTask(() -> Actions.simulateAOTV(tank, new Location(world, -207.5, 50, -196.5)), 192);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Spider 2/9 (Opened Chest)");
			world.playSound(tank.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 193);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 52.2f, 6.8f);
			Actions.simulateAOTV(tank, new Location(world, -213.969, 47.5, -165.769));
		}, 194);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -222.5, 48, -159.5)), 195);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -40.5f, 4.5f);
			Actions.setFakePlayerHotbarSlot(tank, 7);
		}, 196);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(tank), 197);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 90f, 65f);
			Actions.setFakePlayerHotbarSlot(tank, 1);
		}, 198);
		Utils.scheduleTask(() -> Actions.simulateAOTV(tank, new Location(world, -223.5, 47, -159.5)), 199);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 90f, 90f);
			Actions.move(tank, new Vector(-1.12242, 0, 0), 1);
		}, 200);
		Utils.scheduleTask(() -> Actions.simulateAOTV(tank, new Location(world, -224.5, 36, -159.5)), 201);
		Utils.scheduleTask(() -> Actions.turnHead(tank, -90f, 35.3f), 202);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -210.5, 28, -159.5)), 203);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(tank);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Spider 4/9 (Opened Chest)");
			world.playSound(tank.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 204);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, -92.7f, -21.4f);
			Actions.simulateAOTV(tank, new Location(world, -215.275, 47, -152.695));
		}, 205);
		Utils.scheduleTask(() -> {
			Actions.simulateEtherwarp(tank, new Location(world, -177.5, 64, -154.5));
			Actions.simulateStonking(null, world.getBlockAt(-183, 84, -164)); // pass null to avoid hand swinging - this is so that the teleport packet in tick 210 works
		}, 206);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 149.5f, -61f), 207);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(tank, new Location(world, -182.5, 84, -163.5)), 208);
		Utils.scheduleTask(() -> {
			Actions.turnHead(tank, 90f, -90f);
			Actions.setFakePlayerHotbarSlot(tank, 0);
		}, 209);
		Utils.scheduleTask(() -> {
			Actions.simulateWitherImpact(tank, new Location(world, -182.5, 84, -163.5));
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Spider 6/9 (Killed Bat)");
			world.playSound(tank.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0F, 1.0F);
		}, 210);
		Utils.scheduleTask(() -> Actions.turnHead(tank, 90f, 90f), 211);
		Utils.scheduleTask(() -> {
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Spider 8/9 (Opened Chest)");
			world.playSound(tank.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
			Bukkit.broadcastMessage(ChatColor.GRAY + "Tank: Clear Finished in 214 Ticks (10.70 seconds)");
		}, 212);
	}

	@SuppressWarnings("unused")
	public static Player getTank() {
		return tank;
	}
}