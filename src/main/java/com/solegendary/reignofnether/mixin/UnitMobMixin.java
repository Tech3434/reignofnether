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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

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
        com.solegendary.reignofnether.unit.UnitDefinition def = level().registryAccess()
                .registryOrThrow(com.solegendary.reignofnether.unit.UnitDefinitions.UNIT_KEY)
                .get(ron$definitionId);
        ron$roleCache = def == null ? null : def.role();
        return ron$roleCache;
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
        com.solegendary.reignofnether.unit.UnitDefinition def = level().registryAccess()
                .registryOrThrow(com.solegendary.reignofnether.unit.UnitDefinitions.UNIT_KEY)
                .get(ron$definitionId);
        if (def == null)
            return;
        Mob self = (Mob) (Object) this;
        Unit me = (Unit) (Object) this;

        if (ron$moveGoal == null) {
            ron$moveGoal = new MoveToTargetBlockGoal(self, false, 0);
            this.goalSelector.addGoal(5, ron$moveGoal);
        }
        if (ron$targetGoal == null) {
            ron$targetGoal = new SelectedTargetGoal<>(self, false, true);
            this.targetSelector.addGoal(5, ron$targetGoal);
        }

        var role = def.role();
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

        if ((def.flags().canGather() || worker) && ron$gatherGoal == null) {
            ron$gatherGoal = new com.solegendary.reignofnether.unit.goals.GatherResourcesGoal(self);
            this.goalSelector.addGoal(2, ron$gatherGoal);
        }
        if (def.flags().canBuild()) {
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
        if (def.flags().canGarrison() && ron$garrisonGoal == null) {
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
