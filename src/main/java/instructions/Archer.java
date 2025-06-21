package instructions;

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
		// look at 9.3 yaw after clear

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
