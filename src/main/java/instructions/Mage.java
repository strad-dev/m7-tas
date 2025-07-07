package instructions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;
import plugin.Utils;

import java.util.ArrayList;

public class Mage {
	private static Player mage;
	private static World world;

	public static void mageInstructions(Player p, String section) {
		mage = p;
		world = Mage.mage.getWorld();

		if(section.equals("all") || section.equals("clear")) {
			Actions.turnHead(Mage.mage, -180f, 0f);
			Actions.simulateAOTV(Mage.mage, new Location(world, -132.5, 69, -76.5));
			Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(mage, 2, 29), 60);
			Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 2), 61);
			Utils.scheduleTask(() -> Actions.simulateRightClickAir(mage), 101);
			Utils.scheduleTask(() -> Actions.move(mage, new Vector(0, 0, 0.8634), 5), 102);
			Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(mage, 4), 121);
			Utils.scheduleTask(() -> {
				mage.teleport(new Location(mage.getWorld(), -120.5, 75, -220.5));
				Actions.swapFakePlayerInventorySlots(mage, 2, 29);
			}, 141);
			// Tick 160 (clear tick 0: run begins)
			// Tick 161 (clear tick 1: teleport back) - watcher sequence begins
			Utils.scheduleTask(() -> clear(section.equals("all")), 162);
		}
	}

	private static void clear(boolean doContinue) {
		/*
		 * ██████╗ ██╗      ██████╗  ██████╗ ██████╗     ██████╗ ██╗   ██╗███████╗██╗  ██╗
		 * ██╔══██╗██║     ██╔═══██╗██╔═══██╗██╔══██╗    ██╔══██╗██║   ██║██╔════╝██║  ██║
		 * ██████╔╝██║     ██║   ██║██║   ██║██║  ██║    ██████╔╝██║   ██║███████╗███████║
		 * ██╔══██╗██║     ██║   ██║██║   ██║██║  ██║    ██╔══██╗██║   ██║╚════██║██╔══██║
		 * ██████╔╝███████╗╚██████╔╝╚██████╔╝██████╔╝    ██║  ██║╚██████╔╝███████║██║  ██║
		 * ╚═════╝ ╚══════╝ ╚═════╝  ╚═════╝ ╚═════╝     ╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚═╝  ╚═╝
		 */
		// Tick 162 (clear tick 5, delay = 3)
		Utils.scheduleTask(() -> Actions.simulateLeap(mage, Archer.getArcher()), 3);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, 3.4f, -3.4f);
			Actions.setFakePlayerHotbarSlot(mage, 1);
		}, 4);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(mage, new Location(world, -120.5, 74, -154.5)), 5);
		Utils.scheduleTask(() -> Actions.turnHead(mage, 0f, 13.4f), 6);
		Utils.scheduleTask(() -> Actions.simulateAOTV(mage, new Location(world, -120.5, 71.21667, -142.83)), 7);
		Utils.scheduleTask(() -> Actions.simulateAOTV(mage, new Location(world, -120.5, 71, -138.5)), 8);
		Utils.scheduleTask(() -> {
			Actions.turnHead(mage, -6.9f, 8.6f);
			Actions.simulateLeftClickAir(mage);
			Server.openWitherDoor();
		}, 9);

		// pearl 1: /execute in minecraft:overworld run tp @s -119.50 69.00 -127.50 -31 15
		// lands -114.535 67 -119.218 in 8 ticks

		// pearl 2: /execute in minecraft:overworld run tp @s -114.54 67.00 -119.22 -124 -78
		// lands -112.3 78.2 -120.7 in 10 ticks

		// pearl 3: /execute in minecraft:overworld run tp @s -122.50 82.00 -129.50 -90.00 4.00
		// lands -113.923 82 -129.565 in 7 ticks
	}

	private static void simulateBeam() {
		Actions.simulateLeftClickAir(mage);

		Location l = mage.getLocation();
		l.add(0, 1.62, 0);
		Vector v = l.getDirection();
		v.setX(v.getX() / 10);
		v.setY(v.getY() / 10);
		v.setZ(v.getZ() / 10);
		for(int i = 0; i < 150; i++) {
			if(l.getBlock().getType().isSolid()) {
				break;
			}
			ArrayList<Entity> entities = (ArrayList<Entity>) world.getNearbyEntities(l, 1, 1, 1);
			for(Entity entity : entities) {
				if(entity instanceof LivingEntity temp && !temp.equals(mage)) {
					double damage = mage.getScoreboardTags().contains("RagBuff") ?
							(temp instanceof Wither ? 75 : 55) :
							(temp instanceof Wither ? 60 : 45);
					Bukkit.getPluginManager().callEvent(new EntityDamageByEntityEvent(mage, temp, EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK, DamageSource.builder(DamageType.PLAYER_ATTACK).build(), damage));
					break;
				}
			}
			world.spawnParticle(Particle.FIREWORK, l, 1);
			l.add(v);
		}
	}

	@SuppressWarnings("unused")
	public static Player getMage() {
		return mage;
	}
}
