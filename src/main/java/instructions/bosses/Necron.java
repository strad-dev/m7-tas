package instructions.bosses;

import instructions.Actions;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Wither;
import org.bukkit.scheduler.BukkitTask;
import plugin.Utils;

@SuppressWarnings("DataFlowIssue")
public class Necron {
	private static Wither goldor;
	private static BossBar goldorBossBar;
	private static BukkitTask bossBarUpdateTask;

	public static void necronInstructions(World temp, boolean doContinue) {
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

		goldor = (Wither) temp.spawnEntity(new Location(temp, 54.5, 65, 76.5, 0f, 0f), EntityType.WITHER);
		goldor.setAI(false);
		goldor.setSilent(true);
		goldor.setPersistent(true);
		goldor.setRemoveWhenFarAway(false);
		goldor.setCustomName(ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " + ChatColor.RED + ChatColor.BOLD + "Necron" + ChatColor.GOLD + ChatColor.BOLD + " ﴿ " + ChatColor.RED + "❤ " + ChatColor.YELLOW + 700 + "/" + 700);
		goldor.setCustomNameVisible(true);
		goldor.getAttribute(Attribute.MAX_HEALTH).setBaseValue(700);
		goldor.getAttribute(Attribute.ARMOR).setBaseValue(0);
		goldor.setHealth(600);
		goldor.addScoreboardTag("TASWither");
		Actions.setWitherArmor(goldor, true);

		Utils.scheduleTask(() -> CustomBossBar.setupWitherBossBar(goldor, "Necron"), 1);

		sendChatMessage("You went further than any human before, congratulations.");
		Utils.scheduleTask(() -> sendChatMessage("I'm afraid your journey ends now."), 60);
		Utils.scheduleTask(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "clone 70 -10 120 38 -6 99 38 59 99"), 100);
		Utils.scheduleTask(() -> sendChatMessage("Goodbye."), 120);
		Utils.scheduleTask(() -> sendChatMessage("That's a very impressive trick.  I guess I'll have to handle this myself."), 180);
	}

	private static void sendChatMessage(String message) {
		Bukkit.broadcastMessage(ChatColor.DARK_RED + "[BOSS] Necron" + ChatColor.RED + ": " + message);
		Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT);
	}
}