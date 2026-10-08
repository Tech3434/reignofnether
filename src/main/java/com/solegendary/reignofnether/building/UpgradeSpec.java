package com.solegendary.reignofnether.building;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.solegendary.reignofnether.unit.UnitDefinition;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

/**
 * One step of a building's upgrade chain (plan CONTENT_JSON_PLAN.md). A building lists its upgrades in
 * order; each upgrade takes a placement from level {@code i} to {@code i+1}, changing its structure,
 * display name and max health. The upgrade is started from the building UI (it is a production item)
 * and gated so only the next level can be bought.
 *
 * <p>Deferred for now: per-level production/researches/addons/abilities/icon (they need per-placement
 * overrides of the shared building template).
 */
public record UpgradeSpec(
        Optional<Map<String, String>> name,
        Optional<ResourceLocation> icon,
        Optional<ResourceLocation> structure,
        Optional<UnitDefinition.CostSpec> cost,
        Optional<Integer> maxHealth
) {

    public static final Codec<UpgradeSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("name").forGetter(UpgradeSpec::name),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(UpgradeSpec::icon),
            ResourceLocation.CODEC.optionalFieldOf("structure").forGetter(UpgradeSpec::structure),
            UnitDefinition.CostSpec.CODEC.optionalFieldOf("cost").forGetter(UpgradeSpec::cost),
            Codec.INT.optionalFieldOf("maxHealth").forGetter(UpgradeSpec::maxHealth)
    ).apply(instance, UpgradeSpec::new));

    /** Localized ("en_us") display name if one is defined. */
    public Optional<String> displayName() {
        return name.flatMap(m -> Optional.ofNullable(m.get("en_us")));
    }
}
