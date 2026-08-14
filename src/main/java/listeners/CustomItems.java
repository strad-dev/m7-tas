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
import instructions.bosses.maxor.Maxor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.profile.CraftPlayerProfile;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
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
	// Hard rate cap on right-click abilities, in ticks. TWO, not one: a single physical right-click reaches
	// handleCustomItems twice (PlayerPacketInterceptor's netty dispatch, and vanilla's own PlayerInteractEvent),
	// as two separate main-thread tasks. They normally drain in the same tick and collapse, but the server stops
	// draining its task queue when it runs out of tick time, so on a lagging tick the pair straddles a boundary and
	// both fire - which is a DOUBLE etherwarp, since the second dispatch ray-traces from where the first one landed.
	// They can only ever be ONE tick apart (adjacent tasks in one FIFO queue, so at most one boundary falls between
	// them), which is why 2 is enough.  Note the pair can PRINT the same [tick: N]: Utils.debug stamps
	// MinecraftServer.getTickCount(), which is bumped inside tickServer, i.e. AFTER runAllTasksAtTickStart drains the
	// queue, while currentTick is bumped at the top of the runServer iteration - so a task drained at tick start
	// reads currentTick N with the tick counter still on N-1.  A doubled ability on "one tick" is this, not a
	// bypassed gate.
	// lastRightBlockTick below already uses this same one-tick tolerance for the AIR that trails a BLOCK
	// click, but that guard doesn't help a BLOCK+BLOCK or AIR+AIR pair. Nothing here wants a faster
	// rate: every ability in the switch has its own longer cooldown, and the Terminator only records a packet tick
	// for pollTerminators (4-5t). Vanilla-item right-clicks (ender pearls, food) never reach this gate.
	private static final int RIGHT_CLICK_GATE_TICKS = 2;
	// Tick of the last RIGHT_CLICK_BLOCK dispatch per player. A physical right-click on a block sends UseItemOn
	// (RIGHT_CLICK_BLOCK) immediately followed by UseItem (RIGHT_CLICK_AIR); this lets the right-click handler drop
	// the trailing AIR so the ability fires once even when the pair straddles a tick boundary (see handleCustomItems).
	private static final Map<UUID, Integer> lastRightBlockTick = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> lastLeftClickAbilityTick = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> lastWitherShieldSoundTick = new ConcurrentHashMap<>();
	// True while mageBeam's damage call is on the stack.  Damage is applied synchronously, so
	// damage/Damage.witherHurtSound reads this to skip its at-location broadcast for beam hits: the beam routes
	// its own constant-volume hurt sound to the beamer, so an at-location one would double up and be
	// distance-attenuated.
	public static boolean beamDamageInProgress = false;
	private static final Set<UUID> droppingPlayers = new HashSet<>();
	// The Berserk damage ramp now lives in damage/CombatState, with MAP.md §1.14's real figures
	// (+165% per repeated hit to a +950% cap, +180%/+1200% solo) rather than the old +10%-to-3x approximation.
	// resetBerserkDamage() below still clears it, alongside the rest of the per-run combat state.
	// Terminator firing is poller-driven (NOT fired directly on the right-click packet). A right-click records the
	// packet tick; pollTerminators() fires on the first tick where a new packet exists AND the cooldown has elapsed
	// (5 ticks, or 4 with 4/4 Thermodynamic armor). This caps the rate at 1 shot / cooldown regardless of spam.
	private static final Map<UUID, Integer> termLastPacketTick = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> termLastFireTick = new ConcurrentHashMap<>();
	// Class-ability (drop-triggered) cooldowns.  Each map stores the tick that ability is next usable.  Reset on
	// entering a boss fight (WitherLord.start) and at run start.  Guided Sheep 600t, Rapid Fire 2000t, Explosive Shot 400t.
	private static final Map<UUID, Integer> guidedSheepReady = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> rapidFireReady = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> explosiveShotReady = new ConcurrentHashMap<>();
	// Salvation (Terminator left-click) cooldown: the tick the next Salvation beam is usable.  The shared left-click
	// guard only caps to 1/tick, so this enforces the ability's own 5-tick cooldown.  Note that the Terminator and
	// Salvation are weapons, NOT abilities, so they deliberately skip the Mage cooldown reduction.
	private static final int SALVATION_COOLDOWN_TICKS = 5;
	private static final Map<UUID, Integer> salvationReady = new ConcurrentHashMap<>();
	// Mage beam (Mage-class left-click) cooldown.  The shared left-click guard only caps to 1/tick, so this enforces
	// the beam's own 5-tick cooldown: fired on tick N → next usable on tick N+5.
	private static final int MAGE_BEAM_COOLDOWN_TICKS = 5;
	private static final Map<UUID, Integer> mageBeamReady = new ConcurrentHashMap<>();
	// Secondary mage weapons: in a MAGE's hand (same class gate as the Hyperion/Claymore) these fire the SAME mage
	// beam on left-click, with the same 5-tick cooldown, geometry and hit test.  Their real ability is on the
	// right-click and is unaffected.  While the beam is armed, left-clicking them must also never break a block
	// (leftClickAbilityItem).
	// They used to chip for a flat 1 damage.  They no longer do: the beam is a formula output now, so each of
	// these swings its OWN stat block (MAP.md §8) - which for the Aspect of the Void and the Ragnarock Axe
	// is a real weapon, and for the Bonzo Staff a small one.  This set is now only "which items a Mage may beam
	// with", not a damage tier.
	private static final Set<String> WEAK_BEAM_IDS = Set.of(
			"skyblock/combat/bonzo",
			"skyblock/combat/aotv",
			"skyblock/combat/ice_spray",
			"skyblock/combat/rag");
	// Per-ability cooldowns, in base ticks before the Mage class's 50% reduction (see effectiveCooldown).  Each map
	// stores the tick that ability is next usable, keyed by player.  Reset in resetAbilityCooldowns().
	private static final int GYRO_COOLDOWN_TICKS = 600;       // Gyrokinetic Wand: 30s
	private static final int RAG_COOLDOWN_TICKS = 400;        // Ragnarock Axe: 20s
	private static final int ICE_SPRAY_COOLDOWN_TICKS = 100;  // Ice Spray Wand: 5s
	private static final int TAC_COOLDOWN_TICKS = 400;        // Tactical Insertion: 20s
	private static final int GUIDED_SHEEP_COOLDOWN_TICKS = 600; // Guided Sheep: 30s
	private static final int GOLEM_SWORD_COOLDOWN_TICKS = 60; // Golem Sword: 3s
	// Berserk's two drop abilities (MAP.md §1.14): the ultimate is x1.5 melee for 15s on a 60s cooldown,
	// and the regular ability throws an axe for the player's highest hit in the last 60s.  The ultimate's WINDOW
	// lives in damage/CombatState, since that is what the damage math reads; only its cooldown lives here.
	private static final int BERSERK_ULTIMATE_COOLDOWN_TICKS = 1200; // 60s
	private static final int BERSERK_THROW_COOLDOWN_TICKS = 100;     // 5s
	private static final Map<UUID, Integer> berserkUltimateReady = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> berserkThrowReady = new ConcurrentHashMap<>();
	// Axe of the Shredded: the throw deals 10% of melee, and CONSECUTIVE throws double it to a x16 cap (§1.9).
	// A throw counts as consecutive if it lands inside this window of the previous one.
	private static final double AOTS_THROW_SHARE = 0.10;
	private static final double AOTS_THROW_CAP = 16.0;
	private static final int AOTS_STREAK_TICKS = 100;
	private static final Map<UUID, Integer> aotsStreak = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> aotsStreakExpiry = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> gyroReady = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> ragReady = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> iceSprayReady = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> tacReady = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> golemSwordReady = new ConcurrentHashMap<>();
	// Server tick the RagBuff currently expires at, per player. Each cast's buff lands at +60 and lasts 200 ticks
	// (10s); a re-cast while still active pushes this out so the earlier cast's removal no-ops (see rag()).
	private static final Map<UUID, Integer> ragBuffExpiry = new ConcurrentHashMap<>();
	// Server tick the in-flight Ragnarock cast started at, per player. The 3s wind-up (ragWindup) polls the main hand
	// every tick and drops the cast the moment the axe leaves it, so swapping off mid-wind-up no longer lands the
	// buff. The stamp doubles as the chain's identity: a re-cast overwrites it, so an older chain sees a start tick
	// that isn't its own and stops.
	private static final Map<UUID, Integer> ragCastStart = new ConcurrentHashMap<>();
	public static final Map<Location, BlockData> pendingStonkRestorations = new HashMap<>();
	public static final Map<Location, BukkitTask> pendingStonkTasks = new HashMap<>();
	// Crypt + Superboom-wall restorations. Mirrors the stonk maps above: a crypt/wall is temporarily set to AIR and
	// restored after 40 ticks via a raw scheduler task (NOT Utils.scheduleTask), so /reset and /setup can flush them
	// immediately via flushBlockRestorations(). Using Utils.scheduleTask here would let Reset's cancelAllScheduled()
	// kill the pending restoration, leaving permanent AIR holes and orphaned crypt mobs.
	private static final Map<Location, BlockData> pendingBlockRestorations = new HashMap<>();
	private static final List<BukkitTask> pendingBlockTasks = new ArrayList<>();
	private static final List<Zombie> pendingCryptMobs = new ArrayList<>();
	// DETECTION radius of every explosion that routes through triggerSuperboomRadius: Superboom TNT, Explosive Shot
	// and Guided Sheep.  This is the FIRST of the two searches, a cube half-extent around the impact block scanned
	// for a *valid* crypt/wall block.  It uses Chebyshev distance with no line-of-sight test, so air neither triggers
	// nor blocks it.  2 → a 5x5x5 box.  The SECOND search runs per hit block in triggerSuperboomAt: the crypt
	// rectangle validation in checkAndActivateCrypt and the cracked-brick 6-face flood-fill, which decide how much is
	// actually removed.  That one is deliberately NOT scaled by this constant.  Reach is separate again, and comes
	// from vanilla's interaction range (see superboom).
	private static final int SUPERBOOM_RADIUS = 2;
	// Tick of the last Superboom-TNT detonation per player.  The TNT detonates either from the ability dispatch (any
	// click path) or from a raw vanilla placement caught in onInfinityboomPlace, so this caps it to one blast per
	// player per tick.  A click that somehow reaches both paths won't double-boom.
	private static final Map<UUID, Integer> lastSuperboomTick = new ConcurrentHashMap<>();
	// Tick of the last ordinary melee hit per player, so one swing lands exactly one hit (see meleeAttack).
	private static final Map<UUID, Integer> lastMeleeTick = new ConcurrentHashMap<>();

	public static boolean abilityFiredThisTick(Player p) {
		return lastLeftClickAbilityTick.getOrDefault(p.getUniqueId(), -1) == MinecraftServer.currentTick;
	}

	/**
	 * True if this item, in this player's hand, fires the mage beam on left-click: a Mage's Hyperion or Dark Claymore,
	 * or one of the {@link #WEAK_BEAM_IDS} secondary mage weapons.  The class gate is the whole point - the same iron
	 * sword in a Berserk's hand is a melee weapon.
	 */
	private static boolean isMageBeamItem(Player p, ItemStack item) {
		if(item == null) return false;
		// {@link #isMageClass}, not a second inline "named Mage* or tagged Mage" test.  The two used to differ, and the
		// difference bites exactly here: a real player who PICKED Berserk but happens to be named Mage-something was a
		// mage to the beam gate and a Berserk to everything else, so their Hyperion beamed instead of swinging.  The
		// exclusive class tag wins in isMageClass, which is the answer that matches /class.
		return isMageClass(p) && (item.getType() == Material.IRON_SWORD || item.getType() == Material.STONE_SWORD
				|| WEAK_BEAM_IDS.contains(getID(item)));
	}

	/**
	 * True if this item's LEFT click is an ability rather than a swing.  Two things read it: the block-break
	 * suppression in {@link #handleCustomItems} (the ability must never break a block, even on cooldown), and
	 * {@link #meleeAttack}, which stands down entirely for these so a Mage's beam or a Terminator volley is never
	 * accompanied by a melee hit.
	 */
	private static boolean leftClickIsAbility(Player p, ItemStack item) {
		String id = getID(item);
		return isMageBeamItem(p, item) || id.equals("skyblock/combat/terminator")
				|| id.equals("skyblock/combat/gyro") || id.equals("skyblock/combat/infinityboom");
	}

	/**
	 * An ordinary melee swing on a mob - <b>the plugin's one melee damage path</b>, dispatched from
	 * {@code PlayerPacketInterceptor}'s attack-packet branch.
	 * <p>
	 * There was no such path at all until now, and the gap was invisible because the Mage never needed one: a Mage's
	 * swing fires the beam, which applies its own damage, so the only class whose sword mattered was already served.
	 * Every other class fell through to VANILLA melee damage, and vanilla is not a participant in this model
	 * (MAP.md §7) - so a swing did a couple of hearts against a mob whose HP is SB/1e6, and against a boss
	 * wither it did precisely nothing, because {@code MiscListener.onWitherLordDamage} cancels every non-plugin hit on
	 * a TASWither.  That is the whole of "a Berserk's melee hits do nothing": the class with the biggest melee
	 * multipliers in the plan was the one class routed through the path that had been switched off.
	 * <p>
	 * Everything downstream already existed and simply had no caller: {@code Damage.melee} for the formula, and
	 * {@code Damage.deal} for the application, which in turn drives the repeated-hit stack, the post-kill buff, Fire
	 * Aspect / Venomous / Thunderlord and the Cleave sweep.
	 */
	public static void meleeAttack(Player p, org.bukkit.entity.Entity hit) {
		if(p == null || !(hit instanceof LivingEntity target)) return;
		if(p.getGameMode() == org.bukkit.GameMode.SPECTATOR) return;
		if(Spectate.getSpectatorMap().containsKey(p)) return;
		// Players are invulnerable in this model, and a spectated fake player must never be hittable either.
		if(target instanceof Player) return;
		ItemStack held = p.getInventory().getItemInMainHand();
		if(leftClickIsAbility(p, held)) return;
		// One hit per swing.  The attack packet arrives once per swing, but the drop-key ability path also swings, and
		// a duplicate attack packet would otherwise double the damage AND the repeated-hit stack.
		int now = MinecraftServer.currentTick;
		if(lastMeleeTick.getOrDefault(p.getUniqueId(), -1) == now) return;
		lastMeleeTick.put(p.getUniqueId(), now);

		damage.Damage.deal(target, damage.Damage.melee(p, target, held), damage.DamageKind.NORMAL, p,
				damage.DamagePath.MELEE);
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
		// Right-clicking a button or a lever owns the click, so the held item's right-click ability must not fire.
		// Skip custom-item handling entirely so we also don't cancel the event (the block still actuates).
		if(e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null
				&& (e.getClickedBlock().getType() == Material.LEVER || Tag.BUTTONS.isTagged(e.getClickedBlock().getType()))) {
			return;
		}
		// Right-clicking a clear-phase secret (chest/essence) owns the click, so don't fire the held item's ability.
		if(e.getAction() == Action.RIGHT_CLICK_BLOCK && instructions.clear.ClearManager.isSecretBlock(e.getClickedBlock())) {
			return;
		}
		// getClickedBlock() is vanilla's own hit result, null for air clicks.  Block abilities use it as their reach.
		handleCustomItems(e, e.getHand(), e.getItem(), e.getAction(), e.getPlayer(), e.getClickedBlock());
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
		// Right-clicking a pickupable Energy Crystal picks it up (Maxor); that click must not ALSO fire the held
		// item's right-click ability.
		if(e.getRightClicked() instanceof EnderCrystal crystal && !Maxor.INSTANCE.notEnergyCrystal(crystal)) return;
		handleCustomItems(e, e.getHand(), e.getPlayer().getInventory().getItemInMainHand(), Action.RIGHT_CLICK_AIR, e.getPlayer());
	}

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if(e.getEntity() instanceof LivingEntity entity && !entity.getScoreboardTags().contains("TASNoName")) {
			Utils.scheduleTask(() -> Utils.changeName(entity), 1);
		}
		// Ability dispatch AND the melee hit itself for real players are handled by PlayerPacketInterceptor
		// (fires for every attack packet, including no-damage cases like iframe/dying mobs).
		// Routing EDBEE through handleCustomItems caused double-fire because the interceptor's
		// runTask landed on tick T+1 while EDBEE fired on tick T, bypassing the same-tick dedupe.
		//
		// So VANILLA melee damage is suppressed outright.  It used to be suppressed only for a Mage's iron/stone
		// sword, back when the mage beam was the one melee-ish thing the plugin applied itself; every other class's
		// swing fell through to vanilla, which is a handful of hearts against a mob whose HP is SB/1e6 and is the
		// wrong number by six orders of magnitude.  Now that CustomItems.meleeAttack applies every swing at SkyBlock
		// scale, vanilla must not land on top of any of them - the same "vanilla is not a participant" rule the rest
		// of MAP.md §7 runs on.  Knockback goes with it, which is fine: nothing in the floor depends on
		// melee knock.
		if(e.getDamager() instanceof Player) e.setCancelled(true);
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		// Superboom TNT carries a can_break stamp purely so the adventure-mode client reports the clicked block on a
		// left-click (see Utils.placeAndBreakAnythingInAdventure).  It must never actually break anything.  The
		// left-click interact event is already cancelled for it in handleCustomItems, so this is the backstop.  It
		// matters because the fall-through below would otherwise remove the block PERMANENTLY.
		if(getID(e.getPlayer().getInventory().getItemInMainHand()).equals("skyblock/combat/infinityboom")) {
			e.setCancelled(true);
			return;
		}
		// CREATIVE BYPASS: a creative-mode player breaks anything, anywhere, past every protection below, and does it
		// through vanilla (physics and all) rather than our no-physics path - that's what someone editing the map
		// expects.  This is the same bypass GoldorListener already grants for the S3 item frames.  It sits BELOW the
		// Superboom check on purpose: that one is item behaviour (the TNT must never break a block), not a protection.
		if(e.getPlayer().getGameMode() == GameMode.CREATIVE) return;
		// Protected Goldor interactables and the Maxor Energy-Crystal pressure plates are unbreakable outright, with
		// any tool (stonk, dungeonbreaker, …) and in any phase.
		if(Goldor.INSTANCE.isProtected(e.getBlock()) || Maxor.INSTANCE.isProtected(e.getBlock())) {
			e.setCancelled(true);
			return;
		}
		// Static-map fixtures: secret chests, the Quiz answer buttons, wither-skeleton skulls (Wither Essence)
		// anywhere, and the Ice Fill puzzle's ice and polished andesite.  Gated on NOTHING - the Dungeonbreaker works
		// whatever the run is doing, and its break writes AIR into the world for good, so one break outside a run
		// takes that block out of every run after it as well.
		if(instructions.clear.ClearManager.isMapFixture(e.getBlock())) {
			e.setCancelled(true);
			return;
		}
		// ONCE THE RUN HAS STARTED, two more things lock: every door (frame included, see Rooms.Door) and every room's
		// ceiling.  Both are open during the pre-run prep window, which is the same deal the out-of-bounds kill gives
		// the crevices: get into position however you like before the countdown ends, but no shortcuts during the run.
		// The wither/blood doors were already covered by this rule; Rooms.inDoor now covers all 15 plus their frames,
		// and the key + door-click path in MiscListener is still the only way to open one.
		Block b = e.getBlock();
		if(Server.isRunStarted()
				&& (instructions.clear.Rooms.inDoor(b.getX(), b.getY(), b.getZ())
				|| instructions.clear.Rooms.isCeiling(b.getX(), b.getY(), b.getZ()))) {
			e.setCancelled(true);
			return;
		}
		// The vertical faces (perimeter walls) of a room can't be broken through.  Only the floor and the room interior
		// can be stonked.  I restrict by the room's horizontal perimeter at any Y, because rooms have varying heights
		// and a Y-based wall rule is impossible.  Multi-cell rooms (e.g. the 2x2 Museum) protect only their OUTER
		// perimeter, so the middle of the room stays stonkable.  The three doors (start / wither / blood) sit in these
		// walls and must remain stonkable pre-run, so they're exempt.
		if(instructions.clear.Rooms.isRoomFace(b.getX(), b.getZ())
				&& !Server.inStartDoor(b) && !Server.inWitherDoor(b) && !Server.inBloodDoor(b)) {
			e.setCancelled(true);
			return;
		}
		// Do the removal ourselves WITHOUT physics instead of letting vanilla break the block.  A vanilla break runs
		// updateNeighbourShapes on the six neighbours, which tears out anything support-dependent sitting on or
		// against the block: carpets, torches, flowers, rails, redstone.  It also cascades a nether portal to air via
		// its frame-completeness check.  That removal path (updateShape → destroyBlock) fires NO BlockPhysicsEvent, so
		// it can't be vetoed from a listener.  The only place to stop it is here, at the source.  setType(AIR, false)
		// uses applyPhysics=false, which skips the neighbour shape updates entirely, so nothing attached pops off.
		e.setCancelled(true);
		if(getID(e.getPlayer().getInventory().getItemInMainHand()).equals("skyblock/combat/stonk")) {
			stonk(e.getPlayer(), e.getBlock()); // temporary: removes the block no-physics + restores it after 200 ticks
		} else {
			e.getBlock().setType(Material.AIR, false); // permanent break (Dungeonbreaker, etc.), still no-physics
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
		// The drop key is an ABILITY key here, never a way to lose an item, so cancel FIRST, for everyone.  This used
		// to be gated on being an Archer or a Mage (the only two classes with a drop ability), which meant a Berserk,
		// Healer, Tank, or anyone with no class tag at all physically threw their kit item on the floor.  Whether the
		// class has an ability to fire is a separate question, answered by dispatchDrop.
		e.setCancelled(true);
		boolean ultimate = !p.isSprinting();
		// Real players get their ability from the interceptor's DROP_ITEM/DROP_ALL_ITEMS path, which is not
		// rate-limited by vanilla's drop handling.  This handler only owns the cancel for them.
		if(!FakePlayerManager.getFakePlayers().containsValue(p)) return;
		dispatchDrop(p, ultimate);
	}

	public static void handleDrop(Player p, boolean ultimate) {
		if(Spectate.getSpectatorMap().containsKey(p)) return;
		droppingPlayers.add(p.getUniqueId());
		Utils.scheduleTask(() -> droppingPlayers.remove(p.getUniqueId()), 1);
		dispatchDrop(p, ultimate);
	}

	private static void dispatchDrop(Player p, boolean ultimate) {
		int now = MinecraftServer.currentTick;
		UUID id = p.getUniqueId();
		if(p.getName().equals("Archer") || p.getScoreboardTags().contains("Archer")) {
			if(ultimate) {
				if(now < rapidFireReady.getOrDefault(id, 0)) { sendCooldownMessage(p, rapidFireReady.getOrDefault(id, now) - now); return; } // Rapid Fire: 100s
				rapidFireReady.put(id, now + 2000);
				rapidFire(p);
			} else {
				if(now < explosiveShotReady.getOrDefault(id, 0)) { sendCooldownMessage(p, explosiveShotReady.getOrDefault(id, now) - now); return; } // Explosive Shot: 20s
				explosiveShotReady.put(id, now + 400);
				explosiveShot(p);
			}
		} else if(p.getName().startsWith("Mage") || p.getScoreboardTags().contains("Mage")) {
			if(!ultimate) {
				if(now < guidedSheepReady.getOrDefault(id, 0)) { sendCooldownMessage(p, guidedSheepReady.getOrDefault(id, now) - now); return; } // Guided Sheep: 30s
				guidedSheepReady.put(id, now + effectiveCooldown(p, GUIDED_SHEEP_COOLDOWN_TICKS));
				guidedSheep(p);
			}
		} else if(p.getName().startsWith("Berserk") || p.getScoreboardTags().contains("Berserk")) {
			// MAP.md §1.14.  The ultimate (`drop`) is x1.5 melee for 15s on a 60s cooldown, read by
			// damage/ClassBonuses through damage/CombatState; the regular ability (`drop stack`) throws an axe for
			// the player's highest hit in the last 60s, off the shared rolling damage history.
			if(ultimate) {
				if(now < berserkUltimateReady.getOrDefault(id, 0)) { sendCooldownMessage(p, berserkUltimateReady.getOrDefault(id, now) - now); return; }
				berserkUltimateReady.put(id, now + BERSERK_ULTIMATE_COOLDOWN_TICKS);
				damage.CombatState.startBerserkUltimate(p);
				Utils.playLocalSound(p, Sound.ENTITY_WITHER_SPAWN, 1.0F, 2.0F);
				p.sendMessage(Utils.msg("<red>Ragnarok! <gray>Your melee hits deal <red>1.5x<gray> damage for 15 seconds."));
			} else {
				if(now < berserkThrowReady.getOrDefault(id, 0)) { sendCooldownMessage(p, berserkThrowReady.getOrDefault(id, now) - now); return; }
				berserkThrowReady.put(id, now + BERSERK_THROW_COOLDOWN_TICKS);
				// "The axe throw copies the Axe of the Shredded ability but does NOT pierce", so one target only.
				// The figure is the highest hit in the last 60s, i.e. already finished - hence derived.
				throwAxe(p, damage.CombatState.maxInLastTicks(p, 1200), false, true);
			}
		}
	}

	@EventHandler
	public void onEntityShootBow(EntityShootBowEvent e) {
		if(e.getEntity() instanceof Player p) {
			ItemStack bow = p.getInventory().getItemInMainHand();
			// Vanilla's charge, min(useTicks/20, 1).  A DRAWN bow scales its damage by this AND loses the whole
			// crit term below a full draw (MAP.md §1.4), which makes a partial draw much worse than the
			// fraction alone suggests.  The Terminator is a shortbow and never comes through here.
			double charge = Math.clamp(e.getForce(), 0f, 1f);
			String id = getID(bow);
			if(id.equals("skyblock/combat/explosive_bow")) {
				if(e.getProjectile() instanceof Arrow primary) {
					// Re-aim the vanilla primary IN PLACE, with no cancel and no second entity: just override its
					// velocity with the clean eye direction to strip the random spread (inaccuracy 1.0).  The bonus
					// arrows are new entities, so they go through the deterministic spawner.  aimFrom/speed are
					// captured once so the staggered bonus arrows spawn at the same point and direction.
					Location aimFrom = p.getEyeLocation().clone();
					float speed = (float) primary.getVelocity().length();
					primary.setVelocity(aimFrom.getDirection().multiply(speed));
					primary.addScoreboardTag("ExplosiveBowArrow");
					primary.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
					damage.Arrows.stamp(primary, p, bow, charge, 1.0);
					boolean isArcher = p.getName().contains("Archer") || p.getScoreboardTags().contains("Archer");

					Utils.scheduleTask(() -> {
						Arrow a = Actions.fireDeterministicArrow(p, aimFrom, speed, 1.0);
						a.addScoreboardTag("ExplosiveBowArrow");
						damage.Arrows.stamp(a, p, bow, charge, 0.2); // Duplex's one extra arrow, at x0.2
						p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F);
					}, 3);

					if(isArcher) {
						// The Archer's two bonus arrows never build a Last Breath stack (moot here, since this is
						// the Explosive Bow, but the flag is set consistently at every bonus-arrow site).
						Utils.scheduleTask(() -> {
							Arrow a = Actions.fireDeterministicArrow(p, aimFrom, speed, 1.0);
							a.addScoreboardTag("ExplosiveBowArrow");
							damage.Arrows.stamp(a, p, bow, charge, 1.0, false);
							p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.2F);
						}, 5);

						Utils.scheduleTask(() -> {
							Arrow a = Actions.fireDeterministicArrow(p, aimFrom, speed, 1.0);
							a.addScoreboardTag("ExplosiveBowArrow");
							damage.Arrows.stamp(a, p, bow, charge, 1.0, false);
							p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.2F);
						}, 10);
					}
				}
				return;
			}
			if(id.equals("skyblock/combat/last_breath")) {
				if(!(e.getProjectile() instanceof Arrow primary)) return;
				// Re-aim the vanilla primary IN PLACE, with no cancel and no second entity: override its velocity with
				// the clean eye direction to strip the random spread.  Bonus arrows are new entities, so they go
				// through the deterministic spawner.  aimFrom/speed are captured once so the staggered bonus arrows
				// spawn at the same point and direction.
				Location aimFrom = p.getEyeLocation().clone();
				float speed = (float) primary.getVelocity().length();
				boolean isArcher = p.getName().contains("Archer") || p.getScoreboardTags().contains("Archer");

				primary.setVelocity(aimFrom.getDirection().multiply(speed));
				primary.addScoreboardTag("TerminatorArrow");
				primary.addScoreboardTag("LastBreathArrow");
				primary.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
				damage.Arrows.stamp(primary, p, bow, charge, 1.0);

				Utils.scheduleTask(() -> {
					Arrow a = Actions.fireDeterministicArrow(p, aimFrom, speed, 1.0);
					a.addScoreboardTag("TerminatorArrow");
					a.addScoreboardTag("LastBreathArrow");
					damage.Arrows.stamp(a, p, bow, charge, 0.2); // Duplex's one extra arrow, at x0.2
					p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F);
				}, 3);

				if(isArcher) {
					// The Archer's two BONUS arrows do NOT build Last Breath stacks, unlike the shot itself and
					// its Duplex arrow above.  Full damage, just no stack.
					Utils.scheduleTask(() -> {
						Arrow a = Actions.fireDeterministicArrow(p, aimFrom, speed, 1.0);
						a.addScoreboardTag("TerminatorArrow");
						a.addScoreboardTag("LastBreathArrow");
						damage.Arrows.stamp(a, p, bow, charge, 1.0, false);
						p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.2F);
					}, 5);

					Utils.scheduleTask(() -> {
						Arrow a = Actions.fireDeterministicArrow(p, aimFrom, speed, 1.0);
						a.addScoreboardTag("TerminatorArrow");
						a.addScoreboardTag("LastBreathArrow");
						damage.Arrows.stamp(a, p, bow, charge, 1.0, false);
						p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.2F);
					}, 10);
				}
			}
		}
	}

	public static void handleCustomItems(Cancellable e, EquipmentSlot hand, ItemStack item, Action action, Player p) {
		handleCustomItems(e, hand, item, action, p, null);
	}

	/**
	 * @param clickedBlock the block VANILLA reported this click landed on, either
	 *                     {@code PlayerInteractEvent.getClickedBlock()} or {@code ServerboundUseItemOnPacket}'s hit
	 *                     result.  It is {@code null} for an air click, an entity interaction, or a fake-player
	 *                     dispatch.  Abilities that act on a block (Superboom TNT) use this instead of ray-tracing a
	 *                     reach of their own, so their range is exactly vanilla's block-interaction range and their
	 *                     target is exactly the block the client aimed at.
	 */
	public static void handleCustomItems(Cancellable e, EquipmentSlot hand, ItemStack item, Action action, Player p, Block clickedBlock) {
		if(p.getGameMode() == org.bukkit.GameMode.SPECTATOR) return; // spectators never fire item abilities (e.g. a right-click on a block)
		if(Spectate.getSpectatorMap().containsKey(p)) return;
		// Trap room disables all right-click item abilities EXCEPT ender pearls and Dungeonbreaker.
		if((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
				&& instructions.clear.ClearManager.isActive()
				&& instructions.clear.Rooms.roomAt(p.getLocation()) == instructions.clear.Rooms.TRAP) {
			boolean allowed = item != null && (item.getType() == Material.ENDER_PEARL || getID(item).equals("skyblock/combat/stonk"));
			if(!allowed) return;
		}
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
					boolean isMageBeamItem = isMageBeamItem(p, item);
					// Items whose left-click is an ability are weapons and wands, never pickaxes.  Their left-click
					// must NEVER break a block, even when the ability is on cooldown or capped by the 1/tick guard.
					boolean leftClickAbilityItem = leftClickIsAbility(p, item);
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
									superboom(p, clickedBlock);
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
					// followed by UseItem (RIGHT_CLICK_AIR).  These normally land on the same tick and collapse via
					// the `cooldowns` gate below, but the first click after a server restart can straddle a
					// tick boundary from one-time warmup lag, and then the trailing AIR fires the ability twice.
					// Drop an AIR that trails a BLOCK click by at most one tick.  Genuine standalone air-clicks
					// carry no recent BLOCK so they still fire, and fake-player air-spam sends no BLOCK at all.
					if(action.equals(Action.RIGHT_CLICK_BLOCK)) {
						lastRightBlockTick.put(p.getUniqueId(), currentTick);
					} else {
						int blockTick = lastRightBlockTick.getOrDefault(p.getUniqueId(), Integer.MIN_VALUE);
						if(currentTick == blockTick || currentTick == blockTick + 1) return;
					}
					if(currentTick >= cooldowns.getOrDefault(p.getUniqueId(), 0)) {
						cooldowns.put(p.getUniqueId(), currentTick + RIGHT_CLICK_GATE_TICKS);
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
								superboom(p, clickedBlock);
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
							case "skyblock/combat/golem_sword" -> {
								UUID uid = p.getUniqueId();
								if(currentTick >= golemSwordReady.getOrDefault(uid, 0)) {
									golemSword(p);
									golemSwordReady.put(uid, currentTick + effectiveCooldown(p, GOLEM_SWORD_COOLDOWN_TICKS));
									fired = true;
								} else {
									sendCooldownMessage(p, golemSwordReady.getOrDefault(uid, currentTick) - currentTick);
								}
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
								// Don't fire here, just record the right-click.  pollTerminators() decides whether a
								// shot fires this tick or next based on the 5-tick (or Thermo 4-tick) cooldown.
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
		double dealt = 0;
		ItemStack wand = p.getInventory().getItemInMainHand();
		for(Entity entity : entities) {
			// Never damage players, whether real, fake or spectating.  This matches the other AoE abilities
			// (iceSpray, the AOTS beam, terminator).  The old fake-player-only exclusion let implosion hit
			// fellow practicers.
			if(!doNotKill.contains(entity.getType()) && entity instanceof LivingEntity entity1 && !(entity instanceof Player) && entity1.getHealth() > 0 && !(entity instanceof Wither wither && wither.getInvulnerableTicks() != 0)) {
				// Wither Impact: 10,000 base at 0.3 Intelligence scaling (MAP.md §7), through the ability
				// formula - so no Strength and no Crit Damage, which is why abilities read so differently from
				// the beam.  That is deliberate: they are an option, not a damage strategy.
				double sbDamage = damage.Damage.ability(p, entity1, wand);
				// Sum what DEAL reports, not what we asked for: the message has to read the same as the numbers in
				// the air, i.e. after the target's defense and resistance.  A target that took nothing at all (the
				// Wither King, a villager NPC) isn't counted as hit either.
				double hit = damage.Damage.deal(entity1, sbDamage, damage.DamageKind.MAGIC, p, damage.DamagePath.ABILITY);
				if(hit > 0) {
					dealt += hit;
					damaged += 1;
				}
			}
		}
		if(damaged > 0) {
			p.sendMessage(Utils.msg("<red>Your Implosion hit " + damaged + " enemies for "
					+ damage.Damage.integer(dealt) + " damage"));
		}
		Utils.playLocalSound(p, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

		// wither shield sound, on a 100-tick cooldown per player.
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
								// This is a 1-block gap at or above original height, so skip it
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

			noRotateTeleport(p, l);
			Utils.debug(Utils.DebugType.SERVER, "Teleporting " + p.getName() + " to " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
		} else {
			switch(result.getHitBlockFace()) {
				case SELF -> {
					// empty case
				}
				case UP -> {
					Location l = result.getHitBlock().getLocation().add(0.5, 1, 0.5);
					noRotateTeleport(p, l);
					Utils.debug(Utils.DebugType.SERVER, "Teleporting " + p.getName() + " to " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
				}
				case DOWN -> {
					Location l = result.getHitBlock().getLocation().add(0.5, -2, 0.5);
					noRotateTeleport(p, l);
					Utils.debug(Utils.DebugType.SERVER, "Teleporting " + p.getName() + " to " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
				}
				default -> {
					// Hit a side face, so backtrack until we find a safe spot
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
								noRotateTeleport(p, l);
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
						noRotateTeleport(p, l);
						Utils.debug(Utils.DebugType.SERVER, "Teleporting " + p.getName() + " to " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
					}
				}
			}
		}
		p.setFallDistance(0);
		Utils.playLocalSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
	}

	/**
	 * Teleport a player without touching where they are looking. Yaw/pitch go out as RELATIVE in the position packet,
	 * so the client applies a delta to whatever it is currently looking at instead of being snapped to an absolute
	 * rotation.  A high-ping player who turned their head between clicking and the teleport landing keeps the head
	 * they turned to, rather than being yanked back to the rotation the server last knew about.
	 * <br>
	 * The rotation delta must be ZERO (hence the 0/0 below): relative components are OFFSETS from the current
	 * rotation, not absolutes (vanilla {@code PositionMoveRotation.calculateAbsolute}), so passing the player's own
	 * yaw in would add it on top and spin them.
	 * <br>
	 * Goes through the connection rather than {@code Player#teleport(Location, cause, TeleportFlag...)}: Paper
	 * deprecated {@code TeleportFlag.Relative.X/Y/Z/YAW/PITCH} for removal in 1.21.3, leaving no Bukkit-API way to
	 * ask for a relative rotation.  This overload is CraftBukkit's own, so it still fires {@code PlayerTeleportEvent}
	 * (cause PLUGIN) and honours a cancel.  Same-world only, which every caller here is.
	 */
	private static void noRotateTeleport(Player p, Location l) {
		ServerPlayer sp = ((CraftPlayer) p).getHandle();
		sp.connection.teleport(
				new PositionMoveRotation(new Vec3(l.getX(), l.getY(), l.getZ()), Vec3.ZERO, 0f, 0f),
				Set.of(Relative.Y_ROT, Relative.X_ROT),
				PlayerTeleportEvent.TeleportCause.PLUGIN);
	}

	public static void aotv(Player p) {
		// Aspect of the Void / etherwarp is disabled only inside the boss room while in adventure mode, the practice
		// default.  It can't be used to skip boss mechanics, but still works freely everywhere else.
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
				p.setFallDistance(0);
				Utils.playLocalSound(p, Sound.ENTITY_ENDER_DRAGON_HURT, 1, 0.50F);
				Utils.debug(Utils.DebugType.SERVER, "Etherwarping " + p.getName() + " to " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
				noRotateTeleport(p, l);
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
									// This is a 1-block gap at or above original height, so skip it
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
				noRotateTeleport(p, l);
				Utils.debug(Utils.DebugType.SERVER, "Teleporting " + p.getName() + " to " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
			} else {
				switch(result.getHitBlockFace()) {
					case SELF -> {
						// empty case
					}
					case UP -> {
						Location l = result.getHitBlock().getLocation().add(0.5, 1, 0.5);
						noRotateTeleport(p, l);
						Utils.debug(Utils.DebugType.SERVER, "Teleporting " + p.getName() + " to " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
					}
					case DOWN -> {
						Location l = result.getHitBlock().getLocation().add(0.5, -2, 0.5);
						noRotateTeleport(p, l);
						Utils.debug(Utils.DebugType.SERVER, "Teleporting " + p.getName() + " to " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
					}
					default -> {
						// Hit a side face, so backtrack until we find a safe spot
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
									noRotateTeleport(p, l);
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
							noRotateTeleport(p, l);
							Utils.debug(Utils.DebugType.SERVER, "Teleporting " + p.getName() + " to " + Utils.round(l.getX(), 3) + " " + Utils.round(l.getY(), 5) + " " + Utils.round(l.getZ(), 3));
						}
					}
				}
			}
			p.setFallDistance(0);
			Utils.playLocalSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
		}
	}

	/** Crypts already blown up this run, keyed by min-corner.  A crypt can't be farmed for repeated kills. */
	private static final Set<String> activatedCrypts = new HashSet<>();

	/** Clear the per-run crypt-farm guard (called at run start). */
	public static void resetCrypts() {
		activatedCrypts.clear();
	}

	public static boolean checkAndActivateCrypt(Block clicked, Player p) {
		// There are no crypts in the boss arena - it's a clear-phase secret - but the arena's decorative
		// smooth-stone-slab / stone-brick-stair terrain passes the rectangle test anyway (a lone bottom slab with
		// air under it is a valid 1x1 crypt, and any gold block in that layer makes it a Prince).  So every
		// Superboom thrown in there opened a hole in the floor and handed out a free Crypt Lurker or Prince,
		// which then counted toward the clear phase's bonus score.  Refuse before validating anything; returning
		// false lets triggerSuperboomAt fall through to the cracked-brick flood-fill, which IS wanted in there.
		if(LavaJump.isInBossArena(clicked.getLocation())) return false;
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

		// A crypt can only be farmed once: it still opens + restores visually, but no new lurker spawns on repeat.
		String cryptKey = minX + "," + slabY + "," + minZ;
		boolean alreadyBlownUp = !activatedCrypts.add(cryptKey);
		if(alreadyBlownUp) {
			p.sendMessage(Utils.msg("<red>You have already blown up this crypt!"));
		}

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
		Zombie mob = alreadyBlownUp ? null : Server.spawnCryptLurker(spawnLoc, isPrince);

		pendingBlockRestorations.putAll(stored);
		if(mob != null) pendingCryptMobs.add(mob);
		BukkitTask[] holder = new BukkitTask[1];
		holder[0] = Bukkit.getScheduler().runTaskLater(M7tas.getInstance(), () -> {
			for(Map.Entry<Location, BlockData> entry : stored.entrySet()) {
				entry.getKey().getBlock().setBlockData(entry.getValue(), false);
				pendingBlockRestorations.remove(entry.getKey());
			}
			if(mob != null) {
				if(mob.isValid()) {
					// The lurker/prince was never killed before the crypt regenerated, so this crypt doesn't
					// count as "used".  Un-mark it so it can be blown up again for another attempt at the kill.
					// A killed lurker leaves the mob invalid here, so the key stays and the crypt is spent.
					mob.remove();
					activatedCrypts.remove(cryptKey);
				}
				pendingCryptMobs.remove(mob);
			}
			pendingBlockTasks.remove(holder[0]);
		}, 40);
		pendingBlockTasks.add(holder[0]);

		return true;
	}

	/**
	 * Detonate a Superboom TNT against the block the click landed on. There is deliberately **NO server-side ray
	 * trace** here: {@code clicked} is always the block the CLIENT reported, so reach and target are vanilla's, not an
	 * approximation of them. An own ray trace was both too generous (a fixed 5 blocks, past what the client considers
	 * interactable) and subtly wrong (it skipped passable blocks, so aiming at a lever centred on the wall behind it).
	 * <p>
	 * Every click path now carries a block: RIGHT_CLICK_BLOCK from {@code PlayerInteractEvent.getClickedBlock()} /
	 * {@code ServerboundUseItemOnPacket}'s hit result, LEFT_CLICK_BLOCK from {@code ServerboundPlayerActionPacket}'s
	 * pos.  That is why the TNT carries a can_break stamp (see {@code Utils.placeAndBreakAnythingInAdventure}): without
	 * it the adventure-mode client sends no block-attack packet at all.  A real placement comes from
	 * {@code BlockPlaceEvent.getBlockAgainst()}.  A null {@code clicked} therefore means the client itself saw nothing
	 * interactable (air click) or the click was consumed by an entity.  Vanilla would place no TNT, so nothing booms.
	 */
	public static void superboom(Player p, Block clicked) {
		if(clicked == null) return;
		superboomAt(p, clicked.getLocation());
	}

	/**
	 * Detonate a Superboom TNT centred on {@code center}, at most once per player per tick.  Shared by the ability
	 * dispatch (every click path) and by {@link #onInfinityboomPlace(BlockPlaceEvent)}, which covers the paths where
	 * vanilla physically places the TNT block instead of a click firing the ability.
	 */
	private static void superboomAt(Player p, Location center) {
		int currentTick = MinecraftServer.currentTick;
		if(currentTick == lastSuperboomTick.getOrDefault(p.getUniqueId(), -1)) return;
		lastSuperboomTick.put(p.getUniqueId(), currentTick);
		triggerSuperboomRadius(center, p);
	}

	/**
	 * Catch-all so an Infinityboom TNT is NEVER left in the world as a real block.  Most click paths fire the ability
	 * and cancel the interact event, so vanilla never places anything.  But every path where the click ISN'T consumed
	 * by the ability still lets vanilla place the TNT, since it carries can-place-on-anything for adventure mode:
	 * sneak-right-click on a lever / button / clear-phase secret (all of which return early in onPlayerInteract so the
	 * block keeps its click), a right-click inside the Trap room (right-click abilities disabled there), an off-hand
	 * placement, and presumably more.  Rather than chase each one, veto the placement here and detonate instead.
	 * BlockPlaceEvent fires exactly when vanilla decided to place, which is exactly when no ability consumed the click,
	 * so the TNT behaves identically however it was used.  Cancelling also keeps the stack intact, since Infinityboom
	 * is never consumed.  Creative mode is left alone so setup and building can still place real TNT.
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInfinityboomPlace(BlockPlaceEvent e) {
		if(!getID(e.getItemInHand()).equals("skyblock/combat/infinityboom")) return;
		Player p = e.getPlayer();
		org.bukkit.GameMode gm = p.getGameMode();
		if(gm != org.bukkit.GameMode.SURVIVAL && gm != org.bukkit.GameMode.ADVENTURE) return;
		e.setCancelled(true);
		p.updateInventory(); // the client predicted the placement, so resend the unchanged stack and it won't ghost
		// Mirror the Trap-room restriction in handleCustomItems: no right-click item abilities in there.
		if(instructions.clear.ClearManager.isActive()
				&& instructions.clear.Rooms.roomAt(p.getLocation()) == instructions.clear.Rooms.TRAP) return;
		// Centre on the block clicked against, the same "block vanilla says was interacted with" the click path uses.
		superboomAt(p, e.getBlockAgainst().getLocation());
	}

	public static void triggerSuperboomRadius(Location center, Player p) {
		triggerSuperboomRadius(center, p, new HashSet<>());
	}

	public static void triggerSuperboomRadius(Location center, Player p, Set<Block> visited) {
		// Notify Goldor of any explosion-style impact (Superboom, Explosive Shot, Guided Sheep all route through here).
		instructions.bosses.goldor.Goldor.INSTANCE.notifyExplosionAt(center);
		World world = center.getWorld();
		int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
		for(int dx = -SUPERBOOM_RADIUS; dx <= SUPERBOOM_RADIUS; dx++) {
			for(int dy = -SUPERBOOM_RADIUS; dy <= SUPERBOOM_RADIUS; dy++) {
				for(int dz = -SUPERBOOM_RADIUS; dz <= SUPERBOOM_RADIUS; dz++) {
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
		if(Goldor.INSTANCE.isProtected(b) || Maxor.INSTANCE.isProtected(b)) return;
		if(b.getType().getHardness() != -1) {
			Material m = b.getType();
			BlockData data = b.getBlockData().clone();
			Location loc = b.getLocation();
			Utils.debug(Utils.DebugType.SERVER, p.getName() + " Stonking block at " + Utils.round(loc.getX(), 3) + " " + Utils.round(loc.getY(), 5) + " " + Utils.round(loc.getZ(), 3));

			b.setType(Material.AIR, false); // no-physics: attached neighbours (carpets, portals, …) don't pop off
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

	/** Ragnarock Axe: a 3s wind-up (three lever clicks) and then the buff.  The axe must stay in the main hand for
	*  the whole wind-up; see {@link #ragWindup}. */
	public static void rag(Player p) {
		Utils.playLocalSound(p, Sound.BLOCK_LEVER_CLICK, 1.0F, 2.0F);
		ragCastStart.put(p.getUniqueId(), MinecraftServer.currentTick);
		ragWindup(p, MinecraftServer.currentTick, 0);
	}

	/**
	 * One tick of the Ragnarock wind-up: the remaining lever clicks at +20/+40 and the buff at +60.  Re-checks EVERY
	 * tick that the axe is still in the main hand and drops the cast the moment it isn't.  Holding the axe for the
	 * full 3s is the ability's cost, so swapping to a weapon during the wind-up must not land the buff.  The cooldown
	 * is deliberately NOT refunded, since it is spent at cast time, as on Hypixel.
	 */
	private static void ragWindup(Player p, int castStart, int elapsed) {
		Utils.scheduleTask(() -> {
			UUID uid = p.getUniqueId();
			// Superseded by a later cast, or already cancelled/reset, so this chain is dead.

			if(ragCastStart.getOrDefault(uid, Integer.MIN_VALUE) != castStart) return;
			if(!p.isOnline() || !getID(p.getInventory().getItemInMainHand()).equals("skyblock/combat/rag")) {
				ragCastStart.remove(uid);
				Utils.debug(Utils.DebugType.SERVER, "Rag cast cancelled (axe left the hand) for " + Utils.getRealName(p));
				return;
			}
			int tick = elapsed + 1;
			if(tick == 20 || tick == 40) {
				Utils.playLocalSound(p, Sound.BLOCK_LEVER_CLICK, 1.0F, 2.0F);
			}
			if(tick < 60) {
				ragWindup(p, castStart, tick);
				return;
			}
			ragCastStart.remove(uid);
			Utils.playLocalSound(p, Sound.ENTITY_WOLF_WHINE, 1.0F, 1.5F);
			p.addScoreboardTag(damage.RagnarockBuff.TAG);
			// Buff expires 200 ticks (10s) after THIS application; a later cast overwrites this, extending the buff.
			ragBuffExpiry.put(uid, MinecraftServer.currentTick + 200);
			// The buff is +150% of the AXE'S OWN Strength stat, granted as a bonus stat through the stat layer
			// (MAP.md §1.7) - not the vanilla Strength potion effect it used to be, and not the flat
			// 220->250 damage swap either.  The stat layer reads the tag, so nothing is applied to the player
			// here; damage/Stats.ragnarockStrength computes the figure from the axe's authored terms, which is
			// why it tracks a retune of the axe automatically.
			// It also keeps applying after the axe leaves the hand: casting Ragnarock and THEN switching to a
			// hitting weapon is the entire point of the item.
			damage.Stats.invalidate(p);
			Utils.debug(Utils.DebugType.SERVER, "Rag Buff applied to " + Utils.getRealName(p));
			// Remove only once the latest expiry is reached: a second cast refreshes ragBuffExpiry, so this earlier
			// cast's removal sees currentTick < expiry and no-ops, leaving the tag for the later cast to clear.
			Utils.scheduleTask(() -> {
				if(MinecraftServer.currentTick >= ragBuffExpiry.getOrDefault(uid, 0)) {
					p.removeScoreboardTag(damage.RagnarockBuff.TAG);
					ragBuffExpiry.remove(uid);
					damage.Stats.invalidate(p);
					Utils.debug(Utils.DebugType.SERVER, "Rag Buff expired for " + Utils.getRealName(p));
				}
			}, 200);
		}, 1);
	}

	/**
	 * The Terminator's left-click beam.  It is NOT a bow shot (MAP.md §1.4), so it is never draw-scaled -
	 * it always resolves at full charge, and therefore always crits.  Its old flat 20 is gone; it is now the
	 * Terminator's own stat block through the bow formula.
	 */
	public static void salvation(Player p) {
		ItemStack weapon = p.getInventory().getItemInMainHand();
		Location l = p.getLocation();
		l.add(0, 1.62, 0);

		Vector v = l.getDirection();
		v.setX(v.getX() / 3);
		v.setY(v.getY() / 3);
		v.setZ(v.getZ() / 3);
		World world = l.getWorld();
		Set<Entity> damagedEntities = new HashSet<>();
		List<EntityType> doNotKill = doNotKill();
		damagedEntities.add(p);
		int pierce = 5;
		for(int i = 0; i < 192 && pierce > 0; i++) {
			if(l.getBlock().getType().isSolid()) {
				break;
			}
			assert world != null;
			ArrayList<Entity> entities = (ArrayList<Entity>) world.getNearbyEntities(l, 1, 1, 1);
			for(Entity entity : entities) {
				if(!damagedEntities.contains(entity) && !doNotKill.contains(entity.getType()) && entity instanceof LivingEntity entity1 && !(entity instanceof Player) && entity1.getHealth() > 0 && !(entity instanceof Wither wither && wither.getInvulnerableTicks() != 0)) {
					damagedEntities.add(entity);
					double sbDamage = damage.Damage.bow(p, entity1, weapon, 1.0,
							l.distance(p.getLocation()), false);
					damage.Damage.deal(entity1, sbDamage, damage.DamageKind.NORMAL, p, damage.DamagePath.BOW);
					pierce--;
				}
			}
			Particle.DustOptions particle = new Particle.DustOptions(Color.RED, 1.0F);
			world.spawnParticle(Particle.DUST, l, 1, particle);
			l.add(v);
		}
		Utils.playLocalSound(p, Sound.ENTITY_GUARDIAN_DEATH, 0.5f, 2.0F);
	}

	/**
	 * The Axe of the Shredded's throw: <b>10% of the wielder's melee damage</b>, with consecutive throws doubling
	 * it (and their mana cost) up to a x16 cap (MAP.md §1.9).  It also has to <b>take aggro when it hits a
	 * wither</b>, which is a deliberate requirement rather than incidental, and is what the axe's flight already
	 * did before it dealt any damage at all.
	 */
	public static void aots(Player p) {
		UUID id = p.getUniqueId();
		int now = MinecraftServer.currentTick;
		// A "consecutive" throw is one inside the streak window; letting it lapse resets the doubling to x1.
		int streak = now <= aotsStreakExpiry.getOrDefault(id, 0) ? aotsStreak.getOrDefault(id, 0) + 1 : 0;
		aotsStreak.put(id, streak);
		aotsStreakExpiry.put(id, now + AOTS_STREAK_TICKS);
		double core = damage.Damage.meleeCore(p) * AOTS_THROW_SHARE
				* Math.min(Math.pow(2, streak), AOTS_THROW_CAP);
		throwAxe(p, core, true, false);
	}

	/**
	 * The shared thrown-axe projectile: an ItemDisplay flying 100 blocks, spinning, damaging what it passes
	 * through.  Used by the Axe of the Shredded ({@code pierce} true) and by a Berserk's {@code drop stack}
	 * ability, which copies it but does NOT pierce (§1.14).
	 *
	 * @param derived what {@code core} IS.  False for the Axe of the Shredded, whose core is a stat core and still
	 *                needs the target half at {@code meleeFinish}.  True for the Berserk throw, whose core was read
	 *                out of the damage history and is therefore a FINISHED hit: running the target half on it would
	 *                charge for the Rulers, the repeated-hit stack and the class multiplier a second time, and
	 *                recording the result would let each throw read the last one's inflated output (see
	 *                {@link damage.Damage#dealDerived}).
	 */
	private static void throwAxe(Player p, double core, boolean pierce, boolean derived) {
		Utils.playLocalSound(p, Sound.BLOCK_LAVA_POP, 1.0F, 1.0F);
		ItemStack weapon = p.getInventory().getItemInMainHand();

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
			final Set<UUID> hit = new HashSet<>();

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

				// Taking aggro when it hits a wither is a REQUIREMENT of this ability, not incidental, so it is
				// noted the first tick the axe overlaps a boss whether or not the damage lands.  These projectiles
				// are one of only three things allowed to aggro a fully shielded wither - the mage beam and the
				// Flaming Flay arc are the others; everything else needs the hit to have dealt real damage.
				boolean stop = false;
				for(Entity e : currentLoc.getWorld().getNearbyEntities(currentLoc, 1.0, 2.0, 1.0)) {
					if(!notedAggro && e instanceof Wither w && w.getScoreboardTags().contains("TASWither")) {
						instructions.bosses.WitherActions.noteDamager(p);
						notedAggro = true;
					}
					if(!(e instanceof LivingEntity mob) || e instanceof Player) continue;
					if(mob.isDead() || mob.getHealth() <= 0 || !hit.add(mob.getUniqueId())) continue;
					if(e instanceof Wither w2 && w2.getInvulnerableTicks() != 0) continue;
					if(derived) {
						// The debuffs this hit carries still land (Lethality is a property of the hit, not of the
						// formula); only the damage half is skipped, because it is already in the figure.
						damage.Damage.applyOnHitDebuffs(p, mob, damage.DamagePath.MELEE, weapon);
						damage.Damage.dealDerived(mob, core, damage.DamageKind.NORMAL, p, damage.DamagePath.MELEE);
					} else {
						double sbDamage = damage.Damage.meleeFinish(p, mob, weapon, core);
						damage.Damage.deal(mob, sbDamage, damage.DamageKind.NORMAL, p, damage.DamagePath.MELEE);
					}
					if(!pierce) stop = true;
				}
				if(stop) {
					axe.remove();
					cancel();
					return;
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

	/**
	 * Golem Sword: kills the holder's vertical momentum.  Y velocity is zeroed, X/Z are left alone.  For a real
	 * player this rides out as a velocity packet, and the client's {@code lerpMotion} SETS its delta movement, so
	 * a fall or leap stalls on the spot instead of the value being added to whatever it was already doing.
	 * 3s cooldown, halved or quartered for a Mage like every other ability (see {@link #effectiveCooldown}).
	 */
	public static void golemSword(Player p) {
		Vector v = p.getVelocity();
		v.setY(0);
		p.setVelocity(v);
	}

	public static void iceSpray(Player p) {
		Location l = p.getEyeLocation();
		p.getWorld().spawnParticle(Particle.SNOWFLAKE, l, 256);
		List<Entity> entities = (List<Entity>) p.getWorld().getNearbyEntities(l, 8, 8, 8);
		List<EntityType> doNotKill = doNotKill();
		ItemStack wand = p.getInventory().getItemInMainHand();
		int debuffed = 0;
		int alreadyDebuffed = 0;
		for(Entity entity : entities) {
			if(!doNotKill.contains(entity.getType()) && entity instanceof LivingEntity entity1 && !(entity instanceof Player) && entity1.getHealth() > 0) {
				// Counted BEFORE the apply, which refreshes the window and would otherwise make every target read
				// as already debuffed.  A refresh still counts as "already debuffed", the same split SkyBlock in
				// Vanilla's wand reports - unlike that one, though, a refreshed target here still takes the damage.
				if(damage.TargetDebuffs.iceSprayed(entity1)) alreadyDebuffed++;
				else debuffed++;
				// The cast applies its x1.1 damage debuff to EVERY enemy within 8 blocks of the caster's eyes for
				// 5s, and it lands FIRST so the cast benefits from its own debuff (MAP.md §7).  The debuff
				// applies even to a target the damage cannot reach, e.g. an armoured wither.
				damage.TargetDebuffs.applyIceSpray(entity1);
				if(entity instanceof Wither wither && wither.getInvulnerableTicks() != 0) continue;
				// Ice Spray: 19,000 base at 0.1 Intelligence scaling.  The bigger base does not make up for the
				// scaling against Wither Impact's 0.3 (§7).
				double sbDamage = damage.Damage.ability(p, entity1, wand);
				damage.Damage.deal(entity1, sbDamage, damage.DamageKind.MAGIC, p, damage.DamagePath.ABILITY);
			}
		}
		if(debuffed > 0) {
			p.sendMessage(Utils.msg("<red>Your Ice Spray debuffed " + debuffed + " enemies."));
		}
		if(alreadyDebuffed > 0) {
			p.sendMessage(Utils.msg("<red>" + alreadyDebuffed + " enemies have already been debuffed."));
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
					if(hitEntities.contains(entity) || doNotKill.contains(entity.getType())) continue;
					if(!(entity instanceof LivingEntity entity1) || entity instanceof Player || entity1.getHealth() <= 0) continue;
					// The arc is one of the three things that may pull a boss's aggro through a FULL shield - the
					// mage beam and the thrown-axe projectiles are the others, and both note it at this same point,
					// their own armour check.  Everywhere else aggro needs the hit to have actually dealt damage, so
					// it has to be noted here rather than left to damage/Damage, which by then only sees a zero.
					if(entity instanceof Wither w && w.getScoreboardTags().contains("TASWither")) {
						instructions.bosses.WitherActions.noteDamager(p);
					}
					if(entity instanceof Wither armoured && armoured.getInvulnerableTicks() != 0) continue;
					// The Flaming Flay's ability deals the SAME as its melee hit (MAP.md §1.8), so it
					// goes through the melee formula rather than the ability one.
					double sbDamage = damage.Damage.melee(p, entity1, p.getInventory().getItemInMainHand());
					damage.Damage.deal(entity1, sbDamage, damage.DamageKind.NORMAL, p, damage.DamagePath.MELEE);
					hitEntities.add(entity);
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
			// The Watcher and the villager NPCs (Mort/Wizard) are immune to the Gyrokinetic Wand, so never
			// let the rift pull or hold them.  Boss entities (withers, ender dragons) are also immune.
			if(e instanceof LivingEntity entity && !(entity instanceof Player) && !(entity instanceof Wither)
					&& !(entity instanceof EnderDragon)
					&& !(entity instanceof Villager)
					&& !entity.getScoreboardTags().contains("TASWatcher")) {
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
		MovementAudit.startAirborneAudit(p, "jerrychine"); // SUPER-verbose per-tick trace of the knockback arc
		double horizSpeed = Math.hypot(direction.getX(), direction.getZ());
		Utils.debug(Utils.DebugType.SERVER, "Jerry-chine moved " + p.getName() + " " + Utils.round(direction.getX(), 3) + " " + Utils.round(direction.getY(), 5) + " " + Utils.round(direction.getZ(), 3));
	}

	@EventHandler
	public void onExplosiveBowArrowHit(ProjectileHitEvent e) {
		if(!(e.getEntity() instanceof Arrow arrow)) return;
		if(!arrow.getScoreboardTags().contains("ExplosiveBowArrow")) return;
		if(!(arrow.getShooter() instanceof Player p)) return;

		// On entity contact the arrow behaves like a normal arrow: its arrow damage and the hit ding are applied
		// by the normal damage path (WithersNotImmuneToArrows for a vulnerable wither).  On EITHER an entity or a
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

		// The Explosive Bow's own ability: every mob within 3 blocks takes the weapon's FULL damage
		// (MAP.md §1.9).  The directly-hit entity already took its arrow damage on the normal path, so it
		// is excluded here rather than hit twice.
		for(Entity nearby : impact.getWorld().getNearbyEntities(impact, 3, 3, 3)) {
			if(!(nearby instanceof LivingEntity mob) || nearby instanceof Player) continue;
			if(nearby.equals(e.getHitEntity()) || mob.isDead() || mob.getHealth() <= 0) continue;
			if(nearby instanceof Wither wither && wither.getInvulnerableTicks() != 0) continue;
			damage.Arrows.hit(arrow, p, mob, false);
		}

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
		// PlayerInteractEvent cancelled, because creative bypasses the arrow check.  Cancel the draw
		// next tick: by then vanilla has run and we can release it cleanly.
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

		// The Terminator is a SHORTBOW: it is never drawn, it just shoots, so draw scaling never applies and each
		// of its three arrows is one shot's worth of damage, unchanged by release timing (MAP.md §1.2).
		// Damage is stamped on each arrow at fire time (§1.0.5), so a mid-flight weapon swap cannot change it.
		//
		// The old hand-tuned terms are all gone: the Power/Strength-potion bonuses are now the weapon's own stat
		// block through the formula, the 97.5% Thermodynamic penalty is deleted (that set exists only to raise the
		// attack-speed cap to 150, i.e. the 4-tick cooldown below - it is a rate multiplier, not a per-hit one),
		// and the Spring Boots / Racing Helmet reductions are deleted too, those wearables now costing only the
		// stats their slot would otherwise carry.
		ItemStack bow = p.getInventory().getItemInMainHand();

		// Set Bukkit properties
		for(Arrow arrow : Arrays.asList(left, middle, right)) {
			arrow.setPierceLevel(4);
			arrow.setShooter(p);
			arrow.setWeapon(bow);
			arrow.addScoreboardTag("TerminatorArrow");
			arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
			damage.Arrows.stamp(arrow, p, bow, 1.0, 1.0);
		}

		for(Arrow arrow : Arrays.asList(left, middle, right)) {
			Utils.scheduleTask(() -> {
				if(arrow.isValid() && arrow.getLocation().getBlock().getType().isSolid()) arrow.remove();
			}, 1);
		}

		Utils.playLocalSound(p, Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F);

		// Duplex V: ONE extra arrow per shot at x0.2 of the main arrow's damage - its own damage instance, not a
		// multiplier on the shot.  Every bow runs Duplex, which is also why no bow ever carries Chimera (§7).
		Utils.scheduleTask(() -> fireExtraArrow(p, l, bow, 0.2, true), 3);

		if(p.getName().startsWith("Archer") || p.getScoreboardTags().contains("Archer")) {
			// The Archer's two BONUS arrows never build a Last Breath stack (moot on the Terminator, which is not
			// a Last Breath, but this method is the shared shape and the rule belongs with it).
			Utils.scheduleTask(() -> fireExtraArrow(p, l, bow, 1.0, false), 5);
			Utils.scheduleTask(() -> fireExtraArrow(p, l, bow, 1.0, false), 10);
		}
	}

	/**
	 * One follow-up Terminator arrow: the Duplex arrow at {@code share} 0.2, or an Archer's two extra full-damage
	 * arrows at 1.0.  Stamped like the main volley, so it carries its own damage (§1.0.5).
	 */
	private static void fireExtraArrow(Player p, Location l, ItemStack bow, double share, boolean buildsLastBreath) {
		Arrow arrow = p.getWorld().spawnArrow(l, l.getDirection(), 3.175f, 0);
		arrow.setPierceLevel(4);
		arrow.setShooter(p);
		arrow.setWeapon(bow);
		arrow.addScoreboardTag("TerminatorArrow");
		arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
		damage.Arrows.stamp(arrow, p, bow, 1.0, share, buildsLastBreath);
		Utils.playLocalSound(p, Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F);
		Utils.scheduleTask(() -> {
			if(arrow.isValid() && arrow.getLocation().getBlock().getType().isSolid()) arrow.remove();
		}, 1);
	}

	/**
	 * Clear the per-run combat state: the Berserk repeated-hit stack, the post-kill and combo windows, the rolling
	 * damage history, target debuff stacks, running procs and any floating damage numbers still in the world.
	 * Called at the start of every run.
	 * <p>
	 * The old hand-tuned Berserk ramp (+10% per hit on the same mob, cap 3x) is gone: MAP.md §1.14's real
	 * figures are +165% per repeated hit to a +950% cap (+180% / +1200% solo), applied as an additive damage
	 * source in {@code damage/ClassBonuses}, with the counters in {@code damage/CombatState}.
	 */
	public static void resetBerserkDamage() {
		damage.CombatState.reset();
		damage.TargetDebuffs.reset();
		damage.Procs.reset();
		damage.DamageNumbers.reset();
		damage.Stats.invalidateAll();
	}

	/** Clear terminator cooldown state.  Called at the start of every /tas and /m7practice run. */
	public static void resetTerminatorCooldowns() {
		termLastPacketTick.clear();
		termLastFireTick.clear();
		salvationReady.clear();
		mageBeamReady.clear();
		lastMeleeTick.clear();
	}

	/** Reset all class-ability and weapon-ability cooldowns (Guided Sheep / Rapid Fire / Explosive Shot, plus Gyro /
	*  Ragnarock / Ice Spray / Tactical Insertion).  Called on entering a boss fight (WitherLord.start) and at run start. */
	public static void resetAbilityCooldowns() {
		guidedSheepReady.clear();
		rapidFireReady.clear();
		explosiveShotReady.clear();
		gyroReady.clear();
		ragReady.clear();
		ragBuffExpiry.clear();
		ragCastStart.clear(); // also kills any wind-up chain still in flight (it no longer sees its own start tick)
		iceSprayReady.clear();
		tacReady.clear();
		golemSwordReady.clear();
		berserkUltimateReady.clear();
		berserkThrowReady.clear();
		aotsStreak.clear();
		aotsStreakExpiry.clear();
	}

	/** True if {@code p} is the Mage CLASS, which drives the ability-cooldown reduction.  Real players carry an
	*  exclusive class scoreboard tag (set by /class); fake players carry none and are identified by name.  All four
	*  fake "MageN" players run the Mage inventory and cast Mage abilities, so every "Mage*"-named fake counts as a
	*  Mage.  Mage2/3/4 cosplay Tank/Berserk/Healer's ROLE but are mechanically mages. */
	public static boolean isMageClass(Player p) {
		if(p.getScoreboardTags().contains("Mage")) return true;
		for(String other : new String[]{"Archer", "Berserk", "Healer", "Tank"}) {
			if(p.getScoreboardTags().contains(other)) return false;
		}
		return p.getName().startsWith("Mage");
	}

	/** A base ability cooldown after the Mage class's cooldown reduction: a SOLO mage gets −75% (quarter cooldown),
	*  but with two or more Mage-class players it's the standard −50% (half).  Non-mages are unchanged.  NOT used for
	*  the Terminator or Salvation, which are weapons, not abilities. */
	public static int effectiveCooldown(Player p, int baseTicks) {
		if(!isMageClass(p)) return baseTicks;
		return mageCount() <= 1 ? baseTicks / 4 : baseTicks / 2;
	}

	/** Number of Mage-class players currently online (see {@link #isMageClass}). */
	private static long mageCount() {
		return Bukkit.getOnlinePlayers().stream().filter(CustomItems::isMageClass).count();
	}

	/** Tell {@code p} their ability is on cooldown, showing the remaining time in seconds (e.g. "...for 3.45
	*  seconds!").  {@code ticksRemaining} is the ticks left until the ability is usable again. */
	private static void sendCooldownMessage(Player p, int ticksRemaining) {
		String format = String.format("%.2f", Math.max(0, ticksRemaining) / 20.0);
		p.sendMessage(Utils.msg("<red>This ability is on cooldown for " + format + " seconds!"));
		// During a TAS run (not practice) an ability fired on cooldown means the choreography mistimed it, so flag
		// it with the offending tick and player.  Gated behind regular verbose (ON+) so it doesn't spam the console.
		if(!instructions.bosses.WitherActions.isPracticeMode() && Utils.isVerbose()) {
			ItemStack held = p.getInventory().getItemInMainHand();
			String ability = held.hasItemMeta() && held.getItemMeta().hasDisplayName()
					? Utils.displayName(held.getItemMeta()) : String.valueOf(held.getType());
			Utils.debug(Utils.DebugType.ERROR, Utils.getRealName(p) + " tried to use " + ability + Utils.mmLegacy("<red>")
					+ " on cooldown (" + Math.max(0, ticksRemaining) + "t / " + format + "s left)");
		}
	}

	// The Spring Boots (x0.80) and Racing Helmet (x0.70) outgoing-damage penalties are DELETED, not re-bucketed
	// (MAP.md §1.10, §8), along with Utils.helmetDamageMultiplier's Cow Hat x0.70 and mask x0.85.  They
	// were a hand-tuned proxy for exactly what the stat layer now models properly: a helmet slot is exclusive, so
	// wearing a Cow Hat IS already giving up the Storm's Helmet's 3196.8 Intelligence, and keeping a x0.70 on top
	// double-penalised the same swap.  Every wearable now affects damage only through the stats it contributes -
	// or, for these three, doesn't.  Wearing a hat also costs the Golden Dragon (see damage/Pet), which is now the
	// entire cost.

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
	 * Per-tick terminator cooldown poller.  For each player who has recorded a terminator right-click since their
	 * last shot, fires a bow shot on the first tick at or after {@code lastFire + cooldown} (5 ticks, or 4 with the
	 * full Thermodynamic set).  Shots are anchored to the first shot and clamp to one per cooldown, so spamming the
	 * right-click can't exceed it.  Started from {@link plugin.M7tas}.
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
		// Tactical Insertion is disabled inside the boss room while in adventure mode, the practice default.  It
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

						// Explosive Shot: each arrow deals 100% of the player's highest arrow damage in the last
						// minute (MAP.md §1.14), read off the shared rolling damage history.  Dealt as a
						// DERIVED instance: the figure is already a finished hit, so it gets no second pass through
						// the formula and never goes back into the history it came out of.
						double sbDamage = damage.CombatState.maxInLastTicks(p, 1200);
						for(Entity e : arrow.getNearbyEntities(4, 4, 4)) {
							if(e instanceof LivingEntity target && !alreadyHurt.contains(target) && !(e instanceof Player) && !(target.hasPotionEffect(PotionEffectType.RESISTANCE) && target.getPotionEffect(PotionEffectType.RESISTANCE).getAmplifier() == 255) && !(e instanceof Wither wither && wither.getInvulnerableTicks() != 0)) {
								damage.Damage.dealDerived(target, sbDamage, damage.DamageKind.NORMAL, p,
										damage.DamagePath.BOW);
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
				arrow.setPierceLevel(4);
				arrow.setShooter(p);
				arrow.setWeapon(p.getInventory().getItemInMainHand());
				arrow.addScoreboardTag("TerminatorArrow");
				arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
				// Rapid Fire: each arrow deals 75% of the player's highest arrow damage in the last minute
				// (MAP.md §1.14), off the same rolling history Explosive Shot and the axe throw read.
				// 75% of a FINISHED hit, so stampFlat marks the arrow derived: it lands for this figure exactly and
				// stays out of the history.  Both matter - each of the 50 arrows re-queries the history four ticks
				// after the last one landed, so anything that let an arrow inflate what the next one reads compounds
				// fifty times and overflows.
				damage.Arrows.stampFlat(arrow, p, p.getInventory().getItemInMainHand(),
						damage.CombatState.maxInLastTicks(p, 1200) * 0.75);
			}, i);
		}
	}

	public static void mageBeam(Player p) {
		Location l = p.getLocation();

		// Three range tiers, each doubling the previous total (MAP.md §7): 10+15 = 25 by default,
		// 35+15 = 50 in the boss arena, 70+30 = 100 in the Wither King fight.  The Wither King tier needs a PHASE
		// check rather than a coordinate one, because the WK arena already sits inside the boss-arena box.
		double range = damage.Damage.beamRange(p).maxRange();

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

		// GEOMETRY: the beam is DRAWN from the right hand (`l`) but AIMED from the eye, so it converges on the
		// crosshair.  The trail is a straight line from the hand to wherever the crosshair ray terminates, so the two
		// coincide at the target end (a mob under the crosshair is where the beam visibly lands) while still leaving
		// the hand, which is what it looks like on Hypixel.
		//
		// This supersedes the earlier fix that cast the damage ray FROM the hand along the look direction.  That made
		// the ray match the trail exactly, but the trail was then *parallel* to the crosshair and permanently offset
		// ~0.31 blocks right and ~0.81 down from it, so the beam visibly never went where you were aiming.  Converging
		// the trail instead fixes the same visual/hit mismatch from the other side, and it re-aligns the real beam
		// with Actions.mageBeamWouldHit, the fake-player fire gate, which has always aimed from the eye.
		//
		// Consequence to keep in mind: only the ENDPOINT is shared.  A mob straddling the hand→target segment but not
		// the crosshair ray is crossed by the trail without being hit.  That's what MAGE_BEAM_LENIENCY (0.5 per face)
		// absorbs, and it's inherent to any hand-origin trail that aims by crosshair.
		Location eye = p.getEyeLocation();
		Vector direction = eye.getDirection();
		Vector eyeVec = eye.toVector();

		// Raytrace both entities and blocks from the eye.  Whichever is closer along the ray is what it actually
		// hits, so if a wall is between the player and an entity, the wall stops the beam and the entity takes no
		// damage.  Entity hits get a MAGE_BEAM_LENIENCY-block margin (see findTargetEntity); blocks stay precise.
		RayTraceResult entityResult = findTargetEntity(p, eye, direction, range);
		RayTraceResult blockResult = p.getWorld().rayTraceBlocks(eye, direction, range, FluidCollisionMode.NEVER, true);

		double entityDist = entityResult != null ? entityResult.getHitPosition().distance(eyeVec) : Double.MAX_VALUE;
		double blockDist = blockResult != null ? blockResult.getHitPosition().distance(eyeVec) : Double.MAX_VALUE;

		Vector targetPoint;
		Entity targetEntity;
		if(entityResult != null && entityDist <= blockDist) {
			targetEntity = entityResult.getHitEntity();
			targetPoint = entityResult.getHitPosition();
		} else if(blockResult != null) {
			targetEntity = null;
			targetPoint = blockResult.getHitPosition();
		} else {
			// Nothing in range: aim at the far end of the crosshair ray so the trail still converges toward it.
			targetEntity = null;
			targetPoint = eyeVec.clone().add(direction.clone().multiply(range));
		}

		// The trail runs hand → target point: that convergence is what makes the beam land on the crosshair.
		Vector handToTarget = targetPoint.clone().subtract(l.toVector());
		double distance = handToTarget.length();
		handToTarget.normalize();

		// Iterations based on distance to target, not max range
		int iterations = (int) (distance / 0.33333);
		Vector v = handToTarget.multiply(0.33333);

		for(int i = 0; i < iterations; i++) {
			spawnFireworkParticle(l);
			l.add(v);
		}

		// A dead mob takes no real damage, so the beam passing through it shouldn't emit a hurt sound.  This also
		// covers a boss wither pinned in its dying state (TASDying, HP frozen at 1, so isDead/health won't flag it).
		boolean targetDead = targetEntity instanceof LivingEntity dead
				&& (dead.isDead() || dead.getHealth() <= 0 || dead.getScoreboardTags().contains("TASDying"));

		// Beam hit sounds are routed ONLY to the beamer (and their spectators) at constant volume.
		// There is no at-location sound, so volume doesn't depend on how far the target is.
		ItemStack held = p.getInventory().getItemInMainHand();
		if(targetEntity instanceof Wither wither && wither.getInvulnerableTicks() != 0) {
			// Armored, e.g. mid-intro before the fight is live: no damage lands, but still record the damager so
			// the boss aggros whoever was hitting it the moment its intro completes and aggro turns on.  The mage
			// beam is one of only three things allowed to do that - the thrown-axe projectiles and the Flaming Flay
			// arc are the others; every other path needs the hit to have dealt real damage (see damage/Damage.deal).
			if(wither.getScoreboardTags().contains("TASWither")) instructions.bosses.WitherActions.noteDamager(p);
			// Debuff stacks land even when the damage does not (MAP.md §7): a beam on an armoured boss
			// still builds Lethality, so the moment it opens up the stacks are already there.  Maxor and Storm
			// cannot be arrow-debuffed before they become vulnerable, which is exactly the case this covers.
			damage.Damage.applyOnHitDebuffs(p, wither, damage.DamagePath.BEAM, held);
			if(!targetDead) Utils.playLocalSound(p, Sound.ENTITY_WITHER_HURT, 1.0f, 1.0f);
		} else if(targetEntity instanceof LivingEntity temp) {
			// The beam is the MELEE hit rescaled by the Mage Staff passive, then faded by distance across the
			// three range tiers (§7).  Everything the old hardcoded table did is now a formula output: the
			// Hyperion's "-33% against a non-wither" was this same mechanic written inside out (1/1.5 = 0.667)
			// and is now the Hyperion's x1.5 vs Wither; the Rag buff is +150% of the axe's Strength through the
			// stat layer; and the Spring Boots / Racing Helmet penalties are deleted outright, those wearables
			// now costing only the stats their slot would otherwise carry.
			double sbDamage = damage.Damage.beam(p, temp, held, distance);
			// Silence the target during the hit so vanilla doesn't broadcast its hurt sound at the
			// target's location; beamDamageInProgress tells onWitherHurtSound to skip its manual
			// broadcast the same way (withers are permanently silent, so silence can't signal that).
			boolean wasSilent = temp.isSilent();
			temp.setSilent(true);
			beamDamageInProgress = true;
			try {
				damage.Damage.deal(temp, sbDamage, damage.DamageKind.NORMAL, p, damage.DamagePath.BEAM);
			} finally {
				beamDamageInProgress = false;
				temp.setSilent(wasSilent);
			}
			if(!targetDead) {
				String hurtSound = Utils.getHurtSoundKey(temp);
				if(hurtSound != null) Utils.playLocalSound(p, hurtSound, 1.0f, 1.0f);
			}
		}
	}

	/** How far the beam may travel from a mob's true hitbox and still count as a hit (each face inflated by this). */
	private static final double MAGE_BEAM_LENIENCY = 0.5;

	/**
	 * Nearest valid living entity the beam hits, testing each candidate's bounding box inflated by
	 * {@link #MAGE_BEAM_LENIENCY} on every face, so the beam can pass up to that far from the real hitbox and
	 * still connect.  I do the ray↔AABB test by hand (slab method) rather than using Bukkit's
	 * {@code rayTraceEntities(..., raySize, ...)}: an origin that lands inside an inflated box (common at close
	 * range against big hitboxes like withers) is treated as an immediate hit at t=0, instead of Bukkit returning
	 * an arbitrary exit face.  That was the bug that forced the old precise raySize=0.

	 */
	private static RayTraceResult findTargetEntity(Player p, Location origin, Vector direction, double range) {
		Vector start = origin.toVector();
		double reach = range + MAGE_BEAM_LENIENCY;
		Entity best = null;
		double bestT = Double.MAX_VALUE;
		Vector bestHit = null;
		for(Entity e : p.getWorld().getNearbyEntities(origin, reach, reach, reach)) {
			if(!(e instanceof LivingEntity le) || e instanceof Player || e.isDead()) continue;
			if(le.hasPotionEffect(PotionEffectType.RESISTANCE) && le.getPotionEffect(PotionEffectType.RESISTANCE).getAmplifier() == 255) continue;
			double t = rayBoxDistance(start, direction, e.getBoundingBox().expand(MAGE_BEAM_LENIENCY), range);
			if(t >= 0 && t < bestT) {
				bestT = t;
				best = e;
				bestHit = start.clone().add(direction.clone().multiply(t));
			}
		}
		return best == null ? null : new RayTraceResult(bestHit, best);
	}

	/**
	 * Distance {@code t} along {@code start + t*direction} (direction assumed unit-length) at which the ray first
	 * enters {@code box}, or {@code -1} if it never does within {@code maxDist}. Returns {@code 0} when the origin
	 * is already inside the box (slab method, clamped at 0).
	 */
	private static double rayBoxDistance(Vector start, Vector direction, BoundingBox box, double maxDist) {
		double[] o = {start.getX(), start.getY(), start.getZ()};
		double[] d = {direction.getX(), direction.getY(), direction.getZ()};
		double[] lo = {box.getMinX(), box.getMinY(), box.getMinZ()};
		double[] hi = {box.getMaxX(), box.getMaxY(), box.getMaxZ()};
		double tmin = 0.0, tmax = maxDist;
		for(int i = 0; i < 3; i++) {
			if(Math.abs(d[i]) < 1e-8) {
				if(o[i] < lo[i] || o[i] > hi[i]) return -1; // parallel to this slab and outside it
			} else {
				double t1 = (lo[i] - o[i]) / d[i];
				double t2 = (hi[i] - o[i]) / d[i];
				if(t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
				tmin = Math.max(tmin, t1);
				tmax = Math.min(tmax, t2);
				if(tmin > tmax) return -1;
			}
		}
		return tmin;
	}

	public static void spawnFireworkParticle(Location l) {
		ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(ParticleTypes.FIREWORK, false, false, l.getX(), l.getY(), l.getZ(), 0.0f, 0.0f, 0.0f, 0.0f, 1);
		Utils.broadcastPacket(packet);
	}
}
