package instructions.bosses.goldor;

import instructions.bosses.WitherLord;
import instructions.bosses.necron.Necron;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.entity.CraftWither;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import net.kyori.adventure.title.Title;
import plugin.M7tas;
import plugin.Utils;

import java.time.Duration;
import java.util.*;

@SuppressWarnings("DataFlowIssue")
public final class Goldor extends WitherLord {
	public static final Goldor INSTANCE = new Goldor();

	private static final int PRE_GOLDOR_TICKS = 2098;

	// Patrol waypoints (block-center XZ). Y stays at spawn Y = 118 during patrol.
	private static final double WP_AX = 100.5, WP_AZ = 40.5;
	private static final double WP_BX = 100.5, WP_BZ = 132.5;
	private static final double WP_CX = 8.5,   WP_CZ = 132.5;
	private static final double WP_DX = 8.5,   WP_DZ = 40.5;
	private static final double PATROL_SPEED = 0.1;

	// Core approach — horizontal targets (Y target is 116, descent is independent of horizontal motion)
	private static final double CORE_TARGET_X = 54.5, CORE_TARGET_Z = 40.5;
	private static final double CORE_FINAL_X  = 54.5, CORE_FINAL_Z  = 114.5;
	private static final double CORE_TARGET_Y = 116.0;
	private static final double CORE_APPROACH_SPEED = 0.8;
	private static final double Y_DESCENT_SPEED = 0.1;

	// Item-frame protection AABB — only the S3 frame wall per user: -2,119,74 to -2,125,80 (block coords).
	// Expand by 1 in each direction to tolerate the frame entity's offset from its attached block.
	private static final BoundingBox S3_FRAME_BOUNDS = new BoundingBox(-3, 118, 73, 0, 126, 81);

	// Simon Says button coord (S1 device) — kept in sync with GoldorListener.SIMON_B{X,Y,Z}.
	private static final int SIMON_BX = 110, SIMON_BY = 121, SIMON_BZ = 91;
	// Block directly behind the Simon Says button (also stonk-immune so the button can't be knocked off).
	private static final int SIMON_BEHIND_BX = 111, SIMON_BEHIND_BY = 121, SIMON_BEHIND_BZ = 91;
	// S1 Simon Says ("SS") device protection zone — the whole device column (110..111, 119..124, 91..96),
	// which covers the button, its backing, and the "i1" label sign at (110,121,93). Every block in here is
	// stonk/break-immune so nothing in the device can be knocked out — which is also what keeps the sign's
	// message intact across runs (it can never be broken/replaced). See isProtected.
	private static final int SS_ZONE_X1 = 110, SS_ZONE_X2 = 111;
	private static final int SS_ZONE_Y1 = 119, SS_ZONE_Y2 = 124;
	private static final int SS_ZONE_Z1 = 91,  SS_ZONE_Z2 = 96;
	// S2 "Lights" device — the blocks the wall levers are mounted on (levers at z=142, mount blocks at z=143).
	private static final int LIGHTS_MOUNT_Z = 143, LIGHTS_MOUNT_X1 = 58, LIGHTS_MOUNT_X2 = 62, LIGHTS_MOUNT_Y1 = 133, LIGHTS_MOUNT_Y2 = 136;
	// S4 Sharp Shooter — the block supporting the gold pressure plate (plate at 63,127,35).
	private static final int PLATE_SUPPORT_BX = 63, PLATE_SUPPORT_BY = 126, PLATE_SUPPORT_BZ = 35;

	// Section lever block coords, indexed [sectionIdx][leverIdx] → {x, y, z}. Single source of truth for both
	// the GoldorLever placements (buildS1..buildS4) and the run-start reset (resetSectionLevers). These are the
	// per-section levers a player flips to clear a section — NOT the S2 "Lights" device levers (those live in the
	// LIGHTS_MOUNT region and are reset separately in Server.serverSetup).
	private static final int[][][] SECTION_LEVER_COORDS = {
			{{106, 124, 113}, {94, 124, 113}},  // S1
			{{27, 124, 127},  {23, 132, 138}},  // S2
			{{2, 122, 55},    {14, 122, 55}},   // S3
			{{84, 121, 34},   {86, 128, 46}},   // S4
	};

