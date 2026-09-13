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
 * The Infinityboom TNT.  Both click sides detonate it against the block the CLIENT reported, and it is
 * <b>never consumed and never actually placed</b>.
 * <p>
 * It carries a can-place-and-break-anything stamp purely so an adventure-mode client reports the clicked block
 * at all, which is also why {@link #neverBreaks()} and {@link #onPlace} both exist: every path where the click
 * is NOT consumed by the ability still lets vanilla place the TNT as a real block, so the placement is vetoed
 * and detonated instead.  Chasing each of those paths individually was the alternative, and
 * {@code BlockPlaceEvent} fires exactly when vanilla decided to place, i.e. exactly when no ability took the
 * click, so this behaves identically however the item was used.
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

	/** Vanilla placed it instead, so detonate against the block it was placed against. */
	@Override
	public boolean onPlace(Player p, Block against) {
		ItemUtils.superboomAt(p, against.getLocation());
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
		ItemUtils.superboomAt(p, clicked.getLocation());
	}
}
