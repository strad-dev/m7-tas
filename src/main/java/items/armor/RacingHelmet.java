package items.armor;

import damage.Rarity;
import damage.ReforgeId;
import items.ItemFactory;
import items.Wearable;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * The Renowned Racing Helmet.  Grants no stats at all (Renowned is empty at every rarity), and its whole effect
 * is the movement speed: wearing it is worth 650 where the default is 400.
 * <p>
 * The old x0.70 outgoing-damage penalty that used to come with it is DELETED, not moved here: the helmet slot is
 * exclusive, so wearing this already costs the Storm's Helmet's Intelligence and the Golden Dragon pet, and a
 * multiplier on top double-penalised the same swap (§1.10, §8).
 */
public final class RacingHelmet implements Wearable {
	public static final RacingHelmet INSTANCE = new RacingHelmet();

	private RacingHelmet() {}

	@Override
	public Material material() {
		return Material.PLAYER_HEAD;
	}

	@Override
	public String baseName() {
		return "Racing Helmet";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.LEGENDARY;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.RENOWNED;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.head(colouredName(reforge), "racingHelmet",
				"ewogICJ0aW1lc3RhbXAiIDogMTY1NTg2ODcxMjQwMCwKICAicHJvZmlsZUlkIiA6ICJmZTYxY2RiMjUyMTA0ODYzYTljY2E2ODAwZDRiMzgzZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJNeVNoYWRvd3MiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmNlMDc0NmIxMmVlNDA1Mzk1OGUxNDBiYTI5NTkzMjcyYmQ4NGNhMzRiYWY1MGQwZDgwYjViYzNjNjE1ZTljNiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
				"nXxrmNM9KqMjDMg9Lw6gZRooMCm/GnNIn0beDi/2SCyg6aeTCpKe4//cgxWMcg83qEdunAu7z0YICeL/z+K9ynAuviN0AnbwFdwfMf9/Wucs2KXfV9OPPqLnGc8qkht+qbm6d97QvSVh/Ldq4gh19beWPkEY+TP9bBdVGIn2KMY/VukvKGr9PU4aYT2R/5ntQT+iYMPUryQg48+a6EthLcBPhvy7htew2QtebXUKtEfxbnQ7jbj7WLKRoOTlpBCaT9F/TE4Zc2eJJiNwLPh0Cojce2zBCbmDt69wFKlzwJjjcpogaUNqNuVJLRUuAA/r/N82Skc+uoDHaYg0P9vHRhqmSRQNQ85lLcWBu6wmpl5Y+p/NTrqDglQFp919Bk7gsPu2S3gu7+Mx1O0SkSiw13mui67Y31M+9QYXD5yBn5HBt0aao48rOVkL8eM+DHv9cCrefWWIdFXNUpRKqvYsd/2wYgWqjHkE6CvHq0PHMR6IYC7o8zc99A25Ps3wAJwTM3SUyraiEuHxDcJdZLVS9pS43TXaWV8AnEUbBvhdnG1rLDlIi+O5aGIDuKLyQuJo/iaj2mHgHeIg1wbBSQxdIfPTCncPCAITVHouXSds46vnoMNsDmVRW1cpf7T1xwfsEwlWv+hornChME+KxAzQZOY1OqduagUvruLZi/0UPxo=");
	}

	@Override
	public EquipmentSlot slot() {
		return EquipmentSlot.HEAD;
	}

	@Override
	public int impliedSpeed() {
		return 650;
	}
}
