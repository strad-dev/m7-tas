package listeners;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import plugin.M7tas;
import plugin.MovementAudit;
import plugin.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpringBoots {
	private static final String ITEM_ID = "skyblock/combat/spring_boots";

	// pitch values for BLOCK_NOTE_BLOCK_PLING (F#3=0 → F#5=24; pitch = 2^((n-12)/12))
	private static final float PITCH_C4    = 0.7087f; // note 6  (existing code)
	private static final float PITCH_EB4   = 0.8428f; // note 9  (existing code)
	private static final float PITCH_E4    = 0.8929f; // note 10 (existing code)
	private static final float PITCH_F4    = 0.9439f; // note 11
	private static final float PITCH_G4    = 1.0595f; // note 13
	private static final float PITCH_A4    = 1.1892f; // note 15
	private static final float PITCH_B4    = 1.3348f; // note 17
	private static final float PITCH_E5    = 1.7818f; // note 22

	// Per-note schedule. Each Note = (tickOffset from charge start, pitch, targetHeight if released NOW).
	// Heights for L1-L5 come from the Hypixel community guide. L6-L8 are linearly
	// interpolated from 47 (end of L5) up to 61 (8th-pitch cap from the wiki).
	private static final int[]    NOTE_TICK;
	private static final float[]  NOTE_PITCH;
	private static final double[] NOTE_HEIGHT;

	static {
		java.util.List<int[]> ticks = new java.util.ArrayList<>();
		java.util.List<Float> pitches = new java.util.ArrayList<>();
		java.util.List<Double> heights = new java.util.ArrayList<>();

		// level descriptor: pitch, count, tickGapInto (from previous note), tickGapWithin
		record Level(float pitch, int count, int gapInto, int gapWithin, double[] h) {}
		// Existing-code cadence for C/Eb/first-E: 2-tick spacing, first note at t=0,
		// then user's spec kicks in from the 2nd low-E onward.
		Level[] levels = new Level[] {
				new Level(PITCH_C4,  2, 0, 2, new double[]{3.0, 6.5}),
				new Level(PITCH_EB4, 6, 2, 2, new double[]{9.0, 11.5, 13.5, 16.0, 18.0, 19.0}),
				new Level(PITCH_E4,  8, 2, 4, new double[]{20.5, 22.5, 25.0, 26.5, 28.0, 29.0, 30.0, 31.0}),
				new Level(PITCH_F4,  7, 6, 6, new double[]{33.0, 34.0, 35.5, 37.0, 38.0, 39.5, 40.0}),
				new Level(PITCH_G4,  7, 6, 8, new double[]{41.0, 42.5, 43.5, 44.0, 45.0, 46.0, 47.0}),
				new Level(PITCH_A4, 10, 7,10, rampFrom(48.0, 55.5, 10)),
				new Level(PITCH_B4,  5,30,30, rampFrom(57.0, 60.0, 5)),
				new Level(PITCH_E5,  1,10,40, new double[]{61.0}),
		};
		// E5 repeats indefinitely every E5_PERIOD ticks past E5_FIRST_TICK (handled in maybePlayNote/launch).

		int t = 0;
		boolean first = true;
		for(Level lvl : levels) {
			for(int i = 0; i < lvl.count; i++) {
				if(first) { t = 0; first = false; }
				else t += (i == 0 ? lvl.gapInto : lvl.gapWithin);
				ticks.add(new int[]{t});
				pitches.add(lvl.pitch);
				heights.add(lvl.h[i]);
			}
		}

		NOTE_TICK = ticks.stream().mapToInt(a -> a[0]).toArray();
		NOTE_PITCH = new float[pitches.size()];
		for(int i = 0; i < pitches.size(); i++) NOTE_PITCH[i] = pitches.get(i);
		NOTE_HEIGHT = heights.stream().mapToDouble(Double::doubleValue).toArray();
	}

	private static final int E5_FIRST_TICK = 397;
	private static final int E5_PERIOD = 40;
	private static final double E5_HEIGHT = 61.0;

	private static double[] rampFrom(double first, double last, int count) {
		double[] out = new double[count];
		if(count == 1) { out[0] = first; return out; }
		for(int i = 0; i < count; i++) out[i] = first + (last - first) * i / (double) (count - 1);
		return out;
	}

	private static double[] filled(double v, int count) {
		double[] out = new double[count];
		java.util.Arrays.fill(out, v);
		return out;
	}

	private static final Map<UUID, ChargeState> states = new HashMap<>();
	private static BukkitTask poller;

	private static class ChargeState {
		boolean sneakPrev;
		boolean onGroundPrev;
		boolean charging;
		int ticks;
		int nextNoteIndex;
	}

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
		states.clear();
	}

	private static void tick() {
		for(Player p : Bukkit.getOnlinePlayers()) {
			ItemStack boots = p.getInventory().getBoots();
			boolean wearing = boots != null && ITEM_ID.equals(CustomItems.getID(boots));
			ChargeState st = states.get(p.getUniqueId());
			if(!wearing) {
				if(st != null) {
					st.charging = false;
					st.sneakPrev = p.isSneaking();
				}
				continue;
			}
			if(st == null) {
				st = new ChargeState();
				st.sneakPrev = p.isSneaking();
				st.onGroundPrev = isOnGround(p);
				states.put(p.getUniqueId(), st);
			}

			boolean sneak = p.isSneaking();
			boolean onGround = isOnGround(p);

			if(st.charging) {
				if(!sneak) {
					launch(p, st.ticks);
					st.charging = false;
				} else if(!onGround) {
					st.charging = false;
				} else {
					st.ticks++;
					maybePlayNote(p, st);
				}
			} else {
				boolean sneakEdge = sneak && !st.sneakPrev;
				boolean landEdge = onGround && !st.onGroundPrev;
				if(sneak && onGround && (sneakEdge || landEdge)) {
					st.charging = true;
					st.ticks = 0;
					st.nextNoteIndex = 0;
					maybePlayNote(p, st);
				}
			}

			st.sneakPrev = sneak;
			st.onGroundPrev = onGround;
		}
	}

	private static boolean isOnGround(Player p) {
		if(p instanceof CraftPlayer cp) return cp.getHandle().onGround();
		return p.isOnGround();
	}

	private static void maybePlayNote(Player p, ChargeState st) {
		while(st.nextNoteIndex < NOTE_TICK.length && st.ticks >= NOTE_TICK[st.nextNoteIndex]) {
			if(st.ticks == NOTE_TICK[st.nextNoteIndex]) {
				float pitch = NOTE_PITCH[st.nextNoteIndex];
				Utils.playLocalSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
				Utils.playLocalSound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, pitch);
			}
			st.nextNoteIndex++;
		}
		if(st.nextNoteIndex >= NOTE_TICK.length && st.ticks >= E5_FIRST_TICK + E5_PERIOD
				&& (st.ticks - E5_FIRST_TICK) % E5_PERIOD == 0) {
			Utils.playLocalSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, PITCH_E5);
			Utils.playLocalSound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, PITCH_E5);
		}
	}

	private static void launch(Player p, int chargeTicks) {
		if(!(p instanceof CraftPlayer cp)) return;
		ServerPlayer sp = cp.getHandle();
		if(!sp.onGround()) return;

		int noteIdx = -1;
		for(int i = 0; i < NOTE_TICK.length; i++) {
			if(NOTE_TICK[i] <= chargeTicks) noteIdx = i;
			else break;
		}
		if(noteIdx < 0) return;

		double h = NOTE_HEIGHT[noteIdx];
		if(noteIdx + 1 < NOTE_TICK.length) {
			int t0 = NOTE_TICK[noteIdx], t1 = NOTE_TICK[noteIdx + 1];
			double frac = (chargeTicks - t0) / (double) (t1 - t0);
			h += (NOTE_HEIGHT[noteIdx + 1] - h) * frac;
		}
		double v = heightToVelocity(h);
		v = (int)(v * 8000.0) / 8000.0;
		Vec3 m = sp.getDeltaMovement();
		sp.setDeltaMovement(new Vec3(m.x(), v, m.z()));
		sp.hurtMarked = true;
		Utils.playLocalSound(p, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
		Utils.debug(Utils.DebugType.SERVER, p.getName() + " Spring Boots launched after " + chargeTicks + " sneak ticks with upwards velocity " + Utils.round(v, 4));
		MovementAudit.startAirborneAudit(p, "springboots");
	}

	// invert Minecraft player vertical physics. per-tick: pos += v; v -= 0.08; v *= 0.98.
	private static double heightToVelocity(double h) {
		double lo = 0.0, hi = 4.0;
		for(int i = 0; i < 40; i++) {
			double mid = (lo + hi) / 2.0;
			if(simulatePeak(mid) < h) lo = mid;
			else hi = mid;
		}
		return (lo + hi) / 2.0;
	}

	private static double simulatePeak(double v0) {
		double y = 0, v = v0, peak = 0;
		for(int i = 0; i < 400 && v > 0; i++) {
			y += v;
			if(y > peak) peak = y;
			v = (v - 0.08) * 0.98;
		}
		return peak;
	}
}
