package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.util.MobEffectHelpers;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.resources.ResourceSources;

import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnitProfession;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    public LivingEntityMixin(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(
            method = "tick",
            at = @At("TAIL")
    )
    public void tick(CallbackInfo ci) {
        if (this.level().isClientSide())
            if (this.hasEffect(MobEffects.LEVITATION))
                MiscUtil.spawnFlyingCloudParticles(this);
    }

    @Inject(
            method = "onChangedBlock",
            at = @At("TAIL"),
            cancellable = true
    )
    protected void onChangedBlock(ServerLevel pLevel, BlockPos pPos, CallbackInfo ci) {
    }

    // copied from FrostWalkerEnchantment.onEntityMoved
    private void FrostWalkerOnEntityMoved(LivingEntity pLiving, Level pLevel, BlockPos pPos, int pLevelConflicting) {
        if (pLiving.onGround()) {

            float f = (float)Math.min(16, 2 + pLevelConflicting);
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
            Iterator var7 = BlockPos.betweenClosed(pPos.offset((int) -f, (int) -1.0, (int) -f), pPos.offset((int) f, (int) -1.0, (int) f)).iterator();

            while(true) {
                BlockPos blockpos;
                BlockState blockstate1;
                do {
                    do {
                        if (!var7.hasNext()) {
                            return;
                        }
                        blockpos = (BlockPos)var7.next();
                    } while(!blockpos.closerToCenterThan(pLiving.position(), f));

                    blockpos$mutableblockpos.set(blockpos.getX(), blockpos.getY() + 1, blockpos.getZ());
                    blockstate1 = pLevel.getBlockState(blockpos$mutableblockpos);
                } while(!blockstate1.isAir());

                BlockState blockstate2 = pLevel.getBlockState(blockpos);
                boolean isFull = blockstate2.getBlock() == Blocks.WATER && blockstate2.getValue(LiquidBlock.LEVEL) == 0;

                BlockState iceState = Blocks.FROSTED_ICE.defaultBlockState();
                if (blockstate2.getFluidState().is(FluidTags.WATER) && isFull &&
                        pLevel.isUnobstructed(iceState, blockpos, CollisionContext.empty())) {

                    pLevel.setBlockAndUpdate(blockpos, iceState);
                    pLevel.scheduleTick(blockpos, Blocks.FROSTED_ICE, Mth.nextInt(pLiving.getRandom(), 60, 120));
                }

                isFull = blockstate2.getBlock() == Blocks.LAVA && blockstate2.getValue(LiquidBlock.LEVEL) == 0;
                BlockState magmaState = Blocks.NETHERRACK.defaultBlockState();
                if (blockstate2.getFluidState().is(FluidTags.LAVA) && isFull &&
                        pLevel.isUnobstructed(magmaState, blockpos, CollisionContext.empty())) {

                    pLevel.setBlockAndUpdate(blockpos, magmaState);
                    pLevel.scheduleTick(blockpos, Blocks.NETHERRACK, Mth.nextInt(pLiving.getRandom(), 60, 120));
                }
            }
        }
    }

    @Shadow protected float getDamageAfterArmorAbsorb(DamageSource pDamageSource, float pDamageAmount) { return 0f; }
    @Shadow public boolean isInvulnerableTo(DamageSource pDamageSource) { return false; }

    // In 1.20.1 this hook cancelled actuallyHurt and re-applied the damage by hand, because the
    // Forge damage hooks (onLivingHurt/onLivingDamage) had to be called explicitly there. NeoForge
    // 1.21.1 routes the whole thing through DamageContainer: LivingEntity#hurt pushes a container,
    // fires CommonHooks.onEntityIncomingDamage, and actuallyHurt then reads it back. So instead of
    // cancelling and hand-rolling the damage, we rewrite the incoming damage on the container and
    // let vanilla apply armour/absorption/enchantments/game events as usual. The one behaviour the
    // old copy had was skipping vanilla armour absorption (units use their own armour percentage),
    // which the second redirect preserves.
    @Redirect(
            method = "hurt",
            at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/CommonHooks;onEntityIncomingDamage(Lnet/minecraft/world/entity/LivingEntity;Lnet/neoforged/neoforge/common/damagesource/DamageContainer;)Z")
    )
    private boolean ron$applyUnitAttackDamage(LivingEntity pSelf, DamageContainer pContainer) {
        float dmg = this.ron$unitAttackDamage(pContainer.getSource());
        if (dmg < 0.0F) // not a unit hit, let the vanilla path (and the event) run untouched
            return CommonHooks.onEntityIncomingDamage(pSelf, pContainer);

        if (dmg <= 0.0F || pSelf.isInvulnerableTo(pContainer.getSource()))
            return true; // ci.cancel() in the old copy: no damage at all

        pContainer.setNewDamage(dmg);
        return CommonHooks.onEntityIncomingDamage(pSelf, pContainer);
    }

    @Redirect(
            method = "actuallyHurt",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F")
    )
    // INVOKEVIRTUAL keeps the receiver in the handler's parameters, so the entity being damaged
    // comes first; the rest are the target call's own arguments.
    private float ron$skipVanillaArmour(LivingEntity pSelf, DamageSource pDamageSource, float pAmount) {
        // unit armour is applied as a percentage on the incoming damage instead
        if (pDamageSource.getEntity() instanceof AttackerUnit)
            return pAmount;
        return this.getDamageAfterArmorAbsorb(pDamageSource, pAmount);
    }

    // returns -1 when the hit is not an attacker-unit hit and must be left to vanilla
    private float ron$unitAttackDamage(DamageSource pDamageSource) {
        boolean isProjectile = pDamageSource.is(DamageTypeTags.IS_PROJECTILE);
        boolean isMelee = pDamageSource.is(DamageTypes.MOB_ATTACK) && !isProjectile;

        // the old copy only hijacked plain hits: projectiles, or melee that isn't bypassing
        // shields/armour/resistance
        if (!isProjectile && !(isMelee &&
                !pDamageSource.is(DamageTypeTags.WITCH_RESISTANT_TO) &&
                !pDamageSource.is(DamageTypeTags.BYPASSES_SHIELD) &&
                !pDamageSource.is(DamageTypeTags.BYPASSES_ARMOR) &&
                !pDamageSource.is(DamageTypeTags.BYPASSES_RESISTANCE)))
            return -1.0F;

        if (!(pDamageSource.getEntity() instanceof AttackerUnit attackerUnit))
            return -1.0F;

        // ensure projectiles from units do the damage of the unit, not the item,
        // and that armour and anti-armour effects are considered through absorption
        boolean isHuntableAnimal = ResourceSources.isHuntableAnimal((LivingEntity) (Object) this);

        float dmg = attackerUnit.getUnitAttackDamage();
        if (isMelee && !(pDamageSource.getEntity() instanceof WorkerUnit))
            dmg += AttackerUnit.getWeaponDamageModifier(attackerUnit);

        if (isHuntableAnimal) {
            if (pDamageSource.getEntity() instanceof VillagerUnit vUnit &&
                    vUnit.getUnitProfession() == VillagerUnitProfession.HUNTER) {
                dmg = vUnit.isVeteran() ? 2f : 1.5f;
            } else if (!(pDamageSource.getEntity() instanceof WorkerUnit)) {
                dmg *= 0.5f;
            }
        }

        if (this instanceof Unit unit) {
            dmg *= (1 - unit.getUnitPhysicalArmorPercentage());
            if (isProjectile)
                dmg *= (1 - unit.getUnitRangedArmorPercentage());
            dmg *= (1 - unit.getUnitResistPercentage());
        }

        return dmg;
    }

    @Shadow public boolean hasEffect(Holder<MobEffect> pEffect) { return true; }
    @Shadow public MobEffectInstance getEffect(Holder<MobEffect> pEffect) { return null; }

    // MobEffectEvent.Added stopped being cancellable in 1.21.1, so uninterruptable units drop
    // interrupting effects here instead, before they are ever added.
    @Inject(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ron$blockInterrupt(MobEffectInstance pInstance, Entity pSource, CallbackInfoReturnable<Boolean> cir) {
        if (this instanceof Unit unit && unit.uninterruptable()
                && MobEffectRegistrar.isInterrupt(pInstance.getEffect()))
            cir.setReturnValue(false);
    }

    @Inject(
            method = "baseTick",
            at = @At("TAIL")
    )
    public void baseTick(CallbackInfo ci) {
        Holder<MobEffect> intenseHeat = MobEffectHelpers.holder(MobEffectRegistrar.INTENSE_HEAT.get());
        if (!this.level().isClientSide && this.remainingFireTicks > 0 && !fireImmune() && hasEffect(intenseHeat)) {
            int amp = Math.min(39, getEffect(intenseHeat).getAmplifier());
            int fireTicks = (this.remainingFireTicks + 10);
            if (fireTicks % (80 - (amp * 2)) == 0) {
                this.hurt(this.damageSources().onFire(), 1.0F);
            }
        }
    }
}
