package nms;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class NBT {
    public static ItemStack modify(ItemStack stack, Consumer<CompoundTag> consumer) {
        // ItemStack#update is too freaky
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).update(consumer);
        stack.set(DataComponents.CUSTOM_DATA, data);
        return stack;
    }

    public static org.bukkit.inventory.ItemStack modify(org.bukkit.inventory.ItemStack stack, Consumer<CompoundTag> consumer) {
        return modify(CraftItemStack.asNMSCopy(stack), consumer).asBukkitCopy();
    }

    public static @Nullable String getId(org.bukkit.inventory.ItemStack stack) {
        return getId(CraftItemStack.asNMSCopy(stack));
    }

    public static @Nullable String getId(ItemStack stack) {
        return getCustomData(stack).getString("id").orElse(null);
    }

    public static CompoundTag getCustomData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
