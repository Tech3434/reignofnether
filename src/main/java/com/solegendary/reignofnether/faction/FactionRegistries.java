package com.solegendary.reignofnether.faction;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/**
 * The datapack registry of {@link Faction}s: {@code data/<namespace>/faction/<name>.json}. Registered
 * as a datapack registry (synced to clients) in {@code ReignOfNether#loadDatapacks}.
 */
public final class FactionRegistries {

    public static final ResourceKey<Registry<Faction>> FACTION_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "faction"));

    /** Default faction used when a start does not name one. */
    public static final ResourceLocation DEFAULT_FACTION =
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "villagers");

    private FactionRegistries() { }

    public static Registry<Faction> get(MinecraftServer server) {
        return server.registryAccess().registryOrThrow(FACTION_KEY);
    }

    public static Faction get(MinecraftServer server, ResourceLocation id) {
        if (server == null || id == null)
            return null;
        return get(server).get(id);
    }
}
