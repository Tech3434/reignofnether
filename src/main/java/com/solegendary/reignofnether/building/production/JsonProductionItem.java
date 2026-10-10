package com.solegendary.reignofnether.building.production;

import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.UnitDefinition;
import com.solegendary.reignofnether.unit.UnitDefinitions;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data-driven production item: instead of a code class per unit, it spawns a unit definition
 * ({@code reignofnether:villager_unit}) from the building's production queue (plan CONTENT_JSON_PLAN.md).
 * Its button name/icon and its cost come from the unit definition (falling back to the item's own cost).
 */
public class JsonProductionItem extends ProductionItem {

    private final ResourceLocation unitDefinitionId;
    private final String fallbackName;
    private final UnitDefinition.CostSpec costOverride; // nullable: null = use the unit definition's own cost

    public JsonProductionItem(ResourceLocation unitDefinitionId, ResourceCost cost, String displayName) {
        this(unitDefinitionId, cost, displayName, null);
    }

    public JsonProductionItem(ResourceLocation unitDefinitionId, ResourceCost cost, String displayName,
                              @Nullable UnitDefinition.CostSpec costOverride) {
        super(cost);
        this.unitDefinitionId = unitDefinitionId;
        this.fallbackName = displayName;
        this.costOverride = costOverride;
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide())
                placement.produceUnit((ServerLevel) level, unitDefinitionId, placement.ownerName, true, new Vec3i(0, 0, 0));
        };
    }

    public ResourceLocation getUnitDefinitionId() {
        return unitDefinitionId;
    }

    @Override
    public String getNetworkId() {
        return unitDefinitionId.toString();
    }

    /**
     * Resolves the unit definition on either side (client uses its synced registry, server the current
     * server). Null when the definition is unknown, in which case callers fall back to {@code defaultCost}.
     */
    @Nullable
    private UnitDefinition resolveDefinition(boolean isClientSide) {
        RegistryAccess access;
        if (isClientSide) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            access = mc.level == null ? null : mc.level.registryAccess();
        } else {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            access = server == null ? null : server.registryAccess();
        }        return access == null ? null : UnitDefinitions.resolve(access, unitDefinitionId);
    }

    /**
     * The unit's own cost (the building only lists <em>which</em> units it trains). Without this the item
     * would fall back to the building's cost, which is usually absent, making units free and instant.
     */
    @Override
    public ResourceCost getCost(boolean isClientSide, String ownerName) {
        UnitDefinition def = resolveDefinition(isClientSide);
        int population = def == null ? 1 : def.population().orElse(1);
        // an explicit costOverride replaces the unit's own cost entirely
        UnitDefinition.CostSpec spec = costOverride != null ? costOverride
                : def != null ? def.cost().orElse(null) : null;
        if (spec != null) {
            ResourceCost unitCost = ResourceCost.Unit(spec.food(), spec.wood(), spec.ore(), spec.seconds(), population);
            unitCost.emerald = spec.emerald();
            return unitCost;
        }
        return super.getCost(isClientSide, ownerName);
    }

    @Override
    public String getItemName() {
        UnitDefinition def = resolveDefinition(true);
        if (def != null) {
            Optional<String> name = def.name().flatMap(m -> Optional.ofNullable(m.get("en_us")));
            if (name.isPresent())
                return name.get();
        }
        return fallbackName;
    }

    @Override
    public ResourceLocation getIcon() {
        UnitDefinition def = resolveDefinition(true);
        if (def != null && def.icon().isPresent())
            return def.icon().get();
        return ResourceLocation.withDefaultNamespace("textures/item/iron_sword.png");
    }

    @Override
    public List<FormattedCharSequence> getTooltipLines() {
        ResourceCost cost = getCost(true, "");
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(FormattedCharSequence.forward(getItemName(), Style.EMPTY.withBold(true)));
        if (cost.food > 0 || cost.wood > 0 || cost.ore > 0 || cost.emerald > 0)
            lines.add(ResourceCosts.getFormattedCost(cost));
        if (cost.population > 0)
            lines.add(ResourceCosts.getFormattedPopAndTime(cost));
        else
            lines.add(ResourceCosts.getFormattedTime(cost));
        return lines;
    }
}
