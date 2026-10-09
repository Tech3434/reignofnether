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

        def.equipment().ifPresent(itemId -> {
            net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item != null && item != net.minecraft.world.item.Items.AIR)
                mob.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                        new net.minecraft.world.item.ItemStack(item));
        });

        applyAttributes(def, mob);
        if (mob instanceof Unit unit)
            unit.initialiseGoals();
        if (mob instanceof Unit unit)
            buildAbilities(def, unit);
        return mob;
    }

    /** Instantiates the definition's data-driven abilities and adds them to the unit. */
    private static void buildAbilities(UnitDefinition def, Unit unit) {
        for (com.solegendary.reignofnether.ability.AbilitySpec spec : def.abilities()) {
            com.solegendary.reignofnether.ability.Ability ability =
                    com.solegendary.reignofnether.ability.AbilityTypes.create(spec, unit);
            if (ability == null)
                continue;
            if (spec.cooldown() > 0)
                ability.cooldownMax = Math.round(spec.cooldown());
            if (spec.range() > 0)
                ability.range = spec.range();
            if (spec.radius() > 0)
                ability.radius = spec.radius();
            if (spec.canTargetEntities())
                ability.canTargetEntities = true;
            if (spec.oneClickOneUse())
                ability.oneClickOneUse = true;
            ability.passive = spec.passive();
            if (ability instanceof com.solegendary.reignofnether.ability.HeroAbility heroAbility && spec.mana() > 0)
                heroAbility.manaCost = Math.round(spec.mana());
            ability.requiredResearch = spec.requiredResearch();
            unit.getAbilities().add(ability);
        }
        // a worker always gets the build menu (the buildings it can place)
        if (unit.isWorker()) {
            unit.getAbilities().add(new com.solegendary.reignofnether.ability.BuildMenuAbility(
                    "abilities.reignofnether.build_menu",
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                            com.solegendary.reignofnether.ReignOfNether.MOD_ID,
                            "textures/icons/blocks/repeating_command_block_back.png"),
                    com.solegendary.reignofnether.building.WorkerBuildMenu::buildButtons));
        }
        unit.updateAbilityButtons();
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
