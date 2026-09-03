package plugin;

import net.minecraft.server.MinecraftServer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tick-timestamp ability cooldowns: each {@code (player, key)} records the server tick that ability is next
 * usable, so the remaining time is always known and no per-use scheduler task exists to be cancelled.
 * <p>
 * This replaces the fourteen hand-rolled {@code Map<UUID, Integer> xxxReady} maps {@code CustomItems} used to
 * carry, one per ability, each with its own copy of the same "is it ready / stamp it / how long left" three
 * lines.  An {@code items.AbilityItem} now just declares {@code cooldownTicks()} and the dispatcher does the
 * rest, which is what makes adding an ability a one-file change.
 * <p>
 * <b>Absolute server tick, not the run clock.</b> {@link MinecraftServer#currentTick} is the same source the old
 * maps used and the same one {@code death/CheatDeath} uses, so a cooldown survives a phase change and cannot
 * jump when a run or phase is re-anchored.
 * <p>
 * {@code SkyBlock in Vanilla}'s {@code misc/Cooldowns} is this class's twin (it was ported there first, from the
 * older M7 shape); the two are independent copies on purpose, since the plugins share no code.
 */
public final class Cooldowns {
	private static final Map<UUID, Map<String, Integer>> NEXT_USABLE = new ConcurrentHashMap<>();

	private Cooldowns() {}

	/** Ticks until {@code key} is usable again for {@code p}; 0 if ready, or if the key is blank. */
	public static int remaining(Player p, String key) {
		if(p == null || key == null || key.isEmpty()) return 0;
		Map<String, Integer> keys = NEXT_USABLE.get(p.getUniqueId());
		if(keys == null) return 0;
		Integer next = keys.get(key);
		return next == null ? 0 : Math.max(0, next - MinecraftServer.currentTick);
	}

	public static boolean onCooldown(Player p, String key) {
		return remaining(p, key) > 0;
	}

	/** True if {@code key} is ready, i.e. not on cooldown.  Reads better than negating at the call site. */
	public static boolean ready(Player p, String key) {
		return remaining(p, key) <= 0;
	}

	/** Put {@code key} on cooldown for {@code ticks} from now.  A blank key or {@code ticks <= 0} is a no-op. */
	public static void start(Player p, String key, int ticks) {
		if(p == null || key == null || key.isEmpty() || ticks <= 0) return;
		NEXT_USABLE.computeIfAbsent(p.getUniqueId(), k -> new ConcurrentHashMap<>())
				.put(key, MinecraftServer.currentTick + ticks);
	}

	public static void clear(Player p, String key) {
		Map<String, Integer> keys = NEXT_USABLE.get(p.getUniqueId());
		if(keys != null) keys.remove(key);
	}

	public static void clear(Player p) {
		NEXT_USABLE.remove(p.getUniqueId());
	}

	/**
	 * Drop every cooldown for everyone.  This is what {@code CustomItems.resetAbilityCooldowns} became: called on
	 * entering a boss fight ({@code WitherLord.start}) and at run start, so a practice always begins with every
	 * ability off cooldown.
	 */
	public static void clearAll() {
		NEXT_USABLE.clear();
	}
}
