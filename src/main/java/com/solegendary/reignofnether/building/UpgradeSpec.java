package com.solegendary.reignofnether.building;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.solegendary.reignofnether.building.addon.AddonSpec;
import com.solegendary.reignofnether.unit.UnitDefinition;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One step of a building's upgrade chain (plan CONTENT_JSON_PLAN.md). A building lists its upgrades in
 * order; each upgrade takes a placement from level {@code i} to {@code i+1}. Every field is optional and
 * overrides the previous level's value when present, so an upgrade only states what it changes
 * ({@code structure}, {@code name}, {@code icon}, {@code maxHealth}, {@code populationSupply},
 * {@code production}, {@code researches}, {@code addons}).
 *
 * <p>Levels are materialised as separate {@link com.solegendary.reignofnether.building.buildings.JsonBuilding}
 * variants (base + one per cumulative upgrade); a placement switches to the new variant on upgrade, which
 * makes all level-dependent behaviour (production/queue, addons, abilities, name/icon) follow automatically.
 */
public record UpgradeSpec(
        Optional<Map<String, String>> name,
        Optional<ResourceLocation> icon,
        Optional<ResourceLocation> structure,
        Optional<UnitDefinition.CostSpec> cost,
        Optional<Integer> maxHealth,
        Optional<Integer> populationSupply,
        Optional<List<ResourceLocation>> production,
        Optional<List<ResourceLocation>> researches,
        Optional<List<AddonSpec>> addons
) {

    public static final Codec<UpgradeSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("name").forGetter(UpgradeSpec::name),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(UpgradeSpec::icon),
            ResourceLocation.CODEC.optionalFieldOf("structure").forGetter(UpgradeSpec::structure),
            UnitDefinition.CostSpec.CODEC.optionalFieldOf("cost").forGetter(UpgradeSpec::cost),
            Codec.INT.optionalFieldOf("maxHealth").forGetter(UpgradeSpec::maxHealth),
            Codec.INT.optionalFieldOf("populationSupply").forGetter(UpgradeSpec::populationSupply),
            ResourceLocation.CODEC.listOf().optionalFieldOf("production").forGetter(UpgradeSpec::production),
            ResourceLocation.CODEC.listOf().optionalFieldOf("researches").forGetter(UpgradeSpec::researches),
            AddonSpec.CODEC.listOf().optionalFieldOf("addons").forGetter(UpgradeSpec::addons)
    ).apply(instance, UpgradeSpec::new));

    /** Localized ("en_us") display name if one is defined. */
    public Optional<String> displayName() {
        return name.flatMap(m -> Optional.ofNullable(m.get("en_us")));
    }
}
