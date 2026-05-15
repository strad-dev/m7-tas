package commands;

import instructions.Actions;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import plugin.FakePlayerManager;
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
				Player applyTo = FakePlayerManager.getFakePlayers().get(Character.toUpperCase(args[1].charAt(0)) + args[1].substring(1).toLowerCase());
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
			case "click" -> {
				if(args.length < 2) {
					p.sendMessage(ChatColor.RED + "Please specify a player to apply the click to");
					return true;
				}
				Player applyTo = FakePlayerManager.getFakePlayers().get(Character.toUpperCase(args[1].charAt(0)) + args[1].substring(1).toLowerCase());
				if(applyTo == null) {
					p.sendMessage(ChatColor.RED + "Player " + args[1] + " is not a fake player");
					return true;
				}
				if(args.length < 3) {
					p.sendMessage(ChatColor.RED + "Please specify which click is being simulated");
					return true;
				}
				String click = args[2].toLowerCase();
				if(!click.equals("left") && !click.equals("right") && !click.equals("release")) {
					p.sendMessage(ChatColor.RED + "Invalid click specified.  Valid clicks: left, right, release");
					return true;
				}
				lastSimulated = applyTo;
				lastSimulatedLocation = applyTo.getLocation();
				if(click.equals("left")) {
					Actions.leftClick(applyTo);
				} else if(click.equals("right")) {
					Actions.rightClick(applyTo);
				} else {
					Actions.stopRightClick(applyTo);
				}
				p.sendMessage(ChatColor.GREEN + applyTo.getName() + " " + click + " clicked");
				return true;
			}
			case "turnhead" -> {
				if(args.length < 2) {
					p.sendMessage(ChatColor.RED + "Please specify a player to apply the turn to");
					return true;
				}
				Player applyTo = FakePlayerManager.getFakePlayers().get(Character.toUpperCase(args[1].charAt(0)) + args[1].substring(1).toLowerCase());
				if(applyTo == null) {
					p.sendMessage(ChatColor.RED + "Player " + args[1] + " is not a fake player");
					return true;
				}
				if(args.length < 3) {
					p.sendMessage(ChatColor.RED + "Please specify a yaw");
					return true;
				}
				if(args.length < 4) {
					p.sendMessage(ChatColor.RED + "Please specify a pitch");
					return true;
				}
				float yaw, pitch;
				try {
					yaw = Float.parseFloat(args[2]);
					pitch = Float.parseFloat(args[3]);
				} catch(Exception exception) {
					p.sendMessage(ChatColor.RED + "Yaw and pitch must be numbers");
					return true;
				}
				Actions.turnHead(applyTo, yaw, pitch);
				p.sendMessage(ChatColor.GREEN + applyTo.getName() + " turned to yaw=" + yaw + " pitch=" + pitch);
				return true;
			}
			case "hotbar" -> {
				if(args.length < 2) {
					p.sendMessage(ChatColor.RED + "Please specify a player to apply hotbar change to");
					return true;
				}
				Player applyTo = FakePlayerManager.getFakePlayers().get(Character.toUpperCase(args[1].charAt(0)) + args[1].substring(1).toLowerCase());
				if(applyTo == null) {
					p.sendMessage(ChatColor.RED + "Player " + args[1] + " is not a fake player");
					return true;
				}
				if(args.length < 3) {
					p.sendMessage(ChatColor.RED + "Please specify which slot to change to");
					return true;
				}
				int slot;
				try {
					slot = Integer.parseInt(args[2]);
				} catch(Exception exception) {
					p.sendMessage(ChatColor.RED + "Slot must be an integer");
					return true;
				}
				if(slot < 0 || slot > 8) {
					p.sendMessage(ChatColor.RED + "Slot must be between 0 and 8");
					return true;
				}
				Actions.setHotbarSlot(applyTo, slot);
				p.sendMessage(ChatColor.GREEN + applyTo.getName() + " hotbar slot changed to " + slot);
				return true;
			}
		}
		return false;
	}
}