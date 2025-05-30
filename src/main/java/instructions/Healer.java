package instructions;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import plugin.Utils;

public class Healer {
	private static Player healer;

	public static void healerInstructions(Player p) {
		healer = p;
		System.out.println("Healer Instructions: " + healer.getName());
		healer.teleport(new Location(p.getWorld(), -28.5, 70, -44.5, -168.6f, 2.9f));
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
		Actions.simulateAOTV(healer, new Location(healer.getWorld(), -24.5, 69, -63.5));
		Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Pirate Insta-Cleared");
		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, -83.8f), 1);
		Utils.scheduleTask(() -> Actions.simulateAOTV(healer, new Location(healer.getWorld(), -24.5, 81, -62.5)), 2);
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
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(healer.getWorld(), -24.5, 69.5, -66.5)), 6);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 1.5f, 3.6f), 7);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(healer.getWorld(), -25.5, 69, -32.5)), 8);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -3.8f, 5.8f), 9);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(healer.getWorld(), -24.5, 69, -16.5)), 10);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, -62.7f, 6.9f);
			Actions.setFakePlayerHotbarSlot(healer, 5);
		}, 11);
		Utils.scheduleTask(() -> Actions.simulateRightClickAir(healer), 12);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 0f, 0f), 13);
		Utils.scheduleTask(() -> Actions.simulateRightClickAir(healer), 14);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 64.5f, 6.6f), 15);
		Utils.scheduleTask(() -> Actions.simulateRightClickAir(healer), 16);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 45.4f, 39.2f), 17);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Healer: Three Weirdos Cleared");
			healer.getWorld().playSound(healer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 18);
		Utils.scheduleTask(() -> {
			Actions.turnHead(healer, 175.6f, 6.7f);
			Actions.setFakePlayerHotbarSlot(healer, 1);
		}, 19);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(healer.getWorld(), -25.5, 69, -30.5)), 20);
		Utils.scheduleTask(() -> Actions.turnHead(healer, 175f, -1.3f), 21);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(healer.getWorld(), -21.5, 72, -77.5)), 22);

		/*
		 * ██████╗ ██╗██████╗  █████╗ ████████╗███████╗
		 * ██╔══██╗██║██╔══██╗██╔══██╗╚══██╔══╝██╔════╝
		 * ██████╔╝██║██████╔╝███████║   ██║   █████╗
		 * ██╔═══╝ ██║██╔══██╗██╔══██║   ██║   ██╔══╝
		 * ██║     ██║██║  ██║██║  ██║   ██║   ███████╗
		 * ╚═╝     ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝   ╚═╝   ╚══════╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(healer, 162.1f, -25.8f), 23);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(healer.getWorld(), -26.5, 82, -93.5)), 24);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -180f, -15.4f), 25);
		Utils.scheduleTask(() -> Actions.simulateAOTV(healer, new Location(healer.getWorld(), -26.5, 82, -99.5)), 26);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(healer);
			Bukkit.broadcastMessage(ChatColor.YELLOW + "Berserk: Pirate 1/6 (Opened Chest)");
			healer.getWorld().playSound(healer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 27);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -18.4f, 14.9f), 28);
		Utils.scheduleTask(() -> Actions.simulateAOTV(healer, new Location(healer.getWorld(), -24.5, 82, -93.5)), 29);
		Utils.scheduleTask(() -> Actions.turnHead(healer, -20.6f, 46.6f), 30);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(healer, new Location(healer.getWorld(), -16.5, 60, -72.5)), 31);

		// tp down: /execute in minecraft:overworld run tp @s -24.50 82.00 -93.50 -20.65 46.63
		// pearl travels 9 ticks, lands at -25.341 60 -79.862
		// pearl after getting to 3&4/6: /execute in minecraft:overworld run tp @s -25.34 60.00 -79.95 -90.9 6
		// pearl travels 7 ticks, lands at -16.657 60 -80.053
		// pearl before 6/6: /execute in minecraft:overworld run tp @s -67.50 64.00 -96.50 -89.78 20.83
		// pearl travels 8 ticks, lands -58.080 61 -96.481
	}

	public static Player getHealer() {
		return healer;
	}
}
