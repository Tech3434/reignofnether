package com.solegendary.reignofnether.unit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.solegendary.reignofnether.ability.AbilitySpec;
import com.solegendary.reignofnether.research.ResearchCondition;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.util.HashMap;
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
 *
 * <p>Every inheritable field is {@link Optional}: a definition may set {@code inherits} to another id
 * and override only what differs. Resolution (see {@link UnitDefinitions#resolve}) merges the parent's
 * values for anything the child leaves unset.
 */
public record UnitDefinition(
        Optional<ResourceLocation> base,
        Optional<ResourceLocation> inherits,
        Optional<Map<String, String>> name,
        Optional<ResourceLocation> icon,
        Optional<Role> role,
        Optional<Flags> flags,
        Optional<Double> scale,
        Optional<Map<ResourceLocation, Double>> attributes,
        Optional<CostSpec> cost,
        Optional<Integer> population,
        Optional<List<ResearchCondition>> requiredResearch,
        Optional<ResourceLocation> equipment,
        Optional<ProjectileSpec> projectile,
        Optional<List<AbilitySpec>> abilities
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

    /**
     * A ranged unit's projectile (plan CONTENT_JSON_PLAN.md): the entity spawned by
     * {@code performUnitRangedAttack}. {@code damage < 0} means "use the unit's attack damage".
     */
    public record ProjectileSpec(ResourceLocation entity, double velocity, double damage, double inaccuracy) {
        public static final Codec<ProjectileSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("entity").forGetter(ProjectileSpec::entity),
                Codec.DOUBLE.optionalFieldOf("velocity", 1.6).forGetter(ProjectileSpec::velocity),
                Codec.DOUBLE.optionalFieldOf("damage", -1.0).forGetter(ProjectileSpec::damage),
                Codec.DOUBLE.optionalFieldOf("inaccuracy", 1.0).forGetter(ProjectileSpec::inaccuracy)
        ).apply(instance, ProjectileSpec::new));
    }

    public static final Codec<UnitDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("base").forGetter(UnitDefinition::base),
            ResourceLocation.CODEC.optionalFieldOf("inherits").forGetter(UnitDefinition::inherits),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("name").forGetter(UnitDefinition::name),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(UnitDefinition::icon),
            StringRepresentable.fromEnum(Role::values).optionalFieldOf("role").forGetter(UnitDefinition::role),
            Flags.CODEC.optionalFieldOf("flags").forGetter(UnitDefinition::flags),
            Codec.DOUBLE.optionalFieldOf("scale").forGetter(UnitDefinition::scale),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.DOUBLE).optionalFieldOf("attributes").forGetter(UnitDefinition::attributes),
            CostSpec.CODEC.optionalFieldOf("cost").forGetter(UnitDefinition::cost),
            Codec.INT.optionalFieldOf("population").forGetter(UnitDefinition::population),
            ResearchCondition.CODEC.listOf().optionalFieldOf("requiredResearch").forGetter(UnitDefinition::requiredResearch),
            ResourceLocation.CODEC.optionalFieldOf("equipment").forGetter(UnitDefinition::equipment),
            ProjectileSpec.CODEC.optionalFieldOf("projectile").forGetter(UnitDefinition::projectile),
            AbilitySpec.CODEC.listOf().optionalFieldOf("abilities").forGetter(UnitDefinition::abilities)
    ).apply(instance, UnitDefinition::new));

    /** Effective role, defaulting to melee when unset. */
    public Role roleOrDefault() {
        return role.orElse(Role.MELEE);
    }

    /** Effective flags, defaulting to none when unset. */
    public Flags flagsOrDefault() {
        return flags.orElse(Flags.NONE);
    }

    /** Effective attributes, defaulting to empty when unset. */
    public Map<ResourceLocation, Double> attributesOrDefault() {
        return attributes.orElse(Map.of());
    }

    public List<ResearchCondition> requiredResearchOrDefault() {
        return requiredResearch.orElse(List.of());
    }

    public List<AbilitySpec> abilitiesOrDefault() {
        return abilities.orElse(List.of());
    }

    public int populationOrDefault() {
        return population.orElse(0);
    }

    /**
     * Returns this definition with {@code base}'s values filled in wherever this one left a field unset.
     * Attributes are merged per key (this definition wins), everything else is "this or base".
     */
    public UnitDefinition withInherited(UnitDefinition parent) {
        Optional<Map<ResourceLocation, Double>> mergedAttributes;
        if (attributes.isEmpty())
            mergedAttributes = parent.attributes();
        else if (parent.attributes().isEmpty())
            mergedAttributes = attributes;
        else {
            Map<ResourceLocation, Double> merged = new HashMap<>(parent.attributes().get());
            merged.putAll(attributes.get());
            mergedAttributes = Optional.of(merged);
        }

        return new UnitDefinition(
                base.isPresent() ? base : parent.base(),
                Optional.empty(),
                name.isPresent() ? name : parent.name(),
                icon.isPresent() ? icon : parent.icon(),
                role.isPresent() ? role : parent.role(),
                flags.isPresent() ? flags : parent.flags(),
                scale.isPresent() ? scale : parent.scale(),
                mergedAttributes,
                cost.isPresent() ? cost : parent.cost(),
                population.isPresent() ? population : parent.population(),
                requiredResearch.isPresent() ? requiredResearch : parent.requiredResearch(),
                equipment.isPresent() ? equipment : parent.equipment(),
                projectile.isPresent() ? projectile : parent.projectile(),
                abilities.isPresent() ? abilities : parent.abilities()
        );
    }
}
