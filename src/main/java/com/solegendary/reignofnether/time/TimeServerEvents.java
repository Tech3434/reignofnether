package com.solegendary.reignofnether.time;

import net.neoforged.neoforge.event.tick.LevelTickEvent;

import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;

import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;

public class TimeServerEvents {

    private static long serverStartTime = 0;
    private static long lastTime = -1;

    // Blood moon spawning belonged to the deleted survival/hero content.
    // NightUtils still asks for this state, so the hook stays and answers "off".
    public static boolean isBloodMoonActive() {
        return false;
    }

    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent.Post evt) {
        if (evt.getLevel().isClientSide() || evt.getLevel().dimension() != Level.OVERWORLD)
            return;

        if (serverStartTime == 0)
            serverStartTime = evt.getLevel().getDayTime();

        long normTime = TimeUtils.normaliseTime(evt.getLevel().getDayTime());
        if (!PlayerServerEvents.rtsPlayers.isEmpty() && lastTime >= 0) {
            if (lastTime <= TimeUtils.DUSK && normTime > TimeUtils.DUSK) {
                PlayerServerEvents.sendMessageToAllPlayers("survival.reignofnether.dusk", true);
                SoundClientboundPacket.playSoundForAllPlayers(SoundAction.DUSK_WOLF);
            }
            else if (lastTime <= TimeUtils.DAWN && normTime > TimeUtils.DAWN) {
                PlayerServerEvents.sendMessageToAllPlayers("survival.reignofnether.dawn", true);
                SoundClientboundPacket.playSoundForAllPlayers(SoundAction.DAWN_ROOSTER, 0.9f);
            }
        }
        lastTime = normTime;
    }
}
