package com.solegendary.reignofnether.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingDefinition;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.unit.UnitDefinition;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import org.jetbrains.annotations.NotNull;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Runs {@link ContentValidator} over every datapack definition file and reports the unknown fields in the
 * log, with the file and the field path:
 *
 * <pre>
 * [content-validation] reignofnether:unit/villager_unit.json 'worker': unknown field 'carryCapcity'
 *                      (accepted: buildSpeed, carryCapacity, gatherable)
 * </pre>
 *
 * <p>Datapack registries are loaded by Minecraft before reload listeners run, and the codecs are lenient,
 * so a bad field cannot make the file fail to load — the definition is registered with that field
 * ignored. Reporting it loudly at world load (and on every {@code /reload}) is therefore the only way to
 * surface it at runtime; the hard gate is {@code ContentValidationTest}, which runs the same validator
 * over the shipped files during {@code test}.
 *
 * <p>The folder names are not a guess: a datapack registry key {@code reignofnether:unit} is loaded from
 * {@code data/&lt;any namespace&gt;/unit/}, so those folders are this mod's schema by definition, whichever
 * namespace the authoring pack uses.
 */
public class ContentValidationReloadListener implements ResourceManagerReloadListener {

    /** Folder under {@code data/<namespace>/} → the record its files decode into. */
    private static final Map<String, Class<?>> FOLDERS = Map.of(
            "unit", UnitDefinition.class,
            "building", BuildingDefinition.class,
            "faction", Faction.class);

    /** The folders this listener checks; exposed for the tests. */
    public static Map<String, Class<?>> checkedFolders() {
        return FOLDERS;
    }

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        List<String> problems = new ArrayList<>();
        LinkedHashSet<String> failingFiles = new LinkedHashSet<>();
        int checked = 0;

        for (Map.Entry<String, Class<?>> folder : FOLDERS.entrySet()) {
            Map<ResourceLocation, Resource> files = resourceManager.listResources(folder.getKey(),
                    rl -> rl.getPath().endsWith(".json"));
            checked += files.size();
            for (Map.Entry<ResourceLocation, Resource> entry : files.entrySet()) {
                List<String> fileProblems = validateFile(entry.getKey(), entry.getValue(), folder.getValue());
                if (!fileProblems.isEmpty()) {
                    failingFiles.add(entry.getKey().toString());
                    problems.addAll(fileProblems);
                }
            }
        }

        problems.sort(Comparator.naturalOrder());
        for (String problem : problems)
            ReignOfNether.LOGGER.error("[content-validation] {}", problem);
        if (problems.isEmpty())
            ReignOfNether.LOGGER.info("[content-validation] {} definition file(s) checked, no unknown fields",
                    checked);
        else
            ReignOfNether.LOGGER.error("[content-validation] {} unknown field(s) across {} of {} definition "
                    + "file(s) — the lines above name each file, field and the accepted fields; the definitions "
                    + "still load, with those fields ignored",
                    problems.size(), failingFiles.size(), checked);
    }

    private static List<String> validateFile(ResourceLocation fileId, Resource resource, Class<?> definitionType) {
        try (Reader reader = resource.openAsReader()) {
            JsonElement json = JsonParser.parseReader(reader);
            List<String> problems = new ArrayList<>();
            for (String problem : ContentValidator.unknownFields(definitionType, json))
                problems.add(fileId + " " + problem);
            return problems;
        } catch (Exception e) {
            return List.of(fileId + " could not be read as JSON: " + e);
        }
    }
}
