package items;

import org.bukkit.entity.Player;

/** An item whose right-click opens a GUI: the Infinileap's Spirit Leap menu, and the SkyBlock Menu. */
public interface MenuItem extends Item {

	/** Open this item's menu.  Called only once the caller has decided the menu is available. */
	void open(Player p);

	/**
	 * Whether vanilla's own use of this stack must be suppressed outright.  True for the Infinileap: it IS an
	 * ender pearl, and leaping goes through {@code Actions.leap}, so the pearl must never be thrown - which
	 * {@code listeners/PearlHelper} enforces as a hard backstop on top of the interact cancel.
	 */
	default boolean blocksVanillaUse() {
		return true;
	}
}
