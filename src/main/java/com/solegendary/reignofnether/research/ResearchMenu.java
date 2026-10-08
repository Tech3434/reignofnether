package com.solegendary.reignofnether.research;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple research panel: a toggle button plus a list of every known research with its status
 * (researched / available / locked by prerequisites). Informational for now - actually starting a
 * research needs a server request flow, which is a later step.
 */
public final class ResearchMenu {

    public static boolean menuOpen = false;

    private static final ResourceLocation TOGGLE_ICON =
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/repeating_command_block_back.png");
    private static final ResourceLocation UNKNOWN_ICON =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/knowledge_book.png");

    private ResearchMenu() { }

    public static Button getToggleButton() {
        Minecraft MC = Minecraft.getInstance();
        return new Button(
                I18n.get("hud.research.reignofnether.menu"),
                14,
                TOGGLE_ICON,
                (Keybinding) null,
                () -> menuOpen,
                () -> false,
                () -> true,
                () -> menuOpen = !menuOpen,
                null,
                List.of(Component.literal(I18n.get("hud.research.reignofnether.menu")).getVisualOrderText())
        );
    }

    public static List<Button> render(GuiGraphics gui, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        List<Button> buttons = new ArrayList<>();
        Minecraft MC = Minecraft.getInstance();
        if (MC.player == null)
            return buttons;

        String playerName = MC.player.getName().getString();
        int frameSize = Button.DEFAULT_ICON_FRAME_SIZE;
        int x = 8;
        int y = 64;
        for (ResearchClientEvents.Def def : ResearchClientEvents.definitions()) {
            boolean researched = ResearchClientEvents.has(playerName, def.id());
            boolean available = researched || ResearchUtils.meetsClient(playerName, def.prerequisites());

            String statusKey = researched
                    ? "hud.research.reignofnether.researched"
                    : available ? "hud.research.reignofnether.available" : "hud.research.reignofnether.locked";
            List<FormattedCharSequence> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(I18n.get(def.nameKey())).getVisualOrderText());
            tooltip.add(Component.literal(I18n.get(statusKey)).getVisualOrderText());

            // Status only: starting a research is done from the building UI (deferred to the
            // buildings-JSON phase, where a building lists the researches it offers).
            Button button = new Button(
                    I18n.get(def.nameKey()),
                    frameSize,
                    def.icon() != null ? def.icon() : UNKNOWN_ICON,
                    (Keybinding) null,
                    () -> researched,
                    () -> false,
                    () -> researched,
                    null,
                    null,
                    tooltip
            );
            button.render(gui, x, y, mouseX, mouseY);
            buttons.add(button);
            y += frameSize + 4;
        }
        return buttons;
    }
}
