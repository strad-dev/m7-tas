package plugin;

import org.bukkit.inventory.ItemStack;

import java.util.Base64;

/**
 * Cross-plugin item (de)serialization. Items are stored as base64 of Paper's
 * {@link ItemStack#serializeAsBytes()} so the network plugin's loadout editor and this plugin's
 * catalog export speak the exact same format (both run on the same Paper build, so the bytes and the
 * custom-item lore IDs round-trip cleanly). A null/air slot serializes to {@code null}.
 *
 * NOTE: an identical copy lives in the network plugin ({@code loadout/ItemSerial.java}) - keep the
 * two in sync.
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