	// Per-fight state
	private final List<GoldorSection> sections = new ArrayList<>(4);
	private int currentSectionIdx = 0;
	/** Goldor-relative tick at which the current section began (0 = Goldor start, i.e. S1's start). */
	private int sectionStartTick = 0;
	/** Goldor-relative tick at which the core opened (S4 complete). Used to time the final kill. */
	private int coreOpenTick = 0;
	private boolean phaseActive = false;
	/** True once the core has opened (S4 complete). Before this, Goldor is on patrol and takes no health damage. */
	private boolean coreOpen = false;
	/** Goldor-relative tick of the most recent patrol hit; halves patrol movement speed for 10 ticks after. */
	private int lastDamagedTick = -1000;
	private BukkitTask patrolTask;
	private BukkitTask coreApproachTask;
	private final List<ItemFrame> protectedFrames = new ArrayList<>();
	private ItemFrame arrowAlignFrame;

	// S3 Arrow Align item frame block coord
	private static final int ARROW_X = -2, ARROW_Y = 122, ARROW_Z = 77;
	private final Map<Location, BlockData> coreSnapshot = new HashMap<>();
	private boolean coreBarrierActive = false;

	private Goldor() {
		register(this);
	}

	/** Static facade for the boss-chain. */
	public static void goldorInstructions(World world, boolean doContinue) {
		INSTANCE.start(world, doContinue);
	}

	@Override protected String name() { return "Goldor"; }
	@Override protected String displayName() { return "Goldor"; }
	@Override protected Location spawnLocation() { return new Location(world, 80.5, 118, 40.5, -90f, 0f); }
	@Override protected double maxHealth() { return 700; }
	@Override protected String displayHealth() { return "1.2B"; }
	@Override protected int previousTicks() { return PRE_GOLDOR_TICKS; }

	@Override
	protected void resetState() {
		phaseActive = false;
		coreOpen = false;
		lastDamagedTick = -1000;
		currentSectionIdx = 0;
		sectionStartTick = 0;
		coreOpenTick = 0;
		for(GoldorSection s : sections) s.cleanup();
		sections.clear();
		if(patrolTask != null && !patrolTask.isCancelled()) patrolTask.cancel();
		patrolTask = null;
		if(coreApproachTask != null && !coreApproachTask.isCancelled()) coreApproachTask.cancel();
		coreApproachTask = null;
		if(coreBarrierActive) restoreCoreOriginalBlocks();
		coreSnapshot.clear();
		coreBarrierActive = false;
		for(ItemFrame f : protectedFrames) {
			if(f.isValid()) f.setInvulnerable(false);
		}
		protectedFrames.clear();
	}

	@Override
	protected void onStart() {
		// Storm's section ends as the Goldor (terminals) phase begins — record its end for the practice scoreboard.
		instructions.bosses.WitherActions.recordSplit("Storm", plugin.Utils.runTick());
		startPhase();
		scheduleIntroDialogue();
	}

	@Override
	protected void chainNext(boolean doContinue) {
		if(doContinue) {
			Necron.necronInstructions(world, true);
			runPlayerHandoff(); // start each player's necron() routine the same tick Necron spawns
		} else {
			instructions.bosses.WitherActions.signalRunComplete(); // Goldor/Terminals was the last boss of this practice
		}
	}

	private void scheduleIntroDialogue() {
		sendChatMessage("Who dares trespass into my domain?");
		Utils.scheduleTask(() -> sendChatMessage("Little ants, plotting and scheming, thinking they are invincibile..."), 60);
		Utils.scheduleTask(() -> sendChatMessage("I won't let you break the factory core, I gave my life to my Master."), 120);
		Utils.scheduleTask(() -> sendChatMessage("No one matches me in close quarters."), 180);
	}

	// ---------- Phase setup ----------

	private void startPhase() {
		phaseActive = true;

		// Goldor is hittable while on patrol — drop the wither invulnerability shield so attacks actually
		// land (terminator ding, hurt sound, and the patrol slow all fire). His health bar is still
		// protected by the !coreOpen branch in handleDamage, which cancels the damage itself.
		setArmor(false);

		sections.add(buildS1());
		sections.add(buildS2());
		sections.add(buildS3());
		sections.add(buildS4());

		snapshotCoreOriginalBlocks();
		protectAllItemFrames();
		startPatrolTask();
	}

	private GoldorSection buildS1() {
		List<GoldorTerminal> terms = new ArrayList<>();
		terms.add(new GoldorTerminal(world, 0, 0, 110, 113, 73));
		terms.add(new GoldorTerminal(world, 0, 1, 110, 119, 79));
		terms.add(new GoldorTerminal(world, 0, 2, 90, 112, 92));
		terms.add(new GoldorTerminal(world, 0, 3, 90, 122, 101));
		GoldorDevice dev = new GoldorDevice(world, 0, 110, 121, 91, 1.0);
		List<GoldorLever> lev = buildLevers(0);
		GoldorGate gate = new GoldorGate(world, 0, makeBox(96, 121, 104, 124));
		return new GoldorSection(0, terms, dev, lev, gate);
	}

