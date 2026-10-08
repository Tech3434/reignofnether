package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.goals.GatherResourcesGoal;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import net.minecraft.resources.ResourceLocation;

/**
 * The generic orders every unit may have, as shared ability instances (plan §14.2). Which of them
 * a unit actually gets is decided in {@link Unit#getCommandAbilities()} from its interfaces, so
 * there is no separate, HUD-only action-button list any more.
 */
public class CommandAbilities {

    private static ResourceLocation icon(String path) {
        return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/" + path);
    }

    public static final Ability ATTACK = new CommandAbility(
            UnitAction.ATTACK, "hud.actionbuttons.reignofnether.attack",
            icon("sword.png"), Keybindings.attack, true);

    public static final Ability BUILD_REPAIR = new CommandAbility(
            UnitAction.BUILD_REPAIR, "hud.actionbuttons.reignofnether.build_repair",
            icon("shovel.png"), Keybindings.build, true);

    public static final Ability GATHER = new CommandAbility(
            UnitAction.TOGGLE_GATHER_TARGET, "abilities.reignofnether.gather",
            CommandAbilities::gatherIcon, Keybindings.gather, false,
            CommandAbilities::isGathering);

    public static final Ability GARRISON = new CommandAbility(
            UnitAction.GARRISON, "hud.actionbuttons.reignofnether.garrison",
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/ladder.png"),
            Keybindings.garrison, true);

    public static final Ability UNGARRISON = new CommandAbility(
            UnitAction.UNGARRISON, "hud.actionbuttons.reignofnether.ungarrison",
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/oak_trapdoor.png"),
            Keybindings.garrison, false);

    public static final Ability STOP = new CommandAbility(
            UnitAction.STOP, "hud.actionbuttons.reignofnether.stop",
            icon("barrier.png"), Keybindings.stop, false);

    public static final Ability HOLD = new CommandAbility(
            UnitAction.HOLD, "hud.actionbuttons.reignofnether.hold_position",
            icon("chestplate.png"), Keybindings.hold, false);

    static {
        // these three stay visible in the unit orders row even while a submenu is open
        ATTACK.alwaysVisible = true;
        STOP.alwaysVisible = true;
        HOLD.alwaysVisible = true;
    }

    private static ResourceName targetResource(Unit unit) {
        if (unit instanceof WorkerUnit workerUnit) {
            GatherResourcesGoal goal = workerUnit.getGatherResourceGoal();
            if (goal != null)
                return goal.getTargetResourceName();
        }
        return ResourceName.NONE;
    }

    private static boolean isGathering(Unit unit) {
        return targetResource(unit) != ResourceName.NONE;
    }

    private static ResourceLocation gatherIcon(Unit unit) {
        return switch (targetResource(unit)) {
            case FOOD -> icon("hoe.png");
            case WOOD -> icon("axe.png");
            case ORE -> icon("pickaxe.png");
            default -> icon("no_gather.png");
        };
    }
}
