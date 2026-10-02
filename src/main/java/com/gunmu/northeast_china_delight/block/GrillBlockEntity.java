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

import java.util.ArrayList;
import java.util.List;

/**
 * 烤架营火的方块实体。
 *
 * <p>一个烤架能放 **4 份食材**（什么都能放，可以混着放），每一份各自记：
 * <ul>
 *     <li>食材本身（{@link #items}）；</li>
 *     <li>刷在这份食材上的调料（{@link #seasonings}，每份最多 {@link #SEASONINGS_PER_SLOT} 样）；</li>
 *     <li>烤制进度（{@link #progress}）。</li>
 * </ul>
 *
 * <p><b>为什么不直接用原版的 {@code CampfireBlockEntity}</b>：
 * 方块实体的类型会校验「这个方块是不是我的合法方块」（{@code BlockEntityType.isValid}），
 * 而 `minecraft:campfire` 那个类型只登记了原版的营火。
 */
public class GrillBlockEntity extends BlockEntity {

    /** 一个烤架能放几份食材 */
    public static final int SLOTS = 4;
    /** 每份食材最多刷几样调料 */
    public static final int SEASONINGS_PER_SLOT = 3;

    /** 四份食材在方块里的水平偏移（和原版营火摆 4 份食物的位置一致） */
    private static final float CORNER = 0.3125F;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    /** 按「第几份食材 × 调料位」铺平存放，方便直接存档 */
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

    /** 第 slot 份食材上已经刷了的调料种类 */
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

    /** 给第 slot 份食材记一样调料；这一份的调料位满了返回 false */
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

    /** 把第 slot 份食材连同它身上的调料一起清掉（取走 / 烤好时用） */
    public void clearSlot(int slot) {
        this.items.set(slot, ItemStack.EMPTY);
        int base = slot * SEASONINGS_PER_SLOT;
        for (int i = 0; i < SEASONINGS_PER_SLOT; i++) {
            this.seasonings.set(base + i, ItemStack.EMPTY);
        }
        this.progress[slot] = 0;
        setChanged();
    }

    /** 架烤架时把普通营火上正在烤的东西搬过来 */
    public void setItemsFrom(List<ItemStack> source) {
        for (int i = 0; i < this.items.size(); i++) {
            this.items.set(i, i < source.size() ? source.get(i).copy() : ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.seasonings.clear();
        if (tag.contains("Seasonings")) {
            ContainerHelper.loadAllItems(tag.getCompound("Seasonings"), this.seasonings, registries);
        }
        if (tag.contains("GrillProgress")) {
            int[] saved = tag.getIntArray("GrillProgress");
            System.arraycopy(saved, 0, this.progress, 0, Math.min(saved.length, this.progress.length));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, true, registries);
        CompoundTag seasoningsTag = new CompoundTag();
        ContainerHelper.saveAllItems(seasoningsTag, this.seasonings, true, registries);
        tag.put("Seasonings", seasoningsTag);
        tag.putIntArray("GrillProgress", this.progress);
    }

    /** 食材和调料都要同步给客户端 —— 渲染那层「刷上去的颜色」要用 */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, this.items, true, registries);
        CompoundTag seasoningsTag = new CompoundTag();
        ContainerHelper.saveAllItems(seasoningsTag, this.seasonings, true, registries);
        tag.put("Seasonings", seasoningsTag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
