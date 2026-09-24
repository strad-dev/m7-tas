package items.armor;

import damage.Rarity;
import damage.ReforgeId;
import items.ItemFactory;
import items.Wearable;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Necrotic Wither Goggles: Intelligence and the only Ability Damage on any armour (§1.10). In no default kit;
 * palette only.
 */
public final class WitherGoggles implements Wearable {
	public static final WitherGoggles INSTANCE = new WitherGoggles();

	private WitherGoggles() {}

	@Override
	public Material material() {
		return Material.PLAYER_HEAD;
	}

	@Override
	public String baseName() {
		return "Wither Goggles";
	}

	@Override
	public Rarity baseRarity() {
		return Rarity.EPIC;
	}

	@Override
	public ReforgeId defaultReforge() {
		return ReforgeId.NECROTIC;
	}

	@Override
	public ItemStack build(ReforgeId reforge) {
		return ItemFactory.head(colouredName(reforge), "witherGoggles",
				"ewogICJ0aW1lc3RhbXAiIDogMTYwNTU0MzM0MTg4MSwKICAicHJvZmlsZUlkIiA6ICJiMGQ0YjI4YmMxZDc0ODg5YWYwZTg2NjFjZWU5NmFhYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJNaW5lU2tpbl9vcmciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzdjZWI4ZjA3NThlMmQ4YWM0OWRlNmY5Nzc2MDNjN2JmYzIzZmQ4MmE4NTc0ODEwYTQ1ZjVlOTdjNjQzNmQ3OSIKICAgIH0KICB9Cn0=",
				"mqJHuSSijxc6VikSP8VL+tISFxCoCe6FA1s0ysbo7kp+eE8qYy7vRQTBqAI1rTSQmgQn1Oy0n+tFVFa7ToZSNVBM5VTllx6Nt1cipRTC4NkY4cilcqUeMddt1ao+jckKWsZ8nUD/8m9CCNtagldPKB5mhA458sN59j7iCTI1nR97cgDlIXZh9vDNG8/isTnDyzmpfyYUdSDj2+cSoILua+pgh7TVc/5Hg50kawkQ4dL1F4J05dHlA5eOnUgP352oWAYHmMMgblLY59n4/TkyyfSsz8YGd93enM+egfqQ2ipJU7Tgo0WhhbEdwE44BAi/mi1L2wW4YV18yJ7AxE4cvm9RxFM8Q1B9YBJ7MSUcwqvlf3kxoywaVbLBaOkeHrKIAKkqcyFljyDbLwTfXCAXhmBG0X0HMi+wPQCKki1oONMiZ0A1tJV5IzF/lOWptpSUpF/NSi/U3A+0V/EBjgdpXcCzpITPLC8v0nWfoUktnOytpZU+25/HGzRW+P93VTaT08kIFG8Cw2yIpQ/dAg2eTZwScUqePXpwKMD2vBIIQEEZpapi624g4004VGKQx/C8p4ZQg+/XydVM7doeG46IzM25nn5JQiqLkwZzft447fssR3bfSUxIWeiRmIy51FvYUU73yBF1BsPo0m7R3voZuwo7SEite6S2yW0ptjpAnP8=");
	}

	@Override
	public EquipmentSlot slot() {
		return EquipmentSlot.HEAD;
	}
}
