package com.solegendary.reignofnether.unit;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/**
 * The datapack registry of {@link UnitDefinition}s: {@code data/<namespace>/unit/<name>.json}
 * (id = {@code <namespace>:<name>}). Registered (and synced to clients) in
 * {@code ReignOfNether#loadDatapacks}.
 */
public final class UnitDefinitions {

    public static final ResourceKey<Registry<UnitDefinition>> UNIT_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "unit"));

    private UnitDefinitions() { }

    public static Registry<UnitDefinition> get(MinecraftServer server) {
        return server.registryAccess().registryOrThrow(UNIT_KEY);
    }

    public static UnitDefinition get(MinecraftServer server, ResourceLocation id) {
        if (server == null || id == null)
            return null;
        return get(server).get(id);
    }
}
