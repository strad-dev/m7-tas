package nms;

import commands.Spectate;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import listeners.CustomItems;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import plugin.M7tas;

public class PlayerPacketInterceptor extends ChannelDuplexHandler {
	private final Player player;

	public PlayerPacketInterceptor(Player player) {
		this.player = player;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if(msg instanceof ServerboundMovePlayerPacket) {
			if(Spectate.getSpectatorMap().containsKey(player)) {
				return; // silently drop — don't forward to server
			}
		}
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
		if(msg instanceof ServerboundInteractPacket pkt) {
			Bukkit.getScheduler().runTask(M7tas.getInstance(), () -> {
				ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
				ServerLevel level = nmsPlayer.level();
				net.minecraft.world.entity.Entity target = pkt.getTarget(level);
				if(target instanceof LivingEntity living && living.isDeadOrDying()) {
					pkt.dispatch(new ServerboundInteractPacket.Handler() {
						public void onInteraction(InteractionHand hand) {}
						public void onInteraction(InteractionHand hand, Vec3 pos) {}
						public void onAttack() {
							CustomItems.handleCustomItems(null, EquipmentSlot.HAND, player.getInventory().getItemInMainHand(), Action.LEFT_CLICK_AIR, player);
						}
					});
				}
			});
		}
		super.channelRead(ctx, msg);
	}
}
