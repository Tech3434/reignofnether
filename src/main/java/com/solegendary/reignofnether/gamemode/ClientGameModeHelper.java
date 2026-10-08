package com.solegendary.reignofnether.gamemode;

import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

/**
 * The HUD's game-mode button.
 *
 * <p>There is one mode left, so there is nothing to cycle: the button reports which mode is running and
 * why it is locked. The cycling and the per-mode buttons that used to live here went with the survival
 * and scenario content.
 */
public class ClientGameModeHelper {

    public static GameMode DEFAULT_GAMEMODE = GameMode.CLASSIC;
    public static GameMode gameMode = DEFAULT_GAMEMODE;
    public static boolean gameModeLocked = false; // locked with startRTS() in any gamemode, unlocked with /rts-reset

    private static String getLockedString() {
        return gameModeLocked ? " " + I18n.get("hud.gamemode.reignofnether.locked") : "";
    }

    private static Button getClassicButton() {
        ArrayList<FormattedCharSequence> tooltips = new ArrayList<>();
        tooltips.add(fcs(I18n.get("hud.gamemode.reignofnether.classic1") +
                getLockedString(), true));
        tooltips.add(fcs(""));
        tooltips.add(fcs(I18n.get("hud.gamemode.reignofnether.classic2")));
        tooltips.add(fcs(I18n.get("hud.gamemode.reignofnether.classic3")));

        return new Button(
                "Classic",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/grass_block_side.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                null,
                null,
                tooltips
        );
    }

    public static Button getButton() {
        Button button = switch (gameMode) {
            case CLASSIC -> getClassicButton();
            default -> null;
        };
        if (button != null)
            button.tooltipOffsetY = 15;
        return button;
    }
}
