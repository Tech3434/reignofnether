package com.solegendary.reignofnether.unit;

import com.solegendary.reignofnether.unit.interfaces.DefinedUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Applies a {@link UnitDefinition} to a mob: attributes, scale, ownership, definition id and (via
 * {@link Unit#initialiseGoals()}) its goals. This is how data-driven units come into the world.
 */
public final class UnitDefinitionRuntime {

    private UnitDefinitionRuntime() { }

    public static void applyAttributes(UnitDefinition def, Mob mob) {
        for (Map.Entry<ResourceLocation, Double> entry : def.attributes().entrySet()) {
            Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE.getHolder(entry.getKey()).orElse(null);
            if (attribute == null)
                continue;
            AttributeInstance instance = mob.getAttribute(attribute);
            if (instance != null)
                instance.setBaseValue(entry.getValue());
        }
        def.scale().ifPresent(scale -> {
            AttributeInstance instance = mob.getAttribute(Attributes.SCALE);
            if (instance != null)
                instance.setBaseValue(scale);
        });
    }

    /** Creates the definition's base mob, applies its data and goals, but does NOT add it to the world. */
    @Nullable
    public static Mob create(ServerLevel level, ResourceLocation definitionId, String ownerName) {
        UnitDefinition def = level.registryAccess().registryOrThrow(UnitDefinitions.UNIT_KEY).get(definitionId);
        if (def == null)
            return null;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(def.base());
        if (type == null)
            return null;

        Entity entity = type.create(level);
        if (!(entity instanceof Mob mob))
            return null;

        if (mob instanceof DefinedUnit definedUnit)
            definedUnit.setUnitDefinitionId(definitionId);
        if (mob instanceof Unit unit)
            unit.setOwnerName(ownerName);

        applyAttributes(def, mob);
        if (mob instanceof Unit unit)
            unit.initialiseGoals();
        return mob;
    }

    /** Creates the definition at {@code pos} and adds it to the world. */
    @Nullable
    public static Mob spawn(ServerLevel level, ResourceLocation definitionId, Vec3 pos, String ownerName) {
        Mob mob = create(level, definitionId, ownerName);
        if (mob != null) {
            mob.moveTo(pos.x, pos.y, pos.z);
            level.addFreshEntity(mob);
        }
        return mob;
    }
}