	private GoldorSection buildS2() {
		List<GoldorTerminal> terms = new ArrayList<>();
		terms.add(new GoldorTerminal(world, 1, 0, 68, 109, 122));
		terms.add(new GoldorTerminal(world, 1, 1, 59, 120, 123));
		terms.add(new GoldorTerminal(world, 1, 2, 47, 109, 122));
		terms.add(new GoldorTerminal(world, 1, 3, 39, 108, 142));
		terms.add(new GoldorTerminal(world, 1, 4, 40, 124, 123));
		GoldorDevice dev = new GoldorDevice(world, 1, 60, 131, 142);
		List<GoldorLever> lev = buildLevers(1);
		GoldorGate gate = new GoldorGate(world, 1, makeBox(16, 128, 19, 136));
		return new GoldorSection(1, terms, dev, lev, gate);
	}

	private GoldorSection buildS3() {
		List<GoldorTerminal> terms = new ArrayList<>();
		terms.add(new GoldorTerminal(world, 2, 0, -2, 109, 112));
		terms.add(new GoldorTerminal(world, 2, 1, -2, 119, 93));
		terms.add(new GoldorTerminal(world, 2, 2, 18, 123, 93));
		terms.add(new GoldorTerminal(world, 2, 3, -2, 109, 77));
		GoldorDevice dev = new GoldorDevice(world, 2, -2, 119, 74);
		List<GoldorLever> lev = buildLevers(2);
		GoldorGate gate = new GoldorGate(world, 2, makeBox(4, 48, 12, 51));
		return new GoldorSection(2, terms, dev, lev, gate);
	}

	private GoldorSection buildS4() {
		List<GoldorTerminal> terms = new ArrayList<>();
		terms.add(new GoldorTerminal(world, 3, 0, 41, 109, 30));
		terms.add(new GoldorTerminal(world, 3, 1, 44, 121, 30));
		terms.add(new GoldorTerminal(world, 3, 2, 67, 109, 30));
		terms.add(new GoldorTerminal(world, 3, 3, 72, 115, 47));
		GoldorDevice dev = new GoldorDevice(world, 3, 63, 126, 35);
		List<GoldorLever> lev = buildLevers(3);
		return new GoldorSection(3, terms, dev, lev, null);
	}

	/** Build the section's levers from {@link #SECTION_LEVER_COORDS} (single source of truth shared with
	 *  {@link #resetSectionLevers}). */
	private List<GoldorLever> buildLevers(int sectionIdx) {
		List<GoldorLever> lev = new ArrayList<>();
		int[][] coords = SECTION_LEVER_COORDS[sectionIdx];
		for(int i = 0; i < coords.length; i++) {
			int[] c = coords[i];
			lev.add(new GoldorLever(world, sectionIdx, i, c[0], c[1], c[2]));
		}
		return lev;
	}

	private static BoundingBox makeBox(int x1, int z1, int x2, int z2) {
		return new BoundingBox(
				Math.min(x1, x2), Math.min(115, 135), Math.min(z1, z2),
				Math.max(x1, x2) + 1, Math.max(115, 135) + 1, Math.max(z1, z2) + 1
		);
	}

	private void protectAllItemFrames() {
		// Per user: only frames in the S3 frame wall (-2,119,74 to -2,125,80) are immune.
		Collection<Entity> ents = world.getNearbyEntities(S3_FRAME_BOUNDS);

		// First pass: protect all S3 frames and find the one closest to the Arrow Align target.
		final double targetX = -1.5, targetY = 122.0, targetZ = 77.5;
		double bestDist = Double.MAX_VALUE;
		ItemFrame best = null;
		for(Entity e : ents) {
			if(e instanceof ItemFrame frame) {
				frame.setInvulnerable(true);
				protectedFrames.add(frame);
				Location floc = frame.getLocation();
				double dx = floc.getX() - targetX;
				double dy = floc.getY() - targetY;
				double dz = floc.getZ() - targetZ;
				double dist = dx * dx + dy * dy + dz * dz;
				if(dist < bestDist) {
					bestDist = dist;
					best = frame;
				}
			}
		}
		if(best != null) {
			best.setRotation(Rotation.NONE);
			arrowAlignFrame = best;
		}
	}

