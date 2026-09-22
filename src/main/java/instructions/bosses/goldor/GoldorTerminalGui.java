package instructions.bosses.goldor;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NonNull;
import plugin.Alpha;
import plugin.M7tas;
import plugin.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The terminal puzzle a player actually solves in realistic mode.
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
 * it and throws away all the progress: a view is built fresh every open, so an abandoned puzzle comes back with a
 * new board - a half-finished Melody starts over, the way it does on Hypixel.  Only the TYPE is fixed, on the
 * terminal, so what a terminal asks for never changes mid-phase.
 * <p>
 * <b>Nothing here shares an {@link ItemStack}.</b>  Four of the six puzzles put a per-slot amount or a glint on a
 * pane, and a shared constant would have to be cloned at half the write sites; one rule ("every write builds its
 * own stack") is easier to keep than a list of which slots are safe to alias.  A chest is 54 allocations.
 * <p>
 * <b>{@link #onClosed()} must be called for every close</b>, solved or abandoned.  Melody owns a repeating task,
 * and a repeating task still painting panes into a chest nobody is looking at is the failure mode this class
 * takes the most care to avoid.
 */
public final class GoldorTerminalGui implements InventoryHolder {

	private static final Random RANDOM = new Random();

	/**
	 * The six terminal types.  Each owns its size and its title; the layout and the solve rule live below, because
	 * no two of them are near enough to share.
	 * <p>
	 * <b>None of these titles is Hypixel's string, and that is the point.</b>  Odin and the other SkyBlock
	 * terminal-solver mods find a terminal by matching the GUI title, and then they take the clicks over: on
	 * 2026-09-12 Odin swallowed every click in these puzzles client-side, so no packet ever reached the server
	 * and the GUI looked simply dead.  Every title below is therefore altered just enough to miss an exact
	 * match while still reading normally to a player:
	 * <ul>
	 *   <li>every lowercase {@code o} is a Greek omicron (U+03BF), not an ASCII {@code o};</li>
	 *   <li>where a title has no lowercase {@code o} to swap, one {@code e} is a Cyrillic {@code е} (U+0435) -
	 *       Select All's, in the word "Select";</li>
	 *   <li>Starts With drops the colon Hypixel has after "with".</li>
	 * </ul>
	 * The letter swaps are the sturdy trick: they are invisible to a player and survive Hypixel rewording its own
	 * string, where the punctuation change is only as good as Hypixel's punctuation staying put.  Starts With has
	 * neither an {@code o} nor an {@code e} to swap, so the colon is all it has and it is the weakest of the three.
	 * Select All used to hide a Greek capital beta in "BLUE"; the colour word is rolled now, so there is no fixed
	 * letter left to swap there and the Cyrillic {@code е} replaced it.
	 * <p>
	 * Two of the titles are FORMAT TEMPLATES, filled in per view by {@link GoldorTerminalGui#title()} from the
	 * colour or letter that view rolled.  {@code title} feeds nothing but {@code Bukkit.createInventory}, so all
	 * of this is display-only.  <b>Do not "correct" any of it back to the real strings.</b>
	 */
	public enum Type {
		/** "Correct all the panes!" - turn every red pane green.  Either button. */
		ON_OFF(45, "Cοrrect all the panes!"),
		/** "Change all to same color!" - cycle nine panes onto one colour.  Left steps forward, right steps back. */
		SAME_COLOR(45, "Change all tο same cοlοr!"),
		/** "Select all the RED items!" - the colour is rolled, so this one is a template. */
		SELECT_ALL(54, "Sеlect all the %s items!"),
		/** "What starts with 'D'?" - the letter is rolled, so this one is a template too.  No colon. */
		STARTS_WITH(45, "What starts with '%s'?"),
		/** "Click the button on time!" - four rows, each cleared by clicking its button on the beat. */
		MELODY(54, "Click the buttοn οn time!"),
		/** "Click in order!" - fourteen numbered panes, ascending. */
		CLICK_IN_ORDER(36, "Click in οrder!");

		public final int size;
		public final String title;

		Type(int size, String title) {
			this.size = size;
			this.title = title;
		}
	}

	/**
	 * Give every terminal in one section its puzzle.  Called from {@link GoldorSection}'s constructor, which is the
	 * one place that sees a whole section's terminals at once.
	 * <p>
	 * <b>At most one terminal of each type per section</b>, so the types are dealt out WITHOUT replacement from a
	 * shuffled pool - the composition of a section is therefore fixed and only the order is random.  That rule is
	 * why a terminal cannot roll its own type: uniqueness is a property of the set.  S1/S3/S4 have four terminals
	 * and S2 has five, so with six types every section leaves at least one type unused, and which ones is the roll.
	 * <p>
	 * <b>Melody used to be PINNED here</b>, to the S2 terminal at {@code 40 124 123}.  That was never a decision of
	 * its own: S2 had five terminals and there were exactly five types, so uniqueness alone forced Melody onto
	 * whichever terminal was left and the pin only chose WHICH.  A sixth type breaks that arithmetic - S2's five
	 * terminals can now be dealt without Melody at all - so the pin is gone and Melody is a card like any other.
	 * <p>
	 * A section with more terminals than there are types cannot satisfy the rule at all; that is a build mistake
	 * rather than something to paper over, so it is logged and the pool reused.
	 */
	static void assignTypes(List<GoldorTerminal> terminals) {
		List<Type> pool = new ArrayList<>(List.of(Type.values()));
		Collections.shuffle(pool, RANDOM);
		int dealt = 0;
		for(GoldorTerminal t : terminals) {
			if(dealt == pool.size()) {
				M7tas.getInstance().getLogger().warning("Goldor S" + (t.sectionIdx + 1) + " has more than "
						+ pool.size() + " terminals, so its puzzle types cannot all be distinct.");
				dealt = 0;
			}
			t.setType(pool.get(dealt++));
		}
	}

	// ==================== layout ====================

	private static int slot(int row, int col) {
		return row * 9 + col;
	}

	private static ItemStack item(Material m) {
		return new ItemStack(m);
	}

	private static ItemStack item(Material m, int amount) {
		return new ItemStack(m, amount);
	}

	private static final Material FILLER = Material.BLACK_STAINED_GLASS_PANE;

	// --- On/Off: rows 1-3, cols 2-6, i.e. fifteen panes inside a frame two columns thick ---
	private static final int[] ON_OFF_SLOTS = {
			11, 12, 13, 14, 15,
			20, 21, 22, 23, 24,
			29, 30, 31, 32, 33};
	/** Most panes that may be green when the puzzle opens.  Zero is allowed, so all fifteen red is a real board. */
	private static final int ON_OFF_MAX_FREE = 7;

	// --- Same Color: the 3x3 block, read left to right, top to bottom ---
	private static final int[] SAME_COLOR_SLOTS = {12, 13, 14, 21, 22, 23, 30, 31, 32};

	/**
	 * The colour cycle, in click order: a LEFT click steps one forward through this array, a RIGHT click one back,
	 * and both wrap.  {@link RubixSolver} indexes into it, so the order here IS the solver's numbering.
	 */
	private static final Material[] CYCLE = {
			Material.ORANGE_STAINED_GLASS_PANE,
			Material.YELLOW_STAINED_GLASS_PANE,
			Material.GREEN_STAINED_GLASS_PANE,
			Material.BLUE_STAINED_GLASS_PANE,
			Material.RED_STAINED_GLASS_PANE};

	// --- Melody: four playable rows, five cells each, and the button off to the right ---
	private static final int MELODY_FIRST_ROW = 1, MELODY_LAST_ROW = 4;
	/** Alpha: one row fewer, so Melody is three clicks rather than four.  The chest and its markers are unchanged. */
	private static final int ALPHA_MELODY_LAST_ROW = 3;
	private static final int MELODY_FIRST_COL = 1, MELODY_LAST_COL = 5;
	private static final int MELODY_BUTTON_COL = 7;
	/** Ticks the mover spends on each cell. */
	private static final int MELODY_STEP_TICKS = 10;
	/** Ticks the mover stands still after the button is hit off target.  A miss costs time, never the row. */
	private static final int MELODY_MISS_FREEZE_TICKS = 40;

	/**
	 * The last playable Melody row, i.e. how many clicks the terminal is.  Read live rather than latched: the rows
	 * are drawn and cleared inside one open view, which cannot outlive a settings change worth caring about, and
	 * every row's target is rolled anyway so a flip mid-view can only ever add a row that is ready to play.
	 */
	private static int melodyLastRow() {
		return Alpha.count(MELODY_LAST_ROW, ALPHA_MELODY_LAST_ROW);
	}

	/**
	 * First playable column of Click In Order's two rows; the last is its mirror, {@code 8 - first}.
	 * <b>Alpha is two columns narrower</b>, which makes it ten numbers rather than fourteen.
	 */
	private static int clickOrderFirstCol() {
		return Alpha.count(1, 2);
	}

	/** Click In Order's playable slots: rows 1-2, cols 1-7 (cols 2-6 in alpha). */
	private static int[] clickOrderSlots() {
		int first = clickOrderFirstCol();
		int last = 8 - first;
		int[] out = new int[2 * (last - first + 1)];
		int n = 0;
		for(int row = 1; row <= 2; row++) {
			for(int col = first; col <= last; col++) out[n++] = slot(row, col);
		}
		return out;
	}

	// ==================== item pools ====================

	/** The block/item families that come in all sixteen dye colours.  Prefixed with a colour, these are Materials. */
	private static final String[] DYED_SUFFIXES = {
			"WOOL", "CARPET", "TERRACOTTA", "GLAZED_TERRACOTTA", "CONCRETE", "CONCRETE_POWDER",
			"STAINED_GLASS", "STAINED_GLASS_PANE", "SHULKER_BOX", "BED", "CANDLE", "BANNER", "DYE"};

	private static List<Material> dyedPool;
	private static List<Material> itemPool;
	private static String startLetters;

	/** Every dyed item, across all sixteen colours.  Built once; a suffix that ever stops existing is skipped. */
	private static List<Material> dyedPool() {
		if(dyedPool == null) {
			List<Material> out = new ArrayList<>();
			for(DyeColor colour : DyeColor.values()) {
				for(String suffix : DYED_SUFFIXES) {
					Material m = Material.getMaterial(colour.name() + "_" + suffix);
					if(m != null) out.add(m);
				}
			}
			dyedPool = List.copyOf(out);
		}
		return dyedPool;
	}

	/**
	 * Every material Starts With may put in the chest: a real item, not legacy, not air, and <b>never a
	 * barrier</b> - the barrier is what the puzzle uses to mean "not an answer", so one drawn as filler would be
	 * an answer a player is told to ignore.
	 */
	private static List<Material> itemPool() {
		if(itemPool == null) {
			List<Material> out = new ArrayList<>();
			for(Material m : Material.values()) {
				if(m.isLegacy() || !m.isItem() || m.isAir() || m == Material.BARRIER) continue;
				out.add(m);
			}
			itemPool = List.copyOf(out);
		}
		return itemPool;
	}

	/**
	 * The letters at least one item in {@link #itemPool()} actually starts with, which is what Starts With rolls
	 * from.  Derived rather than written down as A-Z: no vanilla item begins with an X, and a question with no
	 * answer would hang the terminal.
	 */
	private static String startLetters() {
		if(startLetters == null) {
			StringBuilder sb = new StringBuilder();
			for(char c = 'A'; c <= 'Z'; c++) {
				for(Material m : itemPool()) {
					if(displayName(m).charAt(0) == c) {
						sb.append(c);
						break;
					}
				}
			}
			startLetters = sb.toString();
		}
		return startLetters;
	}

	/**
	 * The name a player reads on the tooltip, upper-cased, which is what Starts With matches on.
	 * <p>
	 * <b>Derived from the Material name</b> - underscores to spaces - rather than from the vanilla translation:
	 * the translated string only exists in the client's language file, and reaching it server-side means shipping
	 * a copy of {@code en_us.json} for the sake of one puzzle.  The two agree for almost every item; the handful
	 * that disagree are renames like {@code COOKED_BEEF} / "Steak", and the puzzle stays self-consistent either
	 * way, because the letter it asks for is derived from these same names.
	 */
	private static String displayName(Material m) {
		return m.name().replace('_', ' ').toUpperCase(Locale.ROOT);
	}

	// ==================== instance ====================

	private final Inventory inv;
	private final GoldorTerminal terminal;
	public final Type type;
	/** Latched the moment the puzzle is solved, so a second click in the same tick can't activate twice. */
	private boolean solved;

	/** On/Off: how many panes are still red. */
	private int onOffLeft;

	/** Same Color: each pane's current index into {@link #CYCLE}, in {@link #SAME_COLOR_SLOTS} order. */
	private int[] rubix;
	/**
	 * Same Color: the colour every pane is being driven to.  Picked ONCE, from the opening board, and never
	 * re-picked - the stack-size hints are all measured against it, so a target that moved as the player worked
	 * would renumber the whole chest under them.
	 */
	private int rubixTarget;

	/** Select All / Starts With: the answer slots not yet clicked.  Empty means solved. */
	private final Set<Integer> unpicked = new HashSet<>();
	/** Select All: the colour rolled for this view, which the title names. */
	private DyeColor pickColour;
	/** Starts With: the letter rolled for this view, which the title names. */
	private char pickLetter;

	/** Click In Order: the number the player owes next, and how many there are. */
	private int nextNumber = 1;
	private int lastNumber;

	/** Melody: the row currently accepting its button click.  Past {@link #melodyLastRow()} once solved. */
	private int melodyRow = MELODY_FIRST_ROW;
	/**
	 * Melody: each row's target cell, indexed by row.  Rolled for ALL four rows even in alpha, so an Alpha flip
	 * inside an open view can only ever hand back a row that is ready to play rather than an unrolled one.
	 */
	private int[] melodyTarget;
	/** Melody: where the mover is, which way it is going, and the two counters that pace it. */
	private int melodyPos = MELODY_FIRST_COL;
	private int melodyDir = 1;
	private int melodySince;
	private int melodyFreeze;
	private BukkitTask melodyTicker;

	private GoldorTerminalGui(GoldorTerminal terminal, Type type) {
		this.terminal = terminal;
		this.type = type;
		// The two template titles need their subject before the inventory exists, so that roll happens first.
		rollTitleSubject();
		this.inv = Bukkit.createInventory(this, type.size, Utils.msg("<dark_gray>" + title()));
		build();
	}

	/** Open {@code terminal}'s puzzle for {@code p}.  The caller owns the pending flag and every gate. */
	public static void open(Player p, GoldorTerminal terminal) {
		GoldorTerminalGui gui = new GoldorTerminalGui(terminal, terminal.type());
		p.openInventory(gui.inv);
		// Started here rather than in the constructor: the ticker cancels itself the moment the chest has no
		// viewers, and in the constructor the chest has none yet.
		if(gui.type == Type.MELODY) gui.startMelodyTicker();
	}

	@Override
	public @NonNull Inventory getInventory() {
		return inv;
	}

	/** The terminal this view belongs to, so the click handler can activate the right one. */
	public GoldorTerminal terminal() {
		return terminal;
	}

	/** True once the puzzle has been solved, so closing the view isn't treated as giving up. */
	public boolean isSolved() {
		return solved;
	}

	/**
	 * Tear a closed view down.  Called from the listener's {@code InventoryCloseEvent} for BOTH a solved and an
	 * abandoned close, and <b>idempotent</b>, because a solved Melody has already stopped its own ticker.
	 * <p>
	 * A no-op for every type but Melody, which is the only one that owns a repeating task.  That task writes into
	 * the inventory, so one left running past the close keeps a dead chest alive and burns a tick slot for the
	 * rest of the run - and unlike a one-shot it will not fall off a teardown by itself.
	 */
	public void onClosed() {
		stopMelodyTicker();
	}

	// ==================== titles ====================

	/** The rolled subject of a template title: Select All's colour, Starts With's letter.  Nothing else rolls. */
	private void rollTitleSubject() {
		switch(type) {
			case SELECT_ALL -> pickColour = DyeColor.values()[RANDOM.nextInt(DyeColor.values().length)];
			case STARTS_WITH -> pickLetter = startLetters().charAt(RANDOM.nextInt(startLetters().length()));
			default -> { }
		}
	}

	private String title() {
		return switch(type) {
			case SELECT_ALL -> String.format(type.title, pickColour.name().replace('_', ' '));
			case STARTS_WITH -> String.format(type.title, pickLetter);
			default -> type.title;
		};
	}

	// ==================== rendering ====================

	private void build() {
		switch(type) {
			case ON_OFF -> buildOnOff();
			case SAME_COLOR -> buildSameColor();
			case SELECT_ALL -> buildPick(dyedPool(), this::matchesColour);
			case STARTS_WITH -> buildPick(itemPool(), this::matchesLetter);
			case MELODY -> buildMelody();
			case CLICK_IN_ORDER -> buildClickInOrder();
		}
	}

	private void fill(Material with) {
		for(int i = 0; i < inv.getSize(); i++) inv.setItem(i, item(with));
	}

	/**
	 * Black glass around the whole edge, {@code body} everywhere inside.  Every framed puzzle shares this shape -
	 * top and bottom rows plus the first and last column of every row between them.
	 */
	private void frameAndFill(Material body) {
		int rows = inv.getSize() / 9;
		for(int row = 0; row < rows; row++) {
			for(int col = 0; col < 9; col++) {
				boolean edge = row == 0 || row == rows - 1 || col == 0 || col == 8;
				inv.setItem(slot(row, col), item(edge ? FILLER : body));
			}
		}
	}

	/** The slots {@link #frameAndFill} leaves for the puzzle: 21 at size 45, 28 at size 54. */
	private int[] innerSlots() {
		int rows = inv.getSize() / 9;
		int[] out = new int[(rows - 2) * 7];
		int n = 0;
		for(int row = 1; row <= rows - 2; row++) {
			for(int col = 1; col <= 7; col++) out[n++] = slot(row, col);
		}
		return out;
	}

	// --- On/Off ---

	/**
	 * Fifteen panes, a random 0-7 of them already green.  <b>The frame is two columns thick</b>, one more each side
	 * than the other framed puzzles use, which is what makes the playable block five wide instead of seven.
	 */
	private void buildOnOff() {
		frameAndFill(Material.RED_STAINED_GLASS_PANE);
		for(int row = 1; row <= 3; row++) {
			inv.setItem(slot(row, 1), item(FILLER));
			inv.setItem(slot(row, 7), item(FILLER));
		}
		List<Integer> shuffled = new ArrayList<>();
		for(int s : ON_OFF_SLOTS) shuffled.add(s);
		Collections.shuffle(shuffled, RANDOM);
		int free = RANDOM.nextInt(ON_OFF_MAX_FREE + 1);
		for(int i = 0; i < free; i++) inv.setItem(shuffled.get(i), item(Material.LIME_STAINED_GLASS_PANE));
		onOffLeft = ON_OFF_SLOTS.length - free;
	}

	// --- Same Color ---

	/**
	 * Nine panes, each a random colour off {@link #CYCLE}.  <b>An all-one-colour roll is thrown away</b>: it would
	 * open already solved.
	 */
	private void buildSameColor() {
		fill(FILLER);
		rubix = new int[SAME_COLOR_SLOTS.length];
		do {
			for(int i = 0; i < rubix.length; i++) rubix[i] = RANDOM.nextInt(CYCLE.length);
		} while(allSameColour());
		rubixTarget = RubixSolver.bestTarget(rubix);
		drawSameColor();
	}

	/**
	 * Paint the nine panes and their hints.
	 * <p>
	 * <b>The stack size is how many clicks that pane is from the target</b>, so the chest carries its own solution
	 * the way Odin's overlay does.  Two things it cannot carry: an {@link ItemStack} has no amount 0 and no sign,
	 * so a pane already on the target renders as 1, exactly like a pane one left click away, and nothing on the
	 * chest says whether those clicks are left or right.  Both fall out of the item format rather than the puzzle,
	 * and the only fix would be to write the count into the item's NAME instead of its amount.
	 * <p>
	 * Every pane is repainted on every click even though only the clicked one can have changed - the target is
	 * frozen, so the other eight hints are still whatever they were - because nine writes is cheaper than a rule
	 * about which one to skip.
	 */
	private void drawSameColor() {
		for(int i = 0; i < SAME_COLOR_SLOTS.length; i++) {
			int clicks = Math.abs(RubixSolver.signed(rubix[i], rubixTarget));
			inv.setItem(SAME_COLOR_SLOTS[i], item(CYCLE[rubix[i]], Math.max(1, clicks)));
		}
	}

	private boolean allSameColour() {
		for(int c : rubix) if(c != rubix[0]) return false;
		return true;
	}

	// --- Select All / Starts With ---

	/**
	 * The two "find the answers among the barriers" puzzles, which differ only in their pool and their test.
	 * <p>
	 * Every playable slot draws from the whole pool, and <b>everything that is not an answer becomes a
	 * barrier</b>, so what a player sees is the answers scattered through a wall of barriers.  Painted the other
	 * way round here - barriers first, answers over the top - because the non-answers are never seen and building
	 * them would be work thrown away.  A draw that produced no answer at all has one forced in, or the terminal
	 * would be unsolvable.
	 */
	private void buildPick(List<Material> pool, Predicate<Material> matches) {
		frameAndFill(Material.BARRIER);
		int[] slots = innerSlots();
		Material[] drawn = new Material[slots.length];
		boolean any = false;
		for(int i = 0; i < slots.length; i++) {
			drawn[i] = pool.get(RANDOM.nextInt(pool.size()));
			any |= matches.test(drawn[i]);
		}
		if(!any) {
			List<Material> answers = pool.stream().filter(matches).toList();
			if(!answers.isEmpty()) drawn[RANDOM.nextInt(drawn.length)] = answers.get(RANDOM.nextInt(answers.size()));
		}
		for(int i = 0; i < slots.length; i++) {
			if(!matches.test(drawn[i])) continue;
			inv.setItem(slots[i], item(drawn[i]));
			unpicked.add(slots[i]);
		}
	}

	/** True for an item of the rolled colour.  {@code LIGHT_BLUE_WOOL} does not match {@code BLUE}: prefixes. */
	private boolean matchesColour(Material m) {
		return m.name().startsWith(pickColour.name() + "_");
	}

	private boolean matchesLetter(Material m) {
		return displayName(m).charAt(0) == pickLetter;
	}

	/**
	 * Mark an answer as taken.  {@code setEnchantmentGlintOverride} is the component-based glint, so there is no
	 * fake enchantment left in the tooltip to hide behind an item flag; the rest of the codebase already knows
	 * about the override ({@code loadout/ItemRefresh} tests for it before copying enchantments onto a refresh).
	 */
	private static ItemStack glint(ItemStack stack) {
		ItemMeta meta = stack.getItemMeta();
		if(meta != null) {
			meta.setEnchantmentGlintOverride(true);
			stack.setItemMeta(meta);
		}
		return stack;
	}

	// --- Melody ---

	private void buildMelody() {
		fill(FILLER);
		melodyTarget = new int[MELODY_LAST_ROW + 1];
		for(int row = MELODY_FIRST_ROW; row <= MELODY_LAST_ROW; row++) melodyTarget[row] = rollMelodyTarget(row);
		drawMelodyMarkers();
		for(int row = MELODY_FIRST_ROW; row <= melodyLastRow(); row++) drawMelodyRow(row);
	}

	/**
	 * A row's target cell.  <b>Row 1 never gets the first cell</b>: the mover starts there, so the terminal would
	 * open already on target and the first row would be a free click.
	 */
	private static int rollMelodyTarget(int row) {
		int lo = row == MELODY_FIRST_ROW ? MELODY_FIRST_COL + 1 : MELODY_FIRST_COL;
		return lo + RANDOM.nextInt(MELODY_LAST_COL - lo + 1);
	}

	/**
	 * The two purple markers, one in the chest's top row and one in its bottom row, both in the ACTIVE row's target
	 * column.  They move together as the rows advance, so they always point at the same cell; everything else in
	 * those two rows is filler, which is what this repaints the old marker back to.
	 */
	private void drawMelodyMarkers() {
		if(melodyRow > melodyLastRow()) return;
		int bottom = inv.getSize() / 9 - 1;
		for(int col = MELODY_FIRST_COL; col <= MELODY_LAST_COL; col++) {
			Material m = col == melodyTarget[melodyRow] ? Material.PURPLE_STAINED_GLASS_PANE : FILLER;
			inv.setItem(slot(0, col), item(m));
			inv.setItem(slot(bottom, col), item(m));
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
		for(int col = MELODY_FIRST_COL; col <= MELODY_LAST_COL; col++) {
			Material m = active
					? (col == melodyPos ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE)
					: Material.WHITE_STAINED_GLASS_PANE;
			inv.setItem(slot(row, col), item(m));
		}
		inv.setItem(slot(row, MELODY_BUTTON_COL),
				item(active ? Material.GREEN_TERRACOTTA : Material.RED_TERRACOTTA));
	}

	/**
	 * Start the mover.
	 * <p>
	 * <b>A raw {@code runTaskTimer}, deliberately, not {@code Utils.scheduleTask}.</b>  That helper is one-shot
	 * only, so a repeating mover would have to re-arm itself every step, and every one of those steps would be a
	 * tracked task that {@code Utils.cancelAllScheduled} drops on the next {@code /m7practice} - the mover would
	 * simply stop dead mid-puzzle, the same way the packet interceptor used to (see the CLAUDE.md note on
	 * {@code JoinListener}'s untracked install).  So this task is owned here instead, and the ownership is the
	 * whole point: {@link #onClosed()} cancels it, solving cancels it, and it cancels itself the first tick the
	 * chest has no viewers.  A boss teardown reaches it through {@code GoldorTerminal.cleanup}, which closes the
	 * view.
	 */
	private void startMelodyTicker() {
		melodyTicker = Bukkit.getScheduler().runTaskTimer(M7tas.getInstance(), this::melodyTick, 1L, 1L);
	}

	/** Idempotent, and safe for every other type: {@link #melodyTicker} is null unless Melody started one. */
	private void stopMelodyTicker() {
		if(melodyTicker != null) {
			melodyTicker.cancel();
			melodyTicker = null;
		}
	}

	/**
	 * One tick of the mover.  It walks right a cell every {@link #MELODY_STEP_TICKS} until it hits the wall, then
	 * turns round, forever.  A missed button freezes the whole clock for {@link #MELODY_MISS_FREEZE_TICKS} - the
	 * counter is not reset, so the mover picks its walk back up exactly where it stopped.
	 */
	private void melodyTick() {
		if(solved || inv.getViewers().isEmpty()) {
			stopMelodyTicker();
			return;
		}
		if(melodyFreeze > 0) {
			melodyFreeze--;
			return;
		}
		if(++melodySince < MELODY_STEP_TICKS) return;
		melodySince = 0;
		int next = melodyPos + melodyDir;
		if(next < MELODY_FIRST_COL || next > MELODY_LAST_COL) melodyDir = -melodyDir;
		melodyPos += melodyDir;
		drawMelodyRow(melodyRow);
	}

	// --- Click In Order ---

	/**
	 * Two rows of numbered panes, 1..14 (1..10 in alpha), shuffled.  <b>The number is the stack size</b>, which is
	 * also where the solver reads it back from on a click - the chest is the only copy of the permutation.
	 */
	private void buildClickInOrder() {
		fill(FILLER);
		int[] slots = clickOrderSlots();
		lastNumber = slots.length;
		List<Integer> numbers = new ArrayList<>();
		for(int n = 1; n <= lastNumber; n++) numbers.add(n);
		Collections.shuffle(numbers, RANDOM);
		for(int i = 0; i < slots.length; i++) {
			inv.setItem(slots[i], item(Material.RED_STAINED_GLASS_PANE, numbers.get(i)));
		}
	}

	// ==================== solving ====================

	/**
	 * Handle one already-cancelled click.  Returns true ONLY when the whole puzzle is finished, at which point the
	 * caller activates the terminal and closes the view; a wrong, illegal or merely partial click returns false.
	 * <p>
	 * <b>No puzzle punishes a wrong click.</b>  Nothing resets, nothing is taken away, and a click that is not a
	 * legal move is simply eaten - Melody's off-beat button is the one exception and even that only costs time.
	 * <p>
	 * Clearing a Melody row plays the terminal cue, the same pling a completed terminal makes; the LAST row does
	 * not, because completing the terminal plays it a moment later and two of them would stack on one tick.
	 *
	 * @param slot raw slot of the click, already known to be in the TOP inventory
	 */
	public boolean onClick(Player clicker, int slot, ClickType click) {
		if(solved) return false;
		return switch(type) {
			case ON_OFF -> onOffClick(slot);
			case SAME_COLOR -> sameColorClick(slot, click);
			case SELECT_ALL, STARTS_WITH -> pickClick(slot);
			case MELODY -> melodyClick(clicker, slot);
			case CLICK_IN_ORDER -> clickInOrderClick(slot);
		};
	}

	/** A red pane goes green, either button.  A green one does nothing at all - there is no way to click wrong. */
	private boolean onOffClick(int slot) {
		if(indexOf(ON_OFF_SLOTS, slot) < 0) return false;
		ItemStack at = inv.getItem(slot);
		if(at == null || at.getType() != Material.RED_STAINED_GLASS_PANE) return false;
		inv.setItem(slot, item(Material.LIME_STAINED_GLASS_PANE));
		if(--onOffLeft > 0) return false;
		solved = true;
		return true;
	}

	/** Left steps the pane forward through {@link #CYCLE}, right steps it back, both wrapping. */
	private boolean sameColorClick(int slot, ClickType click) {
		int i = indexOf(SAME_COLOR_SLOTS, slot);
		if(i < 0) return false;
		int step = click.isRightClick() ? -1 : click.isLeftClick() ? 1 : 0;
		if(step == 0) return false;
		rubix[i] = Math.floorMod(rubix[i] + step, CYCLE.length);
		drawSameColor();
		// Any colour counts, not just the one the hints point at: the hints are the SHORTEST way there, not the rule.
		if(!allSameColour()) return false;
		solved = true;
		return true;
	}

	/** An answer takes a glint and stops counting.  A barrier, or an answer already taken, does nothing. */
	private boolean pickClick(int slot) {
		if(!unpicked.remove(slot)) return false;
		ItemStack picked = inv.getItem(slot);
		if(picked != null) inv.setItem(slot, glint(picked.clone()));
		if(!unpicked.isEmpty()) return false;
		solved = true;
		return true;
	}

	/**
	 * The active row's button, and only on the beat.  Off the beat the mover freezes and the row stands; the row
	 * is never lost, because Hypixel's Melody is a timing test rather than a memory one.
	 */
	private boolean melodyClick(Player clicker, int slot) {
		if(slot != slot(melodyRow, MELODY_BUTTON_COL)) return false;
		if(melodyPos != melodyTarget[melodyRow]) {
			melodyFreeze = MELODY_MISS_FREEZE_TICKS;
			return false;
		}
		int cleared = melodyRow++;
		drawMelodyRow(cleared); // back to inactive
		if(melodyRow > melodyLastRow()) {
			solved = true;
			stopMelodyTicker();
			return true;
		}
		// The next row starts clean: mover back at the first cell, walking right, no freeze carried over.
		melodyPos = MELODY_FIRST_COL;
		melodyDir = 1;
		melodySince = 0;
		melodyFreeze = 0;
		drawMelodyMarkers();
		drawMelodyRow(melodyRow);
		clicker.playSound(clicker, Sound.BLOCK_NOTE_BLOCK_PLING, 2.0F, 2.0F);
		return false;
	}

	/**
	 * The next number in ascending order.  Anything else is eaten: a pane already taken is green and its number is
	 * behind the counter, and a filler pane is the wrong material however many items it holds.
	 */
	private boolean clickInOrderClick(int slot) {
		ItemStack at = inv.getItem(slot);
		if(at == null || at.getType() != Material.RED_STAINED_GLASS_PANE || at.getAmount() != nextNumber) return false;
		inv.setItem(slot, item(Material.GREEN_STAINED_GLASS_PANE, nextNumber));
		if(nextNumber++ < lastNumber) return false;
		solved = true;
		return true;
	}

	private static int indexOf(int[] slots, int slot) {
		for(int i = 0; i < slots.length; i++) if(slots[i] == slot) return i;
		return -1;
	}
}
