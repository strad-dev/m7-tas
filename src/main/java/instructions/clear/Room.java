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
 *   <li>START / FAIRY: always GREEN, since they have no mechanics.</li>
 *   <li>PUZZLE (Quiz, Ice Fill): GREEN the instant the puzzle is solved, with no white stage.</li>
 *   <li>YELLOW: GREEN the instant its miniboss dies.</li>
 *   <li>NORMAL: WHITE when the miniboss dies (Wizard, which has no miniboss, is WHITE from the start),
 *       GREEN once WHITE and all counted secrets are found.</li>
 *   <li>TRAP: WHITE when the Power-II chest is opened, GREEN once WHITE and all secrets are found.</li>
 *   <li>BLOOD: WHITE then GREEN when the Watcher camp is cleared.  It has no secrets, so effectively GREEN.</li>
 * </ul>
 */
public class Room {
	public enum Check {NONE, WHITE, GREEN}

	public final String name;
	public final RoomType type;
	/** Grid cells this room occupies, each {@code {gx, gz}}. */
	public final int[][] cells;
	/**
	 * <b>Room depth</b> (1..5), the Roman numeral the dungeon shows on the room.  Every mob in the room has its
	 * stats scaled by {@code 1 + 0.10 x (depth - 1)}, so depth I is x1.00 and depth V is x1.40 - see
	 * {@code damage.MobStats.depthMultiplier}, and the observed 13.2M Angry Archaeologist in Deathmite (depth II)
	 * which is 12M x 1.10.
	 * <p>
	 * This used to be documented as a "difficulty tier", which read as decorative and sent an earlier draft of
	 * DAMAGE_PLAN.md off computing depth by BFS instead.  It is the depth; use it directly.  Note grid adjacency
	 * is NOT the door graph, so a geometric BFS would give wrong answers for exactly the rooms whose depth was
	 * missing.
	 */
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
	/** True once any player has set foot inside this room.  Until then the map draws it grey with a "?". */
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
				// White once the miniboss is killed, or for the miniboss-less Wizard once a player has
				// entered it, since ClearManager sets `cleared` on entry.  Green once white AND all secrets found.
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
