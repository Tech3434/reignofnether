package com.solegendary.reignofnether.gamerules;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class GameruleClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<GameruleClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "gamerule_clientbound"));

    @Override
    public CustomPacketPayload.Type<GameruleClientboundPacket> type() {
        return TYPE;
    }

    GameruleAction action;
    String playerName;
    Long value;

    public static void setLogFalling(boolean logFalling) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_LOG_FALLING, "", logFalling ? 1L : 0L));
    }
    public static void setNeutralAggro(boolean neutralAggro) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_NEUTRAL_AGGRO, "", neutralAggro ? 1L : 0L));
    }
    public static void setMaxPopulation(long maxPopulation) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_MAX_POPULATION, "", maxPopulation));
    }
    public static void setSlantedBuilding(boolean slantedBuilding) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_SLANTED_BUILDING, "", slantedBuilding ? 1L : 0L));
    }
    public static void setLockAlliances(boolean lockAlliances) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_LOCK_ALLIANCES, "", lockAlliances ? 1L : 0L));
    }
    public static void setBuildingsOutsideBorder(boolean buildingsOutsideBorder) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_BUILDINGS_OUTSIDE_BORDER, "", buildingsOutsideBorder ? 1L : 0L));
    }
    public static void setRtsPathfinding(boolean rtsPathfinding) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_RTS_PATHFINDING, "", rtsPathfinding ? 1L : 0L));
    }
    public static void setAnimalSpawnYDiff(long yDiff) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameruleClientboundPacket(GameruleAction.SET_ANIMAL_SPAWN_Y_DIFF, "", yDiff));
    }

    public GameruleClientboundPacket(GameruleAction action, String playerName, Long value) {
        this.action = action;
        this.playerName = playerName;
        this.value = value;
    }

    public GameruleClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.action = buffer.readEnum(GameruleAction.class);
        this.playerName = buffer.readUtf();
        this.value = buffer.readLong();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeUtf(this.playerName);
        buffer.writeLong(this.value);
    }

    // client-side packet-consuming functions
    public void handle(IPayloadContext ctx) {

        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        switch (action) {
                            case SET_LOG_FALLING -> GameruleClient.doLogFalling = value == 1L;
                            case SET_NEUTRAL_AGGRO -> GameruleClient.neutralAggro = value == 1L;
                            case SET_MAX_POPULATION -> GameruleClient.maxPopulation = Math.toIntExact(value);
                            case SET_SLANTED_BUILDING -> GameruleClient.slantedBuilding = value == 1L;
                            case SET_LOCK_ALLIANCES -> GameruleClient.lockAlliances = value == 1L;
                            case SET_BUILDINGS_OUTSIDE_BORDER -> GameruleClient.buildingsOutsideBorder = value == 1L;
                            case SET_RTS_PATHFINDING -> GameruleClient.rtsPathfinding = value == 1L;
                            case SET_ANIMAL_SPAWN_Y_DIFF -> GameruleClient.animalSpawnYDiff = Math.toIntExact(value);
                        }
                    });
        });
        return;
    }
}
