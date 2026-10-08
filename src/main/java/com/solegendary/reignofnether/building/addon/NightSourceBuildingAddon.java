package com.solegendary.reignofnether.building.addon;

import com.solegendary.reignofnether.blocks.BlockClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.util.MiscUtil;

import java.util.Set;

import net.minecraft.core.BlockPos;

/**
 * Engine data-driven addon backing {@link NightSourceAddon} and {@link RangeIndicatorAddon}
 * (plan CONTENT_JSON_PLAN.md). A building enables it with
 * {@code { "type": "reignofnether:night_source", "params": { "range": 24 } }}.
 *
 * <p>Params: {@code range} (blocks; 0 disables the aura) and {@code showOnlyWhenSelected}
 * (default 1 - the border is only drawn while the building is selected).
 */
public class NightSourceBuildingAddon implements NightSourceAddon, RangeIndicatorAddon {

    private final int range;
    private final boolean showOnlyWhenSelected;

    public NightSourceBuildingAddon(AddonSpec spec) {
        this.range = (int) spec.param("range", 0);
        this.showOnlyWhenSelected = spec.param("showOnlyWhenSelected", 1) != 0;
    }

    @Override
    public int getNightRange(BuildingPlacement placement) {
        return placement.isBuilt ? range : 0;
    }

    @Override
    public int getDefaultNightRange() {
        return range;
    }

    @Override
    public int getRange(BuildingPlacement placement) {
        return getNightRange(placement);
    }

    @Override
    public boolean showOnlyWhenSelected(BuildingPlacement placement) {
        return showOnlyWhenSelected;
    }

    @Override
    public void updateHighlightBps(BuildingPlacement placement) {
        if (!placement.level.isClientSide() || getNightRange(placement) <= 0)
            return;
        Set<BlockPos> highlightBps = placement.getDataStorage().getData(RangeIndicatorAddon.HIGHLIGHT_BPS_CACHE);
        highlightBps.clear();
        highlightBps.addAll(MiscUtil.getRangeIndicatorCircleBlocks(
                placement.centrePos,
                getNightRange(placement) - BlockClientEvents.VISIBLE_BORDER_ADJ,
                placement.level,
                true
        ));
    }
}
