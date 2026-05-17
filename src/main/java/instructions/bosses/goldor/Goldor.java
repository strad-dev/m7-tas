package instructions.bosses.goldor;

import instructions.bosses.WitherLord;
import instructions.bosses.necron.Necron;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftWither;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import plugin.M7tas;
import plugin.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("DataFlowIssue")
public final class Goldor extends WitherLord {
	public static final Goldor INSTANCE = new Goldor();

	private static final int PRE_GOLDOR_TICKS = 2416;

	// Patrol waypoints (block-center XZ). Y fixed at spawn Y = 116.
	private static final double WP_AX = 100.5, WP_AZ = 40.5;
	private static final double WP_BX = 100.5, WP_BZ = 132.5;
	private static final double WP_CX = 8.5,   WP_CZ = 132.5;
	private static final double WP_DX = 8.5,   WP_DZ = 40.5;
	private static final double PATROL_SPEED = 0.1;

	// Core approach
	private static final double CORE_TARGET_X = 54.5, CORE_TARGET_Z = 40.5;
	private static final double CORE_FINAL_X  = 54.5, CORE_FINAL_Z  = 114.5;
	private static final double CORE_APPROACH_SPEED = 0.8;

	// Item-frame protection AABB (covers all four sections + margin)
	private static final BoundingBox ARENA_BOUNDS = new BoundingBox(-10, 100, 20, 120, 140, 150);

	// Per-fight state
	private final List<GoldorSection> sections = new ArrayList<>(4);
	private int currentSectionIdx = 0;
	private boolean phaseActive = false;
	private BukkitTask patrolTask;
	private BukkitTask coreApproachTask;
	private final List<ItemFrame> protectedFrames = new ArrayList<>();
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
	@Override protected Location spawnLocation() { return new Location(world, 80.5, 116, 40.5, -90f, 0f); }
	@Override protected double maxHealth() { return 700; }
	@Override protected String displayHealth() { return "1.2B"; }
	@Override protected int previousTicks() { return PRE_GOLDOR_TICKS; }

	@Override
	protected void resetState() {
		phaseActive = false;
		currentSectionIdx = 0;
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
		startPhase();
		scheduleIntroDialogue();
	}

