package com.solegendary.reignofnether.building.buildings;

import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingDefinition;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.addon.AddonSpec;
import com.solegendary.reignofnether.building.addon.AddonTypes;
import com.solegendary.reignofnether.building.addon.BuildingAddon;
import com.solegendary.reignofnether.building.production.JsonProductionItem;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;

import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A building fully described by a {@link BuildingDefinition} (plan CONTENT_JSON_PLAN.md, phase 5):
 * the block layout comes from the definition's NBT structure, everything else from data. One instance
 * is created per datapack building definition.
 */
public class JsonBuilding extends ProductionBuilding {

    private final ResourceLocation definitionId;
    private final BuildingDefinition definition;

    public JsonBuilding(ResourceLocation definitionId, BuildingDefinition definition, ResourceCost cost) {
        super(definition.structure().getPath(), cost, definition.isCapitol());
        this.definitionId = definitionId;
        this.definition = definition;

        this.name = definition.name()
                .flatMap(m -> java.util.Optional.ofNullable(m.get("en_us")))
                .orElse(definitionId.getPath());
        this.icon = definition.icon()
                .orElse(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/stone.png"));
        this.portraitBlock = Blocks.STONE;
        this.maxHealth = definition.maxHealth();

        for (ResourceLocation unitId : definition.production())
            this.productions.add(new JsonProductionItem(unitId, cost, unitId.getPath()), Keybindings.abilitySlot1);

        for (ResourceLocation researchId : definition.researches())
            this.productions.add(com.solegendary.reignofnether.research.ResearchProductionItem.fromId(researchId),
                    Keybindings.abilitySlot1);

        for (AddonSpec spec : definition.addons()) {
            BuildingAddon addon = AddonTypes.create(spec, this);
            if (addon != null)
                addActiveAddon(addon);
        }
    }

    public ResourceLocation getDefinitionId() {
        return definitionId;
    }

    public BuildingDefinition getDefinition() {
        return definition;
    }

    /** Resolves one of this building's production/research items by its {@link ProductionItem#getNetworkId()}. */
    @Nullable
    public ProductionItem getProductionItem(String networkId) {
        for (ProductionItem item : this.productions.get())
            if (item.getNetworkId().equals(networkId))
                return item;
        return null;
    }

    public String getFaction() {
        return "";
    }

    @Override
    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        return new BuildingPlaceButton(
                this.name,
                this.icon,
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == this,
                () -> true,
                () -> true,
                List.of(FormattedCharSequence.forward(this.name, Style.EMPTY.withBold(true))),
                this
        );
    }
}
