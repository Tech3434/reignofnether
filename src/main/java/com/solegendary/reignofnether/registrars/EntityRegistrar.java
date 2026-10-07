package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import com.solegendary.reignofnether.unit.units.villagers.*;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class EntityRegistrar {

    private static final int UNIT_CLIENT_TRACKING_RANGE = 100;

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ReignOfNether.MOD_ID);

    public static final Supplier<EntityType<VillagerUnit>> VILLAGER_UNIT = ENTITIES.register("villager_unit",
            () -> EntityType.Builder.of(VillagerUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.VILLAGER.getWidth(), EntityType.VILLAGER.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "villager_unit").toString()));

    public static final Supplier<EntityType<VindicatorUnit>> VINDICATOR_UNIT = ENTITIES.register("vindicator_unit",
            () -> EntityType.Builder.of(VindicatorUnit::new, MobCategory.CREATURE)
                    .sized(EntityType.VINDICATOR.getWidth(), EntityType.VINDICATOR.getHeight())
                    .clientTrackingRange(UNIT_CLIENT_TRACKING_RANGE)
                    .build(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "vindicator_unit").toString()));

    public static void init(ModContainer container) {
        ENTITIES.register(container.getEventBus());
    }
}
