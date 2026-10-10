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

    public VatBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VAT.get(), pos, state);
    }

    public VatRecipes.Kind kind() {
        return this.kind;
    }

    public boolean isFermented() {
        return this.getBlockState().getValue(Vat.FERMENTED);
    }

    public boolean isEmpty() {
        // 只用盐不算「有东西」：盐已经被吸收了，不该挡住加水或继续操作
        return this.kind == VatRecipes.Kind.NONE && this.pasteMb <= 0 && this.soySauceMb <= 0 && this.vinegarMb <= 0
                && this.sourWaterMb <= 0 && this.whiteVinegarMb <= 0
                && this.fishSauceMb <= 0 && this.shrimpPasteMb <= 0 && this.kvassMb <= 0
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
        return this.pasteMb / VatRecipes.SERVING_MB;
    }

    public int pasteMb() {
        return this.pasteMb;
    }

    public int soySauce() {
        return this.soySauceMb / VatRecipes.SERVING_MB;
    }

    public int soySauceMb() {
        return this.soySauceMb;
    }

    public int vinegar() {
        return this.vinegarMb / VatRecipes.SERVING_MB;
    }

    public int vinegarMb() {
        return this.vinegarMb;
    }

    public int sourWater() {
        return this.sourWaterMb / VatRecipes.SERVING_MB;
    }

    public int sourWaterMb() {
        return this.sourWaterMb;
    }

    public int whiteVinegar() {
        return this.whiteVinegarMb / VatRecipes.SERVING_MB;
    }

    public int whiteVinegarMb() {
        return this.whiteVinegarMb;
    }

    public int fishSauce() {
        return this.fishSauceMb / VatRecipes.SERVING_MB;
    }

    public int fishSauceMb() {
        return this.fishSauceMb;
    }

    public int shrimpPaste() {
        return this.shrimpPasteMb / VatRecipes.SERVING_MB;
    }

    public int shrimpPasteMb() {
        return this.shrimpPasteMb;
    }

    public int kvass() {
        return this.kvassMb / VatRecipes.SERVING_MB;
    }

    public int kvassMb() {
        return this.kvassMb;
    }

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

    public void addProduct(com.gunmu.northeast_china_delight.crafting.VatRecipe.Fluid fluid, int mb) {
        if (mb <= 0) {
            return;
        }
        this.setProductMb(fluid, this.productMb(fluid) + mb);
    }

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
        }
    }

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
            case KVASS -> this.kvassMb > 0 ? ModFluids.kvassSource() : null;
            case PICKLE, SPICY_PICKLE -> this.sourWaterMb > 0 ? ModFluids.sourWaterSource() : null;
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
                case KVASS -> this.kvassMb -= drained;
                case NONE, MEAT, SALTED_FISH, BEAN_SPROUTS, SOUR_CORN, FROZEN_PEAR -> {
                }
            }
            // 液体抽空后酱渣还留在缸里，等玩家取走（所以这里不能 reset）
            this.sync();
            awardPumpSeasoning();
            if (productAmount() <= 0 && this.level != null) {
                BlockState state = this.getBlockState();
                if (state.getValue(Vat.FERMENTED)) {
                    this.level.setBlock(this.worldPosition,
                            state.setValue(Vat.FERMENTED, false).setValue(Vat.PROGRESS, 0), 3);
                }
                if (isEmpty()) {
                    this.kind = VatRecipes.Kind.NONE;
                }
            }
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
