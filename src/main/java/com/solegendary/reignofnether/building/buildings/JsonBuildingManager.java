package com.solegendary.reignofnether.building.buildings;

import com.solegendary.reignofnether.building.BuildingDefinition;
import com.solegendary.reignofnether.building.BuildingDefinitions;
import com.solegendary.reignofnether.building.UpgradeSpec;
import com.solegendary.reignofnether.resources.ResourceCost;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime registry of data-driven buildings: for each datapack {@link BuildingDefinition} it builds one
 * {@link JsonBuilding} per upgrade level (level 0 = base, level n = base with upgrades 1..n applied).
 * Rebuilt whenever the server's datapack registries are (re)loaded.
 *
 * <p>Uses its own maps because the code {@code ReignOfNetherRegistries.BUILDING} registry is frozen by
 * the time datapacks load and cannot accept late entries.
 */
public final class JsonBuildingManager {

    private static final Map<ResourceLocation, List<JsonBuilding>> LEVELS = new LinkedHashMap<>();

    private JsonBuildingManager() { }

    /**
     * Ensures the level variants exist for every datapack definition. Existing instances are kept:
     * a building placement holds its Building instance, and {@code BuildingSaveData.load} may create it
     * first, so this is safe whether it runs before or after world building load.
     */
    public static void reload(MinecraftServer server) {
        if (server == null)
            return;
        for (Map.Entry<net.minecraft.resources.ResourceKey<BuildingDefinition>, BuildingDefinition> entry
                : BuildingDefinitions.get(server).entrySet()) {
            ResourceLocation id = entry.getKey().location();
            if (!LEVELS.containsKey(id))
                LEVELS.put(id, createVariants(id, entry.getValue()));
        }
    }

    private static List<JsonBuilding> createVariants(ResourceLocation id, BuildingDefinition base) {
        List<JsonBuilding> variants = new ArrayList<>();
        ResourceCost cost = costOf(base);
        BuildingDefinition def = base;
        variants.add(new JsonBuilding(id, def, cost, 0));
        int level = 1;
        for (UpgradeSpec upgrade : base.upgrades()) {
            def = def.withUpgrade(upgrade);
            variants.add(new JsonBuilding(id, def, cost, level++));
        }
        return variants;
    }

    private static ResourceCost costOf(BuildingDefinition def) {
        return def.cost()
                .map(c -> ResourceCost.Research(c.food(), c.wood(), c.ore(), c.seconds()))
                .orElseGet(() -> ResourceCost.Research(0, 0, 0, 0));
    }

    /** Level-0 building, or one built from {@code level}'s (synced) datapack registry on a client. */
    @Nullable
    public static JsonBuilding getOrCreate(@Nullable net.minecraft.world.level.LevelAccessor level, ResourceLocation id) {
        return getOrCreateLevel(level, id, 0);
    }

    /** The building at {@code upgradeLevel} (clamped), creating the variants from the registry if needed. */
    @Nullable
    public static JsonBuilding getOrCreateLevel(@Nullable net.minecraft.world.level.LevelAccessor level,
                                                ResourceLocation id, int upgradeLevel) {
        List<JsonBuilding> variants = LEVELS.get(id);
        if (variants == null) {
            if (level == null)
                return null;
            BuildingDefinition def = level.registryAccess().registryOrThrow(BuildingDefinitions.BUILDING_KEY).get(id);
            if (def == null)
                return null;
            variants = createVariants(id, def);
            LEVELS.put(id, variants);
        }
        int clamped = Math.max(0, Math.min(upgradeLevel, variants.size() - 1));
        return variants.get(clamped);
    }

    /** The level-0 building, or null if unknown. */
    @Nullable
    public static JsonBuilding get(ResourceLocation id) {
        List<JsonBuilding> variants = LEVELS.get(id);
        return variants == null || variants.isEmpty() ? null : variants.get(0);
    }

    /** The building at {@code upgradeLevel} (clamped), or null if unknown. */
    @Nullable
    public static JsonBuilding getLevel(ResourceLocation id, int upgradeLevel) {
        List<JsonBuilding> variants = LEVELS.get(id);
        if (variants == null || variants.isEmpty())
            return null;
        int clamped = Math.max(0, Math.min(upgradeLevel, variants.size() - 1));
        return variants.get(clamped);
    }

    /** Level-0 variants of every known building (for the worker build menu). */
    public static Collection<JsonBuilding> all() {
        List<JsonBuilding> result = new ArrayList<>();
        for (List<JsonBuilding> variants : LEVELS.values())
            if (!variants.isEmpty())
                result.add(variants.get(0));
        return result;
    }
}