	/** Reset the S3 Arrow Align item frame (used by /setup). Finds the frame closest to the Arrow Align
	 *  target inside the S3 frame wall and rotates it back to NONE so the device starts the next run unsolved. */
	public static void resetS3Device(World world) {
		final double targetX = -1.5, targetY = 122.0, targetZ = 77.5;
		double bestDist = Double.MAX_VALUE;
		ItemFrame best = null;
		for(Entity e : world.getNearbyEntities(S3_FRAME_BOUNDS)) {
			if(e instanceof ItemFrame frame) {
				Location floc = frame.getLocation();
				double dx = floc.getX() - targetX;
				double dy = floc.getY() - targetY;
				double dz = floc.getZ() - targetZ;
				double dist = dx * dx + dy * dy + dz * dz;
				if(dist < bestDist) {
					bestDist = dist;
					best = frame;
				}
			}
		}
		if(best != null) best.setRotation(Rotation.NONE);
	}

	/** Reset every section lever (the per-section levers a player flips, NOT the S2 "Lights" device levers) to
	 *  powered=false so a new run starts with them all off. Preserves each lever's face/facing — only the
	 *  powered state is flipped. Called from {@link instructions.Server#serverSetup} on each run start. */
	public static void resetSectionLevers(World world) {
		for(int[][] section : SECTION_LEVER_COORDS) {
			for(int[] c : section) {
				Block b = world.getBlockAt(c[0], c[1], c[2]);
				if(b.getBlockData() instanceof org.bukkit.block.data.Powerable pw && pw.isPowered()) {
					pw.setPowered(false);
					b.setBlockData(pw, false);
				}
			}
		}
	}

	public ItemFrame getArrowAlignFrame() {
		return arrowAlignFrame;
	}

	/** Returns true if this item frame is within the S3 protected zone (immune to rotation/punch/break).
	 *  Uses live coord check rather than the cached set so frames loaded after phase-start still match. */
	public boolean isProtectedFrame(ItemFrame frame) {
		if(!phaseActive) return false;
		return S3_FRAME_BOUNDS.contains(frame.getLocation().toVector());
	}

	/** Phase-independent variant of {@link #isProtectedFrame}: is this frame within the S3 frame wall,
	 *  regardless of whether the phase is active yet? Lets GoldorListener defer an arrow-frame solve that
	 *  arrives before the phase spins up (the active-phase checks can't identify the frame yet). */
	public boolean isInS3FrameRegion(ItemFrame frame) {
		return S3_FRAME_BOUNDS.contains(frame.getLocation().toVector());
	}

	// ---------- Patrol ----------

	private void startPatrolTask() {
		final int[] leg = {0};
		patrolTask = new BukkitRunnable() {
			@Override
			public void run() {
				if(boss == null || !boss.isValid() || dying) {
					cancel();
					return;
				}
				Location loc = boss.getLocation();
				double x = loc.getX(), z = loc.getZ();
				// Halve patrol speed if Goldor was damaged within the last 5 ticks.
				double speed = (tick - lastDamagedTick < 10) ? PATROL_SPEED * 0.5 : PATROL_SPEED;
				double yaw;
				double dx = 0, dz = 0;
				switch(leg[0]) {
					case 0 -> {
						yaw = -90f;
						double rem = WP_AX - x;
						double step = Math.clamp(rem, 0, speed);
						dx = step;
						if(rem - step <= 1e-5) leg[0] = 1;
					}
					case 1 -> {
						yaw = 0f;
						double rem = WP_BZ - z;
						double step = Math.clamp(rem, 0, speed);
						dz = step;
						if(rem - step <= 1e-5) leg[0] = 2;
					}
					case 2 -> {
						yaw = 90f;
						double rem = x - WP_CX;
						double step = Math.clamp(rem, 0, speed);
						dx = -step;
						if(rem - step <= 1e-5) leg[0] = 3;
					}
					default -> {
						yaw = 180f;
						double rem = z - WP_DZ;
						double step = Math.clamp(rem, 0, speed);
						dz = -step;
						if(rem - step <= 1e-5) leg[0] = 0;
					}
				}
				moveBossTo(x + dx, loc.getY(), z + dz, (float) yaw);
			}
		}.runTaskTimer(M7tas.getInstance(), 1L, 1L);
	}

