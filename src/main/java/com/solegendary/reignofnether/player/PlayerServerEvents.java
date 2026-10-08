package com.solegendary.reignofnether.player;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.alliance.AllyCommand;
import com.solegendary.reignofnether.building.*;

import com.solegendary.reignofnether.building.buildings.placements.CustomBuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;

import com.solegendary.reignofnether.gamerules.GameruleClientboundPacket;
import com.solegendary.reignofnether.research.ResearchClientboundPacket;
import com.solegendary.reignofnether.research.ResearchSaveData;
import com.solegendary.reignofnether.guiscreen.TopdownGuiContainer;


import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;

import com.solegendary.reignofnether.time.TimeServerEvents;
import com.solegendary.reignofnether.time.TimeUtils;

import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.packets.UnitSyncClientboundPacket;

import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.worldborder.WorldBorderServerEvents;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.solegendary.reignofnether.building.BuildingServerEvents.random;
import static com.solegendary.reignofnether.building.BuildingServerEvents.saveBuildings;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.entity.animal.Rabbit;
import com.solegendary.reignofnether.player.RTSPlayerScoresCommand;
import com.solegendary.reignofnether.player.RTSPlayerSaveData;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.PlayerClientboundPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import com.solegendary.reignofnether.player.MatchStatsClientboundPacket;

import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingClientboundPacket;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.Building;

// this class tracks all available players so that any serverside functions that need to affect the player can be
// performed here by sending a client->server packet containing MC.player.getId()

public class PlayerServerEvents {

    // list of what gamemode these players should be in when outside of RTS cam
    private static final Map<String, GameType> playerDefaultGameModes = new HashMap<>();
    private static final Map<String, Boolean> playerGuiOpenStatus = new HashMap<>();

    public static final ArrayList<ServerPlayer> players = new ArrayList<>();
    public static final ArrayList<ServerPlayer> orthoviewPlayers = new ArrayList<>();
    // players that are currently playing a match
    public static final List<RTSPlayer> rtsPlayers = Collections.synchronizedList(new ArrayList<>());
    // list of players after a match ends
    public static final List<RTSPlayer> postGameRtsPlayers = Collections.synchronizedList(new ArrayList<>());
    public static boolean rtsLocked = false; // can players join as RTS players or not?
    public static boolean rtsSyncingEnabled = true; // will logging in players sync units and buildings?

    // H.1: the RTS pass is ordinary operator permission level 2. Nothing new carries it - no entity,
    // no capability, no saved field - so any op already has it and no new concept enters the game.
    public static final int RTS_PASS_OP_LEVEL = 2;

    /** Whether this player is allowed to enter RTS mode. */
    public static boolean hasRTSPass(ServerPlayer player) {
        return player != null && player.hasPermissions(RTS_PASS_OP_LEVEL);
    }

    private static final int MONSTER_START_TIME_OF_DAY = 500; // 500 = dawn, 6500 = noon, 12500 = dusk

    public static final int TICKS_TO_REVEAL = 60 * ResourceCost.TICKS_PER_SECOND;

    public static long rtsGameTicks = 0; // ticks up as long as there is at least 1 rtsPlayer

    public static ServerLevel serverLevel = null;

    public static void saveRTSPlayers() {
        if (serverLevel == null) {
            return;
        }
        // don't create an empty save file in a world that has no RTS players and none stored
        if (rtsPlayers.isEmpty() && !RTSPlayerSaveData.isStored(serverLevel)) {
            serverLevel.getDataStorage().save();
            return;
        }
        RTSPlayerSaveData data = RTSPlayerSaveData.getInstance(serverLevel);
        data.rtsPlayers.clear();
        data.rtsPlayers.addAll(rtsPlayers);
        data.save();
        serverLevel.getDataStorage().save();
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);

