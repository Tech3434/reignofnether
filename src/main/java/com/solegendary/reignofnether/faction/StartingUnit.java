package com.solegendary.reignofnether.faction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/** One entry of a faction's starting army: an entity type id and how many of it to spawn. */
public record StartingUnit(ResourceLocation entityType, int count) {

    public static final Codec<StartingUnit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("unit").forGetter(StartingUnit::entityType),
            Codec.INT.optionalFieldOf("count", 1).forGetter(StartingUnit::count)
    ).apply(instance, StartingUnit::new));
}
