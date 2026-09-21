package com.gunmu.dongbei_delight.block;

import com.gunmu.dongbei_delight.crafting.VatRecipes;
import com.gunmu.dongbei_delight.fluid.ModFluids;
import com.gunmu.dongbei_delight.item.ModItems;
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
    private VatRecipes.Kind kind = VatRecipes.Kind.NONE;

    public VatBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VAT.get(), pos, state);
    }

    // ===== 查询 =====

    public VatRecipes.Kind kind() {
        return this.kind;
    }

    public boolean isEmpty() {
        // 只用盐不算「有东西」：盐已经被吸收了，不该挡住加水或继续操作
        return this.kind == VatRecipes.Kind.NONE && this.pasteMb <= 0 && this.soySauceMb <= 0 && this.vinegarMb <= 0
                && this.sourWaterMb <= 0 && this.whiteVinegarMb <= 0
                && this.fishSauceMb <= 0 && this.shrimpPasteMb <= 0
                && this.count(stack -> !stack.is(com.gunmu.dongbei_delight.item.ModItems.SALT.get())) == 0;
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

    /** 取辣白菜时把用掉的调料清掉，返回要返还的空容器（辣椒酱还碗，鱼露 / 虾酱还玻璃瓶） */
    public List<ItemStack> consumeSeasonings() {
        List<ItemStack> refunds = new ArrayList<>();
        boolean changed = false;
        for (int i = 0; i < MAX_ENTRIES; i++) {
            ItemStack stack = this.contents.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModItems.FISH_SAUCE.get()) || stack.is(ModItems.SHRIMP_PASTE.get())) {
                this.contents.set(i, ItemStack.EMPTY);
                refunds.add(new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));
                changed = true;
            } else if (stack.is(ModItems.CHILI_SAUCE.get())) {
                this.contents.set(i, ItemStack.EMPTY);
                refunds.add(new ItemStack(net.minecraft.world.item.Items.BOWL));
                changed = true;
            }
        }
        if (changed) {
            this.sync();
        }
        return refunds;
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
                || this.fishSauceMb > 0 || this.shrimpPasteMb > 0;
    }

    /** 把已经用掉的盐清掉（腌制完成后盐就被吸收了） */
    public void consumeSalt() {
        removeAllMatching(stack -> stack.is(com.gunmu.dongbei_delight.item.ModItems.SALT.get()));
    }

    public void removeAllMatching(Predicate<ItemStack> filter) {
        boolean changed = false;
        for (int i = 0; i < MAX_ENTRIES; i++) {
            ItemStack stack = this.contents.get(i);
            if (!stack.isEmpty() && filter.test(stack)) {
                this.contents.set(i, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) {
            this.sync();
        }
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
            if (this.isSaltDissolved() && stack.is(com.gunmu.dongbei_delight.item.ModItems.SALT.get())) {
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

    public boolean removeOneOf(Item item) {
        return removeOneMatching(stack -> stack.is(item));
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

    /** 直接按份数灌满（发酵完成时用），1 份 = 250 mB */
    public void setPaste(int servings) {
        this.pasteMb = Math.max(0, servings) * VatRecipes.SERVING_MB;
        this.sync();
    }

    /** 直接按份数灌满（发酵完成时用），1 份 = 250 mB */
    public void setSoySauce(int servings) {
        this.soySauceMb = Math.max(0, servings) * VatRecipes.SERVING_MB;
        this.sync();
    }

    /** 直接按份数灌满（发酵完成时用），1 份 = 250 mB */
    public void setVinegar(int servings) {
        this.vinegarMb = Math.max(0, servings) * VatRecipes.SERVING_MB;
        this.sync();
    }

    /** 泡菜腌好了：把缸里的水等量换算成酸引水（一层水 = 1000 mB） */
    public void setSourWaterMb(int mb) {
        this.sourWaterMb = Math.max(0, mb);
        this.refreshSourWaterLevel();
        this.sync();
    }

    /** 往缸里倒酸引水（玻璃瓶倒回来） */
    public void addSourWater(int mb) {
        if (mb > 0) {
            this.sourWaterMb += mb;
            this.refreshSourWaterLevel();
            this.sync();
        }
    }

    /** 直接按份数灌满（发酵完成时用），1 份 = 250 mB */
    public void setWhiteVinegar(int servings) {
        this.whiteVinegarMb = Math.max(0, servings) * VatRecipes.SERVING_MB;
        this.sync();
    }

    /** 鱼露发酵完成：只出很少的量 */
    public void setFishSauce(int servings) {
        this.fishSauceMb = Math.max(0, servings) * VatRecipes.SERVING_MB;
        this.sync();
    }

    /** 虾酱发酵完成 */
    public void setShrimpPaste(int servings) {
        this.shrimpPasteMb = Math.max(0, servings) * VatRecipes.SERVING_MB;
        this.sync();
    }

    /** 只清空内容物，保留 kind 与成品数量（鱼露 / 虾酱发酵完成后鱼肉都被分解了） */
    public void consumeAllContents() {
        for (int i = 0; i < MAX_ENTRIES; i++) {
            this.contents.set(i, ItemStack.EMPTY);
        }
        this.sync();
    }

    /** 用碗盛走一份大酱；不足一份时把剩下的都盛走，返回剩余碗数 */
    public int takeOnePaste() {
        if (this.pasteMb > 0) {
            this.pasteMb = Math.max(0, this.pasteMb - VatRecipes.SERVING_MB);
            this.sync();
        }
        return this.paste();
    }

    /** 用瓶装走一份酱油；不足一份时把剩下的都装走，返回剩余瓶数 */
    public int takeOneSoySauce() {
        if (this.soySauceMb > 0) {
            this.soySauceMb = Math.max(0, this.soySauceMb - VatRecipes.SERVING_MB);
            this.sync();
        }
        return this.soySauce();
    }

    /** 用瓶装走一份醋；不足一份时把剩下的都装走，返回剩余瓶数 */
    public int takeOneVinegar() {
        if (this.vinegarMb > 0) {
            this.vinegarMb = Math.max(0, this.vinegarMb - VatRecipes.SERVING_MB);
            this.sync();
        }
        return this.vinegar();
    }

    /** 用瓶装走一份酸引水 */
    public int takeOneSourWater() {
        if (this.sourWaterMb > 0) {
            this.sourWaterMb = Math.max(0, this.sourWaterMb - VatRecipes.SERVING_MB);
            this.refreshSourWaterLevel();
            this.sync();
        }
        return this.sourWater();
    }

    /** 用瓶装走一份白醋 */
    public int takeOneWhiteVinegar() {
        if (this.whiteVinegarMb > 0) {
            this.whiteVinegarMb = Math.max(0, this.whiteVinegarMb - VatRecipes.SERVING_MB);
            this.sync();
        }
        return this.whiteVinegar();
    }

    /** 用瓶装走一份鱼露 */
    public int takeOneFishSauce() {
        if (this.fishSauceMb > 0) {
            this.fishSauceMb = Math.max(0, this.fishSauceMb - VatRecipes.SERVING_MB);
            this.sync();
        }
        return this.fishSauce();
    }

    /** 用瓶装走一份虾酱 */
    public int takeOneShrimpPaste() {
        if (this.shrimpPasteMb > 0) {
            this.shrimpPasteMb = Math.max(0, this.shrimpPasteMb - VatRecipes.SERVING_MB);
            this.sync();
        }
        return this.shrimpPaste();
    }

    /**
     * 酸引水按「一层水 = 1000 mB」折算回水位，
     * 这样用瓶子 / 管道取走一部分之后，缸里的水面也会跟着降。
     */
    private void refreshSourWaterLevel() {
        if (this.level == null
                || (this.kind != VatRecipes.Kind.PICKLE
                        && this.kind != VatRecipes.Kind.SPICY_PICKLE
                        && this.kind != VatRecipes.Kind.WHITE_VINEGAR)) {
            return;
        }
        BlockState state = this.getBlockState();
        int level = Math.min(ModBlockStateProperties.VAT_MAX_WATER,
                (int) Math.ceil(this.sourWaterMb / (double) VatRecipes.WATER_MB_PER_LEVEL));
        if (state.getValue(Vat.WATER_LEVEL) != level) {
            this.level.setBlock(this.worldPosition, state.setValue(Vat.WATER_LEVEL, level), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * 大酱 / 酱油酿造完成：三块酱块变成酱渣，盐和小麦都被吸收掉。
     * 酱渣会留在缸里，等液体取空后由玩家取出（也可以继续酿醋）。
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

    /** 醋酿造完成：谷物被消耗掉，酱渣留在缸里 */
    public void finishVinegar() {
        for (int i = 0; i < MAX_ENTRIES; i++) {
            ItemStack stack = this.contents.get(i);
            if (!stack.isEmpty() && VatRecipes.isVinegarGrain(stack)) {
                this.contents.set(i, ItemStack.EMPTY);
            }
        }
        this.sync();
    }

    /** 生豆芽完成：每份黄豆变成一份豆芽 */
    public void finishSprouting() {
        for (int i = 0; i < MAX_ENTRIES; i++) {
            ItemStack stack = this.contents.get(i);
            if (stack.is(ModItems.SOYBEAN.get())) {
                this.contents.set(i, new ItemStack(VatRecipes.beanSprouts()));
            }
        }
        this.sync();
    }

    /** 酸玉米粒发酵完成：每份玉米粒变成一份酸玉米粒 */
    public void finishSourCorn() {
        for (int i = 0; i < MAX_ENTRIES; i++) {
            ItemStack stack = this.contents.get(i);
            if (stack.is(ModItems.CORN_SEEDS.get())) {
                this.contents.set(i, new ItemStack(VatRecipes.sourCornKernels()));
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
            case PICKLE, SPICY_PICKLE -> this.sourWaterMb > 0 ? ModFluids.sourWaterSource() : null;
            default -> null;
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
            default -> 0;
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
                    this.refreshSourWaterLevel();
                }
                default -> {
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
        this.kind = VatRecipes.Kind.valueOf(tag.getString("kind"));

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

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
