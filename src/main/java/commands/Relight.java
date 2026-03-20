package commands;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_21_R7.CraftWorld;
import org.bukkit.entity.Player;

/*
 * Relight
 * - Forces the server light engine to recalculate light for all loaded chunks.
 *   Uses NMS to call propagateLightSources and checkBlock on every block position
 *   in each chunk, forcing the light engine to initialize previously empty light sections.
 */
public class Relight implements CommandExecutor {
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(!(sender instanceof Player p)) {
			sender.sendMessage("Only players can run this");
			return true;
		}

		World world = p.getWorld();
		ServerLevel serverLevel = ((CraftWorld) world).getHandle();
		LevelLightEngine lightEngine = serverLevel.getLightEngine();

		int chunksRelit = 0;

		for(Chunk chunk : world.getLoadedChunks()) {
			ChunkPos chunkPos = new ChunkPos(chunk.getX(), chunk.getZ());

			// Force the light engine to propagate light sources for this chunk
			lightEngine.propagateLightSources(chunkPos);

			// Also check every block in the chunk to force initialization of empty light sections
			int baseX = chunk.getX() << 4;
			int baseZ = chunk.getZ() << 4;
			for(int x = baseX; x < baseX + 16; x++) {
				for(int z = baseZ; z < baseZ + 16; z++) {
					for(int y = world.getMinHeight(); y < world.getMaxHeight(); y += 16) {
						lightEngine.checkBlock(new BlockPos(x, y, z));
					}
				}
			}
			chunksRelit++;
		}

		// Process all queued light updates
		lightEngine.runLightUpdates();

		p.sendMessage("Relit " + chunksRelit + " chunks");
		return true;
	}
}
