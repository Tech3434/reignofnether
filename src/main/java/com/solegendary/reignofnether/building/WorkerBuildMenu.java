package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.building.buildings.JsonBuilding;
import com.solegendary.reignofnether.building.buildings.JsonBuildingManager;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents;
import com.solegendary.reignofnether.hud.buttons.Button;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The worker's build menu (plan §14.2 / CONTENT_JSON_PLAN.md): the buildings a data-driven worker can
 * place - the code template buildings plus every datapack building and custom building - gated by
 * research. Replaces the old {@code VillagerUnit.getBuildingButtons} that went with the code units.
 */
public final class WorkerBuildMenu {

    private WorkerBuildMenu() { }

    public static List<Button> buildButtons() {
        List<Button> buttons = new ArrayList<>();
        buttons.add(gate(Buildings.TOWN_CENTRE.getBuildButton(null), Buildings.TOWN_CENTRE));
        buttons.add(gate(Buildings.BARRACKS.getBuildButton(null), Buildings.BARRACKS));

        CustomBuildingClientEvents.customBuildings.forEach(cb -> {
            if (cb.buildableByVillagers)
                buttons.add(gate(cb.getWorkerBuildButton(null), cb));
        });

        for (JsonBuilding jb : JsonBuildingManager.all())
            buttons.add(gate(jb.getBuildButton(null), jb));

        return buttons;
    }

    /** Disables a build button while the local player has not satisfied the building's research. */
    private static Button gate(Button button, Building building) {
        if (button == null || building == null || building.requiredResearch.isEmpty())
            return button;
        Supplier<Boolean> original = button.isEnabled;
        button.isEnabled = () -> {
            if (original != null && !Boolean.TRUE.equals(original.get()))
                return false;
            Minecraft mc = Minecraft.getInstance();
            return mc.level == null || mc.player == null
                    || building.meetsResearch(mc.level, mc.player.getName().getString());
        };
        return button;
    }
}
