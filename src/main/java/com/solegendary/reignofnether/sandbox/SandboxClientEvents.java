package com.solegendary.reignofnether.sandbox;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.cursor.CursorClientEvents;

import com.solegendary.reignofnether.gamemode.ClientGameModeHelper;
import com.solegendary.reignofnether.gamemode.GameMode;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.buttons.UnitSpawnButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.Cheats;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerServerboundPacket;
import com.solegendary.reignofnether.registrars.BlockRegistrar;

import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;

import com.solegendary.reignofnether.unit.units.villagers.VillagerUnit;
import com.solegendary.reignofnether.util.ArrayUtil;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class SandboxClientEvents {

    public static Relationship relationship = Relationship.OWNED;
    public static SandboxMenuType sandboxMenuType = SandboxMenuType.UNITS;
    public static CustomBuildingSortOption customBuildingSortOption = CustomBuildingSortOption.NAME;

    private static final Minecraft MC = Minecraft.getInstance();

    public static String spawnUnitName = "";

    /**
     * Sandbox is gated on operator permission (level 2), not on a game mode.
     *
     * <p>It used to be a game mode of its own, reached by cycling the HUD's mode button, and existed
     * only so its tools could be gated. With the modes reduced to CLASSIC, the tools key off permission
     * directly and the mode toggle is gone.
     */
    public static boolean isSandboxPlayer(String playerName) {
        return MC.player != null && playerName.equals(MC.player.getName().getString()) && isSandboxPlayer();
    }

    public static boolean isSandboxPlayer() {
        return PlayerClientEvents.isRTSPlayer() && MC.player != null && MC.player.hasPermissions(2);
    }

    public static List<BuildingPlaceButton> getBuildingButtons() {
        return VillagerUnit.getBuildingButtons();
    }

    public static List<Button> getCustomBuildingButtons() {
        if (CustomBuildingClientEvents.customBuildings.isEmpty()) {
            return List.of(new Button(
                    "Custom building info",
                    Button.itemIconSize,
                    ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/help.png"),
                    (Keybinding) null,
                    () -> false,
                    () -> false,
                    () -> true,
                    () -> {
                        if (MC.player != null) {
                            MC.player.addItem(new ItemStack(BlockRegistrar.RTS_STRUCTURE_BLOCK.get()));
                        }
                    },
                    null,
                    List.of(
                            fcs(I18n.get("sandbox.reignofnether.custom_buildings_info.tooltip1")),
                            fcs(I18n.get("sandbox.reignofnether.custom_buildings_info.tooltip2")),
                            fcs(""),
                            fcs(I18n.get("sandbox.reignofnether.custom_buildings_info.tooltip3"))
                    )
            ));
        } else {
            List<Button> list = new ArrayList<>();
            for (CustomBuilding cb : CustomBuildingClientEvents.customBuildings) {
                Button buildButton = cb.getBuildButton(null);
                list.add(buildButton);
            }
            return list;
        }
    }

    public static List<UnitSpawnButton> getUnitButtons() {
        // One roster now, so the faction toggle has nothing to switch between. This list is the single
        // place to add a unit: give it a Prod, register the entity type, then list it here.
        return List.of(ProductionItems.VILLAGER.getPlaceButton(),
                       ProductionItems.VINDICATOR.getPlaceButton());
    }

    public static String getRelationshipName(Relationship relationship) {
        return switch (relationship) {
            case OWNED -> I18n.get("hud.relationship.reignofnether.owned");
            case FRIENDLY -> I18n.get("hud.relationship.reignofnether.allied");
            case NEUTRAL -> I18n.get("hud.relationship.reignofnether.neutral");
            case HOSTILE -> I18n.get("hud.relationship.reignofnether.enemy");
        };
    }

    public static Button getToggleRelationshipButton() {
        return new Button(
                "Toggle Relationship",
                Button.itemIconSize,
                switch (relationship) {
                    case OWNED -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/lime_wool.png");
                    case FRIENDLY -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/blue_wool.png");
                    case NEUTRAL -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/yellow_wool.png");
                    case HOSTILE -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/red_wool.png");
                },
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                () -> {
                    switch (relationship) {
                        default -> relationship = Relationship.NEUTRAL;
                        case NEUTRAL -> relationship = Relationship.HOSTILE;
                        case HOSTILE -> relationship = Relationship.OWNED;
                    }
                },
                () -> {
                    switch (relationship) {
                        default -> relationship = Relationship.HOSTILE;
                        case NEUTRAL -> relationship = Relationship.OWNED;
                        case HOSTILE -> relationship = Relationship.NEUTRAL;
                    }
                },
                List.of(
                        fcs(I18n.get("hud.relationship.reignofnether.owned"), relationship == Relationship.OWNED),
                        fcs(I18n.get("hud.relationship.reignofnether.neutral"), relationship == Relationship.NEUTRAL),
                        fcs(I18n.get("hud.relationship.reignofnether.enemy"), relationship == Relationship.HOSTILE)
                )
        );
    }

    public static Button getCycleBuildingOrUnitsButton() {
        return new Button(
                "Toggle Building or Units",
                Button.itemIconSize,
                switch (sandboxMenuType) {
                    case UNITS -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/spawn_egg.png");
                    case BUILDINGS -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/crafting_table_front.png");
                    case CUSTOM_BUILDINGS -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/smithing_table_front.png");
                },
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                () -> {
                    switch (sandboxMenuType) {
                        case UNITS -> sandboxMenuType = SandboxMenuType.BUILDINGS;
                        case BUILDINGS -> sandboxMenuType = SandboxMenuType.CUSTOM_BUILDINGS;
                        case CUSTOM_BUILDINGS -> sandboxMenuType = SandboxMenuType.UNITS;
                    }
                },
                () -> {
                    switch (sandboxMenuType) {
                        case UNITS -> sandboxMenuType = SandboxMenuType.CUSTOM_BUILDINGS;
                        case BUILDINGS -> sandboxMenuType = SandboxMenuType.UNITS;
                        case CUSTOM_BUILDINGS -> sandboxMenuType = SandboxMenuType.BUILDINGS;
                    }
                },
                List.of(
                        fcs(I18n.get("sandbox.reignofnether.menu_type_button_units"), sandboxMenuType == SandboxMenuType.UNITS),
                        fcs(I18n.get("sandbox.reignofnether.menu_type_button_buildings"), sandboxMenuType == SandboxMenuType.BUILDINGS),
                        fcs(I18n.get("sandbox.reignofnether.menu_type_button_custom_buildings"), sandboxMenuType == SandboxMenuType.CUSTOM_BUILDINGS)
                )
        );
    }

    public static Button getToggleBuildingCheatsButton() {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player == null)
            return null;
        boolean hasCheats = Cheats.hasCheat("warpten") && Cheats.hasCheat("modifythephasevariance");
        return new Button(
                "Toggle Building Cheats",
                Button.itemIconSize,
                hasCheats ?
                        ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/command_block_side.png") :
                        ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/command_block_side_dark.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                () -> {
                    for (String cheat : List.of("warpten", "modifythephasevariance"))
                        if (Cheats.hasCheat(cheat) == hasCheats)
                            PlayerServerboundPacket.setCheat(cheat);
                },
                null,
                List.of(hasCheats ? fcs(I18n.get("sandbox.reignofnether.building_cheats_on")) :
                                fcs(I18n.get("sandbox.reignofnether.building_cheats_off")),
                        fcs(I18n.get("sandbox.reignofnether.building_cheats1"))
                )
        );
    }

    public static Button getToggleUnitCheatsButton() {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player == null)
            return null;
        List<String> cheats = List.of("operationcwal", "medievalman", "foodforthought", "slipslopslap");
        boolean hasCheats = cheats.stream().allMatch(Cheats::hasCheat);
        return new Button(
                "Toggle Unit Cheats",
                Button.itemIconSize,
                hasCheats ?
                        ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/chain_command_block_side.png") :
                        ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/chain_command_block_side_dark.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                () -> {
                    for (String cheat : cheats)
                        if (Cheats.hasCheat(cheat) == hasCheats)
                            PlayerServerboundPacket.setCheat(cheat);
                },
                null,
                List.of(hasCheats ? fcs(I18n.get("sandbox.reignofnether.unit_cheats_on")) :
                                    fcs(I18n.get("sandbox.reignofnether.unit_cheats_off")),
                                    fcs(I18n.get("sandbox.reignofnether.unit_cheats1")),
                                    fcs(I18n.get("sandbox.reignofnether.unit_cheats2"))
                )
        );
    }

    public static Button getToggleNonUnitControlButton() {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player == null)
            return null;
        boolean hasCheat = Cheats.hasCheat("wouldyoukindly");
        return new Button(
                "Toggle Full Unit Control",
                Button.itemIconSize,
                hasCheat ?
                        ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/repeating_command_block_side.png") :
                        ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/repeating_command_block_side_dark.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                () -> PlayerServerboundPacket.setCheat("wouldyoukindly"),
                null,
                List.of(hasCheat ? fcs(I18n.get("sandbox.reignofnether.nonunit_control_cheat_on")) :
                                fcs(I18n.get("sandbox.reignofnether.nonunit_control_cheat_off")),
                        fcs(I18n.get("sandbox.reignofnether.nonunit_control_cheat1"))
                )
        );
    }

    public static Button getSortCustomBuildingsButton() {
        return new Button(
                "Sort Custom Buildings",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/hopper.png"),
                (Keybinding) null,
                () -> false,
                () -> sandboxMenuType != SandboxMenuType.CUSTOM_BUILDINGS,
                () -> true,
                () -> {
                    switch (customBuildingSortOption) {
                        case NAME -> customBuildingSortOption = CustomBuildingSortOption.SIZE;
                        case SIZE -> customBuildingSortOption = CustomBuildingSortOption.FACTION;
                        case FACTION -> customBuildingSortOption = CustomBuildingSortOption.NAME;
                    }
                    sortCustomBuildings();
                },
                () -> {
                    switch (customBuildingSortOption) {
                        case NAME -> customBuildingSortOption = CustomBuildingSortOption.FACTION;
                        case SIZE -> customBuildingSortOption = CustomBuildingSortOption.NAME;
                        case FACTION -> customBuildingSortOption = CustomBuildingSortOption.SIZE;
                    }
                    sortCustomBuildings();
                },
                List.of(
                        fcs(I18n.get("sandbox.reignofnether.sort_custom_buildings.name"), customBuildingSortOption == CustomBuildingSortOption.NAME),
                        fcs(I18n.get("sandbox.reignofnether.sort_custom_buildings.size"), customBuildingSortOption == CustomBuildingSortOption.SIZE),
                        fcs(I18n.get("sandbox.reignofnether.sort_custom_buildings.faction"), customBuildingSortOption == CustomBuildingSortOption.FACTION)
                )
        );
    }

    public static void sortCustomBuildings() {
        switch (customBuildingSortOption) {
            case NAME -> CustomBuildingClientEvents.customBuildings.sort(Comparator.comparing(b -> b.name));
            case SIZE -> CustomBuildingClientEvents.customBuildings.sort(Comparator.comparing(b -> b.structureSize.getX() * b.structureSize.getY() * b.structureSize.getZ()));
            case FACTION -> CustomBuildingClientEvents.customBuildings.sort(
                    Comparator.comparing((CustomBuilding b) -> b.buildableByVillagers)
                            .thenComparing((CustomBuilding b) -> b.buildableByMonsters)
                            .thenComparing((CustomBuilding b) -> b.buildableByPiglins).reversed());
        }
    }

    public static Button getExitSandboxButton() {
        return new Button(
                "Exit Sandbox Mode",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/cross.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                PlayerServerboundPacket::resetRTS,
                null,
                List.of(
                        fcs(I18n.get("sandbox.reignofnether.exit1"), true)
                )
        );
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Post evt) {
        if (!OrthoviewClientEvents.isEnabled()) return;

        // prevent clicking behind HUDs
        if (HudClientEvents.isMouseOverAnyButtonOrHud() || MC.player == null) {
            CursorClientEvents.setLeftClickSandboxAction(null);
            return;
        }

        SandboxAction sandboxAction = CursorClientEvents.getLeftClickSandboxAction();
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1 && sandboxAction != null) {

            String ownerName = switch (relationship) {
               case NEUTRAL -> "";
               case HOSTILE -> "Enemy";
               default -> MC.player.getName().getString();
            };

            switch (sandboxAction) {
                case SPAWN_UNIT -> SandboxServerboundPacket.spawnUnit(CursorClientEvents.getLeftClickSandboxAction(), ownerName, spawnUnitName, CursorClientEvents.getPreselectedBlockPos());
                case SET_ANCHOR -> SandboxServerboundPacket.setAnchor(CursorClientEvents.getPreselectedBlockPos(), ArrayUtil.livingEntityListToIdArray(UnitClientEvents.getSelectedUnits()));
            }

            if (!Keybindings.shiftMod.isDown()) {
                spawnUnitName = "";
                CursorClientEvents.setLeftClickSandboxAction(null);
            }
        }
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            spawnUnitName = "";
            CursorClientEvents.setLeftClickSandboxAction(null);
        }
    }
}
