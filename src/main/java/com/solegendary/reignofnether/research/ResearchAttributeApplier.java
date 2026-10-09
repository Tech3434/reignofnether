package com.solegendary.reignofnether.research;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.DefinedUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.LevelAccessor;

/**
 * Applies (and removes) the attribute boosts granted by researched {@code ATTRIBUTE_BOOST} researches.
 *
 * <p>Modifiers are named after the research, so applying twice is idempotent and revoking a research
 * can remove exactly what it granted. Server-side only; the client gets the resulting attribute values
 * through normal entity attribute sync.
 */
public final class ResearchAttributeApplier {

    private ResearchAttributeApplier() { }

    private static ResourceLocation modifierId(Research research) {
        return ResourceLocation.fromNamespaceAndPath(research.getId().getNamespace(),
                "research/" + research.getId().getPath());
    }

    /** Adds every researched attribute boost that applies to this unit. Idempotent. */
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
            if (research.getType() != ResearchType.ATTRIBUTE_BOOST)
                continue;
            if (!ResearchUtils.isResearched(level, ownerName, research.getId()))
                continue;
            for (ResearchAttributeModifier mod : research.getAttributeModifiers()) {
                if (mod.unitFilter() != null
                        && !mod.unitFilter().equals(typeId)
                        && !mod.unitFilter().equals(definitionId))
                    continue;
                Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE.getHolder(mod.attributeId()).orElse(null);
                if (attribute == null) {
                    ReignOfNether.LOGGER.warn("Research '{}' boosts unknown attribute '{}'",
                            research.getId(), mod.attributeId());
                    continue;
                }
                AttributeInstance instance = living.getAttribute(attribute);
                if (instance == null)
                    continue;
                ResourceLocation id = modifierId(research);
                if (instance.getModifier(id) == null)
                    instance.addPermanentModifier(new AttributeModifier(id, mod.amount(),
                            AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }

    /** Removes every research-granted modifier from this unit. */
    public static void removeFor(Unit unit) {
        LivingEntity living = (LivingEntity) unit;
        for (Research research : ResearchRegistry.all()) {
            if (research.getType() != ResearchType.ATTRIBUTE_BOOST)
                continue;
            for (ResearchAttributeModifier mod : research.getAttributeModifiers()) {
                Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE.getHolder(mod.attributeId()).orElse(null);
                if (attribute == null)
                    continue;
                AttributeInstance instance = living.getAttribute(attribute);
                if (instance != null)
                    instance.removeModifier(modifierId(research));
            }
        }
    }

    /** Rebuilds every research-granted modifier for all of one owner's units (call after grant/revoke). */
    public static void refreshForOwner(LevelAccessor level, String ownerName) {
        if (level.isClientSide() || ownerName == null || ownerName.isEmpty())
            return;
        for (LivingEntity entity : UnitServerEvents.getAllUnits())
            if (entity instanceof Unit unit && unit.isRtsUnit() && ownerName.equals(unit.getOwnerName())) {
                removeFor(unit);
                applyFor(level, unit);
            }
    }
}
