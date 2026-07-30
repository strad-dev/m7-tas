package instructions.clear;

import instructions.Server;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import plugin.Utils;

import java.util.List;

/**
 * Interactive Oruo quiz (grid 0,0). The intro fires once a player enters the room; each question plays the
 * TAS animation (particle trails + floating ⓐ/ⓑ/ⓒ labels, reused from {@link Server.Quiz#animateQuestion}) and
 * answering is only accepted after option (c) has appeared. Answers are given by right-clicking one of three
 * buttons (A/B/C, ±1 block); the correct answer is always <b>B</b>. A wrong answer plays Oruo's mocking
 * dialogue and restarts the whole quiz from the intro. Three correct answers → green check + score immediately,
 * then Time V once Oruo's reward line has played.
 * <p>Question 3 is built per-run: it asks whether the player who <i>opened</i> the Quiz room (the first one to
 * set foot in it, which is also what starts the quiz) is bald. The answer is "Yes" for everyone except
 * {@code Beethoven_}, who isn't — for him the A/B options are swapped so that <b>B</b> stays the right button.
 */
public final class PuzzleQuiz {
	private PuzzleQuiz() {
	}

	// Answer buttons (A, B, C) — index 1 (B) is always correct. ±1 block tolerance around each.
	private static final int[][] BUTTONS = {{-20, 70, -34}, {-25, 70, -31}, {-30, 70, -34}};
	private static final int CORRECT = 1; // B

	// Unpadded and unwrapped — animateQuestion word-wraps and centers each line via ChatFont.centerLines. Index 2
	// is a template filled in by questionText() with whoever opened the room, so its width isn't known here.
	private static final String[] QUESTIONS = {
			"How is the run going so far?",
			"Did you know that you can sub scribe to Stradivarius Violin to see more content like this?!",
			"Is %s bald?"
	};
	private static final String[][] ANSWERS = {
			{"Alright", "Trash", "Literally tick-perfect"},
			{"Oh wow, I should sub scribe!", "Oh wow, I should sub scribe!!", "Oh wow, I should sub scribe!!!"},
			{"No", "Yes", "Decline to Answer"}
	};
	/** Q3's options for the one player who isn't bald: A/B swapped, so B ("No") is still the correct button. */
	private static final String[] ANSWERS_NOT_BALD = {"Yes", "No", "Decline to Answer"};
	/** The player Q3 answers "No" for. Matched against {@link Utils#getRealName(Player)}, so the Mage1 fake counts. */
	private static final String NOT_BALD = "Beethoven_";
	/** Q3's subject if nobody was recorded as opening the room (shouldn't happen — the quiz needs an entry to start). */
	private static final String FALLBACK_OPENER = "akc0303";
	private static final String ORUO = "<dark_red>[STATUE] Oruo the Omniscient<white>: ";

	private static final TextDisplay[] options = new TextDisplay[3];
	private static World world;
	private static boolean started, solved, awaiting;
	private static int question; // 0-based index of the current question
	private static int gen;       // generation guard so restart/stop cancels stale scheduled tasks
	/** Display name of the player who opened the Quiz room — the subject of question 3. Null until entry. */
	private static String opener;

	public static void reset() {
		gen++;
		started = solved = awaiting = false;
		question = 0;
		opener = null;
		Server.Quiz.removeOptions(options);
	}

	public static void stop() {
		gen++;
		started = awaiting = false;
		Server.Quiz.removeOptions(options);
	}

	/** Start the intro the first tick a real player is standing in the Quiz room. */
	public static void tick(World w, List<Player> players) {
		if(started || solved || w == null) return;
		for(Player p : players) {
			if(Rooms.roomAt(p.getLocation()) == Rooms.QUIZ) {
				world = w;
				opener = Utils.getRealName(p); // question 3 is about whoever walked in first
				begin();
				return;
			}
		}
	}

