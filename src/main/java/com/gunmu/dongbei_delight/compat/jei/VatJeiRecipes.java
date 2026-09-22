package com.gunmu.dongbei_delight.compat.jei;

import com.gunmu.dongbei_delight.crafting.VatRecipes;
import com.gunmu.dongbei_delight.fluid.ModFluids;
import com.gunmu.dongbei_delight.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 大缸在 JEI 里展示的全部配方，规则与 {@link VatRecipes} / {@code Vat} 里的判定一一对应。
 *
 * 每条配方都从自己的原料出发，不描述前置步骤（想看酸引水怎么来，点酸引水本身）。
 * 按水位配比的配方（泡菜、辣白菜、酸玉米粒、豆芽）会做成多档，JEI 里自动轮换着演示。
 */
public final class VatJeiRecipes {

    private VatJeiRecipes() {
    }

    /** 一层水 = 1 桶 */
    private static final long ONE_LAYER = VatRecipes.WATER_MB_PER_LEVEL;
    /** 三层水 = 满缸 */
    private static final long FULL = 3 * ONE_LAYER;
    /** 水位最高就是 3 层 */
    private static final int MAX_LAYERS = 3;

    private static List<VatJeiRecipe> recipes;

    public static List<VatJeiRecipe> all() {
        if (recipes == null) {
            recipes = build();
        }
        return recipes;
    }

