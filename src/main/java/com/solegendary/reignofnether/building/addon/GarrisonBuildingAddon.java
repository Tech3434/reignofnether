package com.solegendary.reignofnether.building.addon;

import com.solegendary.reignofnether.building.BuildingPlacement;

import net.minecraft.core.BlockPos;

import org.jetbrains.annotations.Nullable;

/**
 * Engine data-driven addon backing {@link GarrisonableBuildingAddon} (plan CONTENT_JSON_PLAN.md).
 * {@code params}: {@code capacity} (0 disables), {@code attackRange}, {@code externalAttackRangeBonus}
 * (default {@code min(15, attackRange/2)}), and optional entry/exit offsets relative to the building
 * origin ({@code entryX/Y/Z}, {@code exitX/Y/Z}, default 0 - meaning the origin corner itself).
 */
public class GarrisonBuildingAddon implements GarrisonableBuildingAddon {

    private final int capacity;
    private final int attackRange;
    private final int externalAttackRangeBonus;
    private final BlockPos entryOffset;
    private final BlockPos exitOffset;

    public GarrisonBuildingAddon(AddonSpec spec) {
        this.capacity = (int) spec.param("capacity", 0);
        this.attackRange = (int) spec.param("attackRange", 20);
        this.externalAttackRangeBonus = (int) spec.param("externalAttackRangeBonus", Math.min(15, attackRange / 2));
        this.entryOffset = new BlockPos(
                (int) spec.param("entryX", 0), (int) spec.param("entryY", 0), (int) spec.param("entryZ", 0));
        this.exitOffset = new BlockPos(
                (int) spec.param("exitX", 0), (int) spec.param("exitY", 0), (int) spec.param("exitZ", 0));
    }

    @Override
    public int getAttackRange() {
        return attackRange;
    }

    @Override
    public int getExternalAttackRangeBonus() {
        return externalAttackRangeBonus;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    @Nullable
    public BlockPos getEntryPosition(BuildingPlacement placement) {
        return capacity <= 0 ? null : placement.originPos.offset(entryOffset);
    }

    @Override
    @Nullable
    public BlockPos getExitPosition(BuildingPlacement placement) {
        return capacity <= 0 ? null : placement.originPos.offset(exitOffset);
    }
}
