package com.solegendary.reignofnether.gamemode;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;

/**
 * Locks every connected client to the running game mode as soon as the match starts.
 *
 * <p>Survival and Scenario are gone, so the mode is always {@link GameMode#CLASSIC}; the class is kept
 * because the lock itself is still what stops a client from walking off into a different mode once a
 * match is running.
 */
public class GameModeServerEvents {

    private static GameMode getGameMode(ServerLevel level) {
        return GameMode.CLASSIC;
    }

    private static boolean isGameModeLocked() {
        return !PlayerServerEvents.rtsPlayers.isEmpty();
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (isGameModeLocked() && !evt.getEntity().level().isClientSide())
            GameModeClientboundPacket.setAndLockAllClientGameModes(getGameMode((ServerLevel) evt.getEntity().level()));
    }
}
