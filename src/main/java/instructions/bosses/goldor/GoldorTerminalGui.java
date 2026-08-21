package instructions.bosses.goldor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import plugin.M7tas;
import plugin.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The terminal puzzle a player actually solves in ultra-realistic mode.
 * <p>
 * Every other mode keeps the original one-click terminal ({@code GoldorListener.tryActivateTerminal}); here the
 * click opens one of these instead, and the terminal only activates when the puzzle is solved.  <b>Nothing else
 * about the Goldor phase changes</b>: the activation still goes through {@code Goldor.onActivation}, so the
 * section counter, the broadcast, the timing lines and the gate all behave identically whichever mode is on.
 * <p>
 * <b>Every click in the view is cancelled</b>, in both inventories, before anything else happens.  The puzzles are
 * click targets, not inventories: nothing may be picked up, moved, dropped, shift-clicked in from the player's own
 * inventory, or dragged.  A solving click is cancelled too and then handled.
 * <p>
 * <b>One player at a time.</b>  Opening marks the terminal pending, which is the same flag that already stopped two
 * players activating one terminal, so a second player's click is consumed and opens nothing.  Closing early clears
 * it and throws away any progress - a half-finished Melody starts over, the way it does on Hypixel.
 */
public final class GoldorTerminalGui implements InventoryHolder {

	/** The one terminal Melody is pinned to: the S2 terminal whose Interaction spawns here. */
	private static final int MELODY_X = 40, MELODY_Y = 124, MELODY_Z = 123;

	private static final Random RANDOM = new Random();

	/**
	 * The five terminal types.  Each owns its size, its title and its layout; the solve rule lives in
	 * {@link GoldorTerminalGui#onClick}, because two of them need more than "the right slot was clicked".
	 */
	public enum Type {
		/** "Change all to same color!" - one green pane among blue; left-click it. */
		SAME_COLOR(45, "Change all to same color!"),
		/** "What starts with: 'D'?" - one diamond among barriers. */
		STARTS_WITH(45, "What starts with: 'D'?"),
		/** "Select all the BLUE items!" - one blue concrete among barriers. */
		SELECT_ALL(54, "Select all the BLUE items!"),
		/** "Click the button on time!" - four rows, each cleared by clicking its lime terracotta button. */
		MELODY(54, "Click the button on time!"),
		/** "Correct all the panes!" - one red pane among lime; click it with either button. */
		ON_OFF(45, "Correct all the panes!");

		public final int size;
		public final String title;

		Type(int size, String title) {
			this.size = size;
			this.title = title;
		}
	}

	/** Every type Melody is NOT, i.e. what the other fifteen terminals draw from. */
	private static final List<Type> WITHOUT_MELODY =
			List.of(Type.SAME_COLOR, Type.STARTS_WITH, Type.SELECT_ALL, Type.ON_OFF);

	/**
	 * Give every terminal in one section its puzzle.  Called from {@link GoldorSection}'s constructor, which is the
	 * one place that sees a whole section's terminals at once.
	 * <p>
	 * <b>At most one terminal of each type per section</b>, so the four non-Melody types are dealt out WITHOUT
	 * replacement from a shuffled pool - the composition of a section is therefore fixed and only the order is
	 * random. That rule is why a terminal cannot roll its own type: uniqueness is a property of the set.
	 * <p>
	 * <b>Melody is pinned</b> to the one S2 terminal whose Interaction spawns at {@code 40 124 123} - it always
	 * poses Melody, and no other terminal ever can. That falls out of the uniqueness rule rather than being a second
	 * decision: S2 has five terminals and there are five types, so if that terminal took anything else, S2 would
	 * need a fifth type at a terminal Melody is not allowed at. Keyed on the <b>coordinates</b>, not the
	 * section/terminal index, so the pin follows the terminal if {@code Goldor.buildS2}'s order is ever reshuffled.
	 * <p>
	 * A section with more non-Melody terminals than there are types cannot satisfy the rule at all; that is a build
	 * mistake rather than something to paper over, so it is logged and the pool reused.
	 */
	static void assignTypes(List<GoldorTerminal> terminals) {
		List<Type> pool = new ArrayList<>(WITHOUT_MELODY);
		Collections.shuffle(pool, RANDOM);
		int dealt = 0;
		for(GoldorTerminal t : terminals) {
			if(isMelodyTerminal(t)) {
				t.setType(Type.MELODY);
				continue;
			}
			if(dealt == pool.size()) {
				M7tas.getInstance().getLogger().warning("Goldor S" + (t.sectionIdx + 1) + " has more than "
						+ pool.size() + " non-Melody terminals, so its puzzle types cannot all be distinct.");
				dealt = 0;
			}
			t.setType(pool.get(dealt++));
		}
	}

