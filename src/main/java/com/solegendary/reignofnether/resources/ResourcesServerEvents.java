package com.solegendary.reignofnether.resources;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ActiveProduction;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;

import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;

import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.*;

import static com.solegendary.reignofnether.blocks.BlockUtils.isLogBlock;

public class ResourcesServerEvents {

    // tracks all players' resources
    public static ArrayList<Resources> resourcesList = new ArrayList<>();

    public static final int STARTING_FOOD_TUTORIAL = 750;
    public static final int STARTING_WOOD_TUTORIAL = 850;
    public static final int STARTING_ORE_TUTORIAL = 250;
    public static final int STARTING_FOOD_SANDBOX = 999999;
    public static final int STARTING_WOOD_SANDBOX = 999999;
    public static final int STARTING_ORE_SANDBOX = 999999;
    public static final int STARTING_EMERALD_SANDBOX = 999999;
    public static final int STARTING_FOOD = 150;
    public static final int STARTING_WOOD = 500;
    public static final int STARTING_ORE = 300;
    public static final int STARTING_FOOD_READIED = 150;
    public static final int STARTING_WOOD_READIED = 150;
    public static final int STARTING_ORE_READIED = 50;

    public static final float UNIT_BOUNTY_PERCENT_PER_LOOTING_LEVEL = 0.25f;
    public static final float NEUTRAL_UNIT_BOUNTY_PERCENT = 0.25f;
    public static final float NEUTRAL_BUILDING_BOUNTY_PERCENT = 0.25f;

    public static void resetResources(String playerName, boolean readiedStart) {
        for (Resources resources : resourcesList) {
            if (resources.ownerName.equals(playerName)) {
                if (readiedStart) {
                    resources.food = STARTING_FOOD_READIED;
                    resources.wood = STARTING_WOOD_READIED;
                    resources.ore = STARTING_ORE_READIED;
                } else {
                    resources.food = STARTING_FOOD;
                    resources.wood = STARTING_WOOD;
                    resources.ore = STARTING_ORE;
                }
                ResourcesClientboundPacket.syncResources(resourcesList);
                break;
            }
        }
    }

    public static void addSubtractResources(Resources resourcesToAdd) {
        for (Resources resources : resourcesList) {
            if (resources.ownerName.equals(resourcesToAdd.ownerName)) {
                // change serverside instantly
                resources.changeInstantly(resourcesToAdd.food, resourcesToAdd.wood, resourcesToAdd.ore, resourcesToAdd.emerald);
                // change clientside over time
                ResourcesClientboundPacket.addSubtractResources(new Resources(resourcesToAdd.ownerName,
                    resourcesToAdd.food,
                    resourcesToAdd.wood,
                    resourcesToAdd.ore,
                    resourcesToAdd.emerald
                ));
            }
        }
    }

    public static boolean canAfford(String ownerName, ResourceName resourceName, int cost) {
        if (cost <= 0) {
            return true;
        }
        for (Resources resources : ResourcesServerEvents.resourcesList)
            if (resources.ownerName.equals(ownerName)) {
                switch (resourceName) {
                    case FOOD -> {
                        return resources.food >= cost;
                    }
                    case WOOD -> {
                        return resources.wood >= cost;
                    }
                    case ORE -> {
                        return resources.ore >= cost;
                    }
                    case EMERALD -> {
                        return resources.emerald >= cost;
                    }
                }
            }
        return false;
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        String playerName = evt.getEntity().getName().getString();
        ResourcesClientboundPacket.syncResources(resourcesList);
    }

    public static void assignResources(String playerName) {
        resourcesList.removeIf(r -> r.ownerName.equals(playerName));
        Resources resources = new Resources(playerName, STARTING_FOOD, STARTING_WOOD, STARTING_ORE);
        resourcesList.add(resources);
        ResourcesClientboundPacket.syncResources(resourcesList);
    }