	private void moveBossTo(double x, double y, double z, float yaw) {
		net.minecraft.world.entity.LivingEntity nms = ((CraftWither) boss).getHandle();
		nms.absSnapTo(x, y, z, yaw, nms.getXRot());
		nms.setDeltaMovement(Vec3.ZERO);
		nms.hurtMarked = true;
		PositionMoveRotation pmr = new PositionMoveRotation(new Vec3(x, y, z), Vec3.ZERO, yaw, nms.getXRot());
		ClientboundTeleportEntityPacket pkt = ClientboundTeleportEntityPacket.teleport(nms.getId(), pmr, Collections.emptySet(), nms.onGround());
		Utils.broadcastPacket(pkt);
	}

	// ---------- Activation API ----------

	public GoldorSection getSection(int idx) {
		if(idx < 0 || idx >= sections.size()) return null;
		return sections.get(idx);
	}

	public GoldorSection getCurrentSection() {
		return getSection(currentSectionIdx);
	}

	public int getCurrentSectionIdx() {
		return currentSectionIdx;
	}

	public boolean isPhaseInactive() {
		return !phaseActive;
	}

	/** Force-end any lingering Goldor phase immediately. Called when a new run is triggered so a previous
	 *  run's still-active phase (phaseActive=true with its S4 device already marked activated, since the
	 *  fight never reached a clean end) doesn't reject this run's pre-fired sharpshooter arrows as
	 *  "device already activated". Mirrors the cleanup {@link #start} does, but runs now instead of waiting
	 *  for the new phase to spin up — so the gap between trigger and new start() is a clean, inactive phase. */
	public void forceEndPhase() {
		if(boss != null) {
			boss.remove();
			boss = null;
		}
		if(tickerTask != null && !tickerTask.isCancelled()) {
			tickerTask.cancel();
			tickerTask = null;
		}
		resetState();
	}

	/** Called from GoldorListener when a terminal/device/lever is activated. */
	public void onActivation(Player p, GoldorSection ownSection, String thingLabel) {
		onActivation(p, ownSection, thingLabel, false);
	}

	/** {@code wasDeferred} is true only for a device whose interaction landed before the phase spun up and was
	 *  held by GoldorListener's one-tick grace — that grace runs the activation a tick late, so this single
	 *  activation's displayed times are credited to the tick the click actually happened ({@code tick - 1}).
	 *  Every non-deferred activation reads the live {@code tick}, which is already phase-relative-correct. */
	public void onActivation(Player p, GoldorSection ownSection, String thingLabel, boolean wasDeferred) {
		if(!phaseActive) return;
		int now = wasDeferred ? Math.max(0, displayTick() - 1) : displayTick();
		GoldorSection cur = getCurrentSection();
		if(cur == null) return;

		ownSection.completed++;

		int order, total;
		if(ownSection == cur) {
			order = cur.completed;
			total = cur.totalItems;
		} else {
			total = cur.totalItems;
			if(cur.completed == 0) {
				order = 1;
			} else {
				order = cur.completed;
			}
		}
		broadcastActivation(p, thingLabel, order, total);
		Utils.timer(verboseTimingLine(now));

		if(ownSection == cur && cur.completed >= cur.totalItems) {
			onAllItemsComplete(cur, now);
		}
	}

	/** Verbose per-activation timing: elapsed ticks within the current section and within the whole Goldor fight.
	 *  Used after each terminal/device/lever activation (gated on {@link Utils#isVerbose()} by the caller).
	 *  {@code now} is the activation's effective tick (live {@code tick}, or {@code tick - 1} if grace-deferred). */
	public String verboseTimingLine(int now) {
		int secTicks = now - sectionStartTick;
		return "<green>" + String.format("S%d: %s ticks (%.2f seconds) | Terminals: %s ticks (%.2f seconds)",
				currentSectionIdx + 1, formatWithSpaces(secTicks), secTicks / 20.0, formatWithSpaces(now), now / 20.0);
	}

	/** Verbose line for a destroyed gate: same shape as an activation line (section-relative + Goldor-relative),
	 *  but headed "Gate destroyed in" instead of an "S#:" label. Section time is measured from that gate's section. */
	public String gateDestroyedLine(int gateSectionStartTick) {
		int secTicks = displayTick() - gateSectionStartTick;
		int termTicks = displayTick();
		return "<green>" + String.format("Gate destroyed in %s ticks (%.2f seconds) | Terminals: %s ticks (%.2f seconds)",
				formatWithSpaces(secTicks), secTicks / 20.0, formatWithSpaces(termTicks), termTicks / 20.0);
	}

