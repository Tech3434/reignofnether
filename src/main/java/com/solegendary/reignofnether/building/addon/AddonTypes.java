package com.solegendary.reignofnether.building.addon;

import com.solegendary.reignofnether.building.Building;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of data-driven addon classes: an author's addon class registers its {@code type} id and a
 * factory that turns an {@link AddonSpec} into a configured {@link BuildingAddon}. That is how
 * {@code { "type": "garrison", ... }} in a building's JSON becomes behaviour (plan CONTENT_JSON_PLAN.md).
 */
public final class AddonTypes {

    public interface Factory {
        BuildingAddon create(AddonSpec spec, Building building);
    }

    private static final Map<ResourceLocation, Factory> TYPES = new LinkedHashMap<>();

    private AddonTypes() { }

    public static void register(ResourceLocation type, Factory factory) {
        TYPES.put(type, factory);
    }

    @Nullable
    public static BuildingAddon create(AddonSpec spec, Building building) {
        Factory factory = TYPES.get(spec.type());
        return factory == null ? null : factory.create(spec, building);
    }

    public static boolean exists(ResourceLocation type) {
        return TYPES.containsKey(type);
    }
}
