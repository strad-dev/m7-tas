package instructions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import plugin.Utils;

public class Tank {
	private static Player tank;

	public static void tankInstructions(Player p) {
		tank = p;
		System.out.println("Tank Instructions: " + p.getName());
		p.teleport(new Location(p.getWorld(), -197.5, 67, -223.5, -90, 0));
		Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(p, 2, 29), 60);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(p, 2), 61);
		Utils.scheduleTask(() -> Actions.simulateRightClickAir(p), 101);
		Utils.scheduleTask(() -> Actions.move(p, new Vector(-0.8634, 0, 0), 4), 102);
		Utils.scheduleTask(() -> p.teleport(new Location(p.getWorld(), -120.5, 75, -220.5)), 141);
	}

	public static Player getTank() {
		return tank;
	}
}