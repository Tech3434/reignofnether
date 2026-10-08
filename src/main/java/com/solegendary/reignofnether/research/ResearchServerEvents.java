package com.solegendary.reignofnether.research;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Sends a joining player their completed-research set so the HUD can unlock what it should, and sends
 * every research definition (on server start and on join) so the HUD can render the research panel.
 */
public class ResearchServerEvents {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (evt.getEntity() instanceof ServerPlayer sp && !sp.level().isClientSide()) {
            String playerName = sp.getName().getString();
            ResearchClientboundPacket.sync(playerName,
                    ResearchSaveData.getLoaded(sp.level()).getFor(playerName));
            ResearchDefinitionsClientboundPacket.sync(buildDefs());
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent evt) {
        ResearchDefinitionsClientboundPacket.sync(buildDefs());
    }

    private static List<ResearchClientEvents.Def> buildDefs() {
        List<ResearchClientEvents.Def> list = new ArrayList<>();
        for (Research research : ResearchRegistry.all())
            list.add(new ResearchClientEvents.Def(research.getId(), research.getNameKey(),
                    research.getIcon(), research.getPrerequisites()));
        return list;
    }
}
