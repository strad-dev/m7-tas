package instructions.bosses;

import instructions.Actions;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import plugin.Utils;

import java.util.Random;

@SuppressWarnings("DataFlowIssue")
public class Goldor {
	private static Wither goldor;
	private static World world;
	private static BossBar goldorBossBar;
	private static BukkitTask bossBarUpdateTask;
	private static final Random random = new Random();

	public static void goldorInstructions(World temp, boolean doContinue) {
		world = temp;

		if(goldor != null) {
			goldor.remove();
		}

		if(goldorBossBar != null) {
			goldorBossBar.removeAll();
			goldorBossBar = null;
		}

		if(bossBarUpdateTask != null) {
			bossBarUpdateTask.cancel();
			bossBarUpdateTask = null;
		}

		goldor = (Wither) world.spawnEntity(new Location(world, 80.5, 116, 40.5, -90f, 0f), EntityType.WITHER);
		goldor.setAI(false);
		goldor.setSilent(true);
		goldor.setPersistent(true);
		goldor.setRemoveWhenFarAway(false);
		goldor.setCustomName(ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " + ChatColor.RED + ChatColor.BOLD + "Goldor" + ChatColor.GOLD + ChatColor.BOLD + " ﴿ " + ChatColor.RED + "❤ " + ChatColor.YELLOW + 600 + "/" + 600);
		goldor.setCustomNameVisible(true);
		goldor.getAttribute(Attribute.MAX_HEALTH).setBaseValue(600);
		goldor.getAttribute(Attribute.ARMOR).setBaseValue(0);
		goldor.setHealth(600);
		goldor.addScoreboardTag("TASWither");
		Actions.setWitherArmor(goldor, true);

		Utils.scheduleTask(() -> CustomBossBar.setupWitherBossBar(goldor, "Goldor"), 1);

		sendChatMessage("Who dares trespass into my domain?");
		Actions.forceMove(goldor, new Vector(0.1, 0, 0), 200);
		Utils.scheduleTask(() -> sendChatMessage("Little ants, plotting and scheming, thinking they are invincibile..."), 60);
		Utils.scheduleTask(() -> sendChatMessage("I won't let you break the factory core, I gave my life to my Master."), 120);
		Utils.scheduleTask(() -> sendChatMessage("No one matches me in close quarters."), 180);
		Utils.scheduleTask(() -> Actions.turnHead(goldor, 0f, 0f), 199);
		Utils.scheduleTask(() -> Actions.forceMove(goldor, new Vector(0, 0, 0.1), 58), 200);
		Utils.scheduleTask(() -> {
			sendChatMessage("You have done it, you destroyed the factory...");
			Actions.turnHead(goldor, 97.186f, 0f);
			Actions.forceMove(goldor, new Vector(-0.7937, 0, -0.1), 52);
			Actions.setWitherArmor(goldor, false);
		}, 258);
		Utils.scheduleTask(() -> {
			sendChatMessage("...");
			Bukkit.broadcastMessage(ChatColor.GREEN + "Goldor killed in 54 ticks (2.70 seconds) | Goldor: 310 ticks (15.50 seconds) | Overall: 2 626 ticks (131.30 seconds)");
		}, 310);
		Utils.scheduleTask(() -> sendChatMessage("But you have nowhere to hide anymore!"), 318);
		Utils.scheduleTask(() -> sendChatMessage("Necron, forgive me."), 370);
		Utils.scheduleTask(() -> sendChatMessage("YOU ARE FACE TO FACE WITH GOLDOR!"), 378);
		Utils.scheduleTask(() -> {
			Bukkit.broadcastMessage(ChatColor.GREEN + "Goldor finished in 410 ticks (20.50 seconds) | Overall: 2 726 ticks (136.30 seconds)");
		}, 410);
		Utils.scheduleTask(goldor::remove, 470);
	}

	private static void sendChatMessage(String message) {
		Bukkit.broadcastMessage(ChatColor.DARK_RED + "[BOSS] Goldor" + ChatColor.RED + ": " + message);
		Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT);
	}

	public static void broadcastTerminalComplete(Player p, String type, int count, int total) {
		String message;
		if(type.equals("gate")) {
			message = ChatColor.GREEN + "The gate has been destroyed!";
		} else {
			String name;
			switch(p.getName()) {
				case "Archer" -> name = "akc0303";
				case "Berserk" -> name = "AsapIcey";
				case "Healer" -> name = "Meepy_";
				case "Mage" -> name = "Beethoven_";
				case "Tank" -> name = "cookiethebald";
				default -> name = "???";
			}
			message = ChatColor.GOLD + name + ChatColor.GREEN + " activated a " + type + "! (" + ChatColor.RED + count + ChatColor.GREEN + "/" + total + ")";
		}
		Bukkit.broadcastMessage(message);
		for(Player player : Bukkit.getOnlinePlayers()) {
			player.sendTitle("", message, 0, 40, 0);
		}
		Utils.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_PLING, 2.0F, 2.0F);
	}
}
