package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.gamemode.ClientGameModeHelper;
import com.solegendary.reignofnether.gamemode.GameMode;
import com.solegendary.reignofnether.gamemode.GameModeServerboundPacket;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;

import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class PlayerServerboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<PlayerServerboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "player_serverbound"));

    @Override
    public CustomPacketPayload.Type<PlayerServerboundPacket> type() {
        return TYPE;
    }
    PlayerAction action;
    public int playerId;
    public double x;
    public double y;
    public double z;

    public static void teleportPlayer(Double x, Double y, Double z) {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null) {
            PacketHandler.sendToServer(new PlayerServerboundPacket(PlayerAction.TELEPORT,
                MC.player.getId(),
                x, y, z
            ));
        }
    }

    public static void enableOrthoview() {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null) {
            PacketHandler.sendToServer(new PlayerServerboundPacket(PlayerAction.ENABLE_ORTHOVIEW,
                MC.player.getId(),
                0d, 0d, 0d
            ));
        }
    }

    public static void disableOrthoview() {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null) {
            PacketHandler.sendToServer(new PlayerServerboundPacket(PlayerAction.DISABLE_ORTHOVIEW,
                MC.player.getId(),
                0d, 0d, 0d
            ));
        }
    }

    public static void startRTS(Double x, Double y, Double z) {
        Minecraft MC = Minecraft.getInstance();

        if (MC.player != null && MC.level != null) {
            BlockState bs = MC.level.getBlockState(new BlockPos(x.intValue(), y.intValue(), z.intValue()));
            if (!bs.getFluidState().isEmpty()) {
                HudClientEvents.showTemporaryMessage(I18n.get("hud.reignofnether.invalid_start_location"));
                return;
            }
            PacketHandler.sendToServer(new PlayerServerboundPacket(PlayerAction.START_RTS, MC.player.getId(), x, y, z));
            GameModeServerboundPacket.setAndLockAllClientGameModes(ClientGameModeHelper.gameMode);
            CompletableFuture.delayedExecutor(2000, TimeUnit.MILLISECONDS).execute(() -> {
                MC.player.sendSystemMessage(Component.literal(""));
                MC.player.sendSystemMessage(Component.translatable("hud.gamemode.reignofnether.classic1")
                        .withStyle(Style.EMPTY.withBold(true)));
                MC.player.sendSystemMessage(Component.literal("--------"));
                MC.player.sendSystemMessage(Component.translatable("hud.gamemode.reignofnether.classic2"));
                MC.player.sendSystemMessage(Component.translatable("hud.gamemode.reignofnether.classic3"));
                MC.player.sendSystemMessage(Component.literal(""));
            });
        }
    }

    public static void surrender() {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null) {
            PacketHandler.sendToServer(new PlayerServerboundPacket(
                PlayerAction.DEFEAT,
                MC.player.getId(),
                0d, 0d, 0d
            ));
        }
    }

    public static void enableRTSSyncing() {
        PacketHandler.sendToServer(new PlayerServerboundPacket(
            PlayerAction.ENABLE_RTS_SYNCING,
            -1, 0d, 0d, 0d
        ));
    }

    public static void disableRTSSyncing() {
        PacketHandler.sendToServer(new PlayerServerboundPacket(
            PlayerAction.DISABLE_RTS_SYNCING,
            -1, 0d, 0d, 0d
        ));
    }

    public static void resetRTS() {
        PacketHandler.sendToServer(new PlayerServerboundPacket(PlayerAction.RESET_RTS, -1, 0d, 0d, 0d));
    }

    // packet-handler functions
    public PlayerServerboundPacket(PlayerAction action, int playerId, Double x, Double y, Double z) {
        this.action = action;
        this.playerId = playerId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public PlayerServerboundPacket(PlayerAction action, int playerId) {
        this(action, playerId, 0d, 0d, 0d);
    }

    public PlayerServerboundPacket(RegistryFriendlyByteBuf buffer) {
        this.action = buffer.readEnum(PlayerAction.class);
        this.playerId = buffer.readInt();
        this.x = buffer.readDouble();
        this.y = buffer.readDouble();
        this.z = buffer.readDouble();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeInt(this.playerId);
        buffer.writeDouble(this.x);
        buffer.writeDouble(this.y);
        buffer.writeDouble(this.z);
    }

    private static final List<PlayerAction> opOnlyActions = List.of(
            PlayerAction.RESET_RTS,
            PlayerAction.RESET_RTS_HARD
    );

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {

            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) {
                ReignOfNether.LOGGER.warn("PlayerServerboundPacket: Sender was null");
                return;
            } else if (playerId != -1 && player.getId() != playerId) {
                ReignOfNether.LOGGER.warn("PlayerServerboundPacket: Tried to process packet from " + player.getName() + " for id: " + this.playerId);
                return;
            } else if (opOnlyActions.contains(action) && !player.hasPermissions(4)) {
                ReignOfNether.LOGGER.warn("PlayerServerboundPacket: Non-op player " + player.getName() + " tried to run action: " + this.action.name());
                return;
            }

            ReignOfNether.LOGGER.info("[Player] {} performed {} (pos: [{}, {}, {}])", player.getName(), action, this.x, this.y, this.z);

            switch (action) {
                case TELEPORT -> PlayerServerEvents.movePlayer(this.playerId, this.x, this.y, this.z);
                case ENABLE_ORTHOVIEW -> PlayerServerEvents.enableOrthoview(this.playerId);
                case DISABLE_ORTHOVIEW -> PlayerServerEvents.disableOrthoview(this.playerId);
                case START_RTS -> PlayerServerEvents.startRTS(this.playerId, new Vec3(this.x, this.y, this.z));
                case DEFEAT -> PlayerServerEvents.defeat(this.playerId, Component.translatable("server.reignofnether.surrendered").getString());
                case RESET_RTS -> PlayerServerEvents.resetRTS(false);
                case RESET_RTS_HARD -> PlayerServerEvents.resetRTS(true);
                case LOCK_RTS -> PlayerServerEvents.setRTSLock(true);
                case UNLOCK_RTS -> PlayerServerEvents.setRTSLock(false);
                case ENABLE_RTS_SYNCING -> PlayerServerEvents.setRTSSyncingEnabled(true);
                case DISABLE_RTS_SYNCING -> PlayerServerEvents.setRTSSyncingEnabled(false);
            }
        });
        return;
    }
}
