package com.solegendary.reignofnether.unit.interfaces;

import com.solegendary.reignofnether.util.AttributeHelpers;
import com.solegendary.reignofnether.util.MobEffectHelpers;
import com.solegendary.reignofnether.ability.Abilities;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.CommandAbilities;

import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.blocks.BlockServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.addon.GarrisonableBuildingAddon;

import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.debug.RtsDebugClientEvents;
import com.solegendary.reignofnether.debug.RtsDebugPathPreview;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.effecticons.MobEffectIcon;

import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.registrars.AttributeRegistrar;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.resources.*;

import com.solegendary.reignofnether.time.NightUtils;
import com.solegendary.reignofnether.unit.*;
import com.solegendary.reignofnether.unit.goals.*;
import com.solegendary.reignofnether.unit.packets.UnitAnimationClientboundPacket;
import com.solegendary.reignofnether.unit.packets.UnitSyncClientboundPacket;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.hud.TooltipColours;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.util.MyMath;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.registrars.ParticleRegistrar;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.util.ParticleUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.jetbrains.annotations.NotNull;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import com.solegendary.reignofnether.mixin.LivingEntityAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.ibm.icu.impl.ValidIdentifiers.Datatype.unit;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import net.minecraft.core.Holder;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.UnitStatType;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.unit.goals.SelectedTargetGoal;
import com.solegendary.reignofnether.unit.goals.ReturnResourcesGoal;
import com.solegendary.reignofnether.resources.Resources;

import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.goals.MoveToTargetBlockGoal;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.goals.GenericUntargetedSpellGoal;
import com.solegendary.reignofnether.unit.goals.GenericTargetedSpellGoal;
import com.solegendary.reignofnether.unit.goals.GatherResourcesGoal;
import com.solegendary.reignofnether.unit.goals.GarrisonGoal;
import com.solegendary.reignofnether.unit.goals.FlyingMoveToTargetGoal;
import net.minecraft.world.level.material.Fluid;

import com.solegendary.reignofnether.unit.Checkpoint;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import com.solegendary.reignofnether.unit.interfaces.Unit;

// Defines method bodies for Units
// workaround for trying to have units inherit from both their base vanilla Mob class and a Unit class
// Note that we can't write any default methods if they need to use Unit fields without a getter/setter
// (including getters/setters themselves)

public interface Unit {

    int DEFAULT_SIGHT_RANGE = 16;
    int ANCHOR_RETREAT_RANGE = 30;

    int NIGHT_SOURCE_HEALING_TICKS = 8 * ResourceCost.TICKS_PER_SECOND;

    // used for increasing pathfinding calculation range, default is 16 for most mobs
    int FOLLOW_RANGE_IMPROVED = 64;
    int FOLLOW_RANGE = 16;

    static Object2ObjectArrayMap<Ability, Float> createCooldownMap() {
        Object2ObjectArrayMap<Ability, Float> map = new Object2ObjectArrayMap<>();
        map.defaultReturnValue(0F);
        return map;
    }

    // position that neutral units run back to when past leash range
    void setAnchor(BlockPos bp);
    BlockPos getAnchor();

    static int getFollowRange() {
        return FOLLOW_RANGE_IMPROVED;
    }

    // list of positions to draw lines between to indicate unit intents - will fade over time unless shift is held
    ArrayList<Checkpoint> getCheckpoints();

    GarrisonGoal getGarrisonGoal();
    boolean canGarrison();


    Abilities getAbilities();

    /**
     * §14.2: generic orders a unit has because of what it is (attacker, worker, garrisonable),
     * expressed as abilities so the HUD has a single list of buttons per unit and building.
     */
    default List<Ability> getCommandAbilities() {
        List<Ability> commands = new ArrayList<>();
        if (Unit.isAttacker(this))
            commands.add(CommandAbilities.ATTACK);
        if (Unit.isWorker(this)) {
            commands.add(CommandAbilities.BUILD_REPAIR);
            commands.add(CommandAbilities.GATHER);
        }
        if (canGarrison() && getGarrison() == null)
            commands.add(CommandAbilities.GARRISON);
        else if (getGarrison() != null)
            commands.add(CommandAbilities.UNGARRISON);
        if (!(Unit.isWorker(this)))
            commands.add(CommandAbilities.HOLD);
        commands.add(CommandAbilities.STOP);
        return commands;
    }

    default List<Button> getAbilityButtons() {
        List<Button> buttons = new ArrayList<>(getAbilities().getButtons(this));
        for (Ability command : getCommandAbilities()) {
            Button button = command.getButton(this);
            if (button != null)
                buttons.add(button);
        }
        return buttons;
    }
    List<ItemStack> getItems();
    int getMaxResources();

    /**
     * Tool tier used by {@link com.solegendary.reignofnether.ability.DigAbility} to decide how fast
     * this unit breaks a block. It is a property of the unit and is iron by default; a unit may
     * override it.
     */
    default com.solegendary.reignofnether.ability.DigToolTier getDigToolTier() {
        return com.solegendary.reignofnether.ability.DigToolTier.IRON;
    }

    // note that attackGoal is specific to unit types
    MoveToTargetBlockGoal getMoveGoal();
    SelectedTargetGoal<?> getTargetGoal();
    ReturnResourcesGoal getReturnResourcesGoal();

    public default float getBaseMovementSpeed() {
        AttributeInstance attr = ((LivingEntity) this).getAttribute(Attributes.MOVEMENT_SPEED);
        return (float) (attr != null ?  attr.getBaseValue() : Attributes.MOVEMENT_SPEED.value().getDefaultValue());
    }
    public default float getMovementSpeed() {
        AttributeInstance attr = ((LivingEntity) this).getAttribute(Attributes.MOVEMENT_SPEED);
        float ms = (float) (attr != null ?  attr.getValue() : Attributes.MOVEMENT_SPEED.value().getDefaultValue());
        boolean isInWater = ((LivingEntity) this).isInWater();
        // 1.21.1 made LivingEntity#getWaterSlowDown protected; the invoker reads the value the
        // unit may have overridden (StriderUnit/DrownedUnit/etc. do).
        LivingEntity self = (LivingEntity) this;
        float slowdown = ((LivingEntityAccessor) self).reignOfNether$getWaterSlowDown();
        float waterSlowdown = slowdown * slowdown;
        return ms * (isInWater ? waterSlowdown : 1f);
    }
    public default float getUnitMaxHealth() {
        float bonus = 0;
        if (this instanceof Unit heroUnit && heroUnit.isHero()) {
            bonus = heroUnit.getHealthBonusPerLevel() * heroUnit.getHeroLevel();
        }
        AttributeInstance attr = ((LivingEntity) this).getAttribute(Attributes.MAX_HEALTH);
        return (float) (attr != null ?  attr.getValue() : Attributes.MAX_HEALTH.value().getDefaultValue()) + bonus;
    }
    public default int getSightRange() {
        AttributeInstance attr = ((LivingEntity) this).getAttribute(AttributeHelpers.holder(AttributeRegistrar.SIGHT_RANGE.get()));
        return (int) Math.round(attr != null ?  attr.getValue() : AttributeRegistrar.SIGHT_RANGE.get().getDefaultValue());
    }

    public ResourceCost getCost();

    LivingEntity getFollowTarget();
    boolean getHoldPosition();
    void setHoldPosition(boolean holdPosition);

    String getOwnerName();
    void setOwnerName(String name);

    String getOnDeathCommand();
    void setOnDeathCommand(String command);

    default double getDamageTakenIncrease() {
        MobEffectInstance mei = ((LivingEntity) this).getEffect(MobEffectHelpers.holder(MobEffectRegistrar.DAMAGE_TAKEN_INCREASE.get()));
        double value = mei == null ? 0 : (mei.getAmplifier() + 1) * 0.05d;
        return Math.round(value / 0.05d) * 0.05d;
    }

