package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.crafting.VatRecipes;
import com.gunmu.northeast_china_delight.fluid.ModFluids;
import com.gunmu.northeast_china_delight.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 大缸里的东西：内容物（蔬菜/肉/盐/酱块）、压着的石头、蒙着的羊毛地毯、剩余酱量。
 *
 * 内容物按放入顺序记录，所以「一层肉一层盐」这种层叠关系会被保留下来。
 * 大酱 / 酱油按 mB 记账（1 份 = 250 mB），这样既能用碗、瓶取，也能被流体管道抽走。
 */
public class VatBlockEntity extends BlockEntity implements IFluidHandler {

    /** 内容物条目上限（5 份蔬菜 + 1 份盐，或 5 块肉 + 5 份盐） */
    public static final int MAX_ENTRIES = 12;

    private final NonNullList<ItemStack> contents = NonNullList.withSize(MAX_ENTRIES, ItemStack.EMPTY);
    /** 压缸用的石头（保持原样，取出时还给玩家） */
    private ItemStack press = ItemStack.EMPTY;
    /** 蒙缸用的羊毛地毯 */
    private ItemStack cover = ItemStack.EMPTY;
    /** 大酱剩余量（mB） */
    private int pasteMb;
    /** 酱油剩余量（mB） */
    private int soySauceMb;
    /** 醋剩余量（mB） */
    private int vinegarMb;
    /** 酸引水剩余量（mB，泡菜腌完后由缸里的水转化而来） */
    private int sourWaterMb;
    /** 白醋剩余量（mB） */
    private int whiteVinegarMb;
    /** 鱼露剩余量（mB） */
    private int fishSauceMb;
    /** 虾酱剩余量（mB） */
    private int shrimpPasteMb;
    /** 格瓦斯剩余量（mB） */
    private int kvassMb;
    /**
     * 缸里的**清水**（mB，一层 = 1000 mB）。
     *
     * <p>以前清水直接用方块状态的水位记，而酸引水又会按 mB 折算回同一个水位 ——
     * 两者混用一个字段，才会出现"倒一瓶酸引水当一层水腌菜、腌完水翻倍"这类问题。
     * 现在清水单独记账，水位只是"显示值" = ceil((清水 + 酸引水) / 1000)。
     */
    private int waterMb;
    private VatRecipes.Kind kind = VatRecipes.Kind.NONE;

