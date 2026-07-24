package instructions.clear;

import instructions.Server;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import plugin.Utils;

import java.util.List;

/**
 * Interactive Oruo quiz (grid 0,0). The intro fires only once a player enters the room; questions are then
 * answered by right-clicking one of three answer buttons (A/B/C, ±1 block tolerance). The correct answer is
 * always <b>B</b>. A wrong answer plays Oruo's mocking dialogue and restarts the quiz from question 1. Three
 * correct answers → {@link ClearManager#puzzleSolved} (green check + Time V).
 */
public final class PuzzleQuiz {
	private PuzzleQuiz() {
	}

	// Answer buttons (A, B, C) — index 1 (B) is always correct. ±1 block tolerance around each.
	private static final int[][] BUTTONS = {{-20, 70, -34}, {-25, 70, -31}, {-30, 70, -34}};
	private static final int CORRECT = 1; // B

	private static final String[] QUESTIONS = {
			"                      How is the run going so far?",
			"Did you know that you can sub scribe to Stradivarius Violin to                         see more content like this?!",
			"                             Is akc0303 bald?"
	};
	private static final String[][] ANSWERS = {
			{"Alright", "Trash", "Literally tick-perfect"},
			{"Oh wow, I should sub scribe!", "Oh wow, I should sub scribe!!", "Oh wow, I should sub scribe!!!"},
			{"No", "Yes", "Decline to Answer"}
	};
	private static final String[] LABELS = {"ⓐ", "ⓑ", "ⓒ"};
	private static final String ORUO = "<dark_red>[STATUE] Oruo the Omniscient<white>: ";

	private static boolean started, solved, awaiting;
	private static int question; // 0-based index of the current question
	private static int gen;       // generation guard so restart/stop cancels stale scheduled asks

	public static void reset() {
		started = solved = awaiting = false;
		question = 0;
		gen++;
	}

	public static void stop() {
		gen++;
		started = awaiting = false;
	}

	/** Start the intro the first tick a real player is standing in the Quiz room. */
	public static void tick(World world, List<Player> players) {
		if(started || solved || world == null) return;
		for(Player p : players) {
			if(Rooms.roomAt(p.getLocation()) == Rooms.QUIZ) {
				begin();
				return;
			}
		}
	}

	private static void begin() {
		started = true;
		final int g = ++gen;
		Server.Quiz.oruoMessage("Prove your knowledge by answering 3 questions and I shall reward you in ways that transcend time!");
		Utils.scheduleTask(() -> {
			if(g != gen) return;
			Server.Quiz.oruoMessage("Answer incorrectly, and your moment of ineptitude will live on for generations.");
		}, 40);
		Utils.scheduleTask(() -> {
			if(g != gen) return;
			question = 0;
			ask();
		}, 80);
	}

	private static void ask() {
		awaiting = true;
		Bukkit.broadcast(Utils.msg(""));
		Bukkit.broadcast(Utils.msg("<gold>                                <bold>Question #" + (question + 1)));
		Bukkit.broadcast(Utils.msg("<gold>" + QUESTIONS[question]));
		Bukkit.broadcast(Utils.msg(""));
		for(int i = 0; i < 3; i++) {
			Bukkit.broadcast(Utils.msg("<gold>     " + LABELS[i] + " <green>" + ANSWERS[question][i]));
		}
		Bukkit.broadcast(Utils.msg(""));
		Utils.playGlobalSound(Sound.ENTITY_GUARDIAN_HURT, 2.0f, 0.5f);
	}

	/** A player right-clicked answer {@code index} (0=A,1=B,2=C). */
	public static void answer(Player p, int index) {
		if(!started || solved || !awaiting) return;
		awaiting = false;
		if(index == CORRECT) {
			Bukkit.broadcast(Utils.msg(ORUO + "<gold>" + Utils.getRealName(p) + " <green>answered <gold>Question #" + (question + 1) + "<green> correctly!"));
			Utils.playGlobalSound(Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 0.75f);
			if(question < 2) {
				final int g = ++gen;
				Utils.scheduleTask(() -> {
					if(g != gen) return;
					question++;
					ask();
				}, 20);
			} else {
				complete(p);
			}
		} else {
			// Wrong answer → mocking dialogue, then restart the whole quiz.
			final int g = ++gen;
			Server.Quiz.oruoMessage("<dark_red>Y<red>i<gold>k<yellow>e<green>s");
			Utils.scheduleTask(() -> {
				if(g != gen) return;
				Bukkit.broadcast(Utils.msg(ORUO + "<gold><name><red> chose the wrong answer!  I shall never forget this moment of misrememberance.",
						Placeholder.unparsed("name", Utils.getRealName(p))));
				Utils.playGlobalSound(Sound.ENTITY_GUARDIAN_HURT, 2.0f, 0.5f);
			}, 20);
			Utils.scheduleTask(() -> {
				if(g != gen) return;
				question = 0;
				ask();
			}, 40);
		}
	}

	private static void complete(Player p) {
		solved = true;
		gen++;
		Server.Quiz.oruoMessage("I bestow upon you all the power of a hundred years!");
		ClearManager.puzzleSolved(Rooms.QUIZ, p); // green check + Time V
	}

	/** True if {@code b} is within 1 block of any answer button (used for Dungeonbreaker immunity). */
	public static boolean isButtonArea(Block b) {
		for(int[] btn : BUTTONS) {
			if(Math.abs(b.getX() - btn[0]) <= 1 && Math.abs(b.getY() - btn[1]) <= 1 && Math.abs(b.getZ() - btn[2]) <= 1) {
				return true;
			}
		}
		return false;
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
