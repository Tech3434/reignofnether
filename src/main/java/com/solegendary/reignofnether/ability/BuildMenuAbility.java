package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.function.Supplier;

/**
 * The worker's build menu (plan §14.2): the building-place buttons are opened from a menu instead
 * of always occupying a row of the HUD. The items are built on demand because custom buildings are
 * only known at runtime, so this menu hands out pre-built buttons rather than abilities.
 */
public class BuildMenuAbility extends Ability {

    private final String nameKey;
    private final ResourceLocation icon;
    private final Supplier<List<Button>> buttonsSupplier;

    public BuildMenuAbility(String nameKey, ResourceLocation icon, Supplier<List<Button>> buttonsSupplier) {
        super(null, 0, 0, 0, false);
        this.nameKey = nameKey;
        this.icon = icon;
        this.buttonsSupplier = buttonsSupplier;
        this.defaultHotkey = Keybindings.abilitySlot4;
    }

    @Override
    public boolean isMenu() {
        return true;
    }

    @Override
    protected boolean hasDynamicSubButtons() {
        return true;
    }

    @Override
    public List<Button> getSubButtons(Unit unit) {
        return buttonsSupplier.get();
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        return new AbilityButton(
                I18n.get(nameKey),
                icon,
                hotkey,
                () -> HudClientEvents.isSubmenuOpenFor(this, unit, null),
                () -> false,
                () -> true,
                () -> HudClientEvents.openSubmenu(this, unit),
                null,
                List.of(FormattedCharSequence.forward(I18n.get(nameKey), Style.EMPTY)),
                this,
                unit
        );
    }
}
