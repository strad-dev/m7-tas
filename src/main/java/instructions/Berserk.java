package instructions;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import plugin.Utils;

import java.util.Objects;

public class Berserk {
	private static Player berserk;

	public static void berserkInstructions(Player p) {
		berserk = p;
		System.out.println("Berserk Instructions: " + berserk.getName());
		Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Tic Tac Toe Pre-Cleared");
		berserk.teleport(new Location(berserk.getWorld(), -21.5, 70, -197.5, 90f, 36.5f));
		Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(berserk, 2, 29), 60);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 2), 61);
		Utils.scheduleTask(() -> Actions.simulateRightClickAir(berserk), 101);
		Utils.scheduleTask(() -> {
			Actions.setFakePlayerHotbarSlot(berserk, 1);
			Actions.move(berserk, new Vector(0, 0, -0.8634), 4);
		}, 102);
		Utils.scheduleTask(() -> berserk.teleport(new Location(berserk.getWorld(), -120.5, 75, -220.5)), 141);
		// Tick 160 (clear tick 0: run begins)
		// Tick 161 (clear tick 1: teleport back)
		Utils.scheduleTask(Berserk::clear, 162);
	}

	private static void clear() {
		/*
		 * ████████╗██╗ ██████╗    ████████╗ █████╗  ██████╗    ████████╗ ██████╗ ███████╗
		 * ╚══██╔══╝██║██╔════╝    ╚══██╔══╝██╔══██╗██╔════╝    ╚══██╔══╝██╔═══██╗██╔════╝
		 *    ██║   ██║██║            ██║   ███████║██║            ██║   ██║   ██║█████╗
		 *    ██║   ██║██║            ██║   ██╔══██║██║            ██║   ██║   ██║██╔══╝
		 *    ██║   ██║╚██████╗       ██║   ██║  ██║╚██████╗       ██║   ╚██████╔╝███████╗
		 *    ╚═╝   ╚═╝ ╚═════╝       ╚═╝   ╚═╝  ╚═╝ ╚═════╝       ╚═╝    ╚═════╝ ╚══════╝
		 */
		// Tick 162 (clear tick 2, delay = 0)
		Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -24.5, 69, -197.5));
		Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers Insta-Cleared");
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -175.3f, 3.5f), 1);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -22.5, 69, -223.5)), 2);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -126.9f, 12.2f);
			Actions.setFakePlayerHotbarSlot(berserk, 5);
		}, 3);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, berserk.getWorld().getBlockAt(-22, 70, -224)), 4);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, berserk.getWorld().getBlockAt(-22, 70, -225)), 5);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, berserk.getWorld().getBlockAt(-21, 70, -225)), 6);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Tic Tac Toe 1/1 (Opened Chest)");
			berserk.getWorld().playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 7);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 3.1f, 2.4f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 8);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -24.5, 69, -184.5)), 9);

		/*
		 *  ██████╗██╗  ██╗ █████╗ ███╗   ███╗██████╗ ███████╗██████╗ ███████╗
		 * ██╔════╝██║  ██║██╔══██╗████╗ ████║██╔══██╗██╔════╝██╔══██╗██╔════╝
		 * ██║	   ███████║███████║██╔████╔██║██████╔╝█████╗  ██████╔╝███████╗
		 * ██║	   ██╔══██║██╔══██║██║╚██╔╝██║██╔══██╗██╔══╝  ██╔══██╗╚════██║
		 * ╚██████╗██║  ██║██║  ██║██║ ╚═╝ ██║██████╔╝███████╗██║  ██║███████║
		 *  ╚═════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝	 ╚═╝╚═════╝ ╚══════╝╚═╝  ╚═╝╚══════╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -60.4f, 28), 10);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -20.5, 69, -182.5)), 11);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -138.5f, 90), 12);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -20.5, 59, -182.5)), 13);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -134.2f, 51.1f), 14);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 1/5 (Opened Chest)");
			berserk.getWorld().playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 15);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0, -90), 16);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -20.5, 72, -182.5)), 17);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 81.0f, -67.7f), 18);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -23.5, 82, -181.5)), 19);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 57.5f, 9.3f), 20);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -31.5, 82, -176.5)), 21);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 60.1f, 38.9f);
			Actions.setFakePlayerHotbarSlot(berserk, 0);
		}, 22);
		Utils.scheduleTask(() -> {
			Actions.simulateRightClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 2/5 (Killed Bat)");
			berserk.getWorld().playSound(berserk.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0F, 1.0F);
		}, 23);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 161.0f, 9.5f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 24);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -38.5, 80, -183.5)), 25);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 108.8f, 5.1f), 26);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -55.5, 80, -189.5)), 27);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -144.1f, 3.7f), 28);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -45.5, 81, -203.5)), 29);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -168.7f, 9.2f), 30);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -44.5, 81, -213.5)), 31);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 146.5f, -2.3f);
			Actions.setFakePlayerHotbarSlot(berserk, 3);
		}, 32);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(berserk, -47, 81, -217, -46, 84, -216), 33);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 1), 34);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -54.5, 83.5, -228.5)), 35);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 116.2f, 38.9f), 36);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 3/5 (Opened Chest)");
			berserk.getWorld().playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 37);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0.7f, -3.3f), 38);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -54.5, 86, -219.5)), 39);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 0f, 45f);
			Actions.setFakePlayerHotbarSlot(berserk, 5);
		}, 40);
		Utils.scheduleTask(() -> {
			Actions.simulateStonking(berserk, berserk.getWorld().getBlockAt(-55, 87, -219));
			Actions.move(berserk, new Vector(0, 0, 1.1224), 3);
		}, 41);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, berserk.getWorld().getBlockAt(-55, 86, -219)), 42);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 14.8f, 45f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 43);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -59.5, 69, -198.5)), 44);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 36f, 16f), 45);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -62.5, 69, -194.5)), 46);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 5), 47);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, berserk.getWorld().getBlockAt(-64, 70, -194)), 48);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 4/5 (Opened Chest)");
			berserk.getWorld().playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 49);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -90f, 14.9f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 50);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -56.5, 69, -194.5)), 51);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 9.3f, 5.2f), 52);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -58.5, 69, -176.5)), 53);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 75f, 15.9f);
			Actions.setFakePlayerHotbarSlot(berserk, 3);
		}, 54);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(berserk, -61, 69, -177, -62, 73, -174), 55);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 1), 56);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -64.5, 69.0625, -175.5)), 57);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 5/5 (Opened Chest)");
			berserk.getWorld().playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 58);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -95.4f, -3.2f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 59);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -54.5, 71.5, -176.5)), 60);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 165.2f, 6.8f), 61);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -64.5, 69, -215.5)), 62);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 90f, 4.4f), 63);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -86.5, 69, -215.5)), 64);

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
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -93.5, 70.5, -210.5)), 73);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 64.3f, 34.8f), 74);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -101.5, 66, -206.5)), 75);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -152.6f, 69.1f), 76);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Catwalk 1/6 (Opened Chest)");
			berserk.getWorld().playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 77);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -120.0f, 26.4f), 78);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -93.5, 63, -211.5)), 79);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -33.2f, 68.7f), 80);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -96.5, 50, -206.5)), 81);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -96.4f, 17.5f);
			Actions.setFakePlayerHotbarSlot(berserk, 3);
		}, 82);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(berserk, -94, 50, -207, -93, 53, -209), 83);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 1), 84);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -90.5, 50, -207.5)), 85);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Catwalk 2/6 (Opened Chest)");
			berserk.getWorld().playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 86);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 72.4f, 16.4f), 87);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -95.5, 50, -205.5)), 88);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -2.6f, -18.9f), 89);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -94.5, 59, -184.5)), 90);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 1.6f, -2.4f), 91);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -95.5, 63, -146.5)), 92);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -106.2f, 12.2f), 93);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -92.5, 64, -147.5)), 94);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -90.4f, 22.1f), 95);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Catwalk 3/6 (Opened Chest)");
			berserk.getWorld().playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 96);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 90f, 36.6f), 97);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -95.5, 63, -147.5)), 98);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -85.1f, 87.4f), 99);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -94.5, 50, -147.5)), 100);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0f, 24.1f), 101);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -94.5, 49, -141.5)), 102);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -110.2f, 18.8f), 103);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Catwalk 4/6 (Opened Chest)");
			berserk.getWorld().playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 104);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 180f, 1.7f), 105);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -94.5, 51, -148.5)), 106);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -114.5f, 16.2f), 107);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -91.5, 50, -149.5)), 108);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -90f, 0f);
			Actions.setFakePlayerHotbarSlot(berserk, 3);
			Actions.move(berserk, new Vector(1.1224, 0, 0), 5);
		}, 109);
		Utils.scheduleTask(() -> Actions.simulateCrypt(berserk, -86, 49, -150, -84, 51, -149), 110);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 4), 111);
		Utils.scheduleTask(Berserk::simulateShoot, 112);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -106f, 12.6f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: +1 Crypt!  2/5");
		}, 113);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -80.5, 49, -150.5)), 114);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 135.4f, 14.9f);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Catwalk 5/6 (Obtained Item)");
			berserk.getWorld().playSound(berserk.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
		}, 115);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Catwalk 6/6 (Obtained Wither Essence)");
			berserk.getWorld().playSound(berserk.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		}, 116);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 5.3f, 3.3f), 117);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -81.5, 49, -139.5)), 118);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -160.2f, -65.2f), 119);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -78.5, 70.5, -148.5)), 120);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -136.3f, 28.3f), 121);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -74.5, 69, -152.5)), 122);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -95f, 2.8f), 123);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -41.5, 69, -155.5)), 124);

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
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -33.5, 91, -142.5)), 135);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 0f, 10f);
			Actions.setFakePlayerHotbarSlot(berserk, 3);
		}, 136);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(berserk, -35, 92, -141, -33, 96, -140), 137);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 1), 138);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -33.5, 92, -136.5)), 139);
		// pearl originally landed on tick 151, accounting for efficient catwalk, pearl lands on tick 147
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Dino Dig Site 1/4 (Opened Chest)");
			berserk.getWorld().playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
			Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -50.059, 68.82469, -151.404));
		}, 140);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 44f, 65.2f), 141);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -54.5, 57, -146.5)), 142);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0f, 0f), 143);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -54.5, 57, -144.5)), 144);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 135.7f, 34.6f);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Dino Dig Site 2/4 (Obtained Item)");
			berserk.getWorld().playSound(berserk.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
		}, 145);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -61.5, 50, -151.5)), 146);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 90f, 40.2f), 147);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -63.5, 50, -151.5)), 148);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 180.0f, -6.1f);
			Actions.setFakePlayerHotbarSlot(berserk, 5);
		}, 149);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, berserk.getWorld().getBlockAt(-64, 51, -156)), 150);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Dino Dig Site 3/4 (Opened Chest)");
			berserk.getWorld().playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 151);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -70.5f, -23.5f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 152);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -50.5, 58, -146.5)), 153);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -111.1f, 19.6f), 154);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -18.5, 48, -158.5)), 155);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -10.7f, 10.4f), 156);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -16.5, 48, -147.5)), 157);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 0f, 34f);
			Actions.setFakePlayerHotbarSlot(berserk, 0);
		}, 158);
		Utils.scheduleTask(() -> {
			Actions.simulateRightClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Dino Dig Site 4/4 (Killed Bat)");
			berserk.getWorld().playSound(berserk.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0F, 1.0F);
		}, 159);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 175.6f, 3.2f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 160);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -17.5, 48, -157.5)), 161);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 26.5f, -64.1f), 162);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -20.5, 62, -152.5)), 163);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 17.8f, -7.6f), 164);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -31.5, 69, -119.5)), 165);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 93.6f, 4.9f), 166);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -50.5, 69, -120.5)), 167);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 90f, 13.8f);
			Actions.setFakePlayerHotbarSlot(berserk, 5);
		}, 168);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, berserk.getWorld().getBlockAt(-52, 70, -121)), 169);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, berserk.getWorld().getBlockAt(-53, 70, -121)), 170);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Deathmite 6/6 (Opened Chest)");
			berserk.getWorld().playSound(berserk.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Clear Finished in 173 Ticks (8.65 seconds)");
		}, 171);
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
			Arrow arrow = berserk.getWorld().spawnArrow(l, l.getDirection(), 3, 0.1F);
			arrow.setDamage(0.5 + add);
			arrow.setPierceLevel(4);
			arrow.setShooter(berserk);
			arrow.setWeapon(berserk.getInventory().getItemInMainHand());
			arrow.addScoreboardTag("TerminatorArrow");
		}, 3);

		PotionEffect strength = berserk.getPotionEffect(PotionEffectType.STRENGTH);
		if(strength == null) {
			berserk.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 0));
		} else {
			if(strength.getAmplifier() < 5) {
				berserk.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, strength.getAmplifier() + 1));
			} else {
				berserk.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 5));
			}
		}
	}

	public static Player getBerserk() {
		return berserk;
	}
}