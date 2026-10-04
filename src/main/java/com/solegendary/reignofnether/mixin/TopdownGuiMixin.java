package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// disable the vanilla HUD entirely when in orthoview mode as it has cheaty functions like tp to player

@Mixin(Gui.class)
public class TopdownGuiMixin {

    // 1.21.1 folded renderHotbar and renderCrosshair into Gui#render, so they can no longer be
    // cancelled one by one. Skipping the whole vanilla HUD is the same outcome the two separate
    // injections had: no hotbar, no crosshair, no spectator-only affordances. The mod's own HUD is
    // drawn from its own render events and is unaffected.
    @Inject(
            method = "render",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onOrthoviewHideVanillaHud(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (OrthoviewClientEvents.isEnabled())
            ci.cancel();
    }
}
