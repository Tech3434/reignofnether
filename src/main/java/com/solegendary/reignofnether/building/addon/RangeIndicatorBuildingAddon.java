package com.solegendary.reignofnether.building.addon;

import com.solegendary.reignofnether.blocks.BlockClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.util.MiscUtil;

import net.minecraft.core.BlockPos;

import java.util.Set;

/**
 * Engine data-driven addon backing {@link RangeIndicatorAddon} (plan CONTENT_JSON_PLAN.md): draws a
 * radius circle around the building. {@code params}: {@code range} (blocks), {@code showOnlyWhenSelected}
 * (default 1).
 */
public class RangeIndicatorBuildingAddon implements RangeIndicatorAddon {

    private final int range;
    private final boolean showOnlyWhenSelected;

    public RangeIndicatorBuildingAddon(AddonSpec spec) {
        this.range = (int) spec.param("range", 0);
        this.showOnlyWhenSelected = spec.param("showOnlyWhenSelected", 1) != 0;
    }

    @Override
    public int getRange(BuildingPlacement placement) {
        return placement.isBuilt ? range : 0;
    }

    @Override
    public boolean showOnlyWhenSelected(BuildingPlacement placement) {
        return showOnlyWhenSelected;
    }

    @Override
    public void updateHighlightBps(BuildingPlacement placement) {
        if (!placement.level.isClientSide() || getRange(placement) <= 0)
            return;
        Set<BlockPos> highlightBps = placement.getDataStorage().getData(RangeIndicatorAddon.HIGHLIGHT_BPS_CACHE);
        highlightBps.clear();
        highlightBps.addAll(MiscUtil.getRangeIndicatorCircleBlocks(
                placement.centrePos,
                getRange(placement) - BlockClientEvents.VISIBLE_BORDER_ADJ,
                placement.level,
                true
        ));
    }
}