	/** Full run of the quiz from Oruo's intro dialogue — also the restart target after a wrong answer. */
	private static void begin() {
		started = true;
		awaiting = false;
		question = 0;
		final int g = ++gen;
		Server.Quiz.removeOptions(options);
		Server.Quiz.oruoMessage("I am <dark_red>Oruo the Omniscient<white>.  I have lived many lives.  I have learned all there is to know.");
		Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER);
		Utils.scheduleTask(() -> {
			if(g != gen) return;
			Server.Quiz.oruoMessage("Though I sit stationary in this prison that is <red>The Catacombs<white>, my knowledge knows no bounds.");
		}, 40);
		Utils.scheduleTask(() -> {
			if(g != gen) return;
			Server.Quiz.oruoMessage("Prove your knowledge by answering 3 questions and I shall reward you in ways that transcend time!");
		}, 80);
		Utils.scheduleTask(() -> {
			if(g != gen) return;
			Server.Quiz.oruoMessage("Answer incorrectly, and your moment of ineptitude will live on for generations.");
		}, 120);
		Utils.scheduleTask(() -> {
			if(g != gen) return;
			ask();
		}, 160);
	}

	private static void ask() {
		final int g = gen;
		awaiting = false;
		Server.Quiz.removeOptions(options);
		Player p = ClearManager.nearestRealPlayer(new Location(world, -25, 71, -31));
		Server.Quiz.animateQuestion(world, p, question + 1, questionText(question), answers(question), options);
		// Answering is only allowed after option (c) has been shown (spawns at +60t).
		Utils.scheduleTask(() -> {
			if(g == gen) awaiting = true;
		}, 62);
	}

	/** The question line, unpadded — {@code animateQuestion} centers it. Q3 names {@link #opener}. */
	private static String questionText(int index) {
		if(index != 2) return QUESTIONS[index];
		return String.format(QUESTIONS[2], opener == null ? FALLBACK_OPENER : opener);
	}

	/** The three options. Q3 flips to {@link #ANSWERS_NOT_BALD} when {@link #NOT_BALD} opened the room — the
	 *  correct text becomes "No", still on button B. */
	private static String[] answers(int index) {
		if(index == 2 && NOT_BALD.equals(opener)) return ANSWERS_NOT_BALD;
		return ANSWERS[index];
	}

	/** A player right-clicked answer {@code index} (0=A,1=B,2=C). */
	public static void answer(Player p, int index) {
		if(!started || solved || !awaiting) return;
		awaiting = false;
		Server.Quiz.removeOptions(options);
		if(index == CORRECT) {
			Bukkit.broadcast(Utils.msg(ORUO + "<gold>" + Utils.getRealName(p) + " <green>answered <gold>Question #" + (question + 1) + "<green> correctly!"));
			Utils.playGlobalSound(Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 0.75f);
			if(question < 2) {
				final int g = ++gen;
				String line = question == 0 ? "2 question left... then you will have proven your worth to me!" : "One more question!";
				Server.Quiz.oruoMessage(line);
				Utils.scheduleTask(() -> {
					if(g != gen) return;
					question++;
					ask();
				}, 40);
			} else {
				complete(p);
			}
		} else {
			// Wrong answer → Oruo's mocking dialogue, then restart the whole quiz from the intro.
			final int g = ++gen;
			Utils.playGlobalSound(Sound.ENTITY_CAT_AMBIENT, 2.0f, 0.5f);
			Server.Quiz.oruoMessage("<dark_red>Y<red>i<gold>k<yellow>e<green>s");
			Utils.scheduleTask(() -> {
				if(g != gen) return;
				Bukkit.broadcast(Utils.msg(ORUO + "<gold><name><red> chose the wrong answer!  I shall never forget this moment of misrememberance.",
						Placeholder.unparsed("name", Utils.getRealName(p))));
				Utils.playGlobalSound(Sound.ENTITY_GUARDIAN_HURT, 2.0f, 0.5f);
			}, 20);
			Utils.scheduleTask(() -> {
				if(g != gen) return;
				begin();
			}, 60);
		}
	}

	private static void complete(Player p) {
		solved = true;
		Server.Quiz.removeOptions(options);
		final int g = ++gen;
		// The room is beaten the moment the last question lands, so the green check + score go up now — waiting for
		// Oruo's dialogue only delayed the score by 40 ticks.
		ClearManager.puzzleSolved(Rooms.QUIZ, p, false);
		// TAS timing: Q3 answered → (+20t) Oruo's reward line → (+20t) the blessing.
		Utils.scheduleTask(() -> {
			if(g != gen) return;
			Server.Quiz.oruoMessage("I bestow upon you all the power of a hundred years!");
		}, 20);
		Utils.scheduleTask(() -> {
			if(g != gen) return;
			ClearManager.awardRoomBlessings(Rooms.QUIZ, p); // Time V
		}, 40);
	}

	/** True if {@code b} is within 1 block of any answer button (used for Dungeonbreaker immunity). */
	public static boolean isButtonArea(Block b) {
		return buttonIndex(b) >= 0;
	}

	/** The button index (0=A,1=B,2=C) a clicked block belongs to (±1 tolerance), or -1 if none. */
	public static int buttonIndex(Block b) {
		for(int i = 0; i < BUTTONS.length; i++) {
			int[] btn = BUTTONS[i];
			if(Math.abs(b.getX() - btn[0]) <= 1 && Math.abs(b.getY() - btn[1]) <= 1 && Math.abs(b.getZ() - btn[2]) <= 1) {
				return i;
			}
		}
		return -1;
	}
}
