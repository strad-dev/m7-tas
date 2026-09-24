package items;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

/**
 * One resolved click, handed to an {@link AbilityItem} hook. Captured once because one physical click reaches the
 * dispatcher twice ({@code CustomItems.RIGHT_CLICK_GATE_TICKS}) and a teleporting ability must not re-look-up its
 * target block afterwards.
 *
 * @param stack        main-hand stack the ability was resolved from
 * @param clickedBlock block VANILLA reported ({@code getClickedBlock} or {@code ServerboundPlayerActionPacket.getPos}),
 *                     null for air, entity or fake-player clicks. Block abilities use this instead of their own ray
 *                     trace, so range is exactly vanilla's and the target is what the client aimed at.
 * @param tick         {@code MinecraftServer.currentTick} at dispatch, so every hook in one click agrees
 */
public record Cast(Player player, ItemStack stack, Block clickedBlock, Action action, int tick) {

	public boolean rightClick() {
		return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
	}

	public boolean leftClick() {
		return action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
	}
}
