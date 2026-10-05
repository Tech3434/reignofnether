package com.solegendary.reignofnether.gamemode;

/**
 * The match modes the mod knows about.
 *
 * <p>Survival (wave defence) and Scenario (mapmaker-authored matches) went with their content. What is
 * left is the standard RTS match. Sandbox is not a mode any more: it was a mode only so that the
 * mapmaker tools could be gated, and it is now gated on operator permission directly - see
 * {@code SandboxClientEvents#isSandboxMode}.
 */
public enum GameMode {
    CLASSIC, // Standard RTS match
    NONE     // used for packets
}
