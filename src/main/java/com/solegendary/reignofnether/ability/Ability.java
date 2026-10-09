package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommand;

public class Ability {
    public final UnitAction action; // null for worker building production items (handled specially in BuildingClientEvents)
    public float cooldownMax;
    public float range; // if <= 0, is melee
    public float radius; // if <= 0, is single target
    public boolean canTargetEntities;
    public boolean oneClickOneUse; // if true, a group of units/buildings will use their abilities one by one
    public boolean passive; // passive abilities have no button and are never clicked

    /**
     * Researches that must be satisfied before this ability can be used (empty = always available).
     * Conditions carry {@code invert}, so an ability can also be locked while a research IS present.
     */
    public java.util.List<com.solegendary.reignofnether.research.ResearchCondition> requiredResearch = java.util.List.of();

    /** Fluent setter for abilities locked behind research. */
    public Ability requireResearch(com.solegendary.reignofnether.research.ResearchCondition... conditions) {
        this.requiredResearch = java.util.List.of(conditions);
        return this;
    }

    /** Whether {@code ownerName} satisfies this ability's research requirements (server or client). */
    public boolean meetsResearch(Level level, String ownerName) {
        if (requiredResearch.isEmpty())
            return true;
        if (level.isClientSide())
            return com.solegendary.reignofnether.research.ResearchUtils.meetsClient(ownerName, requiredResearch);
        return com.solegendary.reignofnether.research.ResearchUtils.meets(level, ownerName, requiredResearch);
    }

    public UnitAction autocastEnableAction = null;
    public UnitAction autocastDisableAction = null;
    public int maxCharges = 1;
    private boolean defaultAutocast = false;
    public void setAutocast(boolean value, Unit unit) { unit.setAutocast(value ? this : null); }
    public void setAutocast(boolean value, BuildingPlacement placement) { placement.setAutocast(value ? this : null); }
    public boolean isAutocasting(Unit unit) { return unit.hasAutocast(this); }
    public boolean isAutocasting(BuildingPlacement placement) { return placement.hasAutocast(this); }
    protected Keybinding defaultHotkey = Keybindings.abilitySlot1;

    /**
     * Abilities this ability opens as a submenu. An ability with a non-empty submenu behaves as a
     * menu button: clicking it opens the list instead of running {@link #use}. This is what turns a
     * unit or building into "an entity with a set of abilities" (plan §14.1) and makes
     * menu-in-menu possible without any per-class code in the HUD.
     */
    public final java.util.List<Ability> subAbilities = new java.util.ArrayList<>();

    /**
     * Pinned to the unit orders row even while a submenu is open. The owner asked for the key orders
     * (attack / stop / hold) to stay put; set on those in {@link CommandAbilities}.
     */
    public boolean alwaysVisible = false;

    /**
     * If true, opening this ability as a submenu hides even the always-visible orders. Menus that
     * should leave the pinned orders in place set it to false.
     */
    public boolean hidePinnedOnOpen = true;

    public boolean showRangeLine = false;
    public boolean showRadiusCircle = false;
    public boolean showRangeCircle = true;

    public Ability(UnitAction action, int cooldownMax, float range, float radius, boolean canTargetEntities) {
        this.action = action;
        this.cooldownMax = cooldownMax;
        this.range = range;
        this.radius = radius;
        this.canTargetEntities = canTargetEntities;
        this.oneClickOneUse = false;
    }

    public Ability(UnitAction action, int cooldownMax, float range, float radius, boolean canTargetEntities, boolean oneClickOneUse) {
        this.action = action;
        this.cooldownMax = cooldownMax;
        this.range = range;
        this.radius = radius;
        this.canTargetEntities = canTargetEntities;
        this.oneClickOneUse = oneClickOneUse;
    }

    public boolean isMenu() {
        return !subAbilities.isEmpty();
    }

    /** A menu whose items are built on demand rather than stored in {@link #subAbilities}. */
    protected boolean hasDynamicSubButtons() {
        return false;
    }

    public java.util.List<Ability> getSubAbilities() {
        return subAbilities;
    }

    /**
     * Optional pre-built buttons for a menu that is not a plain list of abilities (the worker's
     * build menu hands out the existing building-place buttons). A null return means the menu
     * renders {@link #subAbilities} instead.
     */
    public java.util.List<com.solegendary.reignofnether.hud.buttons.Button> getSubButtons(Unit unit) {
        return null;
    }

    /**
     * Optional explicit {@code [row, col]} grid position for one of this menu's sub-abilities; a null
     * return means the HUD lays it out automatically. Used by {@link DataMenuAbility} for row/col.
     */
    public int[] getSubButtonPosition(Ability subAbility) {
        return null;
    }

