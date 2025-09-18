package instructions.bosses;

import instructions.Actions;
import instructions.Server;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Wither;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import plugin.Utils;

import java.util.Random;

@SuppressWarnings("DataFlowIssue")
public class Necron {
	private static World world;
	private static Wither necron;
	private static BossBar necronBossBar;
	private static BukkitTask bossBarUpdateTask;
	private static final Random random = new Random();
	private static final String[] frenzyStartMessages = {"Sometimes when you have a problem, you just need to destroy it all and start again.", "WITNESS MY RAW NUCLEAR POWER!"};
	private static final String[] frenzyEndMessages = {"ARGH!", "Let's make some space!"};

	public static void necronInstructions(World temp, boolean doContinue) {
		world = temp;

		if(necron != null) {
			necron.remove();
		}

		if(necronBossBar != null) {
			necronBossBar.removeAll();
			necronBossBar = null;
		}

		if(bossBarUpdateTask != null) {
			bossBarUpdateTask.cancel();
			bossBarUpdateTask = null;
		}

		necron = (Wither) temp.spawnEntity(new Location(temp, 54.5, 66, 76.5, 0f, 0f), EntityType.WITHER);
		necron.setAI(false);
		necron.setSilent(true);
		necron.setPersistent(true);
		necron.setRemoveWhenFarAway(false);
		necron.setCustomName(ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " + ChatColor.RED + ChatColor.BOLD + "Necron" + ChatColor.GOLD + ChatColor.BOLD + " ﴿ " + ChatColor.RED + "❤ " + ChatColor.YELLOW + 700 + "/" + 700);
		necron.setCustomNameVisible(true);
		necron.getAttribute(Attribute.MAX_HEALTH).setBaseValue(700);
		necron.getAttribute(Attribute.ARMOR).setBaseValue(0);
		necron.setHealth(700);
		necron.addScoreboardTag("TASWither");
		Actions.setWitherArmor(necron, true);

		Utils.scheduleTask(() -> CustomBossBar.setupWitherBossBar(necron, "Necron"), 1);

		sendChatMessage("You went further than any human before, congratulations.");
		Utils.scheduleTask(() -> {
			sendChatMessage("I'm afraid your journey ends now.");
			destroyPlatform();
		}, 60);
		Utils.scheduleTask(() -> sendChatMessage("Goodbye."), 120);
		Utils.scheduleTask(() -> Actions.setWitherArmor(necron, false), 160);
		// first frenzy
		Utils.scheduleTask(() -> {
			Actions.setWitherArmor(necron, true);
			frenzy();
		}, 161);
		Utils.scheduleTask(() -> necron.setHealth(560), 162);
		Utils.scheduleTask(() -> sendChatMessage("That's a very impressive trick.  I guess I'll have to handle this myself."), 180);
		// damageable on tick 302
		// blow up platform
		Utils.scheduleTask(() -> {
			Actions.setWitherArmor(necron, true);
			destroyPlatform();
		}, 307);
		Utils.scheduleTask(() -> necron.setHealth(175), 308);
		// damagable on tick 368 (mage one beam)
		Utils.scheduleTask(() -> {
			Actions.setWitherArmor(necron, false);
			sendChatMessage(frenzyEndMessages[random.nextInt(frenzyEndMessages.length)]);
		}, 367);
		// second frenzy
		Utils.scheduleTask(() -> {
			Actions.setWitherArmor(necron, true);
			frenzy();
		}, 368);
		Utils.scheduleTask(() -> necron.setHealth(35), 369);
		// damageable on tick 508
		// die on tick 509
		Utils.scheduleTask(() -> {
			sendChatMessage("All this, for nothing...");
			Server.playWitherDeathSound(necron);
			Bukkit.broadcastMessage(ChatColor.GREEN + "Necron killed in 509 ticks (25.45 seconds) | Overall: 3 175 ticks (158.75 seconds)");
		}, 509);
		Utils.scheduleTask(() -> sendChatMessage("I understand your words now, my master."), 569);
		Utils.scheduleTask(() -> {
			Bukkit.broadcastMessage(ChatColor.GREEN + "Necron finished in 609 ticks (30.45 seconds) | Overall: 3 275 ticks (163.75 seconds)");
		}, 609);
		Utils.scheduleTask(() -> sendChatMessage("The Catacombs... are no more."), 629);

		/*
		 * note: in normal floor 7, Wither EHP is ridiculously low to the point that true one-tick is possible for all partitions
		 * in addition to negating the need to debuff, given this information, the following timesaves will occur in normal floor 7
		 * maxor: none
		 * storm: 13 ticks (8 ticks from first crush, 5 ticks from second crush)
		 * goldor: approx. 35-40 ticks, due to not needing to debuff, as well as being able to be one-tapped the moment he is not obstructed by blocks
		 * necron: 5 ticks (between first frenzy and platform destroy)
		 * thus, a solution of ~3 260 ticks will be the perfect F7 (163.00 seconds | 2:43.00)
		 */
		Utils.scheduleTask(() -> Bukkit.broadcastMessage(ChatColor.GOLD + "Normal Floor 7 Finishes Here in 3 315 ticks (167.75 seconds | 2:47.75)"), 649);
	}

	private static void destroyPlatform() {
		shootFireball();
		Utils.scheduleTask(Necron::shootFireball, 10);
		Utils.scheduleTask(Necron::shootFireball, 20);
		Utils.scheduleTask(Necron::shootFireball, 30);
		Utils.scheduleTask(() -> {
			Necron.shootFireball();
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "clone 70 -10 120 38 -6 99 38 59 99");
		}, 40);
		Utils.scheduleTask(Necron::shootFireball, 50);
		Utils.scheduleTask(Necron::shootFireball, 60);
		Utils.scheduleTask(Necron::shootFireball, 70);
	}

	private static void shootFireball() {
		Fireball fireball = (Fireball) world.spawnEntity(necron.getLocation().add(0, 3, 0), EntityType.FIREBALL);
		fireball.setVelocity(new Vector(0, -0.25, 1.25));
		Utils.scheduleTask(fireball::remove, 21);
		Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_IMPACT);
		Utils.playGlobalSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER);
	}

	private static void frenzy() {
		sendChatMessage(frenzyStartMessages[random.nextInt(frenzyStartMessages.length)]);
		Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE);
		Utils.scheduleTask(() -> {
			Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
			Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.67f);
		}, 20);
		Utils.scheduleTask(() -> {
			Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
			Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.67f);
		}, 40);
		Utils.scheduleTask(() -> {
			Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
			Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.67f);
		}, 60);
		Utils.scheduleTask(() -> {
			Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
			Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.67f);
		}, 80);
		Utils.scheduleTask(() -> {
			Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
			Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.67f);
		}, 100);
		Utils.scheduleTask(() -> {
			Utils.playGlobalSound(Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
			Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.67f);
		}, 120);
		Utils.scheduleTask(() -> {
			sendChatMessage(frenzyEndMessages[random.nextInt(frenzyEndMessages.length)]);
			Actions.setWitherArmor(necron, false);
		}, 140);
	}

	private static void sendChatMessage(String message) {
		Bukkit.broadcastMessage(ChatColor.DARK_RED + "[BOSS] Necron" + ChatColor.RED + ": " + message);
		Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT);
	}
}