    // prevent vanilla growth mechanics because they're slow and random, see FarmPlacement instead
    @SubscribeEvent
    public static void onCropGrow(CropGrowEvent.Pre evt) {
        if (BuildingUtils.isPosInsideAnyBuilding(evt.getLevel().isClientSide(), evt.getPos()))
            evt.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent evt) {
        if (evt.getEntity() instanceof ItemEntity ie &&
                (ie.getItem().getItem() == Items.POTATO || ie.getItem().getItem() == Items.CARROT) &&
                BuildingUtils.isPosInsideAnyBuilding(false, ie.getOnPos())) {
            evt.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerBlockBreak(BlockEvent.BreakEvent evt) {
        // A block that belongs to a building is never dug up: the break is cancelled on the spot (the
        // block stays, nothing drops) and the building takes the damage instead - its own HP system
        // removes blocks as the HP falls. Blocks outside every building are left entirely to vanilla.
        BuildingPlacement building = BuildingUtils.findBuilding(false, evt.getPos());
        if (building != null && building.isPosPartOfBuilding(evt.getPos(), true)) {
            evt.setCanceled(true);
            building.destroyRandomBlocks(building.getHealthPerBlock() / 2d);
            return;
        }

        if (isLogBlock(evt.getState())) {
            fellAdjacentLogs(evt.getPos(), new ArrayList<>(), (Level) evt.getLevel());
        }
    }

    // if a tree is touched, destroy any adjacent logs that are above the ground after some time to avoid leaving
    // tall trees behind
    public static void fellAdjacentLogs(BlockPos bp, ArrayList<BlockPos> bpsExcluded, Level level) {
        if (!level.getGameRules().getRule(GameRuleRegistrar.LOG_FALLING).get())
            return;

        BlockState bs = level.getBlockState(bp);

        List<BlockPos> bpsAdj = List.of(bp.north(),
            bp.south(),
            bp.east(),
            bp.west(),
            bp.above(),
            bp.above().north(),
            bp.above().south(),
            bp.above().east(),
            bp.above().west(),
            bp.north().east(),
            bp.north().west(),
            bp.south().east(),
            bp.south().west(),
            bp.above().north().east(),
            bp.above().north().west(),
            bp.above().south().east(),
            bp.above().south().west()
        );

        for (BlockPos bpAdj : bpsAdj) {
            BlockState bsAdj = level.getBlockState(bpAdj);
            if (isLogBlock(bsAdj) && !bpsExcluded.contains(bpAdj)) {
                Block fallingLogBlock = FALLING_LOGS.get(bsAdj.getBlock());
                if (fallingLogBlock != null && !BuildingUtils.isPosInsideAnyBuilding(level.isClientSide(), bpAdj)) {
                    if (bsAdj.hasProperty(BlockStateProperties.AXIS)) {
                        level.setBlockAndUpdate(bpAdj, fallingLogBlock.defaultBlockState()
                                .setValue(BlockStateProperties.AXIS, bsAdj.getValue(BlockStateProperties.AXIS)));
                    } else {
                        level.setBlockAndUpdate(bpAdj, fallingLogBlock.defaultBlockState());
                    }
                    bpsExcluded.add(bpAdj);
                    fellAdjacentLogs(bpAdj, bpsExcluded, level);
                }
                bpsExcluded.add(bpAdj);
                fellAdjacentLogs(bpAdj, bpsExcluded, level);
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent evt) {

        // Moving resources between players moves a whole balance sheet, so these need an operator
        // permission like the rest of the resource and building commands. They used to have no
        // requirement at all, which let any player drain another's resources.
        evt.getDispatcher().register(Commands.literal("sendfood")
            .requires(cs -> cs.hasPermission(2))
            .then(Commands.argument("player", EntityArgument.player())
            .then(Commands.argument("amount", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
            .executes((command) -> trySendingResources(command, ResourceName.FOOD)))));

        evt.getDispatcher().register(Commands.literal("sendwood")
            .requires(cs -> cs.hasPermission(2))
            .then(Commands.argument("player", EntityArgument.player())
            .then(Commands.argument("amount", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
            .executes((command) -> trySendingResources(command, ResourceName.WOOD)))));

        evt.getDispatcher().register(Commands.literal("sendore")
            .requires(cs -> cs.hasPermission(2))
            .then(Commands.argument("player", EntityArgument.player())
            .then(Commands.argument("amount", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
            .executes((command) -> trySendingResources(command, ResourceName.ORE)))));

        evt.getDispatcher().register(Commands.literal("sendemerald")
            .requires(cs -> cs.hasPermission(2))
            .then(Commands.argument("player", EntityArgument.player())
            .then(Commands.argument("amount", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
            .executes((command) -> trySendingResources(command, ResourceName.EMERALD)))));
    }

    public static int trySendingResources(CommandContext<CommandSourceStack> context, ResourceName resourceName) throws CommandSyntaxException {
        Player sendingPlayer = context.getSource().getPlayer();
        Player receivingPlayer = EntityArgument.getPlayer(context, "player");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        if (sendingPlayer == null) {
            return 0;
        } else {
            String sendingPlayerName = sendingPlayer.getName().getString();
            String receivingPlayerName = receivingPlayer.getName().getString();
            return trySendingResources(sendingPlayerName, receivingPlayerName, resourceName, amount);
        }
    }

    // send resources including any available when you don't have enough
    public static void trySendingAnyResources(String receivingPlayerName, Resources sentResources) {
        Resources res = null;
        for (Resources resources : resourcesList)
            if (resources.ownerName.equals(sentResources.ownerName))
                res = resources;
        if (res != null) {
            int foodAmount = Math.min(res.food, sentResources.food);
            if (foodAmount > 0)
                trySendingResources(sentResources.ownerName,receivingPlayerName, ResourceName.FOOD, foodAmount);
            int woodAmount = Math.min(res.wood, sentResources.wood);
            if (woodAmount > 0)
                trySendingResources(sentResources.ownerName,receivingPlayerName, ResourceName.WOOD, woodAmount);
            int oreAmount = Math.min(res.ore, sentResources.ore);
            if (oreAmount > 0)
                trySendingResources(sentResources.ownerName,receivingPlayerName, ResourceName.ORE, oreAmount);
            int emeraldAmount = Math.min(res.emerald, sentResources.emerald);
            if (oreAmount > 0)
                trySendingResources(sentResources.ownerName,receivingPlayerName, ResourceName.EMERALD, emeraldAmount);

            if (sentResources.getTotalValue() > 0)
                SoundClientboundPacket.playSoundForPlayer(SoundAction.CHAT, receivingPlayerName);
        }
    }

    public static int trySendingResources(String sendingPlayerName, String receivingPlayerName, ResourceName resourceName, int amount) {
        Resources res = null;
        for (Resources resources : resourcesList)
            if (resources.ownerName.equals(sendingPlayerName))
                res = resources;
        if (res == null)
            return 0;

        if (sendingPlayerName.equals(receivingPlayerName)) {
            PlayerServerEvents.sendMessageToPlayer(sendingPlayerName, "server.resources.reignofnether.sending_to_self");
            return 0;
        } else if (!PlayerServerEvents.isRTSPlayer(receivingPlayerName)) {
            PlayerServerEvents.sendMessageToPlayer(sendingPlayerName, "server.resources.reignofnether.not_rts_player");
            return 0;
        } else if (!canAfford(sendingPlayerName, resourceName, amount)) {
            ResourcesClientboundPacket.warnInsufficientResources(sendingPlayerName,
                    resourceName == ResourceName.FOOD,
                    resourceName == ResourceName.WOOD,
                    resourceName == ResourceName.ORE,
                    resourceName == ResourceName.EMERALD
            );
            return 0;
        } else {
            addSubtractResources(new Resources(sendingPlayerName,
                    resourceName == ResourceName.FOOD ? -amount : 0,
                    resourceName == ResourceName.WOOD ? -amount : 0,
                    resourceName == ResourceName.ORE ? -amount : 0,
                    resourceName == ResourceName.EMERALD ? -amount : 0
            ));
            addSubtractResources(new Resources(receivingPlayerName,
                    resourceName == ResourceName.FOOD ? amount : 0,
                    resourceName == ResourceName.WOOD ? amount : 0,
                    resourceName == ResourceName.ORE ? amount : 0,
                    resourceName == ResourceName.EMERALD ? amount : 0
            ));
            switch (resourceName) {
                case FOOD -> {
                    PlayerServerEvents.sendMessageToPlayer(sendingPlayerName, "server.resources.reignofnether.sent_food", false, amount, receivingPlayerName);
                    PlayerServerEvents.sendMessageToPlayer(receivingPlayerName, "server.resources.reignofnether.received_food", false, amount, sendingPlayerName);
                }
                case WOOD -> {
                    PlayerServerEvents.sendMessageToPlayer(sendingPlayerName, "server.resources.reignofnether.sent_wood", false, amount, receivingPlayerName);
                    PlayerServerEvents.sendMessageToPlayer(receivingPlayerName, "server.resources.reignofnether.received_wood", false, amount, sendingPlayerName);
                }
                case ORE -> {
                    PlayerServerEvents.sendMessageToPlayer(sendingPlayerName, "server.resources.reignofnether.sent_ore", false, amount, receivingPlayerName);
                    PlayerServerEvents.sendMessageToPlayer(receivingPlayerName, "server.resources.reignofnether.received_ore", false, amount, sendingPlayerName);
                }
                case EMERALD -> {
                    PlayerServerEvents.sendMessageToPlayer(sendingPlayerName, "server.resources.reignofnether.sent_emerald", false, amount, receivingPlayerName);
                    PlayerServerEvents.sendMessageToPlayer(receivingPlayerName, "server.resources.reignofnether.received_emerald", false, amount, sendingPlayerName);
                }
            }
            return 1;
        }
    }

    private static final Map<Block, Block> FALLING_LOGS = new HashMap<>();
    static {
        FALLING_LOGS.put(Blocks.OAK_LOG, BlockRegistrar.FALLING_OAK_LOG.get());
        FALLING_LOGS.put(Blocks.SPRUCE_LOG, BlockRegistrar.FALLING_SPRUCE_LOG.get());
        FALLING_LOGS.put(Blocks.BIRCH_LOG, BlockRegistrar.FALLING_BIRCH_LOG.get());
        FALLING_LOGS.put(Blocks.JUNGLE_LOG, BlockRegistrar.FALLING_JUNGLE_LOG.get());
        FALLING_LOGS.put(Blocks.ACACIA_LOG, BlockRegistrar.FALLING_ACACIA_LOG.get());
        FALLING_LOGS.put(Blocks.DARK_OAK_LOG, BlockRegistrar.FALLING_DARK_OAK_LOG.get());
        FALLING_LOGS.put(Blocks.MANGROVE_LOG, BlockRegistrar.FALLING_MANGROVE_LOG.get());
        FALLING_LOGS.put(Blocks.CHERRY_LOG, BlockRegistrar.FALLING_CHERRY_LOG.get());
        FALLING_LOGS.put(Blocks.OAK_WOOD, BlockRegistrar.FALLING_OAK_LOG.get());
        FALLING_LOGS.put(Blocks.SPRUCE_WOOD, BlockRegistrar.FALLING_SPRUCE_LOG.get());
        FALLING_LOGS.put(Blocks.BIRCH_WOOD, BlockRegistrar.FALLING_BIRCH_LOG.get());
        FALLING_LOGS.put(Blocks.JUNGLE_WOOD, BlockRegistrar.FALLING_JUNGLE_LOG.get());
        FALLING_LOGS.put(Blocks.ACACIA_WOOD, BlockRegistrar.FALLING_ACACIA_LOG.get());
        FALLING_LOGS.put(Blocks.DARK_OAK_WOOD, BlockRegistrar.FALLING_DARK_OAK_LOG.get());
        FALLING_LOGS.put(Blocks.MANGROVE_WOOD, BlockRegistrar.FALLING_MANGROVE_LOG.get());
        FALLING_LOGS.put(Blocks.CHERRY_WOOD, BlockRegistrar.FALLING_CHERRY_LOG.get());
        FALLING_LOGS.put(Blocks.WARPED_STEM, BlockRegistrar.FALLING_WARPED_STEM.get());
        FALLING_LOGS.put(Blocks.WARPED_HYPHAE, BlockRegistrar.FALLING_WARPED_STEM.get());
        FALLING_LOGS.put(Blocks.CRIMSON_STEM, BlockRegistrar.FALLING_CRIMSON_STEM.get());
        FALLING_LOGS.put(Blocks.CRIMSON_HYPHAE, BlockRegistrar.FALLING_CRIMSON_STEM.get());
    }
}

