/*
 * MIT License
 *
 * Copyright ©2025 Stradivarius Violin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package plugin;

import commands.*;
import listeners.*;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public final class M7tas extends JavaPlugin {
	private static Plugin plugin;
	private static final Map<Player, PlayerInventoryBackup> originalInventories = new HashMap<>();
	private static Team noCollisionTeam;

	public static void addPlayerInventoryBackup(Player player) {
		originalInventories.put(player, new PlayerInventoryBackup(player));
	}

	public static PlayerInventoryBackup removePlayerInventoryBackup(Player player) {
		return originalInventories.remove(player);
	}

	public static class PlayerInventoryBackup {
		private final ItemStack[] contents;
		private final ItemStack[] armorContents;
		private final ItemStack offHand;
		private final int heldItemSlot;

		public PlayerInventoryBackup(Player player) {
			PlayerInventory inv = player.getInventory();
			this.contents = inv.getContents().clone();
			this.armorContents = inv.getArmorContents().clone();
			this.offHand = inv.getItemInOffHand().clone();
			this.heldItemSlot = inv.getHeldItemSlot();
		}

		public void restore(Player player) {
			PlayerInventory inv = player.getInventory();
			inv.setContents(contents);
			inv.setArmorContents(armorContents);
			inv.setItemInOffHand(offHand);
			inv.setHeldItemSlot(heldItemSlot);
			player.updateInventory();
		}
	}

	@Override
	public void onEnable() {
		plugin = this;

		setupNoCollisionTeam();

		for(String cmd : List.of("setup", "spectate", "unspectate", "tas", "simulate", "reset", "getcustomitems", "verbose")) {
			PluginCommand command = getCommand(cmd);
			switch(cmd) {
				case "setup" -> command.setExecutor(new Setup());
				case "spectate", "unspectate" -> command.setExecutor(new Spectate());
				case "tas" -> command.setExecutor(new TAS());
				case "simulate" -> command.setExecutor(new Simulate());
				case "reset" -> command.setExecutor(new Reset());
				case "getcustomitems" -> command.setExecutor(new GetCustomItems());
				case "verbose" -> command.setExecutor(new Verbose());
			}
			command.setTabCompleter(new TabCompletor());
		}
		getServer().getPluginManager().registerEvents(new JoinListener(), this);
		getServer().getPluginManager().registerEvents(new SpectatorListener(), this);
		getServer().getPluginManager().registerEvents(new WithersNotImmuneToArrows(), this);
		getServer().getPluginManager().registerEvents(new PearlHelper(), this);
		getServer().getPluginManager().registerEvents(new MiscListener(), this);
		getServer().getPluginManager().registerEvents(new CustomItems(), this);
		getServer().getPluginManager().registerEvents(new AllMobsHaveNames(), this);

		Utils.startInventorySync();
		Spectate.startSpectatorSync();
	}

	@Override
	public void onDisable() {
		Utils.stopInventorySync();
		TAS.stopCustomConnection();
		Spectate.stopSpectatorSync();

		if(noCollisionTeam != null) {
			for(String entry : new HashSet<>(noCollisionTeam.getEntries())) {
				noCollisionTeam.removeEntry(entry);
			}
			noCollisionTeam.unregister();
		}

		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tag @e[type=wither] remove TASWither");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kill @e[type=!item_frame,type=!player,type=!villager]");

		originalInventories.clear();
	}

	private static void setupNoCollisionTeam() {
		ScoreboardManager manager = Bukkit.getScoreboardManager();
		if(manager != null) {
			Scoreboard scoreboard = manager.getMainScoreboard();

			// Remove existing team if it exists
			Team existingTeam = scoreboard.getTeam("nocollision");
			if(existingTeam != null) {
				existingTeam.unregister();
			}

			// Create new team with no collision
			noCollisionTeam = scoreboard.registerNewTeam("nocollision");
			noCollisionTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
			noCollisionTeam.setCanSeeFriendlyInvisibles(false);
		}
	}

	public static void addToNoCollisionTeam(Player player) {
		if(noCollisionTeam != null) {
			noCollisionTeam.addEntry(player.getName());
		}
	}

	public static void removeFromNoCollisionTeam(Player player) {
		if(noCollisionTeam != null) {
			noCollisionTeam.removeEntry(player.getName());
		}
	}

	public static void addEntityToNoCollisionTeam(Entity entity) {
		if(noCollisionTeam != null) {
			noCollisionTeam.addEntry(entity.getUniqueId().toString());
		}
	}

	public static void removeEntityFromNoCollisionTeam(Entity entity) {
		if(noCollisionTeam != null) {
			noCollisionTeam.removeEntry(entity.getUniqueId().toString());
		}
	}

	public static void preventPlayerCollision(Player realPlayer, Player fakePlayer) {
		// Add both players to the no-collision team
		addToNoCollisionTeam(realPlayer);
		addToNoCollisionTeam(fakePlayer);

		// Also make the real player unable to be hit by projectiles while spectating
		realPlayer.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 0, true, false));
	}

	public static Plugin getInstance() {
		return plugin;
	}
}