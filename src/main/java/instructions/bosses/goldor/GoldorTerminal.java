package instructions.bosses.goldor;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Player;
import plugin.Utils;

/**
 * One Goldor terminal: the Interaction hitbox players click and the two floating labels above it.
 * <p>
 * <b>Two behaviours, one terminal.</b>  In classic and realistic mode a click activates it outright.  In
 * ultra-realistic mode the click opens {@link GoldorTerminalGui} and only solving the puzzle activates it - hence
 * {@link #type}, rolled once at construction so a terminal keeps the same puzzle for the whole phase however many
 * times it is opened and abandoned.  Either way {@link #markActivated} is the single finish line.
 */
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
	/**
	 * Which puzzle this terminal poses in ultra-realistic mode.
	 * <p>
	 * <b>Assigned by the SECTION, not here</b> ({@link GoldorTerminalGui#assignTypes}, from
	 * {@link GoldorSection}'s constructor).  A terminal cannot roll its own: the rule is "at most one of each type
	 * per section", which is a property of the whole set and unknowable from inside one member of it.  Set once per
	 * phase and never re-rolled, so a terminal keeps its puzzle however many times it is opened and abandoned.
	 */
	private GoldorTerminalGui.Type type;

	/** Block the Interaction hitbox was spawned on.  Read by {@link GoldorTerminalGui} to place Melody. */
	public final int x, y, z;

	public GoldorTerminal(World world, int sectionIdx, int terminalIdx, int x, int y, int z) {
		this.sectionIdx = sectionIdx;
		this.terminalIdx = terminalIdx;
		this.x = x;
		this.y = y;
		this.z = z;

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

	/** Give the terminal back up, so somebody else can open it.  Closing a puzzle without solving it lands here. */
	public void clearPending() {
		pending = false;
	}

	/** The puzzle this terminal poses in ultra-realistic mode. */
	public GoldorTerminalGui.Type type() {
		return type;
	}

	/** Set by {@link GoldorTerminalGui#assignTypes} only, once, as the section is built. */
	void setType(GoldorTerminalGui.Type type) {
		this.type = type;
	}

	public void markActivated() {
		activated = true;
		pending = false;
		// Active label replaces the bottom display; top display is emptied (no background when text is empty).
		displayTop.text(Utils.msg(""));
		displayBottom.text(Utils.msg(ACTIVE_TEXT));
	}

	public void cleanup() {
		// A phase teardown must not leave somebody staring into a puzzle for a terminal that no longer exists.
		for(Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
			if(p.getOpenInventory().getTopInventory().getHolder() instanceof GoldorTerminalGui gui
					&& gui.terminal() == this) {
				p.closeInventory();
			}
		}
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
