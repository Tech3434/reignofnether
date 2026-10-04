package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.time.NightUtils;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.time.TimeUtils;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// prevent syncing time from serverside under some conditions

@Mixin(ClientPacketListener.class)
public class ClientPacketMixin {

    // 1.21.1 moved the Minecraft field up to ClientCommonPacketListenerImpl, so the shadow no longer
    // resolves from this target. It was only ever read, and Minecraft.getInstance() is the same
    // object, so the field is not needed at all.
    private static final Minecraft MINECRAFT = Minecraft.getInstance();

    @Inject(
            method = "handleSetTime",
            at = @At("HEAD"),
            cancellable = true
    )
    private void handleSetTime(ClientboundSetTimePacket pPacket, CallbackInfo ci) {
        // Bookkeeping only - the packet is deliberately NOT cancelled.
        //
        // Vanilla sends this once every 20 ticks and ClientLevel#tickTime is expected to apply it.
        // Cancelling it left the client clock as a second, competing authority: every second the
        // server's time replaced our target, and near a night-distortion source that target is
        // pinned to midnight, so the client kept re-racing towards midnight and back - the sun
        // jerking roughly once a second. The mod already moves the day on the server side
        // (SurvivalServerEvents, PlayerServerEvents), so the server is the only authority the client
        // needs to mirror.
        TimeClientEvents.serverNormDayTime = TimeUtils.normaliseTime(pPacket.getDayTime());
        TimeClientEvents.serverGameTime = pPacket.getGameTime();

        Vec3 pos;
        if (OrthoviewClientEvents.isEnabled())
            pos = MiscUtil.getOrthoviewCentreWorldPos(MINECRAFT);
        else if (MINECRAFT.player != null && MINECRAFT.level != null)
            pos = MINECRAFT.player.position();
        else
            return;

        if (NightUtils.isInRangeOfNightSource(pos, true))
            TimeClientEvents.targetClientTime = 18000; // midnight
        else
            TimeClientEvents.targetClientTime = TimeClientEvents.serverNormDayTime;
    }
}