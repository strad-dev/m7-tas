package instructions.clear;

import org.bukkit.Location;
import org.bukkit.World;
import plugin.Utils;

import java.util.UUID;

/**
 * A single interactable secret (or reward chest). One instance per interactable, built once in {@link Rooms}.
 *
 * <p>Coordinates are stored as given in the spec: chests / essence use integer block coords; items / bats use
 * the decimal spawn coords. {@link #counted} is {@code false} for things that are NOT part of the 47 scored
 * secrets (the two Ice-Fill reward chests) so they can share the chest-open path without inflating the count.
 */
public class Secret {
	/** How this secret is completed / what sound it plays (reuses the existing enum). */
	public final Utils.SecretType type;
	public final double x, y, z;
	/** Blessing contained in this chest, or {@code null}. A non-null blessing makes a chest a BLESSING_CHEST. */
	public final Blessing blessing;
	/** A Mimic chest: right-click spawns the Mimic; the secret completes when the Mimic is killed. */
	public final boolean mimic;
	/** Whether this counts toward the dungeon's 47 scored secrets (false for Ice-Fill reward chests). */
	public final boolean counted;

	/** Owning room — set by {@link Rooms} when the secret is registered. */
	Room room;
	/** Run-time completion flag. */
	public boolean found;
	/** Run-time entity backing this secret (dropped item / bat / essence interaction / spawned mimic), if any. */
	public UUID entityId;

	private Secret(Utils.SecretType type, double x, double y, double z, Blessing blessing, boolean mimic, boolean counted) {
		this.type = type;
		this.x = x;
		this.y = y;
		this.z = z;
		this.blessing = blessing;
		this.mimic = mimic;
		this.counted = counted;
	}

	// --- factories ---
	public static Secret chest(double x, double y, double z) {
		return new Secret(Utils.SecretType.CHEST, x, y, z, null, false, true);
	}

	public static Secret blessingChest(double x, double y, double z, Utils.BlessingType t, int level) {
		return new Secret(Utils.SecretType.BLESSING_CHEST, x, y, z, new Blessing(t, level), false, true);
	}

	/** An Ice-Fill reward chest: a blessing chest that does NOT count toward the 47. */
	public static Secret rewardChest(double x, double y, double z, Utils.BlessingType t, int level) {
		return new Secret(Utils.SecretType.BLESSING_CHEST, x, y, z, new Blessing(t, level), false, false);
	}

	public static Secret mimicChest(double x, double y, double z) {
		return new Secret(Utils.SecretType.CHEST, x, y, z, null, true, true);
	}

	public static Secret item(double x, double y, double z) {
		return new Secret(Utils.SecretType.ITEM, x, y, z, null, false, true);
	}

	public static Secret bat(double x, double y, double z) {
		return new Secret(Utils.SecretType.BAT, x, y, z, null, false, true);
	}

	public static Secret essence(double x, double y, double z) {
		return new Secret(Utils.SecretType.ESSENCE, x, y, z, null, false, true);
	}

	public Room room() {
		return room;
	}

	/** Whether this secret is opened/collected by right-clicking a block (chest, mimic chest). */
	public boolean isChest() {
		return type == Utils.SecretType.CHEST || type == Utils.SecretType.BLESSING_CHEST;
	}

	/** Integer block coordinates (for chest / essence coordinate matching). */
	public int blockX() { return (int) Math.floor(x); }
	public int blockY() { return (int) Math.floor(y); }
	public int blockZ() { return (int) Math.floor(z); }

	public Location location(World world) {
		return new Location(world, x, y, z);
	}
}
