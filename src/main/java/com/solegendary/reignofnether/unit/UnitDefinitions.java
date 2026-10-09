package com.solegendary.reignofnether.unit;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

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

    /**
     * The definition with its {@code inherits} chain resolved: parent values fill in whatever the child
     * left unset (transitive, cycle-safe). Falls back to the raw definition on a missing/cyclic parent.
     */
    @Nullable
    public static UnitDefinition resolve(net.minecraft.core.RegistryAccess access, ResourceLocation id) {
        if (access == null || id == null)
            return null;
        return resolve(access.registryOrThrow(UNIT_KEY), id, new java.util.HashSet<>());
    }

    private static UnitDefinition resolve(Registry<UnitDefinition> registry, ResourceLocation id,
                                          java.util.Set<ResourceLocation> seen) {
        UnitDefinition def = registry.get(id);
        if (def == null || def.inherits().isEmpty())
            return def;
        if (!seen.add(id))
            return def;
        UnitDefinition base = resolve(registry, def.inherits().get(), seen);
        return base == null ? def : def.withInherited(base);
    }
}
