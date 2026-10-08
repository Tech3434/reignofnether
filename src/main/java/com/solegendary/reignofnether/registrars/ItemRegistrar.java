package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.items.FoilableItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Item registry.
 *
 * <p>Only what the surviving template content needs is registered: spawn eggs for the two unit
 * types, plus the mod's own utility items. Everything that belonged to the removed factions - 46 more
 * spawn eggs and the hero trinket set - went with it.
 */
public class ItemRegistrar {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, com.solegendary.reignofnether.ReignOfNether.MOD_ID);

    /**
     * A throwable block of TNT. Unlike a primed TNT it lands where it is thrown and becomes an
     * adjustable one, which is what makes it usable as a unit ability rather than a world hazard.
     */
    public static final Supplier<Item> THROWABLE_TNT =
            ITEMS.register("throwable_tnt", () -> new FoilableItem(new Item.Properties()));

    public static void init(net.neoforged.fml.ModContainer container) {
        ITEMS.register(container.getEventBus());
    }
}
