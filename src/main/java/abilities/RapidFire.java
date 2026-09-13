package abilities;

import damage.DungeonClass;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import plugin.Utils;

/**
 * The Archer's ULTIMATE: fifty arrows over 200 ticks, one every four.
 * <p>
 * Each arrow deals 75%% of the player's highest arrow damage in the last minute (§1.14), read off the same
 * rolling history the axe throw and Explosive Shot use.  That is 75%% of a FINISHED hit, so it is stamped
 * DERIVED - it lands for that figure exactly and stays out of the history.  Both halves matter: each of the
 * fifty arrows re-queries the history four ticks after the last one landed, so anything that let an arrow
 * inflate what the next one reads compounds fifty times and overflows.
 */
public final class RapidFire implements ClassAbility {
	public static final RapidFire INSTANCE = new RapidFire();

	private RapidFire() {}

	@Override
	public DungeonClass owner() {
		return DungeonClass.ARCHER;
	}

	@Override
	public boolean ultimate() {
		return true;
	}

	@Override
	public int cooldownTicks() {
		return 2000; // 100s
	}

	@Override
	public boolean cast(Player p) {
		rapidFire(p);
		return true;
	}

	public static void rapidFire(Player p) {
		for(int i = 0; i < 200; i += 4) {
			Utils.scheduleTask(() -> {
				ServerLevel nmsWorld = ((CraftWorld) p.getWorld()).getHandle();
				ServerPlayer nmsPlayer = ((CraftPlayer) p).getHandle();

				Location eyeLoc = p.getEyeLocation();
				Vector dir = eyeLoc.getDirection().normalize();
				Location spawnLoc = eyeLoc.add(0, -0.1, 0);
				double speed = 2;

				net.minecraft.world.entity.projectile.arrow.Arrow nmsArrow = new net.minecraft.world.entity.projectile.arrow.Arrow(nmsWorld, 0, 0, 0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
				nmsArrow.setPos(spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ());
				nmsArrow.shoot(dir.getX(), dir.getY(), dir.getZ(), (float) speed, 0);
				nmsArrow.setOwner(nmsPlayer);
				nmsWorld.addFreshEntity(nmsArrow);

				Arrow arrow = (Arrow) nmsArrow.getBukkitEntity();
				arrow.setPierceLevel(4);
				arrow.setShooter(p);
				arrow.setWeapon(p.getInventory().getItemInMainHand());
				arrow.addScoreboardTag("TerminatorArrow");
				arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
				// Rapid Fire: each arrow deals 75% of the player's highest arrow damage in the last minute
				// (MAP.md §1.14), off the same rolling history Explosive Shot and the axe throw read.
				// 75% of a FINISHED hit, so stampFlat marks the arrow derived: it lands for this figure exactly and
				// stays out of the history.  Both matter - each of the 50 arrows re-queries the history four ticks
				// after the last one landed, so anything that let an arrow inflate what the next one reads compounds
				// fifty times and overflows.
				damage.Arrows.stampFlat(arrow, p, p.getInventory().getItemInMainHand(),
						damage.CombatState.maxInLastTicks(p, 1200) * 0.75);
			}, i);
		}
	}}
