package instructions.bosses.goldor;

import instructions.bosses.WitherLord;
import instructions.bosses.necron.Necron;
import net.kyori.adventure.title.Title;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import plugin.BossScheduler;
import plugin.M7tas;
import plugin.Utils;

import java.time.Duration;
import java.util.*;

public final class Goldor extends WitherLord {
	public static final Goldor INSTANCE = new Goldor();

	private static final int PRE_GOLDOR_TICKS = 2098;

	// Patrol waypoints (block-center XZ). Y stays at spawn Y = 118 during patrol.
	private static final double WP_AX = 100.5, WP_AZ = 40.5;
	private static final double WP_BX = 100.5, WP_BZ = 132.5;
	private static final double WP_CX = 8.5,   WP_CZ = 132.5;
	private static final double WP_DX = 8.5,   WP_DZ = 40.5;
	private static final double PATROL_SPEED = 0.1;

	// Core approach, horizontal targets.  Y target is 116, and the descent is independent of horizontal motion.
	private static final double CORE_TARGET_X = 54.5, CORE_TARGET_Z = 40.5;
	private static final double CORE_FINAL_X  = 54.5, CORE_FINAL_Z  = 114.5;
	private static final double CORE_TARGET_Y = 116.0;
	private static final double CORE_APPROACH_SPEED = 0.8;
	private static final double Y_DESCENT_SPEED = 0.1;

	// Item-frame protection AABB, only the S3 frame wall: -2,119,74 to -2,125,80 in block coords.
	// Expand by 1 in each direction to tolerate the frame entity's offset from its attached block.
	private static final BoundingBox S3_FRAME_BOUNDS = new BoundingBox(-3, 118, 73, 0, 126, 81);

	// Simon Says button coord (S1 device), kept in sync with GoldorListener.SIMON_B{X,Y,Z}.
	private static final int SIMON_BX = 110, SIMON_BY = 121, SIMON_BZ = 91;
	// Block directly behind the Simon Says button (also stonk-immune so the button can't be knocked off).
	private static final int SIMON_BEHIND_BX = 111, SIMON_BEHIND_BY = 121, SIMON_BEHIND_BZ = 91;
	// S1 Simon Says ("SS") device protection zone: the whole device column (110..111, 119..124, 91..96),
	// which covers the button, its backing, and the "i1" label sign at (110,121,93).  Every block in here is
	// stonk and break immune so nothing in the device can be knocked out.  That is also what keeps the sign's
	// message intact across runs, since it can never be broken or replaced.  See isProtected.
	private static final int SS_ZONE_X1 = 110, SS_ZONE_X2 = 111;
	private static final int SS_ZONE_Y1 = 119, SS_ZONE_Y2 = 124;
	private static final int SS_ZONE_Z1 = 91,  SS_ZONE_Z2 = 96;
	// S2 "Lights" device: the blocks the wall levers are mounted on (levers at z=142, mount blocks at z=143).
	private static final int LIGHTS_MOUNT_Z = 143, LIGHTS_MOUNT_X1 = 58, LIGHTS_MOUNT_X2 = 62, LIGHTS_MOUNT_Y1 = 133, LIGHTS_MOUNT_Y2 = 136;
	// S4 Sharp Shooter: the block supporting the gold pressure plate (plate at 63,127,35).
	private static final int PLATE_SUPPORT_BX = 63, PLATE_SUPPORT_BY = 126, PLATE_SUPPORT_BZ = 35;

	// Section lever block coords, indexed [sectionIdx][leverIdx] → {x, y, z}. Single source of truth for both
	// the GoldorLever placements (buildS1..buildS4) and the run-start reset (resetSectionLevers). These are the
	// per-section levers a player flips to clear a section, NOT the S2 "Lights" device levers, which live in the
	// LIGHTS_MOUNT region and are reset separately in Server.serverSetup.
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

	/**
	 * S3 Arrow Align item frame block coord: the ONE frame in the wall that starts unrotated and has to be turned.
	 * <p>
	 * <b>Single source of truth.</b>  {@link #protectAllItemFrames} and {@link #resetS3Device} both pick the frame
	 * nearest this point, and both used to carry the block centre as their own literal - so moving the device meant
	 * finding three places, and these constants sat unused while the literals did the work.  Both now derive from
	 * {@link #arrowTarget()}.
	 */
	private static final int ARROW_X = -2, ARROW_Y = 120, ARROW_Z = 78;

