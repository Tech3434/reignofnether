package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Entity registry. The surviving template units are now data-driven (base mobs like
 * {@code minecraft:villager}), so the mod registers no content entity types of its own. Kept as a
 * hook for a faction that needs a custom body.
 */
public class EntityRegistrar {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ReignOfNether.MOD_ID);

    public static void init(ModContainer container) {
        ENTITIES.register(container.getEventBus());
    }
}
