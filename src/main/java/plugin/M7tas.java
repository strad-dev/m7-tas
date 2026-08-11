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
import instructions.Server;
import instructions.bosses.goldor.Goldor;
import listeners.*;
import loadout.ClassCommand;
import loadout.LoadoutEditor;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

public final class M7tas extends JavaPlugin {
	private static Plugin plugin;

	/**
	 * Ceiling we raise {@code minecraft:max_health} to.  Real SkyBlock HP is divided by
	 * {@code damage.Scale.SB_PER_MC_HP} before it reaches an entity, so the largest value the plugin ever sets is
	 * Necron's 1400 - this is a guardrail with four orders of magnitude of headroom, not a target.
	 */
	private static final double MAX_HEALTH_CEILING = 1_000_000.0;

	/**
	 * Raise the {@code minecraft:max_health} attribute's own ceiling before any world or entity loads.
	 * <p>
	 * Paper's shipped default is 1024, and {@code spigot.yml}'s {@code max-health} key is the only supported way to
	 * change it - which means the boss HP in {@code damage/MobStats} would silently clamp on any server whose config
	 * has not been hand-edited (a fresh install, a wiped config, a new box).  {@code RangedAttribute.maxValue} is
	 * public and non-final on Paper (that is exactly how Spigot applies the config key), so the plugin sets it
	 * itself and stops depending on per-server config at all.
	 * <p>
	 * This has to be {@code onLoad}: {@code AttributeInstance} caches its computed value and clamps a base value at
	 * set time, so anything that loads with a lower ceiling stays clamped.  Entities already persisted in a world
	 * under the old cap reload clamped too - the bosses set their HP in {@code onSpawn}, so only a boss currently
	 * standing in the world is affected, and it needs one re-spawn.
	 */
	@Override
	public void onLoad() {
		net.minecraft.world.entity.ai.attributes.Attribute maxHealth =
				net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH.value();
		if(maxHealth instanceof net.minecraft.world.entity.ai.attributes.RangedAttribute ranged) {
			if(ranged.maxValue < MAX_HEALTH_CEILING) ranged.maxValue = MAX_HEALTH_CEILING;
		} else {
			getLogger().warning("minecraft:max_health is not a RangedAttribute; boss HP may clamp.");
		}
	}

	@Override
	public void onEnable() {
		plugin = this;

		// Start the boss-priority heartbeat FIRST so its scheduler task id is the lowest of anything created at
		// runtime, so every registered boss ticker runs each tick before any per-run choreography (see BossScheduler).
		BossScheduler.start();

		PlayerCollision.setupNoCollisionTeam();

		// TAS-only commands (tas, simulate, spectate/unspectate, reset, kickallfakes) are disabled in the practice fork.
		LoadoutEditor loadoutEditor = new LoadoutEditor();
		for(String cmd : List.of("setup", "m7practice", "eq", "reset", "verbose", "setspeed",
				"class", "m7loadout", "toggledungeondifficulty")) {
			PluginCommand command = getCommand(cmd);
			switch(cmd) {
				case "setup" -> command.setExecutor(new Setup());
				case "m7practice" -> command.setExecutor(new Practice());
				case "reset" -> command.setExecutor(new Reset());
				case "eq" -> command.setExecutor(new Eq());
				case "verbose" -> command.setExecutor(new Verbose());
				case "setspeed" -> command.setExecutor(new SetSpeed());
				case "class" -> command.setExecutor(new ClassCommand());
				case "m7loadout" -> command.setExecutor(loadoutEditor);
				case "toggledungeondifficulty" -> command.setExecutor(new ToggleDungeonDifficulty());
			}
			command.setTabCompleter(new TabCompletor());
		}
		getServer().getPluginManager().registerEvents(new JoinListener(), this);
		// getServer().getPluginManager().registerEvents(new SpectatorListener(), this); // TAS-only fake-spectate feature, disabled in the practice fork
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
		getServer().getPluginManager().registerEvents(new listeners.ClearListener(), this);
		getServer().getPluginManager().registerEvents(loadoutEditor, this);
		// Keeps the (player, path) stat cache honest across equipment and inventory changes (DAMAGE_PLAN.md §7).
		getServer().getPluginManager().registerEvents(new damage.StatListener(), this);

		// One repeating driver for every running damage-over-time chain (Fire Aspect, Venomous), rather than one
		// scheduler task per proc.
		damage.Procs.start();

		PlayerInventoryBackup.startInventorySync();
		HelmetSpeedSync.start();
		// Terminator firing cooldown poller (5-tick, or 4 with Thermodynamic), which runs every tick.
		getServer().getScheduler().runTaskTimer(this, listeners.CustomItems::pollTerminators, 1L, 1L);
		// Practice-only boss-movement driver: in practice the fake ticker gates its own runMovementTickers call off
		// (and may not be running at all, since fakes are kicked), so drive the lane here. In a TAS this is a no-op
		// (practiceMode is false → the fake ticker drives it), so the TAS tick ordering is untouched.
		getServer().getScheduler().runTaskTimer(this,
				() -> { if(instructions.bosses.WitherActions.isPracticeMode()) BossScheduler.runMovementTickers(); }, 1L, 1L);
		Spectate.startSpectatorSync();
		SpringBoots.start();
		LavaJump.start();

		// Export the item catalog (palette + per-class default kits) to the shared data folder so the network
		// plugin's lobby loadout editor can load the real M7 items. M7 is the sole writer of this file.
		Catalog.export();

		// /class and /m7loadout exist in BOTH plugins: this one owns them here, the network plugin provides its own
		// copy on every server M7 isn't installed on. A bare label goes to whichever plugin registers it FIRST, and
		// the network plugin softdepends on us so we normally already have it, and this claim is the backstop for when we
		// don't (load order changed, its jar deployed alone first, a reload). It no longer claims these labels on
		// m7, so this is not a tug-of-war; and being enabled earlier, our task id is lower, so we would lose a
		// tug-of-war anyway if one were reintroduced there.
		getServer().getScheduler().runTask(this, () -> {
			forceOwnLabel("class");
			forceOwnLabel("m7loadout");
		});
	}

	/**
	 * Take a bare command label back from another plugin that registered the same name. Bukkit maps a bare label to
	 * exactly one command; the loser keeps only its prefixed form ({@code /stradnetworkplugin:class}). Mirrors the
	 * network plugin's {@code Main.forceOwnLabel}.
	 */
	private void forceOwnLabel(String name) {
		PluginCommand ours = getCommand(name);
		if(ours == null) return;
		if(!(getServer().getCommandMap() instanceof SimpleCommandMap map)) return;
		Map<String, Command> known = map.getKnownCommands();
		if(known.get(name) == ours) return; // already ours, nothing to do
		known.remove(name); // drop the other plugin's bare-label mapping (it keeps its prefixed form)
		known.put(name, ours);
		ours.register(map);
		getLogger().info("Claimed /" + name + " for M7 (was another plugin's).");
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

		// Stop the clear HUD/map loop (hardMobCleanup below removes the secret entities).
		if(!org.bukkit.Bukkit.getWorlds().isEmpty()) instructions.clear.ClearManager.stop(org.bukkit.Bukkit.getWorlds().getFirst());

		Goldor.INSTANCE.shutdownRegenerateGates();

		PlayerCollision.cleanup();

		Server.hardMobCleanup();

		PlayerInventoryBackup.clearAll();
	}

	public static Plugin getInstance() {
		return plugin;
	}
}
