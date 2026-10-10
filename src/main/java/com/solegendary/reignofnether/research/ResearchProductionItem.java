package com.solegendary.reignofnether.research;

import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProdDupeRule;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A research offered by a building (plan CONTENT_JSON_PLAN.md / RESEARCH_AND_EXTENSIBILITY_PLAN.md):
 * it is a {@link ProductionItem}, so it shares the building's production queue, occupies a slot, and is
 * cancelled with a refund like anything else. Completing it grants the research to the building's owner.
 *
 * <p>A building opts in with {@code "researches": [ "myns:some_research" ]}; the item is created from
 * the research definition (code/datapack on the server, the synced client mirror on a client).
 */
public class ResearchProductionItem extends ProductionItem {

    private final ResourceLocation researchId;
    private final String nameKey;
    @Nullable private final ResourceLocation icon;
    private final List<ResearchCondition> prerequisites;

    /** One shared item per research id, so duplicate prevention across a player's buildings works. */
    private static final Map<ResourceLocation, ResearchProductionItem> CACHE = new HashMap<>();

    public ResearchProductionItem(ResourceLocation researchId, String nameKey, @Nullable ResourceLocation icon,
                                  List<ResearchCondition> prerequisites, @Nullable ResourceCost cost) {
        super(cost != null ? cost : ResourceCost.Research(0, 0, 0, 0));
        this.researchId = researchId;
        this.nameKey = nameKey;
        this.icon = icon;
        this.prerequisites = List.copyOf(prerequisites);
        this.dupeRule = ProdDupeRule.DISALLOW;
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (level.isClientSide())
                return;
            ResearchSaveData data = ResearchSaveData.getInstance(level);
            data.grant(placement.ownerName, researchId);
            data.save();
            ResearchClientboundPacket.sync(placement.ownerName, data.getFor(placement.ownerName));
            ResearchAttributeApplier.refreshForOwner(level, placement.ownerName);
            ResearchEquipApplier.refreshForOwner(level, placement.ownerName);
        };
    }

    /** Resolves a research id to its item: server/datapack definition first, then the client mirror. */
    public static ResearchProductionItem fromId(ResourceLocation researchId) {
        ResearchProductionItem cached = CACHE.get(researchId);
        if (cached != null)
            return cached;
        ResearchProductionItem created = create(researchId);
        CACHE.put(researchId, created);
        return created;
    }

    /** Drops cached items so a datapack reload re-reads names/icons/prerequisites. */
    public static void clearCache() {
        CACHE.clear();
    }

    private static ResearchProductionItem create(ResourceLocation researchId) {
        Research research = ResearchRegistry.get(researchId);
        if (research != null)
            return new ResearchProductionItem(research.getId(), research.getNameKey(), research.getIcon(),
                    research.getPrerequisites(), research.getCost());

        ResearchClientEvents.Def def = ResearchClientEvents.definition(researchId);
        if (def != null)
            return new ResearchProductionItem(def.id(), def.nameKey(), def.icon(), def.prerequisites(), null);

        return new ResearchProductionItem(researchId, researchId.toString(), null, List.of(), null);
    }

    public ResourceLocation getResearchId() {
        return researchId;
    }

    @Override
    public String getItemName() {
        return nameKey;
    }

    @Override
    public String getNetworkId() {
        return researchId.toString();
    }

    @Override
    @Nullable
    public ResourceLocation getIcon() {
        return icon;
    }

    @Override
    public String getProduceErrorMsg(ProductionPlacement pp) {
        boolean client = pp.getLevel().isClientSide();
        boolean alreadyDone = client
                ? ResearchUtils.isResearchedClient(pp.ownerName, researchId)
                : ResearchUtils.isResearched(pp.getLevel(), pp.ownerName, researchId);
        if (alreadyDone)
            return "hud.research.reignofnether.already_researched";

        boolean prereqsMet = client
                ? ResearchUtils.meetsClient(pp.ownerName, prerequisites)
                : ResearchUtils.meets(pp.getLevel(), pp.ownerName, prerequisites);
        if (!prereqsMet)
            return "hud.research.reignofnether.prerequisites_not_met";
        return null;
    }

    @Override
    public List<FormattedCharSequence> getTooltipLines() {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(FormattedCharSequence.forward(I18n.get(nameKey), Style.EMPTY.withBold(true)));
        if (defaultCost.food > 0 || defaultCost.wood > 0 || defaultCost.ore > 0)
            lines.add(ResourceCosts.getFormattedCost(defaultCost));
        lines.add(ResourceCosts.getFormattedTime(defaultCost));
        return lines;
    }
}
