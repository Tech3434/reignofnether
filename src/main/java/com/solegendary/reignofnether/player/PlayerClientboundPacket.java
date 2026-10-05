package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.registrars.PacketHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class PlayerClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<PlayerClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "player_clientbound"));

    @Override
    public CustomPacketPayload.Type<PlayerClientboundPacket> type() {
        return TYPE;
    }

    PlayerAction playerAction;
    String playerName;
    Long value1;
    int value2;
    BlockPos pos;

    public static void addRTSPlayer(String playerName, Long id, int startPosColorId) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.ADD_RTS_PLAYER, playerName, id, startPosColorId));
    }

    public static void removeRTSPlayer(String playerName) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.REMOVE_RTS_PLAYER, playerName, 0L));
    }

    public static void defeat(String playerName) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.DEFEAT, playerName, 0L));
    }

    public static void victory(String playerName) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.VICTORY, playerName, 0L));
    }

    public static void resetRTS(boolean hard) {
        if (hard) {
            PacketHandler.send(PacketHandler.allPlayers(),
                    new PlayerClientboundPacket(PlayerAction.RESET_RTS_HARD, "", 0L));
        } else {
            PacketHandler.send(PacketHandler.allPlayers(),
                    new PlayerClientboundPacket(PlayerAction.RESET_RTS, "", 0L));
        }
    }

    public static void syncRtsGameTime(Long rtsGameTicks) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.SYNC_RTS_GAME_TIME, "", rtsGameTicks));
    }

    public static void lockRTS(String playerName) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.LOCK_RTS, playerName, 0L));
    }

    public static void unlockRTS(String playerName) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.UNLOCK_RTS, playerName, 0L));
    }

    // prevent one particular player from joining the match
    public static void disableStartRTS(String playerName) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.DISABLE_START_RTS, playerName, 0L));
    }
    public static void enableStartRTS(String playerName) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.ENABLE_START_RTS, playerName, 0L));
    }

    public static void setRTSCamera(String playerName, boolean value) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.SET_RTS_CAMERA, playerName, (long) (value ? 1 : 0)));
    }

    public static void teleport(String playerName, BlockPos pos) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new PlayerClientboundPacket(PlayerAction.TELEPORT, playerName, pos));
    }

    public PlayerClientboundPacket(PlayerAction playerAction, String playerName, BlockPos pos) {
        this.playerAction = playerAction;
        this.playerName = playerName;
        this.value1 = 0L;
        this.value2 = 0;
        this.pos = pos;
    }

    public PlayerClientboundPacket(PlayerAction playerAction, String playerName, Long value1, int value2) {
        this.playerAction = playerAction;
        this.playerName = playerName;
        this.value1 = value1;
        this.value2 = value2;
        this.pos = new BlockPos(0, 0, 0);
    }

    public PlayerClientboundPacket(PlayerAction playerAction, String playerName, Long value1) {
        this.playerAction = playerAction;
        this.playerName = playerName;
        this.value1 = value1;
        this.value2 = 0;
        this.pos = new BlockPos(0, 0, 0);
    }

    public PlayerClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.playerAction = buffer.readEnum(PlayerAction.class);
        this.playerName = buffer.readUtf();
        this.value1 = buffer.readLong();
        this.value2 = buffer.readInt();
        this.pos = buffer.readBlockPos();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.playerAction);
        buffer.writeUtf(this.playerName);
        buffer.writeLong(this.value1);
        buffer.writeInt(this.value2);
        buffer.writeBlockPos(this.pos);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {

        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        switch (playerAction) {
                            case TELEPORT -> com.solegendary.reignofnether.orthoview.OrthoviewClientEvents.centreCameraOnPosForPlayer(playerName, pos);
                            case DEFEAT -> PlayerClientEvents.defeat(playerName);
                            case VICTORY -> PlayerClientEvents.victory(playerName);
                            case ADD_RTS_PLAYER -> PlayerClientEvents.addRTSPlayer(playerName, value1, value2);
                            case REMOVE_RTS_PLAYER -> PlayerClientEvents.removeRTSPlayer(playerName);
                            case RESET_RTS -> PlayerClientEvents.resetRTS(false);
                            case RESET_RTS_HARD -> PlayerClientEvents.resetRTS(true);
                            case SYNC_RTS_GAME_TIME -> PlayerClientEvents.syncRtsGameTime(value1);
                            case LOCK_RTS -> PlayerClientEvents.setRTSLock(true);
                            case UNLOCK_RTS -> PlayerClientEvents.setRTSLock(false);
                            case ENABLE_START_RTS -> PlayerClientEvents.setCanStartRTS(true);
                            case DISABLE_START_RTS -> PlayerClientEvents.setCanStartRTS(false);
                            case SET_RTS_CAMERA -> com.solegendary.reignofnether.orthoview.OrthoviewClientEvents.tryToSetCamera(playerName, value1 == 1L);
                        }
                    });
        });
        return;
    }
}
