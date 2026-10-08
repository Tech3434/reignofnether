package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.buildings.JsonBuilding;
import com.solegendary.reignofnether.building.buildings.JsonBuildingManager;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import static com.solegendary.reignofnether.building.BuildingUtils.findBuilding;

public class BuildingClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<BuildingClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "building_clientbound"));

    @Override
    public CustomPacketPayload.Type<BuildingClientboundPacket> type() {
        return TYPE;
    }
    public static final ResourceLocation EMPTY = ResourceLocation.fromNamespaceAndPath("", "");

    // pos is used to identify the building object serverside
    public BuildingAction action;
    public BlockPos buildingPos;
    public ResourceLocation itemKey;
    public String itemName;
    public Rotation rotation;
    public String ownerName;
    public int scenarioRoleIndex;
    public int blocksPlaced; // for syncing out-of-view clientside buildings
    public int numQueuedBlocks; // used for delaying destroy checks clientside
    public boolean isDiagonalBridge;
    public int upgradeLevel;
    public boolean isBuilt;
    public double partialBlocksDestroyed = 0;

    // send to every player that has the building loaded
    private static void sendFiltered(BlockPos buildingPos, BuildingClientboundPacket packet) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            PacketHandler.send(PacketHandler.toPlayer(() -> sp), packet);
        }
    }

    public static void placeBuilding(
            BlockPos buildingPos,
            Building building,
            Rotation rotation,
            String ownerName,
            int scenarioRoleIndex,
            int numQueuedBlocks,
            boolean isDiagonalBridge,
            int upgradeLevel,
            boolean isBuilt
    ) {
        sendFiltered(buildingPos, new BuildingClientboundPacket(
                building instanceof CustomBuilding ? BuildingAction.PLACE_CUSTOM
                        : building instanceof JsonBuilding ? BuildingAction.PLACE_JSON
                        : BuildingAction.PLACE,
                building instanceof CustomBuilding ? EMPTY
                        : building instanceof JsonBuilding jsonBuilding ? jsonBuilding.getDefinitionId()
                        : ReignOfNetherRegistries.BUILDING.getKey(building),
                building.name,
                buildingPos,
                rotation,
                ownerName,
                scenarioRoleIndex,
                0,
                numQueuedBlocks,
                isDiagonalBridge,
                upgradeLevel,
                isBuilt
        ));
    }

    public static void syncBuilding(BlockPos buildingPos, int blocksPlaced, double partialBlocksDestroyed, String ownerName, int scenarioRoleIndex) {
        BuildingClientboundPacket packet = new BuildingClientboundPacket(BuildingAction.SYNC_BLOCKS_AND_OWNER,
                EMPTY,
                "",
                buildingPos,
                Rotation.NONE,
                ownerName,
                scenarioRoleIndex,
                blocksPlaced,
                0,
                false,
                0,
                false
        );
        packet.partialBlocksDestroyed = partialBlocksDestroyed;
        sendFiltered(buildingPos, packet);
    }

    public static void changeStructure(BlockPos buildingPos, String structureName) {
        sendFiltered(buildingPos,
                new BuildingClientboundPacket(BuildingAction.CHANGE_STRUCTURE,
                        EMPTY,
                        structureName,
                        buildingPos,
                        Rotation.NONE,
                        "",
                        0,
                        0,
                        0,
                        false,
                        0,
                        false
                )
        );
    }

    public static void setUpgradeLevel(BlockPos buildingPos, int upgradeLevel) {
        sendFiltered(buildingPos,
                new BuildingClientboundPacket(BuildingAction.SET_UPGRADE_LEVEL,
                        EMPTY,
                        "",
                        buildingPos,
                        Rotation.NONE,
                        "",
                        0,
                        0,
                        0,
                        false,
                        upgradeLevel,
                        false
                )
        );
    }

    public static void removeBuilding(BlockPos buildingPos) {
        sendFiltered(buildingPos,
                new BuildingClientboundPacket(BuildingAction.REMOVE, EMPTY, "", buildingPos)
        );
    }

    public BuildingClientboundPacket(
            BuildingAction action,
            ResourceLocation itemKey,
            String itemName,
            BlockPos buildingPos
    ) {
        this.action = action;
        this.itemKey = itemKey;
        this.itemName = itemName;
        this.buildingPos = buildingPos;
        this.rotation = Rotation.NONE;
        this.ownerName = "";
        this.scenarioRoleIndex = 0;
        this.blocksPlaced = 0;
        this.numQueuedBlocks = 0;
        this.isDiagonalBridge = false;
        this.isBuilt = false;
        this.upgradeLevel = 0;
    }

    public BuildingClientboundPacket(
            BuildingAction action,
            ResourceLocation itemKey,
            String itemName,
            BlockPos buildingPos,
            Rotation rotation,
            String ownerName,
            int scenarioRoleIndex,
            int blocksPlaced,
            int numQueuedBlocks,
            boolean isDiagonalBridge,
            int upgradeLevel,
            boolean isBuilt
    ) {
        this.action = action;
        this.itemKey = itemKey;
        this.itemName = itemName;
        this.buildingPos = buildingPos;
        this.rotation = rotation;
        this.ownerName = ownerName;
        this.scenarioRoleIndex = scenarioRoleIndex;
        this.blocksPlaced = blocksPlaced;
        this.numQueuedBlocks = numQueuedBlocks;
        this.isDiagonalBridge = isDiagonalBridge;
        this.isBuilt = isBuilt;
        this.upgradeLevel = upgradeLevel;
    }

    public BuildingClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.action = buffer.readEnum(BuildingAction.class);
        this.itemKey = buffer.readResourceLocation();
        this.itemName = buffer.readUtf();
        this.buildingPos = buffer.readBlockPos();
        this.rotation = buffer.readEnum(Rotation.class);
        this.ownerName = buffer.readUtf();
        this.scenarioRoleIndex = buffer.readInt();
        this.blocksPlaced = buffer.readInt();
        this.numQueuedBlocks = buffer.readInt();
        this.isDiagonalBridge = buffer.readBoolean();
        this.isBuilt = buffer.readBoolean();
        this.upgradeLevel = buffer.readInt();
        this.partialBlocksDestroyed = buffer.readDouble();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeResourceLocation(this.itemKey);
        buffer.writeUtf(this.itemName);
        buffer.writeBlockPos(this.buildingPos);
        buffer.writeEnum(this.rotation);
        buffer.writeUtf(this.ownerName);
        buffer.writeInt(this.scenarioRoleIndex);
        buffer.writeInt(this.blocksPlaced);
        buffer.writeInt(this.numQueuedBlocks);
        buffer.writeBoolean(this.isDiagonalBridge);
        buffer.writeBoolean(this.isBuilt);
        buffer.writeInt(this.upgradeLevel);
        buffer.writeDouble(this.partialBlocksDestroyed);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {

        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                BuildingPlacement building = null;
                if (this.action != BuildingAction.PLACE &&
                        this.action != BuildingAction.PLACE_CUSTOM &&
                        this.action != BuildingAction.PLACE_JSON) {
                    building = findBuilding(true, this.buildingPos);
                    if (building == null) {

                        // if the client was missing a building, replace it
                        if (this.action == BuildingAction.SYNC_BLOCKS_AND_OWNER) {
                            BuildingServerboundPacket.requestReplacement(this.buildingPos);
                        }
                        return;
                    }
                }
                switch (action) {
                    case PLACE -> BuildingClientEvents.placeBuilding(
                            ReignOfNetherRegistries.BUILDING.get(this.itemKey),
                            this.buildingPos,
                            this.rotation,
                            this.ownerName,
                            this.numQueuedBlocks,
                            this.isDiagonalBridge,
                            this.upgradeLevel,
                            this.isBuilt
                    );
                    case PLACE_CUSTOM -> BuildingClientEvents.placeBuilding(
                            CustomBuildingClientEvents.getCustomBuilding(this.itemName),
                            this.buildingPos,
                            this.rotation,
                            this.ownerName,
                            this.numQueuedBlocks,
                            this.isDiagonalBridge,
                            this.upgradeLevel,
                            this.isBuilt
                    );
                    case PLACE_JSON -> BuildingClientEvents.placeBuilding(
                            JsonBuildingManager.getOrCreate(net.minecraft.client.Minecraft.getInstance().level, this.itemKey),
                            this.buildingPos,
                            this.rotation,
                            this.ownerName,
                            this.numQueuedBlocks,
                            this.isDiagonalBridge,
                            this.upgradeLevel,
                            this.isBuilt
                    );
                    case SYNC_BLOCKS_AND_OWNER -> {
                        BuildingClientEvents.syncBuilding(building, this.blocksPlaced, this.partialBlocksDestroyed, this.ownerName, this.scenarioRoleIndex);
                    }
                    case CHANGE_STRUCTURE -> {
                        building.changeStructure(itemName);
                    }
                    case SET_UPGRADE_LEVEL -> {
                        building.setUpgradeLevel(this.upgradeLevel);
                    }
                    case REMOVE -> {
                        BuildingClientEvents.removeBuilding(buildingPos);
                    }
                }
            });
        });
        return;
    }
}
