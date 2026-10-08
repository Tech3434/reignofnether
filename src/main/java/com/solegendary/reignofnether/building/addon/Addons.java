package com.solegendary.reignofnether.building.addon;

import com.solegendary.reignofnether.ReignOfNether;

import net.minecraft.resources.ResourceLocation;

/**
 * Bootstraps the engine's built-in data-driven building addon types, mirroring how abilities are
 * registered in {@code AbilityTypes} (plan CONTENT_JSON_PLAN.md). A faction author registers its own
 * addon types from its own init hook the same way.
 */
public final class Addons {

    private Addons() { }

    public static void init() {
        AddonTypes.register(id("night_source"), (spec, building) -> new NightSourceBuildingAddon(spec));
        AddonTypes.register(id("range_indicator"), (spec, building) -> new RangeIndicatorBuildingAddon(spec));
        AddonTypes.register(id("garrison"), (spec, building) -> new GarrisonBuildingAddon(spec));
        AddonTypes.register(id("nether_converting"), (spec, building) -> new NetherConvertingBuildingAddon(spec));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, path);
    }
}
