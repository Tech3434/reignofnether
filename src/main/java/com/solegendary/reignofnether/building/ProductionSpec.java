package com.solegendary.reignofnether.building;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.solegendary.reignofnether.unit.UnitDefinition;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.function.Function;

/**
 * One entry of a building's {@code production} list (plan CONTENT_JSON_PLAN.md): usually just the unit
 * definition id, but a building may also state its own cost for that unit by using the object form.
 *
 * <pre>
 * "production": [
 *   "reignofnether:villager_unit",
 *   { "unit": "reignofnether:skeleton_unit", "costOverride": { "food": 30, "seconds": 8 } }
 * ]
 * </pre>
 *
 * <p>A {@link UnitDefinition.CostSpec} only overrides the fields it sets; the rest still come from the
 * unit definition. Encoded without an override, the entry is written as the plain string form.
 */
public record ProductionSpec(ResourceLocation unit, Optional<UnitDefinition.CostSpec> costOverride) {

    private static final Codec<ProductionSpec> OBJECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("unit").forGetter(ProductionSpec::unit),
            UnitDefinition.CostSpec.CODEC.optionalFieldOf("costOverride").forGetter(ProductionSpec::costOverride)
    ).apply(instance, ProductionSpec::new));

    public static final Codec<ProductionSpec> CODEC = Codec.either(ResourceLocation.CODEC, OBJECT_CODEC).xmap(
            either -> either.map(ProductionSpec::of, Function.identity()),
            spec -> spec.costOverride().isPresent() ? Either.right(spec) : Either.left(spec.unit()));

    public static ProductionSpec of(ResourceLocation unit) {
        return new ProductionSpec(unit, Optional.empty());
    }
}
