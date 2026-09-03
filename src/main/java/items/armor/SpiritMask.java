package items.armor;

import damage.Rarity;
import damage.ReforgeId;
import death.CheatDeath;
import items.ItemFactory;
import items.Wearable;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import plugin.*;

import java.util.*;

/**
 * The Spirit Mask.  A worn cheat-death: 30s cooldown, 3s immune.  It cannot compete with Bonzo's Mask, since
 * both are helmets and a player wears one hat; precedence between the savers is
 * {@code CheatDeath.Saver}'s declaration order.
 */
public final class SpiritMask implements Wearable {
	public static final SpiritMask INSTANCE = new SpiritMask();

	private SpiritMask() {}

	@Override
	public Material material() {
		return Material.PLAYER_HEAD;
	}

	@Override
	public String baseName() {
		return "Spirit Mask";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.LEGENDARY;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.ANCIENT;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.head(colouredName(reforge), "spiritMask",
				"ewogICJ0aW1lc3RhbXAiIDogMTYxNDYyNDQzODQxMywKICAicHJvZmlsZUlkIiA6ICIzMmQ0YzJmN2NlODg0OTAxOGIyZjA3OWM5ZmFiODQxYSIsCiAgInByb2ZpbGVOYW1lIiA6ICJQbHV0byIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83Y2JhOTAyYzhiZGE0NTA2MDlmZGU0OTE4ODgzNDc2MmE3ODA5ZjY1ZjlkZjI2ODQ1ZTM1MWU4MGUxMmJlODMxIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=",
				"v3AvlOspp/7rzGxKdoNj56ZldCLMP0ntaKeHNwjAZA1Ai0WtSsM0j8PIf3YgYIGDs+To5WetlTtgxCp2Mj6fSY6OWeGW2n3Jz+08FkXFUterwfAuqguF90ktROcMG4sEZTOESItehng/LSVShHSwtg5IekQgi8mr07mntH6CGy/xhVu1SMuTIthLBBUbOI6NwfsqZe60BvBPv899C8k5zGzRAVYsDD4cdjXC6ALbMVfIIspyHM6vUuqo7MlNmOJxrr1HfhNXqlFbwDdP9CaL3DmGbHBfTLk8dhLxE89+SNI9HGbvn3YUP5M4f5K4mnire6kIwgXzVNrjXwNR0wkD86dC5ridVRFP6f5VUKLoNSbdQqnAhWhpS2PyV653609dCWR7ES5T2GNxqGv+XufDBqSNSDQc0w/Bhavd0SA3evDX4tc33t3ho6z/XUUYYP5lF8lXtAZmq64MvJB3NOj9XQMBr46b0Zjf986nZKpfmi5hhL/ddXsigl5HszR3EvAQj0M/OHZ0IAu3LRWm9bqwNdPS+soF5n+hOuDAWv0/fh5Dzy8O8PqCUWE0sOkwzB3on3ih7qt8nduCS0kkkCZRvd1eE0Xh1KxnHlKR0QdIIYkM2R2Y3RNDM1mI45NcCTxoIg1I6F/FmXI+aKWDibTHNCK6ix9Nc5We1z6S6vccRHw=");
	}

	@Override
	public EquipmentSlot slot() {
		return EquipmentSlot.HEAD;
	}

	@Override
	public CheatDeath.Saver saver() {
		return CheatDeath.Saver.SPIRIT;
	}
}
