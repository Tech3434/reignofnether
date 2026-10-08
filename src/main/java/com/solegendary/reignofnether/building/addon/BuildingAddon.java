package com.solegendary.reignofnether.building.addon;

import com.solegendary.reignofnether.building.BuildingPlacement;

import net.minecraft.world.level.Level;

/**
 * Marker interface for building addons (plan CONTENT_JSON_PLAN.md): engine behaviour a building enables
 * by id in its JSON ({@code addons: [{ "type": ..., "params": {...} }]}). Concrete addons live in this
 * package and are registered in {@link AddonTypes} (engine ones via {@link Addons}).
 *
 * <p>Specific behaviour is expressed through the sub-interfaces ({@code GarrisonableBuildingAddon},
 * {@code NightSourceAddon}, {@code NetherConvertingAddon}, {@code RangeIndicatorAddon}), which building
 * code queries with {@code Building.getActiveAddon(...)}. The lifecycle hooks below let an addon act when
 * the building is completed and each tick.
 */
public interface BuildingAddon {

    /** Called once on both sides when the building finishes construction. */
    default void onBuildingBuilt(BuildingPlacement placement) { }

    /** Called every tick on both sides. */
    default void onBuildingTick(Level level, BuildingPlacement placement) { }
}
