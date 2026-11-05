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
 *   <li>料：食材、调料（有哪条配方把它写在调料里的，盐也算）按放进去的顺序摞成一层层，和最上面那层一样就摞上去。
 *       食材和调料互不相干（2026-11-03 用户）：食材随时能拿出来补救，最后放进去的先出来（中间隔着调料也照拿）；
 *       调料倒进去就拿不出来，只有把锅里的液体全舀走时跟着没了；拆灶时只掉食材，调料不掉（盐也不掉）；</li>
 *   <li>正在煮哪条配方、煮了多少（按不盖锅盖的时长算，盖着盖走得快）；</li>
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
    /** 锅里的料（见类注释），最后一层是栈顶。最多放几个食材、几份调料由配方定（{@code BigPotRecipes}），见 {@link BigPotCooking} */
    private final List<Layer> layers = new ArrayList<>();
    /** {@link #contents()} 合起来的样子，锅里的东西一变就作废 */
    @Nullable
    private List<ItemStack> merged;
    @Nullable
    private ResourceLocation cooking;
    /** 煮了多少：按不盖锅盖的时长算的刻数（盖着锅盖每刻多走一些，见 {@link #tickCooking}） */
    private float cookProgress;
    /** 这一锅不盖锅盖要煮多少刻，给 Jade 算进度 */
    private int cookTotal;
    /** 再过几刻把进度发给客户端（不存档） */
    private int syncCountdown;
    private ItemStack result = ItemStack.EMPTY;
    private int servings;
    private ItemStack container = ItemStack.EMPTY;
    private float experience;
    /**
     * 盖在锅上的那个木锅盖，原样存着（耐久、附魔、改的名字都在；2026-11-04 锅盖有了耐久，用户问大锅能不能保存锅盖的耐久）。
     * 揭下来、拆灶时还回去的就是它；以前的存档里没有（空），由 BigStoveBlock 给个新的
     */
    private ItemStack lid = ItemStack.EMPTY;

    /** 水面高度（像素，整口灶 32×32 的坐标里）：1000 / 2000 / 3000 / 4000 mB，都避开锅里台阶的整数高度免得闪 */
    private static final float[] LIQUID_SURFACE_Y = {13.5F, 14.5F, 15.5F, 16.25F};

    /** 栈里的一层：一堆同样的东西，是不是调料 */
    private static final class Layer {
        final ItemStack stack;
        final boolean seasoning;

        Layer(ItemStack stack, boolean seasoning) {
            this.stack = stack;
            this.seasoning = seasoning;
        }
    }

    public BigPotBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIG_POT.get(), pos, state);
    }

    /** 盖上锅盖时存下这一个（只存一个） */
    public void putLid(ItemStack stack) {
        lid = stack.copyWithCount(1);
        setChanged();
    }

    /** 揭盖、拆灶时拿走存着的锅盖；没存过（以前的存档）就是空的 */
    public ItemStack takeLid() {
        ItemStack out = lid;
        lid = ItemStack.EMPTY;
        setChanged();
        return out;
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
    /*@Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return renderBounds();
    }*/
    //?}

    // ---------------------------------------------------------------- 读

    public Fluid liquid() {
        return liquid;
    }

    public int liquidMb() {
        return liquidMb;
    }

    /**
     * 锅里的料合在一起看：食材按放进去的先后、同样的并成一堆，后面跟着调料（同一种调料只认物品种类）。
     * 凑配方（一样一堆、个数对上）、画锅里的东西、Jade 都用这个；不能改
     */
    public List<ItemStack> contents() {
        if (merged == null) {
            List<ItemStack> food = new ArrayList<>();
            List<ItemStack> spice = new ArrayList<>();
            for (Layer layer : layers) {
                mergeInto(layer.seasoning ? spice : food, layer.stack, layer.seasoning);
            }
            food.addAll(spice);
            merged = Collections.unmodifiableList(food);
        }
        return merged;
    }

    private static void mergeInto(List<ItemStack> out, ItemStack stack, boolean byItem) {
        for (ItemStack existing : out) {
            if (byItem ? existing.is(stack.getItem()) : same(existing, stack)) {
                existing.grow(stack.getCount());
                return;
            }
        }
        out.add(stack.copy());
    }

    private static boolean same(ItemStack a, ItemStack b) {
        //? if <1.20.5 {
        /*return ItemStack.isSameItemSameTags(a, b);
        *///?} else {
        return ItemStack.isSameItemSameComponents(a, b);
        //?}
    }

    /** 锅里一共几个食材 */
    public int ingredientCount() {
        return count(false);
    }

    /** 锅里一共倒了几份调料 */
    public int seasoningCount() {
        return count(true);
    }

    private int count(boolean seasoning) {
        int n = 0;
        for (Layer layer : layers) {
            if (layer.seasoning == seasoning) {
                n += layer.stack.getCount();
            }
        }
        return n;
    }

    @Nullable
    public ResourceLocation cooking() {
        return cooking;
    }

    /** 煮了多少（按不盖锅盖的时长算的刻数，满 {@link #cookTotal()} 就好了） */
    public float cookProgress() {
        return cookProgress;
    }

    public int cookTotal() {
        return cookTotal;
    }

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
        return liquidMb <= 0 && layers.isEmpty() && !hasResult();
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
            dropSeasonings();
        }
        changed();
        return true;
    }

    /** 调料跟着水没了：栈里的调料层去掉，上下挨着的同样食材并回一层 */
    private void dropSeasonings() {
        layers.removeIf(layer -> layer.seasoning);
        for (int i = layers.size() - 1; i > 0; i--) {
            ItemStack below = layers.get(i - 1).stack;
            ItemStack above = layers.get(i).stack;
            if (same(below, above) && below.getCount() + above.getCount() <= below.getMaxStackSize()) {
                below.grow(above.getCount());
                layers.remove(i);
            }
        }
    }

    /**
     * 放一个料：压在栈顶，和栈顶那层一样就摞上去。调料只认物品种类：瓶装的每倒一剂瓶子的耐久都不一样，
     * 不然同一瓶倒两剂会分成两层、凑不成「调料 ×2」
     */
    public void push(ItemStack one, boolean seasoning) {
        if (!layers.isEmpty()) {
            Layer top = layers.get(layers.size() - 1);
            boolean fits = top.seasoning == seasoning && (seasoning ? top.stack.is(one.getItem())
                    : same(top.stack, one) && top.stack.getCount() < top.stack.getMaxStackSize());
            if (fits) {
                top.stack.grow(1);
                changed();
                return;
            }
        }
        layers.add(new Layer(one.copyWithCount(1), seasoning));
        changed();
    }

    /** 拿出最后放进去的那个食材（上面压着调料也照拿，调料留在锅里）；锅里没有食材返回空 */
    public ItemStack popIngredient() {
        for (int i = layers.size() - 1; i >= 0; i--) {
            Layer layer = layers.get(i);
            if (layer.seasoning) {
                continue;
            }
            ItemStack taken = layer.stack.split(1);
            if (layer.stack.isEmpty()) {
                layers.remove(i);
            }
            changed();
            return taken;
        }
        return ItemStack.EMPTY;
    }

    /** 正在煮的配方换了（或者凑不上任何配方）就从头算 */
    public void setCooking(@Nullable ResourceLocation recipe) {
        if (recipe == null ? cooking != null : !recipe.equals(cooking)) {
            cooking = recipe;
            cookProgress = 0;
            if (recipe == null) {
                cookTotal = 0;
            }
            sync();
        }
    }

    /**
     * 煮一刻，进度走 rate（= 不盖盖时长 / 盖盖时长；2026-11-05 起不盖锅盖不煮，见 BigPotCooking#serverTick）。
     * 进度单位还是不盖盖时长的刻数，所以中途揭盖停下、再盖上接着走，已经煮过的不跟着放大
     * （2026-11-03 用户：以前一半多盖上锅盖就直接好了）
     */
    public void tickCooking(float rate) {
        cookProgress += rate;
        if (--syncCountdown <= 0) {
            syncCountdown = 20;
            sync();       // 每秒发一次进度（Jade）
        } else {
            setChanged();
        }
    }

    /**
     * 煮好了：料清空、液体全没了（水都煮进菜里，2026-11-03 用户：煮完锅里不该再有水位），锅里换成一锅菜
     */
    public void finish(ItemStack dish, ItemStack serveWith, float xpPerServing) {
        layers.clear();
        liquidMb = 0;
        liquid = Fluids.EMPTY;
        result = dish.copyWithCount(1);
        servings = dish.getCount();
        container = serveWith.isEmpty() ? ItemStack.EMPTY : serveWith.copyWithCount(1);
        experience = xpPerServing;
        cooking = null;
        cookProgress = 0;
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
        for (Layer layer : layers) {
            if (!layer.seasoning) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, layer.stack.copy());
            }
        }
        liquid = Fluids.EMPTY;
        liquidMb = 0;
        layers.clear();
        cooking = null;
        cookProgress = 0;
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
    /*protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        this.writeToTag(tag, NbtOps.INSTANCE);
    }*/
    //?} else {
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.writeToTag(tag, registries.createSerializationContext(NbtOps.INSTANCE));
    }
    //?}

    /**
     * 物品用带注册表的 ops 编码（1.21.1 上物品组件里有附魔这类引用注册表的东西时，光 NbtOps 编不出来，
     * 会被 result() 悄悄丢掉——锅里的东西存档、同步时就少了）
     */
    private void writeToTag(CompoundTag tag, com.mojang.serialization.DynamicOps<Tag> ops) {
        if (liquidMb > 0) {
            tag.putString("Liquid", BuiltInRegistries.FLUID.getKey(liquid).toString());
            tag.putInt("LiquidMb", liquidMb);
        }
        ListTag list = new ListTag();
        for (Layer layer : layers) {
            ItemStack.CODEC.encodeStart(ops, layer.stack).result().ifPresent(item -> {
                CompoundTag entry = new CompoundTag();
                entry.put("Item", item);
                if (layer.seasoning) {
                    entry.putBoolean("Seasoning", true);
                }
                list.add(entry);
            });
        }
        tag.put("Layers", list);
        if (cooking != null) {
            tag.putString("Cooking", cooking.toString());
        }
        tag.putFloat("CookProgress", cookProgress);
        tag.putInt("CookTotal", cookTotal);
        if (!result.isEmpty()) {
            ItemStack.CODEC.encodeStart(ops, result).result().ifPresent(saved -> tag.put("Result", saved));
            tag.putInt("Servings", servings);
            tag.putFloat("Experience", experience);
        }
        if (!container.isEmpty()) {
            ItemStack.CODEC.encodeStart(ops, container).result().ifPresent(saved -> tag.put("Container", saved));
        }
        if (!lid.isEmpty()) {
            ItemStack.CODEC.encodeStart(ops, lid).result().ifPresent(saved -> tag.put("Lid", saved));
        }
    }

    private static void decode(ListTag list, List<ItemStack> into, com.mojang.serialization.DynamicOps<Tag> ops) {
        for (int i = 0; i < list.size(); i++) {
            ItemStack.CODEC.parse(ops, list.get(i)).result().ifPresent(into::add);
        }
    }

    @Override
    //? if <1.20.5 {
    /*public void load(CompoundTag tag) {
        super.load(tag);
        this.readFromTag(tag, NbtOps.INSTANCE);
    }*/
    //?} else {
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.readFromTag(tag, registries.createSerializationContext(NbtOps.INSTANCE));
    }
    //?}

    private void readFromTag(CompoundTag tag, com.mojang.serialization.DynamicOps<Tag> ops) {
        liquidMb = tag.getInt("LiquidMb");
        ResourceLocation fluidId = tag.contains("Liquid") ? ResourceLocation.tryParse(tag.getString("Liquid")) : null;
        liquid = fluidId != null && BuiltInRegistries.FLUID.containsKey(fluidId) ? BuiltInRegistries.FLUID.get(fluidId) : Fluids.EMPTY;
        if (liquid == Fluids.EMPTY) {
            liquidMb = 0;
        }
        layers.clear();
        merged = null;
        if (tag.contains("Layers")) {
            ListTag list = tag.getList("Layers", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                if (entry.contains("Item")) {
                    ItemStack.CODEC.parse(ops, entry.get("Item")).result()
                            .ifPresent(stack -> layers.add(new Layer(stack, entry.getBoolean("Seasoning"))));
                }
            }
        } else {
            // 以前的存档：先是食材、调料分开两个列表（Ingredients / Seasonings），再早混在一个 Contents 里
            // （倒进去时退回了瓶子 / 碗的算调料）。读进来食材在下、调料压在上面
            List<ItemStack> food = new ArrayList<>();
            List<ItemStack> spice = new ArrayList<>();
            if (tag.contains("Contents")) {
                List<ItemStack> old = new ArrayList<>();
                decode(tag.getList("Contents", Tag.TAG_COMPOUND), old, ops);
                for (ItemStack stack : old) {
                    (stack.hasCraftingRemainingItem() ? spice : food).add(stack);
                }
            } else {
                decode(tag.getList("Ingredients", Tag.TAG_COMPOUND), food, ops);
                decode(tag.getList("Seasonings", Tag.TAG_COMPOUND), spice, ops);
            }
            food.forEach(stack -> layers.add(new Layer(stack, false)));
            spice.forEach(stack -> layers.add(new Layer(stack, true)));
        }
        cooking = tag.contains("Cooking") ? ResourceLocation.tryParse(tag.getString("Cooking")) : null;
        cookProgress = tag.contains("CookProgress") ? tag.getFloat("CookProgress") : tag.getInt("CookTicks");
        cookTotal = tag.getInt("CookTotal");
        result = tag.contains("Result")
                ? ItemStack.CODEC.parse(ops, tag.get("Result")).result().orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        servings = result.isEmpty() ? 0 : tag.getInt("Servings");
        experience = tag.getFloat("Experience");
        container = tag.contains("Container")
                ? ItemStack.CODEC.parse(ops, tag.get("Container")).result().orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        lid = tag.contains("Lid")
                ? ItemStack.CODEC.parse(ops, tag.get("Lid")).result().orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    //? if <1.20.5 {
    /*public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        this.writeToTag(tag, NbtOps.INSTANCE);
        return tag;
    }*/
    //?} else {
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        this.writeToTag(tag, registries.createSerializationContext(NbtOps.INSTANCE));
        return tag;
    }
    //?}
}
