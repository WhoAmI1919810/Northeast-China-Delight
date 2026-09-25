package com.gunmu.dongbei_delight.compat.jei;

import com.gunmu.dongbei_delight.crafting.VatRecipe;
import com.gunmu.dongbei_delight.crafting.VatRecipes;
import com.gunmu.dongbei_delight.fluid.ModFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * 大缸在 JEI 里展示的全部配方 —— **从 {@link VatRecipes#REGISTRY} 自动生成**，
 * 这里不再手写任何一份"投料 / 时长 / 产物"的副本，所以游戏里的判定和 JEI 页面永远一致。
 *
 * <p>生成规则：
 * <ol>
 *   <li>一格收什么 → 直接拿这一格的匹配器去扫一遍物品表，扫出来的物品就是"能放进去的东西"
 *       （所以"任意生鱼""任意泡菜用蔬菜"这种标签 / 映射会自己展开）；</li>
 *   <li>要几份 → 用这一格的**数量规则**按水位算出来（一层水两份菜、盐数和肉数一样，都算得出来）；</li>
 *   <li>产出什么 → {@code KEEP} 的格子用它的换算规则（大白菜→酸白菜、生猪排→咸腊肉），
 *       {@code CONVERT} 的格子直接用它变成的东西（酱块→酱渣）；</li>
 *   <li>封口物、发酵时长、液体（缸底的原料液体与酿出来的成品液体）都照抄配方。</li>
 * </ol>
 *
 * <p>"投什么"有多个候选的配方（泡菜有四种菜、咸鱼有各种鱼）会一页展开一样；
 * 按水位配比的配方（泡菜、辣白菜、酸玉米粒）会做成多档，JEI 里自动轮换着演示。
 */
public final class VatJeiRecipes {

    private VatJeiRecipes() {
    }

    /** 一格最多展示几种候选，免得某个宽松的匹配器把半张物品表都扫进来 */
    private static final int MAX_CANDIDATES = 12;
    /** 一层水 = 1 桶 */
    private static final long ONE_LAYER = VatRecipes.WATER_MB_PER_LEVEL;

    private static List<VatJeiRecipe> recipes;

    public static List<VatJeiRecipe> all() {
        if (recipes == null) {
            recipes = build();
        }
        return recipes;
    }

    private static List<VatJeiRecipe> build() {
        List<VatJeiRecipe> list = new ArrayList<>();
        for (VatRecipe recipe : VatRecipes.all()) {
            list.addAll(expand(recipe));
        }
        return List.copyOf(list);
    }

    // ===== 一条配方 → 若干页 =====

    /**
     * 一条配方展开成 JEI 上的页。
     *
     * <p>每个投料格的候选物品先扫出来；第一个有多个候选的格子当"页码驱动"（一样一页），
     * 其余的格子在同一格里轮播（表示任选其一，比如辣白菜的鱼露 / 虾酱）。
     */
    private static List<VatJeiRecipe> expand(VatRecipe recipe) {
        List<VatRecipe.Slot> slots = recipe.slots();
        List<List<ItemStack>> candidates = new ArrayList<>(slots.size());
        for (VatRecipe.Slot slot : slots) {
            candidates.add(candidatesOf(slot));
        }
        int driver = -1;
        for (int i = 0; i < slots.size(); i++) {
            if (candidates.get(i).size() > 1) {
                driver = i;
                break;
            }
        }
        int pages = driver < 0 ? 1 : candidates.get(driver).size();
        List<VatJeiRecipe> out = new ArrayList<>(pages);
        for (int page = 0; page < pages; page++) {
            List<VatJeiRecipe.State> states = new ArrayList<>();
            for (int level : levelsOf(recipe)) {
                states.add(stateOf(recipe, candidates, driver, page, level));
            }
            out.add(new VatJeiRecipe(List.copyOf(states)));
        }
        return out;
    }

    /** 按水位配比的配方要把每一档水位都演示一遍（大缸一层水配两份菜） */
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

    private static VatJeiRecipe.State stateOf(VatRecipe recipe, List<List<ItemStack>> candidates,
                                             int driver, int page, int level) {
        int[] counts = resolveCounts(recipe, candidates, level);
        List<List<ItemStack>> seasoning = new ArrayList<>();
        List<List<ItemStack>> primary = new ArrayList<>();
        List<ItemStack> results = new ArrayList<>();

        for (int i = 0; i < recipe.slots().size(); i++) {
            VatRecipe.Slot slot = recipe.slots().get(i);
            List<ItemStack> items = candidates.get(i);
            if (items.isEmpty()) {
                continue;
            }
            List<ItemStack> shown;
            ItemStack sample;
            if (i == driver) {
                sample = items.get(page % items.size());
                shown = List.of(withCount(sample, counts[i]));
            } else {
                sample = items.get(0);
                shown = new ArrayList<>(items.size());
                for (ItemStack item : items) {
                    shown.add(withCount(item, counts[i]));
                }
            }
            if (slot.layer() == VatRecipe.Layer.SEASONING) {
                seasoning.add(List.copyOf(shown));
            } else {
                primary.add(List.copyOf(shown));
            }
            ItemStack produced = productOf(slot, sample, counts[i]);
            if (!produced.isEmpty()) {
                results.add(produced);
            }
        }

        VatRecipe.Liquid liquid = recipe.liquid();
        // 缸底的原料液体：水位配方画水，白醋那种配方画酸引水
        VatRecipe.Fluid baseKind = liquid.waterMin() > 0 ? VatRecipe.Fluid.WATER
                : liquid.sourWaterMb() > 0 ? VatRecipe.Fluid.SOUR_WATER : null;
        Fluid base = fluidOf(baseKind);
        long baseMb = baseKind == VatRecipe.Fluid.WATER ? level * ONE_LAYER : liquid.sourWaterMb();
        Fluid product = fluidOf(liquid.product());
        long productMb = product == null ? 0 : recipe.productMbFor(level);
        ItemStack result = merge(results);
        if (product == null) {
            // 缸里出的东西没有对应的流体（格瓦斯就是这样）：那就按"装瓶"展示
            ItemStack bottled = bottledProduct(liquid, level);
            if (!bottled.isEmpty()) {
                result = result.isEmpty() ? bottled : result;
            }
        }

        return new VatJeiRecipe.State(
                base, baseMb, Math.max(baseMb, (long) liquid.waterMax() * ONE_LAYER),
                List.copyOf(seasoning), List.copyOf(primary), sealOf(recipe.seal()),
                result, product, productMb,
                Math.max(productMb, VatRecipes.PRODUCT_CAPACITY_MB),
                recipe.secondsFor(level));
    }

    /** 缸底液体的流体：水位配方画水，酸引水配方画酸引水 */
    @Nullable
    private static Fluid fluidOf(@Nullable VatRecipe.Fluid fluid) {
        if (fluid == null) {
            return null;
        }
        return switch (fluid) {
            case WATER -> Fluids.WATER;
            case SOUR_WATER -> ModFluids.SOUR_WATER.get();
            case PASTE -> ModFluids.SOY_PASTE.get();
            case SOY_SAUCE -> ModFluids.SOY_SAUCE.get();
            case VINEGAR -> ModFluids.VINEGAR.get();
            case WHITE_VINEGAR -> ModFluids.WHITE_VINEGAR.get();
            case FISH_SAUCE -> ModFluids.FISH_SAUCE.get();
            case SHRIMP_PASTE -> ModFluids.SHRIMP_PASTE.get();
            // 格瓦斯还没有对应的流体：不画液面、改成展示几瓶
            case KVASS -> null;
        };
    }

    /** 没有流体产物的配方（格瓦斯）：按"一缸出几瓶"展示瓶子 */
    private static ItemStack bottledProduct(VatRecipe.Liquid liquid, int level) {
        if (liquid.product() == null) {
            return ItemStack.EMPTY;
        }
        VatRecipes.Serving serving = VatRecipes.servingOf(liquid.product());
        if (serving == null) {
            return ItemStack.EMPTY;
        }
        int mb = liquid.productPerLayer()
                ? liquid.productMb() * Math.max(1, level) : liquid.productMb();
        return new ItemStack(serving.item(), Math.max(1, mb / VatRecipes.SERVING_MB));
    }

    // ===== 数量、候选物品、产物 =====

    /**
     * 这一格要几份：把配方的数量规则按当前水位跑一遍。
     *
     * <p>规则之间会互相引用（盐数 = 肉数），所以多跑两遍让它们收敛。
     */
    private static int[] resolveCounts(VatRecipe recipe, List<List<ItemStack>> candidates, int level) {
        int size = recipe.slots().size();
        int[] counts = new int[size];
        for (int i = 0; i < size; i++) {
            counts[i] = candidates.get(i).isEmpty()
                    ? 0 : Math.max(1, candidates.get(i).get(0).getCount());
        }
        Counts view = new Counts(recipe, level, counts);
        for (int pass = 0; pass < 3; pass++) {
            for (int i = 0; i < size; i++) {
                VatRecipe.Bounds bounds = recipe.slots().get(i).bounds().get(view);
                int want = bounds.max() == Integer.MAX_VALUE ? Math.max(1, bounds.min()) : bounds.max();
                counts[i] = Math.max(0, Math.min(bounds.max(), Math.max(bounds.min(), want)));
            }
        }
        return counts;
    }

    /** 按槽位号取数量的"缸内情况"，给数量规则用（格子之间靠匹配器认亲） */
    private record Counts(VatRecipe recipe, int water, int[] counts) implements VatRecipe.Counts {

        @Override
        public int count(Predicate<ItemStack> matcher) {
            for (int i = 0; i < this.recipe.slots().size(); i++) {
                if (this.recipe.slots().get(i).matcher() == matcher) {
                    return this.counts[i];
                }
            }
            return 0;
        }
    }

    /** 扫物品表，找出这一格收得下的东西 */
    private static List<ItemStack> candidatesOf(VatRecipe.Slot slot) {
        List<ItemStack> list = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty() || !slot.matcher().test(stack)) {
                continue;
            }
            list.add(stack);
            if (list.size() >= MAX_CANDIDATES) {
                break;
            }
        }
        return list;
    }

    /** 这一格发酵 / 取货之后变成什么（JEI 右边的产物格） */
    private static ItemStack productOf(VatRecipe.Slot slot, ItemStack sample, int count) {
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        return switch (slot.fate()) {
            case KEEP -> slot.converter() == null
                    ? withCount(sample, count)
                    : withCount(slot.converter().apply(sample.copyWithCount(1)), count);
            case CONVERT -> slot.product() == null
                    ? ItemStack.EMPTY : new ItemStack(slot.product(), count);
            case CONSUME, ABSORB -> ItemStack.EMPTY;
        };
    }

    /** 多个格子都有产物时合成一格：同一种东西就把份数加起来 */
    private static ItemStack merge(List<ItemStack> results) {
        if (results.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack first = results.get(0);
        int count = 0;
        for (ItemStack stack : results) {
            if (!stack.is(first.getItem())) {
                return first;
            }
            count += stack.getCount();
        }
        return first.copyWithCount(count);
    }

    private static ItemStack withCount(ItemStack stack, int count) {
        return stack.copyWithCount(Math.max(1, count));
    }

    // ===== 封口物 =====

    private static List<ItemStack> sealOf(VatRecipe.Seal seal) {
        return switch (seal) {
            // 压缸石：任意石头类方块，这里挑几个代表
            case PRESS -> oneOf(Items.STONE, Items.STONE_BRICKS, Items.COBBLESTONE, Items.DEEPSLATE);
            // 蒙缸用的羊毛地毯
            case CARPET -> oneOf(Items.WHITE_CARPET, Items.RED_CARPET, Items.BROWN_CARPET);
            case CLOTH -> {
                Item rug = BuiltInRegistries.ITEM.get(
                        ResourceLocation.fromNamespaceAndPath("farmersdelight", "canvas_rug"));
                yield rug == Items.AIR ? List.of() : List.of(new ItemStack(rug));
            }
            case NONE -> List.of();
        };
    }

    private static List<ItemStack> oneOf(Item... items) {
        return Arrays.stream(items).map(ItemStack::new).toList();
    }
}
