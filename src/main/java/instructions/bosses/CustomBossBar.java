package instructions.bosses;

import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftWither;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Wither;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import plugin.M7tas;
import plugin.Utils;

@SuppressWarnings("DataFlowIssue")
public class CustomBossBar {
	// Add these fields to your class
	private static BossBar activeWitherBossBar;
	private static Wither activeWither;
	private static BukkitTask bossBarUpdateTask;
	private static TextDisplay activeStunIndicator;

	// Generic method to handle any Wither boss bar
	public static void setupWitherBossBar(Wither wither, String witherName) {
		// Clean up any existing boss bar
		cleanupActiveBossBar();

		activeWither = wither;
		disableVanillaWitherBossBar(wither);
		createWitherBossBar(witherName);
	}

	private static void disableVanillaWitherBossBar(Wither wither) {
		if(!(wither instanceof CraftWither)) {
			return;
		}

		try {
			Utils.scheduleTask(() -> {
				WitherBoss nmsWither = ((CraftWither) wither).getHandle();
				// Remove all players from vanilla bossbar
				nmsWither.bossEvent.removeAllPlayers(); // Remove player from vanilla bossbar
			}, 1);
		} catch(Exception e) {
			Bukkit.getLogger().warning("Failed to disable vanilla wither bossbar");
		}
	}

	private static void createWitherBossBar(String witherName) {
		if(activeWither == null) {
			return;
		}

		double maxHealth = activeWither.getAttribute(Attribute.MAX_HEALTH).getValue();
		boolean exempt = activeWither.getScoreboardTags().contains("TASWitherKing");
		String healthStr = exempt ? String.valueOf((int) maxHealth) : Utils.formatHealthM(activeWither);

		String title = ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " + ChatColor.RED + ChatColor.BOLD + witherName + ChatColor.RESET + ChatColor.GOLD + ChatColor.BOLD + " ﴿ " + ChatColor.YELLOW + healthStr + ChatColor.RED + "❤";

		activeWitherBossBar = Bukkit.createBossBar(title, BarColor.PURPLE, BarStyle.SOLID);
		activeWitherBossBar.setProgress(1.0);

		// Add all current online players
		for(Player player : Bukkit.getOnlinePlayers()) {
			activeWitherBossBar.addPlayer(player);
		}

		// Start the update task
		startBossBarUpdateTask(witherName);
	}

	private static void startBossBarUpdateTask(String witherName) {
		bossBarUpdateTask = new BukkitRunnable() {
			@Override
			public void run() {
				if(activeWither == null || activeWither.isDead()) {
					cleanupActiveBossBar();
					cancel();
					return;
				}

				updateWitherBossBar(witherName);
				disableVanillaWitherBossBar(activeWither);
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	private static void updateWitherBossBar(String witherName) {
		if(activeWitherBossBar == null || activeWither == null) {
			return;
		}

		double currentHealth = activeWither.getHealth();
		double maxHealth = activeWither.getAttribute(Attribute.MAX_HEALTH).getValue();
		boolean exempt = activeWither.getScoreboardTags().contains("TASWitherKing");
		String healthStr = exempt ? String.valueOf((int) Math.floor(currentHealth)) : Utils.formatHealthM(activeWither);

		// Update title with current health
		String title = ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " + ChatColor.RED + ChatColor.BOLD + witherName + ChatColor.GOLD + ChatColor.BOLD + " ﴿ " + ChatColor.YELLOW + healthStr + ChatColor.RED + "❤";

		activeWitherBossBar.setTitle(title);

		// Update progress bar
		double progress = Math.max(0.0, Math.min(1.0, currentHealth / maxHealth));
		activeWitherBossBar.setProgress(progress);

		// Ensure all online players can see it
		for(Player player : Bukkit.getOnlinePlayers()) {
			if(!activeWitherBossBar.getPlayers().contains(player)) {
				activeWitherBossBar.addPlayer(player);
			}
		}
	}

	// Clean up method
	private static void cleanupActiveBossBar() {
		if(activeWitherBossBar != null) {
			activeWitherBossBar.removeAll();
			activeWitherBossBar = null;
		}
		if(bossBarUpdateTask != null) {
			bossBarUpdateTask.cancel();
			bossBarUpdateTask = null;
		}
		activeWither = null;
	}

	public static void forceCleanup() {
		// Remove the wither from the world first
		if(activeWither != null && !activeWither.isDead()) {
			activeWither.remove();
		}

		// Then clean up the boss bar
		cleanupActiveBossBar();
	}

	public static BossBar getActiveBossBar() {
		return activeWitherBossBar;
	}

	public static Wither getActiveWither() {
		return activeWither;
	}

	public static void spawnAnimatedStunnedIndicator(Wither wither, int duration) {
		removeStunIndicator();

		Location loc = wither.getLocation().add(0, wither.getHeight() + 0.5, 0);
		TextDisplay indicator = wither.getWorld().spawn(loc, TextDisplay.class);
		activeStunIndicator = indicator;

		indicator.setBillboard(Display.Billboard.CENTER);
		indicator.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
		indicator.setSeeThrough(true);
		indicator.setShadowed(true);

		// Follow-the-wither + color rotation animation. Runs every tick so the indicator tracks a MOVING boss
		// (e.g. Necron mid-chase); colors still rotate only every 5 ticks.
		new BukkitRunnable() {
			int colorOffset = 0;
			int tickCount = 0;

			@Override
			public void run() {
				if(!wither.isValid() || !indicator.isValid()) {
					indicator.remove();
					cancel();
					return;
				}

				// Keep the indicator above the boss's head wherever he moves.
				indicator.teleport(wither.getLocation().add(0, wither.getHeight() + 0.5, 0));

				// Rotate the colors every 5 ticks.
				if(tickCount++ % 5 == 0) {
					ChatColor[] colors = {ChatColor.RED, ChatColor.YELLOW, ChatColor.BLUE};
					StringBuilder text = new StringBuilder();
					for(int i = 0; i < 3; i++) {
						int colorIndex = (i + colorOffset) % 3;
						text.append(colors[colorIndex]).append(ChatColor.BOLD).append("?");
					}
					indicator.setText(text.toString().trim());
					colorOffset = (colorOffset + 1) % 3;
				}
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);

		Utils.scheduleTask(() -> {
			if(activeStunIndicator == indicator) activeStunIndicator = null;
			indicator.remove();
		}, duration);

	}

	public static void removeStunIndicator() {
		if(activeStunIndicator != null) {
			if(activeStunIndicator.isValid()) activeStunIndicator.remove();
			activeStunIndicator = null;
		}
	}
}