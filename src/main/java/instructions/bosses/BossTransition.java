package instructions.bosses;

import plugin.Utils;

/**
 * Boss-to-boss arena transitions. When a boss dies, the wall/floor separating its arena from the next
 * boss's arena opens (fills with air) after a fixed delay; on the next setup ({@code /reset}, {@code /tas},
 * {@code /practice}) every wall is restored via {@link #resetAll()} from {@code Server.serverSetup}.
 *
 * <p>Each {@code open*} is fired from the dying boss's death sequence at the tick offset noted below
 * (relative to the killing blow). Resets clone a buried copy of the original blocks back into place — or,
 * for Storm's mixed wall, refill the original block types and re-place the embedded sea lanterns.
 */
public final class BossTransition {
	private BossTransition() {}

	/** Maxor → Storm: 100t after Maxor dies. */
	public static void openMaxorToStorm() {
		Utils.runCommand("fill 68 220 49 78 220 32 minecraft:air");
	}

	/** Storm → Goldor: 100t after Storm dies. A red_terracotta wall (y168) over barrier (y167), plus four embedded sea lanterns. */
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
		// Maxor → Storm: clone the buried y=-1 slab back up to y=220.
		Utils.runCommand("clone 68 -1 49 78 -1 32 68 220 32");
		// Storm → Goldor: refill the original block types, then re-place the embedded sea lanterns (the fill would
		// otherwise overwrite their positions with red_terracotta), so the sea lanterns are set last.
		Utils.runCommand("fill 95 168 36 105 168 46 minecraft:red_terracotta replace minecraft:air");
		Utils.runCommand("fill 95 167 36 105 167 46 minecraft:barrier replace minecraft:air");
		Utils.runCommand("setblock 100 168 45 minecraft:sea_lantern");
		Utils.runCommand("setblock 96 168 41 minecraft:sea_lantern");
		Utils.runCommand("setblock 100 168 37 minecraft:sea_lantern");
		Utils.runCommand("setblock 104 168 41 minecraft:sea_lantern");
		// Goldor → Necron: clone the buried y=-11 slab back up to y=113.
		Utils.runCommand("clone 51 -11 111 57 -11 117 51 113 111");
		// Necron → Wither King: clone the buried y=-2..-1 two-layer block back up to y=63..64.
		Utils.runCommand("clone 61 -2 83 47 -1 69 47 63 69");
	}
}