	/** True for the single terminal Melody is pinned to. */
	private static boolean isMelodyTerminal(GoldorTerminal t) {
		return t.x == MELODY_X && t.y == MELODY_Y && t.z == MELODY_Z;
	}

	// ==================== layout constants ====================

	private static final ItemStack BLACK = item(Material.BLACK_STAINED_GLASS_PANE);
	private static final ItemStack BLUE = item(Material.BLUE_STAINED_GLASS_PANE);
	private static final ItemStack GREEN = item(Material.GREEN_STAINED_GLASS_PANE);
	private static final ItemStack LIME = item(Material.LIME_STAINED_GLASS_PANE);
	private static final ItemStack RED = item(Material.RED_STAINED_GLASS_PANE);
	private static final ItemStack WHITE = item(Material.WHITE_STAINED_GLASS_PANE);
	private static final ItemStack MAGENTA = item(Material.MAGENTA_STAINED_GLASS_PANE);
	private static final ItemStack BARRIER = item(Material.BARRIER);
	private static final ItemStack DIAMOND = item(Material.DIAMOND);
	private static final ItemStack BLUE_CONCRETE = item(Material.BLUE_CONCRETE);
	private static final ItemStack LIME_BUTTON = item(Material.LIME_TERRACOTTA);
	private static final ItemStack RED_BUTTON = item(Material.RED_TERRACOTTA);

	private static ItemStack item(Material m) {
		return new ItemStack(m);
	}

	private static int slot(int row, int col) {
		return row * 9 + col;
	}

	// --- Same Color: a 3x3 block of blue at rows 1-3, cols 3-5, with the bottom-middle green ---
	private static final int SAME_COLOR_GREEN = 31;
	private static final int[] SAME_COLOR_BLUE = {12, 13, 14, 21, 22, 23, 30, 32};

	// --- Starts With / Select All: the one right answer among barriers ---
	private static final int STARTS_WITH_ANSWER = 31;
	private static final int SELECT_ALL_ANSWER = 40;

	// --- On/Off: the one red pane among lime ---
	private static final int ON_OFF_ANSWER = 31;

	// --- Melody: four playable rows, the marker column, and where each row's button sits ---
	private static final int MELODY_FIRST_ROW = 1, MELODY_LAST_ROW = 4;
	private static final int MELODY_MARKER_COL = 1;
	private static final int MELODY_BUTTON_COL = 7;

	// ==================== instance ====================

	private final Inventory inv;
	private final GoldorTerminal terminal;
	public final Type type;
	/** Melody only: the row currently accepting its button click.  Past {@link #MELODY_LAST_ROW} once solved. */
	private int melodyRow = MELODY_FIRST_ROW;
	/** Latched the moment the puzzle is solved, so a second click in the same tick can't activate twice. */
	private boolean solved;

	private GoldorTerminalGui(GoldorTerminal terminal, Type type) {
		this.terminal = terminal;
		this.type = type;
		this.inv = Bukkit.createInventory(this, type.size, Utils.msg("<dark_gray>" + type.title));
		build();
	}

	/** Open {@code terminal}'s puzzle for {@code p}.  The caller owns the pending flag and every gate. */
	public static void open(Player p, GoldorTerminal terminal) {
		p.openInventory(new GoldorTerminalGui(terminal, terminal.type()).inv);
	}

	@Override
	public @NonNull Inventory getInventory() {
		return inv;
	}

	// ==================== rendering ====================

	private void build() {
		switch(type) {
			case SAME_COLOR -> {
				fill(BLACK);
				for(int s : SAME_COLOR_BLUE) inv.setItem(s, BLUE);
				inv.setItem(SAME_COLOR_GREEN, GREEN);
			}
			case STARTS_WITH -> {
				frameAndFill(BARRIER);
				inv.setItem(STARTS_WITH_ANSWER, DIAMOND);
			}
			case SELECT_ALL -> {
				frameAndFill(BARRIER);
				inv.setItem(SELECT_ALL_ANSWER, BLUE_CONCRETE);
			}
			case ON_OFF -> {
				frameAndFill(LIME);
				// One more frame column each side than the other puzzles use, so the playable block is 5 wide.
				for(int row = 1; row <= 3; row++) {
					inv.setItem(slot(row, 1), BLACK);
					inv.setItem(slot(row, 7), BLACK);
				}
				inv.setItem(ON_OFF_ANSWER, RED);
			}
			case MELODY -> {
				fill(BLACK);
				// The marker column, top and bottom, is what the rows line up against.
				inv.setItem(slot(0, MELODY_MARKER_COL), MAGENTA);
				inv.setItem(slot(5, MELODY_MARKER_COL), MAGENTA);
				for(int row = MELODY_FIRST_ROW; row <= MELODY_LAST_ROW; row++) drawMelodyRow(row);
			}
		}
	}

