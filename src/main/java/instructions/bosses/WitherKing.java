package instructions.bosses;

import instructions.Actions;
import org.bukkit.*;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import plugin.M7tas;
import plugin.Utils;

import java.util.Random;

@SuppressWarnings("unused")
public class WitherKing {
	private static World world;
	private static Wither witherKing;
	private static BossBar witherKingBossBar;
	private static BukkitTask bossBarUpdateTask;
	private static final Random random = new Random();
	private static final String[] dragonDieMessage = {"Oh, this one hurts!", "I have more of those.", "My sould is disposable."};

	public static void witherKingInstructions(World temp) {
		world = temp;
	}

	public static void pickUpRelic(Player player) {
		ItemStack itemStack;
		String name;
		switch(player.getName()) {
			case "Archer" -> {
				itemStack = new ItemStack(Material.RED_WOOL);
				ItemMeta meta = itemStack.getItemMeta();
				assert meta != null;
				meta.setDisplayName(ChatColor.RED + "Corrupted Red Relic");
				itemStack.setItemMeta(meta);
				name = "akc0303";
			}
			case "Berserk" -> {
				itemStack = new ItemStack(Material.ORANGE_WOOL);
				ItemMeta meta = itemStack.getItemMeta();
				assert meta != null;
				meta.setDisplayName(ChatColor.GOLD + "Corrupted Orange Relic");
				itemStack.setItemMeta(meta);
				name = "AsapIcey";
			}
			case "Healer" -> {
				itemStack = new ItemStack(Material.PURPLE_WOOL);
				ItemMeta meta = itemStack.getItemMeta();
				assert meta != null;
				meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Corrupted Purple Relic");
				itemStack.setItemMeta(meta);
				name = "Meepy_";
			}
			case "Mage" -> {
				itemStack = new ItemStack(Material.BLUE_WOOL);
				ItemMeta meta = itemStack.getItemMeta();
				assert meta != null;
				meta.setDisplayName(ChatColor.BLUE + "Corrupted Blue Relic");
				itemStack.setItemMeta(meta);
				name = "Beethoven_";
			}
			case "Tank" -> {
				itemStack = new ItemStack(Material.GREEN_WOOL);
				ItemMeta meta = itemStack.getItemMeta();
				assert meta != null;
				meta.setDisplayName(ChatColor.GREEN + "Corrupted Green Relic");
				itemStack.setItemMeta(meta);
				name = "cookiethebald";
			}
			default -> {
				itemStack = new ItemStack(Material.BLACK_WOOL);
				ItemMeta meta = itemStack.getItemMeta();
				assert meta != null;
				meta.setDisplayName(ChatColor.BLACK + "Corrupted Unknown Relic");
				itemStack.setItemMeta(meta);
				name = "Unknown";
			}
		}
		Bukkit.broadcastMessage(ChatColor.GOLD + name + ChatColor.GREEN + " picked up the " + itemStack.getItemMeta().getDisplayName() + ChatColor.GREEN + "!");
		player.getInventory().setItem(8, itemStack);
		Actions.setFakePlayerHotbarSlot(player, 8);
		Utils.playGlobalSound(Sound.ENTITY_ENDERMAN_SCREAM, 2.0f, 0.5f);
	}

	public static void placeRelic(Player player) {
		player.getInventory().setItem(8, M7tas.getSkyBlockItem(Material.NETHER_STAR, ChatColor.GREEN + "SkyBlock Menu (Click)", ""));
	}
}