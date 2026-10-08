package com.solegendary.reignofnether.research;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.util.DistHelper;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Sends one player's completed-research set to the clients (whole set, replaced on receipt). */
public class ResearchClientboundPacket implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<ResearchClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "research_clientbound"));

    @Override
    public CustomPacketPayload.Type<ResearchClientboundPacket> type() {
        return TYPE;
    }

    private final String playerName;
    private final List<ResourceLocation> researched;

    public static void sync(String playerName, java.util.Collection<ResourceLocation> researched) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new ResearchClientboundPacket(playerName, List.copyOf(researched)));
    }

    public ResearchClientboundPacket(String playerName, List<ResourceLocation> researched) {
        this.playerName = playerName;
        this.researched = researched;
    }

    public ResearchClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.playerName = buffer.readUtf();
        this.researched = buffer.readList(buf -> buf.readResourceLocation());
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(this.playerName);
        buffer.writeCollection(this.researched, (buf, rl) -> buf.writeResourceLocation(rl));
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ResearchClientEvents.set(this.playerName, this.researched)));
    }
}
