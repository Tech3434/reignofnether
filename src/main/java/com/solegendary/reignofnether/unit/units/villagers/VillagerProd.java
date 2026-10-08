package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.StopProductionButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.building.production.StartProductionButton;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.List;

public class VillagerProd extends ProductionItem {

    public final static String itemName = "Villager";
    public final static ResourceCost cost = ResourceCosts.VILLAGER;

    public VillagerProd() {
        super(cost);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide())
                placement.produceUnit((ServerLevel) level,
                        ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "villager_unit"),
                        placement.ownerName, true, new net.minecraft.core.Vec3i(0, 0, 0));
        };
    }

    public String getItemName() {
        return VillagerProd.itemName;
    }

    @Override
    public EntityType<? extends Mob> getEntityType() {
        return EntityType.VILLAGER;
    }

    @Override
    public ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/villager.png");
    }

    @Override
    public java.util.List<net.minecraft.util.FormattedCharSequence> getTooltipLines() {
        return java.util.List.of(
                FormattedCharSequence.forward(I18n.get("entity.reignofnether.villager_unit"), Style.EMPTY.withBold(true)),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedPopAndTime(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("entity.reignofnether.villager_unit.tooltip1"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("entity.reignofnether.villager_unit.tooltip2"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("entity.reignofnether.villager_unit.tooltip3"), Style.EMPTY)
        );
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartProductionButton(
            VillagerProd.itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/villager.png"),
            hotkey,
            () -> false,
            () -> true,
            List.of(
                FormattedCharSequence.forward(I18n.get("entity.reignofnether.villager_unit"), Style.EMPTY.withBold(true)),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedPopAndTime(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("entity.reignofnether.villager_unit.tooltip1"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("entity.reignofnether.villager_unit.tooltip2"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("entity.reignofnether.villager_unit.tooltip3"), Style.EMPTY)
            ),
            this
        );
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(
            VillagerProd.itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/villager.png"),
            prodBuilding,
            this,
            first
        );
    }
}
