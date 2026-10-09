package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitDefinitionRuntime;
import com.solegendary.reignofnether.unit.interfaces.Unit;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Engine data-driven ability (plan CONTENT_JSON_PLAN.md): a self-cast summon. It shows the string/
 * resource param support - {@code unit} names a {@link com.solegendary.reignofnether.unit.UnitDefinition},
 * e.g. {@code { "type": "reignofnether:summon", "cooldown": 600,
 * "params": { "unit": "reignofnether:skeleton_unit", "count": 1 } }}.
 */
public class SummonUnitAbility extends Ability {

    private final ResourceLocation unitDefinitionId;
    private final int count;

    public SummonUnitAbility(AbilitySpec spec) {
        super(UnitAction.SUMMON_UNIT, Math.round(spec.cooldown()), 0, 0, false);
        this.unitDefinitionId = spec.resourceParam("unit");
        this.count = Math.max(1, (int) spec.param("count", 1));
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        if (level.isClientSide()) {
            setToMaxCooldown(unitUsing);
            return;
        }
        if (unitDefinitionId == null || !(level instanceof ServerLevel serverLevel) || !(unitUsing instanceof LivingEntity caster))
            return;

        String ownerName = unitUsing.getOwnerName();
        for (int i = 0; i < count; i++) {
            Mob summon = UnitDefinitionRuntime.create(serverLevel, unitDefinitionId, ownerName);
            if (summon == null)
                break;
            double offset = (i - (count - 1) / 2.0) * 1.5;
            summon.moveTo(caster.getX() + offset, caster.getY(), caster.getZ(), caster.getYRot(), 0);
            serverLevel.addFreshEntity(summon);
        }
        serverLevel.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.0F);
        setToMaxCooldown(unitUsing);
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        String name = "abilities.reignofnether.summon";
        return new AbilityButton(
                I18n.get(name),
                ResourceLocation.withDefaultNamespace("textures/item/skeleton_skull.png"),
                hotkey,
                () -> false,
                () -> false,
                () -> true,
                () -> UnitClientEvents.sendUnitCommand(action),
                null,
                List.of(FormattedCharSequence.forward(I18n.get(name), Style.EMPTY)),
                this,
                unit
        );
    }
}
