package com.solegendary.reignofnether.resources;

import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.util.FormattedCharSequence;

/**
 * Cost values for the template content, plus the formatting helpers the HUD uses to draw them.
 *
 * <p>It used to hold one constant per piece of content - over 150 of them - each backed by an entry
 * in {@code ReignOfNetherCommonConfigs}. With the default factions gone there is nothing left to
 * configure: every remaining cost belongs to a template and belongs to the template's own class.
 * New factions define their costs where they define their units and buildings.
 */
public class ResourceCosts {
    public static final int REPLANT_WOOD_COST = 1;
    public static final int REDUCED_REPLANT_WOOD_COST = 0;

    /**
     * Hard cap on army size, before any capitol bonus. Deliberately 1: the base army is one unit, and
     * growing past that is a game-design choice a new faction makes by giving itself a capitol that
     * adds supply - see {@code UnitServerEvents#getPopulationBonusFromCapitols}.
     */
    public static final int DEFAULT_MAX_POPULATION = 1;

    // units: food, wood, ore, seconds, population
    public static final ResourceCost VILLAGER = ResourceCost.Unit(50, 0, 0, 15, 0);
    public static final ResourceCost VINDICATOR = ResourceCost.Unit(160, 0, 0, 32, 0);

    // buildings: food, wood, ore, supply
    public static final ResourceCost TOWN_CENTRE = ResourceCost.Building(0, 350, 250, 0);
    public static final ResourceCost BARRACKS = ResourceCost.Building(0, 150, 0, 0);

    public static FormattedCharSequence getFormattedCost(ResourceCost resCost) {
        String str = "";
        if (resCost.food > 0)
            str += "\uE000  " + resCost.food + "     ";
        if (resCost.wood > 0)
            str += "\uE001  " + resCost.wood + "     ";
        if (resCost.ore > 0)
            str += "\uE002  " + resCost.ore + "     ";
        if (resCost.emerald > 0)
            str += "\uE010  " + resCost.emerald + "     ";

        if (str.isEmpty())
            str += "\uE000  0     ";
        str = str.trim();
        return FormattedCharSequence.forward(str, MyRenderer.iconStyle);
    }

    public static FormattedCharSequence getFormattedPopAndTime(ResourceCost resCost) {
        return FormattedCharSequence.forward("\uE003  " + resCost.population + "     \uE004  " + resCost.ticks / ResourceCost.TICKS_PER_SECOND + "s", MyRenderer.iconStyle);
    }

    public static FormattedCharSequence getFormattedPop(ResourceCost resCost) {
        return FormattedCharSequence.forward("\uE003  " + resCost.population, MyRenderer.iconStyle);
    }

    public static FormattedCharSequence getFormattedTime(ResourceCost resCost) {
        return FormattedCharSequence.forward("\uE004  " + resCost.ticks / ResourceCost.TICKS_PER_SECOND + "s", MyRenderer.iconStyle);
    }
}
