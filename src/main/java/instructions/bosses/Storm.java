package instructions.bosses;

import instructions.Actions;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Wither;
import org.bukkit.scheduler.BukkitTask;
import plugin.Utils;

@SuppressWarnings("DataFlowIssue")
public class Storm {
	private static Wither storm;
	@SuppressWarnings("FieldCanBeLocal")
	private static World world;
	private static BossBar stormBossBar;
	private static BukkitTask bossBarUpdateTask;

	public static void stormInstructions(World temp, boolean doContinue) {
		world = temp;

		if(storm != null) {
			storm.remove();
		}

		if(stormBossBar != null) {
			stormBossBar.removeAll();
			stormBossBar = null;
		}

		if(bossBarUpdateTask != null) {
			bossBarUpdateTask.cancel();
			bossBarUpdateTask = null;
		}

		storm = (Wither) world.spawnEntity(new Location(world, 73.5, 226, 53.5, 0f, 0f), EntityType.WITHER);
		storm.setAI(false);
		storm.setSilent(true);
		storm.setPersistent(true);
		storm.setRemoveWhenFarAway(false);
		storm.setCustomName(ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " + ChatColor.RED + ChatColor.BOLD + "Storm" + ChatColor.GOLD + ChatColor.BOLD + " ﴿ " + ChatColor.RED + "❤ " + ChatColor.YELLOW + 500 + "/" + 500);
		storm.setCustomNameVisible(true);
		storm.getAttribute(Attribute.MAX_HEALTH).setBaseValue(500);
		storm.getAttribute(Attribute.ARMOR).setBaseValue(0);
		storm.setHealth(400);
		storm.addScoreboardTag("TASWither");
		Actions.setWitherArmor(storm, true);

		Utils.scheduleTask(() -> CustomBossBar.setupWitherBossBar(storm, "Storm"), 1);
	}
}