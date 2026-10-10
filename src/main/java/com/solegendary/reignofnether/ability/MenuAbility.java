package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * A menu button: it has no server-side effect of its own, it only opens its
 * {@link Ability#subAbilities} list in the HUD. This is the "open a submenu" ability from plan
 * §14.1/§14.2 - menu-in-menu works because a sub-ability may itself be a MenuAbility.
 */
public class MenuAbility extends Ability {

    private final String nameKey;
    private final ResourceLocation icon;

    public MenuAbility(String nameKey, ResourceLocation icon) {
        super(null, 0, 0, 0, false);
        this.nameKey = nameKey;
        this.icon = icon;
    }

    /**
     * Whether the menu button must be hidden because it would open an empty menu. A subclass whose
     * items are not stored in {@link #subAbilities} (see {@link DataMenuAbility}, which mixes abilities
     * and building-place buttons) must override this, or a menu holding only such items would be hidden
     * and could never be opened.
     */
    protected boolean hasNoEntries() {
        return subAbilities.isEmpty();
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        return new AbilityButton(
                I18n.get(nameKey),
                icon,
                hotkey,
                () -> HudClientEvents.isSubmenuOpenFor(this, unit, null),
                () -> hasNoEntries(),
                () -> true,
                () -> HudClientEvents.openSubmenu(this, unit),
                null,
                List.of(FormattedCharSequence.forward(I18n.get(nameKey), Style.EMPTY)),
                this,
                unit
        );
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        return new AbilityButton(
                I18n.get(nameKey),
                icon,
                hotkey,
                () -> HudClientEvents.isSubmenuOpenFor(this, null, placement),
                () -> hasNoEntries(),
                () -> true,
                () -> HudClientEvents.openSubmenu(this, placement),
                null,
                List.of(FormattedCharSequence.forward(I18n.get(nameKey), Style.EMPTY)),
                this,
                placement
        );
    }
}
