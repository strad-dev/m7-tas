package nms;

import commands.Spectate;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import listeners.CustomItems;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R7.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import plugin.M7tas;

import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerPacketInterceptor extends ChannelDuplexHandler {
	private final Player player;

	public PlayerPacketInterceptor(Player player) {
		this.player = player;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if(msg instanceof ServerboundMovePlayerPacket pkt) {
			if(Spectate.getSpectatorMap().containsKey(player)) {
				if(pkt.hasPosition()) {
					Spectate.updateClientPosition(player, pkt.getX(0), pkt.getY(0), pkt.getZ(0));
				}
				return;
			}
		}
		if(msg instanceof ServerboundSetCarriedItemPacket) {
			Player fakePlayer = Spectate.getSpectatorMap().get(player);
			if(fakePlayer != null) {
				((CraftPlayer) player).getHandle().connection.send(
					new ClientboundSetHeldSlotPacket(fakePlayer.getInventory().getHeldItemSlot()));
				return;
			}
		}
		if(msg instanceof ServerboundPlayerActionPacket pkt) {
			var action = pkt.getAction();
			AtomicBoolean usedAbility = new AtomicBoolean(false);
			if(action == ServerboundPlayerActionPacket.Action.DROP_ITEM) {
				Bukkit.getScheduler().runTask(M7tas.getInstance(), () -> {
					usedAbility.set(CustomItems.handleDrop(player, true));
					player.updateInventory();
				});
			} else if(action == ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS) {
				Bukkit.getScheduler().runTask(M7tas.getInstance(), () -> {
					usedAbility.set(CustomItems.handleDrop(player, false));
					player.updateInventory();
				});
			} else if(action == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) {
				String heldId = CustomItems.getID(player.getInventory().getItemInMainHand());
				if("skyblock/combat/last_breath".equals(heldId)) {
					Bukkit.getScheduler().runTask(M7tas.getInstance(), () -> {
						ServerPlayer sp = ((CraftPlayer) player).getHandle();
						boolean was = sp.getAbilities().instabuild;
						sp.getAbilities().instabuild = true;
						sp.releaseUsingItem();
						sp.getAbilities().instabuild = was;
					});
					return;
				}
			}
			if(usedAbility.get()) {
				return;
			}
		}
		if(msg instanceof ServerboundInteractPacket pkt) {
			// Dispatch the LEFT_CLICK_AIR ability path for every attack action. EntityDamageByEntityEvent
			// only fires when damage actually lands, which excludes invulnerable-shield withers, dying
			// entities, and other no-damage cases — meaning abilities like the mage beam never fire on
			// those hits via the normal EDBEE path. The same-tick cooldown in handleCustomItems
			// (lastLeftClickAbilityTick) dedupes against the EDBEE dispatch when damage does land.
			Bukkit.getScheduler().runTask(M7tas.getInstance(), () -> {
				pkt.dispatch(new ServerboundInteractPacket.Handler() {
					public void onInteraction(InteractionHand hand) {}
					public void onInteraction(InteractionHand hand, Vec3 pos) {}
					public void onAttack() {
						CustomItems.handleCustomItems(null, EquipmentSlot.HAND, player.getInventory().getItemInMainHand(), Action.LEFT_CLICK_AIR, player);
					}
				});
			});
		}
		// Reset vanilla's interact dedupe so repeated clicks on the same block keep firing
		// PlayerInteractEvent. Without this, vanilla's ServerPlayerGameMode.useItemOn caches
		// the (block, hand, item) triple of the previous event and reuses its result for
		// subsequent matching packets — so rapid clicks on the same block only fire one
		// ability and the rest are silently absorbed. TASGamePacketListenerImpl resets at
		// the top of handleUseItemOn for fake players (line 277); real players use vanilla's
		// listener which does not, so we reset here.
		//
		// Scheduled onto the server's executor (not Bukkit.scheduler.runTask) so it lands on
		// the main thread *in the same queue* as the packet handler — both are dequeued in
		// FIFO order, so the reset runs immediately before vanilla processes this packet.
		// Dispatch the right-click ability directly, bypassing whatever vanilla mechanism
		// is suppressing PlayerInteractEvent for repeated same-block clicks. This is wired
		// at the netty layer so every UseItem(On) packet hits CustomItems.handleCustomItems,
		// which has its own 1-tick anti-spam cooldown to dedupe against vanilla's event if
		// it also fires.
		//
		// ServerboundUseItemOnPacket → RIGHT_CLICK_BLOCK
		// ServerboundUseItemPacket   → RIGHT_CLICK_AIR
		// Scheduled on the server executor (same queue as vanilla's packet handler) so it
		// runs on the main thread immediately before vanilla processes the packet.
		if(msg instanceof ServerboundUseItemOnPacket usePkt && usePkt.getHand() == InteractionHand.MAIN_HAND) {
			MinecraftServer.getServer().execute(() ->
					CustomItems.handleCustomItems(null, EquipmentSlot.HAND,
							player.getInventory().getItemInMainHand(), Action.RIGHT_CLICK_BLOCK, player));
		} else if(msg instanceof ServerboundUseItemPacket airPkt && airPkt.getHand() == InteractionHand.MAIN_HAND) {
			MinecraftServer.getServer().execute(() ->
					CustomItems.handleCustomItems(null, EquipmentSlot.HAND,
							player.getInventory().getItemInMainHand(), Action.RIGHT_CLICK_AIR, player));
		}
		super.channelRead(ctx, msg);
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if(msg instanceof ClientboundContainerSetContentPacket pkt && pkt.containerId() == 0) {
			Player fakePlayer = Spectate.getSpectatorMap().get(player);
			if(fakePlayer instanceof CraftPlayer craftFake) {
				ServerPlayer nmsFake = craftFake.getHandle();
				msg = new ClientboundContainerSetContentPacket(
					0, pkt.stateId(),
					nmsFake.containerMenu.getItems(), nmsFake.containerMenu.getCarried()
				);
			}
		}
		super.write(ctx, msg, promise);
	}
}
