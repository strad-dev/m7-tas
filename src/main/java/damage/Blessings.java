package damage;

import plugin.Utils;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The dungeon blessing table, reproduced from Hypixel (MAP.md §1.13).
 * <p>
 * <b>Every blessing figure in this plugin comes from here.</b>  Nothing else may author one: the old code carried
 * {@code 0.0363}, {@code 7.26}, {@code 10.89} and {@code 5.445} as hand-tuned constants in three different files,
 * and each of those is just a base figure times {@link #effectIncrease()} - which is not a constant at all, since
 * Mayor Paul is one of its four terms.
 *
 * <h2>The table</h2>
 * A blessing grants a fixed set of stats, and <b>each stat gets a percent, a flat, or both</b> - the columns are
 * per stat, not per blessing.  Two stats deliberately have no percent: Stone's Damage and Wisdom's Speed.
 *
 * <table>
 *   <caption>Per level, BEFORE the effect increase</caption>
 *   <tr><th>Blessing</th><th>Stat</th><th>Percent</th><th>Flat</th></tr>
 *   <tr><td>Power</td><td>Strength</td><td>2%</td><td>+4</td></tr>
 *   <tr><td>Power</td><td>Crit Damage</td><td>2%</td><td>+4</td></tr>
 *   <tr><td>Stone</td><td>Defense</td><td>2%</td><td>+4</td></tr>
 *   <tr><td>Stone</td><td><b>Damage</b></td><td><b>none</b></td><td>+6</td></tr>
 *   <tr><td>Wisdom</td><td>Intelligence</td><td>2%</td><td>+4</td></tr>
 *   <tr><td>Wisdom</td><td><b>Speed</b></td><td><b>none</b></td><td>+4</td></tr>
 *   <tr><td>Time</td><td>Health, Defense, Strength, Intelligence</td><td>2%</td><td>+4</td></tr>
 *   <tr><td>Life</td><td>Health, Health Regen</td><td>3%</td><td>none</td></tr>
 * </table>
 *
 * <h2>The two-stage shape</h2>
 * <b>Flats sum, then percents multiply, and the flat lands INSIDE the percent:</b>
 * <pre>
 * flat(stat)       = SUM over blessings granting it of ( level x flatPerLevel x effectIncrease )
 * multiplier(stat) = PRODUCT over blessings granting it of ( 1 + level x percentPerLevel x effectIncrease )
 * final            = ( everything else ) + flat(stat) ) x multiplier(stat)
 * </pre>
 * A stat granted by two blessings gets a factor from each - Strength from Power AND Time, Intelligence from Wisdom
 * AND Time - which is why {@link #multiplier(Stat)} is a product over grants rather than a per-type lookup.
 * <b>Blessings are the LAST stage of the stat pipeline</b> ({@code Stats.compute}), after every other additive
 * percent and multiplier, because that is the "Stat" the flat is added to.
 *
 * <h2>Unmodelled stats</h2>
 * Health, Health Regen, Defense and Speed are authored here in full so the table is the real one and the chat
 * announcement can print them, but {@link Stat} has no constant for any of them and nothing consumes them: players
 * are invulnerable in every mode, so Health and Defense decide nothing, and movement speed is owned by
 * {@code Utils.setSpeed}.  A {@link Grant} with a null {@link Grant#modelled} is exactly that case - fill the
 * constant in and it starts counting, with no other change.
 */
public final class Blessings {
	private Blessings() {}

	// ==================== the effect increase ====================

	/** Wither Essence shop's Forbidden Blessing, at its maxed 10%. */
	public static final double FORBIDDEN_BLESSING = 0.10;
	/** The Floor III+ dungeon buff, 20%.  It does not scale past Floor VII, so M7 gets exactly this. */
	public static final double FLOOR_BUFF = 0.20;
	/** The Mimic Shard's Faker attribute, at its maxed 10%. */
	public static final double FAKER = 0.10;

	/**
	 * How much stronger every blessing figure is right now: <b>x1.815 under Mayor Paul, x1.452 without him</b>.
	 * <p>
	 * The four sources MULTIPLY, and this plugin assumes all of them are maxed except the mayor, which is a live
	 * setting ({@link Mayor#PAUL_BLESSING_BOOST}):
	 * {@code 1.25 x 1.10 x 1.20 x 1.10 = 1.815}, i.e. Hypixel's published +81.5% ceiling.
	 * <p>
	 * <b>It scales the flat half and the percent half alike</b>, which is the whole reason this is one function -
	 * the two used to be separate hand-tuned constants that could drift apart.
	 */
	public static double effectIncrease() {
		return (Mayor.blessingsBoosted() ? Mayor.PAUL_BLESSING_BOOST : 1.0)
				* (1.0 + FORBIDDEN_BLESSING) * (1.0 + FLOOR_BUFF) * (1.0 + FAKER);
	}

	// ==================== the table ====================

	/**
	 * One blessing's grant on one stat.
	 *
	 * @param display        the stat's name as the announcement writes it
	 * @param colour         MiniMessage colour tag for that name
	 * @param glyph          the SkyBlock stat glyph the announcement prefixes it with
	 * @param modelled       the {@link Stat} this feeds, or <b>null</b> for a stat this plugin does not track
	 * @param percentPerLevel per level, as a FRACTION (0.02 = 2%); 0 for a stat that gets no percent
	 * @param flatPerLevel   per level, raw; 0 for a stat that gets no flat
	 */
	public record Grant(String display, String colour, String glyph, Stat modelled,
			double percentPerLevel, double flatPerLevel) {

		/** This grant's multiplicative factor at a level: {@code 1 + level x percent x effectIncrease}. */
		public double multiplierAt(int level) {
			return percentPerLevel == 0 ? 1.0 : 1.0 + level * percentPerLevel * effectIncrease();
		}

		/** This grant's flat contribution at a level: {@code level x flat x effectIncrease}. */
		public double flatAt(int level) {
			return flatPerLevel == 0 ? 0.0 : level * flatPerLevel * effectIncrease();
		}
	}

	private static final double PCT = 0.02;      // every blessing but Life
	private static final double LIFE_PCT = 0.03;
	private static final double FLAT = 4;        // every flat but Stone's Damage
	private static final double STONE_DAMAGE_FLAT = 6;

	private static final Map<Utils.BlessingType, List<Grant>> TABLE = new EnumMap<>(Utils.BlessingType.class);

	static {
		TABLE.put(Utils.BlessingType.POWER, List.of(
				new Grant("Strength", "<red>", "❁", Stat.STRENGTH, PCT, FLAT),
				new Grant("Crit Damage", "<blue>", "☠", Stat.CRIT_DAMAGE, PCT, FLAT)));
		TABLE.put(Utils.BlessingType.STONE, List.of(
				new Grant("Defense", "<green>", "❈", null, PCT, FLAT),
				// Damage takes the flat ONLY - no percent.  Confirmed twice over: Hypixel's own announcement says
				// "+X & +Yx Defense and +Z Damage", and the wiki's per-stat table leaves Damage's percent empty.
				new Grant("Damage", "<red>", "❁", Stat.DAMAGE, 0, STONE_DAMAGE_FLAT)));
		TABLE.put(Utils.BlessingType.WISDOM, List.of(
				new Grant("Intelligence", "<aqua>", "✎", Stat.INTELLIGENCE, PCT, FLAT),
				// Speed, like Stone's Damage, is flat only.
				new Grant("Speed", "<white>", "✦", null, 0, FLAT)));
		TABLE.put(Utils.BlessingType.TIME, List.of(
				new Grant("Health", "<red>", "❤", null, PCT, FLAT),
				new Grant("Intelligence", "<aqua>", "✎", Stat.INTELLIGENCE, PCT, FLAT),
				new Grant("Defense", "<green>", "❈", null, PCT, FLAT),
				new Grant("Strength", "<red>", "❁", Stat.STRENGTH, PCT, FLAT)));
		TABLE.put(Utils.BlessingType.LIFE, List.of(
				new Grant("Health", "<red>", "❤", null, LIFE_PCT, 0),
				new Grant("Health Regen", "<red>", "❣", null, LIFE_PCT, 0)));
	}

	/** What one blessing type grants, in announcement order.  Empty for a type with no row. */
	public static List<Grant> grants(Utils.BlessingType type) {
		return TABLE.getOrDefault(type, List.of());
	}

	// ==================== what the stat pipeline reads ====================

	/**
	 * The product of every blessing factor on one stat right now, or 1.0 if no blessing grants it a percent.
	 * <p>
	 * A product over GRANTS, not over types: Strength is granted by both Power and Time and takes a factor from
	 * each, exactly as Hypixel stacks them.
	 */
	public static double multiplier(Stat stat) {
		double product = 1.0;
		for(Map.Entry<Utils.BlessingType, List<Grant>> e : TABLE.entrySet()) {
			int level = Difficulty.blessingLevel(e.getKey());
			if(level <= 0) continue;
			for(Grant g : e.getValue()) if(g.modelled() == stat) product *= g.multiplierAt(level);
		}
		return product;
	}

	/** The sum of every blessing's flat contribution to one stat right now. */
	public static double flat(Stat stat) {
		double sum = 0;
		for(Map.Entry<Utils.BlessingType, List<Grant>> e : TABLE.entrySet()) {
			int level = Difficulty.blessingLevel(e.getKey());
			if(level <= 0) continue;
			for(Grant g : e.getValue()) if(g.modelled() == stat) sum += g.flatAt(level);
		}
		return sum;
	}

	// ==================== what the reports read ====================

	/**
	 * One type's multiplicative factor at its current level - the number its announcement prints.
	 * <p>
	 * Every stat a blessing grants a percent to gets the same one, so this is a single figure per type: read off
	 * the first grant that has a percent, and 1.0 for a type with none.
	 */
	public static double multiplier(Utils.BlessingType type) {
		int level = Difficulty.blessingLevel(type);
		for(Grant g : grants(type)) if(g.percentPerLevel() != 0) return g.multiplierAt(level);
		return 1.0;
	}

	/** One type's flat contribution to one stat at its current level, or 0 if it does not grant it. */
	public static double flat(Utils.BlessingType type, Stat stat) {
		int level = Difficulty.blessingLevel(type);
		double sum = 0;
		for(Grant g : grants(type)) if(g.modelled() == stat) sum += g.flatAt(level);
		return sum;
	}

	/**
	 * The stat half of the "DUNGEON BUFF!" announcement for one blessing at one level, as MiniMessage.
	 * <p>
	 * Generated from the table rather than written out, so it can never disagree with what was actually applied -
	 * and so it follows the mayor, which the four hand-written constants it replaced could not.  Each stat reads
	 * "+flat & +Nx Name", "+flat Name" or "+Nx Name" depending on which halves it gets.
	 */
	public static String describe(Utils.BlessingType type, int level) {
		List<Grant> grants = grants(type);
		StringBuilder sb = new StringBuilder("<gray>     Granted you ");
		for(int i = 0; i < grants.size(); i++) {
			if(i > 0) sb.append(i == grants.size() - 1 ? (grants.size() > 2 ? "<gray>, and " : "<gray> and ") : "<gray>, ");
			Grant g = grants.get(i);
			double flat = g.flatAt(level);
			double mult = g.multiplierAt(level);
			if(flat != 0) sb.append("<green>+").append(Utils.round(flat, 1)).append("</green>");
			if(flat != 0 && mult != 1.0) sb.append("<gray> & ");
			if(mult != 1.0) sb.append("<green>+").append(Utils.round(mult, 2)).append("x</green>");
			sb.append(g.colour()).append(' ').append(g.glyph()).append(' ').append(g.display()).append("</").append(g.colour().substring(1));
		}
		return sb.toString();
	}
}
