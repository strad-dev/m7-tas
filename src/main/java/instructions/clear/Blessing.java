package instructions.clear;

import plugin.Utils;

/**
 * A dungeon blessing award (type + roman level). Used both to describe a room's clear reward / a chest's
 * contents, and as the key for {@link ClearManager}'s blessing tally (which is tracked for future use even
 * though nothing consumes it in gameplay yet).
 */
public record Blessing(Utils.BlessingType type, int level) {
	public static Blessing of(Utils.BlessingType type, int level) {
		return new Blessing(type, level);
	}
}
