package commands;

import damage.Difficulty;
import damage.Mayor;
import plugin.Alpha;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import plugin.Utils;

/**
 * {@code /dungeonsettings [difficulty [<mode>] | mayor [<paul|derpy|other>] | alpha [<on|off>]]} - this server's
 * dungeon settings (MAP.md §0). No args prints them; a setting with no value steps it to the next value.
 * <p>
 * Three independent settings, any combination is valid:
 * <ul>
 *   <li><b>difficulty</b> ({@link Difficulty}) - <i>classic</i> assumes all four debuffs applied and blessings
 *       maxed, so you can focus on movement and routing. <i>perfect_rng</i> makes those live inputs (debuffs have
 *       to be built, blessings are what the party collected) and turns on the instakills in {@code death/Deaths},
 *       but the dungeon still rolls your way: one-click terminals, short devices, assumed pet. <i>rta</i> (shown
 *       as "Realistic") adds the manual work: generated terminal puzzles, working devices, your own pet menu.</li>
 *   <li><b>mayor</b> ({@link Mayor}) - <i>paul</i> (default) gives EZPZ +10 bonus score and boosted blessings,
 *       <i>derpy</i> gives neither and doubles every mob's health, <i>other</i> gives neither and leaves health
 *       alone.</li>
 * </ul>
 * Both are flags on inputs, never second damage paths - see the two classes.
 * <p>
 * Bare {@code /dungeonsettings} from a player opens {@link SettingsMenu}, standalone only: on the network these
 * are party settings owned by the lobby menu, so text output stands.
 * <p>
 * Replaced {@code /toggledungeondifficulty}, which was the difficulty half.
 * <p>
 * On the network the party leader sets these ({@code /p settings difficulty <mode>},
 * {@code /p settings mayor <paul|derpy|other>}) and they ride the practice request so the whole party inherits
 * them: a mixed-mode party would make one boss take different damage per player and let half of it die. This is
 * the standalone equivalent.
 * <p>
 * <b>Times from different settings aren't comparable.</b> So difficulty travels on the run payload
 * ({@code plugin/RunResult}) and network leaderboards key on it as a third axis; mayor travels too, but boards
 * don't split on it yet.
 */
public class DungeonSettings implements CommandExecutor {
	private static final String USAGE =
			"<red>Usage: /dungeonsettings [difficulty [" + modeIds() + "] | mayor [paul|derpy|other] | alpha [on|off]]";

	/**
	 * Typed mode names, joined from {@link Difficulty} so the usage line can't drift from the enum.
	 * {@code commandName()}, not {@code id()}: Realistic is typed {@code realistic}, only stored as {@code rta}.
	 */
	private static String modeIds() {
		return java.util.Arrays.stream(Difficulty.values())
				.map(Difficulty::commandName)
				.collect(java.util.stream.Collectors.joining("|"));
	}

	/** Menu a bare {@code /dungeonsettings} opens standalone. See {@link SettingsMenu#suppressed()}. */
	private final SettingsMenu menu;

	public DungeonSettings(SettingsMenu menu) {
		this.menu = menu;
	}

