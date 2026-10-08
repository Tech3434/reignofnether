package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.util.MobEffectHelpers;
import com.solegendary.reignofnether.registrars.MobEffectRegistrar;
import com.solegendary.reignofnether.resources.ResourceSources;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
        // only units get the mod's flight trail; vanilla entities with levitation are untouched
        if (this.level().isClientSide() && Unit.isUnit(this) && this.hasEffect(MobEffects.LEVITATION))
            MiscUtil.spawnFlyingCloudParticles(this);
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
        if (Unit.isAttacker(pDamageSource.getEntity()))
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

        if (!(pDamageSource.getEntity() instanceof Unit attackerUnit))
            return -1.0F;

        // ensure projectiles from units do the damage of the unit, not the item,
        // and that armour and anti-armour effects are considered through absorption
        boolean isHuntableAnimal = ResourceSources.isHuntableAnimal((LivingEntity) (Object) this);

        float dmg = attackerUnit.getUnitAttackDamage();
        if (isMelee && !(Unit.isWorker(pDamageSource.getEntity())))
            dmg += Unit.getWeaponDamageModifier(attackerUnit);

        if (isHuntableAnimal && !(Unit.isWorker(pDamageSource.getEntity()))) {
            dmg *= 0.5f;
        }

        if (this instanceof Unit unit && unit.isRtsUnit()) {
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
        if (this instanceof Unit unit && unit.isRtsUnit() && unit.uninterruptable()
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
