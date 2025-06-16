package instructions;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import plugin.Utils;

public class Healer {
	private static Player healer;
	private static World world;

	public static void healerInstructions(Player p) {
		healer = p;
		world = healer.getWorld();
		Actions.turnHead(healer, -168.6f, 2.9f);
		Actions.simulateAOTV(healer, new Location(world, -28.5, 69, -44.5));
		Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(p, 2, 29), 60);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(p, 2), 61);
		Utils.scheduleTask(() -> Actions.simulateRightClickAir(p), 101);
		Utils.scheduleTask(() -> Actions.move(p, new Vector(0, 0, 0.8634), 5), 102);
		Utils.scheduleTask(() -> p.teleport(new Location(p.getWorld(), -120.5, 75, -220.5)), 141);
		// Tick 160 (clear tick 0: run begins)
		// Tick 161 (clear tick 1: teleport back)
		Utils.scheduleTask(Healer::clear, 162);
	}

	private static void clear() {
		/*
		 * ██████╗     ██╗    ██╗███████╗██╗██████╗ ██████╗  ██████╗ ███████╗
		 * ╚════██╗    ██║    ██║██╔════╝██║██╔══██╗██╔══██╗██╔═══██╗██╔════╝
		 *  █████╔╝    ██║ █╗ ██║█████╗  ██║██████╔╝██║  ██║██║   ██║███████╗
		 *  ╚═══██╗    ██║███╗██║██╔══╝  ██║██╔══██╗██║  ██║██║   ██║╚════██║
		 * ██████╔╝    ╚███╔███╔╝███████╗██║██║  ██║██████╔╝╚██████╔╝███████║
		 * ╚═════╝      ╚══╝╚══╝ ╚══════╝╚═╝╚═╝  ╚═╝╚═════╝  ╚═════╝ ╚══════╝
		 */
		// Tick 162 (clear tick 2, delay = 0)
		Actions.simulateAOTV(healer, new Location(world, -24.5, 69.5, -63.5));
		Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Pirate Insta-Cleared");
		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, -83.8f), 1);
		Utils.scheduleTask(() -> Actions.simulateAOTV(healer, new Location(world, -24.5, 81, -62.5)), 2);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 0f, 11.8f);
			Actions.setFakePlayerHotbarSlot(healer, 5);
		}, 3);
		Utils.scheduleTask(() -> {
			Actions.simulateRightClickAir(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Pirate Lever Activated");
			Server.activatePirateDoor();
		}, 4);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 180f, 72.4f);
			Actions.setFakePlayerHotbarSlot(healer, 1);
		}, 5);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -24.5, 69.5, -66.5)), 6);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 1.5f, 3.6f), 7);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -25.5, 69, -32.5)), 8);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -3.8f, 5.8f), 9);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -24.5, 69, -16.5)), 10);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -62.7f, 6.9f);
			Actions.setFakePlayerHotbarSlot(healer, 5);
		}, 11);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(healer);
			world.playSound(healer.getLocation(), Sound.ENTITY_DONKEY_HURT, 1.0F, 0.5F);
		}, 12);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, 0f), 13);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(healer);
			world.playSound(healer.getLocation(), Sound.ENTITY_DONKEY_HURT, 1.0F, 0.5F);
		}, 14);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 64.5f, 6.6f), 15);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(healer);
			world.playSound(healer.getLocation(), Sound.ENTITY_DONKEY_HURT, 1.0F, 0.5F);
		}, 16);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 45.4f, 39.2f), 17);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Three Weirdos Cleared");
			world.playSound(healer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 18);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 175.6f, 6.7f);
			Actions.setFakePlayerHotbarSlot(healer, 1);
		}, 19);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -25.5, 69, -30.5)), 20);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -175f, -1.3f), 21);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -21.5, 72, -77.5)), 22);

		/*
		 * ██████╗ ██╗██████╗  █████╗ ████████╗███████╗
		 * ██╔══██╗██║██╔══██╗██╔══██╗╚══██╔══╝██╔════╝
		 * ██████╔╝██║██████╔╝███████║   ██║   █████╗
		 * ██╔═══╝ ██║██╔══██╗██╔══██║   ██║   ██╔══╝
		 * ██║     ██║██║  ██║██║  ██║   ██║   ███████╗
		 * ╚═╝     ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝   ╚═╝   ╚══════╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(healer, 162.1f, -25.8f), 23);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -26.5, 82, -93.5)), 24);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -180f, 15.4f), 25);
		Utils.scheduleTask(() -> Actions.simulateAOTV(healer, new Location(world, -26.5, 82, -99.5)), 26);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Pirate 1/6 (Opened Chest)");
			world.playSound(healer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 27);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -18.4f, 14.9f), 28);
		Utils.scheduleTask(() -> Actions.simulateAOTV(healer, new Location(world, -24.5, 82, -93.5)), 29);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -20.6f, 46.6f), 30);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -16.5, 60, -72.5)), 31);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 129f, -2f);
			Actions.setFakePlayerHotbarSlot(healer, 7);
		}, 32);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(healer), 33);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -4.4f, -0.7f);
			Actions.setFakePlayerHotbarSlot(healer, 1);
		}, 34);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -14.5, 61.5, -47.5)), 35);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 85.4f, 12.7f), 36);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -28.5, 60, -46.5)), 37);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, 51f), 38);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Pirate 2/6 (Obtained Wither Essence)");
			world.playSound(healer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		}, 39);
		Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(healer, 6, 34), 41);
		Utils.scheduleTask(() -> Actions.simulateAOTV(healer, new Location(world, -25.539, 60, -79.631)), 42);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -90.9f, 6f);
			Actions.setFakePlayerHotbarSlot(healer, 7);
		}, 43);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(healer), 44);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 180f, 14.6f);
			Actions.setFakePlayerHotbarSlot(healer, 3);
		}, 45);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(healer, -27, 62, -84, -26, 60, -83), 46);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(healer, 0), 47);
		Utils.scheduleTask(() -> {
			Actions.simulateRightClickAir(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Pirate 3/6 (Killed Bat)");
			world.playSound(healer.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0F, 1.0F);
		}, 48);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -117.7f, 29.8f), 49);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Pirate 4/6 (Opened Chest)");
			world.playSound(healer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 50);
		Utils.scheduleTask(() -> Actions.simulateAOTV(healer, new Location(world, -16.657, 60, -80.053)), 51);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -173.3f, 4.5f);
			Actions.setFakePlayerHotbarSlot(healer, 1);
		}, 52);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -15.5, 60, -97.5)), 53);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 88.9f, -2.2f);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Pirate 5/6 (Obtained Item)");
			world.playSound(healer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
		}, 54);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -67.5f, 64, -96.5f)), 55);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -90f, 27f);
			Actions.setFakePlayerHotbarSlot(healer, 7);
		}, 56);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(healer), 57);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -128.9f, -75.3f);
			Actions.setFakePlayerHotbarSlot(healer, 1);
		}, 58);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -63.5, 84, -99.5)), 59);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -82.1f, 16f), 60);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -57.5, 83, -98.5)), 61);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Pirate 6/6 (Opened Chest)");
			world.playSound(healer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 62);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -98.8f, -4f), 63);
		Utils.scheduleTask(() -> Actions.simulateAOTV(healer, new Location(world, -59.735, 60, -96.457)), 64);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -48.5, 63, -98.5)), 65);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 17.6f, -44.6f), 66);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -50.5, 70, -93.5)), 67);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 79.4f, 4.8f), 68);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -81.5, 69, -87.5)), 69);

		/*
		 * ███████╗ ██████╗ ██████╗ ██████╗
		 * ╚══███╔╝██╔═══██╗██╔══██╗██╔══██╗
		 *   ███╔╝ ██║   ██║██║  ██║██║  ██║
		 *  ███╔╝  ██║   ██║██║  ██║██║  ██║
		 * ███████╗╚██████╔╝██████╔╝██████╔╝
		 * ╚══════╝ ╚═════╝ ╚═════╝ ╚═════╝
		 */
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 109f, 11.7f);
			Actions.setFakePlayerHotbarSlot(healer, 4);
		}, 70);
		Utils.scheduleTask(() -> Actions.simulateRightClickAir(healer), 71);
		Utils.scheduleTask(() -> Actions.simulateLeftClickAir(healer), 75);
		Utils.scheduleTask(() -> Actions.simulateRightClickAir(healer), 76);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 72.5f, 9f);
			Actions.setFakePlayerHotbarSlot(healer, 7);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Zodd Cleared");
		}, 77);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(healer), 78);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 86.8f, 5.2f);
			Actions.setFakePlayerHotbarSlot(healer, 1);
		}, 79);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -97.5, 69.5, -86.5)), 80);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, 12.5f), 81);
		Utils.scheduleTask(() -> Actions.simulateAOTV(healer, new Location(world, -97.5, 70, -81.5)), 82);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 72.8f, 20.4f), 83);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Zodd 1/1 (Opened Chest)");
			world.playSound(healer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 84);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 0f, -10f);
			Actions.simulateAOTV(healer, new Location(world, -89.68, 67.5, -84.89));
		}, 85);
		Utils.scheduleTask(() -> Actions.simulateAOTV(healer, new Location(world, -89.68, 69, -73.068)), 86);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -4.6f, 6.4f), 87);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -88.5, 69, -58.5)), 88);

		/*
		 * ███╗   ███╗███████╗██╗      ██████╗ ███╗   ██╗
		 * ████╗ ████║██╔════╝██║     ██╔═══██╗████╗  ██║
		 * ██╔████╔██║█████╗  ██║     ██║   ██║██╔██╗ ██║
		 * ██║╚██╔╝██║██╔══╝  ██║     ██║   ██║██║╚██╗██║
		 * ██║ ╚═╝ ██║███████╗███████╗╚██████╔╝██║ ╚████║
		 * ╚═╝     ╚═╝╚══════╝╚══════╝ ╚═════╝ ╚═╝  ╚═══╝
		 */
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(healer, 4), 89);
		Utils.scheduleTask(() -> {
			Actions.simulateRightClickAir(healer);
			healer.removeScoreboardTag("SalvationCooldown"); // sb in vanilla mc plugin has a 1s cd on salvation, but this does not exist in hypixel
		}, 90);
		Utils.scheduleTask(() -> Actions.simulateLeftClickAir(healer), 91);
		Utils.scheduleTask(() -> {
			Actions.simulateRightClickAir(healer);
			healer.removeScoreboardTag("SalvationCooldown"); // sb in vanilla mc plugin has a 1s cd on salvation, but this does not exist in hypixel
		}, 95);
		Utils.scheduleTask(() -> Actions.simulateLeftClickAir(healer), 96);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -91f, -10.5f);
			Actions.setFakePlayerHotbarSlot(healer, 7);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Melon Cleared");
		}, 97);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(healer), 98);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -173f, -57f), 99);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(healer), 100);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 164f, 8.3f), 101);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(healer), 102);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -70.9f, -19.3f);
			Actions.setFakePlayerHotbarSlot(healer, 1);
		}, 103);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -74.5, 76, -53.5)), 104);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -166.8f, -32.3f), 105);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -73.5, 80.5, -57.5)), 106);
		Utils.scheduleTask(() -> Actions.move(healer, new Vector(0, 0, -1.1224), 2), 107);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 161.5f, 29.7f), 108);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Melon 1/7 (Opened Chest)");
			world.playSound(healer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 109);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 83f, 37.9f), 110);
		Utils.scheduleTask(() -> Actions.simulateAOTV(healer, new Location(world, -91.682, 71, -69.267)), 111);
		Utils.scheduleTask(() -> Actions.simulateAOTV(healer, new Location(world, -96.5, 69, -68.5)), 112);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 80.2f, 16.3f);
			Actions.setFakePlayerHotbarSlot(healer, 6);
		}, 113);
		Utils.scheduleTask(() -> Actions.simulateStonking(healer, world.getBlockAt(-100, 69, -69)), 114);
		Utils.scheduleTask(() -> Actions.simulateStonking(healer, world.getBlockAt(-100, 69, -68)), 115);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Melon 2/7 (Opened Chest)");
			world.playSound(healer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 116);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -81f, 12.8f);
			Actions.setFakePlayerHotbarSlot(healer, 1);
			Actions.simulateAOTV(healer, new Location(world, -87.339, 84.5, -70.463));
		}, 117);
		Utils.scheduleTask(() -> Actions.simulateAOTV(healer, new Location(world, -82.5, 85, -69.5)), 118);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, 16.8f), 119);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Melon 3/7 (Opened Chest)");
			world.playSound(healer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
			Actions.simulateAOTV(healer, new Location(world, -60.722, 69, -59.041));
		}, 120);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 25.9f, -5f);
			Actions.setFakePlayerHotbarSlot(healer, 7);
		}, 121);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(healer), 122);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -69.2f, 4.9f);
			Actions.setFakePlayerHotbarSlot(healer, 1);
		}, 123);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -43.5, 69, -52.5)), 124);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 0f, 5f);
			Actions.setFakePlayerHotbarSlot(healer, 3);
		}, 125);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(healer, -44, 69, -51, -46, 74, -50), 126);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(healer, 1), 127);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -43.5, 69.5, -35.5)), 128);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 97.5f, -9.5f);
			Actions.setFakePlayerHotbarSlot(healer, 0);
		}, 129);
		Utils.scheduleTask(() -> {
			Actions.simulateRightClickAir(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Melon 4/7 (Killed Bat)");
			world.playSound(healer.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0F, 1.0F);
		}, 130);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 124.6f, 16f);
			Actions.setFakePlayerHotbarSlot(healer, 1);
		}, 131);
		Utils.scheduleTask(() -> Actions.simulateAOTV(healer, new Location(world, -57.5, 69.5, -40.5)), 132);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -166.2f, 22.6f), 133);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Melon 5/7 (Opened Chest)");
			world.playSound(healer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
			Actions.simulateAOTV(healer, new Location(world, -67.697, 69, -45.179));
		}, 134);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -0.4f, 4.8f), 135);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -67.5, 69, -25.5)), 136);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -18f, -69f);
			Actions.setFakePlayerHotbarSlot(healer, 7);
		}, 137);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(healer), 138);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 85.7f, 0f), 139);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(healer), 140);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -45.3f, -32.8f);
			Actions.setFakePlayerHotbarSlot(healer, 1);
		}, 141);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -62.5, 74.6875, -20.5)), 142);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -55f, -26.5f), 143);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -53.5, 82, -14.5)), 144);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -82.3f, 18.5f), 145);
		Utils.scheduleTask(() -> Actions.simulateAOTV(healer, new Location(world, -48.5, 82, -13.5)), 146);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Melon 6/7 (Opened Chest)");
			world.playSound(healer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 147);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 6.4f, 23f);
			Actions.simulateAOTV(healer, new Location(world, -66.3, 81.30773, -21.152));
		}, 148);
		Utils.scheduleTask(() -> Actions.simulateAOTV(healer, new Location(world, -67.5, 79, -12.5)), 149);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Melon 7/7 (Opened Chest)");
			world.playSound(healer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
			Actions.simulateAOTV(healer, new Location(world, -81.627, 69, -24.343));
		}, 150);

		/*
		 *  █████╗ ██████╗ ███╗   ███╗██╗███╗   ██╗
		 * ██╔══██╗██╔══██╗████╗ ████║██║████╗  ██║
		 * ███████║██║  ██║██╔████╔██║██║██╔██╗ ██║
		 * ██╔══██║██║  ██║██║╚██╔╝██║██║██║╚██╗██║
		 * ██║  ██║██████╔╝██║ ╚═╝ ██║██║██║ ╚████║
		 * ╚═╝  ╚═╝╚═════╝ ╚═╝     ╚═╝╚═╝╚═╝  ╚═══╝
		 */
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 91f, 2f);
			Actions.setFakePlayerHotbarSlot(healer, 4);
		}, 151);
		Utils.scheduleTask(() -> Actions.simulateRightClickAir(healer), 152);
		Utils.scheduleTask(() -> Actions.simulateLeftClickAir(healer), 156);
		Utils.scheduleTask(() -> Actions.simulateRightClickAir(healer), 157);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 88.6f, 0.4f);
			Actions.setFakePlayerHotbarSlot(healer, 1);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Admin Cleared");
		}, 158);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -120.5, 71, -23.5)), 159);

		/*
		 * ███╗   ███╗██╗   ██╗███████╗███████╗██╗   ██╗███╗   ███╗
		 * ████╗ ████║██║   ██║██╔════╝██╔════╝██║   ██║████╗ ████║
		 * ██╔████╔██║██║   ██║███████╗█████╗  ██║   ██║██╔████╔██║
		 * ██║╚██╔╝██║██║   ██║╚════██║██╔══╝  ██║   ██║██║╚██╔╝██║
		 * ██║ ╚═╝ ██║╚██████╔╝███████║███████╗╚██████╔╝██║ ╚═╝ ██║
		 * ╚═╝     ╚═╝ ╚═════╝ ╚══════╝╚══════╝ ╚═════╝ ╚═╝     ╚═╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(healer, 66.6f, 7.8f), 160);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -133.5, 71, -17.5)), 161);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 56.7f, 43.4f), 162);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -136.5, 69, -15.5)), 163);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 0f, 0f);
			Actions.setFakePlayerHotbarSlot(healer, 3);
		}, 164);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(healer, -138, 69, -15, -136, 74, -14), 165);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Museum 1/5 (Opened Chest)");
			world.playSound(healer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 166);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -130.4f, -44.3f);
			Actions.setFakePlayerHotbarSlot(healer, 1);
		}, 167);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -128.5, 82, -22.5)), 168);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -64.5f, -61.9f), 169);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -124.5, 93, -20.5)), 170);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 29.1f, 31.7f), 171);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Museum 2/5 (Obtained Wither Essence)");
			world.playSound(healer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		}, 172);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -171f, 46.3f), 173);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -122.5, 82, -32.5)), 174);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 152.8f, 3.5f), 175);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -134.5, 82, -56.5)), 176);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -135f, 0f), 177);
		Utils.scheduleTask(() -> Actions.simulateAOTV(healer, new Location(world, -133.5, 82, -57.5)), 178);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -128f, 37.2f), 179);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -132.5, 82, -58.5)), 180);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 135f, 77.5f);
			Actions.move(healer, new Vector(-0.183848, 0, -0.183848), 1);
		}, 181);
		Utils.scheduleTask(() -> Actions.simulateLeftClickAir(healer), 182);
		Utils.scheduleTask(() -> Actions.simulateStonking(healer, world.getBlockAt(-133, 81, -59)), 183);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 157.9f, 20.1f), 184);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Museum 3/5 (Opened Chest)");
			world.playSound(healer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 185);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 136.1f, 71.7f), 186);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -135.5, 70, -61.5)), 187);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 135f, 50f);
			Actions.setFakePlayerHotbarSlot(healer, 4);
		}, 188);
		Utils.scheduleTask(() -> {
			Actions.simulateRightClickAir(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Museum 4/5 (Mimic Chest)");
			Actions.mimicChest(healer, world.getBlockAt(-137, 70, -63));
		}, 189);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -89.1f, 7.6f);
			Actions.setFakePlayerHotbarSlot(healer, 1);
		}, 190);
		Utils.scheduleTask(() -> {
			Actions.simulateEtherwarp(healer, new Location(world, -128.5, 71, -61.5));
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Mimic Killed!");
		}, 191);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -97.7f, 23.6f), 192);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -120.5, 69, -62.5)), 193);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 121.4f, 15.1f), 194);
		Utils.scheduleTask(() -> Actions.simulateAOTV(healer, new Location(world, -125.5, 69, -65.5)), 195);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -90f, 90f);
			Actions.setFakePlayerHotbarSlot(healer, 3);
		}, 196);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(healer, -123, 68, -63, -126, 67, -70), 197);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -90f, 60f), 198);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(world, -120.5, 62, -65.5)), 199);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Museum 5/5 (Opened Chest)");
			world.playSound(healer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Clear Finished in 202 Ticks (10.1 seconds)");
		}, 200);
	}

	@SuppressWarnings("unused")
	public static Player getHealer() {
		return healer;
	}
}
