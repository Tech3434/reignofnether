package com.solegendary.reignofnether.research;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Client-side mirror of every player's completed researches, filled by {@link ResearchClientboundPacket}
 * and read by the HUD to grey out locked abilities, production items and build buttons.
 */
public final class ResearchClientEvents {

    private static final Map<String, Set<ResourceLocation>> RESEARCHED = new HashMap<>();

    private ResearchClientEvents() { }

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

    public static void clear() {
        RESEARCHED.clear();
    }
}