	private void fill(ItemStack with) {
		for(int i = 0; i < inv.getSize(); i++) inv.setItem(i, with);
	}

	/**
	 * Black glass around the whole edge, {@code body} everywhere inside.  The three barrier/pane puzzles share this
	 * shape exactly - top and bottom rows plus the first and last column of every row between them.
	 */
	private void frameAndFill(ItemStack body) {
		int rows = inv.getSize() / 9;
		for(int row = 0; row < rows; row++) {
			for(int col = 0; col < 9; col++) {
				boolean edge = row == 0 || row == rows - 1 || col == 0 || col == 8;
				inv.setItem(slot(row, col), edge ? BLACK : body);
			}
		}
	}

	/**
	 * Draw one Melody row in whichever of its two states it is in.
	 * <p>
	 * <b>Only two states exist</b>, active and inactive, so a row that has been cleared renders as inactive again -
	 * there is no third "done" look.  The progress a player reads is which row is lit, not which rows are behind it.
	 */
	private void drawMelodyRow(int row) {
		boolean active = row == melodyRow;
		if(active) {
			// The lit cell sits in the marker column, which is what the magenta panes top and bottom point at.
			inv.setItem(slot(row, MELODY_MARKER_COL), LIME);
			for(int col = MELODY_MARKER_COL + 1; col <= 5; col++) inv.setItem(slot(row, col), RED);
			inv.setItem(slot(row, MELODY_BUTTON_COL), LIME_BUTTON);
		} else {
			for(int col = MELODY_MARKER_COL; col <= 5; col++) inv.setItem(slot(row, col), WHITE);
			inv.setItem(slot(row, MELODY_BUTTON_COL), RED_BUTTON);
		}
	}

	// ==================== solving ====================

	/**
	 * Handle one already-cancelled click.  Returns true once the puzzle is finished, at which point the caller
	 * activates the terminal and closes the view.
	 * <p>
	 * <b>Same Color takes a LEFT click only</b>, because that is the button that recolours its pane.  Every other
	 * puzzle - On/Off included - accepts either button.
	 * <p>
	 * Clearing a Melody row plays the terminal-completion cue ({@code Goldor.playActivationSound}); the FOURTH row
	 * does not, because completing the terminal plays it a moment later and two of them would stack on one tick.
	 *
	 * @param slot raw slot of the click, already known to be in the TOP inventory
	 */
	public boolean onClick(int slot, ClickType click) {
		if(solved) return false;
		boolean hit = switch(type) {
			case SAME_COLOR -> slot == SAME_COLOR_GREEN && click.isLeftClick();
			case ON_OFF -> slot == ON_OFF_ANSWER;
			case STARTS_WITH -> slot == STARTS_WITH_ANSWER;
			case SELECT_ALL -> slot == SELECT_ALL_ANSWER;
			case MELODY -> slot == slot(melodyRow, MELODY_BUTTON_COL);
		};
		if(!hit) return false;

		if(type == Type.MELODY) {
			int cleared = melodyRow;
			melodyRow++;
			drawMelodyRow(cleared); // back to inactive
			if(melodyRow > MELODY_LAST_ROW) {
				solved = true;
				return true;
			}
			drawMelodyRow(melodyRow);
			// Clearing a row sounds exactly like completing a terminal - the same cue, deliberately.  Only the
			// non-final rows: the last one falls through to the activation, which plays it itself.
			Goldor.playActivationSound();
			return false;
		}
		solved = true;
		return true;
	}

	/** The terminal this view belongs to, so the click handler can activate the right one. */
	public GoldorTerminal terminal() {
		return terminal;
	}

	/** True once the puzzle has been solved, so closing the view isn't treated as giving up. */
	public boolean isSolved() {
		return solved;
	}
}
