package commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
// import plugin.FakePlayerManager; // unused after TAS tab-completions removed in the practice fork

import java.util.ArrayList;
import java.util.List;

public class TabCompletor implements TabCompleter {
	@SuppressWarnings("NullableProblems")
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if(!(sender instanceof Player)) {
			return new ArrayList<>();
		}

		List<String> completions = new ArrayList<>();
		String cmdName = command.getName().toLowerCase();

		switch(cmdName) {
			case "practice" -> {
				if(args.length == 1) {
					String[] sections = {"all", "clear", "boss", "maxor", "storm", "goldor", "necron", "witherking"};
					String input = args[0].toLowerCase();

					for(String section : sections) {
						// Check if the section starts with the input
						if(section.toLowerCase().startsWith(input)) {
							completions.add(section);
						}
					}
				} else if(args.length == 2) {
					// /practice <section> [--no-teleport]
					if("--no-teleport".startsWith(args[1].toLowerCase())) {
						completions.add("--no-teleport");
					}
				}
			}

			// "spectate" tab-completion removed — TAS-only command, disabled in the practice fork.

			case "getcustomitems" -> {
				if(args.length == 1) {
					for(String val : new String[]{"all", "archer", "berserk", "healer", "mage", "tank"}) {
						if(val.startsWith(args[0].toLowerCase())) {
							completions.add(val);
						}
					}
				}
			}

			case "verbose" -> {
				if(args.length == 1) {
					for(String val : new String[]{"off", "timer", "on", "super", "true", "false"}) {
						if(val.startsWith(args[0].toLowerCase())) {
							completions.add(val);
						}
					}
				}
			}

			case "setspeed" -> {
				String[] speedPresets = {"400", "500", "600", "650"};
				if(args.length == 1) {
					String input = args[0].toLowerCase();
					for(Player online : Bukkit.getOnlinePlayers()) {
						if(online.getName().toLowerCase().startsWith(input)) {
							completions.add(online.getName());
						}
					}
					for(String preset : speedPresets) {
						if(preset.startsWith(input)) {
							completions.add(preset);
						}
					}
				} else if(args.length == 2) {
					String input = args[1].toLowerCase();
					for(String preset : speedPresets) {
						if(preset.startsWith(input)) {
							completions.add(preset);
						}
					}
				}
			}

			// "simulate" tab-completion removed — TAS-only command, disabled in the practice fork.
		}

		return completions;
	}
}