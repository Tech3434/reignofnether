package com.solegendary.reignofnether.faction;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.util.DistHelper;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** Syncs the faction list to clients and opens/closes the faction-selection menu. */
public class FactionClientboundPacket implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<FactionClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "faction_clientbound"));

    @Override
    public CustomPacketPayload.Type<FactionClientboundPacket> type() {
        return TYPE;
    }

    private static final byte ACTION_SYNC = 0;
    private static final byte ACTION_OPEN = 1;
    private static final byte ACTION_CLOSE = 2;

    private byte action;
    private List<FactionClientEvents.Info> factions = List.of();
    private String forcedFaction = "";

    public static void sync(List<FactionClientEvents.Info> factions) {
        FactionClientboundPacket p = new FactionClientboundPacket();
        p.action = ACTION_SYNC;
        p.factions = factions;
        PacketHandler.send(PacketHandler.allPlayers(), p);
    }

    public static void open(ServerPlayer player, ResourceLocation forcedFaction) {
        FactionClientboundPacket p = new FactionClientboundPacket();
        p.action = ACTION_OPEN;
        p.forcedFaction = forcedFaction == null ? "" : forcedFaction.toString();
        PacketHandler.send(PacketHandler.toPlayer(() -> player), p);
    }

    public static void close(ServerPlayer player) {
        FactionClientboundPacket p = new FactionClientboundPacket();
        p.action = ACTION_CLOSE;
        PacketHandler.send(PacketHandler.toPlayer(() -> player), p);
    }

    public FactionClientboundPacket() { }

    public FactionClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.action = buffer.readByte();
        int size = buffer.readVarInt();
        List<FactionClientEvents.Info> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buffer.readResourceLocation();
            String name = buffer.readUtf();
            ResourceLocation icon = buffer.readResourceLocation();
            list.add(new FactionClientEvents.Info(id, name, icon));
        }
        this.factions = list;
        this.forcedFaction = buffer.readUtf();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeByte(this.action);
        buffer.writeVarInt(this.factions.size());
        for (FactionClientEvents.Info info : this.factions) {
            buffer.writeResourceLocation(info.id());
            buffer.writeUtf(info.nameKey());
            buffer.writeResourceLocation(info.icon());
        }
        buffer.writeUtf(this.forcedFaction == null ? "" : this.forcedFaction);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> DistHelper.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            switch (action) {
                case ACTION_SYNC -> FactionClientEvents.setFactions(this.factions);
                case ACTION_OPEN -> FactionClientEvents.open(
                        this.forcedFaction == null || this.forcedFaction.isEmpty()
                                ? null : ResourceLocation.tryParse(this.forcedFaction));
                case ACTION_CLOSE -> FactionClientEvents.close();
            }
        }));
    }
}
