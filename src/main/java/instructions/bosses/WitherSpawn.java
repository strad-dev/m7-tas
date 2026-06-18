package instructions.bosses;

import instructions.bosses.storm.PadAndPillar;
import org.bukkit.World;
import plugin.Utils;

/**
 * Static helpers shared across wither bosses that aren't owned by any single
 * {@link WitherLord} subclass — in particular, setup-time block restoration
 * for the Storm pillars that runs at every {@code /tas} invocation.
 */
public final class WitherSpawn {
	private WitherSpawn() {}

	/**
	 * Restore all three active Storm pillars to their initial state
	 * (column y175..y196 of pillar material, with air below y175).
	 *
	 * Runs as part of every {@code /tas} setup, not just at Storm fight start —
	 * the user wants the world reset across all TAS invocations.
	 *
	 * For each pillar:
	 * <ol>
	 *   <li>Air-clear y169..y175 — wipes any stale blocks left below the initial bottom
	 *       if a previous run ended with the pillar over-extended down to y169..y174.
	 *   <li>Exponentially clone-down from the anchor at y196: each step doubles the
	 *       already-filled region, so a 21-row column rebuilds in ~5 clones instead of 21.
	 *       The anchor at y196 stays put as the seed for the first clone.
	 * </ol>
	 *
	 * Final state: 22-block pillar column from y175 to y196, with air at y169..y174.
	 */
	public static void restoreStormPillars(World world) {
		for(PadAndPillar p : PadAndPillar.ACTIVE) {
			// Step 1: air-clear the lower extension (y169..y175 inclusive)
			Utils.runCommand(
					String.format("fill %d %d %d %d %d %d minecraft:air",
							p.pillarX1(), PadAndPillar.PILLAR_BOTTOM_MIN, p.pillarZ1(),
							p.pillarX2(), PadAndPillar.PILLAR_BOTTOM_INITIAL, p.pillarZ2()));

			// Step 2: exponential clone-down. Each pass copies the lowest `rowsToAdd` rows
			// of the filled region to the next `rowsToAdd` rows below it, doubling the
			// column height (capped so we don't shoot past PILLAR_BOTTOM_INITIAL).
			int bottom = PadAndPillar.PILLAR_ANCHOR_Y; // currently filled down to here (just the anchor row)
			while(bottom > PadAndPillar.PILLAR_BOTTOM_INITIAL) {
				int filledRows = PadAndPillar.PILLAR_ANCHOR_Y - bottom + 1;
				int rowsToAdd = Math.min(filledRows, bottom - PadAndPillar.PILLAR_BOTTOM_INITIAL);
				int srcY1 = bottom;
				int srcY2 = bottom + rowsToAdd - 1;
				int dstY1 = bottom - rowsToAdd;
				Utils.runCommand(
						String.format("clone %d %d %d %d %d %d %d %d %d",
								p.pillarX1(), srcY1, p.pillarZ1(),
								p.pillarX2(), srcY2, p.pillarZ2(),
								p.pillarX1(), dstY1, p.pillarZ1()));
				bottom = dstY1;
			}
		}
	}
}
