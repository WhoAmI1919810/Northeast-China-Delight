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
 * 大缸在 JEI 里展示的全部配方 —— 从 {@link VatRecipes#REGISTRY} 自动生成，这里不再手写任何一份"投料 / 时长 / 产物"的副本，所以游戏里
 * 的判定和 JEI 页面永远一致。
 */
public final class VatJeiRecipes {

    private VatJeiRecipes() {
    }

    private static final int MAX_CANDIDATES = 12;
    private static final long ONE_LAYER = VatRecipes.WATER_MB_PER_LEVEL;

    private static List<VatJeiRecipe> recipes;

    public static List<VatJeiRecipe> all() {
        if (recipes == null) {
            recipes = build();
        }
        return recipes;
    }

    public static List<VatJeiRecipe> pagesFor(VatRecipe recipe) {
        return expand(recipe);
    }

    private static List<VatJeiRecipe> build() {
        List<VatJeiRecipe> list = new ArrayList<>();
        for (VatRecipe recipe : VatRecipes.all()) {
            list.addAll(expand(recipe));
        }
        return List.copyOf(list);
    }

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
        VatRecipe.Fluid baseKind = liquid.waterMin() > 0 ? VatRecipe.Fluid.WATER
                : liquid.sourWaterMb() > 0 ? VatRecipe.Fluid.SOUR_WATER : null;
        Fluid base = fluidOf(baseKind);
        long baseMb = baseKind == VatRecipe.Fluid.WATER ? level * ONE_LAYER : liquid.sourWaterMb();
        Fluid product = fluidOf(liquid.product());
        long productMb = product == null ? 0 : recipe.productMbFor(level);
        ItemStack result = merge(results);
        if (product == null) {
            ItemStack bottled = bottledProduct(liquid, level);
            if (!bottled.isEmpty()) {
                result = result.isEmpty() ? bottled : result;
            }
        }

        return new VatJeiRecipe.State(
                base, baseMb, Math.max(baseMb, (long) liquid.waterMax() * ONE_LAYER),
                List.copyOf(seasoning), List.copyOf(primary), sealOf(recipe),
                result, product, productMb,
                Math.max(productMb, VatRecipes.PRODUCT_CAPACITY_MB),
                recipe.secondsFor(level), noteOf(recipe));
    }

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
            case KVASS -> null;
        };
    }

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
        boolean varies = false;
        for (VatRecipe.BiomeBand band : VatRecipe.BiomeBand.values()) {
            if (recipe.biomeMultiplier(band) != null) {
                varies = true;
                break;
            }
        }
        return varies
                ? net.minecraft.network.chat.Component.translatable("jei.northeast_china_delight.vat.biome_time")
                : null;
    }
}
