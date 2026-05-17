package instructions.bosses.goldor;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;

public final class GoldorTerminal {
	public static final String TAG_PREFIX = "goldor_terminal_";

	private static final String INACTIVE_TEXT =
			ChatColor.RED + "Inactive Terminal\n" + ChatColor.GOLD + "" + ChatColor.BOLD + "CLICK HERE";
	private static final String ACTIVE_TEXT =
			ChatColor.GREEN + "Terminal Active";

	public final int sectionIdx;
	public final int terminalIdx;

	private final Interaction interaction;
	private final TextDisplay display;
	private boolean activated = false;
	private boolean pending = false;

	public GoldorTerminal(World world, int sectionIdx, int terminalIdx, int x, int y, int z) {
		this.sectionIdx = sectionIdx;
		this.terminalIdx = terminalIdx;

		Location interactionLoc = new Location(world, x + 0.5, y, z + 0.5);
		Location displayLoc = new Location(world, x + 0.5, y + 1.0, z + 0.5);

		this.interaction = world.spawn(interactionLoc, Interaction.class, e -> {
			e.setInteractionWidth(1.0f);
			e.setInteractionHeight(1.0f);
			e.setResponsive(true);
			e.addScoreboardTag(TAG_PREFIX + sectionIdx + "_" + terminalIdx);
			e.addScoreboardTag("TASNoName");
		});

		this.display = world.spawn(displayLoc, TextDisplay.class, e -> {
			e.setText(INACTIVE_TEXT);
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
		display.setText(ACTIVE_TEXT);
	}

	public void cleanup() {
		if(interaction != null && interaction.isValid()) interaction.remove();
		if(display != null && display.isValid()) display.remove();
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
