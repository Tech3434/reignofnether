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
        evt.put(EntityRegistrar.VILLAGER_UNIT.get(), VillagerUnit.createAttributes().build());
        evt.put(EntityRegistrar.VINDICATOR_UNIT.get(), VindicatorUnit.createAttributes().build());
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
        if(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(event.getTab())==CreativeModeTabs.SPAWN_EGGS.location()){
            event.accept(ItemRegistrar.ZOMBIE_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.HUSK_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.DROWNED_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.ZOMBIE_PIGLIN_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.ZOGLIN_UNIT.get());
            event.accept(ItemRegistrar.SKELETON_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.STRAY_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.BOGGED_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.CREEPER_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.SPIDER_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.POISON_SPIDER_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.WRAITH_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.VILLAGER_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.MILITIA_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.ZOMBIE_VILLAGER_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.VINDICATOR_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.PILLAGER_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.WINDCALLER_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.IRON_GOLEM_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.WITCH_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.EVOKER_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.ENDERMAN_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.WARDEN_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.RAVAGER_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.SILVERFISH_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.GRUNT_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.BRUTE_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.HEADHUNTER_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.MARAUDER_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.HOGLIN_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.BLAZE_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.WITHER_SKELETON_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.GHAST_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.MAGMA_CUBE_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.SLIME_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.ROYAL_GUARD_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.NECROMANCER_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.PIGLIN_MERCHANT_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.WOLF_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.LLAMA_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.PANDA_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.GRIZZLY_BEAR_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.POLAR_BEAR_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.SCOUT_DOG_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.SCOUT_CAT_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.STRIDER_UNIT_SPAWN_EGG.get());
            event.accept(ItemRegistrar.BAT_UNIT_SPAWN_EGG.get());
        }
        if (BuiltInRegistries.CREATIVE_MODE_TAB.getKey(event.getTab())==CreativeModeTabs.TOOLS_AND_UTILITIES.location()){
            event.accept(ItemRegistrar.THROWABLE_TNT.get());
            event.accept(ItemRegistrar.THROWN_HERO_EXPERIENCE_BOTTLE.get());
            event.accept(ItemRegistrar.STAFF_OF_LIGHTNING.get());
        }
    }
}

