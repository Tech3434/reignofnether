package com.solegendary.reignofnether.faction;

import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.player.PlayerServerboundPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * The faction-selection menu: one button per registered faction. When the menu was opened with a named
 * faction, the others are shown but disabled, exactly like locked abilities/buildings.
 */
public final class FactionMenu {

    private FactionMenu() { }

    public static List<Button> render(GuiGraphics gui, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        List<Button> buttons = new ArrayList<>();
        Minecraft MC = Minecraft.getInstance();
        if (MC.player == null)
            return buttons;

        int frameSize = Button.DEFAULT_ICON_FRAME_SIZE;
        int x = 8;
        int y = 44;
        for (FactionClientEvents.Info info : FactionClientEvents.all()) {
            boolean selectable = FactionClientEvents.isSelectable(info.id());
            Button button = new Button(
                    I18n.get(info.nameKey()),
                    frameSize,
                    info.icon(),
                    (Keybinding) null,
                    () -> false,
                    () -> false,
                    () -> selectable,
                    () -> {
                        BlockPos pos = MC.player.getOnPos();
                        PlayerServerboundPacket.startRTS((double) pos.getX(), (double) pos.getY(),
                                (double) pos.getZ(), info.id());
                        FactionClientEvents.close();
                    },
                    null,
                    List.of()
            );
            button.render(gui, x, y, mouseX, mouseY);
            buttons.add(button);
            y += frameSize + 4;
        }
        return buttons;
    }
}
