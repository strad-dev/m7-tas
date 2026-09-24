package instructions.clear;

import plugin.Utils;

/**
 * Blessing award (type + roman level). Describes a room's clear reward or a chest's contents, and keys
 * {@link ClearManager}'s blessing tally (tracked for later; nothing consumes it yet).
 */
public record Blessing(Utils.BlessingType type, int level) {
	public static Blessing of(Utils.BlessingType type, int level) {
		return new Blessing(type, level);
	}
}
