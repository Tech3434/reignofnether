package com.solegendary.reignofnether.building.production;

import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.resources.ResourceCost;

import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Data-driven production item: instead of a code class per unit, it spawns a unit definition
 * ({@code reignofnether:villager_unit}) from the building's production queue (plan CONTENT_JSON_PLAN.md).
 */
public class JsonProductionItem extends ProductionItem {

    private final ResourceLocation unitDefinitionId;
    private final String displayName;

    public JsonProductionItem(ResourceLocation unitDefinitionId, ResourceCost cost, String displayName) {
        super(cost);
        this.unitDefinitionId = unitDefinitionId;
        this.displayName = displayName;
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide())
                placement.produceUnit((ServerLevel) level, unitDefinitionId, placement.ownerName, true, new Vec3i(0, 0, 0));
        };
    }

    public ResourceLocation getUnitDefinitionId() {
        return unitDefinitionId;
    }

    @Override
    public String getNetworkId() {
        return unitDefinitionId.toString();
    }

    @Override
    public String getItemName() {
        return displayName;
    }

    @Override
    public ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/iron_sword.png");
    }

    @Override
    public List<FormattedCharSequence> getTooltipLines() {
        return List.of(FormattedCharSequence.forward(displayName, Style.EMPTY));
    }
}
