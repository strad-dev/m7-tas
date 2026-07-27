package instructions.clear;

import java.util.ArrayList;
import java.util.List;

/**
 * A dungeon room: its static definition (name, type, grid cells, miniboss/blessing, secrets) plus the
 * per-run mutable state that drives its checkmark. Built once in {@link Rooms}; {@link #reset()} clears the
 * run state between runs.
 *
 * <p>Checkmark rules (see {@link #check()}):
 * <ul>
 *   <li>START / FAIRY — always GREEN (no mechanics).</li>
 *   <li>PUZZLE (Quiz, Ice Fill) — GREEN the instant the puzzle is solved (no white stage).</li>
 *   <li>YELLOW — GREEN the instant its miniboss dies.</li>
 *   <li>NORMAL — WHITE when the miniboss dies (Wizard, which has no miniboss, is WHITE from the start),
 *       GREEN once WHITE and all counted secrets are found.</li>
 *   <li>TRAP — WHITE when the Power-II chest is opened, GREEN once WHITE and all secrets are found.</li>
 *   <li>BLOOD — WHITE/GREEN when the Watcher camp is cleared (it has no secrets, so effectively GREEN).</li>
 * </ul>
 */
public class Room {
	public enum Check {NONE, WHITE, GREEN}

	public final String name;
	public final RoomType type;
	/** Grid cells this room occupies, each {@code {gx, gz}}. */
	public final int[][] cells;
	/** Roman-numeral difficulty tier (1..5) for NORMAL miniboss rooms; 0 otherwise. */
	public final int level;
	public final boolean hasMiniboss;
	/** Blessing(s) granted when the room is cleared (miniboss kill / puzzle solve); empty for none. */
	public final Blessing[] clearBlessings;
	public final List<Secret> secrets = new ArrayList<>();

	// --- run-time state ---
	/** Objective done: miniboss killed (NORMAL/YELLOW) / Power-II chest opened (TRAP) / camp cleared (BLOOD). */
	public boolean cleared;
	/** Puzzle solved (PUZZLE rooms only). */
	public boolean solved;
	/** True once any player has set foot inside this room — until then the map draws it grey with a "?". */
	public boolean explored;

	Room(String name, RoomType type, int[][] cells, int level, boolean hasMiniboss, Blessing[] clearBlessings) {
		this.name = name;
		this.type = type;
		this.cells = cells;
		this.level = level;
		this.hasMiniboss = hasMiniboss;
		this.clearBlessings = clearBlessings;
	}

	Room addSecret(Secret s) {
		s.room = this;
		secrets.add(s);
		return this;
	}

	public int countedSecretTotal() {
		int n = 0;
		for(Secret s : secrets) if(s.counted) n++;
		return n;
	}

	public int countedSecretFound() {
		int n = 0;
		for(Secret s : secrets) if(s.counted && s.found) n++;
		return n;
	}

	public boolean allCountedFound() {
		return countedSecretFound() >= countedSecretTotal();
	}

	public Check check() {
		switch(type) {
			case START, FAIRY -> {
				return Check.GREEN;
			}
			case PUZZLE -> {
				return solved ? Check.GREEN : Check.NONE;
			}
			case YELLOW -> {
				return cleared ? Check.GREEN : Check.NONE;
			}
			case BLOOD, TRAP -> {
				if(!cleared) return Check.NONE;
				return allCountedFound() ? Check.GREEN : Check.WHITE;
			}
			case NORMAL -> {
				// White once the miniboss is killed — or, for the miniboss-less Wizard, once a player has
				// entered it (ClearManager sets `cleared` on entry). Green once white AND all secrets found.
				if(!cleared) return Check.NONE;
				return allCountedFound() ? Check.GREEN : Check.WHITE;
			}
			default -> {
				return Check.NONE;
			}
		}
	}

	public void reset() {
		cleared = false;
		solved = false;
		explored = false;
		for(Secret s : secrets) {
			s.found = false;
			s.entityId = null;
		}
	}
}
