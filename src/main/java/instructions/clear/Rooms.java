package instructions.clear;

import org.bukkit.Location;
import plugin.Utils.BlessingType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Static registry of the whole static F7 dungeon: the 16 rooms, their cells / vertical extents / minibosses /
 * blessings, the 15 doors between them, and all 47 scored secrets (plus the two Ice-Fill reward chests, which are
 * not counted). Also owns the room-grid coordinate math (see {@link #roomAt}).
 *
 * <p>Grid: 32-block pitch (31-block room + 1-block buffer), origin at world {@code (-10,-10)} = cell (0,0);
 * cell {@code (gx,gz)} occupies {@code X ∈ [-40-32gx, -10-32gx]}, likewise Z. The dungeon spans X/Z −10..−200,
 * <b>except</b> that Start runs back to {@code Z -213} - see {@link #ANNEXES}, and don't assume a room is only
 * its cells.
 */
public final class Rooms {
	public static final int ORIGIN = -10;
	public static final int PITCH = 32;

	private static final List<Room> ALL = new ArrayList<>();
	private static final Map<Long, Room> BY_CELL = new HashMap<>();
	private static final Map<String, Room> BY_NAME = new HashMap<>();

	// Handy references for the puzzle / special-room logic.
	public static final Room QUIZ, ICE_FILL, WIZARD, TRAP, YELLOW, BLOOD, START;

	/**
	 * Registers a room.  {@code minY}/{@code maxY} are its vertical extent (see {@link Room#minY}).  {@code level}
	 * is its DEPTH (see {@link Room#level}), which every mob in it scales its stats by, so every room needs one -
	 * an Angry Archaeologist can spawn anywhere.
	 * <p>
	 * The eight NORMAL miniboss rooms carry the depths the dungeon itself shows.  The other seven are hand-written
	 * (MAP.md §5 rules that the right approach, because grid adjacency is not the door graph, so deriving
	 * them geometrically would be wrong for exactly these rooms).  Yellow's II is the one the plan pins directly,
	 * because it holds the Shadow Assassin: 140M x 1.10 = 154M.
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
		// The two Power-V reward chests, revealed on solve but claimed by hand.  Not part of the 47.
		ICE_FILL.addSecret(Secret.rewardChest(-71, 75, -152, BlessingType.POWER, 5))
				.addSecret(Secret.rewardChest(-71, 75, -154, BlessingType.POWER, 5));
		reg("Red Blue", RoomType.NORMAL, new int[][]{{3, 4}, {4, 4}, {5, 4}}, 66, 99, 1, true, new Blessing(BlessingType.POWER, 5))
				.addSecret(Secret.essence(-185, 83, -153))
				.addSecret(Secret.item(-165.5, 86, -159.5))
				.addSecret(Secret.blessingChest(-145, 90, -164, BlessingType.POWER, 2))
				.addSecret(Secret.chest(-163, 70, -143));
		// Start is the one room that is not just its cell - see ANNEXES.
		START = reg("Start", RoomType.START, new int[][]{{3, 5}}, 66, 98, 1, false);
		TRAP = reg("Trap", RoomType.TRAP, new int[][]{{4, 5}}, 60, 100, 2, false);
		// The Power-II chest whose opening earns Trap its white checkmark (see ClearManager).
		TRAP.addSecret(Secret.chest(-143, 67, -182))
				.addSecret(Secret.bat(-158.5, 92, -190.5))
				.addSecret(Secret.blessingChest(-164, 90, -184, BlessingType.POWER, 2));
		// Depth II, which MAP.md §5 pins directly: it holds the Shadow Assassin, and 140M x 1.10 = 154M.
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

	/** The room a world location sits in, or {@code null} if outside the grid or in a between-room buffer. */
	public static Room roomAt(Location loc) {
		int[] cell = cellAt(loc.getX(), loc.getZ());
		return cell == null ? null : byCell(cell[0], cell[1]);
	}

	/**
	 * The room whose <b>footprint</b> the block column at (x,z) belongs to, or {@code null} off the grid or in a
	 * between-room seam.  A footprint is a room's own cells, <b>plus the 1-block seams that fall between two (or four)
	 * cells of the SAME room</b> - so a multi-cell room reads as one solid blob and walking the length of the 4-cell
	 * Hallway or across Museum's 2x2 never leaves it - <b>plus any {@link #ANNEXES}</b>, the parts of a room that sit
	 * off the grid entirely.  A seam between two DIFFERENT rooms belongs to neither: that is a crevice, and the only
	 * legal way across one is a door.
	 * <p>
	 * <b>The intra-room seams are the whole reason this is not {@code cellAt}</b>, which hands back null for every
	 * seam alike.  Routing the bounds test through that killed players walking from one cell of a room to the next
	 * (Hallway crosses x -73, -105 and -137).
	 * <p>
	 * Also not the same answer as {@link #roomAt} for a player: that takes the unfloored X/Z, so someone standing on
	 * the seam column at {@code x = -168.5} has {@code ox = 30.5} and reads as the room beside the seam, while the
	 * block they are actually inside ({@code x = -169}) is the seam.  Anything deciding whether a player is in a
	 * room - the out-of-bounds test - has to go by the block.
	 */
	public static Room roomAtBlock(int worldX, int worldZ) {
		Room r = gridRoomAt(worldX, worldZ);
		return r != null ? r : annexRoomAt(worldX, worldZ);
	}

	/** The footprint lookup for the part of a room that IS on the 32-block grid. */
	private static Room gridRoomAt(int worldX, int worldZ) {
		long dx = (long) ORIGIN - worldX;
		long dz = (long) ORIGIN - worldZ;
		if(dx < 0 || dz < 0) return null;
		int gx = (int) (dx / PITCH);
		int gz = (int) (dz / PITCH);
		if(gx > 5 || gz > 5) return null;
		boolean xSeam = (int) (dx - (long) gx * PITCH) == 31; // seam between cell gx and gx+1
		boolean zSeam = (int) (dz - (long) gz * PITCH) == 31; // seam between cell gz and gz+1
		Room r = byCell(gx, gz);
		if(r == null) return null;
		// On a seam the column is only the room's if EVERY cell it touches is that same room.
		if(xSeam && byCell(gx + 1, gz) != r) return null;
		if(zSeam && byCell(gx, gz + 1) != r) return null;
		if(xSeam && zSeam && byCell(gx + 1, gz + 1) != r) return null;
		return r;
	}

	// --- annexes: the parts of a room that are NOT on the grid ---

	/** An X/Z rectangle belonging to {@code room} but lying outside its grid cells. Y is still the room's own
	 *  {@link Room#minY}..{@link Room#maxY}, so an annex only ever widens the footprint horizontally. */
	private record Annex(Room room, int minX, int minZ, int maxX, int maxZ) {
		boolean contains(int x, int z) {
			return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
		}
	}

	private static final List<Annex> ANNEXES = new ArrayList<>();

	static {
		// THE ONE ROOM THAT IS NOT JUST ITS CELL. Start's cell (3,5) ends at Z -200, but the room physically runs back
		// to Z -213 - the entrance area behind the spawn point at Z -183.5. Without this the whole of it is off-grid,
		// so standing where players spawn and wait was out of bounds. Full cell width in X: only the Z extent is known
		// to differ, and reading a few extra columns of solid rock as "inside" costs nothing, the same call the door
		// frames make.
		ANNEXES.add(new Annex(START, cellMinX(3), -213, cellMaxX(3), cellMinZ(5) - 1));
	}

	private static Room annexRoomAt(int worldX, int worldZ) {
		for(Annex a : ANNEXES) {
			if(a.contains(worldX, worldZ)) return a.room();
		}
		return null;
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

	// --- doors ---

	/**
	 * The 15 doors, as the cell pairs {@code {gx,gz}→{gx,gz}} they join.  <b>Grid adjacency is not the door graph</b>
	 * (see {@link Room#level}), so this is the only statement of which rooms actually connect.  {@link DungeonMap}
	 * draws it, {@link #inDoor} keeps a player standing in one from counting as out of bounds, and
	 * {@code CustomItems.onBlockBreak} makes the frames unbreakable during a run.
	 */
	public static final int[][][] DOORS = {
			{{0, 0}, {0, 1}}, {{4, 0}, {5, 0}}, {{5, 1}, {5, 2}}, {{3, 2}, {4, 2}}, {{3, 2}, {3, 1}},
			{{3, 2}, {3, 3}}, {{2, 2}, {2, 1}}, {{0, 1}, {0, 2}}, {{3, 3}, {3, 4}}, {{3, 4}, {3, 5}},
			{{4, 4}, {4, 5}}, {{5, 4}, {5, 5}}, {{3, 3}, {2, 3}}, {{2, 5}, {1, 5}}, {{0, 4}, {1, 4}}
	};

	/** Every door's passage is the same 3 wide x 4 tall opening at the same absolute Y across the whole floor - rooms
	 *  vary in height, the level rooms connect at does not.  Matches the three {@code Server} door fill regions. */
	private static final int DOOR_MIN_Y = 69, DOOR_MAX_Y = 72;

	/**
	 * A door's world box, grown by one block in the wall plane so the frame AROUND the opening is included: 3 blocks
	 * across the seam (one into each room), 5 along the wall, 6 tall.  Both callers want the frame, not just the hole:
	 * a player is in bounds anywhere in here, and nothing in here breaks once the run is live.
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
	 * The bordered box of the door joining two orthogonally adjacent cells.  Derived rather than listed, because every
	 * door is the same opening at the same height in the same place: it sits in the 1-block seam between the cells
	 * (one block past the near cell's far face) and is centred on the shared edge, 15 blocks in.
	 * <p>
	 * The centring is on the <b>GRID</b> edge, not on a multi-cell room's own centre: the Start door is at
	 * {@code x -121}, cell {@code gx=3}'s centre, while the Red Blue behind it spans cells (3,4)(4,4)(5,4).
	 * <p>
	 * Cross-checked against the three hardcoded fill regions in {@code Server}: Start {@code {3,5}-{3,4}} gives
	 * {@code x -122..-120, z -170..-168}, wither {@code {3,2}-{3,3}} gives {@code z -106..-104}, blood
	 * {@code {3,1}-{3,2}} gives {@code z -74..-72}, all at {@code y 69..72}.
	 */
	private static Door doorBox(int[] a, int[] b) {
		if(a[1] == b[1]) { // horizontal neighbours: the seam is a column of X, the wall runs along Z
			int seamX = cellMinX(Math.min(a[0], b[0])) - 1;
			int zc = cellMaxZ(a[1]) - 15;
			return new Door(seamX - 1, DOOR_MIN_Y - 1, zc - 2, seamX + 1, DOOR_MAX_Y + 1, zc + 2);
		}
		int seamZ = cellMinZ(Math.min(a[1], b[1])) - 1;
		int xc = cellMaxX(a[0]) - 15;
		return new Door(xc - 2, DOOR_MIN_Y - 1, seamZ - 1, xc + 2, DOOR_MAX_Y + 1, seamZ + 1);
	}

	/** Whether a block position is inside any door (frame included). */
	public static boolean inDoor(int worldX, int worldY, int worldZ) {
		for(Door d : DOOR_BOXES) {
			if(d.contains(worldX, worldY, worldZ)) return true;
		}
		return false;
	}

	// --- vertical bounds ---

	/**
	 * Whether a location is inside a room's volume: its block column is in a room's footprint AND its block Y is
	 * within that room's {@link Room#minY}..{@link Room#maxY}.  A seam between two DIFFERENT rooms is in no
	 * footprint, which is the whole point - the crevices are out of bounds and only the doors cross them - while a
	 * multi-cell room's own internal seams are interior, so moving between its cells stays in bounds.
	 */
	public static boolean inRoomBounds(Location loc) {
		Room r = roomAtBlock(loc.getBlockX(), loc.getBlockZ());
		return r != null && loc.getBlockY() >= r.minY && loc.getBlockY() <= r.maxY;
	}

	/**
	 * Whether a block is at or above its room's ceiling.  {@link Room#maxY} is the topmost block a player may legally
	 * be in, so from there up is roof: breaking it is what a hole in the ceiling starts as, whether the layer sits at
	 * maxY or just above it.  Off the grid and in the seams this is false; the seams are out of bounds anyway.
	 */
	public static boolean isCeiling(int worldX, int worldY, int worldZ) {
		Room r = roomAtBlock(worldX, worldZ);
		return r != null && worldY >= r.maxY;
	}

	/**
	 * True if the block column at world (x,z) sits on the vertical outer face (perimeter wall) of a room, at ANY
	 * height. Used to stop blocks being broken through room walls without touching the floor (the ceiling has its
	 * own run-gated rule, {@link #isCeiling}). A room is only its own outer perimeter, per {@link #roomAtBlock}'s
	 * footprint: for a multi-cell room (e.g. the 2x2 Museum) the internal seams between its cells count as interior,
	 * so the middle of the room is NOT a face and stays breakable - but where such a seam reaches the room's outer
	 * wall line it IS a face, since one of its neighbours is then another room. Crevices between two different rooms
	 * and everything off the grid return false.
	 */
	public static boolean isRoomFace(int worldX, int worldZ) {
		Room r = roomAtBlock(worldX, worldZ);
		if(r == null) return false; // between-room seam or outside the grid, so not a room's own wall
		// A perimeter column has at least one of its four horizontal neighbours outside this room's footprint.
		return roomAtBlock(worldX + 1, worldZ) != r || roomAtBlock(worldX - 1, worldZ) != r
				|| roomAtBlock(worldX, worldZ + 1) != r || roomAtBlock(worldX, worldZ - 1) != r;
	}

	public static void reset() {
		for(Room r : ALL) r.reset();
	}
}
