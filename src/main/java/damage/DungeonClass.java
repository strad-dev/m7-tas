package damage;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

/**
 * The five dungeon classes.  Which one a player is decides their equipment set (§1.11), their Accessory Power
 * (§1.12) and their class bonuses (§1.14).
 * <p>
 * Identification mirrors {@code CustomItems.isMageClass}: a real player carries an exclusive class scoreboard tag
 * set by {@code /class}, and a fake player carries none and is identified by name (all four {@code MageN} fakes run
 * the Mage inventory and cast Mage abilities, so every {@code Mage*}-named fake counts as a Mage).
 */
public enum DungeonClass {
	MAGE, ARCHER, BERSERK, HEALER, TANK;

	/**
	 * The class {@code p} is playing.  A class scoreboard tag wins; failing that the player's NAME is matched, which
	 * is how the fake players are identified ({@code Mage1}-{@code Mage4} and {@code Archer}).  Defaults to MAGE,
	 * matching the beam gate every other class check in the plugin falls back to.
	 */
	public static DungeonClass of(Player p) {
		if(p == null) return MAGE;
		for(DungeonClass c : values()) {
			if(p.getScoreboardTags().contains(name(c))) return c;
		}
		for(DungeonClass c : values()) {
			if(p.getName().startsWith(name(c))) return c;
		}
		return MAGE;
	}

	/** The scoreboard tag / loadout role name for a class, e.g. {@code "Berserk"}. */
	public static String name(DungeonClass c) {
		String n = c.name().toLowerCase(java.util.Locale.ROOT);
		return Character.toUpperCase(n.charAt(0)) + n.substring(1);
	}

	/**
	 * True if {@code p} is the ONLY player in the party on this class, which is the bracketed "solo" column of
	 * every §1.14 table.  Party composition, so it has to be evaluated live: {@code /practice} parties are
	 * whatever players picked with {@code /class}, and baking in the old fake-player layout (four Mages and one
	 * Archer, so the Archer was always solo) would be wrong the moment a real party runs.
	 * <p>
	 * Spectators are not on the team, so they do not count towards anyone's class.
	 */
	public static boolean isSoloOnClass(Player p) {
		DungeonClass mine = of(p);
		int count = 0;
		for(Player other : Bukkit.getOnlinePlayers()) {
			if(other.getGameMode() == GameMode.SPECTATOR) continue;
			if(of(other) == mine) count++;
		}
		return count <= 1;
	}

	/**
	 * The damage path this class's aggregate is shown at by default, for readouts that have to pick one
	 * ({@code /eq}, item lore).  A Mage has no melee path at all - the Mage Staff passive turns every melee attack
	 * into the beam (§1.11) - so the beam set always applies to them.
	 */
	public DamagePath primaryPath() {
		return switch(this) {
			case MAGE -> DamagePath.BEAM;
			case ARCHER -> DamagePath.BOW;
			default -> DamagePath.MELEE;
		};
	}
}
