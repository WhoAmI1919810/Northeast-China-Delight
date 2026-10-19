package com.gunmu.northeast_china_delight.crafting;

import com.gunmu.northeast_china_delight.block.Vat;
import com.gunmu.northeast_china_delight.block.VatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 大缸的通用引擎。这里没有任何一条针对具体物品的规则：所有"收什么、要几份、什么时候能开工、完成后变成什么、取货能拿到什么"都来自
 * 数据包里的 {@link VatRecipe}。想加配方 / 改数量，动对应 JSON 即可；这个文件不需要改。
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
        return vat.hasBottledProduct();
    }

    @Nullable
    public static VatRecipe forInsert(VatBlockEntity vat, ItemStack stack) {
        if (stack.isEmpty() || vat.hasMissingUnfinishedRecipe()) {
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
        // 封口后到第一次 tick 之间 PROGRESS 还是 0，但配方已经绑定；必须把 recipeId 也算进去，
        // 否则这段窗口里还能塞进另一条配方的材料。
        boolean processing = !vat.isFermented() && (vat.recipeId() != null
                || vat.getBlockState().getValue(com.gunmu.northeast_china_delight.block.Vat.PROGRESS) > 0);

        VatRecipe best = null;
        int[] bestScore = null;
        for (VatRecipe recipe : VatRecipes.all(vat.getLevel())) {
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
            if (vat.isFermented() && (vat.recipeId() != null
                    ? recipe.id().equals(vat.recipeId()) : recipe.kind() == vat.kind())) {
                continue;
            }
            if (vat.isFermented() && !continuationFits(vat, recipe, now)) {
                continue;
            }
            if (processing && (vat.recipeId() != null
                    ? !recipe.id().equals(vat.recipeId()) : recipe.kind() != vat.kind())) {
                continue;
            }
            int[] score = score(recipe, after, sour, vat);
            if (bestScore == null || better(score, bestScore)) {
                best = recipe;
                bestScore = score;
            }
        }
        return best;
    }

    /**
     * 完成的缸能否再倒清水。不看上一条是什么，只看有没有另一条配方接得住现在的剩料，并且还要更多水。
     */
    public static boolean canContinueWithWater(VatBlockEntity vat) {
        if (!vat.isFermented() || vat.hasMissingUnfinishedRecipe()
                || hasBottledProduct(vat) || vat.sourWaterMb() > 0) {
            return false;
        }
        VatRecipe.Counts now = counts(vat, null);
        for (VatRecipe recipe : VatRecipes.all(vat.getLevel())) {
            if (isCurrent(vat, recipe) || recipe.liquid().waterMax() <= now.water()
                    || !recipe.canStillMatch(now.water(), 0) || !continuationFits(vat, recipe, now)) {
                continue;
            }
            return true;
        }
        return false;
    }

    public static boolean isCurrent(VatBlockEntity vat, VatRecipe recipe) {
        return vat.recipeId() != null
                ? recipe.id().equals(vat.recipeId()) : recipe.kind() == vat.kind();
    }

    /** 缸里现有的固体或酸引水，都能被这条配方当作原料，且没有超出数量上限。 */
    private static boolean continuationFits(VatBlockEntity vat, VatRecipe recipe, VatRecipe.Counts counts) {
        boolean matched = vat.sourWaterMb() > 0 && recipe.liquid().sourWaterMb() > 0;
        for (ItemStack content : vat.contentSnapshot()) {
            if (!fitsExisting(recipe, content, counts)) {
                return false;
            }
            matched = true;
        }
        return matched;
    }

    private static boolean fitsExisting(VatRecipe recipe, ItemStack content, VatRecipe.Counts counts) {
        for (int i = 0; i < recipe.slots().size(); i++) {
            VatRecipe.Slot slot = recipe.slots().get(i);
            if (!slot.matcher().test(content)) {
                continue;
            }
            int have = counts.count(slot.matcher());
            if (have > 0 && have <= recipe.boundsOf(i, counts).max()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static VatRecipe ready(VatBlockEntity vat) {
        if (vat.hasMissingUnfinishedRecipe()) {
            return null;
        }
        // 上一道工序的成品还没取完时，不允许开始下一道工序：
        // 否则新配方会消耗原料，而 addProduct 又因为缸里已有另一种液体而拒绝写入，成品直接丢失。
        if (vat.hasBottledProduct()) {
            return null;
        }
        VatRecipe.Counts counts = counts(vat, null);
        int sour = vat.sourWaterMb();
        VatRecipe best = null;
        int[] bestScore = null;
        for (VatRecipe recipe : VatRecipes.all(vat.getLevel())) {
            if (vat.recipeId() != null && !vat.isFermented()
                    && vat.getBlockState().getValue(com.gunmu.northeast_china_delight.block.Vat.PROGRESS) > 0
                    && !recipe.id().equals(vat.recipeId())) {
                continue;
            }
            if (vat.isFermented() && (isCurrent(vat, recipe) || !continuationFits(vat, recipe, counts))) {
                continue;
            }
            if (!recipe.materialsReady(counts, sour) || !sealMatches(vat, recipe)) {
                continue;
            }
            if (recipe.requiresSnowyBiome() && !isSnowyBiome(vat)) {
                continue;
            }
            if (sour > 0 && recipe.liquid().sourWaterMb() <= 0) {
                continue;
            }
            int[] score = score(recipe, counts, sour, vat);
            if (bestScore == null || better(score, bestScore)) {
                best = recipe;
                bestScore = score;
            }
        }
        return best;
    }

    @Nullable
    public static VatRecipe current(VatBlockEntity vat) {
        if (vat.hasMissingUnfinishedRecipe()) {
            return null;
        }
        VatRecipe.Counts counts = counts(vat, null);
        int sour = vat.sourWaterMb();
        VatRecipe best = null;
        int[] bestScore = null;
        for (VatRecipe recipe : VatRecipes.all(vat.getLevel())) {
            if (!recipe.canStillMatch(counts.water(), sour)) {
                continue;
            }
            if (sour > 0 && recipe.liquid().sourWaterMb() <= 0) {
                continue;
            }
            int[] score = score(recipe, counts, sour, vat);
            if (bestScore == null || better(score, bestScore)) {
                best = recipe;
                bestScore = score;
            }
        }
        return best;
    }

    /**
     * 手里这个封口物该盖到哪条配方上。
     *
     * <p>默认就是缸现在认的那条（{@link #current}）：先盖布、后倒水这种玩法靠的就是它。但「一缸两吃」会卡在旧
     * kind 上 —— 大酱 / 酱油取空液体、只剩酱渣时加水加谷物，缸里其实已经是醋的料，可 kind 还是 PASTE，
     * {@link #current} 里 sticky 压过满足的格子数，封口这一关只认地毯，粗布毯放不上去，缸永远转不到醋。
     * 所以当前配方不肯收这个封口物时，再找一条「材料都齐、只差封口」的配方。
     */
    @Nullable
    public static VatRecipe sealRecipe(VatBlockEntity vat, ItemStack stack) {
        if (vat.hasMissingUnfinishedRecipe()) {
            return null;
        }
        // 成品液体没取完时不能封口开工；先把液体装走，再继续下一道工序。
        if (vat.hasBottledProduct()) {
            return null;
        }
        VatRecipe now = current(vat);
        if (now != null && acceptsSeal(now, stack)) {
            return now;
        }
        VatRecipe.Counts counts = counts(vat, null);
        int sour = vat.sourWaterMb();
        VatRecipe best = null;
        int[] bestScore = null;
        for (VatRecipe recipe : VatRecipes.all(vat.getLevel())) {
            if (!acceptsSeal(recipe, stack) || !recipe.ingredientsReady(counts)
                    || !recipe.canStillMatch(counts.water(), sour)) {
                continue;
            }
            if (vat.isFermented() && (isCurrent(vat, recipe) || !continuationFits(vat, recipe, counts))) {
                continue;
            }
            if (recipe.requiresSnowyBiome() && !isSnowyBiome(vat)) {
                continue;
            }
            if (sour > 0 && recipe.liquid().sourWaterMb() <= 0) {
                continue;
            }
            int[] score = score(recipe, counts, sour, vat);
            if (bestScore == null || better(score, bestScore)) {
                best = recipe;
                bestScore = score;
            }
        }
        return best;
    }

    /** 手里这个封口物能不能盖在这条配方上：石 / 毯 / 布三大类对得上；「顶上必须压某一样东西」的还要物品对得上 */
    private static boolean acceptsSeal(VatRecipe recipe, ItemStack stack) {
        return switch (recipe.seal()) {
            case NONE -> false;
            case PRESS -> {
                java.util.function.Predicate<ItemStack> required = recipe.requiredSealItem();
                yield required != null ? required.test(stack) : Vat.isPressStone(stack);
            }
            case CARPET -> stack.is(ItemTags.WOOL_CARPETS);
            case CLOTH -> Vat.isClothRug(stack);
        };
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
                               VatBlockEntity vat) {
        boolean ready = recipe.materialsReady(counts, sourWaterMb);
        int satisfied = recipe.satisfiedSlots(counts);
        int sticky = vat.recipeId() != null ? (recipe.id().equals(vat.recipeId()) ? 1 : 0)
                : (recipe.kind() == vat.kind() ? 1 : 0);
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
        vat.setRecipe(recipe);
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
                net.minecraft.world.level.block.Block.UPDATE_CLIENTS
                        | net.minecraft.world.level.block.Block.UPDATE_NEIGHBORS);
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
                case CONSUME, ABSORB -> vat.removeOneMatching(same);
                case CONVERT -> {
                    if (slot.product() != null) {
                        vat.replaceOneMatching(same, new ItemStack(slot.product()));
                    }
                }
                case KEEP -> {
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
            net.minecraft.world.level.material.Fluid product = VatRecipes.productOf(vat);
            VatRecipes.Serving serving = VatRecipes.servingOf(vat);
            if (serving != null && held.is(serving.container()) && vat.productMb(product) > 0) {
                vat.takeOneServing(product);
                return new ItemStack(serving.item());
            }
        }
        if (vat.sourWaterMb() > 0 && held.is(net.minecraft.world.item.Items.GLASS_BOTTLE)) {
            vat.takeOneServing(com.gunmu.northeast_china_delight.fluid.ModFluids.sourWaterSource());
            return new ItemStack(com.gunmu.northeast_china_delight.item.ModItems.SOUR_WATER.get());
        }
        return null;
    }

    public static ItemStack takeOne(VatBlockEntity vat, List<ItemStack> refund) {
        absorbAll(vat, refund);

        VatRecipe recipe = VatRecipes.recipeOf(vat);
        for (ItemStack content : vat.contentSnapshot()) {
            ItemStack taken = takeFrom(vat, recipe, content);
            if (!taken.isEmpty()) {
                vat.removeOneMatching(stack -> stack.is(content.getItem()));
                return taken;
            }
        }
        return ItemStack.EMPTY;
    }

    /** 这一份东西能不能取、取出来是什么 */
    private static ItemStack takeFrom(VatBlockEntity vat, @Nullable VatRecipe recipe, ItemStack content) {
        if (recipe != null) {
            for (VatRecipe.Slot slot : recipe.slots()) {
                if (takeable(slot, content)) {
                    return takeResult(slot, content);
                }
            }
        }
        if (vat.recipeId() != null) {
            return content.copyWithCount(1);
        }
        for (VatRecipe other : VatRecipes.all(vat.getLevel())) {
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
            if (!absorbRefund(vat, content, refund)) {
                continue;
            }
            vat.removeOneMatching(stack -> stack.is(content.getItem()));
        }
    }

    private static boolean absorbRefund(VatBlockEntity vat, ItemStack content, List<ItemStack> refund) {
        VatRecipe bound = VatRecipes.recipeOf(vat);
        if (vat.recipeId() != null && bound == null) {
            return false;
        }
        for (VatRecipe recipe : vat.recipeId() == null
                ? VatRecipes.all(vat.getLevel()) : List.of(bound)) {
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
