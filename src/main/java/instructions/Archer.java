package instructions;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import plugin.Utils;

import java.util.Objects;

public class Archer {
	private static Player archer;
	private static World world;

	public static void archerInstructions(Player p) {
		archer = p;
		world = archer.getWorld();
		archer.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, -1, 1));
		Objects.requireNonNull(archer.getInventory().getItem(4)).addUnsafeEnchantment(Enchantment.POWER, 7);
		Actions.simulateAOTV(archer, new Location(world, -118.5, 70, -202.5));
		Actions.setFakePlayerHotbarSlot(archer, 1);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0, 0, 1.12242), 5), 160);
		Utils.scheduleTask(Archer::clear, 162);
	}

	public static void clear() {
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
				Actions.move(archer, new Vector(0, 0, 1.11242), 2);
		}, 33);
		Utils.scheduleTask(() -> {
			Actions.setFakePlayerHotbarSlot(archer, 1);
			Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "Archer: Deathmite Cleared");
		}, 34);
		Utils.scheduleTask(() -> Actions.turnHead(archer, 90f, 0f), 35);
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
		}, 41);

		// pearl 1: /execute in minecraft:overworld run tp @s -149.50 88.00 -119.50 120 0
		// lands -164.153 87 -128.022 in 13 ticks

		// pearl 2: /execute in minecraft:overworld run tp @s -143.50 90.00 -119.50 106 66
		// lands -151.559 69 -121.912 in 16 ticks

		// pearl 3: /execute in minecraft:overworld run tp @s -181.50 70.00 -120.50 82 11
		// lands -187.230 70 -119.704 in 5 ticks

		/*
		 * ████████╗ ██████╗ ███╗   ███╗██╗ ██████╗ ██╗  ██╗ █████╗
		 * ╚══██╔══╝██╔═══██╗████╗ ████║██║██╔═══██╗██║ ██╔╝██╔══██╗
		 *    ██║   ██║   ██║██╔████╔██║██║██║   ██║█████╔╝ ███████║
		 *    ██║   ██║   ██║██║╚██╔╝██║██║██║   ██║██╔═██╗ ██╔══██║
		 *    ██║   ╚██████╔╝██║ ╚═╝ ██║██║╚██████╔╝██║  ██╗██║  ██║
		 *    ╚═╝    ╚═════╝ ╚═╝     ╚═╝╚═╝ ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝
		 */

		/*
		 *  ██████╗ ██████╗  █████╗ ██╗   ██╗███████╗██╗
		 * ██╔════╝ ██╔══██╗██╔══██╗██║   ██║██╔════╝██║
		 * ██║  ███╗██████╔╝███████║██║   ██║█████╗  ██║
		 * ██║   ██║██╔══██╗██╔══██║╚██╗ ██╔╝██╔══╝  ██║
		 * ╚██████╔╝██║  ██║██║  ██║ ╚████╔╝ ███████╗███████╗
		 *  ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝  ╚═══╝  ╚══════╝╚══════╝
		 */

		// pearl 1: /execute in minecraft:overworld run tp @s -149.50 73.00 -100.50 50 29
		// lands -155.261 69 -95.686 in 7 ticks

		// pearl 2: /execute in minecraft:overworld run tp @s -155.26 69.00 -95.69 -7 0
		// lands -153.855 69 -83.063 in 10 ticks

		/*
		 * ███╗   ███╗██╗   ██╗███████╗███████╗██╗   ██╗███╗   ███╗
		 * ████╗ ████║██║   ██║██╔════╝██╔════╝██║   ██║████╗ ████║
		 * ██╔████╔██║██║   ██║███████╗█████╗  ██║   ██║██╔████╔██║
		 * ██║╚██╔╝██║██║   ██║╚════██║██╔══╝  ██║   ██║██║╚██╔╝██║
		 * ██║ ╚═╝ ██║╚██████╔╝███████║███████╗╚██████╔╝██║ ╚═╝ ██║
		 * ╚═╝     ╚═╝ ╚═════╝ ╚══════╝╚══════╝ ╚═════╝ ╚═╝     ╚═╝
		 */

		/*
		 * ███╗   ███╗ █████╗ ██████╗ ██╗  ██╗███████╗████████╗
		 * ████╗ ████║██╔══██╗██╔══██╗██║ ██╔╝██╔════╝╚══██╔══╝
		 * ██╔████╔██║███████║██████╔╝█████╔╝ █████╗     ██║
		 * ██║╚██╔╝██║██╔══██║██╔══██╗██╔═██╗ ██╔══╝     ██║
		 * ██║ ╚═╝ ██║██║  ██║██║  ██║██║  ██╗███████╗   ██║
		 * ╚═╝     ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝   ╚═╝
		 */

		// pearl 1: /execute in minecraft:overworld run tp @s -175.50 69.00 -58.50 80 1
		// lands -186.655 69 -56.488 in 9 ticks

		/*
		 * ██╗   ██╗███████╗██╗     ██╗      ██████╗ ██╗    ██╗
		 * ╚██╗ ██╔╝██╔════╝██║     ██║     ██╔═══██╗██║    ██║
		 *  ╚████╔╝ █████╗  ██║     ██║     ██║   ██║██║ █╗ ██║
		 *   ╚██╔╝  ██╔══╝  ██║     ██║     ██║   ██║██║███╗██║
		 *    ██║   ███████╗███████╗███████╗╚██████╔╝╚███╔███╔╝
		 *    ╚═╝   ╚══════╝╚══════╝╚══════╝ ╚═════╝  ╚══╝╚══╝
		 */
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

	public static Player getArcher() {
		return archer;
	}
}
