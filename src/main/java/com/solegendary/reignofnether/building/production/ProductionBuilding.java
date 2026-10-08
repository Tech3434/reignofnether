package com.solegendary.reignofnether.building.production;

import com.solegendary.reignofnether.ability.Abilities;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.resources.ResourceCost;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

public abstract class ProductionBuilding extends Building {
    public ProductionItemList productions = new ProductionItemList();
    public boolean canSetRallyPoint = true;
    public float spawnRadiusOffset = 1.0F;

    /**
     * §14.3: production is exposed through the same ability list as everything else a building can
     * do, so there is no separate "productions" mechanism in the HUD. Built lazily once, because
     * subclasses add their production items in their own constructors, after ours.
     */
    private Abilities combinedAbilities = null;

    @Override
    public Abilities getAbilities() {
        if (combinedAbilities == null) {
            combinedAbilities = super.getAbilities().clone();
            for (ProductionItem item : productions.get())
                combinedAbilities.add(new ProductionAbility(item));
        }
        return combinedAbilities;
    }

    public ProductionBuilding(String structureName, ResourceCost cost, boolean isCapitol) {
        super(structureName, cost, isCapitol);
    }

    public BuildingPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        return new ProductionPlacement(this, level, pos, rotation, ownerName, BuildingUtils.getAbsoluteBlockData(this.getRelativeBlockData(level), level, pos, rotation), this.isCapitol);
    }

    public BlockPos getDefaultOutdoorSpawnPoint(BlockPos minCorner, ProductionPlacement pp) {
        return minCorner.offset((int) -spawnRadiusOffset, 0, (int) -spawnRadiusOffset);
    }

    public BlockPos getIndoorSpawnPoint(ServerLevel level, BuildingPlacement placement) {
        BlockPos spawnPoint;
        for(spawnPoint = placement.centrePos; level.getBlockState(spawnPoint.below()).isAir(); spawnPoint = spawnPoint.offset(0, -1, 0)) {
        }

        return spawnPoint;
    }
}
