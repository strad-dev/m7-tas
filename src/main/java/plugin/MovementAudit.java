package plugin;

import instructions.Actions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MovementAudit {

	private static final Map<UUID, BukkitRunnable> airborneAudits = new HashMap<>();

	public static boolean hasAirborneAudit(UUID id) {
		return airborneAudits.containsKey(id);
	}

	public static void startAirborneAudit(Player p, String source) {
		if(!Utils.isVerbose()) return;

		UUID id = p.getUniqueId();
		BukkitRunnable existing = airborneAudits.get(id);
		if(existing != null) existing.cancel();

		ServerPlayer npc = ((CraftPlayer) p).getHandle();

		BukkitRunnable runnable = new BukkitRunnable() {
			int tick = 0;
			Vec3 prev = npc.position();

			@Override
			public void run() {
				if(!p.isValid() || npc.isRemoved()) {
					airborneAudits.remove(id);
					cancel();
					return;
				}

				Vec3 pos = npc.position();
				double dx = pos.x - prev.x;
				double dy = pos.y - prev.y;
				double dz = pos.z - prev.z;
				prev = pos;

				String input = Actions.getActiveInput(id);
				String sprintSneak = "";
				if(!input.isEmpty()) {
					sprintSneak = npc.isSprinting() ? " sprinting" : npc.isShiftKeyDown() ? " sneaking" : "";
				}

				if(npc.onGround() && tick > 0) {
					cancel();
					return;
				}

				if(tick > 0 && Utils.isSuperVerbose()) {
					Utils.debug(Utils.DebugType.CLIENT, p.getName() + " " + source + "ed " + Utils.round(dx, 4) + " " + Utils.round(dy, 4) + " " + Utils.round(dz, 4) + sprintSneak);
				}
				tick++;
			}

			@Override
			public void cancel() {
				net.minecraft.world.phys.Vec3 landPos = npc.position();
				Utils.debug(Utils.DebugType.CLIENT, p.getName() + " " + source + " landed after " + tick + " ticks at " + Utils.round(landPos.x, 2) + " " + Utils.round(landPos.y, 2) + " " + Utils.round(landPos.z, 2));
				airborneAudits.remove(id);
				super.cancel();
			}
		};
		runnable.runTaskTimer(M7tas.getInstance(), 0L, 1L);
		airborneAudits.put(id, runnable);
	}

	public static void auditMove(Player p, ServerPlayer npc, double dx, double dy, double dz) {
		if(!Utils.isSuperVerbose()) return;

		UUID id = p.getUniqueId();
		String input = Actions.getActiveInput(id);

		if(hasAirborneAudit(id)) {
			return;
		}

		if(input.isEmpty()) return;

		Utils.debug(Utils.DebugType.CLIENT, p.getName() + " moved " + Utils.round(dx, 4) + " " + Utils.round(dy, 4) + " " + Utils.round(dz, 4) + (npc.isSprinting() ? " sprinting" : npc.isShiftKeyDown() ? " sneaking" : "") + (npc.onGround() ? " on ground" : ""));
	}

	public static void cancelAll() {
		for(BukkitRunnable runnable : airborneAudits.values()) {
			runnable.cancel();
		}
		airborneAudits.clear();
	}
}
