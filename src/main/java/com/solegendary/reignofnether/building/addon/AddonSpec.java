package com.solegendary.reignofnether.building.addon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.solegendary.reignofnether.ability.AbilityParam;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A data-driven building addon (plan CONTENT_JSON_PLAN.md): {@code type} names a code addon class
 * registered in {@link AddonTypes}; {@code params} carries its parameters.
 *
 * <p>Params are typed like ability params: a JSON number or a string (a resource name, a
 * {@link ResourceLocation} to an entity/block/item, …). Numeric access via {@link #param} keeps the
 * old call sites working unchanged.
 */
public record AddonSpec(ResourceLocation type, Map<String, AbilityParam> params) {

    public static final Codec<AddonSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("type").forGetter(AddonSpec::type),
            Codec.unboundedMap(Codec.STRING, AbilityParam.CODEC).optionalFieldOf("params", Map.of()).forGetter(AddonSpec::params)
    ).apply(instance, AddonSpec::new));

    /** A numeric param (JSON number), or {@code fallback} if absent. */
    public double param(String key, double fallback) {
        AbilityParam param = params.get(key);
        return param == null ? fallback : param.asDouble(fallback);
    }

    /** A string param, or {@code fallback} if absent. */
    public String stringParam(String key, String fallback) {
        AbilityParam param = params.get(key);
        return param == null ? fallback : param.asString(fallback);
    }

    /** A string param parsed as a {@link ResourceLocation} (entity/block/item id), or null. */
    @Nullable
    public ResourceLocation resourceParam(String key) {
        AbilityParam param = params.get(key);
        return param == null ? null : param.asResource();
    }
}