	/** Every terminal/device/lever in this section is now done. A section is NOT yet "complete":
	 *  for S1–S3 it stays the current section (its terminals already done, the next section's still
	 *  locked) until its gate is actually destroyed — {@link GoldorGate} calls {@link #onGateDestroyed}
	 *  at that moment to finalize the timing and advance. S4 has no gate, so it completes immediately. */
	private void onAllItemsComplete(GoldorSection s, int now) {
		if(s.idx < 3) {
			// Kick off the gate's destruction (immediate if already blown, else the 100t auto-destruct).
			s.gate.onSectionComplete();
		} else {
			// S4 has no gate, so it completes the instant its items are done — credit the activation's tick.
			reportSectionFinished(s, now);
			onCoreOpen();
		}
	}

	/** Called by {@link GoldorGate} the instant its blocks are removed — which only ever happens after
	 *  the section's items are done. This is the true "section complete" event for S1–S3: report the
	 *  timing (measured to now, i.e. gate destruction) and advance to the next section. */
	public void onGateDestroyed(int sectionIdx) {
		GoldorSection s = getSection(sectionIdx);
		if(s == null || sectionIdx != currentSectionIdx) return;
		// Gate destruction is its own event at the live tick — not a grace-deferred activation.
		reportSectionFinished(s, displayTick());
		currentSectionIdx++;
		sectionStartTick = displayTick();
		// The next section is now active — stamp its gate so a later destruction reports time-into-that-section.
		GoldorSection next = getCurrentSection();
		if(next != null && next.gate != null) next.gate.setSectionStartTick(displayTick());
	}

	/** Broadcast this section's duration (measured to now), the cumulative terminal-phase time, and the run-overall time. */
	private void reportSectionFinished(GoldorSection s, int now) {
		int sectionTicks = now - sectionStartTick;
		Utils.timer("<green>" + String.format("S%d finished in %s ticks (%.2f seconds) | Terminals: ",
				s.idx + 1, formatWithSpaces(sectionTicks), sectionTicks / 20.0) + formatTick(now));
	}

