package instructions.clear;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The hotbar minimap: one shared {@link MapView} with a custom {@link MapRenderer} that draws the 6×6 room
 * grid (each room in its {@link RoomType} colour), black door connectors, room-colour connectors within
 * multi-cell rooms, white/green checkmarks per {@link Room#check()}, and a marker cursor for every real
 * player. {@link #mapItem()} builds the FILLED_MAP that goes in hotbar slot 8 during the clear.
 *
 * <p>Orientation: grid column {@code gx} → map X (left→right), row {@code gz} → map Y (top→bottom).
 */
public final class DungeonMap {
	private DungeonMap() {
	}

	// layout: 4px border + 6 tiles of 18px + 5 gaps of 2px = 126px, centered in the 128px canvas
	private static final int BORDER = 4, TILE = 18, GAP = 2;
	private static final Color BG = new Color(24, 24, 24);

	// The 15 doors, as cell pairs {gx,gz}→{gx,gz}. Drawn as black connectors.
	private static final int[][][] DOORS = {
			{{0, 0}, {0, 1}}, {{4, 0}, {5, 0}}, {{5, 1}, {5, 2}}, {{3, 2}, {4, 2}}, {{3, 2}, {3, 1}},
			{{3, 2}, {3, 3}}, {{2, 2}, {2, 1}}, {{0, 1}, {0, 2}}, {{3, 3}, {3, 4}}, {{3, 4}, {3, 5}},
			{{4, 4}, {4, 5}}, {{5, 4}, {5, 5}}, {{3, 3}, {2, 3}}, {{2, 5}, {1, 5}}, {{0, 4}, {1, 4}}
	};

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
		meta.displayName(plugin.Utils.nameComponent(plugin.Utils.mmLegacy("<green>Magical Map")));
		map.setItemMeta(meta);
		return map;
	}

	private static int tileX(int gx) { return BORDER + gx * (TILE + GAP); }
	private static int tileY(int gz) { return BORDER + gz * (TILE + GAP); }

	private static class Renderer extends MapRenderer {
		Renderer() {
			super(true); // contextual: render(...) is called per-player
		}

		@Override
		public void render(MapView mv, MapCanvas canvas, Player player) {
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

			// room tiles
			for(Room r : Rooms.all()) {
				for(int[] c : r.cells) fillRect(canvas, tileX(c[0]), tileY(c[1]), TILE, TILE, r.type.color);
			}
			// connectors within a multi-cell room (room colour)
			for(Room r : Rooms.all()) {
				for(int[] a : r.cells) {
					for(int[] b : r.cells) {
						if(adjacent(a, b) && cellOrder(a, b)) fillConnector(canvas, a, b, r.type.color);
					}
				}
			}
			// door connectors between rooms (black)
			for(int[][] d : DOORS) {
				Room ra = Rooms.byCell(d[0][0], d[0][1]);
				Room rb = Rooms.byCell(d[1][0], d[1][1]);
				if(ra != null && rb != null && ra != rb) fillConnector(canvas, d[0], d[1], Color.BLACK);
			}
			// checkmarks
			for(Room r : Rooms.all()) {
				Room.Check ch = r.check();
				if(ch == Room.Check.NONE) continue;
				int[] c = r.cells[0];
				Color col = ch == Room.Check.GREEN ? RoomType.GREEN_CHECK : RoomType.WHITE_CHECK;
				drawCheck(canvas, tileX(c[0]) + TILE / 2, tileY(c[1]) + TILE / 2, col);
			}
		}

		private void paintCursors(MapCanvas canvas) {
			MapCursorCollection cursors = new MapCursorCollection();
			for(Player p : ClearManager.realPlayers()) {
				double u = (double) (Rooms.ORIGIN - p.getLocation().getX()) / Rooms.PITCH;
				double v = (double) (Rooms.ORIGIN - p.getLocation().getZ()) / Rooms.PITCH;
				if(u < 0 || v < 0 || u > 6 || v > 6) continue; // off the map
				int px = (int) Math.round(BORDER + u * (TILE + GAP));
				int py = (int) Math.round(BORDER + v * (TILE + GAP));
				byte cx = (byte) Math.max(-128, Math.min(127, px * 2 - 128));
				byte cy = (byte) Math.max(-128, Math.min(127, py * 2 - 128));
				byte dir = (byte) (Math.round(p.getLocation().getYaw() / 22.5f) & 15);
				cursors.addCursor(new MapCursor(cx, cy, dir, MapCursor.Type.WHITE_POINTER, true));
			}
			canvas.setCursors(cursors);
		}
	}

	// --- drawing helpers ---
	private static void fillRect(MapCanvas canvas, int x0, int y0, int w, int h, Color c) {
		for(int x = x0; x < x0 + w; x++)
			for(int y = y0; y < y0 + h; y++)
				if(x >= 0 && x < 128 && y >= 0 && y < 128) canvas.setPixelColor(x, y, c);
	}

	/** Fill the gap strip between two orthogonally adjacent cells. */
	private static void fillConnector(MapCanvas canvas, int[] a, int[] b, Color c) {
		int agx = a[0], agz = a[1], bgx = b[0], bgz = b[1];
		if(agz == bgz) { // horizontal neighbours → vertical gap strip
			int leftGx = Math.min(agx, bgx);
			int x0 = tileX(leftGx) + TILE;
			fillRect(canvas, x0, tileY(agz), GAP, TILE, c);
		} else if(agx == bgx) { // vertical neighbours → horizontal gap strip
			int topGz = Math.min(agz, bgz);
			int y0 = tileY(topGz) + TILE;
			fillRect(canvas, tileX(agx), y0, TILE, GAP, c);
		}
	}

	private static boolean adjacent(int[] a, int[] b) {
		return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) == 1;
	}

	/** Stable order so each same-room connector is drawn once (a before b). */
	private static boolean cellOrder(int[] a, int[] b) {
		return a[0] < b[0] || (a[0] == b[0] && a[1] < b[1]);
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
