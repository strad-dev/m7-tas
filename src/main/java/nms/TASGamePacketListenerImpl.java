package nms;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import commands.Spectate;
import listeners.CustomItems;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.v1_21_R7.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_21_R7.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.slf4j.Logger;
import plugin.Utils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class TASGamePacketListenerImpl extends ServerGamePacketListenerImpl {
	static final Logger LOGGER = LogUtils.getLogger();
	public TASGamePacketListenerImpl(MinecraftServer minecraftserver, Connection networkmanager, ServerPlayer entityplayer, CommonListenerCookie commonlistenercookie) {
		super(minecraftserver, networkmanager, entityplayer, commonlistenercookie);
	}

	@Override
	public void handleAnimate(ServerboundSwingPacket packet) {
		super.handleAnimate(packet);
		Player cp = getCraftPlayer();
		CustomItems.handleCustomItems(null, EquipmentSlot.HAND, cp.getInventory().getItemInMainHand(), Action.LEFT_CLICK_AIR, cp);
	}

	public void handleInteract(ServerboundInteractPacket packetplayinuseentity) {
		super.player.level();
		final ServerLevel worldserver = this.player.level();
		final Entity entity = packetplayinuseentity.getTarget(worldserver);
		if(entity == this.player && !this.player.isSpectator()) {
			this.disconnect(Component.literal("Cannot interact with self!"));
			return;
		}

		this.player.resetLastActionTime();
		this.player.setShiftKeyDown(packetplayinuseentity.isUsingSecondaryAction());
		if(entity != null) {
			if(!worldserver.getWorldBorder().isWithinBounds(entity.blockPosition())) {
				return;
			}

			AABB axisalignedbb = entity.getBoundingBox();
			if(packetplayinuseentity.isWithinRange(this.player, axisalignedbb, 3.0F)) {
				packetplayinuseentity.dispatch(new ServerboundInteractPacket.Handler() {

					private void performInteraction(InteractionHand enumhand, EntityInteraction playerconnection_a, PlayerInteractEntityEvent event) {
						ItemStack itemstack = TASGamePacketListenerImpl.this.player.getItemInHand(enumhand);
						if(itemstack.isItemEnabled(worldserver.enabledFeatures())) {
							ItemStack itemstack1 = itemstack.copy();
							ItemStack itemInHand = TASGamePacketListenerImpl.this.player.getItemInHand(enumhand);
							boolean triggerLeashUpdate = itemInHand != null && itemInHand.getItem() == Items.LEAD && entity instanceof Mob;
							Item origItem = TASGamePacketListenerImpl.this.player.getInventory().getSelectedItem() == null ? null : TASGamePacketListenerImpl.this.player.getInventory().getSelectedItem().getItem();
							TASGamePacketListenerImpl.this.cserver.getPluginManager().callEvent(event);
							if(entity instanceof Bucketable && entity instanceof LivingEntity && origItem != null && origItem.asItem() == Items.WATER_BUCKET && (event.isCancelled() || TASGamePacketListenerImpl.this.player.getInventory().getSelectedItem() == null || TASGamePacketListenerImpl.this.player.getInventory().getSelectedItem().getItem() != origItem)) {
								entity.getBukkitEntity().update(TASGamePacketListenerImpl.this.player);
								TASGamePacketListenerImpl.this.player.containerMenu.sendAllDataToRemote();
							}

							if(triggerLeashUpdate && (event.isCancelled() || TASGamePacketListenerImpl.this.player.getInventory().getSelectedItem() == null || TASGamePacketListenerImpl.this.player.getInventory().getSelectedItem().getItem() != origItem)) {
								TASGamePacketListenerImpl.this.send(new ClientboundSetEntityLinkPacket(entity, ((Mob) entity).getLeashHolder()));
							}

							if(event.isCancelled() || TASGamePacketListenerImpl.this.player.getInventory().getSelectedItem() == null || TASGamePacketListenerImpl.this.player.getInventory().getSelectedItem().getItem() != origItem) {
								entity.refreshEntityData(TASGamePacketListenerImpl.this.player);
								if(entity instanceof Allay) {
									TASGamePacketListenerImpl.this.send(new ClientboundSetEquipmentPacket(entity.getId(), Arrays.stream(net.minecraft.world.entity.EquipmentSlot.values()).map((slot) -> Pair.of(slot, ((LivingEntity) entity).getItemBySlot(slot).copy())).collect(Collectors.toList())));
									TASGamePacketListenerImpl.this.player.containerMenu.sendAllDataToRemote();
								}
							}

							if(event.isCancelled()) {
								return;
							}

							InteractionResult enuminteractionresult = playerconnection_a.run(TASGamePacketListenerImpl.this.player, entity, enumhand);
							if(!itemInHand.isEmpty() && itemInHand.getCount() <= -1) {
								TASGamePacketListenerImpl.this.player.containerMenu.sendAllDataToRemote();
							}

							if(enuminteractionresult instanceof InteractionResult.Success enuminteractionresult_d) {
								ItemStack itemstack2 = enuminteractionresult_d.wasItemInteraction() ? itemstack1 : ItemStack.EMPTY;
								CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY.trigger(TASGamePacketListenerImpl.this.player, itemstack2, entity);
								if(enuminteractionresult_d.swingSource() == InteractionResult.SwingSource.SERVER) {
									TASGamePacketListenerImpl.this.player.swing(enumhand, true);
								}
							}
						}

					}

					public void onInteraction(InteractionHand enumhand) {
						this.performInteraction(enumhand, net.minecraft.world.entity.player.Player::interactOn, new PlayerInteractEntityEvent(TASGamePacketListenerImpl.this.getCraftPlayer(), entity.getBukkitEntity(), enumhand == InteractionHand.OFF_HAND ? org.bukkit.inventory.EquipmentSlot.OFF_HAND : org.bukkit.inventory.EquipmentSlot.HAND));
					}

					public void onInteraction(InteractionHand enumhand, Vec3 vec3d) {
						this.performInteraction(enumhand, (entityplayer, entity1, enumhand1) -> entity1.interactAt(entityplayer, vec3d, enumhand1), new PlayerInteractAtEntityEvent(TASGamePacketListenerImpl.this.getCraftPlayer(), entity.getBukkitEntity(), new Vector(vec3d.x, vec3d.y, vec3d.z), enumhand == InteractionHand.OFF_HAND ? org.bukkit.inventory.EquipmentSlot.OFF_HAND : org.bukkit.inventory.EquipmentSlot.HAND));
					}

					public void onAttack() {
						// Check if target is a spectator of this fake player
						if(entity instanceof ServerPlayer targetNms) {
							Player targetBukkit = targetNms.getBukkitEntity();
							Player attacker = TASGamePacketListenerImpl.this.getCraftPlayer();
							if(Spectate.getSpectatingPlayers(attacker).contains(targetBukkit)) {
								PlayerInteractEvent event = new PlayerInteractEvent(attacker, Action.LEFT_CLICK_AIR, attacker.getInventory().getItemInMainHand(), null, null, org.bukkit.inventory.EquipmentSlot.HAND);
								TASGamePacketListenerImpl.this.cserver.getPluginManager().callEvent(event);
								return;
							}
						}

						if(!(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrb) && (entity != TASGamePacketListenerImpl.this.player || TASGamePacketListenerImpl.this.player.isSpectator())) {
							label43:
							{
								if(entity instanceof AbstractArrow entityarrow) {
									if(!entityarrow.isAttackable()) {
										break label43;
									}
								}

								ItemStack itemstack = TASGamePacketListenerImpl.this.player.getItemInHand(InteractionHand.MAIN_HAND);
								if(!itemstack.isItemEnabled(worldserver.enabledFeatures())) {
									return;
								}

								if(TASGamePacketListenerImpl.this.player.cannotAttackWithItem(itemstack, 5)) {
									return;
								}

								// Suppress attack if a custom ability already fired on the swing this tick
								if(CustomItems.abilityFiredThisTick(TASGamePacketListenerImpl.this.getCraftPlayer())) return;

								// Check if the entity won't produce a damage event
								boolean immune;
								if(entity instanceof LivingEntity living) {
									immune = living.isDeadOrDying() || living.invulnerableTime > 0 || (entity instanceof ServerPlayer target && target.isCreative());
								} else {
									immune = false;
								}

								if(immune) {
									Player attacker = TASGamePacketListenerImpl.this.getCraftPlayer();
									PlayerInteractEvent event = new PlayerInteractEvent(attacker, Action.LEFT_CLICK_AIR, attacker.getInventory().getItemInMainHand(), null, null, org.bukkit.inventory.EquipmentSlot.HAND);
									TASGamePacketListenerImpl.this.cserver.getPluginManager().callEvent(event);
								}

								TASGamePacketListenerImpl.this.player.attack(entity);
								if(!itemstack.isEmpty() && itemstack.getCount() <= -1) {
									TASGamePacketListenerImpl.this.player.containerMenu.sendAllDataToRemote();
								}

								return;
							}
						}

						TASGamePacketListenerImpl.this.disconnect(Component.translatable("multiplayer.disconnect.invalid_entity_attacked"));
						TASGamePacketListenerImpl.LOGGER.warn("Player {} tried to attack an invalid entity", TASGamePacketListenerImpl.this.player.getPlainTextName());
					}
				});
			}
		}
	}

	public void handlePlayerAction(ServerboundPlayerActionPacket packetplayinblockdig) {
		BlockPos blockposition = packetplayinblockdig.getPos();
		this.player.resetLastActionTime();
		ServerboundPlayerActionPacket.Action packetplayinblockdig_enumplayerdigtype = packetplayinblockdig.getAction();
		switch(packetplayinblockdig_enumplayerdigtype) {
			case STAB:
				if(!this.player.isSpectator()) {
					ItemStack itemstack = this.player.getItemInHand(InteractionHand.MAIN_HAND);
					if(this.player.cannotAttackWithItem(itemstack, 5)) {
						return;
					}

					PiercingWeapon piercingweapon = itemstack.get(DataComponents.PIERCING_WEAPON);
					if(piercingweapon != null) {
						piercingweapon.attack(this.player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
					}

				}
				return;
			case SWAP_ITEM_WITH_OFFHAND:
				if(!this.player.isSpectator()) {
					ItemStack itemstack1 = this.player.getItemInHand(InteractionHand.OFF_HAND);
					CraftItemStack mainHand = CraftItemStack.asCraftMirror(itemstack1);
					CraftItemStack offHand = CraftItemStack.asCraftMirror(this.player.getItemInHand(InteractionHand.MAIN_HAND));
					PlayerSwapHandItemsEvent swapItemsEvent = new PlayerSwapHandItemsEvent(this.getCraftPlayer(), mainHand.clone(), offHand.clone());
					this.cserver.getPluginManager().callEvent(swapItemsEvent);
					if(swapItemsEvent.isCancelled()) {
						return;
					}

					if(swapItemsEvent.getOffHandItem().equals(offHand)) {
						this.player.setItemInHand(InteractionHand.OFF_HAND, this.player.getItemInHand(InteractionHand.MAIN_HAND));
					} else {
						this.player.setItemInHand(InteractionHand.OFF_HAND, CraftItemStack.asNMSCopy(swapItemsEvent.getOffHandItem()));
					}

					if(swapItemsEvent.getMainHandItem().equals(mainHand)) {
						this.player.setItemInHand(InteractionHand.MAIN_HAND, itemstack1);
					} else {
						this.player.setItemInHand(InteractionHand.MAIN_HAND, CraftItemStack.asNMSCopy(swapItemsEvent.getMainHandItem()));
					}

					this.player.stopUsingItem();
				}

				return;
			case DROP_ITEM:
				if(!this.player.isSpectator()) {
					CustomItems.handleDrop(this.player.getBukkitEntity(), true);
					this.player.getBukkitEntity().updateInventory();
				}

				return;
			case DROP_ALL_ITEMS:
				if(!this.player.isSpectator()) {
					CustomItems.handleDrop(this.player.getBukkitEntity(), false);
					this.player.getBukkitEntity().updateInventory();
				}

				return;
			case RELEASE_USE_ITEM:
				this.player.releaseUsingItem();
				return;
			case START_DESTROY_BLOCK:
			case ABORT_DESTROY_BLOCK:
			case STOP_DESTROY_BLOCK:
				this.player.gameMode.handleBlockBreakAction(blockposition, packetplayinblockdig_enumplayerdigtype, packetplayinblockdig.getDirection(), this.player.level().getMaxY(), packetplayinblockdig.getSequence());
				return;
			default:
				throw new IllegalArgumentException("Invalid player action");
		}
	}

	public void handleUseItemOn(ServerboundUseItemOnPacket packetplayinuseitem) {
		this.player.gameMode.firedInteract = false;
		ServerLevel worldserver = this.player.level();
		InteractionHand enumhand = packetplayinuseitem.getHand();
		ItemStack itemstack = this.player.getItemInHand(enumhand);
		if(itemstack.isItemEnabled(worldserver.enabledFeatures())) {
			BlockHitResult movingobjectpositionblock = packetplayinuseitem.getHitResult();
			Vec3 vec3d = movingobjectpositionblock.getLocation();
			BlockPos blockposition = movingobjectpositionblock.getBlockPos();
			if(this.player.isWithinBlockInteractionRange(blockposition, 1.0F)) {
				Vec3 vec3d1 = vec3d.subtract(Vec3.atCenterOf(blockposition));
				if(Math.abs(vec3d1.x()) < 1.0000001 && Math.abs(vec3d1.y()) < 1.0000001 && Math.abs(vec3d1.z()) < 1.0000001) {
					Direction enumdirection = movingobjectpositionblock.getDirection();
					this.player.resetLastActionTime();
					int i = this.player.level().getMaxY();
					if(blockposition.getY() <= i) {
						this.player.stopUsingItem();
						InteractionResult enuminteractionresult = this.player.gameMode.useItemOn(this.player, worldserver, itemstack, enumhand, movingobjectpositionblock);
						if(enuminteractionresult == InteractionResult.PASS) {
							ServerboundUseItemPacket packet = new ServerboundUseItemPacket(enumhand, 0, this.player.getYRot(), this.player.getXRot());
							this.handleUseItem(packet);
						} else {
							if(enuminteractionresult.consumesAction()) {
								CriteriaTriggers.ANY_BLOCK_USE.trigger(this.player, movingobjectpositionblock.getBlockPos(), itemstack.copy());
							}

							if(enumdirection == Direction.UP && !enuminteractionresult.consumesAction() && blockposition.getY() >= i && wasBlockPlacementAttempt(this.player, itemstack)) {
								Component ichatbasecomponent = Component.translatable("build.tooHigh", i).withStyle(ChatFormatting.RED);
								this.player.sendSystemMessage(ichatbasecomponent, true);
							} else if(enuminteractionresult instanceof InteractionResult.Success enuminteractionresult_d) {
								if(enuminteractionresult_d.swingSource() == InteractionResult.SwingSource.SERVER) {
									this.player.swing(enumhand, true);
								}
							}
						}
					} else {
						Component ichatbasecomponent1 = Component.translatable("build.tooHigh", i).withStyle(ChatFormatting.RED);
						this.player.sendSystemMessage(ichatbasecomponent1, true);
					}

					this.send(new ClientboundBlockUpdatePacket(worldserver, blockposition));
					this.send(new ClientboundBlockUpdatePacket(worldserver, blockposition.relative(enumdirection)));
				} else {
					LOGGER.warn("Rejecting UseItemOnPacket from {}: Location {} too far away from hit block {}.", this.player.getGameProfile().name(), vec3d, blockposition);
				}
			}
		}
	}

	public void handleUseItem(ServerboundUseItemPacket packetplayinblockplace) {
		this.player.gameMode.firedInteract = false;
		ServerLevel worldserver = this.player.level();
		InteractionHand enumhand = packetplayinblockplace.getHand();
		ItemStack itemstack = this.player.getItemInHand(enumhand);
		this.player.resetLastActionTime();
		if(!itemstack.isEmpty() && itemstack.isItemEnabled(worldserver.enabledFeatures())) {
			float f = Mth.wrapDegrees(packetplayinblockplace.getYRot());
			float f1 = Mth.wrapDegrees(packetplayinblockplace.getXRot());
			if(f1 != this.player.getXRot() || f != this.player.getYRot()) {
				this.player.absSnapRotationTo(f, f1);
			}

			double d0 = this.player.getX();
			double d1 = this.player.getY() + (double) this.player.getEyeHeight();
			double d2 = this.player.getZ();
			Vec3 vec3d = new Vec3(d0, d1, d2);
			float f3 = Mth.cos(-f * ((float) Math.PI / 180F) - (float) Math.PI);
			float f4 = Mth.sin(-f * ((float) Math.PI / 180F) - (float) Math.PI);
			float f5 = -Mth.cos(-f1 * ((float) Math.PI / 180F));
			float f6 = Mth.sin(-f1 * ((float) Math.PI / 180F));
			float f7 = f4 * f5;
			float f8 = f3 * f5;
			double d3 = this.player.blockInteractionRange();
			Vec3 vec3d1 = vec3d.add((double) f7 * d3, (double) f6 * d3, (double) f8 * d3);
			BlockHitResult movingobjectposition = this.player.level().clip(new ClipContext(vec3d, vec3d1, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this.player));
			boolean cancelled;
			if(movingobjectposition != null && movingobjectposition.getType() == HitResult.Type.BLOCK) {
				if(this.player.gameMode.firedInteract && this.player.gameMode.interactPosition.equals(movingobjectposition.getBlockPos()) && this.player.gameMode.interactHand == enumhand && ItemStack.isSameItemSameComponents(this.player.gameMode.interactItemStack, itemstack)) {
					cancelled = this.player.gameMode.interactResult;
				} else {
					PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(this.player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, movingobjectposition.getBlockPos(), movingobjectposition.getDirection(), itemstack, true, enumhand, movingobjectposition.getLocation());
					cancelled = event.useItemInHand() == Event.Result.DENY;
				}

				this.player.gameMode.firedInteract = false;
			} else {
				PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(this.player, org.bukkit.event.block.Action.RIGHT_CLICK_AIR, itemstack, enumhand);
				cancelled = event.useItemInHand() == Event.Result.DENY;
			}

			if(cancelled) {
				this.player.stopUsingItem(); // prevent lingering bow-draw state
				this.player.containerMenu.sendAllDataToRemote();
				return;
			}

			itemstack = this.player.getItemInHand(enumhand);
			if(itemstack.isEmpty()) {
				return;
			}

			InteractionResult enuminteractionresult = this.player.gameMode.useItem(this.player, worldserver, itemstack, enumhand);
			if(enuminteractionresult instanceof InteractionResult.Success enuminteractionresult_d) {
				if(enuminteractionresult_d.swingSource() == InteractionResult.SwingSource.SERVER) {
					this.player.swing(enumhand, true);
				}
			}
		}
	}

	private static boolean wasBlockPlacementAttempt(ServerPlayer entityplayer, ItemStack itemstack) {
		if(!itemstack.isEmpty()) {
			label21:
			{
				Item item = itemstack.getItem();
				if(!(item instanceof BlockItem)) {
					if(!(item instanceof BucketItem itembucket)) {
						break label21;
					}

					if(itembucket.getContent() == Fluids.EMPTY) {
						break label21;
					}
				}

				if(!entityplayer.getCooldowns().isOnCooldown(itemstack)) {
					return true;
				}
			}

		}
		return false;
	}

	@FunctionalInterface
	private interface EntityInteraction {
		InteractionResult run(ServerPlayer var1, Entity var2, InteractionHand var3);
	}
}