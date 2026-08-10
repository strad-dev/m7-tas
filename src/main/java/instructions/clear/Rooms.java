package instructions.clear;

import org.bukkit.Location;
import plugin.Utils.BlessingType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Static registry of the whole static F7 dungeon: the 16 rooms, their cells / minibosses / blessings, and all
 * 47 scored secrets (plus the two Ice-Fill reward chests, which are not counted). Also owns the room-grid
 * coordinate math (see {@link #roomAt}).
 *
 * <p>Grid: 32-block pitch (31-block room + 1-block buffer), origin at world {@code (-10,-10)} = cell (0,0);
 * cell {@code (gx,gz)} occupies {@code X ∈ [-40-32gx, -10-32gx]}, likewise Z. The dungeon spans X/Z −10..−200.
 */
public final class Rooms {
	public static final int ORIGIN = -10;
	public static final int PITCH = 32;

	private static final List<Room> ALL = new ArrayList<>();
	private static final Map<Long, Room> BY_CELL = new HashMap<>();
	private static final Map<String, Room> BY_NAME = new HashMap<>();

	// Handy references for the puzzle / special-room logic.
	public static final Room QUIZ, ICE_FILL, WIZARD, TRAP, YELLOW, BLOOD;

	private static Room reg(String name, RoomType type, int[][] cells, int level, boolean hasMiniboss, Blessing... blessings) {
		Room r = new Room(name, type, cells, level, hasMiniboss, blessings);
		ALL.add(r);
		BY_NAME.put(name, r);
		for(int[] c : cells) BY_CELL.put(key(c[0], c[1]), r);
		return r;
	}

	static {
		QUIZ = reg("Quiz", RoomType.PUZZLE, new int[][]{{0, 0}}, 0, false, new Blessing(BlessingType.TIME, 5));
		reg("Hallway", RoomType.NORMAL, new int[][]{{1, 0}, {2, 0}, {3, 0}, {4, 0}}, 5, true, new Blessing(BlessingType.LIFE, 5))
				.addSecret(Secret.chest(-140, 69, -37))
				.addSecret(Secret.chest(-114, 69, -35))
				.addSecret(Secret.chest(-83, 85, -34));
		reg("Market", RoomType.NORMAL, new int[][]{{5, 0}, {4, 1}, {5, 1}}, 4, true, new Blessing(BlessingType.STONE, 5))
				.addSecret(Secret.chest(-186, 79, -26))
				.addSecret(Secret.chest(-186, 61, -40))
				.addSecret(Secret.chest(-194, 86, -56))
				.addSecret(Secret.essence(-182, 59, -65))
				.addSecret(Secret.bat(-181.5, 63, -61.5));
		reg("Gravel", RoomType.NORMAL, new int[][]{{0, 1}, {1, 1}, {2, 1}}, 3, true, new Blessing(BlessingType.LIFE, 5))
				.addSecret(Secret.chest(-69, 69, -61))
				.addSecret(Secret.bat(-68.5, 91, -57.5))
				.addSecret(Secret.essence(-12, 69, -69))
				.addSecret(Secret.item(-16.5, 69, -63.5))
				.addSecret(Secret.essence(-38, 87, -58))
				.addSecret(Secret.item(-36.5, 87, -59.5));
		BLOOD = reg("Blood", RoomType.BLOOD, new int[][]{{3, 1}}, 0, false,
				new Blessing(BlessingType.POWER, 5), new Blessing(BlessingType.LIFE, 5));
		reg("Museum", RoomType.NORMAL, new int[][]{{4, 2}, {5, 2}, {4, 3}, {5, 3}}, 3, true, new Blessing(BlessingType.WISDOM, 5))
				.addSecret(Secret.blessingChest(-169, 70, -134, BlessingType.STONE, 2))
				.addSecret(Secret.essence(-180, 93, -127))
				.addSecret(Secret.blessingChest(-172, 83, -85, BlessingType.STONE, 2))
				.addSecret(Secret.mimicChest(-169, 70, -83))
				.addSecret(Secret.chest(-186, 62, -80));
		reg("Deathmite", RoomType.NORMAL, new int[][]{{1, 2}, {2, 2}, {3, 2}}, 2, true, new Blessing(BlessingType.POWER, 5))
				.addSecret(Secret.item(-130.5, 69, -79.5))
				.addSecret(Secret.blessingChest(-111, 60, -84, BlessingType.LIFE, 2))
				.addSecret(Secret.blessingChest(-109, 82, -89, BlessingType.LIFE, 2))
				.addSecret(Secret.blessingChest(-125, 92, -101, BlessingType.LIFE, 2))
				.addSecret(Secret.bat(-123.5, 95, -98.5))
				.addSecret(Secret.chest(-54, 69, -89));
		reg("Dino Dig Site", RoomType.NORMAL, new int[][]{{0, 2}, {0, 3}, {1, 3}}, 4, true, new Blessing(BlessingType.LIFE, 5))
				.addSecret(Secret.chest(-34, 92, -103))
				.addSecret(Secret.bat(-17.5, 47, -106.5))
				.addSecret(Secret.item(-55.5, 57, -110.5))
				.addSecret(Secret.chest(-64, 52, -125));
		WIZARD = reg("Wizard", RoomType.NORMAL, new int[][]{{2, 3}, {2, 4}, {2, 5}}, 0, false);
		WIZARD.addSecret(Secret.item(-94.5, 76, -196.5))
				.addSecret(Secret.blessingChest(-100, 92, -183, BlessingType.WISDOM, 2))
				.addSecret(Secret.bat(-83.5, 53, -179.5))
				.addSecret(Secret.blessingChest(-98, 89, -110, BlessingType.WISDOM, 1));
		reg("Fairy", RoomType.FAIRY, new int[][]{{3, 3}}, 0, false);
		reg("Well", RoomType.NORMAL, new int[][]{{0, 4}, {0, 5}, {1, 5}}, 3, true, new Blessing(BlessingType.LIFE, 5))
				.addSecret(Secret.chest(-70, 89, -185))
				.addSecret(Secret.item(-68.5, 91, -173.5))
				.addSecret(Secret.chest(-22, 88, -188))
				.addSecret(Secret.essence(-12, 95, -197))
				.addSecret(Secret.essence(-17, 95, -194))
				.addSecret(Secret.chest(-29, 91, -163))
				.addSecret(Secret.item(-22.5, 57, -154.5));
		ICE_FILL = reg("Ice Fill", RoomType.PUZZLE, new int[][]{{1, 4}}, 0, false);
		// The two Power-V reward chests, revealed on solve but claimed by hand.  Not part of the 47.
		ICE_FILL.addSecret(Secret.rewardChest(-71, 75, -152, BlessingType.POWER, 5))
				.addSecret(Secret.rewardChest(-71, 75, -154, BlessingType.POWER, 5));
		reg("Red Blue", RoomType.NORMAL, new int[][]{{3, 4}, {4, 4}, {5, 4}}, 1, true, new Blessing(BlessingType.POWER, 5))
				.addSecret(Secret.essence(-185, 83, -153))
				.addSecret(Secret.item(-165.5, 86, -159.5))
				.addSecret(Secret.blessingChest(-145, 90, -164, BlessingType.POWER, 2))
				.addSecret(Secret.chest(-163, 70, -143));
		reg("Start", RoomType.START, new int[][]{{3, 5}}, 0, false);
		TRAP = reg("Trap", RoomType.TRAP, new int[][]{{4, 5}}, 0, false);
		// The Power-II chest whose opening earns Trap its white checkmark (see ClearManager).
		TRAP.addSecret(Secret.chest(-143, 67, -182))
				.addSecret(Secret.bat(-158.5, 92, -190.5))
				.addSecret(Secret.blessingChest(-164, 90, -184, BlessingType.POWER, 2));
		YELLOW = reg("Yellow", RoomType.YELLOW, new int[][]{{5, 5}}, 0, true, new Blessing(BlessingType.WISDOM, 5));
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

	/** The room a world location sits in, or {@code null} if outside the grid or in a between-room buffer. */
	public static Room roomAt(Location loc) {
		int[] cell = cellAt(loc.getX(), loc.getZ());
		return cell == null ? null : byCell(cell[0], cell[1]);
	}

	/** Whether a location sits within the overall room-grid rectangle, i.e. inside a room OR a 1-block between-room
	*  buffer, as opposed to fully outside the grid such as the boss arena.  Unlike {@link #roomAt}, this stays
	 *  true while walking across the gap between two rooms. */
	public static boolean inGrid(Location loc) {
		double dx = ORIGIN - loc.getX();
		double dz = ORIGIN - loc.getZ();
		if(dx < 0 || dz < 0) return false;
		return (int) (dx / PITCH) <= 5 && (int) (dz / PITCH) <= 5;
	}

	/** The grid cell {@code {gx,gz}} for a world (x,z), or {@code null} if outside the grid / in a buffer. */
	public static int[] cellAt(double worldX, double worldZ) {
		double dx = ORIGIN - worldX;
		double dz = ORIGIN - worldZ;
		if(dx < 0 || dz < 0) return null;
		int gx = (int) (dx / PITCH);
		int gz = (int) (dz / PITCH);
		if(gx > 5 || gz > 5) return null;
		double ox = dx - (double) gx * PITCH;
		double oz = dz - (double) gz * PITCH;
		if(ox >= 31 || oz >= 31) return null; // 1-block buffer between rooms belongs to no room
		return new int[]{gx, gz};
	}

	// --- cell → world bounds (used by the map renderer & puzzle/ice scans) ---
	public static int cellMaxX(int gx) { return ORIGIN - PITCH * gx; }        // e.g. gx0 → -10
	public static int cellMinX(int gx) { return cellMaxX(gx) - 30; }          // e.g. gx0 → -40
	public static int cellMaxZ(int gz) { return ORIGIN - PITCH * gz; }
	public static int cellMinZ(int gz) { return cellMaxZ(gz) - 30; }

	/**
	 * True if the block column at world (x,z) sits on the vertical outer face (perimeter wall) of a room, at ANY
	 * height. Used to stop blocks being broken through room walls without touching the floor/ceiling (rooms have
	 * varying heights, so we can't restrict by Y). A room is only its own outer perimeter: for a multi-cell room
	 * (e.g. the 2x2 Museum) the internal seams between its cells count as interior, so the middle of the room is
	 * NOT a face and stays breakable. Between-room seams and everything off the grid return false.
	 */
	public static boolean isRoomFace(int worldX, int worldZ) {
		int[] cell = cellAt(worldX, worldZ);
		if(cell == null) return false; // between-room buffer or outside the grid, so not a room's own wall
		Room r = byCell(cell[0], cell[1]);
		if(r == null) return false;
		// A perimeter column has at least one of its four horizontal neighbours outside this room's footprint.
		return !inFootprint(worldX + 1, worldZ, r) || !inFootprint(worldX - 1, worldZ, r)
				|| !inFootprint(worldX, worldZ + 1, r) || !inFootprint(worldX, worldZ - 1, r);
	}

	/** Whether the column at (x,z) belongs to room {@code r}'s physical footprint: its own cells, plus the 1-block
	 *  seams that fall BETWEEN two cells of the same room (so multi-cell rooms read as one solid blob). */
	private static boolean inFootprint(int x, int z, Room r) {
		long dx = (long) ORIGIN - x;
		long dz = (long) ORIGIN - z;
		if(dx < 0 || dz < 0) return false;
		int gx = (int) (dx / PITCH);
		int gz = (int) (dz / PITCH);
		if(gx > 5 || gz > 5) return false;
		boolean xSeam = (int) (dx - (long) gx * PITCH) == 31; // seam between cell gx and gx+1
		boolean zSeam = (int) (dz - (long) gz * PITCH) == 31; // seam between cell gz and gz+1
		if(!xSeam && !zSeam) return byCell(gx, gz) == r;
		if(xSeam && !zSeam) return byCell(gx, gz) == r && byCell(gx + 1, gz) == r;
		if(!xSeam) return byCell(gx, gz) == r && byCell(gx, gz + 1) == r;
		return byCell(gx, gz) == r && byCell(gx + 1, gz) == r && byCell(gx, gz + 1) == r && byCell(gx + 1, gz + 1) == r;
	}

	public static void reset() {
		for(Room r : ALL) r.reset();
	}
}