	@Override
	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label,
			String @NonNull [] args) {
		if(args.length == 0) {
			// Standalone player gets the menu; console and everyone on the network get text.
			if(sender instanceof Player p && !SettingsMenu.suppressed()) menu.open(p);
			else show(sender);
			return true;
		}
		switch(args[0].toLowerCase()) {
			case "difficulty", "mode" -> difficulty(sender, args);
			case "mayor" -> mayor(sender, args);
			case "alpha" -> alpha(sender, args);
			default -> sender.sendMessage(Utils.msg(USAGE));
		}
		return true;
	}

	/** Current settings, one line each. What bare {@code /dungeonsettings} prints. */
	private static void show(CommandSender sender) {
		sender.sendMessage(Utils.msg("<gold><bold>DUNGEON SETTINGS"));
		// Name, not id: "rta" is a storage key. Every name is also a parse alias, so what's shown can be typed back.
		sender.sendMessage(Utils.msg("<dark_gray>- <gray>difficulty: <yellow><value>  <dark_gray><desc>",
				Placeholder.unparsed("value", Difficulty.current().displayName()),
				Placeholder.unparsed("desc", describe(Difficulty.current()))));
		sender.sendMessage(Utils.msg("<dark_gray>- <gray>mayor: <yellow><value>  <dark_gray><desc>",
				Placeholder.unparsed("value", Mayor.current().id()),
				Placeholder.unparsed("desc", describe(Mayor.current()))));
		sender.sendMessage(Utils.msg("<dark_gray>- <gray>alpha: <yellow><value>  <dark_gray><desc>",
				Placeholder.unparsed("value", Alpha.current().id()),
				Placeholder.unparsed("desc", describe(Alpha.current()))));
		sender.sendMessage(Utils.msg("<dark_gray>Change one with <white>/dungeonsettings <setting> [value]"));
	}

	private static void difficulty(CommandSender sender, String[] args) {
		Difficulty next;
		if(args.length >= 2) {
			next = Difficulty.parse(args[1]);
			if(next == null) {
				sender.sendMessage(Utils.msg(USAGE));
				return;
			}
			Difficulty.set(next);
		} else {
			// No value: step to next mode, like the old bare /toggledungeondifficulty.
			next = Difficulty.toggle();
		}
		applyDifficulty(next);
	}

	/**
	 * Announce a new difficulty. Split out because {@link SettingsMenu} sets the same global from a click and must
	 * announce it the same way: a silent server-wide change scores someone else's run under a mode they never chose.
	 */
	static void applyDifficulty(Difficulty next) {
		Bukkit.broadcast(Utils.msg("<gold><bold>DUNGEON DIFFICULTY<reset><gray> is now <yellow><value>",
				Placeholder.unparsed("value", next.displayName())));
		Bukkit.broadcast(Utils.msg("<gray><desc>", Placeholder.unparsed("desc", describe(next))));
	}

	private static void mayor(CommandSender sender, String[] args) {
		Mayor next;
		if(args.length >= 2) {
			next = Mayor.parse(args[1]);
			if(next == null) {
				sender.sendMessage(Utils.msg(USAGE));
				return;
			}
			Mayor.set(next);
		} else {
			next = Mayor.toggle();
		}
		applyMayor(next);
	}

	/** Announce a new mayor. Same split and reason as {@link #applyDifficulty}. */
	static void applyMayor(Mayor next) {
		Bukkit.broadcast(Utils.msg("<gold><bold>MAYOR<reset><gray> is now <yellow><value>",
				Placeholder.unparsed("value", next.id())));
		Bukkit.broadcast(Utils.msg("<gray><desc>", Placeholder.unparsed("desc", describe(next))));
		// HP is latched at spawn, so a mid-session change leaves mobs already on the floor alone.
		if(instructions.bosses.WitherActions.isPracticeMode()) {
			Bukkit.broadcast(Utils.msg("<dark_gray>Mob health is set when a mob spawns, so this only affects what spawns from now on."));
		}
	}

	private static void alpha(CommandSender sender, String[] args) {
		Alpha next;
		if(args.length >= 2) {
			next = Alpha.parse(args[1]);
			if(next == null) {
				sender.sendMessage(Utils.msg(USAGE));
				return;
			}
			Alpha.set(next);
		} else {
			next = Alpha.toggle();
		}
		applyAlpha(next);
	}

	/** Announce alpha timings on/off. Same split and reason as {@link #applyDifficulty}. */
	static void applyAlpha(Alpha next) {
		Bukkit.broadcast(Utils.msg("<gold><bold>ALPHA TIMINGS<reset><gray> are now <yellow><value>",
				Placeholder.unparsed("value", next.id())));
		Bukkit.broadcast(Utils.msg("<gray><desc>", Placeholder.unparsed("desc", describe(next))));
		// Timings latch when a phase arms its schedule, so a mid-run flip only reaches phases not yet started.
		if(instructions.bosses.WitherActions.isPracticeMode()) {
			Bukkit.broadcast(Utils.msg("<dark_gray>A phase arms its timings when it starts, so this only affects phases that have not begun."));
		}
	}

	/**
	 * One line on what a value means. Plain text: {@link #show} inserts it as an unparsed placeholder, so a
	 * MiniMessage tag here would print as literal angle brackets.
	 */
	private static String describe(Difficulty d) {
		return switch(d) {
			case CLASSIC -> "Debuffs are automatically applied and blessings are always maxed.";
			case PERFECT_RNG -> "Live debuffs and real blessing levels, and you can die - but the dungeon always rolls your way: one-click terminals, short devices, the pet you need is the pet you have.";
			case REALISTIC -> "Perfect RNG plus what a real run makes you do by hand: generated terminal puzzles, working devices, and your own pet menu.";
		};
	}

	private static String describe(Alpha a) {
		return switch(a) {
			case OFF -> "The normal Hypixel timings.";
			case ON -> "Experimental short timings.  Times set under alpha are NOT valid for the leaderboards.";
		};
	}

	private static String describe(Mayor m) {
		return switch(m) {
			case PAUL -> "EZPZ gives +10 bonus score and blessings are boosted; a perfect clear scores 319.";
			case DERPY -> "Mobs have double health, blessings are weaker and there is no +10 bonus score (max 309).";
			case OTHER -> "A mayor with no dungeon perks: no +10 bonus score (max 309) and weaker blessings.";
		};
	}
}