    private static List<VatJeiRecipe> build() {
        List<VatJeiRecipe> list = new ArrayList<>();

        // ===== 泡菜：每层水 2 份菜 + 1 份盐，压缸石；腌好后水变成等量酸引水 =====
        addPickle(list, ModItems.NAPA_CABBAGE.get(), ModItems.SOUR_CABBAGE.get());
        addPickle(list, ModItems.CUCUMBER.get(), ModItems.PICKLED_CUCUMBER.get());
        addPickle(list, Items.CARROT, ModItems.PICKLED_CARROT.get());

        // ===== 辣白菜：白菜 + 辣椒酱 + 鱼露 / 虾酱，6 份时调味品要两份，压缸石 =====
        List<VatJeiRecipe.State> spicy = new ArrayList<>();
        for (int layers = 1; layers <= MAX_LAYERS; layers++) {
            int cabbage = layers * VatRecipes.VEGETABLES_PER_WATER;
            int seasoningNeed = VatRecipes.spicySeasoningNeed(layers);
            spicy.add(new VatJeiRecipe.State(
                    Fluids.WATER, layers * ONE_LAYER, FULL,
                    List.of(single(ModItems.CHILI_SAUCE.get()),
                            oneOf(seasoningNeed, ModItems.FISH_SAUCE.get(), ModItems.SHRIMP_PASTE.get())),
                    List.of(single(ModItems.NAPA_CABBAGE.get(), cabbage)),
                    sealStone(),
                    stack(ModItems.SPICY_CABBAGE.get(), cabbage),
                    ModFluids.SOUR_WATER.get(), layers * ONE_LAYER, FULL,
                    VatRecipes.processSeconds(VatRecipes.Kind.SPICY_PICKLE, layers)));
        }
        list.add(new VatJeiRecipe(List.copyOf(spicy)));

        // ===== 咸腊肉：一层肉一层盐，最多 5 块 =====
        list.add(new VatJeiRecipe(
                new VatJeiRecipe.State(
                        null, 0, 0,
                        List.of(single(ModItems.SALT.get(), VatRecipes.MAX_MEATS)),
                        List.of(single(Items.PORKCHOP, VatRecipes.MAX_MEATS)),
                        sealStone(),
                        stack(ModItems.SALTED_PORK.get(), VatRecipes.MAX_MEATS),
                        null, 0, 0,
                        VatRecipes.processSeconds(VatRecipes.Kind.MEAT, 0))));

        // ===== 咸鱼：一条鱼一层盐，最多 5 条（任意生鱼都行） =====
        list.add(new VatJeiRecipe(
                new VatJeiRecipe.State(
                        null, 0, 0,
                        List.of(single(ModItems.SALT.get(), VatRecipes.MAX_MEATS)),
                        List.of(rawFish(VatRecipes.MAX_MEATS)),
                        sealStone(),
                        stack(ModItems.SALTED_FISH.get(), VatRecipes.MAX_MEATS),
                        null, 0, 0,
                        VatRecipes.processSeconds(VatRecipes.Kind.SALTED_FISH, 0))));

        // ===== 大酱：满水 + 3 块酱块 + 3 份盐，蒙羊毛地毯 =====
        list.add(new VatJeiRecipe(
                new VatJeiRecipe.State(
                        Fluids.WATER, FULL, FULL,
                        List.of(single(ModItems.SALT.get(), VatRecipes.PASTE_SALT)),
                        List.of(single(ModItems.SOY_PASTE_CHUNK.get(), VatRecipes.PASTE_CHUNKS)),
                        sealCarpet(),
                        // 酱块发酵完变成 3 块酱渣留在缸里，大酱是缸里的液体
                        stack(ModItems.SOY_RESIDUE.get(), VatRecipes.RESIDUE_COUNT),
                        ModFluids.SOY_PASTE.get(), VatRecipes.PRODUCT_CAPACITY_MB, VatRecipes.PRODUCT_CAPACITY_MB,
                        VatRecipes.processSeconds(VatRecipes.Kind.PASTE, 0))));

        // ===== 酱油：大酱的配方再加 1 份小麦，蒙羊毛地毯 =====
        list.add(new VatJeiRecipe(
                new VatJeiRecipe.State(
                        Fluids.WATER, FULL, FULL,
                        List.of(single(ModItems.SALT.get(), VatRecipes.PASTE_SALT),
                                single(VatRecipes.wheatInput())),
                        List.of(single(ModItems.SOY_PASTE_CHUNK.get(), VatRecipes.PASTE_CHUNKS)),
                        sealCarpet(),
                        stack(ModItems.SOY_RESIDUE.get(), VatRecipes.RESIDUE_COUNT),
                        ModFluids.SOY_SAUCE.get(), VatRecipes.PRODUCT_CAPACITY_MB, VatRecipes.PRODUCT_CAPACITY_MB,
                        VatRecipes.processSeconds(VatRecipes.Kind.SOY_SAUCE, 0))));

        // ===== 醋：3 块酱渣 + 3 份谷物，蒙粗布毯 =====
        list.add(new VatJeiRecipe(
                new VatJeiRecipe.State(
                        null, 0, 0,
                        List.of(oneOf(VatRecipes.VINEGAR_GRAIN_COUNT,
                                ModItems.CORN_SEEDS.get(), ModItems.BUCKWHEAT.get())),
                        List.of(single(ModItems.SOY_RESIDUE.get(), VatRecipes.RESIDUE_COUNT)),
                        sealCloth(),
                        // 谷物被消耗掉，酱渣留在缸里
                        stack(ModItems.SOY_RESIDUE.get(), VatRecipes.RESIDUE_COUNT),
                        ModFluids.VINEGAR.get(), VatRecipes.PRODUCT_CAPACITY_MB, VatRecipes.PRODUCT_CAPACITY_MB,
                        VatRecipes.processSeconds(VatRecipes.Kind.VINEGAR, 0))));

        // ===== 白醋：3 瓶酸引水 + 3 份谷物，蒙粗布毯 =====
        list.add(new VatJeiRecipe(
                new VatJeiRecipe.State(
                        ModFluids.SOUR_WATER.get(), VatRecipes.SOUR_WATER_MB, VatRecipes.SOUR_WATER_MB,
                        List.of(oneOf(VatRecipes.VINEGAR_GRAIN_COUNT,
                                ModItems.CORN_SEEDS.get(), ModItems.BUCKWHEAT.get())),
                        List.of(),
                        sealCloth(),
                        // 谷物被消耗掉，缸里只剩白醋液体
                        ItemStack.EMPTY,
                        ModFluids.WHITE_VINEGAR.get(), VatRecipes.PRODUCT_CAPACITY_MB, VatRecipes.PRODUCT_CAPACITY_MB,
                        VatRecipes.processSeconds(VatRecipes.Kind.WHITE_VINEGAR, 0))));

        // ===== 鱼露：6 份任意生鱼 + 3 份盐，压缸石（鱼自身汁水不多，只出 1 瓶） =====
        list.add(new VatJeiRecipe(
                new VatJeiRecipe.State(
                        null, 0, 0,
                        List.of(single(ModItems.SALT.get(), VatRecipes.FISH_SAUCE_SALT)),
                        List.of(rawFish(VatRecipes.FISH_SAUCE_FISH)),
                        sealStone(),
                        // 鱼和盐都被分解掉，只剩鱼露液体
                        ItemStack.EMPTY,
                        ModFluids.FISH_SAUCE.get(), VatRecipes.SERVING_MB, VatRecipes.SERVING_MB,
                        VatRecipes.processSeconds(VatRecipes.Kind.FISH_SAUCE, 0))));

        // ===== 虾酱：6 只大虾 + 3 份盐，压缸石 =====
        list.add(new VatJeiRecipe(
                new VatJeiRecipe.State(
                        null, 0, 0,
                        List.of(single(ModItems.SALT.get(), VatRecipes.SHRIMP_PASTE_SALT)),
                        List.of(single(ModItems.SHRIMP.get(), VatRecipes.SHRIMP_PASTE_SHRIMP)),
                        sealStone(),
                        // 虾和盐都化成了酱，缸里只剩虾酱液体
                        ItemStack.EMPTY,
                        ModFluids.SHRIMP_PASTE.get(), VatRecipes.SERVING_MB, VatRecipes.SERVING_MB,
                        VatRecipes.processSeconds(VatRecipes.Kind.SHRIMP_PASTE, 0))));

        // ===== 豆芽：1 层水 + 1~2 份黄豆，一份黄豆出一份豆芽，蒙粗布毯 =====
        List<VatJeiRecipe.State> sprouts = new ArrayList<>();
        for (int soybeans = 1; soybeans <= VatRecipes.SPROUT_SOYBEAN_MAX; soybeans++) {
            sprouts.add(new VatJeiRecipe.State(
                    Fluids.WATER, ONE_LAYER, ONE_LAYER,
                    List.of(),
                    List.of(single(ModItems.SOYBEAN.get(), soybeans)),
                    sealCloth(),
                    stack(ModItems.BEAN_SPROUTS.get(), soybeans),
                    null, 0, 0,
                    VatRecipes.processSeconds(VatRecipes.Kind.BEAN_SPROUTS, 0)));
        }
        list.add(new VatJeiRecipe(List.copyOf(sprouts)));

        // ===== 酸玉米粒：每层水配 2 份玉米粒，一份玉米粒出一份酸玉米粒，蒙粗布毯 =====
        List<VatJeiRecipe.State> sourCorn = new ArrayList<>();
        for (int layers = 1; layers <= MAX_LAYERS; layers++) {
            int corn = layers * VatRecipes.SOUR_CORN_PER_WATER;
            sourCorn.add(new VatJeiRecipe.State(
                    Fluids.WATER, layers * ONE_LAYER, FULL,
                    List.of(),
                    List.of(single(ModItems.CORN_SEEDS.get(), corn)),
                    sealCloth(),
                    stack(ModItems.SOUR_CORN_KERNELS.get(), corn),
                    null, 0, 0,
                    VatRecipes.processSeconds(VatRecipes.Kind.SOUR_CORN, layers)));
        }
        list.add(new VatJeiRecipe(List.copyOf(sourCorn)));

        return List.copyOf(list);
    }

