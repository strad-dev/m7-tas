package instructions.bosses.goldor;

/**
 * Where Same Color's nine panes should end up, and how far each of them is from it.
 * <p>
 * <b>This is Odin's {@code RubixHandler}, reimplemented against its source.</b>  The point of matching it is the
 * hints: a player who has used the mod reads the stack sizes expecting the mod's answer, and a solver that picks
 * a different target on the same board is not "also correct", it is a chest full of numbers that disagree with
 * what they have practised.  {@link GoldorTerminalGui}'s {@code CYCLE} is the numbering - index 0 is the first
 * colour in click order - and a LEFT click steps forward through it.
 * <p>
 * Two things a reimplementation gets wrong, so both are spelled out below: the tie rule in {@link #bestTarget},
 * and why {@link #signed} needs no tie rule of its own.
 */
final class RubixSolver {

	/** Length of the colour cycle.  <b>Odd</b>, and {@link #signed} leans on that. */
	static final int CYCLE = 5;

	private RubixSolver() {}

	/** Left clicks from {@code from} to {@code to}: 0 to 4, since the cycle wraps. */
	static int forward(int from, int to) {
		return Math.floorMod(to - from, CYCLE);
	}

	/**
	 * Clicks from {@code from} to {@code to}, signed: <b>positive is left clicks, negative is right clicks</b>.
	 * A forward distance of 3 comes back as -2 and 4 as -1, because going back round is shorter.  The cost of a
	 * pane is therefore {@code min(forward, 5 - forward)}, never more than 2.
	 * <p>
	 * <b>The cycle length is odd, so the two directions can never be equal</b> on a single pane - which is why
	 * this is a plain comparison with no rule for a tie.  An even cycle would need one.
	 */
	static int signed(int from, int to) {
		int f = forward(from, to);
		return f <= CYCLE / 2 ? f : f - CYCLE;
	}

	/**
	 * The colour the board should be driven to: the one with the lowest total click count over all nine panes.
	 * <p>
	 * <b>A tie goes to the first colour in cycle order.</b>  Odin's Kotlin is a {@code minBy} over the cycle in
	 * order, and {@code minBy} keeps the first minimum because it only replaces on a strict {@code <}.  Written
	 * with {@code <=} instead, this returns the LAST tied colour - a legal answer to a different question, and
	 * every hint on the chest is then wrong by a step or two on any board that ties.
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
