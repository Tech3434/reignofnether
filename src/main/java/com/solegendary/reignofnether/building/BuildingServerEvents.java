package com.solegendary.reignofnether.building;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import com.google.common.collect.Sets;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.addon.GarrisonableBuildingAddon;
import com.solegendary.reignofnether.building.addon.NetherConvertingAddon;
import com.solegendary.reignofnether.building.addon.NightSourceAddon;

import com.solegendary.reignofnether.building.custombuilding.CustomBuildingServerEvents;
import com.solegendary.reignofnether.building.data.DataType;
import com.solegendary.reignofnether.building.production.ActiveProduction;
import com.solegendary.reignofnether.building.buildings.placements.CustomBuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.commands.rtsapi.ResourceObjectiveCriteria;

import com.solegendary.reignofnether.hud.HudClientboundPacket;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.resources.*;

import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;

import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;

public class BuildingServerEvents {
    
    public static final DataType<Set<String>> BUILDING_TAGS = DataType.createRegistered(
        ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "building_tags"),
        (tag, server) -> {
            Set<String> tags = Sets.newHashSet();
            if (tag.contains("tags", Tag.TAG_LIST)) {
                ListTag tagList = tag.getList("tags", Tag.TAG_STRING);
                for (int i = 0; i < tagList.size(); i++) {
                    tags.add(tagList.getString(i));
                }
            }
            return tags;
        },
        tags -> {
            CompoundTag nbt = new CompoundTag();
            ListTag tagList = new ListTag();
            for (String tagName : tags) {
                tagList.add(StringTag.valueOf(tagName));
            }
            nbt.put("tags", tagList);
            return nbt;
        },
        Sets::newHashSet
    );
    public static final DataType<ArrayList<BuildingCommand>> BUILDING_COMMANDS = DataType.createRegistered(
        ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "building_commands"),
        (tag, server) -> {
            ArrayList<BuildingCommand> commands = new ArrayList<>();
            if (tag.contains("commands", Tag.TAG_LIST)) {
                ListTag commandList = tag.getList("commands", Tag.TAG_COMPOUND);
                for (int i = 0; i < commandList.size(); i++) {
                    CompoundTag commandTag = commandList.getCompound(i);
                    commands.add(BuildingCommand.getFromNbt(commandTag));
                }
            }
            return commands;
        },
        commands -> {
            CompoundTag nbt = new CompoundTag();
            ListTag commandList = new ListTag();
            for (BuildingCommand command : commands) {
                CompoundTag commandTag = new CompoundTag();
                commandTag.putInt("tickCooldown", command.tickCooldown);
                commandTag.putInt("tickCooldownMax", command.tickCooldownMax);
                commandTag.putString("commandStr", command.commandStr);
                commandTag.putString("condition", command.condition.name());
                commandList.add(commandTag);
            }
            nbt.put("commands", commandList);
            return nbt;
        },
        ArrayList::new
    );
    private static final int BUILDING_SYNC_TICKS_MAX = 20; // how often we send out unit syncing packets
    private static int buildingSyncTicks = BUILDING_SYNC_TICKS_MAX;

    private static final int TNT_BUILDING_BASE_DAMAGE = 20;
    private static final int MAX_SCAFFOLD_DEPTH = 5;

    private static @Nullable ServerLevel serverLevel = null;

    public static @Nullable ServerLevel getServerLevel() { return serverLevel; }

    // buildings that currently exist serverside
    private static final ArrayList<BuildingPlacement> buildings = new ArrayList<>();
    
    public static Object2IntArrayMap<String> populations = new Object2IntArrayMap<>();
    public static final ArrayList<NetherZone> netherZones = new ArrayList<>();

    public static ArrayList<BuildingPlacement> getBuildings() {
        return buildings;
    }

    public static List<BuildingPlacement> getGarrisonableBuildings() {
        ArrayList<BuildingPlacement> garrs = new ArrayList<>();
        for (BuildingPlacement bpl : buildings)
            if (bpl.getBuilding().hasActiveAddon(GarrisonableBuildingAddon.class))
                garrs.add(bpl);
        return garrs;
    }

    public static final Random random = new Random();

    private static final int SAVE_TICKS_MAX = 600;
    private static int saveTicks = 0;
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post evt) {
                saveTicks += 1;
        if (saveTicks >= SAVE_TICKS_MAX) {
            ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);
            if (level != null) {
                saveBuildings(level);
                saveNetherZones(level);
                saveTicks = 0;
            }
        }
    }

    public static void saveBuildings(ServerLevel level) {
        if (level == null)
            return;
        // don't create an empty save file in a world that has no buildings and none stored
        if (getBuildings().isEmpty() && !BuildingSaveData.isStored(level)) {
            level.getDataStorage().save();
            return;
        }
        BuildingSaveData buildingData = BuildingSaveData.getInstance(level);
        buildingData.buildings.clear();

getBuildings().forEach(b -> {
            b.getDataStorage().setData(BUILDING_TAGS, b.tags);
            b.getDataStorage().setData(BUILDING_COMMANDS, b.commands);

            if (b instanceof CustomBuildingPlacement cb) {
                cb.packCommandsNbt();
            }
            buildingData.buildings.add(new BuildingSave(b.originPos,
                    level,
                    b.getBuilding(),
                    b.ownerName,
                    b.rotation,
                    b instanceof ProductionPlacement pb ? pb.getFinalRallyPoint() : b.originPos,
                    b.isDiagonalBridge,
                    b.isBuilt,
                    b.getUpgradeLevel(),
                    b.scenarioRoleIndex,
                    b.getDataStorage(),
                    b.partialBlocksDestroyed,
                    b instanceof CustomBuildingPlacement cb ? cb.commandsNbt : new ListTag()
            ));
            //ReignOfNether.LOGGER.info("saved buildings/nether in serverevents: " + b.originPos);
        });

        // deduplicate saved buildings by removing any additional buildings with the same originPos
        Set<BlockPos> seenOriginPoses = new HashSet<>();
        buildingData.buildings.removeIf(b -> !seenOriginPoses.add(b.originPos));

        buildingData.save();
        level.getDataStorage().save();
    }

    public static void saveNetherZones(ServerLevel level) {
        // don't create an empty save file in a world that has no nether zones and none stored
        if (netherZones.isEmpty() && !NetherZoneSaveData.isStored(level)) {
            level.getDataStorage().save();
            return;
        }
        NetherZoneSaveData netherData = NetherZoneSaveData.getInstance(level);
        netherData.netherZones.clear();
        netherData.netherZones.addAll(netherZones);
        netherData.save();
        level.getDataStorage().save();

        ReignOfNether.LOGGER.info("saved " + netherZones.size() + " netherzones in serverevents");
    }

    @SubscribeEvent
    public static void loadBuildingsAndNetherZones(ServerStartedEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);

        if (level != null) {
            CustomBuildingServerEvents.loadCustomBuildings(level);
            BuildingSaveData buildingData = BuildingSaveData.getLoaded(level);
            NetherZoneSaveData netherData = NetherZoneSaveData.getLoaded(level);
            ArrayList<BlockPos> placedNZs = new ArrayList<>();
            BuildingServerEvents.getBuildings().clear();
buildingData.buildings.forEach(b -> {
                BuildingPlacement building = BuildingUtils.getNewBuildingPlacement(b.building,
                    level,
                    b.originPos,
                    b.rotation,
                    b.ownerName,
                    b.isDiagonalBridge
                );

                if (building != null) {
                    building.partialBlocksDestroyed = b.partialBlocksDestroyed;
                    building.dataStorage = b.dataStorage;

                    building.tags = b.dataStorage.getData(BUILDING_TAGS);
                    building.commands = b.dataStorage.getData(BUILDING_COMMANDS);

                    building.scenarioRoleIndex = b.scenarioRoleIndex;
                    building.isBuilt = b.isBuilt;
                    BuildingServerEvents.getBuildings().add(building);
                    if (building instanceof ProductionPlacement pb) {
                        pb.setRallyPoint(b.rallyPoint);
                    }

                    if (b.upgradeLevel > 0) {
                        String upgradedStructureName = building.getBuilding().getUpgradedStructureName(b.upgradeLevel);
                        if (!upgradedStructureName.equals(building.getBuilding().structureName)) {
                            building.changeStructure(upgradedStructureName);
                        }
                    }
                    // setNetherZone can only be run once - this supercedes where it normally happens in tick() ->
                    // onBuilt()
                    NetherConvertingAddon ncb;
                    if ((ncb = building.getBuilding().getActiveAddon(NetherConvertingAddon.class)) != null && ncb.getMaxNetherRange(building) > 0) {
                        for (NetherZone nz : netherData.netherZones)
                            if (building.isPosInsideBuilding(nz.getOrigin())) {
                                ncb.setNetherZone(building, nz, false);
                                placedNZs.add(nz.getOrigin());
                                ReignOfNether.LOGGER.info("loaded netherzone for: " + b.building.name + "|" + b.originPos);
                                break;
                            }
                    }
                    if (building instanceof CustomBuildingPlacement cb) {
                        cb.setAndUnpackCommandsNbt(b.commandsNbt);
                    }

                    ReignOfNether.LOGGER.info("loaded building in serverevents: " + b.building.name + "|" + b.originPos);
                }
            });
            netherData.netherZones.forEach(nz -> {
                if (!placedNZs.contains(nz.getOrigin())) {
                    BuildingServerEvents.netherZones.add(nz);
                    nz.startRestoring();
                    ReignOfNether.LOGGER.info("loaded orphaned netherzone: " + nz.getOrigin());
                }
            });
            saveNetherZones(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);
        if (level != null) {
            for (BuildingPlacement bp : getBuildings()) {
                if (bp instanceof ProductionPlacement pp)
                    for (ActiveProduction activeProd : new ArrayList<>(pp.productionQueue))
                        pp.cancelProductionItem(activeProd.item, true);
            }
            saveNetherZones(level);
            saveBuildings(level);
            netherZones.clear();
            buildings.clear();
        }
    }

    private static boolean isOverlappingAnyOtherBuilding(BuildingPlacement buildingToPlace) {
        BlockPos minPos = buildingToPlace.minCorner;
        BlockPos maxPos = buildingToPlace.maxCorner;

        for (BuildingPlacement building : buildings) {
            for (BuildingBlock block : building.blocks) {
                if (false &&
                        false) {
                    continue;
                }
                BlockPos bp = block.getBlockPos();
                if (bp.getX() >= minPos.getX() && bp.getX() <= maxPos.getX() && bp.getY() >= minPos.getY()
                        && bp.getY() <= maxPos.getY() && bp.getZ() >= minPos.getZ() && bp.getZ() <= maxPos.getZ()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    public static BuildingPlacement placeBuilding(
        Building building,
        BlockPos originPos,
        Rotation rotation,
        String ownerName,
        int[] builderUnitIds,
        boolean queue, // shift-queue the building for assigned workers
        boolean isDiagonalBridge,
        boolean fromCommand, // ignore resources, terrain or any other restrictions and self-build
        boolean ignoreFog
    ) {
        if (serverLevel == null)
            return null;

        BuildingPlacement newBuilding = BuildingUtils.getNewBuildingPlacement(building,
            serverLevel,
            originPos,
            rotation,
            ownerName,
            isDiagonalBridge
        );
        return placeBuilding(newBuilding, originPos, rotation, ownerName, builderUnitIds, queue, isDiagonalBridge, fromCommand, ignoreFog);
    }

    public static BuildingPlacement placeBuilding(
        BuildingPlacement newBuilding,
        BlockPos originPos,
        Rotation rotation,
        String ownerName,
        int[] builderUnitIds,
        boolean queue, // shift-queue the building for assigned workers
        boolean isDiagonalBridge,
        boolean fromCommand, // ignore resources, terrain or any other restrictions and self-build
        boolean ignoreFog
    ) {
        boolean buildingExists = false;
        for (BuildingPlacement placement : buildings) {
            if (placement.originPos.equals(originPos)) {
                buildingExists = true;
                break;
            }
        }
        if (newBuilding != null && !buildingExists && serverLevel != null) {

            boolean canAfford = fromCommand || newBuilding.canAfford(ownerName);
            if (!canAfford) {
                warnInsufficientResources(newBuilding);
                FogBuildingClientboundPacket.removeFogQueuedBuilding(originPos);
                return null;
            }

            if (!fromCommand && !BuildingValidators.isInBrightChunk(serverLevel, newBuilding.centrePos, ownerName) && !ignoreFog) {
                for (int id : builderUnitIds) {
                    Entity entity = serverLevel.getEntity(id);
                    if (entity instanceof WorkerUnit workerUnit) {
                        if (!queue) {
                            Unit.fullResetBehaviours((Unit) entity);
                        }
                        workerUnit.getExploreBuildLocationGoal().getFogQueuedBuildings().add(newBuilding);
                    }
                }
                return null;
            } else if (!fromCommand) {
                String errorMsgKey = BuildingValidators.getPlacementValidityError(serverLevel, newBuilding.getBuilding(), originPos, ownerName, rotation, isDiagonalBridge, false, true);
                if (errorMsgKey != null) {
                    HudClientboundPacket.showTempMessageI18n(ownerName, errorMsgKey);
                    FogBuildingClientboundPacket.removeFogQueuedBuilding(originPos);
                    return null;
                }
            }

            if (fromCommand ||
                    (serverLevel.getGameRules().getRule(GameRuleRegistrar.SLANTED_BUILDING).get() &&
                            !(false))) {
                BuildingUtils.clearBuildingArea(newBuilding);
            }
            buildings.add(newBuilding);
            newBuilding.forceChunk(true);
            int minY = BuildingUtils.getMinCorner(newBuilding.blocks).getY();

            if (!(false))
                for (BuildingBlock block : newBuilding.blocks)
                    if (block.getBlockPos().getY() == minY && !block.getBlockState().isAir())
                        placeScaffoldingUnder(block, newBuilding);

            // speed up first capitol
            if (newBuilding.isCapitol && BuildingUtils.getTotalCompletedBuildingsOwned(false, ownerName) == 0) {
                newBuilding.blocksPerBuild = 2;
                newBuilding.maxBlocksPerTick = 2;
            }
            for (BuildingBlock block : newBuilding.blocks) {
                if (block.getBlockPos().getY() <= minY + (newBuilding.getBuilding().foundationYLayers - 1)
                        && newBuilding.getBuilding().startingBlockTypes.contains(block.getBlockState().getBlock())) {
                    newBuilding.addToBlockPlaceQueue(block);
                }
            }

            BuildingClientboundPacket.placeBuilding(originPos,
                    newBuilding.getBuilding(),
                    rotation,
                    false ? "" : ownerName,
                    -1,
                    newBuilding.blockPlaceQueue.size(),
                    isDiagonalBridge,
                    0,
                    false
            );
            if (!fromCommand) {
                ResourcesServerEvents.addSubtractResources(new Resources(ownerName,
                        -newBuilding.getBuilding().cost.food,
                        -newBuilding.getBuilding().cost.wood,
                        -newBuilding.getBuilding().cost.ore
                ));
            }

            assignBuilderUnits(builderUnitIds, queue, newBuilding);

            for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
                if (entity instanceof Unit unit && unit.getOwnerName().equals(ownerName) &&
                        newBuilding.isPosInsideBuilding(entity.getOnPos().above().above()) &&
                        (unit.getMoveGoal().getMoveTarget() == null ||
                                newBuilding.isPosInsideBuilding(unit.getMoveGoal().getMoveTarget()))) {
                    moveNonBuildersAwayFromBuildingFoundations(entity, builderUnitIds, newBuilding);
                }
            }
            moveAnimalsAwayFromBuildingFoundations(newBuilding);

            if (false)
                newBuilding.ownerName = "";

            return newBuilding;
        }
        return null;
    }

    private static void placeScaffoldingUnder(BuildingBlock block, BuildingPlacement newBuilding) {
        BlockPos basePos = block.getBlockPos();
        int yBelow = 0;
        BlockState bsBelow;

        // Search downward for a solid block up to -5 levels below
        while (yBelow > -MAX_SCAFFOLD_DEPTH) {
            yBelow--;
            BlockPos bpBelow = basePos.offset(0, yBelow, 0);
            if (MiscUtil.isSolidBlocking(newBuilding.level, bpBelow)) {
                break; // Found a solid block, exit loop
            }
        }
        if (yBelow <= -MAX_SCAFFOLD_DEPTH) {
            return;
        }

        // Place scaffolding from the lowest point back to the original block's level
        for (int y = yBelow + 1; y < 0; y++) {
            BlockPos scaffoldPos = basePos.offset(0, y, 0);
            BuildingBlock scaffold = new BuildingBlock(scaffoldPos, scaffoldStateFor(newBuilding, scaffoldPos));
            newBuilding.getScaffoldBlocks().add(scaffold);
            newBuilding.addToBlockPlaceQueue(scaffold);
        }
    }

    /**
     * The filler block dropped under a foundation. A building picks one of three modes:
     * a single block for everything, the mod's own scaffolding, or a surface-aware pair where the top
     * layer is grass and everything under it is dirt, so a base cut into a hillside does not leave a
     * row of grey scaffolding along the surface.
     */
    private static BlockState scaffoldStateFor(BuildingPlacement placement, BlockPos pos) {
        Building.ScaffoldFill fill = placement.getBuilding().scaffoldFill;
        Level level = placement.level;
        if (fill == Building.ScaffoldFill.BIOME_AWARE) {
            boolean topLayer = level.getBlockState(pos.above()).isAir();
            return (topLayer ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState();
        }
        if (fill == Building.ScaffoldFill.CUSTOM && placement.getBuilding().scaffoldBlock != null) {
            return placement.getBuilding().scaffoldBlock.defaultBlockState();
        }
        return Blocks.SCAFFOLDING.defaultBlockState();
    }

    private static void assignBuilderUnits(int[] builderUnitIds, boolean queue, BuildingPlacement newBuilding) {
        if (serverLevel == null)
            return;

        for (int id : builderUnitIds) {
            Entity entity = serverLevel.getEntity(id);
            if (entity instanceof WorkerUnit workerUnit) {
                if (queue) {
                    if (workerUnit.getBuildRepairGoal().queuedBuildings.isEmpty()) {
                        ((Unit) entity).resetBehaviours();
                        WorkerUnit.resetBehavioursExceptExploreBuild(workerUnit);
                    }
                    workerUnit.getBuildRepairGoal().queuedBuildings.add(newBuilding);
                    if (workerUnit.getBuildRepairGoal().getBuildingTarget() == null) {
                        workerUnit.getBuildRepairGoal().startNextQueuedBuilding();
                    }
                } else {
                    ((Unit) entity).resetBehaviours();
                    WorkerUnit.resetBehavioursExceptExploreBuild(workerUnit);
                    workerUnit.getBuildRepairGoal().setBuildingTarget(newBuilding);
                }
            }
        }
    }

    private static void warnInsufficientResources(BuildingPlacement newBuilding) {
        ResourcesClientboundPacket.warnInsufficientResources(newBuilding.ownerName,
            ResourcesServerEvents.canAfford(newBuilding.ownerName, ResourceName.FOOD, newBuilding.getBuilding().cost.food),
            ResourcesServerEvents.canAfford(newBuilding.ownerName, ResourceName.WOOD, newBuilding.getBuilding().cost.wood),
            ResourcesServerEvents.canAfford(newBuilding.ownerName, ResourceName.ORE, newBuilding.getBuilding().cost.ore),
            ResourcesServerEvents.canAfford(newBuilding.ownerName, ResourceName.EMERALD, newBuilding.getBuilding().cost.emerald)
        );
    }

    private static void moveNonBuildersAwayFromBuildingFoundations(
        LivingEntity entity, int[] builderUnitIds, BuildingPlacement newBuilding
    ) {
        boolean b = true;
        for (int id : builderUnitIds) {
            if (id == entity.getId()) {
                b = false;
                break;
            }
        }
        if (b) {
            UnitServerEvents.addActionItem(((Unit) entity).getOwnerName(),
                UnitAction.MOVE,
                -1,
                new int[] { entity.getId() },
                newBuilding.getClosestGroundPos(entity.getOnPos(), 3, true),
                new BlockPos(0, 0, 0)
            );
        }
    }

    private static void moveAnimalsAwayFromBuildingFoundations(BuildingPlacement newBuilding) {
        List<Mob> mobs = MiscUtil.getEntitiesWithinRange(newBuilding.centrePos.getCenter(), 10, Mob.class, newBuilding.level);
        for (Mob mob : mobs) {
            if (ResourceSources.isHuntableAnimal(mob)) {
                BlockPos bp = newBuilding.getClosestGroundPos(mob.getOnPos(), 3, true);
                Path path = mob.getNavigation().createPath(bp.getX(), bp.getY(), bp.getZ(), 0);
                mob.getNavigation().moveTo(path, 1);
            }
        }
    }

    public static void cancelBuilding(BuildingPlacement building, String playerName) {
        if (building == null)
            return;
        if (building.isBuilt &&
            BuildingUtils.getTotalCompletedBuildingsOwned(false, building.ownerName) == 1) {
            HudClientboundPacket.showTempMessageI18n(playerName,"hud.helperbuttons.reignofnether.cancel.error");
            return;
        }
        if (building.getBuilding().capturable) {
            HudClientboundPacket.showTempMessageI18n(playerName,"hud.helperbuttons.reignofnether.cancel.error");
            return;
        }
        NightSourceAddon nsa = building.getBuilding().getActiveAddon(NightSourceAddon.class);
        if (nsa != null && nsa.getNightRange(building) > 0 && building.isBuilt) {
            HudClientboundPacket.showTempMessageI18n(playerName,"hud.helperbuttons.reignofnether.cancel.error");
            return;
        }

        // remove from tracked buildings, all of its leftover queued blocks and then blow it up
        buildings.remove(building);
        NetherConvertingAddon ncb;
        if ((ncb = building.getBuilding().getActiveAddon(NetherConvertingAddon.class)) != null && ncb.getMaxNetherRange(building) > 0 && ncb.getNetherZone(building) != null) {
            NetherZone nz = ncb.getNetherZone(building);
            if (nz != null)
                nz.startRestoring();
            saveNetherZones(serverLevel);
        }

        // AOE2-style refund: return the % of the non-built portion of the building
        // eg. cancelling a building at 70% completion will refund only 30% cost
        // in survival, refund 50% of this amount
        if (!building.isBuilt || false) {

            float buildPercent = building.getBlocksPlacedPercent();
            int food = Math.round(building.getBuilding().cost.food * (1 - buildPercent));
            int wood = Math.round(building.getBuilding().cost.wood * (1 - buildPercent));
            int ore = Math.round(building.getBuilding().cost.ore * (1 - buildPercent));

            if (building.isBuilt && false) {
                food = Math.round(building.getBuilding().cost.food * 0.5f * buildPercent);
                wood = Math.round(building.getBuilding().cost.wood * 0.5f * buildPercent);
                ore = Math.round(building.getBuilding().cost.ore * 0.5f * buildPercent);
            }
            if (food > 0 || wood > 0 || ore > 0) {
                Resources res = new Resources(building.ownerName, food, wood, ore);
                ResourcesServerEvents.addSubtractResources(res);
                ResourcesClientboundPacket.showFloatingText(res, building.centrePos);
            }
        }
        building.destroy((ServerLevel) building.getLevel());
    }

    // removes a placed building without destroying its blocks, telling clients; a nether zone under it starts restoring
    public static void removeBuildingPlacement(BlockPos pos) {
        buildings.removeIf(b -> {
            if (b.originPos.equals(pos)) {
                BuildingClientboundPacket.removeBuilding(pos);
                NetherConvertingAddon ncb;
                if ((ncb = b.getBuilding().getActiveAddon(NetherConvertingAddon.class)) != null
                        && ncb.getMaxNetherRange(b) > 0 && ncb.getNetherZone(b) != null)
                    ncb.getNetherZone(b).startRestoring();
                return true;
            }
            return false;
        });
    }

    public static int getTotalPopulationSupply(String ownerName) {
        // base limit is building-independent; only capitols raise it (Building.populationSupply)
        return UnitServerEvents.maxPopulation + UnitServerEvents.getPopulationBonusFromCapitols(ownerName);
    }

    // similar to BuildingClientEvents getPlayerToBuildingRelationship: given a Unit and Building, what is the
    // relationship between them
    public static Relationship getUnitToBuildingRelationship(Unit unit, BuildingPlacement building) {
        if (unit.getOwnerName().equals(building.ownerName)) {
            return Relationship.OWNED;
        } else {
            return Relationship.HOSTILE;
        }
    }

    private static void placeBuildingsClientside() {
        for (BuildingPlacement building : buildings) {
            BuildingClientboundPacket.placeBuilding(building.originPos,
                    building.getBuilding(),
                    building.rotation,
                    building.ownerName,
                    building.scenarioRoleIndex,
                    building.blockPlaceQueue.size(),
                    building.isDiagonalBridge,
                    building.getUpgradeLevel(),
                    building.isBuilt
            );
        }
    }

    public static void placeBuildingClientside(BlockPos pos) {
        for (BuildingPlacement building : buildings) {
            if (building.originPos.equals(pos)) {
                BuildingClientboundPacket.placeBuilding(building.originPos,
                        building.getBuilding(),
                        building.rotation,
                        building.ownerName,
                        building.scenarioRoleIndex,
                        building.blockPlaceQueue.size(),
                        building.isDiagonalBridge,
                        building.getUpgradeLevel(),
                        building.isBuilt
                );
            }
            break;
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (!PlayerServerEvents.rtsSyncingEnabled) {
            return;
        }
        MinecraftServer server = evt.getEntity().level().getServer();
        if (server == null || !server.isDedicatedServer()) {
            CompletableFuture.delayedExecutor(1000, TimeUnit.MILLISECONDS).execute(BuildingServerEvents::placeBuildingsClientside);
        } else {
            placeBuildingsClientside();
        }
        //ReignOfNether.LOGGER.info("Synced " + buildings.size() + " buildings with player logged in");
    }

    // if blocks are destroyed manually by a player then help it along by causing periodic explosions
    @SubscribeEvent
    public static void onPlayerBlockBreak(BlockEvent.BreakEvent evt) {
        if (!evt.getLevel().isClientSide()) {
            for (BuildingPlacement building : buildings)
                if (building.isPosPartOfBuilding(evt.getPos(), true)) {
                    building.onBlockBreak((ServerLevel) evt.getLevel(), evt.getPos(), true);
                }
        }
    }

    // prevent dungeons spawners from actually spawning
    // (1.21.1 removed MobSpawnEvent.FinalizeSpawn, so this hooks entity-joins instead and
    //  looks the building up at the mob.s position rather than at the spawner block)
    @SubscribeEvent
    public static void onLivingSpawn(EntityJoinLevelEvent evt) {
        if (evt.getLevel().isClientSide() || !(evt.getEntity() instanceof Mob mob)) return;

    }

    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent.Post evt) {
        if (evt.getLevel().isClientSide() || evt.getLevel().dimension() != Level.OVERWORLD) {
            return;
        }

        serverLevel = (ServerLevel) evt.getLevel();

        buildingSyncTicks -= 1;
        if (buildingSyncTicks <= 0) {
            buildingSyncTicks = BUILDING_SYNC_TICKS_MAX;
            for (BuildingPlacement building : buildings)
                BuildingClientboundPacket.syncBuilding(building.originPos, building.getBlocksPlaced(),
                        building.partialBlocksDestroyed, building.ownerName, building.scenarioRoleIndex);
        }
        // need to remove from the list first as destroy() will read it to check defeats
        List<BuildingPlacement> buildingsToDestroy = new ArrayList<>();
        for (BuildingPlacement buildingPlacement : buildings) {
            if (buildingPlacement.shouldBeDestroyed()) {
                buildingsToDestroy.add(buildingPlacement);
            }
        }
        buildings.removeIf(b -> {
            if (b.shouldBeDestroyed()) {
                NetherConvertingAddon ncb;
                if ((ncb = b.getBuilding().getActiveAddon(NetherConvertingAddon.class)) != null && ncb.getMaxNetherRange(b) > 0 && ncb.getNetherZone(b) != null) {
                    NetherZone nz = ncb.getNetherZone(b);
                    if (nz != null)
                        nz.startRestoring();
                    saveNetherZones(serverLevel);
                }
                return true;
            }
            return false;
        });

        for (BuildingPlacement building : buildingsToDestroy) {
            // Tell clients to drop the placement too. destroy() only clears the world blocks; without
            // this the client keeps the building in its list, so a destroyed building stayed standing
            // on screen and could not be repaired (the server no longer had it).
            BuildingClientboundPacket.removeBuilding(building.originPos);
            building.destroy(serverLevel);
        }

        ArrayList<BuildingPlacement> bpls = new ArrayList<>(buildings);
        for (BuildingPlacement building : bpls)
            building.tick(serverLevel);

        for (NetherZone netherConversionZone : netherZones)
            netherConversionZone.tick(serverLevel);

        int nzSizeBefore = netherZones.size();
        netherZones.removeIf(NetherZone::isDone);
        int nzSizeAfter = netherZones.size();
        if (nzSizeBefore != nzSizeAfter) {
            saveNetherZones(serverLevel);
        }

        String playerName;
        if (serverLevel.getServer().getTickCount() % 10 == 0) {
            for (Objective objective : serverLevel.getScoreboard().getObjectives()) {
                if (objective.getCriteria().equals(ResourceObjectiveCriteria.POPULATION))
                    for (ServerPlayer player : serverLevel.players()) {
                        playerName = player.getName().getString();
                        int currentPopulation = UnitServerEvents.getCurrentPopulation(playerName);
                        if (serverLevel != null && currentPopulation != populations.getInt(playerName)) {
                            populations.put(playerName, currentPopulation);
	                        // 1.21.1's forAllObjectives takes a ScoreHolder, not a String, and
	                        // ScoreAccess renamed setScore to set.
	                        serverLevel.getScoreboard().forAllObjectives(ResourceObjectiveCriteria.POPULATION, player, (p_9178_) -> p_9178_.set(currentPopulation));
                        }
                    }
            }
            
        }
    }

    // cancel all explosion damage to non-building blocks
    // cancel damage to entities and non-building blocks if it came from a non-entity source such as:
    // - building block breaks
    // - beds (vanilla)
    // - respawn anchors (vanilla)
    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate evt) {
        Explosion exp = evt.getExplosion();

        if (exp.getDirectSourceEntity() == null) {
            evt.getAffectedEntities().clear();
        }

        // bonus damage from vanilla TNT to the buildings it overlaps
        if (exp.getDirectSourceEntity() instanceof PrimedTnt) {
            Set<BuildingPlacement> affectedBuildings = new HashSet<>();

            for (BlockPos bp : evt.getAffectedBlocks()) {
                BuildingPlacement building = BuildingUtils.findBuilding(false, bp);
                if (building != null)
                    affectedBuildings.add(building);
            }
            for (BuildingPlacement building : affectedBuildings) {
                float atkDmg = TNT_BUILDING_BASE_DAMAGE;

                if (atkDmg > 0) {
                    // all explosion damage will directly hit all occupants at an average of 1/4 rate
                    GarrisonableBuildingAddon garr;
                    if ((garr = building.getBuilding().getActiveAddon(GarrisonableBuildingAddon.class)) != null) {
                        for (LivingEntity le : garr.getOccupants(building))
                            // 1.21.1 made Explosion's damage source a field opened up by the AT
                            // rather than a getter.
                            le.hurt(exp.damageSource, (random.nextFloat(atkDmg + 1)) / 2f);
                    }

                    building.destroyRandomBlocks(atkDmg);
                }

            }
        }
        // Explosions must not level the world just because a unit happened to be nearby. Only the building
        // footprint is protected: blocks that are part of a placed building are removed from the
        // explosion's block list, everything else is left to vanilla. The previous version cleared all
        // block damage except leaves and TNT unless a gamerule was set, which made creepers, TNT and
        // beds harmless everywhere in the world rather than near a base.
        evt.getAffectedBlocks().removeIf(bp -> BuildingUtils.findBuilding(true, bp) != null);

    }

    @SubscribeEvent
    public static void onEntityTravelToDimension(EntityTravelToDimensionEvent evt) {
        BuildingPlacement building = BuildingUtils.findBuilding(evt.getEntity().level().isClientSide(), evt.getEntity().getOnPos());
        if (building != null) {
            evt.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCropTrample(BlockEvent.FarmlandTrampleEvent evt) {
        if (BuildingUtils.isPosInsideAnyBuilding(evt.getEntity().level().isClientSide(), evt.getPos())) {
            evt.setCanceled(true);
        }
    }

    // prevent crops from becoming items when a farm is damaged
    @SubscribeEvent
    public static void onItemDrop(EntityJoinLevelEvent evt) {
        if (evt.getEntity() instanceof ItemEntity ie && BuildingUtils.isPosInsideFarm(evt.getLevel().isClientSide(), ie.getOnPos())) {
            Item item = ie.getItem().getItem();
            if (List.of(Items.NETHER_WART, Items.WHEAT, Items.CARROT, Items.BEETROOT, Items.POTATO).contains(item)) {
                evt.setCanceled(true);
            }
        }
    }

    public static void replaceClientBuilding(BlockPos buildingPos) {
        if (!PlayerServerEvents.rtsSyncingEnabled) {
            return;
        }
        for (BuildingPlacement building : buildings) {
            if (building.originPos.equals(buildingPos)) {
                BuildingClientboundPacket.placeBuilding(
                        building.originPos,
                        building.getBuilding(),
                        building.rotation,
                        building.ownerName,
                        building.scenarioRoleIndex,
                        building.blockPlaceQueue.size(),
                        building.isDiagonalBridge,
                        building.getUpgradeLevel(),
                        building.isBuilt
                );
                return;
            }
        }
    }
}
