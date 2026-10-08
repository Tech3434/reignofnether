package com.solegendary.reignofnether.resources;

/** Applies a {@link ResourceGenerator}'s output to the owning player. */
public final class ResourceGenerators {

    private ResourceGenerators() { }

    /** Adds one production tick of the generator to its owner. Server-side only. */
    public static void produce(String ownerName, ResourceGenerator generator) {
        if (ownerName == null || ownerName.isEmpty())
            return;
        int amount = generator.getResourceAmount();
        if (amount <= 0)
            return;

        Resources produced = switch (generator.getResourceType()) {
            case FOOD -> new Resources(ownerName, amount, 0, 0);
            case WOOD -> new Resources(ownerName, 0, amount, 0);
            case ORE -> new Resources(ownerName, 0, 0, amount);
            case EMERALD -> new Resources(ownerName, 0, 0, 0, amount);
            case NONE -> null;
        };
        if (produced != null)
            ResourcesServerEvents.addSubtractResources(produced);
    }
}
