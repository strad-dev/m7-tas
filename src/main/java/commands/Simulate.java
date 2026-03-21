package commands;

import instructions.Actions;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import plugin.M7tas;
import plugin.Utils;

public class Simulate implements CommandExecutor {
	private static Player lastSimulated;
	private static Location lastSimulatedLocation;

	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command cmd, @NonNull String label, String @NonNull [] args) {
		if(!(sender instanceof Player p)) {
			sender.sendMessage("Only players can run this");
			return true;
		}

		if(args.length < 1) {
			p.sendMessage(ChatColor.RED + "Please specify a movement to simulate");
			return true;
		}
		switch(args[0]) {
			case "undo" -> {
				if(lastSimulated == null || lastSimulatedLocation == null) {
					p.sendMessage(ChatColor.RED + "No previous movement to undo!");
					return true;
				}
				lastSimulated.teleport(lastSimulatedLocation);
				lastSimulated = null;
				lastSimulatedLocation = null;
				p.sendMessage(ChatColor.GREEN + "Undid previous simulation instruction");
				return true;
			}
			case "allblessings" -> {
				Utils.broadcastBlessing(p, Utils.BlessingType.LIFE, 1);
				Utils.broadcastBlessing(p, Utils.BlessingType.LIFE, 2);
				Utils.broadcastBlessing(p, Utils.BlessingType.LIFE, 5);
				Utils.broadcastBlessing(p, Utils.BlessingType.POWER, 1);
				Utils.broadcastBlessing(p, Utils.BlessingType.POWER, 2);
				Utils.broadcastBlessing(p, Utils.BlessingType.POWER, 5);
				Utils.broadcastBlessing(p, Utils.BlessingType.STONE, 1);
				Utils.broadcastBlessing(p, Utils.BlessingType.STONE, 2);
				Utils.broadcastBlessing(p, Utils.BlessingType.STONE, 5);
				Utils.broadcastBlessing(p, Utils.BlessingType.WISDOM, 1);
				Utils.broadcastBlessing(p, Utils.BlessingType.WISDOM, 2);
				Utils.broadcastBlessing(p, Utils.BlessingType.WISDOM, 5);
				Utils.broadcastBlessing(p, Utils.BlessingType.TIME, 5);
				return true;
			}
			case "bonzo" -> {
				p.sendMessage(ChatColor.YELLOW + "This command is being reworked and will be available in 3-5 business days");
//						if(args.length < 2) {
//							p.sendMessage(ChatColor.RED + "Please specify a player to apply the movement to");
//							return true;
//						}
//						Player applyTo = fakePlayers.get(Character.toUpperCase(args[1].charAt(0)) + args[1].substring(1).toLowerCase());
//						if(args.length < 5) {
//							p.sendMessage(ChatColor.RED + "Please specify X Y Z of the movement");
//							return true;
//						}
//						double x;
//						double y;
//						double z;
//						try {
//							x = Double.parseDouble(args[2]);
//							y = Double.parseDouble(args[3]);
//							z = Double.parseDouble(args[4]);
//						} catch(Exception exception) {
//							p.sendMessage(ChatColor.RED + "Movement must be an double");
//							return true;
//						}
//						lastSimulated = applyTo;
//						lastSimulatedLocation = applyTo.getLocation();
////						Actions.bonzo(applyTo, new Vector(x, y, z));
//						p.sendMessage(ChatColor.GREEN + "Simulating Bonzo movement for " + applyTo.getName());
				return true;
			}
			// Syntax: /simulate move [player] [set from: {W, A, S, D, J, P, N}] [ticks]
			// WASD - self explainatory
			// J - jump
			// P - sprint
			// N - sneak
			// ticks - number of ticks to hold down, must be greater than zero
			// Example: /simulate move WDP 5
			// Moves the plaer forward and to the right while sprinting for 5 ticks
			case "move" -> {
				if(args.length < 2) {
					p.sendMessage(ChatColor.RED + "Please specify a player to apply the movement to");
					return true;
				}
				Player applyTo = M7tas.getFakePlayers().get(Character.toUpperCase(args[1].charAt(0)) + args[1].substring(1).toLowerCase());
				if(applyTo == null) {
					p.sendMessage(ChatColor.RED + "Player " + args[1] + " is not a fake player");
					return true;
				}
				if(args.length < 3) {
					p.sendMessage(ChatColor.RED + "Please specify which keys are held down");
					return true;
				}
				String inputString = args[2].toUpperCase();
				if(!inputString.matches("[WASDJPN]*")) {
					p.sendMessage(ChatColor.RED + "Bad characters detected!  Accepted characters: WASDJPN");
					return true;
				}
				if(args.length < 4) {
					p.sendMessage(ChatColor.RED + "Must provide a valid duration");
					return true;
				}
				int duration;
				try {
					duration = Integer.parseInt(args[3]);
				} catch(Exception exception) {
					p.sendMessage(ChatColor.RED + "Duration must be an integer");
					return true;
				}
				if(duration <= 0) {
					p.sendMessage(ChatColor.RED + "Duration must be one or higher");
					return true;
				}
				lastSimulated = applyTo;
				lastSimulatedLocation = applyTo.getLocation();
				Actions.move(applyTo, inputString, duration);
				p.sendMessage(ChatColor.GREEN + "Moved " + applyTo.getName() + " for " + duration + " ticks");
				return true;
			}
//					case "explosiveshot" -> Archer.explosiveShot();
		}
		return false;
	}
}