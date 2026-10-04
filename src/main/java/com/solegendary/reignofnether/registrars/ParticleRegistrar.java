package com.solegendary.reignofnether.registrars;

import java.util.function.Supplier;
import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ParticleRegistrar {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, ReignOfNether.MOD_ID);

    public static final Supplier<SimpleParticleType> BIG_ENCHANT =
            PARTICLES.register("big_enchant",
                    () -> new SimpleParticleType(false));

    public static final Supplier<SimpleParticleType> BIG_SOUL_FLAME =
            PARTICLES.register("big_soul_flame",
                    () -> new SimpleParticleType(false));

    public static final Supplier<SimpleParticleType> LEVEL_UP =
            PARTICLES.register("level_up",
                    () -> new SimpleParticleType(false));

    public static void init(ModContainer context) {
        PARTICLES.register(context.getEventBus());
    }
}
