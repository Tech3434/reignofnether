package com.solegendary.reignofnether.unit.interfaces;

import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.addon.GarrisonableBuildingAddon;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.unit.EnemySearchBehaviour;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.goals.*;

import com.solegendary.reignofnether.util.MobEffectHelpers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import javax.annotation.Nullable;

/**
 * Attacker helper statics. The instance contract (goal accessors, damage/range defaults) was collapsed
 * into {@link Unit} (plan CONTENT_JSON_PLAN.md); what is left are the statics used by attacker ticks.
 */
public interface AttackerUnit extends Unit {

    static void resetBehaviours(AttackerUnit unit) {
        unit.setUnitAttackTarget(null);
        unit.setAttackMoveTarget(null);

        Goal attackGoal = unit.getAttackGoal();
        if (attackGoal instanceof MeleeWindupAttackUnitGoal mwaug)
            mwaug.resetWindup();

        Goal attackBuildingGoal = unit.getAttackBuildingGoal();
        if (attackBuildingGoal instanceof RangedAttackBuildingGoal<?> rabg)
            rabg.stop();
        else if (attackBuildingGoal instanceof MeleeAttackBuildingGoal mabg)
            mabg.stopAttacking();

        unit.setEnemySearchBehaviour(EnemySearchBehaviour.NONE);
    }

    static boolean isAttackingBuilding(AttackerUnit attackerUnit) {
        boolean isAttackingBuilding = false;
        Goal attackBuildingGoal = attackerUnit.getAttackBuildingGoal();
        if (attackBuildingGoal instanceof RangedAttackBuildingGoal<?> rabg)
            isAttackingBuilding = rabg.getBuildingTarget() != null;
        else if (attackBuildingGoal instanceof MeleeAttackBuildingGoal mabg)
            isAttackingBuilding = mabg.getBuildingTarget() != null;
        return isAttackingBuilding;
    }

