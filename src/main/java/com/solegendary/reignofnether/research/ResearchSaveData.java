package com.solegendary.reignofnether.research;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.util.SavedDataCompat;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-player completed researches, persisted with the world.
 *
 * <p>Reads use {@link #getLoaded(LevelAccessor)} so merely asking "is this researched?" never writes an
 * empty file (see the lazy-SavedData rule). Writes go through {@link #getInstance(LevelAccessor)}.
 */
public class ResearchSaveData extends SavedData {

    /** playerName -> researched ids. */
    public final Map<String, Set<ResourceLocation>> researched = new HashMap<>();

    private static ResearchSaveData create() {
        return new ResearchSaveData();
    }

    @Nonnull
    public static ResearchSaveData getInstance(LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null)
            return create();
        return SavedDataCompat.computeIfAbsent(server, "saved-research-data", ResearchSaveData::create, ResearchSaveData::load);
    }

    /** Read-only accessor: no entry is created when the world has none. */
    @Nonnull
    public static ResearchSaveData getLoaded(LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null)
            return create();
        return SavedDataCompat.getLoaded(server, "saved-research-data", ResearchSaveData::create, ResearchSaveData::load);
    }

    public boolean hasResearch(String playerName, ResourceLocation id) {
        Set<ResourceLocation> set = researched.get(playerName);
        return set != null && set.contains(id);
    }

    public Set<ResourceLocation> getFor(String playerName) {
        return researched.getOrDefault(playerName, Set.of());
    }

    public void grant(String playerName, ResourceLocation id) {
        researched.computeIfAbsent(playerName, k -> new HashSet<>()).add(id);
        setDirty();
    }

    public void revoke(String playerName, ResourceLocation id) {
        Set<ResourceLocation> set = researched.get(playerName);
        if (set != null && set.remove(id))
            setDirty();
    }

    /** Clears every research of one player (used on defeat). */
    public void clear(String playerName) {
        if (researched.remove(playerName) != null)
            setDirty();
    }

    public static ResearchSaveData load(CompoundTag tag) {
        //ReignOfNether.LOGGER.info("ResearchSaveData.load");
        ResearchSaveData data = create();
        ListTag players = (ListTag) tag.get("players");
        if (players != null) {
            for (Tag pTagRaw : players) {
                CompoundTag pTag = (CompoundTag) pTagRaw;
                String playerName = pTag.getString("name");
                Set<ResourceLocation> set = new HashSet<>();
                ListTag ids = (ListTag) pTag.get("researched");
                if (ids != null)
                    for (Tag idTag : ids) {
                        ResourceLocation id = ResourceLocation.tryParse(idTag.getAsString());
                        if (id != null)
                            set.add(id);
                    }
                data.researched.put(playerName, set);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag players = new ListTag();
        this.researched.forEach((playerName, ids) -> {
            CompoundTag pTag = new CompoundTag();
            pTag.putString("name", playerName);
            ListTag idList = new ListTag();
            ids.forEach(id -> idList.add(StringTag.valueOf(id.toString())));
            pTag.put("researched", idList);
            players.add(pTag);
        });
        tag.put("players", players);
        return tag;
    }

    public void save() {
        this.setDirty();
    }
}
