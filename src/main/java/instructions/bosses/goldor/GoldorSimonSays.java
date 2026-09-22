package instructions.bosses.goldor;

import listeners.GoldorListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import plugin.Alpha;
import plugin.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The real S1 "Simon Says" device: the 4x4 obsidian grid lights a sequence and the party plays it back on the
 * buttons in front of it.
 * <p>
 * <b>Realistic mode only</b> ({@code damage.Difficulty.realPuzzles()}).  Classic and Perfect RNG keep the 15-click
 * stand-in, which stays where it always was in {@code GoldorListener.processSimonClick} - two devices behind one
 * entry point, the same shape the Sharp Shooter has had all along.  Nothing in here runs in the other two modes.
 * <p>
 * <b>The run, in order:</b>
 * <ol>
 *   <li>A right-click on the start button begins the device and opens a {@value #SPAM_WINDOW_TICKS}-tick spam
 *       window.  That opening click is click #1.</li>
 *   <li>Every further start-button click inside the window queues another lantern.  Counted <b>per player</b>,
 *       because skips are per player.</li>
 *   <li>The window closes and each player's own count becomes skips off {@link #SKIP_THRESHOLDS}, largest
 *       threshold reached; the skips are then SUMMED across players.  Three people clicking 6, 6 and 3 contribute
 *       2 + 2 + 1 = 5.</li>
 *   <li>Enough skips to start past the last phase completes the device on the spot.</li>
 *   <li>Otherwise {@code N} lanterns play back to back and the answer is the last {@code S + 1} of them, which
 *       then becomes the running sequence.</li>
 *   <li>The lanterns stop, the 16 buttons go UP, and the party plays the sequence back on them.</li>
 *   <li>The answer lands, the buttons come DOWN, and each correct answer appends one new cell and replays the
 *       whole sequence.</li>
 *   <li>Answering a sequence of {@link #targetLength()} completes the device.</li>
 * </ol>
 *
 * <p><b>The buttons are part of the device, not scenery.</b>  The user's rule, verbatim: <i>"The buttons show up
 * when the sea lantern sequence finishes.  They disappear after the player correctly inputs the solution for each
 * step."</i>  <b>"For each step" is read as per PHASE, not per press</b> - the 16 go up together the moment playback
 * ends, stay up for the whole input window, and all come down together the moment that phase's answer is complete.
 * That is the only reading that leaves a sequence longer than one cell playable.  See {@link #placeButtons} /
 * {@link #removeButtons}.
 *
 * <p><b>The device solves itself.</b>  The button it is waiting for is OAK and the other fifteen are stone, so the
 * answer is readable straight off the wall - see {@link #paintButtons}.  Same bargain as Same Color's click-count
 * hints in {@code GoldorTerminalGui}: the plugin ships the solver rather than leaving it to Odin, and the playback
 * becomes a formality.
 *
 * <p><b>A wrong button is a no-op</b> - no reset, no penalty, nothing.  The spec gives re-clicking the start button
 * as the reset, and says nothing about punishing a misclick; every other puzzle in this plugin ignores an incorrect
 * click too.  Hypixel's own device resets the sequence on a wrong press, so this is the one rule here that is a
 * deliberate divergence rather than a transcription - change it here if that turns out to be wanted.
 */
public final class GoldorSimonSays {

	/** Single instance, reached statically the same way {@code Goldor.INSTANCE} is. */
	public static final GoldorSimonSays INSTANCE = new GoldorSimonSays();

	/**
	 * <b>The one coordinate table.</b>  The 4x4 obsidian grid, {@code [row][column] -> {x, y, z}}: row 0 is the TOP
	 * row ({@code y = 123}) and column 0 is {@code z = 92}, so the table reads the way the wall looks to a player
	 * standing west of it.  The grid's exposed face is WEST ({@code x = 110} is air), which is why the buttons come
	 * out at {@code x - 1}.
	 * <p>
	 * <b>Read out of the project's world file</b> (and cross-checked against the live {@code m7-1 test} world, which
	 * is identical) - <b>pending the user's confirmation</b>.  Everything downstream derives from here:
	 * {@link #BUTTONS} is computed from it and nothing else re-derives a coordinate, so moving the device is a
	 * one-edit change.
	 * <p>
	 * Two things the world file says that are worth knowing before touching this:
	 * <ul>
	 *   <li><b>There is no hidden lantern layer.</b>  {@code x = 112} behind the grid is plain stone bricks, which is
	 *       why the flash is a block swap on the obsidian itself ({@link #flash}) rather than a light behind it.</li>
	 *   <li><b>The 16 buttons are not in the world at rest.</b>  The only permanent stone button in the device is
	 *       the START button at {@code 110 121 91}, on the emerald frame pillar outside the grid.  The 16 are put
	 *       up and taken down by {@link #placeButtons} / {@link #removeButtons} around each input window, so
	 *       outside one the slots hold whatever the map has there - and one of them, {@code 110 121 93} (row
	 *       {@code y=121}, column {@code z=93}), holds the {@code i1} sign, which is why the teardown restores
	 *       {@link org.bukkit.block.BlockState}s rather than block data.</li>
	 * </ul>
	 */
	private static final int[][][] GRID = {
			{{111, 123, 92}, {111, 123, 93}, {111, 123, 94}, {111, 123, 95}},  // top row
			{{111, 122, 92}, {111, 122, 93}, {111, 122, 94}, {111, 122, 95}},
			{{111, 121, 92}, {111, 121, 93}, {111, 121, 94}, {111, 121, 95}},
			{{111, 120, 92}, {111, 120, 93}, {111, 120, 94}, {111, 120, 95}},  // bottom row
	};

	private static final int ROWS = GRID.length;
	private static final int COLS = GRID[0].length;
	/** How many cells the grid has, and therefore the hard ceiling on a sequence length.  See {@link #CELL_COUNT} uses. */
	private static final int CELL_COUNT = ROWS * COLS;

	/**
	 * How far the button for a cell sits from the cell itself, on X.  The grid's exposed face is west, so a button
	 * hangs one block west of its obsidian at the same {@code y} and {@code z} - {@code face=wall, facing=west}, the
	 * convention the existing start button already proves.
	 */
	private static final int BUTTON_DX = -1;

	/** The 16 button positions, DERIVED from {@link #GRID}.  Never author one of these by hand. */
	private static final int[][] BUTTONS = new int[CELL_COUNT][];

	static {
		for(int i = 0; i < CELL_COUNT; i++) {
			int[] c = cell(i);
			BUTTONS[i] = new int[]{c[0] + BUTTON_DX, c[1], c[2]};
		}
	}

	/** How long one lantern stays lit, and therefore the playback step: they run back to back with no gap. */
	private static final int FLASH_TICKS = 10;

	/** How long the start button accepts spam clicks after the opening one. */
	private static final int SPAM_WINDOW_TICKS = 10;

	/**
	 * Clicks needed for 1, 2, 3, 4 and 5 skips - the largest threshold a player reaches is their skip count.
	 * <p>
	 * <b>Per player</b>, then summed.  Note the per-tick click dedupe in
	 * {@code GoldorListener.tryRegisterSimonClick} caps ONE player at one click per tick, so a single person tops
	 * out at {@value #SPAM_WINDOW_TICKS} clicks (3 skips) inside the window and the last two thresholds are a
	 * team's to reach.
	 */
	private static final int[] SKIP_THRESHOLDS = {3, 6, 10, 15, 21};

	private enum State {
		/** Nothing running: the start button opens a window. */
		IDLE,
		/** Inside the spam window: start clicks are counted, not a reset. */
		SPAM,
		/** Lanterns are playing.  Nothing a player presses on the grid counts. */
		PLAYING,
		/** Waiting for the sequence to be played back on the buttons. */
		AWAITING
	}

	private State state = State.IDLE;
	/** The running sequence, as cell indices 0..15.  Always distinct, which is what makes a gapless playback readable. */
	private final List<Integer> sequence = new ArrayList<>();
	/** How much of {@link #sequence} the party has pressed correctly in this phase. */
	private int inputIdx = 0;
	/** Clicks inside the spam window, per player.  Skips are per player, so this is never collapsed to a total. */
	private final Map<UUID, Integer> spamClicks = new HashMap<>();
	/** Server tick the spam window opened on, or -1.  The window is judged by tick arithmetic, not by task order. */
	private int windowStartTick = -1;
	/** Who opened the window, so a short-circuited device has somebody to credit. */
	private UUID starterId;
	/**
	 * Bumped by every reset and every fresh start.  Each queued playback step captures it and does nothing if it no
	 * longer matches, so a re-click on the start button orphans the run in flight rather than racing it.
	 */
	private int runToken = 0;
	/** World the device is running in, remembered so the force-restore never has to guess one. */
	private World deviceWorld;
	/**
	 * Cells currently swapped to sea lantern, with the block data they replaced.
	 * <p>
	 * This is the whole reason {@link #cleanup()} exists.  Playback runs on tracked {@code Utils.scheduleTask}s, so
	 * {@code Utils.cancelAllScheduled} kills the timer that would have put the obsidian back - a flash in flight when
	 * a run ends is only restored because the teardown does it synchronously.
	 */
	private final Map<Integer, BlockData> litCells = new LinkedHashMap<>();
	/**
	 * What was in each of the 16 button slots before the buttons went up, keyed by cell index, or empty while they
	 * are down.
	 * <p>
	 * <b>{@link BlockState}, not {@link BlockData}, because one of the slots is the {@code i1} sign</b> - a sign is
	 * a block entity, and writing over it with block data alone throws its text away for good.  A state carries the
	 * text and puts it back.  The sign is still correct for the stand-in device classic and Perfect RNG keep, so it
	 * has to survive a realistic run untouched.
	 * <p>
	 * Snapshotted the first time buttons go up in a run rather than once at startup, so a world reload can never
	 * leave this holding a state from a world that no longer exists.
	 */
	private final Map<Integer, BlockState> buttonSnapshot = new LinkedHashMap<>();
	/** True while the 16 buttons are in the world.  {@link #buttonSnapshot} is the restore; this is the flag. */
	private boolean buttonsUp = false;
	/** The button block, built once on first use: {@code stone_button[face=wall,facing=west]}, the same convention
	 *  the permanent start button proves.  Lazy, so nothing calls into Bukkit at class-init time. */
	private BlockData buttonData;
	/** The HINT button, same face and facing, oak instead of stone.  See {@link #paintButtons}. */
	private BlockData hintButtonData;

	private GoldorSimonSays() {
	}

	// ---------- Coordinate table accessors: the ONLY things that read GRID/BUTTONS ----------

	/** The obsidian cell at flat index {@code i} (row-major over {@link #GRID}), as {@code {x, y, z}}. */
	private static int[] cell(int i) {
		return GRID[i / COLS][i % COLS];
	}

	/**
	 * Flat index of the grid cell whose button is at {@code (bx, by, bz)}, or -1 for "not a Simon Says button".
	 * <p>
	 * Public because {@code GoldorListener} funnels both click entry points - the vanilla interact event and the raw
	 * {@code ServerboundUseItemOnPacket} path - through one guard, and that guard has to know whether the coords are
	 * ours before it spends the per-tick dedupe on them.
	 */
	public static int cellAtButton(int bx, int by, int bz) {
		for(int i = 0; i < CELL_COUNT; i++) {
			int[] b = BUTTONS[i];
			if(b[0] == bx && b[1] == by && b[2] == bz) return i;
		}
		return -1;
	}

	// ---------- Input ----------

	/**
	 * A right-click on the START button, in realistic mode.  Three meanings, decided by where the device is:
	 * opening the device, spamming inside the window, or <b>resetting it from scratch</b>.
	 * <p>
	 * The window is judged on {@link #windowStartTick} rather than on whether the close task has run, so a click
	 * landing on the boundary tick always resolves the same way whichever of the two the scheduler drains first.
	 */
	public void onStartClick(Player p, boolean wasDeferred) {
		if(GoldorListener.cannotSolve(p)) return; // the listener gates this too; a public entry point owns its own gate
		GoldorSection s1 = Goldor.INSTANCE.getSection(0);
		if(s1 == null || s1.device.isActivated()) return;
		if(state == State.SPAM && Utils.serverTick() - windowStartTick < SPAM_WINDOW_TICKS) {
			spamClicks.merge(p.getUniqueId(), 1, Integer::sum);
			Utils.debug(Utils.DebugType.BOSS, "Simon spam click by " + Utils.getRealName(p)
					+ " (" + spamClicks.get(p.getUniqueId()) + ")");
			return;
		}
		// Anything else restarts the device outright, including mid-playback and mid-answer: re-clicking start IS
		// the reset, and it is the only way out of a sequence somebody has lost track of.
		beginRun(p);
	}

	/**
	 * A right-click on one of the 16 grid buttons, in realistic mode.
	 * <p>
	 * <b>Nothing counts unless the device is waiting for an answer</b> - a press during playback is dropped, which
	 * is what stops a player racing ahead of the lanterns - and a press on the wrong cell is a plain no-op.
	 * <p>
	 * The state check is now belt and braces: the real gate is that the buttons only EXIST inside the input window,
	 * so a click outside one has nothing to land on.  It stays because the two must never be able to disagree, and
	 * because a button somebody placed by hand should still do nothing.
	 */
	public void onCellClick(Player p, int cellIdx, boolean wasDeferred) {
		if(GoldorListener.cannotSolve(p)) return; // the listener gates this too; a public entry point owns its own gate
		GoldorSection s1 = Goldor.INSTANCE.getSection(0);
		if(s1 == null || s1.device.isActivated()) return;
		if(state != State.AWAITING || sequence.isEmpty()) return;
		if(inputIdx >= sequence.size()) return;
		if(sequence.get(inputIdx) != cellIdx) return; // wrong button: no reset, no penalty
		inputIdx++;
		// The hint has to move with the answer: the button just pressed goes back to stone and the next one owed
		// becomes the oak one.
		if(inputIdx < sequence.size()) { paintButtons(); cue(p); return; }

		// The phase's answer is in, so the buttons come down.  complete() takes them down too (through
		// resetRuntime), and playBack does it on the way into the next sequence; doing it here as well is what
		// makes "down" the state between windows rather than something each branch has to remember.
		removeButtons();
		if(sequence.size() >= targetLength()) {
			complete(p, wasDeferred);
			return;
		}
		// The press that finished a PHASE still counts as progress, so it gets the cue like any other correct one.
		// Only the press that finishes the whole DEVICE is silent, because complete() -> Goldor.onActivation plays
		// its own noise a moment later and two on one tick stack into a single louder note.
		cue(p);
		appendCell();
		playBack(sequence);
	}

	/**
	 * The progress cue for a correct button: the same note-block pling the terminal puzzles play, and for the same
	 * reason - a press that lands and a press that is eaten looked identical, and this device eats every wrong one.
	 * <p>
	 * To the presser only, not the room.  A party spamming the grid would otherwise hear each other's presses as
	 * well as their own, and the point of the noise is to tell YOU that yours registered.
	 */
	private static void cue(Player p) {
		p.playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 2.0F, 2.0F);
	}

	// ---------- The run ----------

	/** Open a fresh spam window.  The opening click is that player's click #1. */
	private void beginRun(Player p) {
		resetRuntime();
		deviceWorld = p.getWorld();
		state = State.SPAM;
		windowStartTick = Utils.serverTick();
		starterId = p.getUniqueId();
		spamClicks.put(p.getUniqueId(), 1);
		final int token = runToken;
		Utils.scheduleTask(() -> { if(token == runToken) closeSpamWindow(); }, SPAM_WINDOW_TICKS);
		Utils.debug(Utils.DebugType.BOSS, "Simon Says started by " + Utils.getRealName(p));
	}

	/** Turn the window's clicks into skips, then either short-circuit the device or deal the opening sequence. */
	private void closeSpamWindow() {
		if(state != State.SPAM) return;
		GoldorSection s1 = Goldor.INSTANCE.getSection(0);
		if(s1 == null || s1.device.isActivated()) { resetRuntime(); return; }

		int clicks = 0, skips = 0;
		for(int c : spamClicks.values()) {
			clicks += c;
			skips += skipsFor(c);
		}

		// S skips means STARTING at phase S + 1, so skipping past the last phase is just finishing.  With the normal
		// target of 5 that is exactly the spec's "5 or more skips completes it"; written as the comparison rather
		// than as a literal 5 so the alpha target of 4 stays consistent instead of needing a second rule.
		if(skips + 1 > targetLength()) {
			Utils.debug(Utils.DebugType.BOSS, "Simon Says short-circuited on " + skips + " skips");
			complete(resolveSolver(), false);
			return;
		}

		// N is the total clicks, and the playback needs N DISTINCT cells, so it cannot exceed the 16 the grid has.
		// One player cannot reach 16 (the per-tick dedupe caps them at SPAM_WINDOW_TICKS clicks) and 21 clicks
		// short-circuits above, so the clamp only ever bites on a big team spread thin - five people on two clicks
		// each is 10 clicks and no skips at all, and the same five could reach 19 clicks on 4 skips.  Clamping is
		// the honest answer there: the device still plays the longest sequence the wall can hold.
		int n = Math.max(1, Math.min(clicks, CELL_COUNT));
		List<Integer> played = randomDistinctCells(n);
		int answerLen = Math.min(skips + 1, n);
		sequence.clear();
		sequence.addAll(played.subList(n - answerLen, n));
		inputIdx = 0;
		Utils.debug(Utils.DebugType.BOSS, "Simon Says: " + clicks + " clicks, " + skips + " skips, "
				+ n + " lanterns, answer is the last " + answerLen);
		// The whole of N plays, but only the tail is the answer - and that tail is what the device carries forward.
		playBack(played);
	}

	/**
	 * Light {@code toPlay} one cell at a time, {@value #FLASH_TICKS} ticks each, back to back, then take answers.
	 * <p>
	 * Back to back with no gap only reads correctly because no sequence ever repeats a cell in a row: the opening
	 * sequence is dealt distinct and {@link #appendCell} only ever adds a cell the running sequence does not already
	 * hold.  Two identical neighbours would look like one long flash.
	 */
	private void playBack(List<Integer> toPlay) {
		state = State.PLAYING;
		inputIdx = 0;
		removeButtons(); // nothing to press while the lanterns are running, and a restart can land mid-input
		final int token = ++runToken;
		for(int i = 0; i < toPlay.size(); i++) {
			final int cellIdx = toPlay.get(i);
			Utils.scheduleTask(() -> {
				if(token != runToken) return;
				restoreLitCells();
				flash(cellIdx);
			}, (long) i * FLASH_TICKS);
		}
		Utils.scheduleTask(() -> {
			if(token != runToken) return;
			restoreLitCells();
			state = State.AWAITING;
			inputIdx = 0;
			placeButtons(); // "the buttons show up when the sea lantern sequence finishes"
		}, (long) toPlay.size() * FLASH_TICKS);
	}

	/** Append one cell the running sequence does not already hold. */
	private void appendCell() {
		List<Integer> free = new ArrayList<>(CELL_COUNT);
		for(int i = 0; i < CELL_COUNT; i++) if(!sequence.contains(i)) free.add(i);
		if(free.isEmpty()) return; // unreachable at a target of 5, but a sequence must never repeat a cell
		sequence.add(free.get(ThreadLocalRandom.current().nextInt(free.size())));
	}

	/** {@code n} distinct cells in a random order. */
	private static List<Integer> randomDistinctCells(int n) {
		List<Integer> all = new ArrayList<>(CELL_COUNT);
		for(int i = 0; i < CELL_COUNT; i++) all.add(i);
		java.util.Collections.shuffle(all, ThreadLocalRandom.current());
		return new ArrayList<>(all.subList(0, Math.min(n, CELL_COUNT)));
	}

	/** How many skips {@code clicks} is worth: the largest {@link #SKIP_THRESHOLDS} entry reached. */
	private static int skipsFor(int clicks) {
		int skips = 0;
		for(int t : SKIP_THRESHOLDS) if(clicks >= t) skips++;
		return skips;
	}

	/** The sequence length that completes the device.  The one alpha knob on this device. */
	private static int targetLength() {
		return Alpha.count(5, 4);
	}

	private void complete(Player p, boolean wasDeferred) {
		GoldorSection s1 = Goldor.INSTANCE.getSection(0);
		if(s1 == null || s1.device.isActivated()) return;
		resetRuntime();
		s1.device.markActivated();
		if(p != null) Goldor.INSTANCE.onActivation(p, s1, "device", wasDeferred);
	}

	/**
	 * Who to credit when the device finishes with no click of its own - the short-circuit, which lands on the spam
	 * window's close task.  The player who opened it, or any non-spectator if they have gone; same fallback shape as
	 * the Sharp Shooter's ninth-hit credit.
	 */
	private Player resolveSolver() {
		Player starter = starterId == null ? null : Bukkit.getPlayer(starterId);
		if(starter != null && !GoldorListener.cannotSolve(starter)) return starter;
		for(Player pl : Bukkit.getOnlinePlayers()) if(!GoldorListener.cannotSolve(pl)) return pl;
		return null;
	}

	// ---------- Blocks ----------

	/**
	 * Swap one obsidian cell for a sea lantern.  There is nothing behind the grid to light, so the flash IS the
	 * block - and it is written with physics suppressed like every other block write in this plugin.
	 * <p>
	 * The block data it replaced is kept rather than assumed to be obsidian, so a cell that is something else after
	 * a map edit still goes back to what it was.
	 */
	private void flash(int cellIdx) {
		if(deviceWorld == null || litCells.containsKey(cellIdx)) return;
		int[] c = cell(cellIdx);
		Block b = deviceWorld.getBlockAt(c[0], c[1], c[2]);
		litCells.put(cellIdx, b.getBlockData().clone());
		b.setType(Material.SEA_LANTERN, false);
	}

	/**
	 * Put the 16 input buttons up, snapshotting whatever each slot held first.
	 * <p>
	 * Called the moment a playback ends, so the buttons exist for exactly as long as the device is taking an
	 * answer.  Idempotent: a second call while they are already up would snapshot the buttons over the real
	 * blocks and lose the {@code i1} sign.
	 * <p>
	 * Written with physics suppressed, like every other block write here, and here it is not just house style: a
	 * button is attachment-sensitive, and a physics update pops it straight back off the wall.
	 */
	private void placeButtons() {
		if(deviceWorld == null || buttonsUp) return;
		if(buttonData == null) buttonData = Bukkit.createBlockData("minecraft:stone_button[face=wall,facing=west]");
		// The slots are only emptied and snapshotted here; paintButtons below is what puts the blocks in.
		for(int i = 0; i < CELL_COUNT; i++) {
			int[] b = BUTTONS[i];
			Block block = deviceWorld.getBlockAt(b[0], b[1], b[2]);
			// NEVER snapshot a button as the thing to restore.  If one of ours somehow outlived a teardown, taking
			// its picture here would make it the "original" and every later restore would faithfully put it back,
			// so a single stray button would become permanent.  Air is what is really under them.
			if(Tag.BUTTONS.isTagged(block.getType())) {
				Block air = deviceWorld.getBlockAt(b[0], b[1], b[2]);
				air.setType(Material.AIR, false);
				buttonSnapshot.put(i, air.getState());
			} else {
				buttonSnapshot.put(i, block.getState());
			}
		}
		buttonsUp = true;
		paintButtons();
	}

	/**
	 * <b>The built-in solver.</b>  Write all 16 buttons, the one the device is waiting for as OAK and the other
	 * fifteen as stone, so the answer is readable off the wall the way Same Color's click counts are readable off
	 * its panes ({@code GoldorTerminalGui.drawSameColor}) - this plugin ships the hint rather than leaving it to
	 * Odin.  It makes the playback a formality, which is the point: the device is here to be practised as a
	 * routine, not memorised.
	 * <p>
	 * <b>Paints all 16 every time</b>, not just the two that changed, because sixteen block writes are cheaper
	 * than a rule about which ones to skip - and "exactly one button is oak" is then a property of the painter
	 * rather than something each caller has to keep true.
	 * <p>
	 * <b>Never touches {@link #buttonSnapshot}</b>.  The snapshot is taken once, by {@link #placeButtons}, and
	 * re-snapshotting here would photograph our own buttons and make them the blocks to restore.  Physics stays
	 * suppressed for the same reason the place has it suppressed: a button pops off the wall on an update.
	 */
	private void paintButtons() {
		if(deviceWorld == null || !buttonsUp) return;
		if(hintButtonData == null) hintButtonData = Bukkit.createBlockData("minecraft:oak_button[face=wall,facing=west]");
		int hint = hintCell();
		for(int i = 0; i < CELL_COUNT; i++) {
			int[] b = BUTTONS[i];
			deviceWorld.getBlockAt(b[0], b[1], b[2]).setBlockData(i == hint ? hintButtonData : buttonData, false);
		}
	}

	/** The cell the device is waiting for, or -1 when it is not waiting for one - then no button is oak. */
	private int hintCell() {
		if(state != State.AWAITING || inputIdx < 0 || inputIdx >= sequence.size()) return -1;
		return sequence.get(inputIdx);
	}

	/**
	 * Take the 16 buttons down and put each slot back to what it was, synchronously.  Idempotent, and a no-op
	 * while they are down.
	 * <p>
	 * Restored from the {@link BlockState}s, and {@code update(true, false)} is the whole of it: force, because
	 * the block there is a button and not what the state describes, and no physics for the same reason the place
	 * has none.  A state knows its own world, so this works with no {@link #deviceWorld} - which matters, because
	 * the teardown paths can reach it after the device has been thrown away.
	 */
	private void removeButtons() {
		if(!buttonsUp && buttonSnapshot.isEmpty()) return;
		for(BlockState st : buttonSnapshot.values()) st.update(true, false);
		buttonSnapshot.clear();
		buttonsUp = false;
	}

	/** Put every lit cell back, synchronously.  Idempotent, and a no-op when nothing is lit. */
	private void restoreLitCells() {
		if(litCells.isEmpty()) return;
		if(deviceWorld != null) {
			for(Map.Entry<Integer, BlockData> e : litCells.entrySet()) {
				int[] c = cell(e.getKey());
				deviceWorld.getBlockAt(c[0], c[1], c[2]).setBlockData(e.getValue(), false);
			}
		}
		litCells.clear();
	}

	// ---------- Teardown ----------

	/**
	 * <b>The force-restore, and the only public teardown.</b>  Puts any lantern in flight back to obsidian and
	 * takes the 16 input buttons down, on the spot, then throws the run away.
	 * <p>
	 * <b>One restore entry point</b>, covering both of the device's world-block changes, so the two can never be
	 * torn down out of step or one of them forgotten at a call site.
	 * <p>
	 * Called directly from all three teardown paths - {@code TAS.endPractice}, {@code Server.serverSetup} (also
	 * via {@code GoldorListener.resetSimon}) and {@code M7tas.onDisable} - per the CLAUDE.md rule that a
	 * world-block change needs a synchronous restore on every one of them and not just a timer.  The playback
	 * timer cannot cover any of them: it is a tracked {@code Utils.scheduleTask}, so
	 * {@code Utils.cancelAllScheduled} kills it mid-flash and the lantern would be saved into the world.
	 */
	public void cleanup() {
		resetRuntime();
	}

	/** Orphan anything queued, restore the blocks, and go back to IDLE. */
	private void resetRuntime() {
		runToken++;
		restoreLitCells();
		removeButtons();
		state = State.IDLE;
		sequence.clear();
		inputIdx = 0;
		spamClicks.clear();
		windowStartTick = -1;
		starterId = null;
	}
}
