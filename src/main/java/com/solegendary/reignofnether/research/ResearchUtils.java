package com.solegendary.reignofnether.research;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;

import java.util.Collection;
import java.util.List;

/**
 * The universal research gate.
 *
 * <p>Any gate point (ability use, production, building placement, attribute boosts) asks here whether
 * a player satisfies a set of {@link ResearchCondition}s. Inversion is built into the condition, so
 * "blocked until researched" and "blocked while researched" are the same call.
 */
public final class ResearchUtils {

    private ResearchUtils() { }

    // ---- server side (authoritative) ----

    public static boolean isResearched(LevelAccessor level, String playerName, ResourceLocation id) {
        return ResearchSaveData.getLoaded(level).hasResearch(playerName, id);
    }

    public static boolean meets(LevelAccessor level, String playerName, List<ResearchCondition> conditions) {
        for (ResearchCondition condition : conditions)
            if (isResearched(level, playerName, condition.researchId()) == condition.invert())
                return false;
        return true;
    }

    // ---- client side (HUD mirror, filled by ResearchClientboundPacket) ----

    public static boolean isResearchedClient(String playerName, ResourceLocation id) {
        return ResearchClientEvents.has(playerName, id);
    }

    public static boolean meetsClient(String playerName, List<ResearchCondition> conditions) {
        for (ResearchCondition condition : conditions)
            if (isResearchedClient(playerName, condition.researchId()) == condition.invert())
                return false;
        return true;
    }

    public static Collection<ResourceLocation> getClient(String playerName) {
        return ResearchClientEvents.get(playerName);
    }
}
