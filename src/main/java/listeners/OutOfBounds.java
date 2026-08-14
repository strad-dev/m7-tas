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
 * Kills anyone who leaves the dungeon during a live run.  In bounds is exactly three things:
 * <ul>
 *   <li>the boss arena, {@link LavaJump#isInBossArena} - one box covering all five phases;</li>
 *   <li>a clear room's volume, {@link Rooms#inRoomBounds} - its cells and its own floor..ceiling range;</li>
 *   <li>a door, {@link Rooms#inDoor} - the frame included, so the whole archway is walkable.</li>
 * </ul>
 * Everything else is out: above a room's ceiling, below its floor, and the 1-block crevices between rooms, which
 * are the ones worth caring about because they run the length of the grid and a player in one can walk past a
 * closed door.  A door is the only legal way through a crevice.
 *
 * <p><b>Gated on {@link Server#isRunStarted()}</b>, so the pre-run window is deliberately unpoliced: getting into
 * position out of bounds before the countdown ends is allowed, and the moment the run goes live it isn't.  Same
 * gate {@code CustomItems.onBlockBreak} uses to lock the doors and ceilings, and the same reasoning.  Note the flag
 * stays true after a run finishes until the next {@code /setup} or run start; {@code TAS.endPractice} puts everyone
 * in spectator, and spectators are exempt, so that costs nothing in practice.
 *
 * <p><b>Only SURVIVAL and ADVENTURE players are killed.</b>  Creative is exempt - the map-editing mode, the same
 * bypass {@code CustomItems.onBlockBreak} grants above the door/ceiling lock - and so are spectators, both by game
 * mode and by {@code Spectate.isSpectating}, which leaves the player in adventure.  Creative is not a hole in the
 * anti-cheat either: the practice scoreboard already flags anyone who switched into it mid-run.
 *
 * <p>The kill does NOT end the session: the player respawns at the dungeon entrance (see
 * {@code JoinListener.onRespawn}) with their kit intact and carries on.  Two different messages come out of it - the
 * death screen says what they did wrong, chat says what happened - which is why {@code deathScreenMessageOverride}
 * exists alongside {@code deathMessage}.
 */
public final class OutOfBounds implements Listener {
	/** What the dying player sees on their own death screen. */
	private static final Component DEATH_SCREEN = Utils.msg("<red>bad action(s) detected!");

	/** Players we killed ourselves, awaiting their {@link PlayerDeathEvent} so we can restyle it.  Anyone not in
	 *  here died some other way and keeps vanilla's message and drops. */
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
			// ONLY someone actually playing the run: survival or adventure.  Creative is the map-editing mode and gets
			// the same bypass the block-break protections give it (CustomItems.onBlockBreak's creative branch sits above
			// the door/ceiling lock for the same reason), and a spectator is not playing.  An allowlist rather than
			// "not creative": there is no game mode this should newly apply to without someone saying so.
			GameMode gm = p.getGameMode();
			if(gm != GameMode.SURVIVAL && gm != GameMode.ADVENTURE) continue;
			// Separate from the game mode: the plugin's own spectate state leaves the player in ADVENTURE.
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
		// Players are invulnerable in this model, so there is no damage to deal: write the health and vanilla runs the
		// death from there.
		p.setHealth(0.0);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onDeath(PlayerDeathEvent e) {
		Player p = e.getPlayer();
		if(!killed.remove(p.getUniqueId())) return;
		e.deathScreenMessageOverride(DEATH_SCREEN);
		e.deathMessage(Utils.msg("<gold><name><red> went out of bounds!",
				Placeholder.unparsed("name", Utils.getRealName(p))));
		// The run continues, so the kit has to survive the death: a practicer whose loadout hit the floor could not
		// carry on, which is the whole point of respawning them rather than ending the session.
		e.setKeepInventory(true);
		e.getDrops().clear();
		e.setKeepLevel(true);
		e.setShouldDropExperience(false);
	}
}
