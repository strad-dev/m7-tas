package listeners;

import instructions.Server;
import instructions.clear.Rooms;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import plugin.FakePlayerManager;
import plugin.M7tas;
import plugin.Utils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
 * into position before the countdown ends is allowed. The flag stays true after a run until the next {@code /setup}
 * or run start; harmless, since {@code TAS.endPractice} makes everyone a spectator.
 * <p>
 * Only SURVIVAL and ADVENTURE die. Creative is exempt (map editing, same bypass as {@code CustomItems.onBlockBreak};
 * the practice scoreboard flags a mid-run switch), and so are spectators, by game mode and by
 * {@code Spectate.isSpectating}, which leaves them in adventure.
 * <p>
 * The kill doesn't end the session: they respawn at the entrance ({@code JoinListener.onRespawn}) with their kit.
 * The death screen says what they did wrong and chat says what happened, hence {@code deathScreenMessageOverride}
 * next to {@code deathMessage}.
 */
public final class OutOfBounds implements Listener {
	/** What the dying player sees on their own death screen. */
	private static final Component DEATH_SCREEN = Utils.msg("<red>bad action(s) detected!");

	/** Players this killed, awaiting their {@link PlayerDeathEvent} to restyle it. Anyone else keeps vanilla's
	 *  message and drops. */
	private static final Set<UUID> killed = new HashSet<>();

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
		killed.clear();
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
		killed.add(p.getUniqueId());
		// Players are invulnerable here, so write the health and vanilla runs the death.
		p.setHealth(0.0);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onDeath(PlayerDeathEvent e) {
		Player p = e.getPlayer();
		if(!killed.remove(p.getUniqueId())) return;
		e.deathScreenMessageOverride(DEATH_SCREEN);
		e.deathMessage(Utils.msg("<gold><name><red> went out of bounds!",
				Placeholder.unparsed("name", Utils.getRealName(p))));
		// The run continues, so the kit must survive the death.
		e.setKeepInventory(true);
		e.getDrops().clear();
		e.setKeepLevel(true);
		e.setShouldDropExperience(false);
	}
}
