package nms;

import commands.Spectate;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import listeners.CustomItems;
import listeners.GoldorListener;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
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
			// Drop-key ability triggers (Q / Ctrl+Q) and bow release. For DROP we let vanilla process the packet
			// (super.channelRead below): CustomItems' PlayerDropItemEvent handler cancels the physical drop for
			// class players, so the item is kept while handleDrop fires the ability. For a bow RELEASE we consume
			// the packet (return) and release the draw manually with creative-mode instabuild so it fires without
			// consuming a real arrow.
			var action = pkt.getAction();
			if(action == ServerboundPlayerActionPacket.Action.DROP_ITEM) {
				Bukkit.getScheduler().runTask(M7tas.getInstance(), () -> {
					CustomItems.handleDrop(player, true);
					player.updateInventory();
				});
			} else if(action == ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS) {
				Bukkit.getScheduler().runTask(M7tas.getInstance(), () -> {
					CustomItems.handleDrop(player, false);
					player.updateInventory();
				});
			} else if(action == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) {
				String heldId = CustomItems.getID(player.getInventory().getItemInMainHand());
				if("skyblock/combat/last_breath".equals(heldId) || "skyblock/combat/explosive_bow".equals(heldId)) {
					Bukkit.getScheduler().runTask(M7tas.getInstance(), () -> {
						ServerPlayer sp = ((CraftPlayer) player).getHandle();
						boolean was = sp.getAbilities().instabuild;
						sp.getAbilities().instabuild = true;
						sp.releaseUsingItem();
						sp.getAbilities().instabuild = was;
					});
					return;
				}
			} else if(action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
				// A left-click ON A BLOCK.  This is the ONLY place the client tells us which block a left-click hit,
				// since the swing packet carries no position, so block abilities (Superboom TNT) take their target
				// from here instead of the server ray-tracing its own reach.  Dispatched as LEFT_CLICK_BLOCK carrying
				// the packet's pos.  The block itself is still never broken: handleCustomItems cancels the follow-up
				// PlayerInteractEvent for left-click ability items, and onBlockBreak refuses outright as a backstop.
				//
				// Note that this only arrives for items stamped can_break (Dungeonbreaker, and Superboom TNT via
				// Utils.placeAndBreakAnythingInAdventure) while the player is in adventure mode.  Verified in the 26.2
				// client: MultiPlayerGameMode.startDestroyBlock returns early when Player.blockActionRestricted is
				// true.  In survival every item sends it.  Either way vanilla's own PlayerInteractEvent fires for the
				// same packet, so both dispatches land on the same tick and collapse via the 1/tick ability guards.
				//
				// On the server executor (same main-thread FIFO queue as vanilla's handler, drained before
				// super.channelRead's packet), matching the UseItemOn path below.
				net.minecraft.core.BlockPos bp = pkt.getPos();
				int bx = bp.getX(), by = bp.getY(), bz = bp.getZ();
				MinecraftServer.getServer().execute(() ->
					CustomItems.handleCustomItems(null, EquipmentSlot.HAND, player.getInventory().getItemInMainHand(),
							Action.LEFT_CLICK_BLOCK, player, player.getWorld().getBlockAt(bx, by, bz)));
			}
		}
		if(msg instanceof ServerboundAttackPacket) {
			// 26.2 split melee attacks into their own ServerboundAttackPacket (ServerboundInteractPacket is now
			// interact / interact-at only). Dispatch the LEFT_CLICK_AIR ability path for every attack:
			// EntityDamageByEntityEvent only fires when damage actually lands, which excludes shield-invulnerable
			// withers, dying entities, and other no-damage cases, so abilities like the mage beam need this path.
			// This is what fires the beam when a mob is in melee range, where no PlayerInteractEvent fires.  The
			// same-tick cooldown in handleCustomItems (lastLeftClickAbilityTick) dedupes against the EDBEE dispatch
			// when damage does land.
			Bukkit.getScheduler().runTask(M7tas.getInstance(), () ->
				CustomItems.handleCustomItems(null, EquipmentSlot.HAND, player.getInventory().getItemInMainHand(), Action.LEFT_CLICK_AIR, player));
		}
		// Reset vanilla's interact dedupe so repeated clicks on the same block keep firing
		// PlayerInteractEvent.  Without this, vanilla's ServerPlayerGameMode.useItemOn caches
		// the (block, hand, item) triple of the previous event and reuses its result for
		// subsequent matching packets, so rapid clicks on the same block only fire one
		// ability and the rest are silently absorbed.  TASGamePacketListenerImpl resets at
		// the top of handleUseItemOn for fake players (line 277); real players use vanilla's
		// listener, which does not, so I reset here.
		//
		// Scheduled onto the server's executor rather than Bukkit.scheduler.runTask so it lands
		// on the main thread *in the same queue* as the packet handler.  Both are dequeued in
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
			net.minecraft.core.BlockPos bp = usePkt.getHitResult().getBlockPos();
			int bx = bp.getX(), by = bp.getY(), bz = bp.getZ();
			MinecraftServer.getServer().execute(() -> {
				org.bukkit.Material clicked = player.getWorld().getBlockAt(bx, by, bz).getType();
				// Reset vanilla's interact dedupe so rapid repeat clicks on the same block keep firing
				// PlayerInteractEvent, e.g. the Simon Says button, instead of reusing the cached
				// (block,hand,item) result.  Fake players do this in TASGamePacketListenerImpl#handleUseItemOn;
				// real players use the vanilla listener, which doesn't, so reset it here, before vanilla
				// processes the packet, in the same main-thread FIFO queue as super.channelRead below.
				//
				// EXCEPTION: never reset it for a LEVER.  A single physical right-click sends the UseItemOn packet
				// more than once; vanilla's dedupe collapses the duplicates into one toggle, but resetting it defeats
				// that and toggles the lever twice (flip → unflip).  That's invisible on the section levers, whose
				// solve tracks a boolean set on the first interact rather than the block state, but it breaks the S2
				// "Lights" device, which reads the physical lamp state, so the lever appeared to "undo" itself.
				// Buttons still get the reset so rapid Simon clicks each register.
				if(clicked != org.bukkit.Material.LEVER) {
					((CraftPlayer) player).getHandle().gameMode.firedInteract = false;
				}
				// Count Simon Says button clicks straight from the packet, bypassing vanilla's interact-event
				// suppression so rapid real-player clicks all register (deduped to one per tick in GoldorListener).
				GoldorListener.tryRegisterSimonClick(player, bx, by, bz);
				// Right-clicking a lever or button owns the click, so the held item's right-click ability must NOT
				// fire on top of it.  This mirrors the guard in CustomItems.onPlayerInteract, which only covers the
				// Bukkit event path; this interceptor path had no such guard, so a combat or utility item's
				// right-click was firing on an S2 lever and hijacking the vanilla toggle.

				org.bukkit.block.Block clickedBlock = player.getWorld().getBlockAt(bx, by, bz);
				if(clicked != org.bukkit.Material.LEVER && !org.bukkit.Tag.BUTTONS.isTagged(clicked)
						&& !instructions.clear.ClearManager.isSecretBlock(clickedBlock)) {
					// Pass vanilla's own hit-result block through: block abilities (Superboom TNT) centre on it rather
					// than ray-tracing a reach of their own, so they match vanilla's interaction range and target.
					CustomItems.handleCustomItems(null, EquipmentSlot.HAND,
							player.getInventory().getItemInMainHand(), Action.RIGHT_CLICK_BLOCK, player, clickedBlock);
				}
			});
		} else if(msg instanceof ServerboundUseItemPacket airPkt && airPkt.getHand() == InteractionHand.MAIN_HAND) {
			MinecraftServer.getServer().execute(() -> {
				((CraftPlayer) player).getHandle().gameMode.firedInteract = false;
				CustomItems.handleCustomItems(null, EquipmentSlot.HAND,
						player.getInventory().getItemInMainHand(), Action.RIGHT_CLICK_AIR, player);
			});
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
