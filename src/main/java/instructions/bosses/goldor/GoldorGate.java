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
	private final BoundingBox bounds;
	private final BoundingBox expandedBounds;
	private final Map<Location, BlockData> snapshot = new HashMap<>();

	private boolean sectionComplete = false;
	private boolean explosionMarked = false;
	private boolean blocksRemoved = false;
	private BukkitTask pendingDelayedRemoval;
	private BukkitTask pendingRegen;

	public GoldorGate(World world, BoundingBox bounds) {
		this.world = world;
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

	/** Event A from plan §8: explosion lands on/near this gate. */
	public void onExplosion() {
		if(blocksRemoved) return;
		if(sectionComplete) {
			removeBlocksNow();
		} else {
			explosionMarked = true;
		}
	}

	/** Event B from plan §8: this gate's section just completed. */
	public void onSectionComplete() {
		if(sectionComplete) return;
		sectionComplete = true;
		// Always schedule regen 100t after section complete.
		pendingRegen = Bukkit.getScheduler().runTaskLater(plugin.M7tas.getInstance(), this::regenerate, 100L);
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
		broadcastDestroyed();
	}

	private void regenerate() {
		for(Map.Entry<Location, BlockData> entry : snapshot.entrySet()) {
			entry.getKey().getBlock().setBlockData(entry.getValue(), false);
		}
		blocksRemoved = false;
		explosionMarked = false;
	}

	private void broadcastDestroyed() {
		String msg = ChatColor.YELLOW + "The gate has been destroyed!";
		Bukkit.broadcastMessage(msg);
		for(Player pl : Bukkit.getOnlinePlayers()) {
			pl.sendTitle("", msg, 0, 40, 0);
		}
		Utils.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_PLING, 2.0F, 2.0F);
	}

	/** Called from resetState() — restore blocks, cancel pending tasks. */
	public void cleanup() {
		if(pendingDelayedRemoval != null && !pendingDelayedRemoval.isCancelled()) pendingDelayedRemoval.cancel();
		if(pendingRegen != null && !pendingRegen.isCancelled()) pendingRegen.cancel();
		if(blocksRemoved) regenerate();
	}
}
