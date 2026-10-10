package com.solegendary.reignofnether.building.addon;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.resources.ResourceGenerator;
import com.solegendary.reignofnether.resources.ResourceGenerators;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;

import net.minecraft.world.level.Level;

/**
 * Engine data-driven addon (plan H.9): a building that passively generates a resource for its owner.
 * Enables it with
 * {@code { "type": "reignofnether:resource_generator", "params": { "resource": "wood", "amount": 5, "interval": 100, "capacity": 500 } }}.
 *
 * <p>Params: {@code resource} (one of {@code food}/{@code wood}/{@code ore}/{@code emerald}, default
 * {@code food}), {@code amount} (per production tick; 0 disables), {@code interval} (ticks between
 * ticks, default 20 = once per second) and optional {@code capacity} ({@code <= 0} = uncapped).
 */
public class ResourceGeneratorAddon implements BuildingAddon, ResourceGenerator {

    private final ResourceName resourceType;
    private final int amount;
    private final int interval;
    private final int capacity;
    private int ticks = 0;

    public ResourceGeneratorAddon(AddonSpec spec) {
        this.resourceType = parseResource(spec.stringParam("resource", "food"));
        this.amount = (int) spec.param("amount", 0);
        this.interval = Math.max(1, (int) spec.param("interval", 20));
        this.capacity = (int) spec.param("capacity", -1);
    }

    @Override
    public int getTickInterval() {
        return interval;
    }

    @Override
    public int getResourceAmount() {
        return amount;
    }

    @Override
    public ResourceName getResourceType() {
        return resourceType;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public void onBuildingTick(Level level, BuildingPlacement placement) {
        if (level.isClientSide() || !placement.isBuilt)
            return;
        if (amount <= 0 || resourceType == ResourceName.NONE)
            return;
        if (++ticks < interval)
            return;
        ticks = 0;
        if (atCapacity(placement.ownerName))
            return;
        ResourceGenerators.produce(placement.ownerName, this);
    }

    private boolean atCapacity(String ownerName) {
        if (capacity <= 0 || ownerName == null || ownerName.isEmpty())
            return false;
        for (Resources resources : ResourcesServerEvents.resourcesList) {
            if (!resources.ownerName.equals(ownerName))
                continue;
            int current = switch (resourceType) {
                case FOOD -> resources.food;
                case WOOD -> resources.wood;
                case ORE -> resources.ore;
                case EMERALD -> resources.emerald;
                case NONE -> 0;
            };
            return current >= capacity;
        }
        return false;
    }

    private static ResourceName parseResource(String name) {
        if (name == null)
            return ResourceName.NONE;
        try {
            return ResourceName.valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ResourceName.NONE;
        }
    }
}
