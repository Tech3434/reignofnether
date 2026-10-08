package com.solegendary.reignofnether.research;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lookup of every known {@link Research} by id.
 *
 * <p>Phase 1 fills this from code; phase 4 will additionally feed it from datapack JSON. Insertion
 * order is kept so a HUD tree can list researches in registration order.
 */
public final class ResearchRegistry {

    private static final Map<ResourceLocation, Research> RESEARCH = new LinkedHashMap<>();

    private ResearchRegistry() { }

    public static Research register(Research research) {
        RESEARCH.put(research.getId(), research);
        return research;
    }

    public static Research get(ResourceLocation id) {
        return RESEARCH.get(id);
    }

    public static boolean exists(ResourceLocation id) {
        return RESEARCH.containsKey(id);
    }

    public static Collection<Research> all() {
        return RESEARCH.values();
    }

    /** Clears the registry; the datapack reload uses this so JSON definitions can replace code ones. */
    public static void clear() {
        RESEARCH.clear();
    }
}
