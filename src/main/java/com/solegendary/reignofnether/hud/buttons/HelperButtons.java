package com.solegendary.reignofnether.hud.buttons;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.addon.GarrisonableBuildingAddon;

import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerServerboundPacket;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.hud.HudClientEvents.hudSelectedPlacement;
import static com.solegendary.reignofnether.unit.UnitClientEvents.getPlayerToEntityRelationship;
import static com.solegendary.reignofnether.unit.UnitClientEvents.idleWorkerIds;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class HelperButtons {

    private static final Minecraft MC = Minecraft.getInstance();

    public static final int ICON_SIZE = 14;

    private static int idleWorkerIndex = 0;
    public static Button idleWorkerButton;
    public static Button chatButton;
    public static Button buildingCancelButton;
    public static Button armyButton;

    public static void updateButtons() {
        chatButton = new Button(
                "Chat",
                ICON_SIZE,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/speech_bubble.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                () -> MC.setScreen(new ChatScreen("")),
                null,
                List.of(FormattedCharSequence.forward(I18n.get("hud.helperbuttons.reignofnether.chat"), Style.EMPTY))
        );
        idleWorkerButton = new Button(
                "Idle workers (CTRL-click to select all)",
                ICON_SIZE,
                getIdleWorkerIcon(),
                Keybindings.hotkey6,
                () -> false,
                idleWorkerIds::isEmpty,
                () -> true,
                () -> {
                    if (MC.level == null)
                        return;

                    if (Keybindings.ctrlMod.isDown()) {
                        UnitClientEvents.clearSelectedUnits();
                        for (int id : idleWorkerIds) {
                            Entity entity = MC.level.getEntity(id);
                            if (entity instanceof WorkerUnit)
                                UnitClientEvents.addSelectedUnit((LivingEntity) entity);
                        }
                    } else {
                        if (idleWorkerIndex >= idleWorkerIds.size())
                            idleWorkerIndex = 0; // Reset to zero if out of bounds

                        Entity entity = MC.level.getEntity(idleWorkerIds.get(idleWorkerIndex));
                        if (entity instanceof WorkerUnit) {
                            OrthoviewClientEvents.centreCameraOnPos(entity.position());
                            UnitClientEvents.clearSelectedUnits();
                            UnitClientEvents.addSelectedUnit((LivingEntity) entity);
                        }
                        idleWorkerIndex += 1;

                        // Reset idleWorkerIndex if it exceeds the size after increment
                        if (idleWorkerIndex >= idleWorkerIds.size())
                            idleWorkerIndex = 0;
                    }
                },
                null,
                List.of(
                        FormattedCharSequence.forward(I18n.get("hud.helperbuttons.reignofnether.idle_workers"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("hud.helperbuttons.reignofnether.idle_workers_shift"), Style.EMPTY)
                )
        );
        buildingCancelButton = new Button(
                "Cancel",
                ICON_SIZE,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/barrier.png"),
                Keybindings.cancelBuild,
                () -> false,
                () -> {
                    if (hudSelectedPlacement == null)
                        return false;
                    return BuildingUtils.getTotalCompletedBuildingsOwned(true, hudSelectedPlacement.ownerName) == 0;
                },
                () -> true,
                () -> {
                    if (MC.player != null)
                        BuildingServerboundPacket.cancelBuilding(hudSelectedPlacement.minCorner, MC.player.getName().getString());
                    hudSelectedPlacement = null;
                },
                null,
                List.of(FormattedCharSequence.forward(I18n.get("hud.helperbuttons.reignofnether.cancel"), Style.EMPTY))
        );
        armyButton = new Button(
                "Select all military units",
                ICON_SIZE,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/sword_and_bow.png"),
                Keybindings.hotkey7,
                () -> false,
                () -> {
                    var flag = false;
                    for (LivingEntity u : UnitClientEvents.getAllUnits()) {
                        if (!(u instanceof WorkerUnit) &&
                            GarrisonableBuildingAddon.getGarrison((Unit) u) == null &&
                            getPlayerToEntityRelationship(u) == Relationship.OWNED) {
                            flag = true;
                            break;
                        }
                    }
                    return !flag;
                },
                () -> true,
                () -> {
                    List<LivingEntity> militaryUnits = new ArrayList<>();

                    if (Keybindings.shiftMod.isDown()) {
                        militaryUnits.addAll(UnitClientEvents.getMilitaryUnitsOnScreen());
                    } else {
                        for (LivingEntity u : UnitClientEvents.getAllUnits()) {
                            if (u instanceof Unit unit && unit.isRtsUnit() &&
                                !(u instanceof WorkerUnit) &&
                                GarrisonableBuildingAddon.getGarrison(unit) == null &&
                                getPlayerToEntityRelationship(u) == Relationship.OWNED) {
                                militaryUnits.add(u);
                            }
                        }
                    }
                    UnitClientEvents.clearSelectedUnits();
                    for (LivingEntity militaryUnit : militaryUnits)
                        UnitClientEvents.addSelectedUnit(militaryUnit);
                    HudClientEvents.setLowestCdHudEntity();
                },
                null,
                List.of(
                        fcs(I18n.get("hud.helperbuttons.reignofnether.select_all_military_units"), Style.EMPTY),
                        fcs(I18n.get("hud.helperbuttons.reignofnether.select_all_military_units_shift"), Style.EMPTY)
                )
        );
    }

    private static ResourceLocation getIdleWorkerIcon() {
        return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/villager.png");
    }

    /**
     * The one button that puts the player into the match.
     *
     * <p>It replaces the per-faction start buttons: with the default factions gone there is one match
     * and one start, so the button just starts it where the player is standing. It stays hidden in
     * sandbox, where a match is already running.
     */
    public static Button getStartButton() {
        return new Button(
                "Start RTS",
                ICON_SIZE,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/command_block_side.png"),
                (Keybinding) null,
                () -> false,
                () -> PlayerClientEvents.rtsLocked,
                () -> true,
                () -> {
                    if (MC.player == null)
                        return;
                    var pos = MC.player.getOnPos();
                    PlayerServerboundPacket.startRTS((double) pos.getX(), (double) pos.getY(), (double) pos.getZ());
                },
                null,
                List.of(
                        fcs(I18n.get("hud.helperbuttons.reignofnether.start"), Style.EMPTY)
                )
        );
    }

    static {
        updateButtons();
    }
}
