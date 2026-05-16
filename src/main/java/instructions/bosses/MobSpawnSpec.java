package instructions.bosses;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * Specification for a group of mobs that all share configuration but spawn at
 * uniformly-random locations within a bounding box. Storm uses 16 of these for
 * its various Wither Miner / Sentry / Shadow Assassin clusters; Maxor could
 * also use this for his 10 center miners.
 */
public record MobSpawnSpec(
		String groupName,
		EntityType type,
		int count,
		Function<Random, Vector> locationProvider,
		double maxHealth,
		double armor,
		double armorToughness,
		String customName,
		ItemStack mainHand,
		boolean aiEnabled,
		boolean aggressive,
		boolean adult,
		boolean silent,
		boolean persistent,
		Location facingTarget,
		List<PotionEffect> potionEffects,
		List<ItemStack> armorPieces,  // helmet, chestplate, leggings, boots — order matters; null entries skipped
		int startTick,
		Sound spawnSound,              // played once at first mob's location when the group spawns; null = silent
		float spawnSoundPitch
) {

	/**
	 * Builds a uniform-random location provider over the given inclusive AABB.
	 * Y is integer-quantized (matches the existing in-game behavior where mob spawns
	 * use the box's Y coordinate directly, not a random Y within the box).
	 */
	public static Function<Random, Vector> uniformIn(BoundingBox box) {
		double x1 = box.getMinX(), x2 = box.getMaxX();
		double y1 = box.getMinY(), y2 = box.getMaxY();
		double z1 = box.getMinZ(), z2 = box.getMaxZ();
		return rng -> {
			double x = x1 + rng.nextDouble() * (x2 - x1);
			double y = y1 + rng.nextDouble() * (y2 - y1);
			double z = z1 + rng.nextDouble() * (z2 - z1);
			return new Vector(x, y, z);
		};
	}

	/** Builds a uniform-random provider over a 3D inclusive AABB given by two corner coords. */
	public static Function<Random, Vector> uniformIn(int x1, int y1, int z1, int x2, int y2, int z2) {
		int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
		int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
		int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
		return rng -> new Vector(
				minX + rng.nextDouble() * (maxX - minX),
				minY + rng.nextDouble() * (maxY - minY),
				minZ + rng.nextDouble() * (maxZ - minZ)
		);
	}

	/** Always returns the given fixed location (for one-off spawns like the Shadow Assassin corners). */
	public static Function<Random, Vector> fixed(double x, double y, double z) {
		Vector v = new Vector(x, y, z);
		return rng -> v.clone();
	}
}
