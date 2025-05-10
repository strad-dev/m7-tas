package instructions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import plugin.M7tas;

public class Archer {
	public static void archerInstructions(Player p) {
		System.out.println("Archer Instructions: " + p.getName());
		p.teleport(new Location(p.getWorld(), -120.5, 72, -220.5));
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> Actions.move(p, new Vector(0.8634, 0, 0), 4), 40);
	}
}
