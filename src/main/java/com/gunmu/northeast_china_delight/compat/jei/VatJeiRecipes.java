package com.gunmu.northeast_china_delight.compat.jei;

import com.gunmu.northeast_china_delight.crafting.VatRecipe;
import com.gunmu.northeast_china_delight.crafting.VatRecipes;
import com.gunmu.northeast_china_delight.fluid.ModFluids;
import com.gunmu.northeast_china_delight.util.DdIds;
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

/**
 * 大缸在 JEI 里展示的全部配方 —— 从数据包配方自动生成，这里不再手写任何一份"投料 / 时长 / 产物"的副本，所以游戏里
 * 的判定和 JEI 页面永远一致。
 */
public final class VatJeiRecipes {

    private VatJeiRecipes() {
    }

    private static final int MAX_CANDIDATES = 12;
    private static final long ONE_LAYER = VatRecipes.WATER_MB_PER_LEVEL;

    public static List<VatJeiRecipe> all(net.minecraft.world.item.crafting.RecipeManager manager) {
        List<VatJeiRecipe> list = new ArrayList<>();
        for (VatRecipe recipe : VatRecipes.all(manager)) {
            list.addAll(expand(recipe));
        }
        return List.copyOf(list);
    }

    public static List<VatJeiRecipe> pagesFor(VatRecipe recipe) {
        return expand(recipe);
    }

    /**
     * 一页对应配方的一个水位档。
     * <p>
     * 泡菜是例外：一条配方覆盖几种蔬菜，为了能在缸里混着放，实际判定仍是同一个配方 id；
     * 但 JEI 用配方 id 当页面的唯一键，同一 id 的多页会被并成一页，所以这里按「放进去的蔬菜 → 拿出来的泡菜」
     * 拆成多页，每页用产物的注册名当后缀区分。拆页只是给人看的，不改变游戏里怎么酵。
     */
    private static List<VatJeiRecipe> expand(VatRecipe recipe) {
        List<VatRecipe.Slot> slots = recipe.slots();
        List<List<ItemStack>> candidates = new ArrayList<>(slots.size());
        for (VatRecipe.Slot slot : slots) {
            candidates.add(candidatesOf(slot));
        }
        int splitting = splittingSlot(slots, candidates);
        if (splitting < 0) {
            return List.of(page(recipe, candidates, null, null));
        }
        List<VatJeiRecipe> pages = new ArrayList<>();
        for (ItemStack sample : candidates.get(splitting)) {
            ItemStack produced = productOf(slots.get(splitting), sample, 1);
            if (produced.isEmpty()) {
                continue;
            }
            ResourceLocation productId = BuiltInRegistries.ITEM.getKey(produced.getItem());
            pages.add(page(recipe, candidates, sample.getItem(), productId.getPath()));
        }
        return pages.isEmpty() ? List.of(page(recipe, candidates, null, null)) : List.copyOf(pages);
    }

    /**
     * 找出「一格放进去的东西会按种类变出不同产物」的那一格。泡菜是这样（白菜→酸菜、黄瓜→腌黄瓜），
     * 而咸腊肉那种几种生肉都变成同一块咸腊肉的不算，仍旧合在一页里滚动展示。
     */
    private static int splittingSlot(List<VatRecipe.Slot> slots, List<List<ItemStack>> candidates) {
        for (int i = 0; i < slots.size(); i++) {
            if (distinctProducts(slots.get(i), candidates.get(i)).size() > 1) {
                return i;
            }
        }
        return -1;
    }

    private static List<Item> distinctProducts(VatRecipe.Slot slot, List<ItemStack> samples) {
        List<Item> products = new ArrayList<>();
        for (ItemStack sample : samples) {
            ItemStack produced = productOf(slot, sample, 1);
            if (!produced.isEmpty() && !products.contains(produced.getItem())) {
                products.add(produced.getItem());
            }
        }
        return products;
    }

