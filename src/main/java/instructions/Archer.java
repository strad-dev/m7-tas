package instructions;

import instructions.bosses.Goldor;
import instructions.bosses.WitherKing;
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
		Objects.requireNonNull(archer.getInventory().getItem(4)).addUnsafeEnchantment(Enchantment.POWER, 24);

		switch(section) {
			case "all", "clear" -> {
				Actions.teleport(archer, new Location(world, -118.5, 70, -202.5, 0f, 0f));
				Actions.setHotbarSlot(archer, 1);
				Utils.scheduleTask(() -> Actions.move(archer, new Vector(0, 0, 1.12242), 5), 160);
				Utils.scheduleTask(() -> clear(section.equals("all")), 162);
			}
			case "maxor", "boss" -> {
				Actions.teleport(archer, new Location(world, 73.5, 221, 13.5, 0f, 0f));
				Actions.swapItems(archer, 1, 28);
				Actions.swapItems(archer, 7, 35);
				if(section.equals("maxor")) {
					Utils.scheduleTask(() -> maxor(false), 60);
				} else {
					Utils.scheduleTask(() -> maxor(true), 60);
				}
			}
			case "storm" -> {
				Actions.teleport(archer, new Location(world, 46.687, 169, 57.747, 177.8f, 0f));
				Actions.swapItems(archer, 1, 28);
				Actions.swapItems(archer, 7, 35);
				Utils.scheduleTask(() -> storm(false), 60);
			}
			case "goldor" -> {
				Actions.teleport(archer, new Location(world, 89.565, 115.0625, 132.272, -128f, -19f));
				Actions.swapItems(archer, 1, 28);
				Actions.swapItems(archer, 6, 33);
				Actions.swapItems(archer, 7, 35);
				Utils.scheduleTask(Archer::explosiveShot, 57);
				Utils.scheduleTask(() -> {
					Actions.setHotbarSlot(archer, 1);
					Actions.turnHead(archer, 90f, 0f);
				}, 58);
				Utils.scheduleTask(() -> Actions.move(archer, new Vector(-1.403, 0, 0), 1), 59);
				Utils.scheduleTask(() -> goldor(false), 60);
			}
			case "necron" -> {
				Actions.teleport(archer, new Location(world, 56.488, 64, 111.700, -180f, 0f));
				Actions.swapItems(archer, 1, 28);
				Actions.swapItems(archer, 5, 32);
				Actions.swapItems(archer, 6, 33);
				Actions.swapItems(archer, 7, 35);
				Utils.scheduleTask(() -> necron(false), 60);
			}
			case "witherking" -> {
				Actions.teleport(archer, new Location(world, 22.3, 6, 59.408, 65.6f, 29.3f));
				Actions.swapItems(archer, 1, 28);
				Actions.swapItems(archer, 3, 30);
				Actions.swapItems(archer, 5, 32);
				Actions.swapItems(archer, 6, 33);
				Actions.swapItems(archer, 7, 35);
				Actions.swapItems(archer, 11, 39);
				Utils.scheduleTask(Archer::witherKing, 60);
			}
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
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -119.5, 69, -187.5)), 4);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 20.4f, 8f);
			Actions.setHotbarSlot(archer, 4);
		}, 5);
		Utils.scheduleTask(() -> {
			shoot();
			Actions.move(archer, new Vector(-0.39124, 0, 1.05202), 2);
		}, 6);
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(archer, 2);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Red Blue Cleared");
		}, 7);
		Utils.scheduleTask(() -> Bukkit.broadcastMessage(ChatColor.GOLD + "[MVP" + ChatColor.DARK_BLUE + "++" + ChatColor.GOLD + "] akc0303 " + ChatColor.GREEN + "has obtained " + ChatColor.DARK_GRAY + "Wither Key" + ChatColor.GREEN + "!"), 8);
		Utils.scheduleTask(() -> Actions.leap(archer, Mage.get()), 9);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 0f, 8.6f);
			Actions.setHotbarSlot(archer, 1);
		}, 10);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -120.5, 69, -127.5)), 29);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 0f, 35f), 30);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -120.5, 67, -122.5)), 31);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 4), 32);
		Utils.scheduleTask(() -> {
			shoot();
			Actions.move(archer, new Vector(0, 0, 1.12242), 2);
		}, 33);
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(archer, 1);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Deathmite Cleared");
		}, 34);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 90f, 0f);
			Bukkit.broadcastMessage(ChatColor.GOLD + "[MVP" + ChatColor.DARK_BLUE + "++" + ChatColor.GOLD + "] akc0303 " + ChatColor.GREEN + "has obtained " + ChatColor.RED + "Blood Key" + ChatColor.GREEN + "!");
		}, 35);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -127.5, 69, -120.5)), 36);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 90f, 3.5f), 37);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -154.5, 69, -120.5)), 38);

		/*
		 * ██╗    ██╗███████╗██╗     ██╗
		 * ██║    ██║██╔════╝██║     ██║
		 * ██║ █╗ ██║█████╗  ██║     ██║
		 * ██║███╗██║██╔══╝  ██║     ██║
		 * ╚███╔███╔╝███████╗███████╗███████╗
		 *  ╚══╝╚══╝ ╚══════╝╚══════╝╚══════╝
		 */
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 4), 39);
		Utils.scheduleTask(Archer::shoot, 40);
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(archer, 1);
			Actions.turnHead(archer, -175.9f, -8.7f);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Well Cleared");
		}, 41);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -152.5, 76, -152.5)), 42);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -180f, -36.5f), 43);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -152.5, 87, -163.5)), 44);
		Utils.scheduleTask(() -> {
			Actions.swingHand(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Well 1/7 (Opened Chest)");
			world.playSound(archer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 45);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 22.9f, -17.2f), 46);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -163.5, 98, -136.5)), 47);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 175.3f, 18.4f), 48);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -165.5, 91, -162.5)), 49);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -6.2f, 4f);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Well 2/7 (Obtained Item)");
			world.playSound(archer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
		}, 50);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -162.5, 91, -139.5)), 51);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -67.8f, 25.3f), 52);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -151.5, 87, -135.5)), 53);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -7.3f, 2.9f), 54);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -149.5, 88, -119.5)), 55);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 0f, 21.3f);
			Actions.setHotbarSlot(archer, 5);
		}, 56);
		Utils.scheduleTask(() -> Actions.stonk(archer, world.getBlockAt(-150, 89, -119)), 57);
		Utils.scheduleTask(() -> {
			Actions.swingHand(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Well 3/7 (Opened Chest)");
			world.playSound(archer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 58);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 120f, 0f);
			Actions.setHotbarSlot(archer, 7);
		}, 59);
		Utils.scheduleTask(() -> Actions.throwPearl(archer), 60);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -90f, 0f);
			Actions.setHotbarSlot(archer, 1);
		}, 61);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -143.5, 90, -119.5)), 62);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 106f, 66f);
			Actions.setHotbarSlot(archer, 7);
		}, 63);
		Utils.scheduleTask(() -> Actions.throwPearl(archer), 64);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -3f, 14.3f);
			Actions.setHotbarSlot(archer, 1);
		}, 65);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -142.5, 89, -109.5)), 66);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -90f, -72.6f), 67);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -141.5, 95, -109.5)), 68);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 146.8f, 19.3f), 69);
		Utils.scheduleTask(() -> {
			Actions.swingHand(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Well 4/7 (Obtained Wither Essence)");
			world.playSound(archer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		}, 70);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 45.9f, 45f), 71);
		Utils.scheduleTask(() -> {
			Actions.swingHand(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Well 5/7 (Obtained Wither Essence)");
			world.playSound(archer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		}, 72);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 83.9f, 8.7f);
			Actions.AOTV(archer, new Location(world, -164.153, 87, -128.022));
		}, 73);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -174.5, 87, -126.5)), 74);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 0f, -50f);
			Actions.setHotbarSlot(archer, 5);
		}, 75);
		Utils.scheduleTask(() -> Actions.stonk(archer, world.getBlockAt(-175, 89, -126)), 76);
		Utils.scheduleTask(() -> Actions.stonk(archer, world.getBlockAt(-175, 90, -126)), 77);
		Utils.scheduleTask(() -> Actions.stonk(archer, world.getBlockAt(-175, 90, -125)), 78);
		Utils.scheduleTask(() -> {
			Actions.swingHand(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Well 6/7 (Opened Chest)");
			world.playSound(archer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 79);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 87.5f, 1.8f);
			Actions.setHotbarSlot(archer, 1);
			Actions.AOTV(archer, new Location(world, -151.559, 69, -121.912));
		}, 80);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -181.5, 70, -120.5)), 81);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 82f, 11f);
			Actions.setHotbarSlot(archer, 7);
		}, 82);
		Utils.scheduleTask(() -> Actions.throwPearl(archer), 83);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 106f, 55f);
			Actions.setHotbarSlot(archer, 1);
		}, 84);
		Utils.scheduleTask(() -> Actions.AOTV(archer, new Location(world, -188.116, 59, -122.397)), 85);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -53.8f, 29.3f), 86);
		Utils.scheduleTask(() -> Actions.AOTV(archer, new Location(world, -183.5, 57, -119.5)), 87);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 97.9f, 5.4f);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Well 7/7 (Obtained Item)");
			world.playSound(archer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
			Actions.AOTV(archer, new Location(world, -187.230, 70, -119.704));
		}, 88);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -214.5, 69, -123.5)), 89);

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
			Actions.setHotbarSlot(archer, 4);
		}, 90);
		Utils.scheduleTask(Archer::shoot, 91);
		Utils.scheduleTask(() -> Actions.salvation(archer), 92);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 2.7f, 4.7f);
			Actions.setHotbarSlot(archer, 1);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Tomioka Cleared");
		}, 93);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -215.5, 69, -103.5)), 94);

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
			Actions.crypt(archer, -217, 70, -99, -215, 69, -97);
		}, 96);
		Utils.scheduleTask(() -> {
			Actions.etherwarp(archer, new Location(world, -227.5, 69, -92.5));
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Crypt 3/5");
		}, 97);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -52.8f, 9.2f), 98);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -219.5, 69, -86.5)), 99);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -107.7f, 4.6f), 100);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -200.5, 69, -92.5)), 101);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -90f, 13.5f);
			Actions.setHotbarSlot(archer, 5);
		}, 102);
		Utils.scheduleTask(() -> Actions.stonk(archer, world.getBlockAt(-199, 70, -93)), 103);
		Utils.scheduleTask(() -> {
			Actions.swingHand(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Gravel 1/6 (Opened Chest)");
			world.playSound(archer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 104);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 22.2f, -57.9f);
			Actions.setHotbarSlot(archer, 1);
			Actions.move(archer, new Vector(-0.8634, 0, 0), 1);
		}, 105);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -203.5, 81.5, -86.5)), 106);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -99.7f, -52.3f);
			Actions.setHotbarSlot(archer, 0);
		}, 107);
		Utils.scheduleTask(() -> {
			Actions.witherImpact(archer, new Location(world, -197.472, 89.41224, -87.53));
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Gravel 2/6 (Killed Bat)");
			world.playSound(archer.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0F, 1.0F);
		}, 108);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 80.1f, 52.2f);
			Actions.setHotbarSlot(archer, 1);
		}, 109);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -213.5, 69, -84.5)), 110);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -85.1f, 2.75f), 111); // requires 3 sig figs of presicion here
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -179.5, 69, -81.5)), 112);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -62.3f, 3.3f);
			Actions.setHotbarSlot(archer, 4);
		}, 113);
		Utils.scheduleTask(Archer::shoot, 114);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -87.1f, 4.4f);
			Actions.setHotbarSlot(archer, 1);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Gravel Cleared");
		}, 115);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -158.5, 69, -80.5)), 116);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -157f, -5f), 117);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -149.5, 73, -100.5)), 118);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 50f, 29f);
			Actions.setHotbarSlot(archer, 7);
		}, 119);
		Utils.scheduleTask(() -> Actions.throwPearl(archer), 120);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -74.5f, 22f);
			Actions.setHotbarSlot(archer, 1);
			Actions.move(archer, new Vector(1.0816, 0, 0.3), 1);
		}, 121);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -138.5, 70.5, -97.5)), 122);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 161f, 42.7f), 123);
		Utils.scheduleTask(() -> {
			Actions.swingHand(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Gravel 3/6 (Obtained Wither Essence)");
			world.playSound(archer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		}, 124);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 70f, 19.5f), 125);
		Utils.scheduleTask(() -> {
			Actions.AOTV(archer, new Location(world, -144.5, 69, -95.5));
			Actions.turnHead(archer, 97.9f, 5.4f);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Gravel 4/6 (Obtained Item)");
			world.playSound(archer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
		}, 126);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -7f, 0f);
			Actions.AOTV(archer, new Location(world, -155.261, 69, -95.686));
			Actions.setHotbarSlot(archer, 7);
		}, 127);
		Utils.scheduleTask(() -> Actions.throwPearl(archer), 128);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 47.8f, -54.7f);
			Actions.setHotbarSlot(archer, 1);
		}, 129);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -160.5, 81, -90.5)), 130);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 39.3f, 33.6f), 131);
		Utils.scheduleTask(() -> Actions.AOTV(archer, new Location(world, -161.5, 81, -89.5)), 132);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 90f, -90f);
			Actions.move(archer, new Vector(-1.12242, 0, 0), 1);
		}, 133);
		Utils.scheduleTask(() -> Actions.AOTV(archer, new Location(world, -162.5, 88.38, -89.5)), 134);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 90f, 43.8f), 135);
		Utils.scheduleTask(() -> {
			Actions.swingHand(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Gravel 5/6 (Obtained Wither Essence)");
			world.playSound(archer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
			Actions.AOTV(archer, new Location(world, -165.5, 88, -89.5));
		}, 136);
		Utils.scheduleTask(() -> {
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Gravel 6/6 (Obtained Item)");
			world.playSound(archer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
		}, 137);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -10.5f, -1.7f);
			Actions.AOTV(archer, new Location(world, -153.855, 69, -83.063));
		}, 138);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -148.5, 72, -54.5)), 139);

		/*
		 * ███╗   ███╗██╗   ██╗███████╗███████╗██╗   ██╗███╗   ███╗
		 * ████╗ ████║██║   ██║██╔════╝██╔════╝██║   ██║████╗ ████║
		 * ██╔████╔██║██║   ██║███████╗█████╗  ██║   ██║██╔████╔██║
		 * ██║╚██╔╝██║██║   ██║╚════██║██╔══╝  ██║   ██║██║╚██╔╝██║
		 * ██║ ╚═╝ ██║╚██████╔╝███████║███████╗╚██████╔╝██║ ╚═╝ ██║
		 * ╚═╝     ╚═╝ ╚═════╝ ╚══════╝╚══════╝ ╚═════╝ ╚═╝     ╚═╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(archer, -45f, 27.1f), 140);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -136.5, 65, -42.5)), 141);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 0f, 0f);
			Actions.setHotbarSlot(archer, 4);
		}, 142);
		Utils.scheduleTask(Archer::shoot, 143);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 129.4f, -14.5f);
			Actions.setHotbarSlot(archer, 1);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Museum Cleared");
		}, 144);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -149.5, 71, -52.5)), 145);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 103.1f, 7.8f), 146);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -175.5, 69, -58.5)), 147);

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
			Actions.setHotbarSlot(archer, 7);
		}, 148);
		Utils.scheduleTask(() -> Actions.throwPearl(archer), 149);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 180f, 90f);
			Actions.move(archer, new Vector(0, 0, -1.12242), 1);
			Actions.setHotbarSlot(archer, 3);
		}, 150);
		Utils.scheduleTask(() -> Actions.superboom(archer, -174, 67, -61, -178, 66, -63), 151);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 119.1f, 67.6f);
			Actions.setHotbarSlot(archer, 1);
		}, 152);
		Utils.scheduleTask(() -> Actions.AOTV(archer, new Location(world, -179.5, 59, -62.5)), 153);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -13.3f, 19.3f);
			Actions.setHotbarSlot(archer, 0);
		}, 154);
		Utils.scheduleTask(() -> {
			Actions.witherImpact(archer, new Location(world, -178.5, 59, -57.5));
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Market 1/5 (Killed Bat)");
			world.playSound(archer.getLocation(), Sound.ENTITY_BAT_DEATH, 1.0F, 1.0F);
		}, 155);
		Utils.scheduleTask(() -> {
			Actions.swingHand(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Market 2/5 (Obtained Wither Essence)");
			world.playSound(archer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
		}, 156);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 168.9f, -76.3f);
			Actions.setHotbarSlot(archer, 1);
			Actions.AOTV(archer, new Location(world, -186.655, 69, -56.488));
		}, 157);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -187.5, 86, -60.5)), 158);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 180f, 45f);
			Actions.setHotbarSlot(archer, 5);
			Actions.move(archer, new Vector(0.79367, 0, -0.79367), 1);
		}, 159);
		Utils.scheduleTask(() -> Actions.stonk(archer, world.getBlockAt(-188, 87, -62)), 160);
		Utils.scheduleTask(() -> Actions.stonk(archer, world.getBlockAt(-188, 86, -62)), 161);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -160f, 11.9f), 162);
		Utils.scheduleTask(() -> Actions.stonk(archer, world.getBlockAt(-187, 87, -62)), 163);
		Utils.scheduleTask(() -> {
			Actions.stonk(archer, world.getBlockAt(-187, 87, -63));
			Actions.move(archer, new Vector(0, 0, -0.26), 1);
		}, 164);
		Utils.scheduleTask(() -> {
			Actions.swingHand(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Market 3/5 (Opened Chest)");
			world.playSound(archer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 165);
		Utils.scheduleTask(() -> {
			Actions.AOTV(archer, new Location(world, -187.3, 86, -60.7)); // get rubberbanded back to the last valid location
			Actions.turnHead(archer, 44.6f, 50f);
			Actions.setHotbarSlot(archer, 1);
		}, 166);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -199.5, 67, -48.5)), 167);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 43.5f, -5.9f), 168);
		Utils.scheduleTask(() -> Actions.AOTV(archer, new Location(world, -203.5, 67, -44.5)), 169);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -72.1f, 39.2f), 170);
		Utils.scheduleTask(() -> Actions.AOTV(archer, new Location(world, -201.5, 66, -43.5)), 171);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -180f, 90f), 172);
		Utils.scheduleTask(() -> Actions.AOTV(archer, new Location(world, -201.5, 60, -43.5)), 173);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -180f, 5f), 174);
		Utils.scheduleTask(() -> Actions.AOTV(archer, new Location(world, -201.5, 60, -55.5)), 175);
		Utils.scheduleTask(() -> {
			Actions.swingHand(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Market 4/5 (Opened Chest)");
			world.playSound(archer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 176);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -44.1f, -57.6f), 177);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -199.5, 67, -53.5)), 178);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -180f, -51.5f), 179);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -199.5, 69.5, -54.5)), 180);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 101.4f, 11.6f), 181);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -209.5, 69, -56.5)), 182);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 90f, 0f);
			Actions.setHotbarSlot(archer, 4);
		}, 183);
		Utils.scheduleTask(Archer::shoot, 184);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 90f, -52f);
			Actions.setHotbarSlot(archer, 1);
		}, 185);
		Utils.scheduleTask(() -> {
			Actions.etherwarp(archer, new Location(world, -212.5, 75, -56.5));
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Market Cleared");
		}, 186);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 113f, -55f);
			Actions.setHotbarSlot(archer, 5);
			Actions.move(archer, new Vector(-0.79476, 0, -0.33736), 1);
		}, 187);
		Utils.scheduleTask(() -> Actions.stonk(archer, world.getBlockAt(-215, 78, -58)), 188);
		Utils.scheduleTask(() -> {
			Actions.swingHand(archer);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Market 5/5 (Opened Chest)");
			world.playSound(archer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 189);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 8.9f, 19f);
			Actions.setHotbarSlot(archer, 1);
		}, 190);
		Utils.scheduleTask(() -> Actions.etherwarp(archer, new Location(world, -216.5, 69, -34.5)), 191);


		/*
		 * ██╗   ██╗███████╗██╗     ██╗      ██████╗ ██╗    ██╗
		 * ╚██╗ ██╔╝██╔════╝██║     ██║     ██╔═══██╗██║    ██║
		 *  ╚████╔╝ █████╗  ██║     ██║     ██║   ██║██║ █╗ ██║
		 *   ╚██╔╝  ██╔══╝  ██║     ██║     ██║   ██║██║███╗██║
		 *    ██║   ███████╗███████╗███████╗╚██████╔╝╚███╔███╔╝
		 *    ╚═╝   ╚══════╝╚══════╝╚══════╝ ╚═════╝  ╚══╝╚══╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(archer, 0f, 11.4f), 192);
		Utils.scheduleTask(() -> Actions.AOTV(archer, new Location(world, -216.5, 69, -26.5)), 193);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 4), 194);
		Utils.scheduleTask(Archer::shoot, 195);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -137.3f, 2.9f);
			Actions.setHotbarSlot(archer, 1);
		}, 196);
		Utils.scheduleTask(() -> Actions.AOTV(archer, new Location(world, -209.5, 70, -34.5)), 197);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -135f, 11.4f);
			Actions.setHotbarSlot(archer, 3);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Yellow Cleared");
		}, 198);
		Utils.scheduleTask(() -> Actions.crypt(archer, -207, 70, -35, -209, 72, -37), 199);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 4), 200);
		Utils.scheduleTask(() -> {
			shoot();
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Crypt 5/5");
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Clear finished in 204 ticks (10.20 seconds)");
		}, 201);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 45f, 11.4f), 202);
		Utils.scheduleTask(() -> {
			Actions.swapItems(archer, 1, 28);
			Actions.swapItems(archer, 7, 35);
		}, 203);
		if(doContinue) {
			Utils.scheduleTask(() -> {
				maxor(true);
				Actions.teleport(archer, new Location(world, 73.5, 221, 13.5));
			}, 1025);
		}
	}

	public static void maxor(boolean doContinue) {
		Actions.setHotbarSlot(archer, 6);
		Actions.move(archer, new Vector(-0.0611, 0, 1.1208), 49);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 3.1f, 0f), 1);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.01416, 0, 0.2596), 1), 2);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 90f, 0f), 51);
		Utils.scheduleTask(() -> {
			Actions.jump(archer);
			Actions.move(archer, new Vector(-1.12242, 0, 0), 4);
		}, 52);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -57.3f, 10.2f), 56);
		Utils.scheduleTask(() -> Actions.gyro(archer, new Location(world, 73.5, 225, 73.5)), 161); // gyro comes up in 30 seconds (600 ticks) | tick 761 | storm tick 262
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 4), 162);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -63.2f, -11f), 163);
		Utils.scheduleTask(Archer::shoot, 163);
		Utils.scheduleTask(() -> Actions.salvation(archer), 164);
		Utils.scheduleTask(Archer::shoot, 168);
		Utils.scheduleTask(Archer::shoot, 198); // maxor is damageable here | tank's "DPS" will get it down to 382/400 | 337.8
		Utils.scheduleTask(() -> Actions.salvation(archer), 199); // 329.3
		Utils.scheduleTask(Archer::shoot, 203); // 285.1
		Utils.scheduleTask(() -> Actions.salvation(archer), 204); // 276.6
		Utils.scheduleTask(Archer::shoot, 208); // 232.4
		Utils.scheduleTask(() -> Actions.salvation(archer), 209); // 223.9
		Utils.scheduleTask(Archer::shoot, 213); // 179.7
		Utils.scheduleTask(() -> Actions.salvation(archer), 214); // 171.2
		Utils.scheduleTask(Archer::shoot, 218); // 127
		Utils.scheduleTask(() -> Actions.salvation(archer), 219); // 118.5
		Utils.scheduleTask(() -> Actions.turnHead(archer, -52.7f, -8.3f), 220);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 2), 221);
		Utils.scheduleTask(() -> Actions.leap(archer, Berserk.get()), 332);
		Utils.scheduleTask(() -> {
			Actions.move(archer, new Vector(-0.9261, 0, 0.6341), 13);
			Actions.setHotbarSlot(archer, 4);
		}, 333);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 177.8f, 0f), 346);
		if(doContinue) {
			Utils.scheduleTask(() -> storm(true), 499);
		}
	}

	public static void storm(boolean doContinue) {
		Actions.setHotbarSlot(archer, 4);
		Utils.scheduleTask(Archer::shoot, 1);
		Utils.scheduleTask(() -> Actions.salvation(archer), 2);
		Utils.scheduleTask(Archer::shoot, 6);
		Utils.scheduleTask(() -> Actions.salvation(archer), 7);
		Utils.scheduleTask(Archer::shoot, 11);
		Utils.scheduleTask(() -> Actions.salvation(archer), 12);
		Utils.scheduleTask(Archer::shoot, 16);
		Utils.scheduleTask(() -> Actions.salvation(archer), 17);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 90.9f, -9f), 18);
		Utils.scheduleTask(Archer::shoot, 21);
		Utils.scheduleTask(() -> Actions.salvation(archer), 22);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 100.8f, -8.2f), 23);
		Utils.scheduleTask(Archer::shoot, 26);
		Utils.scheduleTask(() -> Actions.salvation(archer), 27);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 110f, -7.5f), 28);
		Utils.scheduleTask(Archer::shoot, 31);
		Utils.scheduleTask(() -> Actions.salvation(archer), 32);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 180f, 0f), 33);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0, 0, -1.12242), 8), 34);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 163.6f, -4f), 41);
		Utils.scheduleTask(Archer::shoot, 44);
		Utils.scheduleTask(() -> Actions.salvation(archer), 45);
		Utils.scheduleTask(Archer::shoot, 49);
		Utils.scheduleTask(() -> Actions.salvation(archer), 50);
		Utils.scheduleTask(Archer::shoot, 54);
		Utils.scheduleTask(() -> Actions.salvation(archer), 55);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 159f, -4f), 51);
		Utils.scheduleTask(Archer::shoot, 54);
		Utils.scheduleTask(() -> Actions.salvation(archer), 55);
		Utils.scheduleTask(Archer::shoot, 59);
		Utils.scheduleTask(() -> Actions.salvation(archer), 60);
		Utils.scheduleTask(Archer::shoot, 64);
		Utils.scheduleTask(() -> Actions.salvation(archer), 65);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 154f, -4f), 66);
		Utils.scheduleTask(Archer::shoot, 69);
		Utils.scheduleTask(() -> Actions.salvation(archer), 70);
		Utils.scheduleTask(Archer::shoot, 74);
		Utils.scheduleTask(() -> Actions.salvation(archer), 75);
		Utils.scheduleTask(Archer::shoot, 79);
		Utils.scheduleTask(() -> Actions.salvation(archer), 80);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -90f, 0f), 81);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(1.112242, 0, 0), 11), 82);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -120.9f, 22.7f), 93);
		Utils.scheduleTask(Archer::shoot, 94);
		Utils.scheduleTask(() -> Actions.salvation(archer), 95);
		Utils.scheduleTask(Archer::shoot, 99);
		Utils.scheduleTask(() -> Actions.salvation(archer), 100);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -104.3f, 14.5f), 101);
		Utils.scheduleTask(Archer::shoot, 104);
		Utils.scheduleTask(() -> Actions.salvation(archer), 105);
		Utils.scheduleTask(Archer::shoot, 109);
		Utils.scheduleTask(() -> Actions.salvation(archer), 110);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -94.8f, 9.9f), 111);
		Utils.scheduleTask(Archer::shoot, 114);
		Utils.scheduleTask(() -> Actions.salvation(archer), 115);
		Utils.scheduleTask(Archer::shoot, 119);
		Utils.scheduleTask(() -> Actions.salvation(archer), 120);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -85.7f, 30.4f), 121);
		Utils.scheduleTask(Archer::shoot, 124);
		Utils.scheduleTask(() -> Actions.salvation(archer), 125);
		Utils.scheduleTask(Archer::shoot, 129);
		Utils.scheduleTask(() -> Actions.salvation(archer), 130);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -87.7f, 16.2f), 131);
		Utils.scheduleTask(Archer::shoot, 134);
		Utils.scheduleTask(() -> Actions.salvation(archer), 135);
		Utils.scheduleTask(Archer::shoot, 139);
		Utils.scheduleTask(() -> Actions.salvation(archer), 140);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -87.7f, 3.7f), 141);
		Utils.scheduleTask(Archer::shoot, 144);
		Utils.scheduleTask(() -> Actions.salvation(archer), 145);
		Utils.scheduleTask(Archer::shoot, 149);
		Utils.scheduleTask(() -> Actions.salvation(archer), 150);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -55.1f, 24.7f), 151);
		Utils.scheduleTask(Archer::shoot, 154);
		Utils.scheduleTask(() -> Actions.salvation(archer), 155);
		Utils.scheduleTask(Archer::shoot, 159);
		Utils.scheduleTask(() -> Actions.salvation(archer), 160);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -66.9f, 14.6f), 161);
		Utils.scheduleTask(Archer::shoot, 164);
		Utils.scheduleTask(() -> Actions.salvation(archer), 165);
		Utils.scheduleTask(Archer::shoot, 169);
		Utils.scheduleTask(() -> Actions.salvation(archer), 170);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -67.3f, 6.3f), 171);
		Utils.scheduleTask(Archer::shoot, 174);
		Utils.scheduleTask(() -> Actions.salvation(archer), 175);
		Utils.scheduleTask(Archer::shoot, 179);
		Utils.scheduleTask(() -> Actions.salvation(archer), 180);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -27.4f, 18.2f), 181);
		Utils.scheduleTask(Archer::shoot, 184);
		Utils.scheduleTask(() -> Actions.salvation(archer), 185);
		Utils.scheduleTask(Archer::shoot, 189);
		Utils.scheduleTask(() -> Actions.salvation(archer), 190);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -43.9f, 11.2f), 191);
		Utils.scheduleTask(Archer::shoot, 194);
		Utils.scheduleTask(() -> Actions.salvation(archer), 195);
		Utils.scheduleTask(Archer::shoot, 199);
		Utils.scheduleTask(() -> Actions.salvation(archer), 200);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -56.7f, 6f), 201);
		Utils.scheduleTask(Archer::shoot, 204);
		Utils.scheduleTask(() -> Actions.salvation(archer), 205);
		Utils.scheduleTask(Archer::shoot, 209);
		Utils.scheduleTask(() -> Actions.salvation(archer), 210);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -89f, 12f), 211);
		Utils.scheduleTask(Archer::shoot, 214);
		Utils.scheduleTask(() -> Actions.salvation(archer), 215);
		Utils.scheduleTask(Archer::shoot, 219);
		Utils.scheduleTask(() -> Actions.salvation(archer), 220);
		Utils.scheduleTask(Archer::shoot, 224);
		Utils.scheduleTask(() -> Actions.salvation(archer), 225);
		Utils.scheduleTask(Archer::shoot, 229);
		Utils.scheduleTask(() -> Actions.salvation(archer), 230);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 2), 231);
		Utils.scheduleTask(() -> Actions.leap(archer, Healer.get()), 232);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 5), 233);
		Utils.scheduleTask(() -> {
			Actions.swapItems(archer, 5, 32);
			Actions.swapItems(archer, 6, 33);
		}, 234);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -108.4f, -83.4f), 654);
		Utils.scheduleTask(() -> Actions.lastBreath(archer, 30), 654);
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(archer, 2);
			Actions.swapItems(archer, 5, 32);
		}, 684);
		Utils.scheduleTask(() -> Actions.leap(archer, Tank.get()), 685);
		Utils.scheduleTask(() -> Actions.leap(archer, Healer.get()), 728);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 0f, 0f);
			Actions.setHotbarSlot(archer, 1);
		}, 729);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0, 0, 1.12242), 5), 730);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 8f, 82f), 734);
		Utils.scheduleTask(() -> Actions.bonzo(archer, new Vector(0.2123, 0.5, 1.5107)), 735);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 8f, 0f), 736);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 0f, 0f), 747);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.7937, 0, 0.7937), 3), 748);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -6f, 82f), 750);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.1173, 0, 1.1163), 1), 751);
		Utils.scheduleTask(() -> Actions.bonzo(archer, new Vector(0.1595, 0.5, 1.5172)), 752);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -6f, 0f), 753);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -5.8f, 0f), 763);
		Utils.scheduleTask(() -> {
			Actions.jump(archer);
			Actions.move(archer, new Vector(0.1144, 0, 1.1166), 1);
			Actions.setHotbarSlot(archer, 5);
		}, 764);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.0286, 0, 0.279), 8), 765);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 24.6f, 64.9f), 780);
		Utils.scheduleTask(() -> Actions.stonk(archer, world.getBlockAt(96, 120, 121)), 781);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 90f, 0f), 782);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.8634, 0, 0), 1), 783);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 0f, 26.1f), 784);
		Utils.scheduleTask(() -> {
			Actions.stonk(archer, world.getBlockAt(96, 121, 122));
			Actions.move(archer, new Vector(0, 0, 1.12242), 5);
		}, 785);
		Utils.scheduleTask(() -> Actions.stonk(archer, world.getBlockAt(96, 120, 122)), 786);
		Utils.scheduleTask(() -> Actions.stonk(archer, world.getBlockAt(96, 121, 123)), 787);
		Utils.scheduleTask(() -> Actions.stonk(archer, world.getBlockAt(96, 120, 123)), 788);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0, 0, 0.2806), 11), 790);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 1), 791);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 90f, 0f), 800);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-1.12242, 0, 0), 6), 801);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -128f, -19f), 808);
		if(doContinue) {
			Utils.scheduleTask(Archer::explosiveShot, 887);
			Utils.scheduleTask(() -> {
				Actions.setHotbarSlot(archer, 1);
				Actions.turnHead(archer, 90f, 0f);
			}, 888);
			Utils.scheduleTask(() -> Actions.move(archer, new Vector(-1.403, 0, 0), 1), 889);
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
		Goldor.broadcastTerminalComplete(archer, "gate", 1, 3);
		Actions.move(archer, new Vector(-0.2806, 0, 0), 13);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 75f, 82f), 12);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-1.355, 0, 0.363), 1), 13);
		Utils.scheduleTask(() -> Actions.bonzo(archer, new Vector(-1.4735, 0.5, 0.3948)), 14);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 77.5f, 0f), 15);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 126.6f, 0f), 27);
		Utils.scheduleTask(() -> {
			Actions.jump(archer);
			Actions.move(archer, new Vector(-1.1264, 0, -0.837), 1);
		}, 28);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.2253, 0, -0.1673), 8), 29);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 90f, 0f), 37);
		// jump ends on tick 41

		/*
		 * ██████╗
		 * ╚════██╗
		 *  █████╔╝
		 * ██╔═══╝
		 * ███████╗
		 * ╚══════╝
		 */
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-1.403, 0, 0), 7), 54);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.2806, 0, 0), 10), 61);
		Utils.scheduleTask(() -> Actions.lavaJump(archer, false), 68);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 180f, 30f), 69);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0, 0, -0.2806), 10), 76);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0, 0.001, -1.403), 3), 89);
		Utils.scheduleTask(() -> {
			Actions.swingHand(archer);
			Bukkit.broadcastMessage(ChatColor.BLUE + "Party " + ChatColor.DARK_GRAY + "> " + ChatColor.GOLD + "[MVP" + ChatColor.DARK_BLUE + "++" + ChatColor.GOLD + "] akc0303" + ChatColor.WHITE + ": THIS TERMINAL IS BALDER THAN ME");
		}, 92);
		Utils.scheduleTask(() -> Bukkit.broadcastMessage(ChatColor.BLUE + "Party " + ChatColor.DARK_GRAY + "> " + ChatColor.GOLD + "[MVP" + ChatColor.DARK_BLUE + "++" + ChatColor.GOLD + "] akc0303" + ChatColor.WHITE + ": THIS TERMINAL IS BALDER THAN ME 1/4"), 93);
		Utils.scheduleTask(() -> Bukkit.broadcastMessage(ChatColor.BLUE + "Party " + ChatColor.DARK_GRAY + "> " + ChatColor.GOLD + "[MVP" + ChatColor.DARK_BLUE + "++" + ChatColor.GOLD + "] akc0303" + ChatColor.WHITE + ": THIS TERMINAL IS BALDER THAN ME 2/4"), 94);
		Utils.scheduleTask(() -> Bukkit.broadcastMessage(ChatColor.BLUE + "Party " + ChatColor.DARK_GRAY + "> " + ChatColor.GOLD + "[MVP" + ChatColor.DARK_BLUE + "++" + ChatColor.GOLD + "] akc0303" + ChatColor.WHITE + ": THIS TERMINAL IS BALDER THAN ME 3/4"), 95);
		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(archer, "terminal", 3, 8), 96);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -90f, 25f), 97);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.2806, 0, 0), 15), 98);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 7), 99);
		Utils.scheduleTask(() -> Actions.swingHand(archer), 100); // equip phoenix
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 2), 101);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(1.12242, 0, 0), 1), 113);
		Utils.scheduleTask(() -> Actions.swingHand(archer), 114);
		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(archer, "terminal", 6, 8), 115);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 2), 116);
		Utils.scheduleTask(() -> Actions.leap(archer, Berserk.get()), 117);

		/*
		 * ██████╗
		 * ╚════██╗
		 *  █████╔╝
		 *  ╚═══██╗
		 * ██████╔╝
		 * ╚═════╝
		 */
		Utils.scheduleTask(() -> Actions.turnHead(archer, -146.3f, 8.6f), 118);
		Utils.scheduleTask(() -> {
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Phoenix Procced!");
			world.playSound(archer.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1f, 1.6f);
		}, 120);
		Utils.scheduleTask(() -> Actions.lavaJump(archer, false), 131);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.1557, 0, -0.2335), 7), 132);
		Utils.scheduleTask(() -> {
			Actions.rightClickLever(archer);
			Goldor.broadcastTerminalComplete(archer, "lever", 1, 7);
		}, 140);
		Utils.scheduleTask(() -> {
			Actions.setHotbarSlot(archer, 3);
			Actions.turnHead(archer, 180f, 0f);
			Actions.clearVelocity(archer);
		}, 141);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0, 0, -0.2806), 10), 142);
		Utils.scheduleTask(() -> {
			Actions.swingHand(archer);
			Goldor.broadcastTerminalComplete(archer, "gate", 3, 3);
		}, 152);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 7), 153);
		Utils.scheduleTask(() -> Actions.swingHand(archer), 154); // equip black cat
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 1), 155);
		Utils.scheduleTask(() -> Actions.swapItems(archer, 9, 39), 156);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 85f, 82f), 162);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-1.3977, 0, 0.1223), 1), 163);
		final BukkitRunnable[] temp = new BukkitRunnable[1];
		Utils.scheduleTask(() -> temp[0] = Actions.bonzo(archer, new Vector(-1.5197, 0.5, 0.133)), 164);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 85f, 33f), 165);
		Utils.scheduleTask(() -> {
			Actions.rightClickLever(archer);
			Goldor.broadcastTerminalComplete(archer, "lever", 3, 7);
		}, 169);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 2), 170);
		Utils.scheduleTask(() -> {
			temp[0].cancel();
			Actions.leap(archer, Mage.get());
		}, 171);

		/*
		 * ██╗  ██╗
		 * ██║  ██║
		 * ███████║
		 * ╚════██║
		 *      ██║
		 *      ╚═╝
		 */
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -148f, 82f);
			Actions.setHotbarSlot(archer, 1);
		}, 172);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.7435, 0, -1.19), 1), 173);
		Utils.scheduleTask(() -> Actions.bonzo(archer, new Vector(0.8967, 0.5, -1.2342)), 174);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -148f, 0f), 175);
		Utils.scheduleTask(() -> {
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Bonzo Procced!");
			world.playSound(archer.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1f, 2f);
		}, 180);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.7435, 0, -1.19), 2), 188);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.1649, 0, -0.227), 6), 190);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 180f, 45f), 196);
		Utils.scheduleTask(() -> Actions.swingHand(archer), 197);
		Utils.scheduleTask(() -> Goldor.broadcastTerminalComplete(archer, "terminal", 2, 7), 198);
		Utils.scheduleTask(() -> world.playSound(archer.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1f), 200);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0, 0, 1.08), 1), 202);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -90f, 82f), 203);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(1.403, 0, 0), 1), 204);
		Utils.scheduleTask(() -> temp[0] = Actions.bonzo(archer, new Vector(1.52552, 0, 0)), 205);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -90f, 0f), 206);
		Utils.scheduleTask(() -> Actions.swapItems(archer, 9, 39), 207);
		Utils.scheduleTask(() -> {
			temp[0].cancel();
			Actions.lavaJump(archer, false);
		}, 219);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -76.5f, -4.9f);
			world.playSound(archer.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1f);
		}, 220);
		Utils.scheduleTask(() -> {
			Actions.rightClickLever(archer);
			Goldor.broadcastTerminalComplete(archer, "lever", 5, 7);
		}, 227);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 3), 228);
		Utils.scheduleTask(() -> Actions.leap(archer, Mage.get()), 229);
		

		/*
		 * ███████╗██╗ ██████╗ ██╗  ██╗████████╗
		 * ██╔════╝██║██╔════╝ ██║  ██║╚══██╔══╝
		 * █████╗  ██║██║  ███╗███████║   ██║
		 * ██╔══╝  ██║██║   ██║██╔══██║   ██║
		 * ██║     ██║╚██████╔╝██║  ██║   ██║
		 * ╚═╝     ╚═╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝
		 */
		Utils.scheduleTask(() -> Actions.swapItems(archer, 5, 32), 230);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 5), 231);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0, 0, -1.403), 11), 256);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -82.5f, -10f), 266);
		// tick 267: swap to gdrag
		Utils.scheduleTask(() -> Actions.lastBreath(archer, 10), 268);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 4), 279);
		Utils.scheduleTask(Archer::shoot, 280);
		Utils.scheduleTask(() -> Actions.salvation(archer), 284);
		Utils.scheduleTask(Archer::shoot, 285);
		Utils.scheduleTask(() -> Actions.salvation(archer), 289);
		Utils.scheduleTask(Archer::shoot, 290);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 2), 291);
		Utils.scheduleTask(() -> Actions.leap(archer, Healer.get()), 292);
		if(doContinue) {
			Utils.scheduleTask(() -> necron(true), 350);
		}
	}

	private static void necron(boolean doContinue) {
		Actions.setHotbarSlot(archer, 2);
		Utils.scheduleTask(() -> Actions.swapItems(archer, 3, 30), 1);
		Utils.scheduleTask(() -> Actions.leap(archer, Tank.get()), 121);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 5), 122);
		Utils.scheduleTask(() -> Actions.lastBreath(archer, 36), 123);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 4), 160);
		for(int i = 161; i < 368; i += 5) {
			Utils.scheduleTask(Archer::shoot, i);
			Utils.scheduleTask(() -> Actions.salvation(archer), i + 4);
		}
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 3), 368);
		Utils.scheduleTask(() -> Actions.swingHand(archer), 369);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 4), 410);
		for(int i = 411; i < 507; i += 5) {
			Utils.scheduleTask(Archer::shoot, i);
			Utils.scheduleTask(() -> Actions.salvation(archer), i + 4);
		}
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 2), 507);
		Utils.scheduleTask(() -> Actions.leap(archer, Healer.get()), 508);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 121.3f, 0f), 509);
		// tick 510: equip black cat
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-1.198, 0, -0.729), 20), 511);
		Utils.scheduleTask(() -> Actions.jump(archer), 530);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.2398, 0, -0.1458), 9), 531);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-1.198, 0, -0.729), 2), 540);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 65.6f, 29.3f), 541);
		Utils.scheduleTask(() -> {
			Actions.swapItems(archer, 5, 32);
			Actions.swapItems(archer, 11, 39);
		}, 542);
		if(doContinue) {
			Utils.scheduleTask(Archer::witherKing, 609);
		}
	}

	private static void witherKing() {
		Utils.scheduleTask(() -> WitherKing.pickUpRelic(archer), 1);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -115f, 0f), 2);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(1.2716, 0, -0.5929), 6), 3);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.2543, 0, -0.1186), 5), 9);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(1.2716, 0, -0.5929), 15), 14);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 146.1f, 0f), 28);
		Utils.scheduleTask(() -> WitherKing.placeRelic(archer), 29);
		// tick 30: equip greg
		Utils.scheduleTask(() -> Actions.swapItems(archer, 11, 39), 31);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 36f, 0f);
			Actions.setHotbarSlot(archer, 6);
		}, 32);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.6597, 0, 0.9081), 1), 33);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.1649, 0, 0.227), 5), 34);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.6597, 0, 0.9081), 31), 39);
		Utils.scheduleTask(() -> Actions.jump(archer), 69);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.1649, 0, 0.227), 9), 70);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.6597, 0, 0.9081), 4), 79);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -37.5f, -16f), 82);
		Utils.scheduleTask(() -> Actions.rag(archer), 169);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 4), 230);
		// arrows take 13 ticks to reach the Dragon
		// rag on tick 169
		// rag is activated on tick 229
		// start shooting tick 368
		// begin moving tick 388
		// Dragon spawns tick 401
		// last arrow fired tick 428
		// rag wears off tick 429
		// rag is back tick 569
		for(int i = 368; i < 430; i += 5) {
			Utils.scheduleTask(Archer::shoot, i);
			Utils.scheduleTask(() -> Actions.salvation(archer), i + 1);
		}
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.7033, 0, 0.8747), 27), 388);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -37.5f, -18.5f), 395);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -37.5f, -22f), 405);
		Utils.scheduleTask(() -> Actions.jump(archer), 414);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -37.5f, -28f);
			Actions.move(archer, new Vector(0.1758, 0, 0.2187), 9);
		}, 415);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.7033, 0, 0.8747), 6), 424);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, 177.6f, 0f);
			WitherKing.playDragonDeathSound(true);
			Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "Soul Dragon " + ChatColor.GREEN + "killed in 30 ticks (1.50 seconds) | Wither King: 431 ticks (21.55 seconds) | Overall: 3 706 ticks (185.30 seconds)");
		}, 433);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.047, 0, -1.121), 8), 434);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.0118, 0, -0.2804), 5), 442);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.047, 0, -1.121), 9), 447);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-0.0118, 0, -0.2804), 5), 456);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -134.6f, -17f), 461);
		// arrows take 13 ticks to reach the Dragon
		// start shooting tick 718
		// begin moving tick 728
		// last arrow fired tick 738
		// Dragon spawns tick 741
		for(int i = 718; i < 740; i += 5) {
			Utils.scheduleTask(Archer::shoot, i);
			Utils.scheduleTask(() -> Actions.salvation(archer), i + 1);
		}
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.7992, 0, -0.788), 10), 728);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -134.6f, -20f), 735);
		Utils.scheduleTask(() -> {
			Actions.turnHead(archer, -151.4f, 0f);
			Actions.setHotbarSlot(archer, 6);
		}, 739);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.5373, 0, -0.9855), 35), 740);
		Utils.scheduleTask(() -> Actions.rag(archer), 750);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 80.7f, -17f), 775);
		Utils.scheduleTask(() -> Actions.setHotbarSlot(archer, 4), 811);
		// arrows take 13 ticks to reach the Dragon
		// start shooting tick 814
		// begin moving tick 834
		// last arrow fired tick 844
		// Dragon spawns tick 847
		Utils.scheduleTask(Archer::rapidFire, 814);
		for(int i = 814; i < 850; i += 5) {
			Utils.scheduleTask(Archer::shoot, i);
			Utils.scheduleTask(() -> Actions.salvation(archer), i + 1);
		}
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-1.1077, 0, 0.1814), 10), 834);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 80.7f, -20f), 840);
		Utils.scheduleTask(() -> Actions.turnHead(archer, -18f, 0f), 851);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0.3469, 0, 1.0675), 23), 852);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 70.6f, -17f), 875);
		// arrows take 13 ticks to reach the Dragon
		// start shooting tick 920
		// begin moving tick 940
		// last arrow fired tick 950
		// Dragon spawns tick 953
		for(int i = 920; i < 955; i += 5) {
			Utils.scheduleTask(Archer::shoot, i);
			Utils.scheduleTask(() -> Actions.salvation(archer), i + 1);
		}
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(-1.0587, 0, 0.373), 10), 940);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 70.6f, -20f), 950);
	}

	private static void shoot() {
		Actions.rightClick(archer);
		Location l = archer.getEyeLocation();
		l.add(l.getDirection());
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
			Arrow arrow = world.spawnArrow(l, l.getDirection(), 4, 0);
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
			Arrow arrow = world.spawnArrow(l, l.getDirection(), 4, 0);
			arrow.setDamage(2.5 + finalAdd1);
			arrow.setPierceLevel(4);
			arrow.setShooter(archer);
			arrow.setWeapon(archer.getInventory().getItemInMainHand());
			arrow.addScoreboardTag("TerminatorArrow");
		}, 5);

		double finalAdd2 = add;
		Utils.scheduleTask(() -> {
			Arrow arrow = world.spawnArrow(l, l.getDirection(), 4, 0);
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
			Arrow arrow = world.spawnArrow(l, shootLoc.getDirection(), 4, 0.1F);
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

	private static void rapidFire() {
		for(int i = 0; i < 201; i += 4) {
			Utils.scheduleTask(() -> {
				Arrow arrow = world.spawnArrow(archer.getEyeLocation().add(archer.getEyeLocation()), archer.getEyeLocation().getDirection(), 4, 0);
				arrow.setDamage(15);
				arrow.setPierceLevel(4);
				arrow.setShooter(archer);
				arrow.setWeapon(archer.getInventory().getItemInMainHand());
				arrow.addScoreboardTag("TerminatorArrow");
			}, i);
		}
	}

	public static Player get() {
		return archer;
	}
}
