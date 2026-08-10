package instructions.bosses.storm;

import org.bukkit.util.BoundingBox;

import java.util.List;

/**
 * One of the three active Storm pads + its associated pillar column.
 * The "Red" pad/pillar exists in the world but is inert: it's in {@link #ALL} for display purposes only,
 * never in {@link #ACTIVE}.
 * <br>
 * The pad detect box is a 3D bounding box matched against the player's feet
 * BlockX/Y/Z (inclusive on both ends). The higher X/Z corner is offset -1 from
 * the raw world dimensions because we're checking which block the player is
 * standing on top of, not the half-block edges.
 * <br>
 * The pillar column is a 7×7 horizontal footprint at (pillarX1..pillarX2,
 * pillarZ1..pillarZ2). The pillar's vertical extent is bounded by
 * {@link #PILLAR_BOTTOM_MIN}..{@link #PILLAR_BOTTOM_MAX} for the bottom-most
 * block, with the anchor (always-present source layer) at {@link #PILLAR_ANCHOR_Y}.
 */
public record PadAndPillar(
		String name,
		/** MiniMessage colour tag matching the pad's wool, used by Storm's action-bar HUD. */
		String color,
		BoundingBox padBox,
		int pillarX1, int pillarX2,
		int pillarZ1, int pillarZ2
) {
	/** Initial bottom-y of every pillar at setup time. */
	public static final int PILLAR_BOTTOM_INITIAL = 175;

	/** Lowest the pillar's bottom-most block can reach during oscillation. */
	public static final int PILLAR_BOTTOM_MIN = 169;

	/** Highest the pillar's bottom-most block can reach during oscillation. */
	public static final int PILLAR_BOTTOM_MAX = 189;

	/** Anchor layer: always contains pillar material.  The explosion filter preserves y >= this. */
	public static final int PILLAR_ANCHOR_Y = 196;

	/**
	 * Red pillar footprint, the inert fourth pillar.  Not in {@link #ACTIVE} because it
	 * never oscillates and has no pad, but the crush-explosion filter uses these bounds
	 * to explicitly preserve Red's column even when an active pillar's blast radius
	 * reaches it.
	 */
	public static final int RED_PILLAR_X1 = 97;
	public static final int RED_PILLAR_X2 = 103;
	public static final int RED_PILLAR_Z1 = 38;
	public static final int RED_PILLAR_Z2 = 44;

	public static final PadAndPillar PURPLE = new PadAndPillar(
			"Purple", "<dark_purple>",
			new BoundingBox(111, 169, 91, 117, 171, 97),
			97, 103, 62, 68
	);

	public static final PadAndPillar YELLOW = new PadAndPillar(
			"Yellow", "<yellow>",
			new BoundingBox(29, 169, 91, 35, 171, 97),
			43, 49, 62, 68
	);

	public static final PadAndPillar GREEN = new PadAndPillar(
			"Green", "<green>",
			new BoundingBox(29, 169, 9, 35, 171, 15),
			43, 49, 38, 44
	);

	/**
	 * The inert fourth pad/pillar.  <b>Display-only</b>, deliberately kept out of {@link #ACTIVE} so nothing in the
	 * oscillation or crush path can pick it up.  It exists solely so the action-bar HUD can name and colour the pad
	 * a player is standing nearest to.  Its pad box mirrors the other three into the remaining SE corner.
	 */
	public static final PadAndPillar RED = new PadAndPillar(
			"Red", "<red>",
			new BoundingBox(111, 169, 9, 117, 171, 15),
			RED_PILLAR_X1, RED_PILLAR_X2, RED_PILLAR_Z1, RED_PILLAR_Z2
	);

	/** The three pads that actually oscillate.  Red is excluded; see {@link #RED}. */
	public static final List<PadAndPillar> ACTIVE = List.of(PURPLE, YELLOW, GREEN);

	/** All four pads including the inert Red one.  For display and lookup only; never iterate this to drive a pillar. */

	public static final List<PadAndPillar> ALL = List.of(PURPLE, YELLOW, GREEN, RED);
}
