package com.solegendary.reignofnether.research;

import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.DefinedUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;

import java.util.Locale;

/**
 * Applies the equipment granted by {@link ResearchType#EQUIP} researches to the owner's units.
 *
 * <p>Idempotent (an already-present item is left alone) and server-side only; the resulting equipment
 * reaches clients through the normal entity-equipment sync. Revoking a research does not strip the
 * item back off - there is no way to know whether the unit had it for another reason - so treat
 * equipment grants as one-way.
 */
public final class ResearchEquipApplier {

    private ResearchEquipApplier() { }

    /** Gives every matching unit of this unit's owner the researched equipment. */
    public static void applyFor(LevelAccessor level, Unit unit) {
        if (level.isClientSide())
            return;
        String ownerName = unit.getOwnerName();
        if (ownerName == null || ownerName.isEmpty())
            return;

        LivingEntity living = (LivingEntity) unit;
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(((Entity) unit).getType());
        ResourceLocation definitionId = unit instanceof DefinedUnit defined ? defined.getUnitDefinitionId() : null;

        for (Research research : ResearchRegistry.all()) {
            if (research.getType() != ResearchType.EQUIP)
                continue;
            if (!ResearchUtils.isResearched(level, ownerName, research.getId()))
                continue;
            for (ResearchEquipModifier mod : research.getEquipModifiers()) {
                if (mod.unitFilter() != null
                        && !mod.unitFilter().equals(typeId)
                        && !mod.unitFilter().equals(definitionId))
                    continue;
                Item item = BuiltInRegistries.ITEM.get(mod.itemId());
                if (item == null || item == Items.AIR)
                    continue;
                EquipmentSlot slot = slotFromName(mod.slot());
                ItemStack current = living.getItemBySlot(slot);
                if (current.is(item))
                    continue;
                living.setItemSlot(slot, new ItemStack(item));
            }
        }
    }

    /** Re-applies owned equipment grants for all of one owner's units (call after grant). */
    public static void refreshForOwner(LevelAccessor level, String ownerName) {
        if (level.isClientSide() || ownerName == null || ownerName.isEmpty())
            return;
        for (LivingEntity entity : UnitServerEvents.getAllUnits())
            if (entity instanceof Unit unit && unit.isRtsUnit() && ownerName.equals(unit.getOwnerName()))
                applyFor(level, unit);
    }

    private static EquipmentSlot slotFromName(String name) {
        if (name == null)
            return EquipmentSlot.MAINHAND;
        return switch (name.trim().toLowerCase(Locale.ROOT)) {
            case "offhand" -> EquipmentSlot.OFFHAND;
            case "head" -> EquipmentSlot.HEAD;
            case "chest" -> EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS;
            case "feet" -> EquipmentSlot.FEET;
            default -> EquipmentSlot.MAINHAND;
        };
    }
}
