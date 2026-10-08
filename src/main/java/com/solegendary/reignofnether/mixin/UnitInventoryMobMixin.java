package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.items.ItemClientboundPacket;
import com.solegendary.reignofnether.items.UnitInventory;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.EnchantmentUtil;
import com.solegendary.reignofnether.util.ItemTagCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

/**
 * Gives every mob the mod's six-slot container.
 *
 * <p>This used to be the unit item layer: a slot held a modded {@code UnitItem} with its own
 * abilities, mana cost and cooldown, and the mixin answered "is this unit holding that item,
 * switched on". That layer is gone, so what is left is the plain container - carry, drop, swap,
 * hand over - which is what a faction's units need to pick things up off the ground and carry
 * them home.
 */
@Mixin(Mob.class)
public abstract class UnitInventoryMobMixin extends LivingEntity implements UnitInventory {

    @Unique
    private static final String RON$UNIT_ITEMS_KEY = "reignofnether:UnitItems";

    @Unique
    private final NonNullList<ItemStack> unitItems =
            NonNullList.withSize(MAX_INVENTORY_SIZE, ItemStack.EMPTY);

    protected UnitInventoryMobMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public NonNullList<ItemStack> getAllItems() {
        return this.unitItems;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.unitItems)
            if (!stack.isEmpty())
                return false;
        return true;
    }

    // 1.21.1 dropped Container#isFull, so this is the mod's own check now.
    public boolean isFull() {
        for (ItemStack itemStack : getAllItems())
            if (itemStack == ItemStack.EMPTY || itemStack.isEmpty())
                return false;
        return true;
    }

    @Override
    public ItemStack get(int index) {
        return this.unitItems.get(index);
    }

    @Override
    @Nullable
    public ItemStack get(UUID uuid) {
        for (ItemStack itemStack : this.unitItems) {
            CompoundTag tag = ItemTagCompat.tag(itemStack);
            if (tag != null && tag.hasUUID("uuid") && tag.getUUID("uuid").equals(uuid))
                return itemStack;
        }
        return null;
    }

    @Override
    public void set(int index, ItemStack stack) {
        set(index, stack, null);
    }

    @Override
    public void set(int index, ItemStack stack, UUID uuid) {
        ItemStack newStack = stack == null ? ItemStack.EMPTY : stack;
        if (!newStack.isEmpty()) {
            CompoundTag tag = ItemTagCompat.getOrCreateTag(newStack);
            if (uuid != null)
                tag.putUUID("uuid", uuid);
            else if (!tag.hasUUID("uuid"))
                tag.putUUID("uuid", UUID.randomUUID());
        }
        this.unitItems.set(index, newStack);
        syncToClient();
    }

    @Override
    public void swapSlots(int index1, int index2) {
        Objects.checkIndex(index1, MAX_INVENTORY_SIZE);
        Objects.checkIndex(index2, MAX_INVENTORY_SIZE);
        if (index1 == index2) return;
        ItemStack tmp = this.unitItems.get(index1);
        this.unitItems.set(index1, this.unitItems.get(index2));
        this.unitItems.set(index2, tmp);
        syncToClient();
    }

    @Override
    public boolean dropUUID(UUID uuid, BlockPos bp) {
        for (int i = 0; i < unitItems.size(); i++) {
            ItemStack stack = get(i);
            CompoundTag tag = ItemTagCompat.tag(stack);
            if (stack != null && tag != null && stack.getItem() != Items.AIR) {
                UUID stackuuid = tag.getUUID("uuid");
                if (stackuuid.equals(uuid) && !stack.isEmpty()) {
                    if (EnchantmentUtil.hasBindingCurse(stack))
                        return false;
                    BehaviorUtils.throwItem(this, stack, bp.getCenter(), new Vec3(0.25f, 0.25f, 0.25f), 0.3F);
                    this.unitItems.set(i, ItemStack.EMPTY);
                    syncToClient();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean deleteItem(UUID uuid) {
        for (int i = 0; i < unitItems.size(); i++) {
            ItemStack stack = get(i);
            CompoundTag tag = ItemTagCompat.tag(stack);
            if (stack != null && tag != null && stack.getItem() != Items.AIR) {
                UUID stackuuid = tag.getUUID("uuid");
                if (stackuuid.equals(uuid) && !stack.isEmpty()) {
                    if (EnchantmentUtil.hasBindingCurse(stack))
                        return false;
                    this.unitItems.set(i, ItemStack.EMPTY);
                    if (this instanceof Unit heroUnit && heroUnit.isHero())
                        heroUnit.setStatsForLevel();
                    syncToClient();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean tryAdding(ItemStack newItemStack) {
        if (newItemStack == null || newItemStack.isEmpty())
            return false;
        for (int i = 0; i < getAllItems().size(); i++) {
            if (getAllItems().get(i).getItem() == Items.AIR) {
                set(i, newItemStack);
                syncToClient();
                return true;
            }
        }
        return false;
    }

    @Override
    public void giveTo(UUID uuid, UnitInventory inv) {
        ItemStack itemStack = get(uuid);
        if (itemStack != null && !EnchantmentUtil.hasBindingCurse(itemStack)) {
            if (inv.tryAdding(get(uuid))) {
                this.deleteItem(uuid);
                ItemEntity itemEntity = this.spawnAtLocation(itemStack);
                if (itemEntity != null) {
                    ((LivingEntity) inv).take(itemEntity, itemStack.getCount());
                    itemEntity.discard();
                }
            }
        }
    }

    private void syncToClient() {
        if (!this.level().isClientSide())
            ItemClientboundPacket.syncInventory(this.getId(), getAllItems());
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void ron$saveUnitItems(CompoundTag tag, CallbackInfo ci) {
        // only mobs that actually carry something write the block, so the tag stays off every
        // other mob in the world
        if (isEmpty()) {
            tag.remove(RON$UNIT_ITEMS_KEY);
            return;
        }
        ListTag list = new ListTag();
        for (ItemStack stack : this.unitItems) {
            CompoundTag itemTag = new CompoundTag();
            if (!stack.isEmpty())
                stack.save(this.level().registryAccess(), itemTag);
            list.add(itemTag);
        }
        tag.put(RON$UNIT_ITEMS_KEY, list);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void ron$readUnitItems(CompoundTag tag, CallbackInfo ci) {
        if (!tag.contains(RON$UNIT_ITEMS_KEY, Tag.TAG_LIST)) return;
        ListTag list = tag.getList(RON$UNIT_ITEMS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < this.unitItems.size(); i++) {
            ItemStack stack = i < list.size() ? ItemStack.parseOptional(this.level().registryAccess(), list.getCompound(i)) : ItemStack.EMPTY;
            this.unitItems.set(i, stack);
        }
    }

    @Inject(method = "dropCustomDeathLoot", at = @At("RETURN"))
    // 1.21.1 passes the ServerLevel instead of the looting level.
    private void ron$dropUnitItemsOnDeath(ServerLevel pLevel, DamageSource source, boolean recentlyHit, CallbackInfo ci) {
        if (Unit.isHero(this)) return; // heroes keep their gear

        for (int i = 0; i < this.unitItems.size(); i++) {
            ItemStack stack = this.unitItems.get(i);
            if (!stack.isEmpty())
                this.spawnAtLocation(stack);
            this.unitItems.set(i, ItemStack.EMPTY);
        }
    }
}
