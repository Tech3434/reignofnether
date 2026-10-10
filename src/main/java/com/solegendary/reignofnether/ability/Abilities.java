package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.List;

public class Abilities {
    List<Pair<Ability, Keybinding>> abilities = new ArrayList<>();

    public Abilities() { }

    public Abilities(List<Pair<Ability, Keybinding>> abilities) {this.abilities = abilities;}

    public void add(Ability ability) {
        abilities.add(new Pair<>(ability, null));
    }

    public void add(Ability ability, Keybinding keybind) {
        abilities.add(new Pair<>(ability, keybind));
    }

    public List<AbilityButton> getButtons(BuildingPlacement placement) {
        List<Keybinding> keybindings = List.of(
                Keybindings.abilitySlot1,
                Keybindings.abilitySlot2,
                Keybindings.abilitySlot3,
                Keybindings.abilitySlot4,
                Keybindings.abilitySlot5,
                Keybindings.abilitySlot6,
                Keybindings.abilitySlot7,
                Keybindings.abilitySlot8
        );
        List<AbilityButton> buttons = new ArrayList<>();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            for (int i = 0; i < abilities.size(); i++) {
                Pair<Ability, Keybinding> ability = abilities.get(i);
                if (ability.getA().passive)
                    continue;
                AbilityButton button = ability.getA().getButton(ability.getB() != null ? ability.getB() : keybindings.get(i) , placement);
                if (button != null) {
                    applyResearchGate(button, ability.getA(), placement.level, placement.ownerName);
                    buttons.add(button);
                }
            }
        }
        return buttons;
    }

    public List<Button> getButtons(Unit unit) {
        List<Keybinding> keybindings = List.of(
                Keybindings.abilitySlot1,
                Keybindings.abilitySlot2,
                Keybindings.abilitySlot3,
                Keybindings.abilitySlot4,
                Keybindings.abilitySlot5,
                Keybindings.abilitySlot6,
                Keybindings.abilitySlot7,
                Keybindings.abilitySlot8
        );
        List<Button> buttons = new ArrayList<>();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            for (int i = 0; i < abilities.size(); i++) {
                Pair<Ability, Keybinding> ability = abilities.get(i);
                if (ability.getA().passive)
                    continue;
                Button button = ability.getA().getButton(ability.getB() != null ? ability.getB() : keybindings.get(i) , unit);
                if (button != null) {
                    applyResearchGate(button, ability.getA(),
                            ((net.minecraft.world.entity.Entity) unit).level(), unit.getOwnerName());
                    buttons.add(button);
                }
            }
        }
        return buttons;
    }

    /**
     * Disables a freshly built ability button while the owner has not satisfied the ability's
     * required researches. Server-side use is re-checked independently, so this is presentation only.
     * Public because menus that build their sub-buttons outside this class (e.g. {@link DataMenuAbility})
     * must apply the same gate to their entries.
     */
    public static void applyResearchGate(Button button, Ability ability,
                                         net.minecraft.world.level.Level level, String ownerName) {
        if (ability.requiredResearch.isEmpty())
            return;
        java.util.function.Supplier<Boolean> original = button.isEnabled;
        button.isEnabled = () -> (original == null || Boolean.TRUE.equals(original.get()))
                && ability.meetsResearch(level, ownerName);
    }

    /**
     * All abilities reachable from this list, including abilities nested inside menus, in menu order.
     * Server dispatch ({@code UnitActionItem}), passive ticking and cooldown bookkeeping all run over
     * this flat view, so an active ability placed inside a data menu still works. Cycles do not occur
     * in practice, but a visited set keeps this safe.
     */
    public List<Ability> get() {
        var list = new ArrayList<Ability>();
        for (Pair<Ability, Keybinding> ability : abilities) {
            collect(ability.getA(), list, new java.util.IdentityHashMap<>());
        }
        return list;
    }

    private static void collect(Ability ability, List<Ability> out, java.util.Map<Ability, Boolean> seen) {
        if (ability == null || seen.put(ability, Boolean.TRUE) != null)
            return;
        out.add(ability);
        for (Ability sub : ability.subAbilities)
            collect(sub, out, seen);
    }

    public Ability getDefaultAutocast() {
        for (Pair<Ability, Keybinding> ability:abilities) {
            if (ability.getA().isDefaultAutocast())
                return ability.getA();
        }
        return null;
    }

    public Abilities clone() {
        return new Abilities(new ArrayList<>(abilities));
    }

    public boolean isEmpty() {
        return abilities.isEmpty();
    }
}
