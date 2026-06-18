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
import instructions.bosses.goldor.Goldor;
import listeners.*;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class M7tas extends JavaPlugin {
	private static Plugin plugin;

	@Override
	public void onEnable() {
		plugin = this;

		// Start the boss-priority heartbeat FIRST so its scheduler task id is the lowest of anything created at
		// runtime — every registered boss ticker then runs each tick before any per-run choreography (see BossScheduler).
		BossScheduler.start();

		// Suppress Paper's deprecated-event registration warnings (EntityKnockbackEvent / EntityKnockbackByEntityEvent).
		// Must be set before registerEvents() below, since the warning is logged there.
		getLogger().setFilter(record -> {
			String msg = record.getMessage();
			return msg == null || !msg.contains("but the event is Deprecated");
		});

		PlayerCollision.setupNoCollisionTeam();

		for(String cmd : List.of("setup", "spectate", "unspectate", "tas", "practice", "eq", "simulate", "reset", "getcustomitems", "verbose", "setspeed", "kickallfakes")) {
			PluginCommand command = getCommand(cmd);
			switch(cmd) {
				case "setup" -> command.setExecutor(new Setup());
				case "spectate", "unspectate" -> command.setExecutor(new Spectate());
				case "tas" -> command.setExecutor(new TAS());
				case "practice" -> command.setExecutor(new Practice());
				case "eq" -> command.setExecutor(new Eq());
				case "simulate" -> command.setExecutor(new Simulate());
				case "reset" -> command.setExecutor(new Reset());
				case "getcustomitems" -> command.setExecutor(new GetCustomItems());
				case "verbose" -> command.setExecutor(new Verbose());
				case "setspeed" -> command.setExecutor(new SetSpeed());
				case "kickallfakes" -> command.setExecutor(new KickAllFakes());
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
		getServer().getPluginManager().registerEvents(new StormCrushExplosion(), this);
		getServer().getPluginManager().registerEvents(new GoldorListener(), this);
		getServer().getPluginManager().registerEvents(new WitherKingListener(), this);
		getServer().getPluginManager().registerEvents(new SpiritLeapListener(), this);
		getServer().getPluginManager().registerEvents(new Eq(), this);
		getServer().getPluginManager().registerEvents(new LinkedSlots(), this);

		PlayerInventoryBackup.startInventorySync();
		HelmetSpeedSync.start();
		// Terminator firing cooldown poller (5-tick, or 4 with Thermodynamic) — runs every tick.
		getServer().getScheduler().runTaskTimer(this, listeners.CustomItems::pollTerminators, 1L, 1L);
		// Practice-only boss-movement driver: in practice the fake ticker gates its own runMovementTickers call off
		// (and may not be running at all, since fakes are kicked), so drive the lane here. In a TAS this is a no-op
		// (practiceMode is false → the fake ticker drives it), so the TAS tick ordering is untouched.
		getServer().getScheduler().runTaskTimer(this,
				() -> { if(instructions.bosses.WitherActions.isPracticeMode()) BossScheduler.runMovementTickers(); }, 1L, 1L);
		Spectate.startSpectatorSync();
		SpringBoots.start();
		LavaJump.start();
	}

	@Override
	public void onDisable() {
		PlayerInventoryBackup.stopInventorySync();
		FakePlayerManager.stopCustomConnection();
		Spectate.stopSpectatorSync();
		SpringBoots.stop();
		LavaJump.stop();
		BossScheduler.stop();

		CustomItems.flushStonkRestorations();

		Goldor.INSTANCE.shutdownRegenerateGates();

		PlayerCollision.cleanup();

		Utils.runCommand("tag @e[type=wither] remove TASWither");
		Utils.runCommand("kill @e[type=!item_frame,type=!player,type=!villager]");

		PlayerInventoryBackup.clearAll();
	}

	public static Plugin getInstance() {
		return plugin;
	}
}
