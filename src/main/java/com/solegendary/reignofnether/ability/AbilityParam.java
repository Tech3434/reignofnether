package com.solegendary.reignofnether.ability;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * A single data-driven ability parameter (plan CONTENT_JSON_PLAN.md): a JSON number or string. Strings
 * are how an ability can name something (an entity/sound/item via a {@link ResourceLocation}); numbers
 * are the class-specific magnitudes (amount, radius, …).
 */
public record AbilityParam(Optional<Double> number, Optional<String> string) {

    public static final Codec<AbilityParam> CODEC = Codec.either(Codec.DOUBLE, Codec.STRING).xmap(
            either -> either.map(
                    d -> new AbilityParam(Optional.of(d), Optional.empty()),
                    s -> new AbilityParam(Optional.empty(), Optional.of(s))),
            param -> param.number().<Either<Double, String>>map(Either::left)
                    .orElseGet(() -> Either.right(param.string().orElse(""))));

    public double asDouble(double fallback) {
        return number.orElse(fallback);
    }

    public String asString(String fallback) {
        return string.orElseGet(() -> number.map(String::valueOf).orElse(fallback));
    }

    @Nullable
    public ResourceLocation asResource() {
        return string.map(ResourceLocation::tryParse).orElse(null);
    }
}
