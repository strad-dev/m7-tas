package commands;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import plugin.M7tas;
import plugin.Utils;

public class SetSpeed implements CommandExecutor {
	public boolean onCommand(@NonNull CommandSender sender, @NonNull Command cmd, @NonNull String label, String @NonNull [] args) {
		if(!(sender instanceof Player p)) {
			sender.sendMessage("Only players can run this");
			return true;
		}
		if(args.length == 0) {
			NamespacedKey key = new NamespacedKey(M7tas.getInstance(), "speed");
			int current = 100;
			for(AttributeModifier mod : p.getAttribute(Attribute.MOVEMENT_SPEED).getModifiers()) {
				if(mod.getKey().equals(key)) {
					current = (int) Math.round(mod.getAmount() * 100 + 100);
					break;
				}
			}
			sender.sendMessage("Current speed: " + current);
			return true;
		}
		if(args.length != 1) {
			sender.sendMessage("Usage: /setspeed <speed>");
			return true;
		}
		int speed;
		try {
			speed = Integer.parseInt(args[0]);
		} catch(NumberFormatException e) {
			sender.sendMessage("Speed must be an integer");
			return true;
		}
		Utils.setSpeed(p, speed);
		p.sendMessage("Speed set to " + speed);
		return true;
	}
}
