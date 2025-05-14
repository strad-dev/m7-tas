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
		// Begin Tic Tac Toe clear
		Utils.scheduleTask(() -> {
			Actions.setFakePlayerHotbarSlot(berserk, 1);
			Actions.turnHead(berserk, 90, 37.5f);
		}, 0);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -24.5, 69, -197.5)), 4);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -176, 2.9f), 8);
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
			Actions.turnHead(berserk, 3.1f, 1.9f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 36);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -24.5f, 69, -184.5f)), 40);

		// Chambers  
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -60.4f, 28), 44);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -20.5, 69, -182.5f)), 48);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -138.5f, 90), 52);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -20.5, 59, -182.5f)), 56);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -134.2f, 51.1f), 60);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 1/5 (Opened Chest)");
			berserk.playSound(berserk, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 64);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0, -90), 68);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -20.5, 72, -182.5f)), 72);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 80.7f, -68.3f), 76);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -23.5f, 82, -181.5f)), 80);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 57.5f, 7.6f), 84);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -31.5f, 82, -176.5f)), 88);
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
			Actions.turnHead(berserk, 163.5f, 7.9f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 100);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -38.5f, 80, -183.5f)), 104);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 109.5f, 4.1f), 108);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -55.5f, 80, -189.5f)), 112);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, -143.8f, 2.1f), 116);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -45.5f, 81, -203.5f)), 120);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 174.1f, 7.4f), 124);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -44.5f, 81, -213.5f)), 128);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 146.5f, -2.2f);
			Actions.setFakePlayerHotbarSlot(berserk, 3);
		}, 132);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(berserk, -47, 81, -217, -46, 84, -216), 136);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 1), 140);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -54.5f, 83.5, -228.5f)), 144);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 116.2f, 38.9f), 148);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 3/5 (Opened Chest)");
			berserk.playSound(berserk, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 152);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 0.7f, -5.8f), 156);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -54.5f, 86, -219.5f)), 160);
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
			Actions.turnHead(berserk, 14.8f, 44f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 176);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -59.5f, 69, -198.5f)), 180);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 33.5f, 17.3f), 184);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -62.5f, 69, -194.5f)), 188);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 5), 192);
		Utils.scheduleTask(() -> Actions.simulateStonking(berserk, berserk.getWorld().getBlockAt(-64, 70, -194)), 196);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 4/5 (Opened Chest)");
			berserk.playSound(berserk, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 200);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -90f, 11.7f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 204);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -56.5f, 69, -194.5f)), 208);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 9.3f, 3.8f), 212);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -58.5f, 69, -176.5f)), 216);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 74.9f, 8.4f);
			Actions.setFakePlayerHotbarSlot(berserk, 3);
		}, 220);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(berserk, -61, 69, -177, -62, 73, -174), 224);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(berserk, 1), 228);
		Utils.scheduleTask(() -> Actions.simulateAOTV(berserk, new Location(berserk.getWorld(), -66.5f, 69f, -174.5f)), 232);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(berserk);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 5/5 (Opened Chest)");
			berserk.playSound(berserk, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 236);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, -99.7f, -3.4f);
			Actions.setFakePlayerHotbarSlot(berserk, 1);
		}, 240);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -54.5f, 71.5, -176.5f)), 244);
		Utils.scheduleTask(() -> Actions.turnHead(berserk, 165.2f, 5.9f), 248);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(berserk, new Location(berserk.getWorld(), -64.5f, 69, -215.5)), 252);
		Utils.scheduleTask(() -> {
			Actions.turnHead(berserk, 90f, 0f);
			Actions.setFakePlayerHotbarSlot(berserk, 4);
		}, 256);
		Utils.scheduleTask(() -> {
			Actions.move(berserk, new Vector(-0.2806, 0, 0), 80);
			simulateShoot();
		}, 260);
		Utils.scheduleTask(Berserk::simulateShoot, 280);
		Utils.scheduleTask(Berserk::simulateShoot, 300);
		Utils.scheduleTask(() -> Actions.simulateLeftClickAir(berserk), 304);
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
				strengthBonus = 0.15 * Objects.requireNonNull(berserk.getPotionEffect(PotionEffectType.STRENGTH)).getAmplifier();
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