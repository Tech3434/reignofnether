package com.solegendary.reignofnether.building.production;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.debug.RtsDebugClientEvents;
import com.solegendary.reignofnether.gamerules.GameruleClient;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.player.RTSPlayerScoresEnum;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;

// units and/or research tech that a ProductionBuilding can produce
public abstract class ProductionItem {

    public ResourceCost defaultCost;
    public BiConsumer<Level, ProductionPlacement> onComplete;
    public ProdDupeRule dupeRule;

    /** Researches required before this can be produced (empty = always available). */
    public List<com.solegendary.reignofnether.research.ResearchCondition> requiredResearch = List.of();

    /** Fluent setter for production items locked behind research. */
    public ProductionItem requireResearch(com.solegendary.reignofnether.research.ResearchCondition... conditions) {
        this.requiredResearch = List.of(conditions);
        return this;
    }

    public boolean meetsResearch(ProductionPlacement pp) {
        if (requiredResearch.isEmpty())
            return true;
        if (pp.getLevel().isClientSide())
            return com.solegendary.reignofnether.research.ResearchUtils.meetsClient(pp.ownerName, requiredResearch);
        return com.solegendary.reignofnether.research.ResearchUtils.meets(pp.getLevel(), pp.ownerName, requiredResearch);
    }

    public ProductionItem(ResourceCost cost, ProdDupeRule dupeRule, BiConsumer<Level, ProductionPlacement> onComplete) {
        this.defaultCost = cost;
        this.dupeRule = dupeRule;
        this.onComplete = onComplete;
    }

    public ProductionItem(ResourceCost cost, ProdDupeRule dupeRule) {
        this.defaultCost = cost;
        this.dupeRule = dupeRule;
    }

    public ProductionItem(ResourceCost cost) {
        this.defaultCost = cost;
        this.dupeRule = ProdDupeRule.ALLOW;
    }

    // is the player allowed to start this production item?
    public boolean canProduce(ProductionPlacement pp) {
        return meetsResearch(pp) && getProduceErrorMsg(pp) == null;
    }

    @Nullable
    public String getProduceErrorMsg(ProductionPlacement pp) {
        return null;
    }

    // allows for dynamic costs in subclasses
    public ResourceCost getCost(boolean isClientSide, String ownerName) {
        return defaultCost;
    }

    public abstract String getItemName();

    /**
     * Stable id used to identify this item across the network (it is echoed to the client queue and
     * back). Code items use their registry key; data-driven items override this with their own id
     * because they are not registered in the code registry.
     */
    public String getNetworkId() {
        ResourceLocation key = com.solegendary.reignofnether.api.ReignOfNetherRegistries.PRODUCTION_ITEM.getKey(this);
        return key != null ? key.toString() : getItemName();
    }

    /** Icon shown on the ability button (plan §14.3). Null falls back to an empty frame. */
    @Nullable
    public ResourceLocation getIcon() {
        return null;
    }

    /** Tooltip lines for the ability button; empty by default. */
    public List<FormattedCharSequence> getTooltipLines() {
        return List.of();
    }

    /** The entity this item summons, or null if it produces something else (eg. tech). */
    @Nullable
    public EntityType<? extends Mob> getEntityType() {
        return null;
    }

    public boolean canAfford(ProductionPlacement pp) {
        for (Resources resources : ResourcesServerEvents.resourcesList)
            if (resources.ownerName.equals(pp.ownerName))
                return (resources.food >= getCost(pp.getLevel().isClientSide(), pp.ownerName).food &&
                        resources.wood >= getCost(pp.getLevel().isClientSide(), pp.ownerName).wood &&
                        resources.ore >= getCost(pp.getLevel().isClientSide(), pp.ownerName).ore &&
                        canAffordPopulation(pp));
        return false;
    }

    public boolean canAffordPopulation(ProductionPlacement pp) {
        if (getCost(pp.getLevel().isClientSide(), pp.ownerName).population == 0)
            return true;

        int currentPop = UnitServerEvents.getCurrentPopulation(pp.ownerName);
        int popSupply = BuildingServerEvents.getTotalPopulationSupply(pp.ownerName);

        for (Resources resources : ResourcesServerEvents.resourcesList)
            if (resources.ownerName.equals(pp.ownerName))
                return (currentPop + getCost(pp.getLevel().isClientSide(), pp.ownerName).population) <= popSupply;
        return false;
    }

