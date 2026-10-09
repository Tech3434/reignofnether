package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Engine data-driven ability (plan CONTENT_JSON_PLAN.md): a self-cast heal, looked up by the type id
 * {@code reignofnether:heal} and configured from JSON, e.g.
 * {@code { "type": "reignofnether:heal", "cooldown": 200, "params": { "amount": 15 } }}.
 *
 * <p>It is a plain {@link Ability} dispatched by its {@link UnitAction}: the generic branch of
 * {@code UnitActionItem} finds the ability whose action matches and calls
 * {@link #use(Level, Unit, BlockPos)} on both sides, so the button only has to send the action.
 */
public class SimpleHealAbility extends Ability {

    private final float amount;

    public SimpleHealAbility(AbilitySpec spec) {
        super(UnitAction.HEAL_SELF, Math.round(spec.cooldown()), 0, 0, false);
        this.amount = (float) spec.param("amount", 20);
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        if (!(unitUsing instanceof LivingEntity living))
            return;
        if (level.isClientSide()) {
            // server owns the actual heal; the client only needs the cooldown for the HUD
            setToMaxCooldown(unitUsing);
            return;
        }
        if (living.isAlive()) {
            living.heal(amount);
            setToMaxCooldown(unitUsing);
        }
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        String name = "abilities.reignofnether.heal";
        return new AbilityButton(
                I18n.get(name),
                ResourceLocation.withDefaultNamespace("textures/item/golden_apple.png"),
                hotkey,
                () -> false,
                () -> false,
                () -> true,
                () -> UnitClientEvents.sendUnitCommand(action),
                null,
                List.of(FormattedCharSequence.forward(
                        I18n.get("abilities.reignofnether.heal.tooltip1", Math.round(amount)), Style.EMPTY)),
                this,
                unit
        );
    }
}
