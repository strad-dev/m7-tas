package instructions;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Archer {
	private static Player archer;

	public static void archerInstructions(Player p) {
		archer = p;
		System.out.println("Archer Instructions: " + p.getName());
		p.teleport(new Location(p.getWorld(), -120.5, 75, -202.5));
	}

	public static Player getArcher() {
		return archer;
	}
}
