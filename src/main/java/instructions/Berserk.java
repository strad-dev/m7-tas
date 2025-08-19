package instructions;

import instructions.bosses.Maxor;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import plugin.Utils;

import java.util.List;
import java.util.Objects;

public class Berserk {
	private static Player berserk;
	private static World world;

	public static void berserkInstructions(Player p, String section) {
		berserk = p;
		world = berserk.getWorld();
		Objects.requireNonNull(berserk.getInventory().getItem(4)).addUnsafeEnchantment(Enchantment.POWER, 2);

		switch(section) {
			case "all", "clear" -> {
				Actions.teleport(berserk, new Location(world, -21.5, 70, -197.5, 90f, 36.5f));
				Utils.scheduleTask(() -> {
					Actions.swapFakePlayerInventorySlots(berserk, 2, 29);
					Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Tic Tac Toe Pre-Cleared");
				}, 60);
				Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 2), 61);
				Utils.scheduleTask(() -> Actions.simulateRightClickAirWithSpectators(berserk), 101);
				Utils.scheduleTask(() -> {
					Actions.setFakePlayerHotbarSlot(berserk, 1);
					Actions.move(berserk, new Vector(0, 0, -0.8634), 4);
				}, 102);
				Utils.scheduleTask(() -> {
					Actions.teleport(berserk, new Location(world, -120.5, 75, -220.5));
					Actions.swapFakePlayerInventorySlots(berserk, 2, 29);
				}, 141);
				Utils.scheduleTask(() -> clear(section.equals("all")), 162);
			}
			case "maxor", "boss" -> {
				Actions.teleport(berserk, new Location(world, 73.5, 221, 13.5, 0f, 0f));
				Actions.swapFakePlayerInventorySlots(berserk, 11, 36);
				Actions.swapFakePlayerInventorySlots(berserk, 1, 28);
				Actions.swapFakePlayerInventorySlots(berserk, 7, 35);
				if(section.equals("maxor")) {
					Utils.scheduleTask(() -> maxor(false), 60);
				} else {
					Utils.scheduleTask(() -> maxor(true), 60);
				}
			}
			case "storm" -> {
				Actions.teleport(berserk, new Location(world, 100.422, 169, 49.624, -1f, 23f));
				Actions.swapFakePlayerInventorySlots(berserk, 11, 36);
				Actions.swapFakePlayerInventorySlots(berserk, 1, 28);
				Actions.swapFakePlayerInventorySlots(berserk, 7, 35);
				Utils.scheduleTask(() -> storm(false), 60);
			}
		}
	}

	private static void clear(boolean doContinue) {
		/*
		 * ████████╗██╗ ██████╗    ████████╗ █████╗  ██████╗    ████████╗ ██████╗ ███████╗
		 * ╚══██╔══╝██║██╔════╝    ╚══██╔══╝██╔══██╗██╔════╝    ╚══██╔══╝██╔═══██╗██╔════╝
		 *    ██║   ██║██║            ██║   ███████║██║            ██║   ██║   ██║█████╗
		 *    ██║   ██║██║            ██║   ██╔══██║██║            ██║   ██║   ██║██╔══╝
		 *    ██║   ██║╚██████╗       ██║   ██║  ██║╚██████╗       ██║   ╚██████╔╝███████╗
		 *    ╚═╝   ╚═╝ ╚═════╝       ╚═╝   ╚═╝  ╚═╝ ╚═════╝       ╚═╝    ╚═════╝ ╚══════╝
		 */
		// Tick 162 (clear tick 2, delay = 0)
		Actions.simulateAOTV(berserk, new Location(world, -24.5, 69, -197.5));
		Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers Insta-Cleared");
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -175.3f, 3.5f), 1);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(world, -22.5, 69, -223.5)), 2);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -126.9f, 12.2f);
			Actions.setFakePlayerHotbarSlot(berserk, 5);
		}, 3);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, world.getBlockAt(-22, 70, -224)), 4);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, world.getBlockAt(-22, 70, -225)), 5);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, world.getBlockAt(-21, 70, -225)), 6);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Tic Tac Toe 1/1 (Opened Chest)");
			world.playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 7);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 3.1f, 2.4f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 8);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -24.5, 69, -184.5)), 9);

		/*
		 *  ██████╗██╗  ██╗ █████╗ ███╗   ███╗██████╗ ███████╗██████╗ ███████╗
		 * ██╔════╝██║  ██║██╔══██╗████╗ ████║██╔══██╗██╔════╝██╔══██╗██╔════╝
		 * ██║	   ███████║███████║██╔████╔██║██████╔╝█████╗  ██████╔╝███████╗
		 * ██║	   ██╔══██║██╔══██║██║╚██╔╝██║██╔══██╗██╔══╝  ██╔══██╗╚════██║
		 * ╚██████╗██║  ██║██║  ██║██║ ╚═╝ ██║██████╔╝███████╗██║  ██║███████║
		 *  ╚═════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝	 ╚═╝╚═════╝ ╚══════╝╚═╝  ╚═╝╚══════╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -60.4f, 28), 10);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(world, -20.5, 69, -182.5)), 11);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -138.5f, 90), 12);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(world, -20.5, 59, -182.5)), 13);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -134.2f, 51.1f), 14);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 1/5 (Opened Chest)");
			world.playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 15);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0, -90), 16);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(world, -20.5, 72, -182.5)), 17);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 81.0f, -67.7f), 18);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -23.5, 82, -181.5)), 19);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 57.5f, 9.3f), 20);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -31.5, 82, -176.5)), 21);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 60.1f, 38.9f);
			Actions.setFakePlayerHotbarSlot(berserk, 0);
		}, 22);
		Utils.scheduleTask(() -> {
			Actions.simulateWitherImpact(berserk, new Location(world, -35.5, 80, -174.5));
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 2/5 (Killed Bat)");
			world.playSound(berserk.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0F, 1.0F);
		}, 23);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 161.0f, 9.5f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 24);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -38.5, 80, -183.5)), 25);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 108.8f, 5.1f), 26);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -55.5, 80, -189.5)), 27);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -144.1f, 3.7f), 28);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -45.5, 81, -203.5)), 29);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -168.7f, 9.2f), 30);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -44.5, 81, -213.5)), 31);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 146.5f, -2.3f);
			Actions.setFakePlayerHotbarSlot(berserk, 3);
		}, 32);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(berserk, -47, 81, -217, -46, 84, -216), 33);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 1), 34);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -54.5, 83.5, -228.5)), 35);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 116.2f, 38.9f), 36);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 3/5 (Opened Chest)");
			world.playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 37);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0.7f, -3.3f), 38);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -54.5, 86, -219.5)), 39);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 0f, 45f);
			Actions.setFakePlayerHotbarSlot(berserk, 5);
		}, 40);
		Utils.scheduleTask(() -> {
			Actions.simulateStonking(berserk, world.getBlockAt(-55, 87, -219));
			Actions.move(berserk, new Vector(0, 0, 1.12242), 3);
		}, 41);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, world.getBlockAt(-55, 86, -219)), 42);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 14.8f, 45f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 43);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -59.5, 69, -198.5)), 44);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 36f, 16f), 45);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(world, -62.5, 69, -194.5)), 46);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 5), 47);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, world.getBlockAt(-64, 70, -194)), 48);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 4/5 (Opened Chest)");
			world.playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 49);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -90f, 14.9f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 50);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -56.5, 69, -194.5)), 51);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 9.3f, 5.2f), 52);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -58.5, 69, -176.5)), 53);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 75f, 15.9f);
			Actions.setFakePlayerHotbarSlot(berserk, 3);
		}, 54);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(berserk, -61, 69, -177, -62, 73, -174), 55);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 1), 56);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(world, -64.5, 69.0625, -175.5)), 57);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 5/5 (Opened Chest)");
			world.playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 58);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -95.4f, -3.2f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 59);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -54.5, 71.5, -176.5)), 60);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 165.2f, 6.8f), 61);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -64.5, 69, -215.5)), 62);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 90f, 4.4f), 63);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -86.5, 69, -215.5)), 64);

		/*
		 *  ██████╗ █████╗ ████████╗██╗    ██╗ █████╗ ██╗     ██╗  ██╗
		 * ██╔════╝██╔══██╗╚══██╔══╝██║    ██║██╔══██╗██║     ██║ ██╔╝
		 * ██║     ███████║   ██║   ██║ █╗ ██║███████║██║     █████╔╝
		 * ██║     ██╔══██║   ██║   ██║███╗██║██╔══██║██║     ██╔═██╗
		 * ╚██████╗██║  ██║   ██║   ╚███╔███╔╝██║  ██║███████╗██║  ██╗
		 *  ╚═════╝╚═╝  ╚═╝   ╚═╝    ╚══╝╚══╝ ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝
		 */
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 4), 65);
		Utils.scheduleTask(Berserk::simulateShoot, 66);
		Utils.scheduleTask(Berserk::simulateShoot, 71);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 53.4f, 2.2f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Catwalk Cleared");
		}, 72);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -93.5, 70.5, -210.5)), 73);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 64.3f, 34.8f), 74);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -101.5, 66, -206.5)), 75);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -152.6f, 69.1f), 76);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Catwalk 1/6 (Opened Chest)");
			world.playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 77);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -120.0f, 26.4f), 78);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -93.5, 63, -211.5)), 79);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -33.2f, 68.7f), 80);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -96.5, 50, -206.5)), 81);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -96.4f, 17.5f);
			Actions.setFakePlayerHotbarSlot(berserk, 3);
		}, 82);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(berserk, -94, 50, -207, -93, 53, -209), 83);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 1), 84);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(world, -90.5, 50, -207.5)), 85);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Catwalk 2/6 (Opened Chest)");
			world.playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 86);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 72.4f, 16.4f), 87);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(world, -95.5, 50, -205.5)), 88);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -2.6f, -18.9f), 89);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -94.5, 59, -184.5)), 90);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 1.6f, -2.4f), 91);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -95.5, 63, -146.5)), 92);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -106.2f, 12.2f), 93);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(world, -92.5, 64, -147.5)), 94);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -90.4f, 22.1f), 95);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Catwalk 3/6 (Opened Chest)");
			world.playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 96);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 90f, 36.6f), 97);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(world, -95.5, 63, -147.5)), 98);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -85.1f, 87.4f), 99);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -94.5, 50, -147.5)), 100);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0f, 24.1f), 101);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(world, -94.5, 49, -141.5)), 102);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -110.2f, 18.8f), 103);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Catwalk 4/6 (Opened Chest)");
			world.playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 104);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 180f, 1.7f), 105);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -94.5, 51, -148.5)), 106);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -114.5f, 16.2f), 107);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(world, -91.5, 50, -149.5)), 108);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -90f, 0f);
			Actions.setFakePlayerHotbarSlot(berserk, 3);
			Actions.move(berserk, new Vector(1.12242, 0, 0), 5);
		}, 109);
		Utils.scheduleTask(() -> Actions.simulateCrypt(berserk, -86, 49, -150, -84, 51, -149), 110);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 4), 111);
		Utils.scheduleTask(Berserk::simulateShoot, 112);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -106f, 12.6f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Crypt 4/5");
		}, 113);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(world, -80.5, 49, -150.5)), 114);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 135.4f, 14.9f);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Catwalk 5/6 (Obtained Item)");
			world.playSound(berserk.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
		}, 115);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Catwalk 6/6 (Obtained Wither Essence)");
			world.playSound(berserk.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		}, 116);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 5.3f, 3.3f), 117);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(world, -81.5, 49, -139.5)), 118);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -160.2f, -65.2f), 119);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -78.5, 70.5, -148.5)), 120);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -136.3f, 28.3f), 121);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(world, -74.5, 69, -152.5)), 122);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -95f, 2.8f), 123);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(world, -41.5, 69, -155.5)), 124);

		/*
		 * ██████╗ ██╗███╗   ██╗ ██████╗     ██████╗ ██╗ ██████╗     ███████╗██╗████████╗███████╗
		 * ██╔══██╗██║████╗  ██║██╔═══██╗    ██╔══██╗██║██╔════╝     ██╔════╝██║╚══██╔══╝██╔════╝
		 * ██║  ██║██║██╔██╗ ██║██║   ██║    ██║  ██║██║██║  ███╗    ███████╗██║   ██║   █████╗
		 * ██║  ██║██║██║╚██╗██║██║   ██║    ██║  ██║██║██║   ██║    ╚════██║██║   ██║   ██╔══╝
		 * ██████╔╝██║██║ ╚████║╚██████╔╝    ██████╔╝██║╚██████╔╝    ███████║██║   ██║   ███████╗
		 * ╚═════╝ ╚═╝╚═╝  ╚═══╝ ╚═════╝     ╚═════╝ ╚═╝ ╚═════╝     ╚══════╝╚═╝   ╚═╝   ╚══════╝
		 */
		Utils.scheduleTask(() -> {
			Actions.setFakePlayerHotbarSlot(berserk, 4);
			Actions.turnHead(berserk, -62.8f, 1.4f);
		}, 125);
		Utils.scheduleTask(Berserk::simulateShoot, 126);
		Utils.scheduleTask(Berserk::simulateShoot, 131);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 85.0f, 7.0f);
			Actions.setFakePlayerHotbarSlot(berserk, 7);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Dino Dig Site Cleared");
		}, 132);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(berserk), 133);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -32.0f, -53.5f), 134);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -33.5, 91, -142.5)), 135);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 0f, 10f);
			Actions.setFakePlayerHotbarSlot(berserk, 3);
		}, 136);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(berserk, -35, 92, -141, -33, 96, -140), 137);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 1), 138);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(world, -33.5, 92, -136.5)), 139);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Dino Dig Site 1/4 (Opened Chest)");
			world.playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
			Actions.simulateAOTV(berserk, new Location(world, -50.059, 68.82469, -151.404));
		}, 140);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 44f, 65.2f), 141);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -54.5, 57, -146.5)), 142);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0f, 0f), 143);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(world, -54.5, 57, -144.5)), 144);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 135.7f, 34.6f);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Dino Dig Site 2/4 (Obtained Item)");
			world.playSound(berserk.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
		}, 145);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(world, -61.5, 50, -151.5)), 146);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 90f, 40.2f), 147);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -63.5, 50, -151.5)), 148);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 180.0f, -6.1f);
			Actions.setFakePlayerHotbarSlot(berserk, 5);
		}, 149);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, world.getBlockAt(-64, 51, -156)), 150);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Dino Dig Site 3/4 (Opened Chest)");
			world.playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 151);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -70.5f, -23.5f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 152);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -50.5, 58, -146.5)), 153);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -111.1f, 19.6f), 154);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -18.5, 48, -158.5)), 155);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -10.7f, 10.4f), 156);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -16.5, 48, -147.5)), 157);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 0f, 34f);
			Actions.setFakePlayerHotbarSlot(berserk, 0);
		}, 158);
		Utils.scheduleTask(() -> {
			Actions.simulateWitherImpact(berserk, new Location(world, -16.5, 47, -143.5));
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Dino Dig Site 4/4 (Killed Bat)");
			world.playSound(berserk.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0F, 1.0F);
		}, 159);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 175.6f, 3.2f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 160);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -17.5, 48, -157.5)), 161);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 26.5f, -64.1f), 162);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -20.5, 62, -152.5)), 163);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 17.8f, -7.6f), 164);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -31.5, 69, -119.5)), 165);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 93.6f, 4.9f), 166);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -50.5, 69, -120.5)), 167);

		/*
		 * ██████╗ ███████╗ █████╗ ████████╗██╗  ██╗███╗   ███╗██╗████████╗███████╗
		 * ██╔══██╗██╔════╝██╔══██╗╚══██╔══╝██║  ██║████╗ ████║██║╚══██╔══╝██╔════╝
		 * ██║  ██║█████╗  ███████║   ██║   ███████║██╔████╔██║██║   ██║   █████╗
		 * ██║  ██║██╔══╝  ██╔══██║   ██║   ██╔══██║██║╚██╔╝██║██║   ██║   ██╔══╝
		 * ██████╔╝███████╗██║  ██║   ██║   ██║  ██║██║ ╚═╝ ██║██║   ██║   ███████╗
		 * ╚═════╝ ╚══════╝╚═╝  ╚═╝   ╚═╝   ╚═╝  ╚═╝╚═╝     ╚═╝╚═╝   ╚═╝   ╚══════╝
		 */
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 90f, 13.8f);
			Actions.setFakePlayerHotbarSlot(berserk, 5);
		}, 168);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, world.getBlockAt(-52, 70, -121)), 169);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, world.getBlockAt(-53, 70, -121)), 170);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Deathmite 6/6 (Opened Chest)");
			world.playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 171);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 2), 172);
		Utils.scheduleTask(() -> Actions.simulateLeap(berserk, Tank.getTank()), 173);

		/*
		 * ███████╗██████╗ ██╗██████╗ ███████╗██████╗
		 * ██╔════╝██╔══██╗██║██╔══██╗██╔════╝██╔══██╗
		 * ███████╗██████╔╝██║██║  ██║█████╗  ██████╔╝
		 * ╚════██║██╔═══╝ ██║██║  ██║██╔══╝  ██╔══██╗
		 * ███████║██║     ██║██████╔╝███████╗██║  ██║
		 * ╚══════╝╚═╝     ╚═╝╚═════╝ ╚══════╝╚═╝  ╚═╝
		 */
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 18.5f, -5.3f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 174);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -218.5, 73, -163.5)), 175);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -62.9f, 6.5f), 176);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -207.5, 74, -157.5)), 177);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -47.1f, -3.5f), 178);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -195.5, 77, -146.5)), 179);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 64f, -38.4f), 180);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -199.5, 83, -144.5)), 181);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 90f, 23.5f);
			Actions.setFakePlayerHotbarSlot(berserk, 3);
		}, 182);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(berserk, -206, 83, -146, -205, 86, -144), 183);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 92.6f, 4.4f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 184);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -220.5, 83, -145.5)), 185);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 26.7f, 27.8f), 186);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Spider 1/9 (Opened Chest)");
			world.playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 187);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -125.1f, 41.3f), 188);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -219.5, 84, -146.5)), 189);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 180f, 35f);
			Actions.setFakePlayerHotbarSlot(berserk, 5);
			Actions.move(berserk, new Vector(0, 0, -1.12242), 5);
		}, 190);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, world.getBlockAt(-220, 85, -148)), 191);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, world.getBlockAt(-220, 84, -148)), 192);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, world.getBlockAt(-220, 85, -149)), 193);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, world.getBlockAt(-220, 84, -149)), 194);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -135f, -38.3f);
			Actions.setFakePlayerHotbarSlot(berserk, 7);
		}, 195);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(berserk), 196);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -164.3f, -28.6f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 197);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -216.5, 91.5, -161.5)), 198);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 156.1f, 0.4f), 199);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(world, -219.5, 92, -167.5)), 200);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -103.7f, 19.5f), 201);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Spider 3/9 (Opened Chest)");
			world.playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 202);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -75f, 8.4f), 203);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -212.5, 92, -165.5)), 204);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -90f, 35f);
			Actions.setFakePlayerHotbarSlot(berserk, 5);
			Actions.move(berserk, new Vector(1.12242, 0, 0), 3);
		}, 205);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, world.getBlockAt(-212, 93, -166)), 206);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, world.getBlockAt(-212, 92, -166)), 207);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -107.9f, 6.4f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 208);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(world, -204.5, 92, -167.5)), 209);
		Utils.scheduleTask(() -> {
			Actions.simulateRightClickAir(berserk);
			Server.activateSpiderGate();
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Spider Lever Activated");
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Spider 5/9 (Obtained Item)");
			world.playSound(berserk.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
		}, 210);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 96.9f, 62.8f);
			Actions.simulateAOTV(berserk, new Location(world, -208.156, 91, -160.633));
		}, 211);
		Utils.scheduleTask(() -> {
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Spider 7/9 (Obtained Item)");
			world.playSound(berserk.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
		}, 212);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -213.5, 83, -161.5)), 213);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -170.6f, 15.9f), 214);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(world, -212.5, 83, -167.5)), 215);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -90f, 12.6f), 216);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(world, -207.5, 83, -167.5)), 217);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Spider 9/9 (Opened Chest)");
			world.playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Clear Finished in 220 Ticks (11.00 seconds)");
		}, 218);
		Utils.scheduleTask(() -> {
			Actions.swapFakePlayerInventorySlots(berserk, 11, 36);
			Actions.swapFakePlayerInventorySlots(berserk, 1, 28);
			Actions.swapFakePlayerInventorySlots(berserk, 7, 35);
		}, 219);
		if(doContinue) {
			Utils.scheduleTask(() -> {
				Actions.teleport(berserk, new Location(world, 73.5, 221, 13.5, 11.3f, 0f));
				maxor(true);
			}, 1025);
		}
	}

	public static void maxor(boolean doContinue) {
		Actions.setFakePlayerHotbarSlot(berserk, 5);
		Actions.move(berserk, new Vector(-0.22, 0, 1.1), 28);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 11.31f, 0f), 1);
		Utils.scheduleTask(() -> {
			Actions.move(berserk, new Vector(-0.051, 0, 0.255), 16);
			Actions.simulateSpringBoots(berserk);
		}, 28);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 33f, 0f), 45);
		Utils.scheduleTask(() -> {
			Maxor.pickUpCrystal(berserk);
			Bukkit.broadcastMessage(ChatColor.GOLD + "AsapIcey" + ChatColor.GREEN + " picked up an " + ChatColor.AQUA + "Energy Crystal" + ChatColor.GREEN + "!");
		}, 56);
		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.1403, 0, 0.243), 2), 57);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 90f, 0f), 58);
		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.2806, 0, 0), 16), 59);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 128f, 0f), 75);
		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.8844, 0, -0.691), 1), 79);
		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.2211, 0, -0.1728), 19), 81);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -52f, 0f), 100);
		Utils.scheduleTask(() -> {
			Maxor.placeCrystal(berserk);
			Actions.move(berserk, new Vector(0.205, 0, 0.16), 15);
			Actions.simulateSpringBoots(berserk);
		}, 160);
		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(0.2211, 0, 0.1728), 24), 177);
		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(0.0512, 0, 0.04), 1), 206);
		Utils.scheduleTask(() -> {
			Maxor.pickUpCrystal(berserk);
			Bukkit.broadcastMessage(ChatColor.GOLD + "AsapIcey" + ChatColor.GREEN + " picked up an " + ChatColor.AQUA + "Energy Crystal" + ChatColor.GREEN + "!");
		}, 239);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 128f, 0f), 240);
		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.8844, 0, -0.691), 1), 241);
		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.2211, 0, -0.1728), 20), 242);
		Utils.scheduleTask(() -> {
			Maxor.placeCrystal(berserk);
			Actions.turnHead(berserk, -90f, 0f);
		}, 262);
		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(1.12242, 0, 0), 10), 263);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -34.5f, 0f), 273);
		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(0.636, 0, 0.925), 7), 274);
		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(0.489, 0, 0.7116), 1), 281);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -90f, 90f), 282);
		Utils.scheduleTask(() -> {
			Actions.move(berserk, new Vector(0.26, 0, 0), 1);
			Actions.simulateStonking(berserk, world.getBlockAt(69, 220, 49));
		}, 283);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -180f, 70f), 284);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, world.getBlockAt(69, 220, 48)), 285);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 88.8f, 0f), 286);
		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.2805, 0, 0.000588), 40), 287);
		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-1.12217, 0, 0.0235), 5), 327);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 55.6f, 0f), 332);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -77f, 0f), 334);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 90f, 0f), 384);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -105f, 0f), 401);
		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(1.0842, 0, -0.2905), 29), 402);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -90f, 0f), 431);
		Utils.scheduleTask(() -> {
			Actions.move(berserk, new Vector(1.12242, 0, 0), 1);
			Actions.simulateGhostPick(berserk, world.getBlockAt(91, 166, 41));
		}, 432);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(92, 166, 41)), 433);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(93, 166, 41)), 434);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(94, 166, 41)), 435);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(95, 166, 41)), 436);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -93.6f, 0f), 437);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(91, 166, 40)), 438);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(92, 166, 40)), 439);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(93, 166, 40)), 440);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(94, 166, 40)), 441);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(95, 166, 40)), 442);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -93.6f, -16.5f), 443);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(91, 167, 40)), 444);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(92, 167, 40)), 445);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(93, 167, 40)), 446);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(94, 167, 40)), 447);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(95, 167, 40)), 448);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -90f, -16.5f), 449);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(91, 167, 41)), 450);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(92, 167, 41)), 451);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(93, 167, 41)), 452);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(94, 167, 41)), 453);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(95, 167, 41)), 454);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -93.3f, 25.7f), 455);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(91, 165, 41)), 456);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(92, 165, 41)), 457);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(92, 165, 40)), 458);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(93, 165, 40)), 459);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(94, 165, 40)), 460);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -91, 10.9f), 461);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(93, 165, 41)), 462);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(94, 165, 41)), 463);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(95, 165, 41)), 464);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(95, 165, 40)), 465);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -94.3f, 27f), 466);
		Utils.scheduleTask(() -> Actions.simulateGhostPick(berserk, world.getBlockAt(91, 165, 40)), 467);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 77.5f, 0f);
			Actions.setFakePlayerHotbarSlot(berserk, 6);
		}, 468);
		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-1.096, 0, 0.243), 6), 469);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0f, 0f), 475);
		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(0, 0, 1.12242), 6), 476);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -89f, 0f), 482);
		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(1.12225, 0, 0.0196), 18), 483);
		if(doContinue) {
			Utils.scheduleTask(() -> storm(true), 499);
		}
	}

	public static void storm(boolean doContinue) {
		// move continues for 2 more ticks from previous instruction
		Actions.setFakePlayerHotbarSlot(berserk, 6);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -1f, 23f), 2);
		Utils.scheduleTask(() -> Actions.simulateGyro(berserk, new Location(world, 100.5, 169, 53.5)), 3);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -1f, 0f);
			Actions.setFakePlayerHotbarSlot(berserk, 4);
		}, 4);
		Utils.scheduleTask(Berserk::simulateShoot, 5);
		Utils.scheduleTask(() -> Actions.simulateSalvation(berserk), 6);
		Utils.scheduleTask(Berserk::simulateShoot, 10);
		Utils.scheduleTask(() -> Actions.simulateSalvation(berserk), 11);
		Utils.scheduleTask(Berserk::simulateShoot, 15);
		Utils.scheduleTask(() -> Actions.simulateSalvation(berserk), 16);
		Utils.scheduleTask(Berserk::simulateShoot, 20);
		Utils.scheduleTask(() -> Actions.simulateSalvation(berserk), 21);
		Utils.scheduleTask(Berserk::simulateShoot, 25);
		Utils.scheduleTask(() -> Actions.simulateSalvation(berserk), 26);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -163.5f, -4f), 27);
		Utils.scheduleTask(Berserk::simulateShoot, 30);
		Utils.scheduleTask(() -> Actions.simulateSalvation(berserk), 31);
		Utils.scheduleTask(Berserk::simulateShoot, 35);
		Utils.scheduleTask(() -> Actions.simulateSalvation(berserk), 36);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -155.2f, -4f), 37);
		Utils.scheduleTask(Berserk::simulateShoot, 40);
		Utils.scheduleTask(() -> Actions.simulateSalvation(berserk), 41);
		Utils.scheduleTask(Berserk::simulateShoot, 45);
		Utils.scheduleTask(() -> Actions.simulateSalvation(berserk), 46);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -149f, -4f), 47);
		Utils.scheduleTask(Berserk::simulateShoot, 50);
		Utils.scheduleTask(() -> Actions.simulateSalvation(berserk), 51);
		Utils.scheduleTask(Berserk::simulateShoot, 55);
		Utils.scheduleTask(() -> Actions.simulateSalvation(berserk), 56);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -90.6f, -10f), 57);
		Utils.scheduleTask(Berserk::simulateShoot, 60);
		Utils.scheduleTask(() -> Actions.simulateSalvation(berserk), 61);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -81.4f, -10f), 62);
		Utils.scheduleTask(Berserk::simulateShoot, 65);
		Utils.scheduleTask(() -> Actions.simulateSalvation(berserk), 66);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -70.9f, -10f), 67);
		Utils.scheduleTask(Berserk::simulateShoot, 70);
		Utils.scheduleTask(() -> Actions.simulateSalvation(berserk), 71);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 151.5f, -5f), 72);
		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.4991, 0, -1.0054), 17), 73);
		Utils.scheduleTask(Berserk::simulateShoot, 75);
		Utils.scheduleTask(() -> Actions.simulateSalvation(berserk), 76);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 151.5f, 6f), 79);
		Utils.scheduleTask(Berserk::simulateShoot, 80);
		Utils.scheduleTask(() -> Actions.simulateSalvation(berserk), 81);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 151.5f, 9f), 84);
		Utils.scheduleTask(Berserk::simulateShoot, 85);
		Utils.scheduleTask(() -> Actions.simulateSalvation(berserk), 86);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 167f, 11f), 89);
		Utils.scheduleTask(Berserk::simulateShoot, 90);
		Utils.scheduleTask(() -> Actions.simulateSalvation(berserk), 91);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 148f, -5.2f), 92);
		Utils.scheduleTask(Berserk::simulateShoot, 95);
		Utils.scheduleTask(() -> Actions.simulateSalvation(berserk), 96);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 143f, -5.2f), 97);
		Utils.scheduleTask(Berserk::simulateShoot, 100);
		Utils.scheduleTask(() -> Actions.simulateSalvation(berserk), 101);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 135f, 8f), 102);
		Utils.scheduleTask(Berserk::simulateShoot, 105);
		Utils.scheduleTask(() -> Actions.simulateSalvation(berserk), 106);
		Utils.scheduleTask(Berserk::simulateShoot, 110);
		Utils.scheduleTask(() -> Actions.simulateSalvation(berserk), 111);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 2), 112);
		Utils.scheduleTask(() -> Actions.simulateLeap(berserk, Archer.getArcher()), 113);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 4), 114);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 120f, 0f), 115);
		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(-0.972, 0, -0.5612), 12), 116);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 4), 128);
		for(int tick = 130; tick <= 545; tick += 5) {
			Utils.scheduleTask(() -> {
				List<Entity> nearbyEntities = berserk.getNearbyEntities(6, 6, 6);

				for(Entity entity : nearbyEntities) {
					if(entity instanceof WitherSkeleton) {
						Location healerLoc = berserk.getLocation();
						Location witherLoc = entity.getLocation();

						double deltaX = witherLoc.getX() - healerLoc.getX();
						double deltaY = witherLoc.getY() - healerLoc.getY();
						double deltaZ = witherLoc.getZ() - healerLoc.getZ();

						float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;
						float pitch = (float) -(Math.atan2(deltaY, Math.sqrt(deltaX * deltaX + deltaZ * deltaZ)) * 180.0 / Math.PI);

						Actions.turnHead(berserk, yaw, pitch);

						Utils.scheduleTask(() -> Actions.simulateRightClickAir(berserk), 1);

						break;
					}
				}
			}, tick);
		}
	}

	private static void simulateShoot() {
		Actions.simulateRightClickAir(berserk);

		// Duplex Arrow
		Utils.scheduleTask(() -> {
			Location l = berserk.getLocation();
			l.add(l.getDirection());
			l.setY(l.getY() + 1.62);

			double powerBonus;
			try {
				int power = berserk.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.POWER);
				powerBonus = power * 0.05;
				if(power == 7) {
					powerBonus += 0.05;
				}
			} catch(Exception exception) {
				powerBonus = 0;
			}

			double strengthBonus;
			try {
				strengthBonus = 0.15 + 0.15 * Objects.requireNonNull(berserk.getPotionEffect(PotionEffectType.STRENGTH)).getAmplifier();
			} catch(Exception exception) {
				strengthBonus = 0;
			}

			double add = powerBonus + strengthBonus;
			Arrow arrow = world.spawnArrow(l, l.getDirection(), 3, 0.1F);
			arrow.setDamage(0.5 + add);
			arrow.setPierceLevel(4);
			arrow.setShooter(berserk);
			arrow.setWeapon(berserk.getInventory().getItemInMainHand());
			arrow.addScoreboardTag("TerminatorArrow");
		}, 3);

		PotionEffect strength = berserk.getPotionEffect(PotionEffectType.STRENGTH);
		boolean hasRagBuff = berserk.getScoreboardTags().contains("RagBuff");
		int maxAmplifier = hasRagBuff ? 8 : 7;
		int baseAmplifier = hasRagBuff ? 1 : 0;

		int newAmplifier;
		if (strength == null) {
			newAmplifier = baseAmplifier;
		} else {
			newAmplifier = Math.min(strength.getAmplifier() + 1, maxAmplifier);
		}

		berserk.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, newAmplifier));
	}

	@SuppressWarnings("unused")
	public static Player getBerserk() {
		return berserk;
	}
}