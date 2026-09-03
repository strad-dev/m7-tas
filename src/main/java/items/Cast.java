package items;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

/**
 * One click, as the dispatcher resolved it, handed to an {@link AbilityItem}'s hook.
 * <p>
 * Everything an ability used to re-read for itself is captured here, which matters because the same physical
 * click reaches the dispatcher twice (see {@code CustomItems.RIGHT_CLICK_GATE_TICKS}) and because an ability that
 * teleports its caster must not then look up "the block I am aiming at" a second time.
 *
 * @param player       who clicked
 * @param stack        the main-hand stack this ability was resolved from
 * @param clickedBlock the block VANILLA reported the click landed on ({@code PlayerInteractEvent.getClickedBlock}
 *                     or {@code ServerboundPlayerActionPacket.getPos}), or null for an air click, an entity
 *                     interaction, or a fake-player dispatch.  An ability that acts on a block uses this instead
 *                     of ray-tracing a reach of its own, so its range is exactly vanilla's interaction range and
 *                     its target is exactly the block the client aimed at.
 * @param action       the Bukkit action, for the handful of abilities that behave differently per click side
 * @param tick         {@code MinecraftServer.currentTick} at dispatch, so every hook in one click agrees on it
 */
public record Cast(Player player, ItemStack stack, Block clickedBlock, Action action, int tick) {

	public boolean rightClick() {
		return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
	}

	public boolean leftClick() {
		return action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
	}
}
