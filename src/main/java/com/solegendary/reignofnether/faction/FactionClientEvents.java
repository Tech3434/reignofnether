package com.solegendary.reignofnether.faction;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side mirror of the registered factions plus the state of the faction-selection menu.
 *
 * <p>The menu is opened by the server (the {@code /startrts} command): with no faction named every
 * faction is selectable; with a faction named only that one is, the rest are shown but disabled, the
 * same way locked abilities/buildings are.
 */
public final class FactionClientEvents {

    /** A faction as shown in the menu. */
    public record Info(ResourceLocation id, String nameKey, ResourceLocation icon) { }

    private static final Map<ResourceLocation, Info> FACTIONS = new LinkedHashMap<>();
    private static boolean menuOpen = false;
    private static ResourceLocation forcedFaction = null;

    private FactionClientEvents() { }

    public static void setFactions(List<Info> infos) {
        FACTIONS.clear();
        for (Info info : infos)
            FACTIONS.put(info.id(), info);
    }

    public static Collection<Info> all() {
        return FACTIONS.values();
    }

    public static Info get(ResourceLocation id) {
        return FACTIONS.get(id);
    }

    public static void open(ResourceLocation forced) {
        forcedFaction = forced;
        menuOpen = true;
    }

    public static void close() {
        menuOpen = false;
        forcedFaction = null;
    }

    public static boolean isMenuOpen() {
        return menuOpen;
    }

    public static ResourceLocation getForcedFaction() {
        return forcedFaction;
    }

    /** Whether {@code id} may be picked right now (false for locked factions when one is forced). */
    public static boolean isSelectable(ResourceLocation id) {
        return forcedFaction == null || forcedFaction.equals(id);
    }

    public static void clear() {
        FACTIONS.clear();
        close();
    }
}
