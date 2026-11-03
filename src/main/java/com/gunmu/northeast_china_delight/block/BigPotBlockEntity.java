package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 大铁锅里装的东西。只挂在大灶 nw 那一格上（{@link BigStoveBlock#newBlockEntity}），四格共用这一份；
 * 怎么往里放、怎么煮、怎么盛见 {@link BigPotCooking}。
 *
 * <ul>
 *   <li>液体：一种流体 + 毫升数（水桶 / 水盆一次倒 1000 mB，最多 {@link #LIQUID_CAPACITY}）；</li>
 *   <li>料：放进去的食材、调料，同样的东西摞成一堆，最多 {@link #MAX_STACKS} 堆。倒进去的调料（瓶装、碗装，
 *       放的时候退回了瓶子 / 碗）拿不回来，拆灶时也不掉；</li>
 *   <li>正在煮哪条配方、煮了多久；</li>
 *   <li>煮好的菜、还剩几份、得用什么盛、每盛一份给多少经验。</li>
 * </ul>
 *
 * <p>锅里有什么会同步给客户端（右键时客户端要先知道锅里煮好了没、该不该让给手里的东西，锅里的东西也要画出来，
 * 见 {@code client/BigPotRenderer}），只有每刻的烹饪进度不发。</p>
 */
public class BigPotBlockEntity extends BlockEntity {

    public static final int LIQUID_CAPACITY = 4000;
    /** 食材 6 格 + 调料 3 格 */
    public static final int MAX_STACKS = 9;

    private Fluid liquid = Fluids.EMPTY;
    private int liquidMb;
    private final List<ItemStack> contents = new ArrayList<>();
    @Nullable
    private ResourceLocation cooking;
    private int cookTicks;
    private ItemStack result = ItemStack.EMPTY;
    private int servings;
    private ItemStack container = ItemStack.EMPTY;
    private float experience;

    /** 水面高度（像素，整口灶 32×32 的坐标里）：1000 / 2000 / 3000 / 4000 mB，都避开锅里台阶的整数高度免得闪 */
    private static final float[] LIQUID_SURFACE_Y = {13.5F, 14.5F, 15.5F, 16.25F};

    public BigPotBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIG_POT.get(), pos, state);
    }

    /** 锅里装 mb 毫升液体时液面多高（像素）；画液面、放料溅水花都用这个 */
    public static float liquidSurfaceY(int mb) {
        int n = Math.max(1, Math.min(LIQUID_SURFACE_Y.length, (mb + 999) / 1000));
        return LIQUID_SURFACE_Y[n - 1];
    }

    /** 渲染范围：整口灶 2×2、往上一格（锅里的东西会画到别的格子上） */
    public net.minecraft.world.phys.AABB renderBounds() {
        BlockState state = getBlockState();
        BlockPos origin = state.getBlock() instanceof BigStoveBlock ? BigStoveBlock.origin(worldPosition, state)
                : worldPosition;
        return new net.minecraft.world.phys.AABB(origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + 2, origin.getY() + 2, origin.getZ() + 2);
    }

    //? if <1.20.2 {
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return renderBounds();
    }
    //?}

    // ---------------------------------------------------------------- 读

    public Fluid liquid() {
        return liquid;
    }

    public int liquidMb() {
        return liquidMb;
    }

    public List<ItemStack> contents() {
        return Collections.unmodifiableList(contents);
    }

    @Nullable
    public ResourceLocation cooking() {
        return cooking;
    }

    public int cookTicks() {
        return cookTicks;
    }

    public boolean hasResult() {
        return !result.isEmpty() && servings > 0;
    }

    public ItemStack result() {
        return result;
    }

    public int servings() {
        return servings;
    }

    public ItemStack container() {
        return container;
    }

    /** 锅里什么都没有（没液体、没料、没有煮好的菜） */
    public boolean isEmpty() {
        return liquidMb <= 0 && contents.isEmpty() && !hasResult();
    }

    // ---------------------------------------------------------------- 改

    /** 倒液体：锅里没液体或者是同一种、而且倒得下才行 */
    public boolean addLiquid(Fluid fluid, int mb) {
        if (liquidMb > 0 && !liquid.isSame(fluid) || liquidMb + mb > LIQUID_CAPACITY) {
            return false;
        }
        liquid = fluid;
        liquidMb += mb;
        sync();
        return true;
    }

    /** 舀走液体：够量才舀 */
    public boolean removeLiquid(int mb) {
        if (liquidMb < mb) {
            return false;
        }
        liquidMb -= mb;
        if (liquidMb <= 0) {
            liquidMb = 0;
            liquid = Fluids.EMPTY;
        }
        sync();
        return true;
    }

    /**
     * 放一个料进锅：和已有的同样东西摞在一起，摞不下或者堆数满了返回 false。
     * 倒进去的调料（瓶装的每倒一剂瓶子的耐久都不一样）只认物品种类，不然同一瓶倒两剂会分成两堆、凑不成「调料 ×2」。
     */
    public boolean addItem(ItemStack one) {
        boolean poured = one.hasCraftingRemainingItem();
        for (ItemStack stack : contents) {
            boolean same = poured
                    ? stack.is(one.getItem()) && stack.hasCraftingRemainingItem()
                    //? if <1.20.5 {
                    : ItemStack.isSameItemSameTags(stack, one);
                    //?} else {
                    /*: ItemStack.isSameItemSameComponents(stack, one);
                    *///?}
            if (same) {
                if (stack.getCount() >= stack.getMaxStackSize()) {
                    return false;
                }
                stack.grow(1);
                sync();
                return true;
            }
        }
        if (contents.size() >= MAX_STACKS) {
            return false;
        }
        contents.add(one.copyWithCount(1));
        sync();
        return true;
    }

    /** 拿出最后放进去的那堆里的一个；倒进去的调料（有剩余物的）拿不出来，返回空 */
    public ItemStack takeLastItem() {
        if (contents.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack last = contents.get(contents.size() - 1);
        if (last.hasCraftingRemainingItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = last.split(1);
        if (last.isEmpty()) {
            contents.remove(contents.size() - 1);
        }
        sync();
        return taken;
    }

    /** 正在煮的配方换了（或者凑不上任何配方）就从头算 */
    public void setCooking(@Nullable ResourceLocation recipe) {
        if (recipe == null ? cooking != null : !recipe.equals(cooking)) {
            cooking = recipe;
            cookTicks = 0;
            sync();
        }
    }

    public void tickCooking() {
        cookTicks++;
        setChanged();     // 每刻的进度不往客户端发
    }

    /** 煮好了：料清空、液体扣掉配方用的那么多，锅里换成一锅菜 */
    public void finish(ItemStack dish, ItemStack serveWith, int liquidUsed, float xpPerServing) {
        contents.clear();
        liquidMb = Math.max(0, liquidMb - liquidUsed);
        if (liquidMb == 0) {
            liquid = Fluids.EMPTY;
        }
        result = dish.copyWithCount(1);
        servings = dish.getCount();
        container = serveWith.isEmpty() ? ItemStack.EMPTY : serveWith.copyWithCount(1);
        experience = xpPerServing;
        cooking = null;
        cookTicks = 0;
        sync();
    }

    /** 盛走一份，返回这一份的菜（经验另算） */
    public ItemStack serve() {
        if (!hasResult()) {
            return ItemStack.EMPTY;
        }
        ItemStack dish = result.copyWithCount(1);
        servings--;
        if (servings <= 0) {
            result = ItemStack.EMPTY;
            container = ItemStack.EMPTY;
            servings = 0;
        }
        sync();
        return dish;
    }

    public float experience() {
        return experience;
    }

    /** 灶被拆时：没倒进去的料、没盛完的菜掉在 pos（液体就没了） */
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : contents) {
            if (!stack.hasCraftingRemainingItem()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack.copy());
            }
        }
        for (int i = 0; i < servings && !result.isEmpty(); i++) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, result.copyWithCount(1));
        }
        liquid = Fluids.EMPTY;
        liquidMb = 0;
        contents.clear();
        cooking = null;
        cookTicks = 0;
        result = ItemStack.EMPTY;
        container = ItemStack.EMPTY;
        servings = 0;
        sync();
    }

    /** 存档 + 发给附近的客户端 */
    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ---------------------------------------------------------------- 存档

    @Override
    //? if <1.20.5 {
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        this.writeToTag(tag);
    }
    //?} else {
    /*protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.writeToTag(tag);
    }
    *///?}

    private void writeToTag(CompoundTag tag) {
        if (liquidMb > 0) {
            tag.putString("Liquid", BuiltInRegistries.FLUID.getKey(liquid).toString());
            tag.putInt("LiquidMb", liquidMb);
        }
        ListTag list = new ListTag();
        for (ItemStack stack : contents) {
            ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).result().ifPresent(list::add);
        }
        tag.put("Contents", list);
        if (cooking != null) {
            tag.putString("Cooking", cooking.toString());
        }
        tag.putInt("CookTicks", cookTicks);
        if (!result.isEmpty()) {
            ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, result).result().ifPresent(saved -> tag.put("Result", saved));
            tag.putInt("Servings", servings);
            tag.putFloat("Experience", experience);
        }
        if (!container.isEmpty()) {
            ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, container).result().ifPresent(saved -> tag.put("Container", saved));
        }
    }

    @Override
    //? if <1.20.5 {
    public void load(CompoundTag tag) {
        super.load(tag);
        this.readFromTag(tag);
    }
    //?} else {
    /*protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.readFromTag(tag);
    }
    *///?}

    private void readFromTag(CompoundTag tag) {
        liquidMb = tag.getInt("LiquidMb");
        ResourceLocation fluidId = tag.contains("Liquid") ? ResourceLocation.tryParse(tag.getString("Liquid")) : null;
        liquid = fluidId != null && BuiltInRegistries.FLUID.containsKey(fluidId) ? BuiltInRegistries.FLUID.get(fluidId) : Fluids.EMPTY;
        if (liquid == Fluids.EMPTY) {
            liquidMb = 0;
        }
        contents.clear();
        ListTag list = tag.getList("Contents", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack.CODEC.parse(NbtOps.INSTANCE, list.get(i)).result().ifPresent(contents::add);
        }
        cooking = tag.contains("Cooking") ? ResourceLocation.tryParse(tag.getString("Cooking")) : null;
        cookTicks = tag.getInt("CookTicks");
        result = tag.contains("Result")
                ? ItemStack.CODEC.parse(NbtOps.INSTANCE, tag.get("Result")).result().orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        servings = result.isEmpty() ? 0 : tag.getInt("Servings");
        experience = tag.getFloat("Experience");
        container = tag.contains("Container")
                ? ItemStack.CODEC.parse(NbtOps.INSTANCE, tag.get("Container")).result().orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    //? if <1.20.5 {
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        this.writeToTag(tag);
        return tag;
    }
    //?} else {
    /*public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        this.writeToTag(tag);
        return tag;
    }
    *///?}
}