	@Override
	protected void chainNext(boolean doContinue) {
		if(doContinue) {
			Necron.necronInstructions(world, true);
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
		GoldorDevice dev = new GoldorDevice(world, 0, 110, 121, 91);
		List<GoldorLever> lev = new ArrayList<>();
		lev.add(new GoldorLever(world, 0, 0, 106, 124, 113));
		lev.add(new GoldorLever(world, 0, 1, 94, 124, 113));
		GoldorGate gate = new GoldorGate(world, makeBox(96, 115, 122, 104, 135, 124));
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
		List<GoldorLever> lev = new ArrayList<>();
		lev.add(new GoldorLever(world, 1, 0, 27, 124, 127));
		lev.add(new GoldorLever(world, 1, 1, 23, 132, 138));
		GoldorGate gate = new GoldorGate(world, makeBox(16, 115, 128, 18, 135, 136));
		return new GoldorSection(1, terms, dev, lev, gate);
	}

	private GoldorSection buildS3() {
		List<GoldorTerminal> terms = new ArrayList<>();
		terms.add(new GoldorTerminal(world, 2, 0, -2, 109, 112));
		terms.add(new GoldorTerminal(world, 2, 1, -2, 119, 93));
		terms.add(new GoldorTerminal(world, 2, 2, 18, 123, 93));
		terms.add(new GoldorTerminal(world, 2, 3, -2, 109, 77));
		GoldorDevice dev = new GoldorDevice(world, 2, -2, 119, 74);
		List<GoldorLever> lev = new ArrayList<>();
		lev.add(new GoldorLever(world, 2, 0, 2, 122, 55));
		lev.add(new GoldorLever(world, 2, 1, 14, 122, 55));
		GoldorGate gate = new GoldorGate(world, makeBox(4, 115, 48, 12, 135, 50));
		return new GoldorSection(2, terms, dev, lev, gate);
	}

	private GoldorSection buildS4() {
		List<GoldorTerminal> terms = new ArrayList<>();
		terms.add(new GoldorTerminal(world, 3, 0, 41, 109, 30));
		terms.add(new GoldorTerminal(world, 3, 1, 44, 121, 30));
		terms.add(new GoldorTerminal(world, 3, 2, 67, 109, 30));
		terms.add(new GoldorTerminal(world, 3, 3, 72, 115, 47));
		GoldorDevice dev = new GoldorDevice(world, 3, 63, 126, 35);
		List<GoldorLever> lev = new ArrayList<>();
		lev.add(new GoldorLever(world, 3, 0, 84, 121, 34));
		lev.add(new GoldorLever(world, 3, 1, 86, 128, 46));
		return new GoldorSection(3, terms, dev, lev, null);
	}

	private static BoundingBox makeBox(int x1, int y1, int z1, int x2, int y2, int z2) {
		return new BoundingBox(
				Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
				Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, Math.max(z1, z2) + 1
		);
	}

	private void protectAllItemFrames() {
		Collection<Entity> ents = world.getNearbyEntities(ARENA_BOUNDS);
		for(Entity e : ents) {
			if(e instanceof ItemFrame frame) {
				frame.setInvulnerable(true);
				protectedFrames.add(frame);
			}
		}
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
				double yaw;
				double dx = 0, dz = 0;
				switch(leg[0]) {
					case 0 -> {
						yaw = -90f;
						double rem = WP_AX - x;
						double step = Math.clamp(rem, 0, PATROL_SPEED);
						dx = step;
						if(rem - step <= 1e-5) leg[0] = 1;
					}
					case 1 -> {
						yaw = 0f;
						double rem = WP_BZ - z;
						double step = Math.clamp(rem, 0, PATROL_SPEED);
						dz = step;
						if(rem - step <= 1e-5) leg[0] = 2;
					}
					case 2 -> {
						yaw = 90f;
						double rem = x - WP_CX;
						double step = Math.clamp(rem, 0, PATROL_SPEED);
						dx = -step;
						if(rem - step <= 1e-5) leg[0] = 3;
					}
					default -> {
						yaw = 180f;
						double rem = z - WP_DZ;
						double step = Math.clamp(rem, 0, PATROL_SPEED);
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

	/** Called from GoldorListener when a terminal/device/lever is activated. */
	public void onActivation(Player p, GoldorSection ownSection, String thingLabel) {
		if(!phaseActive) return;
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

		if(ownSection == cur && cur.completed >= cur.totalItems) {
			onSectionComplete(cur);
		}
	}

	private void onSectionComplete(GoldorSection s) {
		if(s.idx < 3) {
			s.gate.onSectionComplete();
			currentSectionIdx++;
		} else {
			onCoreOpen();
		}
	}

	public static void broadcastActivation(Player p, String thing, int order, int total) {
		String msg = ChatColor.GOLD + Utils.getRealName(p) + " "
				+ ChatColor.GREEN + "activated a " + thing + " ("
				+ ChatColor.RED + order + ChatColor.GREEN + "/" + total + ")";
		Bukkit.broadcastMessage(msg);
		for(Player pl : Bukkit.getOnlinePlayers()) {
			pl.sendTitle("", msg, 0, 40, 0);
		}
		Utils.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_PLING, 2.0F, 2.0F);
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
		String msg = ChatColor.YELLOW + "The Core entrance is opening!";
		Bukkit.broadcastMessage(msg);
		for(Player pl : Bukkit.getOnlinePlayers()) {
			pl.sendTitle("", msg, 0, 40, 0);
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
				if(phase[0] == 0) {
					double dx = CORE_TARGET_X - x;
					double dz = CORE_TARGET_Z - z;
					double mag = Math.sqrt(dx * dx + dz * dz);
					if(mag <= CORE_APPROACH_SPEED) {
						float yaw = computeYaw(dx, dz);
						moveBossTo(CORE_TARGET_X, y, CORE_TARGET_Z, yaw);
						phase[0] = 1;
					} else {
						double nx = x + dx / mag * CORE_APPROACH_SPEED;
						double nz = z + dz / mag * CORE_APPROACH_SPEED;
						float yaw = computeYaw(dx, dz);
						moveBossTo(nx, y, nz, yaw);
					}
				} else {
					double rem = CORE_FINAL_Z - z;
					if(rem <= PATROL_SPEED) {
						moveBossTo(CORE_FINAL_X, y, CORE_FINAL_Z, 0f);
						cancel();
					} else {
						moveBossTo(CORE_FINAL_X, y, z + PATROL_SPEED, 0f);
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
		Bukkit.broadcastMessage(ChatColor.GREEN + "Goldor killed in " + formatTick(tick));
		Utils.scheduleTask(() -> {
			sendChatMessage("Necron, forgive me.");
			chainNext(doContinue);
		}, 60);
		Utils.scheduleTask(() -> {
			if(boss != null && boss.isValid()) boss.remove();
			if(tickerTask != null && !tickerTask.isCancelled()) tickerTask.cancel();
		}, 160);
	}
}