    private static VatJeiRecipe page(VatRecipe recipe, List<List<ItemStack>> candidates,
                                     @Nullable Item only, @Nullable String suffix) {
        List<VatJeiRecipe.State> states = new ArrayList<>();
        for (int level : levelsOf(recipe)) {
            states.add(stateOf(recipe, candidates, level, only));
        }
        ResourceLocation id = com.gunmu.northeast_china_delight.util.DdIds.parse(recipe.id());
        if (suffix != null) {
            id = com.gunmu.northeast_china_delight.util.DdIds.of(id.getNamespace(), id.getPath() + "/" + suffix);
        }
        return new VatJeiRecipe(id, List.copyOf(states));
    }

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
                                             int level, @Nullable Item only) {
        int[] counts = resolveCounts(recipe, candidates, level);
        List<List<ItemStack>> seasoning = new ArrayList<>();
        List<List<ItemStack>> primary = new ArrayList<>();
        List<ItemStack> products = new ArrayList<>();

        for (int i = 0; i < recipe.slots().size(); i++) {
            VatRecipe.Slot slot = recipe.slots().get(i);
            List<ItemStack> items = only == null ? candidates.get(i)
                    : candidates.get(i).stream().filter(stack -> stack.is(only)).toList();
            if (items.isEmpty()) {
                continue;
            }
            List<ItemStack> shown = new ArrayList<>(items.size());
            for (ItemStack item : items) {
                shown.add(withCount(item, counts[i]));
            }
            if (slot.layer() == VatRecipe.Layer.SEASONING) {
                seasoning.add(List.copyOf(shown));
            } else {
                primary.add(List.copyOf(shown));
            }
            products.addAll(productsOf(slot, items, counts[i]));
        }

        VatRecipe.Liquid liquid = recipe.liquid();
        Fluid base = liquid.waterMin() > 0 ? Fluids.WATER
                : liquid.sourWaterMb() > 0 ? ModFluids.SOUR_WATER.get() : null;
        long baseMb = liquid.waterMin() > 0 ? level * ONE_LAYER : liquid.sourWaterMb();
        Fluid product = liquid.product();
        long productMb = product == null ? 0 : recipe.productMbFor(level);
        List<ItemStack> results = mergeAll(products);

        return new VatJeiRecipe.State(
                base, baseMb, Math.max(baseMb, (long) liquid.waterMax() * ONE_LAYER),
                List.copyOf(seasoning), List.copyOf(primary), sealOf(recipe),
                results, product, productMb,
                Math.max(productMb, VatRecipes.PRODUCT_CAPACITY_MB),
                recipe.secondsFor(level), recipe.verb(), recipe.timeKey(),
                collectionContainersOf(recipe), noteOf(recipe));
    }

    private static List<ItemStack> collectionContainersOf(VatRecipe recipe) {
        net.minecraft.world.item.Item container = recipe.liquid().servingContainer();
        if (container != null && recipe.liquid().servingItem() != null) {
            return List.of(new ItemStack(container));
        }
        VatRecipes.Serving serving = VatRecipes.servingOf(recipe.productFluid());
        return serving == null ? List.of() : List.of(new ItemStack(serving.container()));
    }

    private static int[] resolveCounts(VatRecipe recipe, List<List<ItemStack>> candidates, int level) {
        int[] counts = recipe.displayedCounts(level);
        for (int i = 0; i < counts.length; i++) {
            if (candidates.get(i).isEmpty()) {
                counts[i] = 0;
            }
        }
        return counts;
    }

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

    /**
     * 一格里的每个候选各自会变出什么产物（放白菜得酸菜、放黄瓜得腌黄瓜）；同一件产物在这一格里只留一份（咸腊肉那种多个候选映射到同一件也只留一份）。
     */
    private static List<ItemStack> productsOf(VatRecipe.Slot slot, List<ItemStack> items, int count) {
        List<ItemStack> products = new ArrayList<>();
        for (ItemStack item : items) {
            ItemStack produced = productOf(slot, item, count);
            if (produced.isEmpty() || indexOfItem(products, produced) >= 0) {
                continue;
            }
            products.add(produced);
        }
        return products;
    }

    /** 多格产物合到一起：同一件物品把份数加起来，不同物品各占一格（JEI 里滚动展示） */
    private static List<ItemStack> mergeAll(List<ItemStack> products) {
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack stack : products) {
            int index = indexOfItem(merged, stack);
            if (index < 0) {
                merged.add(stack.copy());
            } else {
                ItemStack have = merged.get(index);
                merged.set(index, have.copyWithCount(have.getCount() + stack.getCount()));
            }
        }
        return List.copyOf(merged);
    }

    private static int indexOfItem(List<ItemStack> stacks, ItemStack stack) {
        for (int i = 0; i < stacks.size(); i++) {
            if (stacks.get(i).is(stack.getItem())) {
                return i;
            }
        }
        return -1;
    }

    private static ItemStack withCount(ItemStack stack, int count) {
        return stack.copyWithCount(Math.max(1, count));
    }

    private static List<ItemStack> sealOf(VatRecipe recipe) {
        var matcher = recipe.requiredSealItem();
        if (matcher != null) {
            return sealCandidatesOf(matcher);
        }
        return switch (recipe.seal()) {
            case PRESS -> pressStoneCandidates();
            case CARPET -> oneOf(Items.WHITE_CARPET, Items.RED_CARPET, Items.BROWN_CARPET);
            case CLOTH -> {
                Item rug = BuiltInRegistries.ITEM.get(
                        DdIds.of("farmersdelight", "canvas_rug"));
                yield rug == Items.AIR ? List.of() : List.of(new ItemStack(rug));
            }
            case NONE -> List.of();
        };
    }

    private static List<ItemStack> oneOf(Item... items) {
        return Arrays.stream(items).map(ItemStack::new).toList();
    }

    private static List<ItemStack> pressStoneCandidates() {
        List<ItemStack> list = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof net.minecraft.world.item.BlockItem blockItem)) {
                continue;
            }
            var state = blockItem.getBlock().defaultBlockState();
            if (state.is(com.gunmu.northeast_china_delight.block.Vat.PRESS_STONES) && state.isSolid()) {
                list.add(new ItemStack(item));
            }
        }
        return list;
    }
    private static List<ItemStack> sealCandidatesOf(java.util.function.Predicate<ItemStack> matcher) {
        List<ItemStack> list = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (!stack.isEmpty() && matcher.test(stack)) {
                list.add(stack);
                if (list.size() >= MAX_CANDIDATES) {
                    break;
                }
            }
        }
        return list;
    }

    @Nullable
    private static net.minecraft.network.chat.Component noteOf(VatRecipe recipe) {
        if (recipe.requiresSnowyBiome()) {
            return net.minecraft.network.chat.Component.translatable("jei.northeast_china_delight.vat.snowy_biome");
        }
        return null;
    }
}
