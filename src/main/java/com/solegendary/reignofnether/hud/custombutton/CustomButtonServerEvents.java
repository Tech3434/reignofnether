package com.solegendary.reignofnether.hud.custombutton;

import net.neoforged.fml.common.EventBusSubscriber;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.registrars.PacketHandler;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;

@EventBusSubscriber
public class CustomButtonServerEvents {
	
	
	public static final ResourceKey<Registry<CustomButton>> CUSTOM_BUTTON_REGISTRY_KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "rts_buttons"));
	
	public static final Map<ResourceLocation, CustomButton> customButtons = new HashMap<>();
	public static final Map<EntityType<?>, List<ResourceLocation>> entityMappings = new HashMap<>();
	public static final Map<ResourceLocation, List<ResourceLocation>> buildingMappings = new HashMap<>();
	public static final Map<ResourceLocation, List<ResourceLocation>> unitDefinitionMappings = new HashMap<>();
	public static final ArrayList<ResourceLocation> alwaysRenderButtons = new ArrayList<>();
//	public static final Map<ResourceLocation, CustomButton> customFrozenButtons = new HashMap<>();
	
	public static CustomButton getButton(ResourceLocation id) {
		return customButtons.get(id);
	}
	
	public static void registerButtons(CustomButtonMappingManager.MappingData data) {
		entityMappings.clear();
		buildingMappings.clear();
		unitDefinitionMappings.clear();
		alwaysRenderButtons.clear();
		
		Set<ResourceLocation> allocated = new HashSet<>(customButtons.size());
		Map<ResourceLocation, List<ResourceLocation>> entityMappings = new HashMap<>();
		Map<ResourceLocation, List<ResourceLocation>> buildingMappings = new HashMap<>();
		Map<ResourceLocation, List<ResourceLocation>> unitDefinitionMappings = new HashMap<>();
		
		for (Map.Entry<ResourceLocation, List<ResourceLocation>> entry : data.entities().entrySet()) {
			EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entry.getKey());
			if (entityType != null) {
				ArrayList<ResourceLocation> list = new ArrayList<>(entry.getValue());
				list.retainAll(customButtons.keySet());
				if (!list.isEmpty()) {
					CustomButtonServerEvents.entityMappings.put(entityType, list);
					allocated.addAll(list);
					entityMappings.put(entry.getKey(), list);
				}
			}
		}
		
		// buildings/units are keyed by their full id (code registry key or datapack definition id), so
		// data-driven content can attach custom buttons too; the client resolves them by the same id.
		for (Map.Entry<ResourceLocation, List<ResourceLocation>> entry : data.buildings().entrySet()) {
			ArrayList<ResourceLocation> list = new ArrayList<>(entry.getValue());
			list.retainAll(customButtons.keySet());
			if (!list.isEmpty()) {
				CustomButtonServerEvents.buildingMappings.put(entry.getKey(), list);
				allocated.addAll(list);
				buildingMappings.put(entry.getKey(), list);
			}
		}
		
		for (Map.Entry<ResourceLocation, List<ResourceLocation>> entry : data.unitDefinitions().entrySet()) {
			ArrayList<ResourceLocation> list = new ArrayList<>(entry.getValue());
			list.retainAll(customButtons.keySet());
			if (!list.isEmpty()) {
				CustomButtonServerEvents.unitDefinitionMappings.put(entry.getKey(), list);
				allocated.addAll(list);
				unitDefinitionMappings.put(entry.getKey(), list);
			}
		}
		
		for (ResourceLocation button : customButtons.keySet()) {
			if (!allocated.contains(button)) {
				alwaysRenderButtons.add(button);
			}
		}
		syncCustomButtons(entityMappings, buildingMappings, unitDefinitionMappings);
	}
	
	@SubscribeEvent
	public static void registerButtonMappings(ServerStartedEvent evt) {
		customButtons.clear();
		
		Registry<CustomButton> registry = evt.getServer().registryAccess().registryOrThrow(CustomButtonServerEvents.CUSTOM_BUTTON_REGISTRY_KEY);
		for (CustomButton button : registry) {
			button.id = registry.getKey(button);
			customButtons.put(button.id, button);
		}
	}

	@SubscribeEvent
	public static void onServerAboutToStart(PlayerEvent.PlayerLoggedInEvent evt) {
		MinecraftServer server = evt.getEntity().level().getServer();
		if (server != null) {
			CustomButtonMappingManager.registerMappings(server.getResourceManager());
		}
	}
	
	private static void syncCustomButtons(Map<ResourceLocation, List<ResourceLocation>> entityMappings,
										   Map<ResourceLocation, List<ResourceLocation>> buildingMappings,
										   Map<ResourceLocation, List<ResourceLocation>> unitDefinitionMappings) {
		PacketHandler.send(PacketHandler.allPlayers(), new CustomButtonClientboundPacket(
			(byte) 0,
			null,
			null,
			null,
			0,
			0,
			0,
			null,
			false,
			false,
			false,
			false
		));
		for (CustomButton button : customButtons.values()) {
			PacketHandler.send(PacketHandler.allPlayers(), new CustomButtonClientboundPacket(
				(byte) 1,
				button.id,
				button.name,
				button.iconResource,
				button.OffsetX,
				button.OffsetY,
				button.iconSize,
				null,
				!button.leftClickActions.isEmpty(),
				!button.rightClickActions.isEmpty(),
				button.lightUpOnHover,
				button.isEnabled
			));
		}
		PacketHandler.send(PacketHandler.allPlayers(), new CustomButtonClientboundPacket(
			(byte) 2,
			null,
			null,
			null,
			0,
			0,
			0,
			entityMappings,
			false,
			false,
			false,
			false
		));
		PacketHandler.send(PacketHandler.allPlayers(), new CustomButtonClientboundPacket(
			(byte) 3,
			null,
			null,
			null,
			0,
			0,
			0,
			buildingMappings,
			false,
			false,
			false,
			false
		));
		PacketHandler.send(PacketHandler.allPlayers(), new CustomButtonClientboundPacket(
			(byte) 4,
			null,
			null,
			null,
			0,
			0,
			0,
			Map.of(CustomButtonClientboundPacket.ALWAYS_KEY, alwaysRenderButtons),
			false,
			false,
			false,
			false
		));
		PacketHandler.send(PacketHandler.allPlayers(), new CustomButtonClientboundPacket(
			(byte) 5,
			null,
			null,
			null,
			0,
			0,
			0,
			unitDefinitionMappings,
			false,
			false,
			false,
			false
		));
	}
}
