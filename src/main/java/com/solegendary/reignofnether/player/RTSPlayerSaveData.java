package com.solegendary.reignofnether.player;

import net.minecraft.core.HolderLookup;
import com.solegendary.reignofnether.ReignOfNether;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import com.solegendary.reignofnether.util.SavedDataCompat;

public class RTSPlayerSaveData extends SavedData {

    public final ArrayList<RTSPlayer> rtsPlayers = new ArrayList<>();

    private static RTSPlayerSaveData create() {
        return new RTSPlayerSaveData();
    }

    @Nonnull
    public static RTSPlayerSaveData getInstance(LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return create();
        }
        return SavedDataCompat.computeIfAbsent(server, "saved-rtsplayer-data", RTSPlayerSaveData::create, RTSPlayerSaveData::load);
    }

    /** Read-only accessor for world load: no entry is created when the world has none. */
    @Nonnull
    public static RTSPlayerSaveData getLoaded(LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null)
            return create();
        return SavedDataCompat.getLoaded(server, "saved-rtsplayer-data", RTSPlayerSaveData::create, RTSPlayerSaveData::load);
    }

    public static boolean isStored(LevelAccessor level) {
        MinecraftServer server = level.getServer();
        return server != null && SavedDataCompat.isStored(server, "saved-rtsplayer-data", RTSPlayerSaveData::create, RTSPlayerSaveData::load);
    }

    public static RTSPlayerSaveData load(CompoundTag tag) {
        ReignOfNether.LOGGER.info("RTSPlayerSaveData.load");

        RTSPlayerSaveData data = create();
        ListTag ltag = (ListTag) tag.get("rtsplayers");

        if (ltag != null) {
            for (Tag ctag : ltag) {
                CompoundTag ptag = (CompoundTag) ctag;

                String name = ptag.getString("name");
                int id = ptag.getInt("id");
                int ticksWithoutCapitol = ptag.getInt("ticksWithoutCapitol");
                int startPosColorId = ptag.getInt("startPosColorId");
                int[] scores = ptag.contains("scores") ? ptag.getIntArray("scores") : new RTSPlayerScores().getScoreListAsArray();
                boolean hasEverOwnedBuilding = ptag.getBoolean("hasEverOwnedBuilding");

                data.rtsPlayers.add(RTSPlayer.getFromSave(name, id, ticksWithoutCapitol, startPosColorId, scores, hasEverOwnedBuilding));

                ReignOfNether.LOGGER.info("RTSPlayerSaveData.load: " + name + "|" + id);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        //ReignOfNether.LOGGER.info("RTSPlayerSaveData.save");

        ListTag list = new ListTag();
        this.rtsPlayers.forEach(p -> {
            CompoundTag cTag = new CompoundTag();
            cTag.putString("name", p.name);
            cTag.putInt("id", p.id);
            cTag.putInt("ticksWithoutCapitol", p.ticksWithoutCapitol);
            cTag.putInt("startPosColorId", p.startPosColorId);
            cTag.putBoolean("hasEverOwnedBuilding", p.hasEverOwnedBuilding);
            cTag.putIntArray("scores", p.scores.getScoreListAsArray());
            list.add(cTag);
        });
        tag.put("rtsplayers", list);
        return tag;
    }

    public void save() {
        this.setDirty();
    }
}
