package items.combat;

import damage.Rarity;
import damage.ReforgeId;
import items.AbilityItem;
import items.Cast;
import items.ItemFactory;
import items.ItemUtils;
import items.Weapon;
import net.minecraft.server.MinecraftServer;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import plugin.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Ragnarock Axe.  Its ability is a <b>3-second wind-up</b> (three lever clicks) and only then the buff, and
 * the axe has to stay in the main hand for the whole of it: holding it is the ability's cost.  The cooldown is
 * deliberately NOT refunded when a wind-up is dropped, since it is spent at cast time, as on Hypixel.
 * <p>
 * <b>The base name carries a Greek omicron</b> (U+03BF) rather than an ASCII o - deliberate, so the name stops
 * colliding with one of the owner's client mods.  The two spellings render identically and are not equal;
 * getting it wrong drops the item to the tail of the palette and freezes every saved copy.
 * {@code damage/Items}, {@code damage/Stats.ragnarockStrength}, {@code Catalog.PALETTE_ORDER} and both
 * {@code ItemRefresh.RENAMED} tables all carry the Greek form.  Grep for it, never type it.
 */
public final class RagnarockAxe implements Weapon, AbilityItem {
	public static final RagnarockAxe INSTANCE = new RagnarockAxe();

	private RagnarockAxe() {}

	@Override
	public String loreId() {
		return "skyblock/combat/rag";
	}

	@Override
	public Material material() {
		return Material.GOLDEN_AXE;
	}

	@Override
	public String baseName() {
		return "Ragnarοck Axe";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.RARE;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.WITHERED;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.item(material(), colouredName(reforge), loreId(), "RAGNAROCK_AXE");
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
	public int cooldownTicks() {
		return 400; // 20s
	}

	@Override
	public boolean onRightClick(Cast cast) {
		rag(cast.player());
		return true;
	}

	// Server tick the RagBuff currently expires at, per player. Each cast's buff lands at +60 and lasts 200 ticks
	// (10s); a re-cast while still active pushes this out so the earlier cast's removal no-ops (see rag()).
	private static final Map<UUID, Integer> ragBuffExpiry = new ConcurrentHashMap<>();

	// Server tick the in-flight Ragnarock cast started at, per player. The 3s wind-up (ragWindup) polls the main hand
	// every tick and drops the cast the moment the axe leaves it, so swapping off mid-wind-up no longer lands the
	// buff. The stamp doubles as the chain's identity: a re-cast overwrites it, so an older chain sees a start tick
	// that isn't its own and stops.
	private static final Map<UUID, Integer> ragCastStart = new ConcurrentHashMap<>();

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
			if(!p.isOnline() || !ItemUtils.getID(p.getInventory().getItemInMainHand()).equals("skyblock/combat/rag")) {
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
	 * Drop any wind-up still in flight and forget the buff windows.  Part of the run reset: clearing the cast
	 * stamp is what kills a live chain, since it then sees a start tick that is not its own.
	 */
	public static void reset() {
		ragBuffExpiry.clear();
		ragCastStart.clear();
	}

}
