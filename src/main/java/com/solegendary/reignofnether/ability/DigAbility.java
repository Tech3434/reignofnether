package com.solegendary.reignofnether.ability;

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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The DIG_BLOCK worker order (plan §14.4) expressed as an ability, so it shows up in the HUD just
 * like every other unit ability instead of needing its own switch branch. The former DIG_AREA
 * (outlined rectangle) was removed at the owner's request - it did not behave acceptably in game.
 *
 * <p>A dig is not instant: the targeted block is broken over time, like a player mining it. The time
 * scales with the block's vanilla destroy speed and is divided by the unit's tool tier
 * ({@link Unit#getDigToolTier()}, iron by default), so the tier is a property of the unit rather
 * than of the order. Drops go into the unit's six-slot inventory, and the rest falls on the ground.
 *
 * <p>DIG is world-only: blocks that belong to a building are never dug and never damaged, so digging
 * can never carve a hole in a structure or chip its HP. Structures are broken with the normal attack
 * orders (ATTACK_BUILDING / melee).
 */
public class DigAbility extends Ability {

    public static final float DIG_RANGE = 12f;
    /** Ticks a block of destroy-speed 1 takes at tool speed 1; divided by the unit's tool speed. */
    private static final float TICKS_PER_HARDNESS = 30f;

    public DigAbility(UnitAction action) {
        super(action, 20, DIG_RANGE, 0, false);
    }

    // Per-unit dig queue and progress, advanced by serverTick(). Keyed by entity id; server-side only.
    private static final Map<Integer, DigState> DIG_STATES = new ConcurrentHashMap<>();

    private static final class DigState {
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos current;
        float ticksLeft;
        float ticksTotal;
    }

    /**
     * Abandons a unit's dig queue and progress. Called when the unit is given any other order, so an
     * interrupted dig resets (owner's decision: progress is not kept).
     */
    public static void clear(int unitId) {
        DIG_STATES.remove(unitId);
    }

    @Override
    public void use(Level level, Unit unitUsing, BlockPos targetBp) {
        if (!(level instanceof ServerLevel serverLevel))
            return;

        LivingEntity le = (LivingEntity) unitUsing;
        BlockPos centre = (targetBp == null || targetBp.equals(new BlockPos(0, 0, 0)))
                ? le.blockPosition().below()
                : targetBp;

        enqueue(serverLevel, unitUsing, List.of(centre));
    }

    /** Replaces the unit's dig queue with the diggable candidate blocks. */
    private void enqueue(ServerLevel level, Unit unit, List<BlockPos> candidates) {
        DigState state = new DigState();
        for (BlockPos bp : candidates)
            if (isDiggable(level, bp))
                state.queue.add(bp);
        DIG_STATES.put(((Entity) unit).getId(), state);
    }

    private static boolean isDiggable(ServerLevel level, BlockPos bp) {
        // DIG is world-only: a block that belongs to a building is left completely alone.
        if (BuildingUtils.isPosInsideAnyBuilding(false, bp))
            return false;
        BlockState bs = level.getBlockState(bp);
        if (bs.isAir())
            return false;
        return bs.getDestroySpeed(level, bp) >= 0; // bedrock and other unbreakables
    }

    /**
     * Advances every in-flight dig by one tick and breaks finished blocks. Called from the server
     * world tick. A block further than {@link #DIG_RANGE} is walked to rather than dropped; the queue
     * is abandoned only when its blocks stop being diggable, or when another order clears the dig
     * (progress is not kept).
     */
    public static void serverTick(ServerLevel level) {
        if (DIG_STATES.isEmpty())
            return;

        Iterator<Map.Entry<Integer, DigState>> it = DIG_STATES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, DigState> entry = it.next();
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof Unit unit && unit.isRtsUnit()) || !entity.isAlive()) {
                it.remove();
                continue;
            }
            DigState state = entry.getValue();

            if (state.current == null || !isDiggable(level, state.current)) {
                state.current = nextTarget(level, state);
                if (state.current == null) {
                    it.remove();
                    continue;
                }
                state.ticksTotal = ticksToBreak(level, state.current, unit);
                state.ticksLeft = state.ticksTotal;
            }

            if (((LivingEntity) unit).distanceToSqr(Vec3.atCenterOf(state.current)) > DIG_RANGE * DIG_RANGE) {
                // too far to reach: walk there and keep the rest of the queue
                if (unit.getMoveGoal() != null)
                    unit.getMoveGoal().setMoveTarget(state.current);
                continue;
            }

            state.ticksLeft -= 1;
            if (state.ticksLeft <= 0) {
                breakBlock(level, unit, state.current);
                state.current = null;
            }
        }
    }

    private static BlockPos nextTarget(ServerLevel level, DigState state) {
        while (!state.queue.isEmpty()) {
            BlockPos bp = state.queue.poll();
            if (isDiggable(level, bp))
                return bp;
        }
        return null;
    }

    private static float ticksToBreak(ServerLevel level, BlockPos bp, Unit unit) {
        float hardness = level.getBlockState(bp).getDestroySpeed(level, bp);
        if (hardness < 0)
            hardness = 0;
        float speed = unit.getDigToolTier().speed;
        return Math.max(1f, hardness * TICKS_PER_HARDNESS / speed);
    }

    /** Removes one block and hands its drops to the unit. */
    private static void breakBlock(ServerLevel level, Unit unit, BlockPos bp) {
        BlockState bs = level.getBlockState(bp);
        if (bs.isAir())
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
        String name = "abilities.reignofnether.dig_block";
        return new AbilityButton(
                I18n.get(name),
                ResourceLocation.withDefaultNamespace("textures/item/iron_pickaxe.png"),
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
