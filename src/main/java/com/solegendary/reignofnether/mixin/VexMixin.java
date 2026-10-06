package com.solegendary.reignofnether.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

// make vexes follow their parent evoker while out of combat or out of range of their target
@Mixin(Vex.class)
public abstract class VexMixin extends Mob {

    @Unique private static final Random reignofnether$random = new Random();

    @Shadow public abstract Mob getOwner();

    protected VexMixin(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(
            method = "tick",
            at = @At("HEAD")
    )
    public void tick(CallbackInfo ci) {
    }
}
