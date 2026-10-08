package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * The datapack registry of {@link BuildingDefinition}s: {@code data/<namespace>/building/<name>.json}.
 * Registered (and synced to clients) in {@code ReignOfNether#loadDatapacks}.
 */
public final class BuildingDefinitions {

    public static final ResourceKey<Registry<BuildingDefinition>> BUILDING_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "building"));

    private BuildingDefinitions() { }

    public static Registry<BuildingDefinition> get(MinecraftServer server) {
        return server.registryAccess().registryOrThrow(BUILDING_KEY);
    }

    @Nullable
    public static BuildingDefinition get(MinecraftServer server, ResourceLocation id) {
        if (server == null || id == null)
            return null;
        return get(server).get(id);
    }
}
