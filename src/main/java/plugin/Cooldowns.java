package plugin;

import net.minecraft.server.MinecraftServer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ability cooldowns as tick stamps: each {@code (player, key)} holds the tick it's next usable, so time left is
 * always known and there's no scheduler task to cancel.
 * <p>
 * Replaced fourteen {@code xxxReady} maps in {@code CustomItems}. An {@code items.AbilityItem} declares
 * {@code cooldownTicks()} and the dispatcher does the rest, so a new ability is a one-file change.
 * <p>
 * Absolute server tick ({@link MinecraftServer#currentTick}, same as {@code death/CheatDeath}), not the run clock, so
 * a cooldown survives a phase change and can't jump on a re-anchor.
 * <p>
 * Twin of {@code SkyBlock in Vanilla}'s {@code misc/Cooldowns}; independent copies on purpose, the plugins share no code.
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

	/** Not on cooldown. */
	public static boolean ready(Player p, String key) {
		return remaining(p, key) <= 0;
	}

	/** Cooldown for {@code ticks} from now. A blank key or {@code ticks <= 0} is a no-op. */
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

	/** Every cooldown for everyone (was {@code CustomItems.resetAbilityCooldowns}). Called at {@code WitherLord.start} and run start. */
	public static void clearAll() {
		NEXT_USABLE.clear();
	}
}
