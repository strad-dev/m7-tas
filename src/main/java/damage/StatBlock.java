package damage;

/**
 * An immutable bag of {@link Stat} values.  Every stat source in DAMAGE_PLAN.md §2 returns one of these and no
 * source knows about any other, so the aggregate is a plain sum.
 * <p>
 * Values are raw SkyBlock numbers (a Fabled Hyperion's Strength is 560 unscaled, 3729.6 after the dungeon stage).
 * Instances are cheap and short-lived; the aggregator caches the finished sum, not the intermediates.
 */
public final class StatBlock {
	public static final StatBlock EMPTY = new StatBlock(new double[Stat.values().length]);

	private final double[] values;

	private StatBlock(double[] values) {
		this.values = values;
	}

	/** One stat, one value. */
	public static StatBlock of(Stat stat, double value) {
		double[] v = new double[Stat.values().length];
		v[stat.ordinal()] = value;
		return new StatBlock(v);
	}

	/** Two stats at once, the common shape for a reforge or gemstone row. */
	public static StatBlock of(Stat a, double av, Stat b, double bv) {
		double[] v = new double[Stat.values().length];
		v[a.ordinal()] = av;
		v[b.ordinal()] = bv;
		return new StatBlock(v);
	}

	/** Three stats at once (the Ancient armour reforge, and the Bizarre power's Strength/Crit Damage/Intelligence). */
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

	/** Every stat multiplied by the same factor.  Used by the Necron Head's x2 in M7 (§1.10). */
	public StatBlock times(double factor) {
		if(factor == 1.0) return this;
		double[] v = values.clone();
		for(int i = 0; i < v.length; i++) v[i] *= factor;
		return new StatBlock(v);
	}

	/**
	 * The dungeon scaling stage (§1.0.1-3, §2.4): the four core stats take {@code coreMult}, everything else takes
	 * {@code otherMult}.  It is a PIPELINE STAGE applied after summing an item's terms, never baked into a term, so
	 * every authored value in {@link Items} stays the plain SkyBlock number the wiki prints.
	 */
	public StatBlock scaled(double coreMult, double otherMult) {
		double[] v = values.clone();
		for(Stat s : Stat.values()) v[s.ordinal()] *= s.core() ? coreMult : otherMult;
		return new StatBlock(v);
	}

	/** True if every stat is zero, i.e. this source contributes nothing (used to skip lore rows). */
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
