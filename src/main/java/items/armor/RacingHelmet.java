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
 * is <b>+100 Max Speed</b> - a bonus, summed with everything else by {@code plugin/MaxSpeedSync}, not a finished
 * number.  It also forces the Black Cat in the assumed modes ({@code damage/Pet.forPlayer}), which is where the
 * other +150 in the 650 it used to be credited with came from.
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

	/**
	 * What the helmet itself adds to Max Speed.  <b>Its own +100 and nothing else</b>: the 650 this used to
	 * report was the base 400 plus the Black Cat's 150 (which the helmet forces in the assumed modes) plus this,
	 * and the alpha 700 was the same sum over a base carrying the alpha shard's +50.  {@code MaxSpeedSync} adds
	 * those up, so the same number is right under alpha and in realistic, where no hat grants a pet.
	 */
	private static final int MAX_SPEED_BONUS = 100;

	@Override
	public int maxSpeedBonus() {
		return MAX_SPEED_BONUS;
	}
}
