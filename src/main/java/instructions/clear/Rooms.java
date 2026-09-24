package instructions.clear;

import org.bukkit.Location;
import plugin.Utils.BlessingType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of the static F7 dungeon: 16 rooms (cells, Y extents, minibosses, blessings), 15 doors, 47 scored
 * secrets plus the two uncounted Ice-Fill reward chests. Also owns grid coordinate math ({@link #roomAt}).
 *
 * <p>Grid: 32-block pitch (31 room + 1 buffer), world {@code (-10,-10)} = cell (0,0); cell {@code (gx,gz)} is
 * {@code X ∈ [-40-32gx, -10-32gx]}, same for Z. Spans X/Z −10..−200, except Start runs back to {@code Z -213}
 * ({@link #ANNEXES}): a room isn't only its cells.
 */
public final class Rooms {
	public static final int ORIGIN = -10;
	public static final int PITCH = 32;

	private static final List<Room> ALL = new ArrayList<>();
	private static final Map<Long, Room> BY_CELL = new HashMap<>();
	private static final Map<String, Room> BY_NAME = new HashMap<>();

	public static final Room QUIZ, ICE_FILL, WIZARD, TRAP, YELLOW, BLOOD, START;

	/**
	 * {@code minY}/{@code maxY}: see {@link Room#minY}. {@code level} is depth ({@link Room#level}); every room needs
	 * one since an Angry Archaeologist can spawn anywhere.
	 * <p>
	 * The eight NORMAL miniboss rooms use the depths the dungeon shows. The other seven are hand-written (MAP.md §5:
	 * grid adjacency isn't the door graph, so deriving them would be wrong). Yellow's II is pinned by its Shadow
	 * Assassin: 140M x 1.10 = 154M.
	 */
	private static Room reg(String name, RoomType type, int[][] cells, int minY, int maxY, int level, boolean hasMiniboss, Blessing... blessings) {
		Room r = new Room(name, type, cells, minY, maxY, level, hasMiniboss, blessings);
		ALL.add(r);
		BY_NAME.put(name, r);
		for(int[] c : cells) BY_CELL.put(key(c[0], c[1]), r);
		return r;
	}

	static {
		QUIZ = reg("Quiz", RoomType.PUZZLE, new int[][]{{0, 0}}, 46, 105, 5, false, new Blessing(BlessingType.TIME, 5));
		reg("Hallway", RoomType.NORMAL, new int[][]{{1, 0}, {2, 0}, {3, 0}, {4, 0}}, 66, 99, 5, true, new Blessing(BlessingType.LIFE, 5))
				.addSecret(Secret.chest(-140, 69, -37))
				.addSecret(Secret.chest(-114, 69, -35))
				.addSecret(Secret.chest(-83, 85, -34));
		reg("Market", RoomType.NORMAL, new int[][]{{5, 0}, {4, 1}, {5, 1}}, 56, 99, 4, true, new Blessing(BlessingType.STONE, 5))
				.addSecret(Secret.chest(-186, 79, -26))
				.addSecret(Secret.chest(-186, 61, -40))
				.addSecret(Secret.chest(-194, 86, -56))
				.addSecret(Secret.essence(-182, 59, -65))
				.addSecret(Secret.bat(-181.5, 63, -61.5));
		reg("Gravel", RoomType.NORMAL, new int[][]{{0, 1}, {1, 1}, {2, 1}}, 56, 99, 3, true, new Blessing(BlessingType.LIFE, 5))
				.addSecret(Secret.chest(-69, 69, -61))
				.addSecret(Secret.bat(-68.5, 91, -57.5))
				.addSecret(Secret.essence(-12, 69, -69))
				.addSecret(Secret.item(-16.5, 69, -63.5))
				.addSecret(Secret.essence(-38, 87, -58))
				.addSecret(Secret.item(-36.5, 87, -59.5));
		BLOOD = reg("Blood", RoomType.BLOOD, new int[][]{{3, 1}}, 66, 99, 5, false,
				new Blessing(BlessingType.POWER, 5), new Blessing(BlessingType.LIFE, 5));
		reg("Museum", RoomType.NORMAL, new int[][]{{4, 2}, {5, 2}, {4, 3}, {5, 3}}, 58, 119, 3, true, new Blessing(BlessingType.WISDOM, 5))
				.addSecret(Secret.chest(-169, 70, -134))
				.addSecret(Secret.essence(-180, 93, -127))
				.addSecret(Secret.blessingChest(-172, 83, -85, BlessingType.STONE, 2))
				.addSecret(Secret.mimicChest(-169, 70, -83))
				.addSecret(Secret.blessingChest(-186, 62, -80, BlessingType.STONE, 2));
		reg("Deathmite", RoomType.NORMAL, new int[][]{{1, 2}, {2, 2}, {3, 2}}, 56, 99, 2, true, new Blessing(BlessingType.POWER, 5))
				.addSecret(Secret.item(-130.5, 69, -79.5))
				.addSecret(Secret.blessingChest(-111, 60, -84, BlessingType.LIFE, 2))
				.addSecret(Secret.blessingChest(-109, 82, -89, BlessingType.LIFE, 2))
				.addSecret(Secret.blessingChest(-125, 92, -101, BlessingType.LIFE, 2))
				.addSecret(Secret.bat(-123.5, 95, -98.5))
				.addSecret(Secret.chest(-54, 69, -89));
		reg("Dino Dig Site", RoomType.NORMAL, new int[][]{{0, 2}, {0, 3}, {1, 3}}, 36, 101, 4, true, new Blessing(BlessingType.LIFE, 5))
				.addSecret(Secret.chest(-34, 92, -103))
				.addSecret(Secret.bat(-17.5, 47, -106.5))
				.addSecret(Secret.item(-55.5, 57, -110.5))
				.addSecret(Secret.chest(-64, 52, -125));
		WIZARD = reg("Wizard", RoomType.NORMAL, new int[][]{{2, 3}, {2, 4}, {2, 5}}, 46, 99, 2, false);
		WIZARD.addSecret(Secret.item(-94.5, 76, -196.5))
				.addSecret(Secret.chest(-100, 92, -183))
				.addSecret(Secret.bat(-83.5, 53, -179.5))
				.addSecret(Secret.chest(-98, 89, -110));
		reg("Fairy", RoomType.FAIRY, new int[][]{{3, 3}}, 62, 99, 2, false);
		reg("Well", RoomType.NORMAL, new int[][]{{0, 4}, {0, 5}, {1, 5}}, 53, 119, 3, true, new Blessing(BlessingType.LIFE, 5))
				.addSecret(Secret.blessingChest(-70, 89, -185, BlessingType.WISDOM, 2))
				.addSecret(Secret.item(-68.5, 91, -173.5))
				.addSecret(Secret.blessingChest(-22, 88, -188, BlessingType.WISDOM, 1))
				.addSecret(Secret.essence(-12, 95, -197))
				.addSecret(Secret.essence(-17, 95, -194))
				.addSecret(Secret.chest(-29, 91, -163))
				.addSecret(Secret.item(-22.5, 57, -154.5));
		ICE_FILL = reg("Ice Fill", RoomType.PUZZLE, new int[][]{{1, 4}}, 55, 90, 3, false);
		// Power-V reward chests, revealed on solve, claimed by hand. Not in the 47.
		ICE_FILL.addSecret(Secret.rewardChest(-71, 75, -152, BlessingType.POWER, 5))
				.addSecret(Secret.rewardChest(-71, 75, -154, BlessingType.POWER, 5));
		reg("Red Blue", RoomType.NORMAL, new int[][]{{3, 4}, {4, 4}, {5, 4}}, 66, 99, 1, true, new Blessing(BlessingType.POWER, 5))
				.addSecret(Secret.essence(-185, 83, -153))
				.addSecret(Secret.item(-165.5, 86, -159.5))
				.addSecret(Secret.blessingChest(-145, 90, -164, BlessingType.POWER, 2))
				.addSecret(Secret.chest(-163, 70, -143));
		// Start is more than its cell, see ANNEXES.
		START = reg("Start", RoomType.START, new int[][]{{3, 5}}, 66, 98, 1, false);
		TRAP = reg("Trap", RoomType.TRAP, new int[][]{{4, 5}}, 60, 100, 2, false);
		// Opening the Power-II chest gives Trap its white check (ClearManager).
		TRAP.addSecret(Secret.chest(-143, 67, -182))
				.addSecret(Secret.bat(-158.5, 92, -190.5))
				.addSecret(Secret.blessingChest(-164, 90, -184, BlessingType.POWER, 2));
		// Depth II per MAP.md §5: Shadow Assassin 140M x 1.10 = 154M.
		YELLOW = reg("Yellow", RoomType.YELLOW, new int[][]{{5, 5}}, 64, 99, 2, true, new Blessing(BlessingType.WISDOM, 5));
	}

	private Rooms() {
	}

	private static long key(int gx, int gz) {
		return ((long) gx << 8) | (gz & 0xFF);
	}

	public static List<Room> all() {
		return ALL;
	}

	public static Room byName(String name) {
		return BY_NAME.get(name);
	}

	public static Room byCell(int gx, int gz) {
		return BY_CELL.get(key(gx, gz));
	}

	/** Null outside the grid or in a buffer. */
	public static Room roomAt(Location loc) {
		int[] cell = cellAt(loc.getX(), loc.getZ());
		return cell == null ? null : byCell(cell[0], cell[1]);
	}

	/**
	 * Room whose footprint contains block column (x,z), or null. Footprint = own cells, plus seams between cells of
	 * the same room (so walking Hallway or Museum never leaves it), plus {@link #ANNEXES}. A seam between two
	 * different rooms is a crevice, crossable only by a door.
	 * <p>
	 * Not {@code cellAt}, which returns null for every seam: the bounds test on that killed players walking between
	 * a room's cells (Hallway crosses x -73, -105, -137).
	 * <p>
	 * Not {@link #roomAt} either: that uses unfloored X/Z, so a player at {@code x = -168.5} ({@code ox = 30.5})
	 * reads as the next room while their block ({@code x = -169}) is the seam. Out-of-bounds must go by block.
	 */
	public static Room roomAtBlock(int worldX, int worldZ) {
		Room r = gridRoomAt(worldX, worldZ);
		return r != null ? r : annexRoomAt(worldX, worldZ);
	}

	/** Footprint lookup for the on-grid part. */
	private static Room gridRoomAt(int worldX, int worldZ) {
		long dx = (long) ORIGIN - worldX;
		long dz = (long) ORIGIN - worldZ;
		if(dx < 0 || dz < 0) return null;
		int gx = (int) (dx / PITCH);
		int gz = (int) (dz / PITCH);
		if(gx > 5 || gz > 5) return null;
		boolean xSeam = (int) (dx - (long) gx * PITCH) == 31; // between gx and gx+1
		boolean zSeam = (int) (dz - (long) gz * PITCH) == 31; // between gz and gz+1
		Room r = byCell(gx, gz);
		if(r == null) return null;
		// A seam is the room's only if every cell it touches is that room.
		if(xSeam && byCell(gx + 1, gz) != r) return null;
		if(zSeam && byCell(gx, gz + 1) != r) return null;
		if(xSeam && zSeam && byCell(gx + 1, gz + 1) != r) return null;
		return r;
	}

	/** Off-grid X/Z rectangle of {@code room}. Y stays the room's minY..maxY, so it only widens horizontally. */
	private record Annex(Room room, int minX, int minZ, int maxX, int maxZ) {
		boolean contains(int x, int z) {
			return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
		}
	}

	private static final List<Annex> ANNEXES = new ArrayList<>();

	static {
		// Start's cell (3,5) ends at Z -200 but the room runs to Z -213, the entrance behind spawn (Z -183.5).
		// Without this, standing where players spawn was out of bounds. Full cell width in X: only Z is known to
		// differ, and a few extra rock columns counted as inside cost nothing (same call as the door frames).
		ANNEXES.add(new Annex(START, cellMinX(3), -213, cellMaxX(3), cellMinZ(5) - 1));
	}

	private static Room annexRoomAt(int worldX, int worldZ) {
		for(Annex a : ANNEXES) {
			if(a.contains(worldX, worldZ)) return a.room();
		}
		return null;
	}

	/** Inside the grid rectangle, room or buffer (not e.g. the boss arena). Unlike {@link #roomAt}, stays true in gaps. */
	public static boolean inGrid(Location loc) {
		double dx = ORIGIN - loc.getX();
		double dz = ORIGIN - loc.getZ();
		if(dx < 0 || dz < 0) return false;
		return (int) (dx / PITCH) <= 5 && (int) (dz / PITCH) <= 5;
	}

	/** {@code {gx,gz}}, or null outside the grid or in a buffer. */
	public static int[] cellAt(double worldX, double worldZ) {
		double dx = ORIGIN - worldX;
		double dz = ORIGIN - worldZ;
		if(dx < 0 || dz < 0) return null;
		int gx = (int) (dx / PITCH);
		int gz = (int) (dz / PITCH);
		if(gx > 5 || gz > 5) return null;
		double ox = dx - (double) gx * PITCH;
		double oz = dz - (double) gz * PITCH;
		if(ox >= 31 || oz >= 31) return null; // buffer
		return new int[]{gx, gz};
	}

	public static int cellMaxX(int gx) { return ORIGIN - PITCH * gx; }        // e.g. gx0 → -10
	public static int cellMinX(int gx) { return cellMaxX(gx) - 30; }          // e.g. gx0 → -40
	public static int cellMaxZ(int gz) { return ORIGIN - PITCH * gz; }
	public static int cellMinZ(int gz) { return cellMaxZ(gz) - 30; }

	/**
	 * Cell pairs each door joins. Grid adjacency isn't the door graph, so this is the only record of what connects.
	 * Used by {@link DungeonMap}, {@link #inDoor} (in bounds) and {@code CustomItems.onBlockBreak} (unbreakable frames).
	 */
	public static final int[][][] DOORS = {
			{{0, 0}, {0, 1}}, {{4, 0}, {5, 0}}, {{5, 1}, {5, 2}}, {{3, 2}, {4, 2}}, {{3, 2}, {3, 1}},
			{{3, 2}, {3, 3}}, {{2, 2}, {2, 1}}, {{0, 1}, {0, 2}}, {{3, 3}, {3, 4}}, {{3, 4}, {3, 5}},
			{{4, 4}, {4, 5}}, {{5, 4}, {5, 5}}, {{3, 3}, {2, 3}}, {{2, 5}, {1, 5}}, {{0, 4}, {1, 4}}
	};

	/** Every door is 3 wide x 4 tall at the same Y floor-wide. Matches the three {@code Server} door fill regions. */
	private static final int DOOR_MIN_Y = 69, DOOR_MAX_Y = 72;

	/**
	 * Door box grown one block to include the frame: 3 across the seam, 5 along the wall, 6 tall. In bounds anywhere
	 * inside, and nothing inside breaks during a run.
	 */
	public record Door(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		public boolean contains(int x, int y, int z) {
			return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
		}
	}

	private static final List<Door> DOOR_BOXES = new ArrayList<>();

	static {
		for(int[][] d : DOORS) DOOR_BOXES.add(doorBox(d[0], d[1]));
	}

	/**
	 * Derived, not listed: every door sits in the seam, centred 15 blocks in on the shared grid edge (not a
	 * multi-cell room's centre: Start door is at {@code x -121}, cell gx=3's centre, though Red Blue spans 3 cells).
	 * <p>
	 * Checked against {@code Server}'s fill regions: Start {@code {3,5}-{3,4}} → {@code x -122..-120, z -170..-168},
	 * wither {@code {3,2}-{3,3}} → {@code z -106..-104}, blood {@code {3,1}-{3,2}} → {@code z -74..-72}, all
	 * {@code y 69..72}.
	 */
	private static Door doorBox(int[] a, int[] b) {
		if(a[1] == b[1]) { // horizontal neighbours: seam is an X column, wall runs along Z
			int seamX = cellMinX(Math.min(a[0], b[0])) - 1;
			int zc = cellMaxZ(a[1]) - 15;
			return new Door(seamX - 1, DOOR_MIN_Y - 1, zc - 2, seamX + 1, DOOR_MAX_Y + 1, zc + 2);
		}
		int seamZ = cellMinZ(Math.min(a[1], b[1])) - 1;
		int xc = cellMaxX(a[0]) - 15;
		return new Door(xc - 2, DOOR_MIN_Y - 1, seamZ - 1, xc + 2, DOOR_MAX_Y + 1, seamZ + 1);
	}

	/** Frame included. */
	public static boolean inDoor(int worldX, int worldY, int worldZ) {
		for(Door d : DOOR_BOXES) {
			if(d.contains(worldX, worldY, worldZ)) return true;
		}
		return false;
	}

	/** Block column in a footprint ({@link #roomAtBlock}) and block Y within that room's minY..maxY. */
	public static boolean inRoomBounds(Location loc) {
		Room r = roomAtBlock(loc.getBlockX(), loc.getBlockZ());
		return r != null && loc.getBlockY() >= r.minY && loc.getBlockY() <= r.maxY;
	}

	/**
	 * At or above {@link Room#maxY}: roof, whether the layer sits at maxY or just above. False off-grid and in
	 * seams (out of bounds anyway).
	 */
	public static boolean isCeiling(int worldX, int worldY, int worldZ) {
		Room r = roomAtBlock(worldX, worldZ);
		return r != null && worldY >= r.maxY;
	}

	/**
	 * Column is on a room's outer perimeter wall, any height. Stops breaking through walls (ceiling has its own
	 * run-gated rule, {@link #isCeiling}). Internal seams of a multi-cell room are interior and stay breakable,
	 * except where one meets the outer wall. Crevices and off-grid return false.
	 */
	public static boolean isRoomFace(int worldX, int worldZ) {
		Room r = roomAtBlock(worldX, worldZ);
		if(r == null) return false;
		// perimeter: a horizontal neighbour is outside the footprint
		return roomAtBlock(worldX + 1, worldZ) != r || roomAtBlock(worldX - 1, worldZ) != r
				|| roomAtBlock(worldX, worldZ + 1) != r || roomAtBlock(worldX, worldZ - 1) != r;
	}

	public static void reset() {
		for(Room r : ALL) r.reset();
	}
}
