package com.solegendary.reignofnether.building.production;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingProductionServerboundPacket;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * A {@link ProductionItem} presented as a building ability (plan §14.3), so a building's
 * production is no longer a second, parallel "what this building can do" mechanism next to
 * abilities. The click still goes through the existing server-authoritative
 * {@link BuildingProductionServerboundPacket#startProduction}, so the behaviour is unchanged.
 */
public class ProductionAbility extends Ability {

    public final ProductionItem item;

    public ProductionAbility(ProductionItem item) {
        super(null, 0, 0, 0, false);
        this.item = item;
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        return new AbilityButton(
                item.getItemName(),
                item.getIcon(),
                hotkey,
                () -> false,
                () -> false,
                () -> true,
                () -> BuildingProductionServerboundPacket.startProduction(item),
                null,
                item.getTooltipLines(),
                this,
                placement
        );
    }
}
