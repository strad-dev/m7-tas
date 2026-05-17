package instructions.bosses;

import instructions.bosses.storm.PadAndPillar;
import org.bukkit.Bukkit;
import org.bukkit.World;

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
	 *   <li>Iteratively clone-down from the anchor at y196: clone (y+1)→y for y from
	 *       y195 down to y175. The anchor at y196 is the source and never moves.
	 * </ol>
	 *
	 * Final state: 22-block pillar column from y175 to y196, with air at y169..y174.
	 */
	public static void restoreStormPillars(World world) {
		for(PadAndPillar p : PadAndPillar.ACTIVE) {
			// Step 1: air-clear the lower extension (y169..y175 inclusive)
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
					String.format("fill %d %d %d %d %d %d minecraft:air",
							p.pillarX1(), PadAndPillar.PILLAR_BOTTOM_MIN, p.pillarZ1(),
							p.pillarX2(), PadAndPillar.PILLAR_BOTTOM_INITIAL, p.pillarZ2()));

			// Step 2: clone-down from the anchor at y196 to rebuild the column to y175
			for(int y = PadAndPillar.PILLAR_ANCHOR_Y - 1; y >= PadAndPillar.PILLAR_BOTTOM_INITIAL; y--) {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
						String.format("clone %d %d %d %d %d %d %d %d %d",
								p.pillarX1(), y + 1, p.pillarZ1(),
								p.pillarX2(), y + 1, p.pillarZ2(),
								p.pillarX1(), y, p.pillarZ1()));
			}
		}
	}
}
