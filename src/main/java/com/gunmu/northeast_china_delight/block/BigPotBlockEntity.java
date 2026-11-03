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
 *   <li>食材：一个栈，按放进去的顺序摞（和栈顶那堆一样就摞上去），只能从栈顶拿出来（最后放进去的先出来）；
 *       拆灶时掉出来；</li>
 *   <li>调料（有哪条配方把它写在调料里，盐也算）：同样的摞成一堆，倒进去就拿不出来；锅里的液体全舀走时跟着没了，
 *       拆灶时也不掉（2026-11-03 用户：调料随水消失、锅被破坏不返还调料，盐也不掉）；</li>
 *   <li>正在煮哪条配方、煮了多久；</li>
 *   <li>煮好的菜、还剩几份、得用什么盛、每盛一份给多少经验（拆灶时没盛完的就没了）。</li>
 * </ul>
 *
 * <p>锅里有什么会同步给客户端（右键时客户端要先知道锅里煮好了没、该不该让给手里的东西，锅里的东西也要画出来，
 * 见 {@code client/BigPotRenderer}）；烹饪进度不每刻发，每秒发一次（Jade 显示进度条用）。</p>
 */
public class BigPotBlockEntity extends BlockEntity {

    public static final int LIQUID_CAPACITY = 4000;

    private Fluid liquid = Fluids.EMPTY;
    private int liquidMb;
    /** 食材栈：最后一堆是栈顶。最多放几个由配方定（{@code BigPotRecipes.maxIngredients}），见 {@link BigPotCooking} */
    private final List<ItemStack> ingredients = new ArrayList<>();
    /** 倒进去的调料，同一种摞成一堆。最多几份同样由配方定（{@code BigPotRecipes.maxSeasonings}） */
    private final List<ItemStack> seasonings = new ArrayList<>();
    /** {@link #contents()} 合起来的样子，锅里的东西一变就作废 */
    @Nullable
    private List<ItemStack> merged;
    @Nullable
    private ResourceLocation cooking;
    private int cookTicks;
    /** 这一锅一共要煮多少刻（盖着锅盖时是缩短后的），给 Jade 算进度 */
    private int cookTotal;
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

    /**
     * 锅里的料合在一起看：食材按放进去的先后、同样的并成一堆，后面跟着调料。凑配方（一样一堆、个数对上）、
     * 画锅里的东西、Jade 都用这个；不能改
     */
    public List<ItemStack> contents() {
        if (merged == null) {
            List<ItemStack> out = new ArrayList<>();
            for (ItemStack stack : ingredients) {
                mergeInto(out, stack);
            }
            for (ItemStack stack : seasonings) {
                mergeInto(out, stack);
            }
            merged = Collections.unmodifiableList(out);
        }
        return merged;
    }

    private static void mergeInto(List<ItemStack> out, ItemStack stack) {
        for (ItemStack existing : out) {
            if (same(existing, stack)) {
                existing.grow(stack.getCount());
                return;
            }
        }
        out.add(stack.copy());
    }

    private static boolean same(ItemStack a, ItemStack b) {
        //? if <1.20.5 {
        return ItemStack.isSameItemSameTags(a, b);
        //?} else {
        /*return ItemStack.isSameItemSameComponents(a, b);
        *///?}
    }

    /** 锅里一共几个食材 */
    public int ingredientCount() {
        int n = 0;
        for (ItemStack stack : ingredients) {
            n += stack.getCount();
        }
        return n;
    }

    /** 锅里一共倒了几份调料 */
    public int seasoningCount() {
        int n = 0;
        for (ItemStack stack : seasonings) {
            n += stack.getCount();
        }
        return n;
    }

    @Nullable
    public ResourceLocation cooking() {
        return cooking;
    }

    public int cookTicks() {
        return cookTicks;
    }

    public int cookTotal() {
        return cookTotal;
    }

