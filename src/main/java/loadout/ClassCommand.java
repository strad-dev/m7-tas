package loadout;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import plugin.Utils;

/**
 * {@code /class <archer|mage|tank|berserk|healer>} - pick your M7 class. Selecting a class for the first time seeds
 * its loadout from the current default kit so it's never empty; edit it with {@code /m7loadout}. The selection and
 * loadouts live in the shared data folder, so they are the same ones the network plugin's lobby copy edits.
 * <p>
 * Unlike the network plugin's copy this also applies the class scoreboard TAG immediately: the tag is what gates
 * the mage beam and the per-class damage paths, and standalone (no network plugin, so no {@code M7Bridge}) this is
 * the only place it can come from.
 * <p>
 * NOTE: twin of the network plugin's {@code loadout/ClassCommand.java} - keep in sync.
 */
public class ClassCommand implements CommandExecutor {
	@Override
	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
		if(!(sender instanceof Player p)) {
			sender.sendMessage(Utils.msg("<red>Only players can pick a class"));
			return true;
		}
		String list = String.join(", ", Loadouts.CLASSES);
		if(args.length < 1) {
			String sel = Loadouts.getSelectedClass(p.getUniqueId());
			p.sendMessage(Utils.msg("<gray>Your class: <white><sel></white>  <gray>Pick one: <yellow><list>",
					Placeholder.unparsed("sel", sel == null ? "none" : sel),
					Placeholder.unparsed("list", list)));
			return true;
		}
		String role = Loadouts.normalize(args[0]);
		if(role == null) {
			p.sendMessage(Utils.msg("<red>Unknown class. Pick one: <yellow><list>", Placeholder.unparsed("list", list)));
			return true;
		}
		Loadouts.setSelectedClass(p.getUniqueId(), role);
		Loadouts.seedIfAbsent(p.getUniqueId(), role);
		Loadouts.applyClassTag(p, role);
		p.sendMessage(Utils.msg("<green>Class set to <white><role></white>  <gray>Edit your kit with <white>/m7loadout",
				Placeholder.unparsed("role", role)));
		return true;
	}
}
