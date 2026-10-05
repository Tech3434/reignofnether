package com.solegendary.reignofnether.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Holder for the mod's common (both-sides) configuration spec.
 *
 * <p>It used to declare 154 cost entries - {@code UnitCosts}, {@code BuildingCosts},
 * {@code ResearchCosts} and {@code AbilityCosts} - one per piece of content. With the default
 * factions gone those entries priced content that no longer exists, and every one of them had to be
 * edited by hand when anything was added or removed.
 *
 * <p>Cost now lives on the {@code ProductionItem} itself rather than in a per-content config table,
 * so there is nothing left to declare here. Behavioural toggles, when they are needed, belong in
 * their own config class next to the feature they control - not in this file.
 */
public class ReignOfNetherCommonConfigs {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec SPEC;

    static {
        SPEC = BUILDER.build();
    }

    /**
     * Kept so the registration call in the mod constructor still has something to register. Mods
     * without any common config should drop the registration entirely rather than ship an empty file.
     */
    public static Pair<?, ?>[] emptyForRegistration() {
        return new Pair<?, ?>[0];
    }
}
