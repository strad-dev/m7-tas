package nms;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import listeners.CustomItems;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import plugin.M7tas;

public class PlayerPacketInterceptor extends ChannelDuplexHandler {
	private final Player player;

	public PlayerPacketInterceptor(Player player) {
		this.player = player;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if(msg instanceof ServerboundPlayerActionPacket pkt) {
			var action = pkt.getAction();
			if(action == ServerboundPlayerActionPacket.Action.DROP_ITEM) {
				Bukkit.getScheduler().runTask(M7tas.getInstance(), () -> {
					CustomItems.handleDrop(player, true);
					player.updateInventory();
				});
				return;
			}
			if(action == ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS) {
				Bukkit.getScheduler().runTask(M7tas.getInstance(), () -> {
					CustomItems.handleDrop(player, false);
					player.updateInventory();
				});
				return;
			}
		}
		super.channelRead(ctx, msg);
	}
}
