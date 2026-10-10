package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.gunmu.northeast_china_delight.util.DdNbt;

import java.util.ArrayList;
import java.util.List;

/**
 * 烤架营火的方块实体。
 */
public class GrillBlockEntity extends BlockEntity {

    public static final int SLOTS = 4;
    public static final int SEASONINGS_PER_SLOT = 4;

    private static final float CORNER = 0.3125F;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final NonNullList<ItemStack> seasonings =
            NonNullList.withSize(SLOTS * SEASONINGS_PER_SLOT, ItemStack.EMPTY);
    private final int[] progress = new int[SLOTS];

    public GrillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRILL_CAMPFIRE.get(), pos, state);
    }

    /**
     * 第 {@code slot} 份食材在方块里的水平偏移，和原版营火摆 4 份食物的算法一致。
     * 渲染和「右键的是哪一份」都靠它，两处必须用同一个公式。
     */
    public static float[] slotOffset(int slot, Direction facing) {
        Direction direction = Direction.from2DDataValue(Math.floorMod(slot + facing.get2DDataValue(), 4));
        return new float[] {
                -direction.getStepX() * CORNER + direction.getClockWise().getStepX() * CORNER,
                -direction.getStepZ() * CORNER + direction.getClockWise().getStepZ() * CORNER
        };
    }

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    public List<Item> seasoningsOf(int slot) {
        List<Item> list = new ArrayList<>();
        int base = slot * SEASONINGS_PER_SLOT;
        for (int i = 0; i < SEASONINGS_PER_SLOT; i++) {
            ItemStack stack = this.seasonings.get(base + i);
            if (!stack.isEmpty()) {
                list.add(stack.getItem());
            }
        }
        return list;
    }

    public boolean addSeasoning(int slot, ItemStack stack) {
        int base = slot * SEASONINGS_PER_SLOT;
        for (int i = 0; i < SEASONINGS_PER_SLOT; i++) {
            if (this.seasonings.get(base + i).isEmpty()) {
                this.seasonings.set(base + i, new ItemStack(stack.getItem()));
                setChanged();
                return true;
            }
        }
        return false;
    }

    public int progress(int slot) {
        return this.progress[slot];
    }

    public void setProgress(int slot, int value) {
        if (this.progress[slot] != value) {
            this.progress[slot] = value;
            setChanged();
        }
    }

    public void clearSlot(int slot) {
        this.items.set(slot, ItemStack.EMPTY);
        int base = slot * SEASONINGS_PER_SLOT;
        for (int i = 0; i < SEASONINGS_PER_SLOT; i++) {
            this.seasonings.set(base + i, ItemStack.EMPTY);
        }
        this.progress[slot] = 0;
        setChanged();
    }

    public void setItemsFrom(List<ItemStack> source) {
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, i < source.size() ? source.get(i).copy() : ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    //? if <1.20.5 {
    public void load(CompoundTag tag) {
        super.load(tag);
        this.readFromTag(tag, null);
    }
    //?} else {
    /*protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.readFromTag(tag, registries);
    }
    *///?}

    private void readFromTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.items.clear();
        DdNbt.loadAllItems(tag, this.items, registries);
        this.seasonings.clear();
        if (tag.contains("Seasonings")) {
            DdNbt.loadAllItems(tag.getCompound("Seasonings"), this.seasonings, registries);
        }
        if (tag.contains("GrillProgress")) {
            int[] saved = tag.getIntArray("GrillProgress");
            System.arraycopy(saved, 0, this.progress, 0, Math.min(saved.length, this.progress.length));
        }
    }

    @Override
    //? if <1.20.5 {
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        this.writeToTag(tag, null);
    }
    //?} else {
    /*protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.writeToTag(tag, registries);
    }
    *///?}

    private void writeToTag(CompoundTag tag, HolderLookup.Provider registries) {
        DdNbt.saveAllItems(tag, this.items, true, registries);
        CompoundTag seasoningsTag = new CompoundTag();
        DdNbt.saveAllItems(seasoningsTag, this.seasonings, true, registries);
        tag.put("Seasonings", seasoningsTag);
        tag.putIntArray("GrillProgress", this.progress);
    }

    @Override
    //? if <1.20.5 {
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        this.writeToTag(tag, null);
        return tag;
    }
    //?} else {
    /*public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        this.writeToTag(tag, registries);
        return tag;
    }
    *///?}

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
