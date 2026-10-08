package com.solegendary.reignofnether.building.buildings;

import com.solegendary.reignofnether.building.BuildingDefinition;
import com.solegendary.reignofnether.building.BuildingDefinitions;
import com.solegendary.reignofnether.resources.ResourceCost;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime registry of data-driven buildings: one {@link JsonBuilding} per datapack
 * {@link BuildingDefinition}, rebuilt whenever the server's datapack registries are (re)loaded.
 *
 * <p>Uses its own map because the code {@code ReignOfNetherRegistries.BUILDING} registry is frozen by
 * the time datapacks load and cannot accept late entries.
 */
public final class JsonBuildingManager {

    private static final Map<ResourceLocation, JsonBuilding> BUILDINGS = new LinkedHashMap<>();

    private JsonBuildingManager() { }

    /** Rebuilds every JSON building from the server's datapack registry. */
    public static void reload(MinecraftServer server) {
        BUILDINGS.clear();
        if (server == null)
            return;
        for (Map.Entry<net.minecraft.resources.ResourceKey<BuildingDefinition>, BuildingDefinition> entry
                : BuildingDefinitions.get(server).entrySet()) {
            BuildingDefinition def = entry.getValue();
            ResourceCost cost = def.cost()
                    .map(c -> ResourceCost.Research(c.food(), c.wood(), c.ore(), c.seconds()))
                    .orElseGet(() -> ResourceCost.Research(0, 0, 0, 0));
            ResourceLocation id = entry.getKey().location();
            BUILDINGS.put(id, new JsonBuilding(id, def, cost));
        }
    }

    @Nullable
    public static JsonBuilding get(ResourceLocation id) {
        return BUILDINGS.get(id);
    }

    public static Collection<JsonBuilding> all() {
        return BUILDINGS.values();
    }
}
