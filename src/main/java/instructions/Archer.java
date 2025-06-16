package instructions;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class Archer {
	private static Player archer;
	@SuppressWarnings("FieldCanBeLocal")
	private static World world;

	public static void archerInstructions(Player p) {
		archer = p;
		world = archer.getWorld();
		Actions.simulateAOTV(archer, new Location(world, -120.5, 69, -202.5));
	}

	public static Player getArcher() {
		return archer;
	}
}
