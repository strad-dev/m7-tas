package instructions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import plugin.M7tas;

public class Healer {
	private static Player healer;

	public static void healerInstructions(Player p) {
		healer = p;
		System.out.println("Healer Instructions: " + p.getName());
		p.teleport(new Location(p.getWorld(), -120.5, 72, -220.5));
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> Actions.move(p, new Vector(0, 0, 0.8634), 4), 120);
	}

	public static Player getHealer() {
		return healer;
	}
}
