package com.solegendary.reignofnether.research;

import net.minecraft.resources.ResourceLocation;

/**
 * One attribute boost a research grants to its owner's units.
 *
 * <p>Phase 3 resolves {@code attributeId} against {@code BuiltInRegistries.ATTRIBUTE} and applies it;
 * {@code unitFilter} (nullable) limits the effect to a single unit type.
 */
public record ResearchAttributeModifier(ResourceLocation attributeId, double amount,
                                        ResourceLocation unitFilter) {

    public ResearchAttributeModifier(ResourceLocation attributeId, double amount) {
        this(attributeId, amount, null);
    }
}
