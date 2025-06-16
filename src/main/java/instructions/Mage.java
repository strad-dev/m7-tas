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

	public static void mageInstructions(Player p) {
		mage = p;
		world = mage.getWorld();
		Actions.turnHead(mage, -180f, 0f);
		Actions.simulateAOTV(mage, new Location(world, -132.5, 69, -76.5));
		Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(p, 2, 29), 60);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(p, 2), 61);
		Utils.scheduleTask(() -> Actions.simulateRightClickAir(p), 101);
		Utils.scheduleTask(() -> Actions.move(p, new Vector(0, 0, 0.8634), 4), 102);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(p, 4), 121);
		Utils.scheduleTask(() -> p.teleport(new Location(p.getWorld(), -120.5, 75, -220.5)), 141);
		Utils.scheduleTask(() -> p.teleport(Archer.getArcher()), 162);
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
