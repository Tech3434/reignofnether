package com.solegendary.reignofnether;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.solegendary.reignofnether.ability.AbilitySpec;
import com.solegendary.reignofnether.building.BuildingDefinition;
import com.solegendary.reignofnether.data.ContentValidationReloadListener;
import com.solegendary.reignofnether.data.ContentValidator;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.unit.UnitDefinition;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the strict definition validation: every shipped {@code unit}/{@code building}/{@code faction}
 * JSON must be free of unknown fields, and a typo must be reported with its path instead of being
 * dropped silently. This is the gate for {@link ContentValidator}; at runtime the same check runs in
 * {@link ContentValidationReloadListener}.
 */
class ContentValidationTest {

    @Test
    void shippedDefinitionsHaveNoUnknownFields() throws IOException, URISyntaxException {
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, Class<?>> folder : ContentValidationReloadListener.checkedFolders().entrySet()) {
            for (Path file : jsonFiles("/data/reignofnether/" + folder.getKey())) {
                try (Reader reader = Files.newBufferedReader(file)) {
                    JsonElement json = JsonParser.parseReader(reader);
                    for (String problem : ContentValidator.unknownFields(folder.getValue(), json))
                        problems.add(folder.getKey() + "/" + file.getFileName() + " " + problem);
                }
            }
        }
        assertTrue(problems.isEmpty(),
                "shipped definitions contain unknown fields:\n" + String.join("\n", problems));
    }

    /** A field the codec does not know must be named, along with the fields it does accept. */
    @Test
    void unknownTopLevelFieldIsReported() {
        List<String> problems = check(UnitDefinition.class,
                "{ \"base\": \"minecraft:villager\", \"role\": \"worker\", \"carryCapacity\": 100 }");

        assertFalse(problems.isEmpty(), "a top-level unknown field was accepted");
        assertTrue(problems.get(0).startsWith("<root>: unknown field 'carryCapacity'"), problems.toString());
        assertTrue(problems.get(0).contains("worker"), "the accepted list should be shown: " + problems);
    }

    @Test
    void unknownNestedFieldIsReported() {
        List<String> problems = check(BuildingDefinition.class,
                "{ \"structure\": \"reignofnether:barracks\", \"flags\": { \"foundationYLayer\": 2 } }");

        assertTrue(problems.stream().anyMatch(p -> p.startsWith("'flags': unknown field 'foundationYLayer'")),
                problems.toString());
        assertTrue(problems.get(0).contains("foundationYLayers"), "should suggest the right field: " + problems);
    }

    @Test
    void unknownFieldInsideListElementIsReported() {
        List<String> problems = check(BuildingDefinition.class,
                "{ \"structure\": \"reignofnether:barracks\", \"production\": ["
                        + "\"reignofnether:villager_unit\","
                        + "{ \"unit\": \"reignofnether:skeleton_unit\", \"costOveride\": { \"food\": 5 } } ] }");

        assertTrue(problems.stream().anyMatch(p -> p.startsWith("'production[1]': unknown field 'costOveride'")),
                problems.toString());
    }

    @Test
    void unknownFieldInsideAbilityOrMenuIsReported() {
        assertTrue(check(AbilitySpec.class, "{ \"type\": \"reignofnether:heal\", \"coolDown\": 20 }")
                        .stream().anyMatch(p -> p.contains("'coolDown'")),
                "a typo in a common ability field was accepted");

        assertTrue(check(UnitDefinition.class,
                        "{ \"base\": \"minecraft:skeleton\", \"abilities\": [ { \"type\": \"reignofnether:menu\","
                                + " \"submenu\": [ { \"command\": \"stop\", \"rows\": 1 } ] } ] }")
                        .stream().anyMatch(p -> p.startsWith("'abilities[0].submenu[0]': unknown field 'rows'")),
                "a typo inside a menu entry was accepted");
    }

    /** attributes / params / localized names are free-form maps: any key is legitimate there. */
    @Test
    void freeFormKeyValueMapsAreAccepted() {
        List<String> problems = check(UnitDefinition.class,
                "{ \"base\": \"minecraft:villager\", \"role\": \"worker\","
                        + " \"name\": { \"en_us\": \"Villager\", \"xx_yy\": \"Villager\" },"
                        + " \"attributes\": { \"minecraft:generic.max_health\": 25, \"myns:some_attribute\": 1 },"
                        + " \"abilities\": [ { \"type\": \"reignofnether:heal\","
                        + " \"params\": { \"amount\": 15, \"sound\": \"minecraft:entity.player.levelup\" } } ] }");

        assertTrue(problems.isEmpty(), "free-form maps were reported: " + problems);
    }

    @Test
    void factionJsonUsesItsDocumentedFieldNames() {
        String valid = "{ \"name\": \"faction.reignofnether.villagers\","
                + " \"capitol\": \"reignofnether:town_centre\","
                + " \"starting_units\": [ { \"unit\": \"reignofnether:villager_unit\", \"count\": 1 } ],"
                + " \"food\": 0, \"wood\": 0, \"ore\": 0, \"emerald\": 0 }";
        assertTrue(check(Faction.class, valid).isEmpty(), "a valid faction was rejected");

        // the Java component names are not the JSON field names - a file using them is a real error
        List<String> problems = check(Faction.class, "{ \"name\": \"x\", \"capitol\": \"a:b\", \"startingFood\": 5 }");
        assertTrue(problems.stream().anyMatch(p -> p.contains("'startingFood'")), problems.toString());
    }

    /** A production entry is either a plain id or an object, and both forms are checked. */
    @Test
    void productionEntryAcceptsBothForms() {
        assertTrue(check(BuildingDefinition.class,
                        "{ \"structure\": \"reignofnether:barracks\", \"production\": ["
                                + "\"reignofnether:villager_unit\","
                                + "{ \"unit\": \"reignofnether:skeleton_unit\" },"
                                + "{ \"unit\": \"reignofnether:skeleton_unit\", \"costOverride\": { \"food\": 55 } } ] }")
                        .isEmpty(),
                "a valid production list was rejected");
    }

    @Test
    void wrongJsonShapeIsReported() {
        assertTrue(check(UnitDefinition.class, "{ \"base\": \"minecraft:villager\", \"flags\": 5 }")
                        .stream().anyMatch(p -> p.startsWith("'flags':") && p.contains("must be an object")),
                "a scalar where an object is expected was accepted");
        assertTrue(check(BuildingDefinition.class, "{ \"structure\": \"reignofnether:barracks\", \"production\": 5 }")
                        .stream().anyMatch(p -> p.startsWith("'production':") && p.contains("must be an array")),
                "a scalar where an array is expected was accepted");
    }

    private static List<String> check(Class<?> definitionType, String json) {
        return ContentValidator.unknownFields(definitionType, JsonParser.parseString(json));
    }

    private static List<Path> jsonFiles(String classpathDir) throws IOException, URISyntaxException {
        Path dir = Path.of(ContentValidationTest.class.getResource(classpathDir).toURI());
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
    }
}
