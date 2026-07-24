package instructions.clear;

import instructions.Server;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import plugin.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Interactive Ice Fill puzzle (grid 1,4). Three ice levels at y=69/70/71. For the active level the player must
 * turn every ice block to packed ice by walking a single continuous stroke — no diagonal moves, no doubling
 * back onto a frozen block, and no jumping to a non-adjacent block. Any violation breaks that level's layer and
 * resets it after 60 ticks. Completing all three levels → {@link ClearManager#puzzleSolved} (green check) and
 * reveals the two reward chests via {@link Server#openIceFillRewards()} (the Power-V blessings are claimed by
 * opening those chests, not automatically).
 */
public final class PuzzleIceFill {
	private PuzzleIceFill() {
	}

	private static final int GX = 1, GZ = 4;
	private static final int[] LEVEL_Y = {69, 70, 71};
	private static final int RESET_TICKS = 60;

	@SuppressWarnings("unchecked")
	private static final List<int[]>[] levels = new List[3];

	private static World world;
	private static int currentLevel;
	private static int[] previous; // coords of the last frozen block, or null
	private static int frozenCount;
	private static boolean solved, failing;
	private static java.util.UUID solver;
	private static int gen;

	public static void begin(World w) {
		world = w;
		gen++;
		currentLevel = 0;
		previous = null;
		frozenCount = 0;
		solved = failing = false;
		solver = null;
		int minX = Rooms.cellMinX(GX), maxX = Rooms.cellMaxX(GX);
		int minZ = Rooms.cellMinZ(GZ), maxZ = Rooms.cellMaxZ(GZ);
		for(int l = 0; l < 3; l++) {
			levels[l] = new ArrayList<>();
			int y = LEVEL_Y[l];
			for(int x = minX; x <= maxX; x++) {
				for(int z = minZ; z <= maxZ; z++) {
					Block b = w.getBlockAt(x, y, z);
					if(b.getType() == Material.ICE || b.getType() == Material.PACKED_ICE) {
						if(b.getType() == Material.PACKED_ICE) b.setType(Material.ICE, false); // fresh start
						levels[l].add(new int[]{x, y, z});
					}
				}
			}
		}
	}

	public static void stop() {
		gen++;
		if(world != null) {
			for(int l = 0; l < 3; l++) {
				if(levels[l] == null) continue;
				for(int[] c : levels[l]) {
					Block b = world.getBlockAt(c[0], c[1], c[2]);
					if(b.getType() == Material.PACKED_ICE || b.getType() == Material.AIR) b.setType(Material.ICE, false);
				}
				levels[l] = null;
			}
		}
	}

	public static void tick(World w, List<Player> players) {
		if(solved || failing || levels[0] == null) return;
		int y = LEVEL_Y[currentLevel];
		for(Player p : players) {
			if(solver != null && !p.getUniqueId().equals(solver)) continue;
			Block below = p.getLocation().getBlock().getRelative(0, -1, 0);
			if(below.getY() != y) continue;
			if(!inCurrentLevel(below.getX(), below.getZ())) continue;
			step(p, below);
		}
	}

	private static void step(Player p, Block below) {
		int[] here = {below.getX(), below.getY(), below.getZ()};
		if(previous != null && sameCoords(here, previous)) return; // standing on the last block
		if(below.getType() == Material.PACKED_ICE) { // already frozen → doubling back
			fail(p);
			return;
		}
		if(below.getType() != Material.ICE) return;
		if(previous == null) {
			solver = p.getUniqueId();
			freeze(p, below, here);
		} else if(adjacent(here, previous)) {
			freeze(p, below, here);
		} else {
			fail(p); // diagonal or disconnected
		}
	}

	private static void freeze(Player p, Block below, int[] here) {
		below.setType(Material.PACKED_ICE, false);
		previous = here;
		frozenCount++;
		float pitch = 1.0f + Math.min(1.0f, frozenCount / (float) Math.max(1, levels[currentLevel].size()));
		Utils.playLocalSound(p, Sound.BLOCK_NOTE_BLOCK_HARP, 1.5f, pitch);
		if(frozenCount >= levels[currentLevel].size()) levelComplete(p);
	}

	private static void levelComplete(Player p) {
		Utils.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_BELL, 1.5f, 1.0f + 0.25f * currentLevel);
		currentLevel++;
		previous = null;
		frozenCount = 0;
		solver = null;
		if(currentLevel >= 3) {
			solved = true;
			Bukkit.broadcast(Utils.msg("<aqua>Ice Fill <green>cleared!"));
			Server.openIceFillRewards();
			ClearManager.puzzleSolved(Rooms.ICE_FILL, p); // green check (reward chests are opened by hand)
		}
	}

	private static void fail(Player p) {
		failing = true;
		solver = null;
		previous = null;
		frozenCount = 0;
		Utils.playGlobalSound(Sound.ENTITY_ITEM_BREAK, 1.5f, 0.5f);
		Bukkit.broadcast(Utils.msg("<red>Ice Fill layer broke — resetting..."));
		final int level = currentLevel;
		final int g = gen;
		for(int[] c : levels[level]) world.getBlockAt(c[0], c[1], c[2]).setType(Material.AIR, false);
		Utils.scheduleTask(() -> {
			if(g != gen || solved) return;
			for(int[] c : levels[level]) {
				Block b = world.getBlockAt(c[0], c[1], c[2]);
				if(b.getType() == Material.AIR) b.setType(Material.ICE, false);
			}
			failing = false;
		}, RESET_TICKS);
	}

	private static boolean inCurrentLevel(int x, int z) {
		for(int[] c : levels[currentLevel]) if(c[0] == x && c[2] == z) return true;
		return false;
	}

	private static boolean adjacent(int[] a, int[] b) {
		return a[1] == b[1] && Math.abs(a[0] - b[0]) + Math.abs(a[2] - b[2]) == 1;
	}

	private static boolean sameCoords(int[] a, int[] b) {
		return a[0] == b[0] && a[1] == b[1] && a[2] == b[2];
	}
}
