package com.solegendary.reignofnether;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.solegendary.reignofnether.ability.AbilitySpec;
import com.solegendary.reignofnether.building.BuildingDefinition;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.unit.UnitDefinition;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Decodes every shipped datapack JSON against the codec that will read it at runtime. A codec or a
 * schema change that would otherwise only surface as a server startup warning (a skipped definition)
 * or a crash is caught here instead. This is the closest thing to a runtime gate that does not need a
 * game client.
 */
class DataCodecTest {

    @Test
    void unitDefinitionsDecode() throws IOException, URISyntaxException {
        decodeAll("unit", UnitDefinition.CODEC);
    }

    @Test
    void buildingDefinitionsDecode() throws IOException, URISyntaxException {
        decodeAll("building", BuildingDefinition.CODEC);
    }

    @Test
    void factionDecode() throws IOException, URISyntaxException {
        decodeAll("faction", Faction.CODEC);
    }

    /** Guard rails so the codec cannot silently drop the K4 worker block or the K6 menu entries. */
    @Test
    void workerBlockAndMenuDecode() throws IOException, URISyntaxException {
        UnitDefinition villager = decode("unit/villager_unit.json", UnitDefinition.CODEC);
        assertTrue(villager.worker().isPresent(), "villager_unit.json worker block did not decode");
        assertEquals(100, villager.worker().get().carryCapacity());
        assertEquals(List.of(ResourceName.FOOD, ResourceName.WOOD, ResourceName.ORE),
                villager.gatherableOrDefault());

        UnitDefinition skeleton = decode("unit/skeleton_unit.json", UnitDefinition.CODEC);
        AbilitySpec menu = skeleton.abilitiesOrDefault().stream()
                .filter(a -> a.submenu().size() > 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("skeleton_unit.json menu ability did not decode"));
        assertEquals(4, menu.submenu().size(), "menu entries");
        assertTrue(menu.submenu().get(1).command().isPresent(), "command entry did not decode");
        assertTrue(menu.submenu().get(2).row().isPresent(), "row did not decode");
        // nested menu-in-menu
        assertTrue(menu.submenu().get(3).ability().isPresent()
                        && !menu.submenu().get(3).ability().get().submenu().isEmpty(),
                "nested menu did not decode");
    }

    /** The menu codec must accept a building entry with an explicit position. */
    @Test
    void menuBuildingEntryDecodes() {
        String json = "{ \"type\": \"reignofnether:menu\", \"submenu\": ["
                + "{ \"building\": \"reignofnether:barracks\", \"row\": 1, \"col\": 0 },"
                + "{ \"command\": \"stop\" } ] }";
        AbilitySpec spec = AbilitySpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(msg -> new AssertionError(msg));
        assertEquals(2, spec.submenu().size());
        assertEquals("reignofnether:barracks", spec.submenu().get(0).building().orElseThrow().toString());
        assertEquals(1, spec.submenu().get(0).row().orElseThrow());
        assertTrue(spec.submenu().get(1).command().isPresent());
    }

    /** The shipped demo must keep an object-form production entry: the runClient checklist relies on it. */
    @Test
    void productionCostOverrideDecodes() throws IOException, URISyntaxException {
        BuildingDefinition barracks = decode("building/barracks.json", BuildingDefinition.CODEC);
        assertTrue(barracks.production().stream()
                        .anyMatch(p -> p.unit().getPath().equals("skeleton_marksman")
                                && p.costOverride().isPresent()),
                "barracks.json lost its costOverride production entry");
    }

    private static <T> void decodeAll(String folder, Codec<T> codec) throws IOException, URISyntaxException {
        Path dir = Path.of(DataCodecTest.class.getResource("/data/reignofnether/" + folder).toURI());
        List<String> failures = new ArrayList<>();
        List<Path> files;
        try (Stream<Path> stream = Files.list(dir)) {
            files = stream.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
        assertTrue(!files.isEmpty(), "no JSON files found in " + dir);

        for (Path file : files) {
            try (Reader reader = Files.newBufferedReader(file)) {
                JsonElement json = JsonParser.parseReader(reader);
                DataResult<T> result = codec.parse(JsonOps.INSTANCE, json);
                result.error().ifPresent(err ->
                        failures.add(file.getFileName() + ": " + err.message()));
            }
        }
        assertTrue(failures.isEmpty(), "datapack JSON failed to decode:\n" + String.join("\n", failures));
    }

    private static <T> T decode(String relativePath, Codec<T> codec) throws IOException, URISyntaxException {
        Path file = Path.of(DataCodecTest.class.getResource("/data/reignofnether/" + relativePath).toURI());
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonElement json = JsonParser.parseReader(reader);
            return codec.parse(JsonOps.INSTANCE, json)
                    .getOrThrow(msg -> new AssertionError(relativePath + ": " + msg));
        }
    }
}
