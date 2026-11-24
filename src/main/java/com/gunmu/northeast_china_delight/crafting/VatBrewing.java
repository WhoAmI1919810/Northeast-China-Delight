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

    private record AssumedWaterCounts(VatBlockEntity vat, int water) implements VatRecipe.Counts {

        @Override
        public int count(java.util.function.Predicate<ItemStack> matcher) {
            return this.vat.count(matcher);
        }

        @Override
        public int water() {
            return this.water;
        }
    }

    private static boolean isSnowyBiome(VatBlockEntity vat) {
        var level = vat.getLevel();
        if (level == null) {
            return false;
        }
        return level.getBiome(vat.getBlockPos()).value().getBaseTemperature() < 0.15F;
    }

    /**
     * 当前参与配方匹配的非清水液体：
     * 已完成且还装着成品液体时取成品流体（用于判断是否允许原地连锁衔接下游配方），
     * 否则取输入槽里的非清水流体（酸引水或数据包自定义的任意输入流体）。
     */
    @Nullable
    private static net.minecraft.world.level.material.Fluid currentNonWaterFluid(VatBlockEntity vat) {
        if (vat.hasFinishedProductLiquid()) {
            net.minecraft.world.level.material.Fluid product = VatRecipes.productOf(vat);
            return product != null && !product.isSame(net.minecraft.world.level.material.Fluids.WATER)
                    && vat.productMb(product) > 0 ? product : null;
        }
        return vat.nonWaterInputFluid();
    }

    private static int currentNonWaterMb(VatBlockEntity vat,
                                         @Nullable net.minecraft.world.level.material.Fluid fluid) {
        if (fluid == null) {
            return 0;
        }
        if (vat.hasFinishedProductLiquid()) {
            return vat.productMb(fluid);
        }
        return vat.inputMb(fluid);
    }

    @Nullable
    public static VatRecipe forInsert(VatBlockEntity vat, ItemStack stack) {
        if (stack.isEmpty() || vat.hasMissingUnfinishedRecipe()) {
            return null;
        }
        VatRecipe.Counts now = counts(vat, null);
        VatRecipe.Counts after = counts(vat, stack);
        int water = now.water();
        net.minecraft.world.level.material.Fluid nonWater = currentNonWaterFluid(vat);
        int nonWaterMb = currentNonWaterMb(vat, nonWater);
        // 正在发酵的那一缸只收"同一条配方"的料（泡菜中途还能再塞菜，但不许中途改酿大酱）。
        // 揭开封口会解除绑定（Vat.interrupt），那之后就不受这条限制，可以补料改做别的。
        // 注意这里必须排掉"已经做完"的缸：完成时 PROGRESS 会写到 MAX_PROGRESS，
        // 把 FERMENTED 的缸也算成"发酵中"的话，二次发酵（泡菜酸引水→白醋、酱渣→醋、自定义流体连锁）就永远投不进料了。
        // 封口后到第一次 tick 之间 PROGRESS 还是 0，但配方已经绑定；必须把 recipeId 也算进去，
        // 否则这段窗口里还能塞进另一条配方的材料。
        boolean processing = !vat.isFermented() && (vat.recipeId() != null
                || vat.getBlockState().getValue(com.gunmu.northeast_china_delight.block.Vat.PROGRESS) > 0);

        VatRecipe best = null;
        int[] bestScore = null;
        for (VatRecipe recipe : VatRecipes.all(vat.getLevel())) {
            int slot = recipe.findSlotFor(stack, now);
            if (slot < 0 || !recipe.canStillMatch(water, nonWater, nonWaterMb)) {
                continue;
            }
            if (vat.isFermented() && isCurrent(vat, recipe)) {
                continue;
            }
            if (vat.isFermented() && !continuationFits(vat, recipe, now, nonWater, nonWaterMb)) {
                continue;
            }
            if (processing && !isCurrent(vat, recipe)) {
                continue;
            }
            int[] score = score(recipe, after, nonWater, nonWaterMb, vat);
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
                || vat.hasFinishedProductLiquid() || vat.nonWaterInputMb() > 0) {
            return false;
        }
        VatRecipe.Counts now = counts(vat, null);
        for (VatRecipe recipe : VatRecipes.all(vat.getLevel())) {
            if (isCurrent(vat, recipe) || recipe.liquid().waterMax() <= now.water()
                    || !recipe.canStillMatch(now.water(), null, 0)
                    || !continuationFits(vat, recipe, now, null, 0)) {
                continue;
            }
            return true;
        }
        return false;
    }

    /**
     * 数据驱动的流体输入上限查询（mB）：不写死任何流体名，完全按当前已加载的配方表推导。
     *
     * <p>返回在当前缸内状态（已有固体、是否二次发酵、是否已盖布）下，{@code fluid} 允许灌入的最高目标总量
     * （范围 {@code 0 .. VatRecipes.CAPACITY_MB}）；若无任何兼容配方接收该流体或存在混液冲突则返回 0。</p>
     */
    public static int maxFluidInputMb(VatBlockEntity vat,
                                      @Nullable net.minecraft.world.level.material.Fluid fluid) {
        if (fluid == null || fluid.isSame(net.minecraft.world.level.material.Fluids.EMPTY)
                || vat.getLevel() == null || vat.hasMissingUnfinishedRecipe()) {
            return 0;
        }
        // 已完成且还有未取走的成品液体 → 成品态（只出不进）
        if (vat.hasFinishedProductLiquid()) {
            return 0;
        }
        // 已有另一种输入液体时绝不混液
        if (vat.inputMb() > 0 && vat.inputFluid() != null && !vat.inputFluid().isSame(fluid)) {
            return 0;
        }
        boolean isWater = fluid.isSame(net.minecraft.world.level.material.Fluids.WATER);
        VatRecipe.Counts now = counts(vat, null);
        List<ItemStack> existing = vat.contentSnapshot();
        boolean hasSolids = !existing.isEmpty();

        int maxMb = 0;
        for (VatRecipe recipe : VatRecipes.all(vat.getLevel())) {
            if (!recipe.liquid().acceptsInputFluid(fluid)) {
                continue;
            }
            if (vat.isCovered() && !sealMatches(vat, recipe)) {
                continue;
            }
            VatRecipe.Counts fitCounts = isWater
                    ? new AssumedWaterCounts(vat, Math.max(now.water(), recipe.liquid().waterMax()))
                    : now;
            if (vat.isFermented()) {
                if (isCurrent(vat, recipe) || !continuationFits(vat, recipe, fitCounts, null, 0)) {
                    continue;
                }
            } else if (hasSolids && !allExistingFit(recipe, existing, fitCounts)) {
                continue;
            }
            int recipeCap = isWater
                    ? recipe.liquid().waterMax() * VatRecipes.WATER_MB_PER_LEVEL
                    : recipe.liquid().inputMb();
            if (recipeCap > maxMb) {
                maxMb = recipeCap;
            }
        }
        return Math.min(VatRecipes.CAPACITY_MB, maxMb);
    }

    private static boolean allExistingFit(VatRecipe recipe, List<ItemStack> existing, VatRecipe.Counts counts) {
        for (ItemStack content : existing) {
            if (!fitsExisting(recipe, content, counts)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isCurrent(VatBlockEntity vat, VatRecipe recipe) {
        return vat.recipeId() != null
                ? recipe.id().equals(vat.recipeId()) : recipe.kind() == vat.kind();
    }

    /** 缸里现有的固体或上一道工序留下的成品流体，都能被这条配方当作原料，且没有超出数量上限。 */
    private static boolean continuationFits(VatBlockEntity vat, VatRecipe recipe, VatRecipe.Counts counts,
                                            @Nullable net.minecraft.world.level.material.Fluid nonWater,
                                            int nonWaterMb) {
        boolean hasNonWater = nonWater != null && nonWaterMb > 0;
        boolean matched = hasNonWater && recipe.liquid().acceptsInputFluid(nonWater);
        if (hasNonWater && !matched) {
            return false;
        }
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
        // 未完成态不应带着成品液开工；已完成态则交给下面的 continuationFits 判定是否属于合法流体连锁配方。
        if (!vat.isFermented() && vat.hasBottledProduct()) {
            return null;
        }
        VatRecipe.Counts counts = counts(vat, null);
        net.minecraft.world.level.material.Fluid nonWater = currentNonWaterFluid(vat);
        int nonWaterMb = currentNonWaterMb(vat, nonWater);
        VatRecipe best = null;
        int[] bestScore = null;
        for (VatRecipe recipe : VatRecipes.all(vat.getLevel())) {
            if (vat.recipeId() != null && !vat.isFermented()
                    && vat.getBlockState().getValue(com.gunmu.northeast_china_delight.block.Vat.PROGRESS) > 0
                    && !recipe.id().equals(vat.recipeId())) {
                continue;
            }
            if (vat.isFermented()
                    && (isCurrent(vat, recipe) || !continuationFits(vat, recipe, counts, nonWater, nonWaterMb))) {
                continue;
            }
            if (!recipe.materialsReady(counts, nonWater, nonWaterMb) || !sealMatches(vat, recipe)) {
                continue;
            }
            if (recipe.requiresSnowyBiome() && !isSnowyBiome(vat)) {
                continue;
            }
            int[] score = score(recipe, counts, nonWater, nonWaterMb, vat);
            if (bestScore == null || better(score, bestScore)) {
                best = recipe;
                bestScore = score;
            }
        }
        return best;
    }

    /** 料、液体、封口都齐了，只差不在会下雪的群系（冻梨、冻豆腐）：封口时换一句提示，别说成"材料没凑齐" */
    public static boolean onlyNeedsSnowyBiome(VatBlockEntity vat) {
        if (vat.hasMissingUnfinishedRecipe() || (!vat.isFermented() && vat.hasBottledProduct())
                || isSnowyBiome(vat)) {
            return false;
        }
        VatRecipe.Counts counts = counts(vat, null);
        net.minecraft.world.level.material.Fluid nonWater = currentNonWaterFluid(vat);
        int nonWaterMb = currentNonWaterMb(vat, nonWater);
        for (VatRecipe recipe : VatRecipes.all(vat.getLevel())) {
            if (vat.isFermented()
                    && (isCurrent(vat, recipe) || !continuationFits(vat, recipe, counts, nonWater, nonWaterMb))) {
                continue;
            }
            if (recipe.requiresSnowyBiome()
                    && recipe.materialsReady(counts, nonWater, nonWaterMb)
                    && sealMatches(vat, recipe)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static VatRecipe current(VatBlockEntity vat) {
        if (vat.hasMissingUnfinishedRecipe()) {
            return null;
        }
        VatRecipe.Counts counts = counts(vat, null);
        net.minecraft.world.level.material.Fluid nonWater = currentNonWaterFluid(vat);
        int nonWaterMb = currentNonWaterMb(vat, nonWater);
        VatRecipe best = null;
        int[] bestScore = null;
        for (VatRecipe recipe : VatRecipes.all(vat.getLevel())) {
            if (!recipe.canStillMatch(counts.water(), nonWater, nonWaterMb)) {
                continue;
            }
            int[] score = score(recipe, counts, nonWater, nonWaterMb, vat);
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
     * 所以当前配方不肯收这个封口物时，再找一条「材料都齐、只差封口」的配方。</p>
     */
    @Nullable
    public static VatRecipe sealRecipe(VatBlockEntity vat, ItemStack stack) {
        if (vat.hasMissingUnfinishedRecipe()) {
            return null;
        }
        if (!vat.isFermented() && vat.hasBottledProduct()) {
            return null;
        }
        VatRecipe.Counts counts = counts(vat, null);
        net.minecraft.world.level.material.Fluid nonWater = currentNonWaterFluid(vat);
        int nonWaterMb = currentNonWaterMb(vat, nonWater);
        if (!vat.isFermented()) {
            VatRecipe now = current(vat);
            if (now != null && acceptsSeal(now, stack)) {
                return now;
            }
        }
        VatRecipe best = null;
        int[] bestScore = null;
        for (VatRecipe recipe : VatRecipes.all(vat.getLevel())) {
            if (!acceptsSeal(recipe, stack) || !recipe.ingredientsReady(counts)
                    || !recipe.canStillMatch(counts.water(), nonWater, nonWaterMb)) {
                continue;
            }
            if (vat.isFermented()
                    && (isCurrent(vat, recipe) || !continuationFits(vat, recipe, counts, nonWater, nonWaterMb))) {
                continue;
            }
            if (recipe.requiresSnowyBiome() && !isSnowyBiome(vat)) {
                continue;
            }
            int[] score = score(recipe, counts, nonWater, nonWaterMb, vat);
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
    private static int[] score(VatRecipe recipe, VatRecipe.Counts counts,
                               @Nullable net.minecraft.world.level.material.Fluid nonWater, int nonWaterMb,
                               VatBlockEntity vat) {
        boolean ready = recipe.materialsReady(counts, nonWater, nonWaterMb);
        int satisfied = recipe.satisfiedSlots(counts);
        int sticky = vat.recipeId() != null ? (recipe.id().equals(vat.recipeId()) ? 1 : 0)
                : (recipe.kind() == vat.kind() ? 1 : 0);
        int liquid = recipe.liquidFits(counts.water(), nonWater, nonWaterMb) ? 1 : 0;
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
        if (liquid.requiresInputFluid()) {
            switch (liquid.inputAfter()) {
                case CLEAR -> vat.setInputFluid(null, 0);
                case DRAIN_REQUIRED -> vat.setInputFluid(liquid.inputFluid(),
                        Math.max(0, vat.inputMb(liquid.inputFluid()) - liquid.inputMb()));
                case KEEP -> {
                }
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
        // 还没开工的缸（没绑配方、没计进度）里倒进去的酸引水，也能再用玻璃瓶装回来，倒多了、改主意了不至于白丢
        boolean notStarted = vat.recipeId() == null
                && vat.getBlockState().getValue(com.gunmu.northeast_china_delight.block.Vat.PROGRESS) == 0;
        if ((!vat.isFermented() && !notStarted) || vat.isCovered()) {
            return null;
        }
        VatRecipes.Kind kind = vat.kind();
        if (vat.isFermented() && kind != VatRecipes.Kind.NONE) {
            net.minecraft.world.level.material.Fluid product = VatRecipes.productOf(vat);
            VatRecipes.Serving serving = VatRecipes.servingOf(vat);
            if (serving != null && held.is(serving.container()) && product != null) {
                int have = vat.productMb(product);
                if (serving.item() instanceof com.gunmu.northeast_china_delight.item.SeasoningBottleItem) {
                    if (have > 0) {
                        int moved = Math.min(VatRecipes.SERVING_MB, have);
                        vat.setProductMb(product, have - moved);
                        ItemStack bottle = new ItemStack(serving.item());
                        int missing = com.gunmu.northeast_china_delight.item.SeasoningBottleItem.CAPACITY_MB - moved;
                        if (missing > 0) {
                            bottle.setDamageValue(missing);
                        }
                        return bottle;
                    }
                } else if (have >= VatRecipes.SERVING_MB && vat.takeOneServing(product)) {
                    return new ItemStack(serving.item());
                }
            }
        }
        net.minecraft.world.level.material.Fluid input = vat.nonWaterInputFluid();
        if (input != null && vat.inputMb(input) >= VatRecipes.SERVING_MB) {
            VatRecipes.Serving serving = VatRecipes.servingOf(input);
            if (serving != null && held.is(serving.container()) && vat.takeInputServing(input)) {
                return new ItemStack(serving.item());
            }
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
