package com.solegendary.reignofnether;

import net.neoforged.fml.common.EventBusSubscriber;
import com.solegendary.reignofnether.blocks.InvisibleBlockRenderer;
import com.solegendary.reignofnether.blocks.SkullTypes;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;

import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.particles.*;
import com.solegendary.reignofnether.registrars.*;
import com.solegendary.reignofnether.unit.modelling.models.IllagerArmorModel;
import com.solegendary.reignofnether.unit.modelling.models.VillagerUnitModel;
import com.solegendary.reignofnether.unit.modelling.renderers.AbstractVillagerUnitRenderer;
import com.solegendary.reignofnether.unit.modelling.renderers.VillagerUnitRenderer;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.HashSet;
import net.minecraft.core.registries.BuiltInRegistries;

    @EventBusSubscriber(modid = ReignOfNether.MOD_ID, value = Dist.CLIENT)
public class ClientModEvents {

    // Only the mod's own blocks get a forced colour. Nothing wraps vanilla providers any more:
    // fog of war needed every block and every baked model wrapped so it could tint them, and with
    // fog gone that was pure per-frame overhead on the whole game.
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onBlockColourEvent(RegisterColorHandlersEvent.Block evt) {
        evt.register((bs, blockAndTintGetter, bp, tintIndex) -> 0xFFFFFF, Blocks.NETHER_PORTAL);

        evt.register(
                (state, level, pos, tintIndex) -> 0xE0E0E0,
                BlockRegistrar.WRAITH_SNOW_LAYER.get()
        );
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers evt) {
        evt.registerEntityRenderer(EntityRegistrar.VILLAGER_UNIT.get(), VillagerUnitRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.VINDICATOR_UNIT.get(), VillagerUnitRenderer::new);

    }

    /**
     * Enchantments are a datapack registry, and on the client the registry only exists once a level
     * with datapacks has been joined - so this is where the mod's enchantment holders start working.
     */
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void bindEnchantmentRegistryOnLogin(ClientPlayerNetworkEvent.LoggingIn evt) {
        EnchantmentRegistrar.bind(evt.getPlayer().level().registryAccess());
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void registerMenuScreens(RegisterMenuScreensEvent evt) {
        // 1.21.1 made MenuScreens#register private; NeoForge exposes the map through this event.
        evt.register(ContainerRegistrar.TOPDOWNGUI_CONTAINER.get(), TopdownGui::new);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onClientSetupEvent(FMLClientSetupEvent evt) {
        evt.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(
                    BlockRegistrar.UNEXTINGUISHABLE_SOUL_FIRE.get(),
                    RenderType.cutout()
            );
        });
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers evt) {
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(VillagerUnitModel.LAYER_LOCATION, VillagerUnitModel::createBodyLayer);
        event.registerLayerDefinition(AbstractVillagerUnitRenderer.VILLAGER_ARMOR_OUTER_LAYER, IllagerArmorModel::createOuterArmorLayer);
        event.registerLayerDefinition(AbstractVillagerUnitRenderer.VILLAGER_ARMOR_INNER_LAYER, IllagerArmorModel::createInnerArmorLayer);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void registerParticles(RegisterParticleProvidersEvent evt) {
        evt.registerSpriteSet(
                ParticleRegistrar.BIG_SOUL_FLAME.get(),
                BigSoulFlameParticle.Provider::new
        );
        evt.registerSpriteSet(
                ParticleRegistrar.LEVEL_UP.get(),
                LevelUpParticle.Provider::new
        );
        evt.registerSpriteSet(
                ParticleRegistrar.FLOATING_CRIT.get(),
                AbstractFloatingParticle.Provider::new
        );
        evt.registerSpriteSet(
                ParticleRegistrar.FLOATING_HEART.get(),
                FloatingHeartParticle.Provider::new
        );
        evt.registerSpriteSet(
                ParticleRegistrar.MANA.get(),
                ManaParticle.Provider::new
        );
        evt.registerSpriteSet(
                ParticleRegistrar.BIG_VIBRATION.get(),
                BigVibrationParticle.Provider::new
        );
    }
}

