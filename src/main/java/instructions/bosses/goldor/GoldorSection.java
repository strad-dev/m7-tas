package instructions.bosses.goldor;

import java.util.List;

public final class GoldorSection {
	public final int idx;
	public final List<GoldorTerminal> terminals;
	public final GoldorDevice device;
	public final List<GoldorLever> levers;
	/** S1..S3 have a gate; S4 has none (core gate is handled separately by Goldor). */
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
	}

	public void cleanup() {
		for(GoldorTerminal t : terminals) t.cleanup();
		device.cleanup();
		for(GoldorLever l : levers) l.cleanup();
		if(gate != null) gate.cleanup();
	}
}
