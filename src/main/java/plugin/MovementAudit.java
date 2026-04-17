package plugin;

import instructions.Actions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MovementAudit {

	private static final Map<UUID, BukkitTask> airborneAudits = new HashMap<>();

	public static boolean hasAirborneAudit(UUID id) {
		return airborneAudits.containsKey(id);
	}

	public static void startAirborneAudit(Player p, String source) {
		if(!Utils.isSuperVerbose()) return;

		UUID id = p.getUniqueId();
		BukkitTask existing = airborneAudits.remove(id);
		if(existing != null) existing.cancel();

		ServerPlayer npc = ((CraftPlayer) p).getHandle();

		BukkitTask task = new BukkitRunnable() {
			int tick = 0;

			@Override
			public void run() {
				if(!p.isValid() || npc.isRemoved()) {
					airborneAudits.remove(id);
					cancel();
					return;
				}

				Vec3 pos = npc.position();
				String input = Actions.getActiveInput(id);
				String sprintSneak = "";
				if(!input.isEmpty()) {
					sprintSneak = npc.isShiftKeyDown() ? " sprinting" : npc.isShiftKeyDown() ? " sneaking" : "";
				}

				if(npc.onGround() && tick > 0) {
					Utils.debug(Utils.DebugType.CLIENT, p.getName() + " " + source + " landed after " + tick + " ticks");
					airborneAudits.remove(id);
					cancel();
					return;
				}

				Utils.debug(Utils.DebugType.CLIENT, p.getName() + " " + source + " moved " + Utils.round(pos.x, 4) + " " + Utils.round(pos.y, 4) + " " + Utils.round(pos.z, 4) + sprintSneak);
				tick++;
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L); airborneAudits.put(id, task);
	}

	public static void auditMove(Player p, ServerPlayer npc) {
		if(!Utils.isSuperVerbose()) return;

		UUID id = p.getUniqueId();
		String input = Actions.getActiveInput(id);

		if(hasAirborneAudit(id)) {
			// Airborne audit handles xyz; only log sprint/sneak changes if move() is active
			return;
		}

		if(input.isEmpty()) return;

		Vec3 pos = npc.position();
		Utils.debug(Utils.DebugType.CLIENT, p.getName() + " moved " + Utils.round(pos.x, 4) + " " + Utils.round(pos.y, 4) + " " + Utils.round(pos.z, 4) + (npc.isShiftKeyDown() ? " sprinting" : npc.isShiftKeyDown() ? " sneaking" : "") + (npc.onGround() ? " on ground" : ""));
	}

	public static void cancelAll() {
		for(BukkitTask task : airborneAudits.values()) {
			task.cancel();
		}
		airborneAudits.clear();
	}
}
