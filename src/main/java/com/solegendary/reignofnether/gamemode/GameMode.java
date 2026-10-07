package com.solegendary.reignofnether.gamemode;

/**
 * The match modes the mod knows about.
 *
 * <p>Survival (wave defence) and Scenario (mapmaker-authored matches) went with their content, and the
 * sandbox tool was removed with them. What is left is the standard RTS match.
 */
public enum GameMode {
    CLASSIC, // Standard RTS match
    NONE     // used for packets
}
