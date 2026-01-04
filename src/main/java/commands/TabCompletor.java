package commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import plugin.M7tas;

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
			case "tas" -> {
				if(args.length == 1) {
					String[] sections = {"all", "clear", "boss", "maxor", "storm", "goldor", "necron", "witherking"};
					String input = args[0].toLowerCase();

					for(String section : sections) {
						// Check if the section starts with the input
						if(section.toLowerCase().startsWith(input)) {
							completions.add(section);
						}
					}
				} else if(args.length > 1) {
					// TAS command only accepts one argument, return empty list for any additional arguments
					return completions;
				}
			}

			case "spectate" -> {
				if(args.length == 1) {
					// Get available classes from fakePlayers map
					for(String role : M7tas.getFakePlayers().keySet()) {
						if(role.toLowerCase().startsWith(args[0].toLowerCase())) {
							completions.add(role);
						}
					}
				} else if(args.length > 1) {
					// Spectate only accepts one argument
					return completions;
				}
			}

			case "simulate" -> {
				if(args.length == 1) {
					String[] simCommands = {"undo", "bonzo", "move"};
					for(String cmd : simCommands) {
						if(cmd.startsWith(args[0].toLowerCase())) {
							completions.add(cmd);
						}
					}
				} else if(args.length == 2 && (args[0].equalsIgnoreCase("bonzo") || args[0].equalsIgnoreCase("move"))) {
					// Player selection for bonzo/move
					for(String role : M7tas.getFakePlayers().keySet()) {
						if(role.toLowerCase().startsWith(args[1].toLowerCase())) {
							completions.add(role);
						}
					}
				} else if((args[0].equalsIgnoreCase("bonzo") || args[0].equalsIgnoreCase("move")) && args.length <= 6) {
					// For bonzo/move, we don't provide completions for x,y,z,duration
					// but we also don't want to show completions after the expected number of args
					return completions;
				}
			}
		}

		return completions;
	}
}