    /**
     * Optional explicit grid positions for the buttons returned by {@link #getSubButtons}, parallel to
     * that list (a null entry means auto layout). Used by {@link DataMenuAbility}, whose sub-buttons mix
     * abilities and building-place buttons and are therefore built on demand.
     */
    public java.util.List<int[]> getSubButtonPositions(Unit unit) {
        return null;
    }

    /** Adds a sub-ability and returns this ability so menus can be built fluently. */
    public Ability addSubAbility(Ability subAbility) {
        subAbilities.add(subAbility);
        return this;
    }

    public boolean usesCharges() {
        return maxCharges > 1;
    }

    protected void toggleAutocast(Unit unit) {
        if (!((Entity) unit).level().isClientSide())
            return;

        if (isAutocasting(unit) && autocastDisableAction != null) {
            sendUnitCommand(autocastDisableAction);
        } else if (!isAutocasting(unit) && autocastEnableAction != null) {
            sendUnitCommand(autocastEnableAction);
        }
    }

    protected void toggleAutocast(BuildingPlacement placement) {
        if (!placement.level.isClientSide())
            return;

        if (isAutocasting(placement) && autocastDisableAction != null) {
            sendUnitCommand(autocastDisableAction);
        } else if (!isAutocasting(placement) && autocastEnableAction != null) {
            sendUnitCommand(autocastEnableAction);
        }
    }

    public boolean isCasting(Unit unit) { return false; }

    public float getCooldown(Unit unit) { return unit.getCooldown(this); }
    public float getCooldown(BuildingPlacement placement) { return placement.getCooldown(this); }

    public boolean isOffCooldown(Unit unit) { return getCooldown(unit) <= 0 || (usesCharges() && getCharges(unit) > 0); }
    public boolean isOffCooldown(BuildingPlacement placement) { return getCooldown(placement) <= 0 || (usesCharges() && getCharges(placement) > 0); }

    public void setToMaxCooldown(Unit unit) {
        unit.setCooldown(this, cooldownMax);
        if (usesCharges() && unit.getCharges(this) > 0)
            unit.setCharges(this, unit.getCharges(this) - 1);
    }

    public void setToMaxCooldown(BuildingPlacement building) {
        building.setCooldown(this, cooldownMax);
        if (usesCharges() && building.getCharges(this) > 0)
            building.setCharges(this, building.getCharges(this) - 1);
    }

    public void setCooldown(float cooldown, Unit unit) {
        this.setCooldown(cooldown, true, unit);
    }

    public void setCooldown(float cooldown, boolean useCharge, Unit unit) {
        if (((Entity) unit).level().isClientSide() && cooldown > 0) {
            HudClientEvents.setLowestCdHudEntity();
        }
        unit.setCooldown(this, Math.min(cooldown, cooldownMax));
        if (useCharge && usesCharges() && unit.getCharges(this) > 0)
            unit.setCharges(this, unit.getCharges(this) - 1);
    }

    public void use(Level level, Unit unitUsing, LivingEntity targetEntity) { }

    public void use(Level level, BuildingPlacement buildingUsing, LivingEntity targetEntity) { }

    public void use(Level level, Unit unitUsing, BlockPos targetBp) { }

    /** Area variant of {@link #use}: two opposite corners of an outlined region (plan §14.4). */
    public void useArea(Level level, Unit unitUsing, BlockPos corner1, BlockPos corner2) {
        use(level, unitUsing, corner1);
    }

    public void use(Level level, BuildingPlacement buildingUsing, BlockPos targetBp) { }

    /**
     * Per-tick hook for passive abilities ({@link #passive}), driven from {@code Unit.tick} on the
     * server. Passive abilities have no button and are never clicked; behaviour that should happen
     * continuously (regeneration, auras, on-interval buffs) goes here.
     */
    public void tickPassive(Unit unit) { }

    // assigns a default hotkey
    public AbilityButton getButton(BuildingPlacement placement) {
        return getButton(defaultHotkey, placement);
    }

    public AbilityButton getButton(Unit unit) {
        return getButton(defaultHotkey, unit);
    }

    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        return null;
    }
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        return null;
    }

    public boolean canBypassCooldown(Unit unit) { return usesCharges() && getCharges(unit) > 0; }
    public boolean canBypassCooldown(BuildingPlacement buildingPlacement) { return usesCharges() && getCharges(buildingPlacement) > 0; }

    public boolean shouldResetBehaviours() { return true; }

    protected void setDefaultAutocast(boolean b) {
        defaultAutocast = b;
    }

    public boolean isDefaultAutocast() {
        return defaultAutocast;
    }

    public int getCharges(Unit unit) {
        return unit.getCharges(this);
    }

    public int getCharges(BuildingPlacement placement) {
        return placement.getCharges(this);
    }

    public void setCharges(Unit unit, int charges) {
        unit.setCharges(this, charges);
    }
}