    static void tick(AttackerUnit attackerUnit) {
        Mob unitMob = (Mob) attackerUnit;
        Unit unit = (Unit) attackerUnit;

        if (!unitMob.level().isClientSide) {
            if (attackerUnit.getAttackGoal() instanceof AbstractMeleeAttackUnitGoal meleeAttackUnitGoal) {
                meleeAttackUnitGoal.tickAttackCooldown();
                // doesn't tick on its own for some reason?
                if (((Mob) attackerUnit).isVehicle())
                    meleeAttackUnitGoal.tick();
                if (meleeAttackUnitGoal instanceof MeleeWindupAttackUnitGoal goal)
                    goal.checkAndPerformAttackWithWindup();
            }
            else if (attackerUnit.getAttackGoal() instanceof UnitRangedAttackGoal rangedAttackGoal)
                rangedAttackGoal.tickAttackCooldown();
            else if (attackerUnit.getAttackGoal() instanceof UnitBowAttackGoal rangedAttackGoal)
                rangedAttackGoal.tickAttackCooldown();

            if (attackerUnit.getAttackBuildingGoal() != null && attackerUnit.canAttackBuildings())
                attackerUnit.getAttackBuildingGoal().tick();
        }

        if (!unitMob.level().isClientSide && unitMob.tickCount % 4 == 0) {
            if (((LivingEntity) unit).getEffect(MobEffectHelpers.holder(MobEffectRegistrar.STUN.get())) != null ||
                ((LivingEntity) unit).getEffect(MobEffectHelpers.holder(MobEffectRegistrar.FREEZE.get())) != null) {
                Unit.fullResetBehaviours(unit);
                return;
            }

            boolean isAttackingBuilding = isAttackingBuilding(attackerUnit);

            // enact attack moving
            // prioritises units and will chase them, resuming attack move once dead or out of range/sight
            if (attackerUnit.getAttackMoveTarget() != null && !unit.hasLivingTarget() && !isAttackingBuilding) {
                attackerUnit.attackClosestEnemy((ServerLevel) unitMob.level());

                if (unit.getTargetGoal().getTarget() == null &&
                    unit.getMoveGoal().getMoveTarget() == null &&
                    !isAttackingBuilding(attackerUnit))
                    unit.setMoveTarget(attackerUnit.getAttackMoveTarget());
            }

            boolean isCasting = unit.isCasting();
            boolean forced1 = ((Unit) attackerUnit).getTargetGoal().forced;
            Goal attackBuildingGoal = attackerUnit.getAttackBuildingGoal();
            boolean forced2 = attackBuildingGoal instanceof RangedAttackBuildingGoal<?> rabg && rabg.forced;
            boolean forced3 = attackBuildingGoal instanceof MeleeAttackBuildingGoal mabg && mabg.forced;
            boolean forced = forced1 || forced2 || forced3;

            // retaliate against a mob that damaged us UNLESS already on another command
            if (unitMob.getLastDamageSource() != null &&
                    attackerUnit.getWillRetaliate() &&
                    unit.getTargetGoal().getTarget() == null &&
                    !isCasting && (unit.isIdle() || (isAttackingBuilding && !forced))) {

                Entity lastDSEntity = unitMob.getLastDamageSource().getEntity();

                boolean isMeleeAttackedByFlyingOrGarrisoned = false;
                Relationship rs = UnitServerEvents.getUnitToEntityRelationship(unit, lastDSEntity);

                if (!isMeleeAttackedByFlyingOrGarrisoned &&
                    lastDSEntity instanceof LivingEntity &&
                    !(lastDSEntity instanceof Player player && player.isCreative()) &&
                    (rs == Relationship.NEUTRAL || rs == Relationship.HOSTILE)) {

                    attackerUnit.setUnitAttackTarget((LivingEntity) lastDSEntity);
                }
            }
            // idle auto-aggression
            if (unit.isIdle() && !isCasting && attackerUnit.getAggressiveWhenIdle())
                attackerUnit.attackClosestEnemy((ServerLevel) unitMob.level());

            // if attacking another unit as melee, retarget the closest unit periodically unless forced on a target
            if (!forced) {
                attackerUnit.retargetToClosestUnit((ServerLevel) unitMob.level());
            }
        }

        if (!unitMob.level().isClientSide && unitMob.tickCount % 40 == 0) {
            if (attackerUnit.getAttackMoveTarget() != null && attackerUnit.getEnemySearchBehaviour() == EnemySearchBehaviour.NONE) {
                boolean hasNoTargets = ((Unit) attackerUnit).getTargetGoal().getTarget() == null;
                if (attackerUnit.getAttackBuildingGoal() instanceof MeleeAttackBuildingGoal mabg && mabg.getBuildingTarget() != null)
                    hasNoTargets = false;
                else if (attackerUnit.getAttackBuildingGoal() instanceof RangedAttackBuildingGoal<?> rabg && rabg.getBuildingTarget() != null)
                    hasNoTargets = false;
                if (hasNoTargets && unitMob.distanceToSqr(attackerUnit.getAttackMoveTarget().getCenter()) < 4)
                    attackerUnit.setAttackMoveTarget(null);
            }
            if (attackerUnit.getAttackMoveTarget() == null || unit.isIdle()) {
                switch (attackerUnit.getEnemySearchBehaviour()) {
                    case NEAREST_ENEMY_BUILDING -> attackerUnit.attackMoveNearestEnemyBuilding();
                    case NEAREST_ENEMY_UNIT -> attackerUnit.attackMoveNearestEnemyUnit(false);
                    case NEAREST_ENEMY_WORKER -> attackerUnit.attackMoveNearestEnemyUnit(true);
                }
            }
        }
    }

    static double getWeaponDamageModifier(AttackerUnit attackerUnit) {
        ItemStack itemStack = ((LivingEntity) attackerUnit).getItemBySlot(EquipmentSlot.MAINHAND);

        if (!itemStack.isEmpty()) {
            ItemAttributeModifiers mods = itemStack.get(DataComponents.ATTRIBUTE_MODIFIERS);
            if (mods == null) return 0;
            for (ItemAttributeModifiers.Entry entry : mods.modifiers()) {
                boolean applies = entry.slot() == EquipmentSlotGroup.MAINHAND || entry.slot() == EquipmentSlotGroup.HAND;
                if (applies && entry.attribute().is(Attributes.ATTACK_DAMAGE)
                        && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE)
                    return entry.modifier().amount();
            }
        }
        return 0;
    }
}
