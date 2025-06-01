package instructions;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import plugin.Utils;

public class Mage {
	private static Player mage;
	private static World world;

	public static void mageInstructions(Player p) {
		mage = p;
		world = mage.getWorld();
		Actions.turnHead(mage, -180f, 0f);
		Actions.simulateAOTV(mage, new Location(world, -132.5, 69, -76.5));
		Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(p, 2, 29), 60);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(p, 2), 61);
		Utils.scheduleTask(() -> Actions.simulateRightClickAir(p), 101);
		Utils.scheduleTask(() -> Actions.move(p, new Vector(0, 0, 0.8634), 4), 102);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(p, 4), 121);
		Utils.scheduleTask(() -> p.teleport(new Location(p.getWorld(), -120.5, 75, -220.5)), 141);
		Utils.scheduleTask(() -> p.teleport(Archer.getArcher()), 162);
	}

	public static Player getMage() {
		return mage;
	}
}
