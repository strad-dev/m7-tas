package instructions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import plugin.M7tas;

public class Archer {
	private static Player archer;

	public static void archerInstructions(Player p) {
		archer = p;
		System.out.println("Archer Instructions: " + p.getName());
		p.teleport(new Location(p.getWorld(), -120.5, 75, -220.5));
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> Actions.move(p, new Vector(0, 0.42, 0.8634), 4), 60);
	}

	public static Player getArcher() {
		return archer;
	}
}
