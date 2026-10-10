package com.solegendary.reignofnether.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.solegendary.reignofnether.building.ProductionSpec;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.StartingUnit;
import com.solegendary.reignofnether.research.ResearchCondition;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Rejects fields a definition codec does not know about.
 *
 * <p>The codecs behind {@code unit}/{@code building}/{@code faction} JSON are {@code RecordCodecBuilder}
 * codecs, which are lenient: a field they do not recognize is dropped without a word, so a typo
 * ({@code "carryCapcity"}) or a leftover from an older schema ({@code "carryCapacity"} on the unit root)
 * silently changes nothing. This validator walks the raw JSON against the record that will decode it and
 * reports every unrecognized field with its path inside the file.
 *
 * <p>The schema is taken from the records themselves (component names are the codec field names), with
 * {@link #JSON_FIELD_NAMES} covering the few fields whose JSON name differs from the component name.
 * That means adding a field to a definition record automatically extends the accepted schema; renaming
 * only the codec field (not the component) would have to be mirrored in {@link #JSON_FIELD_NAMES}.
 *
 * <p>Only the field <em>names</em> are checked — value types, ranges and required fields stay the codec's
 * job. Free-form maps ({@code attributes}, {@code params}, localized {@code name}) accept any keys.
 */
public final class ContentValidator {

    /** Record component name → JSON field name, only where the two differ. */
    private static final Map<Class<?>, Map<String, String>> JSON_FIELD_NAMES = Map.of(
            Faction.class, Map.of(
                    "nameKey", "name",
                    "startingUnits", "starting_units",
                    "startingFood", "food",
                    "startingWood", "wood",
                    "startingOre", "ore",
                    "startingEmerald", "emerald"),
            StartingUnit.class, Map.of("entityType", "unit"),
            ResearchCondition.class, Map.of("researchId", "research")
    );

    /** Records whose entry may also be written as a plain id string instead of an object. */
    private static final Set<Class<?>> STRING_OR_OBJECT = Set.of(ProductionSpec.class);

    private static final int MAX_DEPTH = 32;

    private ContentValidator() { }

    /**
     * Checks one definition file's JSON against the record that decodes it.
     *
     * @param definitionType the record the codec builds ({@code UnitDefinition}, {@code BuildingDefinition}, …)
     * @param json           the parsed file content
     * @return one human-readable message per problem (with the path inside the file), empty when clean
     */
    public static List<String> unknownFields(Class<?> definitionType, JsonElement json) {
        List<String> problems = new ArrayList<>();
        validate(definitionType, json, "", problems, 0);
        return problems;
    }

    private static void validate(Type type, JsonElement json, String path, List<String> problems, int depth) {
        if (json == null || json.isJsonNull() || depth > MAX_DEPTH)
            return;

        Class<?> raw = rawClass(type);

        if (raw == Optional.class && type instanceof ParameterizedType parameterized) {
            validate(parameterized.getActualTypeArguments()[0], json, path, problems, depth + 1);
            return;
        }
        if (raw == List.class && type instanceof ParameterizedType parameterized) {
            if (!json.isJsonArray()) {
                problems.add(describe(path) + "must be an array");
                return;
            }
            Type element = parameterized.getActualTypeArguments()[0];
            JsonArray array = json.getAsJsonArray();
            for (int i = 0; i < array.size(); i++)
                validate(element, array.get(i), path + "[" + i + "]", problems, depth + 1);
            return;
        }
        if (raw == Map.class) {
            if (!json.isJsonObject())
                problems.add(describe(path) + "must be an object");
            return; // free-form: attributes / params / localized name
        }
        if (raw != null && raw.isRecord()) {
            if (STRING_OR_OBJECT.contains(raw) && json.isJsonPrimitive())
                return; // the codec accepts the plain-id form here
            validateRecord(raw, json, path, problems, depth);
        }
        // anything else (resource locations, enums, numbers, booleans) is a leaf the codec already checks
    }

    private static void validateRecord(Class<?> recordType, JsonElement json, String path,
                                       List<String> problems, int depth) {
        if (!json.isJsonObject()) {
            problems.add(describe(path) + "must be an object");
            return;
        }
        JsonObject object = json.getAsJsonObject();
        Map<String, String> renames = JSON_FIELD_NAMES.getOrDefault(recordType, Map.of());

        // JSON field name -> component, so we can both reject unknown keys and recurse into known ones
        Map<String, RecordComponent> byJsonName = new LinkedHashMap<>();
        for (RecordComponent component : recordType.getRecordComponents())
            byJsonName.put(renames.getOrDefault(component.getName(), component.getName()), component);

        for (String key : object.keySet())
            if (!byJsonName.containsKey(key))
                problems.add(describe(path) + "unknown field '" + key + "' (accepted: "
                        + String.join(", ", new TreeSet<>(byJsonName.keySet())) + ")");

        for (Map.Entry<String, RecordComponent> entry : byJsonName.entrySet()) {
            JsonElement value = object.get(entry.getKey());
            if (value == null)
                continue;
            validate(entry.getValue().getGenericType(), value,
                    path.isEmpty() ? entry.getKey() : path + "." + entry.getKey(), problems, depth + 1);
        }
    }

    private static String describe(String path) {
        return path.isEmpty() ? "<root>: " : "'" + path + "': ";
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> clazz)
            return clazz;
        if (type instanceof ParameterizedType parameterized && parameterized.getRawType() instanceof Class<?> clazz)
            return clazz;
        return null;
    }
}
