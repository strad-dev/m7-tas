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
		berserk.teleport(new Location(berserk.getWorld(), -21.5, 70, -197.5, 0, 0));
		Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(berserk, 2, 29), 60);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 2), 61);
		Utils.scheduleTask(() -> Actions.simulateRightClickAir(berserk), 101);
		Utils.scheduleTask(() -> Actions.move(berserk, new Vector(0, 0, -0.8634), 3), 102);
		Utils.scheduleTask(() -> berserk.teleport(new Location(berserk.getWorld(), -120.5, 75, -220.5)), 141);
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
		Utils.scheduleTask(() -> {
			Actions.setFakePlayerHotbarSlot(berserk, 1);
			Actions.turnHead(berserk, 90, 36.5f);
		}, 0);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -24.5, 69, -197.5)), 4);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -175.3f, 3.5f), 8);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -22.5, 69, -223.5)), 12);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -126.9f, 12.2f);
			Actions.setFakePlayerHotbarSlot(berserk, 5);
		}, 16);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, berserk.getWorld().getBlockAt(-22, 70, -224)), 20);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, berserk.getWorld().getBlockAt(-22, 70, -225)), 24);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, berserk.getWorld().getBlockAt(-21, 70, -225)), 28);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Tic Tac Toe 1/1 (Opened Chest)");
			berserk.playSound(berserk, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 32);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 3.1f, 2.4f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 36);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -24.5, 69, -184.5)), 40);

		/*
		 *  ██████╗██╗  ██╗ █████╗ ███╗   ███╗██████╗ ███████╗██████╗ ███████╗
		 * ██╔════╝██║  ██║██╔══██╗████╗ ████║██╔══██╗██╔════╝██╔══██╗██╔════╝
		 * ██║     ███████║███████║██╔████╔██║██████╔╝█████╗  ██████╔╝███████╗
		 * ██║     ██╔══██║██╔══██║██║╚██╔╝██║██╔══██╗██╔══╝  ██╔══██╗╚════██║
		 * ╚██████╗██║  ██║██║  ██║██║ ╚═╝ ██║██████╔╝███████╗██║  ██║███████║
		 *  ╚═════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝     ╚═╝╚═════╝ ╚══════╝╚═╝  ╚═╝╚══════╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -60.4f, 28), 44);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -20.5, 69, -182.5)), 48);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -138.5f, 90), 52);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -20.5, 59, -182.5)), 56);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -134.2f, 51.1f), 60);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 1/5 (Opened Chest)");
			berserk.playSound(berserk, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 64);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0, -90), 68);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -20.5, 72, -182.5)), 72);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 81.0f, -67.7f), 76);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -23.5, 82, -181.5)), 80);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 57.5f, 9.3f), 84);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -31.5, 82, -176.5)), 88);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 62.5f, 10.8f);
			Actions.setFakePlayerHotbarSlot(berserk, 0);
		}, 92);
		Utils.scheduleTask(() -> {
			Actions.simulateRightClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 2/5 (Killed Bat)");
			berserk.playSound(berserk, Sound.ENTITY_BAT_DEATH, 1.0F, 1.0F);
		}, 96);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 161.0f, 9.5f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 100);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -38.5, 80, -183.5)), 104);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 108.8f, 5.1f), 108);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -55.5, 80, -189.5)), 112);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -144.1f, 3.7f), 116);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -45.5, 81, -203.5)), 120);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -168.7f, 9.2f), 124);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -44.5, 81, -213.5)), 128);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 146.5f, -2.3f);
			Actions.setFakePlayerHotbarSlot(berserk, 3);
		}, 132);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(berserk, -47, 81, -217, -46, 84, -216), 136);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 1), 140);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -54.5, 83.5, -228.5)), 144);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 116.2f, 38.9f), 148);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 3/5 (Opened Chest)");
			berserk.playSound(berserk, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 152);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0.7f, -3.3f), 156);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -54.5, 86, -219.5)), 160);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 0f, 45f);
			Actions.setFakePlayerHotbarSlot(berserk, 5);
		}, 164);
		Utils.scheduleTask(() -> {
			Actions.simulateStonking(berserk, berserk.getWorld().getBlockAt(-55, 87, -219));
			Actions.move(berserk, new Vector(0, 0, 0.2806), 12);
		}, 168);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, berserk.getWorld().getBlockAt(-55, 86, -219)), 172);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 14.8f, 45f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 176);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -59.5, 69, -198.5)), 180);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 36f, 16f), 184);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -62.5, 69, -194.5)), 188);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 5), 192);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, berserk.getWorld().getBlockAt(-64, 70, -194)), 196);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 4/5 (Opened Chest)");
			berserk.playSound(berserk, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 200);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -90f, 14.9f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 204);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -56.5, 69, -194.5)), 208);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 9.3f, 5.2f), 212);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -58.5, 69, -176.5)), 216);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 75f, 15.9f);
			Actions.setFakePlayerHotbarSlot(berserk, 3);
		}, 220);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(berserk, -61, 69, -177, -62, 73, -174), 224);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 1), 228);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -64.5, 69.0625, -175.5)), 232);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 5/5 (Opened Chest)");
			berserk.playSound(berserk, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 236);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -95.4f, -3.2f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 240);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -54.5, 71.5, -176.5)), 244);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 165.2f, 6.8f), 248);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -64.5, 69, -215.5)), 252);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 90f, 0f);
			Actions.setFakePlayerHotbarSlot(berserk, 4);
			Actions.move(berserk, new Vector(-0.2806, 0, 0), 48);
		}, 256);

		/*
		 *  ██████╗ █████╗ ████████╗██╗    ██╗ █████╗ ██╗     ██╗  ██╗
		 * ██╔════╝██╔══██╗╚══██╔══╝██║    ██║██╔══██╗██║     ██║ ██╔╝
		 * ██║     ███████║   ██║   ██║ █╗ ██║███████║██║     █████╔╝
		 * ██║     ██╔══██║   ██║   ██║███╗██║██╔══██║██║     ██╔═██╗
		 * ╚██████╗██║  ██║   ██║   ╚███╔███╔╝██║  ██║███████╗██║  ██╗
		 *  ╚═════╝╚═╝  ╚═╝   ╚═╝    ╚══╝╚══╝ ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝
		 */
		Utils.scheduleTask(Berserk::simulateShoot, 260);
		Utils.scheduleTask(Berserk::simulateShoot, 280);
		Utils.scheduleTask(Berserk::simulateShoot, 300);
		Utils.scheduleTask(() -> Actions.simulateLeftClickAir(berserk), 304);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 72.2f, 1.3f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 308);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -93.5, 70.5, -210.5)), 312);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 64.3f, 34.8f), 316);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -101.5, 66, -206.5)), 320);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -152.6f, 69.1f), 324);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Catwalk 1/6 (Opened Chest)");
			berserk.playSound(berserk, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 328);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -120.0f, 26.4f), 332);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -93.5, 63, -211.5)), 336);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -33.2f, 68.7f), 340);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -96.5, 50, -206.5)), 344);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -96.4f, 17.5f);
			Actions.setFakePlayerHotbarSlot(berserk, 3);
		}, 348);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(berserk, -94, 50, -207, -93, 53, -209), 352);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 1), 356);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -90.5, 50, -207.5)), 360);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Catwalk 2/6 (Opened Chest)");
			berserk.playSound(berserk, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 364);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 70.0f, 17.0f), 368);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -95.5, 50, -205.5)), 372);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -2.6f, -18.9f), 376);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -94.5, 59, -184.5)), 380);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 1.6f, -2.4f), 384);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -95.5, 63, -146.5)), 388);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -106.2f, 12.2f), 392);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -92.5, 64, -147.5)), 396);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -90.4f, 22.1f), 400);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Catwalk 3/6 (Opened Chest)");
			berserk.playSound(berserk, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 404);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 90f, 36.6f), 408);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -95.5, 63, -147.5)), 412);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -85.1f, 87.4f), 416);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -94.5, 50, -147.5)), 420);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0f, 24.1f), 424);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -94.5, 49, -141.5)), 428);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -110.2f, 18.8f), 432);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Catwalk 4/6 (Opened Chest)");
			berserk.playSound(berserk, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 436);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 180f, 1.7f), 440);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -94.5, 51, -148.5)), 444);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -114.5f, 16.2f), 448);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -91.5, 51, -149.5)), 452);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -90f, 0f);
			Actions.setFakePlayerHotbarSlot(berserk, 3);
			Actions.move(berserk, new Vector(0.2806, 0, 0), 20);
		}, 456);
		Utils.scheduleTask(() -> Actions.simulateCrypt(berserk, -86, 49, -150, -84, 51, -149), 460);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 4), 464);
		Utils.scheduleTask(Berserk::simulateShoot, 468);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -106f, 12.6f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 472);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -80.5, 49, -150.5)), 476);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -135.4f, 14.9f);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Catwalk 5/6 (Obtained Item)");
			berserk.playSound(berserk, Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
		}, 480);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Catwalk 6/6 (Obtained Wither Essence)");
			berserk.playSound(berserk, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		}, 484);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 5.3f, 3.3f), 488);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -81.5, 49, -139.5)), 492);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -160.2f, -65.2f), 496);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -78.5, 70.5, -148.5)), 500);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -136.3f, -28.3f), 504);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -74.5, 69, -152.5)), 508);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -90, 0);
			Actions.setFakePlayerHotbarSlot(berserk, 4);
			Actions.move(berserk, new Vector(0.2806, 0, 0), 60);
		}, 512);

		/*
		 * ██████╗ ██╗███╗   ██╗ ██████╗     ██████╗ ██╗ ██████╗     ███████╗██╗████████╗███████╗
		 * ██╔══██╗██║████╗  ██║██╔═══██╗    ██╔══██╗██║██╔════╝     ██╔════╝██║╚══██╔══╝██╔════╝
		 * ██║  ██║██║██╔██╗ ██║██║   ██║    ██║  ██║██║██║  ███╗    ███████╗██║   ██║   █████╗
		 * ██║  ██║██║██║╚██╗██║██║   ██║    ██║  ██║██║██║   ██║    ╚════██║██║   ██║   ██╔══╝
		 * ██████╔╝██║██║ ╚████║╚██████╔╝    ██████╔╝██║╚██████╔╝    ███████║██║   ██║   ███████╗
		 * ╚═════╝ ╚═╝╚═╝  ╚═══╝ ╚═════╝     ╚═════╝ ╚═╝ ╚═════╝     ╚══════╝╚═╝   ╚═╝   ╚══════╝
		 */
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