package com.gunmu.northeast_china_delight.crafting;

import com.gunmu.northeast_china_delight.block.VatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 大缸的通用引擎。这里没有任何一条针对具体物品的规则：所有"收什么、要几份、什么时候能开工、完成后变成什么、取货能拿到什么"都来自
 * {@link VatRecipes#REGISTRY} 里的 {@link VatRecipe}。想加配方 / 改数量，动那张表就行；这个文件不需要改。
 */
public final class VatBrewing {

    private VatBrewing() {
    }

    public static VatRecipe.Counts counts(VatBlockEntity vat, @Nullable ItemStack pending) {
        return new Counts(vat, pending);
    }

    private record Counts(VatBlockEntity vat, @Nullable ItemStack pending) implements VatRecipe.Counts {

        @Override
        public int count(java.util.function.Predicate<ItemStack> matcher) {
            int n = this.vat.count(matcher);
            if (this.pending != null && !this.pending.isEmpty() && matcher.test(this.pending)) {
                n++;
            }
            return n;
        }

        @Override
        public int water() {
            return this.vat.waterLayers();
        }
    }

    private static boolean isSnowyBiome(VatBlockEntity vat) {
        var level = vat.getLevel();
        if (level == null) {
            return false;
        }
        return level.getBiome(vat.getBlockPos()).value().getBaseTemperature() < 0.15F;
    }

    private static boolean hasBottledProduct(VatBlockEntity vat) {
        return vat.pasteMb() > 0 || vat.soySauceMb() > 0 || vat.vinegarMb() > 0
                || vat.whiteVinegarMb() > 0 || vat.fishSauceMb() > 0
                || vat.shrimpPasteMb() > 0 || vat.kvassMb() > 0;
    }

    @Nullable
    public static VatRecipe forInsert(VatBlockEntity vat, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        VatRecipe.Counts now = counts(vat, null);
        VatRecipe.Counts after = counts(vat, stack);
        int water = now.water();
        int sour = vat.sourWaterMb();
        boolean bottled = hasBottledProduct(vat);
        boolean sourPot = sour > 0;
        // 正在发酵的那一缸只收"同一条配方"的料（泡菜中途还能再塞菜，但不许中途改酿大酱）。
        // 注意这里必须排掉"已经做完"的缸：完成时 PROGRESS 会写到 MAX_PROGRESS，
        // 把 FERMENTED 的缸也算成"发酵中"的话，二次发酵（泡菜→白醋、酱渣→醋、大酱加小麦→酱油）
        // 就永远投不进料了 —— 那几条正是本轮要修的玩法。
        boolean processing = vat.getBlockState()
                .getValue(com.gunmu.northeast_china_delight.block.Vat.PROGRESS) > 0 && !vat.isFermented();

        VatRecipe best = null;
        int[] bestScore = null;
        for (VatRecipe recipe : VatRecipes.all()) {
            int slot = recipe.findSlotFor(stack, now);
            if (slot < 0 || !recipe.canStillMatch(water, sour)) {
                continue;
            }
            if (sourPot && recipe.liquid().sourWaterMb() <= 0) {
                continue;
            }
            // 成品液体还没取空：先取液体，别往里加料，否则这缸液体会取不出来
            if (bottled) {
                continue;
            }
            if (vat.isFermented() && recipe.kind() == vat.kind()) {
                continue;
            }
            if (processing && recipe.kind() != vat.kind()) {
                continue;
            }
            int[] score = score(recipe, after, sour, vat.kind());
            if (bestScore == null || better(score, bestScore)) {
                best = recipe;
                bestScore = score;
            }
        }
        return best;
    }
    @Nullable
    public static VatRecipe ready(VatBlockEntity vat) {
        VatRecipe.Counts counts = counts(vat, null);
        int sour = vat.sourWaterMb();
        VatRecipe best = null;
        int[] bestScore = null;
        for (VatRecipe recipe : VatRecipes.all()) {
            if (!recipe.materialsReady(counts, sour) || !sealMatches(vat, recipe)) {
                continue;
            }
            if (recipe.requiresSnowyBiome() && !isSnowyBiome(vat)) {
                continue;
            }
            if (sour > 0 && recipe.liquid().sourWaterMb() <= 0) {
                continue;
            }
            int[] score = score(recipe, counts, sour, vat.kind());
            if (bestScore == null || better(score, bestScore)) {
                best = recipe;
                bestScore = score;
            }
        }
        return best;
    }

    @Nullable
    public static VatRecipe current(VatBlockEntity vat) {
        VatRecipe.Counts counts = counts(vat, null);
        int sour = vat.sourWaterMb();
        VatRecipe best = null;
        int[] bestScore = null;
        for (VatRecipe recipe : VatRecipes.all()) {
            if (!recipe.canStillMatch(counts.water(), sour)) {
                continue;
            }
            if (sour > 0 && recipe.liquid().sourWaterMb() <= 0) {
                continue;
            }
            int[] score = score(recipe, counts, sour, vat.kind());
            if (bestScore == null || better(score, bestScore)) {
                best = recipe;
                bestScore = score;
            }
        }
        return best;
    }

    public static boolean sealMatches(VatBlockEntity vat, VatRecipe.Seal seal) {
        return switch (seal) {
            case NONE -> true;
            case PRESS -> vat.isPressed();
            case CLOTH -> vat.isCovered() && vat.isClothCover();
            case CARPET -> vat.isCovered() && !vat.isClothCover();
        };
    }

    /**
     * 封口物既要符合「哪种封口」（压缸石 / 毯子 / 布）这种大类要求，
     * 也要符合「压的必须是哪一种具体物品」（冻梨的"顶上压雪块"）这种专属要求。
     */
    public static boolean sealMatches(VatBlockEntity vat, VatRecipe recipe) {
        if (!sealMatches(vat, recipe.seal())) {
            return false;
        }
        var matcher = recipe.requiredSealItem();
        if (matcher == null) {
            return true;
        }
        return matcher.test(vat.press());
    }

    /**
     * 打分：满足的格子数 → 能不能开工 → 本来就是这条 → 液体对得上 → 要求的总份数少 → priority 高。
     *
     * <p>把"满足的格子数"放在第一位是有原因的：辣白菜比泡菜多两格（辣椒酱和调味品），
     * 泡菜缸里加了辣椒酱之后，泡菜那条配方立刻就"能开工了"，
     * 但玩家要的显然是继续往辣白菜那条路上走 —— 所以谁更"贴切"优先于谁"现在就能开工"。
     */
    private static int[] score(VatRecipe recipe, VatRecipe.Counts counts, int sourWaterMb,
                               VatRecipes.Kind current) {
        boolean ready = recipe.materialsReady(counts, sourWaterMb);
        int satisfied = recipe.satisfiedSlots(counts);
        int sticky = recipe.kind() == current ? 1 : 0;
        int liquid = recipe.liquidFits(counts.water(), sourWaterMb) ? 1 : 0;
        int exactMatch = ready && recipe.allSlotsExact(counts) ? 1 : 0;
        return new int[] { sticky, satisfied, exactMatch, ready ? 1 : 0, liquid, -recipe.minimumTotal(counts),
                recipe.priority() };
    }

    private static boolean better(int[] a, int[] b) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i];
            }
        }
        return false;
    }

    public static void complete(ServerLevel level, BlockPos pos, VatBlockEntity vat, VatRecipe recipe) {
        int layers = Math.max(1, vat.waterLayers());
        VatRecipe.Liquid liquid = recipe.liquid();

        switch (liquid.waterAfter()) {
            case CLEAR -> vat.setWaterMb(0);
            case DRAIN_REQUIRED -> vat.setWaterMb(
                    Math.max(0, vat.waterMb() - liquid.waterMin() * VatRecipes.WATER_MB_PER_LEVEL));
            case KEEP -> {
            }
        }
        switch (liquid.sourWaterAfter()) {
            case CLEAR -> vat.setSourWaterMb(0);
            case DRAIN_REQUIRED -> vat.setSourWaterMb(Math.max(0, vat.sourWaterMb() - liquid.sourWaterMb()));
            case KEEP -> {
            }
        }

        if (liquid.product() != null) {
            vat.addProduct(liquid.product(), recipe.productMbFor(layers));
        }

        applyFates(vat, recipe);

        BlockState current = level.getBlockState(pos);
        level.setBlock(pos, current
                .setValue(com.gunmu.northeast_china_delight.block.Vat.PROGRESS,
                        com.gunmu.northeast_china_delight.block.Vat.MAX_PROGRESS)
                .setValue(com.gunmu.northeast_china_delight.block.Vat.FERMENTED, true),
                net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        level.playSound(null, pos, SoundEvents.COMPOSTER_READY, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static void applyFates(VatBlockEntity vat, VatRecipe recipe) {
        for (ItemStack content : vat.contentSnapshot()) {
            VatRecipe.Slot slot = matchSlot(recipe, content);
            if (slot == null) {
                continue;
            }
            java.util.function.Predicate<ItemStack> same = stack -> stack.is(content.getItem());
            switch (slot.fate()) {
                case CONSUME -> vat.removeOneMatching(same);
                case CONVERT -> {
                    if (slot.product() != null) {
                        vat.replaceOneMatching(same, new ItemStack(slot.product()));
                    }
                }
                case KEEP, ABSORB -> {
                }
            }
        }
    }

    @Nullable
    private static VatRecipe.Slot matchSlot(VatRecipe recipe, ItemStack stack) {
        for (VatRecipe.Slot slot : recipe.slots()) {
            if (slot.matcher().test(stack)) {
                return slot;
            }
        }
        return null;
    }

    @Nullable
    public static ItemStack fillContainer(VatBlockEntity vat, ItemStack held) {
        if (!vat.isFermented() || vat.isCovered()) {
            return null;
        }
        VatRecipes.Kind kind = vat.kind();
        if (kind != VatRecipes.Kind.NONE) {
            VatRecipes.Serving serving = VatRecipes.servingOf(vatProduct(kind));
            if (serving != null && held.is(serving.container()) && vat.productMb(vatProduct(kind)) > 0) {
                vat.takeOneServing(vatProduct(kind));
                return new ItemStack(serving.item());
            }
        }
        if (vat.sourWaterMb() > 0 && held.is(net.minecraft.world.item.Items.GLASS_BOTTLE)) {
            vat.takeOneServing(VatRecipe.Fluid.SOUR_WATER);
            return new ItemStack(com.gunmu.northeast_china_delight.item.ModItems.SOUR_WATER.get());
        }
        return null;
    }

    private static VatRecipe.Fluid vatProduct(VatRecipes.Kind kind) {
        VatRecipe.Fluid fluid = VatRecipes.productOf(kind);
        return fluid == null ? VatRecipe.Fluid.WATER : fluid;
    }

    public static ItemStack takeOne(VatBlockEntity vat, List<ItemStack> refund) {
        absorbAll(vat, refund);

        VatRecipe recipe = VatRecipes.recipeOf(vat.kind());
        for (ItemStack content : vat.contentSnapshot()) {
            ItemStack taken = takeFrom(recipe, content);
            if (!taken.isEmpty()) {
                vat.removeOneMatching(stack -> stack.is(content.getItem()));
                return taken;
            }
        }
        return ItemStack.EMPTY;
    }

    /** 这一份东西能不能取、取出来是什么 */
    private static ItemStack takeFrom(@Nullable VatRecipe recipe, ItemStack content) {
        if (recipe != null) {
            for (VatRecipe.Slot slot : recipe.slots()) {
                if (takeable(slot, content)) {
                    return takeResult(slot, content);
                }
            }
        }
        for (VatRecipe other : VatRecipes.all()) {
            if (other == recipe) {
                continue;
            }
            for (VatRecipe.Slot slot : other.slots()) {
                if (takeable(slot, content)) {
                    return takeResult(slot, content);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 这一格里的这份东西能不能被玩家拿走。
     *
     * <p>两种能拿的：{@code KEEP}（还是原料的样子，取出来才换算成成品）和
     * {@code CONVERT}（完成时已经换过，缸里现在放的就是产物，例如酱渣）。
     */
    private static boolean takeable(VatRecipe.Slot slot, ItemStack content) {
        return switch (slot.fate()) {
            case KEEP -> slot.matcher().test(content);
            case CONVERT -> slot.product() != null && content.is(slot.product());
            case CONSUME, ABSORB -> false;
        };
    }

    private static ItemStack takeResult(VatRecipe.Slot slot, ItemStack content) {
        if (slot.fate() == VatRecipe.Fate.KEEP && slot.converter() != null) {
            return slot.converter().apply(content.copyWithCount(1));
        }
        return content.copyWithCount(1);
    }

    private static void absorbAll(VatBlockEntity vat, List<ItemStack> refund) {
        for (ItemStack content : vat.contentSnapshot()) {
            if (!absorbRefund(content, refund)) {
                continue;
            }
            vat.removeOneMatching(stack -> stack.is(content.getItem()));
        }
    }

    private static boolean absorbRefund(ItemStack content, List<ItemStack> refund) {
        for (VatRecipe recipe : VatRecipes.all()) {
            for (VatRecipe.Slot slot : recipe.slots()) {
                if (slot.fate() != VatRecipe.Fate.ABSORB || !slot.matcher().test(content)) {
                    continue;
                }
                if (slot.refund() != null) {
                    refund.add(new ItemStack(slot.refund()));
                }
                return true;
            }
        }
        return false;
    }

    public static int stepTicks(VatRecipe recipe, VatBlockEntity vat) {
        int total = recipe.secondsFor(Math.max(1, vat.waterLayers()), biomeBand(vat))
                * VatRecipes.TICKS_PER_SECOND;
        return Math.max(1, total / com.gunmu.northeast_china_delight.block.Vat.MAX_PROGRESS);
    }

    private static VatRecipe.BiomeBand biomeBand(VatBlockEntity vat) {
        var level = vat.getLevel();
        if (level == null) {
            return VatRecipe.BiomeBand.TEMPERATE;
        }
        return VatRecipe.BiomeBand.of(level.getBiome(vat.getBlockPos()).value().getBaseTemperature());
    }

}
