package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.unit.interfaces.Unit;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of data-driven ability classes: an author's ability class registers its {@code type} id and
 * a factory that turns an {@link AbilitySpec} into a configured {@link Ability} for a unit/building.
 * That is how {@code { "type": "poison_on_hit", ... }} in JSON becomes behaviour (plan CONTENT_JSON_PLAN.md).
 */
public final class AbilityTypes {

    /** Builds an ability from a data-driven spec and the owning unit. */
    public interface Factory {
        Ability create(AbilitySpec spec, Unit unit);
    }

    private static final Map<ResourceLocation, Factory> TYPES = new LinkedHashMap<>();

    private AbilityTypes() { }

    public static void register(ResourceLocation type, Factory factory) {
        TYPES.put(type, factory);
    }

    @Nullable
    public static Ability create(AbilitySpec spec, Unit unit) {
        Factory factory = TYPES.get(spec.type());
        return factory == null ? null : factory.create(spec, unit);
    }

    public static boolean exists(ResourceLocation type) {
        return TYPES.containsKey(type);
    }
}
