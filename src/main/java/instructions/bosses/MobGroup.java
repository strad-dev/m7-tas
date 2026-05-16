package instructions.bosses;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftWitherSkeleton;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import plugin.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A scheduled group of mobs spawned from a {@link MobSpawnSpec}. Spawning is
 * scheduled at {@link MobSpawnSpec#startTick()} ticks from {@link #spawn(World, Random)}.
 * {@link #cleanup()} removes every entity the group has spawned.
 */
public final class MobGroup {
	private final MobSpawnSpec spec;
	private final List<LivingEntity> spawned = new ArrayList<>();

	public MobGroup(MobSpawnSpec spec) {
		this.spec = spec;
	}

	/** Schedules the spawn at {@code spec.startTick()} ticks from now. */
	public void spawn(World world, Random rng) {
		if(spec.startTick() <= 0) {
			doSpawn(world, rng);
		} else {
			Utils.scheduleTask(() -> doSpawn(world, rng), spec.startTick());
		}
	}

	private void doSpawn(World world, Random rng) {
		Location firstSpawnLoc = null;
		for(int i = 0; i < spec.count(); i++) {
			Vector pos = spec.locationProvider().apply(rng);
			Location spawnLoc = new Location(world, pos.getX(), pos.getY(), pos.getZ());
			if(firstSpawnLoc == null) firstSpawnLoc = spawnLoc;

			LivingEntity mob = (LivingEntity) world.spawnEntity(spawnLoc, spec.type());

			mob.getAttribute(Attribute.MAX_HEALTH).setBaseValue(spec.maxHealth());
			mob.setHealth(spec.maxHealth());
			if(mob.getAttribute(Attribute.ARMOR) != null) {
				mob.getAttribute(Attribute.ARMOR).setBaseValue(spec.armor());
			}
			if(mob.getAttribute(Attribute.ARMOR_TOUGHNESS) != null) {
				mob.getAttribute(Attribute.ARMOR_TOUGHNESS).setBaseValue(spec.armorToughness());
			}
			mob.setAI(spec.aiEnabled());
			mob.setSilent(spec.silent());
			mob.setPersistent(spec.persistent());
			mob.setRemoveWhenFarAway(false);
			mob.setCustomName(spec.customName());
			mob.setCustomNameVisible(true);

			if(spec.adult() && mob instanceof Zombie zombie) {
				zombie.setAdult();
			}

			EntityEquipment eq = mob.getEquipment();
			if(eq != null) {
				if(spec.mainHand() != null) eq.setItemInMainHand(spec.mainHand());
				List<ItemStack> armorPieces = spec.armorPieces();
				if(armorPieces != null) {
					if(armorPieces.size() > 0 && armorPieces.get(0) != null) eq.setHelmet(armorPieces.get(0));
					if(armorPieces.size() > 1 && armorPieces.get(1) != null) eq.setChestplate(armorPieces.get(1));
					if(armorPieces.size() > 2 && armorPieces.get(2) != null) eq.setLeggings(armorPieces.get(2));
					if(armorPieces.size() > 3 && armorPieces.get(3) != null) eq.setBoots(armorPieces.get(3));
				}
			}

			if(spec.potionEffects() != null) {
				for(PotionEffect effect : spec.potionEffects()) {
					mob.addPotionEffect(effect);
				}
			}

			// Face the target if requested — used to point miners/sentries at the room center.
			if(spec.facingTarget() != null) {
				Location target = spec.facingTarget();
				Vector direction = target.toVector().subtract(spawnLoc.toVector()).normalize();
				float yaw = (float) (Math.atan2(-direction.getX(), direction.getZ()) * 180.0 / Math.PI);
				float pitch = (float) (Math.asin(-direction.getY()) * 180.0 / Math.PI);
				Location facingLoc = spawnLoc.clone();
				facingLoc.setYaw(yaw);
				facingLoc.setPitch(pitch);
				mob.teleport(facingLoc);
			}

			// Raised-arms pose for Wither Skeletons (only effect of setAggressive on a non-AI mob).
			if(spec.aggressive() && mob instanceof WitherSkeleton ws) {
				net.minecraft.world.entity.monster.skeleton.WitherSkeleton nmsWs =
						(net.minecraft.world.entity.monster.skeleton.WitherSkeleton) ((CraftWitherSkeleton) ws).getHandle();
				nmsWs.setAggressive(true);
			}

			spawned.add(mob);
		}

		// One sound per group, at the first mob's spawn location.
		if(spec.spawnSound() != null && firstSpawnLoc != null) {
			world.playSound(firstSpawnLoc, spec.spawnSound(), 1.0f, spec.spawnSoundPitch());
		}
	}

	public void cleanup() {
		for(LivingEntity mob : spawned) {
			if(mob != null && mob.isValid()) mob.remove();
		}
		spawned.clear();
	}

	public List<LivingEntity> getSpawned() {
		return spawned;
	}
}
