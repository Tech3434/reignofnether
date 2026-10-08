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
 * <p>Effect: the targeted block is removed and its drops go into the unit's six-slot inventory; if
 * that is full the remainder drops on the ground. DIG_AREA carries an outlined rectangle (two
 * corners, plumbed through UnitActionItem) and digs every non-building block inside it, up to a
 * hard cap so a wide drag cannot flatten a mountain.
 *
 * <p>DIG is a world-only order: blocks that belong to a building are never dug and never damaged,
 * so digging can never carve a hole in a structure or chip its HP. Structures are broken with the
 * normal attack orders (ATTACK_BUILDING / melee). This replaces the earlier §14.5 behaviour where
 * a dig on a building block dealt percentage HP damage.
 */
public class DigAbility extends Ability {

    public static final float DIG_RANGE = 12f;
    /** Cap on blocks a single DIG_AREA may remove. */
    public static final int MAX_AREA_BLOCKS = 256;
    public DigAbility(UnitAction action) {
        super(action, 20, DIG_RANGE, 0, false);
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

        digBlock(serverLevel, unitUsing, centre);
    }

    @Override
    public void useArea(Level level, Unit unitUsing, BlockPos corner1, BlockPos corner2) {
        if (!(level instanceof ServerLevel serverLevel) || corner1 == null || corner2 == null)
            return;

        LivingEntity le = (LivingEntity) unitUsing;
        int minX = Math.min(corner1.getX(), corner2.getX());
        int maxX = Math.max(corner1.getX(), corner2.getX());
        int minY = Math.min(corner1.getY(), corner2.getY());
        int maxY = Math.max(corner1.getY(), corner2.getY());
        int minZ = Math.min(corner1.getZ(), corner2.getZ());
        int maxZ = Math.max(corner1.getZ(), corner2.getZ());

        int dug = 0;
        for (int y = minY; y <= maxY && dug < MAX_AREA_BLOCKS; y++)
            for (int x = minX; x <= maxX && dug < MAX_AREA_BLOCKS; x++)
                for (int z = minZ; z <= maxZ && dug < MAX_AREA_BLOCKS; z++) {
                    BlockPos bp = new BlockPos(x, y, z);
                    if (le.distanceToSqr(Vec3.atCenterOf(bp)) > DIG_RANGE * DIG_RANGE)
                        continue;
                    if (digBlock(serverLevel, unitUsing, bp))
                        dug += 1;
                }
    }

    /** Removes one block and hands its drops to the unit. Returns true if a block was removed. */
    private boolean digBlock(ServerLevel level, Unit unit, BlockPos bp) {
        // DIG is world-only: a block that belongs to a building is left completely alone - no dig,
        // no drops, no HP damage. Structures are destroyed with the normal attack orders.
        if (BuildingUtils.isPosInsideAnyBuilding(false, bp))
            return false;

        BlockState bs = level.getBlockState(bp);
        if (bs.isAir())
            return false;
        if (bs.getDestroySpeed(level, bp) < 0) // bedrock and other unbreakables
            return false;

        List<ItemStack> drops = Block.getDrops(bs, level, bp, null);
        level.destroyBlock(bp, false);

        for (ItemStack drop : drops) {
            if (drop.isEmpty())
                continue;
            if (!(unit instanceof UnitInventory inv) || !inv.tryAdding(drop))
                Block.popResource(level, bp, drop);
        }
        return true;
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
