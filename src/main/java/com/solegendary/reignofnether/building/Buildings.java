package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;

import com.solegendary.reignofnether.building.buildings.villagers.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

public class Buildings {
    public static final TownCentre TOWN_CENTRE = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "town_centre"), new TownCentre());
    public static final Barracks BARRACKS = register(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "barracks"), new Barracks());

    private static <T extends Building> T register(ResourceLocation id, T building) {
        return Registry.register(ReignOfNetherRegistries.BUILDING, id, building);
    }

    public static void init() {}
}
