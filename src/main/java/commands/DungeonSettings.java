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
 * dungeon settings (MAP.md §0).  With no arguments it prints them; with a setting and no value it steps that one
 * to its next value.
 * <p>
 * Three settings, and they are independent - any difficulty can be run under any mayor, with or without alpha:
 * <ul>
 *   <li><b>difficulty</b> ({@link Difficulty}) - <i>classic</i> assumes all four debuffs are applied and blessings
 *       are maxed, so a practising player can concentrate on movement and routing.  <i>perfect_rng</i> makes each
 *       of those a live input - the debuffs have to be built, the blessings are whatever the party actually
 *       collected - and turns on the instakills in {@code death/Deaths}, but the dungeon still rolls your way:
 *       one-click terminals, short devices, the assumed pet.  <i>rta</i> (shown as "Realistic") is that plus the
 *       work a real run makes you do by hand: generated terminal puzzles, working devices and your own pet menu.</li>
 *   <li><b>mayor</b> ({@link Mayor}) - <i>paul</i> (the default) gives the EZPZ +10 bonus score and boosted
 *       blessings, <i>derpy</i> gives neither and doubles every mob's health, and <i>other</i> gives neither and
 *       leaves health alone.</li>
 * </ul>
 * Both are flags on inputs, never second damage paths - see the two classes.
 * <p>
 * A bare {@code /dungeonsettings} typed by a player opens {@link SettingsMenu} instead of printing, but only
 * STANDALONE: on the network these are party settings and the lobby's own menu owns them, so the menu is
 * suppressed and the text output stands.
 * <p>
 * This replaced {@code /toggledungeondifficulty}, which was the difficulty half of it.
 * <p>
 * On the network the party leader sets these instead, with {@code /p settings difficulty <mode>} and
 * {@code /p settings mayor <paul|derpy|other>}, and both ride along with the practice request so everyone in the
 * party inherits them: a mixed-mode party would make the same boss take different damage per player, and would let
 * half of it die.  This command is the standalone equivalent, so M7 keeps working on its own.
 * <p>
 * <b>Times from different settings are not comparable.</b>  That is why the difficulty travels on the run payload
 * ({@code plugin/RunResult}) and the network's leaderboards key on it as a third axis; the mayor travels there too,
 * though the boards do not currently split on it.
 */
public class DungeonSettings implements CommandExecutor {
	private static final String USAGE =
			"<red>Usage: /dungeonsettings [difficulty [" + modeIds() + "] | mayor [paul|derpy|other] | alpha [on|off]]";

	/**
	 * The mode names a player types, joined from {@link Difficulty} itself so the usage line can never drift
	 * from the enum.  {@code commandName()}, not {@code id()}: Realistic is typed {@code realistic} and only
	 * STORED as {@code rta}.
	 */
	private static String modeIds() {
		return java.util.Arrays.stream(Difficulty.values())
				.map(Difficulty::commandName)
				.collect(java.util.stream.Collectors.joining("|"));
	}

	/** The menu a bare {@code /dungeonsettings} opens standalone.  See {@link SettingsMenu#suppressed()}. */
	private final SettingsMenu menu;

	public DungeonSettings(SettingsMenu menu) {
		this.menu = menu;
	}

	@Override
	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label,
			String @NonNull [] args) {
		if(args.length == 0) {
			// A player standalone gets the menu; the console, and everyone on the network, gets the text.
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

	/** The current settings, one line each.  What a bare {@code /dungeonsettings} prints. */
	private static void show(CommandSender sender) {
		sender.sendMessage(Utils.msg("<gold><bold>DUNGEON SETTINGS"));
		// The NAME, not the id: "rta" is a storage key, not something to read off a settings line.  Every name is
		// also a parse alias, so what a player sees here is still something they can type back.
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
			// No value given: step to the next mode, which is what the old /toggledungeondifficulty did bare.
			next = Difficulty.toggle();
		}
		applyDifficulty(next);
	}

	/**
	 * Announce a difficulty that has just been put in force.  Split out because {@link SettingsMenu} sets the same
	 * global from a click and must say so the same way - a server-wide setting that changed silently is how one
	 * player ends up scoring somebody else's run under a mode they never chose.
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

	/** Announce a mayor that has just taken office.  Same split, and same reason, as {@link #applyDifficulty}. */
	static void applyMayor(Mayor next) {
		Bukkit.broadcast(Utils.msg("<gold><bold>MAYOR<reset><gray> is now <yellow><value>",
				Placeholder.unparsed("value", next.id())));
		Bukkit.broadcast(Utils.msg("<gray><desc>", Placeholder.unparsed("desc", describe(next))));
		// Only the HP is latched at spawn, so a mid-session change leaves whatever is already on the floor alone.
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

	/** Announce the alpha timings going on or off.  Same split, and same reason, as {@link #applyDifficulty}. */
	static void applyAlpha(Alpha next) {
		Bukkit.broadcast(Utils.msg("<gold><bold>ALPHA TIMINGS<reset><gray> are now <yellow><value>",
				Placeholder.unparsed("value", next.id())));
		Bukkit.broadcast(Utils.msg("<gray><desc>", Placeholder.unparsed("desc", describe(next))));
		// Timings are latched by the schedules a phase arms at its start, so a mid-run flip only reaches the
		// phases that have not begun yet.
		if(instructions.bosses.WitherActions.isPracticeMode()) {
			Bukkit.broadcast(Utils.msg("<dark_gray>A phase arms its timings when it starts, so this only affects phases that have not begun."));
		}
	}

	/**
	 * One line on what a setting's value means.  PLAIN text: it is interpolated as an unparsed placeholder in
	 * {@link #show}, so a MiniMessage tag in here would print as literal angle brackets.
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
