package com.solegendary.reignofnether.ability;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.items.UnitInventory;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The DIG_BLOCK / DIG_AREA worker orders (plan §14.4) expressed as abilities, so they show up in
 * the HUD just like every other unit ability instead of needing their own switch branch.
 *
 * <p>Effect: the targeted block (or, for DIG_AREA, a small square around it) is removed and its
 * drops go into the unit's six-slot inventory; if that is full the remainder drops on the ground.
 * Blocks that belong to a building are never dug - those are handled through the attack system, so
 * digging can never carve a hole in a faction's own structures (the building-HP variant is §14.5
 * and is deliberately not implemented yet).
 */
public class DigAbility extends Ability {

    public static final float DIG_RANGE = 12f;
    /** Half-width of the DIG_AREA square. A real box-select frame is a later step. */
    public static final int AREA_RADIUS = 2;

    private final int areaRadius;

    public DigAbility(UnitAction action) {
        super(action, 20, DIG_RANGE, 0, false);
        this.areaRadius = action == UnitAction.DIG_AREA ? AREA_RADIUS : 0;
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        if (!(level instanceof ServerLevel serverLevel))
            return;

        LivingEntity le = (LivingEntity) unitUsing;
        BlockPos centre = (targetBp == null || targetBp.equals(new BlockPos(0, 0, 0)))
                ? le.blockPosition().below()
                : targetBp;

        if (le.distanceToSqr(Vec3.atCenterOf(centre)) > DIG_RANGE * DIG_RANGE)
            return;

        if (areaRadius <= 0) {
            digBlock(serverLevel, unitUsing, centre);
        } else {
            for (int dx = -areaRadius; dx <= areaRadius; dx++)
                for (int dz = -areaRadius; dz <= areaRadius; dz++)
                    digBlock(serverLevel, unitUsing, centre.offset(dx, 0, dz));
        }
    }

    private void digBlock(ServerLevel level, Unit unit, BlockPos bp) {
        if (BuildingUtils.isPosInsideAnyBuilding(false, bp))
            return;

        BlockState bs = level.getBlockState(bp);
        if (bs.isAir())
            return;
        if (bs.getDestroySpeed(level, bp) < 0) // bedrock and other unbreakables
            return;

        List<ItemStack> drops = Block.getDrops(bs, level, bp, null);
        level.destroyBlock(bp, false);

        for (ItemStack drop : drops) {
            if (drop.isEmpty())
                continue;
            if (!(unit instanceof UnitInventory inv) || !inv.tryAdding(drop))
                Block.popResource(level, bp, drop);
        }
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, Unit unit) {
        String name = action == UnitAction.DIG_AREA
                ? "abilities.reignofnether.dig_area"
                : "abilities.reignofnether.dig_block";
        return new AbilityButton(
                I18n.get(name),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/shovel.png"),
                hotkey,
                () -> CursorClientEvents.getLeftClickAction() == action,
                () -> false,
                () -> true,
                () -> CursorClientEvents.setLeftClickAction(action),
                null,
                List.of(FormattedCharSequence.forward(I18n.get(name), Style.EMPTY)),
                this,
                unit
        );
    }
}
