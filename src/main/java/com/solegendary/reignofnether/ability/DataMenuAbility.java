package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.WorkerBuildMenu;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.unit.interfaces.Unit;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A data-driven menu ability (plan CONTENT_JSON_PLAN.md): {@code type = "reignofnether:menu"} turns its
 * {@code submenu} entries into the buttons of an opened submenu.
 *
 * <p>Entries may be an inline {@code ability} (which may itself be a menu, giving menu-in-menu), a
 * {@code command} (a standard RTS order), or a {@code building} (the worker's place button for that
 * building). {@code row}/{@code col} give an explicit grid position.
 *
 * <p>The buttons are built on demand in {@link #getSubButtons(Unit)} (client-only, the HUD path), because
 * building-place buttons and custom buildings are only known at runtime. Only the inline {@code ability}
 * entries are created eagerly and added to {@link #subAbilities}, so the server can dispatch them by
 * their {@code UnitAction} (see {@code UnitActionItem}; {@code Abilities#get()} flattens menus). The
 * {@code command} entries come from the shared {@link CommandAbilities} singletons and are dispatched
 * client-side by their button's click handler, so they are deliberately NOT added to {@code subAbilities}
 * (adding a shared instance would pollute every unit's flattened ability list).
 */
public class DataMenuAbility extends MenuAbility {

    private static final String DEFAULT_NAME = "abilities.reignofnether.menu";
    private static final ResourceLocation DEFAULT_ICON =
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/repeating_command_block_back.png");

    /** The standard player orders a menu may mix in by name (plan §14.2). */
    private static final Map<String, Ability> COMMANDS = new HashMap<>();

    private static final List<Keybinding> SLOTS = List.of(
            Keybindings.abilitySlot1, Keybindings.abilitySlot2, Keybindings.abilitySlot3, Keybindings.abilitySlot4,
            Keybindings.abilitySlot5, Keybindings.abilitySlot6, Keybindings.abilitySlot7, Keybindings.abilitySlot8);

    static {
        COMMANDS.put("attack", CommandAbilities.ATTACK);
        COMMANDS.put("stop", CommandAbilities.STOP);
        COMMANDS.put("hold", CommandAbilities.HOLD);
        COMMANDS.put("build", CommandAbilities.BUILD_REPAIR);
        COMMANDS.put("gather", CommandAbilities.GATHER);
        COMMANDS.put("garrison", CommandAbilities.GARRISON);
        COMMANDS.put("ungarrison", CommandAbilities.UNGARRISON);
    }

    /** One resolved entry, in JSON order. */
    private record Entry(@Nullable Ability ability, @Nullable ResourceLocation building, int[] position) { }

    private final List<Entry> entries = new ArrayList<>();
    // rebuilt on each getSubButtons() call and read back by getSubButtonPositions(); the HUD calls them
    // back-to-back, so the two stay aligned
    private List<int[]> lastPositions = new ArrayList<>();

    public DataMenuAbility(AbilitySpec spec, @Nullable Unit unit) {
        super(spec.name().orElse(DEFAULT_NAME), spec.icon().orElse(DEFAULT_ICON));
        if (unit != null)
            build(spec, unit);
    }

    private void build(AbilitySpec spec, Unit unit) {
        int autoIndex = 0;
        for (MenuEntrySpec entry : spec.submenu()) {
            Ability child = null;
            ResourceLocation building = null;
            boolean childIsDataAbility = false;
            AbilitySpec childSpec = entry.ability().orElse(null);
            if (childSpec != null) {
                child = AbilityTypes.create(childSpec, unit);
                childIsDataAbility = child != null;
            } else if (entry.command().isPresent()) {
                child = COMMANDS.get(entry.command().get());
            } else if (entry.building().isPresent()) {
                building = entry.building().get();
            }
            if (child != null || building != null) {
                // only data abilities join subAbilities (server dispatch); a shared command singleton must
                // not, and its button is built from `entries` anyway
                if (childIsDataAbility)
                    addSubAbility(child);
                int[] pos = entry.hasPosition() ? new int[]{entry.rowOr(0), entry.colOr(autoIndex)} : null;
                entries.add(new Entry(child, building, pos));
            } else if (childSpec != null || entry.command().isPresent() || entry.building().isPresent()) {
                ReignOfNether.LOGGER.warn("Data menu entry could not be built (unknown ability type, command or building '{}{}')",
                        entry.command().orElse(""),
                        childSpec != null ? childSpec.type() : entry.building().map(Object::toString).orElse(""));
            }
            autoIndex++;
        }
    }

    @Override
    public boolean isMenu() {
        return true;
    }

    @Override
    protected boolean hasDynamicSubButtons() {
        return true;
    }

    /** Entries may all be commands or buildings, so {@code subAbilities} is not the whole menu. */
    @Override
    protected boolean hasNoEntries() {
        return entries.isEmpty();
    }

    @Override
    public List<Button> getSubButtons(Unit unit) {
        List<Button> buttons = new ArrayList<>();
        List<int[]> positions = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            Button button = null;
            if (entry.ability() != null) {
                Keybinding hotkey = SLOTS.get(Math.min(i, SLOTS.size() - 1));
                button = entry.ability().getButton(hotkey, unit);
                if (unit != null && button != null && !entry.ability().requiredResearch.isEmpty())
                    Abilities.applyResearchGate(button, entry.ability(),
                            ((net.minecraft.world.entity.Entity) unit).level(), unit.getOwnerName());
            } else if (entry.building() != null) {
                button = WorkerBuildMenu.buildButtonFor(entry.building());
            }
            if (button != null) {
                buttons.add(button);
                positions.add(entry.position());
            }
        }
        this.lastPositions = positions;
        return buttons;
    }

    @Override
    public List<int[]> getSubButtonPositions(Unit unit) {
        return lastPositions;
    }
}
