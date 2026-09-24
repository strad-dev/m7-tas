package plugin;

import org.bukkit.inventory.ItemStack;

import java.util.Base64;

/**
 * Cross-plugin item format: base64 of Paper's {@link ItemStack#serializeAsBytes()}, shared by the network plugin's
 * loadout editor and this plugin's catalog export (same Paper build, so it round-trips). Null/air is {@code null}.
 * <br>
 * NOTE: identical copy in the network plugin ({@code loadout/ItemSerial.java}); keep them in sync.
 */
public final class ItemSerial {
	private ItemSerial() {}

	public static String toB64(ItemStack item) {
		if (item == null || item.getType().isAir()) return null;
		return Base64.getEncoder().encodeToString(item.serializeAsBytes());
	}

	public static ItemStack fromB64(String s) {
		if (s == null) return null;
		try {
			return ItemStack.deserializeBytes(Base64.getDecoder().decode(s));
		} catch (Exception e) {
			return null;
		}
	}
}
