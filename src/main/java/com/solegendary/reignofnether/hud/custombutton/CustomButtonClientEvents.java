package com.solegendary.reignofnether.hud.custombutton;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.Building;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;

public class CustomButtonClientEvents {
	
	public static final Map<ResourceLocation, CustomButton> customButtons = new HashMap<>();
	public static final Map<EntityType<?>, ArrayList<ResourceLocation>> entityMappings = new HashMap<>();
	public static final Map<ResourceLocation, ArrayList<ResourceLocation>> buildingMappings = new HashMap<>();
	public static final Map<ResourceLocation, ArrayList<ResourceLocation>> unitDefinitionMappings = new HashMap<>();
	public static final ArrayList<ResourceLocation> alwaysRenderButtons = new ArrayList<>();
//	public static final Map<ResourceLocation, CustomButton> customFrozenButtons = new HashMap<>();
	
	public static CustomButton getButton(ResourceLocation id) {
		return customButtons.get(id);
	}
	
	public static void clear() {
		customButtons.clear();
		alwaysRenderButtons.clear();
		entityMappings.clear();
		buildingMappings.clear();
		unitDefinitionMappings.clear();
	}
	
	
	public static void registerButtons(ResourceLocation id, CustomButton button) {
		customButtons.put(id, button);
	}
	
	public static void registerEntityMappings(Map<ResourceLocation, List<ResourceLocation>> buttons) {
		for (ResourceLocation id : buttons.keySet()) {
			entityMappings.put(BuiltInRegistries.ENTITY_TYPE.get(id), new ArrayList<>(buttons.get(id)));
		}
	}
	
	/** Keyed by full building id: code registry key or datapack definition id. */
	public static void registerBuildingMappings(Map<ResourceLocation, List<ResourceLocation>> buttons) {
		for (ResourceLocation building : buttons.keySet()) {
			buildingMappings.put(building, new ArrayList<>(buttons.get(building)));
		}
	}
	
	public static void registerUnitDefinitionMappings(Map<ResourceLocation, List<ResourceLocation>> buttons) {
		for (ResourceLocation unitDefinition : buttons.keySet()) {
			unitDefinitionMappings.put(unitDefinition, new ArrayList<>(buttons.get(unitDefinition)));
		}
	}
	
	public static void registerAlwaysRenderButtons(List<ResourceLocation> buttons) {
		alwaysRenderButtons.addAll(buttons);
	}
}
