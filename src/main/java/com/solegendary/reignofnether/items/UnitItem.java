package com.solegendary.reignofnether.items;

import net.minecraft.core.Holder;
import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.blocks.RangeIndicator;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.hud.buttons.UnitItemInventoryButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.ItemTagCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import com.solegendary.reignofnether.items.UnitItemType;
import com.solegendary.reignofnether.items.UnitItemBuilder;
import com.solegendary.reignofnether.items.UnitItem;
import mcp.client.Start;
import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

// items that can be held and used by RTS units, especially heroes
// they are still registered as actual Minecraft items
// being a vanilla MC item or a RoN custom item does not determine whether you are a UnitItem, eg.
//    - vanilla golden apple ✔
//    - vanilla dirt block ❌
//    - vanilla fire aspect trident ✔
//    - RoN mana potion ✔
//    - Ron RTS Start Block ❌

// construct via UnitItemBuilder, eg. UnitItemBuilder.of(Items.IRON_SWORD).sellValue(25).build()

public abstract class UnitItem implements RangeIndicator {

    public static final boolean ENABLED = false;

    public static final String RON$COOLDOWN_KEY = "reignofnether:CooldownEndTick";

    public static final Map<Rarity, Integer> RARITY_VALUES = Map.of(
            Rarity.COMMON, 16,
            Rarity.UNCOMMON, 24,
            Rarity.RARE, 32,
            Rarity.EPIC, 48
    );

    public Rarity rarity;
    public boolean toggleActiveOnUse;
    public boolean showRadiusAtCursor;
    public boolean doCastAnimation;
    public boolean resetBehaviours;
    public boolean forceAutocast;
    public boolean canRandomDrop;
    public int maxStackSize; // only affects units; players can stack any items
    protected final Item item;
    public final String descId;
    public final int defaultStackCount;
    public final UUID uuid;
    public final ResourceLocation iconRl;
    public final UnitItemType type;
    public final int sellValue;
    public final int buyCost;
    public final String desc;
    public final Keybinding hotkey;
    public boolean enableTooltip;
    protected final List<Pair<Holder<Enchantment>, Integer>> enchantments;
    protected final List<String> pointDescs;
    public final HashMap<Attribute, AttributeModifier> attributes;
    public BiPredicate<Unit, BlockPos> onUseGround;
    public BiPredicate<Unit, LivingEntity> onUseEntity;
    public BiPredicate<Unit, BuildingPlacement> onUseBuilding;
    public Predicate<Unit> onUse;
    public final boolean consumeOnUse;
    public int manaCost;
    public int cooldownTicksMax;
    public int channelTicks;
    public float range;
    public float radius;
    public boolean showRangeCircle;
    public boolean showRangeLine;
    public boolean showRadiusCircle;
    public boolean suppressDefaultError;

    private Set<BlockPos> highlightBps = new HashSet<>();

    @Override public Set<BlockPos> getHighlightBps() { return highlightBps; }
    @Override public void setHighlightBps(Set<BlockPos> bps) { highlightBps = bps; }

    protected UnitItem(UnitItemBuilder builder) {
        this.rarity = builder.rarity;
        this.toggleActiveOnUse = builder.toggleActiveOnUse;
        this.showRadiusAtCursor = builder.showRadiusAtCursor;
        this.doCastAnimation = builder.doCastAnimation;
        this.resetBehaviours = builder.resetBehaviours;
        this.forceAutocast = builder.forceAutocast;
        this.canRandomDrop = builder.canRandomDrop;
        this.maxStackSize = builder.maxStackSize;

        this.item = builder.item;
        this.defaultStackCount = builder.defaultStackCount;
        if (builder.descId == null || builder.descId.isBlank()) {
            throw new IllegalArgumentException("UnitItemBuilder descId is null or blank!");
        }
        this.descId = builder.descId;
        // 1.5.0 derives the icon texture and the description key from the id, so a new
        // unit item only has to name itself.
        this.iconRl = builder.iconRl != null
                ? builder.iconRl
                : ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID,
                        "textures/item/" + descId + ".png");
        this.uuid = builder.uuid;
        this.type = builder.type;
        this.sellValue = builder.sellValue;
        this.buyCost = builder.buyCost;
        this.desc = builder.desc;
        this.hotkey = builder.hotkey;
        this.enchantments = List.copyOf(builder.enchantments);
        this.pointDescs = List.copyOf(builder.pointDescs);
        this.enableTooltip = builder.enableTooltip;
        this.attributes = builder.attributes;
        this.onUseGround = builder.onUseGround;
        this.onUseEntity = builder.onUseEntity;
        this.onUseBuilding = builder.onUseBuilding;
        this.onUse = builder.onUse;
        this.consumeOnUse = builder.consumeOnUse;
        this.manaCost = builder.manaCost;
        this.cooldownTicksMax = builder.cooldownTicksMax;
        this.channelTicks = builder.channelTicks;
        this.range = builder.range;
        this.radius = builder.radius;
        this.showRangeCircle = builder.showRangeCircle;
        this.showRangeLine = builder.showRangeLine;
        this.showRadiusCircle = builder.showRadiusCircle;
        this.suppressDefaultError = builder.suppressDefaultError;
    }

    public Item getItem() {
        return item;
    }

    public ItemStack getNewItemStack() {
        ItemStack itemStack = new ItemStack(item);
        for (Pair<Holder<Enchantment>, Integer> pair : enchantments) {
            itemStack.enchant(pair.getFirst(), pair.getSecond());
        }
        ItemTagCompat.getOrCreateTag(itemStack).putUUID("uuid", UUID.randomUUID());
        itemStack.setCount(defaultStackCount);
        return itemStack;
    }

    public UnitItemInventoryButton getInventoryButton(int index, ItemStack itemStack, Unit unit, Keybinding hotkey) {
        return new UnitItemInventoryButton(index, this, itemStack, unit, hotkey);
    }

    public Component getName() {
        return new ItemStack(item).getHoverName();
    }

    public String getDescription() {
        return desc;
    }

    /** One string per bullet in the tooltip's passive stat list. */
    public List<String> getPointDescs() {
        List<String> lines = new ArrayList<>();
        for (String desc : pointDescs)
            if (!desc.isBlank())
                lines.add(desc);
        return lines;
    }

    // tooltip rendered when mousing over a ground item entity
    public List<FormattedCharSequence> getEntityTooltip(ItemStack itemStack) {
        return List.of();
    }
}