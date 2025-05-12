package instructions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import plugin.M7tas;

public class Mage {
	private static Player mage;

	public static void mageInstructions(Player p) {
		mage = p;
		System.out.println("Mage Instructions: " + p.getName());
		p.teleport(new Location(p.getWorld(), -132.5, 69, -76.5));
		Actions.swapFakePlayerInventorySlots(p, 3, 29);
		Actions.setFakePlayerHotbarSlot(p, 3);
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> Actions.simulateRightClickAir(p), 101);
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> Actions.move(p, new Vector(0, 0, 0.8634), 4), 102);
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> p.teleport(new Location(p.getWorld(), -120.5, 75, -220.5)), 142);
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> Actions.setFakePlayerHotbarSlot(p, 5), 162);
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> p.teleport(Archer.getArcher()), 163);
	}

	public static Player getMage() {
		return mage;
	}
}
