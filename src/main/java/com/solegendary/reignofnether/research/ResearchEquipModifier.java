package com.solegendary.reignofnether.research;

import net.minecraft.resources.ResourceLocation;

/**
 * One piece of equipment an {@link ResearchType#EQUIP} research gives to its owner's units.
 *
 * <p>{@code slot} is an {@code EquipmentSlot} name ({@code mainhand}/{@code offhand}/{@code head}/
 * {@code chest}/{@code legs}/{@code feet}); anything unknown falls back to {@code mainhand}.
 * {@code unitFilter} (nullable) limits the effect to one unit: an {@code EntityType} id for vanilla
 * bodies, or a unit-definition id for data-driven units.
 */
public record ResearchEquipModifier(ResourceLocation itemId, ResourceLocation unitFilter, String slot) {

    public static final String DEFAULT_SLOT = "mainhand";

    public ResearchEquipModifier(ResourceLocation itemId, ResourceLocation unitFilter) {
        this(itemId, unitFilter, DEFAULT_SLOT);
    }

    public ResearchEquipModifier(ResourceLocation itemId) {
        this(itemId, null, DEFAULT_SLOT);
    }
}
