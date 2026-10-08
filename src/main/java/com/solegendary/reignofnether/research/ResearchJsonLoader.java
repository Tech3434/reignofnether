package com.solegendary.reignofnether.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.resources.ResourceCost;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import org.jetbrains.annotations.NotNull;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads research definitions from datapacks.
 *
 * <p>Files live at {@code data/<namespace>/research/<name>.json}; the research id is
 * {@code <namespace>:<name>}. The file is reloaded on every datapack reload (and thus also every
 * /reload), replacing whatever the registry held before.
 *
 * <p>Schema:
 * <pre>
 * {
 *   "name": "research.reignofnether.example",      // translation key shown in the HUD
 *   "icon": "reignofnether:textures/icons/items/shovel.png",
 *   "type": "unlock" | "attribute_boost",           // optional, defaults to unlock
 *   "cost": { "food": 0, "wood": 100, "ore": 0, "seconds": 20 }, // optional = free
 *   "prerequisites": [ { "research": "reignofnether:other", "invert": false } ],
 *   "attributes": [ { "attribute": "minecraft:generic.attack_damage", "amount": 1.0,
 *                     "unit": "reignofnether:villager_unit" } ] // "unit" optional
 * }
 * </pre>
 */
public class ResearchJsonLoader implements ResourceManagerReloadListener {

    private static final String DIR = "research";

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        ResearchRegistry.clear();

        Map<ResourceLocation, Resource> files = resourceManager.listResources(DIR,
                rl -> rl.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : files.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                ResourceLocation id = idFromFile(fileId);
                ResearchRegistry.register(parse(id, json));
            } catch (Exception e) {
                ReignOfNether.LOGGER.error("Failed to load research file {}: {}", fileId, e.toString());
            }
        }
        ReignOfNether.LOGGER.info("Loaded {} research definition(s)", ResearchRegistry.all().size());
    }

    private static ResourceLocation idFromFile(ResourceLocation fileId) {
        String path = fileId.getPath();
        String name = path.substring(DIR.length() + 1, path.length() - ".json".length());
        return ResourceLocation.fromNamespaceAndPath(fileId.getNamespace(), name);
    }

    private static Research parse(ResourceLocation id, JsonObject json) {
        String nameKey = json.has("name") ? json.get("name").getAsString() : id.toString();
        ResourceLocation icon = json.has("icon")
                ? ResourceLocation.parse(json.get("icon").getAsString())
                : null;
        ResearchType type = json.has("type") && json.get("type").getAsString().equalsIgnoreCase("attribute_boost")
                ? ResearchType.ATTRIBUTE_BOOST
                : ResearchType.UNLOCK;

        ResourceCost cost = null;
        if (json.has("cost")) {
            JsonObject c = json.getAsJsonObject("cost");
            int food = c.has("food") ? c.get("food").getAsInt() : 0;
            int wood = c.has("wood") ? c.get("wood").getAsInt() : 0;
            int ore = c.has("ore") ? c.get("ore").getAsInt() : 0;
            int seconds = c.has("seconds") ? c.get("seconds").getAsInt() : 0;
            cost = ResourceCost.Research(food, wood, ore, seconds);
        }

        List<ResearchCondition> prerequisites = new ArrayList<>();
        if (json.has("prerequisites")) {
            JsonArray arr = json.getAsJsonArray("prerequisites");
            for (JsonElement el : arr) {
                JsonObject p = el.getAsJsonObject();
                boolean invert = p.has("invert") && p.get("invert").getAsBoolean();
                prerequisites.add(new ResearchCondition(
                        ResourceLocation.parse(p.get("research").getAsString()), invert));
            }
        }

        List<ResearchAttributeModifier> attributes = new ArrayList<>();
        if (json.has("attributes")) {
            JsonArray arr = json.getAsJsonArray("attributes");
            for (JsonElement el : arr) {
                JsonObject a = el.getAsJsonObject();
                ResourceLocation attributeId = ResourceLocation.parse(a.get("attribute").getAsString());
                double amount = a.get("amount").getAsDouble();
                ResourceLocation unit = a.has("unit") ? ResourceLocation.parse(a.get("unit").getAsString()) : null;
                attributes.add(new ResearchAttributeModifier(attributeId, amount, unit));
            }
        }

        return new Research(id, nameKey, icon, cost, prerequisites, attributes, type);
    }
}
