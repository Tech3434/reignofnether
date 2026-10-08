package com.solegendary.reignofnether.unit.interfaces;

import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.unit.UnitAnimationAction;
import com.solegendary.reignofnether.unit.goals.BuildRepairGoal;
import com.solegendary.reignofnether.unit.goals.ExploreBuildLocationGoal;
import com.solegendary.reignofnether.unit.goals.GatherResourcesGoal;
import com.solegendary.reignofnether.unit.packets.UnitAnimationClientboundPacket;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface WorkerUnit extends Unit {

    public static void tick(WorkerUnit unit) {
        BuildRepairGoal buildRepairGoal = unit.getBuildRepairGoal();
        if (buildRepairGoal != null)
            buildRepairGoal.tick();
        GatherResourcesGoal gatherResourcesGoal = unit.getGatherResourceGoal();
        if (gatherResourcesGoal != null)
            gatherResourcesGoal.tick();

        LivingEntity entity = (LivingEntity) unit;
        ItemStack mainHandItem = entity.getItemBySlot(EquipmentSlot.MAINHAND);

        if (unit.getBuildRepairGoal().isBuilding()) {
            if (!mainHandItem.is(Items.IRON_SHOVEL)) {
                entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SHOVEL));
            }
        }
        else if (unit.getGatherResourceGoal().isGathering()) {
            switch (unit.getGatherResourceGoal().getTargetResourceName()) {
                case FOOD -> {
                    if (!mainHandItem.is(Items.IRON_HOE)) {
                        entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_HOE));
                    }
                }
                case WOOD -> {
                    if (!mainHandItem.is(Items.IRON_AXE)) {
                        entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
                    }
                }
                case ORE -> {
                    if (!mainHandItem.is(Items.IRON_PICKAXE)) {
                        entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
                    }
                }
                case NONE -> {
                    if (!mainHandItem.is(Items.AIR))
                        entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.AIR));
                }
            }
        } else if (entity instanceof AttackerUnit attackerUnit &&
                ((Unit) entity).getTargetGoal().getTarget() != null) {
            if (!mainHandItem.is(Items.WOODEN_SWORD)) {
                entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
                if (!entity.level().isClientSide())
                    UnitAnimationClientboundPacket.sendEntityPacket(UnitAnimationAction.NON_KEYFRAME_START, entity, ((Unit) entity).getTargetGoal().getTarget());
            }
        } else {
            if (!mainHandItem.is(Items.AIR)) {
                entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.AIR));
                if (!entity.level().isClientSide())
                    UnitAnimationClientboundPacket.sendBasicPacket(UnitAnimationAction.NON_KEYFRAME_STOP, entity);
            }
        }
    }

    public static void resetBehaviours(WorkerUnit unit) {
        unit.getBuildRepairGoal().stopBuilding();
        unit.getGatherResourceGoal().stopGathering();
        unit.getExploreBuildLocationGoal().reset();
    }

    public static void resetBehavioursExceptExploreBuild(WorkerUnit unit) {
        unit.getBuildRepairGoal().stopBuilding();
        unit.getGatherResourceGoal().stopGathering();
    }

    // only properly works serverside - clientside requires packet updates
    public static boolean isIdle(WorkerUnit unit) {
        GatherResourcesGoal resGoal = unit.getGatherResourceGoal();

        boolean isMoving = !((Mob) unit).getNavigation().isDone();
        boolean isGathering = resGoal.isGathering();
        boolean isGatheringIdle = resGoal.isIdle();
        boolean isBuilding = unit.getBuildRepairGoal().getBuildingTarget() != null;
        boolean isFarming = resGoal.isFarming();
        boolean isAttacking = ((Unit) unit).getTargetGoal().getTarget() != null;

        return !isMoving && !isGathering && !isBuilding && isGatheringIdle && !isAttacking && !isFarming;
    }
}
