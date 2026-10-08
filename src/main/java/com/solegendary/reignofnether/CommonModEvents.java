package com.solegendary.reignofnether;

import net.neoforged.fml.common.EventBusSubscriber;
import com.solegendary.reignofnether.registrars.BlockRegistrar;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import com.solegendary.reignofnether.registrars.PacketHandler;

import com.solegendary.reignofnether.unit.units.villagers.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import com.solegendary.reignofnether.registrars.EnchantmentRegistrar;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.SubscribeEvent;

@EventBusSubscriber(modid = ReignOfNether.MOD_ID)
public class CommonModEvents {

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PacketHandler.registerPayloads(event);
    }

    /**
     * Enchantments are a datapack registry in 1.21.1, so they have no {@code BuiltInRegistries}
     * field and {@code ModifyRegistriesEvent} fires before the registry object even exists.
     * The first point where it does is the server's own {@code RegistryAccess}, which is also
     * what the holders are looked up from on demand.
     */
    @SubscribeEvent
    public static void bindEnchantmentRegistryOnServer(ServerStartingEvent event) {
        EnchantmentRegistrar.bind(event.getServer().registryAccess());
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent evt) {
        // no mod content entities - units are data-driven over vanilla base mobs
    }

    @SubscribeEvent
    public static void creativeTabSetup(BuildCreativeModeTabContentsEvent event) {
        if(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(event.getTab())==CreativeModeTabs.BUILDING_BLOCKS.location()){
            for(Item item : BlockRegistrar.blockItems.get(CreativeModeTabs.BUILDING_BLOCKS)){
                event.accept(item);
            }
        }
        if(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(event.getTab())==CreativeModeTabs.FUNCTIONAL_BLOCKS.location()){
            for(Item item : BlockRegistrar.blockItems.get(CreativeModeTabs.FUNCTIONAL_BLOCKS)){
                event.accept(item);
            }
        }
        if(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(event.getTab())==CreativeModeTabs.TOOLS_AND_UTILITIES.location()){
            event.accept(ItemRegistrar.THROWABLE_TNT.get());
        }
    }
}

