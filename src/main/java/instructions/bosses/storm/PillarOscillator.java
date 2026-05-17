package instructions.bosses.storm;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import plugin.Utils;

/**
 * One pillar's oscillation state machine. Polled every 20 ticks by Storm;
 * each call to {@link #runCycle(int)} represents one 20-tick cycle in which
 * the pillar advances if the pad-gate caller has determined a player is
 * standing on the corresponding pad.
 *
 * Motion model: pillar material always extends from {@link #bottomY} up to
 * {@link PadAndPillar#PILLAR_ANCHOR_Y} (y196). Moving DOWN by one block:
 * clone the row at y={@code bottomY} to y={@code bottomY - 1}, then decrement
 * bottomY. Moving UP by one block: air-fill the row at y={@code bottomY},
 * then increment bottomY. The anchor at y196 is the persistent seed and
 * never moves.
 *
 * Cycle motion table (measured in-game):
 * <pre>
 *   bottomY at  | direction | this cycle's behavior
 *   cycle start | at start  |
 *   ------------|-----------|---------------------------------------
 *   limit (169 or 189) | (any) | stall 8t, reverse, 3 clones × 4t in new direction
 *   else, within 5 of limit  | (any) | (limit-bottomY) clones × 4t, then idle
 *   else                     | (any) | 5 clones × 4t
 * </pre>
 */
public final class PillarOscillator {
	private enum Direction {
		DOWN, UP;
		Direction reverse() { return this == DOWN ? UP : DOWN; }
	}

	private final PadAndPillar pillar;
	private int bottomY;
	private Direction direction;
	private int lastMovementTick;
	private boolean used;

	public PillarOscillator(PadAndPillar pillar) {
		this.pillar = pillar;
		reset();
	}

	/** Resets to initial state — bottom at y175, direction DOWN, no movement recorded, not used. */
	public void reset() {
		bottomY = PadAndPillar.PILLAR_BOTTOM_INITIAL;
		direction = Direction.DOWN;
		lastMovementTick = Integer.MIN_VALUE;
		used = false;
	}

	public PadAndPillar getPillar() { return pillar; }
	public int getBottomY() { return bottomY; }

	/** @return true once this pillar has crushed Storm and been consumed — the pad no longer activates it. */
	public boolean isUsed() { return used; }
	public void markUsed() { used = true; }

	/**
	 * Execute one 20-tick cycle's motion, scheduling per-block clone ops at 4-tick
	 * intervals. Caller is responsible for the pad-presence gating — only call
	 * this when the pad is occupied at the cycle boundary.
	 *
	 * @param currentTick the boss-fight tick when the cycle starts; recorded as
	 *                    the most-recent-movement marker for crush-detector arming.
	 */
	public void runCycle(int currentTick) {
		boolean atLimit = (direction == Direction.DOWN && bottomY == PadAndPillar.PILLAR_BOTTOM_MIN)
				|| (direction == Direction.UP && bottomY == PadAndPillar.PILLAR_BOTTOM_MAX);

		int delay;
		int clones;
		if(atLimit) {
			direction = direction.reverse();
			delay = 8;
			clones = 3;
		} else {
			int limit = (direction == Direction.DOWN) ? PadAndPillar.PILLAR_BOTTOM_MIN : PadAndPillar.PILLAR_BOTTOM_MAX;
			clones = Math.min(5, Math.abs(limit - bottomY));
			delay = 0;
		}

		for(int i = 0; i < clones; i++) {
			Utils.scheduleTask(this::moveOne, delay + i * 4L);
		}

		// Only downward motion arms the crush detector — upward cycles don't count.
		if(direction == Direction.DOWN) {
			lastMovementTick = currentTick;
		}
	}

	private void moveOne() {
		if(direction == Direction.DOWN) {
			// Push Storm out from under the descending pillar before placing the
			// new bottom row. If Storm is already at the floor the push no-ops
			// and the next 20-tick poll's crush detector handles him.
			Storm.INSTANCE.tryPushBelowDescendingPillar(pillar, bottomY - 1);
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
					String.format("clone %d %d %d %d %d %d %d %d %d",
							pillar.pillarX1(), bottomY, pillar.pillarZ1(),
							pillar.pillarX2(), bottomY, pillar.pillarZ2(),
							pillar.pillarX1(), bottomY - 1, pillar.pillarZ1()));
			bottomY--;
		} else {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
					String.format("fill %d %d %d %d %d %d minecraft:air",
							pillar.pillarX1(), bottomY, pillar.pillarZ1(),
							pillar.pillarX2(), bottomY, pillar.pillarZ2()));
			bottomY++;
		}
		Utils.playGlobalSound(Sound.BLOCK_PISTON_CONTRACT, 2.0f, 1.0f);
	}

	/**
	 * @return true if the pillar is currently descending AND a DOWN clone op was scheduled
	 * within the last {@code windowTicks} ticks. An UP-moving pillar never arms crush, even
	 * if its prior DOWN cycle was within the window.
	 */
	public boolean movedRecently(int currentTick, int windowTicks) {
		if(direction != Direction.DOWN) return false;
		return lastMovementTick != Integer.MIN_VALUE && (currentTick - lastMovementTick) <= windowTicks;
	}
}