    /** 泡菜：水位 1 / 2 / 3 层各一档，每层 2 份菜 + 1 份盐，腌好后水等量变成酸引水 */
    private static void addPickle(List<VatJeiRecipe> list, Item vegetable, Item result) {
        List<VatJeiRecipe.State> states = new ArrayList<>();
        for (int layers = 1; layers <= MAX_LAYERS; layers++) {
            int count = layers * VatRecipes.VEGETABLES_PER_WATER;
            states.add(new VatJeiRecipe.State(
                    Fluids.WATER, layers * ONE_LAYER, FULL,
                    List.of(single(ModItems.SALT.get(), layers)),
                    List.of(single(vegetable, count)),
                    sealStone(),
                    stack(result, count),
                    ModFluids.SOUR_WATER.get(), layers * ONE_LAYER, FULL,
                    VatRecipes.processSeconds(VatRecipes.Kind.PICKLE, layers)));
        }
        list.add(new VatJeiRecipe(List.copyOf(states)));
    }

    private static ItemStack stack(Item item) {
        return new ItemStack(item);
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }

    /** 一个槽只放一种物品 */
    private static List<ItemStack> single(Item item) {
        return List.of(new ItemStack(item));
    }

    private static List<ItemStack> single(Item item, int count) {
        return List.of(new ItemStack(item, count));
    }

    /** 一个槽里轮播多个物品 = 这些都可以 */
    private static List<ItemStack> oneOf(Item... items) {
        return Arrays.stream(items).map(ItemStack::new).toList();
    }

    /** 一个槽里轮播多个物品，并且都带同样的数量 */
    private static List<ItemStack> oneOf(int count, Item... items) {
        return Arrays.stream(items).map(item -> new ItemStack(item, count)).toList();
    }

    /** 压缸石：任意石头类方块，这里挑几个代表 */
    private static List<ItemStack> sealStone() {
        return oneOf(Items.STONE, Items.STONE_BRICKS, Items.COBBLESTONE, Items.DEEPSLATE);
    }

    /** 蒙缸用的羊毛地毯 */
    private static List<ItemStack> sealCarpet() {
        return oneOf(Items.WHITE_CARPET, Items.RED_CARPET, Items.BROWN_CARPET);
    }

    /** 农夫乐事的粗布毯 */
    private static List<ItemStack> sealCloth() {
        Item rug = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("farmersdelight", "canvas_rug"));
        return rug == Items.AIR ? List.of() : single(rug);
    }

    /** 任意生鱼（c:foods/raw_fish 标签里的东西都能用），带数量 */
    private static List<ItemStack> rawFish(int count) {
        ItemStack[] items = Ingredient.of(VatRecipes.RAW_FISH).getItems();
        if (items.length == 0) {
            return single(Items.COD, count);
        }
        return Arrays.stream(items).map(stack -> new ItemStack(stack.getItem(), count)).toList();
    }
}
