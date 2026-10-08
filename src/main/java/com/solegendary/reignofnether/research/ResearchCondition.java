package com.solegendary.reignofnether.research;

import net.minecraft.resources.ResourceLocation;

/**
 * One requirement of a {@link Research}: another research that must (or, with {@code invert}, must
 * not) be completed before this one can be researched.
 *
 * <p>Inversion is a first-class primitive, not a special case - content can just as easily be gated
 * on the ABSENCE of a technology as on its presence.
 */
public record ResearchCondition(ResourceLocation researchId, boolean invert) {

    public static ResearchCondition of(ResourceLocation researchId) {
        return new ResearchCondition(researchId, false);
    }

    public static ResearchCondition not(ResourceLocation researchId) {
        return new ResearchCondition(researchId, true);
    }
}
