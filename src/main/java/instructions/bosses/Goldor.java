package instructions.bosses;

import instructions.Actions;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;
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

		goldor = (Wither) world.spawnEntity(new Location(world, 80.5, 120, 40.5, -90f, 0f), EntityType.WITHER);
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
		Utils.scheduleTask(() -> sendChatMessage("Little ants, plotting and scheming, thinking they are invincibile..."), 60);
		Utils.scheduleTask(() -> sendChatMessage("I won't let you break the factory core, I gave my life to my Master."), 120);
		Utils.scheduleTask(() -> sendChatMessage("No one matches me in close quarters."), 180);
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
