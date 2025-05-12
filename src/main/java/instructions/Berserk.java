package instructions;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import plugin.Utils;

public class Berserk {
	private static Player berserk;

	public static void berserkInstructions(Player p) {
		berserk = p;
		System.out.println("Berserk Instructions: " + p.getName());
		p.teleport(new Location(p.getWorld(), -21.5, 70, -197.5, 0, 0));
		Utils.scheduleTask(() -> Actions.swapFakePlayerInventorySlots(p, 2, 29), 60);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(p, 2), 61);
		Utils.scheduleTask(() -> Actions.simulateRightClickAir(p), 101);
		Utils.scheduleTask(() -> Actions.move(p, new Vector(0, 0, -0.8634), 3), 102);
		Utils.scheduleTask(() -> p.teleport(new Location(p.getWorld(), -120.5, 75, -220.5)), 141);

		// Begin Tic Tac Toe clear
		Utils.scheduleTask(() -> {
			Actions.setFakePlayerHotbarSlot(p, 1);
			Actions.turnHead(p, 90, 37.5f);
		}, 162);
		Utils.scheduleTask(() -> Actions.simulateAOTV(p, new Location(p.getWorld(), -24.5, 69, -197.5)), 163);
		Utils.scheduleTask(() -> Actions.turnHead(p, -176, 2.9f), 164);
		Utils.scheduleTask(() -> Actions.simulateAOTV(p, new Location(p.getWorld(), -22.5, 69, -223.5)), 165);
		Utils.scheduleTask(() -> {
			Actions.turnHead(p, -126.9f, 12.2f);
			Actions.setFakePlayerHotbarSlot(p, 5);
		}, 166);
		Utils.scheduleTask(() -> Actions.simulateStonking(p, p.getWorld().getBlockAt(-22, 70, -224)), 167);
		Utils.scheduleTask(() -> Actions.simulateStonking(p, p.getWorld().getBlockAt(-22, 70, -225)), 168);
		Utils.scheduleTask(() -> Actions.simulateStonking(p, p.getWorld().getBlockAt(-21, 70, -225)), 169);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(p);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Tic Tac Toe 1/1 (Opened Chest)");
			p.playSound(p, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 170);
		Utils.scheduleTask(() -> {
			Actions.turnHead(p, 3.1f, 1.9f);
			Actions.setFakePlayerHotbarSlot(p, 1);
		}, 171);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(p, new Location(p.getWorld(), -24.5f, 69, -184.5f)), 172);

		// Chambers
		Utils.scheduleTask(() -> Actions.turnHead(p, -60.4f, 28), 173);
		Utils.scheduleTask(() -> Actions.simulateAOTV(p, new Location(p.getWorld(), -20.5, 69, -182.5f)), 174);
		Utils.scheduleTask(() -> Actions.turnHead(p, -138.5f, 90), 175);
		Utils.scheduleTask(() -> Actions.simulateAOTV(p, new Location(p.getWorld(), -20.5, 59, -182.5f)), 176);
		Utils.scheduleTask(() -> Actions.turnHead(p, -134.2f, 51.1f), 177);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(p);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 1/5 (Opened Chest)");
			p.playSound(p, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 178);
		Utils.scheduleTask(() -> Actions.turnHead(p, 0, -90), 179);
		Utils.scheduleTask(() -> Actions.simulateAOTV(p, new Location(p.getWorld(), -20.5, 72, -182.5f)), 180);
		Utils.scheduleTask(() -> Actions.turnHead(p, 80.7f, -68.3f), 181);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(p, new Location(p.getWorld(), -23.5f, 82, -181.5f)), 182);
		Utils.scheduleTask(() -> Actions.turnHead(p, 57.5f, 7.6f), 183);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(p, new Location(p.getWorld(), -31.5f, 82, -176.5f)), 184);
		Utils.scheduleTask(() -> {
			Actions.turnHead(p, 62.5f, 10.8f);
			Actions.setFakePlayerHotbarSlot(p, 0);
		}, 185);
		Utils.scheduleTask(() -> {
			Actions.simulateRightClickAir(p);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 2/5 (Killed Bat)");
			p.playSound(p, Sound.ENTITY_BAT_DEATH, 1.0F, 1.0F);
		}, 186);
		Utils.scheduleTask(() -> {
			Actions.turnHead(p, 163.5f, 7.9f);
			Actions.setFakePlayerHotbarSlot(p, 1);
		}, 187);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(p, new Location(p.getWorld(), -38.5f, 80, -183.5f)), 188);
		Utils.scheduleTask(() -> Actions.turnHead(p, 109.5f, 4.1f), 189);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(p, new Location(p.getWorld(), -55.5f, 80, -189.5f)), 190);
		Utils.scheduleTask(() -> Actions.turnHead(p, 143.8f, 2.1f), 191);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(p, new Location(p.getWorld(), -45.5f, 81, -203.5f)), 192);
		Utils.scheduleTask(() -> Actions.turnHead(p, 174.1f, 7.4f), 193);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(p, new Location(p.getWorld(), -44.5f, 81, -213.5f)), 194);
		Utils.scheduleTask(() -> {
			Actions.turnHead(p, 146.5f, -2.2f);
			Actions.setFakePlayerHotbarSlot(p, 3);
		}, 195);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(p, -47, 81, -217, -46, 84, -216), 196);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(p, 1), 197);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(p, new Location(p.getWorld(), -54.5f, 83.5, -228.5f)), 198);
		Utils.scheduleTask(() -> Actions.turnHead(p, 116.2f, 38.9f), 199);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(p);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 3/5 (Opened Chest)");
			p.playSound(p, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 200);
		Utils.scheduleTask(() -> Actions.turnHead(p, 0.7f, -5.8f), 201);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(p, new Location(p.getWorld(), -54.5f, 86, -219.5f)), 202);
		Utils.scheduleTask(() -> {
			Actions.turnHead(p, 0f, 45f);
			Actions.setFakePlayerHotbarSlot(p, 5);
		}, 203);
		Utils.scheduleTask(() -> Actions.simulateStonking(p, p.getWorld().getBlockAt(-55, 87, -219)), 204);
		Utils.scheduleTask(() -> {
			Actions.simulateStonking(p, p.getWorld().getBlockAt(-55, 86, -219));
			Actions.move(p, new Vector(0, 0, 0.8634), 2);
		}, 205);
		Utils.scheduleTask(() -> {
			Actions.turnHead(p, 14.8f, 44f);
			Actions.setFakePlayerHotbarSlot(p, 1);
		}, 206);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(p, new Location(p.getWorld(), -59.5f, 69, -198.5f)), 207);
		Utils.scheduleTask(() -> Actions.turnHead(p, 33.5f, 17.3f), 208);
		Utils.scheduleTask(() -> Actions.simulateAOTV(p, new Location(p.getWorld(), -62.5f, 69, -194.5f)), 209);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(p, 5), 210);
		Utils.scheduleTask(() -> Actions.simulateStonking(p, p.getWorld().getBlockAt(-64, 70, -194)), 211);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(p);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 4/5 (Opened Chest)");
			p.playSound(p, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 212);
		Utils.scheduleTask(() -> {
			Actions.turnHead(p, -90f, 11.7f);
			Actions.setFakePlayerHotbarSlot(p, 1);
		}, 213);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(p, new Location(p.getWorld(), -56.5f, 69, -194.5f)), 214);
		Utils.scheduleTask(() -> Actions.turnHead(p, 9.3f, 3.8f), 215);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(p, new Location(p.getWorld(), -58.5f, 69, -176.5f)), 216);
		Utils.scheduleTask(() -> {
			Actions.turnHead(p, 74.9f, 8.4f);
			Actions.setFakePlayerHotbarSlot(p, 3);
		}, 217);
		Utils.scheduleTask(() -> Actions.simulateSuperboom(p, -61, 69, -177, -62, 73, -174), 218);
		Utils.scheduleTask(() -> Actions.setFakePlayerHotbarSlot(p, 1), 219);
		Utils.scheduleTask(() -> Actions.simulateAOTV(p, new Location(p.getWorld(), -66.5f, 69f, -174.5f)), 220);
		Utils.scheduleTask(() -> {
			Actions.simulateLeftClickAir(p);
			Bukkit.broadcastMessage(ChatColor.RED + "Berserk: Chambers 5/5 (Opened Chest)");
			p.playSound(p, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
		}, 221);
		Utils.scheduleTask(() -> {
			Actions.turnHead(p, -99.7f, -3.4f);
			Actions.setFakePlayerHotbarSlot(p, 1);
		}, 222);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(p, new Location(p.getWorld(), -54.5f, 71.5, -176.5f)), 223);
		Utils.scheduleTask(() -> Actions.turnHead(p, 165.2f, 5.9f), 224);
		Utils.scheduleTask(() -> Actions.simulateEtherwarp(p, new Location(p.getWorld(), -64.5f, 69, -215.5)), 225);
		Utils.scheduleTask(() -> {
			Actions.turnHead(p, 90f, 0f);
			Actions.setFakePlayerHotbarSlot(p, 4);
		}, 226);
		Utils.scheduleTask(() -> {
			Actions.move(p, new Vector(-0.8634, 0, 0), 5);
			Actions.simulateRightClickAir(p);
		},227);
		Utils.scheduleTask(() -> Actions.simulateRightClickAir(p), 232);
	}

	public static Player getBerserk() {
		return berserk;
	}
}