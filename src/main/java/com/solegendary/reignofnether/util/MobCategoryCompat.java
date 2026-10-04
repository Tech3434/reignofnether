package com.solegendary.reignofnether.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;

/**
 * 1.21.1 removed {@code Mob#getMobType()}; the category is now read off the {@code EntityType}
 * instead. That is not a drop-in replacement here: every unit in this mod is registered as
 * {@link MobCategory#CREATURE} so it can spawn in peaceful and be managed by the mod's own rules,
 * whereas in 1.20.1 {@code getMobType()} was inherited from the vanilla superclass and answered
 * {@code MONSTER} for anything extending {@link Monster} (or a Vindicator, which is a Monster).
 *
 * <p>The old call sites asked "is this a hostile mob", not "what category was this registered
 * under", so they test the superclass chain instead.
 */
public final class MobCategoryCompat {
	private MobCategoryCompat() {}

	/** True when the entity would have reported {@link MobCategory#MONSTER} in 1.20.1. */
	public static boolean isMonster(LivingEntity entity) {
		return entity instanceof Monster;
	}
}
