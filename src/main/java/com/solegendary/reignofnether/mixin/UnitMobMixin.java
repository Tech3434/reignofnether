package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.ability.Abilities;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.Checkpoint;
import com.solegendary.reignofnether.unit.goals.GarrisonGoal;
import com.solegendary.reignofnether.unit.goals.MoveToTargetBlockGoal;
import com.solegendary.reignofnether.unit.goals.ReturnResourcesGoal;
import com.solegendary.reignofnether.unit.goals.SelectedTargetGoal;
import com.solegendary.reignofnether.unit.interfaces.Unit;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Makes every {@link Mob} an {@link Unit} carrying the generic unit state (plan `CONTENT_JSON_PLAN.md`).
 *
 * <p>A mob is only a real RTS unit when it has a definition attached - {@link #isRtsUnit()} returns true
 * only when {@code ron$definitionId} is set (by a data-driven spawn). Every {@Unit.isUnit(code)}
 * check in the codebase must be gated on {@code isRtsUnit()}, otherwise all vanilla mobs would be
 * treated as units. Code unit classes (the templates) override {@code isRtsUnit()} back to true.
 *
 * <p>Behaviour (goals, attributes, abilities) is definition-driven and filled in by later phases; for
 * now the state is inert defaults.
 */
@Mixin(Mob.class)
public abstract class UnitMobMixin extends LivingEntity implements Unit, com.solegendary.reignofnether.unit.interfaces.DefinedUnit {

    @Unique private String ron$ownerName = "";
    @Unique private BlockPos ron$anchor;
    @Unique private ArrayList<Checkpoint> ron$checkpoints;
    @Unique private Abilities ron$abilities;
    @Unique private List<ItemStack> ron$items;
    @Unique private MoveToTargetBlockGoal ron$moveGoal;
    @Unique private SelectedTargetGoal<?> ron$targetGoal;
    @Unique private ReturnResourcesGoal ron$returnResourcesGoal;
    @Unique private GarrisonGoal ron$garrisonGoal;
    @Unique private net.minecraft.world.entity.ai.goal.Goal ron$attackGoal;
    @Unique private net.minecraft.world.entity.ai.goal.Goal ron$attackBuildingGoal;
    @Unique private com.solegendary.reignofnether.unit.goals.GatherResourcesGoal ron$gatherGoal;
    @Unique private com.solegendary.reignofnether.unit.goals.BuildRepairGoal ron$buildRepairGoal;
    @Unique private com.solegendary.reignofnether.unit.goals.ExploreBuildLocationGoal ron$exploreBuildLocationGoal;
    @Unique private boolean ron$willRetaliate = false;
    @Unique private boolean ron$aggressiveWhenIdle = false;
    @Unique private ResourceCost ron$cost;
    @Unique private LivingEntity ron$followTarget;
    @Unique private boolean ron$holdPosition = false;
    @Unique private String ron$onDeathCommand = "";
    @Unique private int ron$maxResources = 0;
    @Unique private Object2ObjectArrayMap<Ability, Float> ron$cooldowns;
    @Unique private Object2ObjectArrayMap<Ability, Integer> ron$charges;
    @Unique private Set<Ability> ron$autocast;
    @Unique private ResourceLocation ron$definitionId;
    @Unique private int ron$experience = 0;
    @Unique private float ron$mana = 0;
    @Unique private float ron$maxMana = 0;
    @Unique private int ron$skillPoints = 0;
    @Unique private boolean ron$needsStatSync = false;
    @Unique private boolean ron$rankUpMenuOpen = false;
    @Unique private int ron$saveCharges = 0;
    @Unique private it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap<com.solegendary.reignofnether.ability.HeroAbility, Integer> ron$heroAbilityRanks;

    protected UnitMobMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean isRtsUnit() {
        return ron$definitionId != null;
    }

    @Override
    public boolean isWorker() {
        return ron$role() == com.solegendary.reignofnether.unit.UnitDefinition.Role.WORKER;
    }

    @Override
    public boolean isAttacker() {
        com.solegendary.reignofnether.unit.UnitDefinition.Role r = ron$role();
        return r == com.solegendary.reignofnether.unit.UnitDefinition.Role.MELEE
                || r == com.solegendary.reignofnether.unit.UnitDefinition.Role.RANGED
                || r == com.solegendary.reignofnether.unit.UnitDefinition.Role.HERO;
    }

    @Override
    public boolean isRangedAttacker() {
        return ron$role() == com.solegendary.reignofnether.unit.UnitDefinition.Role.RANGED;
    }

    @Override
    public boolean isHero() {
        return ron$role() == com.solegendary.reignofnether.unit.UnitDefinition.Role.HERO;
    }

    // ---- hero state (plan CONTENT_JSON_PLAN.md: role: hero) ----
    @Override public boolean needsStatSync() { return ron$needsStatSync; }
    @Override public void setNeedsStatSync(boolean value) { ron$needsStatSync = value; }
    @Override public float getMana() { return ron$mana; }
    @Override public void setMana(float amount) { ron$mana = amount; }
    @Override public float getMaxMana() { return ron$maxMana; }
    @Override public void setMaxMana(float amount) { ron$maxMana = amount; }
    @Override public int getSkillPoints() { return ron$skillPoints; }
    @Override public void setSkillPoints(int points) { ron$skillPoints = points; }
    @Override public boolean isRankUpMenuOpen() { return ron$rankUpMenuOpen; }
    @Override public void showRankUpMenu(boolean show) { ron$rankUpMenuOpen = show; }
    @Override public int getExperience() { return ron$experience; }
    @Override public void setExperience(int experience) { ron$experience = experience; }
    @Override public int getChargesForSaveData() { return ron$saveCharges; }
    @Override public void setChargesFromSaveData(int charges) { ron$saveCharges = charges; }

    @Override
    public it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap<com.solegendary.reignofnether.ability.HeroAbility, Integer> getHeroAbilityRanks() {
        if (ron$heroAbilityRanks == null)
            ron$heroAbilityRanks = new it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap<>();
        return ron$heroAbilityRanks;
    }

    @Unique private com.solegendary.reignofnether.unit.UnitDefinition.Role ron$roleCache;
    @Unique private boolean ron$roleResolved = false;

    @Unique
    private com.solegendary.reignofnether.unit.UnitDefinition.Role ron$role() {
        if (ron$roleResolved)
            return ron$roleCache;
        ron$roleResolved = true;
        if (ron$definitionId == null)
            return null;
        // resolve() so a unit that inherits its role from a parent definition reports it correctly
        com.solegendary.reignofnether.unit.UnitDefinition def = ron$definition();
        ron$roleCache = def == null ? null : def.roleOrDefault();
        return ron$roleCache;
    }

    @Unique private com.solegendary.reignofnether.unit.UnitDefinition ron$definitionCache;
    @Unique private boolean ron$definitionResolved = false;

    @Unique
    @Nullable
    private com.solegendary.reignofnether.unit.UnitDefinition ron$definition() {
        if (ron$definitionResolved)
            return ron$definitionCache;
        ron$definitionResolved = true;
        if (ron$definitionId == null)
            return null;
        ron$definitionCache = com.solegendary.reignofnether.unit.UnitDefinitions.resolve(
                level().registryAccess(), ron$definitionId);
        return ron$definitionCache;
    }

    /**
     * Data-driven ranged attack (plan CONTENT_JSON_PLAN.md): spawns the definition's {@code projectile}
     * (default {@code minecraft:arrow}) toward the target, using the unit's attack damage when the spec
     * does not override it. Server-authoritative.
     */
    @Override
    public void performUnitRangedAttack(double x, double y, double z, float velocity) {
        if (level().isClientSide())
            return;
        com.solegendary.reignofnether.unit.UnitDefinition def = ron$definition();
        com.solegendary.reignofnether.unit.UnitDefinition.ProjectileSpec spec =
                def != null ? def.projectile().orElse(null) : null;

        EntityType<?> type = spec != null
                ? BuiltInRegistries.ENTITY_TYPE.get(spec.entity())
                : EntityType.ARROW;
        if (type == null)
            return;
        Entity projectileEntity = type.create(level());
        if (projectileEntity == null)
            return;

        double speed = spec != null && spec.velocity() > 0 ? spec.velocity() : 1.6;
        double inaccuracy = spec != null ? spec.inaccuracy() : 1.0;
        double damage = spec != null && spec.damage() >= 0
                ? spec.damage() : ((Unit) (Object) this).getUnitAttackDamage();

        projectileEntity.setPos(this.getX(), this.getEyeY() - 0.1, this.getZ());
        if (projectileEntity instanceof Projectile projectile) {
            projectile.setOwner((LivingEntity) (Object) this);
            double dx = x - this.getX();
            double dy = (y + 0.4) - this.getEyeY();
            double dz = z - this.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            projectile.shoot(dx, dy + horizontal * 0.2, dz, (float) speed, (float) inaccuracy);
        }
        if (projectileEntity instanceof AbstractArrow arrow)
            arrow.setBaseDamage(damage);
        level().addFreshEntity(projectileEntity);
        level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 1.0F,
                1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
    }

    /**
     * Data-driven units tick themselves: cooldowns/checkpoints plus the role-specific tick
     * (worker/attacker/hero). The old code unit classes did this in their own tick() overrides.
     */
    @org.spongepowered.asm.mixin.injection.Inject(
            method = "tick",
            at = @org.spongepowered.asm.mixin.injection.At("TAIL")
    )
    private void ron$tickUnit(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (ron$definitionId == null)
            return;
        Unit self = (Unit) (Object) this;
        Unit.tick(self);
        if (isWorker())
            Unit.tickWorker(self);
        if (isAttacker())
            Unit.tickAttacker(self);
        if (isHero())
            Unit.tickHero(self);
    }

    @Override
    public ResourceLocation getUnitDefinitionId() {
        return ron$definitionId;
    }

    @Override
    public void setUnitDefinitionId(ResourceLocation id) {
        ron$definitionId = id;
        ron$roleResolved = false;
        ron$roleCache = null;
        ron$definitionResolved = false;
        ron$definitionCache = null;
    }

    // The removed code unit classes used to call Unit#addUnitSaveData from their addAdditionalSaveData
    // override; data-driven units have no such class, so persist the generic unit state here.
    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void ron$saveUnitData(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        if (isRtsUnit())
            ((Unit) (Object) this).addUnitSaveData(tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void ron$readUnitData(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        ((Unit) (Object) this).readUnitSaveData(tag);
        // a unit loaded from a save has its definition id only now, and the removed code units built
        // their goals in their own constructor - without this a reloaded unit would have no goals at
        // all. There is no registerGoals() hook for the mixin, despite what older comments claimed.
        // initialiseGoals is idempotent (each goal is only added when its field is still null).
        if (isRtsUnit())
            ((Unit) (Object) this).initialiseGoals();
    }

    @Shadow protected net.minecraft.world.entity.ai.goal.GoalSelector goalSelector;
    @Shadow protected net.minecraft.world.entity.ai.goal.GoalSelector targetSelector;

    @Override
    public void setAnchor(BlockPos bp) {
        ron$anchor = bp;
    }

    @Override
    public BlockPos getAnchor() {
        return ron$anchor;
    }

    @Override
    public ArrayList<Checkpoint> getCheckpoints() {
        if (ron$checkpoints == null)
            ron$checkpoints = new ArrayList<>();
        return ron$checkpoints;
    }

    @Override
    public GarrisonGoal getGarrisonGoal() {
        return ron$garrisonGoal;
    }

    @Override
    public boolean canGarrison() {
        return ron$garrisonGoal != null;
    }

    @Override
    public net.minecraft.world.entity.ai.goal.Goal getAttackGoal() {
        return ron$attackGoal;
    }

    @Override
    public net.minecraft.world.entity.ai.goal.Goal getAttackBuildingGoal() {
        return ron$attackBuildingGoal;
    }

    @Override
    public boolean canAttackBuildings() {
        return ron$attackBuildingGoal != null;
    }

    @Override
    public com.solegendary.reignofnether.unit.goals.GatherResourcesGoal getGatherResourceGoal() {
        return ron$gatherGoal;
    }

    @Override
    public com.solegendary.reignofnether.unit.goals.BuildRepairGoal getBuildRepairGoal() {
        return ron$buildRepairGoal;
    }

    @Override
    public com.solegendary.reignofnether.unit.goals.ExploreBuildLocationGoal getExploreBuildLocationGoal() {
        return ron$exploreBuildLocationGoal;
    }

    @Override
    public boolean getWillRetaliate() {
        return ron$willRetaliate;
    }

    @Override
    public boolean getAggressiveWhenIdle() {
        return ron$aggressiveWhenIdle;
    }

    @Override
    public Abilities getAbilities() {
        if (ron$abilities == null)
            ron$abilities = new Abilities();
        return ron$abilities;
    }

    @Override
    public List<ItemStack> getItems() {
        if (ron$items == null)
            ron$items = new ArrayList<>();
        return ron$items;
    }

    @Override
    public int getMaxResources() {
        // derived lazily from the definition: nothing assigns ron$maxResources elsewhere, so without
        // this a data-driven worker would report 0 and Unit.atMaxResources would always be true
        if (ron$maxResources > 0)
            return ron$maxResources;
        com.solegendary.reignofnether.unit.UnitDefinition def = ron$definition();
        if (def != null)
            ron$maxResources = def.carryCapacityOrDefault();
        return ron$maxResources;
    }

    @Override
    public MoveToTargetBlockGoal getMoveGoal() {
        return ron$moveGoal;
    }

    @Override
    public SelectedTargetGoal<?> getTargetGoal() {
        return ron$targetGoal;
    }

    @Override
    public ReturnResourcesGoal getReturnResourcesGoal() {
        return ron$returnResourcesGoal;
    }

    @Override
    public ResourceCost getCost() {
        // the removed code unit classes returned their static ResourceCost here; a data-driven unit
        // takes it from its definition. Never return null: callers read cost.population directly.
        if (ron$cost != null)
            return ron$cost;
        com.solegendary.reignofnether.unit.UnitDefinition def = ron$definition();
        if (def == null)
            return ResourceCost.Unit(0, 0, 0, 0, 0);
        com.solegendary.reignofnether.unit.UnitDefinition.CostSpec c = def.cost().orElse(null);
        ResourceCost cost = c == null
                ? ResourceCost.Unit(0, 0, 0, 0, def.populationOrDefault())
                : ResourceCost.Unit(c.food(), c.wood(), c.ore(), c.seconds(), def.populationOrDefault());
        if (c != null)
            cost.emerald = c.emerald();
        ron$cost = cost;
        return ron$cost;
    }

    @Override
    public LivingEntity getFollowTarget() {
        return ron$followTarget;
    }

    @Override
    public boolean getHoldPosition() {
        return ron$holdPosition;
    }

    @Override
    public void setHoldPosition(boolean holdPosition) {
        ron$holdPosition = holdPosition;
    }

    @Override
    public String getOwnerName() {
        return ron$ownerName == null ? "" : ron$ownerName;
    }

    @Override
    public void setOwnerName(String name) {
        ron$ownerName = name == null ? "" : name;
    }

    @Override
    public String getOnDeathCommand() {
        return ron$onDeathCommand == null ? "" : ron$onDeathCommand;
    }

    @Override
    public void setOnDeathCommand(String command) {
        ron$onDeathCommand = command == null ? "" : command;
    }

    @Override
    public void setFollowTarget(@Nullable LivingEntity target) {
        ron$followTarget = target;
    }

    @Override
    public void initialiseGoals() {
        if (ron$definitionId == null)
            return;
        // resolve() so goals come from the effective (inherited) definition, not just this file's fields
        com.solegendary.reignofnether.unit.UnitDefinition def = ron$definition();
        if (def == null)
            return;
        Mob self = (Mob) (Object) this;
        Unit me = (Unit) (Object) this;

        // behaviour flags from the definition (hold-position etc.)
        ron$holdPosition = def.flagsOrDefault().holdPosition();

        if (ron$moveGoal == null) {
            ron$moveGoal = new MoveToTargetBlockGoal(self, false, 0);
            this.goalSelector.addGoal(5, ron$moveGoal);
        }
        if (ron$targetGoal == null) {
            ron$targetGoal = new SelectedTargetGoal<>(self, false, true);
            this.targetSelector.addGoal(5, ron$targetGoal);
        }

        var role = def.roleOrDefault();
        boolean worker = role == com.solegendary.reignofnether.unit.UnitDefinition.Role.WORKER;
        boolean melee = role == com.solegendary.reignofnether.unit.UnitDefinition.Role.MELEE
                || role == com.solegendary.reignofnether.unit.UnitDefinition.Role.HERO;
        boolean ranged = role == com.solegendary.reignofnether.unit.UnitDefinition.Role.RANGED;

        if (melee || ranged) {
            ron$willRetaliate = true;
            ron$aggressiveWhenIdle = true;
        }

        // a hero starts at level 1: apply its per-level stats once
        if (role == com.solegendary.reignofnether.unit.UnitDefinition.Role.HERO)
            me.setStatsForLevel(true);

        if (melee) {
            if (ron$attackGoal == null) {
                ron$attackGoal = new com.solegendary.reignofnether.unit.goals.MeleeAttackUnitGoal(self, false);
                this.goalSelector.addGoal(2, ron$attackGoal);
            }
            if (ron$attackBuildingGoal == null) {
                ron$attackBuildingGoal = new com.solegendary.reignofnether.unit.goals.MeleeAttackBuildingGoal(self);
                this.goalSelector.addGoal(2, ron$attackBuildingGoal);
            }
        } else if (ranged) {
            if (ron$attackGoal == null) {
                com.solegendary.reignofnether.unit.goals.UnitBowAttackGoal<?> bow =
                        new com.solegendary.reignofnether.unit.goals.UnitBowAttackGoal<>(self);
                ron$attackGoal = bow;
                this.goalSelector.addGoal(2, ron$attackGoal);
                ron$attackBuildingGoal = new com.solegendary.reignofnether.unit.goals.RangedAttackBuildingGoal<>(self, bow);
                this.goalSelector.addGoal(2, ron$attackBuildingGoal);
            }
        }

        if ((def.flagsOrDefault().canGather() || worker) && ron$gatherGoal == null) {
            ron$gatherGoal = new com.solegendary.reignofnether.unit.goals.GatherResourcesGoal(self);
            this.goalSelector.addGoal(2, ron$gatherGoal);
        }
            if (def.flagsOrDefault().canBuild()) {
            if (ron$buildRepairGoal == null) {
                ron$buildRepairGoal = new com.solegendary.reignofnether.unit.goals.BuildRepairGoal(self);
                this.goalSelector.addGoal(2, ron$buildRepairGoal);
            }
            if (ron$exploreBuildLocationGoal == null) {
                ron$exploreBuildLocationGoal = new com.solegendary.reignofnether.unit.goals.ExploreBuildLocationGoal(me);
                this.goalSelector.addGoal(3, ron$exploreBuildLocationGoal);
            }
        }
        if (worker && ron$returnResourcesGoal == null) {
            ron$returnResourcesGoal = new ReturnResourcesGoal(self);
            this.goalSelector.addGoal(2, ron$returnResourcesGoal);
        }
            if (def.flagsOrDefault().canGarrison() && ron$garrisonGoal == null) {
            ron$garrisonGoal = new GarrisonGoal(self);
            this.goalSelector.addGoal(2, ron$garrisonGoal);
        }
    }

    @Override
    public void updateAbilityButtons() {
        // definition-driven: built in a later phase
    }

    @Override
    public Object2ObjectArrayMap<Ability, Float> getCooldowns() {
        if (ron$cooldowns == null)
            ron$cooldowns = Unit.createCooldownMap();
        return ron$cooldowns;
    }

    @Override
    public Object2ObjectArrayMap<Ability, Integer> getCharges() {
        if (ron$charges == null) {
            ron$charges = new Object2ObjectArrayMap<>();
            ron$charges.defaultReturnValue(0);
        }
        return ron$charges;
    }

    @Override
    public boolean hasAutocast(Ability ability) {
        return ron$autocast != null && ron$autocast.contains(ability);
    }

    @Override
    public void setAutocast(Ability ability) {
        if (ron$autocast == null)
            ron$autocast = new HashSet<>();
        if (!ron$autocast.remove(ability))
            ron$autocast.add(ability);
    }
}
