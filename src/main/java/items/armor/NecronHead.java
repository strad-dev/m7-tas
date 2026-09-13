package items.armor;

import damage.Rarity;
import damage.ReforgeId;
import items.ItemFactory;
import items.Wearable;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * The Diamond Necron Head.  <b>SPECIAL rarity</b>, which is why it is red - and since a special tier neither
 * recombobulates nor sits "above Mythic" on the reforge tables, {@code damage/Reforges} carries an explicit
 * {@code ANCIENT / ARMOR / SPECIAL} row for it.  The Necron Head Bonus doubles this piece's own stats and
 * nothing else the player is wearing, which is why that x2 lives on the item's own pipeline (§1.10).
 */
public final class NecronHead implements Wearable {
	public static final NecronHead INSTANCE = new NecronHead();

	private NecronHead() {}

	@Override
	public Material material() {
		return Material.PLAYER_HEAD;
	}

	@Override
	public String baseName() {
		return "Diamond Necron Head";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.SPECIAL;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.ANCIENT;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.head(colouredName(reforge), "dmndNecronHead",
				"ewogICJ0aW1lc3RhbXAiIDogMTYxNzM5MDg5NDU4NiwKICAicHJvZmlsZUlkIiA6ICJjNWFhNTRhNmNmNTI0YmFmYmRiODUwNmUyMjRiNzViZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJOZWNyb25IYW5kbGVQTFMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGIxMTVjZGM0NWZkODRmMjFmYmE3YWMwZjJiYzc3YmMzYjYzMDJiZTY3MDg0MmY2ZTExZjY2ZWI1NTdmMTNlZSIKICAgIH0KICB9Cn0=",
				"hwaaE9h0QvFmtFvk2bNyAYlPo34NvaCjX3VWPv5zaRVM8KjfqQB5sB85vlFRVwCXK/HnQS2qMJ39ZuYplxT895lIShidIVdF5UP8T6cb1svhA9TmEVKFY4pKFyhUjIhD95HvO3OoNWPlCmb9Mho4XIo3K4AavnKPbuu3/I58gQmfKI71xDq7r+DRf9Dlxc8r3mcsUrdEwTfvC2/eFszHc/vqQXNm1smH2QJVfki+AgddNndFt7qumeicVFmsk2GmPNHxjlgH0xPL0hG8WGEmH5+Fnnj/eoYSutnpDRXVPY0H/KOMIa2Prga524stPC0gYmVU9y/wviXzDmKiiAa4uPVhwd/L/DgUSIGio6NlLMyA+Uvyy02HEr3TmzQ6bPqLphSttaDaVWW8Ltd1wvz/+Hhii5tYSSm3l5cAZQAO1O/JN/FKqA7tv0v0ZWp8AS1qw0QeLrRrLKlri2Zmzj5iYv7exfAVUiYB8f95ZZOWg1FLOufSJeFsQC5S7gsnsdJsWvnwvUNQI4RDfIc59a5Hvgzr90jgMTNoBGSXyrsXpeXJb+T9R8xfSEQY5V1XwFd+3lz8XRbBUQHubxN+b9AGj5FpQ2j5oaAz+BXY2+Iq20qVvkFMeJXdTRT0VIZM4r06ml0R3SZ1Jfui4xMH9OmhR+Hz3mmMvLN+BewhmtucN28=");
	}

	@Override
	public EquipmentSlot slot() {
		return EquipmentSlot.HEAD;
	}
}
