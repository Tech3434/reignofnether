package com.solegendary.reignofnether.building.addon;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.NetherZone;

import net.minecraft.core.BlockPos;

import org.jetbrains.annotations.Nullable;

/**
 * Engine data-driven addon backing {@link NetherConvertingAddon} (plan CONTENT_JSON_PLAN.md): a building
 * that converts the terrain around it to nether over time (subject to the
 * {@code DO_NETHER_CONVERSION} game rule). {@code params}: {@code maxRange} (0 disables),
 * {@code startingRange} (default 3).
 */
public class NetherConvertingBuildingAddon implements NetherConvertingAddon {

    private final double maxRange;
    private final double startingRange;

    public NetherConvertingBuildingAddon(AddonSpec spec) {
        this.maxRange = spec.param("maxRange", 0);
        this.startingRange = spec.param("startingRange", 3);
    }

    @Override
    public double getMaxNetherRange(BuildingPlacement placement) {
        return maxRange;
    }

    @Override
    public double getStartingNetherRange(BuildingPlacement placement) {
        return startingRange;
    }

    @Override
    public void onBuildingBuilt(BuildingPlacement placement) {
        if (maxRange <= 0)
            return;
        setNetherZone(placement, new NetherZone(
                new BlockPos(placement.centrePos.getX(), placement.originPos.getY() + 1, placement.centrePos.getZ()),
                maxRange, startingRange), true);
    }

    @Override
    @Nullable
    public NetherZone getNetherZone(BuildingPlacement placement) {
        return maxRange > 0 ? NetherConvertingAddon.super.getNetherZone(placement) : null;
    }
}