	public static void broadcastActivation(Player p, String thing, int order, int total) {
		String msg = "<gold>" + Utils.getRealName(p) + " "
				+ "<green>activated a " + thing + " ("
				+ "<red>" + order + "<green>/" + total + ")";
		Bukkit.broadcast(Utils.msg(msg));
		for(Player pl : Bukkit.getOnlinePlayers()) {
			pl.showTitle(Title.title(Utils.msg(""), Utils.msg(msg),
					Title.Times.times(Duration.ofMillis(0L), Duration.ofMillis(40 * 50L), Duration.ofMillis(0L))));
		}
		Utils.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_PLING, 2.0F, 2.0F);
	}

	/**
	 * True if this block is a Goldor interactable that must never be destroyed — by stonk, dungeonbreaker, or any
	 * other break. A pure positional test, ALWAYS immune regardless of phase or block state: losing any of these
	 * would soft-lock a section (they're the only way to complete it) or knock an interactable off its mount.
	 * Covers: the Simon Says button (S1) and the block behind it; the S2 "Lights" lamp backing (z=143) and its
	 * levers (z=142); the S4 Sharp Shooter pressure-plate support; and every section lever plus the block directly
	 * beneath it (from the static coord table, so it holds even before the phase spins up).
	 */
	public boolean isProtected(Block b) {
		int bx = b.getX(), by = b.getY(), bz = b.getZ();
		// S1 Simon Says device zone — the whole column (button, backing, and the "i1" sign) is immune.
		if(bx >= SS_ZONE_X1 && bx <= SS_ZONE_X2 && by >= SS_ZONE_Y1 && by <= SS_ZONE_Y2 && bz >= SS_ZONE_Z1 && bz <= SS_ZONE_Z2) return true;
		// S1 Simon Says button and the block behind it.
		if(bx == SIMON_BX && by == SIMON_BY && bz == SIMON_BZ) return true;
		if(bx == SIMON_BEHIND_BX && by == SIMON_BEHIND_BY && bz == SIMON_BEHIND_BZ) return true;
		// S2 "Lights" lamp backing (z=143) plus the levers hanging on the z-142 face.
		if((bz == LIGHTS_MOUNT_Z || bz == LIGHTS_MOUNT_Z - 1) && bx >= LIGHTS_MOUNT_X1 && bx <= LIGHTS_MOUNT_X2 && by >= LIGHTS_MOUNT_Y1 && by <= LIGHTS_MOUNT_Y2) return true;
		// S4 Sharp Shooter gold pressure-plate support block.
		if(bx == PLATE_SUPPORT_BX && by == PLATE_SUPPORT_BY && bz == PLATE_SUPPORT_BZ) return true;
		// Section levers and the support block directly beneath each (static coords → phase-independent).
		for(int[][] section : SECTION_LEVER_COORDS) {
			for(int[] c : section) {
				if(bx == c[0] && bz == c[2] && (by == c[1] || by == c[1] - 1)) return true;
			}
		}
		return false;
	}

	/** True if (x,y,z) is the Simon Says button while the Goldor phase is active. Fake players' rightClick
	 *  normally suppresses stone-button presses across the boss arena; this lets the Simon button through
	 *  so a right-click there actually registers (the button press is still cancelled by MiscListener, but
	 *  GoldorListener counts it first). */
	public boolean isSimonButton(int x, int y, int z) {
		return phaseActive && x == SIMON_BX && y == SIMON_BY && z == SIMON_BZ;
	}

	/** Failsafe invoked from M7tas.onDisable() — immediately restore any gates whose blocks were removed,
	 *  so a mid-fight server stop never leaves the world with broken gate blocks. */
	public void shutdownRegenerateGates() {
		for(GoldorSection s : sections) {
			if(s.gate != null) s.gate.cleanup();
		}
		if(coreBarrierActive) {
			restoreCoreOriginalBlocks();
			coreBarrierActive = false;
		}
	}

	/** Hook called from CustomItems.superboom and other explosion sources. */
	public void notifyExplosionAt(Location loc) {
		if(!phaseActive) return;
		for(GoldorSection s : sections) {
			if(s.gate == null) continue;
			if(s.gate.getExpandedBounds().contains(loc.toVector())) {
				s.gate.onExplosion();
			}
		}
	}

	// ---------- Core open + approach + death ----------

	private void snapshotCoreOriginalBlocks() {
		coreSnapshot.clear();
		for(int x = 52; x <= 56; x++) {
			for(int y = 115; y <= 121; y++) {
				int z = 54;
				Block b = world.getBlockAt(x, y, z);
				if(b.getType() != Material.AIR) {
					coreSnapshot.put(b.getLocation(), b.getBlockData().clone());
				}
			}
		}
	}

	private void onCoreOpen() {
		coreOpen = true;
		coreOpenTick = displayTick();
		// Terminals are done (core just opened) — record the Terminals section end for the practice scoreboard.
		instructions.bosses.WitherActions.recordSplit("Terminals", plugin.Utils.runTick());
		if(patrolTask != null && !patrolTask.isCancelled()) patrolTask.cancel();

		sendChatMessage("You have done it, you destroyed the factory...");
		setArmor(false);

		coreGateBarrierTransition();
		boss.setInvulnerable(false);

		startCoreApproach();

		Utils.scheduleTask(() -> sendChatMessage("But you have nowhere to hide anymore!"), 60);
		Utils.scheduleTask(() -> sendChatMessage("YOU ARE FACE TO FACE WITH GOLDOR!"), 120);
	}

	private void coreGateBarrierTransition() {
		coreBarrierActive = true;
		for(Location loc : coreSnapshot.keySet()) {
			loc.getBlock().setType(Material.BARRIER, false);
		}
		String msg = "<green>The Core entrance is opening!";
		Bukkit.broadcast(Utils.msg(msg));
		for(Player pl : Bukkit.getOnlinePlayers()) {
			pl.showTitle(Title.title(Utils.msg(""), Utils.msg(msg),
					Title.Times.times(Duration.ofMillis(0L), Duration.ofMillis(40 * 50L), Duration.ofMillis(0L))));
		}
		Utils.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_PLING, 2.0F, 2.0F);
		Utils.scheduleTask(() -> {
			for(Location loc : coreSnapshot.keySet()) {
				loc.getBlock().setType(Material.AIR, false);
			}
			coreBarrierActive = false;
		}, 20);
	}

	private void restoreCoreOriginalBlocks() {
		for(Map.Entry<Location, BlockData> entry : coreSnapshot.entrySet()) {
			entry.getKey().getBlock().setBlockData(entry.getValue(), false);
		}
	}

	private void startCoreApproach() {
		final int[] phase = {0};
		coreApproachTask = new BukkitRunnable() {
			@Override
			public void run() {
				if(boss == null || !boss.isValid() || dying) {
					cancel();
					return;
				}
				Location loc = boss.getLocation();
				double x = loc.getX(), y = loc.getY(), z = loc.getZ();

				// Vertical motion is independent of horizontal — Y descends toward CORE_TARGET_Y at Y_DESCENT_SPEED each tick.
				double ny = y;
				if(y > CORE_TARGET_Y) {
					ny = Math.max(CORE_TARGET_Y, y - Y_DESCENT_SPEED);
				}

				if(phase[0] == 0) {
					double dx = CORE_TARGET_X - x;
					double dz = CORE_TARGET_Z - z;
					double mag = Math.sqrt(dx * dx + dz * dz);
					if(mag <= CORE_APPROACH_SPEED) {
						float yaw = computeYaw(dx, dz);
						moveBossTo(CORE_TARGET_X, ny, CORE_TARGET_Z, yaw);
						phase[0] = 1;
					} else {
						double nx = x + dx / mag * CORE_APPROACH_SPEED;
						double nz = z + dz / mag * CORE_APPROACH_SPEED;
						float yaw = computeYaw(dx, dz);
						moveBossTo(nx, ny, nz, yaw);
					}
				} else {
					double rem = CORE_FINAL_Z - z;
					if(rem <= PATROL_SPEED) {
						moveBossTo(CORE_FINAL_X, ny, CORE_FINAL_Z, 0f);
						if(ny == CORE_TARGET_Y) cancel();
					} else {
						moveBossTo(CORE_FINAL_X, ny, z + PATROL_SPEED, 0f);
					}
				}
			}
		}.runTaskTimer(M7tas.getInstance(), 1L, 1L);
	}

	private static float computeYaw(double dx, double dz) {
		return (float) Math.toDegrees(Math.atan2(-dx, dz));
	}

	// ---------- Damage / death ----------

	/** Hooked from MiscListener. Goldor dies silently — vanilla death is suppressed. */
	public void handleDamage(EntityDamageEvent e) {
		if(boss == null || !boss.equals(e.getEntity())) return;
		if(e.isCancelled()) return;
		if(dying) {
			e.setCancelled(true);
			return;
		}
		double finalDmg = e.getFinalDamage();
		if(finalDmg <= 0) return;
		// While on patrol (pre-core), Goldor is "damageable" for feedback only: the hit registers
		// (terminator arrow ding still plays in WithersNotImmuneToArrows after wither.damage() returns)
		// but never reduces his health bar. A recent hit halves his patrol speed for 10 ticks.
		if(!coreOpen) {
			lastDamagedTick = tick;
			// Cancelling the damage below suppresses the vanilla hurt flash, so send the hurt animation
			// ourselves — one packet renders the red flash for ~10 ticks (re-armed by follow-up hits),
			// matching the slow window.
			Utils.broadcastPacket(new ClientboundHurtAnimationPacket(((CraftWither) boss).getHandle()));
			e.setCancelled(true);
			return;
		}
		double currentHp = boss.getHealth();
		if(currentHp - finalDmg <= 0) {
			e.setCancelled(true);
			enterDyingState();
		}
	}

	private void enterDyingState() {
		dying = true;
		boss.addScoreboardTag("TASDying");
		boss.setInvulnerable(true);
		if(coreApproachTask != null && !coreApproachTask.isCancelled()) coreApproachTask.cancel();
		Utils.scheduleTask(() -> {
			if(boss != null && boss.isValid()) {
				try { boss.setHealth(0.001); } catch (IllegalArgumentException ignored) {}
				Utils.changeName(boss);
			}
		}, 1);
		playDeathDialogue();
	}

	private void playDeathDialogue() {
		sendChatMessage("...");
		// Three columns: time-since-core-opened (S4 complete), then the shared Terminals (Goldor) + Overall columns.
		int coreTicks = displayTick() - coreOpenTick;
		Utils.timer("<green>" + String.format("Goldor killed in %s ticks (%.2f seconds) | Terminals: ",
				formatWithSpaces(coreTicks), coreTicks / 20.0) + formatTick(displayTick()));
		Utils.scheduleTask(() -> sendChatMessage("Necron, forgive me."), 60);
		// Open the floor to Necron's arena 100t after the killing blow (restored on the next /reset).
		Utils.scheduleTask(instructions.bosses.BossTransition::openGoldorToNecron, 100);
		Utils.scheduleTask(() -> {
			Utils.timer("<green>Goldor finished in " + formatTick(displayTick()));
			if(tickerTask != null && !tickerTask.isCancelled()) tickerTask.cancel();
			chainNext(doContinue);
		}, 80);
		Utils.scheduleTask(() -> {
			if(boss != null && boss.isValid()) boss.remove();
		}, 160);
	}
}
