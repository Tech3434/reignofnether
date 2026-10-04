package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.util.ItemTagCompat;
import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.items.unititems.EdibleFoodItem;
import com.solegendary.reignofnether.time.TimeClientEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Holder;

public class ItemUtil {

    public static final float HEALTH_PER_BREAD = 12;
    public static final float HEALTH_PER_CHICKEN = 18;
    public static final float HEALTH_PER_BEEF = 24;
    public static final float HEAL_PER_NUTRITION = 2.5f;

    public static boolean hasUUID(ItemStack itemStack) {
        return itemStack != null && ItemTagCompat.tag(itemStack) != null && ItemTagCompat.tag(itemStack).hasUUID("uuid");
    }

    public static UUID getUUID(ItemStack itemStack) { // if no uuid, return a random one so we don't crash but just do nothing
        return hasUUID(itemStack) ? ItemTagCompat.tag(itemStack).getUUID("uuid") : UUID.randomUUID();
    }

    public static boolean isUnitItem(ItemStack itemStack) {
        return itemStack != null && getUnitItem(itemStack) != null;
    }

    public static boolean isUnitItem(ItemEntity entity) {
        return entity != null && isUnitItem(entity.getItem());
    }

    @Nullable
    public static UnitItem getUnitItem(ItemStack itemStack) {
        if (itemStack == null)
            return null;
        if (isPreparedEdibleFood(itemStack.getItem()))
            return new EdibleFoodItem(itemStack.getItem());
        outerLoop:
        for (UnitItem unitItem : UnitItems.ITEMS) {
            if (unitItem.item == itemStack.getItem()) {
                for (Pair<Holder<Enchantment>, Integer> pair : unitItem.enchantments) {
                    if (itemStack.getEnchantmentLevel(pair.getFirst()) != pair.getSecond())
                        continue outerLoop;
                }
                return unitItem;
            }
        }
        return null;
    }

    @Nullable
    public static UnitItem getUnitItem(UUID uuid) {
        for (UnitItem unitItem : UnitItems.ITEMS)
            if (unitItem.uuid.equals(uuid))
                return unitItem;

        for (Item item : BuiltInRegistries.ITEM) {
            if (isEdible(item) && EdibleFoodItem.getFoodUUID(item).equals(uuid))
                return new EdibleFoodItem(item);
        }
        return null;
    }

    public static Long getCooldownTicksLeft(ItemStack itemStack, Level level) {
        long gameTime = level.isClientSide() ? TimeClientEvents.serverGameTime : level.getGameTime();
        CompoundTag tag = ItemTagCompat.tag(itemStack);
        if (tag != null) {
            return Math.max(0, tag.getLong(UnitItem.RON$COOLDOWN_KEY) - gameTime);
        }
        return 0L;
    }

    private static List<Item> edibleFoods = List.of(
            Items.COOKED_BEEF,
            Items.COOKED_CHICKEN,
            Items.COOKED_COD,
            Items.COOKED_PORKCHOP,
            Items.COOKED_RABBIT,
            Items.COOKED_SALMON,
            Items.COOKED_MUTTON,
            Items.COOKIE,
            Items.BREAD,
            Items.PUMPKIN_PIE,
            Items.MUSHROOM_STEW,
            Items.RABBIT_STEW,
            Items.BEETROOT_SOUP,
            Items.BAKED_POTATO,
            Items.GOLDEN_APPLE,
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.GOLDEN_CARROT
    );

    /**
     * 1.21.1 removed {@code Item#isEdible()}: whether an item is food now lives in the
     * {@link net.minecraft.core.component.DataComponents#FOOD} component, so an empty stack of the
     * item is the closest equivalent to the old per-item query.
     */
    public static boolean isEdible(Item item) {
        return new ItemStack(item).has(DataComponents.FOOD);
    }

    public static boolean isPreparedEdibleFood(Item item) {
        return isEdible(item) && edibleFoods.contains(item);
    }

    public static float getFoodHealAmount(ItemStack itemStack) {
        // 1.21.1: FoodProperties is a record (nutrition()/saturation()) and Item no longer holds
        // one — the food data lives in the stack's DataComponents.FOOD component.
        FoodProperties props = itemStack.get(DataComponents.FOOD);
        int nutrition = props != null ? props.nutrition() : 0;
        if (itemStack.getItem() == Items.BREAD) {
            return HEALTH_PER_BREAD;
        } else if (itemStack.getItem() == Items.COOKED_CHICKEN) {
            return HEALTH_PER_CHICKEN;
        } else if (itemStack.getItem() == Items.COOKED_BEEF) {
            return HEALTH_PER_BEEF;
        } else {
            return nutrition * HEAL_PER_NUTRITION;
        }
    }
}
