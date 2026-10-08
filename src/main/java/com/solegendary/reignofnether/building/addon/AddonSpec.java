package com.solegendary.reignofnether.building.addon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * A data-driven building addon (plan CONTENT_JSON_PLAN.md): {@code type} names a code addon class
 * registered in {@link AddonTypes}; {@code params} carries its numbers (numeric only for now).
 */
public record AddonSpec(ResourceLocation type, Map<String, Double> params) {

    public static final Codec<AddonSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("type").forGetter(AddonSpec::type),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("params", Map.of()).forGetter(AddonSpec::params)
    ).apply(instance, AddonSpec::new));

    public double param(String key, double fallback) {
        return params.getOrDefault(key, fallback);
    }
}
