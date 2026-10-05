package com.solegendary.reignofnether.player;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Operator cheats: named toggles a player turns on for themselves and every unit or building they own
 * can read.
 *
 * <p>They used to live in {@code ResearchServerEvents}/{@code ResearchClient} next to the research
 * tree, because cheats used to be handed out by research. Research is gone, so the cheat list is now
 * the only thing that class kept - it is an operator tool, not content.
 *
 * <p>Serverside is authoritative and lives in {@link #cheatItems}; the client keeps a mirror in
 * {@link #clientCheats} that {@link CheatsClientboundPacket} fills on login.
 */
public class Cheats {

    /** Serverside: every (player, cheat) pair currently granted. */
    private static final List<Pair<String, String>> cheatItems = new ArrayList<>();

    /** Clientside mirror, only ever written by {@link CheatsClientboundPacket}. */
    private static final List<String> clientCheats = new ArrayList<>();

    // ---------------------------------------------------------------- serverside

    public static boolean playerHasCheat(String playerName, String cheatName) {
        for (Pair<String, String> cheatItem : cheatItems)
            if (cheatItem.getFirst().equals(playerName) && cheatItem.getSecond().equals(cheatName))
                return true;
        return false;
    }

    public static void addCheat(String playerName, String cheatName) {
        if (!playerHasCheat(playerName, cheatName))
            cheatItems.add(new Pair<>(playerName, cheatName));
    }

    public static void removeCheat(String playerName, String cheatName) {
        cheatItems.removeIf(p -> p.getFirst().equals(playerName) && p.getSecond().equals(cheatName));
    }

    public static void removeAllCheatsFor(String playerName) {
        cheatItems.removeIf(r -> r.getFirst().equals(playerName));
    }

    public static void removeAllCheats() {
        cheatItems.clear();
        clientCheats.clear();
    }

    /** Push this player's cheats to their client; called when they log in. */
    public static void syncCheats(Supplier<ServerPlayer> player) {
        clientCheats.clear();
        for (Pair<String, String> cheatItem : cheatItems)
            if (player.get().getGameProfile().getName().equals(cheatItem.getFirst()))
                CheatsClientboundPacket.addCheat(player, cheatItem.getSecond());
    }

    // ---------------------------------------------------------------- clientside

    public static boolean hasCheat(String cheatName) {
        return clientCheats.contains(cheatName);
    }

    static void addClientCheat(String cheatName) {
        if (!clientCheats.contains(cheatName)) {
            clientCheats.add(cheatName);
            ReignOfNether.LOGGER.info("Cheat granted: " + cheatName);
        }
    }
}
