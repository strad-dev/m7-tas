package instructions.bosses.goldor;

import java.util.List;

public final class GoldorSection {
	public final int idx;
	public final List<GoldorTerminal> terminals;
	public final GoldorDevice device;
	public final List<GoldorLever> levers;
	/** S1..S3 only; Goldor handles S4's core gate separately. */
	public final GoldorGate gate;
	public final int totalItems;

	public int completed = 0;

	public GoldorSection(int idx, List<GoldorTerminal> terminals, GoldorDevice device,
	                     List<GoldorLever> levers, GoldorGate gate) {
		this.idx = idx;
		this.terminals = terminals;
		this.device = device;
		this.levers = levers;
		this.gate = gate;
		this.totalItems = terminals.size() + 1 + levers.size();
		// Here, not in GoldorTerminal: "at most one of each type per section" needs the whole set, and every
		// buildS1..buildS4 funnels through this constructor, so no builder can forget.
		GoldorTerminalGui.assignTypes(terminals);
	}

	public void cleanup() {
		for(GoldorTerminal t : terminals) t.cleanup();
		device.cleanup();
		for(GoldorLever l : levers) l.cleanup();
		if(gate != null) gate.cleanup();
	}
}
