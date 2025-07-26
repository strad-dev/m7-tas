package instructions.bosses;

import instructions.Actions;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.boss.wither.EntityWither;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftWither;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import plugin.M7tas;
import plugin.Utils;

import java.util.Objects;

@SuppressWarnings("DataFlowIssue")
public class Maxor {
	private static Wither maxor;
	@SuppressWarnings("FieldCanBeLocal")
	private static World world;
	private static BossBar maxorBossBar;
	private static BukkitTask bossBarUpdateTask;

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

		maxor = (Wither) world.spawnEntity(new org.bukkit.Location(world, 73.5, 227, 53.5, 0f, 0f), org.bukkit.entity.EntityType.WITHER);
		maxor.setAI(false);
		maxor.setSilent(true);
		maxor.setPersistent(true);
		maxor.setRemoveWhenFarAway(false);
		maxor.setInvulnerable(true);
		maxor.setCustomName(ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " + ChatColor.RED + ChatColor.BOLD + "Maxor" + ChatColor.GOLD + ChatColor.BOLD + " ﴿ " + ChatColor.RED + "❤ " + ChatColor.YELLOW + 400 + "/" + 400);
		maxor.setCustomNameVisible(true);
		Objects.requireNonNull(maxor.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(400);
		maxor.setHealth(400);

		disableVanillaWitherBossBar();
		createMaxorBossBar();

		sendChatMessage("WELL WELL WELL, LOOK WHO'S HERE!");
		Utils.scheduleTask(() -> sendChatMessage("I'VE BEEN TOLD I COULD HAVE A BIT OF FUN WITH YOU."), 60);
		Utils.scheduleTask(() -> sendChatMessage("DON'T DISAPPOINT ME, I HAVEN'T HAD A GOOD FIGHT IN A WHILE."), 120);
		Utils.scheduleTask(() -> {
			Actions.move(maxor, new Vector(0, 0, 0.5), 38);
			Utils.playGlobalSound(Sound.ENTITY_WITHER_SPAWN);
		}, 160);
		Utils.scheduleTask(() -> {
			sendChatMessage("YOU TRICKED ME!");
			maxor.setInvulnerable(false);
		}, 198);
	}

	private static void disableVanillaWitherBossBar() {
		if(!(maxor instanceof CraftWither)) return;

		try {
			EntityWither nmsWither = ((CraftWither) maxor).getHandle();

			// Instead of making it invisible, just remove all players
			// This keeps the internal health tracking working
			for(Player player : Bukkit.getOnlinePlayers()) {
				if(player instanceof CraftPlayer craftPlayer) {
					EntityPlayer nmsPlayer = craftPlayer.getHandle();
					nmsWither.ch.b(nmsPlayer); // Remove player from vanilla bossbar
				}
			}
		} catch (Exception e) {
			Bukkit.getLogger().warning("Failed to disable vanilla wither bossbar");
		}
	}

	private static void createMaxorBossBar() {
		if(maxor == null) return;

		String title = ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " +
				ChatColor.RED + ChatColor.BOLD + "Maxor" +
				ChatColor.GOLD + ChatColor.BOLD + " ﴿ " +
				ChatColor.RED + "❤ " + ChatColor.YELLOW +
				400 + "/" + 400;

		maxorBossBar = Bukkit.createBossBar(title, BarColor.PURPLE, BarStyle.SOLID);
		maxorBossBar.setProgress(1.0);

		// Add all current online players
		for(Player player : Bukkit.getOnlinePlayers()) {
			maxorBossBar.addPlayer(player);
		}

		// Start the update task
		startBossBarUpdateTask();
	}

	private static void startBossBarUpdateTask() {
		bossBarUpdateTask = new BukkitRunnable() {
			@Override
			public void run() {
				if(maxor == null || maxor.isDead()) {
					// Clean up when maxor is gone
					if(maxorBossBar != null) {
						maxorBossBar.removeAll();
						maxorBossBar = null;
					}
					cancel();
					return;
				}

				updateMaxorBossBar();
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	private static void updateMaxorBossBar() {
		if(maxorBossBar == null || maxor == null) return;

		double currentHealth = maxor.getHealth();
		double maxHealth = maxor.getAttribute(Attribute.MAX_HEALTH).getValue();

		// Update title with current health
		String title = ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " +
				ChatColor.RED + ChatColor.BOLD + "Maxor" +
				ChatColor.GOLD + ChatColor.BOLD + " ﴿ " +
				ChatColor.RED + "❤ " + ChatColor.YELLOW +
				(int) Math.ceil(currentHealth) + "/" + (int) maxHealth;

		maxorBossBar.setTitle(title);

		// Update progress bar
		double progress = Math.max(0.0, Math.min(1.0, currentHealth / maxHealth));
		maxorBossBar.setProgress(progress);

		// Ensure all online players can see it
		for(Player player : Bukkit.getOnlinePlayers()) {
			if(!maxorBossBar.getPlayers().contains(player)) {
				maxorBossBar.addPlayer(player);
			}
		}
	}

	private static void sendChatMessage(String message) {
		Bukkit.broadcastMessage(ChatColor.DARK_RED + "[BOSS] MAXOR" + ChatColor.RED + ": " + message);
		Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT);
	}
}
