package com.solegendary.reignofnether.research;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client asks the server to start a research. The server validates that it is not already done, that
 * the prerequisites are met and that the owner can afford it, then pays for it and grants it. The
 * research cost's {@code ticks} (duration) is not enforced yet - grants are immediate.
 */
public class ResearchServerboundPacket implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<ResearchServerboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "research_serverbound"));

    @Override
    public CustomPacketPayload.Type<ResearchServerboundPacket> type() {
        return TYPE;
    }

    private final ResourceLocation researchId;

    public static void request(ResourceLocation researchId) {
        PacketHandler.sendToServer(new ResearchServerboundPacket(researchId));
    }

    public ResearchServerboundPacket(ResourceLocation researchId) {
        this.researchId = researchId;
    }

    public ResearchServerboundPacket(RegistryFriendlyByteBuf buffer) {
        this.researchId = buffer.readResourceLocation();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeResourceLocation(this.researchId);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player) || player.level().isClientSide())
                return;

            Research research = ResearchRegistry.get(this.researchId);
            if (research == null)
                return;

            String ownerName = player.getName().getString();
            ServerLevel level = player.serverLevel();
            ResearchSaveData data = ResearchSaveData.getInstance(level);

            if (data.hasResearch(ownerName, this.researchId))
                return;
            if (!ResearchUtils.meets(level, ownerName, research.getPrerequisites())) {
                player.sendSystemMessage(Component.translatable("server.reignofnether.research_prereq"));
                return;
            }

            ResourceCost cost = research.getCost();
            if (cost != null) {
                if (!ResourcesServerEvents.canAfford(ownerName, ResourceName.FOOD, cost.food)
                        || !ResourcesServerEvents.canAfford(ownerName, ResourceName.WOOD, cost.wood)
                        || !ResourcesServerEvents.canAfford(ownerName, ResourceName.ORE, cost.ore)
                        || !ResourcesServerEvents.canAfford(ownerName, ResourceName.EMERALD, cost.emerald)) {
                    player.sendSystemMessage(Component.translatable("server.reignofnether.research_cant_afford"));
                    return;
                }
                ResourcesServerEvents.addSubtractResources(
                        new Resources(ownerName, -cost.food, -cost.wood, -cost.ore, -cost.emerald));
            }

            data.grant(ownerName, this.researchId);
            data.save();
            ResearchClientboundPacket.sync(ownerName, data.getFor(ownerName));
            ResearchAttributeApplier.refreshForOwner(level, ownerName);
        });
    }
}
