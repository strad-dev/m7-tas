package damage;

import plugin.Utils;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Hypixel's dungeon blessing table (MAP.md §1.13).
 * <p>
 * <b>Every blessing figure comes from here.</b> Old code had {@code 0.0363}, {@code 7.26}, {@code 10.89} and
 * {@code 5.445} hand-tuned in three files; each was just base x {@link #effectIncrease()}, which isn't a constant
 * since Mayor Paul is one of its terms.
 *
 * <h2>Table</h2>
 * Each stat gets a percent, a flat, or both; columns are per stat, not per blessing. Stone's Damage and Wisdom's
 * Speed deliberately have no percent.
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
 * <h2>Shape</h2>
 * Flats sum, percents multiply, and the flat lands INSIDE the percent:
 * <pre>
 * flat(stat)       = SUM over blessings granting it of ( level x flatPerLevel x effectIncrease )
 * multiplier(stat) = PRODUCT over blessings granting it of ( 1 + level x percentPerLevel x effectIncrease )
 * final            = ( everything else ) + flat(stat) ) x multiplier(stat)
 * </pre>
 * A stat from two blessings gets a factor from each (Strength from Power AND Time), so {@link #multiplier(Stat)} is
 * a product over grants. Blessings are the LAST stage of {@code Stats.compute}, after every other percent and
 * multiplier, since that is the "Stat" the flat is added to.
 * <p>
 * Health, Health Regen, Defense and Speed are in the table so the announcement prints them, but have no {@link Stat}
 * (null {@link Grant#modelled}): players are invulnerable and speed is owned by {@code Utils.setSpeed}. Fill the
 * constant in and it starts counting.
 */
public final class Blessings {
	private Blessings() {}

	// ==================== the effect increase ====================

	/** Wither Essence shop's Forbidden Blessing, at its maxed 10%. */
	public static final double FORBIDDEN_BLESSING = 0.10;
	/** Floor III+ dungeon buff, 20%. Doesn't scale past Floor VII, so M7 gets exactly this. */
	public static final double FLOOR_BUFF = 0.20;
	/** Mimic Shard's Faker attribute, maxed 10%. */
	public static final double FAKER = 0.10;

	/**
	 * Blessing strength: x1.815 under Paul, x1.452 without. The four sources multiply, all assumed maxed except the
	 * live mayor setting ({@link Mayor#PAUL_BLESSING_BOOST}): {@code 1.25 x 1.10 x 1.20 x 1.10 = 1.815}, Hypixel's
	 * published +81.5% ceiling. Scales flat and percent alike; they used to be separate constants that drifted.
	 */
	public static double effectIncrease() {
		return (Mayor.blessingsBoosted() ? Mayor.PAUL_BLESSING_BOOST : 1.0)
				* (1.0 + FORBIDDEN_BLESSING) * (1.0 + FLOOR_BUFF) * (1.0 + FAKER);
	}

	// ==================== the table ====================

	/**
	 * One blessing's grant on one stat.
	 *
	 * @param display        stat name as the announcement writes it
	 * @param colour         MiniMessage colour tag
	 * @param glyph          SkyBlock stat glyph
	 * @param modelled       the {@link Stat} this feeds, or null for an untracked stat
	 * @param percentPerLevel FRACTION (0.02 = 2%); 0 for no percent
	 * @param flatPerLevel   raw; 0 for no flat
	 */
	public record Grant(String display, String colour, String glyph, Stat modelled,
			double percentPerLevel, double flatPerLevel) {

		/** {@code 1 + level x percent x effectIncrease}. */
		public double multiplierAt(int level) {
			return percentPerLevel == 0 ? 1.0 : 1.0 + level * percentPerLevel * effectIncrease();
		}

		/** {@code level x flat x effectIncrease}. */
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
				// Damage is flat ONLY. Hypixel's announcement says "+X & +Yx Defense and +Z Damage" and the wiki
				// leaves Damage's percent empty.
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

	/** What one type grants, in announcement order. Empty if no row. */
	public static List<Grant> grants(Utils.BlessingType type) {
		return TABLE.getOrDefault(type, List.of());
	}

	// ==================== what the stat pipeline reads ====================

	/** Product of every blessing factor on one stat, 1.0 if none. Over GRANTS, not types: Strength takes Power AND Time's. */
	public static double multiplier(Stat stat) {
		double product = 1.0;
		for(Map.Entry<Utils.BlessingType, List<Grant>> e : TABLE.entrySet()) {
			int level = Difficulty.blessingLevel(e.getKey());
			if(level <= 0) continue;
			for(Grant g : e.getValue()) if(g.modelled() == stat) product *= g.multiplierAt(level);
		}
		return product;
	}

	/** Sum of every blessing's flat on one stat. */
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

	/** One type's factor at its current level, as the announcement prints it. Same for every percent stat, so read off the first; 1.0 if none. */
	public static double multiplier(Utils.BlessingType type) {
		int level = Difficulty.blessingLevel(type);
		for(Grant g : grants(type)) if(g.percentPerLevel() != 0) return g.multiplierAt(level);
		return 1.0;
	}

	/** One type's flat on one stat at its current level, 0 if not granted. */
	public static double flat(Utils.BlessingType type, Stat stat) {
		int level = Difficulty.blessingLevel(type);
		double sum = 0;
		for(Grant g : grants(type)) if(g.modelled() == stat) sum += g.flatAt(level);
		return sum;
	}

	/**
	 * Stat half of the "DUNGEON BUFF!" announcement, as MiniMessage. Generated from the table so it can't disagree
	 * with what was applied and follows the mayor. Each stat reads "+flat & +Nx Name", "+flat Name" or "+Nx Name".
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
