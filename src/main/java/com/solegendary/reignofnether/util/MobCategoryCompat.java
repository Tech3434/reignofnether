package com.solegendary.reignofnether.util;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;

/**
 * 1.21.1 removed {@code Mob#getMobType()}. What it returned is now data-driven: vanilla reads the
 * same classification back from the {@code #undead} / {@code #arthropod} / {@code #aquatic} entity
 * type tags (see {@code EntityTypeTags}), so an entity type can be classified from a datapack -
 * which is how undead and spider-like units keep the responses they had in 1.20.1: Smite, Bane of
 * Arthropods, inverted healing and harm, and the regeneration/poison immunity.
 *
 * <p>The call sites all asked the same question 1.20.1's {@code getMobType()} answered - "does this
 * mob report {@code MobType.UNDEAD}?" - so this helper answers exactly that. It must not be guessed
 * from the spawn category: every unit in this mod is registered as
 * {@link net.minecraft.world.entity.MobCategory#CREATURE} so it can spawn in peaceful and be
 * managed by the mod's own rules, so {@code instanceof Monster} is a different set - it misses the
 * undead units that do not extend {@link net.minecraft.world.entity.monster.Monster}.
 */
public final class MobCategoryCompat {
	private MobCategoryCompat() {}

	/** True when the entity is in {@code #minecraft:undead}, i.e. was {@code MobType.UNDEAD} in 1.20.1. */
	public static boolean isUndead(Entity entity) {
		return entity.getType().is(EntityTypeTags.UNDEAD);
	}
}
