package com.solegendary.reignofnether.building.production;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.unit.units.villagers.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import javax.annotation.Nullable;
import java.util.List;

public class ProductionItems {
    public static final VillagerProd VILLAGER = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "villager"), new VillagerProd());
    public static final VindicatorProd VINDICATOR = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "vindicator"), new VindicatorProd());




    private static <T extends ProductionItem> T register(ResourceLocation id, T building) {
        return Registry.register(ReignOfNetherRegistries.PRODUCTION_ITEM, id, building);
    }

    public static void init() {}

    public static final List<ProductionItem> ALL = List.of(VILLAGER, VINDICATOR);

    @Nullable
    public static ProductionItem getProductionItem(EntityType<? extends Mob> entityType) {
        for (ProductionItem prodItem : ALL)
            if (prodItem.getEntityType() == entityType)
                return prodItem;
        return null;
    }

    @Nullable
    public static ProductionItem getProductionItem(String itemName) {
        for (ProductionItem prodItem : ALL)
            if (prodItem.getItemName().equals(itemName))
                return prodItem;
        return null;
    }
}
