package instructions.bosses;

import plugin.Utils;

/**
 * Walls between boss arenas. Each {@code open*} fires from the dying boss's death sequence at the noted tick
 * offset after the killing blow. {@link #resetAll()} restores them on every setup, from {@code Server.serverSetup}.
 */
public final class BossTransition {
	private BossTransition() {}

	/** Maxor → Storm: 100t after Maxor dies. */
	public static void openMaxorToStorm() {
		Utils.runCommand("fill 68 220 49 78 220 32 minecraft:air");
	}

	/** Storm → Goldor: 100t after Storm dies. red_terracotta (y168) over barrier (y167), four sea lanterns embedded. */
	public static void openStormToGoldor() {
		Utils.runCommand("fill 95 168 36 105 168 46 minecraft:air replace minecraft:red_terracotta");
		Utils.runCommand("fill 95 167 36 105 167 46 minecraft:air replace minecraft:barrier");
		Utils.runCommand("setblock 100 168 45 minecraft:air");
		Utils.runCommand("setblock 96 168 41 minecraft:air");
		Utils.runCommand("setblock 100 168 37 minecraft:air");
		Utils.runCommand("setblock 104 168 41 minecraft:air");
	}

	/** Goldor → Necron: 100t after Goldor dies. */
	public static void openGoldorToNecron() {
		Utils.runCommand("fill 51 113 111 57 113 117 minecraft:air");
	}

	/** Necron → Wither King: 200t after Necron dies. */
	public static void openNecronToWitherKing() {
		Utils.runCommand("fill 61 63 83 47 64 69 minecraft:air");
	}

	/** Restore every transition wall to its pre-death state. Called from {@code Server.serverSetup}. */
	public static void resetAll() {
		// Maxor → Storm: buried copy at y=-1.
		Utils.runCommand("clone 68 -1 49 78 -1 32 68 220 32");
		// Storm → Goldor: refill, then the sea lanterns last since the fill puts red_terracotta in their spots.
		Utils.runCommand("fill 95 168 36 105 168 46 minecraft:red_terracotta replace minecraft:air");
		Utils.runCommand("fill 95 167 36 105 167 46 minecraft:barrier replace minecraft:air");
		Utils.runCommand("setblock 100 168 45 minecraft:sea_lantern");
		Utils.runCommand("setblock 96 168 41 minecraft:sea_lantern");
		Utils.runCommand("setblock 100 168 37 minecraft:sea_lantern");
		Utils.runCommand("setblock 104 168 41 minecraft:sea_lantern");
		// Goldor → Necron: buried copy at y=-11.
		Utils.runCommand("clone 51 -11 111 57 -11 117 51 113 111");
		// Necron → Wither King: buried copy at y=-2..-1.
		Utils.runCommand("clone 61 -2 83 47 -1 69 47 63 69");
	}
}
