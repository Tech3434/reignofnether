package com.solegendary.reignofnether.guiscreen;

import com.solegendary.reignofnether.hud.TextInputClientEvents;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.client.gui.screens.PauseScreen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * Handler for TopdownGui, the GUI screen that allows for cursor movement on screen
 * Doing stuff like initialising it and controlling where it comes up in the game client
 *
 * @author SoLegendary
 */

public class TopdownGuiClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();
    private static int noScreenTicks = 0; // ticks that no screen has been opened
    private static boolean shouldPause = false;

    // if no other screen is open and we've got orthoview enabled, open a screen based on shouldPause
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post evt) {
        
        if (OrthoviewClientEvents.isEnabled() && Minecraft.getInstance().screen == null) {
            noScreenTicks += 1;
            if (noScreenTicks >= 3) {
                if (shouldPause) {
                    shouldPause = false;
                    MC.setScreen(new PauseScreen(true));
                }
                else
                    TopdownGuiServerboundPacket.openTopdownGui(MC.player.getId());
                noScreenTicks = 0;
            }
        }
    }

    // allow closing by pressing escape
    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing evt) {
        if (evt.getScreen().isPauseScreen())
            shouldPause = false;
    }

    @SubscribeEvent
    public static void beforeGuiRender(ScreenEvent.Render.Pre evt) {
        // cancel drawing the GUI
        if (evt.getScreen() instanceof TopdownGui)
            evt.setCanceled(true);
    }

    @SubscribeEvent
    public static void onKeyPress(ScreenEvent.KeyPressed.Pre evt) {
        if (OrthoviewClientEvents.isEnabled() && evt.getKeyCode() == Keybindings.pause.getKey())
            shouldPause = true;
    }
}