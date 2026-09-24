package instructions.bosses.storm;

import org.bukkit.util.BoundingBox;

import java.util.List;

/**
 * A Storm pad and its pillar. Red is inert: in {@link #ALL} for display, never in {@link #ACTIVE}.
 * <br>
 * padBox is matched against the player's feet block X/Y/Z, inclusive; the high X/Z corner is -1 from the raw world
 * size since it tests the block stood on, not half-block edges. The pillar is a 7×7 footprint whose bottom block
 * ranges {@link #PILLAR_BOTTOM_MIN}..{@link #PILLAR_BOTTOM_MAX}, with the always-present anchor at {@link #PILLAR_ANCHOR_Y}.
 */
public record PadAndPillar(
		String name,
		/** MiniMessage tag matching the pad's wool, for Storm's action-bar HUD. */
		String color,
		BoundingBox padBox,
		int pillarX1, int pillarX2,
		int pillarZ1, int pillarZ2
) {
	/** Bottom y at setup. */
	public static final int PILLAR_BOTTOM_INITIAL = 175;

	/** Lowest the bottom block reaches. */
	public static final int PILLAR_BOTTOM_MIN = 169;

	/** Highest the bottom block reaches. */
	public static final int PILLAR_BOTTOM_MAX = 189;

	/** Always pillar material; the explosion filter preserves y >= this. */
	public static final int PILLAR_ANCHOR_Y = 196;

	/** Inert Red pillar's footprint. The crush-explosion filter uses it to preserve Red when a blast reaches it. */
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
	 * Display-only, kept out of {@link #ACTIVE} so oscillation and crush never pick it up. Lets the HUD name the
	 * nearest pad. Pad box mirrors the other three into the SE corner.
	 */
	public static final PadAndPillar RED = new PadAndPillar(
			"Red", "<red>",
			new BoundingBox(111, 169, 9, 117, 171, 15),
			RED_PILLAR_X1, RED_PILLAR_X2, RED_PILLAR_Z1, RED_PILLAR_Z2
	);

	/** The three that oscillate; not {@link #RED}. */
	public static final List<PadAndPillar> ACTIVE = List.of(PURPLE, YELLOW, GREEN);

	/** Includes Red. Display and lookup only; never drive a pillar from this. */

	public static final List<PadAndPillar> ALL = List.of(PURPLE, YELLOW, GREEN, RED);
}
