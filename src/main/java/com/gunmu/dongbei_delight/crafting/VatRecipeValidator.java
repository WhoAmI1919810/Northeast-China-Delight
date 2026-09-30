package com.gunmu.dongbei_delight.crafting;

import com.gunmu.dongbei_delight.block.VatBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 大缸配方注册表的**自检**（新增配方时防踩坑）。
 *
 * <p>声明式配方的代价是"写错了不会报错"：份数算不出来、JEI 一页装不下、
 * 两个格子收同一种东西……这些在代码里都是合法写法，只有玩到才发现。
 * 所以注册表一建好（{@link VatRecipes#all()}）就把下面这些规则跑一遍，把问题写进日志：
 *
 * <ol>
 *   <li><b>Kind 与配方一对一</b>：大缸里只记 Kind，取货 / 流体 / Jade 都按 Kind 反查配方，
 *       同一个 Kind 出现两条配方就会互相串味；</li>
 *   <li><b>展示的份数真的能开工</b>：把 JEI 那一档算出来的份数喂回 {@link VatRecipe#materialsReady}
 *       验一遍 —— "盐数 = 肉数"这类跨格规则算错（例如 JEI 上写 6 肉 1 盐）就是靠这条兜住的；</li>
 *   <li><b>一缸装得下</b>：各格上限之和不能超过 {@link VatBlockEntity#MAX_ENTRIES}，
 *       否则玩家永远放不满，表现为"点了没反应"；</li>
 *   <li><b>JEI 一页画得下</b>：一层最多 {@link #MAX_SLOTS_PER_LAYER} 格、整页最多 {@link #MAX_ROWS} 行
 *       （超了会数组越界或者画到页面外）；</li>
 *   <li><b>同一配方里格子不重叠</b>：投料找格、完成后的去向都只看第一个匹配的格子，
 *       两个格子收同一种东西就会各写各的；</li>
 *   <li><b>数值自洽</b>：min ≤ max、水位区间不反、时长 &gt; 0、CONVERT 必须写产物、每个 Kind 都得有配方。</li>
 * </ol>
 *
 * <p>返回的问题列表里以 {@code WARN} 开头的只是提醒（可能是有意为之），其余按错误处理。
 * 自检只写日志、不抛异常 —— 它不该把游戏拦下来。
 */
public final class VatRecipeValidator {

    private VatRecipeValidator() {
    }

    /** JEI 一页左侧"一层"最多画几格（与 VatRecipeCategory 的槽位名数组长度一致） */
    public static final int MAX_SLOTS_PER_LAYER = 4;
    /** JEI 一页最多几行（与 VatRecipeCategory 的 WIDTH × HEIGHT 一致：5 行 = 88px 刚好放得下） */
    public static final int MAX_ROWS = 5;

    /** 跑一遍全部规则，返回问题清单（空 = 全过） */
    public static List<String> check(List<VatRecipe> recipes) {
        List<String> problems = new ArrayList<>();
        Map<VatRecipes.Kind, String> kinds = new LinkedHashMap<>();
        List<ItemStack> everyItem = allItems();
        for (VatRecipe recipe : recipes) {
            checkKindUnique(recipe, kinds, problems);
            checkNumbers(recipe, problems);
            checkJeiCapacity(recipe, problems);
            checkFillable(recipe, problems);
            checkDisplayedCounts(recipe, problems);
            checkOverlappingSlots(recipe, everyItem, problems);
        }
        for (VatRecipes.Kind kind : VatRecipes.Kind.values()) {
            if (kind != VatRecipes.Kind.NONE && !kinds.containsKey(kind)) {
                problems.add("Kind." + kind + " 没有任何配方：大缸永远不会进入这个状态");
            }
        }
        return problems;
    }

    // ===== 各条规则 =====

    private static void checkKindUnique(VatRecipe recipe, Map<VatRecipes.Kind, String> kinds, List<String> problems) {
        if (recipe.kind() == VatRecipes.Kind.NONE) {
            problems.add("配方 " + recipe.id() + " 用的是 Kind.NONE：空缸不是一个配方，取货规则会认不出来");
            return;
        }
        String other = kinds.putIfAbsent(recipe.kind(), recipe.id());
        if (other != null) {
            problems.add("Kind." + recipe.kind() + " 同时被配方 " + other + " 和 " + recipe.id()
                    + " 使用：大缸只记 Kind，取货 / 流体 / Jade 都会取到 priority 高的那一条");
        }
    }

    private static void checkNumbers(VatRecipe recipe, List<String> problems) {
        if (recipe.slots().isEmpty()) {
            problems.add("配方 " + recipe.id() + " 一个投料格都没有");
        }
        if (recipe.seconds() <= 0) {
            problems.add("配方 " + recipe.id() + " 的时长不是正数：" + recipe.seconds());
        }
        if (recipe.liquid().waterMin() > recipe.liquid().waterMax()) {
            problems.add("配方 " + recipe.id() + " 的水位区间反了："
                    + recipe.liquid().waterMin() + " ~ " + recipe.liquid().waterMax());
        }
        int[] blank = new int[recipe.slots().size()];
        for (int i = 0; i < recipe.slots().size(); i++) {
            VatRecipe.Slot slot = recipe.slots().get(i);
            if (slot.fate() == VatRecipe.Fate.CONVERT && slot.product() == null) {
                problems.add("配方 " + recipe.id() + " 第 " + i + " 格是 CONVERT（完成时变成另一样东西）却没写产物");
            }
            for (int level : levelsOf(recipe)) {
                VatRecipe.Bounds bounds = slot.bounds().get(new VatRecipe.AssumedCounts(level, blank, recipe));
                if (bounds.min() > bounds.max()) {
                    problems.add("配方 " + recipe.id() + " 第 " + i + " 格在 " + level + " 层水时下限大于上限："
                            + bounds.min() + " > " + bounds.max());
                }
            }
        }
    }

    /** JEI 一页的容量：一层最多 4 格、整页最多 5 行（含封口物与缸底液体各一行） */
    private static void checkJeiCapacity(VatRecipe recipe, List<String> problems) {
        int seasoning = 0;
        int primary = 0;
        for (VatRecipe.Slot slot : recipe.slots()) {
            if (slot.layer() == VatRecipe.Layer.SEASONING) {
                seasoning++;
            } else {
                primary++;
            }
        }
        if (seasoning > MAX_SLOTS_PER_LAYER || primary > MAX_SLOTS_PER_LAYER) {
            problems.add("配方 " + recipe.id() + " 的投料格太多（配料 " + seasoning + " / 食材 " + primary
                    + "）：JEI 一层最多画 " + MAX_SLOTS_PER_LAYER + " 格，再多会数组越界");
        }
        int rows = (recipe.seal() == VatRecipe.Seal.NONE ? 0 : 1)
                + layerRows(seasoning) + layerRows(primary)
                + (recipe.liquid().waterMin() > 0 || recipe.liquid().sourWaterMb() > 0 ? 1 : 0);
        if (rows > MAX_ROWS) {
            problems.add("配方 " + recipe.id() + " 在 JEI 里要占 " + rows + " 行，超过一页的 "
                    + MAX_ROWS + " 行（把分类画布加大，或者拆配方）");
        }
    }

    /** 一缸装得下：各格上限之和 ≤ MAX_ENTRIES，否则放满了也凑不齐 */
    private static void checkFillable(VatRecipe recipe, List<String> problems) {
        for (int level : levelsOf(recipe)) {
            int[] counts = recipe.displayedCounts(level);
            long lowest = 0;
            long highest = 0;
            for (int i = 0; i < recipe.slots().size(); i++) {
                lowest += Math.max(1, recipe.slots().get(i).bounds()
                        .get(new VatRecipe.AssumedCounts(level, counts, recipe)).min());
                VatRecipe.Bounds bounds = recipe.slots().get(i).bounds()
                        .get(new VatRecipe.AssumedCounts(level, counts, recipe));
                highest += bounds.max() == Integer.MAX_VALUE ? bounds.min() : bounds.max();
            }
            if (lowest > VatBlockEntity.MAX_ENTRIES || highest > VatBlockEntity.MAX_ENTRIES) {
                problems.add("配方 " + recipe.id() + " 在 " + level + " 层水时最多要放 " + highest
                        + " 份料（至少 " + lowest + "），超过大缸能装的 "
                        + VatBlockEntity.MAX_ENTRIES + " 份");
            }
        }
    }

    /** JEI 上写的份数必须真的能开工（"盐数 = 肉数"这类跨格规则就是靠这条兜住的） */
    private static void checkDisplayedCounts(VatRecipe recipe, List<String> problems) {
        for (int level : levelsOf(recipe)) {
            int[] counts = recipe.displayedCounts(level);
            VatRecipe.Counts view = new VatRecipe.AssumedCounts(level, counts, recipe);
            if (!recipe.materialsReady(view, recipe.liquid().sourWaterMb())) {
                problems.add("配方 " + recipe.id() + " 在 " + level + " 层水时按规则算出来的份数是 "
                        + Arrays.toString(counts) + "，照这个数投料开不了工（JEI 上会写错份数）");
            }
        }
    }

    /** 同一配方里两个格子收同一种东西：投料与"完成后的去向"都只看第一个匹配的格子 */
    private static void checkOverlappingSlots(VatRecipe recipe, List<ItemStack> everyItem, List<String> problems) {
        List<VatRecipe.Slot> slots = recipe.slots();
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                for (ItemStack stack : everyItem) {
                    if (slots.get(i).matcher().test(stack) && slots.get(j).matcher().test(stack)) {
                        problems.add("WARN 配方 " + recipe.id() + " 的第 " + i + " 格和第 " + j + " 格都收 "
                                + BuiltInRegistries.ITEM.getKey(stack.getItem())
                                + "：投料只会落进前面那一格，完成后也只会按那一格的去向处理");
                        break;
                    }
                }
            }
        }
    }

    // ===== 小工具 =====

    /** 这条配方在 JEI 上会演示哪几档水位（和 VatJeiRecipes 保持一致） */
    private static int[] levelsOf(VatRecipe recipe) {
        VatRecipe.Liquid liquid = recipe.liquid();
        if (liquid.waterMin() <= 0) {
            return new int[] { 0 };
        }
        int count = liquid.waterMax() - liquid.waterMin() + 1;
        int[] levels = new int[count];
        for (int i = 0; i < count; i++) {
            levels[i] = liquid.waterMin() + i;
        }
        return levels;
    }

    private static int layerRows(int slots) {
        return slots <= 0 ? 0 : (slots + 1) / 2;
    }

    private static List<ItemStack> allItems() {
        List<ItemStack> list = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR) {
                list.add(new ItemStack(item));
            }
        }
        return list;
    }
}
