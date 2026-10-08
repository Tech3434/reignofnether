package com.solegendary.reignofnether.building.buildings;

import com.solegendary.reignofnether.building.BuildingClientboundPacket;
import com.solegendary.reignofnether.building.UpgradeSpec;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProdDupeRule;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;

import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A data-driven building upgrade (plan CONTENT_JSON_PLAN.md): it is a {@link ProductionItem}, so it is
 * started from the selected building's UI and shares its production queue. Completing it raises the
 * placement's upgrade level to {@code targetLevel}, swaps the structure (if the spec defines one) and
 * syncs the new level to clients. Only the next upgrade in the chain can be bought.
 */
public class JsonUpgradeProductionItem extends ProductionItem {

    private final int targetLevel;
    private final String displayName;
    @Nullable private final ResourceLocation icon;
    @Nullable private final ResourceLocation structure;

    public JsonUpgradeProductionItem(int targetLevel, UpgradeSpec spec, ResourceLocation fallbackIcon) {
        super(spec.cost()
                .map(c -> ResourceCost.Research(c.food(), c.wood(), c.ore(), c.seconds()))
                .orElseGet(() -> ResourceCost.Research(0, 0, 0, 0)));
        this.targetLevel = targetLevel;
        this.displayName = spec.displayName().orElse("Upgrade " + targetLevel);
        this.icon = spec.icon().orElse(fallbackIcon);
        this.structure = spec.structure().orElse(null);
        this.dupeRule = ProdDupeRule.DISALLOW_FOR_BUILDING;
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (level.isClientSide())
                return;
            placement.setUpgradeLevel(targetLevel);
            if (structure != null)
                placement.changeStructure(structure.getPath());
            BuildingClientboundPacket.setUpgradeLevel(placement.originPos, targetLevel);
        };
    }

    public int getTargetLevel() {
        return targetLevel;
    }

    @Override
    public String getItemName() {
        return displayName;
    }

    @Override
    public String getNetworkId() {
        return "upgrade:" + targetLevel;
    }

    @Override
    @Nullable
    public ResourceLocation getIcon() {
        return icon;
    }

    @Override
    public String getProduceErrorMsg(ProductionPlacement pp) {
        if (pp.getUpgradeLevel() != targetLevel - 1)
            return "hud.upgrade.reignofnether.not_next";
        return null;
    }

    @Override
    public List<FormattedCharSequence> getTooltipLines() {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(FormattedCharSequence.forward(displayName, Style.EMPTY.withBold(true)));
        if (defaultCost.food > 0 || defaultCost.wood > 0 || defaultCost.ore > 0)
            lines.add(ResourceCosts.getFormattedCost(defaultCost));
        lines.add(ResourceCosts.getFormattedTime(defaultCost));
        return lines;
    }
}
