package com.solegendary.reignofnether.research;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Sends a joining player their completed-research set so the HUD can unlock what it should. */
public class ResearchServerEvents {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (evt.getEntity() instanceof ServerPlayer sp && !sp.level().isClientSide()) {
            String playerName = sp.getName().getString();
            ResearchClientboundPacket.sync(playerName,
                    ResearchSaveData.getLoaded(sp.level()).getFor(playerName));
        }
    }
}
