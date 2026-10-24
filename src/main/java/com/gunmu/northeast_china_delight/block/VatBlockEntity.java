package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.crafting.VatRecipes;
import com.gunmu.northeast_china_delight.fluid.ModFluids;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.ModTags;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.gunmu.northeast_china_delight.util.DdNbt;
//? if <1.20.2 {
/*import net.minecraft.advancements.Advancement;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
*///?} else {
import net.minecraft.advancements.AdvancementHolder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
//? if <1.20.2 {
/*import net.minecraftforge.fluids.FluidStack;
*///?} else {
import net.neoforged.neoforge.fluids.FluidStack;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.fluids.capability.IFluidHandler;
*///?} else {
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
//?}
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.phys.Vec3;

/**
 * 大缸里的东西：内容物（蔬菜/肉/盐/酱块）、压着的石头、蒙着的羊毛地毯、剩余酱量。内容物按放入顺序记录，所以「一层肉一层盐」这种层叠关系会被保留下来。大酱 / 酱油按 mB 记
 * 账（1 份 = 250 mB），这样既能用碗、瓶取，也能被流体管道抽走。
 */
public class VatBlockEntity extends BlockEntity implements IFluidHandler {

    //? if <1.20.2 {
    /*// 1.20.1（Forge）靠 ICapabilityProvider + LazyOptional 暴露流体能力；
    // 1.20.2 起换成在 RegisterCapabilitiesEvent 里注册（见 NortheastChinaDelight）。
    private final LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(() -> this);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return fluidHandler.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidHandler.invalidate();
    }
    *///?}

    public static final int MAX_ENTRIES = 12;

    private final NonNullList<ItemStack> contents = NonNullList.withSize(MAX_ENTRIES, ItemStack.EMPTY);
    private ItemStack press = ItemStack.EMPTY;
    private ItemStack cover = ItemStack.EMPTY;
    private int pasteMb;
    private int soySauceMb;
    private int vinegarMb;
    private int sourWaterMb;
    private int whiteVinegarMb;
    private int fishSauceMb;
    private int shrimpPasteMb;
    private int kvassMb;
    /**
     * 缸里的清水（mB，一层 = 1000 mB）。
     *
     * <p>以前清水直接用方块状态的水位记，而酸引水又会按 mB 折算回同一个水位 ——
     * 两者混用一个字段，才会出现"倒一瓶酸引水当一层水腌菜、腌完水翻倍"这类问题。
     * 现在清水单独记账，水位只是"显示值" = ceil((清水 + 酸引水) / 1000)。
     */
    private int waterMb;
    private VatRecipes.Kind kind = VatRecipes.Kind.NONE;
    @Nullable
    private String recipeId;
    /**
     * 缸里的成品液体。配方里写什么流体，这里就存什么流体，不再按液体种类各记一个字段。
     * 清水和酸引水不在这里：它们决定水位，单独记在 {@link #waterMb} 和 {@link #sourWaterMb}。
     */
    @Nullable
    private Fluid brewedFluid;
    private int brewedMb;

