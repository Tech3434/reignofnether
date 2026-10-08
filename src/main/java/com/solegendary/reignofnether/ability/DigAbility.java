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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The DIG_BLOCK / DIG_AREA worker orders (plan §14.4) expressed as abilities, so they show up in
 * the HUD just like every other unit ability instead of needing their own switch branch.
 *
 * <p>A dig is not instant: the targeted block is broken over time, like a player mining it. The time
 * scales with the block's vanilla destroy speed and is divided by the unit's tool tier
 * ({@link Unit#getDigToolTier()}, iron by default), so the tier is a property of the unit rather
 * than of the order. DIG_AREA queues every block in the outlined rectangle (up to a hard cap) and
 * the worker works through them one at a time. Drops go into the unit's six-slot inventory, and the
 * rest falls on the ground.
 *
 * <p>DIG is world-only: blocks that belong to a building are never dug and never damaged, so digging
 * can never carve a hole in a structure or chip its HP. Structures are broken with the normal attack
 * orders (ATTACK_BUILDING / melee).
 */
public class DigAbility extends Ability {

    public static final float DIG_RANGE = 12f;
    /** Cap on blocks a single DIG_AREA may queue. */
    public static final int MAX_AREA_BLOCKS = 256;
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

    @Override
    public void useArea(Level level, Unit unitUsing, BlockPos corner1, BlockPos corner2) {
        if (!(level instanceof ServerLevel serverLevel) || corner1 == null || corner2 == null)
            return;

        int minX = Math.min(corner1.getX(), corner2.getX());
        int maxX = Math.max(corner1.getX(), corner2.getX());
        int minY = Math.min(corner1.getY(), corner2.getY());
        int maxY = Math.max(corner1.getY(), corner2.getY());
        int minZ = Math.min(corner1.getZ(), corner2.getZ());
        int maxZ = Math.max(corner1.getZ(), corner2.getZ());

        List<BlockPos> candidates = new ArrayList<>();
        outer:
        for (int y = minY; y <= maxY; y++)
            for (int x = minX; x <= maxX; x++)
                for (int z = minZ; z <= maxZ; z++) {
                    if (candidates.size() >= MAX_AREA_BLOCKS)
                        break outer;
                    candidates.add(new BlockPos(x, y, z));
                }

        enqueue(serverLevel, unitUsing, candidates);
    }

    /** Replaces the unit's dig queue with the diggable candidates that are within range. */
    private void enqueue(ServerLevel level, Unit unit, List<BlockPos> candidates) {
        DigState state = new DigState();
        double maxDist = DIG_RANGE * DIG_RANGE;
        LivingEntity le = (LivingEntity) unit;
        for (BlockPos bp : candidates) {
            state.queue.add(bp);
        }
        // drop candidates the unit cannot reach right now, and anything that is not diggable
        state.queue.removeIf(bp -> le.distanceToSqr(Vec3.atCenterOf(bp)) > maxDist || !isDiggable(level, bp));
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
     * world tick. A dig whose unit walked out of range (or whose block stopped being diggable) is
     * abandoned outright, which is the "reset progress on interrupt" behaviour the owner asked for.
     */
    public static void serverTick(ServerLevel level) {
        if (DIG_STATES.isEmpty())
            return;

        Iterator<Map.Entry<Integer, DigState>> it = DIG_STATES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, DigState> entry = it.next();
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof Unit unit) || !entity.isAlive()) {
                it.remove();
                continue;
            }
            DigState state = entry.getValue();

            if (state.current == null) {
                BlockPos next = null;
                while (!state.queue.isEmpty()) {
                    BlockPos bp = state.queue.poll();
                    if (isDiggable(level, bp)) {
                        next = bp;
                        break;
                    }
                }
                if (next == null) {
                    it.remove();
                    continue;
                }
                state.current = next;
                state.ticksTotal = ticksToBreak(level, next, unit);
                state.ticksLeft = state.ticksTotal;
            }

            if (((LivingEntity) unit).distanceToSqr(Vec3.atCenterOf(state.current)) > DIG_RANGE * DIG_RANGE
                    || !isDiggable(level, state.current)) {
                it.remove();
                continue;
            }

            state.ticksLeft -= 1;
            if (state.ticksLeft <= 0) {
                breakBlock(level, unit, state.current);
                state.current = null;
            }
        }
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
