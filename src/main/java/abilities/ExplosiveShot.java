package abilities;

import abilities.ClassAbility;
import damage.DungeonClass;
import items.ItemUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import plugin.*;

import java.util.*;

/**
 * The Archer's regular drop ability: a spread of arrows whose impacts route through the shared Superboom
 * radius, so it opens crypts and cracked-brick walls the way the TNT does.
 */
public final class ExplosiveShot implements ClassAbility {
	public static final ExplosiveShot INSTANCE = new ExplosiveShot();

	private ExplosiveShot() {}

	@Override
	public DungeonClass owner() {
		return DungeonClass.ARCHER;
	}

	@Override
	public boolean ultimate() {
		return false;
	}

	@Override
	public int cooldownTicks() {
		return 400; // 20s
	}

	@Override
	public boolean cast(Player p) {
		explosiveShot(p);
		return true;
	}

	public static void explosiveShot(Player p) {
		ServerLevel nmsWorld = ((CraftWorld) p.getWorld()).getHandle();
		ServerPlayer nmsPlayer = ((CraftPlayer) p).getHandle();

		Vector baseDirection = p.getEyeLocation().getDirection().normalize();
		Vector leftDirection = baseDirection.clone().rotateAroundY(Math.toRadians(-7.5));
		Vector rightDirection = baseDirection.clone().rotateAroundY(Math.toRadians(7.5));

		Location l = p.getEyeLocation().add(0, -0.1, 0);
		float speed = 1.5f;
		List<LivingEntity> alreadyHurt = new ArrayList<>();
		Set<Block> visitedBlocks = new HashSet<>();
		for(Vector dir : List.of(leftDirection, baseDirection, rightDirection)) {
			net.minecraft.world.entity.projectile.arrow.Arrow nmsArrow = new net.minecraft.world.entity.projectile.arrow.Arrow(nmsWorld, 0, 0, 0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
			nmsArrow.setPos(l.getX(), l.getY(), l.getZ());
			nmsArrow.shoot(dir.getX(), dir.getY(), dir.getZ(), speed, 0);
			nmsArrow.setOwner(nmsPlayer);
			nmsWorld.addFreshEntity(nmsArrow);

			Arrow arrow = (Arrow) nmsArrow.getBukkitEntity();
			arrow.setDamage(0);
			arrow.setPierceLevel(1);
			arrow.setShooter(p);
			arrow.setWeapon(p.getInventory().getItemInMainHand());
			arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);

			new BukkitRunnable() {
				@Override
				public void run() {
					if(!arrow.isValid() || arrow.isDead() || arrow.isOnGround() || arrow.getLocation().getBlock().getType().isSolid()) {
						Location impact = arrow.getLocation();

						// Explosive Shot: each arrow deals 100% of the player's highest arrow damage in the last
						// minute (MAP.md §1.14), read off the shared rolling damage history.  Dealt as a
						// DERIVED instance: the figure is already a finished hit, so it gets no second pass through
						// the formula and never goes back into the history it came out of.
						double sbDamage = damage.CombatState.maxInLastTicks(p, 1200);
						for(Entity e : arrow.getNearbyEntities(4, 4, 4)) {
							if(e instanceof LivingEntity target && !alreadyHurt.contains(target) && !(e instanceof Player) && !(target.hasPotionEffect(PotionEffectType.RESISTANCE) && target.getPotionEffect(PotionEffectType.RESISTANCE).getAmplifier() == 255) && !(e instanceof Wither wither && wither.getInvulnerableTicks() != 0)) {
								damage.Damage.dealDerived(target, sbDamage, damage.DamageKind.NORMAL, p,
										damage.DamagePath.BOW);
								alreadyHurt.add(target);
							}
						}

						// Visual effects
						p.getWorld().spawnParticle(Particle.EXPLOSION, impact, 10, 0.5, 0.5, 0.5, 0);
						p.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1, 1f);

						ItemUtils.triggerSuperboomRadius(impact, p, visitedBlocks);

						arrow.remove();
						cancel();
					}
				}
			}.runTaskTimer(M7tas.getInstance(), 1L, 1L);
		}
	}}
