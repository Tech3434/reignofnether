package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;

import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.sounds.SoundClientEvents;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.solegendary.reignofnether.sounds.SoundClientEvents.getOrthoviewSoundPos;
import static com.solegendary.reignofnether.time.TimeUtils.normaliseTime;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    @Shadow @Final private Minecraft minecraft;

    private boolean isWardenSound(SoundEvent pSoundEvent) {
        return pSoundEvent.getLocation().getPath().contains("warden");
    }
    private boolean isGhastHurt(SoundEvent pSoundEvent) {
        return pSoundEvent.getLocation().getPath().contains("ghast.hurt");
    }

    @Inject(
            method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("HEAD"),
            cancellable = true
    )
    public void playSeededSound(Player pPlayer, double pX, double pY, double pZ, Holder<SoundEvent> pSound, SoundSource pSource, float pVolume, float pPitch, long pSeed, CallbackInfo ci) {
        if (!OrthoviewClientEvents.isEnabled() || SoundClientEvents.STATIC_SOUNDS.contains(pSound.value()))
            return;

        ci.cancel();
        if (pSound.value().equals(SoundEvents.WARDEN_HEARTBEAT))
            return;

        float volumeMult = 0.5f;
        if (isWardenSound(pSound.value()))
            volumeMult = 0.2f;
        else if (isGhastHurt(pSound.value()))
            volumeMult = 0.1f;

        this.playSoundActual(pX, pY, pZ, pSound.value(), pSource, pVolume * volumeMult, pPitch, false, pSeed);
    }

    // plays sounds for orthoview players as though they were on the ground near their selected units/buildings
    @Inject(
            method = "playSound",
            at = @At("HEAD"),
            cancellable = true
    )
    private void playSound(double pX, double pY, double pZ, SoundEvent pSoundEvent, SoundSource pSoundSource,
                           float pVolume, float pPitch, boolean pDistanceDelay, long pSeed, CallbackInfo ci) {
        if (!OrthoviewClientEvents.isEnabled() || SoundClientEvents.STATIC_SOUNDS.contains(pSoundEvent))
            return;

        ci.cancel();
        if (pSoundEvent.equals(SoundEvents.WARDEN_HEARTBEAT))
            return;

        BlockPos bp = new BlockPos((int) pX, (int) pY, (int) pZ);
        if (SoundClientEvents.mutedBps.contains(bp)) {
            SoundClientEvents.mutedBps.remove(bp);
            return;
        }

        float volumeMult = 0.5f;
        if (isWardenSound(pSoundEvent))
            volumeMult = 0.2f;
        else if (isGhastHurt(pSoundEvent))
            volumeMult = 0.1f;

        this.playSoundActual(pX, pY, pZ, pSoundEvent, pSoundSource, pVolume * volumeMult, pPitch, false, pSeed);
    }

    // not a mixin, but called by them
    private void playSoundActual(double pX, double pY, double pZ, SoundEvent pSoundEvent, SoundSource pSource,
                           float pVolume, float pPitch, boolean pDistanceDelay, long pSeed) {
        if (
                !pSoundEvent.getLocation().getPath().contains("ui.button.click") &&
                !pSoundEvent.getLocation().getNamespace().contains("reignofnether"))
            return;

        Vec3 soundPos = getOrthoviewSoundPos(new Vec3(pX, pY, pZ));

        double d0 = this.minecraft.gameRenderer.getMainCamera().getPosition().distanceToSqr(soundPos.x(), soundPos.y(), soundPos.z());
        SimpleSoundInstance simplesoundinstance = new SimpleSoundInstance(
                pSoundEvent, pSource, pVolume, pPitch, RandomSource.create(pSeed), soundPos.x(), soundPos.y(), soundPos.z()
        );
        if (pDistanceDelay && d0 > 100.0) {
            double d1 = Math.sqrt(d0) / 40.0;
            this.minecraft.getSoundManager().playDelayed(simplesoundinstance, (int)(d1 * 20.0));
        } else {
            this.minecraft.getSoundManager().play(simplesoundinstance);
        }
    }

    @Shadow public void setGameTime(long pTime) { }
    @Shadow public void setDayTime(long pTime) { }

    // when near a source of night distortion, speed up time towards midnight (in whichever direction is closest)
    @Inject(
            method = "tickTime",
            at = @At("HEAD"),
            cancellable = true
    )
    private void tickTime(CallbackInfo ci) {
        if (minecraft.level == null)
            return;

        // Only take the clock over when this mod is actually steering it, i.e. while the player is
        // near a source of night distortion and the target has been pinned to midnight. Otherwise
        // targetClientTime is just the server's day time, and letting vanilla advance the clock is both
        // correct and one less thing to disagree with another mod about.
        if (normaliseTime(TimeClientEvents.targetClientTime) == normaliseTime(TimeClientEvents.serverNormDayTime))
            return;

        ci.cancel();

        long timeNow = minecraft.level.getDayTime();
        long targetTime = normaliseTime(TimeClientEvents.targetClientTime);

        // Signed distance to the target on the 24000-tick day circle, so the shortest way round is
        // taken instead of wrapping through the start of the day.
        long diff = targetTime - timeNow;
        if (diff > 12000) diff -= 24000;
        else if (diff < -12000) diff += 24000;

        // Approach the target monotonically and stop on arrival.
        //
        // The previous version took a fixed +/-100 step and flipped its sign whenever the day had
        // passed the target. Since the server rewrites the target on every ClientboundSetTimePacket,
        // the client kept overshooting and reversing: the sun would jump forward confidently, snap
        // back through the wrap to the start of the day, then jitter for a couple of seconds before
        // repeating. Clamping the step to the remaining distance makes overshoot impossible, so the
        // motion is smooth in both directions and there is nothing to jitter.
        long step = Math.min(Math.abs(diff), 1L);
        long timeSet = normaliseTime(timeNow + (diff >= 0 ? step : -step));

        // The day clock is the only one steered. The sky is drawn from the game clock (LevelRenderer
        // calls RenderSystem.setShaderGameTime(this.level.getGameTime(), ...)), and moving that by
        // the day's step - or reversing it - is what made the celestial bodies leap about the sky
        // even though /time query, which reads the day clock, looked correct.
        this.setGameTime(this.minecraft.level.getLevelData().getGameTime() + 1L);
        this.setDayTime(timeSet);
    }

    @Inject(
            method = "addDestroyBlockEffect",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAddDestroyBlockEffect(BlockPos pPos, BlockState pState, CallbackInfo ci) {
        if (!true)
            ci.cancel();
    }

    @Inject(
            method = "getSkyColor",
            at = @At("HEAD"),
            cancellable = true
    )
    public void getSkyColor(Vec3 pPos, float pPartialTick, CallbackInfoReturnable<Vec3> cir) {
        if (TimeClientEvents.isBloodMoonActive())
            cir.setReturnValue(new Vec3(0.25f, 0f, 0f));
    }

    @Inject(
            method = "setServerVerifiedBlockState",
            at = @At("HEAD"),
            cancellable = true
    )
    private void reignofnether$gateFoggedBlockUpdate(BlockPos pPos, BlockState pState, int pFlags, CallbackInfo ci) {
        if (false && !true)
            ci.cancel();
    }
}
