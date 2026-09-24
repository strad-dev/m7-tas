package instructions.bosses.goldor;

/**
 * Same Color's target and each pane's distance to it. Odin's {@code RubixHandler} reimplemented against its source:
 * players read the stack-size hints expecting the mod's answer, so a different target on the same board is wrong.
 * {@link GoldorTerminalGui}'s {@code CYCLE} is the numbering (index 0 = first colour in click order); LEFT steps forward.
 */
final class RubixSolver {

	/** Odd, and {@link #signed} relies on that. */
	static final int CYCLE = 5;

	private RubixSolver() {}

	/** Left clicks from {@code from} to {@code to}: 0 to 4, since the cycle wraps. */
	static int forward(int from, int to) {
		return Math.floorMod(to - from, CYCLE);
	}

	/**
	 * Signed clicks: positive = left, negative = right. Forward 3 becomes -2, 4 becomes -1, so a pane costs at most 2.
	 * The cycle is odd, so both directions are never equal and no tie rule is needed; an even cycle would need one.
	 */
	static int signed(int from, int to) {
		int f = forward(from, to);
		return f <= CYCLE / 2 ? f : f - CYCLE;
	}

	/**
	 * Colour with the lowest total clicks over all nine panes. A tie goes to the FIRST colour in cycle order, like
	 * Odin's {@code minBy}: keep the strict {@code <}. With {@code <=} every hint on a tied board is off by a step or two.
	 */
	static int bestTarget(int[] board) {
		int best = 0;
		int bestCost = Integer.MAX_VALUE;
		for(int target = 0; target < CYCLE; target++) {
			int cost = 0;
			for(int colour : board) cost += Math.abs(signed(colour, target));
			if(cost < bestCost) {
				bestCost = cost;
				best = target;
			}
		}
		return best;
	}
}
