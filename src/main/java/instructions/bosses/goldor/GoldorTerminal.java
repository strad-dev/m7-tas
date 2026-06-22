package instructions.bosses.goldor;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;
import plugin.Utils;

public final class GoldorTerminal {
	public static final String TAG_PREFIX = "goldor_terminal_";

	private static final String INACTIVE_LINE_1 = "<red>Inactive Terminal";
	private static final String INACTIVE_LINE_2 = "<green><bold>CLICK HERE";
	private static final String ACTIVE_TEXT = "<green>Terminal Active";

	public final int sectionIdx;
	public final int terminalIdx;

	private final Interaction interaction;
	/** Top display: "Inactive Terminal" when inactive; "Terminal Active" when activated. */
	private final TextDisplay displayTop;
	/** Bottom display: "CLICK HERE" when inactive; hidden/empty when activated. */
	private final TextDisplay displayBottom;
	private boolean activated = false;
	private boolean pending = false;

	public GoldorTerminal(World world, int sectionIdx, int terminalIdx, int x, int y, int z) {
		this.sectionIdx = sectionIdx;
		this.terminalIdx = terminalIdx;

		Location interactionLoc = new Location(world, x + 0.5, y, z + 0.5);
		// Two separate TextDisplays with vanilla backgrounds; gap between them has no background.
		Location bottomLoc = new Location(world, x + 0.5, y + 1.0, z + 0.5);
		Location topLoc = new Location(world, x + 0.5, y + 1.375, z + 0.5);

		this.interaction = world.spawn(interactionLoc, Interaction.class, e -> {
			e.setInteractionWidth(1.0f);
			e.setInteractionHeight(1.5f);
			e.setResponsive(true);
			e.addScoreboardTag(TAG_PREFIX + sectionIdx + "_" + terminalIdx);
			e.addScoreboardTag("TASNoName");
		});

		this.displayTop = world.spawn(topLoc, TextDisplay.class, e -> {
			e.text(Utils.msg(INACTIVE_LINE_1));
			e.setBillboard(Display.Billboard.CENTER);
			e.setAlignment(TextDisplay.TextAlignment.CENTER);
			e.addScoreboardTag("TASNoName");
		});

		this.displayBottom = world.spawn(bottomLoc, TextDisplay.class, e -> {
			e.text(Utils.msg(INACTIVE_LINE_2));
			e.setBillboard(Display.Billboard.CENTER);
			e.setAlignment(TextDisplay.TextAlignment.CENTER);
			e.addScoreboardTag("TASNoName");
		});
	}

	public boolean isActivated() {
		return activated;
	}

	public boolean isPending() {
		return pending;
	}

	public void setPending() {
		pending = true;
	}

	public void markActivated() {
		activated = true;
		pending = false;
		// Active label replaces the bottom display; top display is emptied (no background when text is empty).
		displayTop.text(Utils.msg(""));
		displayBottom.text(Utils.msg(ACTIVE_TEXT));
	}

	public void cleanup() {
		if(interaction != null && interaction.isValid()) interaction.remove();
		if(displayTop != null && displayTop.isValid()) displayTop.remove();
		if(displayBottom != null && displayBottom.isValid()) displayBottom.remove();
	}

	public static int[] parseTag(String tag) {
		if(!tag.startsWith(TAG_PREFIX)) return null;
		String[] parts = tag.substring(TAG_PREFIX.length()).split("_");
		if(parts.length != 2) return null;
		try {
			return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
