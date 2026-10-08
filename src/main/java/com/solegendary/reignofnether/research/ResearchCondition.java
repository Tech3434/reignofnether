package com.solegendary.reignofnether.research;

import net.minecraft.resources.ResourceLocation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One requirement of a {@link Research}: another research that must (or, with {@code invert}, must
 * not) be completed before this one can be researched.
 *
 * <p>Inversion is a first-class primitive, not a special case - content can just as easily be gated
 * on the ABSENCE of a technology as on its presence.
 */
public record ResearchCondition(ResourceLocation researchId, boolean invert) {

    public static final Codec<ResearchCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("research").forGetter(ResearchCondition::researchId),
            Codec.BOOL.optionalFieldOf("invert", false).forGetter(ResearchCondition::invert)
    ).apply(instance, ResearchCondition::new));

    public static ResearchCondition of(ResourceLocation researchId) {
        return new ResearchCondition(researchId, false);
    }

    public static ResearchCondition not(ResourceLocation researchId) {
        return new ResearchCondition(researchId, true);
    }
}