    // check we didn't dip below pop supply after starting production
    public boolean isBelowPopulationSupply(ProductionPlacement pp) {
        if (getCost(pp.getLevel().isClientSide(), pp.ownerName).population == 0)
            return true;

        int currentPop;
        int popSupply;
        if (pp.getLevel().isClientSide()) {
            currentPop = UnitClientEvents.getCurrentPopulation(pp.ownerName);
            popSupply = BuildingClientEvents.getTotalPopulationSupply(pp.ownerName);
        } else {
            currentPop = UnitServerEvents.getCurrentPopulation(pp.ownerName);
            popSupply = BuildingServerEvents.getTotalPopulationSupply(pp.ownerName);
        }
        return currentPop <= popSupply;
    }

    public boolean isBelowMaxPopulation(ProductionPlacement pp) {
        if (getCost(pp.getLevel().isClientSide(), pp.ownerName).population == 0)
            return true;

        int currentPop = UnitServerEvents.getCurrentPopulation(pp.ownerName);
        // the limit is the base (maxPopulation gamerule) plus what the owner's capitols grant
        int popLimit = pp.getLevel().isClientSide()
                ? GameruleClient.maxPopulation + BuildingClientEvents.getPopulationBonusFromCapitols(pp.ownerName)
                : UnitServerEvents.maxPopulation + UnitServerEvents.getPopulationBonusFromCapitols(pp.ownerName);
        return (currentPop + getCost(pp.getLevel().isClientSide(), pp.ownerName).population) <= popLimit;
    }

    // some items (eg. research) are enabled only if the item doesn't exist in any existing clientside queue
    public boolean itemIsBeingProduced(String ownerName) {
        return itemIsBeingProduced(true, ownerName);
    }

    public boolean itemIsBeingProduced(boolean isClientSide, String ownerName) {
        List<BuildingPlacement> buildings = isClientSide ? BuildingClientEvents.getBuildings() : BuildingServerEvents.getBuildings();

        for (BuildingPlacement building : buildings)
            if (building.ownerName.equals(ownerName) && building instanceof ProductionPlacement prodBuilding)
                for (ActiveProduction prodItem : prodBuilding.productionQueue)
                    if (prodItem.item == this)
                        return true;
        return false;
    }

    // check if this is being produced at one particular building
    public boolean itemIsBeingProducedAt(ProductionPlacement pp) {
        return itemIsBeingProducedAt(true, pp);
    }

    public boolean itemIsBeingProducedAt(boolean isClientSide, ProductionPlacement pp) {
        List<BuildingPlacement> buildings = isClientSide ? BuildingClientEvents.getBuildings() : BuildingServerEvents.getBuildings();

        for (BuildingPlacement building : buildings)
            if (building == pp)
                for (ActiveProduction prodItem : pp.productionQueue)
                    if (prodItem.item == this)
                        return true;
        return false;
    }

    // Button object to build
    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding keybinding) {
        return null;
    }
    // Button object to show in-progress items
    // firstItem means this button will cancel the currently-building item
    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(getItemName(), getIcon(), prodBuilding, this, first);
    }

    public void recordScore(ProductionPlacement placement) {
        if (!placement.getLevel().isClientSide()) {
            RTSPlayer rtsPlayer = PlayerServerEvents.getRTSPlayer(placement.ownerName);
            if (rtsPlayer != null) {
                rtsPlayer.scores.addToScore(RTSPlayerScoresEnum.TOTAL_UNITS_PRODUCED);
                if (this == ProductionItems.VILLAGER)
                    rtsPlayer.scores.addToScore(RTSPlayerScoresEnum.WORKER_UNITS_PRODUCED);
                else
                    rtsPlayer.scores.addToScore(RTSPlayerScoresEnum.MILITARY_UNITS_PRODUCED);
            }
        }
    }

    // return true if the tick finished
    public boolean tick(ProductionPlacement placement, ActiveProduction active) {
        if (active.ticksLeft > 0 && isBelowPopulationSupply(placement) && placement.isBuilt) {
            if (placement.getLevel().isClientSide())
                active.ticksLeft -= (RtsDebugClientEvents.getCappedTPS() / 20D);
            else
                active.ticksLeft -= 1;

            if (active.ticksLeft < 0)
                active.ticksLeft = 0;
        }
        if (!placement.level.isClientSide() && active.ticksLeft <= 0 && isBelowPopulationSupply(placement) && !active.completed) {
            active.complete(placement);
            return true;
        }
        return false;
    }

}
