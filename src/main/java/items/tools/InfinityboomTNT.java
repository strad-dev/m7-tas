package items.tools;

import damage.Rarity;
import damage.ReforgeId;
import items.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import plugin.Utils;

/**
 * Both click sides detonate against the block the CLIENT reported; <b>never consumed, never placed</b>.
 * <p>
 * The can-place-and-break stamp only exists so an adventure client reports the block at all, hence
 * {@link #neverBreaks()} and {@link #onPlace}: any click the ability didn't consume would let vanilla place real TNT,
 * so the placement is vetoed and detonated. {@code BlockPlaceEvent} fires exactly when no ability took the click,
 * so this covers every path without chasing each one.
 */
public final class InfinityboomTNT implements Tool, AbilityItem {
	public static final InfinityboomTNT INSTANCE = new InfinityboomTNT();

	private InfinityboomTNT() {}

	@Override
	public String loreId() {
		return "skyblock/combat/infinityboom";
	}

	@Override
	public Material material() {
		return Material.TNT;
	}

	@Override
	public String baseName() {
		return "Infinityboom TNT";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.EPIC;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.NONE;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return Utils.placeAndBreakAnythingInAdventure(
				ItemFactory.item(material(), colouredName(reforge), loreId(), "INFINITE_SUPERBOOM_TNT"));
	}

	@Override
	public boolean neverBreaks() {
		return true;
	}

	@Override
	public boolean vetoesPlacement() {
		return true;
	}

	@Override
	public boolean suppressesBlockBreak() {
		return true;
	}

	@Override
	public boolean hasRightClick() {
		return true;
	}

	@Override
	public boolean hasLeftClick() {
		return true;
	}

	@Override
	public boolean onRightClick(Cast cast) {
		superboom(cast.player(), cast.clickedBlock());
		return true;
	}

	@Override
	public boolean onLeftClick(Cast cast) {
		superboom(cast.player(), cast.clickedBlock());
		return true;
	}

	/** Vanilla placed it: detonate against the block it was placed on. */
	@Override
	public boolean onPlace(Player p, Block against) {
		ItemUtils.superboomAt(p, against.getLocation());
		return true;
	}

	/**
	 * Deliberately NO server-side ray trace: {@code clicked} is what the CLIENT reported, so reach and target are
	 * vanilla's. The old trace was too generous (fixed 5 blocks) and wrong (skipped passable blocks, so aiming at a
	 * lever hit the wall behind it).
	 * <p>
	 * RIGHT_CLICK_BLOCK comes from {@code getClickedBlock()} / {@code ServerboundUseItemOnPacket}, LEFT_CLICK_BLOCK
	 * from {@code ServerboundPlayerActionPacket}'s pos (needs the can_break stamp, see
	 * {@code Utils.placeAndBreakAnythingInAdventure}, or adventure sends no attack packet), a placement from
	 * {@code getBlockAgainst()}. Null means air click or entity click: vanilla would place nothing, so no boom.
	 */
	public static void superboom(Player p, Block clicked) {
		if(clicked == null) return;
		ItemUtils.superboomAt(p, clicked.getLocation());
	}
}
