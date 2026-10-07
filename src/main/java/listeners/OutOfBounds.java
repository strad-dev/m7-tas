package listeners;

import instructions.Server;
import instructions.bosses.Watcher;
import instructions.bosses.WitherActions;
import instructions.clear.Rooms;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import plugin.FakePlayerManager;
import plugin.M7tas;
import plugin.Utils;

/**
 * Kills anyone who leaves the dungeon during a live run. In bounds is exactly:
 * <ul>
 *   <li>the boss arena, {@link LavaJump#isInBossArena} (all five phases);</li>
 *   <li>a clear room's volume, {@link Rooms#inRoomBounds} (its cells, floor..ceiling);</li>
 *   <li>a door, {@link Rooms#inDoor}, frame included.</li>
 * </ul>
 * Everything else is out, mainly the 1-block crevices between rooms: they run the length of the grid and let a
 * player walk past a closed door. A door is the only legal way through one.
 * <p>
 * Gated on {@link Server#isRunStarted()}, same as {@code CustomItems.onBlockBreak}'s door/ceiling lock, so getting
 * into position before the countdown ends is allowed.
 * <p>
 * Only SURVIVAL and ADVENTURE die. Creative is exempt (map editing, same bypass as {@code CustomItems.onBlockBreak};
 * the practice scoreboard flags a mid-run switch), and so are spectators, by game mode and by
 * {@code Spectate.isSpectating}, which leaves them in adventure.
 * <p>
 * A run death in every mode ({@code Deaths.forceKill}), with the ghost moved to {@link #respawnPoint} and revived
 * there, or it would revive out of bounds and die again.
 */
public final class OutOfBounds {
	private static BukkitTask poller;

	public static void start() {
		if(poller != null) return;
		poller = new BukkitRunnable() {
			@Override
			public void run() {
				tick();
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	public static void stop() {
		if(poller != null) {
			poller.cancel();
			poller = null;
		}
	}

	public static boolean isInBounds(Location loc) {
		if(LavaJump.isInBossArena(loc)) return true;
		if(Rooms.inRoomBounds(loc)) return true;
		return Rooms.inDoor(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
	}

	private static void tick() {
		if(!Server.isRunStarted()) return;
		for(Player p : Bukkit.getOnlinePlayers()) {
			// Only survival or adventure. Creative gets the same bypass as CustomItems.onBlockBreak. An allowlist, not
			// "not creative", so no new game mode gets this without someone saying so.
			GameMode gm = p.getGameMode();
			if(gm != GameMode.SURVIVAL && gm != GameMode.ADVENTURE) continue;
			// The plugin's own spectate state leaves the player in ADVENTURE.
			if(Utils.isSpectator(p)) continue;
			if(FakePlayerManager.getFakePlayers().containsValue(p)) continue;
			if(p.isDead() || p.getHealth() <= 0.0) continue;
			if(isInBounds(p.getLocation())) continue;
			kill(p);
		}
	}

	private static void kill(Player p) {
		Location loc = p.getLocation();
		Utils.debug(Utils.DebugType.SERVER, p.getName() + " out of bounds at "
				+ Utils.round(loc.getX(), 2) + " " + Utils.round(loc.getY(), 2) + " " + Utils.round(loc.getZ(), 2));
		death.Deaths.forceKill(p, death.Deaths.mob("The World Border"), respawnPoint(p.getWorld()));
	}

	/** Dungeon entrance while the clear is on, the boss spawn after the portal or in a boss-only section. */
	private static Location respawnPoint(World world) {
		String section = WitherActions.runSection();
		boolean clear = (section.equals("clear") || section.equals("all")) && WitherActions.getSplitEnd("Clear") == null;
		return clear ? JoinListener.dungeonEntrance() : Watcher.bossSpawn(world);
	}
}
