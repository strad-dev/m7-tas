package instructions;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import plugin.M7tas;
import plugin.Utils;

import java.util.List;
import java.util.Objects;

public class Archer {
	private static Player archer;
	private static World world;

	public static void archerInstructions(Player p, String section) {
		archer = p;
		world = archer.getWorld();
		Objects.requireNonNull(archer.getInventory().getItem(4)).addUnsafeEnchantment(Enchantment.POWER, 26);

		if(section.equals("all") || section.equals("clear")) {
			Actions.teleport(archer, new Location(world, -118.5, 70, -202.5, 0f, 0f));
			Actions.setFakePlayerHotbarSlot(archer, 1);
			Utils.scheduleTask(() -> Actions.move(archer, new Vector(0, 0, 1.12242), 5), 160);
			Utils.scheduleTask(() -> clear(section.equals("all")), 162);
		} else if(section.equals("maxor")) {
			Actions.teleport(archer, new Location(world, 73.5, 221, 13.5));
			Utils.scheduleTask(() -> maxor(false), 60);
		}
	}

	public static void clear(boolean doContinue) {
		/*
		 * ██████╗ ██╗      ██████╗  ██████╗ ██████╗     ██████╗ ██╗   ██╗███████╗██╗  ██╗
		 * ██╔══██╗██║     ██╔═══██╗██╔═══██╗██╔══██╗    ██╔══██╗██║   ██║██╔════╝██║  ██║
		 * ██████╔╝██║     ██║   ██║██║   ██║██║  ██║    ██████╔╝██║   ██║███████╗███████║
		 * ██╔══██╗██║     ██║   ██║██║   ██║██║  ██║    ██╔══██╗██║   ██║╚════██║██╔══██║
		 * ██████╔╝███████╗╚██████╔╝╚██████╔╝██████╔╝    ██║  ██║╚██████╔╝███████║██║  ██║
		 * ╚═════╝ ╚══════╝ ╚═════╝  ╚═════╝ ╚═════╝     ╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚═╝  ╚═╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(archer, 6.6f, 9.9f), 3);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -119.5, 69, -187.5)), 4);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 20.4f, 8f);
			Actions.setFakePlayerHotbarSlot(archer, 4);
		}, 5);
		Utils.scheduleTask(() -> {
			simulateShoot();
			Actions.move(archer, new Vector(-0.39124, 0, 1.05202), 2);
		}, 6);
		Utils.scheduleTask(() -> {
			Actions.setFakePlayerHotbarSlot(archer, 2);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Red Blue Cleared");
		}, 7);
		Utils.scheduleTask(() -> Bukkit.broadcastMessage(ChatColor.GOLD + "[MVP" + ChatColor.DARK_BLUE + "++" + ChatColor.GOLD + "] akc0303 " + ChatColor.GREEN + "has obtained " + ChatColor.DARK_GRAY + "Wither Key" + ChatColor.GREEN + "!"), 8);
		Utils.scheduleTask(() -> Actions.simulateLeap(archer, Mage.getMage()), 9);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 0f, 8.6f);
			Actions.setFakePlayerHotbarSlot(archer, 1);
		}, 10);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -120.5, 69, -127.5)), 29);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 0f, 35f), 30);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -120.5, 67, -122.5)), 31);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(archer, 4), 32);
		Utils.scheduleTask(() -> {
			simulateShoot();
			Actions.move(archer, new Vector(0, 0, 1.12242), 2);
		}, 33);
		Utils.scheduleTask(() -> {
			Actions.setFakePlayerHotbarSlot(archer, 1);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Deathmite Cleared");
		}, 34);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 90f, 0f);
			Bukkit.broadcastMessage(ChatColor.GOLD + "[MVP" + ChatColor.DARK_BLUE + "++" + ChatColor.GOLD + "] akc0303 " + ChatColor.GREEN + "has obtained " + ChatColor.RED + "Blood Key" + ChatColor.GREEN + "!");
		}, 35);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -127.5, 69, -120.5)), 36);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 90f, 3.5f), 37);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -154.5, 69, -120.5)), 38);

		/*
		 * ██╗    ██╗███████╗██╗     ██╗
		 * ██║    ██║██╔════╝██║     ██║
		 * ██║ █╗ ██║█████╗  ██║     ██║
		 * ██║███╗██║██╔══╝  ██║     ██║
		 * ╚███╔███╔╝███████╗███████╗███████╗
		 *  ╚══╝╚══╝ ╚══════╝╚══════╝╚══════╝
		 */
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(archer, 4), 39);
		Utils.scheduleTask(Archer::simulateShoot, 40);
		Utils.scheduleTask(() -> {
			Actions.setFakePlayerHotbarSlot(archer, 1);
			Actions.turnHead(archer, -175.9f, -8.7f);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Well Cleared");
		}, 41);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -152.5, 76, -152.5)), 42);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -180f, -36.5f), 43);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -152.5, 87, -163.5)), 44);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Well 1/7 (Opened Chest)");
			world.playSound(archer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 45);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 22.9f, -17.2f), 46);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -163.5, 98, -136.5)), 47);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 175.3f, 18.4f), 48);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -165.5, 91, -162.5)), 49);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -6.2f, 4f);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Well 2/7 (Obtained Item)");
			world.playSound(archer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
		}, 50);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -162.5, 91, -139.5)), 51);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -67.8f, 25.3f), 52);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -151.5, 87, -135.5)), 53);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -7.3f, 2.9f), 54);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -149.5, 88, -119.5)), 55);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 0f, 21.3f);
			Actions.setFakePlayerHotbarSlot(archer, 5);
		}, 56);
		Utils.scheduleTask(() -> Actions.simulateStonking(archer, world.getBlockAt(-150, 89, -119)), 57);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Well 3/7 (Opened Chest)");
			world.playSound(archer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 58);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 120f, 0f);
			Actions.setFakePlayerHotbarSlot(archer, 7);
		}, 59);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(archer), 60);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -90f, 0f);
			Actions.setFakePlayerHotbarSlot(archer, 1);
		}, 61);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -143.5, 90, -119.5)), 62);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 106f, 66f);
			Actions.setFakePlayerHotbarSlot(archer, 7);
		}, 63);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(archer), 64);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -3f, 14.3f);
			Actions.setFakePlayerHotbarSlot(archer, 1);
		}, 65);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -142.5, 89, -109.5)), 66);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -90f, -72.6f), 67);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -141.5, 95, -109.5)), 68);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 146.8f, 19.3f), 69);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Well 4/7 (Obtained Wither Essence)");
			world.playSound(archer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		}, 70);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 45.9f, 45f), 71);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Well 5/7 (Obtained Wither Essence)");
			world.playSound(archer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		}, 72);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 83.9f, 8.7f);
			Actions.simulateAOTV(archer, new Location(world, -164.153, 87, -128.022));
		}, 73);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -174.5, 87, -126.5)), 74);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 0f, -50f);
			Actions.setFakePlayerHotbarSlot(archer, 5);
		}, 75);
		Utils.scheduleTask(() -> Actions.simulateStonking(archer, world.getBlockAt(-175, 89, -126)), 76);
		Utils.scheduleTask(() -> Actions.simulateStonking(archer, world.getBlockAt(-175, 90, -126)), 77);
		Utils.scheduleTask(() -> Actions.simulateStonking(archer, world.getBlockAt(-175, 90, -125)), 78);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Well 6/7 (Opened Chest)");
			world.playSound(archer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 79);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 87.5f, 1.8f);
			Actions.setFakePlayerHotbarSlot(archer, 1);
			Actions.simulateAOTV(archer, new Location(world, -151.559, 69, -121.912));
		}, 80);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -181.5, 70, -120.5)), 81);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 82f, 11f);
			Actions.setFakePlayerHotbarSlot(archer, 7);
		}, 82);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(archer), 83);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 106f, 55f);
			Actions.setFakePlayerHotbarSlot(archer, 1);
		}, 84);
		Utils.scheduleTask(() -> Actions.simulateAOTV(archer, new Location(world, -188.116, 59, -122.397)), 85);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -53.8f, 29.3f), 86);
		Utils.scheduleTask(() -> Actions.simulateAOTV(archer, new Location(world, -183.5, 57, -119.5)), 87);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 97.9f, 5.4f);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Well 7/7 (Obtained Item)");
			world.playSound(archer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
			Actions.simulateAOTV(archer, new Location(world, -187.230, 70, -119.704));
		}, 88);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -214.5, 69, -123.5)), 89);

		/*
		 * ████████╗ ██████╗ ███╗   ███╗██╗ ██████╗ ██╗  ██╗ █████╗
		 * ╚══██╔══╝██╔═══██╗████╗ ████║██║██╔═══██╗██║ ██╔╝██╔══██╗
		 *    ██║   ██║   ██║██╔████╔██║██║██║   ██║█████╔╝ ███████║
		 *    ██║   ██║   ██║██║╚██╔╝██║██║██║   ██║██╔═██╗ ██╔══██║
		 *    ██║   ╚██████╔╝██║ ╚═╝ ██║██║╚██████╔╝██║  ██╗██║  ██║
		 *    ╚═╝    ╚═════╝ ╚═╝     ╚═╝╚═╝ ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝
		 */
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 32f, 0f);
			Actions.setFakePlayerHotbarSlot(archer, 4);
		}, 90);
		Utils.scheduleTask(() -> Actions.simulateRightClickAir(archer), 91);
		Utils.scheduleTask(() -> Actions.simulateSalvation(archer), 92);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 2.7f, 4.7f);
			Actions.setFakePlayerHotbarSlot(archer, 1);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Tomioka Cleared");
		}, 93);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -215.5, 69, -103.5)), 94);

		/*
		 *  ██████╗ ██████╗  █████╗ ██╗   ██╗███████╗██╗
		 * ██╔════╝ ██╔══██╗██╔══██╗██║   ██║██╔════╝██║
		 * ██║  ███╗██████╔╝███████║██║   ██║█████╗  ██║
		 * ██║   ██║██╔══██╗██╔══██║╚██╗ ██╔╝██╔══╝  ██║
		 * ╚██████╔╝██║  ██║██║  ██║ ╚████╔╝ ███████╗███████╗
		 *  ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝  ╚═══╝  ╚══════╝╚══════╝
		 */
		Utils.scheduleTask(Archer::explosiveShot, 95);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 48.9f, 5.7f);
			Actions.simulateCrypt(archer, -217, 70, -99, -215, 69, -97);
		}, 96);
		Utils.scheduleTask(() -> {
			Actions.simulateEtherwarp(archer, new Location(world, -227.5, 69, -92.5));
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Crypt 3/5");
		}, 97);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -52.8f, 9.2f), 98);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -219.5, 69, -86.5)), 99);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -107.7f, 4.6f), 100);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -200.5, 69, -92.5)), 101);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -90f, 13.5f);
			Actions.setFakePlayerHotbarSlot(archer, 5);
		}, 102);
		Utils.scheduleTask(() -> Actions.simulateStonking(archer, world.getBlockAt(-199, 70, -93)), 103);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Gravel 1/6 (Opened Chest)");
			world.playSound(archer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 104);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 22.2f, -57.9f);
			Actions.setFakePlayerHotbarSlot(archer, 1);
			Actions.move(archer, new Vector(-0.8634, 0, 0), 1);
		}, 105);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -203.5, 81.5, -86.5)), 106);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -99.7f, -52.3f);
			Actions.setFakePlayerHotbarSlot(archer, 0);
		}, 107);
		Utils.scheduleTask(() -> {
			Actions.simulateWitherImpact(archer, new Location(world, -197.472,89.41224, -87.53));
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Gravel 2/6 (Killed Bat)");
			world.playSound(archer.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0F, 1.0F);
		}, 108);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 80.1f, 52.2f);
			Actions.setFakePlayerHotbarSlot(archer, 1);
		}, 109);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -213.5, 69, -84.5)), 110);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -85.1f, 2.75f), 111); // requires 3 sig figs of presicion here
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -179.5, 69, -81.5)), 112);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -62.3f, 3.3f);
			Actions.setFakePlayerHotbarSlot(archer, 4);
		}, 113);
		Utils.scheduleTask(Archer::simulateShoot, 114);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -87.1f, 4.4f);
			Actions.setFakePlayerHotbarSlot(archer, 1);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Gravel Cleared");
		}, 115);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -158.5, 69, -80.5)), 116);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -157f, -5f), 117);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -149.5, 73, -100.5)), 118);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 50f, 29f);
			Actions.setFakePlayerHotbarSlot(archer, 7);
		}, 119);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(archer), 120);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -74.5f, 22f);
			Actions.setFakePlayerHotbarSlot(archer, 1);
			Actions.move(archer, new Vector(1.0816, 0, 0.3), 1);
		}, 121);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -138.5, 70.5, -97.5)), 122);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 161f, 42.7f), 123);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Gravel 3/6 (Obtained Wither Essence)");
			world.playSound(archer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		}, 124);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 70f, 19.5f), 125);
		Utils.scheduleTask(() -> {
			Actions.simulateAOTV(archer, new Location(world, -144.5, 69, -95.5));
			Actions.turnHead(archer, 97.9f, 5.4f);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Gravel 4/6 (Obtained Item)");
			world.playSound(archer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
		}, 126);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -7f, 0f);
			Actions.simulateAOTV(archer, new Location(world, -155.261, 69, -95.686));
			Actions.setFakePlayerHotbarSlot(archer, 7);
		}, 127);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(archer), 128);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 47.8f, -54.7f);
			Actions.setFakePlayerHotbarSlot(archer, 1);
		}, 129);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -160.5, 81, -90.5)), 130);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 39.3f, 33.6f), 131);
		Utils.scheduleTask(() -> Actions.simulateAOTV(archer, new Location(world, -161.5, 81, -89.5)), 132);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 90f, -90f);
			Actions.move(archer, new Vector(-1.12242, 0, 0), 1);
		}, 133);
		Utils.scheduleTask(() -> Actions.simulateAOTV(archer, new Location(world, -162.5, 88.38, -89.5)), 134);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 90f, 43.8f), 135);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Gravel 5/6 (Obtained Wither Essence)");
			world.playSound(archer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
			Actions.simulateAOTV(archer, new Location(world, -165.5, 88, -89.5));
		}, 136);
		Utils.scheduleTask(() -> {
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Gravel 6/6 (Obtained Item)");
			world.playSound(archer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
		}, 137);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -10.5f, -1.7f);
			Actions.simulateAOTV(archer, new Location(world, -153.855, 69, -83.063));
		}, 138);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -148.5, 72, -54.5)), 139);

		/*
		 * ███╗   ███╗██╗   ██╗███████╗███████╗██╗   ██╗███╗   ███╗
		 * ████╗ ████║██║   ██║██╔════╝██╔════╝██║   ██║████╗ ████║
		 * ██╔████╔██║██║   ██║███████╗█████╗  ██║   ██║██╔████╔██║
		 * ██║╚██╔╝██║██║   ██║╚════██║██╔══╝  ██║   ██║██║╚██╔╝██║
		 * ██║ ╚═╝ ██║╚██████╔╝███████║███████╗╚██████╔╝██║ ╚═╝ ██║
		 * ╚═╝     ╚═╝ ╚═════╝ ╚══════╝╚══════╝ ╚═════╝ ╚═╝     ╚═╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(archer, -45f, 27.1f), 140);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -136.5, 65, -42.5)), 141);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 0f, 0f);
			Actions.setFakePlayerHotbarSlot(archer, 4);
		}, 142);
		Utils.scheduleTask(Archer::simulateShoot, 143);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 129.4f, -14.5f);
			Actions.setFakePlayerHotbarSlot(archer, 1);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Museum Cleared");
		}, 144);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -149.5, 71, -52.5)), 145);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 103.1f, 7.8f), 146);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -175.5, 69, -58.5)), 147);

		/*
		 * ███╗   ███╗ █████╗ ██████╗ ██╗  ██╗███████╗████████╗
		 * ████╗ ████║██╔══██╗██╔══██╗██║ ██╔╝██╔════╝╚══██╔══╝
		 * ██╔████╔██║███████║██████╔╝█████╔╝ █████╗     ██║
		 * ██║╚██╔╝██║██╔══██║██╔══██╗██╔═██╗ ██╔══╝     ██║
		 * ██║ ╚═╝ ██║██║  ██║██║  ██║██║  ██╗███████╗   ██║
		 * ╚═╝     ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝   ╚═╝
		 */
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 80f, 1f);
			Actions.setFakePlayerHotbarSlot(archer, 7);
		}, 148);
		Utils.scheduleTask(() -> Actions.simulatePearlThrow(archer), 149);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 180f, 90f);
			Actions.move(archer, new Vector(0, 0, -1.12242), 1);
			Actions.setFakePlayerHotbarSlot(archer, 3);
		}, 150);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(archer, -174, 67, -61, -178, 66, -63), 151);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 119.1f, 67.6f);
			Actions.setFakePlayerHotbarSlot(archer, 1);
		}, 152);
		Utils.scheduleTask(() -> Actions.simulateAOTV(archer, new Location(world, -179.5, 59, -62.5)), 153);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -13.3f, 19.3f);
			Actions.setFakePlayerHotbarSlot(archer, 0);
		}, 154);
		Utils.scheduleTask(() -> {
			Actions.simulateWitherImpact(archer, new Location(world, -178.5, 59, -57.5));
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Market 1/5 (Killed Bat)");
			world.playSound(archer.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0F, 1.0F);
		}, 155);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Market 2/5 (Obtained Wither Essence)");
			world.playSound(archer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		}, 156);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 168.9f, -76.3f);
			Actions.setFakePlayerHotbarSlot(archer, 1);
			Actions.simulateAOTV(archer, new Location(world, -186.655, 69, -56.488));
		}, 157);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -187.5, 86, -60.5)), 158);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 180f, 45f);
			Actions.setFakePlayerHotbarSlot(archer, 5);
			Actions.move(archer, new Vector(0.79367, 0, -0.79367), 1);
		}, 159);
		Utils.scheduleTask(() -> Actions.simulateStonking(archer, world.getBlockAt(-188, 87, -62)), 160);
		Utils.scheduleTask(() -> Actions.simulateStonking(archer, world.getBlockAt(-188, 86, -62)), 161);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -160f, 11.9f), 162);
		Utils.scheduleTask(() -> Actions.simulateStonking(archer, world.getBlockAt(-187, 87, -62)), 163);
		Utils.scheduleTask(() -> {
			Actions.simulateStonking(archer, world.getBlockAt(-187, 87, -63));
			Actions.move(archer, new Vector(0, 0, -0.26), 1);
		}, 164);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Market 3/5 (Opened Chest)");
			world.playSound(archer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 165);
		Utils.scheduleTask(() -> {
			Actions.simulateAOTV(archer, new Location(world, -187.3, 86, -60.7)); // get rubberbanded back to the last valid location
			Actions.turnHead(archer, 44.6f, 50f);
			Actions.setFakePlayerHotbarSlot(archer, 1);
		}, 166);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -199.5, 67, -48.5)), 167);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 43.5f, -5.9f), 168);
		Utils.scheduleTask(() -> Actions.simulateAOTV(archer, new Location(world, -203.5, 67, -44.5)), 169);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -72.1f, 39.2f), 170);
		Utils.scheduleTask(() -> Actions.simulateAOTV(archer, new Location(world, -201.5, 66, -43.5)), 171);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -180f, 90f), 172);
		Utils.scheduleTask(() -> Actions.simulateAOTV(archer, new Location(world, -201.5, 60, -43.5)), 173);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -180f, 5f), 174);
		Utils.scheduleTask(() -> Actions.simulateAOTV(archer, new Location(world, -201.5, 60, -55.5)), 175);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Market 4/5 (Opened Chest)");
			world.playSound(archer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 176);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -44.1f, -57.6f), 177);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -199.5, 67, -53.5)), 178);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -180f, -51.5f), 179);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -199.5, 69.5, -54.5)), 180);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 101.4f, 11.6f), 181);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -209.5, 69, -56.5)), 182);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 90f, 0f);
			Actions.setFakePlayerHotbarSlot(archer, 4);
		}, 183);
		Utils.scheduleTask(Archer::simulateShoot, 184);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 90f, -52f);
			Actions.setFakePlayerHotbarSlot(archer, 1);
		}, 185);
		Utils.scheduleTask(() -> {
			Actions.simulateEtherwarp(archer, new Location(world, -212.5, 75, -56.5));
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Market Cleared");
		}, 186);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 113f, -55f);
			Actions.setFakePlayerHotbarSlot(archer, 5);
			Actions.move(archer, new Vector(-0.79476, 0, -0.33736), 1);
		}, 187);
		Utils.scheduleTask(() -> Actions.simulateStonking(archer, world.getBlockAt(-215, 78, -58)), 188);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Market 5/5 (Opened Chest)");
			world.playSound(archer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 189);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 8.9f, 19f);
			Actions.setFakePlayerHotbarSlot(archer, 1);
		}, 190);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(archer, new Location(world, -216.5, 69, -34.5)), 191);


		/*
		 * ██╗   ██╗███████╗██╗     ██╗      ██████╗ ██╗    ██╗
		 * ╚██╗ ██╔╝██╔════╝██║     ██║     ██╔═══██╗██║    ██║
		 *  ╚████╔╝ █████╗  ██║     ██║     ██║   ██║██║ █╗ ██║
		 *   ╚██╔╝  ██╔══╝  ██║     ██║     ██║   ██║██║███╗██║
		 *    ██║   ███████╗███████╗███████╗╚██████╔╝╚███╔███╔╝
		 *    ╚═╝   ╚══════╝╚══════╝╚══════╝ ╚═════╝  ╚══╝╚══╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(archer, 0f, 11.4f), 192);
		Utils.scheduleTask(() -> Actions.simulateAOTV(archer, new Location(world, -216.5, 69, -26.5)), 193);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(archer, 4), 194);
		Utils.scheduleTask(Archer::simulateShoot, 195);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -137.3f, 2.9f);
			Actions.setFakePlayerHotbarSlot(archer, 1);
		}, 196);
		Utils.scheduleTask(() -> Actions.simulateAOTV(archer, new Location(world, -209.5, 70, -34.5)), 197);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -135f, 11.4f);
			Actions.setFakePlayerHotbarSlot(archer, 3);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Yellow Cleared");
		}, 198);
		Utils.scheduleTask(() -> Actions.simulateCrypt(archer, -207, 70, -35, -209, 72, -37), 199);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(archer, 4), 200);
		Utils.scheduleTask(() -> {
			simulateShoot();
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Crypt 5/5");
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Clear finished in 204 ticks (10.20 seconds)");
		}, 201);
		if(doContinue) {
			Utils.scheduleTask(() -> {
				maxor(true);
				Actions.teleport(archer, new Location(world, 73.5, 221, 13.5));
			}, 1025);
		}
	}

	public static void maxor(boolean doContinue) {
	}

	private static void simulateShoot() {
		Actions.simulateRightClickAir(archer);
		Location l = archer.getLocation();
		l.add(l.getDirection());
		l.setY(l.getY() + 1.62);
		int power = archer.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.POWER);
		int strength;
		try {
			strength = Objects.requireNonNull(archer.getPotionEffect(PotionEffectType.STRENGTH)).getAmplifier();
		} catch(Exception exception) {
			strength = 0;
		}
		double powerBonus;
		try {
			powerBonus = power * 0.05;
			if(power == 7) {
				powerBonus += 0.05;
			}
		} catch(Exception exception) {
			powerBonus = 0;
		}

		double strengthBonus;
		try {
			strengthBonus = 0.15 + 0.15 * strength;
		} catch(Exception exception) {
			strengthBonus = 0;
		}

		double add = powerBonus + strengthBonus;

		// Duplex Arrow
		double finalAdd = add;
		Utils.scheduleTask(() -> {
			Arrow arrow = world.spawnArrow(l, l.getDirection(), 3, 0.1F);
			arrow.setDamage(0.5 + finalAdd);
			arrow.setPierceLevel(4);
			arrow.setShooter(archer);
			arrow.setWeapon(archer.getInventory().getItemInMainHand());
			arrow.addScoreboardTag("TerminatorArrow");
		}, 3);

		// Archer Bonus Arrows
		add *= 5;

		double finalAdd1 = add;
		Utils.scheduleTask(() -> {
			Arrow arrow = world.spawnArrow(l, l.getDirection(), 3, 0.1F);
			arrow.setDamage(2.5 + finalAdd1);
			arrow.setPierceLevel(4);
			arrow.setShooter(archer);
			arrow.setWeapon(archer.getInventory().getItemInMainHand());
			arrow.addScoreboardTag("TerminatorArrow");
		}, 5);

		double finalAdd2 = add;
		Utils.scheduleTask(() -> {
			Arrow arrow = world.spawnArrow(l, l.getDirection(), 3, 0.1F);
			arrow.setDamage(2.5 + finalAdd2);
			arrow.setPierceLevel(4);
			arrow.setShooter(archer);
			arrow.setWeapon(archer.getInventory().getItemInMainHand());
			arrow.addScoreboardTag("TerminatorArrow");
		}, 10);
	}

	private static void explosiveShot() {
		Vector v = archer.getLocation().getDirection();
		Location lLeft = archer.getLocation().clone().add(v);
		lLeft.setYaw(lLeft.getYaw() - 6);
		lLeft.setY(lLeft.getY() + 1.62);

		Location l = archer.getLocation().clone().add(v);
		l.setY(l.getY() + 1.62);

		Location lRight = archer.getLocation().clone().add(v);
		lRight.setYaw(lRight.getYaw() + 6);
		lRight.setY(lRight.getY() + 1.62);

		for(Location shootLoc : List.of(lLeft, l, lRight)) {
			Arrow arrow = archer.getWorld().spawnArrow(l, shootLoc.getDirection(), 4, 0.1F);
			arrow.setDamage(0);
			arrow.setPierceLevel(1);
			arrow.setShooter(archer);
			arrow.setWeapon(archer.getInventory().getItemInMainHand());

			new BukkitRunnable() {
				@Override
				public void run() {
					if(!arrow.isValid() || arrow.isDead() || arrow.isOnGround()) {
						Location impact = arrow.getLocation();

						for(Entity e : arrow.getNearbyEntities(3, 3, 3)) {
							if(e instanceof LivingEntity target && !e.equals(archer)) {
								target.damage(10, archer);
							}
						}

						// Visual effects
						world.spawnParticle(Particle.EXPLOSION, impact, 10, 0.5, 0.5, 0.5, 0);
						world.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1, 1f);

						arrow.remove();
						cancel();
					}
				}
			}.runTaskTimer(M7tas.getInstance(), 1L, 1L);
		}
	}

	public static Player getArcher() {
		return archer;
	}
}
