package instructions.bosses.goldor;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import plugin.Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * State machine for a single Goldor section gate (S1→S2, S2→S3, S3→S4).
 * See plan §8 for state transitions.
 */
public final class GoldorGate {
	private final World world;
	/** Index of the section this gate belongs to (0=S1, 1=S2, 2=S3). Reported back to Goldor on destruction. */
	private final int sectionIdx;
	private final BoundingBox bounds;
	private final BoundingBox expandedBounds;
	private final Map<Location, BlockData> snapshot = new HashMap<>();

	/** Goldor-relative tick at which this gate's section became active (0 = S1, set by Goldor for S2/S3). */
	private int sectionStartTick = 0;

	private boolean sectionComplete = false;
	private boolean explosionMarked = false;
	private boolean blocksRemoved = false;
	/** True once the "gate destroyed" broadcast has fired (either at early explosion or at block removal). */
	private boolean destroyedAnnounced = false;
	private BukkitTask pendingDelayedRemoval;
	private BukkitTask pendingRegen;

	public GoldorGate(World world, int sectionIdx, BoundingBox bounds) {
		this.world = world;
		this.sectionIdx = sectionIdx;
		this.bounds = bounds;
		this.expandedBounds = bounds.clone().expand(1.0, 1.0, 1.0);
		snapshotBlocks();
	}

	private void snapshotBlocks() {
		int minX = (int) Math.floor(bounds.getMinX());
		int minY = (int) Math.floor(bounds.getMinY());
		int minZ = (int) Math.floor(bounds.getMinZ());
		int maxX = (int) Math.floor(bounds.getMaxX());
		int maxY = (int) Math.floor(bounds.getMaxY());
		int maxZ = (int) Math.floor(bounds.getMaxZ());
		for(int x = minX; x <= maxX; x++) {
			for(int y = minY; y <= maxY; y++) {
				for(int z = minZ; z <= maxZ; z++) {
					Block b = world.getBlockAt(x, y, z);
					if(b.getType() != Material.AIR) {
						snapshot.put(b.getLocation(), b.getBlockData().clone());
					}
				}
			}
		}
	}

	public BoundingBox getExpandedBounds() {
		return expandedBounds;
	}

	/** Records when this gate's section became active, so the verbose "Gate destroyed" line can report
	 *  ticks elapsed since the start of that section (not whichever section is current at destruction time). */
	public void setSectionStartTick(int t) {
		this.sectionStartTick = t;
	}

	/** Event A from plan §8: explosion lands on/near this gate. */
	public void onExplosion() {
		if(blocksRemoved) return;
		if(sectionComplete) {
			// Section already finished; gate was sitting in the 100t auto-destruct window. Skip the wait.
			removeBlocksNow();
		} else {
			// Pre-section hit: gate stays standing, but announce "destroyed" now (per user spec).
			if(!explosionMarked) {
				explosionMarked = true;
				announceDestroyed();
			}
		}
	}

	/** Event B from plan §8: this gate's section just completed. */
	public void onSectionComplete() {
		if(sectionComplete) return;
		sectionComplete = true;
		if(explosionMarked) {
			removeBlocksNow();
		} else {
			pendingDelayedRemoval = Bukkit.getScheduler().runTaskLater(plugin.M7tas.getInstance(), () -> {
				if(!blocksRemoved) removeBlocksNow();
			}, 100L);
		}
	}

	private void removeBlocksNow() {
		if(blocksRemoved) return;
		blocksRemoved = true;
		if(pendingDelayedRemoval != null && !pendingDelayedRemoval.isCancelled()) {
			pendingDelayedRemoval.cancel();
		}
		for(Location loc : snapshot.keySet()) {
			loc.getBlock().setType(Material.AIR, false);
		}
		// Announce only if we haven't already (early-explosion path announces before blocks are removed).
		announceDestroyed();
		// Blocks are gone and the section's items were already done — this is the true section-complete
		// moment. Tell Goldor so it reports the section timing and advances to the next section.
		Goldor.INSTANCE.onGateDestroyed(sectionIdx);
		// Regen always fires exactly 100 ticks after blocks are removed from the world.
		pendingRegen = Bukkit.getScheduler().runTaskLater(plugin.M7tas.getInstance(), this::regenerate, 100L);
	}

	private void announceDestroyed() {
		if(destroyedAnnounced) return;
		destroyedAnnounced = true;
		broadcastDestroyed();
	}

	private void regenerate() {
		for(Map.Entry<Location, BlockData> entry : snapshot.entrySet()) {
			entry.getKey().getBlock().setBlockData(entry.getValue(), false);
		}
		blocksRemoved = false;
		explosionMarked = false;
		destroyedAnnounced = false;
	}

	private void broadcastDestroyed() {
		String msg = ChatColor.GREEN + "The gate has been destroyed!";
		Bukkit.broadcastMessage(msg);
		for(Player pl : Bukkit.getOnlinePlayers()) {
			pl.sendTitle("", msg, 0, 40, 0);
		}
		Utils.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_PLING, 2.0F, 2.0F);
		if(Utils.isVerbose()) Bukkit.broadcastMessage(Goldor.INSTANCE.gateDestroyedLine(sectionStartTick));
	}

	/** Called from resetState() — restore blocks, cancel pending tasks. */
	public void cleanup() {
		if(pendingDelayedRemoval != null && !pendingDelayedRemoval.isCancelled()) pendingDelayedRemoval.cancel();
		if(pendingRegen != null && !pendingRegen.isCancelled()) pendingRegen.cancel();
		if(blocksRemoved) regenerate();
	}
}
