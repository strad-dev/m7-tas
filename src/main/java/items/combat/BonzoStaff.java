package items.combat;

import damage.Rarity;
import damage.ReforgeId;
import items.AbilityItem;
import items.Cast;
import items.ItemFactory;
import items.Weapon;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * The Bonzo Staff.  Fires a wind charge whose fire tick is recorded so {@code MiscListener} and {@code Actions}
 * can age it: the charge itself is vanilla, the timing is ours.
 */
public final class BonzoStaff implements Weapon, AbilityItem {
	public static final BonzoStaff INSTANCE = new BonzoStaff();

	private BonzoStaff() {}

	@Override
	public String loreId() {
		return "skyblock/combat/bonzo";
	}

	@Override
	public Material material() {
		return Material.BREEZE_ROD;
	}

	@Override
	public String baseName() {
		return "Bonzo Staff";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.RARE;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.HEROIC;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.item(material(), colouredName(reforge), loreId(), "STARRED_BONZO_STAFF");
	}

	@Override
	public boolean mageBeams() {
		return true;
	}

	@Override
	public boolean hasRightClick() {
		return true;
	}

	@Override
	public boolean onRightClick(Cast cast) {
		bonzo(cast.player());
		return true;
	}

	public static final Map<Integer, Integer> bonzoFireTick = new HashMap<>();

	public static void bonzo(Player p) {
		Location l = p.getEyeLocation();
		WindCharge windCharge = (WindCharge) l.getWorld().spawnEntity(l, EntityType.WIND_CHARGE);
		windCharge.addScoreboardTag("Bonzo");
		windCharge.setShooter(p);
		makeUndeflectable(windCharge);
		bonzoFireTick.put(windCharge.getEntityId(), MinecraftServer.currentTick);
	}

	/**
	 * <b>Make the charge impossible to punch off course, for its whole flight.</b>
	 * <p>
	 * Punching a projectile is vanilla: {@code Player.attack} calls {@code deflectProjectile} on anything in the
	 * {@code minecraft:redirectable_projectile} entity-type tag, which redirects it along the puncher's aim
	 * ({@code ProjectileDeflection.AIM_DEFLECT}) <b>and re-owns it to them</b> - and a Bonzo charge whose heading
	 * or shooter has moved is a launch that no longer goes where the player aimed it.
	 * <p>
	 * <b>The switch is vanilla's own.</b>  {@code WindCharge} carries a private {@code noDeflectTicks} counter and
	 * refuses every deflection while it is positive, ticking it down by one a tick; the constructor a PLAYER-thrown
	 * charge uses sets it to 5, so vanilla already means "you cannot punch a charge you just threw".  We spawn
	 * through Bukkit, which takes the {@code (type, level)} constructor and leaves the counter at 0, which is
	 * exactly why a Bonzo charge is punchable from the tick it appears.  Setting it past any flight the charge
	 * could have is therefore not a new rule, just vanilla's own one extended - and it covers EVERY deflection
	 * path rather than the punch alone.
	 * <p>
	 * Reflection because the field is private, cached the same way {@code loadout.SpectatorGuiAccess} caches its
	 * own, and a lookup that ever fails is remembered so the miss is paid once.  Verified against 26.2 with
	 * {@code javap}: {@code WindCharge.deflect} is {@code noDeflectTicks <= 0 && super.deflect(...)}.
	 * <p>
	 * <b>It cannot reach the client</b>, which is a separate problem with a separate fix: the counter is not
	 * synched data, so the client's copy of the charge sits at 0 and predicts the deflection locally.  See
	 * {@code MiscListener.onPunchBonzoCharge}.
	 */
	private static void makeUndeflectable(WindCharge charge) {
		Field f = noDeflectField();
		if(f == null) return;
		try {
			f.setInt(((CraftEntity) charge).getHandle(), Integer.MAX_VALUE);
		} catch(IllegalAccessException | IllegalArgumentException | ClassCastException ignored) {
			// The lookup succeeded, so this cannot normally happen; a charge that stays punchable is not worth
			// spamming the log for.
		}
	}

	private static Field noDeflectTicks;
	private static boolean noDeflectUnavailable;

	private static Field noDeflectField() {
		if(noDeflectTicks == null && !noDeflectUnavailable) {
			try {
				noDeflectTicks = net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge.class
						.getDeclaredField("noDeflectTicks");
				noDeflectTicks.setAccessible(true);
			} catch(NoSuchFieldException | RuntimeException e) {
				noDeflectUnavailable = true;
			}
		}
		return noDeflectTicks;
	}
}