        if (level != null) {
            RTSPlayerSaveData data = RTSPlayerSaveData.getLoaded(level);

            rtsPlayers.clear();
            rtsPlayers.addAll(data.rtsPlayers);
            

            UnitServerEvents.maxPopulation = level.getGameRules().getInt(GameRuleRegistrar.MAX_POPULATION);
        }
    }

    private static final int SAVE_TICKS_MAX = 1200;
    private static int saveTicks = 0;
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent evt) {
        saveRTSPlayers();
    }

    public static boolean isRTSPlayer(String playerName) {
        synchronized (rtsPlayers) {
            for (RTSPlayer p : rtsPlayers) {
                if (p.name.equals(playerName)) return true;
            }
            return false;
        }
    }

    @Nullable
    public static RTSPlayer getRTSPlayer(String playerName) {
        synchronized (rtsPlayers) {
            for (RTSPlayer rtsPlayer : rtsPlayers)
                if (rtsPlayer.name.equals(playerName))
                    return rtsPlayer;
        }
        return null;
    }

    public static boolean isRTSPlayer(int id) {
        synchronized (rtsPlayers) {
            for (RTSPlayer p : rtsPlayers) {
                if (p.id == id) return true;
            }
            return false;
        }
    }

    public static boolean isBot(String playerName) {
        synchronized (rtsPlayers) {
            for (RTSPlayer rtsPlayer : rtsPlayers)
                if (rtsPlayer.name.equalsIgnoreCase(playerName)) {
                    return rtsPlayer.isBot();
                }
        }
        return false;
    }

    public static boolean isBot(int id) {
        synchronized (rtsPlayers) {
            for (RTSPlayer rtsPlayer : rtsPlayers)
                if (rtsPlayer.id == id) {
                    return rtsPlayer.isBot();
                }
        }
        return false;
    }

    public static boolean isGameActive() {
        return !rtsPlayers.isEmpty();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post evt) {
        serverLevel = evt.getServer().getLevel(Level.OVERWORLD);

        if (false) {
            
        }

        synchronized (rtsPlayers) {
            {
                for (RTSPlayer rtsPlayer : rtsPlayers)
                    rtsPlayer.serverTick();

                if (rtsPlayers.isEmpty()) {
                    rtsGameTicks = 0;
                } else {
                    rtsGameTicks += 1;
                    if (rtsGameTicks % 200 == 0) {
                        PlayerClientboundPacket.syncRtsGameTime(rtsGameTicks);
                    }
                }
            }
        }
        {
            saveTicks += 1;
            if (saveTicks >= SAVE_TICKS_MAX) {
                ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);
                if (level != null) {
                    saveRTSPlayers();
                    saveTicks = 0;
                }
            }
        }
    }

    private static void syncUnits() {
        for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
            if (entity instanceof Unit unit && unit.isRtsUnit()) {
                UnitSyncClientboundPacket.sendSyncResourcesPacket(unit);
                UnitSyncClientboundPacket.sendSyncOwnerNamePacket(unit);
                if (unit.getAnchor() != null)
                    UnitSyncClientboundPacket.sendSyncAnchorPosPacket(entity, unit.getAnchor());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        ServerPlayer serverPlayer = (ServerPlayer) evt.getEntity();

        players.add((ServerPlayer) evt.getEntity());
        String playerName = serverPlayer.getName().getString();
        ReignOfNether.LOGGER.info("Player logged in: " + playerName + ", id: " + serverPlayer.getId());

        // if a player is looking directly at a frozenchunk on login, they may load in the real blocks before
        // they are frozen so move them away then BuildingClientEvents.placeBuilding moves them to their base later
        // don't do this if they don't own any buildings
        /*
        if (isRTSPlayer(playerName) && rtsSyncingEnabled) {
            for (BuildingPlacement building : BuildingServerEvents.getBuildings()) {
                if (building.ownerName.equals(playerName)) {
                    movePlayer(serverPlayer.getId(), 0, ORTHOVIEW_PLAYER_BASE_Y, 0);
                    break;
                }
            }
        }
         */
        if (rtsSyncingEnabled) {
            MinecraftServer server = evt.getEntity().level().getServer();
            if (server == null || !server.isDedicatedServer()) {
                CompletableFuture.delayedExecutor(1000, TimeUnit.MILLISECONDS).execute(PlayerServerEvents::syncUnits);
            } else {
                syncUnits();
            }
        }

        boolean inOrthoviewList = false;
        for (ServerPlayer orthoviewPlayer : orthoviewPlayers) {
            if (orthoviewPlayer.getId() == evt.getEntity().getId())  {
                inOrthoviewList = true;
                break;
            }
        }
        if (!inOrthoviewList)
            orthoviewPlayers.add((ServerPlayer) evt.getEntity());

        {
            if (!isRTSPlayer(serverPlayer.getId())) {
                serverPlayer.sendSystemMessage(Component.translatable("tutorial.reignofnether.welcome")
                    .withStyle(Style.EMPTY.withBold(true)));
                serverPlayer.sendSystemMessage(Component.translatable("tutorial.reignofnether.join"));
                serverPlayer.sendSystemMessage(Component.translatable("tutorial.reignofnether.help"));
                serverPlayer.sendSystemMessage(Component.translatable("tutorial.reignofnether.controls"));
                if (rtsLocked) {
                    serverPlayer.sendSystemMessage(Component.literal(""));
                    serverPlayer.sendSystemMessage(Component.translatable("tutorial.reignofnether.locked"));
                }
            } else {
                serverPlayer.sendSystemMessage(Component.translatable("tutorial.reignofnether.welcome_back")
                    .withStyle(Style.EMPTY.withBold(true)));
            }
            if (serverPlayer.hasPermissions(4)) {
                serverPlayer.sendSystemMessage(Component.literal(""));
                serverPlayer.sendSystemMessage(Component.translatable("tutorial.reignofnether.op_commands"));
                serverPlayer.sendSystemMessage(Component.translatable("tutorial.reignofnether.fog"));
                serverPlayer.sendSystemMessage(Component.translatable("tutorial.reignofnether.lock"));
                serverPlayer.sendSystemMessage(Component.translatable("tutorial.reignofnether.reset"));
                serverPlayer.sendSystemMessage(Component.literal(""));
            }
            if (!rtsSyncingEnabled) {
                serverPlayer.sendSystemMessage(Component.literal(""));
                serverPlayer.sendSystemMessage(Component.translatable("tutorial.reignofnether.sync_disabled1"));
                serverPlayer.sendSystemMessage(Component.translatable("tutorial.reignofnether.sync_disabled2"));
                serverPlayer.sendSystemMessage(Component.literal(""));
            }
        }
        if (getRTSPlayer(playerName) == null) {
            PlayerClientboundPacket.removeRTSPlayer(playerName);
        }
        for (RTSPlayer rtsPlayer : rtsPlayers) {
            PlayerClientboundPacket.addRTSPlayer(rtsPlayer.name, (long) rtsPlayer.id, rtsPlayer.startPosColorId);
        }

        if (rtsLocked) {
            PlayerClientboundPacket.lockRTS(playerName);
        } else {
            PlayerClientboundPacket.unlockRTS(playerName);
        }

        if (rtsSyncingEnabled) {
            PlayerClientboundPacket.enableStartRTS(playerName);
        } else {
            PlayerClientboundPacket.disableStartRTS(playerName);
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent evt) {
        int id = evt.getEntity().getId();
        ReignOfNether.LOGGER.info("Player logged out: " + evt.getEntity().getName().getString() + ", id: " + id);
        players.removeIf(player -> player.getId() == id);
    }

    /**
     * Puts a player into the match.
     *
     * <p>This used to switch on the player's faction to pick which worker and scout types to spawn and
     * which capitol message to print. Factions are gone, so the starting unit and the capitol come from
     * the constants below - a new faction overrides them where it defines its own units.
     */
    public static final ResourceLocation STARTING_WORKER_DEF = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "villager_unit");

    /**
     * H.8: the army a player is handed when a match starts, one entry per unit. The default is a
     * single worker, because the base army limit is one unit (decision 13) - a bigger starting army
     * would have nowhere to live until a capitol raises the cap. A faction overrides this list with
     * its own units.
     */
    public static final List<ResourceLocation> STARTING_ARMY = List.of(STARTING_WORKER_DEF);

    public static void startRTS(int playerId, Vec3 pos) {
        startRTS(playerId, pos, 0, com.solegendary.reignofnether.faction.FactionRegistries.getDefaultFactionId());
    }

    // readied start is a simultaneous start from players using RTS start pos blocks, difference being:
    // - places the capitol foundations automatically
    // - spawns workers outside the foundations
    // - no start messages are sent other than the one from the countdown
    public static void startRTS(int playerId, Vec3 pos, int startPosColorId) {
        startRTS(playerId, pos, startPosColorId, com.solegendary.reignofnether.faction.FactionRegistries.getDefaultFactionId());
    }

    /** Starts a match for the player under the given faction (its capitol and starting army). */
    public static void startRTS(int playerId, Vec3 pos, int startPosColorId, ResourceLocation factionId) {
        ReignOfNether.LOGGER.info("[Player] startRTS: playerId={}, pos=[{},{},{}], startPosColorId={}", playerId, pos.x, pos.y, pos.z, startPosColorId);
        synchronized (rtsPlayers) {
            boolean readiedStart = startPosColorId != 0;

            ServerPlayer serverPlayer = null;
            for (ServerPlayer player : players)
                if (player.getId() == playerId)
                    serverPlayer = player;

            if (serverPlayer == null) {
                return;
            }
            // H.1/H.2: entering RTS mode needs the pass. Without it nothing happens at all - no
            // message, no error - so the entry is invisible to players who may not use it.
            if (!hasRTSPass(serverPlayer)) {
                return;
            }
            if (rtsLocked) {
                serverPlayer.sendSystemMessage(Component.literal(""));
                serverPlayer.sendSystemMessage(Component.translatable("server.reignofnether.locked"));
                serverPlayer.sendSystemMessage(Component.literal(""));
                return;
            }
            if (isRTSPlayer(serverPlayer.getId())) {
                serverPlayer.sendSystemMessage(Component.literal(""));
                serverPlayer.sendSystemMessage(Component.translatable("server.reignofnether.already_started"));
                serverPlayer.sendSystemMessage(Component.literal(""));
                return;
            }
            if (serverPlayer.level().getWorldBorder().getDistanceToBorder(pos.x, pos.z) < 1) {
                serverPlayer.sendSystemMessage(Component.literal(""));
                serverPlayer.sendSystemMessage(Component.translatable("server.reignofnether.outside_border"));
                serverPlayer.sendSystemMessage(Component.literal(""));
                return;
            }

            // first RTS join into a fresh game: snapshot the playable area for late joiners
            if (rtsPlayers.isEmpty() && WorldBorderServerEvents.isRtsOptimisedMap(serverLevel)) {

            }

            rtsPlayers.add(RTSPlayer.getNewPlayer(
                    serverPlayer.getName().getString(),
                    serverPlayer.getId(),
                    startPosColorId
            ));

            String playerName = serverPlayer.getName().getString();
            ResourcesServerEvents.assignResources(playerName);
            PlayerClientboundPacket.addRTSPlayer(playerName, (long) serverPlayer.getId(), startPosColorId);

            ServerLevel level = (ServerLevel) serverPlayer.level();
            ArrayList<Entity> startingWorkers = new ArrayList<>();

            // a faction (datapack registry) names its capitol and starting army; fall back to the
            // built-in constants when the faction or one of its entries is missing
            com.solegendary.reignofnether.faction.Faction faction =
                    com.solegendary.reignofnether.faction.FactionRegistries.get(level.getServer(), factionId);
            List<ResourceLocation> startUnitDefs = new ArrayList<>();
            if (faction != null && !faction.startingUnits().isEmpty())
                for (com.solegendary.reignofnether.faction.StartingUnit su : faction.startingUnits())
                    for (int c = 0; c < Math.max(1, su.count()); c++)
                        startUnitDefs.add(su.entityType());
            if (startUnitDefs.isEmpty())
                startUnitDefs.addAll(STARTING_ARMY);
            Building capitolBuilding = null;
            if (faction != null) {
                capitolBuilding = com.solegendary.reignofnether.api.ReignOfNetherRegistries.BUILDING.get(faction.capitol());
                if (capitolBuilding == null)
                    capitolBuilding = com.solegendary.reignofnether.building.buildings.JsonBuildingManager.get(faction.capitol());
            }
            if (capitolBuilding == null)
                capitolBuilding = com.solegendary.reignofnether.building.buildings.JsonBuildingManager.get(
                        ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "town_centre"));
            if (capitolBuilding == null) {
                ReignOfNether.LOGGER.error("startRTS: no capitol building found for faction {}", factionId);
                return;
            }

            // H.8: one spawn position per starting unit, spread out along x from the start position
            for (int startUnitIdx = 0; startUnitIdx < startUnitDefs.size(); startUnitIdx++) {
                BlockPos bp0 = new BlockPos((int) pos.x + startUnitIdx, 0, (int) pos.z);
                Entity entity = com.solegendary.reignofnether.unit.UnitDefinitionRuntime.create(
                        level, startUnitDefs.get(startUnitIdx), playerName);
                if (entity != null) {
                    BlockPos bp = MiscUtil.getHighestNonAirBlock(level, bp0)
                            .above()
                            .above();
                    entity.moveTo(bp, 0, 0);
                    if (!readiedStart)
                        level.addFreshEntity(entity);
                    startingWorkers.add(entity);
                }
            }

            ResourcesServerEvents.resetResources(playerName, readiedStart);

            if (readiedStart) {
                // the capitol a readied start places automatically - named by the faction
                Building building = capitolBuilding;
                ArrayList<BuildingBlock> blocks = building.getRelativeBlockData(level);
                BlockPos bp = getBuildingOriginPos(new BlockPos((int) pos.x, (int) pos.y, (int) pos.z), blocks);
                for (int i = 0; i < startingWorkers.size(); i++) {
                    startingWorkers.get(i).moveTo(bp.offset(-1, 1, i), 0, 0);
                    level.addFreshEntity(startingWorkers.get(i));
                }
                var workerIds = new int[startingWorkers.size()];
                for (int i = 0; i < startingWorkers.size(); i++) {
                    workerIds[i] = startingWorkers.get(i).getId();
                }
                BuildingServerEvents.placeBuilding(building, bp, Rotation.NONE, playerName, workerIds, false, false, true, true);
                PlayerClientboundPacket.teleport(playerName, BlockPos.containing(pos));

                for (RTSPlayer rtsPlayer : rtsPlayers) {
                    String playerName1 = rtsPlayer.name;
                    String playerName2 = serverPlayer.getName().getString();
                    if (!playerName1.equals(playerName2) && rtsPlayer.startPosColorId == startPosColorId) {
                        AlliancesServerEvents.addAlliance(playerName1, playerName2);
                    }
                }
            }

            if (!readiedStart) {
                serverPlayer.sendSystemMessage(Component.literal(""));
                sendMessageToAllPlayers("server.reignofnether.started", true, playerName);
                sendMessageToAllPlayers("server.reignofnether.total_players", false, rtsPlayers.size());
            }
            PlayerClientboundPacket.syncRtsGameTime(rtsGameTicks);
            saveRTSPlayers();

        }
    }

    public static BlockPos getBuildingOriginPos(BlockPos bp, ArrayList<BuildingBlock> blocks) {
        Vec3i buildingDimensions = BuildingUtils.getBuildingSize(blocks);
        int xRadius = buildingDimensions.getX() / 2;
        int zRadius = buildingDimensions.getZ() / 2;
        return bp.offset(-xRadius, 0 , -zRadius);
    }

    public static void startRTSBot(String name, Vec3 pos) {
        synchronized (rtsPlayers) {
            ServerLevel level;
            if (players.isEmpty()) {
                return;
            } else {
                level = (ServerLevel) players.get(0).level();
            }

            RTSPlayer bot = RTSPlayer.getNewBot(name);
            rtsPlayers.add(bot);

            ResourcesServerEvents.assignResources(bot.name);

            for (int i = -1; i <= 1; i++) {
                BlockPos bp = MiscUtil.getHighestNonAirBlock(level, new BlockPos((int) (pos.x + i), 0, (int) pos.z))
                    .above()
                    .above();
                Entity entity = com.solegendary.reignofnether.unit.UnitDefinitionRuntime.create(
                        level, STARTING_WORKER_DEF, bot.name);
                if (entity != null) {
                    entity.moveTo(bp, 0, 0);
                    level.addFreshEntity(entity);
                }
            }
            ResourcesServerEvents.resetResources(bot.name, false);

            sendMessageToAllPlayers("server.reignofnether.bot_added", true, bot.name);
            sendMessageToAllPlayers("server.reignofnether.total_players", false, rtsPlayers.size());
            saveRTSPlayers();
        }
    }

    public static void enableOrthoview(int id) {
        ServerPlayer player = getPlayerById(id);
        // H.1: the server side of the camera entry is gated too, so a client that asks for RTS mode
        // without the pass simply stays where it is.
        if (!hasRTSPass(player)) {
            return;
        }

        orthoviewPlayers.removeIf(p -> p.getId() == id);
        orthoviewPlayers.add(player);
    }

    public static void disableOrthoview(int id) {
        ServerPlayer leaving = getPlayerById(id);
        orthoviewPlayers.removeIf(p -> p.getId() == id);
        if (leaving != null) {
            // Leaving the RTS camera must also drop the SPECTATOR mode that came with it, otherwise
            // the player is left unable to break blocks anywhere afterwards - not just at buildings.
            restoreGameModeOnLeave(leaving);
        }
    }

    private static ServerPlayer getPlayerById(int playerId) {
        for (ServerPlayer player : players) {
            if (playerId == player.getId()) {
                return player;
            }
        }
        return null;
    }

    public static void openTopdownGui(int playerId) {
        ServerPlayer serverPlayer = getPlayerById(playerId);
        // H.1: same pass on the GUI entry, so RTS mode cannot be forced on a player who may not use it
        if (!hasRTSPass(serverPlayer)) {
            return;
        }

        if (serverPlayer != null) {
            // Open GUI server-side
            MenuConstructor provider = TopdownGuiContainer.getServerContainerProvider();
            MenuProvider namedProvider = new SimpleMenuProvider(provider, TopdownGuiContainer.TITLE);
            serverPlayer.openMenu(namedProvider);

            // Save original game mode only if it's not already saved for this session
            String playerName = serverPlayer.getName().getString();
            // SPECTATOR is never remembered as the mode to return to: it means the player was already
            // spectating (or a previous RTS exit failed to restore), and saving it left players stuck
            // in spectator - unable to break blocks anywhere - after leaving RTS.
            GameType currentGameType = serverPlayer.gameMode.getGameModeForPlayer();
            if (currentGameType != GameType.SPECTATOR)
                playerDefaultGameModes.putIfAbsent(playerName, currentGameType);

            // Mark that this player has the GUI open
            playerGuiOpenStatus.put(playerName, true);

            // Set game mode to SPECTATOR for GUI interaction
            serverPlayer.setGameMode(GameType.SPECTATOR);
        } else {
            ReignOfNether.LOGGER.warn("serverPlayer is null, cannot open topdown GUI");
        }
    }

    public static void closeTopdownGui(int playerId) {
        ServerPlayer serverPlayer = getPlayerById(playerId);
        if (serverPlayer != null)
            restoreGameModeOnLeave(serverPlayer);
        else
            ReignOfNether.LOGGER.warn("serverPlayer is null, cannot close topdown GUI");
    }

    /**
     * Returns a player to the game mode they had before entering RTS. Idempotent and safe to call
     * from either the GUI-close or the orthoview-disable path, so whichever packet arrives does the
     * restore exactly once. A player who was already spectating keeps spectating.
     */
    private static void restoreGameModeOnLeave(ServerPlayer serverPlayer) {
        String playerName = serverPlayer.getName().getString();
        playerGuiOpenStatus.remove(playerName);
        GameType originalGameType = playerDefaultGameModes.remove(playerName);
        if (originalGameType != null && originalGameType != GameType.SPECTATOR)
            serverPlayer.setGameMode(originalGameType);
    }

    public static void movePlayer(int playerId, double x, double y, double z) {
        ServerPlayer serverPlayer = getPlayerById(playerId);
        if (serverPlayer != null && (serverPlayer.isCreative() || serverPlayer.isSpectator())) {
            boolean isOrtho = false;
            for (ServerPlayer op : orthoviewPlayers)
                if (op.getId() == playerId) { isOrtho = true; break; }
            if (isOrtho) {
                net.minecraft.world.level.border.WorldBorder border = serverPlayer.level().getWorldBorder();
                double margin = 1.0D;
                double minX = border.getMinX() + margin;
                double maxX = border.getMaxX() - margin;
                double minZ = border.getMinZ() + margin;
                double maxZ = border.getMaxZ() - margin;
                if (maxX > minX && maxZ > minZ) {
                    x = net.minecraft.util.Mth.clamp(x, minX, maxX);
                    z = net.minecraft.util.Mth.clamp(z, minZ, maxZ);
                }
            }
            serverPlayer.teleportTo(x, y, z);
        }
    }

    public static void sendMessageToAllPlayers(String msg) {
        sendMessageToAllPlayers(msg, false);
    }

    public static void sendMessageToAllPlayers(String msg, int color, boolean bold, Object... formatArgs) {
        for (ServerPlayer player : players) {
            player.sendSystemMessage(Component.literal(""));
            if (bold) {
                player.sendSystemMessage(Component.translatable(msg, formatArgs)
                        .withStyle(Style.EMPTY.withBold(true).withColor(color)));
            } else {
                player.sendSystemMessage(Component.translatable(msg, formatArgs)
                        .withStyle(Style.EMPTY.withBold(false).withColor(color)));
            }
            player.sendSystemMessage(Component.literal(""));
        }
    }

    public static void sendMessageToAllPlayers(String msg, boolean bold,  Object... formatArgs) {
        sendMessageToAllPlayers(msg, 0xFFFFFF, bold, formatArgs);
    }

    public static void sendMessageToAllPlayersNoNewlines(String msg) {
        sendMessageToAllPlayersNoNewlines(msg, false);
    }

    public static void sendMessageToAllPlayersNoNewlines(String msg, boolean bold, Object... formatArgs) {
        for (ServerPlayer player : players) {
            if (bold) {
                player.sendSystemMessage(Component.translatable(msg, formatArgs).withStyle(Style.EMPTY.withBold(true)));
            } else {
                player.sendSystemMessage(Component.translatable(msg, formatArgs));
            }
        }
    }

    public static void sendMessageToPlayerNoNewLines(String playerName, String msg) {
        sendMessageToPlayerNoNewLines(playerName, msg, false);
    }

    public static void sendMessageToPlayer(String playerName, String msg) {
        sendMessageToPlayer(playerName, msg, false);
    }

    public static void sendMessageToPlayerNoNewLines(String playerName, String msg, boolean bold, Object... formatArgs) {
        for (ServerPlayer player : players) {
            if (player.getName().getString().equals(playerName)) {
                if (bold) {
                    player.sendSystemMessage(Component.translatable(msg, formatArgs).withStyle(Style.EMPTY.withBold(true)));
                } else {
                    player.sendSystemMessage(Component.translatable(msg, formatArgs));
                }
                return;
            }
        }
    }

    public static void sendMessageToPlayer(String playerName, String msg, boolean bold, Object... formatArgs) {
        for (ServerPlayer player : players) {
            if (player.getName().getString().equals(playerName)) {
                player.sendSystemMessage(Component.literal(""));
                if (bold) {
                    player.sendSystemMessage(Component.translatable(msg, formatArgs).withStyle(Style.EMPTY.withBold(true)));
                } else {
                    player.sendSystemMessage(Component.translatable(msg, formatArgs));
                }
                player.sendSystemMessage(Component.literal(""));
                return;
            }
        }
    }

    /**
     * H.4: a match only ends for a player who actually had something to lose. Without this a player
     * who has not built anything yet would be defeated the moment their starting units die, even
     * though they still have a whole match ahead of them.
     */
    public static boolean canBeDefeated(String playerName) {
        RTSPlayer rtsPlayer = getRTSPlayer(playerName);
        return rtsPlayer != null && rtsPlayer.hasEverOwnedBuilding;
    }

    // defeat a player, giving them a defeat screen, removing all their unit/building control and removing them from
    // rtsPlayers
    public static void defeat(int playerId, String reason) {
        for (ServerPlayer player : players) {
            if (player.getId() == playerId) {
                defeat(player.getName().getString(), reason);
                return;
            }
        }
    }

    public static void defeat(String playerName, String reason) {
        boolean playerExists = false;
        for (RTSPlayer rtsPlayer : rtsPlayers) {
            if (rtsPlayer.name.equals(playerName)) {
                playerExists = true;
                break;
            }
        }
        if (!playerExists)
            return;

        ReignOfNether.LOGGER.info("[Player] defeat: playerName={}, reason={}", playerName, reason);

        synchronized (rtsPlayers) {
            // Remove the defeated player from the list
            

            CompletableFuture.delayedExecutor(3000, TimeUnit.MILLISECONDS).execute(() -> {
                for (ServerPlayer sp : players) {
                    if (sp.getName().getString().equals(playerName)) {
                        
                        break;
                    }
                }
            });

            rtsPlayers.removeIf(rtsPlayer -> {
                if (rtsPlayer.name.equals(playerName)) {
                    sendMessageToAllPlayers("server.reignofnether.is_defeated", true, playerName, reason);
                    sendMessageToAllPlayers("server.reignofnether.players_remaining", false, (rtsPlayers.size() - 1));

                    postGameRtsPlayers.add(rtsPlayer);

                    PlayerClientboundPacket.defeat(playerName);

                    // H.4: the defeated player's units go neutral right away, not on the next match reset
                    neutraliseUnitsOf(playerName);

                    // losing also wipes the player's research progress
                    if (serverLevel != null) {
                        ResearchSaveData.getInstance(serverLevel).clear(playerName);
                        ResearchClientboundPacket.sync(playerName, java.util.Set.of());
                    }
                    for (BuildingPlacement building : BuildingServerEvents.getBuildings()) {
                        if (building.ownerName.equals(playerName)) {
                            if (building instanceof ProductionPlacement productionBuilding)
                                productionBuilding.productionQueue.clear();
                            building.ownerName = "";
                        }
                    }
                    return true;
                }
                return false;
            });

            // Remove resources associated with the defeated player
            saveRTSPlayers();
            ResourcesServerEvents.resourcesList.removeIf(rl -> rl.ownerName.equals(playerName));

            // Check if only allied players are left or if a single player remains
            if (rtsPlayers.size() > 1) {
                // Get the set of remaining player names
                Set<String> remainingPlayers = new HashSet<>();
                for (RTSPlayer player : rtsPlayers) {
                    String name = player.name;
                    remainingPlayers.add(name);
                }

                // Use the first remaining player as a reference to find all connected allies
                String referencePlayer = remainingPlayers.iterator().next();
                Set<String> factionGroup = AlliancesServerEvents.getAllConnectedAllies(referencePlayer);

                // Check if all remaining players are part of the same alliance group
                if (remainingPlayers.equals(factionGroup)) {
                    // Declare victory for all players in the faction group
                    for (String winner : remainingPlayers) {
                        postGameRtsPlayers.add(getRTSPlayer(winner));
                        sendMessageToAllPlayers("server.reignofnether.victory_alliance", true, winner);
                        PlayerClientboundPacket.victory(winner);
                    }
                    broadcastMatchStats(remainingPlayers);
                }
            } else if (rtsPlayers.size() == 1) {
                // Single remaining player - declare victory
                RTSPlayer winner = rtsPlayers.get(0);
                postGameRtsPlayers.add(winner);
                sendMessageToAllPlayers("server.reignofnether.victorious", true, winner.name);
                PlayerClientboundPacket.victory(winner.name);
                broadcastMatchStats(Set.of(winner.name));
            }
        }
    }

    /**
     * H.4: a defeated player's units become neutral (owned by nobody) immediately, not on the next
     * match reset. The owner name is cleared first, then the behaviour resets run in a try/catch per
     * unit: previously a single reset that threw aborted the whole loop, leaving every remaining unit
     * still owned by the defeated player.
     */
    public static void neutraliseUnitsOf(String playerName) {
        for (LivingEntity entity : new ArrayList<>(UnitServerEvents.getAllUnits())) {
            if (!(entity instanceof Unit unit && unit.isRtsUnit()) || !unit.getOwnerName().equals(playerName))
                continue;
            com.solegendary.reignofnether.research.ResearchAttributeApplier.removeFor(unit);
            unit.setOwnerName("");
            try {
                unit.resetBehaviours();
                Unit.resetBehaviours(unit);
                if (unit instanceof Unit aUnit && aUnit.isAttacker())
                    Unit.resetAttackerBehaviours(aUnit);
                if (unit instanceof Unit wUnit && wUnit.isWorker())
                    Unit.resetWorkerBehaviours(wUnit);
            } catch (Exception e) {
                ReignOfNether.LOGGER.error("Failed to reset behaviours of neutralised unit {}", entity.getId(), e);
            }
        }
    }

    // Sends the final per-player scoreboard to all clients so they can show the
    // end-of-match stats popup (MatchEndScreen). Winners are those in winnerNames;
    // everyone else (already moved to postGameRtsPlayers on defeat) is a loser.
    public static void broadcastMatchStats(Set<String> winnerNames) {
        LinkedHashMap<String, RTSPlayer> byName = new LinkedHashMap<>();
        synchronized (rtsPlayers) {
            for (RTSPlayer p : rtsPlayers)
                if (p != null) byName.putIfAbsent(p.name, p);
        }
        synchronized (postGameRtsPlayers) {
            for (RTSPlayer p : postGameRtsPlayers)
                if (p != null) byName.putIfAbsent(p.name, p);
        }
        List<MatchStatsClientboundPacket.MatchStatRow> rows = new ArrayList<>();
        for (RTSPlayer p : byName.values())
            rows.add(new MatchStatsClientboundPacket.MatchStatRow(
                    p.name, winnerNames.contains(p.name), p.startPosColorId,
                    p.scores.getScoreListAsArray()));
        MatchStatsClientboundPacket.broadcast(rtsGameTicks, rows);
    }

    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent evt) {
        AllyCommand.register(evt.getDispatcher());
        RTSPlayerScoresCommand.register(evt.getDispatcher());

        // H.5: force a player's loss by hand, for running the defeat scenario. Use it as the player
        // itself with "/execute as <player> run rts-force-loose".
        evt.getDispatcher().register(Commands.literal("rts-force-loose")
            .requires(command -> command.hasPermission(RTS_PASS_OP_LEVEL))
            .executes((command) -> {
                ServerPlayer player = command.getSource().getPlayer();
                if (player == null) {
                    return 0;
                }
                defeat(player.getName().getString(), Component.translatable("server.reignofnether.surrendered").getString());
                return 1;
            }));

        evt.getDispatcher().register(Commands.literal("rts-lock").then(Commands.literal("enable").executes((command) -> {
            if ((command.getSource() != null &&
                command.getSource().getPlayer() != null &&
                command.getSource().getPlayer().hasPermissions(4)) ||
                (command.getSource() != null &&
                !command.getSource().isPlayer())) {
                setRTSLock(true);
                return 1;
            }
            return 0;
        })));

        evt.getDispatcher().register(Commands.literal("rts-lock").then(Commands.literal("disable").executes((command) -> {
            if ((command.getSource() != null &&
                command.getSource().getPlayer() != null &&
                command.getSource().getPlayer().hasPermissions(4)) ||
                (command.getSource() != null &&
                !command.getSource().isPlayer())) {
                setRTSLock(false);
                return 1;
            }
            return 0;
        })));
    }

    public static int resetRTS(boolean hardReset) {
        ReignOfNether.LOGGER.info("[Player] resetRTS: hardReset={}", hardReset);

        synchronized (rtsPlayers) {
            rtsPlayers.clear();

            for (LivingEntity entity : UnitServerEvents.getAllUnits())
                if (hardReset || (entity instanceof Unit unit && unit.isRtsUnit() && !Unit.hasAnchor(unit)))
                    entity.kill();

            UnitServerEvents.getAllUnits().removeIf(u -> (hardReset || (u instanceof Unit unit && unit.isRtsUnit() && !Unit.hasAnchor(unit))));

            for (LivingEntity entity : UnitServerEvents.getAllUnits())
                if (entity instanceof Unit unit && unit.isRtsUnit()) {
                    com.solegendary.reignofnether.research.ResearchAttributeApplier.removeFor(unit);
                    unit.setOwnerName("");
                }

            for (BuildingPlacement building : BuildingServerEvents.getBuildings()) {
                if (building instanceof ProductionPlacement productionBuilding)
                    productionBuilding.productionQueue.clear();
                if (building.getBuilding().shouldDestroyOnReset || hardReset)
                    building.destroy((ServerLevel) building.getLevel());
            }
            BuildingServerEvents.getBuildings().removeIf(b -> b.getBuilding().shouldDestroyOnReset || hardReset);

            for (BuildingPlacement building : BuildingServerEvents.getBuildings())
                building.ownerName = "";

            PlayerClientboundPacket.resetRTS(hardReset);
            if (hardReset)
                sendMessageToAllPlayers("server.reignofnether.match_reset_hard", true);
            else
                sendMessageToAllPlayers("server.reignofnether.match_reset", true);
            ResourcesServerEvents.resourcesList.clear();
            saveAll();

            if (rtsLocked)
                setRTSLock(false);
            AlliancesServerEvents.resetAllAlliances();
        }

        for (ServerPlayer player : serverLevel.players())
            player.setGameMode(GameType.SPECTATOR);

        // a fresh match starts everyone without research
        for (ServerPlayer player : serverLevel.players()) {
            String name = player.getName().getString();
            ResearchSaveData.getInstance(serverLevel).clear(name);
            ResearchClientboundPacket.sync(name, java.util.Set.of());
        }

        // deliberately NOT rewriting the saved pre-RTS modes to SPECTATOR: doing so meant a player who
        // later left the RTS camera was restored straight back into spectator and could not break
        // blocks anywhere. The saved originals must survive a match reset.
        AlliancesServerEvents.playersWithAlliedControl.clear();

        for (BuildingPlacement bpl : BuildingServerEvents.getBuildings())
            if (bpl instanceof CustomBuildingPlacement cbpl)
                cbpl.resetAllCommands();

        return 1;
    }

    private static void saveAll() {
        saveRTSPlayers();
        postGameRtsPlayers.clear();
        saveBuildings(serverLevel);
        BuildingServerEvents.saveNetherZones(serverLevel);
        UnitServerEvents.saveGatherTargets(serverLevel);
    }

    public static void setRTSLock(boolean lock) {
        setRTSLock(lock, false);
    }

    public static void setRTSLock(boolean lock, boolean noMsg) {
        rtsLocked = lock;
        serverLevel.players().forEach(p -> {
            if (rtsLocked) {
                PlayerClientboundPacket.lockRTS(p.getName().getString());
            } else {
                PlayerClientboundPacket.unlockRTS(p.getName().getString());
            }
        });
        if (!noMsg) {
            if (rtsLocked) {
                sendMessageToAllPlayers("server.reignofnether.match_locked");
            } else {
                sendMessageToAllPlayers("server.reignofnether.match_unlocked");
            }
        }
    }

    public static void setRTSSyncingEnabled(boolean enable) {
        rtsSyncingEnabled = enable;
        if (rtsSyncingEnabled) {
            sendMessageToAllPlayers("server.reignofnether.sync_enabled");
        } else {
            sendMessageToAllPlayers("server.reignofnether.sync_disabled");
        }
    }

}
