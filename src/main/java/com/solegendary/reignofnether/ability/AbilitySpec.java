package com.solegendary.reignofnether.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.solegendary.reignofnether.research.ResearchCondition;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * A data-driven ability instance (plan CONTENT_JSON_PLAN.md): {@code type} names a code ability class
 * registered in {@link AbilityTypes}; the common fields below are shared by every ability, and
 * {@code params} carries the class-specific numbers (e.g. {@code reignofnether:heal} → {@code amount}).
 *
 * <p>For the numeric flags a value of {@code 0}/{@code false} means "leave the ability class's own
 * default" — the class knows its range/radius/targeting, and JSON only overrides when it says so.
 */
public record AbilitySpec(
        ResourceLocation type,
        float cooldown,
        float mana,
        boolean passive,
        float range,
        float radius,
        boolean canTargetEntities,
        boolean oneClickOneUse,
        List<ResearchCondition> requiredResearch,
        Map<String, AbilityParam> params
) {

    public static final Codec<AbilitySpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("type").forGetter(AbilitySpec::type),
            Codec.FLOAT.optionalFieldOf("cooldown", 0f).forGetter(AbilitySpec::cooldown),
            Codec.FLOAT.optionalFieldOf("mana", 0f).forGetter(AbilitySpec::mana),
            Codec.BOOL.optionalFieldOf("passive", false).forGetter(AbilitySpec::passive),
            Codec.FLOAT.optionalFieldOf("range", 0f).forGetter(AbilitySpec::range),
            Codec.FLOAT.optionalFieldOf("radius", 0f).forGetter(AbilitySpec::radius),
            Codec.BOOL.optionalFieldOf("canTargetEntities", false).forGetter(AbilitySpec::canTargetEntities),
            Codec.BOOL.optionalFieldOf("oneClickOneUse", false).forGetter(AbilitySpec::oneClickOneUse),
            ResearchCondition.CODEC.listOf().optionalFieldOf("requiredResearch", List.of()).forGetter(AbilitySpec::requiredResearch),
            Codec.unboundedMap(Codec.STRING, AbilityParam.CODEC).optionalFieldOf("params", Map.of()).forGetter(AbilitySpec::params)
    ).apply(instance, AbilitySpec::new));

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

    /** A string param parsed as a {@link ResourceLocation} (entity/sound/item id), or null. */
    @Nullable
    public ResourceLocation resourceParam(String key) {
        AbilityParam param = params.get(key);
        return param == null ? null : param.asResource();
    }
}