    /** 盖上 / 揭开锅盖时总时间会变 */
    public void setCookTotal(int total) {
        if (total != cookTotal) {
            cookTotal = total;
            sync();
        }
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
        return liquidMb <= 0 && ingredients.isEmpty() && seasonings.isEmpty() && !hasResult();
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

    /**
     * 舀走液体：够量才舀。舀干了，倒进去的调料跟着水一起没了（舀出来的是清水，调料不还）：
     * 调料放错了就把水舀走再倒回来，等于换一锅清水（2026-11-03 用户）
     */
    public boolean removeLiquid(int mb) {
        if (liquidMb < mb) {
            return false;
        }
        liquidMb -= mb;
        if (liquidMb <= 0) {
            liquidMb = 0;
            liquid = Fluids.EMPTY;
            seasonings.clear();
        }
        changed();
        return true;
    }

    /** 放一个食材：压在栈顶（和栈顶那堆一样、还摞得下就摞上去，不然另起一堆） */
    public void pushIngredient(ItemStack one) {
        if (!ingredients.isEmpty()) {
            ItemStack top = ingredients.get(ingredients.size() - 1);
            if (same(top, one) && top.getCount() < top.getMaxStackSize()) {
                top.grow(1);
                changed();
                return;
            }
        }
        ingredients.add(one.copyWithCount(1));
        changed();
    }

    /**
     * 倒一份调料：同一种摞成一堆。只认物品种类：瓶装的每倒一剂瓶子的耐久都不一样，
     * 不然同一瓶倒两剂会分成两堆、凑不成「调料 ×2」
     */
    public void addSeasoning(ItemStack one) {
        for (ItemStack stack : seasonings) {
            if (stack.is(one.getItem())) {
                stack.grow(1);
                changed();
                return;
            }
        }
        seasonings.add(one.copyWithCount(1));
        changed();
    }

    /** 从栈顶拿出一个食材（最后放进去的那个）；没有食材了返回空。调料拿不出来 */
    public ItemStack popIngredient() {
        if (ingredients.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack top = ingredients.get(ingredients.size() - 1);
        ItemStack taken = top.split(1);
        if (top.isEmpty()) {
            ingredients.remove(ingredients.size() - 1);
        }
        changed();
        return taken;
    }

    /** 正在煮的配方换了（或者凑不上任何配方）就从头算 */
    public void setCooking(@Nullable ResourceLocation recipe) {
        if (recipe == null ? cooking != null : !recipe.equals(cooking)) {
            cooking = recipe;
            cookTicks = 0;
            if (recipe == null) {
                cookTotal = 0;
            }
            sync();
        }
    }

    public void tickCooking() {
        cookTicks++;
        if (cookTicks % 20 == 0) {
            sync();       // 每秒发一次进度（Jade）
        } else {
            setChanged();
        }
    }

    /** 煮好了：料清空、液体扣掉配方用的那么多，锅里换成一锅菜 */
    public void finish(ItemStack dish, ItemStack serveWith, int liquidUsed, float xpPerServing) {
        ingredients.clear();
        seasonings.clear();
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
        cookTotal = 0;
        changed();
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

    /**
     * 灶被拆时：只有还没煮的食材掉在 pos；液体没了，调料跟着没（盐也不掉）；煮好没盛完的菜也没了，
     * 不会掉出盆装菜（2026-11-03 用户：煮好以后打掉锅应该只掉锅）
     */
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : ingredients) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack.copy());
        }
        liquid = Fluids.EMPTY;
        liquidMb = 0;
        ingredients.clear();
        seasonings.clear();
        cooking = null;
        cookTicks = 0;
        result = ItemStack.EMPTY;
        container = ItemStack.EMPTY;
        servings = 0;
        changed();
    }

    /** 料变了：合起来的样子作废，存档 + 发给客户端 */
    private void changed() {
        merged = null;
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
        this.writeToTag(tag, NbtOps.INSTANCE);
    }
    //?} else {
    /*protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.writeToTag(tag, registries.createSerializationContext(NbtOps.INSTANCE));
    }
    *///?}

    /**
     * 物品用带注册表的 ops 编码（1.21.1 上物品组件里有附魔这类引用注册表的东西时，光 NbtOps 编不出来，
     * 会被 result() 悄悄丢掉——锅里的东西存档、同步时就少了）
     */
    private void writeToTag(CompoundTag tag, com.mojang.serialization.DynamicOps<Tag> ops) {
        if (liquidMb > 0) {
            tag.putString("Liquid", BuiltInRegistries.FLUID.getKey(liquid).toString());
            tag.putInt("LiquidMb", liquidMb);
        }
        tag.put("Ingredients", encode(ingredients, ops));
        tag.put("Seasonings", encode(seasonings, ops));
        if (cooking != null) {
            tag.putString("Cooking", cooking.toString());
        }
        tag.putInt("CookTicks", cookTicks);
        tag.putInt("CookTotal", cookTotal);
        if (!result.isEmpty()) {
            ItemStack.CODEC.encodeStart(ops, result).result().ifPresent(saved -> tag.put("Result", saved));
            tag.putInt("Servings", servings);
            tag.putFloat("Experience", experience);
        }
        if (!container.isEmpty()) {
            ItemStack.CODEC.encodeStart(ops, container).result().ifPresent(saved -> tag.put("Container", saved));
        }
    }

    private static ListTag encode(List<ItemStack> stacks, com.mojang.serialization.DynamicOps<Tag> ops) {
        ListTag list = new ListTag();
        for (ItemStack stack : stacks) {
            ItemStack.CODEC.encodeStart(ops, stack).result().ifPresent(list::add);
        }
        return list;
    }

    private static void decode(ListTag list, List<ItemStack> into, com.mojang.serialization.DynamicOps<Tag> ops) {
        for (int i = 0; i < list.size(); i++) {
            ItemStack.CODEC.parse(ops, list.get(i)).result().ifPresent(into::add);
        }
    }

    @Override
    //? if <1.20.5 {
    public void load(CompoundTag tag) {
        super.load(tag);
        this.readFromTag(tag, NbtOps.INSTANCE);
    }
    //?} else {
    /*protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.readFromTag(tag, registries.createSerializationContext(NbtOps.INSTANCE));
    }
    *///?}

    private void readFromTag(CompoundTag tag, com.mojang.serialization.DynamicOps<Tag> ops) {
        liquidMb = tag.getInt("LiquidMb");
        ResourceLocation fluidId = tag.contains("Liquid") ? ResourceLocation.tryParse(tag.getString("Liquid")) : null;
        liquid = fluidId != null && BuiltInRegistries.FLUID.containsKey(fluidId) ? BuiltInRegistries.FLUID.get(fluidId) : Fluids.EMPTY;
        if (liquid == Fluids.EMPTY) {
            liquidMb = 0;
        }
        ingredients.clear();
        seasonings.clear();
        merged = null;
        if (tag.contains("Contents")) {
            // 这一版以前的存档食材、调料混在一个 Contents 里：倒进去时退回了瓶子 / 碗的算调料，其余当食材
            List<ItemStack> old = new ArrayList<>();
            decode(tag.getList("Contents", Tag.TAG_COMPOUND), old, ops);
            for (ItemStack stack : old) {
                (stack.hasCraftingRemainingItem() ? seasonings : ingredients).add(stack);
            }
        } else {
            decode(tag.getList("Ingredients", Tag.TAG_COMPOUND), ingredients, ops);
            decode(tag.getList("Seasonings", Tag.TAG_COMPOUND), seasonings, ops);
        }
        cooking = tag.contains("Cooking") ? ResourceLocation.tryParse(tag.getString("Cooking")) : null;
        cookTicks = tag.getInt("CookTicks");
        cookTotal = tag.getInt("CookTotal");
        result = tag.contains("Result")
                ? ItemStack.CODEC.parse(ops, tag.get("Result")).result().orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        servings = result.isEmpty() ? 0 : tag.getInt("Servings");
        experience = tag.getFloat("Experience");
        container = tag.contains("Container")
                ? ItemStack.CODEC.parse(ops, tag.get("Container")).result().orElse(ItemStack.EMPTY)
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
        this.writeToTag(tag, NbtOps.INSTANCE);
        return tag;
    }
    //?} else {
    /*public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        this.writeToTag(tag, registries.createSerializationContext(NbtOps.INSTANCE));
        return tag;
    }
    *///?}
}
