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
 * 大缸的**通用引擎**。
 *
 * <p>这里没有任何一条针对具体物品的规则：所有"收什么、要几份、什么时候能开工、
 * 完成后变成什么、取货能拿到什么"都来自 {@link VatRecipes#REGISTRY} 里的 {@link VatRecipe}。
 * 想加配方 / 改数量，动那张表就行；这个文件不需要改。
 *
 * <p>三个时间点各有一个入口：
 * <ul>
 *   <li>投料：{@link #forInsert} 挑出"放这份东西会变成哪条配方"（顺便判断能不能放）；</li>
 *   <li>开工：{@link #ready} 挑出"现在材料配齐、封口也对，可以计时的那条配方"；</li>
 *   <li>完成 / 取货：{@link #complete}、{@link #takeOne}。</li>
 * </ul>
 */
public final class VatBrewing {

    private VatBrewing() {
    }

    // ===== 缸内情况的适配 =====

    /** 把大缸的内容物读成配方看得懂的数字；{@code pending} 不为空时先把它算进去（"放了会怎样"） */
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

    /** 大缸所在的地方是不是「会下雪的生物群系」：基准温度 < 0.15（和落雪的阈值一条线） */
    private static boolean isSnowyBiome(VatBlockEntity vat) {
        var level = vat.getLevel();
        if (level == null) {
            return false;
        }
        return level.getBiome(vat.getBlockPos()).value().getBaseTemperature() < 0.15F;
    }

    /** 缸里的"成品液体"（不含酸引水 —— 酸引水是白醋的原料，不算成品） */
    private static boolean hasBottledProduct(VatBlockEntity vat) {
        return vat.pasteMb() > 0 || vat.soySauceMb() > 0 || vat.vinegarMb() > 0
                || vat.whiteVinegarMb() > 0 || vat.fishSauceMb() > 0
                || vat.shrimpPasteMb() > 0 || vat.kvassMb() > 0;
    }

    // ===== 选配方 =====

    /**
     * 放这一份东西之后，这缸最像哪条配方（只看数量与液体，不看封口）。
     *
     * <p>打分顺序见 {@link #score}：满足的格子多 &gt; 能开工 &gt; 本来就是这条配方 &gt;
     * 液体对得上 &gt; 要求的总份数少 &gt; priority。
     * 这样"大酱加小麦变酱油""5 条鱼还是咸鱼、第 6 条转鱼露"这些都会自然发生，不需要任何专门的分支。
     *
     * @return 收得下这份东西、且这缸现在的液体还配得上的那条配方；都不行返回 null
     */
    @Nullable
    public static VatRecipe forInsert(VatBlockEntity vat, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        // 判"还有没有位置"用放进去**之前**的数量；判"放进去之后像哪条配方"用**之后**的数量
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
            // 缸里已经有酸引水：只有"拿酸引水当原料"的配方（白醋）能继续，别的料放不进去
            if (sourPot && recipe.liquid().sourWaterMb() <= 0) {
                continue;
            }
            // 成品液体还没取空：先取液体，别往里加料，否则这缸液体会取不出来
            if (bottled) {
                continue;
            }
            // 这一轮已经做完了：不许再往同一批里加料
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
    /**
     * 现在这缸能开工的那条配方：材料配齐 + 液体对 + 封口物对。
     *
     * @return 可以开始计时的那条配方；条件不够时返回 null
     */
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
            // 这条配方要求「会下雪的群系」（冻梨）：缸不在那种地方就不开工
            if (recipe.requiresSnowyBiome() && !isSnowyBiome(vat)) {
                continue;
            }
            // 缸里已经有酸引水：只有"拿酸引水当原料"的配方能开工（白醋）
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

    /**
     * 这缸现在最像哪条配方（不看封口、也不管材料够不够）。
     * 用来回答"该蒙什么"：压缸石、羊毛地毯还是粗布毯。
     */
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

    /** 封口物是否符合这条配方的要求 */
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
        // requiredSealItem 只查「缸顶压的那一坨」（press 槽位）：毯子那条路天然走不通
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
        // 缸已经认准了哪种加工（kind 已设），同种配方优先 —— 放盐进咸鱼缸继续走咸鱼，
        // 而不是被 satisfied 更多的辣白菜抢走
        int sticky = recipe.kind() == current ? 1 : 0;
        int liquid = recipe.liquidFits(counts.water(), sourWaterMb) ? 1 : 0;
        // 精确命中：材料配齐且每格都没多投（exact 类配方一份不多）。
        // 这一维在 satisfied 之后、ready 之前，保证"正好配齐"的那条压过"顺手多放了一把"的那条。
        // 6 鱼 3 盐对鱼露（exact 6/3）和咸鱼（盐 = 鱼数的一半）都是刚好的份数，
        // 这时按最后一维 priority 判给鱼露；1 鱼 1 盐的咸鱼仍然赢过只放了 1 条鱼的鱼露。
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

    // ===== 完成 =====

    /**
     * 把一条配方"完成"的那一下做掉：液体怎么变、产出什么、每格的东西变成什么，
     * 全部按配方里写的来。方块状态（FERMENTED / PROGRESS / 液面显示高度）最后统一刷一次。
     */
    public static void complete(ServerLevel level, BlockPos pos, VatBlockEntity vat, VatRecipe recipe) {
        int layers = Math.max(1, vat.waterLayers());
        VatRecipe.Liquid liquid = recipe.liquid();

        // 1. 缸里原有的液体
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

        // 2. 产出的液体（泡菜：几层水就出几层酸引水）
        if (liquid.product() != null) {
            vat.addProduct(liquid.product(), recipe.productMbFor(layers));
        }

        // 3. 每一格的去向
        applyFates(vat, recipe);

        // 4. 最后刷方块状态：液面高度要按"液体已经变过之后"的值来定
        BlockState current = level.getBlockState(pos);
        level.setBlock(pos, current
                .setValue(com.gunmu.northeast_china_delight.block.Vat.PROGRESS,
                        com.gunmu.northeast_china_delight.block.Vat.MAX_PROGRESS)
                .setValue(com.gunmu.northeast_china_delight.block.Vat.FERMENTED, true),
                net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        level.playSound(null, pos, SoundEvents.COMPOSTER_READY, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /** 完成时按 {@link VatRecipe.Fate} 处理每一格的内容物 */
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
                // KEEP 留着等玩家取；ABSORB 也留着（还要影响颜色），取货那次才清掉
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

    // ===== 取货 =====

    /**
     * 用碗 / 玻璃瓶从缸里装走一份液体。
     *
     * <p>先装这一缸自己的成品（大酱用碗、酱油 / 醋 / 鱼露 / 虾酱 / 格瓦斯用玻璃瓶），
     * 成品取空了之后，缸里剩下的酸引水也可以用玻璃瓶装出来。
     *
     * @return 装出来的物品；手里的容器装不了这一缸时返回 null（调用方继续走别的分支）
     */
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
        // 泡菜腌完剩下的酸引水（白醋酿完之后剩下的也在里面）：玻璃瓶能装
        if (vat.sourWaterMb() > 0 && held.is(net.minecraft.world.item.Items.GLASS_BOTTLE)) {
            vat.takeOneServing(VatRecipe.Fluid.SOUR_WATER);
            return new ItemStack(com.gunmu.northeast_china_delight.item.ModItems.SOUR_WATER.get());
        }
        return null;
    }

    /** 这一缸记着的成品液体（{@code NONE} 时给个占位，调用方已经先挡掉了） */
    private static VatRecipe.Fluid vatProduct(VatRecipes.Kind kind) {
        VatRecipe.Fluid fluid = VatRecipes.productOf(kind);
        return fluid == null ? VatRecipe.Fluid.WATER : fluid;
    }

    /**
     * 取一份东西出来。
     *
     * @param vat    大缸
     * @param refund 取货时要还给玩家的容器（辣椒酱的碗、鱼露的玻璃瓶）会加到这里
     * @return 玩家拿到的东西；缸里没有能取的东西时返回空
     */
    public static ItemStack takeOne(VatBlockEntity vat, List<ItemStack> refund) {
        // 被吸收的调料（盐、辣椒酱、鱼露…）到这一刻才真正消失，容器先还给玩家
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
        // 这缸是从别的配方转过来的（比如腌好的泡菜缸接着酿白醋），
        // 缸里可能还留着上一条配方的东西，那就按那条配方的规则取。
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

    /** 拿走这一格里的东西时给玩家什么 */
    private static ItemStack takeResult(VatRecipe.Slot slot, ItemStack content) {
        if (slot.fate() == VatRecipe.Fate.KEEP && slot.converter() != null) {
            return slot.converter().apply(content.copyWithCount(1));
        }
        return content.copyWithCount(1);
    }

    /** 清掉"被吸收"的调料，返回要还给玩家的容器 */
    private static void absorbAll(VatBlockEntity vat, List<ItemStack> refund) {
        for (ItemStack content : vat.contentSnapshot()) {
            if (!absorbRefund(content, refund)) {
                continue;
            }
            vat.removeOneMatching(stack -> stack.is(content.getItem()));
        }
    }

    /** 这份东西是不是"被吸收"的调料；是的话把该还的容器加进 refund */
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

    // ===== 时长 =====

    /** 每走一格进度条要多少刻（会带上群系温度带的快慢） */
    public static int stepTicks(VatRecipe recipe, VatBlockEntity vat) {
        int total = recipe.secondsFor(Math.max(1, vat.waterLayers()), biomeBand(vat))
                * VatRecipes.TICKS_PER_SECOND;
        return Math.max(1, total / com.gunmu.northeast_china_delight.block.Vat.MAX_PROGRESS);
    }

    /** 大缸所在群系的温度带；取不到世界时按温带算 */
    private static VatRecipe.BiomeBand biomeBand(VatBlockEntity vat) {
        var level = vat.getLevel();
        if (level == null) {
            return VatRecipe.BiomeBand.TEMPERATE;
        }
        return VatRecipe.BiomeBand.of(level.getBiome(vat.getBlockPos()).value().getBaseTemperature());
    }

}
