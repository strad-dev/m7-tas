package instructions.bosses;

import instructions.Actions;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import plugin.Utils;

@SuppressWarnings("DataFlowIssue")
public class Maxor {
	private static Wither maxor;
	@SuppressWarnings("FieldCanBeLocal")
	private static World world;
	private static BossBar maxorBossBar;
	private static BukkitTask bossBarUpdateTask;
	private static EnderCrystal leftCrystal;
	private static EnderCrystal rightCrystal;

	public static void maxorInstructions(World temp, boolean doContinue) {
		world = temp;

		if(maxor != null) {
			maxor.remove();
		}

		if(maxorBossBar != null) {
			maxorBossBar.removeAll();
			maxorBossBar = null;
		}

		if(bossBarUpdateTask != null) {
			bossBarUpdateTask.cancel();
			bossBarUpdateTask = null;
		}

		if(leftCrystal != null) {
			leftCrystal.remove();
		}

		if(rightCrystal != null) {
			rightCrystal.remove();
		}

		maxor = (Wither) world.spawnEntity(new Location(world, 73.5, 227, 53.5, 0f, 0f), EntityType.WITHER);
		maxor.setAI(false);
		maxor.setSilent(true);
		maxor.setPersistent(true);
		maxor.setRemoveWhenFarAway(false);
		maxor.setCustomName(ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " + ChatColor.RED + ChatColor.BOLD + "Maxor" + ChatColor.GOLD + ChatColor.BOLD + " ﴿ " + ChatColor.RED + "❤ " + ChatColor.YELLOW + 400 + "/" + 400);
		maxor.setCustomNameVisible(true);
		maxor.getAttribute(Attribute.MAX_HEALTH).setBaseValue(400);
		maxor.getAttribute(Attribute.ARMOR).setBaseValue(0);
		maxor.setHealth(400);
		Actions.setWitherArmor(maxor, true);

		Utils.scheduleTask(() -> CustomBossBar.setupWitherBossBar(maxor, "Maxor"), 1);

		leftCrystal = (EnderCrystal) world.spawnEntity(new Location(world, 82.5, 238.48, 50.5), EntityType.END_CRYSTAL);
		leftCrystal.setCustomName("Energy Crystal");
		leftCrystal.setCustomNameVisible(true);
		rightCrystal = (EnderCrystal) world.spawnEntity(new Location(world, 64.5, 238.48, 50.5), EntityType.END_CRYSTAL);
		rightCrystal.setCustomName("Energy Crystal");
		rightCrystal.setCustomNameVisible(true);

		sendChatMessage("WELL WELL WELL, LOOK WHO'S HERE!");
		Utils.scheduleTask(() -> sendChatMessage("I'VE BEEN TOLD I COULD HAVE A BIT OF FUN WITH YOU."), 60);
		Utils.scheduleTask(() -> sendChatMessage("DON'T DISAPPOINT ME, I HAVEN'T HAD A GOOD FIGHT IN A WHILE."), 120);
		Utils.scheduleTask(() -> {
			Actions.move(maxor, new Vector(0, 0, 0.5), 38);
			Utils.playGlobalSound(Sound.ENTITY_WITHER_SPAWN);
		}, 160);
		Utils.scheduleTask(() -> {
			sendChatMessage("YOU TRICKED ME!");
			Actions.setWitherArmor(maxor, false);
		}, 198);
	}

	public static void pickUpCrystal(Player p) {
		if(p.getName().contains("Berserk") && rightCrystal != null) {
			rightCrystal.remove();
		} else if(p.getName().contains("Mage") && leftCrystal != null) {
			leftCrystal.remove();
		}
	}

	private static void sendChatMessage(String message) {
		Bukkit.broadcastMessage(ChatColor.DARK_RED + "[BOSS] MAXOR" + ChatColor.RED + ": " + message);
		Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT);
	}
}
