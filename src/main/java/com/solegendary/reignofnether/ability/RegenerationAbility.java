package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;

import net.minecraft.world.entity.LivingEntity;

/**
 * Engine data-driven passive ability (plan CONTENT_JSON_PLAN.md): heals its owner by {@code amount}
 * every {@code interval} ticks, e.g.
 * {@code { "type": "reignofnether:regeneration", "params": { "amount": 1, "interval": 40 } }}.
 *
 * <p>It is {@link #passive} — no button — and runs on the server via {@link #tickPassive(Unit)},
 * driven from {@code Unit.tick}.
 */
public class RegenerationAbility extends Ability {

    private final float amount;
    private final int interval;
    private int ticks;

    public RegenerationAbility(AbilitySpec spec) {
        super(UnitAction.NONE, 0, 0, 0, false);
        this.passive = true;
        this.amount = (float) spec.param("amount", 1);
        this.interval = Math.max(1, (int) spec.param("interval", 40));
    }

    @Override
    public void tickPassive(Unit unit) {
        if (++ticks < interval)
            return;
        ticks = 0;
        if (unit instanceof LivingEntity living && living.isAlive() && living.getHealth() < living.getMaxHealth())
            living.heal(amount);
    }
}