    public VatBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VAT.get(), pos, state);
    }

    public VatRecipes.Kind kind() {
        return this.kind;
    }

    @Nullable
    public String recipeId() {
        return this.recipeId;
    }

    /** 缸里成品液体的种类；还没酿出东西、或者成品是酸引水（记在水位上）时返回 null */
    @Nullable
    public Fluid brewedFluid() {
        return this.brewedFluid;
    }

    public int brewedMb() {
        return this.brewedMb;
    }

    public boolean isFermented() {
        return this.getBlockState().getValue(Vat.FERMENTED);
    }

    public boolean hasMissingUnfinishedRecipe() {
        return this.recipeId != null && !isFermented() && VatRecipes.recipeOf(this) == null;
    }

    /**
     * 缸里还有没取完的成品液体（不含酸引水）。
     *
     * <p>成品液体可能来自新字段 {@link #brewedFluid}/{@link #brewedMb}，也可能来自旧存档的
     * 分液体字段。所有"能不能接下一道工序"的判断都必须走这里，否则只看旧字段时会漏掉新酿好的
     * 酱油 / 醋，玩家就能在成品还没取完时继续投料或加水，把这一缸液体卡在取不出来的状态。
     */
    public boolean hasBottledProduct() {
        return this.brewedMb > 0 || this.pasteMb > 0 || this.soySauceMb > 0 || this.vinegarMb > 0
                || this.whiteVinegarMb > 0 || this.fishSauceMb > 0
                || this.shrimpPasteMb > 0 || this.kvassMb > 0;
    }

    public boolean isEmpty() {
        // 只用盐不算「有东西」：盐已经被吸收了，不该挡住加水或继续操作
        return this.kind == VatRecipes.Kind.NONE && !this.hasProductLiquid()
                && this.count(stack -> !stack.is(ModTags.FOODS_SALT)) == 0;
    }

    public int rawFishCount() {
        return count(VatRecipes::isRawFish);
    }

    public int shrimpCount() {
        return countOf(ModItems.SHRIMP.get());
    }

    public int soybeanCount() {
        return countOf(ModItems.SOYBEAN.get());
    }

    public int cornKernelCount() {
        return countOf(ModItems.CORN_SEEDS.get());
    }

    public int chiliSauceCount() {
        return countOf(ModItems.CHILI_SAUCE.get());
    }

    public int seasoningCount() {
        return countOf(ModItems.FISH_SAUCE.get()) + countOf(ModItems.SHRIMP_PASTE.get());
    }

    public int residueCount() {
        return countOf(ModItems.SOY_RESIDUE.get());
    }

    public int vinegarGrainCount() {
        return count(VatRecipes::isVinegarGrain);
    }

    public boolean hasProductLiquid() {
        return this.hasBottledProduct() || this.sourWaterMb > 0;
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

    public List<ItemStack> contents() {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack stack : this.contents) {
            if (stack.isEmpty()) {
                continue;
            }
            if (this.isSaltDissolved() && stack.is(ModTags.FOODS_SALT)) {
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
        return count(stack -> stack.is(ModTags.FOODS_RAW_PORK));
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

    public boolean isClothCover() {
        return !this.cover.isEmpty() && Vat.isClothRug(this.cover);
    }

    public ItemStack cover() {
        return this.cover;
    }

    public int paste() {
        return this.pasteMb() / VatRecipes.SERVING_MB;
    }

    public int pasteMb() {
        return this.effectiveProductMb(ModFluids.soyPasteSource(), this.pasteMb);
    }

    public int soySauce() {
        return this.soySauceMb() / VatRecipes.SERVING_MB;
    }

    public int soySauceMb() {
        return this.effectiveProductMb(ModFluids.soySauceSource(), this.soySauceMb);
    }

    public int vinegar() {
        return this.vinegarMb() / VatRecipes.SERVING_MB;
    }

    public int vinegarMb() {
        return this.effectiveProductMb(ModFluids.vinegarSource(), this.vinegarMb);
    }

    public int sourWater() {
        return this.sourWaterMb / VatRecipes.SERVING_MB;
    }

    public int sourWaterMb() {
        return this.sourWaterMb;
    }

    public int whiteVinegar() {
        return this.whiteVinegarMb() / VatRecipes.SERVING_MB;
    }

    public int whiteVinegarMb() {
        return this.effectiveProductMb(ModFluids.whiteVinegarSource(), this.whiteVinegarMb);
    }

    public int fishSauce() {
        return this.fishSauceMb() / VatRecipes.SERVING_MB;
    }

    public int fishSauceMb() {
        return this.effectiveProductMb(ModFluids.fishSauceSource(), this.fishSauceMb);
    }

    public int shrimpPaste() {
        return this.shrimpPasteMb() / VatRecipes.SERVING_MB;
    }

    public int shrimpPasteMb() {
        return this.effectiveProductMb(ModFluids.shrimpPasteSource(), this.shrimpPasteMb);
    }

    public int kvass() {
        return this.kvassMb() / VatRecipes.SERVING_MB;
    }

    public int kvassMb() {
        return this.effectiveProductMb(ModFluids.kvassSource(), this.kvassMb);
    }

    public void setKind(VatRecipes.Kind kind) {
        this.kind = kind;
        this.recipeId = null;
        this.brewedFluid = null;
        this.brewedMb = 0;
        this.sync();
    }

    public void setRecipe(com.gunmu.northeast_china_delight.crafting.VatRecipe recipe) {
        this.kind = recipe.kind();
        this.recipeId = recipe.id();
        this.brewedFluid = recipe.productFluid();
        this.brewedMb = 0;
        this.sync();
    }

    /** 解除"正在做哪条配方"的绑定，但保留内容物和液体；进度失配时用它把缸退回可重新选配方的状态。 */
    public void clearRecipeBinding() {
        if (this.recipeId != null) {
            this.recipeId = null;
            this.sync();
        }
    }

    /**
     * 没做完就揭开封口：解除配方绑定，加工类型也退回「还没开工」。只解绑的话 kind 还会让旧配方优先，
     * 大酱补了小麦照样酿成大酱。缸里有酸引水时按倒酸引水的规矩记作泡菜缸，免得之后倒不进酸引水。
     */
    public void releaseUnfinishedRecipe() {
        this.recipeId = null;
        this.kind = this.sourWaterMb > 0 ? VatRecipes.Kind.PICKLE : VatRecipes.Kind.NONE;
        if (this.brewedMb <= 0) {
            this.brewedFluid = null;
        }
        this.sync();
    }

    /**
     * 液体被取走之后调用（容器、调料瓶、管道都走这里）：酱渣、泡菜这些固体还在就保持「做完了」，
     * 等玩家空手逐份取；固体和液体都没了就整缸复位，下一次投料直接开新的一缸。
     */
    public void resetIfDrained() {
        if (this.level != null && !this.level.isClientSide
                && this.contents().isEmpty() && !this.hasProductLiquid()) {
            Vat.reset(this.level, this.worldPosition, this.getBlockState(), this);
        }
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

    public void setSourWaterMb(int mb) {
        this.sourWaterMb = Math.max(0, mb);
        this.refreshLiquidLevel();
        this.sync();
    }

    public void addSourWater(int mb) {
        if (mb > 0) {
            this.sourWaterMb += mb;
            this.refreshLiquidLevel();
            this.sync();
        }
    }

    public int waterMb() {
        return this.waterMb;
    }

    public int waterLayers() {
        return this.waterMb / VatRecipes.WATER_MB_PER_LEVEL;
    }

    public void setWaterMb(int mb) {
        this.waterMb = Math.max(0, mb);
        this.refreshLiquidLevel();
        this.sync();
    }

    public void addWater(int mb) {
        if (mb > 0) {
            this.waterMb += mb;
            this.refreshLiquidLevel();
            this.sync();
        }
    }

    /**
     * 往缸里加成品液体。酸引水不进成品槽：它决定水位，记在 {@link #sourWaterMb}。
     * 缸里已经有另一种成品液体时不加（一缸只出一种液体）。
     */
    public void addProduct(Fluid fluid, int mb) {
        if (mb <= 0 || fluid == null) {
            return;
        }
        if (fluid.isSame(ModFluids.sourWaterSource())) {
            this.sourWaterMb += mb;
            this.refreshLiquidLevel();
            this.sync();
            return;
        }
        if (this.brewedFluid != null && !this.brewedFluid.isSame(fluid)) {
            return;
        }
        this.brewedFluid = fluid;
        this.brewedMb += mb;
        // 旧字段只用于读旧存档，运行时统一以 brewedFluid / brewedMb 为准，
        // 否则同一缸液体会被两套记账同时算到。
        this.clearLegacyProductMb(fluid);
        this.sync();
    }

    public int productMb(Fluid fluid) {
        if (fluid == null) {
            return 0;
        }
        if (fluid.isSame(Fluids.WATER)) {
            return this.waterMb;
        }
        if (fluid.isSame(ModFluids.sourWaterSource())) {
            return this.sourWaterMb;
        }
        return this.effectiveProductMb(fluid, this.legacyProductMb(fluid));
    }

    public void setProductMb(Fluid fluid, int mb) {
        if (fluid == null) {
            return;
        }
        int amount = Math.max(0, mb);
        if (fluid.isSame(Fluids.WATER)) {
            this.waterMb = amount;
            this.refreshLiquidLevel();
        } else if (fluid.isSame(ModFluids.sourWaterSource())) {
            this.sourWaterMb = amount;
            this.refreshLiquidLevel();
        } else if (amount == 0) {
            if (this.brewedFluid != null && this.brewedFluid.isSame(fluid)) {
                this.brewedFluid = null;
                this.brewedMb = 0;
            }
            this.clearLegacyProductMb(fluid);
        } else {
            this.brewedFluid = fluid;
            this.brewedMb = amount;
            this.clearLegacyProductMb(fluid);
        }
        this.sync();
    }

    /** 旧存档把每种成品液体分开记账；这里按流体读取对应的旧字段。 */
    private int legacyProductMb(Fluid fluid) {
        if (fluid.isSame(ModFluids.soyPasteSource())) {
            return this.pasteMb;
        }
        if (fluid.isSame(ModFluids.soySauceSource())) {
            return this.soySauceMb;
        }
        if (fluid.isSame(ModFluids.vinegarSource())) {
            return this.vinegarMb;
        }
        if (fluid.isSame(ModFluids.whiteVinegarSource())) {
            return this.whiteVinegarMb;
        }
        if (fluid.isSame(ModFluids.fishSauceSource())) {
            return this.fishSauceMb;
        }
        if (fluid.isSame(ModFluids.shrimpPasteSource())) {
            return this.shrimpPasteMb;
        }
        if (fluid.isSame(ModFluids.kvassSource())) {
            return this.kvassMb;
        }
        return 0;
    }

    /** 读取成品量时兼容旧字段：新字段已经记了同一种液体，就以新字段为准。 */
    private int effectiveProductMb(Fluid fluid, int legacy) {
        int brewed = this.brewedFluid != null && this.brewedFluid.isSame(fluid) ? this.brewedMb : 0;
        return Math.max(legacy, brewed);
    }

    private void clearLegacyProductMb(Fluid fluid) {
        if (fluid.isSame(ModFluids.soyPasteSource())) {
            this.pasteMb = 0;
        } else if (fluid.isSame(ModFluids.soySauceSource())) {
            this.soySauceMb = 0;
        } else if (fluid.isSame(ModFluids.vinegarSource())) {
            this.vinegarMb = 0;
        } else if (fluid.isSame(ModFluids.whiteVinegarSource())) {
            this.whiteVinegarMb = 0;
        } else if (fluid.isSame(ModFluids.fishSauceSource())) {
            this.fishSauceMb = 0;
        } else if (fluid.isSame(ModFluids.shrimpPasteSource())) {
            this.shrimpPasteMb = 0;
        } else if (fluid.isSame(ModFluids.kvassSource())) {
            this.kvassMb = 0;
        }
    }

    /**
     * 用容器取走一份（{@link VatRecipes#SERVING_MB} mB）成品液体。
     * 返回是否取到了（不够一份时把剩下的都取走，和以前一样）。
     */
    public boolean takeOneServing(Fluid fluid) {
        int have = this.productMb(fluid);
        if (have <= 0) {
            return false;
        }
        this.setProductMb(fluid, have - VatRecipes.SERVING_MB);
        return true;
    }

    public int drainProduct(int want) {
        Fluid fluid = VatRecipes.productOf(this);
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
     * <p>水位只是显示值：罐子里的液面高度、缸体模型的水面都看它。
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

    public void finishBrewing() {
        for (int i = 0; i < MAX_ENTRIES; i++) {
            ItemStack stack = this.contents.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModItems.SOY_PASTE_CHUNK.get())) {
                this.contents.set(i, new ItemStack(VatRecipes.residue()));
            } else if (stack.is(ModTags.FOODS_SALT) || stack.is(ModTags.CROPS_WHEAT)) {
                this.contents.set(i, ItemStack.EMPTY);
            }
        }
        this.sync();
    }

    public void clearContents() {
        // 逐个置空：既避免 clear() 留下 null 槽位，也保证客户端拿到的是一份干净的数据
        for (int i = 0; i < MAX_ENTRIES; i++) {
            this.contents.set(i, ItemStack.EMPTY);
        }
        this.kind = VatRecipes.Kind.NONE;
        this.recipeId = null;
        this.brewedFluid = null;
        this.brewedMb = 0;
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
        if (this.level instanceof net.minecraft.server.level.ServerLevel server) {
            server.getChunkSource().blockChanged(this.worldPosition);
            server.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
            // 成品液体的存量变了，旁边的红石比较器要重新读
            server.updateNeighbourForOutputSignal(this.worldPosition, this.getBlockState().getBlock());
        }
    }

    /** 缸里现在能被抽走 / 装瓶的成品液体。没发酵完、盖着盖子、或者没有液体时返回 null */
    private Fluid productFluid() {
        if (!this.getBlockState().getValue(Vat.FERMENTED) || this.isCovered()) {
            return null;
        }
        Fluid product = VatRecipes.productOf(this);
        return product != null && this.productMb(product) > 0 ? product : null;
    }

    private int productAmount() {
        Fluid product = VatRecipes.productOf(this);
        return product == null ? 0 : this.productMb(product);
    }

    /**
     * 红石比较器读发酵进度：还没开始是 0，做完是 15，中间按进度线性涨。
     * 进度本身是 0 到 {@link Vat#MAX_PROGRESS}，这里折成比较器的 0 到 15。
     * 只要开始了就至少给 1，避免「正在发酵但信号还是 0」。
     */
    public int comparatorOutput() {
        int progress = this.getBlockState().getValue(Vat.PROGRESS);
        if (progress <= 0) {
            return 0;
        }
        if (progress >= Vat.MAX_PROGRESS) {
            return 15;
        }
        return net.minecraft.util.Mth.clamp(progress * 15 / Vat.MAX_PROGRESS, 1, 14);
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
        Fluid product = VatRecipes.productOf(this);
        return product != null && product.isSame(ModFluids.sourWaterSource())
                ? VatRecipes.SOUR_WATER_MB : VatRecipes.PRODUCT_CAPACITY_MB;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        Fluid fluid = productFluid();
        //? if <1.20.2 {
        /*if (fluid == null || resource.isEmpty() || resource.getFluid() != fluid) {
        *///?} else {
        if (fluid == null || resource.isEmpty() || !resource.is(fluid)) {
        //?}
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
            this.setProductMb(fluid, this.productMb(fluid) - drained);
            this.sync();
            awardPumpSeasoning();
            // 抽空后和用容器取空一样：酱渣、泡菜还在就不能把缸退回「没做完」，否则空手取不出来
            this.resetIfDrained();
        }
        return new FluidStack(fluid, drained);
    }

    private void awardPumpSeasoning() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        //? if <1.20.2 {
        /*Advancement advancement = serverLevel.getServer().getAdvancements().getAdvancement(
                DdIds.of(NortheastChinaDelight.MODID, "pump_seasoning"));
        *///?} else {
        AdvancementHolder advancement = serverLevel.getServer().getAdvancements().get(
                DdIds.of(NortheastChinaDelight.MODID, "pump_seasoning"));
        //?}
        if (advancement == null) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(this.worldPosition);
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(center) <= 32 * 32) {
                player.getAdvancements().award(advancement, "pump");
            }
        }
    }

    @Override
    //? if <1.20.5 {
    /*protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        this.saveVatData(tag, null);
    }*/
    //?} else {
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.saveVatData(tag, registries);
    }
    //?}

    /** 存档主体（两个版本共用）；1.20.1 没有注册表参数，传 null 即可 */
    private void saveVatData(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        DdNbt.saveAllItems(tag, this.contents, true, registries);
        // 注意：空的 ItemStack 不能存档，必须判空
        if (!this.press.isEmpty()) {
            tag.put("press", DdNbt.saveItem(this.press, registries));
        }
        if (!this.cover.isEmpty()) {
            tag.put("cover", DdNbt.saveItem(this.cover, registries));
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
        if (this.recipeId != null) {
            tag.putString("recipe_id", this.recipeId);
        }
        if (this.brewedFluid != null) {
            tag.putString("brewed_fluid", BuiltInRegistries.FLUID.getKey(this.brewedFluid).toString());
            tag.putInt("brewed_mb", this.brewedMb);
        }
    }

    @Override
    //? if <1.20.5 {
    /*public void load(CompoundTag tag) {
        super.load(tag);
        this.loadVatData(tag, null);
    }*/
    //?} else {
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.loadVatData(tag, registries);
    }
    //?}

    /** 读档主体（两个版本共用） */
    private void loadVatData(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        // 关键：loadAllItems 只会覆盖 NBT 里存在的槽位，不会清空原有数据。
        // 不先清空的话，被取走的物品会残留在客户端那份数据里，表现为「取出了但模型不消失」。
        for (int i = 0; i < MAX_ENTRIES; i++) {
            this.contents.set(i, ItemStack.EMPTY);
        }
        DdNbt.loadAllItems(tag, this.contents, registries);
        // 必须先判空：空的 CompoundTag 会被当成一次「解析失败的物品」并刷一条错误日志
        this.press = tag.contains("press")
                ? DdNbt.parseItem(tag.getCompound("press"), registries)
                : ItemStack.EMPTY;
        this.cover = tag.contains("cover")
                ? DdNbt.parseItem(tag.getCompound("cover"), registries)
                : ItemStack.EMPTY;
        // 旧存档里可能记着已经删掉的加工类型（比如早已废弃的「水面团」DOUGH）：
        // 认不出来就当成空缸，绝不因为一条老数据把存档读崩
        this.kind = parseKind(tag.getString("kind"));
        this.recipeId = tag.contains("recipe_id") ? tag.getString("recipe_id") : null;
        this.brewedFluid = null;
        this.brewedMb = 0;
        if (tag.contains("brewed_fluid")) {
            // 新存档：成品液体直接记流体 id 和 mB
            BuiltInRegistries.FLUID.getOptional(DdIds.parse(tag.getString("brewed_fluid")))
                    .ifPresent(fluid -> this.brewedFluid = fluid);
            this.brewedMb = Math.max(0, tag.getInt("brewed_mb"));
        } else if (tag.contains("brewed_product")) {
            // 旧存档：记的是枚举名（"SOY_SAUCE" 这种），翻译成流体。
            // 酸引水不进成品槽，它的量在 sour_water_mb 里
            try {
                this.brewedFluid = com.gunmu.northeast_china_delight.crafting.VatRecipe.Fluid
                        .valueOf(tag.getString("brewed_product")).resolve();
            } catch (IllegalArgumentException ignored) {
                this.brewedFluid = null;
            }
        }

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
        boolean serverSide = !(this.level != null && this.level.isClientSide);
        if (serverSide && this.kind == VatRecipes.Kind.SOY_SAUCE && this.soySauceMb <= 0 && this.pasteMb > 0) {
            this.soySauceMb = this.pasteMb;
            this.pasteMb = 0;
        }
        // 兼容旧存档：本次更新前酿好的大缸里还是酱块，这里补一次「酱块变酱渣」
        if (serverSide && this.level != null
                && this.getBlockState().getValue(Vat.FERMENTED)
                && (this.kind == VatRecipes.Kind.PASTE || this.kind == VatRecipes.Kind.SOY_SAUCE)
                && this.countOf(ModItems.SOY_PASTE_CHUNK.get()) > 0) {
            this.finishBrewing();
        }
        this.migrateLegacyProductAccounting();
    }

    /**
     * 把旧存档里按液体种类分开的字段并到统一成品槽。
     *
     * <p>旧版本只在 {@code brewed_product} 里记了液体种类，份数却还在 {@code soy_sauce_mb} 这类
     * 字段里；迁移时如果不把份数带过来，缸会显示"有成品"但任何容器都取不出，正好是最难自救的卡死状态。
     */
    private void migrateLegacyProductAccounting() {
        if (this.brewedMb <= 0) {
            Fluid fluid = this.brewedFluid;
            int amount = fluid == null ? 0 : this.legacyProductMb(fluid);
            if (amount <= 0) {
                fluid = this.firstLegacyProductFluid();
                amount = fluid == null ? 0 : this.legacyProductMb(fluid);
            }
            if (fluid != null && amount > 0) {
                this.brewedFluid = fluid;
                this.brewedMb = amount;
            } else {
                this.brewedFluid = null;
                this.brewedMb = 0;
            }
        }
        // 运行时只保留一套记账，避免存档里新旧字段各有一份而被重复计算。
        this.pasteMb = 0;
        this.soySauceMb = 0;
        this.vinegarMb = 0;
        this.whiteVinegarMb = 0;
        this.fishSauceMb = 0;
        this.shrimpPasteMb = 0;
        this.kvassMb = 0;
    }

    @Nullable
    private Fluid firstLegacyProductFluid() {
        if (this.pasteMb > 0) {
            return ModFluids.soyPasteSource();
        }
        if (this.soySauceMb > 0) {
            return ModFluids.soySauceSource();
        }
        if (this.vinegarMb > 0) {
            return ModFluids.vinegarSource();
        }
        if (this.whiteVinegarMb > 0) {
            return ModFluids.whiteVinegarSource();
        }
        if (this.fishSauceMb > 0) {
            return ModFluids.fishSauceSource();
        }
        if (this.shrimpPasteMb > 0) {
            return ModFluids.shrimpPasteSource();
        }
        if (this.kvassMb > 0) {
            return ModFluids.kvassSource();
        }
        return null;
    }

    @Override
    //? if <1.20.5 {
    /*public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }*/
    //?} else {
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }
    //?}

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
