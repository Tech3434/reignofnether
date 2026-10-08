package com.solegendary.reignofnether.faction;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.ArrayList;
import java.util.List;

/** Sends the faction list to clients (on server start and on join) so the selection menu can show it. */
public class FactionServerEvents {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent evt) {
        FactionClientboundPacket.sync(build(evt.getServer()));
        com.solegendary.reignofnether.building.buildings.JsonBuildingManager.reload(evt.getServer());
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (evt.getEntity() instanceof ServerPlayer sp && !sp.level().isClientSide())
            FactionClientboundPacket.sync(build(sp.getServer()));
    }

    private static List<FactionClientEvents.Info> build(MinecraftServer server) {
        List<FactionClientEvents.Info> list = new ArrayList<>();
        if (server == null)
            return list;
        for (var entry : FactionRegistries.get(server).entrySet())
            list.add(new FactionClientEvents.Info(
                    entry.getKey().location(), entry.getValue().nameKey(), entry.getValue().icon()));
        return list;
    }
}