    public VatBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VAT.get(), pos, state);
    }

    // ===== 查询 =====

    public VatRecipes.Kind kind() {
        return this.kind;
    }

    /** 这一缸是不是已经做完（记在方块状态上，因为渲染 / Jade 都要看它） */
    public boolean isFermented() {
        return this.getBlockState().getValue(Vat.FERMENTED);
    }

    public boolean isEmpty() {
        // 只用盐不算「有东西」：盐已经被吸收了，不该挡住加水或继续操作
        return this.kind == VatRecipes.Kind.NONE && this.pasteMb <= 0 && this.soySauceMb <= 0 && this.vinegarMb <= 0
                && this.sourWaterMb <= 0 && this.whiteVinegarMb <= 0
                && this.fishSauceMb <= 0 && this.shrimpPasteMb <= 0 && this.kvassMb <= 0
                && this.count(stack -> !stack.is(com.gunmu.northeast_china_delight.item.ModItems.SALT.get())) == 0;
    }

    /** 缸里的生鱼数量（任意生鱼） */
    public int rawFishCount() {
        return count(VatRecipes::isRawFish);
    }

    /** 缸里的大虾数量 */
    public int shrimpCount() {
        return countOf(ModItems.SHRIMP.get());
    }

    /** 缸里的黄豆数量（生豆芽用） */
    public int soybeanCount() {
        return countOf(ModItems.SOYBEAN.get());
    }

    /** 缸里的玉米粒数量（发酸玉米粒用） */
    public int cornKernelCount() {
        return countOf(ModItems.CORN_SEEDS.get());
    }

    /** 辣白菜用的辣椒酱数量 */
    public int chiliSauceCount() {
        return countOf(ModItems.CHILI_SAUCE.get());
    }

    /** 辣白菜的调味品数量：鱼露 + 虾酱（盐算在盐水里，不重复计算） */
    public int seasoningCount() {
        return countOf(ModItems.FISH_SAUCE.get()) + countOf(ModItems.SHRIMP_PASTE.get());
    }

    /** 缸里的酱渣数量 */
    public int residueCount() {
        return countOf(ModItems.SOY_RESIDUE.get());
    }

    /** 酿醋用的谷物数量（玉米粒 + 荞麦） */
    public int vinegarGrainCount() {
        return count(VatRecipes::isVinegarGrain);
    }

    /** 成品液体（大酱 / 酱油 / 醋）是否还没取空 */
    public boolean hasProductLiquid() {
        return this.pasteMb > 0 || this.soySauceMb > 0 || this.vinegarMb > 0
                || this.sourWaterMb > 0 || this.whiteVinegarMb > 0
                || this.fishSauceMb > 0 || this.shrimpPasteMb > 0 || this.kvassMb > 0;
    }

    /**
     * 盐是不是已经溶在水里。
     *
     * 泡菜 / 大酱 / 酱油都是「水里加盐」的流程，发酵完成后水被吸收（水位归零），
     * 但盐不该再变回物品，所以这里主要看流程类型；另外只要缸里当下还有水
     * （例如腊肉缸里误加了水），盐也一律按溶解处理。
     */
    public boolean isSaltDissolved() {
        if (this.getBlockState().getValue(Vat.WATER_LEVEL) > 0) {
            return true;
        }
        return this.kind == VatRecipes.Kind.PICKLE
                || this.kind == VatRecipes.Kind.PASTE
                || this.kind == VatRecipes.Kind.SOY_SAUCE;
    }

    /**
     * 缸里「物理上还在」的内容物，按放入顺序返回。
     *
     * 溶解在水里的盐不算内容物（只通过 {@link #countOf} 做数字记账），
     * 所以渲染、破坏掉落以及以后接漏斗抽取都用这个方法；
     * 腊肉那种把盐一层层撒在肉上的做法没有水，盐仍然是物品。
     */
    public List<ItemStack> contents() {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack stack : this.contents) {
            if (stack.isEmpty()) {
                continue;
            }
            if (this.isSaltDissolved() && stack.is(com.gunmu.northeast_china_delight.item.ModItems.SALT.get())) {
                continue;
            }
            list.add(stack);
        }
        return list;
    }

    public int count(Predicate<ItemStack> filter) {
        int n = 0;
        for (ItemStack stack : this.contents) {
            if (!stack.isEmpty() && filter.test(stack)) {
                n++;
            }
        }
        return n;
    }

    /**
     * 缸里**物理上存在的**所有内容物的快照 —— 不过滤"溶在水里的盐"。
     * 完成时按配方处理每一格、取货时清掉调料，用的都是这一份。
     */
    public List<ItemStack> contentSnapshot() {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack stack : this.contents) {
            if (!stack.isEmpty()) {
                list.add(stack.copyWithCount(1));
            }
        }
        return list;
    }

    public int countOf(Item item) {
        return count(stack -> stack.is(item));
    }

    public int vegetableCount() {
        return count(VatRecipes::isPickleIngredient);
    }

    public int meatCount() {
        return countOf(VatRecipes.meatInput());
    }

    public boolean isPressed() {
        return !this.press.isEmpty();
    }

    public ItemStack press() {
        return this.press;
    }

    public boolean isCovered() {
        return !this.cover.isEmpty();
    }

    /** 缸上蒙的是农夫乐事的粗布毯（酿醋 / 发豆芽 / 格瓦斯用），而不是羊毛地毯（大酱 / 酱油用） */
    public boolean isClothCover() {
        return !this.cover.isEmpty() && Vat.isClothRug(this.cover);
    }

    public ItemStack cover() {
        return this.cover;
    }

    /** 大酱剩余碗数（不足一碗时向下取整） */
    public int paste() {
        return this.pasteMb / VatRecipes.SERVING_MB;
    }

    /** 大酱剩余量（mB），流体管道用 */
    public int pasteMb() {
        return this.pasteMb;
    }

    /** 酱油剩余瓶数（不足一瓶时向下取整） */
    public int soySauce() {
        return this.soySauceMb / VatRecipes.SERVING_MB;
    }

    /** 酱油剩余量（mB），流体管道用 */
    public int soySauceMb() {
        return this.soySauceMb;
    }

    /** 醋剩余瓶数（不足一瓶时向下取整） */
    public int vinegar() {
        return this.vinegarMb / VatRecipes.SERVING_MB;
    }

    /** 醋剩余量（mB），流体管道用 */
    public int vinegarMb() {
        return this.vinegarMb;
    }

    /** 酸引水剩余瓶数（不足一瓶时向下取整） */
    public int sourWater() {
        return this.sourWaterMb / VatRecipes.SERVING_MB;
    }

    /** 酸引水剩余量（mB），流体管道用 */
    public int sourWaterMb() {
        return this.sourWaterMb;
    }

    /** 白醋剩余瓶数（不足一瓶时向下取整） */
    public int whiteVinegar() {
        return this.whiteVinegarMb / VatRecipes.SERVING_MB;
    }

    /** 白醋剩余量（mB），流体管道用 */
    public int whiteVinegarMb() {
        return this.whiteVinegarMb;
    }

    /** 鱼露剩余瓶数 */
    public int fishSauce() {
        return this.fishSauceMb / VatRecipes.SERVING_MB;
    }

    /** 鱼露剩余量（mB），流体管道用 */
    public int fishSauceMb() {
        return this.fishSauceMb;
    }

    /** 虾酱剩余瓶数 */
    public int shrimpPaste() {
        return this.shrimpPasteMb / VatRecipes.SERVING_MB;
    }

    /** 虾酱剩余量（mB），流体管道用 */
    public int shrimpPasteMb() {
        return this.shrimpPasteMb;
    }

    /** 格瓦斯剩余瓶数 */
    public int kvass() {
        return this.kvassMb / VatRecipes.SERVING_MB;
    }

    /** 格瓦斯剩余量（mB，格瓦斯暂时不接流体管道，只用来记账和显示） */
    public int kvassMb() {
        return this.kvassMb;
    }

    // ===== 修改 =====

    public void setKind(VatRecipes.Kind kind) {
        this.kind = kind;
        this.sync();
    }

    public boolean addContent(ItemStack stack) {
        for (int i = 0; i < MAX_ENTRIES; i++) {
            if (this.contents.get(i).isEmpty()) {
                this.contents.set(i, stack.copyWithCount(1));
                this.sync();
                return true;
            }
        }
        return false;
    }

    /** 取出一份符合条件的内容物；返回是否取到 */
    public boolean removeOneMatching(Predicate<ItemStack> filter) {
        for (int i = 0; i < MAX_ENTRIES; i++) {
            ItemStack stack = this.contents.get(i);
            if (!stack.isEmpty() && filter.test(stack)) {
                this.contents.set(i, ItemStack.EMPTY);
                this.sync();
                return true;
            }
        }
        return false;
    }

    /** 把一份符合条件的内容物换成另一样东西（完成时"酱块→酱渣"这类变化用） */
    public boolean replaceOneMatching(Predicate<ItemStack> filter, ItemStack replacement) {
        for (int i = 0; i < MAX_ENTRIES; i++) {
            ItemStack stack = this.contents.get(i);
            if (!stack.isEmpty() && filter.test(stack)) {
                this.contents.set(i, replacement.copyWithCount(1));
                this.sync();
                return true;
            }
        }
        return false;
    }

    public void setPress(ItemStack stack) {
        this.press = stack.copyWithCount(1);
        this.sync();
    }

    public ItemStack takePress() {
        ItemStack stack = this.press;
        this.press = ItemStack.EMPTY;
        this.sync();
        return stack;
    }

    public void setCover(ItemStack stack) {
        this.cover = stack.copyWithCount(1);
        this.sync();
    }

    public ItemStack takeCover() {
        ItemStack stack = this.cover;
        this.cover = ItemStack.EMPTY;
        this.sync();
        return stack;
    }

    /** 泡菜腌好了：把缸里的水等量换算成酸引水（一层水 = 1000 mB） */
    public void setSourWaterMb(int mb) {
        this.sourWaterMb = Math.max(0, mb);
        this.refreshLiquidLevel();
        this.sync();
    }

    /** 往缸里倒酸引水（玻璃瓶倒回来） */
    public void addSourWater(int mb) {
        if (mb > 0) {
            this.sourWaterMb += mb;
            this.refreshLiquidLevel();
            this.sync();
        }
    }

    /** 缸里的清水（mB） */
    public int waterMb() {
        return this.waterMb;
    }

    /** 缸里的清水有几层（一层 = 1000 mB）—— 配方判定要用的就是它 */
    public int waterLayers() {
        return this.waterMb / VatRecipes.WATER_MB_PER_LEVEL;
    }

    /** 直接设置清水量（mB） */
    public void setWaterMb(int mb) {
        this.waterMb = Math.max(0, mb);
        this.refreshLiquidLevel();
        this.sync();
    }

    /** 往缸里加水（水桶，一桶 1000 mB） */
    public void addWater(int mb) {
        if (mb > 0) {
            this.waterMb += mb;
            this.refreshLiquidLevel();
            this.sync();
        }
    }

    /**
     * 往缸里加成品液体（发酵完成时用）。
     *
     * <p>这是**通用**入口：配方里写的是 {@code product(PASTE, 2500)}、{@code product(SOUR_WATER, 1000/层)}，
     * 至于这笔账记到哪个字段，由这里按液体种类决定。新增液体只要在
     * {@link com.gunmu.northeast_china_delight.crafting.VatRecipe.Fluid} 里加一项、在这里加一行。
     */
    public void addProduct(com.gunmu.northeast_china_delight.crafting.VatRecipe.Fluid fluid, int mb) {
        if (mb <= 0) {
            return;
        }
        this.setProductMb(fluid, this.productMb(fluid) + mb);
    }

    /** 缸里某种成品液体还剩多少 mB */
    public int productMb(com.gunmu.northeast_china_delight.crafting.VatRecipe.Fluid fluid) {
        return switch (fluid) {
            case WATER -> this.waterMb;
            case SOUR_WATER -> this.sourWaterMb;
            case PASTE -> this.pasteMb;
            case SOY_SAUCE -> this.soySauceMb;
            case VINEGAR -> this.vinegarMb;
            case WHITE_VINEGAR -> this.whiteVinegarMb;
            case FISH_SAUCE -> this.fishSauceMb;
            case SHRIMP_PASTE -> this.shrimpPasteMb;
            case KVASS -> this.kvassMb;
        };
    }

    /** 按种类设置成品液体量（清水和酸引水会顺手刷新缸里的液面显示） */
    public void setProductMb(com.gunmu.northeast_china_delight.crafting.VatRecipe.Fluid fluid, int mb) {
        int amount = Math.max(0, mb);
        switch (fluid) {
            case WATER -> {
                this.waterMb = amount;
                this.refreshLiquidLevel();
            }
            case SOUR_WATER -> {
                this.sourWaterMb = amount;
                this.refreshLiquidLevel();
            }
            case PASTE -> this.pasteMb = amount;
            case SOY_SAUCE -> this.soySauceMb = amount;
            case VINEGAR -> this.vinegarMb = amount;
            case WHITE_VINEGAR -> this.whiteVinegarMb = amount;
            case FISH_SAUCE -> this.fishSauceMb = amount;
            case SHRIMP_PASTE -> this.shrimpPasteMb = amount;
            case KVASS -> this.kvassMb = amount;
        }
        this.sync();
    }

    /**
     * 用容器取走一份（{@link VatRecipes#SERVING_MB} mB）成品液体。
     * 返回是否取到了（不够一份时把剩下的都取走，和以前一样）。
     */
    public boolean takeOneServing(com.gunmu.northeast_china_delight.crafting.VatRecipe.Fluid fluid) {
        int have = this.productMb(fluid);
        if (have <= 0) {
            return false;
        }
        this.setProductMb(fluid, have - VatRecipes.SERVING_MB);
        return true;
    }

    /**
     * 从缸里的成品液体里抽走最多 want mB —— 给"用过的调料瓶续上"用的。
     * 返回实际抽走的量（缸里不够就有多少给多少）。
     */
    public int drainProduct(VatRecipes.Kind bottled, int want) {
        com.gunmu.northeast_china_delight.crafting.VatRecipe.Fluid fluid = VatRecipes.productOf(bottled);
        if (want <= 0 || fluid == null) {
            return 0;
        }
        int moved = Math.min(want, this.productMb(fluid));
        if (moved > 0) {
            this.setProductMb(fluid, this.productMb(fluid) - moved);
        }
        return moved;
    }

    /**
     * 把「清水 + 酸引水」折算成方块状态里的水位（一层 = 1000 mB，最多 3 层）。
     *
     * <p>水位只是**显示值**：罐子里的液面高度、缸体模型的水面都看它。
     * 配方判定一律走 {@link #waterLayers()}（只有清水）与 {@link #sourWaterMb()}，不会混淆。
     */
    private void refreshLiquidLevel() {
        if (this.level == null) {
            return;
        }
        BlockState state = this.getBlockState();
        int level = Math.min(ModBlockStateProperties.VAT_MAX_WATER,
                (int) Math.ceil((this.waterMb + this.sourWaterMb) / (double) VatRecipes.WATER_MB_PER_LEVEL));
        if (state.getValue(Vat.WATER_LEVEL) != level) {
            this.level.setBlock(this.worldPosition, state.setValue(Vat.WATER_LEVEL, level), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * 大酱 / 酱油酿造完成：三块酱块变成酱渣，盐和小麦都被吸收掉。
     * 酱渣会留在缸里，等液体取空后由玩家取出（也可以继续酿醋）。
     *
     * <p>正常流程里这一步由 {@code VatBrewing.complete} 按配方里的 {@code Fate} 做掉；
     * 这个方法只留给**旧存档**补一次转换（那时酱块还是酱块）。
     */
    public void finishBrewing() {
        for (int i = 0; i < MAX_ENTRIES; i++) {
            ItemStack stack = this.contents.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModItems.SOY_PASTE_CHUNK.get())) {
                this.contents.set(i, new ItemStack(VatRecipes.residue()));
            } else if (stack.is(ModItems.SALT.get()) || stack.is(VatRecipes.wheatInput())) {
                this.contents.set(i, ItemStack.EMPTY);
            }
        }
        this.sync();
    }

    /** 清空内容物（但不包括压缸石与地毯，它们由调用方单独处理） */
    public void clearContents() {
        // 逐个置空：既避免 clear() 留下 null 槽位，也保证客户端拿到的是一份干净的数据
        for (int i = 0; i < MAX_ENTRIES; i++) {
            this.contents.set(i, ItemStack.EMPTY);
        }
        this.kind = VatRecipes.Kind.NONE;
        this.pasteMb = 0;
        this.soySauceMb = 0;
        this.vinegarMb = 0;
        this.sourWaterMb = 0;
        this.whiteVinegarMb = 0;
        this.fishSauceMb = 0;
        this.shrimpPasteMb = 0;
        this.kvassMb = 0;
        this.waterMb = 0;
        this.sync();
    }

    public void sync() {
        this.setChanged();
        // 显式把该位置的方块实体标记为「需要重发」，客户端才会拿到最新内容
        if (this.level instanceof net.minecraft.server.level.ServerLevel server) {
            server.getChunkSource().blockChanged(this.worldPosition);
            server.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ===== 液体（酱油 / 大酱，供流体管道抽取） =====

    /**
     * 当前能被管道抽走的液体。
     * 还在发酵、没酿好、蒙着盖布或者已经抽空时都返回 null。
     *
     * <p>这里的 {@code switch} 故意不写 {@code default}：大缸以后新增一种
     * {@link VatRecipes.Kind} 时，编译器会强制你在这里明确表态
     * （"这种缸到底出不出液体"），而不是悄悄落进 default、表现为"管道抽不出来"。
     */
    private Fluid productFluid() {
        if (!this.getBlockState().getValue(Vat.FERMENTED) || this.isCovered()) {
            return null;
        }
        return switch (this.kind) {
            case PASTE -> this.pasteMb > 0 ? ModFluids.soyPasteSource() : null;
            case SOY_SAUCE -> this.soySauceMb > 0 ? ModFluids.soySauceSource() : null;
            case VINEGAR -> this.vinegarMb > 0 ? ModFluids.vinegarSource() : null;
            case WHITE_VINEGAR -> this.whiteVinegarMb > 0 ? ModFluids.whiteVinegarSource() : null;
            case FISH_SAUCE -> this.fishSauceMb > 0 ? ModFluids.fishSauceSource() : null;
            case SHRIMP_PASTE -> this.shrimpPasteMb > 0 ? ModFluids.shrimpPasteSource() : null;
            // 格瓦斯暂时不接流体管道：缸里按 mB 记账，玩家用玻璃瓶一瓶一瓶取
            case KVASS -> null;
            case PICKLE, SPICY_PICKLE -> this.sourWaterMb > 0 ? ModFluids.sourWaterSource() : null;
            // 没有成品液体的那几种缸
            case NONE, MEAT, SALTED_FISH, BEAN_SPROUTS, SOUR_CORN, FROZEN_PEAR -> null;
        };
    }

    private int productAmount() {
        return switch (this.kind) {
            case PASTE -> this.pasteMb;
            case SOY_SAUCE -> this.soySauceMb;
            case VINEGAR -> this.vinegarMb;
            case WHITE_VINEGAR -> this.whiteVinegarMb;
            case FISH_SAUCE -> this.fishSauceMb;
            case SHRIMP_PASTE -> this.shrimpPasteMb;
            case PICKLE, SPICY_PICKLE -> this.sourWaterMb;
            case KVASS -> this.kvassMb;
            case NONE, MEAT, SALTED_FISH, BEAN_SPROUTS, SOUR_CORN, FROZEN_PEAR -> 0;
        };
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        Fluid fluid = productFluid();
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, productAmount());
    }

    @Override
    public int getTankCapacity(int tank) {
        return VatRecipes.PRODUCT_CAPACITY_MB;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        // 大缸只出料，不接受管道注液（加水仍然走水桶交互）
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        Fluid fluid = productFluid();
        if (fluid == null || resource.isEmpty() || !resource.is(fluid)) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        Fluid fluid = productFluid();
        int available = productAmount();
        if (fluid == null || maxDrain <= 0 || available <= 0) {
            return FluidStack.EMPTY;
        }
        int drained = Math.min(maxDrain, available);
        if (action.execute()) {
            switch (this.kind) {
                case PASTE -> this.pasteMb -= drained;
                case SOY_SAUCE -> this.soySauceMb -= drained;
                case VINEGAR -> this.vinegarMb -= drained;
                case WHITE_VINEGAR -> this.whiteVinegarMb -= drained;
                case FISH_SAUCE -> this.fishSauceMb -= drained;
                case SHRIMP_PASTE -> this.shrimpPasteMb -= drained;
                case PICKLE, SPICY_PICKLE -> {
                    this.sourWaterMb -= drained;
                    this.refreshLiquidLevel();
                }
                // 上面 productFluid() 已经挡掉了"没有液体产物"的缸，这里只是让 switch 完整
                case NONE, MEAT, SALTED_FISH, BEAN_SPROUTS, SOUR_CORN, KVASS -> {
                }
            }
            // 液体抽空后酱渣还留在缸里，等玩家取走（所以这里不能 reset）
            this.sync();
        }
        return new FluidStack(fluid, drained);
    }

    // ===== 存档与同步 =====

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.contents, registries);
        // 注意：空的 ItemStack 不能存档，必须判空
        if (!this.press.isEmpty()) {
            tag.put("press", this.press.save(registries));
        }
        if (!this.cover.isEmpty()) {
            tag.put("cover", this.cover.save(registries));
        }
        tag.putInt("paste_mb", this.pasteMb);
        tag.putInt("soy_sauce_mb", this.soySauceMb);
        tag.putInt("vinegar_mb", this.vinegarMb);
        tag.putInt("sour_water_mb", this.sourWaterMb);
        tag.putInt("white_vinegar_mb", this.whiteVinegarMb);
        tag.putInt("fish_sauce_mb", this.fishSauceMb);
        tag.putInt("shrimp_paste_mb", this.shrimpPasteMb);
        tag.putInt("kvass_mb", this.kvassMb);
        tag.putInt("water_mb", this.waterMb);
        tag.putString("kind", this.kind.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 关键：loadAllItems 只会覆盖 NBT 里存在的槽位，不会清空原有数据。
        // 不先清空的话，被取走的物品会残留在客户端那份数据里，表现为「取出了但模型不消失」。
        for (int i = 0; i < MAX_ENTRIES; i++) {
            this.contents.set(i, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(tag, this.contents, registries);
        // 必须先判空：空的 CompoundTag 会被当成一次「解析失败的物品」并刷一条错误日志
        this.press = tag.contains("press")
                ? ItemStack.parse(registries, tag.getCompound("press")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        this.cover = tag.contains("cover")
                ? ItemStack.parse(registries, tag.getCompound("cover")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        // 旧存档里可能记着已经删掉的加工类型（比如早已废弃的「水面团」DOUGH）：
        // 认不出来就当成空缸，绝不因为一条老数据把存档读崩
        this.kind = parseKind(tag.getString("kind"));

        // 旧存档里存的是份数（paste / soy_sauce），换算成 mB；
        // 更旧的存档没有 soy_sauce 字段：已经在酿酱油的缸按满缸 10 瓶补上，避免卡住取不出来。
        if (tag.contains("paste_mb")) {
            this.pasteMb = tag.getInt("paste_mb");
        } else {
            this.pasteMb = Math.max(0, tag.getInt("paste")) * VatRecipes.SERVING_MB;
        }
        if (tag.contains("soy_sauce_mb")) {
            this.soySauceMb = tag.getInt("soy_sauce_mb");
        } else {
            int legacy = tag.contains("soy_sauce")
                    ? tag.getInt("soy_sauce")
                    : (this.kind == VatRecipes.Kind.SOY_SAUCE ? VatRecipes.SOY_SAUCE_SERVINGS : 0);
            this.soySauceMb = Math.max(0, legacy) * VatRecipes.SERVING_MB;
        }
        this.vinegarMb = Math.max(0, tag.getInt("vinegar_mb"));
        this.sourWaterMb = Math.max(0, tag.getInt("sour_water_mb"));
        this.whiteVinegarMb = Math.max(0, tag.getInt("white_vinegar_mb"));
        this.fishSauceMb = Math.max(0, tag.getInt("fish_sauce_mb"));
        this.shrimpPasteMb = Math.max(0, tag.getInt("shrimp_paste_mb"));
        this.kvassMb = Math.max(0, tag.getInt("kvass_mb"));
        if (tag.contains("water_mb")) {
            this.waterMb = Math.max(0, tag.getInt("water_mb"));
        } else if (this.level != null) {
            // 旧存档没有 water_mb：那时水位字段既代表清水也代表酸引水。
            // 缸里有酸引水就说明那层水已经转化过（清水记 0），否则按水位补成清水。
            int level = this.getBlockState().getValue(Vat.WATER_LEVEL);
            this.waterMb = this.sourWaterMb > 0 ? 0 : level * VatRecipes.WATER_MB_PER_LEVEL;
        }
        // 兼容一种历史状态：酿好的大酱被加了小麦，kind 变成了酱油、份数却还记在大酱里，
        // 结果这缸酱看起来是空的。这里把它救成同等份数的酱油。
        if (this.kind == VatRecipes.Kind.SOY_SAUCE && this.soySauceMb <= 0 && this.pasteMb > 0) {
            this.soySauceMb = this.pasteMb;
            this.pasteMb = 0;
        }
        // 兼容旧存档：本次更新前酿好的大缸里还是酱块，这里补一次「酱块变酱渣」
        if (this.level != null
                && this.getBlockState().getValue(Vat.FERMENTED)
                && (this.kind == VatRecipes.Kind.PASTE || this.kind == VatRecipes.Kind.SOY_SAUCE)
                && this.countOf(ModItems.SOY_PASTE_CHUNK.get()) > 0) {
            this.finishBrewing();
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    /** 把存档里的加工类型名字转回枚举，认不出来（旧版本删掉的类型）就当空缸 */
    private static VatRecipes.Kind parseKind(String name) {
        for (VatRecipes.Kind kind : VatRecipes.Kind.values()) {
            if (kind.name().equals(name)) {
                return kind;
            }
        }
        return VatRecipes.Kind.NONE;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
