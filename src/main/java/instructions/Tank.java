package instructions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import plugin.M7tas;

public class Tank {
	private static Player tank;

	public static void tankInstructions(Player p) {
		tank = p;
		System.out.println("Tank Instructions: " + p.getName());
		p.teleport(new Location(p.getWorld(), -120.5, 72, -220.5));
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> Actions.move(p, new Vector(0, 0.42, 0), 1), 200);
	}

	public static Player getTank() {
		return tank;
	}
}