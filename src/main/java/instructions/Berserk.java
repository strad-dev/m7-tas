package instructions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import plugin.M7tas;

public class Berserk {
	private static Player berserk;

	public static void berserkInstructions(Player p) {
		berserk = p;
		System.out.println("Berserk Instructions: " + p.getName());
		p.teleport(new Location(p.getWorld(), -120.5, 72, -220.5));
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> Actions.move(p, new Vector(-0.8634, 0, 0), 4), 80);
	}

	public static Player getBerserk() {
		return berserk;
	}
}
