package commands;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import plugin.FakePlayerManager;
import plugin.M7tas;
import plugin.Utils;

public class SetSpeed implements CommandExecutor {
	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command cmd, @NonNull String label, String @NonNull [] args) {
		// /setspeed                  -> show self speed
		// /setspeed <speed>          -> set self speed
		// /setspeed <player>         -> show named player's speed
		// /setspeed <player> <speed> -> set named player's speed
		if(args.length == 0) {
			if(!(sender instanceof Player p)) {
				sender.sendMessage("Console must specify a player: /setspeed <player> [speed]");
				return true;
			}
			sender.sendMessage("Current speed: " + getSpeed(p));
			return true;
		}
		if(args.length == 1) {
			Integer parsed = tryParseInt(args[0]);
			if(parsed != null) {
				if(!(sender instanceof Player p)) {
					sender.sendMessage("Console must specify a player: /setspeed <player> <speed>");
					return true;
				}
				Utils.setSpeed(p, parsed);
				sender.sendMessage("Speed set to " + parsed);
				return true;
			}
			Player target = resolvePlayer(args[0]);
			if(target == null) {
				sender.sendMessage("No player named '" + args[0] + "'");
				return true;
			}
			sender.sendMessage(target.getName() + " current speed: " + getSpeed(target));
			return true;
		}
		if(args.length == 2) {
			Player target = resolvePlayer(args[0]);
			if(target == null) {
				sender.sendMessage("No player named '" + args[0] + "'");
				return true;
			}
			Integer speed = tryParseInt(args[1]);
			if(speed == null) {
				sender.sendMessage("Speed must be an integer");
				return true;
			}
			Utils.setSpeed(target, speed);
			sender.sendMessage(target.getName() + " speed set to " + speed);
			return true;
		}
		sender.sendMessage("Usage: /setspeed [player] [speed]");
		return true;
	}

	private static Player resolvePlayer(String name) {
		Player fake = FakePlayerManager.getFakePlayers().get(name);
		if(fake != null) return fake;
		return Bukkit.getPlayerExact(name);
	}

	private static Integer tryParseInt(String s) {
		try {
			return Integer.parseInt(s);
		} catch(NumberFormatException e) {
			return null;
		}
	}

	private static int getSpeed(Player p) {
		NamespacedKey key = new NamespacedKey(M7tas.getInstance(), "speed");
		for(AttributeModifier mod : p.getAttribute(Attribute.MOVEMENT_SPEED).getModifiers()) {
			if(mod.getKey().equals(key)) {
				return (int) Math.round(mod.getAmount() * 100 + 100);
			}
		}
		return 100;
	}
}
