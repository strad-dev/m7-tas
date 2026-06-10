package commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import plugin.FakePlayerManager;

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
					for(String role : FakePlayerManager.getFakePlayers().keySet()) {
						if(role.toLowerCase().startsWith(args[0].toLowerCase())) {
							completions.add(role);
						}
					}
				} else if(args.length > 1) {
					// Spectate only accepts one argument
					return completions;
				}
			}

			case "verbose" -> {
				if(args.length == 1) {
					for(String val : new String[]{"true", "false", "super"}) {
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

			case "simulate" -> {
				if(args.length == 1) {
					String[] simCommands = {"undo", "allblessings", "move", "click", "hotbar", "turnhead", "swapitems"};
					for(String cmd : simCommands) {
						if(cmd.startsWith(args[0].toLowerCase())) {
							completions.add(cmd);
						}
					}
				} else if(args.length == 2 && (args[0].equalsIgnoreCase("move") || args[0].equalsIgnoreCase("click") || args[0].equalsIgnoreCase("hotbar") || args[0].equalsIgnoreCase("turnhead") || args[0].equalsIgnoreCase("swapitems"))) {
					for(String role : FakePlayerManager.getFakePlayers().keySet()) {
						if(role.toLowerCase().startsWith(args[1].toLowerCase())) {
							completions.add(role);
						}
					}
				} else if(args.length == 3 && args[0].equalsIgnoreCase("click")) {
					for(String click : new String[]{"left", "right"}) {
						if(click.startsWith(args[2].toLowerCase())) {
							completions.add(click);
						}
					}
				} else if(args.length == 3 && args[0].equalsIgnoreCase("hotbar")) {
					for(int i = 0; i <= 8; i++) {
						String slot = String.valueOf(i);
						if(slot.startsWith(args[2])) {
							completions.add(slot);
						}
					}
				}
			}
		}

		return completions;
	}
}