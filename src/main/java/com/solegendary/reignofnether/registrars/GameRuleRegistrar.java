package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.pathfinding.PathfinderConfig;
import net.minecraft.world.level.GameRules;

public class GameRuleRegistrar {

    public static GameRules.Key<GameRules.BooleanValue> LOG_FALLING;
    public static GameRules.Key<GameRules.BooleanValue> NEUTRAL_AGGRO;
    public static GameRules.Key<GameRules.IntegerValue> MAX_POPULATION;
    public static GameRules.Key<GameRules.BooleanValue> SLANTED_BUILDING;
    public static GameRules.Key<GameRules.BooleanValue> LOCK_ALLIANCES;
    public static GameRules.Key<GameRules.BooleanValue> DO_NETHER_CONVERSION;
    public static GameRules.Key<GameRules.BooleanValue> BUILDINGS_OUTSIDE_BORDER;
    public static GameRules.Key<GameRules.BooleanValue> RTS_MAP;
    public static GameRules.Key<GameRules.BooleanValue> RTS_PATHFINDING;
    public static GameRules.Key<GameRules.IntegerValue> PATHFINDING_THREADS;
    public static GameRules.Key<GameRules.IntegerValue> PATHFINDING_CHUNK_BUILDS;
    public static GameRules.Key<GameRules.IntegerValue> ANIMAL_SPAWN_Y_DIFF;

    public static void init() {
        // felling a tree turned neighbouring logs into mod falling-log blocks with their own loot tables; off by default so a vanilla world is unaffected
        LOG_FALLING = GameRules.register("doLogFalling", GameRules.Category.MISC,
                GameRules.BooleanValue.create(false)
        );
        // every 20 ticks any PathfinderMob within 20 blocks of a unit was force-targeted at it; off by default so a vanilla world is unaffected
        NEUTRAL_AGGRO = GameRules.register("neutralAggro", GameRules.Category.MOBS,
                GameRules.BooleanValue.create(false)
        );
        // set hard cap on population (max even with infinite houses)
        MAX_POPULATION = GameRules.register("maxPopulation", GameRules.Category.MISC,
                GameRules.IntegerValue.create(ResourceCosts.DEFAULT_MAX_POPULATION)
        );
        // buildings ignore ground flatness
        SLANTED_BUILDING = GameRules.register("slantedBuilding", GameRules.Category.PLAYER,
                GameRules.BooleanValue.create(false)
        );
        // only allow alliances to be made/broken with non-RTS players (ie. before a game starts)
        LOCK_ALLIANCES = GameRules.register("lockAlliances", GameRules.Category.PLAYER,
                GameRules.BooleanValue.create(false)
        );
        // every portal building rewrote the surrounding terrain into nether blocks and back on destruction; off by default so a vanilla world is unaffected
        DO_NETHER_CONVERSION = GameRules.register("doNetherConversion", GameRules.Category.UPDATES,
                GameRules.BooleanValue.create(false)
        );
        // buildings could sit outside the world border, and with it false out-of-border buildings get auto-destroyed; off by default so a vanilla world is unaffected
        BUILDINGS_OUTSIDE_BORDER = GameRules.register("buildingsOutsideBorder", GameRules.Category.MISC,
                GameRules.BooleanValue.create(false)
        );
        // treat this world as purpose-built for RTS. Gates every mode that assumes a small bounded play
        // area: the navmesh prewarm and the RTS pathfinder. OFF by default and never inferred from the
        // world border. Set with /gamerule reignofnetherRtsMap true on maps you want treated as RTS maps.
        RTS_MAP = GameRules.register("reignofnetherRtsMap", GameRules.Category.MISC,
                GameRules.BooleanValue.create(false)
        );
        // use the RTS-optimised pathfinder (async grid A*, walkability cache) instead of vanilla.
        // Enabled at world load for worlds with reignofnetherRtsMap on, which also prewarms the navmesh.
        // Off otherwise.
        RTS_PATHFINDING = GameRules.register("rtsPathfinding", GameRules.Category.MOBS,
                GameRules.BooleanValue.create(false)
        );
        // number of background threads the RTS grid A* pathfinder uses. defaults to ~half the cores; the
        // worker pool clamps to [1, 32] and rebuilds itself live when this changes.
        PATHFINDING_THREADS = GameRules.register("pathfindingThreads", GameRules.Category.MOBS,
                GameRules.IntegerValue.create(PathfinderConfig.WORKER_THREADS)
        );
        // cold chunks the pathfinder warms per tick (main-thread work). higher = faster first paths but more
        // TPS cost. clamped to [1, 64] by the worker pool.
        PATHFINDING_CHUNK_BUILDS = GameRules.register("pathfindingChunkBuildsPerTick", GameRules.Category.MOBS,
                GameRules.IntegerValue.create(PathfinderConfig.CHUNK_BUILDS_PER_TICK_DEFAULT)
        );
        // Difference in level that animals can spawn around capitols at
        ANIMAL_SPAWN_Y_DIFF = GameRules.register("animalSpawnYDiff", GameRules.Category.MOBS,
                GameRules.IntegerValue.create(5)
        );
    }
}
