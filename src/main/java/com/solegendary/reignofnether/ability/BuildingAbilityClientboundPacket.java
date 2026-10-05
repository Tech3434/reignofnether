package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class BuildingAbilityClientboundPacket implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<BuildingAbilityClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "building_ability_clientbound"));

    @Override
    public CustomPacketPayload.Type<BuildingAbilityClientboundPacket> type() {
        return TYPE;
    }

    UnitAction abilityAction;
    BlockPos buildingPos;

    public BuildingAbilityClientboundPacket(UnitAction abilityAction, BlockPos buildingPos) {
        this.abilityAction = abilityAction;
        this.buildingPos = buildingPos;
    }

    public BuildingAbilityClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.abilityAction = buffer.readEnum(UnitAction.class);
        this.buildingPos = buffer.readBlockPos();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(abilityAction);
        buffer.writeBlockPos(buildingPos);
    }

    public static void doAbility(UnitAction abilityAction, BlockPos buildingPos) {
        BuildingPlacement building = BuildingUtils.findBuilding(true, buildingPos);
        if (building == null)
            return;

        for (Ability abl : building.getAbilities())
            if (abl.action == abilityAction)
                BuildingAbilityServerboundPacket.toggleAutoCast(building, abl);
    }

    // client-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> doAbility(abilityAction, buildingPos));
    }
}