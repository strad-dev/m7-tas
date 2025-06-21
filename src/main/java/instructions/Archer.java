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
	@SuppressWarnings("FieldCanBeLocal")
	private static World world;

	public static void archerInstructions(Player p) {
		archer = p;
		world = archer.getWorld();
		archer.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, -1, 1));
		Actions.simulateAOTV(archer, new Location(world, -118.5, 70, -202.5));
		Actions.setFakePlayerHotbarSlot(archer, 4);
		Utils.scheduleTask(() -> Actions.move(archer, new Vector(0, 0, 1.12242), 5), 160);
		Utils.scheduleTask(Archer::clear, 162);
	}

	public static void clear() {

	}

	private static void simulateShoot() {
		Actions.simulateRightClickAir(archer);

		// Duplex Arrow
		Utils.scheduleTask(() -> {
			Location l = archer.getLocation();
			l.add(l.getDirection());
			l.setY(l.getY() + 1.62);

			double powerBonus;
			try {
				int power = archer.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.POWER);
				powerBonus = power * 0.05;
				if(power == 7) {
					powerBonus += 0.05;
				}
			} catch(Exception exception) {
				powerBonus = 0;
			}

			double strengthBonus;
			try {
				strengthBonus = 0.15 + 0.15 * Objects.requireNonNull(archer.getPotionEffect(PotionEffectType.STRENGTH)).getAmplifier();
			} catch(Exception exception) {
				strengthBonus = 0;
			}

			double add = powerBonus + strengthBonus;
			Arrow arrow = world.spawnArrow(l, l.getDirection(), 3, 0.1F);
			arrow.setDamage(0.5 + add);
			arrow.setPierceLevel(4);
			arrow.setShooter(archer);
			arrow.setWeapon(archer.getInventory().getItemInMainHand());
			arrow.addScoreboardTag("TerminatorArrow");
		}, 3);

		PotionEffect strength = archer.getPotionEffect(PotionEffectType.STRENGTH);
		if(strength == null) {
			archer.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 0));
		} else {
			if(strength.getAmplifier() < 5) {
				archer.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, strength.getAmplifier() + 1));
			} else {
				archer.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 5));
			}
		}
	}

	public static Player getArcher() {
		return archer;
	}
}
