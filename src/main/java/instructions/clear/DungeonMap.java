package instructions.clear;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;
import org.jspecify.annotations.NonNull;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The offhand minimap: one shared {@link MapView} with a custom {@link MapRenderer} that draws the 6×6 room
 * grid, door connectors, white/green checkmarks per {@link Room#check()}, and a marker cursor for every real
 * player. {@link #mapItem()} builds the FILLED_MAP the clear keeps in the player's offhand (hotbar slot 8 stays
 * the SkyBlock Menu).
 *
 * <p><b>Fog of war</b> (matching Hypixel's Magical Map): a room is only drawn once it becomes visible.
 * <ul>
 *   <li><b>Explored</b> (a player has set foot inside, {@link Room#explored}): drawn in full, with every cell in
 *       the {@link RoomType} colour and its checkmark.</li>
 *   <li><b>Revealed</b>: an unexplored room a door directly connects to an explored room.  Drawn as a single grey
 *       1×1 "?" at the door-adjacent cell, so a multi-cell room only shows its shape once entered.  Reveal never
 *       chains: a room two doors out from anything explored stays hidden.</li>
 *   <li><b>Hidden</b>: everything else, which is not drawn at all.</li>
 * </ul>
 * Opening the lone wither / blood door auto-explores the room behind it (Deathmite / Blood) as if a player had
 * walked in.  See {@code Server.openWitherDoor/openBloodDoor} → {@link ClearManager#exploreRoom}.
 *
 * <p>Orientation: grid column {@code gx} → map X (left→right), row {@code gz} → map Y (top→bottom).
 */
public final class DungeonMap {
	private DungeonMap() {
	}

	// layout: 6 tiles + 5 gaps, centred so all four borders are (near-)equal
	private static final int TILE = 17, GAP = 3;
	private static final int BORDER = (128 - (6 * TILE + 5 * GAP)) / 2;
	private static final int DOOR_WIDTH = 6; // door opening thickness along the shared wall
	private static final Color BG = new Color(0, 0, 0, 0); // transparent → shows the map's parchment background
	// Door colours by type (real-map values): most doors are brown NORMAL doors; this layout has exactly one
	// WITHER door (black, directly above the Fairy room), one BLOOD door (red, into Blood), and the ENTRANCE
	// (Start-room) door (green).
	private static final Color DOOR_NORMAL = new Color(114, 67, 27);
	private static final Color DOOR_WITHER = new Color(0, 0, 0);
	private static final Color DOOR_BLOOD = new Color(255, 0, 0);
	private static final Color DOOR_ENTRANCE = new Color(0, 124, 0);
	/** The lone wither door, as its {gx,gz} cell pair: Deathmite (3,2) ↔ Fairy (3,3). */
	private static final int[][] WITHER_DOOR = {{3, 2}, {3, 3}};
	// The single "entrance" door of each coloured special room, the one you pass through approaching it FROM
	// SPAWN, painted in that room's own colour instead of brown.  Fairy has three doors, so only its spawn-side
	// door (from Red Blue, below) counts; the wither door above it and the Wizard door beside it keep their own
	// colours.  Blood's one door is already painted blood-red by the RoomType.BLOOD check in doorColor().
	private static final int[][] FAIRY_DOOR = {{3, 3}, {3, 4}};  // Red Blue → Fairy
	private static final int[][] YELLOW_DOOR = {{5, 4}, {5, 5}}; // Red Blue → Yellow
	private static final int[][] TRAP_DOOR = {{4, 4}, {4, 5}};   // Red Blue → Trap
	private static final int[][] QUIZ_DOOR = {{0, 0}, {0, 1}};   // Gravel → Quiz (puzzle)
	private static final int[][] ICE_DOOR = {{0, 4}, {1, 4}};    // Well → Ice Fill (puzzle)
	// Real-Magical-Map values (from NEU's DungeonMap.java, same source as the RoomType colours): undiscovered
	// rooms are grey (65,65,65) with a near-black (13,13,13) "?".
	private static final Color UNEXPLORED = new Color(65, 65, 65);
	private static final Color QUESTION = new Color(13, 13, 13);

	// The 15 doors live in Rooms (the out-of-bounds test and the block protection need them too). Coloured by door
	// type in doorColor(); see the DOOR_* constants.
	private static final int[][][] DOORS = Rooms.DOORS;

	private static MapView view;
	private static int version = 1;
	private static final Map<UUID, Integer> drawn = new HashMap<>();

	/** Bump when room state changes so the renderer repaints the tiles on the next frame. */
	public static void markDirty() {
		version++;
	}

	private static synchronized MapView view() {
		if(view == null) {
			view = Bukkit.createMap(Bukkit.getWorlds().getFirst());
			view.setScale(MapView.Scale.CLOSEST);
			view.setTrackingPosition(false);
			view.setUnlimitedTracking(false);
			view.getRenderers().clear();
			view.addRenderer(new Renderer());
		}
		return view;
	}

	public static ItemStack mapItem() {
		ItemStack map = new ItemStack(Material.FILLED_MAP);
		MapMeta meta = (MapMeta) map.getItemMeta();
		meta.setMapView(view());
		meta.displayName(plugin.Utils.nameComponent(plugin.Utils.mmLegacy("<white>Magical Map")));
		map.setItemMeta(meta);
		return map;
	}

	/** True only if {@code item} is a filled map bound to THIS session's dungeon MapView (not a stale old map). */
	public static boolean isDungeonMap(ItemStack item) {
		return item != null && item.getType() == Material.FILLED_MAP
				&& item.getItemMeta() instanceof MapMeta mm && mm.getMapView() == view();
	}

	private static int tileX(int gx) { return BORDER + gx * (TILE + GAP); }
	private static int tileY(int gz) { return BORDER + gz * (TILE + GAP); }

	private static class Renderer extends MapRenderer {
		Renderer() {
			super(true); // contextual: render(...) is called per-player
		}

		@Override
		public void render(@NonNull MapView mv, @NonNull MapCanvas canvas, Player player) {
			Integer seen = drawn.get(player.getUniqueId());
			if(seen == null || seen != version) {
				paintTiles(canvas);
				drawn.put(player.getUniqueId(), version);
			}
			paintCursors(canvas);
		}

		private void paintTiles(MapCanvas canvas) {
			for(int x = 0; x < 128; x++)
				for(int y = 0; y < 128; y++)
					canvas.setPixelColor(x, y, BG);

			// Fog of war: explored rooms draw in full; each unexplored-but-revealed room draws a single 1×1 at
			// its door-adjacent entry cell; everything else stays hidden. `reveal` maps a revealed room → that cell.
			Map<Room, int[]> reveal = computeReveals();

			// room tiles
			for(Room r : Rooms.all()) {
				if(r.explored) {
					for(int[] c : r.cells) fillRect(canvas, tileX(c[0]), tileY(c[1]), TILE, TILE, r.type.color);
				} else {
					int[] e = reveal.get(r);
					if(e != null) fillRect(canvas, tileX(e[0]), tileY(e[1]), TILE, TILE, UNEXPLORED);
				}
			}
			// connectors within a multi-cell room: full-width joins in the room colour, for explored rooms only,
			// since a revealed room is a lone 1×1 and has no internal joins
			for(Room r : Rooms.all()) {
				if(!r.explored) continue;
				for(int[] a : r.cells) {
					for(int[] b : r.cells) {
						if(adjacent(a, b) && cellOrder(a, b)) fillConnector(canvas, a, b, r.type.color, TILE);
					}
				}
			}
			// fill the centre gap where four cells of the same explored room meet (e.g. Museum's 2x2 doughnut hole)
			for(Room r : Rooms.all()) {
				if(!r.explored) continue;
				for(int[] c : r.cells) {
					int gx = c[0], gz = c[1];
					if(Rooms.byCell(gx + 1, gz) == r && Rooms.byCell(gx, gz + 1) == r && Rooms.byCell(gx + 1, gz + 1) == r) {
						fillRect(canvas, tileX(gx) + TILE, tileY(gz) + TILE, GAP, GAP, r.type.color);
					}
				}
			}
			// door connectors between rooms: a narrow opening centred on the shared wall, coloured by door type.

			// Only drawn between two currently-visible cells: a locked-and-closed door (whose far room is hidden)
			// draws nothing, and a door into a revealed room only draws if it lands on that room's shown 1×1 cell.
			for(int[][] d : DOORS) {
				Room ra = Rooms.byCell(d[0][0], d[0][1]);
				Room rb = Rooms.byCell(d[1][0], d[1][1]);
				if(ra == null || rb == null || ra == rb) continue;
				if(!cellDrawn(reveal, d[0][0], d[0][1]) || !cellDrawn(reveal, d[1][0], d[1][1])) continue;
				fillConnector(canvas, d[0], d[1], doorColor(d, ra, rb), DOOR_WIDTH);
			}
			// revealed rooms get a black "?" at their shown cell; explored rooms get their checkmark (if any)
			for(Room r : Rooms.all()) {
				if(!r.explored) {
					int[] e = reveal.get(r);
					if(e != null) drawQuestion(canvas, tileX(e[0]) + TILE / 2, tileY(e[1]) + TILE / 2);
					continue;
				}
				Room.Check ch = r.check();
				if(ch == Room.Check.NONE) continue;
				Color col = ch == Room.Check.GREEN ? RoomType.GREEN_CHECK : RoomType.WHITE_CHECK;
				int[] c = r.cells[0];
				drawCheck(canvas, tileX(c[0]) + TILE / 2, tileY(c[1]) + TILE / 2, col);
			}
		}

		/** A door's colour: the spawn-side entrance door of Fairy / Yellow / Trap / a puzzle room is painted in
		 *  that room's own colour; Blood's door is red and the Start door green (both the room's colour too);
		 *  the lone {@link #WITHER_DOOR} above the Fairy room is black; every other door is a brown NORMAL door. */
		private Color doorColor(int[][] d, Room a, Room b) {
			if(sameDoor(d, FAIRY_DOOR)) return RoomType.FAIRY.color;
			if(sameDoor(d, YELLOW_DOOR)) return RoomType.YELLOW.color;
			if(sameDoor(d, TRAP_DOOR)) return RoomType.TRAP.color;
			if(sameDoor(d, QUIZ_DOOR) || sameDoor(d, ICE_DOOR)) return RoomType.PUZZLE.color;
			if(a.type == RoomType.BLOOD || b.type == RoomType.BLOOD) return DOOR_BLOOD;
			if(a.type == RoomType.START || b.type == RoomType.START) return DOOR_ENTRANCE;
			if(sameDoor(d, WITHER_DOOR)) return DOOR_WITHER;
			return DOOR_NORMAL;
		}

		private void paintCursors(MapCanvas canvas) {
			MapCursorCollection cursors = new MapCursorCollection();
			for(Player p : ClearManager.realPlayers()) {
				double u = (Rooms.ORIGIN - p.getLocation().getX()) / Rooms.PITCH;
				double v = (Rooms.ORIGIN - p.getLocation().getZ()) / Rooms.PITCH;
				if(u < 0 || v < 0 || u > 6 || v > 6) continue; // off the map
				int px = (int) Math.round(BORDER + u * (TILE + GAP));
				int py = (int) Math.round(BORDER + v * (TILE + GAP));
				byte cx = (byte) Math.clamp(px * 2L - 128, -128, 127);
				byte cy = (byte) Math.clamp(py * 2L - 128, -128, 127);
				// +8 (180°): the cursor arrow otherwise points opposite the player's facing.
				byte dir = (byte) ((Math.round(p.getLocation().getYaw() / 22.5f) + 8) & 15);
				cursors.addCursor(new MapCursor(cx, cy, dir, classCursor(p), true));
			}
			canvas.setCursors(cursors);
		}

		/** Marker sprite coloured by the player's dungeon class (read from the class scoreboard tag set by
		 *  {@code /eq}), using the vanilla map markers: archer green ({@code FRAME}), berserk red, mage blue,
		 *  healer/tank white ({@code PLAYER}). Any untagged player also gets the white {@code PLAYER} arrow. */
		private MapCursor.Type classCursor(Player p) {
			var tags = p.getScoreboardTags();
			if(tags.contains("Archer"))  return MapCursor.Type.FRAME;       // green
			if(tags.contains("Berserk")) return MapCursor.Type.RED_MARKER;  // red
			if(tags.contains("Mage"))    return MapCursor.Type.BLUE_MARKER; // blue
			return MapCursor.Type.PLAYER;                                   // healer, tank, and untagged → white
		}
	}

	// --- fog of war ---

	/** For every unexplored room currently revealed (an OPEN door directly links it to an explored room), the
	 *  single door-adjacent cell {@code {gx,gz}} to draw its 1×1 "?" at. Reveal is one node deep and never chains
	 *  through a revealed room. Explored/hidden rooms are absent from the returned map. */
	private static Map<Room, int[]> computeReveals() {
		Map<Room, int[]> reveal = new HashMap<>();
		for(int[][] d : DOORS) {
			Room ra = Rooms.byCell(d[0][0], d[0][1]);
			Room rb = Rooms.byCell(d[1][0], d[1][1]);
			if(ra == null || rb == null || ra == rb) continue;
			// an explored room reveals its unexplored neighbour at the neighbour's door cell (first door wins)
			if(ra.explored && !rb.explored) reveal.putIfAbsent(rb, d[1]);
			if(rb.explored && !ra.explored) reveal.putIfAbsent(ra, d[0]);
		}
		return reveal;
	}

	/** Whether cell {@code (gx,gz)} is actually painted this frame: any cell of an explored room, or the single
	 *  shown cell of a revealed room. Used to gate door connectors to visible endpoints only. */
	private static boolean cellDrawn(Map<Room, int[]> reveal, int gx, int gz) {
		Room r = Rooms.byCell(gx, gz);
		if(r == null) return false;
		if(r.explored) return true;
		int[] e = reveal.get(r);
		return e != null && e[0] == gx && e[1] == gz;
	}

	/** True if two doors connect the same cell pair, regardless of endpoint order. */
	private static boolean sameDoor(int[][] d, int[][] e) {
		return (d[0][0] == e[0][0] && d[0][1] == e[0][1] && d[1][0] == e[1][0] && d[1][1] == e[1][1])
				|| (d[0][0] == e[1][0] && d[0][1] == e[1][1] && d[1][0] == e[0][0] && d[1][1] == e[0][1]);
	}

	// --- drawing helpers ---
	private static void fillRect(MapCanvas canvas, int x0, int y0, int w, int h, Color c) {
		for(int x = x0; x < x0 + w; x++)
			for(int y = y0; y < y0 + h; y++)
				if(x >= 0 && x < 128 && y >= 0 && y < 128) canvas.setPixelColor(x, y, c);
	}

	/** Fill the gap strip between two orthogonally adjacent cells, {@code span} px long along the wall (centred). */
	private static void fillConnector(MapCanvas canvas, int[] a, int[] b, Color c, int span) {
		int agx = a[0], agz = a[1], bgx = b[0], bgz = b[1];
		int off = (TILE - span) / 2;
		if(agz == bgz) { // horizontal neighbours → vertical gap strip
			int leftGx = Math.min(agx, bgx);
			fillRect(canvas, tileX(leftGx) + TILE, tileY(agz) + off, GAP, span, c);
		} else if(agx == bgx) { // vertical neighbours → horizontal gap strip
			int topGz = Math.min(agz, bgz);
			fillRect(canvas, tileX(agx) + off, tileY(topGz) + TILE, span, GAP, c);
		}
	}

	private static boolean adjacent(int[] a, int[] b) {
		return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) == 1;
	}

	/** Stable order so each same-room connector is drawn once (a before b). */
	private static boolean cellOrder(int[] a, int[] b) {
		return a[0] < b[0] || (a[0] == b[0] && a[1] < b[1]);
	}

	/** Stamp the real vanilla-font "?" (the same glyph Hypixel's map draws) centred on (cx,cy), in the near-black
	 *  {@link #QUESTION} colour. Reads the glyph bitmap straight from {@link MinecraftFont} and plots it with the
	 *  Color-based {@code setPixelColor} (no deprecated byte-palette API). */
	private static void drawQuestion(MapCanvas canvas, int cx, int cy) {
		MapFont.CharacterSprite sprite = MinecraftFont.Font.getChar('?');
		if(sprite == null) return;
		int w = sprite.getWidth(), h = sprite.getHeight();
		int x0 = cx - w / 2, y0 = cy - h / 2;
		for(int row = 0; row < h; row++) {
			for(int col = 0; col < w; col++) {
				if(sprite.get(row, col)) canvas.setPixelColor(x0 + col, y0 + row, QUESTION);
			}
		}
	}

	/** A small check-mark: short down-right stroke then a longer up-right stroke, centred on (cx,cy). */
	private static void drawCheck(MapCanvas canvas, int cx, int cy, Color col) {
		for(int i = 0; i < 3; i++) plot(canvas, cx - 4 + i, cy - 1 + i, col);
		for(int i = 0; i < 6; i++) plot(canvas, cx - 2 + i, cy + 1 - i, col);
	}

	private static void plot(MapCanvas canvas, int x, int y, Color col) {
		for(int dx = 0; dx <= 1; dx++)
			for(int dy = 0; dy <= 1; dy++) {
				int px = x + dx, py = y + dy;
				if(px >= 0 && px < 128 && py >= 0 && py < 128) canvas.setPixelColor(px, py, col);
			}
	}
}