    // SOURCE: armour attribute, armour items and the damage amplifier debuff
    default double getUnitPhysicalArmorPercentage() {
        Mob mob = (Mob) this;
        // 1.21.1's CombatRules needs the hurt entity and the damage source; a generic source carries no
        // weapon, so it matches the old 1.20.1 arithmetic (no enchantment-based armour reduction).
        double dmgAfterAbsorb = CombatRules.getDamageAfterAbsorb(mob, 1f, mob.damageSources().generic(),
                mob.getArmorValue(), (float)mob.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        dmgAfterAbsorb += getDamageTakenIncrease();
        return Math.round((1 - dmgAfterAbsorb)/ 0.01d) * 0.01d;
    }

    // SOURCE: inherent unit stats and abilities
    default double getUnitRangedArmorPercentage() {
        AttributeInstance attr = ((LivingEntity) this).getAttribute(AttributeHelpers.holder(AttributeRegistrar.RANGED_DAMAGE_RESIST.get()));
        return (float) (attr != null ?  attr.getValue() : AttributeRegistrar.RANGED_DAMAGE_RESIST.get().getDefaultValue());
    }

    // SOURCE: inherent unit stats and vanilla mechanics (like resistance)
    default double getUnitMagicArmorPercentage() {
        AttributeInstance attr = ((LivingEntity) this).getAttribute(AttributeHelpers.holder(AttributeRegistrar.MAGIC_DAMAGE_RESIST.get()));
        return (float) (attr != null ?  attr.getValue() : AttributeRegistrar.MAGIC_DAMAGE_RESIST.get().getDefaultValue());
    }

    public default float getEvasionChance() {
        AttributeInstance attr = ((LivingEntity) this).getAttribute(AttributeHelpers.holder(AttributeRegistrar.EVASION_CHANCE.get()));
        return (float) (attr != null ?  attr.getValue() : AttributeRegistrar.EVASION_CHANCE.get().getDefaultValue());
    }

    // SOURCE: resistance mob effect
    default double getUnitResistPercentage() {
        Mob mob = (Mob) this;
        MobEffectInstance mei = mob.getEffect(MobEffects.DAMAGE_RESISTANCE);
        if (mei != null) {
            return (float) (0.2 * (mei.getAmplifier() + 1));
        } else {
            return 0;
        }
    }

    static void tick(Unit unit) {
        Mob unitMob = (Mob) unit;
        if (!unitMob.level().isClientSide() && unitMob.level() instanceof ServerLevel serverLevel) {
            ServerChunkCache chunkProvider = serverLevel.getChunkSource();

            BlockPos unitPos = unitMob.blockPosition();
            ChunkPos currentChunkPos = new ChunkPos(unitPos);

            // Load a 2-chunk radius around the unit
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    ChunkPos chunkPos = new ChunkPos(currentChunkPos.x + dx, currentChunkPos.z + dz);
                    chunkProvider.addRegionTicket(TicketType.FORCED, chunkPos, 2, chunkPos);
                }
            }
        }
        for (Map.Entry<Ability, Float> cooldownEntry : unit.getCooldowns().entrySet()) {
            Ability ability = cooldownEntry.getKey();
            float cooldown = cooldownEntry.getValue();
            if (cooldown > 0 || unit.getCharges(ability) < ability.maxCharges) {
                if (((Entity) unit).level().isClientSide())
                    unit.getCooldowns().put(ability, (float) (cooldown - (RtsDebugClientEvents.getCappedTPS() / 20D)));
                else
                    unit.getCooldowns().put(ability, cooldown - 1);

                if (cooldown <= 0 && ability.usesCharges() && unit.getCharges(ability) < ability.maxCharges) {
                    unit.setCharges(ability, unit.getCharges(ability) + 1);
                    if (unit.getCharges(ability) < ability.maxCharges)
                        unit.getCooldowns().put(ability, ability.cooldownMax);
                    if (unit.getCharges(ability) > ability.maxCharges)
                        unit.setCharges(ability, ability.maxCharges);
                }
            }
        }

        // passive abilities tick themselves (server-authoritative)
        if (!unitMob.level().isClientSide()) {
            for (Ability ability : unit.getAbilities().get())
                if (ability.passive)
                    ability.tickPassive(unit);
        }

        // ------------- CHECKPOINT LOGIC ------------- //
        if (unitMob.level().isClientSide()) {

            unit.getCheckpoints().removeIf(c -> (c.isForEntity() && !c.entity.isAlive()) || c.ticksLeft <= 0);

            for (Checkpoint cp : unit.getCheckpoints()) {
                cp.tick();
                boolean buildingIsDone = false;
                if (Unit.isWorker(unit) && !cp.isForEntity()) {
                    if (cp.placement != null && cp.placement.isBuilt && cp.placement.getHealth() >= cp.placement.getMaxHealth())
                        buildingIsDone = true;
                }
                if (cp.isGreen) {
                    if (((Mob) unit).getOnPos().distToCenterSqr(cp.getPos()) < 4f || buildingIsDone)
                        cp.startFading();
                } else if (cp.isForEntity() && !cp.entity.isAlive()) {
                    cp.startFading();
                }
            }
        } else {
            checkAndPickupResources(unit);
            checkAndPickupEquipment(unit);

            // sync target variables between goals and Mob
            if (unit.getTargetGoal().getTarget() == null || !unit.getTargetGoal().getTarget().isAlive() ||
                    unitMob.getTarget() == null || !unitMob.getTarget().isAlive()) {
                unitMob.setTarget(null);
                unit.getTargetGoal().setTarget(null);
            }

            // no iframes after being damaged so multiple units can attack at once
            unitMob.invulnerableTime = 0;

            // enact target-following, and stop followTarget being reset
            if (unit.getFollowTarget() != null && unitMob.tickCount % 20 == 0)
                unit.setMoveTarget(unit.getFollowTarget().blockPosition());
        }

        // slow regen for any unit standing in a night source's aura at night
        LivingEntity le = (LivingEntity) unit;

        if (!le.level().isClientSide() && !le.level().isDay() &&
                (le.tickCount % NIGHT_SOURCE_HEALING_TICKS == 0 ||
                 ((le.tickCount + NIGHT_SOURCE_HEALING_TICKS / 2) % NIGHT_SOURCE_HEALING_TICKS == 0 &&
                  NightUtils.isInRangeOfNightSource(le.position(), false)))) {
            le.heal(1);
        }

        // H.6: a unit outside the world border used to be killed outright, so on a server with a
        // border an army simply died out at the edge. Push it back inside instead.
        WorldBorder unitBorder = le.level().getWorldBorder();
        if (!unitBorder.isWithinBounds(le.getOnPos())) {
            double borderMargin = 2.0D;
            double minBorderX = unitBorder.getMinX() + borderMargin;
            double maxBorderX = unitBorder.getMaxX() - borderMargin;
            double minBorderZ = unitBorder.getMinZ() + borderMargin;
            double maxBorderZ = unitBorder.getMaxZ() - borderMargin;
            if (maxBorderX > minBorderX && maxBorderZ > minBorderZ)
                le.setPos(Mth.clamp(le.getX(), minBorderX, maxBorderX), le.getY(), Mth.clamp(le.getZ(), minBorderZ, maxBorderZ));
            else
                le.kill(); // degenerate border, nothing to clamp to
        }

        if (unitMob.tickCount % 50 == 0)
            checkAndRetreatToAnchor(unit);

        if (unit.getSunlightEffect() == SunlightEffect.SLOWNESS_II ||
            unit.getSunlightEffect() == SunlightEffect.SLOWNESS_I ||
            unit.getSunlightEffect() == SunlightEffect.SLOWNESS_MINOR) {
            // apply slowness during daytime for a short time repeatedly
            if (unitMob.tickCount % 10 == 0 && !unitMob.level().isClientSide() && unitMob.level().isDay() &&
                    !NightUtils.isInRangeOfNightSource(unitMob.getEyePosition(), false)) {

                if (unit.getSunlightEffect() == SunlightEffect.SLOWNESS_MINOR) {
                    unitMob.addEffect(MobEffectHelpers.instance(MobEffectRegistrar.MINOR_MOVEMENT_SLOWDOWN.get(), 15, 1));
                } else {
                    unitMob.addEffect(MobEffectHelpers.instance(MobEffects.MOVEMENT_SLOWDOWN, 15,
                            unit.getSunlightEffect() == SunlightEffect.SLOWNESS_I ? 0 : 1
                    ));
                }
            }
        }

        if (unitMob.tickCount % 20 == 0) {
            if (unit.hasEffectWithDuration(MobEffectRegistrar.ANGRY.get())) {
                addParticlesAroundSelf(unit, ParticleTypes.ANGRY_VILLAGER);
            }
            if (unit.hasEffectWithDuration(MobEffectRegistrar.FEARFUL.get())) {
                addParticlesAroundSelf(unit, ParticleTypes.SCULK_SOUL);
            }
        }

        if (unitMob.tickCount % 10 == 0 &&
            !(Unit.isWorker(unit)) &&
            !unitMob.level().isClientSide() &&
            !unitMob.level().isDay() &&
            NightUtils.isInRangeOfNightSource(unitMob.getEyePosition(), false)) {
            unitMob.addEffect(MobEffectHelpers.instance(MobEffectRegistrar.MINOR_MOVEMENT_SPEED.get(), 15, 1, true, false));
        }

