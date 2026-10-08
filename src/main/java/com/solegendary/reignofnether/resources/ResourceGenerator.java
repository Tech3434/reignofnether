package com.solegendary.reignofnether.resources;

/**
 * A resource generator a building can host (plan H.9).
 *
 * <p>Deliberately left abstract: the faction author supplies concrete generators. The framework only
 * fixes the shape - how often it runs, how much it makes, what kind, and an optional storage cap - so
 * a faction can wire one in without touching core code. Use {@link ResourceGenerators#produce} to
 * apply a tick's output to the owner.
 */
public interface ResourceGenerator {

    /** Ticks between production ticks (20 = once per second). */
    int getTickInterval();

    /** Amount produced each production tick. */
    int getResourceAmount();

    /** Which resource is produced. */
    ResourceName getResourceType();

    /** Optional cap on stored resources; {@code <= 0} means uncapped. */
    default int getCapacity() {
        return -1;
    }
}
