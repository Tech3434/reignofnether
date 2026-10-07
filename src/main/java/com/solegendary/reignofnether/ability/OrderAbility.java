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

/**
 * An existing player order exposed as an ability (plan §14.2), so gather / return-resources / build
 * stop being special HUD cases and become ordinary entries in a unit's ability list.
 *
 * <p>The order itself is unchanged - the button just issues the same {@link UnitAction} the old
 * dedicated action button did. {@code targeted} decides whether the click arms a cursor action
 * (needs a world target) or fires immediately against the selection.
 */
public class OrderAbility extends Ability {

    private final String nameKey;
    private final ResourceLocation icon;
    private final boolean targeted;

    public OrderAbility(UnitAction action, String nameKey, ResourceLocation icon, boolean targeted) {
        super(action, 0, 0, 0, false);
        this.nameKey = nameKey;
        this.icon = icon;
        this.targeted = targeted;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        Runnable onClick = targeted
                ? () -> CursorClientEvents.setLeftClickAction(action)
                : () -> UnitClientEvents.sendUnitCommand(action);
        return new AbilityButton(
                I18n.get(nameKey),
                icon,
                hotkey,
                () -> targeted && CursorClientEvents.getLeftClickAction() == action,
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