        // possible fix for units getting stuck randomly on rtsPathfinding
        /*
        if (unitMob.tickCount % 60 == 0 && BuildingUtils.isPosInsideAnyBuilding(unitMob.level().isClientSide(), unitMob.getOnPos())) {
            boolean bool1 = unitMob.getRandom().nextBoolean();
            boolean bool2 = unitMob.getRandom().nextBoolean();
            unitMob.push(0.005d * (bool1 ? -1 : 1), 0, 0.005d * (bool2 ? -1 : 1));
        }
         */

    }

    private static void checkAndPickupResources(Unit unit) {
        Mob unitMob = (Mob) unit;
        if (unitMob.canPickUpLoot()) {
            for (ItemEntity itementity : unitMob.level().getEntitiesOfClass(ItemEntity.class, unitMob.getBoundingBox().inflate(1, 0, 1))) {
                if (!itementity.isRemoved() && !itementity.getItem().isEmpty() && !itementity.hasPickUpDelay() && unitMob.isAlive()) {
                    if (!Unit.atMaxResources(unit)) {
                        ItemStack itemstack = itementity.getItem();
                        ResourceSource resBlock = ResourceSources.getFromItem(itemstack.getItem());
                        if (resBlock != null) {
                            while (!Unit.atMaxResources(unit) && itemstack.getCount() > 0) {
                                unitMob.onItemPickup(itementity);
                                unitMob.take(itementity, 1);
                                unit.getItems().add(new ItemStack(itemstack.getItem(), 1));
                                itemstack.setCount(itemstack.getCount() - 1);
                            }
                            if (itemstack.getCount() <= 0)
                                itementity.discard();

                            UnitSyncClientboundPacket.sendSyncResourcesPacket(unit);
                        }
                        if (Unit.atThresholdResources(unit) && unit instanceof Unit workerUnit && workerUnit.isWorker()) {
                            GatherResourcesGoal goal = workerUnit.getGatherResourceGoal();
                            if (goal != null && goal.getTargetResourceName() != ResourceName.NONE)
                                goal.saveAndReturnResources();
                        }
                    }
                }
            }
        }
    }

    private static void checkAndPickupEquipment(Unit unit) {
        Mob unitMob = (Mob) unit;
        for (ItemEntity itementity : unitMob.level().getEntitiesOfClass(ItemEntity.class, unitMob.getBoundingBox().inflate(1, 0, 1))) {
            Relationship rl = UnitServerEvents.getUnitToEntityRelationship(unit, itementity);
            if (rl != Relationship.HOSTILE) {
                if (tryPickingUpEquipment(unit, itementity))
                    break;
            }
        }
    }

    public static boolean tryPickingUpEquipment(Unit unit, ItemEntity itemEntity) {
        Mob unitMob = (Mob) unit;
        ItemStack itemstack = itemEntity.getItem();
        if (unit.canPickUpEquipment(itemstack) && !itemEntity.isRemoved() &&
                !itemstack.isEmpty() && !itemEntity.hasPickUpDelay() && unitMob.isAlive() &&
                (itemEntity.tickCount >= 100)) {
            unitMob.onItemPickup(itemEntity);
            unitMob.take(itemEntity, 1);
            unit.onPickupEquipment(itemstack);
            itemEntity.discard();
            return true;
        }
        return false;
    }

    default boolean canPickUpEquipment(ItemStack itemStack) { return false; }

    default void onPickupEquipment(ItemStack itemStack) { }

    // call from addAdditionalSaveData
    public default void addUnitSaveData(@NotNull CompoundTag pCompound) {
        pCompound.putString("ownerName", getOwnerName());
        if (this instanceof DefinedUnit definedUnit && definedUnit.getUnitDefinitionId() != null)
            pCompound.putString("unitDefinitionId", definedUnit.getUnitDefinitionId().toString());
        if (getAnchor() != null) {
            pCompound.putInt("anchorPosX", getAnchor().getX());
            pCompound.putInt("anchorPosY", getAnchor().getY());
            pCompound.putInt("anchorPosZ", getAnchor().getZ());
        }
        if (this instanceof Unit heroUnit && heroUnit.isHero())
            heroUnit.addHeroUnitSaveData(pCompound);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack itemStack = ((LivingEntity) this).getItemBySlot(slot);
            if (itemStack.getItem() != Items.AIR)
                pCompound.put(slot.name() + "Item", itemStack.save(((LivingEntity) this).level().registryAccess()));
        }
        pCompound.putString("onDeathCommand", getOnDeathCommand());
    }

    // call from readAdditionalSaveData
    public default void readUnitSaveData(@NotNull CompoundTag pCompound) {
        setOwnerName(pCompound.getString("ownerName"));
        if (this instanceof DefinedUnit definedUnit && pCompound.contains("unitDefinitionId")) {
            net.minecraft.resources.ResourceLocation id =
                    net.minecraft.resources.ResourceLocation.tryParse(pCompound.getString("unitDefinitionId"));
            if (id != null)
                definedUnit.setUnitDefinitionId(id);
        }
        BlockPos anchorPos = new BlockPos(
            pCompound.getInt("anchorPosX"),
            pCompound.getInt("anchorPosY"),
            pCompound.getInt("anchorPosZ")
        );
        if (!anchorPos.equals(new BlockPos(0,0,0))) {
            setAnchor(anchorPos);
        }
        if (this instanceof Unit heroUnit && heroUnit.isHero())
            heroUnit.readHeroUnitSaveData(pCompound);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            String keyName = slot.name() + "Item";
            if (pCompound.contains(keyName)) {
                CompoundTag itemNbt = (CompoundTag) pCompound.get(keyName);
                if (itemNbt != null) {
                    ((LivingEntity) this).setItemSlot(slot, ItemStack.parseOptional(((LivingEntity) this).level().registryAccess(), itemNbt));
                }
            }
        }
        setOnDeathCommand(pCompound.getString("onDeathCommand"));
    }

    public enum SunlightEffect {
        NONE,
        SLOWNESS_II,
        SLOWNESS_I,
        SLOWNESS_MINOR,
        FIRE
    }

    public default SunlightEffect getSunlightEffect() {
        return SunlightEffect.NONE;
    }

    static boolean hasAnchor(Unit unit) {
        return unit.getAnchor() != null && !unit.getAnchor().equals(new BlockPos(0,0,0));
    }

    private static void checkAndRetreatToAnchor(Unit unit) {
        LivingEntity le = (LivingEntity) unit;
        if (!hasAnchor(unit) || le.level().isClientSide())
            return;

        if ((unit.isIdle() || le.distanceToSqr(Vec3.atCenterOf(unit.getAnchor())) > ANCHOR_RETREAT_RANGE * ANCHOR_RETREAT_RANGE) &&
                !le.getOnPos().equals(unit.getAnchor())) {
            fullResetBehaviours(unit);
            unit.getMoveGoal().setMoveTarget(unit.getAnchor());
        }
    }

    private static int getThresholdResources(Unit unit) {
        // Scales with carry capacity (the default 100 gives the old 50); the removed carry-bag
        // research used to double it.
        int capacity = unit.getMaxResources();
        return capacity > 0 ? Math.max(1, capacity / 2) : 50;
    }

    static boolean atMaxResources(Unit unit) {
        return Resources.getTotalResourcesFromItems(unit.getItems()).getTotalValue() >= unit.getMaxResources();
    }

    static boolean atThresholdResources(Unit unit) {
        return Resources.getTotalResourcesFromItems(unit.getItems()).getTotalValue() >= getThresholdResources(unit);
    }

    default boolean hasLivingTarget() {
        Mob unitMob = (Mob) this;
        return unitMob.getTarget() != null && unitMob.getTarget().isAlive();
    }

    static void fullResetBehaviours(Unit unit) {
        if (((Entity) unit).level().isClientSide() && !Keybindings.shiftMod.isDown()) {
            unit.getCheckpoints().clear();
            RtsDebugPathPreview.removeUnitPath(((Entity) unit).getId());
        }
        unit.resetBehaviours();
        Unit.resetBehaviours(unit);
        if (unit instanceof Unit workerUnit && workerUnit.isWorker()) {
            Unit.resetWorkerBehaviours(workerUnit);
        }
        if (unit instanceof Unit attackerUnit && attackerUnit.isAttacker()) {
            Unit.resetAttackerBehaviours(attackerUnit);
        }
    }

    static void resetBehaviours(Unit unit) {
        unit.getTargetGoal().setTarget(null);
        unit.getMoveGoal().stopMoving();
        if (unit.getReturnResourcesGoal() != null)
            unit.getReturnResourcesGoal().stopReturning();
        unit.setFollowTarget(null);
        unit.setHoldPosition(false);
        if (unit.canGarrison())
            unit.getGarrisonGoal().stopGarrisoning();
    }

    // can be overridden in the Unit's class to do additional logic on a reset
    default void resetBehaviours() { }

    // this setter sets a Unit field and so can't be defaulted
    // move to a block ignoring all else until reaching it
    default void setMoveTarget(@Nullable BlockPos bp) {
        this.getMoveGoal().setMoveTarget(bp);
    }

    // continuously move to a target until told to do something else
    void setFollowTarget(@Nullable LivingEntity target);

    void initialiseGoals();

    // weapons aren't provided automatically when spawned by custom code
    // also recalculate stats based on upgrades
    default void setupEquipmentAndUpgradesServer() { }

    // equipment only needs to be done serverside, but mod-specific fields need to be done clientside too
    default void setupEquipmentAndUpgradesClient() { }

    /**
     * Base attribute set every unit starts from; per-unit builders chain onto this.
     */
    static AttributeSupplier.Builder createDefaultAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ATTACK_DAMAGE, 0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.MAX_HEALTH, 1)
                .add(Attributes.FOLLOW_RANGE, Unit.getFollowRange())
                .add(Attributes.ARMOR, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0)
                .add(AttributeHelpers.holder(AttributeRegistrar.ATTACK_DAMAGE.get()), 0)
                .add(AttributeHelpers.holder(AttributeRegistrar.ATTACKS_PER_SECOND.get()), 0)
                .add(AttributeHelpers.holder(AttributeRegistrar.ATTACK_RANGE.get()), 0)
                .add(AttributeHelpers.holder(AttributeRegistrar.AGGRO_RANGE.get()), 10)
                .add(AttributeHelpers.holder(AttributeRegistrar.SIGHT_RANGE.get()), Unit.DEFAULT_SIGHT_RANGE)
                .add(AttributeHelpers.holder(AttributeRegistrar.RANGED_DAMAGE_RESIST.get()), 0)
                .add(AttributeHelpers.holder(AttributeRegistrar.MAGIC_DAMAGE_RESIST.get()), 0)
                .add(AttributeHelpers.holder(AttributeRegistrar.EVASION_CHANCE.get()), 0);
    }

    /**
     * Movement multiplier from equipment and abilities; overridden per unit.
     */
    default float getSpeedModifier() {
        return 1.0f;
    }

    public static void startEatingOrDrinking(Unit unit, ItemEntity itemEntity) {
        ItemStack itemStack = itemEntity.getItem();
        ((LivingEntity) unit).onItemPickup(itemEntity);
        ((LivingEntity) unit).take(itemEntity, 1);
        unit.getItems().add(new ItemStack(itemStack.getItem(), 1));
        UnitAnimationClientboundPacket.sendEatFoodPacket(((LivingEntity) unit), BuiltInRegistries.ITEM.getId(itemStack.getItem()));
        itemStack.setCount(itemStack.getCount() - 1);
        if (itemStack.getCount() <= 0)
            itemEntity.discard();
    }

        public default double getAttackerRangeBonus(Mob attacker) {
        return 0f;
    }

    static Ability getAbility(Unit unit, UnitAction abilityAction) {
        for (Ability ability : unit.getAbilities().get())
            if (ability.action.equals(abilityAction))
                return ability;
        return null;
    }

    default boolean isIdle() {
        boolean idleAttacker = true;
        if (this instanceof Unit attackerUnit && attackerUnit.isAttacker()) {
            idleAttacker = attackerUnit.getAttackMoveTarget() == null &&
                    !((Unit) attackerUnit).hasLivingTarget() &&
                    !Unit.isAttackingBuilding(attackerUnit);
        }
        boolean idleRangedAttacker = true;
        if (this instanceof Unit rangedAttackerUnit && rangedAttackerUnit.isRangedAttacker()) {
            idleRangedAttacker = rangedAttackerUnit.getRangedAttackGroundGoal() == null ||
                                rangedAttackerUnit.getRangedAttackGroundGoal().getGroundTarget() == null;
        }
        boolean idleWorker = true;
        if (Unit.isWorker(this))
            idleWorker = Unit.isWorkerIdle((Unit) this);

        for (Goal goal : ((Mob) this).goalSelector.getAvailableGoals()) {
            if (goal instanceof GenericUntargetedSpellGoal spellGoal && spellGoal.isCasting())
                return false;
            if (goal instanceof GenericTargetedSpellGoal spellGoal && spellGoal.isCasting())
                return false;
        }
        // some larger mobs like bears get stuck near their movetarget so nav won't be done but it also won't be null
        boolean stationaryNearMoveTarget = false;
        if (this.getMoveGoal().getMoveTarget() != null) {
            double distToMoveTarget = ((LivingEntity) this).distanceToSqr(this.getMoveGoal().getMoveTarget().getCenter());
            // Genuinely stuck = barely moving on BOTH axes. Must be && (not ||): a unit walking straight along
            // one axis has ~0 velocity on the other, so || wrongly reads it as stationary while it's still moving.
            // Epsilon, not == 0: physics rarely lands exactly on zero.
            net.minecraft.world.phys.Vec3 dm = ((Mob) this).getDeltaMovement();
            boolean stationary = Math.abs(dm.x) < 1.0e-3 && Math.abs(dm.z) < 1.0e-3;
            stationaryNearMoveTarget = stationary && distToMoveTarget < 4;
        }
        boolean isMoving = !((Mob) this).getNavigation().isDone() || this.getMoveGoal().getMoveTarget() != null;
        return (!isMoving || stationaryNearMoveTarget) &&
                this.getFollowTarget() == null &&
                idleAttacker &&
                idleWorker &&
                idleRangedAttacker;
    }

    static Random RANDOM = new Random();

    public static void addParticlesAroundSelf(Unit unit, ParticleOptions pParticleOption) {
        for(int i = 0; i < 5; ++i) {
            double d0 = RANDOM.nextGaussian() * 0.02;
            double d1 = RANDOM.nextGaussian() * 0.02;
            double d2 = RANDOM.nextGaussian() * 0.02;
            Entity entity = (Entity) unit;

            if (!entity.level().isClientSide) {
                ((ServerLevel) entity.level()).sendParticles(pParticleOption,
                        entity.getRandomX(1.0),
                        entity.getRandomY() + 1.0,
                        entity.getRandomZ(1.0),
                        1, d0, d1, d2, 0
                );
            }
        }
    }

    void updateAbilityButtons();

    default boolean isCasting() {
        for (Ability ability : getAbilities().get())
            if (ability.isCasting(this))
                return true;
        return false;
    }

    public default List<FormattedCharSequence> getAttackDamageStatTooltip() {
        return List.of(fcs(I18n.get("unitstats.reignofnether.attack_damage"), true));
    }

    public default List<FormattedCharSequence> getAttackSpeedStatTooltip() {
        return List.of(fcs(I18n.get("unitstats.reignofnether.attack_speed"), true));
    }

    public default List<FormattedCharSequence> getRangeStatTooltip() {
        return List.of(fcs(I18n.get("unitstats.reignofnether.range"), true));
    }

    public default List<FormattedCharSequence> getArmourStatTooltip() {
        ArrayList<FormattedCharSequence> fcsList = new ArrayList<>();
        fcsList.add(fcs(I18n.get("unitstats.reignofnether.armour"), true));
        if (getUnitPhysicalArmorPercentage() != 0) {
            fcsList.add(fcs(I18n.get("unitstats.reignofnether.armour_melee_and_ranged", (int) (getUnitPhysicalArmorPercentage() * 100)), false));
        }
        if (getUnitRangedArmorPercentage() > 0) {
            fcsList.add(fcs(I18n.get("unitstats.reignofnether.armour_ranged", (int) (getUnitRangedArmorPercentage() * 100)), false));
        }
        if (getUnitResistPercentage() > 0) {
            fcsList.add(fcs(I18n.get("unitstats.reignofnether.armour_all", (int) (getUnitResistPercentage() * 100)), false));
        }
        else if (getUnitMagicArmorPercentage() > 0) {
            fcsList.add(fcs(I18n.get("unitstats.reignofnether.armour_magic", (int) (getUnitMagicArmorPercentage() * 100)), false));
        }
        return fcsList;
    }

    public default List<FormattedCharSequence> getMovementSpeedStatTooltip() {
        return List.of(fcs(I18n.get("unitstats.reignofnether.movement_speed"), true));
    }

    public default List<FormattedCharSequence> getStatTooltip(UnitStatType unitStatType) {
        return switch (unitStatType) {
            case ATTACK_DAMAGE -> getAttackDamageStatTooltip();
            case ATTACK_SPEED -> getAttackSpeedStatTooltip();
            case RANGE -> getRangeStatTooltip();
            case ARMOUR -> getArmourStatTooltip();
            case MOVEMENT_SPEED -> getMovementSpeedStatTooltip();
        };
    }

    default void setCooldown(Ability abilityClass, float cooldown) {
        getCooldowns().put(abilityClass, cooldown);
    }

    default float getCooldown(Ability abilityClass) {
        return getCooldowns().get(abilityClass);
    }

    Object2ObjectArrayMap<Ability,Float> getCooldowns();

    boolean hasAutocast(Ability ability);
    void setAutocast(Ability ability);
    default void setCharges(Ability abilityClass, int charges) {
        getCharges().put(abilityClass, Math.min(charges, abilityClass.maxCharges));
    }

    default int getCharges(Ability ability) {
        if (!getCharges().containsKey(ability))
            getCharges().put(ability, ability.maxCharges);
        return getCharges().get(ability);
    }
    Object2ObjectArrayMap<Ability,Integer> getCharges();

    default List<Button> getPassiveIcons() {
        ArrayList<Button> icons = new ArrayList<>();
        LivingEntity entity = (LivingEntity) this;
        synchronized (UnitClientEvents.mobEffectIcons) {
            HashMap<Holder<MobEffect>, MobEffectIcon> mobEffects = UnitClientEvents.mobEffectIcons.get(entity.getId());
            if (mobEffects != null) {
                for (Holder<MobEffect> effect : mobEffects.keySet()) {
                    if (mobEffects.get(effect) != null)
                        icons.add(mobEffects.get(effect));
                }
            }
        }
        return icons;
    }

    default AABB getInflatedSelectionBox() {
        return ((Entity) this).getBoundingBox();
    }

    default boolean hasEffectWithDuration(MobEffect mobEffect) {
        MobEffectInstance mei = ((LivingEntity) this).getEffect(MobEffectHelpers.holder(mobEffect));
        return mei != null && mei.getDuration() > 0;
    }

    default float getBonusMeleeRangeForAttackers() {
        return 0.4f;
    }

    default boolean hasAnyEnchants() {
        return !(((LivingEntity) this).getMainHandItem().getEnchantments().isEmpty()) ||
               !(((LivingEntity) this).getItemBySlot(EquipmentSlot.CHEST).getEnchantments().isEmpty());
    }

    default boolean uninterruptable() {
        return false;
    }

    default boolean hasLineOfSight(Vec3 pos) {
        Entity thisEntity = (Entity) this;
        Vec3 vec3 = new Vec3(thisEntity.getX(), thisEntity.getEyeY(), thisEntity.getZ());
        Vec3 vec31 = new Vec3(pos.x, pos.y, pos.z);
        if (vec31.distanceToSqr(vec3) > 16384) {
            return false;
        } else {
            return thisEntity.level()
                    .clip(new ClipContext(vec3, vec31, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, thisEntity))
                    .getType() == HitResult.Type.MISS;
        }
    }

    default boolean isFlyingUnit() {
        return getMoveGoal() instanceof FlyingMoveToTargetGoal;
    }

    /**
     * Highest Y coordinate a flying unit of this type will path to. Was the server-wide
     * `flyingMaxYLevel` gamerule; a default lives here and each unit type can override it.
     */
    default double getFlyingMaxY() {
        return 320;
    }

    /**
     * Whether this entity is an RTS unit. Code units (the templates) are always units; data-driven
     * units (a plain `Mob` carrying a {@link com.solegendary.reignofnether.unit.UnitDefinition})
     * report true while the definition is attached.
     *
     * <p>Once `Mob` implements `Unit`, every `instanceof Unit` check MUST be gated on this - otherwise
     * every zombie, sheep and cow in the world would be treated as a unit.
     */
    default boolean isRtsUnit() {
        return true;
    }

    /** True only for actual RTS units (code units, or a mob carrying a UnitDefinition). */
    static boolean isUnit(@Nullable Object o) {
        return o instanceof Unit unit && unit.isRtsUnit();
    }

    // ---- Role flags (plan CONTENT_JSON_PLAN.md). ----
    // Temporary bridge to the sub-interfaces; they will be removed and these become definition-driven.
    // The static overloads are what `instanceof WorkerUnit/AttackerUnit/...` translates to.
    default boolean isWorker() { return false; }
    static boolean isWorker(@Nullable Object o) { return o instanceof Unit unit && unit.isWorker(); }
    default boolean isAttacker() { return false; }
    static boolean isAttacker(@Nullable Object o) { return o instanceof Unit unit && unit.isAttacker(); }
    default boolean isRangedAttacker() { return false; }
    static boolean isRangedAttacker(@Nullable Object o) { return o instanceof Unit unit && unit.isRangedAttacker(); }
    default boolean isHero() { return false; }
    static boolean isHero(@Nullable Object o) { return o instanceof Unit unit && unit.isHero(); }

    // ---- Goal accessors (plan CONTENT_JSON_PLAN.md: collapsed from the role sub-interfaces). ----
    default com.solegendary.reignofnether.unit.goals.BuildRepairGoal getBuildRepairGoal() { return null; }
    default com.solegendary.reignofnether.unit.goals.GatherResourcesGoal getGatherResourceGoal() { return null; }
    default com.solegendary.reignofnether.unit.goals.ExploreBuildLocationGoal getExploreBuildLocationGoal() { return null; }
    default net.minecraft.world.level.block.state.BlockState getReplantBlockState() {
        return net.minecraft.world.level.block.Blocks.WHEAT.defaultBlockState();
    }

    // ---- Attacker accessors (collapsed from AttackerUnit). ----
    default boolean getWillRetaliate() { return false; }
    default boolean getAggressiveWhenIdle() { return false; }
    default net.minecraft.core.BlockPos getAttackMoveTarget() { return null; }
    default boolean canAttackBuildings() { return false; }
    default net.minecraft.world.entity.ai.goal.Goal getAttackGoal() { return null; }
    default net.minecraft.world.entity.ai.goal.Goal getAttackBuildingGoal() { return null; }
    default com.solegendary.reignofnether.unit.EnemySearchBehaviour getEnemySearchBehaviour() {
        return com.solegendary.reignofnether.unit.EnemySearchBehaviour.NONE;
    }
    default void setEnemySearchBehaviour(com.solegendary.reignofnether.unit.EnemySearchBehaviour behaviour) { }
    default void setAttackMoveTarget(@Nullable net.minecraft.core.BlockPos bp) { }

    // ==== merged from AttackerUnit ====
    float ATTACK_DAMAGE_REDUCTION_PER_WEAK = 0.2f;
    float ATTACK_DAMAGE_INCREASE_PER_STRENGTH = 0.2f;

    default float getAttacksPerSecond() {
        return 20f / getAttackCooldown();
    }
    default float getAttackCooldown() {
        return ((20 / getNonBaseAttacksPerSecond()) * getAttackCooldownMultiplier());
    }
    default float getNonBaseAttacksPerSecond() {
        AttributeInstance attr = ((LivingEntity) this).getAttribute(AttributeHelpers.holder(AttributeRegistrar.ATTACKS_PER_SECOND.get()));
        return (float) (attr != null ?  attr.getValue() : AttributeRegistrar.ATTACKS_PER_SECOND.get().getDefaultValue());
    }
    default float getBaseAttacksPerSecond() {
        AttributeInstance attr = ((LivingEntity) this).getAttribute(AttributeHelpers.holder(AttributeRegistrar.ATTACKS_PER_SECOND.get()));
        return (float) (attr != null ?  attr.getBaseValue() : AttributeRegistrar.ATTACKS_PER_SECOND.get().getDefaultValue());
    }
    default float getAggroRange() {
        float attackRange = getAttackRange();
        AttributeInstance attr = ((LivingEntity) this).getAttribute(AttributeHelpers.holder(AttributeRegistrar.AGGRO_RANGE.get()));
        float aggroRange = (float) (attr != null ?  attr.getValue() : AttributeRegistrar.AGGRO_RANGE.get().getDefaultValue());
        return Math.max(attackRange, aggroRange);
    }
    default float getAttackRange() {
        AttributeInstance attr = ((LivingEntity) this).getAttribute(AttributeHelpers.holder(AttributeRegistrar.ATTACK_RANGE.get()));
        return (float) (attr != null ?  attr.getValue() : AttributeRegistrar.ATTACK_RANGE.get().getDefaultValue());
    }
    default float getBaseAttackRange() {
        AttributeInstance attr = ((LivingEntity) this).getAttribute(AttributeHelpers.holder(AttributeRegistrar.ATTACK_RANGE.get()));
        return (float) (attr != null ?  attr.getBaseValue() : AttributeRegistrar.ATTACK_RANGE.get().getDefaultValue());
    }
    default float getBaseUnitAttackDamage() {
        float bonus = 0;
        if (this instanceof Unit heroUnit && heroUnit.isHero()) {
            bonus = heroUnit.getAttackBonusPerLevel() * heroUnit.getHeroLevel();
        }
        AttributeInstance attr = ((LivingEntity) this).getAttribute(AttributeHelpers.holder(AttributeRegistrar.ATTACK_DAMAGE.get()));
        return (float) (attr != null ?  attr.getBaseValue() : AttributeRegistrar.ATTACK_DAMAGE.get().getDefaultValue()) + bonus;
    }
    default float getUnitAttackDamage() {
        float bonus = 0;
        if (this instanceof Unit heroUnit && heroUnit.isHero()) {
            bonus = heroUnit.getAttackBonusPerLevel() * heroUnit.getHeroLevel();
        }
        AttributeInstance attr = ((LivingEntity) this).getAttribute(AttributeHelpers.holder(AttributeRegistrar.ATTACK_DAMAGE.get()));
        float value = (float) (attr != null ?  attr.getValue() : AttributeRegistrar.ATTACK_DAMAGE.get().getDefaultValue()) + bonus;

        MobEffectInstance weakMei = ((LivingEntity) this).getEffect(MobEffects.WEAKNESS);
        float weak = 0;
        if (weakMei != null && weakMei.getDuration() > 0) {
            weak = (weakMei.getAmplifier() + 1) * ATTACK_DAMAGE_REDUCTION_PER_WEAK;
        }
        MobEffectInstance strMei = ((LivingEntity) this).getEffect(MobEffects.DAMAGE_BOOST);
        float str = 0;
        if (strMei != null && strMei.getDuration() > 0) {
            str = (strMei.getAmplifier() + 1) * ATTACK_DAMAGE_INCREASE_PER_STRENGTH;
        }
        return Math.max(0, value * (1 - weak + str));
    }

    default void setUnitAttackTarget(@Nullable LivingEntity target) {
        if (target != null) {
            MiscUtil.addUnitCheckpoint((this), target.getId(), false);
            Goal attackBuildingGoal = this.getAttackBuildingGoal();
            if (attackBuildingGoal instanceof RangedAttackBuildingGoal<?> rabg)
                rabg.stop();
            else if (attackBuildingGoal instanceof MeleeAttackBuildingGoal mabg)
                mabg.stopAttacking();
        }
        (this).getTargetGoal().setTarget(target);
    }

    default void setUnitAttackTargetForced(@Nullable LivingEntity target) {
        setUnitAttackTarget(target);
        if (target != null) {
            Goal attackBuildingGoal = this.getAttackBuildingGoal();
            if (attackBuildingGoal instanceof RangedAttackBuildingGoal<?> rabg)
                rabg.stop();
            else if (attackBuildingGoal instanceof MeleeAttackBuildingGoal mabg)
                mabg.stopAttacking();
            (this).getTargetGoal().forced = true;
        }
    }

    default void setAttackBuildingTarget(BlockPos preselectedBlockPos) {
        setAttackBuildingTarget(preselectedBlockPos, true);
    }

    default void setAttackBuildingTarget(BlockPos preselectedBlockPos, boolean forced) {
        if (this.canAttackBuildings()) {
            Goal attackBuildingGoal = this.getAttackBuildingGoal();
            if (attackBuildingGoal instanceof RangedAttackBuildingGoal<?> rabg) {
                rabg.setBuildingTarget(preselectedBlockPos);
                rabg.forced = forced;
            } else if (attackBuildingGoal instanceof MeleeAttackBuildingGoal mabg) {
                mabg.setBuildingTarget(preselectedBlockPos);
                mabg.forced = forced;
            }
        } else {
            Level level = ((LivingEntity) this).level();
            BuildingPlacement building = BuildingUtils.findBuilding(level.isClientSide(), preselectedBlockPos);

            if (building != null) {
                BlockPos groundCentrePos = new BlockPos(building.centrePos.getX(), building.originPos.getY() + 1, building.centrePos.getZ());
                BlockPos targetPos = MyMath.getXZRangeLimitedBlockPos(
                        new BlockPos(groundCentrePos),
                        ((LivingEntity) this).getOnPos(),
                        getAttackRange() - 5
                );
                while (!level.getBlockState(targetPos.above()).isAir())
                    targetPos = targetPos.above();

                (this).setMoveTarget(targetPos);
                if (((LivingEntity) this).level().isClientSide)
                    MiscUtil.addUnitCheckpoint((this), groundCentrePos, false);
            }
        }
    }

    default void retargetToClosestUnit(ServerLevel level) {
        float aggroRange = this.getAggroRange();
        BuildingPlacement garrPlacement = GarrisonableBuildingAddon.getGarrison((Unit) this);
        GarrisonableBuildingAddon garr = garrPlacement != null ? garrPlacement.getBuilding().getActiveAddon(GarrisonableBuildingAddon.class) : null;
        if (garr != null) {
            aggroRange = garr.getAttackRange();
        }
        boolean isAttackingBuilding = isAttackingBuilding(this);
        LivingEntity currentTarget = ((Mob) this).getTarget();
        if (currentTarget == null && !isAttackingBuilding) return;
        LivingEntity closestTarget = MiscUtil.findClosestAttackableEntity((Mob) this, aggroRange, level);
        if (closestTarget == null) return;
        double distClosestTarget =  ((Mob) this).distanceToSqr(closestTarget.position());
        double distCurrentTarget = isAttackingBuilding ? (aggroRange / 2) : ((Mob) this).distanceToSqr(currentTarget.position());

        if (distClosestTarget < distCurrentTarget) {
            if (!((LivingEntity) this).isPassenger())
                (this).getMoveGoal().stopMoving();
            setUnitAttackTarget(closestTarget);
        }
    }

    default void attackClosestEnemy(ServerLevel level) {
        float aggroRange = this.getAggroRange();
        BuildingPlacement garrPlacement = GarrisonableBuildingAddon.getGarrison((Unit) this);
        GarrisonableBuildingAddon garr = garrPlacement != null ? garrPlacement.getBuilding().getActiveAddon(GarrisonableBuildingAddon.class) : null;
        if (garr != null) {
            aggroRange = garr.getAttackRange();
        }
        LivingEntity entity = MiscUtil.findClosestAttackableEntity((Mob) this, aggroRange, level);
        if (entity != null) {
            if (!((LivingEntity) this).isPassenger())
                (this).getMoveGoal().stopMoving();
            setUnitAttackTarget(entity);
            return;
        }
        if (canAttackBuildings() &&
                (!((this).getOwnerName()).isEmpty() || level.getGameRules().getRule(GameRuleRegistrar.NEUTRAL_AGGRO).get()))
        {
            BuildingPlacement closestBuilding = MiscUtil.findClosestAttackableBuilding((Mob) this, aggroRange);
            if (closestBuilding != null) {
                if (!((LivingEntity) this).isPassenger())
                    (this).getMoveGoal().stopMoving();
                setAttackBuildingTarget(closestBuilding.originPos, false);
            }
        }
    }

    default @Nullable SoundAction getAttackSound() { return null; }

    default float getBonusMeleeRange() {
        return 0f;
    }

    default int getDamageTooltipColour() {
        return TooltipColours.WHITE;
    }

    default boolean hasBonusRange() {
        return getAttackRange() > getBaseAttackRange();
    }

    default float getAttackCooldownMultiplier() {
        MobEffectInstance disarm = ((LivingEntity) (this)).getEffect(MobEffectHelpers.holder(MobEffectRegistrar.DISARM.get()));
        if (disarm != null) {
            return 999999;
        }
        MobEffectInstance attackSlowdown = ((LivingEntity) (this)).getEffect(MobEffectHelpers.holder(MobEffectRegistrar.ATTACK_SLOWDOWN.get()));
        int attackSlowdownAmp = attackSlowdown != null ? attackSlowdown.getAmplifier() + 1 : 0;

        MobEffectInstance bloodlust = ((LivingEntity) (this)).getEffect(MobEffectHelpers.holder(MobEffectRegistrar.BLOODLUST.get()));

        return (1 + (attackSlowdownAmp * 0.05f)) / (bloodlust != null ? 1.6f : 1.0f);
    }

    default void attackMoveNearestEnemyBuilding() {
        Mob mob = (Mob) this;
        Unit unit = (Unit) this;
        List<BuildingPlacement> buildings;
        if (mob.level().isClientSide())
            buildings = BuildingClientEvents.getBuildings();
        else
            buildings = BuildingServerEvents.getBuildings();

        ArrayList<BuildingPlacement> eligibleTargets = new ArrayList<>();
        List<BuildingPlacement> buildingsCopy = new ArrayList<>(buildings); // defensive copy
        for (BuildingPlacement buildingPlacement : buildingsCopy) {
            if (!unit.getOwnerName().equals(buildingPlacement.ownerName) &&
                    !AlliancesServerEvents.isAllied(unit.getOwnerName(), buildingPlacement.ownerName) &&
                    !buildingPlacement.ownerName.isBlank() &&
                    buildingPlacement.isAttackable()) {
                eligibleTargets.add(buildingPlacement);
            }
        }
        eligibleTargets.sort(Comparator.comparing(b -> b.centrePos.distToCenterSqr(((Entity) unit).position())));

        if (!eligibleTargets.isEmpty())
            setAttackMoveTarget(eligibleTargets.get(0).getClosestGroundPos(((Entity) unit).getOnPos(), 1));
    }

    default void attackMoveNearestEnemyUnit(boolean workersOnly) {
        Mob mob = (Mob) this;
        Unit unit = (Unit) this;
        List<LivingEntity> units;
        if (mob.level().isClientSide())
            units = UnitClientEvents.getAllUnits();
        else
            units = UnitServerEvents.getAllUnits();

        ArrayList<LivingEntity> eligibleTargets = new ArrayList<>();
        List<LivingEntity> unitsCopy = new ArrayList<>(units); // defensive copy
        for (LivingEntity entity : unitsCopy) {
            if (entity instanceof Unit otherUnit && otherUnit.isRtsUnit() &&
                    (!workersOnly || Unit.isWorker(entity)) &&
                    !unit.getOwnerName().equals(otherUnit.getOwnerName()) &&
                    !AlliancesServerEvents.isAllied(unit.getOwnerName(), otherUnit.getOwnerName()) &&
                    !otherUnit.getOwnerName().isBlank()) {
                eligibleTargets.add(entity);
            }
        }
        eligibleTargets.sort(Comparator.comparing(e -> e.position().distanceToSqr(((Entity) unit).position())));

        if (!eligibleTargets.isEmpty())
            setAttackMoveTarget(eligibleTargets.get(0).getOnPos());
    }

    static boolean isAttackingBuilding(Unit unit) {
        boolean isAttackingBuilding = false;
        Goal attackBuildingGoal = unit.getAttackBuildingGoal();
        if (attackBuildingGoal instanceof RangedAttackBuildingGoal<?> rabg)
            isAttackingBuilding = rabg.getBuildingTarget() != null;
        else if (attackBuildingGoal instanceof MeleeAttackBuildingGoal mabg)
            isAttackingBuilding = mabg.getBuildingTarget() != null;
        return isAttackingBuilding;
    }

    // ==== worker/attacker tick helpers (merged from WorkerUnit / AttackerUnit) ====
    static void tickWorker(Unit unit) {
        com.solegendary.reignofnether.unit.goals.BuildRepairGoal buildRepairGoal = unit.getBuildRepairGoal();
        if (buildRepairGoal != null)
            buildRepairGoal.tick();
        com.solegendary.reignofnether.unit.goals.GatherResourcesGoal gatherResourcesGoal = unit.getGatherResourceGoal();
        if (gatherResourcesGoal != null)
            gatherResourcesGoal.tick();

        LivingEntity entity = (LivingEntity) unit;
        ItemStack mainHandItem = entity.getItemBySlot(EquipmentSlot.MAINHAND);

        if (buildRepairGoal != null && buildRepairGoal.isBuilding()) {
            if (!mainHandItem.is(net.minecraft.world.item.Items.IRON_SHOVEL))
                entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(net.minecraft.world.item.Items.IRON_SHOVEL));
        }
        else if (gatherResourcesGoal != null && gatherResourcesGoal.isGathering()) {
            switch (gatherResourcesGoal.getTargetResourceName()) {
                case FOOD -> { if (!mainHandItem.is(net.minecraft.world.item.Items.IRON_HOE))
                        entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(net.minecraft.world.item.Items.IRON_HOE)); }
                case WOOD -> { if (!mainHandItem.is(net.minecraft.world.item.Items.IRON_AXE))
                        entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(net.minecraft.world.item.Items.IRON_AXE)); }
                case ORE -> { if (!mainHandItem.is(net.minecraft.world.item.Items.IRON_PICKAXE))
                        entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE)); }
                case NONE -> { if (!mainHandItem.is(net.minecraft.world.item.Items.AIR))
                        entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(net.minecraft.world.item.Items.AIR)); }
            }
        } else if (unit.isAttacker() && unit.getTargetGoal().getTarget() != null) {
            if (!mainHandItem.is(net.minecraft.world.item.Items.WOODEN_SWORD)) {
                entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(net.minecraft.world.item.Items.WOODEN_SWORD));
                if (!entity.level().isClientSide())
                    UnitAnimationClientboundPacket.sendEntityPacket(UnitAnimationAction.NON_KEYFRAME_START, entity, unit.getTargetGoal().getTarget());
            }
        } else {
            if (!mainHandItem.is(net.minecraft.world.item.Items.AIR)) {
                entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(net.minecraft.world.item.Items.AIR));
                if (!entity.level().isClientSide())
                    UnitAnimationClientboundPacket.sendBasicPacket(UnitAnimationAction.NON_KEYFRAME_STOP, entity);
            }
        }
    }

    static void resetWorkerBehaviours(Unit unit) {
        // a worker need not have all of the gather/build goals: the flags decide which exist
        if (unit.getBuildRepairGoal() != null)
            unit.getBuildRepairGoal().stopBuilding();
        if (unit.getGatherResourceGoal() != null)
            unit.getGatherResourceGoal().stopGathering();
        if (unit.getExploreBuildLocationGoal() != null)
            unit.getExploreBuildLocationGoal().reset();
    }

    static void resetWorkerBehavioursExceptExploreBuild(Unit unit) {
        if (unit.getBuildRepairGoal() != null)
            unit.getBuildRepairGoal().stopBuilding();
        if (unit.getGatherResourceGoal() != null)
            unit.getGatherResourceGoal().stopGathering();
    }

    static boolean isWorkerIdle(Unit unit) {
        com.solegendary.reignofnether.unit.goals.GatherResourcesGoal resGoal = unit.getGatherResourceGoal();
        boolean isMoving = !((Mob) unit).getNavigation().isDone();
        boolean isGathering = resGoal != null && resGoal.isGathering();
        boolean isGatheringIdle = resGoal == null || resGoal.isIdle();
        com.solegendary.reignofnether.unit.goals.BuildRepairGoal buildGoal = unit.getBuildRepairGoal();
        boolean isBuilding = buildGoal != null && buildGoal.getBuildingTarget() != null;
        boolean isFarming = resGoal.isFarming();
        boolean isAttacking = unit.getTargetGoal().getTarget() != null;
        return !isMoving && !isGathering && !isBuilding && isGatheringIdle && !isAttacking && !isFarming;
    }

    static void resetAttackerBehaviours(Unit unit) {
        unit.setUnitAttackTarget(null);
        unit.setAttackMoveTarget(null);

        Goal attackGoal = unit.getAttackGoal();
        if (attackGoal instanceof MeleeWindupAttackUnitGoal mwaug)
            mwaug.resetWindup();

        Goal attackBuildingGoal = unit.getAttackBuildingGoal();
        if (attackBuildingGoal instanceof RangedAttackBuildingGoal<?> rabg)
            rabg.stop();
        else if (attackBuildingGoal instanceof MeleeAttackBuildingGoal mabg)
            mabg.stopAttacking();

        unit.setEnemySearchBehaviour(com.solegendary.reignofnether.unit.EnemySearchBehaviour.NONE);
    }

    static void tickAttacker(Unit unit) {
        Mob unitMob = (Mob) unit;

        if (!unitMob.level().isClientSide) {
            if (unit.getAttackGoal() instanceof AbstractMeleeAttackUnitGoal meleeAttackUnitGoal) {
                meleeAttackUnitGoal.tickAttackCooldown();
                if (unitMob.isVehicle())
                    meleeAttackUnitGoal.tick();
                if (meleeAttackUnitGoal instanceof MeleeWindupAttackUnitGoal goal)
                    goal.checkAndPerformAttackWithWindup();
            }
            else if (unit.getAttackGoal() instanceof UnitRangedAttackGoal rangedAttackGoal)
                rangedAttackGoal.tickAttackCooldown();
            else if (unit.getAttackGoal() instanceof UnitBowAttackGoal rangedAttackGoal)
                rangedAttackGoal.tickAttackCooldown();

            if (unit.getAttackBuildingGoal() != null && unit.canAttackBuildings())
                unit.getAttackBuildingGoal().tick();
        }

        if (!unitMob.level().isClientSide && unitMob.tickCount % 4 == 0) {
            if (((LivingEntity) unit).getEffect(MobEffectHelpers.holder(MobEffectRegistrar.STUN.get())) != null ||
                ((LivingEntity) unit).getEffect(MobEffectHelpers.holder(MobEffectRegistrar.FREEZE.get())) != null) {
                Unit.fullResetBehaviours(unit);
                return;
            }

            boolean isAttackingBuilding = isAttackingBuilding(unit);

            if (unit.getAttackMoveTarget() != null && !unit.hasLivingTarget() && !isAttackingBuilding) {
                unit.attackClosestEnemy((ServerLevel) unitMob.level());
                if (unit.getTargetGoal().getTarget() == null &&
                    unit.getMoveGoal().getMoveTarget() == null &&
                    !isAttackingBuilding(unit))
                    unit.setMoveTarget(unit.getAttackMoveTarget());
            }

            boolean isCasting = unit.isCasting();
            boolean forced1 = unit.getTargetGoal().forced;
            Goal attackBuildingGoal = unit.getAttackBuildingGoal();
            boolean forced2 = attackBuildingGoal instanceof RangedAttackBuildingGoal<?> rabg && rabg.forced;
            boolean forced3 = attackBuildingGoal instanceof MeleeAttackBuildingGoal mabg && mabg.forced;
            boolean forced = forced1 || forced2 || forced3;

            if (unitMob.getLastDamageSource() != null &&
                    unit.getWillRetaliate() &&
                    unit.getTargetGoal().getTarget() == null &&
                    !isCasting && (unit.isIdle() || (isAttackingBuilding && !forced))) {

                Entity lastDSEntity = unitMob.getLastDamageSource().getEntity();
                com.solegendary.reignofnether.unit.Relationship rs = UnitServerEvents.getUnitToEntityRelationship(unit, lastDSEntity);

                if (lastDSEntity instanceof LivingEntity &&
                    !(lastDSEntity instanceof net.minecraft.world.entity.player.Player player && player.isCreative()) &&
                    (rs == com.solegendary.reignofnether.unit.Relationship.NEUTRAL || rs == com.solegendary.reignofnether.unit.Relationship.HOSTILE)) {
                    unit.setUnitAttackTarget((LivingEntity) lastDSEntity);
                }
            }
            if (unit.isIdle() && !isCasting && unit.getAggressiveWhenIdle())
                unit.attackClosestEnemy((ServerLevel) unitMob.level());

            if (!forced)
                unit.retargetToClosestUnit((ServerLevel) unitMob.level());
        }

        if (!unitMob.level().isClientSide && unitMob.tickCount % 40 == 0) {
            if (unit.getAttackMoveTarget() != null && unit.getEnemySearchBehaviour() == com.solegendary.reignofnether.unit.EnemySearchBehaviour.NONE) {
                boolean hasNoTargets = unit.getTargetGoal().getTarget() == null;
                if (unit.getAttackBuildingGoal() instanceof MeleeAttackBuildingGoal mabg && mabg.getBuildingTarget() != null)
                    hasNoTargets = false;
                else if (unit.getAttackBuildingGoal() instanceof RangedAttackBuildingGoal<?> rabg && rabg.getBuildingTarget() != null)
                    hasNoTargets = false;
                if (hasNoTargets && unitMob.distanceToSqr(unit.getAttackMoveTarget().getCenter()) < 4)
                    unit.setAttackMoveTarget(null);
            }
            if (unit.getAttackMoveTarget() == null || unit.isIdle()) {
                switch (unit.getEnemySearchBehaviour()) {
                    case NEAREST_ENEMY_BUILDING -> unit.attackMoveNearestEnemyBuilding();
                    case NEAREST_ENEMY_UNIT -> unit.attackMoveNearestEnemyUnit(false);
                    case NEAREST_ENEMY_WORKER -> unit.attackMoveNearestEnemyUnit(true);
                }
            }
        }
    }

    static double getWeaponDamageModifier(Unit unit) {
        ItemStack itemStack = ((LivingEntity) unit).getItemBySlot(EquipmentSlot.MAINHAND);
        if (!itemStack.isEmpty()) {
            ItemAttributeModifiers mods = itemStack.get(DataComponents.ATTRIBUTE_MODIFIERS);
            if (mods == null) return 0;
            for (ItemAttributeModifiers.Entry entry : mods.modifiers()) {
                boolean applies = entry.slot() == EquipmentSlotGroup.MAINHAND || entry.slot() == EquipmentSlotGroup.HAND;
                if (applies && entry.attribute().is(Attributes.ATTACK_DAMAGE)
                        && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE)
                    return entry.modifier().amount();
            }
        }
        return 0;
    }

    // ==== merged from RangedAttackerUnit ====
    default com.solegendary.reignofnether.unit.goals.RangedAttackGroundGoal<?> getRangedAttackGroundGoal() { return null; }

    default void performUnitRangedAttack(LivingEntity pTarget, float velocity) {
        performUnitRangedAttack(pTarget.getX(), pTarget.getY(), pTarget.getZ(), velocity);
    }

    default void performUnitRangedAttack(double x, double y, double z, float velocity) { }

    // ==== merged from HeroUnit ====
    float EXP_REQ_MULTIPLIER = 1.6f;
    int MAX_LEVEL = 10;
    int MAX_NEUTRAL_EXP_LEVEL = 5;

    default boolean needsStatSync() { return false; }
    default void setNeedsStatSync(boolean value) { }
    default float getMana() { return 0; }
    default void setMana(float amount) { }
    default float getMaxMana() { return 0; }
    default void setMaxMana(float amount) { }
    default int getSkillPoints() { return 0; }
    default void setSkillPoints(int points) { }
    default boolean isRankUpMenuOpen() { return false; }
    default void showRankUpMenu(boolean show) { }
    default int getExperience() { return 0; }
    default void setExperience(int experience) { }
    default Object2ObjectArrayMap<HeroAbility, Integer> getHeroAbilityRanks() { return new Object2ObjectArrayMap<>(); }

    default float getHealthBonusPerLevel() {
        AttributeInstance attr = ((LivingEntity) this).getAttribute(AttributeHelpers.holder(AttributeRegistrar.MAX_HEALTH_BONUS_PER_LEVEL.get()));
        return (float) (attr != null ?  attr.getValue() : AttributeRegistrar.MAX_HEALTH_BONUS_PER_LEVEL.get().getDefaultValue());
    }
    default float getAttackBonusPerLevel() {
        AttributeInstance attr = ((LivingEntity) this).getAttribute(AttributeHelpers.holder(AttributeRegistrar.ATTACK_DAMAGE_BONUS_PER_LEVEL.get()));
        return (float) (attr != null ?  attr.getValue() : AttributeRegistrar.ATTACK_DAMAGE_BONUS_PER_LEVEL.get().getDefaultValue());
    }
    default float getBaseHealth() {
        AttributeInstance attr = ((LivingEntity) this).getAttribute(AttributeHelpers.holder(AttributeRegistrar.BASE_MAX_HEALTH.get()));
        return (float) (attr != null ?  attr.getValue() : AttributeRegistrar.BASE_MAX_HEALTH.get().getDefaultValue());
    }
    default float getBaseAttack() {
        AttributeInstance attr = ((LivingEntity) this).getAttribute(AttributeHelpers.holder(AttributeRegistrar.ATTACK_DAMAGE.get()));
        return (float) (attr != null ?  attr.getValue() : AttributeRegistrar.ATTACK_DAMAGE.get().getDefaultValue());
    }
    default float getBaseMaxMana() {
        AttributeInstance attr = ((LivingEntity) this).getAttribute(AttributeHelpers.holder(AttributeRegistrar.BASE_MAX_MANA.get()));
        return (float) (attr != null ?  attr.getValue() : AttributeRegistrar.BASE_MAX_MANA.get().getDefaultValue());
    }
    default float getManaRegenPerSecond() {
        AttributeInstance attr = ((LivingEntity) this).getAttribute(AttributeHelpers.holder(AttributeRegistrar.MANA_REGEN_PER_SECOND.get()));
        return (float) (attr != null ?  attr.getValue() : AttributeRegistrar.MANA_REGEN_PER_SECOND.get().getDefaultValue());
    }
    default float getManaBonusPerLevel() {
        AttributeInstance attr = ((LivingEntity) this).getAttribute(AttributeHelpers.holder(AttributeRegistrar.MAX_MANA_BONUS_PER_LEVEL.get()));
        return (float) (attr != null ?  attr.getValue() : AttributeRegistrar.MAX_MANA_BONUS_PER_LEVEL.get().getDefaultValue());
    }

    default int getChargesForSaveData() { return 0; }
    default void setChargesFromSaveData(int charges) { }

    default void setStatsForLevel() {
        setStatsForLevel(false);
    }

    default void setStatsForLevel(boolean heal) {
        AttributeInstance aiMaxHealth = ((LivingEntity) this).getAttribute(Attributes.MAX_HEALTH);
        if (aiMaxHealth != null)
            aiMaxHealth.setBaseValue(getBaseHealth() + ((getHeroLevel() - 1) * getHealthBonusPerLevel()));
        AttributeInstance aiAttackDamage = ((LivingEntity) this).getAttribute(Attributes.ATTACK_DAMAGE);
        if (aiAttackDamage != null)
            aiAttackDamage.setBaseValue(getBaseAttack() + ((getHeroLevel() - 1) * getAttackBonusPerLevel()));
        this.setMaxMana(getBaseMaxMana() + ((getHeroLevel() - 1) * getManaBonusPerLevel()));
        if (heal)
            ((LivingEntity) this).setHealth(((LivingEntity) this).getMaxHealth());
    }

    default void addExperience(int amount) {
        if (((LivingEntity) this).level().isClientSide())
            return;
        int levelBefore = getHeroLevel();
        if (levelBefore >= MAX_LEVEL)
            return;

        setExperience(getExperience() + amount);
        int levelDiff = getHeroLevel() - levelBefore;

        if (levelDiff > 0) {
            setSkillPoints(getSkillPoints() + levelDiff);
            SoundClientboundPacket.playSoundAtPos(SoundAction.LEVEL_UP, ((LivingEntity) this).getOnPos());
            ParticleUtil.addParticleExplosion(ParticleRegistrar.LEVEL_UP.get(), 10,
                    ((LivingEntity) this).level(), ((LivingEntity) this).getEyePosition());
            setStatsForLevel();
            ((LivingEntity) this).heal(levelDiff * getHealthBonusPerLevel());
        }
    }

    default int getHeroLevel() {
        return getHeroLevel(getExperience());
    }

    static int getHeroLevel(int exp) {
        int level = 0;
        int expToNextLevel = (int) (200 * EXP_REQ_MULTIPLIER);
        do {
            level += 1;
            exp -= expToNextLevel;
            expToNextLevel += (100 * EXP_REQ_MULTIPLIER);
        } while (exp >= 0 && level < MAX_LEVEL);
        return level;
    }

    default int getExpOnCurrentLevel() {
        if (getHeroLevel() >= MAX_LEVEL)
            return 0;
        int expToNextLevel = (int) (200 * EXP_REQ_MULTIPLIER);
        int expCount = 0;
        int exp = getExperience();
        while (expCount < exp) {
            if (expCount + expToNextLevel > exp) {
                return exp - expCount;
            }
            expCount += expToNextLevel;
            expToNextLevel += (100 * EXP_REQ_MULTIPLIER);
        }
        return 0;
    }

    default int getExpToNextlevel() {
        if (getHeroLevel() >= MAX_LEVEL)
            return 0;
        return (int) ((getHeroLevel() + 1) * (100 * EXP_REQ_MULTIPLIER));
    }

    default List<HeroAbility> getHeroAbilities() {
        List<HeroAbility> list = new ArrayList<>();
        for (Ability a : getAbilities().get()) {
            if (a instanceof HeroAbility heroAbility) {
                list.add(heroAbility);
            }
        }
        return list;
    }

    default void addHeroUnitSaveData(@NotNull CompoundTag pCompound) {
        pCompound.putInt("experience", getExperience());
        pCompound.putInt("skillPoints", getSkillPoints());
        pCompound.putInt("charges", getChargesForSaveData());
        pCompound.putFloat("mana", getMana());
        pCompound.putFloat("maxMana", getMaxMana());

        List<HeroAbility> abls = getHeroAbilities();
        pCompound.putInt("ability1Rank", abls.size() > 0 ? getHeroAbilityRank(abls.get(0)) : 0);
        pCompound.putInt("ability2Rank", abls.size() > 1 ? getHeroAbilityRank(abls.get(1)) : 0);
        pCompound.putInt("ability3Rank", abls.size() > 2 ? getHeroAbilityRank(abls.get(2)) : 0);
        pCompound.putInt("ability4Rank", abls.size() > 3 ? getHeroAbilityRank(abls.get(3)) : 0);
    }

    default void readHeroUnitSaveData(@NotNull CompoundTag pCompound) {
        setExperience(pCompound.getInt("experience"));
        setSkillPoints(pCompound.getInt("skillPoints"));
        setChargesFromSaveData(pCompound.getInt("charges"));
        setMana(pCompound.getFloat("mana"));
        setMaxMana(pCompound.getFloat("maxMana"));

        List<HeroAbility> abls = getHeroAbilities();
        if (abls.size() > 0) {
            setHeroAbilityRank(abls.get(0), pCompound.getInt("ability1Rank"));
        }
        if (abls.size() > 1) {
            setHeroAbilityRank(abls.get(1), pCompound.getInt("ability2Rank"));
        }
        if (abls.size() > 2) {
            setHeroAbilityRank(abls.get(2), pCompound.getInt("ability3Rank"));
        }
        if (abls.size() > 3) {
            setHeroAbilityRank(abls.get(3), pCompound.getInt("ability4Rank"));
        }
        for (HeroAbility abl : abls)
            abl.updateStatsForRank((Unit) this);
    }

    default void activateAbilityClientside(int abilityIndex) { }
    default void deactivateAbilityClientside(int abilityIndex) { }

    default int getHeroAbilityRank(HeroAbility ability) {
        return getHeroAbilityRanks().getOrDefault(ability, 0);
    }

    default void setHeroAbilityRank(HeroAbility ability, int rank) {
        getHeroAbilityRanks().put(ability, rank);
    }

    static void tickHero(Unit heroUnit) {
        if (heroUnit.needsStatSync()) {
            heroUnit.setStatsForLevel();
            heroUnit.setNeedsStatSync(false);
        }
        if (((LivingEntity) heroUnit).tickCount % 20 == 0) {
            heroUnit.setMana(heroUnit.getMana() + heroUnit.getManaRegenPerSecond());
        }
    }

    static AttributeSupplier.Builder createHeroDefaultAttributes() {
        return Unit.createDefaultAttributes()
                .add((AttributeHelpers.holder(AttributeRegistrar.BASE_MAX_HEALTH.get())), 1)
                .add((AttributeHelpers.holder(AttributeRegistrar.BASE_MAX_MANA.get())), 0)
                .add((AttributeHelpers.holder(AttributeRegistrar.MANA_REGEN_PER_SECOND.get())), 0)
                .add((AttributeHelpers.holder(AttributeRegistrar.MAX_MANA_BONUS_PER_LEVEL.get())), 0)
                .add((AttributeHelpers.holder(AttributeRegistrar.MAX_HEALTH_BONUS_PER_LEVEL.get())), 0)
                .add((AttributeHelpers.holder(AttributeRegistrar.ATTACK_DAMAGE_BONUS_PER_LEVEL.get())), 0);
    }

    static List<Unit> getHeroes(boolean isClientside, String ownerName) {
        return getHeroes(isClientside, ownerName, "");
    }

    static List<Unit> getHeroes(boolean isClientside, String ownerName, String unitName) {
        List<LivingEntity> units = isClientside ? UnitClientEvents.getAllUnits() : UnitServerEvents.getAllUnits();
        List<Unit> list = new ArrayList<>();
        for (LivingEntity e : units) {
            if (e instanceof Unit heroUnit && heroUnit.isHero() &&
                    heroUnit.getOwnerName().equals(ownerName) &&
                    (e.getType().getDescriptionId().equals(unitName) || unitName.isBlank())) {
                list.add(heroUnit);
            }
        }
        return list;
    }

    // if true, will ignore all commands except for stop (S)
    // used for things like channeling blizzard on the wraith to prevent accidental cancels
    default boolean ignoreNonStopCommands() {
        return false;
    }

    default void aggroToEnemyIfIdle(Unit aggroTarget) {
        if (((Entity) this).level().isClientSide())
            return;
        if (isIdle() && !AlliancesServerEvents.isAlliedOrOwned(this.getOwnerName(), aggroTarget.getOwnerName()))
            this.getTargetGoal().setTarget((LivingEntity) aggroTarget);
    }

    public default boolean hasRtsPlayerOwner() {
        RTSPlayer rtsPlayer = ((Entity) this).level().isClientSide() ?
                PlayerClientEvents.getRTSPlayer(getOwnerName()) :
                PlayerServerEvents.getRTSPlayer(getOwnerName());
        return rtsPlayer != null;
    }

    public default boolean isGarrisoned() {
        return getGarrison() != null;
    }

    public default BuildingPlacement getGarrison() {
        return GarrisonableBuildingAddon.getGarrison(this);
    }
}
