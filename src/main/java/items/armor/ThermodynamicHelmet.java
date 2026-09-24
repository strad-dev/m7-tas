package items.armor;

import damage.Rarity;
import damage.ReforgeId;
import items.ItemFactory;
import items.Wearable;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Renowned Thermodynamic piece. No stats; 4/4 is a RATE bonus: the Terminator's 5-tick cooldown becomes 4. The old
 * 97.5% damage penalty is deleted.
 */
public final class ThermodynamicHelmet implements Wearable {
	public static final ThermodynamicHelmet INSTANCE = new ThermodynamicHelmet();

	private ThermodynamicHelmet() {}

	@Override
	public Material material() {
		return Material.PLAYER_HEAD;
	}

	@Override
	public String baseName() {
		return "Thermodynamic Helmet";
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
		return ItemFactory.head(colouredName(reforge), "thermoHelmet",
				"ewogICJ0aW1lc3RhbXAiIDogMTc3MjE1OTIwMzEzMywKICAicHJvZmlsZUlkIiA6ICJkZGRhNjc4ZmYyN2M0NjFhOWUyMjRiMTU1NjI4NDZmYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJSeWxlZTc1NDMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2FkNjdjZjMzMTg4YTU0YjZmNTVkMmVmNTI0OGNkM2I3MjE3Njk5NGUxZDIwMzczMWU4NmIzNjVhODU4MTcyNiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
				"erdSAba2Gk9jBaa6fd87ZzBdUbuKFtDuo4m2+H9s3WISKq+i+VMi/IUa5WvN2dy7i2sFXA8jqtXh5LG87+I3bTSZKfvxlEgQfwBg0aG7HEsqn1OAAsT/4ZFWE7Flt79D22G8kZZe5IGkFj5T8pZpEC6NBKWX+k666Bd+G68TmGFcaSzKgKs/AGRtR6iwJKMp1U9CD9+jr7WAC5j/jDtwHLzYTv+zxEt8ufNv5ewSrGtnXelBkAQmAo8dhvrfik0G/rpB5RhM8FZOfhV+fqjLvtcBd0vOVrDMLHaZJ+2TpjDnjScA+GIS30EEwnx8TpsoJM5PLJUL7b9xNZEECQAklnZt59gSg2e0PF6rI2Q2Fb9HIVDETPKoNK/9X7MfWC9uWr/GEA4G61tLYq5NnMEAqj3+AT6YTWb3Vy5xDauTHftbAogQEWa/2S5GyHUVQ0zh850aOy9AQMCbUaN5hTk/x6AFJIY9bvFEGVJ3Wr8HZPIh/WAqgRqgnLRx6RxSqymBsh/I2SYLaWtM027hkUlcAfj3HbYMkBdD8UXx3AzPvXAwhDojUPMWwK74La9MwiDRDO+fxFdhOIhrbh4ib1MaH7dqrcnDX6OjdQfZ0QR3lNRYet8wKlxVL3xy3ppRfLys8Wrvhdi3kBplKJxW5CyoI4fEJLtyvC2NM3olt9ZvVbM=");
	}

	@Override
	public EquipmentSlot slot() {
		return EquipmentSlot.HEAD;
	}

	@Override
	public String setId() {
		return THERMODYNAMIC;
	}

	/** Set id of every Thermodynamic piece; what {@code ItemUtils.isThermoSet} counts. */
	public static final String THERMODYNAMIC = "Thermodynamic";
}
