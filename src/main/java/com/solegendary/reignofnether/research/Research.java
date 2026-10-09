package com.solegendary.reignofnether.research;

import com.solegendary.reignofnether.resources.ResourceCost;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * A single technology.
 *
 * <p>The "type" is code ({@link ResearchType}); the instance is data: a name, an icon, the researches
 * that must precede it (each possibly inverted), a cost, an optional list of attribute boosts and an
 * optional list of equipment grants. Registered in {@link ResearchRegistry} - from code in phase 1,
 * additionally from datapack JSON in phase 4.
 */
public class Research {

    private final ResourceLocation id;
    private final String nameKey;
    private final ResourceLocation icon;
    private final ResourceCost cost; // nullable: free / GM-granted researches have none
    private final List<ResearchCondition> prerequisites;
    private final List<ResearchAttributeModifier> attributeModifiers;
    private final List<ResearchEquipModifier> equipModifiers;
    private final ResearchType type;

    public Research(ResourceLocation id, String nameKey, ResourceLocation icon, ResourceCost cost,
                    List<ResearchCondition> prerequisites,
                    List<ResearchAttributeModifier> attributeModifiers,
                    List<ResearchEquipModifier> equipModifiers, ResearchType type) {
        this.id = id;
        this.nameKey = nameKey;
        this.icon = icon;
        this.cost = cost;
        this.prerequisites = List.copyOf(prerequisites);
        this.attributeModifiers = List.copyOf(attributeModifiers);
        this.equipModifiers = List.copyOf(equipModifiers);
        this.type = type;
    }

    /** Convenience overload without equipment grants. */
    public Research(ResourceLocation id, String nameKey, ResourceLocation icon, ResourceCost cost,
                    List<ResearchCondition> prerequisites,
                    List<ResearchAttributeModifier> attributeModifiers, ResearchType type) {
        this(id, nameKey, icon, cost, prerequisites, attributeModifiers, List.of(), type);
    }

    public ResourceLocation getId() {
        return id;
    }

    public String getNameKey() {
        return nameKey;
    }

    public ResourceLocation getIcon() {
        return icon;
    }

    public ResourceCost getCost() {
        return cost;
    }

    public List<ResearchCondition> getPrerequisites() {
        return prerequisites;
    }

    public List<ResearchAttributeModifier> getAttributeModifiers() {
        return attributeModifiers;
    }

    public List<ResearchEquipModifier> getEquipModifiers() {
        return equipModifiers;
    }

    public ResearchType getType() {
        return type;
    }
}
