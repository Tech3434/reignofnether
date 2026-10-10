package com.solegendary.reignofnether.mixin;

import com.mojang.blaze3d.platform.Window;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Moves the chat history window up by {@link OrthoviewClientEvents#CHAT_Y_OFFSET} while the RTS
 * camera is on, so it clears the bottom-left hotkey bar.
 *
 * <p>{@link ChatComponentMixin} already shifts the chat's click detection by the same offset, so
 * without this half the window is drawn at the vanilla Y while every click is measured 55px higher -
 * picking a line, a link or a suggested command hits the wrong row. Upstream did the shift in a
 * mixin on Forge's ForgeGui; NeoForge has no ForgeGui, but it posts its own chat overlay position
 * event from {@code Gui#renderChat}, so the offset is applied to that event's Y before it is built.
 */
@Mixin(Gui.class)
public class OrthoChatGuiMixin {

    @Redirect(
            method = "renderChat",
            at = @At(
                    value = "NEW",
                    target = "net/neoforged/neoforge/client/event/CustomizeGuiOverlayEvent$Chat"
            )
    )
    private CustomizeGuiOverlayEvent.Chat ron$shiftChatUp(Window window, GuiGraphics guiGraphics,
                                                          DeltaTracker partialTick, int posX, int posY) {
        if (OrthoviewClientEvents.isEnabled())
            posY += OrthoviewClientEvents.CHAT_Y_OFFSET;
        return new CustomizeGuiOverlayEvent.Chat(window, guiGraphics, partialTick, posX, posY);
    }
}
