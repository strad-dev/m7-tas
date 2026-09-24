package items.armor;

import damage.Rarity;
import damage.ReforgeId;
import items.ItemFactory;
import items.Wearable;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Mage set's Intelligence hat. */
public final class StormHelmet implements Wearable {
	public static final StormHelmet INSTANCE = new StormHelmet();

	private StormHelmet() {}

	@Override
	public Material material() {
		return Material.PLAYER_HEAD;
	}

	@Override
	public String baseName() {
		return "Storm's Helmet";
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
		return ItemFactory.head(colouredName(reforge), "stormHelmet",
				"ewogICJ0aW1lc3RhbXAiIDogMTc0NjgwNzcwMTU2MSwKICAicHJvZmlsZUlkIiA6ICJjNDIzYjQwMWZiOGU0ODc3YjMzMmVmMjhiZDdlZGZmZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJSZWFjdGlvbkJyaW5lWVQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTliYWVjNWZiNGNkOWRjNTk2ZDYxMGI2YzZiZDM4YWI5OTAxYjY4Yzk1OTQ5ZTJkNzFiOTI1MzE3MjcwZDAxMSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
				"s8X+QmhjqwppG9pqW9SYQloIzPVTw3PBpMprwnx9pl9j2uNdBgJbpwhahgo3WjpXOV9aiewogO7HDqZ71fns/rkPLVBANO6mlnYS8J+J8rLkQFiinQERx4ucYtHM9atzZnG7dDv6QTK6Bvur8SwVhZIOYSj7YWN1ecrbm9RskNhiRSXVwFH/TcWdSv4z/c0zG2b+OXaD68NAwxTd8lszNl+JSWFU6dP/l8GP1EWDNz8WagfwzeOTaHU2rDztRCUXlNGeF16QdZBXgFUva3Kel6D0QSE492Q1vTt5f55xwk38Yjbw6wkv2se+arcd9sbInuxlJamev6J4FX0r1QhGpgxHDvu30O/htK7ni8Og4AWgESQg/ONo/R7GUYsysao3lV46cHGK9JBEQEG0Zlq+gQ9ajzLojLchfSMM03/V8FpyLKBsplMJuG3NNz4QLXlflWU3UpuXD7SDGgIcn4UVRlANhC/Nj2qO4DUVkMA6V3OSGFWdLe9ICMZfLPXQiGFkZd4SmJLp6dy/Z2C7DGZci7qSkTXBW8j1Zmz52dSvaNqQvb10nSS+EVG8yggniRMheW8s8d6fs4fwrXfj+so2ayTjtImr8eafK1CpIARWCDEXZhQEs/rFv4dpuRaziJw69eVpem0ZwMRe7V4bf98SA5+yxgdYMtxoWUi+uMvKC8U=");
	}

	@Override
	public EquipmentSlot slot() {
		return EquipmentSlot.HEAD;
	}
}
