package listeners;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import commands.Spectate;
import instructions.Actions;
import instructions.Server;
import instructions.bosses.goldor.Goldor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.profile.CraftPlayerProfile;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import plugin.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CustomItems implements Listener {
	private static final Map<UUID, Integer> cooldowns = new ConcurrentHashMap<>();
	// Tick of the last RIGHT_CLICK_BLOCK dispatch per player. A physical right-click on a block sends UseItemOn
	// (RIGHT_CLICK_BLOCK) immediately followed by UseItem (RIGHT_CLICK_AIR); this lets the right-click handler drop
	// the trailing AIR so the ability fires once even when the pair straddles a tick boundary (see handleCustomItems).
	private static final Map<UUID, Integer> lastRightBlockTick = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> lastLeftClickAbilityTick = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> lastWitherShieldSoundTick = new ConcurrentHashMap<>();
	// True while mageBeam's hurtEntity call is on the stack — damage events fire synchronously, so
	// onWitherHurtSound reads this to skip its at-location broadcast for beam hits (package-private).
	static boolean beamDamageInProgress = false;
	private static final Set<UUID> droppingPlayers = new HashSet<>();
	// Berserk-exclusive damage ramp: each successive hit on the SAME mob deals +10% damage, capped at 3x (+200%).
	// Keyed player → (mob → number of prior hits). Different terminator arrows from one shot land as separate hits,
	// so each ramps further. Reset at the start of every /tas and /practice run (CustomItems.resetBerserkDamage()).
	private static final Map<UUID, Map<UUID, Integer>> berserkHitCounts = new HashMap<>();
	// Terminator firing is poller-driven (NOT fired directly on the right-click packet). A right-click records the
	// packet tick; pollTerminators() fires on the first tick where a new packet exists AND the cooldown has elapsed
	// (5 ticks, or 4 with 4/4 Thermodynamic armor). This caps the rate at 1 shot / cooldown regardless of spam.
	private static final Map<UUID, Integer> termLastPacketTick = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> termLastFireTick = new ConcurrentHashMap<>();
	// Class-ability (drop-triggered) cooldowns — store the tick each ability is next usable. Reset on entering a
	// boss fight (WitherLord.start) and at run start. Guided Sheep 600t, Rapid Fire 2000t, Explosive Shot 400t.
	private static final Map<UUID, Integer> guidedSheepReady = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> rapidFireReady = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> explosiveShotReady = new ConcurrentHashMap<>();
	// Salvation (Terminator left-click) cooldown — stores the tick the next Salvation beam is usable. The shared
	// left-click guard only caps to 1/tick; this enforces the ability's own 5-tick cooldown. NOTE: the Terminator
	// and Salvation are weapons, NOT abilities, so they are deliberately exempt from the Mage cooldown reduction.
	private static final int SALVATION_COOLDOWN_TICKS = 5;
	private static final Map<UUID, Integer> salvationReady = new ConcurrentHashMap<>();
	// Mage beam (Mage-class left-click) cooldown — the shared left-click guard only caps to 1/tick; this enforces
	// the beam's own 5-tick cooldown (fired on tick N → next usable on tick N+5).
	private static final int MAGE_BEAM_COOLDOWN_TICKS = 5;
	private static final Map<UUID, Integer> mageBeamReady = new ConcurrentHashMap<>();
	// Per-ability cooldowns (base ticks before the Mage class's 50% reduction — see effectiveCooldown). Each map
	// stores the tick that ability is next usable, keyed by player. Reset in resetAbilityCooldowns().
	private static final int GYRO_COOLDOWN_TICKS = 600;       // Gyrokinetic Wand: 30s
	private static final int RAG_COOLDOWN_TICKS = 400;        // Ragnarok Axe: 20s
	private static final int ICE_SPRAY_COOLDOWN_TICKS = 100;  // Ice Spray Wand: 5s
	private static final int TAC_COOLDOWN_TICKS = 400;        // Tactical Insertion: 20s
	private static final int GUIDED_SHEEP_COOLDOWN_TICKS = 600; // Guided Sheep: 30s
	private static final Map<UUID, Integer> gyroReady = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> ragReady = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> iceSprayReady = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> tacReady = new ConcurrentHashMap<>();
	// Server tick the RagBuff currently expires at, per player. Each cast's buff lands at +60 and lasts 200 ticks
	// (10s); a re-cast while still active pushes this out so the earlier cast's removal no-ops (see rag()).
	private static final Map<UUID, Integer> ragBuffExpiry = new ConcurrentHashMap<>();
	public static final Map<Location, BlockData> pendingStonkRestorations = new HashMap<>();
	public static final Map<Location, BukkitTask> pendingStonkTasks = new HashMap<>();
	// Crypt + Superboom-wall restorations. Mirrors the stonk maps above: a crypt/wall is temporarily set to AIR and
	// restored after 40 ticks via a raw scheduler task (NOT Utils.scheduleTask), so /reset and /setup can flush them
	// immediately via flushBlockRestorations(). Using Utils.scheduleTask here would let Reset's cancelAllScheduled()
	// kill the pending restoration, leaving permanent AIR holes and orphaned crypt mobs.
	private static final Map<Location, BlockData> pendingBlockRestorations = new HashMap<>();
	private static final List<BukkitTask> pendingBlockTasks = new ArrayList<>();
	private static final List<Zombie> pendingCryptMobs = new ArrayList<>();

	public static boolean abilityFiredThisTick(Player p) {
		return lastLeftClickAbilityTick.getOrDefault(p.getUniqueId(), -1) == MinecraftServer.currentTick;
	}

	public static String getID(ItemStack item) {
		if(item == null || !item.hasItemMeta()) {
			return "";
		} else if(!item.getItemMeta().hasLore()) {
			return "";
		} else return Utils.firstLorePlain(item.getItemMeta());
	}

	public static List<EntityType> doNotKill() {
		List<EntityType> doNotKill = new ArrayList<>();
		doNotKill.add(EntityType.ACACIA_BOAT);
		doNotKill.add(EntityType.ACACIA_CHEST_BOAT);
		doNotKill.add(EntityType.ALLAY);
		doNotKill.add(EntityType.ARMOR_STAND);
		doNotKill.add(EntityType.ARROW);
		doNotKill.add(EntityType.AXOLOTL);
		doNotKill.add(EntityType.BLOCK_DISPLAY);
		doNotKill.add(EntityType.BIRCH_BOAT);
		doNotKill.add(EntityType.BIRCH_CHEST_BOAT);
		doNotKill.add(EntityType.CAT);
		doNotKill.add(EntityType.CHERRY_BOAT);
		doNotKill.add(EntityType.CHERRY_CHEST_BOAT);
		doNotKill.add(EntityType.CHEST_MINECART);
		doNotKill.add(EntityType.COMMAND_BLOCK_MINECART);
		doNotKill.add(EntityType.DARK_OAK_BOAT);
		doNotKill.add(EntityType.DARK_OAK_CHEST_BOAT);
		doNotKill.add(EntityType.DONKEY);
		doNotKill.add(EntityType.DRAGON_FIREBALL);
		doNotKill.add(EntityType.FIREBALL);
		doNotKill.add(EntityType.EGG);
		doNotKill.add(EntityType.ENDER_PEARL);
		doNotKill.add(EntityType.EXPERIENCE_BOTTLE);
		doNotKill.add(EntityType.EXPERIENCE_ORB);
		doNotKill.add(EntityType.FALLING_BLOCK);
		doNotKill.add(EntityType.FIREWORK_ROCKET);
		doNotKill.add(EntityType.FISHING_BOBBER);
		doNotKill.add(EntityType.FURNACE_MINECART);
		doNotKill.add(EntityType.GLOW_ITEM_FRAME);
		doNotKill.add(EntityType.HOPPER_MINECART);
		doNotKill.add(EntityType.HORSE);
		doNotKill.add(EntityType.ITEM_FRAME);
		doNotKill.add(EntityType.ITEM_DISPLAY);
		doNotKill.add(EntityType.INTERACTION);
		doNotKill.add(EntityType.JUNGLE_BOAT);
		doNotKill.add(EntityType.JUNGLE_CHEST_BOAT);
		doNotKill.add(EntityType.LEASH_KNOT);
		doNotKill.add(EntityType.LIGHTNING_BOLT);
		doNotKill.add(EntityType.LLAMA);
		doNotKill.add(EntityType.LLAMA_SPIT);
		doNotKill.add(EntityType.MANGROVE_BOAT);
		doNotKill.add(EntityType.MANGROVE_CHEST_BOAT);
		doNotKill.add(EntityType.MARKER);
		doNotKill.add(EntityType.MINECART);
		doNotKill.add(EntityType.MULE);
		doNotKill.add(EntityType.OAK_BOAT);
		doNotKill.add(EntityType.OAK_CHEST_BOAT);
		doNotKill.add(EntityType.OCELOT);
		doNotKill.add(EntityType.PAINTING);
		doNotKill.add(EntityType.PARROT);
		doNotKill.add(EntityType.SHULKER_BULLET);
		doNotKill.add(EntityType.SKELETON_HORSE);
		doNotKill.add(EntityType.SMALL_FIREBALL);
		doNotKill.add(EntityType.SNOWBALL);
		doNotKill.add(EntityType.SPAWNER_MINECART);
		doNotKill.add(EntityType.SPECTRAL_ARROW);
		doNotKill.add(EntityType.SPRUCE_BOAT);
		doNotKill.add(EntityType.SPRUCE_CHEST_BOAT);
		doNotKill.add(EntityType.TEXT_DISPLAY);
		doNotKill.add(EntityType.TNT);
		doNotKill.add(EntityType.TRIDENT);
		doNotKill.add(EntityType.UNKNOWN);
		doNotKill.add(EntityType.VILLAGER);
		doNotKill.add(EntityType.WITHER_SKULL);
		doNotKill.add(EntityType.WOLF);
		return doNotKill;
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		// Right-clicking a button or a lever owns the click — the held item's right-click ability must not fire.
		// Skip custom-item handling entirely so we also don't cancel the event (the block still actuates).
		if(e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null
				&& (e.getClickedBlock().getType() == Material.LEVER || Tag.BUTTONS.isTagged(e.getClickedBlock().getType()))) {
			return;
		}
		handleCustomItems(e, e.getHand(), e.getItem(), e.getAction(), e.getPlayer());
	}

	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
		// Cancel entity interaction for custom items to prevent side effects (e.g. bow drawing)
		// The ability itself fires from PlayerInteractAtEntityEvent (real clients) or UseItemPacket (fake players)
		// Mirror the right-click exemption list at handleCustomItems so items without right-click
		// abilities (Dungeonbreaker/stonk, gyro, last_breath) can still interact with entities like
		// item frames normally.
		String id = getID(e.getPlayer().getInventory().getItemInMainHand());
		if(!id.isEmpty() && !id.equals("skyblock/combat/gyro") && !id.equals("skyblock/combat/dungeonbreaker") && !id.equals("skyblock/combat/stonk") && !id.equals("skyblock/combat/last_breath") && !id.equals("skyblock/combat/explosive_bow")) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e) {
		// Right-clicking an item frame or an interaction entity must not fire the held item's right-click ability.
		if(e.getRightClicked() instanceof ItemFrame || e.getRightClicked() instanceof Interaction) return;
		handleCustomItems(e, e.getHand(), e.getPlayer().getInventory().getItemInMainHand(), Action.RIGHT_CLICK_AIR, e.getPlayer());
	}

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if(e.getEntity() instanceof LivingEntity entity && !entity.getScoreboardTags().contains("TASNoName")) {
			Utils.scheduleTask(() -> Utils.changeName(entity), 1);
		}
		// Ability dispatch for real-player melee hits is handled by PlayerPacketInterceptor
		// (fires for every attack packet, including no-damage cases like iframe/dying mobs).
		// Routing EDBEE through handleCustomItems caused double-fire because the interceptor's
		// runTask landed on tick T+1 while EDBEE fired on tick T, bypassing the same-tick dedupe.
		//
		// Still suppress vanilla melee damage for Mage-class iron/stone swords so the target
		// only takes mage-beam damage, not vanilla sword damage on top of it.
		if(e.getDamager() instanceof Player p) {
			ItemStack held = p.getInventory().getItemInMainHand();
			boolean isMage = p.getName().startsWith("Mage") || p.getScoreboardTags().contains("Mage");
			if(isMage && (held.getType() == Material.IRON_SWORD || held.getType() == Material.STONE_SWORD)) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		// Protected Goldor interactables are unbreakable outright — any tool (stonk, dungeonbreaker, …), any phase.
		if(Goldor.INSTANCE.isProtected(e.getBlock())) {
			e.setCancelled(true);
			return;
		}
		if(getID(e.getPlayer().getInventory().getItemInMainHand()).equals("skyblock/combat/stonk")) {
			stonk(e.getPlayer(), e.getBlock());
		}
	}

	@EventHandler
	public void onPlayerAnimation(PlayerAnimationEvent e) {
		Player p = e.getPlayer();
		if(e.getAnimationType().equals(PlayerAnimationType.ARM_SWING) && FakePlayerManager.getFakePlayers().containsValue(p) && Spectate.getReverseSpectatorMap().containsKey(p)) {
			for(Player spectator : Spectate.getReverseSpectatorMap().get(p)) {
				spectator.swingMainHand();
			}
		}
	}

	@EventHandler
	public void onPlayerDropItem(PlayerDropItemEvent e) {
		Player p = e.getPlayer();
		droppingPlayers.add(p.getUniqueId());
		Utils.scheduleTask(() -> droppingPlayers.remove(p.getUniqueId()), 1);
		boolean ultimate = !p.isSprinting();
		boolean isClassPlayer = p.getName().equals("Archer") || p.getScoreboardTags().contains("Archer") || p.getName().startsWith("Mage") || p.getScoreboardTags().contains("Mage");
		if(!isClassPlayer) return;
		e.setCancelled(true);
		if(!FakePlayerManager.getFakePlayers().containsValue(p)) return;
		dispatchDrop(p, ultimate);
	}

	public static boolean handleDrop(Player p, boolean ultimate) {
		if(Spectate.getSpectatorMap().containsKey(p)) return false;
		droppingPlayers.add(p.getUniqueId());
		Utils.scheduleTask(() -> droppingPlayers.remove(p.getUniqueId()), 1);
		return dispatchDrop(p, ultimate);
	}

	private static boolean dispatchDrop(Player p, boolean ultimate) {
		int now = MinecraftServer.currentTick;
		UUID id = p.getUniqueId();
		if(p.getName().equals("Archer") || p.getScoreboardTags().contains("Archer")) {
			if(ultimate) {
				if(now < rapidFireReady.getOrDefault(id, 0)) { sendCooldownMessage(p, rapidFireReady.getOrDefault(id, now) - now); return false; } // Rapid Fire: 100s
				rapidFireReady.put(id, now + 2000);
				rapidFire(p);
			} else {
				if(now < explosiveShotReady.getOrDefault(id, 0)) { sendCooldownMessage(p, explosiveShotReady.getOrDefault(id, now) - now); return false; } // Explosive Shot: 20s
				explosiveShotReady.put(id, now + 400);
				explosiveShot(p);
			}
			return true;
		} else if(p.getName().startsWith("Mage") || p.getScoreboardTags().contains("Mage")) {
			if(!ultimate) {
				if(now < guidedSheepReady.getOrDefault(id, 0)) { sendCooldownMessage(p, guidedSheepReady.getOrDefault(id, now) - now); return false; } // Guided Sheep: 30s
				guidedSheepReady.put(id, now + effectiveCooldown(p, GUIDED_SHEEP_COOLDOWN_TICKS));
				guidedSheep(p);
			}
			return true;
		}
		return false;
	}

	@EventHandler
	public void onEntityShootBow(EntityShootBowEvent e) {
		if(e.getEntity() instanceof Player p) {
			String id = getID(p.getInventory().getItemInMainHand());
			if(id.equals("skyblock/combat/explosive_bow")) {
				if(e.getProjectile() instanceof Arrow primary) {
					// Re-aim the vanilla primary IN PLACE (no cancel, no second entity) — just override its velocity
					// with the clean eye direction to strip the random spread (inaccuracy 1.0). The bonus arrows are
					// new entities, so they go through the deterministic spawner. aimFrom/speed captured once so the
					// staggered bonus arrows spawn at the same point/direction (matching the old launchLoc/velocity).
					Location aimFrom = p.getEyeLocation().clone();
					float speed = (float) primary.getVelocity().length();
					primary.setVelocity(aimFrom.getDirection().multiply(speed));
					primary.setDamage(1.0);
					primary.addScoreboardTag("ExplosiveBowArrow");
					primary.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
					boolean isArcher = p.getName().contains("Archer") || p.getScoreboardTags().contains("Archer");

					Utils.scheduleTask(() -> {
						Actions.fireDeterministicArrow(p, aimFrom, speed, 1.0).addScoreboardTag("ExplosiveBowArrow");
						p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F);
					}, 3);

					if(isArcher) {
						Utils.scheduleTask(() -> {
							Actions.fireDeterministicArrow(p, aimFrom, speed, 1.0).addScoreboardTag("ExplosiveBowArrow");
							p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.2F);
						}, 5);

						Utils.scheduleTask(() -> {
							Actions.fireDeterministicArrow(p, aimFrom, speed, 1.0).addScoreboardTag("ExplosiveBowArrow");
							p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.2F);
						}, 10);
					}
				}
				return;
			}
			if(id.equals("skyblock/combat/last_breath")) {
				if(!(e.getProjectile() instanceof Arrow primary)) return;
				// Re-aim the vanilla primary IN PLACE (no cancel, no second entity) — override its velocity with the
				// clean eye direction to strip the random spread. Bonus arrows are new entities → deterministic
				// spawner. aimFrom/speed captured once so the staggered bonus arrows spawn at the same point/dir.
				Location aimFrom = p.getEyeLocation().clone();
				float speed = (float) primary.getVelocity().length();
				boolean isArcher = p.getName().contains("Archer") || p.getScoreboardTags().contains("Archer");

				primary.setVelocity(aimFrom.getDirection().multiply(speed));
				primary.setDamage(isArcher ? 8.0 : 1.5);
				primary.addScoreboardTag("TerminatorArrow");
				primary.addScoreboardTag("LastBreathArrow");
				primary.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);

				Utils.scheduleTask(() -> {
					Arrow a = Actions.fireDeterministicArrow(p, aimFrom, speed, isArcher ? 1.6 : 0.3);
					a.addScoreboardTag("TerminatorArrow");
					a.addScoreboardTag("LastBreathArrow");
					p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F);
				}, 3);

				if(isArcher) {
					Utils.scheduleTask(() -> {
						Arrow a = Actions.fireDeterministicArrow(p, aimFrom, speed, 8.0);
						a.addScoreboardTag("TerminatorArrow");
						a.addScoreboardTag("LastBreathArrow");
						p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.2F);
					}, 5);

					Utils.scheduleTask(() -> {
						Arrow a = Actions.fireDeterministicArrow(p, aimFrom, speed, 8.0);
						a.addScoreboardTag("TerminatorArrow");
						a.addScoreboardTag("LastBreathArrow");
						p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.2F);
					}, 10);
				}
			}
		}
	}

	public static void handleCustomItems(Cancellable e, EquipmentSlot hand, ItemStack item, Action action, Player p) {
		if(Spectate.getSpectatorMap().containsKey(p)) return;
		if(action == Action.LEFT_CLICK_AIR && droppingPlayers.contains(p.getUniqueId())) return;
		boolean fired = false;
		if(Objects.equals(hand, EquipmentSlot.HAND)) {
			String id = getID(item);
			if(item != null && id.startsWith("skyblock/")) {
				// Cancel early for right-clicks to prevent vanilla item use (bow drawing, etc.)
				// Skip for items without right-click abilities
				boolean isRightClick = action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK);
				if(e != null && isRightClick && !id.equals("skyblock/combat/gyro") && !id.equals("skyblock/combat/dungeonbreaker") && !id.equals("skyblock/combat/stonk")) {
					e.setCancelled(true);
				}
				if(action.equals(Action.LEFT_CLICK_AIR) || action.equals(Action.LEFT_CLICK_BLOCK)) {
					boolean isMageBeamItem = (item.getType() == Material.IRON_SWORD || item.getType() == Material.STONE_SWORD) && (p.getName().startsWith("Mage") || p.getScoreboardTags().contains("Mage"));
					// Items whose left-click is an ability are weapons/wands, never pickaxes — their left-click must
					// NEVER break a block, even when the ability is on cooldown or capped by the 1/tick guard.
					boolean leftClickAbilityItem = isMageBeamItem || id.equals("skyblock/combat/terminator") || id.equals("skyblock/combat/gyro") || id.equals("skyblock/combat/infinityboom");
					int currentTick = MinecraftServer.currentTick;
					if(currentTick > lastLeftClickAbilityTick.getOrDefault(p.getUniqueId(), -1)) {
						if(isMageBeamItem) {
							if(currentTick >= mageBeamReady.getOrDefault(p.getUniqueId(), 0)) {
								mageBeam(p);
								mageBeamReady.put(p.getUniqueId(), currentTick + MAGE_BEAM_COOLDOWN_TICKS);
								fired = true;
							}
						} else {
							switch(id) {
								case "skyblock/combat/terminator" -> {
									// Left-click ALSO fires the terminator bow, on the terminator's own cooldown (not the
									// salvation cooldown). Just record the packet tick like the right-click path does;
									// pollTerminators() enforces the 5/4-tick cooldown and fires at most one volley per player
									// per tick, so a left- and right-click landing on the same tick collapse into one shot.
									termLastPacketTick.put(p.getUniqueId(), currentTick);
									if(currentTick >= salvationReady.getOrDefault(p.getUniqueId(), 0)) {
										salvation(p);
										salvationReady.put(p.getUniqueId(), currentTick + SALVATION_COOLDOWN_TICKS);
										fired = true;
									}
								}
								case "skyblock/combat/gyro" -> {
									UUID uid = p.getUniqueId();
									if(currentTick >= gyroReady.getOrDefault(uid, 0)) {
										gyro(p);
										gyroReady.put(uid, currentTick + effectiveCooldown(p, GYRO_COOLDOWN_TICKS));
										fired = true;
									} else {
										sendCooldownMessage(p, gyroReady.getOrDefault(uid, currentTick) - currentTick);
									}
								}
								case "skyblock/combat/infinityboom" -> {
									superboom(p);
									fired = true;
								}
							}
						}
						if(fired) {
							lastLeftClickAbilityTick.put(p.getUniqueId(), currentTick);
						}
					}
					// Suppress vanilla block-breaking for these ability items regardless of fire/cooldown state.
					if(e != null && leftClickAbilityItem) e.setCancelled(true);
				}
				if(isRightClick) {
					int currentTick = MinecraftServer.currentTick;
					// A single physical right-click on a block sends UseItemOn (RIGHT_CLICK_BLOCK) immediately
					// followed by UseItem (RIGHT_CLICK_AIR). These normally land on the same tick and collapse via
					// the 1/tick `cooldowns` gate below, but the first click after a server restart can straddle a
					// tick boundary (one-time warmup lag) — then the trailing AIR fires the ability a second time.
					// Drop an AIR that trails a BLOCK click by at most one tick. Genuine standalone air-clicks carry
					// no recent BLOCK so they still fire, and fake-player air-spam is unaffected (it sends no BLOCK).
					if(action.equals(Action.RIGHT_CLICK_BLOCK)) {
						lastRightBlockTick.put(p.getUniqueId(), currentTick);
					} else {
						int blockTick = lastRightBlockTick.getOrDefault(p.getUniqueId(), Integer.MIN_VALUE);
						if(currentTick == blockTick || currentTick == blockTick + 1) return;
					}
					if(currentTick >= cooldowns.getOrDefault(p.getUniqueId(), 0)) {
						cooldowns.put(p.getUniqueId(), currentTick + 1);
						switch(id) {
							case "skyblock/combat/scylla" -> {
								witherImpact(p);
								fired = true;
							}
							case "skyblock/combat/aotv" -> {
								aotv(p);
								fired = true;
							}
							case "skyblock/combat/infinityboom" -> {
								superboom(p);
								fired = true;
							}
							case "skyblock/combat/rag" -> {
								UUID uid = p.getUniqueId();
								if(currentTick >= ragReady.getOrDefault(uid, 0)) {
									rag(p);
									ragReady.put(uid, currentTick + effectiveCooldown(p, RAG_COOLDOWN_TICKS));
									fired = true;
								} else {
									sendCooldownMessage(p, ragReady.getOrDefault(uid, currentTick) - currentTick);
								}
							}
							case "skyblock/combat/aots" -> {
								aots(p);
								fired = true;
							}
							case "skyblock/combat/ice_spray" -> {
								UUID uid = p.getUniqueId();
								if(currentTick >= iceSprayReady.getOrDefault(uid, 0)) {
									iceSpray(p);
									iceSprayReady.put(uid, currentTick + effectiveCooldown(p, ICE_SPRAY_COOLDOWN_TICKS));
									fired = true;
								} else {
									sendCooldownMessage(p, iceSprayReady.getOrDefault(uid, currentTick) - currentTick);
								}
							}
							case "skyblock/combat/flaming_flay" -> {
								flamingFlay(p);
								fired = true;
							}
							case "skyblock/combat/bonzo" -> {
								bonzo(p);
								fired = true;
							}
							case "skyblock/combat/terminator" -> {
								// Don't fire here — just record the right-click. pollTerminators() decides whether a
								// shot fires this/next tick based on the 5-tick (or Thermo 4-tick) cooldown.
								termLastPacketTick.put(p.getUniqueId(), MinecraftServer.currentTick);
								fired = true;
							}
							case "skyblock/combat/tac" -> {
								UUID uid = p.getUniqueId();
								if(currentTick >= tacReady.getOrDefault(uid, 0)) {
									tac(p);
									tacReady.put(uid, currentTick + effectiveCooldown(p, TAC_COOLDOWN_TICKS));
									fired = true;
								} else {
									sendCooldownMessage(p, tacReady.getOrDefault(uid, currentTick) - currentTick);
								}
							}
							case "skyblock/combat/jerrychine" -> {
								jerrychine(p);
								fired = true;
							}
							case "skyblock/combat/last_breath", "skyblock/combat/explosive_bow" -> {
								((CraftPlayer) p).getHandle().startUsingItem(InteractionHand.MAIN_HAND);
								fired = true;
							}
						}
					}
				}
			}
		}
		// Cancel left-click events only if an ability actually fired
		if(e != null && fired) e.setCancelled(true);
	}

	private static void playWitherShieldSound(Player p) {
		int currentTick = MinecraftServer.currentTick;
		Integer lastTick = lastWitherShieldSoundTick.get(p.getUniqueId());
		if(lastTick == null || currentTick - lastTick >= 100) {
			Utils.playLocalSound(p, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1f, 0.66666f);
			lastWitherShieldSoundTick.put(p.getUniqueId(), currentTick);
		}
	}

	public static void witherImpact(Player p) {
		// implosion
		p.getWorld().spawnParticle(Particle.EXPLOSION, p.getEyeLocation(), 1);
		List<Entity> entities = p.getNearbyEntities(10, 10, 10);
		List<EntityType> doNotKill = CustomItems.doNotKill();
		int damaged = 0;
		double damage = 0;
		for(Entity entity : entities) {
			if(!doNotKill.contains(entity.getType()) && !entity.equals(p) && entity instanceof LivingEntity entity1 && entity1.getHealth() > 0 && !(entity instanceof Wither wither && wither.getInvulnerableTicks() != 0) && !(entity instanceof Player pl && FakePlayerManager.getFakePlayers().containsValue(pl))) {
				Utils.hurtEntity(entity1, 1, p);
				entity1.setNoDamageTicks(0);
				Utils.changeName(entity1);
				damaged += 1;
				damage += 1;
			}
		}
		if(damaged > 0) {
			p.sendMessage(Utils.msg("<red>Your Implosion hit " + damaged + " enemies for " + ((int) damage) + " damage"));
		}
		Utils.playLocalSound(p, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

		// wither shield sound — 100-tick cooldown per player.
		playWitherShieldSound(p);

		// Inside the F7 Goldor/Necron arena, Wither Impact implodes but does not teleport.
		if(LavaJump.isInBossArena(p.getLocation())) {
			return;
		}

		Location origin = p.getLocation().clone();
		RayTraceResult result = p.rayTraceBlocks(11.65);
		if(result == null) {
			Location l = p.getLocation().add(p.getLocation().getDirection().multiply(10));
			l.setX(Math.floor(l.getX()) + 0.5);
			l.setY(Math.floor(l.getY()));
			l.setZ(Math.floor(l.getZ()) + 0.5);

			// Check if the target location is safe
			Block feetBlock = l.getBlock();
			Block headBlock = feetBlock.getRelative(BlockFace.UP);

			// If either block is solid, we need to adjust
			if(!feetBlock.isPassable() || !headBlock.isPassable()) {
				// Try to move up until we find a safe spot or reach original height
				double originalY = p.getLocation().getY();
				Location checkLoc = l.clone();
				boolean foundSafe = false;

				// Check up to 10 blocks up or until at original height
				for(int i = 0; i < 10; i++) {
					checkLoc.add(0, 1, 0);
					Block checkFeet = checkLoc.getBlock();
					Block checkHead = checkFeet.getRelative(BlockFace.UP);

					// Check if this position is safe (2 blocks of air)
					if(checkFeet.isPassable() && checkHead.isPassable()) {
						// Also check we're not in a 1-block gap if above original height
						if(checkLoc.getY() >= originalY) {
							Block aboveHead = checkHead.getRelative(BlockFace.UP);
							if(!aboveHead.isPassable()) {
								// This is a 1-block gap at or above original height - skip it
								continue;
							}
						}

						l = checkLoc.clone();
						foundSafe = true;
						break;
					}

					// Stop if we've reached or passed original height and no safe spot
					if(checkLoc.getY() >= originalY) {
						break;
					}
				}

				// If no safe spot found, don't teleport
				if(!foundSafe) {
					p.sendMessage(Utils.msg("<red>No safe teleport location found!"));
					return;
				}
			}

			// Additional check for 1-block tall spaces when below original height
			if(l.getY() < p.getLocation().getY()) {
				Block aboveHead = l.getBlock().getRelative(BlockFace.UP, 2);
				if(!aboveHead.isPassable()) {
					// This would put player in crawl mode below their starting position
					// Try to find a better spot
					for(int i = 1; i <= 3; i++) {
						Location upLoc = l.clone().add(0, i, 0);
						Block upFeet = upLoc.getBlock();
						Block upHead = upFeet.getRelative(BlockFace.UP);
						Block upAbove = upHead.getRelative(BlockFace.UP);

						if(upFeet.isPassable() && upHead.isPassable() && upAbove.isPassable()) {
							l = upLoc;
							break;
						}
					}
				}
			}

			l.setYaw(origin.getYaw());
			l.setPitch(origin.getPitch());
			p.teleport(l);
			Utils.debug(Utils.DebugType.SERVER, "Teleporting " + p.getName() + " to " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
		} else {
			switch(result.getHitBlockFace()) {
				case SELF -> {
					// empty case
				}
				case UP -> {
					Location l = result.getHitBlock().getLocation().add(0.5, 1, 0.5);
					l.setYaw(origin.getYaw());
					l.setPitch(origin.getPitch());
					p.teleport(l);
					Utils.debug(Utils.DebugType.SERVER, "Teleporting " + p.getName() + " to " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
				}
				case DOWN -> {
					Location l = result.getHitBlock().getLocation().add(0.5, -2, 0.5);
					l.setYaw(origin.getYaw());
					l.setPitch(origin.getPitch());
					p.teleport(l);
					Utils.debug(Utils.DebugType.SERVER, "Teleporting " + p.getName() + " to " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
				}
				default -> {
					// Hit a side face - backtrack until we find a safe spot
					Location hitLocation = result.getHitPosition().toLocation(p.getWorld());
					Vector direction = origin.getDirection().normalize();

					// Calculate max backtrack distance (don't go past player's origin)
					double maxBacktrack = origin.distance(hitLocation);

					// Backtrack from the exact hit point
					Location checkLoc = hitLocation.clone();
					Location lastSafe = null;
					double totalBacktracked = 0;

					// Backtrack in smaller increments for more precision
					for(int i = 0; i < 100; i++) { // 120 * 0.1 = 12 blocks
						// Backtrack by 0.1 blocks for precision
						checkLoc.subtract(direction.clone().multiply(0.1));
						totalBacktracked += 0.1;

						// Don't go past the player's starting position
						if(totalBacktracked > maxBacktrack) {
							break;
						}

						// Check current block
						Block feetBlock = checkLoc.getBlock();
						Block headBlock = feetBlock.getRelative(BlockFace.UP);

						if(feetBlock.isPassable() && headBlock.isPassable()) {
							// This spot is safe, but keep checking for the optimal position
							lastSafe = checkLoc.clone();

							// Check if we've backtracked enough (at least 0.5 blocks from wall)
							if(checkLoc.distance(hitLocation) >= 0.5) {
								// Center on the block we're in
								Location l = new Location(checkLoc.getWorld(), Math.floor(checkLoc.getX()) + 0.5, Math.floor(checkLoc.getY()), Math.floor(checkLoc.getZ()) + 0.5);
								l.setYaw(origin.getYaw());
								l.setPitch(origin.getPitch());
								p.teleport(l);
								Utils.debug(Utils.DebugType.SERVER, "Teleporting " + p.getName() + " to " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
								p.setFallDistance(0);
								Utils.playLocalSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
								Utils.playLocalSound(p, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
								playWitherShieldSound(p);
								return;
							}
						}
					}

					// If we found a safe spot but didn't teleport yet
					if(lastSafe != null) {
						Location l = new Location(lastSafe.getWorld(), Math.floor(lastSafe.getX()) + 0.5, Math.floor(lastSafe.getY()), Math.floor(lastSafe.getZ()) + 0.5);
						l.setYaw(origin.getYaw());
						l.setPitch(origin.getPitch());
						p.teleport(l);
						Utils.debug(Utils.DebugType.SERVER, "Teleporting " + p.getName() + " to " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
					}
				}
			}
		}
		p.setFallDistance(0);
		Utils.playLocalSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
	}

	public static void aotv(Player p) {
		// Aspect of the Void / etherwarp is disabled only inside the boss room while in adventure mode (the practice
		// default) — so it can't be used to skip boss mechanics, but still works freely everywhere else.
		if(p.getGameMode() == org.bukkit.GameMode.ADVENTURE && LavaJump.isInBossArena(p.getLocation())) return;
		Utils.debug(Utils.DebugType.SERVER, "Starting at " + Utils.round(p.getLocation().getX(), 2) + " " + Utils.round(p.getLocation().getY(), 2) + " " + Utils.round(p.getLocation().getZ(), 2) + " " + Utils.round(p.getLocation().getYaw(), 2) + " " + Utils.round(p.getLocation().getPitch(), 2));
		if(p.isSneaking()) {
			RayTraceResult result = p.rayTraceBlocks(61);
			if(result != null) {
				Block b = result.getHitBlock();
				Location l = b.getLocation().add(0.5, 1, 0.5);
				if(l.getBlock().getType().isSolid() || l.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
					Utils.debug(Utils.DebugType.SERVER, "Could not Etherwarp " + p.getName() + " to " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
					return;
				}
				l.setYaw(p.getEyeLocation().getYaw());
				l.setPitch(p.getEyeLocation().getPitch());
				p.setFallDistance(0);
				Utils.playLocalSound(p, Sound.ENTITY_ENDER_DRAGON_HURT, 1, 0.50F);
				Utils.debug(Utils.DebugType.SERVER, "Etherwarping " + p.getName() + " to " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
				p.teleport(l);
			} else {
				Utils.debug(Utils.DebugType.SERVER, "Could not Etherwarp " + p.getName() + " at all");
			}
		} else {
			Location origin = p.getLocation().clone();
			RayTraceResult result = p.rayTraceBlocks(13.65);
			if(result == null) {
				Location l = p.getLocation().add(p.getLocation().getDirection().multiply(12));
				l.setX(Math.floor(l.getX()) + 0.5);
				l.setY(Math.floor(l.getY()));
				l.setZ(Math.floor(l.getZ()) + 0.5);

				// Check if the target location is safe
				Block feetBlock = l.getBlock();
				Block headBlock = feetBlock.getRelative(BlockFace.UP);

				// If either block is solid, we need to adjust
				if(!feetBlock.isPassable() || !headBlock.isPassable()) {
					// Try to move up until we find a safe spot or reach original height
					double originalY = p.getLocation().getY();
					Location checkLoc = l.clone();
					boolean foundSafe = false;

					// Check up to 10 blocks up or until at original height
					for(int i = 0; i < 10; i++) {
						checkLoc.add(0, 1, 0);
						Block checkFeet = checkLoc.getBlock();
						Block checkHead = checkFeet.getRelative(BlockFace.UP);

						// Check if this position is safe (2 blocks of air)
						if(checkFeet.isPassable() && checkHead.isPassable()) {
							// Also check we're not in a 1-block gap if above original height
							if(checkLoc.getY() >= originalY) {
								Block aboveHead = checkHead.getRelative(BlockFace.UP);
								if(!aboveHead.isPassable()) {
									// This is a 1-block gap at or above original height - skip it
									continue;
								}
							}

							l = checkLoc.clone();
							foundSafe = true;
							break;
						}

						// Stop if we've reached or passed original height and no safe spot
						if(checkLoc.getY() >= originalY) {
							break;
						}
					}

					// If no safe spot found, don't teleport
					if(!foundSafe) {
						return;
					}
				}

				// Additional check for 1-block tall spaces when below original height
				if(l.getY() < p.getLocation().getY()) {
					Block aboveHead = l.getBlock().getRelative(BlockFace.UP, 2);
					if(!aboveHead.isPassable()) {
						// This would put player in crawl mode below their starting position
						// Try to find a better spot
						for(int i = 1; i <= 3; i++) {
							Location upLoc = l.clone().add(0, i, 0);
							Block upFeet = upLoc.getBlock();
							Block upHead = upFeet.getRelative(BlockFace.UP);
							Block upAbove = upHead.getRelative(BlockFace.UP);

							if(upFeet.isPassable() && upHead.isPassable() && upAbove.isPassable()) {
								l = upLoc;
								break;
							}
						}
					}
				}

				l.setYaw(origin.getYaw());
				l.setPitch(origin.getPitch());
				p.teleport(l);
				Utils.debug(Utils.DebugType.SERVER, "Teleporting " + p.getName() + " to " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
			} else {
				switch(result.getHitBlockFace()) {
					case SELF -> {
						// empty case
					}
					case UP -> {
						Location l = result.getHitBlock().getLocation().add(0.5, 1, 0.5);
						l.setYaw(origin.getYaw());
						l.setPitch(origin.getPitch());
						p.teleport(l);
						Utils.debug(Utils.DebugType.SERVER, "Teleporting " + p.getName() + " to " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
					}
					case DOWN -> {
						Location l = result.getHitBlock().getLocation().add(0.5, -2, 0.5);
						l.setYaw(origin.getYaw());
						l.setPitch(origin.getPitch());
						p.teleport(l);
						Utils.debug(Utils.DebugType.SERVER, "Teleporting " + p.getName() + " to " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
					}
					default -> {
						// Hit a side face - backtrack until we find a safe spot
						Location hitLocation = result.getHitPosition().toLocation(p.getWorld());
						Vector direction = origin.getDirection().normalize();

						// Calculate max backtrack distance (don't go past player's origin)
						double maxBacktrack = origin.distance(hitLocation);

						// Backtrack from the exact hit point
						Location checkLoc = hitLocation.clone();
						Location lastSafe = null;
						double totalBacktracked = 0;

						// Backtrack in smaller increments for more precision
						for(int i = 0; i < 120; i++) { // 120 * 0.1 = 12 blocks
							// Backtrack by 0.1 blocks for precision
							checkLoc.subtract(direction.clone().multiply(0.1));
							totalBacktracked += 0.1;

							// Don't go past the player's starting position
							if(totalBacktracked > maxBacktrack) {
								break;
							}

							// Check current block
							Block feetBlock = checkLoc.getBlock();
							Block headBlock = feetBlock.getRelative(BlockFace.UP);

							if(feetBlock.isPassable() && headBlock.isPassable()) {
								// This spot is safe, but keep checking for the optimal position
								lastSafe = checkLoc.clone();

								// Check if we've backtracked enough (at least 0.5 blocks from wall)
								if(checkLoc.distance(hitLocation) >= 0.5) {
									// Center on the block we're in
									Location l = new Location(checkLoc.getWorld(), Math.floor(checkLoc.getX()) + 0.5, Math.floor(checkLoc.getY()), Math.floor(checkLoc.getZ()) + 0.5);
									l.setYaw(origin.getYaw());
									l.setPitch(origin.getPitch());
									p.teleport(l);
									Utils.debug(Utils.DebugType.SERVER, "Teleporting " + p.getName() + " to " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
									p.setFallDistance(0);
									Utils.playLocalSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
									return;
								}
							}
						}

						// If we found a safe spot but didn't teleport yet
						if(lastSafe != null) {
							Location l = new Location(lastSafe.getWorld(), Math.floor(lastSafe.getX()) + 0.5, Math.floor(lastSafe.getY()), Math.floor(lastSafe.getZ()) + 0.5);
							l.setYaw(origin.getYaw());
							l.setPitch(origin.getPitch());
							p.teleport(l);
							Utils.debug(Utils.DebugType.SERVER, "Teleporting " + p.getName() + " to " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
						}
					}
				}
			}
			p.setFallDistance(0);
			Utils.playLocalSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
		}
	}

	public static boolean checkAndActivateCrypt(Block clicked, Player p) {
		Material type = clicked.getType();
		int slabY;

		if(type == Material.SMOOTH_STONE_SLAB || type == Material.GOLD_BLOCK) {
			slabY = clicked.getY();
		} else if(type == Material.STONE_BRICK_STAIRS) {
			slabY = clicked.getY() + 1;
		} else {
			return false;
		}

		// Flood-fill horizontally to collect slab-layer blocks
		Set<Block> slabBlocks = new HashSet<>();
		Queue<Block> queue = new LinkedList<>();
		World world = clicked.getWorld();
		Block startBlock = world.getBlockAt(clicked.getX(), slabY, clicked.getZ());
		Material startType = startBlock.getType();
		if(startType != Material.SMOOTH_STONE_SLAB && startType != Material.GOLD_BLOCK) return false;
		if(startType == Material.SMOOTH_STONE_SLAB) {
			BlockData bd = startBlock.getBlockData();
			if(bd instanceof Slab slab && slab.getType() == Slab.Type.TOP) return false;
		}
		queue.add(startBlock);
		slabBlocks.add(startBlock);

		while(!queue.isEmpty() && slabBlocks.size() <= 1000) {
			Block current = queue.poll();
			for(BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
				Block neighbor = current.getRelative(face);
				if(slabBlocks.contains(neighbor)) continue;
				Material nType = neighbor.getType();
				if(nType == Material.SMOOTH_STONE_SLAB) {
					BlockData bd = neighbor.getBlockData();
					if(bd instanceof Slab slab && slab.getType() == Slab.Type.TOP) continue;
					slabBlocks.add(neighbor);
					queue.add(neighbor);
				} else if(nType == Material.GOLD_BLOCK) {
					slabBlocks.add(neighbor);
					queue.add(neighbor);
				}
			}
		}
		if(slabBlocks.size() > 1000) return false;

		// Compute bounding box
		int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
		int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
		for(Block b : slabBlocks) {
			if(b.getX() < minX) minX = b.getX();
			if(b.getX() > maxX) maxX = b.getX();
			if(b.getZ() < minZ) minZ = b.getZ();
			if(b.getZ() > maxZ) maxZ = b.getZ();
		}

		// Verify full rectangle
		int expectedSize = (maxX - minX + 1) * (maxZ - minZ + 1);
		if(slabBlocks.size() != expectedSize) return false;
		Set<Long> slabPositions = new HashSet<>();
		for(Block b : slabBlocks) {
			slabPositions.add(((long) (b.getX() - minX)) * 10000 + (b.getZ() - minZ));
		}
		for(int x = minX; x <= maxX; x++) {
			for(int z = minZ; z <= maxZ; z++) {
				if(!slabPositions.contains(((long) (x - minX)) * 10000 + (z - minZ))) return false;
			}
		}

		// Validate bottom layer
		Set<Block> stairBlocks = new HashSet<>();
		for(int x = minX; x <= maxX; x++) {
			for(int z = minZ; z <= maxZ; z++) {
				Block below = world.getBlockAt(x, slabY - 1, z);
				Material bType = below.getType();
				if(bType == Material.STONE_BRICK_STAIRS) {
					stairBlocks.add(below);
				} else if(bType != Material.AIR) {
					return false;
				}
			}
		}

		boolean isPrince = slabBlocks.stream().anyMatch(b -> b.getType() == Material.GOLD_BLOCK);

		// Store block data
		Map<Location, BlockData> stored = new HashMap<>();
		for(Block b : slabBlocks) {
			stored.put(b.getLocation(), b.getBlockData().clone());
			b.setType(Material.AIR, false);
		}
		for(Block b : stairBlocks) {
			stored.put(b.getLocation(), b.getBlockData().clone());
			b.setType(Material.AIR, false);
		}

		Utils.playLocalSound(p, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

		double centerX = (minX + maxX) / 2.0 + 0.5;
		double centerZ = (minZ + maxZ) / 2.0 + 0.5;
		Location spawnLoc = new Location(world, centerX, slabY - 1, centerZ);
		Zombie mob = Server.spawnCryptLurker(spawnLoc, isPrince);

		pendingBlockRestorations.putAll(stored);
		pendingCryptMobs.add(mob);
		BukkitTask[] holder = new BukkitTask[1];
		holder[0] = Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
			for(Map.Entry<Location, BlockData> entry : stored.entrySet()) {
				entry.getKey().getBlock().setBlockData(entry.getValue(), false);
				pendingBlockRestorations.remove(entry.getKey());
			}
			if(mob.isValid()) mob.remove();
			pendingCryptMobs.remove(mob);
			pendingBlockTasks.remove(holder[0]);
		}, 40);
		pendingBlockTasks.add(holder[0]);

		return true;
	}

	public static void superboom(Player p) {
		RayTraceResult blockRay = p.rayTraceBlocks(5.0);
		if(blockRay == null || blockRay.getHitBlock() == null) return;
		triggerSuperboomRadius(blockRay.getHitBlock().getLocation(), p);
	}

	public static void triggerSuperboomRadius(Location center, Player p) {
		triggerSuperboomRadius(center, p, new HashSet<>());
	}

	public static void triggerSuperboomRadius(Location center, Player p, Set<Block> visited) {
		// Notify Goldor of any explosion-style impact (Superboom, Explosive Shot, Guided Sheep all route through here).
		instructions.bosses.goldor.Goldor.INSTANCE.notifyExplosionAt(center);
		World world = center.getWorld();
		int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
		for(int dx = -1; dx <= 1; dx++) {
			for(int dy = -1; dy <= 1; dy++) {
				for(int dz = -1; dz <= 1; dz++) {
					Block b = world.getBlockAt(cx + dx, cy + dy, cz + dz);
					Material type = b.getType();
					if((type == Material.SMOOTH_STONE_SLAB || type == Material.GOLD_BLOCK || type == Material.STONE_BRICK_STAIRS || type == Material.CRACKED_STONE_BRICKS) && visited.add(b)) {
						triggerSuperboomAt(b, p);
					}
				}
			}
		}
	}

	public static void triggerSuperboomAt(Block block, Player p) {
		// 1. Try crypt
		if(block.getType() == Material.SMOOTH_STONE_SLAB || block.getType() == Material.GOLD_BLOCK || block.getType() == Material.STONE_BRICK_STAIRS) {
			if(checkAndActivateCrypt(block, p)) return;
		}

		// 2. Cracked stone bricks flood-fill
		if(block.getType() != Material.CRACKED_STONE_BRICKS) return;

		Set<Block> connected = new HashSet<>();
		Queue<Block> queue = new LinkedList<>();
		queue.add(block);
		connected.add(block);
		while(!queue.isEmpty()) {
			Block current = queue.poll();
			for(BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN}) {
				Block neighbor = current.getRelative(face);
				if(neighbor.getType() == Material.CRACKED_STONE_BRICKS && connected.add(neighbor)) {
					queue.add(neighbor);
				}
			}
		}

		Map<Location, BlockData> original = new HashMap<>();
		for(Block b : connected) {
			original.put(b.getLocation(), b.getBlockData().clone());
			b.setType(Material.AIR, false);
		}
		Utils.playLocalSound(p, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
		pendingBlockRestorations.putAll(original);
		BukkitTask[] holder = new BukkitTask[1];
		holder[0] = Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
			for(Map.Entry<Location, BlockData> entry : original.entrySet()) {
				entry.getKey().getBlock().setBlockData(entry.getValue(), false);
				pendingBlockRestorations.remove(entry.getKey());
			}
			pendingBlockTasks.remove(holder[0]);
		}, 40);
		pendingBlockTasks.add(holder[0]);
	}

	public static void stonk(Player p, Block b) {
		if(Goldor.INSTANCE.isProtected(b)) return;
		if(b.getType().getHardness() != -1) {
			Material m = b.getType();
			BlockData data = b.getBlockData().clone();
			Location loc = b.getLocation();
			Utils.debug(Utils.DebugType.SERVER, p.getName() + " Stonking block at " + Utils.round(loc.getX(), 3) + " " + Utils.round(loc.getY(), 5) + " " + Utils.round(loc.getZ(), 3));

			pendingStonkRestorations.put(loc, data);
			BukkitTask task = Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
				b.setType(m);
				b.setBlockData(data);
				pendingStonkRestorations.remove(loc);
				pendingStonkTasks.remove(loc);
			}, 200);
			pendingStonkTasks.put(loc, task);
		}
	}

	public static void flushStonkRestorations() {
		pendingStonkTasks.values().forEach(BukkitTask::cancel);
		pendingStonkTasks.clear();
		for(Map.Entry<Location, BlockData> entry : pendingStonkRestorations.entrySet()) {
			entry.getKey().getBlock().setBlockData(entry.getValue(), false);
		}
		pendingStonkRestorations.clear();
	}

	/**
	 * Immediately restore every superboomed wall / crypt currently set to AIR and despawn any active crypt mobs,
	 * cancelling their pending 40-tick restorations. Mirrors {@link #flushStonkRestorations()}; called from
	 * Server.serverSetup so /reset and /setup replace all crypts and walls at once.
	 */
	public static void flushBlockRestorations() {
		pendingBlockTasks.forEach(BukkitTask::cancel);
		pendingBlockTasks.clear();
		for(Map.Entry<Location, BlockData> entry : pendingBlockRestorations.entrySet()) {
			entry.getKey().getBlock().setBlockData(entry.getValue(), false);
		}
		pendingBlockRestorations.clear();
		for(Zombie mob : pendingCryptMobs) {
			if(mob.isValid()) mob.remove();
		}
		pendingCryptMobs.clear();
	}

	public static void rag(Player p) {
		Utils.playLocalSound(p, Sound.BLOCK_LEVER_CLICK, 1.0F, 2.0F);
		Utils.scheduleTask(() -> Utils.playLocalSound(p, Sound.BLOCK_LEVER_CLICK, 1.0F, 2.0F), 20);
		Utils.scheduleTask(() -> Utils.playLocalSound(p, Sound.BLOCK_LEVER_CLICK, 1.0F, 2.0F), 40);
		Utils.scheduleTask(() -> {
			Utils.playLocalSound(p, Sound.ENTITY_WOLF_WHINE, 1.0F, 1.5F);
			p.addScoreboardTag("RagBuff");
			// Buff expires 200 ticks (10s) after THIS application; a later cast overwrites this, extending the buff.
			ragBuffExpiry.put(p.getUniqueId(), MinecraftServer.currentTick + 200);
			Utils.debug(Utils.DebugType.SERVER, "Rag Buff applied to " + Utils.getRealName(p));
			if(p.getName().equals("Archer")) {
				p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 3));
			} else if(p.getName().equals("Berserk")) {
				p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 0));
			}
		}, 60);
		// Remove only once the latest expiry is reached: a second cast refreshes ragBuffExpiry, so this earlier
		// cast's removal sees currentTick < expiry and no-ops, leaving the tag for the later cast's removal to clear.
		Utils.scheduleTask(() -> {
			if(MinecraftServer.currentTick >= ragBuffExpiry.getOrDefault(p.getUniqueId(), 0)) {
				p.removeScoreboardTag("RagBuff");
				ragBuffExpiry.remove(p.getUniqueId());
				Utils.debug(Utils.DebugType.SERVER, "Rag Buff expired for " + Utils.getRealName(p));
			}
		}, 260);
	}

	public static void salvation(Player p) {
		double powerBonus;
		try {
			int power = Objects.requireNonNull(p.getInventory().getItem(p.getInventory().getHeldItemSlot())).getEnchantmentLevel(Enchantment.POWER);
			powerBonus = power * 0.25;
			if(power == 7) {
				powerBonus += 0.25;
			}
		} catch(Exception exception) {
			powerBonus = 0;
		}

		double strengthBonus;
		try {
			strengthBonus = 0.75 + Objects.requireNonNull(p.getPotionEffect(PotionEffectType.STRENGTH)).getAmplifier();
		} catch(Exception exception) {
			strengthBonus = 0;
		}

		double add = powerBonus + strengthBonus;
		Location l = p.getLocation();
		l.add(0, 1.62, 0);

		Vector v = l.getDirection();
		v.setX(v.getX() / 4);
		v.setY(v.getY() / 4);
		v.setZ(v.getZ() / 4);
		World world = l.getWorld();
		Set<Entity> damagedEntities = new HashSet<>();
		List<EntityType> doNotKill = doNotKill();
		damagedEntities.add(p);
		int pierce = 5;
		for(int i = 0; i < 256 && pierce > 0; i++) {
			if(l.getBlock().getType().isSolid()) {
				break;
			}
			assert world != null;
			ArrayList<Entity> entities = (ArrayList<Entity>) world.getNearbyEntities(l, 1, 1, 1);
			for(Entity entity : entities) {
				if(!damagedEntities.contains(entity) && !doNotKill.contains(entity.getType()) && entity instanceof LivingEntity entity1 && !(entity instanceof Player) && entity1.getHealth() > 0 && !(entity instanceof Wither wither && wither.getInvulnerableTicks() != 0)) {
					damagedEntities.add(entity);
					Utils.hurtEntity(entity1, (float) (2.5 + add), p);
					entity1.setNoDamageTicks(0);
					Utils.changeName(entity1);
					pierce--;
				}
			}
			Particle.DustOptions particle = new Particle.DustOptions(Color.RED, 1.0F);
			world.spawnParticle(Particle.DUST, l, 1, particle);
			l.add(v);
		}
		Utils.playLocalSound(p, Sound.ENTITY_GUARDIAN_DEATH, 0.5f, 2.0F);
	}

	public static void aots(Player p) {
		Utils.playLocalSound(p, Sound.BLOCK_LAVA_POP, 1.0F, 1.0F);

		// Create the axe item display
		Location startLoc = p.getEyeLocation();
		Vector direction = startLoc.getDirection().normalize();

		// Calculate the horizontal perpendicular to the direction of travel
		// Project direction onto the XZ plane and get perpendicular
		double dx = direction.getX();
		double dz = direction.getZ();

		// The perpendicular in the XZ plane (rotate 90 degrees clockwise when viewed from above)
		Vector spinAxis = new Vector(-dz, 0, dx).normalize();

		// If looking straight up/down (no horizontal component), use player yaw
		if(Math.abs(dx) < 0.001 && Math.abs(dz) < 0.001) {
			float yaw = startLoc.getYaw();
			spinAxis = new Vector(-Math.cos(Math.toRadians(yaw)), 0, -Math.sin(Math.toRadians(yaw)));
		}

		// Spawn an ItemDisplay entity
		ItemDisplay axe = p.getWorld().spawn(startLoc, ItemDisplay.class);
		axe.setItemStack(new ItemStack(Material.DIAMOND_AXE));
		axe.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND);

		Vector finalSpinAxis = spinAxis;
		new BukkitRunnable() {
			int distance = 0;
			Location currentLoc = startLoc.clone();
			float spinRotation = 0;
			boolean notedAggro = false;

			@Override
			public void run() {
				if(distance >= 100 || !axe.isValid()) {
					axe.remove();
					cancel();
					return;
				}

				// Check if we hit a wall (solid block)
				Location nextLoc = currentLoc.clone().add(direction);
				if(nextLoc.getBlock().getType().isSolid()) {
					axe.remove();
					cancel();
					return;
				}

				// Move 1 block per tick
				currentLoc = nextLoc;

				// The thrown axe doesn't damage the wither, but travelling through it still counts as aggro —
				// note the thrower as the boss's damager the first tick the axe overlaps a wither boss.
				if(!notedAggro) {
					for(Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.0, 2.0, 1.0)) {
						if(e instanceof Wither w && w.getScoreboardTags().contains("TASWither")) {
							instructions.bosses.WitherActions.noteDamager(p);
							notedAggro = true;
							break;
						}
					}
				}

				// Update spin rotation
				spinRotation += 36; // Positive for forward spin

				// Create rotation using axis-angle rotation around the spin axis
				Quaternionf rotation = new Quaternionf().rotateAxis((float) Math.toRadians(spinRotation), (float) finalSpinAxis.getX(), (float) finalSpinAxis.getY(), (float) finalSpinAxis.getZ());

				axe.setTransformation(new Transformation(new Vector3f(0, 0, 0), // No translation offset
						rotation, new Vector3f(1, 1, 1), // Normal scale
						new Quaternionf() // No right rotation
				));

				// Teleport to new position
				axe.teleport(currentLoc);

				distance++;
			}
		}.runTaskTimer(M7tas.getInstance(), 1L, 1L);
	}

	public static void iceSpray(Player p) {
		Location l = p.getEyeLocation();
		p.getWorld().spawnParticle(Particle.SNOWFLAKE, l, 512);
		List<Entity> entities = (List<Entity>) p.getWorld().getNearbyEntities(l, 8, 8, 8);
		List<EntityType> doNotKill = doNotKill();
		for(Entity entity : entities) {
			if(!doNotKill.contains(entity.getType()) && entity instanceof LivingEntity entity1 && !(entity instanceof Player) && entity1.getHealth() > 0 && !(entity instanceof Wither wither && wither.getInvulnerableTicks() != 0)) {
				Utils.hurtEntity(entity1, 1, p);
				entity1.setNoDamageTicks(0);
				Utils.changeName(entity1);
			}
		}
		Utils.playLocalSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 1.0F);
	}

	public static void flamingFlay(Player p) {
		Location startLoc = p.getEyeLocation();

		// Angle up by 5 degrees from player's look direction
		Vector direction = startLoc.getDirection().normalize();
		float pitch = Math.max(startLoc.getPitch() - 15, -90); // Subtract 5 degrees (negative pitch is up)
		float yaw = startLoc.getYaw();

		// Recalculate direction with adjusted pitch
		double xz = Math.cos(Math.toRadians(pitch));
		direction.setX(-xz * Math.sin(Math.toRadians(yaw)));
		direction.setY(-Math.sin(Math.toRadians(pitch)));
		direction.setZ(xz * Math.cos(Math.toRadians(yaw)));

		Vector velocity = direction.normalize().multiply(1.5); // Initial velocity

		new BukkitRunnable() {
			final Location currentLoc = startLoc.clone();
			final Vector currentVelocity = velocity.clone();
			int colorIndex = 0;
			double totalDistance = 0;
			final Set<Entity> hitEntities = new HashSet<>(); // Track hit entities to avoid duplicate damage

			@Override
			public void run() {
				// Apply reduced gravity to velocity
				currentVelocity.add(new Vector(0, -0.08, 0)); // Reduced gravity

				// Move the particle location
				Location previousLoc = currentLoc.clone();
				currentLoc.add(currentVelocity);

				// Calculate distance traveled this tick
				double distanceThisTick = previousLoc.distance(currentLoc);
				int particleCount = (int) (distanceThisTick * 5); // 5 particles per block

				// Spawn particles along the path
				for(int i = 0; i < particleCount; i++) {
					double t = (double) i / particleCount;
					Location particleLoc = previousLoc.clone().add(currentVelocity.clone().multiply(t));

					// Cycle through colors: Red, Yellow, Green
					Particle.DustOptions dust = switch(colorIndex % 3) {
						case 0 -> new Particle.DustOptions(Color.RED, 1.5f);
						case 1 -> new Particle.DustOptions(Color.YELLOW, 1.5f);
						default -> new Particle.DustOptions(Color.LIME, 1.5f); // Green
					};

					p.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, dust);
					colorIndex++;
				}

				// Check for mob hits
				List<EntityType> doNotKill = doNotKill();
				for(Entity entity : Objects.requireNonNull(currentLoc.getWorld()).getNearbyEntities(currentLoc, 0.5, 0.5, 0.5)) {
					if(!hitEntities.contains(entity) && !doNotKill.contains(entity.getType()) && entity instanceof LivingEntity entity1 && !(entity instanceof Player) && entity1.getHealth() > 0 && !(entity instanceof Wither wither && wither.getInvulnerableTicks() != 0)) {
						// Deal 1 damage using NMS
						Utils.hurtEntity(entity1, 1, p);
						entity1.setNoDamageTicks(0);
						Utils.changeName(entity1);
						hitEntities.add(entity);
					}
				}

				totalDistance += distanceThisTick;

				// Stop conditions
				if(currentLoc.getBlock().getType().isSolid() || // Hit a block
						currentLoc.getY() < -64 || // Fell into void
						totalDistance > 12) { // Max distance reduced to 15 blocks
					cancel();
				}
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	public static void gyro(Player p) {
		RayTraceResult result = p.rayTraceBlocks(24);
		if(result == null) {
			return;
		}
		Location l = result.getHitBlock().getLocation();
		p.getWorld().spawnParticle(Particle.PORTAL, l, 512);
		l.setY(l.getY() + 1);
		new BukkitRunnable() {
			float pitch = 0.5f;

			@Override
			public void run() {
				if(pitch >= 2.0f) {
					cancel();
					return;
				}
				Utils.playLocalSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, pitch);
				pitch += 0.025f;
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);

		Material[] blockTypes = {Material.OBSIDIAN, Material.PURPLE_CONCRETE, Material.PURPLE_STAINED_GLASS};
		List<FallingBlock> fallingBlocks = new ArrayList<>();
		boolean[] effectEnded = {false}; // Flag to track if effect has ended

		// Initial block spawning
		for(double angle = 0; angle < Math.PI * 2; angle += Math.PI / 32) {
			double x = l.getX() + Math.cos(angle) * 10;
			double z = l.getZ() + Math.sin(angle) * 10;

			// Find the next air block going upwards from the requested location
			double y = l.getY();
			while(!Objects.requireNonNull(l.getWorld()).getBlockAt((int) x, (int) y, (int) z).getType().isAir() && y < l.getWorld().getMaxHeight()) {
				y++;
			}
			y += 0.05; // Place falling block 0.05 above the air block
			Location blockLoc = new Location(l.getWorld(), x, Math.max(y, l.getY()), z);

			Material blockType = blockTypes[(int) (Math.random() * blockTypes.length)];
			FallingBlock block = l.getWorld().spawn(blockLoc, FallingBlock.class, fb -> fb.setBlockData(blockType.createBlockData()));
			block.setGravity(false);
			block.setDropItem(false);
			block.setInvulnerable(true);

			block.setHurtEntities(false);
			block.setPersistent(true);
			block.setCancelDrop(true);

			fallingBlocks.add(block);
		}

		// Ensure blocks don't place by constantly checking and respawning if needed
		BukkitRunnable blockChecker = new BukkitRunnable() {
			@Override
			public void run() {
				if(effectEnded[0]) {
					cancel();
					return;
				}

				List<FallingBlock> toRemove = new ArrayList<>();
				List<FallingBlock> toAdd = new ArrayList<>();

				for(FallingBlock block : fallingBlocks) {
					if(block.isDead() || !block.isValid()) {
						// The block has turned into a real block or disappeared
						toRemove.add(block);

						// Create a new block at the same location
						Material blockType = blockTypes[(int) (Math.random() * blockTypes.length)];
						Location respawnLoc = block.getLocation().clone().add(0, 0.1, 0);

						FallingBlock newBlock = Objects.requireNonNull(respawnLoc.getWorld()).spawn(respawnLoc, FallingBlock.class, fb -> fb.setBlockData(blockType.createBlockData()));
						newBlock.setGravity(false);
						newBlock.setDropItem(false);
						newBlock.setInvulnerable(true);
						newBlock.setHurtEntities(false);
						newBlock.setPersistent(true);
						newBlock.setCancelDrop(true);

						toAdd.add(newBlock);
					}
				}

				// Update our block list
				fallingBlocks.removeAll(toRemove);
				fallingBlocks.addAll(toAdd);

				if(fallingBlocks.isEmpty()) {
					cancel();
				}
			}
		};
		blockChecker.runTaskTimer(M7tas.getInstance(), 1L, 1L);

		new BukkitRunnable() {
			int tick = 0;
			final List<Location> initialPositions = new ArrayList<>();
			final double TOTAL_TICKS = 60.0; // 3 seconds
			final double CONVERGE_TICKS = 40.0; // 2 seconds to converge

			@Override
			public void run() {
				if(tick == 0) {
					for(FallingBlock block : fallingBlocks) {
						initialPositions.add(block.getLocation());
					}
				}

				if(tick >= TOTAL_TICKS) {
					effectEnded[0] = true;
					fallingBlocks.forEach(block -> {
						if(block.isValid()) {
							block.remove();
						}
					});
					fallingBlocks.clear();
					cancel();
					return;
				}

				double progress = Math.min(tick / CONVERGE_TICKS, 1.0);

				for(int i = 0; i < fallingBlocks.size(); i++) {
					FallingBlock block = fallingBlocks.get(i);
					if(!block.isValid()) continue; // Skip invalid blocks

					Location startLoc = i < initialPositions.size() ? initialPositions.get(i) : block.getLocation();

					// Linear interpolation between start and rift
					Vector direction = l.toVector().subtract(startLoc.toVector());
					Vector currentPos = startLoc.toVector().add(direction.multiply(progress));

					if(progress < 1.0) {
						// Add constant wobble effect
						Vector wobble = new Vector((Math.random() - 0.5) * 4, (Math.random() - 0.5) * 4, (Math.random() - 0.5) * 4);

						currentPos.add(wobble);
					} else {
						// Keep blocks at center with slight wobble
						Vector wobble = new Vector((Math.random() - 0.5) * 2, (Math.random() - 0.5) * 2, (Math.random() - 0.5) * 2);
						currentPos = l.toVector().add(wobble);
					}

					Location targetLoc = currentPos.toLocation(l.getWorld());

					// Ensure minimum height above ground
					// Find the next air block going upwards from the requested location
					double minY = l.getY();
					while(!Objects.requireNonNull(l.getWorld()).getBlockAt(targetLoc.getBlockX(), (int) minY, targetLoc.getBlockY()).getType().isAir() && minY < l.getWorld().getMaxHeight()) {
						minY++;
					}
					minY += 0.05; // Place falling block 0.05 above the air block
					targetLoc.setY(Math.max(targetLoc.getY(), minY));

					// Calculate velocity vector based on distance to target with increased speed
					Vector velocity = targetLoc.toVector().subtract(block.getLocation().toVector()).multiply(0.3);

					block.setVelocity(velocity);
				}

				tick++;
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);


		for(Entity e : Objects.requireNonNull(l.getWorld()).getNearbyEntities(l, 10, 10, 10)) {
			if(e instanceof LivingEntity entity && !(entity instanceof Player) && !(entity instanceof Wither)) {
				new BukkitRunnable() {
					int tick = 0;

					@Override
					public void run() {
						if(tick >= 60) { // 3 seconds * 20 ticks
							cancel();
							return;
						}

						if(tick < 10) { // First 0.5 seconds - pull in
							Location entityLoc = entity.getLocation();
							double x = (l.getX() - entityLoc.getX()) / 5;
							double y = (l.getY() - entityLoc.getY()) / 5;
							double z = (l.getZ() - entityLoc.getZ()) / 5;
							entity.setVelocity(new Vector(x, y, z));
						} else { // Next 2.5 seconds - keep at rift
							entity.teleport(l);
						}

						tick++;
					}
				}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
			}
		}
	}

	public static final Map<Integer, Integer> bonzoFireTick = new HashMap<>();

	public static void bonzo(Player p) {
		Location l = p.getEyeLocation();
		WindCharge windCharge = (WindCharge) l.getWorld().spawnEntity(l, EntityType.WIND_CHARGE);
		windCharge.addScoreboardTag("Bonzo");
		windCharge.setShooter(p);
		bonzoFireTick.put(windCharge.getEntityId(), MinecraftServer.currentTick);
	}

	private static final double JERRY_SPEED = 20.0 / 30.0; // 20 blocks in 30 ticks
	private static final double JERRY_BOOST_V = 0.6;
	private static final double JERRY_BOOST_H = 0.446;
	private static final double JERRY_BOOST_RADIUS = 3.5;
	private static final float JERRY_HEAD_SCALE = 1.0f;
	// MHF_Villager head texture (Hypixel's generic Jerry villager skin)
	private static final String JERRY_HEAD_TEXTURE = "eyJ0aW1lc3RhbXAiOjE1MTIyMTE4MjQ0MzAsInByb2ZpbGVJZCI6ImJkNDgyNzM5NzY3YzQ1ZGNhMWY4YzMzYzQwNTMwOTUyIiwicHJvZmlsZU5hbWUiOiJNSEZfVmlsbGFnZXIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzgyMmQ4ZTc1MWM4ZjJmZDRjODk0MmM0NGJkYjJmNWNhNGQ4YWU4ZTU3NWVkM2ViMzRjMThhODZlOTNiIn19fQ==";

	private static ItemStack jerryHeadItem() {
		ItemStack head = new ItemStack(Material.PLAYER_HEAD);
		Multimap<String, Property> props = HashMultimap.create();
		props.put("textures", new Property("textures", JERRY_HEAD_TEXTURE));
		PropertyMap propertyMap = new PropertyMap(props);
		GameProfile gp = new GameProfile(UUID.randomUUID(), "jerryhead", propertyMap);
		CraftPlayerProfile profile = new CraftPlayerProfile(gp);
		SkullMeta meta = (SkullMeta) head.getItemMeta();
		assert meta != null;
		meta.setPlayerProfile(profile);
		head.setItemMeta(meta);
		return head;
	}

	public static void jerrychine(Player p) {
		Location l = p.getEyeLocation();
		Snowball s = (Snowball) l.getWorld().spawnEntity(l, EntityType.SNOWBALL);
		s.addScoreboardTag("jerrychine");
		s.setShooter(p);
		s.setGravity(false);
		s.setVisibleByDefault(false);
		s.setVelocity(l.getDirection().multiply(JERRY_SPEED));

		ItemDisplay head = (ItemDisplay) l.getWorld().spawnEntity(l, EntityType.ITEM_DISPLAY);
		head.setItemStack(jerryHeadItem());
		head.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
		Transformation t = head.getTransformation();
		head.setTransformation(new Transformation(
				new Vector3f(0f, 0f, 0f),
				t.getLeftRotation(),
				new Vector3f(JERRY_HEAD_SCALE, JERRY_HEAD_SCALE, JERRY_HEAD_SCALE),
				t.getRightRotation()));
		head.setBillboard(Display.Billboard.FIXED);
		head.setInterpolationDuration(1);
		head.setTeleportDuration(1);
		head.addScoreboardTag("jerrychineHead");

		new BukkitRunnable() {
			@Override
			public void run() {
				if(!s.isValid() || !head.isValid()) {
					head.remove();
					cancel();
					return;
				}
				Location to = s.getLocation();
				Vector v = s.getVelocity();
				double speed = v.length();
				if(speed > 1e-6) {
					to.setYaw((float) Math.toDegrees(Math.atan2(-v.getX(), v.getZ())));
					to.setPitch((float) Math.toDegrees(-Math.asin(v.getY() / speed)));
				}
				head.teleport(to);
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);

		l.getWorld().playSound(l, Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
		Utils.debug(Utils.DebugType.SERVER, p.getName() + " fired Jerry-chine #" + s.getEntityId() + " from " + Utils.round(l.getX(), 2) + " " + Utils.round(l.getY(), 2) + " " + Utils.round(l.getZ(), 2));
	}

	@EventHandler
	public void onJerrychineHit(ProjectileHitEvent e) {
		if(!(e.getEntity() instanceof Snowball s)) return;
		if(!s.getScoreboardTags().contains("jerrychine")) return;
		e.setCancelled(true);
		s.remove();

		if(!(s.getShooter() instanceof Player p)) return;

		double distance = p.getLocation().distanceSquared(s.getLocation());
		if(distance > JERRY_BOOST_RADIUS * JERRY_BOOST_RADIUS) return;
		if(!(p instanceof CraftPlayer craftPlayer)) return;
		ServerPlayer serverPlayer = craftPlayer.getHandle();

		Vector direction = p.getLocation().toVector().subtract(s.getLocation().toVector()).normalize();
		direction.setY(0);
		direction.normalize();

		// Horizontal push magnitude scales by cos(firing pitch). Snowball has no gravity, so its velocity
		// direction equals the shooter's fire-time look direction; cos(pitch) = horizontal-speed / total-speed.
		Vector vel = s.getVelocity();
		double speed = vel.length();
		double cosPitch = speed > 1e-6 ? Math.hypot(vel.getX(), vel.getZ()) / speed : 0;
		double horizMag = JERRY_BOOST_H * cosPitch;

		direction.multiply(horizMag);
		direction.setY(JERRY_BOOST_V);

		if(!Double.isFinite(direction.getX())) direction.setX(0);
		if(!Double.isFinite(direction.getZ())) direction.setZ(0);

		serverPlayer.setOnGround(false);
		p.setVelocity(direction);
		double horizSpeed = Math.hypot(direction.getX(), direction.getZ());
		Utils.debug(Utils.DebugType.SERVER, "Jerry-chine moved " + p.getName() + " " + Utils.round(direction.getX(), 3) + " " + Utils.round(direction.getY(), 5) + " " + Utils.round(direction.getZ(), 3));
	}

	@EventHandler
	public void onExplosiveBowArrowHit(ProjectileHitEvent e) {
		if(!(e.getEntity() instanceof Arrow arrow)) return;
		if(!arrow.getScoreboardTags().contains("ExplosiveBowArrow")) return;
		if(!(arrow.getShooter() instanceof Player p)) return;

		// On entity contact the arrow behaves like a normal arrow — its arrow damage + the hit ding are applied
		// by the normal damage path (WithersNotImmuneToArrows for a vulnerable wither). On EITHER an entity or a
		// block hit it then detonates an added explosion bonus at the point of impact.
		Location impact;
		if(e.getHitEntity() != null) {
			impact = e.getHitEntity().getLocation().add(0, e.getHitEntity().getHeight() / 2.0, 0);
		} else if(e.getHitBlock() != null) {
			impact = e.getHitBlock().getLocation();
		} else {
			return;
		}
		impact.getWorld().spawnParticle(Particle.EXPLOSION, impact.clone().add(0.5, 0.5, 0.5), 10, 0.5, 0.5, 0.5, 0);
		impact.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1, 1f);
		triggerSuperboomRadius(impact, p);
		arrow.remove();
	}

	public static void terminator(Player p) {
		// you don't need arrows
		p.getInventory().remove(Material.ARROW);
		p.getInventory().remove(Material.TIPPED_ARROW);
		p.getInventory().remove(Material.SPECTRAL_ARROW);

		// Get NMS world and player
		ServerLevel nmsWorld = ((CraftWorld) p.getWorld()).getHandle();
		ServerPlayer nmsPlayer = ((CraftPlayer) p).getHandle();

		// In creative mode, vanilla's BowItem.use() will start the bow-draw animation even with
		// PlayerInteractEvent cancelled (creative bypasses the arrow check). Cancel the draw next
		// tick — by then vanilla has run and we can release it cleanly.
		Utils.scheduleTask(() -> {
			if(nmsPlayer.isUsingItem()) nmsPlayer.stopUsingItem();
		}, 1);

		// Calculate directions
		Vector baseDirection = p.getEyeLocation().getDirection().normalize();
		Vector leftDirection = baseDirection.clone().rotateAroundY(Math.toRadians(-5));
		Vector rightDirection = baseDirection.clone().rotateAroundY(Math.toRadians(5));

		// Calculate spawn position (vanilla: eyeY - 0.1)
		Location l = p.getEyeLocation().add(0, -0.1, 0);

		// Create NMS arrows directly
		net.minecraft.world.entity.projectile.arrow.Arrow nmsLeft = new net.minecraft.world.entity.projectile.arrow.Arrow(nmsWorld, 0, 0, 0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
		net.minecraft.world.entity.projectile.arrow.Arrow nmsMiddle = new net.minecraft.world.entity.projectile.arrow.Arrow(nmsWorld, 0, 0, 0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
		net.minecraft.world.entity.projectile.arrow.Arrow nmsRight = new net.minecraft.world.entity.projectile.arrow.Arrow(nmsWorld, 0, 0, 0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);

		// Set positions
		nmsLeft.setPos(l.getX(), l.getY(), l.getZ());
		nmsMiddle.setPos(l.getX(), l.getY(), l.getZ());
		nmsRight.setPos(l.getX(), l.getY(), l.getZ());

		// shoot() sets both velocity and rotation from the direction vector
		float speed = 3.175f;
		nmsLeft.shoot(leftDirection.getX(), leftDirection.getY(), leftDirection.getZ(), speed, 0);
		nmsMiddle.shoot(baseDirection.getX(), baseDirection.getY(), baseDirection.getZ(), speed, 0);
		nmsRight.shoot(rightDirection.getX(), rightDirection.getY(), rightDirection.getZ(), speed, 0);

		// Set other properties
		nmsLeft.setOwner(nmsPlayer);
		nmsMiddle.setOwner(nmsPlayer);
		nmsRight.setOwner(nmsPlayer);

		// Add to world
		nmsWorld.addFreshEntity(nmsLeft);
		nmsWorld.addFreshEntity(nmsMiddle);
		nmsWorld.addFreshEntity(nmsRight);

		// Get Bukkit wrappers for further modification
		Arrow left = (Arrow) nmsLeft.getBukkitEntity();
		Arrow middle = (Arrow) nmsMiddle.getBukkitEntity();
		Arrow right = (Arrow) nmsRight.getBukkitEntity();

		// Calculate bonuses
		double powerBonus;
		try {
			int power = p.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.POWER);
			powerBonus = power * 0.25;
		} catch(Exception exception) {
			powerBonus = 0;
		}

		double strengthBonus;
		try {
			strengthBonus = 0.75 + 0.75 * p.getPotionEffect(PotionEffectType.STRENGTH).getAmplifier();
		} catch(Exception exception) {
			strengthBonus = 0;
		}

		double add = powerBonus + strengthBonus;
		// 4/4 Thermodynamic armor grants the 4-tick cooldown but a 97.5% terminator damage penalty (it isn't a
		// dungeon item, so it carries no dungeon stats). Spring Boots add a 20% reduction and the Racing Helmet a
		// 30% reduction (stacking multiplicatively). Bake them all into every arrow's damage at fire time.
		final double dmgMult = (isThermoSet(p) ? 0.025 : 1.0) * springBootsMultiplier(p) * racingHelmetMultiplier(p);

		// Set Bukkit properties
		for(Arrow arrow : Arrays.asList(left, middle, right)) {
			arrow.setDamage((2.5 + add) * dmgMult);
			arrow.setPierceLevel(4);
			arrow.setShooter(p);
			arrow.setWeapon(p.getInventory().getItemInMainHand());
			arrow.addScoreboardTag("TerminatorArrow");
			arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
		}

		for(Arrow arrow : Arrays.asList(left, middle, right)) {
			Utils.scheduleTask(() -> {
				if(arrow.isValid() && arrow.getLocation().getBlock().getType().isSolid()) arrow.remove();
			}, 1);
		}

		Utils.playLocalSound(p, Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F);

		// Duplex Arrow
		Utils.scheduleTask(() -> {
			Arrow arrow = p.getWorld().spawnArrow(l, l.getDirection(), 3.175f, 0);
			arrow.setDamage((0.5 + add * 0.2) * dmgMult);
			arrow.setPierceLevel(4);
			arrow.setShooter(p);
			arrow.setWeapon(p.getInventory().getItemInMainHand());
			arrow.addScoreboardTag("TerminatorArrow");
			arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
			Utils.playLocalSound(p, Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F);
			Utils.scheduleTask(() -> {
				if(arrow.isValid() && arrow.getLocation().getBlock().getType().isSolid()) arrow.remove();
			}, 1);
		}, 3);

		if(p.getName().startsWith("Archer") || p.getScoreboardTags().contains("Archer")) {
			Utils.scheduleTask(() -> {
				Arrow arrow = p.getWorld().spawnArrow(l, l.getDirection(), 3.175f, 0);
				arrow.setDamage((2.5 + add) * dmgMult);
				arrow.setPierceLevel(4);
				arrow.setShooter(p);
				arrow.setWeapon(p.getInventory().getItemInMainHand());
				arrow.addScoreboardTag("TerminatorArrow");
				arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
				Utils.playLocalSound(p, Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F);
				Utils.scheduleTask(() -> {
					if(arrow.isValid() && arrow.getLocation().getBlock().getType().isSolid()) arrow.remove();
				}, 1);
			}, 5);

			Utils.scheduleTask(() -> {
				Arrow arrow = p.getWorld().spawnArrow(l, l.getDirection(), 3.175f, 0);
				arrow.setDamage((2.5 + add) * dmgMult);
				arrow.setPierceLevel(4);
				arrow.setShooter(p);
				arrow.setWeapon(p.getInventory().getItemInMainHand());
				arrow.addScoreboardTag("TerminatorArrow");
				arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
				p.playSound(p, Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F);
				Utils.scheduleTask(() -> {
					if(arrow.isValid() && arrow.getLocation().getBlock().getType().isSolid()) arrow.remove();
				}, 1);
			}, 10);
		}
	}

	/**
	 * Berserk-exclusive damage ramp. Each successive hit a Berserk lands on the SAME mob deals +10% damage
	 * (1.0×, 1.1×, 1.2×, …) capped at 3× (+200%). Each call counts as one hit — so the separate terminator
	 * arrows from a single right-click each ramp the multiplier further. Returns {@code base} unchanged for
	 * non-Berserk shooters. Per-mob counts are cleared at run start via {@link #resetBerserkDamage()}.
	 */
	public static double scaleBerserkDamage(Player p, LivingEntity target, double base) {
		if(p == null || target == null) return base;
		if(!(p.getName().startsWith("Berserk") || p.getScoreboardTags().contains("Berserk"))) return base;
		Map<UUID, Integer> perMob = berserkHitCounts.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
		int hits = perMob.getOrDefault(target.getUniqueId(), 0);
		double multiplier = Math.min(1.0 + 0.1 * hits, 3.0);
		perMob.put(target.getUniqueId(), hits + 1);
		return base * multiplier;
	}

	/** Clear all Berserk per-mob hit counts — called at the start of every /tas and /practice run. */
	public static void resetBerserkDamage() {
		berserkHitCounts.clear();
	}

	/** Clear terminator cooldown state — called at the start of every /tas and /practice run. */
	public static void resetTerminatorCooldowns() {
		termLastPacketTick.clear();
		termLastFireTick.clear();
		salvationReady.clear();
		mageBeamReady.clear();
	}

	/** Reset all class-ability + weapon-ability cooldowns (Guided Sheep / Rapid Fire / Explosive Shot, plus Gyro /
	 *  Ragnarok / Ice Spray / Tactical Insertion) — called on entering a boss fight (WitherLord.start) and at run start. */
	public static void resetAbilityCooldowns() {
		guidedSheepReady.clear();
		rapidFireReady.clear();
		explosiveShotReady.clear();
		gyroReady.clear();
		ragReady.clear();
		ragBuffExpiry.clear();
		iceSprayReady.clear();
		tacReady.clear();
	}

	/** True if {@code p} is the Mage CLASS (drives ability-cooldown reduction). Real players carry an exclusive class
	 *  scoreboard tag (set by /getcustomitems); fake players carry none and are identified by name. All four fake
	 *  "MageN" players run the Mage inventory and cast Mage abilities, so every "Mage*"-named fake counts as a Mage
	 *  (Mage2/3/4 cosplay Tank/Berserk/Healer's ROLE but are mechanically mages). */
	public static boolean isMageClass(Player p) {
		if(p.getScoreboardTags().contains("Mage")) return true;
		for(String other : new String[]{"Archer", "Berserk", "Healer", "Tank"}) {
			if(p.getScoreboardTags().contains(other)) return false;
		}
		return p.getName().startsWith("Mage");
	}

	/** A base ability cooldown after the Mage class's cooldown reduction: a SOLO mage gets −75% (quarter cooldown),
	 *  but with two or more Mage-class players it's the standard −50% (half). Non-mages are unchanged. NOT used for
	 *  the Terminator/Salvation — those are weapons, not abilities. */
	public static int effectiveCooldown(Player p, int baseTicks) {
		if(!isMageClass(p)) return baseTicks;
		return mageCount() <= 1 ? baseTicks / 4 : baseTicks / 2;
	}

	/** Number of Mage-class players currently online (see {@link #isMageClass}). */
	private static long mageCount() {
		return Bukkit.getOnlinePlayers().stream().filter(CustomItems::isMageClass).count();
	}

	/** Tell {@code p} their ability is on cooldown, showing the remaining time in seconds (e.g. "...for 3.45
	 *  seconds!"). {@code ticksRemaining} is the ticks left until the ability is usable again. */
	private static void sendCooldownMessage(Player p, int ticksRemaining) {
		String format = String.format("%.2f", Math.max(0, ticksRemaining) / 20.0);
		p.sendMessage(Utils.msg("<red>This ability is on cooldown for " + format + " seconds!"));
		// During a TAS run (not practice) an ability fired on cooldown means the choreography mistimed it — flag it
		// loudly so the offending tick/player is visible. (ERROR always prints with a [tick: N] stamp.)
		if(!instructions.bosses.WitherActions.isPracticeMode()) {
			ItemStack held = p.getInventory().getItemInMainHand();
			String ability = held.hasItemMeta() && held.getItemMeta().hasDisplayName()
					? Utils.displayName(held.getItemMeta()) : String.valueOf(held.getType());
			Utils.debug(Utils.DebugType.ERROR, Utils.getRealName(p) + " tried to use " + ability + Utils.mmLegacy("<red>")
					+ " on cooldown (" + Math.max(0, ticksRemaining) + "t / " + format + "s left)");
		}
	}

	/** Outgoing-damage multiplier from Spring Boots: a 20% reduction (×0.8) while the boots are worn, else 1.0. */
	public static double springBootsMultiplier(Player p) {
		ItemStack boots = p.getInventory().getBoots();
		return boots.getType() == Material.CHAINMAIL_BOOTS && boots.hasItemMeta() && boots.getItemMeta().hasDisplayName() && Utils.displayName(boots.getItemMeta()).contains("Spring Boots")
				? 0.8 : 1.0;
	}

	/** Outgoing-damage multiplier from the Racing Helmet: a 30% reduction (×0.7) while worn, else 1.0. Stacks
	 *  multiplicatively with {@link #springBootsMultiplier} (both are folded into the same damage multiplier). */
	public static double racingHelmetMultiplier(Player p) {
		return FakePlayerInventory.isRacingHelmet(p.getInventory().getHelmet()) ? 0.7 : 1.0;
	}

	/** True if the player is wearing the full (4/4) Thermodynamic armor set. */
	public static boolean isThermoSet(Player p) {
		org.bukkit.inventory.PlayerInventory inv = p.getInventory();
		return isThermoPiece(inv.getHelmet()) && isThermoPiece(inv.getChestplate())
				&& isThermoPiece(inv.getLeggings()) && isThermoPiece(inv.getBoots());
	}

	private static boolean isThermoPiece(ItemStack item) {
		return item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()
				&& Utils.displayName(item.getItemMeta()).contains("Thermodynamic");
	}

	/**
	 * Per-tick terminator cooldown poller. For each player who has recorded a terminator right-click since their
	 * last shot, fires a bow shot on the first tick at/after {@code lastFire + cooldown} (5 ticks, or 4 with the
	 * full Thermodynamic set). Shots are anchored to the first shot and clamp to one per cooldown — spamming the
	 * right-click can't exceed it. Started from {@link plugin.M7tas}.
	 */
	public static void pollTerminators() {
		int now = MinecraftServer.currentTick;
		for(Map.Entry<UUID, Integer> entry : termLastPacketTick.entrySet()) {
			UUID id = entry.getKey();
			int lastPacket = entry.getValue();
			int lastFire = termLastFireTick.getOrDefault(id, Integer.MIN_VALUE / 2);
			if(lastPacket <= lastFire) continue;                 // no new right-click since the last shot
			Player p = Bukkit.getPlayer(id);
			if(p == null || !p.isOnline()) continue;
			if(!"skyblock/combat/terminator".equals(getID(p.getInventory().getItemInMainHand()))) continue; // must hold it
			int cooldown = isThermoSet(p) ? 4 : 5;
			if(now >= lastFire + cooldown) {
				terminator(p);
				termLastFireTick.put(id, now);
			}
		}
	}

	public static void tac(Player p) {
		// Tactical Insertion is disabled inside the boss room while in adventure mode (the practice default) — so it
		// can't be used to cheat boss mechanics, but still works freely everywhere else.
		if(p.getGameMode() == org.bukkit.GameMode.ADVENTURE && LavaJump.isInBossArena(p.getLocation())) return;
		Location l = p.getLocation();
		Utils.debug(Utils.DebugType.SERVER, "Activating Tactical Insertion at " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
		Utils.playLocalSound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0F, 0.707107F);
		Utils.playLocalSound(p, Sound.ITEM_FLINTANDSTEEL_USE, 1.0F, 1.0F);
		Utils.scheduleTask(() -> Utils.playLocalSound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0F, 0.793701F), 10);
		Utils.scheduleTask(() -> Utils.playLocalSound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0F, 0.890899F), 20);
		Utils.scheduleTask(() -> Utils.playLocalSound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0F, 0.943874F), 30);
		Utils.scheduleTask(() -> Utils.playLocalSound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0F, 1F), 40);
		Utils.scheduleTask(() -> Utils.playLocalSound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0F, 1.059463F), 50);
		Utils.scheduleTask(() -> {
			Utils.playLocalSound(p, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0F, 1.0F);
			p.teleport(l);
			Utils.debug(Utils.DebugType.SERVER, "Returning " + p.getName() + " to " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
			p.setVelocity(new Vector(0, 0, 0));
			Utils.scheduleTask(() -> p.getWorld().spawnParticle(Particle.FLAME, p.getLocation(), 512), 1);
		}, 60);
		Utils.scheduleTask(() -> Utils.playLocalSound(p, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0F, 1.0F), 63);
		Utils.scheduleTask(() -> Utils.playLocalSound(p, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0F, 1.0F), 66);
	}

	public static void explosiveShot(Player p) {
		ServerLevel nmsWorld = ((CraftWorld) p.getWorld()).getHandle();
		ServerPlayer nmsPlayer = ((CraftPlayer) p).getHandle();

		Vector baseDirection = p.getEyeLocation().getDirection().normalize();
		Vector leftDirection = baseDirection.clone().rotateAroundY(Math.toRadians(-7.5));
		Vector rightDirection = baseDirection.clone().rotateAroundY(Math.toRadians(7.5));

		Location l = p.getEyeLocation().add(0, -0.1, 0);
		float speed = 1.5f;
		List<LivingEntity> alreadyHurt = new ArrayList<>();
		Set<Block> visitedBlocks = new HashSet<>();
		for(Vector dir : List.of(leftDirection, baseDirection, rightDirection)) {
			net.minecraft.world.entity.projectile.arrow.Arrow nmsArrow = new net.minecraft.world.entity.projectile.arrow.Arrow(nmsWorld, 0, 0, 0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
			nmsArrow.setPos(l.getX(), l.getY(), l.getZ());
			nmsArrow.shoot(dir.getX(), dir.getY(), dir.getZ(), speed, 0);
			nmsArrow.setOwner(nmsPlayer);
			nmsWorld.addFreshEntity(nmsArrow);

			Arrow arrow = (Arrow) nmsArrow.getBukkitEntity();
			arrow.setDamage(0);
			arrow.setPierceLevel(1);
			arrow.setShooter(p);
			arrow.setWeapon(p.getInventory().getItemInMainHand());
			arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);

			new BukkitRunnable() {
				@Override
				public void run() {
					if(!arrow.isValid() || arrow.isDead() || arrow.isOnGround() || arrow.getLocation().getBlock().getType().isSolid()) {
						Location impact = arrow.getLocation();

						for(Entity e : arrow.getNearbyEntities(4, 4, 4)) {
							if(e instanceof LivingEntity target && !alreadyHurt.contains(target) && !(e instanceof Player) && !(target.hasPotionEffect(PotionEffectType.RESISTANCE) && target.getPotionEffect(PotionEffectType.RESISTANCE).getAmplifier() == 255) && !(e instanceof Wither wither && wither.getInvulnerableTicks() != 0)) {
								Utils.hurtEntity(target, 19, p);
								target.setNoDamageTicks(0);
								Utils.changeName(target);
								alreadyHurt.add(target);
							}
						}

						// Visual effects
						p.getWorld().spawnParticle(Particle.EXPLOSION, impact, 10, 0.5, 0.5, 0.5, 0);
						p.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1, 1f);

						triggerSuperboomRadius(impact, p, visitedBlocks);

						arrow.remove();
						cancel();
					}
				}
			}.runTaskTimer(M7tas.getInstance(), 1L, 1L);
		}
	}

	public static void guidedSheep(Player p) {
		Location spawnLoc = p.getEyeLocation().add(0, -0.65, 0);
		Vector direction = spawnLoc.getDirection().normalize();
		double speed = 1; // blocks/tick

		Sheep sheep = (Sheep) p.getWorld().spawnEntity(spawnLoc, EntityType.SHEEP);
		sheep.setAI(false);
		sheep.setGravity(false);
		sheep.setInvulnerable(true);
		sheep.setSilent(true);
		sheep.setColor(DyeColor.WHITE);
		sheep.customName(null);
		sheep.setCustomNameVisible(false);
		sheep.setCollidable(false);
		sheep.addScoreboardTag("TASNoName");
		PlayerCollision.addEntityToNoCollisionTeam(sheep);

		Vector velocity = direction.multiply(speed);

		new BukkitRunnable() {
			int ticks = 0;
			static final int MAX_TICKS = 200; // safety cap: 10 s

			@Override
			public void run() {
				if(ticks++ >= MAX_TICKS || !sheep.isValid()) {
					Location loc = sheep.getLocation();
					loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 10, 0.5, 0.5, 0.5, 0);
					loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1f);
					PlayerCollision.removeEntityFromNoCollisionTeam(sheep);
					sheep.remove();
					cancel();
					return;
				}

				Location next = sheep.getLocation().add(velocity);
				Block nextBlock = next.getBlock();

				if(nextBlock.getType().isSolid()) {
					next.getWorld().spawnParticle(Particle.EXPLOSION, next, 10, 0.5, 0.5, 0.5, 0);
					next.getWorld().playSound(next, Sound.ENTITY_GENERIC_EXPLODE, 1, 1f);
					triggerSuperboomRadius(next, p);
					PlayerCollision.removeEntityFromNoCollisionTeam(sheep);
					sheep.remove();
					cancel();
					return;
				}

				sheep.teleport(next);
			}
		}.runTaskTimer(M7tas.getInstance(), 0L, 1L);
	}

	public static void rapidFire(Player p) {
		for(int i = 0; i < 200; i += 4) {
			Utils.scheduleTask(() -> {
				ServerLevel nmsWorld = ((CraftWorld) p.getWorld()).getHandle();
				ServerPlayer nmsPlayer = ((CraftPlayer) p).getHandle();

				Location eyeLoc = p.getEyeLocation();
				Vector dir = eyeLoc.getDirection().normalize();
				Location spawnLoc = eyeLoc.add(0, -0.1, 0);
				double speed = 2;

				net.minecraft.world.entity.projectile.arrow.Arrow nmsArrow = new net.minecraft.world.entity.projectile.arrow.Arrow(nmsWorld, 0, 0, 0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW), null);
				nmsArrow.setPos(spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ());
				nmsArrow.shoot(dir.getX(), dir.getY(), dir.getZ(), (float) speed, 0);
				nmsArrow.setOwner(nmsPlayer);
				nmsWorld.addFreshEntity(nmsArrow);

				Arrow arrow = (Arrow) nmsArrow.getBukkitEntity();
				arrow.setDamage(35);
				arrow.setPierceLevel(4);
				arrow.setShooter(p);
				arrow.setWeapon(p.getInventory().getItemInMainHand());
				arrow.addScoreboardTag("TerminatorArrow");
				arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
			}, i);
		}
	}

	public static void mageBeam(Player p) {
		Location l = p.getLocation();

		// Range depends on boss fight state: 50 blocks during a boss fight, 25 otherwise.
		double range = LavaJump.isInBossArena(p.getLocation()) ? 50.0 : 25.0;

		// Get player's yaw in radians
		double yaw = Math.toRadians(l.getYaw());

		// Calculate perpendicular vector (90 degrees to the right)
		double rightYaw = yaw + Math.toRadians(90);

		// Calculate offsets (16 pixels = 1 block)
		double offsetX = -Math.sin(rightYaw) * (5.0 / 16.0);
		double offsetZ = Math.cos(rightYaw) * (5.0 / 16.0);
		double offsetY = 1.62 - (13.0 / 16.0);

		// Apply offsets
		l.add(offsetX, offsetY, offsetZ);

		// Get the eye location and direction
		Location eyeLocation = p.getEyeLocation();
		Vector eyeDirection = eyeLocation.getDirection();

		// Raytrace both entities and blocks. Whichever is closer along the line of sight is
		// what the beam actually hits — so if a wall is between the player and an entity,
		// the wall stops the beam and the entity takes no damage.
		RayTraceResult entityResult = findTargetEntity(p, eyeLocation, eyeDirection, range);
		RayTraceResult blockResult = p.getWorld().rayTraceBlocks(eyeLocation, eyeDirection, range, FluidCollisionMode.NEVER, true);

		Vector eye = eyeLocation.toVector();
		double entityDist = entityResult != null ? entityResult.getHitPosition().distance(eye) : Double.MAX_VALUE;
		double blockDist = blockResult != null ? blockResult.getHitPosition().distance(eye) : Double.MAX_VALUE;

		Vector targetPoint;
		Entity targetEntity;
		if(entityResult != null && entityDist <= blockDist) {
			targetEntity = entityResult.getHitEntity();
			targetPoint = entityResult.getHitPosition();
		} else if(blockResult != null) {
			targetEntity = null;
			targetPoint = blockResult.getHitPosition();
		} else {
			targetEntity = null;
			targetPoint = eye.clone().add(eyeDirection.clone().multiply(range));
		}

		// Calculate the direction from hand to the target point
		Vector handToTarget = targetPoint.clone().subtract(l.toVector());
		double distance = handToTarget.length();
		handToTarget.normalize();

		// Iterations based on distance to target, not max range
		int iterations = (int) (distance / 0.25);
		Vector v = handToTarget.multiply(0.25);

		for(int i = 0; i < iterations; i++) {
			spawnFireworkParticle(l);
			l.add(v);
		}

		// A dead mob — or a boss wither pinned in its dying state (TASDying, HP frozen at 1, so isDead/health
		// won't flag it) — takes no real damage, so the beam passing through it shouldn't emit a hurt sound.
		boolean targetDead = targetEntity instanceof LivingEntity dead
				&& (dead.isDead() || dead.getHealth() <= 0 || dead.getScoreboardTags().contains("TASDying"));

		// Beam hit sounds are routed ONLY to the beamer (and their spectators) at constant volume —
		// no at-location sound, so volume doesn't depend on how far the target is.
		if(targetEntity instanceof Wither wither && wither.getInvulnerableTicks() != 0) {
			// Armored (e.g. mid-intro, before the fight is live): no damage lands, but still record the damager so
			// the boss aggros whoever was hitting it the moment its intro completes and aggro turns on.
			if(wither.getScoreboardTags().contains("TASWither")) instructions.bosses.WitherActions.noteDamager(p);
			if(!targetDead) Utils.playLocalSound(p, Sound.ENTITY_WITHER_HURT, 1.0f, 1.0f);
		} else if(targetEntity instanceof LivingEntity temp) {
			boolean ragBuff = p.getScoreboardTags().contains("RagBuff");
			// Per-weapon mage-beam damage (ID "...scylla" is the Hyperion, "...claymore" the Dark Claymore):
			//   Hyperion: 195 (225 RagBuffed) — full vs minecraft:wither ONLY; −33% against any other mob type.
			//   Dark Claymore (and any other mage-beam item): 170 (200 RagBuffed) on ALL entities.
			float damage;
			if("skyblock/combat/scylla".equals(getID(p.getInventory().getItemInMainHand()))) {
				damage = ragBuff ? 225 : 195;
				if(!(temp instanceof Wither)) damage *= (1f - 0.33f);
			} else {
				damage = ragBuff ? 200 : 170;
			}
			damage *= (float) springBootsMultiplier(p); // Spring Boots: 20% outgoing-damage reduction while worn
			damage *= (float) racingHelmetMultiplier(p); // Racing Helmet: 30% outgoing-damage reduction (stacks multiplicatively)
			// Silence the target during the hit so vanilla doesn't broadcast its hurt sound at the
			// target's location; beamDamageInProgress tells onWitherHurtSound to skip its manual
			// broadcast the same way (withers are permanently silent, so silence can't signal that).
			boolean wasSilent = temp.isSilent();
			temp.setSilent(true);
			beamDamageInProgress = true;
			try {
				Utils.hurtEntity(temp, damage, p);
			} finally {
				beamDamageInProgress = false;
				temp.setSilent(wasSilent);
			}
			temp.setNoDamageTicks(0);
			Utils.changeName(temp);
			if(!targetDead) {
				String hurtSound = Utils.getHurtSoundKey(temp);
				if(hurtSound != null) Utils.playLocalSound(p, hurtSound, 1.0f, 1.0f);
			}
		}
	}

	private static RayTraceResult findTargetEntity(Player p, Location eyeLocation, Vector eyeDirection, double range) {
		// raySize=0 (precise) instead of 0.5: the 0.5 inflate blew up geometry at close
		// range to large hitboxes (e.g. withers), because the player's eye ends up inside
		// the inflated AABB and the raytrace returns an arbitrary exit face.
		return p.getWorld().rayTraceEntities(eyeLocation, eyeDirection, range, 0, entity -> entity instanceof LivingEntity livingEntity && !(entity instanceof Player) && !entity.isDead() && !(livingEntity.hasPotionEffect(PotionEffectType.RESISTANCE) && livingEntity.getPotionEffect(PotionEffectType.RESISTANCE).getAmplifier() == 255));
	}

	public static void spawnFireworkParticle(Location l) {
		ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(ParticleTypes.FIREWORK, false, false, l.getX(), l.getY(), l.getZ(), 0.0f, 0.0f, 0.0f, 0.0f, 1);
		Utils.broadcastPacket(packet);
	}
}
