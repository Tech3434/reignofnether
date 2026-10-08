package com.solegendary.reignofnether.unit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.solegendary.reignofnether.research.ResearchCondition;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A data-defined unit (plan `CONTENT_JSON_PLAN.md`): the body is a {@code base} EntityType (vanilla or
 * mod), everything else is data. Several definitions can share a {@code base} (e.g. a wolf and a big
 * wolf), differing by {@code scale}/abilities, so a unit's identity is its definition, not its type.
 *
 * <p>Loaded from {@code data/<namespace>/unit/<name>.json} into the datapack registry
 * {@link UnitDefinitions#UNIT_KEY}. Rendering is free: the base mob draws itself.
 */
public record UnitDefinition(
        ResourceLocation base,
        Optional<ResourceLocation> inherits,
        Optional<Map<String, String>> name,
        Optional<ResourceLocation> icon,
        Role role,
        Flags flags,
        Optional<Double> scale,
        Map<ResourceLocation, Double> attributes,
        Optional<CostSpec> cost,
        int population,
        List<ResearchCondition> requiredResearch
) {

    public enum Role implements StringRepresentable {
        MELEE, RANGED, WORKER, FLYING, HERO;

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    /** Behaviour toggles that decide which RTS subsystems apply to the unit. */
    public record Flags(boolean canGather, boolean canBuild, boolean canGarrison,
                        boolean holdPosition, boolean canPickupEquipment) {
        public static final Flags NONE = new Flags(false, false, false, false, false);

        public static final Codec<Flags> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("canGather", false).forGetter(Flags::canGather),
                Codec.BOOL.optionalFieldOf("canBuild", false).forGetter(Flags::canBuild),
                Codec.BOOL.optionalFieldOf("canGarrison", false).forGetter(Flags::canGarrison),
                Codec.BOOL.optionalFieldOf("holdPosition", false).forGetter(Flags::holdPosition),
                Codec.BOOL.optionalFieldOf("canPickupEquipment", false).forGetter(Flags::canPickupEquipment)
        ).apply(instance, Flags::new));
    }

    /** Cost of producing the unit; converted to {@code ResourceCost} on use. */
    public record CostSpec(int food, int wood, int ore, int emerald, int seconds) {
        public static final Codec<CostSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("food", 0).forGetter(CostSpec::food),
                Codec.INT.optionalFieldOf("wood", 0).forGetter(CostSpec::wood),
                Codec.INT.optionalFieldOf("ore", 0).forGetter(CostSpec::ore),
                Codec.INT.optionalFieldOf("emerald", 0).forGetter(CostSpec::emerald),
                Codec.INT.optionalFieldOf("seconds", 0).forGetter(CostSpec::seconds)
        ).apply(instance, CostSpec::new));
    }

    public static final Codec<UnitDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("base").forGetter(UnitDefinition::base),
            ResourceLocation.CODEC.optionalFieldOf("inherits").forGetter(UnitDefinition::inherits),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("name").forGetter(UnitDefinition::name),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(UnitDefinition::icon),
            StringRepresentable.fromEnum(Role::values).optionalFieldOf("role", Role.MELEE).forGetter(UnitDefinition::role),
            Flags.CODEC.optionalFieldOf("flags", Flags.NONE).forGetter(UnitDefinition::flags),
            Codec.DOUBLE.optionalFieldOf("scale").forGetter(UnitDefinition::scale),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.DOUBLE).optionalFieldOf("attributes", Map.of()).forGetter(UnitDefinition::attributes),
            CostSpec.CODEC.optionalFieldOf("cost").forGetter(UnitDefinition::cost),
            Codec.INT.optionalFieldOf("population", 0).forGetter(UnitDefinition::population),
            ResearchCondition.CODEC.listOf().optionalFieldOf("requiredResearch", List.of()).forGetter(UnitDefinition::requiredResearch)
    ).apply(instance, UnitDefinition::new));
}
