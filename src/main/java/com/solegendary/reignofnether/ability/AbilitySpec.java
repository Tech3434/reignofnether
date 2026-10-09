package com.solegendary.reignofnether.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.solegendary.reignofnether.research.ResearchCondition;

import net.minecraft.resources.ResourceLocation;

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
        Map<String, Double> params
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
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("params", Map.of()).forGetter(AbilitySpec::params)
    ).apply(instance, AbilitySpec::new));

    public double param(String key, double fallback) {
        return params.getOrDefault(key, fallback);
    }
}
