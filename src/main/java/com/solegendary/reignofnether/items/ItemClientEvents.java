package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.RectZone;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.resources.ResourceSource;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.unit.Checkpoint;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.ItemTagCompat;
import com.solegendary.reignofnether.util.LevelRenderCompat;
import com.solegendary.reignofnether.util.MyRenderer;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The client side of a unit's carried items: the six-slot panel, picking a stack up out of it, and
 * dropping, swapping or handing it over.
 *
 * <p>What left with the unit item layer: every action was driven by a {@code UnitItem} - use on the
 * ground, on a building, on an entity, sell at a market, buy from a shop. What is left is the
 * container itself. Slots are addressed by index while the panel is open and by UUID once a stack is
 * in hand, because the stack can be dropped on a different unit before the release resolves.
 */
public class ItemClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    // slot the player picked up, if any
    private static int actionableInvIndex = -1;
    private static UUID actionableInvUUID = null;
    private static boolean leftClickUseItem = true;

    private static int mouseX = 0;
    private static int mouseY = 0;
    private static int mouseLeftDownX = 0;
    private static int mouseLeftDownY = 0;

    // last positions of hudSelectedEntity and cursor, so the highlight box only redraws when it moves
    private static BlockPos lastOnPos = new BlockPos(0, 0, 0);
    private static BlockPos lastCursorPos = new BlockPos(0, 0, 0);

    public static final ArrayList<Button> renderedButtons = new ArrayList<>();

    // items moused over
    private static final ArrayList<ItemEntity> preselectedItems = new ArrayList<>();

    public static void addPreselectedItem(ItemEntity itemEntity) {
        preselectedItems.add(itemEntity);
    }

    public static void clearPreselectedItems() {
        preselectedItems.clear();
    }

    public static ArrayList<ItemEntity> getPreselectedItems() {
        return preselectedItems;
    }

    public static boolean hasDragActionItem() {
        return actionableInvUUID != null && (mouseX != mouseLeftDownX || mouseY != mouseLeftDownY);
    }

    public static boolean hasLeftClickAction() {
        return leftClickUseItem && actionableInvUUID != null;
    }

    public static void syncInventory(int unitId, List<ItemStack> items) {
        if (MC.level != null && MC.level.getEntity(unitId) instanceof UnitInventory inv)
            for (int i = 0; i < items.size() && i < inv.getAllItems().size(); i++)
                inv.set(i, items.get(i));
    }

    private static final int BUTTON_WIDTH = 22;
    public static final int INV_WIDTH = BUTTON_WIDTH * 2;
    public static final int INV_HEIGHT = BUTTON_WIDTH * 3;
    public static final List<Keybinding> hotkeys = List.of(
            Keybindings.item1,
            Keybindings.item2,
            Keybindings.item3,
            Keybindings.item4,
            Keybindings.item5,
            Keybindings.item6
    );

    /**
     * Draws the six slots. Slots render the raw stack, so what a unit carries is whatever it picked
     * up off the ground - there is no item to describe it in the code any more.
     */
    public static RectZone renderUnitInventory(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, UnitInventory inv) {
        ItemClientEvents.renderedButtons.clear();
        guiGraphics.pose().pushPose();

        for (int i = 0; i < inv.getAllItems().size(); i++) {
            Keybinding hotkey = i < hotkeys.size() ? hotkeys.get(i) : null;
            ItemStack itemStack = inv.getAllItems().get(i);
            int xi = x + ((i % 2) * BUTTON_WIDTH);
            int yi = y + ((i / 2) * BUTTON_WIDTH);

            // empty slot backdrop, so it is obvious there is room to pick something up
            guiGraphics.fill(xi, yi, xi + BUTTON_WIDTH, yi + BUTTON_WIDTH, 0x60000000);

            if (!itemStack.isEmpty()) {
                MyRenderer.renderItem(guiGraphics, itemStack, xi + (BUTTON_WIDTH / 2) - 8, yi + (BUTTON_WIDTH / 2) - 8, 1.0f);

                if (hotkey != null && hotkey.isDown())
                    MyRenderer.renderItem(guiGraphics, itemStack, xi + (BUTTON_WIDTH / 2) - 8, yi + (BUTTON_WIDTH / 2) - 8, 1.0f);
            }
            ItemClientEvents.renderedButtons.add(new Button(
                    "Item " + (i + 1),
                    Button.itemIconSize,
                    (ResourceLocation) null,
                    hotkey,
                    () -> false,
                    () -> itemStack.isEmpty(),
                    () -> true,
                    () -> pickUpSlot(inv, i, itemStack),
                    null,
                    itemStack.isEmpty() ? List.of() : List.of(
                            com.solegendary.reignofnether.util.MiscUtil.fcs(itemStack.getHoverName().getString()))
            ));
        }

        for (Button button : ItemClientEvents.renderedButtons)
            button.render(guiGraphics, x, y, mouseX, mouseY);

        for (Button button : ItemClientEvents.renderedButtons)
            if (button.isMouseOver(mouseX, mouseY))
                button.renderTooltip(guiGraphics, mouseX, mouseY);

        guiGraphics.pose().popPose();
        return RectZone.getZoneByLW(x, y, INV_WIDTH, INV_HEIGHT);
    }

    private static void pickUpSlot(UnitInventory inv, int index, ItemStack itemStack) {
        if (itemStack.isEmpty()) return;
        actionableInvIndex = index;
        UUID uuid = ItemTagCompat.tag(itemStack).getUUID("uuid");
        actionableInvUUID = uuid != null ? uuid : UUID.randomUUID();
        ItemTagCompat.tag(itemStack).putUUID("uuid", actionableInvUUID);
        leftClickUseItem = true;
    }

    public static void resetActions() {
        actionableInvUUID = null;
        actionableInvIndex = -1;
        leftClickUseItem = false;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post evt) {
        // keep the tracked cursor/position current so the highlight box knows when to move
        if (HudClientEvents.hudSelectedEntity instanceof LivingEntity le) {
            lastOnPos = le.getOnPos();
            lastCursorPos = CursorClientEvents.getPreselectedBlockPos();
        }
    }

    @SubscribeEvent
    public static void onLeftMouseRelease(ScreenEvent.MouseButtonReleased.Post evt) {
        if (MC.player == null || evt.getButton() != GLFW.GLFW_MOUSE_BUTTON_1)
            return;

        for (Button button : renderedButtons)
            button.checkClickedReleased((int) evt.getMouseX(), (int) evt.getMouseY(), true);

        if (hasDragActionItem() && HudClientEvents.hudSelectedEntity instanceof UnitInventory inv
                && HudClientEvents.hudSelectedEntity instanceof Unit unit) {
            Button mousedOverButton = getMousedOverSlot();
            Button hudMousedOverButton = HudClientEvents.getMousedOverButton();

            if (mousedOverButton != null && mousedOverButton != renderedButtons.get(actionableInvIndex)) {
                inv.swapSlots(actionableInvIndex, renderedButtons.indexOf(mousedOverButton));
                ItemServerboundPacket.swap(((Entity) inv).getId(), actionableInvIndex, renderedButtons.indexOf(mousedOverButton));
            } else if (hudMousedOverButton != null &&
                    hudMousedOverButton.entity != HudClientEvents.hudSelectedEntity &&
                    hudMousedOverButton.entity instanceof UnitInventory) {
                Relationship rlu = UnitClientEvents.getPlayerToEntityRelationship(hudMousedOverButton.entity);
                if (rlu == Relationship.FRIENDLY || rlu == Relationship.OWNED) {
                    // hand over via the unit under the cursor
                    unit.getCheckpoints().clear();
                    unit.getCheckpoints().add(new Checkpoint(hudMousedOverButton.entity, true));
                    ItemServerboundPacket.give(((Entity) inv).getId(), actionableInvUUID, hudMousedOverButton.entity.getId());
                }
            } else if (!HudClientEvents.isMouseOverAnyButtonOrHud()) {
                if (!UnitClientEvents.getPreselectedUnits().isEmpty()) {
                    LivingEntity le = UnitClientEvents.getPreselectedUnits().get(0);
                    Relationship rlu = UnitClientEvents.getPlayerToEntityRelationship(le);
                    if (le instanceof UnitInventory && le != HudClientEvents.hudSelectedEntity &&
                            (rlu == Relationship.FRIENDLY || rlu == Relationship.OWNED)) {
                        // hand over via direct entity selection
                        unit.getCheckpoints().clear();
                        unit.getCheckpoints().add(new Checkpoint(le, true));
                        ItemServerboundPacket.give(((Entity) inv).getId(), actionableInvUUID, le.getId());
                    }
                } else {
                    // drop on the ground
                    BlockPos bp = CursorClientEvents.getPreselectedBlockPos();
                    unit.getCheckpoints().clear();
                    unit.getCheckpoints().add(new Checkpoint(bp, true));
                    ItemServerboundPacket.drop(((Entity) inv).getId(), actionableInvUUID, bp);
                }
            }
            resetActions();
            CursorClientEvents.setLeftClickAction(null);
        }
    }

    @SubscribeEvent
    public static void onMousePress(ScreenEvent.MouseButtonPressed.Post evt) {
        if (!(MC.screen instanceof com.solegendary.reignofnether.guiscreen.TopdownGui) || MC.player == null)
            return;
        for (Button button : renderedButtons) {
            if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
                button.checkClicked((int) evt.getMouseX(), (int) evt.getMouseY(), true);
            } else if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
                button.checkClicked((int) evt.getMouseX(), (int) evt.getMouseY(), false);
            }
        }
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            resetActions();
            CursorClientEvents.setLeftClickAction(null);
        }
    }

    private static Button getMousedOverSlot() {
        for (Button button : renderedButtons)
            if (button.isMouseOver(mouseX, mouseY))
                return button;
        return null;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS ||
                HudClientEvents.isMouseOverAnyButtonOrHud())
            return;

        if (MC.level != null && OrthoviewClientEvents.isEnabled()) {
            // see LevelRenderCompat: at AFTER_CUTOUT_BLOCKS RenderSystem's model-view does not have
            // the camera rotation yet, so the draw and the flush both have to happen under it
            LevelRenderCompat.drawAndFlush(evt, () -> {
                for (ItemEntity itemEntity : preselectedItems) {
                    ResourceSource res = ResourceSources.getFromItem(itemEntity.getItem().getItem());
                    if (res != null && res.resourceValue > 0) {
                        MyRenderer.drawBoxBottom(
                                evt.getPoseStack(),
                                itemEntity.getBoundingBox().inflate(0.25, 0, 0.25),
                                1, 1, 1,
                                CursorClientEvents.isRightClickDown() ? 1.0f : 0.25f
                        );
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.Render.Post evt) {
        mouseX = evt.getMouseX();
        mouseY = evt.getMouseY();
        // clear to avoid hiding ghost renders if the player happens to mouse back over this exact pixel
        if (mouseX != mouseLeftDownX || mouseY != mouseLeftDownY) {
            mouseLeftDownX = 0;
            mouseLeftDownY = 0;
        }
    }
}
