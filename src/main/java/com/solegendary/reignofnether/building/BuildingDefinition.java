package com.solegendary.reignofnether.building;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.solegendary.reignofnether.research.ResearchCondition;
import com.solegendary.reignofnether.unit.UnitDefinition;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A data-defined building (plan CONTENT_JSON_PLAN.md, phase 5): the block layout comes from an NBT
 * structure ({@code structure}), everything else is data. Loaded from
 * {@code data/<namespace>/building/<name>.json} into the datapack registry
 * {@link BuildingDefinitions#BUILDING_KEY}.
 *
 * <p>{@code production} lists the unit-definition ids this building can train; {@code researches}
 * lists the research ids it offers; both share the building's queue (owner decision).
 */
public record BuildingDefinition(
        ResourceLocation structure,
        Optional<Map<String, String>> name,
        Optional<ResourceLocation> icon,
        Optional<UnitDefinition.CostSpec> cost,
        int maxHealth,
        int populationSupply,
        boolean isCapitol,
        List<ResourceLocation> production,
        List<ResourceLocation> researches,
        List<ResearchCondition> requiredResearch
) {

    public static final Codec<BuildingDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("structure").forGetter(BuildingDefinition::structure),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("name").forGetter(BuildingDefinition::name),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(BuildingDefinition::icon),
            UnitDefinition.CostSpec.CODEC.optionalFieldOf("cost").forGetter(BuildingDefinition::cost),
            Codec.INT.optionalFieldOf("maxHealth", 100).forGetter(BuildingDefinition::maxHealth),
            Codec.INT.optionalFieldOf("populationSupply", 0).forGetter(BuildingDefinition::populationSupply),
            Codec.BOOL.optionalFieldOf("isCapitol", false).forGetter(BuildingDefinition::isCapitol),
            ResourceLocation.CODEC.listOf().optionalFieldOf("production", List.of()).forGetter(BuildingDefinition::production),
            ResourceLocation.CODEC.listOf().optionalFieldOf("researches", List.of()).forGetter(BuildingDefinition::researches),
            ResearchCondition.CODEC.listOf().optionalFieldOf("requiredResearch", List.of()).forGetter(BuildingDefinition::requiredResearch)
    ).apply(instance, BuildingDefinition::new));
}
