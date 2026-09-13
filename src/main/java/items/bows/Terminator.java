package items.bows;

import damage.Rarity;
import damage.ReforgeId;
import items.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.*;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import plugin.Cooldowns;
import plugin.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Terminator.  A <b>shortbow</b>: never drawn, so draw scaling never applies and every shot is a
 * full-damage crit (§1.2).
 *
 * <h2>Firing is POLLER-driven, not click-driven</h2>
 * A click only records the tick a packet arrived; {@link #pollAll()} fires on the first tick where a new packet
 * exists AND the cooldown has elapsed (5 ticks, or 4 with the full Thermodynamic set, which is what that set is
 * for - a rate cap, never a per-hit multiplier).  That caps the rate at one volley per cooldown however hard the
 * click is spammed, and it collapses a left- and a right-click landing on the same tick into one shot.
 * <p>
 * <b>Salvation is a separate clock.</b>  The left-click beam runs its own 5-tick cooldown, held here rather than
 * declared through {@link #cooldownTicks()}, because a RIGHT-click must not spend it: the two sides of this bow
 * are two rate limits on one item.  Neither takes the Mage reduction, since the Terminator and its beam are
 * weapons, not abilities.
 */
public final class Terminator implements Bow, AbilityItem {
	public static final Terminator INSTANCE = new Terminator();

	private Terminator() {}

	@Override
	public String loreId() {
		return "skyblock/combat/terminator";
	}

	@Override
	public Material material() {
		return Material.BOW;
	}

	@Override
	public String baseName() {
		return "Terminator";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.LEGENDARY;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.PRECISE;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.item(material(), colouredName(reforge), loreId(), "TERMINATOR");
	}

	@Override
	public boolean shortbow() {
		return true;
	}

	@Override
	public List<String> arrowTags() {
		return List.of("TerminatorArrow");
	}

	@Override
	public Arrow spawnBonusArrow(Player p, Location aimFrom, ItemStack bow, float speed) {
		Arrow arrow = p.getWorld().spawnArrow(aimFrom, aimFrom.getDirection(), speed, 0);
		arrow.setPierceLevel(4);
		arrow.setShooter(p);
		arrow.setWeapon(bow);
		arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
		return arrow;
	}

	@Override
	public void bonusArrowSound(Player p, boolean archerBonus) {
		Utils.playLocalSound(p, Sound.ENTITY_ARROW_SHOOT, 1.0F, 1.0F);
	}

	@Override
	public boolean removesArrowInSolid() {
		return true;
	}

	@Override
	public boolean suppressesBlockBreak() {
		return true;
	}

	@Override
	public boolean mageReduced() {
		return false;
	}

	@Override
	public boolean hasRightClick() {
		return true;
	}

	@Override
	public boolean hasLeftClick() {
		return true;
	}

	/** A right-click only ARMS a volley.  {@link #pollAll()} decides whether it fires this tick or the next. */
	@Override
	public boolean onRightClick(Cast cast) {
		lastPacketTick.put(cast.player().getUniqueId(), cast.tick());
		return true;
	}

	/**
	 * A left-click arms the volley too, and additionally fires Salvation if its own cooldown has elapsed.  The
	 * packet tick is recorded either way, so a beam on cooldown still shoots.
	 */
	@Override
	public boolean onLeftClick(Cast cast) {
		Player p = cast.player();
		lastPacketTick.put(p.getUniqueId(), cast.tick());
		if(!Cooldowns.ready(p, SALVATION_KEY)) return false;
		Cooldowns.start(p, SALVATION_KEY, SALVATION_COOLDOWN_TICKS);
		salvation(p);
		return true;
	}

	/** Salvation's own cooldown key.  Kept off {@link #cooldownKey()} so a right-click cannot spend it. */
	private static final String SALVATION_KEY = "skyblock/combat/terminator/salvation";

	// Salvation (Terminator left-click) cooldown: the tick the next Salvation beam is usable.  The shared left-click
	// guard only caps to 1/tick, so this enforces the ability's own 5-tick cooldown.  Note that the Terminator and
	// Salvation are weapons, NOT abilities, so they deliberately skip the Mage cooldown reduction.
	private static final int SALVATION_COOLDOWN_TICKS = 5;

	/** Tick a new volley was armed on, per player, and the tick one last left the bow. */
	private static final Map<UUID, Integer> lastPacketTick = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> lastFireTick = new ConcurrentHashMap<>();

	/**
	 * Per-tick terminator cooldown poller.  For each player who has recorded a terminator right-click since their
	 * last shot, fires a bow shot on the first tick at or after {@code lastFire + cooldown} (5 ticks, or 4 with the
	 * full Thermodynamic set).  Shots are anchored to the first shot and clamp to one per cooldown, so spamming the
	 * right-click can't exceed it.  Started from {@link plugin.M7tas}.
	 */
	public static void pollAll() {
		int now = MinecraftServer.currentTick;
		for(Map.Entry<UUID, Integer> entry : lastPacketTick.entrySet()) {
			UUID id = entry.getKey();
			int lastPacket = entry.getValue();
			int lastFire = lastFireTick.getOrDefault(id, Integer.MIN_VALUE / 2);
			if(lastPacket <= lastFire) continue;                 // no new right-click since the last shot
			Player p = Bukkit.getPlayer(id);
			if(p == null || !p.isOnline()) continue;
			if(!"skyblock/combat/terminator".equals(ItemUtils.getID(p.getInventory().getItemInMainHand()))) continue; // must hold it
			int cooldown = ItemUtils.isThermoSet(p) ? 4 : 5;
			if(now >= lastFire + cooldown) {
				fire(p);
				lastFireTick.put(id, now);
			}
		}
	}

	/** Clear the volley and Salvation clocks.  Part of the run reset. */
	public static void reset() {
		lastPacketTick.clear();
		lastFireTick.clear();
	}

	private static void fire(Player p) {
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

		// Duplex and the Archer's two bonus arrows are Bow's shared implementation, off its constants.  This
		// used to be a copy of the same block that both branches of onEntityShootBow carried, and the copies had
		// already drifted apart on how they tested for an Archer.
		INSTANCE.fireBonusArrows(p, l, bow, speed, 1.0);
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
		List<EntityType> doNotKill = ItemUtils.doNotKill();
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
}
