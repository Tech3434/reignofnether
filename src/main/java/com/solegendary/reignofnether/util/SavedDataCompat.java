package com.solegendary.reignofnether.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Bridges the mod's {@code SavedData} subclasses to 1.21.1's storage API.
 *
 * <p>In 1.20.1 {@code DimensionDataStorage#computeIfAbsent} took a loader and a supplier
 * directly: {@code computeIfAbsent(Class::load, Class::create, "saved-building-data")}. 1.21.1
 * wraps the pair in a {@code SavedData.Factory} and hands the deserializer a
 * {@code HolderLookup.Provider} alongside the tag.
 *
 * <p>None of the mod's saved data deserializes registry objects, so the provider is accepted and
 * ignored. That keeps every existing {@code static X load(CompoundTag)} untouched.
 */
public final class SavedDataCompat {

    private SavedDataCompat() { }

    public static <T extends SavedData> T computeIfAbsent(MinecraftServer server, String name,
                                                          Supplier<T> constructor,
                                                          Function<CompoundTag, T> loader) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(constructor,
                        (CompoundTag tag, HolderLookup.Provider registries) -> loader.apply(tag)),
                name);
    }

    public static <T extends SavedData> T get(MinecraftServer server, String name,
                                              Supplier<T> constructor,
                                              Function<CompoundTag, T> loader) {
        return server.overworld().getDataStorage().get(
                new SavedData.Factory<>(constructor,
                        (CompoundTag tag, HolderLookup.Provider registries) -> loader.apply(tag)),
                name);
    }

    /**
     * Load-path accessor: the stored data if the world has any, otherwise a fresh empty instance.
     *
     * <p>{@link #computeIfAbsent} registers a brand new entry and marks the storage dirty, so calling
     * it just to read on world load makes every world write an empty file for that entry. This never
     * registers anything, so reading is free of side effects; the save path is what creates the entry.
     */
    public static <T extends SavedData> T getLoaded(MinecraftServer server, String name,
                                                    Supplier<T> constructor,
                                                    Function<CompoundTag, T> loader) {
        T stored = get(server, name, constructor, loader);
        return stored != null ? stored : constructor.get();
    }

    /** True when the world already has data stored under this name (without creating it). */
    public static <T extends SavedData> boolean isStored(MinecraftServer server, String name,
                                                         Supplier<T> constructor,
                                                         Function<CompoundTag, T> loader) {
        return get(server, name, constructor, loader) != null;
    }
}
