package instructions.clear;

import java.awt.Color;

/**
 * The seven dungeon room categories, each with its authoritative Magical-Map colour (RGB pulled from the
 * NotEnoughUpdates / Hypixel map renderer). Used both for map rendering and for the clear logic that decides
 * how a room earns its checkmark.
 */
public enum RoomType {
	START(new Color(0, 124, 0)),
	NORMAL(new Color(114, 67, 27)),
	FAIRY(new Color(242, 127, 165)),
	PUZZLE(new Color(178, 76, 216)),
	TRAP(new Color(216, 127, 51)),
	YELLOW(new Color(229, 229, 51)),
	BLOOD(new Color(255, 0, 0));

	public final Color color;

	RoomType(Color color) {
		this.color = color;
	}

	/** White checkmark = room "cleared" (miniboss/objective done). */
	public static final Color WHITE_CHECK = new Color(255, 255, 255);
	/** Green checkmark = room "fully complete" (cleared AND all secrets found). Same green as a START room. */
	public static final Color GREEN_CHECK = new Color(0, 124, 0);
}
