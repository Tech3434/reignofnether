package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/** Grants one cheat to the receiving client. */
public class CheatsClientboundPacket implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<CheatsClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "cheats_clientbound"));

    @Override
    public CustomPacketPayload.Type<CheatsClientboundPacket> type() {
        return TYPE;
    }

    private final String cheatName;

    public static void addCheat(Supplier<ServerPlayer> player, String cheatName) {
        PacketHandler.send(PacketHandler.toPlayer(player), new CheatsClientboundPacket(cheatName));
    }

    public CheatsClientboundPacket(String cheatName) {
        this.cheatName = cheatName;
    }

    public CheatsClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.cheatName = buffer.readUtf();
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(this.cheatName);
    }

    @Override
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> Cheats.addClientCheat(cheatName));
    }
}
