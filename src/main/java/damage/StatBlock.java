package damage;

/**
 * Immutable bag of {@link Stat} values. Every §2 source returns one and none knows the others, so the aggregate is a
 * plain sum. Raw SkyBlock numbers (Fabled Hyperion Strength 560 unscaled, 3729.6 after the dungeon stage). The
 * aggregator caches only the finished sum.
 */
public final class StatBlock {
	public static final StatBlock EMPTY = new StatBlock(new double[Stat.values().length]);

	private final double[] values;

	private StatBlock(double[] values) {
		this.values = values;
	}

	public static StatBlock of(Stat stat, double value) {
		double[] v = new double[Stat.values().length];
		v[stat.ordinal()] = value;
		return new StatBlock(v);
	}

	public static StatBlock of(Stat a, double av, Stat b, double bv) {
		double[] v = new double[Stat.values().length];
		v[a.ordinal()] = av;
		v[b.ordinal()] = bv;
		return new StatBlock(v);
	}

	public static StatBlock of(Stat a, double av, Stat b, double bv, Stat c, double cv) {
		double[] v = new double[Stat.values().length];
		v[a.ordinal()] = av;
		v[b.ordinal()] = bv;
		v[c.ordinal()] = cv;
		return new StatBlock(v);
	}

	public double get(Stat stat) {
		return values[stat.ordinal()];
	}

	public StatBlock plus(StatBlock other) {
		if(other == null) return this;
		double[] v = values.clone();
		for(int i = 0; i < v.length; i++) v[i] += other.values[i];
		return new StatBlock(v);
	}

	public StatBlock plus(Stat stat, double value) {
		if(value == 0) return this;
		double[] v = values.clone();
		v[stat.ordinal()] += value;
		return new StatBlock(v);
	}

	/** Used by the Necron Head's x2 (§1.10). */
	public StatBlock times(double factor) {
		if(factor == 1.0) return this;
		double[] v = values.clone();
		for(int i = 0; i < v.length; i++) v[i] *= factor;
		return new StatBlock(v);
	}

	/**
	 * Dungeon scaling stage (§1.0.1-3, §2.4): core stats x{@code coreMult}, rest x{@code otherMult}. Applied after
	 * summing terms, never baked in, so {@link Items} values stay the wiki's numbers.
	 */
	public StatBlock scaled(double coreMult, double otherMult) {
		double[] v = values.clone();
		for(Stat s : Stat.values()) v[s.ordinal()] *= s.core() ? coreMult : otherMult;
		return new StatBlock(v);
	}

	/** All zero; used to skip lore rows. */
	public boolean isEmpty() {
		for(double d : values) if(d != 0) return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("StatBlock[");
		boolean first = true;
		for(Stat s : Stat.values()) {
			double d = get(s);
			if(d == 0) continue;
			if(!first) sb.append(", ");
			sb.append(s.display()).append('=').append(plugin.Utils.round(d, 2));
			first = false;
		}
		return sb.append(']').toString();
	}
}
