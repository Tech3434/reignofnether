package com.solegendary.reignofnether.worldborder;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.gamerules.GameruleClientboundPacket;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.pathfinding.PathfinderConfig;
import com.solegendary.reignofnether.unit.pathfinding.WalkabilityGrid;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class WorldBorderServerEvents {

    // A world border this small (blocks) is still required for the "bounded play area" assumption that
    // fog of war and the navmesh precompute rely on. 1280 = 80x80 chunks.
    public static final int RTS_OPTIMIZED_BORDER = 1280;

    // Upper bound on the navmesh prewarm, in chunks, so a mis-set or oversized border cannot turn server
    // start into a multi-minute terrain generation. 4096 = an 64x64 chunk square = 1024x1024 blocks.
    // Overflow is skipped and logged rather than silently truncated.
    public static final int PREWARM_MAX_CHUNKS = 4096;

    public static boolean prewarmedNavmesh = false;

    // Whether this world is treated as purpose-built for RTS, which gates everything that assumes a small
    // bounded play area: the navmesh prewarm, the fog-of-war chunk snapshot and fog of war itself.
    //
    // The reignofnetherRtsMap gamerule is the opt-in and is checked FIRST, so an admin has to ask for this
    // mode explicitly. The border size is only a second condition, never an opt-in by itself: a reduced
    // border is ordinary for a modpack survival map, and inferring RTS mode from it made the mod generate
    // a whole border's terrain on server start in someone else's world.
    public static boolean isRtsOptimisedMap(ServerLevel level) {
        if (level == null) return false;
        if (level.getServer() == null) return false;
        if (!level.getServer().getGameRules().getRule(GameRuleRegistrar.RTS_MAP).get()) return false;
        return level.getWorldBorder().getSize() <= RTS_OPTIMIZED_BORDER;
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent evt) {
        MinecraftServer server = evt.getServer();
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null)
            return;

        // opt-in only: reignofnetherRtsMap, plus a border small enough to bound the play area
        if (!server.getGameRules().getRule(GameRuleRegistrar.RTS_MAP).get()) {
            ReignOfNether.LOGGER.info(
                    "Not treating this world as an RTS map (reignofnetherRtsMap is off) - no navmesh prewarm, no fog-of-war snapshot");
            return;
        }
        if (!isRtsOptimisedMap(level)) {
            ReignOfNether.LOGGER.info(
                    "reignofnetherRtsMap is on but the world border is {} blocks (> {}), so the bounded-play-area assumption does not hold - leaving the world alone",
                    (int) level.getWorldBorder().getSize(), RTS_OPTIMIZED_BORDER);
            return;
        }

        WorldBorder border = level.getWorldBorder();
        ReignOfNether.LOGGER.info(
                "RTS map mode enabled by gamerule (world border = {} blocks) - enabling improved pathfinding + navmesh precompute",
                (int) border.getSize());

        // Turn the rtsPathfinding gamerule on for this map (the gamerule's own default stays off, so
        // worlds that did not opt in are unaffected) and mirror it to the server flag + any clients,
        // reusing the existing gamerule plumbing. At server-start there are no clients yet; player-join
        // sync handles late joiners.
        server.getGameRules().getRule(GameRuleRegistrar.RTS_PATHFINDING).set(true, server);
        UnitServerEvents.rtsPathfinding = true;
        GameruleClientboundPacket.setRtsPathfinding(true);

        // Elevate the per-tick warm budget across the prewarm load window, restoring the configured value
        // after. (The synchronous prewarm loop below bypasses this budget; this only affects any runtime
        // warming that occurs during loading.)
        int configuredBudget = PathfinderConfig.maxChunkBuildsPerTick;
        PathfinderConfig.maxChunkBuildsPerTick = PathfinderConfig.CHUNK_BUILDS_PER_TICK_PREWARM;
        try {
            prewarmNavmesh(level, border);
        } finally {
            PathfinderConfig.maxChunkBuildsPerTick = configuredBudget;
        }
    }

    // Force-load and classify every chunk inside the world border so the navmesh is fully warm before play.
    // Synchronous by design: this is the load-time "prepare the world" phase, paid up front and bounded by
    // the (small) RTS-optimised border. The cache is sized to hold all of it, so the prewarm never evicts itself.
    //
    // This DOES generate terrain for every chunk it touches, which is why it only runs on an explicitly opted-in
    // world and why the chunk count is capped at PREWARM_MAX_CHUNKS.
    private static void prewarmNavmesh(ServerLevel level, WorldBorder border) {
        WalkabilityGrid grid = WalkabilityGrid.get(level);

        int cxMin = (int) Math.floor((border.getCenterX() - border.getSize() / 2.0) / 16.0);
        int cxMax = (int) Math.floor((border.getCenterX() + border.getSize() / 2.0) / 16.0);
        int czMin = (int) Math.floor((border.getCenterZ() - border.getSize() / 2.0) / 16.0);
        int czMax = (int) Math.floor((border.getCenterZ() + border.getSize() / 2.0) / 16.0);

        int total = (cxMax - cxMin + 1) * (czMax - czMin + 1);
        if (total > PREWARM_MAX_CHUNKS) {
            ReignOfNether.LOGGER.warn(
                    "Navmesh prewarm skipped: {} chunks inside the world border exceeds the cap of {}. " +
                            "Shrink the world border or raise {} - refusing to generate that much terrain on server start",
                    total, PREWARM_MAX_CHUNKS, "WorldBorderServerEvents.PREWARM_MAX_CHUNKS");
            return;
        }

        ReignOfNether.LOGGER.info("Prewarming RTS navmesh: {} chunks within world border ({}..{}, {}..{})...",
                total, cxMin, cxMax, czMin, czMax);

        long startMs = System.currentTimeMillis();
        int done = 0;
        int nextProgress = 10; // log at ~every 10%

        for (int cx = cxMin; cx <= cxMax; cx++) {
            for (int cz = czMin; cz <= czMax; cz++) {
                try {
                    // Force-load/generate the chunk so its block states are readable, then classify a
                    // surface-aligned Y band around its terrain height.
                    ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.FULL, true);
                    int[] band = surfaceBand(chunk);
                    grid.getOrBuild(level, cx, cz, band[0], band[1]);
                } catch (Throwable t) {
                    ReignOfNether.LOGGER.error("Navmesh prewarm failed for chunk ({}, {})", cx, cz, t);
                }
                done++;
                int pct = (int) (100L * done / total);
                if (pct >= nextProgress) {
                    ReignOfNether.LOGGER.info("Prewarming RTS navmesh: {}% ({}/{} chunks)", pct, done, total);
                    nextProgress = pct - (pct % 10) + 10;
                }
            }
        }

        ReignOfNether.LOGGER.info("Prewarmed RTS navmesh: {} chunks in {} ms",
                total, System.currentTimeMillis() - startMs);

        prewarmedNavmesh = true;
    }

    // Y band to classify for a chunk: span the chunk's surface heights (sampled at its corners + centre) and
    // pad to the pathfinder's vertical window so a unit pathing across this chunk has headroom above/below.
    // The pad below must match what runtime requests want (ChunkSnapshot.regionFor pads refY by
    // VERTICAL_RADIUS + SLACK both ways, and refY ~= the surface feet Y this heightmap returns) - a shallower
    // prewarm band fails covers() on first touch and every prewarmed chunk gets union-rebuilt, wasting the prewarm.
    private static int[] surfaceBand(ChunkAccess chunk) {
        int[] xs = {0, 15, 0, 15, 8};
        int[] zs = {0, 0, 15, 15, 8};
        int minSurf = Integer.MAX_VALUE;
        int maxSurf = Integer.MIN_VALUE;
        for (int i = 0; i < xs.length; i++) {
            int h = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, xs[i], zs[i]);
            if (h < minSurf) minSurf = h;
            if (h > maxSurf) maxSurf = h;
        }
        int band = PathfinderConfig.VERTICAL_RADIUS + PathfinderConfig.VERTICAL_WINDOW_SLACK;
        int minY = Math.max(chunk.getMinBuildHeight(), minSurf - band);
        int maxY = Math.min(chunk.getMaxBuildHeight(), maxSurf + band);
        return new int[]{minY, maxY};
    }
}
