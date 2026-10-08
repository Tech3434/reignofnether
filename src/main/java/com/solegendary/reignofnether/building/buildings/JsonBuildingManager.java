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

    /**
     * Ensures one {@link JsonBuilding} exists per datapack definition. Existing instances are kept:
     * a building placement holds its Building instance, and {@code BuildingSaveData.load} may create it
     * first, so this is safe whether it runs before or after world building load.
     */
    public static void reload(MinecraftServer server) {
        if (server == null)
            return;
        for (Map.Entry<net.minecraft.resources.ResourceKey<BuildingDefinition>, BuildingDefinition> entry
                : BuildingDefinitions.get(server).entrySet()) {
            ResourceLocation id = entry.getKey().location();
            if (!BUILDINGS.containsKey(id))
                BUILDINGS.put(id, create(id, entry.getValue()));
        }
    }

    private static JsonBuilding create(ResourceLocation id, BuildingDefinition def) {
        ResourceCost cost = def.cost()
                .map(c -> ResourceCost.Research(c.food(), c.wood(), c.ore(), c.seconds()))
                .orElseGet(() -> ResourceCost.Research(0, 0, 0, 0));
        return new JsonBuilding(id, def, cost);
    }

    /** Existing building, or one built from {@code level}'s (synced) datapack registry on a client. */
    @Nullable
    public static JsonBuilding getOrCreate(@Nullable net.minecraft.world.level.LevelAccessor level, ResourceLocation id) {
        JsonBuilding existing = BUILDINGS.get(id);
        if (existing != null)
            return existing;
        if (level == null)
            return null;
        BuildingDefinition def = level.registryAccess().registryOrThrow(BuildingDefinitions.BUILDING_KEY).get(id);
        if (def == null)
            return null;
        JsonBuilding created = create(id, def);
        BUILDINGS.put(id, created);
        return created;
    }

    @Nullable
    public static JsonBuilding get(ResourceLocation id) {
        return BUILDINGS.get(id);
    }

    public static Collection<JsonBuilding> all() {
        return BUILDINGS.values();
    }
}
