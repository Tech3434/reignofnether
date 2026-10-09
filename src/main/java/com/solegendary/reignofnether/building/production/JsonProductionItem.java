package com.solegendary.reignofnether.building.production;

import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.UnitDefinition;
import com.solegendary.reignofnether.unit.UnitDefinitions;

import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data-driven production item: instead of a code class per unit, it spawns a unit definition
 * ({@code reignofnether:villager_unit}) from the building's production queue (plan CONTENT_JSON_PLAN.md).
 * Its button name/icon come from the unit definition (client side), falling back to the id.
 */
public class JsonProductionItem extends ProductionItem {

    private final ResourceLocation unitDefinitionId;
    private final String fallbackName;

    public JsonProductionItem(ResourceLocation unitDefinitionId, ResourceCost cost, String displayName) {
        super(cost);
        this.unitDefinitionId = unitDefinitionId;
        this.fallbackName = displayName;
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

    /** The unit definition, resolved on the client so the button can use its name/icon. */
    @Nullable
    private UnitDefinition resolveDefinition() {
        if (FMLEnvironment.dist != Dist.CLIENT)
            return null;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null)
            return null;
        return UnitDefinitions.resolve(mc.level.registryAccess(), unitDefinitionId);
    }

    @Override
    public String getItemName() {
        UnitDefinition def = resolveDefinition();
        if (def != null) {
            Optional<String> name = def.name().flatMap(m -> Optional.ofNullable(m.get("en_us")));
            if (name.isPresent())
                return name.get();
        }
        return fallbackName;
    }

    @Override
    public ResourceLocation getIcon() {
        UnitDefinition def = resolveDefinition();
        if (def != null && def.icon().isPresent())
            return def.icon().get();
        return ResourceLocation.withDefaultNamespace("textures/item/iron_sword.png");
    }

    @Override
    public List<FormattedCharSequence> getTooltipLines() {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(FormattedCharSequence.forward(getItemName(), Style.EMPTY.withBold(true)));
        if (defaultCost.food > 0 || defaultCost.wood > 0 || defaultCost.ore > 0 || defaultCost.emerald > 0)
            lines.add(ResourceCosts.getFormattedCost(defaultCost));
        if (defaultCost.population > 0)
            lines.add(ResourceCosts.getFormattedPopAndTime(defaultCost));
        else
            lines.add(ResourceCosts.getFormattedTime(defaultCost));
        return lines;
    }
}
