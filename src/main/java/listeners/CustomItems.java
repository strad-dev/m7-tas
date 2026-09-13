package listeners;

import abilities.ClassAbilities;
import commands.Spectate;
import damage.DamageKind;
import damage.DamagePath;
import instructions.Server;
import instructions.bosses.goldor.Goldor;
import instructions.bosses.maxor.Maxor;
import items.*;
import items.Item;
import items.bows.Terminator;
import items.combat.AxeOfTheShredded;
import items.combat.RagnarockAxe;
import net.minecraft.server.MinecraftServer;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
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
import plugin.Cooldowns;
import plugin.FakePlayerManager;
import plugin.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>The ability dispatcher.</b>  Resolves the held stack to an {@code items.Item}, applies the gates that
 * belong to the CLICK rather than to any item, and calls the item's hook.
 * <p>
 * Everything item-specific moved out: each ability now lives on its own class under {@code items/}, the shared
 * combat helpers on {@code items/ItemUtils}, the fourteen per-ability cooldown maps on {@code plugin/Cooldowns},
 * and the drop-key abilities on {@code abilities/}.  What is left here is the plumbing, and it is here precisely
 * because none of it is a property of an item:
 * <ul>
 *   <li>the spectator gate (vanilla does not do this for you, and a ghost is a spectator);</li>
 *   <li>the Trap room's blanket ban on right-click abilities;</li>
 *   <li>{@link #RIGHT_CLICK_GATE_TICKS}, the two-tick right-click rate gate;</li>
 *   <li>{@link #lastRightBlockTick}, which drops the AIR click trailing a BLOCK click;</li>
 *   <li>{@link #lastLeftClickAbilityTick}, the one-ability-per-tick left-click cap;</li>
 *   <li>the melee path, and the mage-beam and left-click-is-an-ability tests, which depend on the holder's
 *       CLASS and so cannot be answered by the item alone.</li>
 * </ul>
 *
 * <h2>ONE CLICK, TWO DISPATCHES</h2>
 * A real player's right-click reaches {@link #handleCustomItems} twice - once from
 * {@code PlayerPacketInterceptor} and once from vanilla's {@code PlayerInteractEvent} - as two separate
 * main-thread tasks.  They normally drain in the same tick and collapse, but the server stops draining its task
 * queue when it runs out of tick time, so on a lagging tick the pair straddles a boundary and both fire, which
 * is a DOUBLE etherwarp: the second dispatch ray-traces from where the first one landed.  They can only ever be
 * ONE tick apart (adjacent tasks in one FIFO queue), which is why {@link #RIGHT_CLICK_GATE_TICKS} is 2 and
 * <b>must not be set to 1</b>.
 * <p>
 * The pair can PRINT the same {@code [tick: N]}: {@code Utils.debug} stamps {@code getTickCount()}, bumped
 * inside {@code tickServer} i.e. AFTER the start-of-tick task drain, while {@code currentTick} is bumped at the
 * top of the {@code runServer} iteration.  A doubled ability on "one tick" is that, not a bypassed gate.
 */
public class CustomItems implements Listener {

	/**
	 * Hard rate cap on right-click abilities, in ticks.  TWO, not one - see the class javadoc.  Nothing wants a
	 * faster rate: every ability has its own longer cooldown, and the Terminator only records a packet tick.
	 * Vanilla-item right-clicks (ender pearls, food) never reach this gate.
	 */
	private static final int RIGHT_CLICK_GATE_TICKS = 2;
	private static final Map<UUID, Integer> rightClickGate = new ConcurrentHashMap<>();

	/**
	 * Tick of the last RIGHT_CLICK_BLOCK dispatch per player.  A physical right-click on a block sends UseItemOn
	 * (RIGHT_CLICK_BLOCK) immediately followed by UseItem (RIGHT_CLICK_AIR); this lets the right-click handler
	 * drop the trailing AIR so the ability fires once even when the pair straddles a tick boundary.
	 */
	private static final Map<UUID, Integer> lastRightBlockTick = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> lastLeftClickAbilityTick = new ConcurrentHashMap<>();
	private static final Set<UUID> droppingPlayers = new HashSet<>();

	/** Tick of the last ordinary melee hit per player, so one swing lands exactly one hit (see {@link #meleeAttack}). */
	private static final Map<UUID, Integer> lastMeleeTick = new ConcurrentHashMap<>();

	/**
	 * Mage beam cooldown.  The shared left-click guard only caps to one per tick, so this enforces the beam's own
	 * 5-tick cooldown.  It is a rate cap rather than an ability cooldown, so it never messages and it never takes
	 * the Mage reduction - which would be circular anyway.
	 */
	private static final int MAGE_BEAM_COOLDOWN_TICKS = 5;
	private static final String MAGE_BEAM_KEY = "mage-beam";

	public static boolean abilityFiredThisTick(Player p) {
		return lastLeftClickAbilityTick.getOrDefault(p.getUniqueId(), -1) == MinecraftServer.currentTick;
	}

	/**
	 * True if this item, in this player's hand, fires the mage beam on left-click.
	 * <p>
	 * The CLASS gate is the whole point: the same weapon in a Berserk's hand is a melee weapon.  It asks
	 * {@link ItemUtils#isMageClass}, not a second inline "named Mage* or tagged Mage" test - the two used to
	 * differ, and the difference bit exactly here, because a real player who PICKED Berserk but happened to be
	 * named Mage-something was a mage to the beam gate and a Berserk to everything else, so their Hyperion beamed
	 * instead of swinging.
	 * <p>
	 * Which WEAPONS beam is now {@code Weapon.mageBeams()} rather than a material test plus a set of four lore
	 * IDs.  One behaviour change comes with that, and it is intended: an UNREGISTERED iron or stone sword in a
	 * Mage's hand no longer beams.
	 */
	private static boolean isMageBeamItem(Player p, ItemStack stack) {
		if(stack == null) return false;
		return ItemUtils.isMageClass(p) && ItemRegistry.of(stack) instanceof Weapon weapon && weapon.mageBeams();
	}

	/**
	 * True if this item's LEFT click is an ability rather than a swing.  Two things read it: the block-break
	 * suppression in {@link #handleCustomItems} (the ability must never break a block, even on cooldown), and
	 * {@link #meleeAttack}, which stands down entirely for these so a Mage's beam or a Terminator volley is never
	 * accompanied by a melee hit.
	 */
	private static boolean leftClickIsAbility(Player p, ItemStack stack) {
		Item item = ItemRegistry.of(stack);
		return isMageBeamItem(p, stack) || (item != null && item.suppressesBlockBreak());
	}

	/**
	 * An ordinary melee swing on a mob - <b>the plugin's one melee damage path</b>, dispatched from
	 * {@code PlayerPacketInterceptor}'s attack-packet branch.
	 * <p>
	 * There was no such path at all until it was added, and the gap was invisible because the Mage never needed
	 * one: a Mage's swing fires the beam, which applies its own damage, so the only class whose sword mattered was
	 * already served.  Every other class fell through to VANILLA melee damage, and vanilla is not a participant in
	 * this model (MAP.md §7) - so a swing did a couple of hearts against a mob whose HP is SB/1e6, and against a
	 * boss wither it did precisely nothing, because {@code MiscListener.onWitherLordDamage} cancels every
	 * non-plugin hit on a TASWither.  That is the whole of "a Berserk's melee hits do nothing".
	 * <p>
	 * Everything downstream already existed and simply had no caller: {@code Damage.melee} for the formula, and
	 * {@code Damage.deal} for the application, which in turn drives the repeated-hit stack, the post-kill buff,
	 * Fire Aspect / Venomous / Thunderlord and the Cleave sweep.
	 */
	public static void meleeAttack(Player p, org.bukkit.entity.Entity hit) {
		if(p == null || !(hit instanceof LivingEntity target)) return;
		if(p.getGameMode() == GameMode.SPECTATOR) return;
		if(Spectate.getSpectatorMap().containsKey(p)) return;
		// Players are invulnerable in this model, and a spectated fake player must never be hittable either.
		if(target instanceof Player) return;
		ItemStack held = p.getInventory().getItemInMainHand();
		if(leftClickIsAbility(p, held)) return;
		// One hit per swing.  The attack packet arrives once per swing, but the drop-key ability path also swings,
		// and a duplicate attack packet would otherwise double the damage AND the repeated-hit stack.
		int now = MinecraftServer.currentTick;
		if(lastMeleeTick.getOrDefault(p.getUniqueId(), -1) == now) return;
		lastMeleeTick.put(p.getUniqueId(), now);

		// ONLY A MELEE WEAPON SWINGS.  Anything else - a bow, a wearable head held in the hand, a vanilla stack -
		// lands a bare PUNCH instead, which is the melee formula with the held item contributing nothing.  This
		// used to run the full melee path whatever was held, so hitting a mob with a Precise Terminator folded the
		// bow's 310 Damage, its Strength and its 250 Crit Damage into the swing and punched for most of what a
		// sword does.  Unregistered stacks are unaffected either way: they contribute no stats, so the two paths
		// already agreed for them.
		Item item = ItemRegistry.of(held);
		double sbDamage = item instanceof Weapon weapon && weapon.swingsMelee()
				? damage.Damage.melee(p, target, held)
				: damage.Damage.punch(p, target);
		damage.Damage.deal(target, sbDamage, DamageKind.NORMAL, p, DamagePath.MELEE);
	}

	// ===================================== listeners =====================================

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

	/**
	 * Cancel entity interaction for custom items to prevent side effects (e.g. bow drawing).  The ability itself
	 * fires from {@code PlayerInteractAtEntityEvent} (real clients) or the use-item packet (fake players).
	 * <p>
	 * Gated on the raw lore ID rather than on the registry, matching the old rule exactly: ANY item carrying an
	 * ID is cancelled, so an item the registry does not know still behaves as it always has.  A registered item
	 * opts out with {@code Item.allowsEntityInteract()}, which is how the Gyrokinetic Wand, the Dungeonbreaker
	 * and the two drawn bows can still use an item frame.
	 */
	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
		ItemStack held = e.getPlayer().getInventory().getItemInMainHand();
		if(ItemUtils.getID(held).isEmpty()) return;
		Item item = ItemRegistry.of(held);
		if(item == null || !item.allowsEntityInteract()) e.setCancelled(true);
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
		// wrong number by six orders of magnitude.  Now that meleeAttack applies every swing at SkyBlock scale,
		// vanilla must not land on top of any of them - the same "vanilla is not a participant" rule the rest of
		// MAP.md §7 runs on.  Knockback goes with it, which is fine: nothing in the floor depends on melee knock.
		if(e.getDamager() instanceof Player) e.setCancelled(true);
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		Player p = e.getPlayer();
		Item held = ItemRegistry.of(p.getInventory().getItemInMainHand());
		// The Superboom TNT carries a can_break stamp purely so the adventure-mode client reports the clicked
		// block on a left-click (see Utils.placeAndBreakAnythingInAdventure).  It must never actually break
		// anything.  The left-click interact event is already cancelled for it in handleCustomItems, so this is
		// the backstop.  It matters because the fall-through below would otherwise remove the block PERMANENTLY.
		if(held instanceof Tool tool && tool.neverBreaks()) {
			e.setCancelled(true);
			return;
		}
		// CREATIVE BYPASS: a creative-mode player breaks anything, anywhere, past every protection below, and does
		// it through vanilla (physics and all) rather than our no-physics path - that's what someone editing the
		// map expects.  This is the same bypass GoldorListener already grants for the S3 item frames.  It sits
		// BELOW the Superboom check on purpose: that one is item behaviour (the TNT must never break a block),
		// not a protection.
		if(p.getGameMode() == GameMode.CREATIVE) return;
		// Protected Goldor interactables and the Maxor Energy-Crystal pressure plates are unbreakable outright,
		// with any tool (stonk, dungeonbreaker, …) and in any phase.
		if(Goldor.INSTANCE.isProtected(e.getBlock()) || Maxor.INSTANCE.isProtected(e.getBlock())) {
			e.setCancelled(true);
			return;
		}
		// Static-map fixtures: secret chests, the Quiz answer buttons, wither-skeleton skulls (Wither Essence)
		// anywhere, and the Ice Fill puzzle's ice and polished andesite.  Gated on NOTHING - the Dungeonbreaker
		// works whatever the run is doing, and its break writes AIR into the world for good, so one break outside
		// a run takes that block out of every run after it as well.
		if(instructions.clear.ClearManager.isMapFixture(e.getBlock())) {
			e.setCancelled(true);
			return;
		}
		// ONCE THE RUN HAS STARTED, two more things lock: every door (frame included, see Rooms.Door) and every
		// room's ceiling.  Both are open during the pre-run prep window, which is the same deal the out-of-bounds
		// kill gives the crevices: get into position however you like before the countdown ends, but no shortcuts
		// during the run.  The wither/blood doors were already covered by this rule; Rooms.inDoor now covers all
		// 15 plus their frames, and the key + door-click path in MiscListener is still the only way to open one.
		Block b = e.getBlock();
		if(Server.isRunStarted()
				&& (instructions.clear.Rooms.inDoor(b.getX(), b.getY(), b.getZ())
				|| instructions.clear.Rooms.isCeiling(b.getX(), b.getY(), b.getZ()))) {
			e.setCancelled(true);
			return;
		}
		// The vertical faces (perimeter walls) of a room can't be broken through.  Only the floor and the room
		// interior can be stonked.  I restrict by the room's horizontal perimeter at any Y, because rooms have
		// varying heights and a Y-based wall rule is impossible.  Multi-cell rooms (e.g. the 2x2 Museum) protect
		// only their OUTER perimeter, so the middle of the room stays stonkable.  The three doors (start / wither
		// / blood) sit in these walls and must remain stonkable pre-run, so they're exempt.
		if(instructions.clear.Rooms.isRoomFace(b.getX(), b.getZ())
				&& !Server.inStartDoor(b) && !Server.inWitherDoor(b) && !Server.inBloodDoor(b)) {
			e.setCancelled(true);
			return;
		}
		// Do the removal ourselves WITHOUT physics instead of letting vanilla break the block.  A vanilla break
		// runs updateNeighbourShapes on the six neighbours, which tears out anything support-dependent sitting on
		// or against the block: carpets, torches, flowers, rails, redstone.  It also cascades a nether portal to
		// air via its frame-completeness check.  That removal path (updateShape → destroyBlock) fires NO
		// BlockPhysicsEvent, so it can't be vetoed from a listener.  The only place to stop it is here, at the
		// source.  setType(AIR, false) uses applyPhysics=false, so nothing attached pops off.
		e.setCancelled(true);
		// A Tool handles its own removal (the Dungeonbreaker's is TEMPORARY, restored after 200 ticks); anything
		// else is a permanent break, still no-physics.
		if(!(held instanceof Tool tool && tool.onBreak(p, b))) b.setType(Material.AIR, false);
	}

	/**
	 * Catch-all so an item that must never exist as a real block is never left as one.  Most click paths fire the
	 * ability and cancel the interact event, so vanilla never places anything.  But every path where the click
	 * ISN'T consumed by the ability still lets vanilla place it, since the Superboom TNT carries
	 * can-place-on-anything for adventure mode: a sneak-right-click on a lever / button / clear-phase secret (all
	 * of which return early in {@link #onPlayerInteract} so the block keeps its click), a right-click inside the
	 * Trap room, an off-hand placement, and presumably more.  Rather than chase each one, veto the placement here
	 * and let the item act instead.  {@code BlockPlaceEvent} fires exactly when vanilla decided to place, which is
	 * exactly when no ability consumed the click, so the item behaves identically however it was used.  Cancelling
	 * also keeps the stack intact, since Infinityboom is never consumed.  Creative mode is left alone so setup and
	 * building can still place real TNT.
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onCustomBlockPlace(BlockPlaceEvent e) {
		if(!(ItemRegistry.of(e.getItemInHand()) instanceof Tool tool) || !tool.vetoesPlacement()) return;
		Player p = e.getPlayer();
		GameMode gm = p.getGameMode();
		if(gm != GameMode.SURVIVAL && gm != GameMode.ADVENTURE) return;
		e.setCancelled(true);
		p.updateInventory(); // the client predicted the placement, so resend the unchanged stack and it won't ghost
		// Mirror the Trap-room restriction in handleCustomItems: no right-click item abilities in there.
		if(instructions.clear.ClearManager.isActive()
				&& instructions.clear.Rooms.roomAt(p.getLocation()) == instructions.clear.Rooms.TRAP) return;
		tool.onPlace(p, e.getBlockAgainst());
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
		// The drop key is an ABILITY key here, never a way to lose an item, so cancel FIRST, for everyone.  This
		// used to be gated on being an Archer or a Mage (the only two classes with a drop ability), which meant a
		// Berserk, Healer, Tank, or anyone with no class tag at all physically threw their kit item on the floor.
		// Whether the class has an ability to fire is a separate question, answered by abilities/ClassAbilities.
		e.setCancelled(true);
		boolean ultimate = !p.isSprinting();
		// Real players get their ability from the interceptor's DROP_ITEM/DROP_ALL_ITEMS path, which is not
		// rate-limited by vanilla's drop handling.  This handler only owns the cancel for them.
		if(!FakePlayerManager.getFakePlayers().containsValue(p)) return;
		ClassAbilities.dispatch(p, ultimate);
	}

	/** The drop-key entry point for a REAL player, from the packet interceptor. */
	public static void handleDrop(Player p, boolean ultimate) {
		if(Spectate.getSpectatorMap().containsKey(p)) return;
		droppingPlayers.add(p.getUniqueId());
		Utils.scheduleTask(() -> droppingPlayers.remove(p.getUniqueId()), 1);
		ClassAbilities.dispatch(p, ultimate);
	}

	/**
	 * A drawn bow released.  The Terminator is a shortbow and never comes through here; it fires from its own
	 * poller instead.
	 */
	@EventHandler
	public void onEntityShootBow(EntityShootBowEvent e) {
		if(!(e.getEntity() instanceof Player p)) return;
		ItemStack bow = p.getInventory().getItemInMainHand();
		if(!(ItemRegistry.of(bow) instanceof items.Bow custom) || custom.shortbow()) return;
		if(!(e.getProjectile() instanceof org.bukkit.entity.Arrow primary)) return;
		// Vanilla's charge, min(useTicks/20, 1).  A DRAWN bow scales its damage by this AND loses the whole crit
		// term below a full draw (MAP.md §1.4), which makes a partial draw much worse than the fraction alone
		// suggests.
		custom.onShoot(p, bow, primary, Math.clamp(e.getForce(), 0f, 1f));
	}

	/**
	 * One of a {@code ProjectileItem}'s projectiles landed.  Matched on the projectile's SCOREBOARD TAG, not on
	 * what the shooter is holding: the shot has already left, and by the time it lands the shooter may well have
	 * swapped.
	 */
	@EventHandler
	public void onProjectileHit(ProjectileHitEvent e) {
		Projectile projectile = e.getEntity();
		items.ProjectileItem source = ItemRegistry.projectileSource(projectile.getScoreboardTags());
		if(source == null) return;
		source.onProjectileHit(e, projectile.getShooter() instanceof Player p ? p : null);
	}

	// ===================================== dispatch =====================================

	public static void handleCustomItems(Cancellable e, EquipmentSlot hand, ItemStack stack, Action action, Player p) {
		handleCustomItems(e, hand, stack, action, p, null);
	}

	/**
	 * @param clickedBlock the block VANILLA reported this click landed on, either
	 *                     {@code PlayerInteractEvent.getClickedBlock()} or
	 *                     {@code ServerboundPlayerActionPacket}'s pos.  It is {@code null} for an air click, an
	 *                     entity interaction, or a fake-player dispatch.  Abilities that act on a block (the
	 *                     Superboom TNT) use this instead of ray-tracing a reach of their own, so their range is
	 *                     exactly vanilla's block-interaction range and their target is exactly the block the
	 *                     client aimed at.
	 */
	public static void handleCustomItems(Cancellable e, EquipmentSlot hand, ItemStack stack, Action action, Player p, Block clickedBlock) {
		if(p.getGameMode() == GameMode.SPECTATOR) return; // spectators never fire item abilities
		if(Spectate.getSpectatorMap().containsKey(p)) return;
		if(!Objects.equals(hand, EquipmentSlot.HAND)) return;

		boolean isRightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
		boolean isLeftClick = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
		Item item = ItemRegistry.of(stack);

		// Trap room disables all right-click item abilities EXCEPT vanilla ender pearls and the Dungeonbreaker.
		if(isRightClick && instructions.clear.ClearManager.isActive()
				&& instructions.clear.Rooms.roomAt(p.getLocation()) == instructions.clear.Rooms.TRAP) {
			boolean allowed = stack != null && (stack.getType() == Material.ENDER_PEARL
					|| (item != null && item.usableInTrapRoom()));
			if(!allowed) return;
		}
		if(action == Action.LEFT_CLICK_AIR && droppingPlayers.contains(p.getUniqueId())) return;

		// Only a stack carrying an item ID is a custom item as far as the click plumbing is concerned.  Armour,
		// the wearable heads, the SkyBlock Menu and the Pitchin' Rod carry none, and a right-click holding one has
		// always gone through untouched.
		if(stack == null || !ItemUtils.getID(stack).startsWith("skyblock/")) return;

		int tick = MinecraftServer.currentTick;
		boolean fired = false;

		// Cancel early for right-clicks to prevent vanilla item use (bow drawing, TNT placement, throwing the
		// Infinileap's pearl).  Skipped for the items whose right-click is not an ability at all.
		if(e != null && isRightClick && (item == null || item.cancelsInteract())) e.setCancelled(true);

		AbilityItem ability = item instanceof AbilityItem a ? a : null;

		if(isLeftClick) {
			if(tick > lastLeftClickAbilityTick.getOrDefault(p.getUniqueId(), -1)) {
				if(isMageBeamItem(p, stack)) {
					if(Cooldowns.ready(p, MAGE_BEAM_KEY)) {
						ItemUtils.mageBeam(p);
						Cooldowns.start(p, MAGE_BEAM_KEY, MAGE_BEAM_COOLDOWN_TICKS);
						fired = true;
					}
				} else if(ability != null && ability.hasLeftClick()) {
					fired = fire(p, ability, new Cast(p, stack, clickedBlock, action, tick), false);
				}
				if(fired) lastLeftClickAbilityTick.put(p.getUniqueId(), tick);
			}
			// Suppress vanilla block-breaking for these ability items regardless of fire/cooldown state.
			if(e != null && leftClickIsAbility(p, stack)) e.setCancelled(true);
		}

		if(isRightClick) {
			// A single physical right-click on a block sends UseItemOn (RIGHT_CLICK_BLOCK) immediately followed by
			// UseItem (RIGHT_CLICK_AIR).  These normally land on the same tick and collapse via the rate gate
			// below, but the first click after a server restart can straddle a tick boundary from one-time warmup
			// lag, and then the trailing AIR fires the ability twice.  Drop an AIR that trails a BLOCK click by at
			// most one tick.  Genuine standalone air-clicks carry no recent BLOCK so they still fire, and
			// fake-player air-spam sends no BLOCK at all.
			if(action == Action.RIGHT_CLICK_BLOCK) {
				lastRightBlockTick.put(p.getUniqueId(), tick);
			} else {
				int blockTick = lastRightBlockTick.getOrDefault(p.getUniqueId(), Integer.MIN_VALUE);
				if(tick == blockTick || tick == blockTick + 1) return;
			}
			if(ability != null && ability.hasRightClick() && tick >= rightClickGate.getOrDefault(p.getUniqueId(), 0)) {
				rightClickGate.put(p.getUniqueId(), tick + RIGHT_CLICK_GATE_TICKS);
				fired = fire(p, ability, new Cast(p, stack, clickedBlock, action, tick), true);
			}
		}

		// Cancel left-click events only if an ability actually fired
		if(e != null && fired) e.setCancelled(true);
	}

	/**
	 * Run one ability hook, honouring its cooldown.  <b>The one place a cooldown is checked, reported and
	 * spent</b>, which is what collapsed fourteen near-identical inline copies of these six lines.
	 *
	 * @return whether the ability fired, which is what the caller cancels the event on
	 */
	private static boolean fire(Player p, AbilityItem ability, Cast cast, boolean rightClick) {
		int cooldown = ability.cooldownTicks();
		if(cooldown > 0) {
			int remaining = Cooldowns.remaining(p, ability.cooldownKey());
			if(remaining > 0) {
				if(ability.announcesCooldown()) ItemUtils.sendCooldownMessage(p, remaining);
				return false;
			}
		}
		boolean fired = rightClick ? ability.onRightClick(cast) : ability.onLeftClick(cast);
		if(fired && cooldown > 0) {
			Cooldowns.start(p, ability.cooldownKey(),
					ability.mageReduced() ? ItemUtils.effectiveCooldown(p, cooldown) : cooldown);
		}
		return fired;
	}

	// ===================================== run resets =====================================

	/**
	 * Clear the per-run combat state: the Berserk repeated-hit stack, the post-kill and combo windows, the rolling
	 * damage history, target debuff stacks, running procs and any floating damage numbers still in the world.
	 * Called at the start of every run.
	 */
	public static void resetBerserkDamage() {
		damage.CombatState.reset();
		damage.TargetDebuffs.reset();
		damage.Procs.reset();
		damage.DamageNumbers.reset();
		damage.Stats.invalidateAll();
	}

	/** Clear the Terminator's volley clocks and the melee dedupe.  Called at the start of every practice run. */
	public static void resetTerminatorCooldowns() {
		Terminator.reset();
		lastMeleeTick.clear();
	}

	/**
	 * Reset every ability and class-ability cooldown, plus the two abilities that carry state of their own beyond
	 * a cooldown.  Called on entering a boss fight ({@code WitherLord.start}) and at run start.
	 * <p>
	 * One store now, so this can no longer miss an ability: it used to be fourteen {@code .clear()} calls, and
	 * adding an ability meant remembering to add a fifteenth.
	 */
	public static void resetAbilityCooldowns() {
		Cooldowns.clearAll();
		RagnarockAxe.reset(); // also kills any wind-up chain still in flight
		AxeOfTheShredded.reset();
	}
}
