package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;

import static com.solegendary.reignofnether.player.PlayerServerEvents.TICKS_TO_REVEAL;

/**
 * A player in an RTS match: real or bot, human-controlled or started by the match code.
 *
 * <p>It used to also carry the player's faction, their scenario role, their beacon progress, their
 * market rates and their unit-item drop queue. All of that belonged to content that is gone: the
 * faction has no carriers left (see stage F), scenario roles went with the scenario mode, and the
 * beacon, markets and unit items were removed content. What remains is identity, colour and scores.
 */
public class RTSPlayer {
    public String name;
    public int id; // for AI, always negative
    public int ticksWithoutCapitol = 0;
    public int startPosColorId = 0;
    public RTSPlayerScores scores = new RTSPlayerScores();
    /**
     * H.4: whether this player ever owned a building. A match only ends for a player who had
     * something to lose, so a player who never built is not defeated the moment their starting
     * units die.
     */
    public boolean hasEverOwnedBuilding = false;

    public static RTSPlayer getNewPlayer(String playerName, int id, int startPosColorId) {
        return new RTSPlayer(playerName, id, startPosColorId);
    }

    private RTSPlayer(String playerName, int id, int startPosColorId) {
        this.name = playerName;
        this.id = id;
        this.startPosColorId = startPosColorId;
    }

    public static RTSPlayer getNewBot(String name) {
        return new RTSPlayer(name);
    }

    private RTSPlayer(String name) {
        int minId = Integer.MAX_VALUE;
        if (!PlayerServerEvents.rtsPlayers.isEmpty()) {
            for (RTSPlayer r : PlayerServerEvents.rtsPlayers) {
                minId = Math.min(r.id, minId);
            }
        }
        this.id = minId >= 0 ? -1 : minId - 1;
        this.name = name;
    }

    public static RTSPlayer getFromSave(String name, int id, int ticksWithoutCapitol, int startPosColorId, int[] scores, boolean hasEverOwnedBuilding) {
        RTSPlayer rtsPlayer = new RTSPlayer(name, id, startPosColorId);
        rtsPlayer.ticksWithoutCapitol = ticksWithoutCapitol;
        rtsPlayer.scores.setScoreListFromArray(scores);
        rtsPlayer.hasEverOwnedBuilding = hasEverOwnedBuilding;
        return rtsPlayer;
    }

    public boolean isBot() {
        return id < 0;
    }

    public void serverTick() {
        int numBuildingsOwned = 0;
        int numCapitolsOwned = 0;
        for (BuildingPlacement buildingPlacement : BuildingServerEvents.getBuildings()) {
            if (!buildingPlacement.ownerName.equals(this.name))
                continue;
            numBuildingsOwned++;
            if (buildingPlacement.isCapitol)
                numCapitolsOwned++;
        }

        // H.4: remember that this player had a building, so the match only ends on a real loss
        if (numBuildingsOwned > 0)
            this.hasEverOwnedBuilding = true;

        // Losing the last capitol is what hides a player from the minimap; see isRevealed().
        if (numBuildingsOwned > 0 && numCapitolsOwned == 0) {
            if (ticksWithoutCapitol < TICKS_TO_REVEAL)
                this.ticksWithoutCapitol += 1;
        } else {
            this.ticksWithoutCapitol = 0;
        }
    }

    public boolean isRevealed() {
        return this.ticksWithoutCapitol >= TICKS_TO_REVEAL;
    }
}
