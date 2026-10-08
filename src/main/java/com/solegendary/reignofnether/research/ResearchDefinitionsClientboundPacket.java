package com.solegendary.reignofnether.research;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.util.DistHelper;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** Sends every research definition (name/icon/prerequisites) to clients for the HUD research panel. */
public class ResearchDefinitionsClientboundPacket implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<ResearchDefinitionsClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "research_definitions_clientbound"));

    @Override
    public CustomPacketPayload.Type<ResearchDefinitionsClientboundPacket> type() {
        return TYPE;
    }

    private final List<ResearchClientEvents.Def> definitions;

    public static void sync(List<ResearchClientEvents.Def> definitions) {
        PacketHandler.send(PacketHandler.allPlayers(), new ResearchDefinitionsClientboundPacket(definitions));
    }

    public ResearchDefinitionsClientboundPacket(List<ResearchClientEvents.Def> definitions) {
        this.definitions = List.copyOf(definitions);
    }

    public ResearchDefinitionsClientboundPacket(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<ResearchClientEvents.Def> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buffer.readResourceLocation();
            String nameKey = buffer.readUtf();
            String iconStr = buffer.readUtf();
            ResourceLocation icon = iconStr.isEmpty() ? null : ResourceLocation.tryParse(iconStr);
            int prereqCount = buffer.readVarInt();
            List<ResearchCondition> prereqs = new ArrayList<>(prereqCount);
            for (int p = 0; p < prereqCount; p++) {
                ResourceLocation pid = buffer.readResourceLocation();
                boolean invert = buffer.readBoolean();
                prereqs.add(new ResearchCondition(pid, invert));
            }
            list.add(new ResearchClientEvents.Def(id, nameKey, icon, prereqs));
        }
        this.definitions = list;
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(this.definitions.size());
        for (ResearchClientEvents.Def def : this.definitions) {
            buffer.writeResourceLocation(def.id());
            buffer.writeUtf(def.nameKey() == null ? "" : def.nameKey());
            buffer.writeUtf(def.icon() == null ? "" : def.icon().toString());
            buffer.writeVarInt(def.prerequisites().size());
            for (ResearchCondition prereq : def.prerequisites()) {
                buffer.writeResourceLocation(prereq.researchId());
                buffer.writeBoolean(prereq.invert());
            }
        }
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ResearchClientEvents.setDefinitions(this.definitions)));
    }
}
