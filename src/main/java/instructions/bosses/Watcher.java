package instructions.bosses;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_21_R3.profile.CraftPlayerProfile;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;
import java.util.UUID;

public class Watcher {
	@SuppressWarnings("FieldCanBeLocal")
	private static Zombie watcher;

	public static void watcherInstructions(World world) {
		if(watcher != null) {
			watcher.remove();
		}
		watcher = (Zombie) world.spawnEntity(new Location(world, -120.5, 72.0, -88.5, -180, 0), EntityType.ZOMBIE);
		watcher.setAI(false);
		watcher.setSilent(true);
		watcher.setPersistent(true);
		watcher.setRemoveWhenFarAway(false);

		GameProfile gp = new GameProfile(UUID.randomUUID(), "watcher");
		gp.getProperties().put("textures", new Property("textures", "ewogICJ0aW1lc3RhbXAiIDogMTc0Njg0OTQ1NjQxOSwKICAicHJvZmlsZUlkIiA6ICIxZjk0OTQzN2RlYmQ0ODgyYTlhYzZhZmZmN2RhNDcxMSIsCiAgInByb2ZpbGVOYW1lIiA6ICJNaWlra2FLYSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83YjBkNTI3OGNkZWUwNGM1MjBhOWY1ZDE1M2E1MmI0ZWZjNzBmMzAzMjM5MjY2OGQyMTExNjJkNWFkYzAxYjExIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=", "awvIfqy7f12hqzBY/BZrhqCpC3xl0zeb0xTOVERZlzXsmk+ZivSyC8ZAlsR1Kmam0aLNDlvO3Nrl8ZGg5n77H+aUkZsoGz4DsuV2GoFv71UxXpPgAVkiCw0kPmNr9O17JChNU2HrO2hd1X3kqPX9gbA/JZ4+kCpcmbEtr7+VAl7xScOEWvKZPimdijG6hkNrBnkcttk+TYdIenrKNrZf346l2nD9nRif+1istHv9ouxZ7GguZPFFTTqtuljhdjsDQ5lQnFN/Q0b4cENMErlAkzam4n2jwTBJPWz9BeIUdgpOr4qyp4bTOLrD3mVfdSEJ+Q4hMjQLZZeYLxMZLSCqm56ns+rzm7O0aj7/+sjxngWZuT8z4U+g2J5QOOA3n8R3Z+QvEHitb1RZdM8DccYb9VwSbGG2jZ8acInxSoIT5bFWWfp0Bh+rwfuNe+v2hFReyUz35BwKrYUOxqL4+A7/McSpik/C+9BVMYL5n78FMD+1+SlJniMwAoPlRpz87yGYivEH9aAlEnTLE+7Tpp6wsiFCaQp5WJ8vfJnV9HVxDYjFs7xB29Cw+FIQnYSsT5U7Uv6znjBMWRmHI9zeU7GzQ0eNQkThSbzX+dE/c1WyPXVuL/wTfefbgh6jm1i6rNGz/a3RdnWk8ItXu/pYQjSmKnc2FJH+x28VXkYl3qQr0gw="));

		CraftPlayerProfile profile = new CraftPlayerProfile(gp);

		ItemStack helmet = new ItemStack(Material.PLAYER_HEAD);
		SkullMeta meta = (SkullMeta) helmet.getItemMeta();
		assert meta != null;
		meta.setOwnerProfile(profile);
		helmet.setItemMeta(meta);

		Objects.requireNonNull(watcher.getEquipment()).setHelmet(helmet);
		watcher.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 255, false, false));
		watcher.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 255, false, false));
		Objects.requireNonNull(watcher.getAttribute(Attribute.SCALE)).setBaseValue(1.5);
	}
}