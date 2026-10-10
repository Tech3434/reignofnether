package com.solegendary.reignofnether.building.buildings;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingBlockData;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingDefinition;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.ProductionSpec;
import com.solegendary.reignofnether.building.UpgradeSpec;
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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A building fully described by a {@link BuildingDefinition} (plan CONTENT_JSON_PLAN.md, phase 5):
 * the block layout comes from the definition's NBT structure, everything else from data.
 *
 * <p>Each datapack definition produces one instance per upgrade level: level 0 is the base definition,
 * level {@code n} is the base with upgrades {@code 1..n} applied cumulatively ({@link UpgradeSpec}).
 * A placement switches to the matching variant when it is upgraded, so production, researches, addons,
 * name, icon, max health and structure all follow the level automatically.
 */
public class JsonBuilding extends ProductionBuilding {

    private final ResourceLocation definitionId;
    private final BuildingDefinition definition;
    private final int level;

    public JsonBuilding(ResourceLocation definitionId, BuildingDefinition definition, ResourceCost cost) {
        this(definitionId, definition, cost, 0);
    }

    public JsonBuilding(ResourceLocation definitionId, BuildingDefinition definition, ResourceCost cost, int level) {
        super(definition.structure().getPath(), cost, definition.isCapitol());
        this.definitionId = definitionId;
        this.definition = definition;
        this.level = level;

        this.name = definition.name()
                .flatMap(m -> java.util.Optional.ofNullable(m.get("en_us")))
                .orElse(definitionId.getPath());
        this.icon = definition.icon()
                .orElse(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/stone.png"));
        this.portraitBlock = Blocks.STONE;
        this.maxHealth = definition.maxHealth();
        this.populationSupply = definition.populationSupply();
        this.requiredResearch = definition.requiredResearch();
        BuildingDefinition.Flags flags = definition.flags();
        this.canAcceptResources = flags.canAcceptResources();
        this.buildTimeModifier = (float) flags.buildTimeModifier();
        this.captureRange = flags.captureRange();
        this.capturable = flags.capturable();
        this.invulnerable = flags.invulnerable();
        this.repairable = flags.repairable();
        this.repairTimeModifier = (float) flags.repairTimeModifier();
        this.drawAggro = flags.drawAggro();
        this.foundationYLayers = Math.max(1, flags.foundationYLayers());
        flags.scaffoldFill().ifPresent(fill -> this.scaffoldFill = fill);
        flags.scaffoldBlock().ifPresent(id -> {
            net.minecraft.world.level.block.Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id);
            if (block != null)
                this.scaffoldBlock = block;
        });
        flags.portrait().ifPresent(id -> {
            net.minecraft.world.level.block.Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id);
            if (block != null)
                this.portraitBlock = block;
        });

        for (ProductionSpec spec : definition.production())
            this.productions.add(new JsonProductionItem(spec.unit(), cost, spec.unit().getPath(),
                    spec.costOverride().orElse(null)), Keybindings.abilitySlot1);

        for (ResourceLocation researchId : definition.researches())
            this.productions.add(com.solegendary.reignofnether.research.ResearchProductionItem.fromId(researchId),
                    Keybindings.abilitySlot1);

        int upgradeLevel = 1;
        for (UpgradeSpec upgrade : definition.upgrades())
            this.productions.add(new JsonUpgradeProductionItem(upgradeLevel++, definitionId, upgrade, this.icon),
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

    /**
     * Two variants of the same definition (upgrade levels are separate instances) are the same type
     * of building: this keeps "has a finished one" checks and the build menu working after an
     * upgrade replaces the placement's building with a higher-level variant.
     */
    @Override
    public boolean isTypeOf(Building building) {
        if (building instanceof JsonBuilding other)
            return this.definitionId.equals(other.definitionId);
        return super.isTypeOf(building);
    }

    /**
     * Derives the foundation block types from the structure NBT (the bottom layer), the same way a
     * custom building does. Without this, {@code startingBlockTypes} stays empty, so a freshly placed
     * JSON building queues no blocks and {@link com.solegendary.reignofnether.building.BuildingPlacement#shouldBeDestroyed()}
     * can treat the still-empty placement as destroyed on the first server tick.
     */
    @Override
    public ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        ArrayList<BuildingBlock> blocks = BuildingBlockData.getBuildingBlocksFromNbt(definition.structure(), level);
        if (this.startingBlockTypes.isEmpty() && !blocks.isEmpty()) {
            int minY = Integer.MAX_VALUE;
            for (BuildingBlock block : blocks)
                minY = Math.min(minY, block.getBlockPos().getY());
            // the bottom `foundationYLayers` Y layers are the foundation: their block types are pre-queued
            // so the placement is never empty (see BuildingPlacement#shouldBeDestroyed)
            int foundationMaxY = minY + Math.max(1, this.foundationYLayers) - 1;
            for (BuildingBlock block : blocks) {
                if (block.getBlockPos().getY() > foundationMaxY || block.getBlockState().isAir())
                    continue;
                Block type = block.getBlockState().getBlock();
                if (!this.startingBlockTypes.contains(type))
                    this.startingBlockTypes.add(type);
            }
        }
        return blocks;
    }

    public BuildingDefinition getDefinition() {
        return definition;
    }

    public int getLevel() {
        return level;
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
