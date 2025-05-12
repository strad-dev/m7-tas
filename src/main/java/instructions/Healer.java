package instructions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import plugin.Utils;

public class Healer {
	private static Player healer;

	public static void healerInstructions(Player p) {
		healer = p;
		System.out.println("Healer Instructions: " + p.getName());
		p.teleport(new Location(p.getWorld(), -28.5, 70, -44.5, -180, 0));
		Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(p, 2, 29), 60);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(p, 2), 61);
		Utils.scheduleTask(() -> Actions.simulateRightClickAir(p), 101);
		Utils.scheduleTask(() -> Actions.move(p, new Vector(0, 0, 0.8634), 4), 102);
		Utils.scheduleTask(() -> p.teleport(new Location(p.getWorld(), -120.5, 75, -220.5)), 141);
	}

	public static Player getHealer() {
		return healer;
	}
}
