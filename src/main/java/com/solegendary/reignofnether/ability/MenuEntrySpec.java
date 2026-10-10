package com.solegendary.reignofnether.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * One entry inside a data-driven menu ability ({@link DataMenuAbility}, plan CONTENT_JSON_PLAN.md):
 * an inline {@code ability} (which may itself be a {@code reignofnether:menu}, giving menu-in-menu),
 * a named {@code command} (one of the built-in player orders), or a {@code building} to place, with an
 * optional explicit grid position.
 *
 * <pre>
 * { "ability": { "type": "myns:rally" }, "row": 0, "col": 2 }
 * { "command": "stop" }
 * { "building": "myns:barracks" }
 * </pre>
 *
 * <p>{@code command} is one of {@code attack}, {@code stop}, {@code hold}, {@code build}, {@code gather},
 * {@code garrison}, {@code ungarrison}. {@code building} shows the same build/place button the worker's
 * build menu uses, but lets an author curate which buildings appear (and in what order). Production
 * entries are intentionally not part of a unit menu - a building lists its own {@code production}.
 *
 * <p>{@code row}/{@code col} are 0-based; {@code col} is the item column (the back button sits to the
 * left of column 0). Entries with a position are drawn at exactly that cell. Entries without one are
 * auto-placed by the HUD in list order using its own running index, which does not skip cells already
 * taken by an explicit position - so mixing explicit and auto-placed entries in one menu can overlap.
 */
public record MenuEntrySpec(
        Optional<AbilitySpec> ability,
        Optional<String> command,
        Optional<ResourceLocation> building,
        Optional<Integer> row,
        Optional<Integer> col
) {

    public static final Codec<MenuEntrySpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AbilitySpec.CODEC.optionalFieldOf("ability").forGetter(MenuEntrySpec::ability),
            Codec.STRING.optionalFieldOf("command").forGetter(MenuEntrySpec::command),
            ResourceLocation.CODEC.optionalFieldOf("building").forGetter(MenuEntrySpec::building),
            Codec.INT.optionalFieldOf("row").forGetter(MenuEntrySpec::row),
            Codec.INT.optionalFieldOf("col").forGetter(MenuEntrySpec::col)
    ).apply(instance, MenuEntrySpec::new));

    public boolean hasPosition() {
        return row.isPresent() || col.isPresent();
    }

    public int rowOr(int fallback) {
        return row.orElse(fallback);
    }

    public int colOr(int fallback) {
        return col.orElse(fallback);
    }
}