	/** The block centre of {@link #ARROW_X}/{@link #ARROW_Y}/{@link #ARROW_Z}, which is where a wall frame sits. */
	private static double[] arrowTarget() {
		return new double[]{ARROW_X + 0.5, ARROW_Y, ARROW_Z + 0.5};
	}
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
	@Override protected double maxHealth() { return damage.MobStats.GOLDOR.internalHealth(); }
	@Override protected String displayHealth() { return "1.2B"; }
	@Override protected int previousTicks() { return PRE_GOLDOR_TICKS; }

	@Override
	protected void resetState() {
		phaseActive = false;
		coreOpen = false;
		lastDamagedTick = -1000;
		currentSectionIdx = 0;
		bossSectionIdx = 0;
		sectionStartTick = 0;
		coreOpenTick = 0;
		for(GoldorSection s : sections) s.cleanup();
		sections.clear();
		if(patrolTask != null && !patrolTask.isCancelled()) patrolTask.cancel();
		patrolTask = null;
		if(coreApproachTask != null && !coreApproachTask.isCancelled()) coreApproachTask.cancel();
		coreApproachTask = null;
		cancelInvalidLocationTicker();
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
		// Storm's section ends as the Goldor (terminals) phase begins, so record its end for the practice scoreboard.
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

		// Goldor is hittable while on patrol, so drop the wither invulnerability shield and attacks actually
		// land: the terminator ding, the hurt sound, and the patrol slow all fire.  His health bar is still
		// protected by the !coreOpen branch in handleDamage, which cancels the damage itself.
		setArmor(false);

		sections.add(buildS1());
		sections.add(buildS2());
		sections.add(buildS3());
		sections.add(buildS4());

		snapshotCoreOriginalBlocks();
		protectAllItemFrames();
		startPatrolTask();
		startInvalidLocationTicker();
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
		double[] target = arrowTarget();
		final double targetX = target[0], targetY = target[1], targetZ = target[2];
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
		double[] target = arrowTarget();
		final double targetX = target[0], targetY = target[1], targetZ = target[2];
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
	*  powered=false so a new run starts with them all off.  Each lever's face and facing are preserved, and only
	*  the powered state is flipped.  Called from {@link instructions.Server#serverSetup} on each run start. */
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
		if(!phaseActive) return true;
		return !S3_FRAME_BOUNDS.contains(frame.getLocation().toVector());
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


	// ---------- Ultra-realistic: the invalid-location sweep ----------

	/**
	 * Each section's floor footprint as {@code {xMin, xMax, zMin, zMax}}, inclusive, in progression order S1..S4.
	 * The four corridors ring the arena: S1 runs north, S2 west, S3 south, S4 east back to S1.
	 * <p>
	 * <b>Unbounded in Y on purpose.</b> A corridor is the only thing in the arena at its X/Z, so being anywhere in
	 * one of these columns is being in that section - there is no height at which it stops counting.
	 * <p>
	 * <b>Measured calibration points</b>, each an in-game pair of "this block is outside / the next one is inside".
	 * They pin four of the eight edges exactly, and every one of them agrees with the table:
	 * <ul>
	 *   <li>S2 starts at Z 122 (Z 121 is not S2)</li>
	 *   <li>S2 ends at Z 145 (Z 146 is not S2)</li>
	 *   <li>S3 ends at X 17 (X 18 is not S3 - it is the S2 side of their shared gate)</li>
	 *   <li>S4 ends at Z 49 (Z 50 is not S4)</li>
	 * </ul>
	 * Two consequences of the geometry, inherited from that data rather than chosen here:
	 * <ul>
	 *   <li><b>S4 and S1 share the block line X 89.</b>  {@link #sectionAt} resolves overlaps in progression order,
	 *       so that line reads as S1 (the section it is the START of), not S4's far end.  Moot in practice, since
	 *       S4 is exempt either way.</li>
	 *   <li><b>Z 50 belongs to no section</b>, the one-block gap between S3's near edge (Z 51) and S4's far edge
	 *       (Z 49).  It sits inside S3's gate box, so it is the doorway between them, and standing in a doorway is
	 *       deliberately never invalid.</li>
	 * </ul>
	 * The corridor widths (23 / 24 / 21 / 21 blocks) and lengths (93 / 94 / 95 / 93) are not uniform.  That is the
	 * data as given, not an error here.
	 */
	private static final int[][] SECTION_BOUNDS = {
			{89, 111, 29, 121},   // S1
			{18, 111, 122, 145},  // S2
			{-3, 17, 51, 145},    // S3
			{-3, 89, 29, 49},     // S4
	};

	/** How often the sweep runs.  A player has this long to get out of a section they should not be in. */
	private static final int INVALID_LOCATION_POLL_TICKS = 60;

	private Runnable invalidLocationTicker;

	/**
	 * Kill anyone standing somewhere they have no business being, every {@link #INVALID_LOCATION_POLL_TICKS} ticks
	 * (ultra-realistic only).  <b>Two independent rules, on two different clocks.</b>
	 * <ul>
	 *   <li><b>Ahead of the party</b> - a section past the current one, i.e. one whose gate has not been opened.
	 *       Judged against {@link #currentSectionIdx}, the party's progress, and unconditional: this is the rule
	 *       that stops a gate being skipped.</li>
	 *   <li><b>Overtaken by Goldor</b> - Goldor is <b>physically standing in a section past yours</b>.  Judged
	 *       against {@link #bossSectionIdx}, his position, and <b>nothing to do with the party's progress</b>:
	 *       finishing a section early does not put you in danger, and failing to finish one does not protect you.
	 *       Two worked cases, both from live play: finish S2 while he is still walking S1 or S2 and you live;
	 *       linger in S1 while he walks into S2 and you die, S1 complete or not.</li>
	 * </ul>
	 *
	 * <p><b>Standing in S4 is never invalid</b>, whichever section is current and wherever Goldor is - it is the one
	 * corridor the sweep exempts, under both rules.  Note which end that exemption is on: it is the PLAYER's section
	 * that has to be S4, not Goldor's.
	 *
	 * <p>Being outside every corridor - the core approach, a gateway, the arena floor - is never invalid either.
	 * The sweep only ever judges somebody who is definitely inside S1, S2 or S3.
	 *
	 * <p>A poll is 60 ticks and {@code CheatDeath}'s shortest immunity is 60, so a proc on one sweep has expired by
	 * the next: a player who stays put is saved once and then dies, which is the intent.
	 */
	private void startInvalidLocationTicker() {
		invalidLocationTicker = () -> {
			if(!phaseActive || dying) return;
			// Both EVERY tick, not on the poll grid: the bar is a countdown, and the tracker is a state machine over
			// Goldor's position that must not miss a crossing.  Only the sweep itself is throttled.
			updateActionBar();
			trackBossSection();
			if(displayTick() % INVALID_LOCATION_POLL_TICKS != 0) return;
			pollInvalidLocations();
		};
		BossScheduler.addTicker(invalidLocationTicker);
	}

	private void cancelInvalidLocationTicker() {
		if(invalidLocationTicker != null) {
			BossScheduler.removeTicker(invalidLocationTicker);
			invalidLocationTicker = null;
			// Wipe the HUD rather than leaving the last "Death Ticks 3t" on screen for its fade-out, the same as
			// Storm's cancelCycleTask.  A cheat-death cooldown still showing is intended - see Utils.sendActionBar.
			Utils.broadcastActionBar(net.kyori.adventure.text.Component.empty());
		}
	}

	/**
	 * The Goldor phase's action bar: how long until the next death-tick sweep.
	 * <p>
	 * "Death ticks" is the Hypixel name for {@link #pollInvalidLocations} - the periodic check that kills anyone
	 * standing where they should not be. The phase had no HUD of its own before this, which is worth knowing because
	 * it was the one stretch where {@code death/Deaths}' action-bar fallback was the only thing drawing cooldowns;
	 * that fallback now defers to this, since {@code Utils.sendActionBar} stamps the tick.
	 * <p>
	 * <b>Ultra-realistic only.</b> Nothing happens on the grid in the other two modes, and a countdown to nothing is
	 * worse than no countdown. Counts {@code POLL} → 1 on the absolute phase-tick grid the sweep itself gates on, so
	 * the bar can never drift from the mechanic - the same anchor-not-a-counter rule the other three boss HUDs follow.
	 */
	private void updateActionBar() {
		if(!damage.Difficulty.deathsEnabled()) return;
		int left = INVALID_LOCATION_POLL_TICKS - Math.floorMod(displayTick(), INVALID_LOCATION_POLL_TICKS);
		Utils.broadcastActionBar(Utils.msg("<gold>Death Ticks <white>" + left + "t"));
	}

	private void pollInvalidLocations() {
		if(!damage.Difficulty.deathsEnabled()) return;
		for(Player p : world.getPlayers()) {
			int section = sectionAt(p.getLocation());
			if(section < 0) continue; // not in a corridor at all, so nothing to judge
			if(!isInvalidSection(section)) continue;
			death.Deaths.kill(p, "Goldor");
		}
	}

	/** True if being in section {@code idx} is fatal right now.  See {@link #startInvalidLocationTicker}. */
	private boolean isInvalidSection(int idx) {
		if(idx == S4_INDEX) return false;          // the one exempt corridor, under either rule
		if(idx > currentSectionIdx) return true;   // gate not opened yet
		return bossSectionIdx > idx;               // Goldor has physically walked past this corridor
	}

	/** S4's index in {@link #SECTION_BOUNDS}, the corridor {@link #isInvalidSection} exempts. */
	private static final int S4_INDEX = 3;

	/**
	 * Which corridor Goldor is considered to be patrolling, as an index into {@link #SECTION_BOUNDS}.
	 * <p>
	 * <b>Tracked, not derived.</b> {@link #sectionAt} on his live position is wrong twice over. He <b>spawns at
	 * (80.5, 40.5), which is inside S4's box</b> - reading that literally would say he is three corridors ahead of a
	 * party still in S1 and kill all of them on the first sweep. And his patrol is a loop, so raw geometry falls
	 * back to S1 every lap and would keep un-overtaking stragglers.
	 * <p>
	 * So this starts at S1 - Goldor starts with the party - and {@link #trackBossSection} only ever advances it to
	 * the NEXT corridor in ring order. That ignores the pre-S1 spawn stretch (from S1, the only accepted step is to
	 * S2) and makes the S4 → S1 wrap a real advance rather than a reset.
	 */
	private int bossSectionIdx = 0;

	/**
	 * Advance {@link #bossSectionIdx} if Goldor has just walked into the next corridor of the ring.
	 * <p>
	 * Only single forward steps are accepted, which is what makes it a monotonic lap counter rather than a position
	 * readout. A sample that lands between corridors reads -1 and is simply ignored; his patrol speed is 0.1/tick
	 * against ~90-block corridors, so no crossing can be missed at any sane sample rate.
	 */
	private void trackBossSection() {
		if(boss == null || !boss.isValid()) return;
		int geo = sectionAt(boss.getLocation());
		if(geo < 0) return;
		if(geo == (bossSectionIdx + 1) % SECTION_BOUNDS.length) bossSectionIdx = geo;
	}

	/** Index of the section containing {@code loc}, or -1 for none.  First match in progression order wins. */
	private static int sectionAt(Location loc) {
		int x = loc.getBlockX(), z = loc.getBlockZ();
		for(int i = 0; i < SECTION_BOUNDS.length; i++) {
			int[] b = SECTION_BOUNDS[i];
			if(x >= b[0] && x <= b[1] && z >= b[2] && z <= b[3]) return i;
		}
		return -1;
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

	/** Called from GoldorListener when a terminal/device/lever is activated. */
	public void onActivation(Player p, GoldorSection ownSection, String thingLabel) {
		onActivation(p, ownSection, thingLabel, false);
	}

	/** {@code wasDeferred} is true only for a device whose interaction landed before the phase spun up and was
	*  held by GoldorListener's one-tick grace.  That grace runs the activation a tick late, so this single
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
	*  Used after each terminal, device or lever activation (gated on {@link Utils#isVerbose()} by the caller).
	*  {@code now} is the activation's effective tick: the live {@code tick}, or {@code tick - 1} if grace-deferred. */
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

	/** Every terminal, device and lever in this section is now done.  A section is NOT yet "complete":
	*  for S1–S3 it stays the current section (its terminals already done, the next section's still
	*  locked) until its gate is actually destroyed.  {@link GoldorGate} calls {@link #onGateDestroyed}
	*  at that moment to finalize the timing and advance.  S4 has no gate, so it completes immediately. */
	private void onAllItemsComplete(GoldorSection s, int now) {
		if(s.idx < 3) {
			// Kick off the gate's destruction (immediate if already blown, else the 100t auto-destruct).
			s.gate.onSectionComplete();
		} else {
			// S4 has no gate, so it completes the instant its items are done.  Credit the activation's tick.
			reportSectionFinished(s, now);
			onCoreOpen();
		}
	}

	/** Called by {@link GoldorGate} the instant its blocks are removed, which only ever happens after
	*  the section's items are done.  This is the true "section complete" event for S1–S3: report the
	*  timing (measured to now, i.e. gate destruction) and advance to the next section. */
	public void onGateDestroyed(int sectionIdx) {
		GoldorSection s = getSection(sectionIdx);
		if(s == null || sectionIdx != currentSectionIdx) return;
		// Gate destruction is its own event at the live tick, not a grace-deferred activation.
		reportSectionFinished(s, displayTick());
		currentSectionIdx++;
		sectionStartTick = displayTick();
		// The next section is now active, so stamp its gate and a later destruction reports time-into-that-section.
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
		playActivationSound();
	}

	/**
	 * The activation cue: the pling every completed terminal, device and lever makes.
	 * <p>
	 * <b>One definition</b>, because Melody's per-row button deliberately makes the same noise
	 * ({@link GoldorTerminalGui#onClick}) - clearing a row should sound like progress, and "the same as a completion"
	 * is the spec, not a coincidence.  Move the sound and both move.
	 */
	public static void playActivationSound() {
		Utils.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_PLING, 2.0F, 2.0F);
	}

	/**
	 * True if this block is a Goldor interactable that must never be destroyed by stonk, dungeonbreaker, or any
	 * other break.  This is a pure positional test and is ALWAYS immune regardless of phase or block state, because
	 * losing any of these would soft-lock a section (they're the only way to complete it) or knock an interactable
	 * off its mount.
	 * Covers: the Simon Says button (S1) and the block behind it; the S2 "Lights" lamp backing (z=143) and its
	 * levers (z=142); the S4 Sharp Shooter pressure-plate support; and every section lever plus the block directly
	 * beneath it (from the static coord table, so it holds even before the phase spins up).
	 */
	public boolean isProtected(Block b) {
		int bx = b.getX(), by = b.getY(), bz = b.getZ();
		// S1 Simon Says device zone: the whole column (button, backing, and the "i1" sign) is immune.
		if(bx >= SS_ZONE_X1 && bx <= SS_ZONE_X2 && by >= SS_ZONE_Y1 && by <= SS_ZONE_Y2 && bz >= SS_ZONE_Z1 && bz <= SS_ZONE_Z2) return true;
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

	/** Failsafe invoked from M7tas.onDisable().  It immediately restores any gates whose blocks were removed,
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
		// Terminals are done now that the core opened, so record the Terminals section end for the scoreboard.
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

				// Vertical motion is independent of horizontal: Y descends toward CORE_TARGET_Y at Y_DESCENT_SPEED each tick.
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

	/** Damage clamp for Goldor, called from {@code damage.Damage.deal}.  He dies silently, since vanilla death is
	 *  suppressed. */
	@Override
	public double clampDamage(double incoming) {
		if(boss == null) return incoming;
		if(dying) return 0;
		if(incoming <= 0) return 0;
		// While on patrol (pre-core), Goldor is "damageable" for feedback only: the hit registers (the terminator
		// arrow ding still plays) but never reduces his health bar.  A recent hit halves his patrol speed for 10
		// ticks.  Blocking the damage means no hurt flash, so send the animation ourselves - one packet renders
		// the red flash for ~10 ticks, re-armed by follow-up hits, which matches the slow window.
		if(!coreOpen) {
			lastDamagedTick = tick;
			Utils.broadcastPacket(new ClientboundHurtAnimationPacket(((CraftWither) boss).getHandle()));
			return 0;
		}
		if(boss.getHealth() - incoming <= 0) {
			// Killing blow: deal everything except DYING_SLIVER, rather than the 0 this used to return.  Returning 0
			// meant the hit that killed Goldor moved his health bar not at all, and enterDyingState's deferred pin did
			// all the work a tick later.
			double currentHp = boss.getHealth();
			enterDyingState();
			return Math.max(0, currentHp - DYING_SLIVER);
		}
		return incoming;
	}

	private void enterDyingState() {
		dying = true;
		boss.addScoreboardTag("TASDying");
		boss.setInvulnerable(true);
		if(coreApproachTask != null && !coreApproachTask.isCancelled()) coreApproachTask.cancel();
		Utils.scheduleTask(() -> {
			if(boss != null && boss.isValid()) {
				try { boss.setHealth(DYING_SLIVER); } catch (IllegalArgumentException ignored) {}
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
			// Stamp the leaderboard duration at the phase's real end (this tick), not the killing blow.  It must
			// come before chainNext, which spawns Necron and re-anchors the phase clock.  The board times the WHOLE
			// Goldor phase, terminals and core, matching what /m7practice goldor times.  It is not the core-only
			// column printed at the killing blow.

			instructions.bosses.WitherActions.recordPhaseDuration("Goldor", displayTick());
			if(tickerTask != null && !tickerTask.isCancelled()) tickerTask.cancel();
			chainNext(doContinue);
		}, 80);
		Utils.scheduleTask(() -> {
			if(boss != null && boss.isValid()) boss.remove();
		}, 160);
	}
}
