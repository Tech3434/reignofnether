package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.ReignOfNether;

import net.minecraft.resources.ResourceLocation;

/**
 * Bootstraps the engine's built-in data-driven ability types, mirroring {@code Addons.init()} for
 * buildings. A faction author registers its own ability classes the same way
 * ({@code AbilityTypes.register(id, spec -> new MyAbility(spec))}).
 */
public final class BuiltInAbilities {

    private BuiltInAbilities() { }

    public static void init() {
        AbilityTypes.register(id("heal"), (spec, unit) -> new SimpleHealAbility(spec));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, path);
    }
}
