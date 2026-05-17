package instructions.bosses.goldor;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

public final class GoldorDevice {
	private static final String INACTIVE_TEXT =
			ChatColor.RED + "Inactive\nDevice";
	private static final String ACTIVE_TEXT =
			ChatColor.GREEN + "Device\nActive";

	public final int sectionIdx;
	private final TextDisplay display;
	private boolean activated = false;

	public GoldorDevice(World world, int sectionIdx, int x, int y, int z) {
		this.sectionIdx = sectionIdx;
		Location displayLoc = new Location(world, x + 0.5, y + 1.0, z + 0.5);
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

	public void markActivated() {
		activated = true;
		display.setText(ACTIVE_TEXT);
	}

	public void cleanup() {
		if(display != null && display.isValid()) display.remove();
	}
}
