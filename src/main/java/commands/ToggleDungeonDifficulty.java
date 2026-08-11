package commands;

import damage.Difficulty;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;
import plugin.Utils;

/**
 * {@code /toggledungeondifficulty [classic|realistic]} - flip this server's damage difficulty (DAMAGE_PLAN.md §0).
 * <p>
 * <b>Classic</b> assumes all four debuffs are permanently applied and blessings are maxed, so a practising player
 * can concentrate on movement and routing.  <b>Realistic</b> makes each of those a live input: the debuffs have to
 * be built and the blessings are whatever the party actually collected.
 * <p>
 * It is a flag on inputs, not a second damage path - see {@link Difficulty}.
 * <p>
 * On the network the party leader sets it instead, with {@code /p settings difficulty <classic|realistic>}, and it
 * rides along with the practice request so everyone in the party inherits it: a mixed-mode party would make the
 * same boss take different damage per player.  This command is the standalone equivalent, so M7 keeps working on
 * its own.
 * <p>
 * <b>Times from the two modes are not comparable</b>, which is why the mode travels on the run payload
 * ({@code plugin/RunResult}) and the network's leaderboards key on it as a third axis.
 */
public class ToggleDungeonDifficulty implements CommandExecutor {
	@Override
	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label,
			String @NonNull [] args) {
		Difficulty next;
		if(args.length >= 1) {
			next = Difficulty.parse(args[0]);
			if(next == null) {
				sender.sendMessage(Utils.msg("<red>Usage: /toggledungeondifficulty [classic|realistic]"));
				return true;
			}
			Difficulty.set(next);
		} else {
			next = Difficulty.toggle();
		}
		Bukkit.broadcast(Utils.msg("<gold><bold>DUNGEON DIFFICULTY<reset><gray> is now <yellow><mode>",
				Placeholder.unparsed("mode", next.id())));
		Bukkit.broadcast(Utils.msg(next == Difficulty.CLASSIC
				? "<gray>     Debuffs are assumed applied and blessings are maxed."
				: "<gray>     Debuffs must be built, and blessings are whatever the party collected."));
		return true;
	}
}
