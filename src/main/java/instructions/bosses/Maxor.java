package instructions.bosses;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Wither;
import org.bukkit.util.Vector;
import plugin.Utils;

public class Maxor {
	private static Wither maxor;
	private static World world;

	public static void maxorInstructions(World temp, boolean doContinue) {
		world = temp;

		if(maxor != null) {
			maxor.remove();
		}
		maxor = (Wither) world.spawnEntity(new org.bukkit.Location(world, 73.5, 221, 53.5, 0f, 0f), org.bukkit.entity.EntityType.WITHER);
		maxor.setAI(false);
		maxor.setSilent(true);
		maxor.setPersistent(true);
		maxor.setRemoveWhenFarAway(false);
		maxor.setInvulnerable(true);
		maxor.setCustomName(ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "﴾ " + ChatColor.RED + ChatColor.BOLD + "Maxor" + ChatColor.GOLD + ChatColor.BOLD + " ﴿ " + ChatColor.RED + "❤ " + ChatColor.YELLOW + 400 + "/" + 400);
		maxor.setCustomNameVisible(true);

		sendChatMessage("WELL WELL WELL, LOOK WHO'S HERE!");
		Utils.scheduleTask(() -> sendChatMessage("I'VE BEEN TOLD I COULD HAVE A BIT OF FUN WITH YOU."), 60);
		Utils.scheduleTask(() -> sendChatMessage("DON'T DISAPPOINT ME, I HAVEN'T HAD A GOOD FIGHT IN A WHILE."), 120);
		Utils.scheduleTask(() -> maxor.setVelocity(new Vector(0, 0, 0.5)), 160);
		Utils.scheduleTask(() -> {
			maxor.setVelocity(new Vector(0, 0, 0));
			sendChatMessage("YOU TRICKED ME!");
		}, 199);
	}

	private static void sendChatMessage(String message) {
		Bukkit.broadcastMessage(ChatColor.DARK_RED + "[BOSS] MAXOR" + ChatColor.RED + ": " + message);
		Utils.playGlobalSound(Sound.ENTITY_WITHER_AMBIENT);
	}
}
