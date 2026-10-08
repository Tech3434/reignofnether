package com.solegendary.reignofnether.ability;

/**
 * Digging speed tiers (plan §14.4). A unit's tier is a property of the unit itself
 * ({@link com.solegendary.reignofnether.unit.interfaces.Unit#getDigToolTier()}, iron by default) and
 * only scales how fast a block is broken - the same idea as vanilla tool speed, without real items.
 */
public enum DigToolTier {
    STONE(2f),
    IRON(4f),
    DIAMOND(6f),
    NETHERITE(8f);

    /** Relative break speed; higher breaks blocks faster. */
    public final float speed;

    DigToolTier(float speed) {
        this.speed = speed;
    }
}
