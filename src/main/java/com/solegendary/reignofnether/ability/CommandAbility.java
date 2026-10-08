package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.function.Predicate;

/**
 * A generic player order exposed as an ability (plan §14.2): attack, stop, hold, build/repair,
 * gather, garrison. These used to live in {@code ActionButtons} as a HUD-only special case; now
 * they are ordinary abilities, so {@code Unit#getAbilityButtons} can return one uniform list.
 *
 * <p>The command issued is identical to before ({@link CursorClientEvents#setLeftClickAction} for a
 * targeted order, {@link UnitClientEvents#sendUnitCommand} for an immediate one).
 */
public class CommandAbility extends Ability {

    /** Resolves the button icon per unit - used by gather, whose icon follows the target resource. */
    public interface IconResolver {
        ResourceLocation get(Unit unit);
    }

    private final String nameKey;
    private final ResourceLocation staticIcon;
    private final IconResolver iconResolver;
    private final boolean targeted;
    private final Predicate<Unit> selected;

    public CommandAbility(UnitAction action, String nameKey, ResourceLocation icon,
                          Keybinding hotkey, boolean targeted) {
        this(action, nameKey, icon, null, hotkey, targeted, null);
    }

    public CommandAbility(UnitAction action, String nameKey, IconResolver iconResolver,
                          Keybinding hotkey, boolean targeted, Predicate<Unit> selected) {
        this(action, nameKey, null, iconResolver, hotkey, targeted, selected);
    }

    private CommandAbility(UnitAction action, String nameKey, ResourceLocation staticIcon,
                           IconResolver iconResolver, Keybinding hotkey, boolean targeted,
                           Predicate<Unit> selected) {
        super(action, 0, 0, 0, false);
        this.nameKey = nameKey;
        this.staticIcon = staticIcon;
        this.iconResolver = iconResolver;
        this.targeted = targeted;
        this.selected = selected;
        if (hotkey != null)
            this.defaultHotkey = hotkey;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        ResourceLocation icon = iconResolver != null ? iconResolver.get(unit) : staticIcon;
        Runnable onClick = targeted
                ? () -> CursorClientEvents.setLeftClickAction(action)
                : () -> UnitClientEvents.sendUnitCommand(action);
        return new AbilityButton(
                I18n.get(nameKey),
                icon,
                hotkey,
                () -> selected != null && selected.test(unit),
                () -> false,
                () -> true,
                onClick,
                null,
                List.of(FormattedCharSequence.forward(I18n.get(nameKey), Style.EMPTY)),
                this,
                unit
        );
    }
}
