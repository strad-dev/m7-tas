package plugin;

import java.util.Locale;

/**
 * <b>Bad Bonzo</b>: an experimental dungeon setting. On, the Bonzo Staff's wind charge spawns 1 tick after the click
 * (read by {@code items/combat/BonzoStaff.bonzo}). Leaderboards still take the run.
 */
public enum BadBonzo {
	/** Wind charge spawns on the click tick. Default. */
	OFF,
	/** Wind charge spawns 1 tick after the click. */
	ON;

	private static BadBonzo current = OFF;

	public static BadBonzo current() {
		return current;
	}

	public static void set(BadBonzo b) {
		if(b != null) current = b;
	}

	public static boolean enabled() {
		return current == ON;
	}

	/** Next value, wrapping. */
	public static BadBonzo toggle() {
		BadBonzo[] all = values();
		set(all[(current.ordinal() + 1) % all.length]);
		return current;
	}

	/** Previous value, wrapping: a right-click on the menu button. */
	public static BadBonzo toggleBack() {
		BadBonzo[] all = values();
		set(all[(current.ordinal() + all.length - 1) % all.length]);
		return current;
	}

	/** Parse any case, or null. Accepts {@link #id()} (the network's ids) and hand-typed spellings. */
	public static BadBonzo parse(String s) {
		if(s == null) return null;
		return switch(s.toLowerCase(Locale.ROOT)) {
			case "on", "true", "enabled", "yes" -> ON;
			case "off", "false", "disabled", "no" -> OFF;
			default -> null;
		};
	}

	/** Lower-case id for the run payload and the network's stored setting. Must match the network's. */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}
}
