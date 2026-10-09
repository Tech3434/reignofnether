package com.solegendary.reignofnether.building;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.solegendary.reignofnether.building.addon.AddonSpec;
import com.solegendary.reignofnether.research.ResearchCondition;
import com.solegendary.reignofnether.unit.UnitDefinition;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A data-defined building (plan CONTENT_JSON_PLAN.md, phase 5): the block layout comes from an NBT
 * structure ({@code structure}), everything else is data. Loaded from
 * {@code data/<namespace>/building/<name>.json} into the datapack registry
 * {@link BuildingDefinitions#BUILDING_KEY}.
 *
 * <p>{@code production} lists what this building can train (a plain unit-definition id, or an object
 * with a {@code costOverride}); {@code researches} lists the research ids it offers; both share the
 * building's queue (owner decision). The behaviour flags mirror {@link Building}'s fields and live
 * under {@code flags}.
 */
public record BuildingDefinition(
        ResourceLocation structure,
        Optional<Map<String, String>> name,
        Optional<ResourceLocation> icon,
        Optional<UnitDefinition.CostSpec> cost,
        int maxHealth,
        int populationSupply,
        boolean isCapitol,
        Flags flags,
        List<ProductionSpec> production,
        List<ResourceLocation> researches,
        List<AddonSpec> addons,
        List<UpgradeSpec> upgrades,
        List<ResearchCondition> requiredResearch
) {

    /** Behaviour flags mirroring {@link Building}'s fields (all optional in JSON, under {@code flags}). */
    public record Flags(
            boolean canAcceptResources,
            double buildTimeModifier,
            int captureRange,
            boolean capturable,
            boolean invulnerable,
            boolean repairable,
            double repairTimeModifier,
            boolean drawAggro,
            Optional<Building.ScaffoldFill> scaffoldFill,
            Optional<ResourceLocation> scaffoldBlock,
            Optional<ResourceLocation> portrait,
            int foundationYLayers
    ) {
        public static final Flags DEFAULT = new Flags(false, 1.0, 20, false, false, true, 1.25, true,
                Optional.empty(), Optional.empty(), Optional.empty(), 1);

        public static final Codec<Flags> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("canAcceptResources", false).forGetter(Flags::canAcceptResources),
                Codec.DOUBLE.optionalFieldOf("buildTimeModifier", 1.0).forGetter(Flags::buildTimeModifier),
                Codec.INT.optionalFieldOf("captureRange", 20).forGetter(Flags::captureRange),
                Codec.BOOL.optionalFieldOf("capturable", false).forGetter(Flags::capturable),
                Codec.BOOL.optionalFieldOf("invulnerable", false).forGetter(Flags::invulnerable),
                Codec.BOOL.optionalFieldOf("repairable", true).forGetter(Flags::repairable),
                Codec.DOUBLE.optionalFieldOf("repairTimeModifier", 1.25).forGetter(Flags::repairTimeModifier),
                Codec.BOOL.optionalFieldOf("drawAggro", true).forGetter(Flags::drawAggro),
                StringRepresentable.fromEnum(Building.ScaffoldFill::values).optionalFieldOf("scaffoldFill").forGetter(Flags::scaffoldFill),
                ResourceLocation.CODEC.optionalFieldOf("scaffoldBlock").forGetter(Flags::scaffoldBlock),
                ResourceLocation.CODEC.optionalFieldOf("portrait").forGetter(Flags::portrait),
                // how many bottom Y layers of the structure are foundation: their block types become
                // startingBlockTypes (pre-queued on placement) and they are exempt from the
                // "not yet built -> destroyed" check
                Codec.INT.optionalFieldOf("foundationYLayers", 1).forGetter(Flags::foundationYLayers)
        ).apply(instance, Flags::new));
    }

    public static final Codec<BuildingDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("structure").forGetter(BuildingDefinition::structure),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("name").forGetter(BuildingDefinition::name),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(BuildingDefinition::icon),
            UnitDefinition.CostSpec.CODEC.optionalFieldOf("cost").forGetter(BuildingDefinition::cost),
            Codec.INT.optionalFieldOf("maxHealth", 100).forGetter(BuildingDefinition::maxHealth),
            Codec.INT.optionalFieldOf("populationSupply", 0).forGetter(BuildingDefinition::populationSupply),
            Codec.BOOL.optionalFieldOf("isCapitol", false).forGetter(BuildingDefinition::isCapitol),
            Flags.CODEC.optionalFieldOf("flags", Flags.DEFAULT).forGetter(BuildingDefinition::flags),
            ProductionSpec.CODEC.listOf().optionalFieldOf("production", List.of()).forGetter(BuildingDefinition::production),
            ResourceLocation.CODEC.listOf().optionalFieldOf("researches", List.of()).forGetter(BuildingDefinition::researches),
            AddonSpec.CODEC.listOf().optionalFieldOf("addons", List.of()).forGetter(BuildingDefinition::addons),
            UpgradeSpec.CODEC.listOf().optionalFieldOf("upgrades", List.of()).forGetter(BuildingDefinition::upgrades),
            ResearchCondition.CODEC.listOf().optionalFieldOf("requiredResearch", List.of()).forGetter(BuildingDefinition::requiredResearch)
    ).apply(instance, BuildingDefinition::new));

    /**
     * Applies one upgrade on top of this definition, overriding only the fields the upgrade specifies
     * (see {@link UpgradeSpec}). {@code upgrades}/{@code isCapitol}/{@code cost}/{@code flags} are never
     * changed by an upgrade. Used to build the cumulative definition for a given level.
     */
    public BuildingDefinition withUpgrade(UpgradeSpec upgrade) {
        return new BuildingDefinition(
                upgrade.structure().orElse(structure),
                upgrade.name().isPresent() ? upgrade.name() : name,
                upgrade.icon().isPresent() ? upgrade.icon() : icon,
                cost,
                upgrade.maxHealth().orElse(maxHealth),
                upgrade.populationSupply().orElse(populationSupply),
                isCapitol,
                flags,
                upgrade.production().orElse(production),
                upgrade.researches().orElse(researches),
                upgrade.addons().isPresent() ? upgrade.addons().get() : addons,
                upgrades,
                requiredResearch
        );
    }
}
