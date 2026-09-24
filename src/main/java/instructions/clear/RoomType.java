package instructions.clear;

import java.awt.*;

/**
 * Room categories with their Magical-Map colour (RGB from the NotEnoughUpdates / Hypixel map renderer). Drives
 * map rendering and how a room earns its checkmark.
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

	/** Room cleared (miniboss/objective done). */
	public static final Color WHITE_CHECK = new Color(255, 255, 255);
	/** Cleared and all secrets found. Same green as START. */
	public static final Color GREEN_CHECK = new Color(0, 124, 0);
}
