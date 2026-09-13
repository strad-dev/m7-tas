package abilities;

import damage.DungeonClass;
import items.ItemUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import plugin.M7tas;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Archer's regular drop ability: a spread of three arrows whose impacts route through the shared Superboom
 * radius, so it opens crypts and cracked-brick walls the way the TNT does.
 * <p>
 * <b>Each arrow detonates on the first thing it touches</b>, a mob or a block, and does <b>not</b> pierce.  The
 * blast is the damage - an arrow that flew through its target and carried on was dropping its explosion several
 * blocks past whatever it was aimed at.
 * <p>
 * The "hit N enemies" line is printed <b>once, when all three arrows are spent</b>, not once per arrow: the three
 * share one {@code alreadyHurt} list, so they are one ability landing one total, and three lines for one drop press
 * would be noise.
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

	/** How close a mob has to be to an arrow to stop it.  Matches the guided carriers' own contact range. */
	private static final double HIT_RANGE = 1;

	/** Every mob within this of the impact takes the shot's full damage. */
	private static final double BLAST_RADIUS = 4;

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
		// Shared across the three arrows, so the summary line is the ability's total rather than one arrow's.
		// int[] rather than fields because they are written from three separate runnables.
		int[] damaged = {0};
		double[] dealt = {0};
		List<Vector> directions = List.of(leftDirection, baseDirection, rightDirection);
		// DERIVED from the list rather than a constant, so a fourth arrow can never leave the summary line waiting
		// on one that was never fired.
		int[] pending = {directions.size()};
		for(Vector dir : directions) {
			net.minecraft.world.entity.projectile.arrow.Arrow nmsArrow = new net.minecraft.world.entity.projectile.arrow.Arrow(nmsWorld, 0, 0, 0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
			nmsArrow.setPos(l.getX(), l.getY(), l.getZ());
			nmsArrow.shoot(dir.getX(), dir.getY(), dir.getZ(), speed, 0);
			nmsArrow.setOwner(nmsPlayer);
			nmsWorld.addFreshEntity(nmsArrow);

			Arrow arrow = (Arrow) nmsArrow.getBukkitEntity();
			arrow.setDamage(0);
			// NO PIERCE: the arrow stops at the first mob and detonates there.  The blast IS the damage, so an
			// arrow that passed through its target put its explosion several blocks past whatever was aimed at.
			arrow.setPierceLevel(0);
			arrow.setShooter(p);
			arrow.setWeapon(p.getInventory().getItemInMainHand());
			arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);

			new BukkitRunnable() {
				@Override
				public void run() {
					// The arrow is spent the moment it touches ANYTHING: a mob (found the same way a guided
					// carrier finds one, so the two agree on what counts as a target), the ground, or a solid
					// block.  The mob check is ours rather than vanilla's, because the arrow-hit listeners cancel
					// ProjectileHitEvent for a wither, which would otherwise leave it flying on.
					LivingEntity struck = ItemUtils.firstMobNear(arrow.getLocation(), HIT_RANGE);
					if(struck == null && arrow.isValid() && !arrow.isDead() && !arrow.isOnGround()
							&& !arrow.getLocation().getBlock().getType().isSolid()) {
						return;
					}
					Location impact = struck != null
							? struck.getLocation().add(0, struck.getHeight() / 2.0, 0)
							: arrow.getLocation();

					// Explosive Shot: each arrow deals 100% of the player's highest arrow damage in the last
					// minute (MAP.md §1.14), read off the shared rolling damage history.  Dealt as a
					// DERIVED instance: the figure is already a finished hit, so it gets no second pass through
					// the formula and never goes back into the history it came out of.
					double sbDamage = damage.CombatState.maxInLastTicks(p, 1200);
					for(Entity e : impact.getWorld().getNearbyEntities(impact, BLAST_RADIUS, BLAST_RADIUS, BLAST_RADIUS)) {
						if(e instanceof LivingEntity target && !alreadyHurt.contains(target) && !(e instanceof Player) && !(target.hasPotionEffect(PotionEffectType.RESISTANCE) && target.getPotionEffect(PotionEffectType.RESISTANCE).getAmplifier() == 255) && !(e instanceof Wither wither && wither.getInvulnerableTicks() != 0)) {
							double reported = damage.Damage.dealDerived(target, sbDamage, damage.DamageKind.NORMAL, p,
									damage.DamagePath.BOW);
							alreadyHurt.add(target);
							if(reported > 0) {
								dealt[0] += reported;
								damaged[0]++;
							}
						}
					}

					// Visual effects
					p.getWorld().spawnParticle(Particle.EXPLOSION, impact, 10, 0.5, 0.5, 0.5, 0);
					p.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1, 1f);

					ItemUtils.triggerSuperboomRadius(impact, p, visitedBlocks);

					arrow.remove();
					cancel();
					// The last arrow to land owns the summary line.
					if(--pending[0] == 0) damage.Damage.reportAoe(p, "Explosive Shot", damaged[0], dealt[0]);
				}
			}.runTaskTimer(M7tas.getInstance(), 1L, 1L);
		}
	}}
