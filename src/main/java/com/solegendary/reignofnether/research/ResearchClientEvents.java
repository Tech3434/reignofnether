package com.solegendary.reignofnether.research;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Client-side mirror of every player's completed researches, filled by {@link ResearchClientboundPacket}
 * and read by the HUD to grey out locked abilities, production items and build buttons.
 *
 * <p>Also mirrors the research <b>definitions</b> (name/icon/prerequisites) sent by
 * {@link ResearchDefinitionsClientboundPacket}, so the HUD can render a research panel.
 */
public final class ResearchClientEvents {

    private static final Map<String, Set<ResourceLocation>> RESEARCHED = new HashMap<>();
    private static final Map<ResourceLocation, Def> DEFINITIONS = new LinkedHashMap<>();

    private ResearchClientEvents() { }

    /** A research definition as shown in the HUD. */
    public record Def(ResourceLocation id, String nameKey, ResourceLocation icon,
                      List<ResearchCondition> prerequisites) { }

    public static void set(String playerName, Collection<ResourceLocation> ids) {
        RESEARCHED.put(playerName, Set.copyOf(ids));
    }

    public static boolean has(String playerName, ResourceLocation id) {
        Set<ResourceLocation> set = RESEARCHED.get(playerName);
        return set != null && set.contains(id);
    }

    public static Set<ResourceLocation> get(String playerName) {
        return RESEARCHED.getOrDefault(playerName, Set.of());
    }

    public static void setDefinitions(List<Def> defs) {
        DEFINITIONS.clear();
        for (Def def : defs)
            DEFINITIONS.put(def.id(), def);
    }

    public static Collection<Def> definitions() {
        return DEFINITIONS.values();
    }

    public static Def definition(ResourceLocation id) {
        return DEFINITIONS.get(id);
    }

    public static void clear() {
        RESEARCHED.clear();
        DEFINITIONS.clear();
    }
}
