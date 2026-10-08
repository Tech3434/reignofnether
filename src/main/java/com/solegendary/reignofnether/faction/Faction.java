package com.solegendary.reignofnether.faction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * A playable faction, defined in datapacks at {@code data/<namespace>/faction/<name>.json}
 * (id = {@code <namespace>:<name>}).
 *
 * <p>A faction is the object a match start reads from: which capitol to place, which units to hand
 * the player and with how many resources to begin. Units and buildings opt into a faction through
 * their own faction id.
 *
 * <p>Schema:
 * <pre>
 * {
 *   "name": "faction.<ns>.<id>",
 *   "icon": "minecraft:textures/block/polished_granite.png",
 *   "capitol": "reignofnether:town_centre",
 *   "starting_units": [ { "unit": "reignofnether:villager_unit", "count": 1 } ],
 *   "food": 0, "wood": 0, "ore": 0, "emerald": 0
 * }
 * </pre>
 */
public record Faction(
        String nameKey,
        ResourceLocation icon,
        ResourceLocation capitol,
        List<StartingUnit> startingUnits,
        int startingFood,
        int startingWood,
        int startingOre,
        int startingEmerald
) {

    private static final ResourceLocation DEFAULT_ICON =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/polished_granite.png");

    public static final Codec<Faction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(Faction::nameKey),
            ResourceLocation.CODEC.optionalFieldOf("icon", DEFAULT_ICON).forGetter(Faction::icon),
            ResourceLocation.CODEC.fieldOf("capitol").forGetter(Faction::capitol),
            StartingUnit.CODEC.listOf().optionalFieldOf("starting_units", List.of()).forGetter(Faction::startingUnits),
            Codec.INT.optionalFieldOf("food", 0).forGetter(Faction::startingFood),
            Codec.INT.optionalFieldOf("wood", 0).forGetter(Faction::startingWood),
            Codec.INT.optionalFieldOf("ore", 0).forGetter(Faction::startingOre),
            Codec.INT.optionalFieldOf("emerald", 0).forGetter(Faction::startingEmerald)
    ).apply(instance, Faction::new));
}
