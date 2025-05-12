package instructions;

import instructions.bosses.Watcher;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import plugin.M7tas;
import plugin.Utils;

public class Server {
	public static void serverInstructions(World world) {
		// Begin with 3 seconds of delay
		Bukkit.broadcastMessage("TAS starts in 3 seconds.");

		// 5-second countdown
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
			Bukkit.broadcastMessage(ChatColor.GREEN + "Starting in 5 seconds.");
			Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
		}, 60);
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
			Bukkit.broadcastMessage(ChatColor.GREEN + "Starting in 4 seconds.");
			Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
		}, 80);
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
			Bukkit.broadcastMessage(ChatColor.GREEN + "Starting in 3 seconds.");
			Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
		}, 100);
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
			Bukkit.broadcastMessage(ChatColor.GREEN + "Starting in 2 seconds.");
			Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
		}, 120);
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
			Bukkit.broadcastMessage(ChatColor.GREEN + "Starting in 1 seconds.");
			Utils.playGlobalSound(Sound.BLOCK_LEVER_CLICK, 2.0F, 1.0F);
		}, 140);
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> Utils.playGlobalSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0F, 1.0F), 140);
		Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> Watcher.watcherInstructions(world), 161);

//		Bukkit.broadcastMessage(ChatColor.RED + "The " + ChatColor.BOLD + "BLOOD DOOR" + ChatColor.RESET + ChatColor.RED + " has been opened!");
//		Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "A shiver runs down your spine...");
//		Utils.playGlobalSound(Sound.ENTITY_GHAST_HURT, 2.0F, 0.5F);
	}
}