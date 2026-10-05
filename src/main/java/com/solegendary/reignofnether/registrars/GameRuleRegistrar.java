package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.pathfinding.PathfinderConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameRules;

public class GameRuleRegistrar {

    public static GameRules.Key<GameRules.BooleanValue> LOG_FALLING;
    public static GameRules.Key<GameRules.BooleanValue> NEUTRAL_AGGRO;
    public static GameRules.Key<GameRules.IntegerValue> MAX_POPULATION;
    public static GameRules.Key<GameRules.BooleanValue> DO_PLAYER_GRIEFING;
    public static GameRules.Key<GameRules.IntegerValue> GROUND_Y_LEVEL;
    public static GameRules.Key<GameRules.IntegerValue> FLYING_MAX_Y_LEVEL;
    public static GameRules.Key<GameRules.BooleanValue> ALLOW_BEACONS;
    public static GameRules.Key<GameRules.IntegerValue> BEACON_WIN_MINUTES;
    public static GameRules.Key<GameRules.BooleanValue> PVP_MODES_ONLY;
    public static GameRules.Key<GameRules.BooleanValue> SLANTED_BUILDING;
    public static GameRules.Key<GameRules.IntegerValue> ALLOWED_HEROES;
    public static GameRules.Key<GameRules.BooleanValue> LOCK_ALLIANCES;
    public static GameRules.Key<GameRules.BooleanValue> SCENARIO_MODE;
    public static GameRules.Key<GameRules.BooleanValue> COOP_MODE;
    public static GameRules.Key<GameRules.BooleanValue> DO_NETHER_CONVERSION;
    public static GameRules.Key<GameRules.BooleanValue> BUILDINGS_OUTSIDE_BORDER;
    public static GameRules.Key<GameRules.BooleanValue> RTS_MAP;
    public static GameRules.Key<GameRules.BooleanValue> RTS_PATHFINDING;
    public static GameRules.Key<GameRules.IntegerValue> PATHFINDING_THREADS;
    public static GameRules.Key<GameRules.IntegerValue> PATHFINDING_CHUNK_BUILDS;
    public static GameRules.Key<GameRules.IntegerValue> ANIMAL_SPAWN_Y_DIFF;
    public static GameRules.Key<GameRules.IntegerValue> RANDOM_ITEM_DROPS;

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
        // the mod overrode the vanilla default of false, so players could break blocks where vanilla forbids it; off by default so a vanilla world is unaffected
        DO_PLAYER_GRIEFING = GameRules.register("doPlayerGriefing", GameRules.Category.PLAYER,
                GameRules.BooleanValue.create(false)
        );
        // sets the minimum Y level for the camera so it doesn't fall into the void
        GROUND_Y_LEVEL = GameRules.register("groundYLevel", GameRules.Category.PLAYER,
                GameRules.IntegerValue.create(-320)
        );
        // locks the camera to a specific Y level instead of it being calculated dynamically
        FLYING_MAX_Y_LEVEL = GameRules.register("flyingMaxYLevel", GameRules.Category.MOBS,
                GameRules.IntegerValue.create(320)
        );
        // allow beacons to be built by workers as a win condition
        ALLOW_BEACONS = GameRules.register("allowBeacons", GameRules.Category.PLAYER,
                GameRules.BooleanValue.create(true)
        );
        // allow only classic/king of the beacon gamemodes
        PVP_MODES_ONLY = GameRules.register("pvpModesOnly", GameRules.Category.PLAYER,
                GameRules.BooleanValue.create(false)
        );
        // ticks to win with a beacon
        BEACON_WIN_MINUTES = GameRules.register("beaconWinMinutes", GameRules.Category.PLAYER,
                GameRules.IntegerValue.create(10)
        );
        // buildings ignore ground flatness
        SLANTED_BUILDING = GameRules.register("slantedBuilding", GameRules.Category.PLAYER,
                GameRules.BooleanValue.create(false)
        );
        // enable heroes in all gamemodes
        ALLOWED_HEROES = GameRules.register("allowedHeroes", GameRules.Category.PLAYER,
                GameRules.IntegerValue.create(2)
        );
        // only allow alliances to be made/broken with non-RTS players (ie. before a game starts)
        LOCK_ALLIANCES = GameRules.register("lockAlliances", GameRules.Category.PLAYER,
                GameRules.BooleanValue.create(false)
        );
        // map is set to be played as a scenario by the player that opens it
        SCENARIO_MODE = GameRules.register("scenarioMode", GameRules.Category.MISC,
                GameRules.BooleanValue.create(false)
        );
        // all players are allied and cannot change alliances, normal victory is disabled and can only be achieved via commands
        COOP_MODE = GameRules.register("coopMode", GameRules.Category.PLAYER,
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
        // area: the navmesh prewarm, the fog-of-war chunk snapshot, and fog of war itself.
        // OFF by default and never inferred from the world border: a reduced border is a normal thing for
        // a modpack survival map to have, and generating a whole border on server start is not something a
        // world should get without being asked. The border size stays as a second condition - a small border
        // is still required for the bounded-area assumption to hold - but it no longer opts you in by itself.
        // Set with /gamerule reignofnetherRtsMap true on maps you want treated as RTS maps.
        RTS_MAP = GameRules.register("reignofnetherRtsMap", GameRules.Category.MISC,
                GameRules.BooleanValue.create(false)
        );
        // use the RTS-optimised pathfinder (async grid A*, walkability cache) instead of vanilla.
        // Enabled at world load for worlds with reignofnetherRtsMap on, which also prewarms the navmesh.
        // Off otherwise.
        // May cause additional TPS lag on worlds without world borders as they will not have a pregenerated navmesh
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
        // Difference in level that animals can spawn around capitols at
        RANDOM_ITEM_DROPS = GameRules.register("randomItemDrops", GameRules.Category.DROPS,
                boundedInt(1, 0, 2)
        );
    }

    private static GameRules.Type<GameRules.IntegerValue> boundedInt(int def, int min, int max) {
        return GameRules.IntegerValue.create(def, (server, value) -> {
            int clamped = Mth.clamp(value.get(), 0, 2);
            if (clamped != value.get())
                value.set(clamped, server); // only re-set if different, avoids infinite recursion
        });
    }
}
