package com.solegendary.reignofnether.unit.interfaces;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A mob that carries a data-driven unit definition (plan `CONTENT_JSON_PLAN.md`). Implemented by
 * {@code UnitMobMixin}; the presence of a definition id is what makes a plain mob an RTS unit.
 */
public interface DefinedUnit {

    @Nullable
    ResourceLocation getUnitDefinitionId();

    void setUnitDefinitionId(@Nullable ResourceLocation id);
}
