package instructions.clear;

import org.bukkit.Location;
import org.bukkit.World;
import plugin.Utils;

import java.util.UUID;

/**
 * One interactable secret or reward chest, built once in {@link Rooms}.
 *
 * <p>Coords as given in the spec: chests/essence are block coords, items/bats decimal spawn coords.
 * {@link #counted} is false for the two Ice-Fill reward chests, so they share the chest-open path without
 * inflating the 47.
 */
public class Secret {
	/** How it's completed and what sound it plays. */
	public final Utils.SecretType type;
	public final double x, y, z;
	/** Chest's blessing or null. Non-null makes it a BLESSING_CHEST. */
	public final Blessing blessing;
	/** Right-click spawns the Mimic; completes when it dies. */
	public final boolean mimic;
	/** Counts toward the 47 scored secrets (false for Ice-Fill reward chests). */
	public final boolean counted;

	/** Set by {@link Rooms} on registration. */
	Room room;
	public boolean found;
	/** Run-time entity backing it (item, bat, essence interaction, mimic), if any. */
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

	public static Secret chest(double x, double y, double z) {
		return new Secret(Utils.SecretType.CHEST, x, y, z, null, false, true);
	}

	public static Secret blessingChest(double x, double y, double z, Utils.BlessingType t, int level) {
		return new Secret(Utils.SecretType.BLESSING_CHEST, x, y, z, new Blessing(t, level), false, true);
	}

	/** Ice-Fill reward chest: blessing chest that doesn't count toward the 47. */
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

	/** Collected by right-clicking a block (chest, mimic chest). */
	public boolean isChest() {
		return type == Utils.SecretType.CHEST || type == Utils.SecretType.BLESSING_CHEST;
	}

	/** Block coords, for chest/essence matching. */
	public int blockX() { return (int) Math.floor(x); }
	public int blockY() { return (int) Math.floor(y); }
	public int blockZ() { return (int) Math.floor(z); }

	public Location location(World world) {
		return new Location(world, x, y, z);
	}
}
