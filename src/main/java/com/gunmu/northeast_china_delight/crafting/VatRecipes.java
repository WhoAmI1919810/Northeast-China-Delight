package com.gunmu.northeast_china_delight.crafting;

import com.mojang.logging.LogUtils;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.ModTags;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 大缸的全部规则集中在这里。目前仍然写在代码里，但结构已经和将来的数据包配方一一对应，之后要做成自定义配方类型时把这张表换成 JSON 读取即可。
 */
public final class VatRecipes {

    private static final Logger LOGGER = LogUtils.getLogger();

    private VatRecipes() {
    }

    /**
     * 大缸当前在处理什么
     */
    public enum Kind {
        NONE,
        PICKLE,
        MEAT,
        SALTED_FISH,
        PASTE,
        SOY_SAUCE,
        VINEGAR,
        WHITE_VINEGAR,
        FISH_SAUCE,
        SHRIMP_PASTE,
        SPICY_PICKLE,
        BEAN_SPROUTS,
        SOUR_CORN,
        KVASS,
        FROZEN_PEAR
    }

    public static final int VEGETABLES_PER_WATER = 2;
    public static final int SALT_PER_WATER = 1;
    public static final int MAX_MEATS = 6;
    public static final int SALTED_FISH_FISH = 3;
    public static final int SALTED_FISH_SALT = 3;
    public static final int SALTED_SALT_MAX = 3;
    public static final int PASTE_SERVINGS = 10;
    public static final int SOY_SAUCE_SERVINGS = 10;
    public static final int VINEGAR_SERVINGS = 10;
    public static final int WHITE_VINEGAR_SERVINGS = 10;
    public static final int WATER_MB_PER_LEVEL = 1000;
    public static final int SERVING_MB = 250;
    public static final int SOUR_WATER_MB = 3 * SERVING_MB;
    public static final int FISH_SAUCE_FISH = 3;
    public static final int FISH_SAUCE_SALT = 6;
    public static final int FISH_SAUCE_SERVINGS = 1;
    public static final int SHRIMP_PASTE_SHRIMP = 6;
    public static final int SHRIMP_PASTE_SALT = 3;
    public static final int SHRIMP_PASTE_SERVINGS = 1;
    public static final int SPICY_SAUCE_MAX = 1;
    public static final int SPICY_SEASONING_MAX = 2;
    public static final int SPROUT_SOYBEAN_MAX = 2;
    public static final int SOUR_CORN_PER_WATER = 2;
    public static int spicySeasoningNeed(int waterLevel) {
        return waterLevel >= 3 ? 2 : 1;
    }
    public static final int KVASS_BREAD = 6;
    public static final int KVASS_WATER_LEVEL = 3;
    public static final int KVASS_SERVINGS = 6;
    public static final int PRODUCT_CAPACITY_MB = PASTE_SERVINGS * SERVING_MB;
    public static final int PASTE_CHUNKS = 3;
    public static final int PASTE_SALT = 3;
    public static final int RESIDUE_COUNT = 3;
    public static final int VINEGAR_GRAIN_COUNT = 3;

    public static boolean isWheat(ItemStack stack) {
        return stack.is(ModTags.CROPS_WHEAT);
    }

    public static boolean isPear(ItemStack stack) {
        return stack.is(ModTags.FOODS_FRUITS_PEAR);
    }

    public static final int PICKLE_SECONDS_PER_LAYER = 60;
    public static final int SPICY_PICKLE_SECONDS_PER_LAYER = 60;
    public static final int MEAT_SECONDS = 120;
    public static final int SALTED_FISH_SECONDS = 120;
    public static final int PASTE_SECONDS = 300;
    public static final int SOY_SAUCE_SECONDS = 300;
    public static final int VINEGAR_SECONDS = 240;
    public static final int WHITE_VINEGAR_SECONDS = 180;
    public static final int FISH_SAUCE_SECONDS = 240;
    public static final int SHRIMP_PASTE_SECONDS = 240;
    public static final int BEAN_SPROUTS_SECONDS = 30;
    public static final int SOUR_CORN_SECONDS = 60;
    public static final int KVASS_SECONDS = 60;
    public static final int FROZEN_PEAR_SECONDS = 30;

    private static final java.util.function.Consumer<java.util.EnumMap<VatRecipe.BiomeBand, Float>> FERMENT =
            m -> { m.put(VatRecipe.BiomeBand.COLD, 1.5F); m.put(VatRecipe.BiomeBand.WARM, 0.5F); m.put(VatRecipe.BiomeBand.HOT, 0.25F); };
    /** 酱酵类（大酱/酱油/醋/白醋/鱼露/虾酱）：热带不减半那么多，防止过快 */
    private static final java.util.function.Consumer<java.util.EnumMap<VatRecipe.BiomeBand, Float>> BREW =
            m -> { m.put(VatRecipe.BiomeBand.COLD, 1.25F); m.put(VatRecipe.BiomeBand.WARM, 0.5F); m.put(VatRecipe.BiomeBand.HOT, 0.5F); };
    private static final java.util.function.Consumer<java.util.EnumMap<VatRecipe.BiomeBand, Float>> SPROUT =
            m -> { m.put(VatRecipe.BiomeBand.COLD, 1.5F); m.put(VatRecipe.BiomeBand.HOT, 0.75F); };
    private static final java.util.function.Consumer<java.util.EnumMap<VatRecipe.BiomeBand, Float>> YEAST =
            m -> { m.put(VatRecipe.BiomeBand.COLD, 1.25F); m.put(VatRecipe.BiomeBand.WARM, 0.75F); m.put(VatRecipe.BiomeBand.HOT, 0.75F); };
    private static final java.util.function.Consumer<java.util.EnumMap<VatRecipe.BiomeBand, Float>> SALTED =
            m -> { m.put(VatRecipe.BiomeBand.HOT, 1.5F); };
    public static final int TICKS_PER_SECOND = 20;

    private static Map<Item, Item> pickles;

    public static Map<Item, Item> pickles() {
        if (pickles == null) {
            Map<Item, Item> map = new HashMap<>();
            map.put(ModItems.NAPA_CABBAGE.get(), ModItems.SOUR_CABBAGE.get());
            map.put(ModItems.CUCUMBER.get(), ModItems.PICKLED_CUCUMBER.get());
            map.put(Items.CARROT, ModItems.PICKLED_CARROT.get());
            map.put(ModItems.GREEN_RADISH.get(), ModItems.PICKLED_GREEN_RADISH.get());
            pickles = Map.copyOf(map);
        }
        return pickles;
    }

    /**
     * 泡菜能用的蔬菜：本模组自己的那几样，或者别家塞进对应通用标签的同类菜。
     *
     * <p>{@code c:crops/cabbage}（卷心菜 / 卷心菜叶）特意不收 —— 酸菜是大白菜腌的，
     * 卷心菜腌出来不该叫酸菜；别家的大白菜往 {@code c:crops/napa_cabbage} 里塞就行。
     */
    public static boolean isPickleIngredient(ItemStack stack) {
        return pickles().containsKey(stack.getItem())
                || stack.is(ModTags.CROPS_NAPA_CABBAGE)
                || stack.is(ModTags.CROPS_CUCUMBER)
                || stack.is(ModTags.CROPS_CARROT)
                || stack.is(ModTags.CROPS_RADISH);
    }

    private static ItemStack convertPickle(ItemStack stack) {
        Item result = pickles().get(stack.getItem());
        if (result != null) {
            return new ItemStack(result);
        }
        if (stack.is(ModTags.CROPS_CUCUMBER)) return new ItemStack(ModItems.PICKLED_CUCUMBER.get());
        if (stack.is(ModTags.CROPS_CARROT)) return new ItemStack(ModItems.PICKLED_CARROT.get());
        if (stack.is(ModTags.CROPS_RADISH)) return new ItemStack(ModItems.PICKLED_GREEN_RADISH.get());
        return new ItemStack(ModItems.SOUR_CABBAGE.get());
    }

    public static boolean isMeatInput(ItemStack stack) {
        return stack.is(ModTags.FOODS_RAW_PORK) && !isBacon(stack);
    }

    private static boolean isBacon(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getPath().equals("bacon") || id.getPath().endsWith("_bacon");
    }

    public static Item meatResult() {
        return ModItems.SALTED_PORK.get();
    }

    public static Item saltedFishResult() {
        return ModItems.SALTED_FISH.get();
    }

    public static boolean isCornKernels(ItemStack stack) {
        return stack.is(ModTags.SEEDS_CORN);
    }

    public static boolean isVinegarGrain(ItemStack stack) {
        return stack.is(ModTags.SEEDS_CORN) || stack.is(ModTags.CROPS_BUCKWHEAT);
    }

    /** 生鱼：虾 / 生蚝 / 海参不算，切好的鱼片也不算（免得鱼片腌完又变回整条） */
    public static boolean isRawFish(ItemStack stack) {
        return stack.is(ModTags.FOODS_RAW_FISH)
                && !stack.is(ModTags.FOODS_SHRIMP)
                && !stack.is(ModTags.FOODS_OYSTER)
                && !stack.is(ModTags.FOODS_SEA_CUCUMBER)
                && !stack.is(FarmersDelightItems.COD_SLICE)
                && !stack.is(FarmersDelightItems.SALMON_SLICE);
    }

    /**
     * 农夫乐事的两种生鱼片（腌咸鱼不收，免得切好的鱼片拿去腌又变回整条）
     */
    private static final class FarmersDelightItems {
        private static final Item COD_SLICE = item("farmersdelight", "cod_slice");
        private static final Item SALMON_SLICE = item("farmersdelight", "salmon_slice");

        private static Item item(String namespace, String path) {
            return BuiltInRegistries.ITEM.get(DdIds.of(namespace, path));
        }
    }

    public static Item residue() {
        return ModItems.SOY_RESIDUE.get();
    }

    private static List<VatRecipe> registry;
    private static Map<Kind, VatRecipe> byKind;

    public static List<VatRecipe> all() {
        if (registry == null) {
            registry = buildRegistry();
        }
        return registry;
    }

    @Nullable
    public static VatRecipe recipeOf(Kind kind) {
        if (byKind == null) {
            Map<Kind, VatRecipe> map = new java.util.EnumMap<>(Kind.class);
            for (VatRecipe recipe : all()) {
                map.putIfAbsent(recipe.kind(), recipe);
            }
            byKind = map;
        }
        return byKind.get(kind);
    }

    private static Predicate<ItemStack> salt() {
        return stack -> stack.is(ModTags.FOODS_SALT);
    }

    public static boolean isSalt(ItemStack stack) {
        return stack.is(ModTags.FOODS_SALT);
    }

    private static Predicate<ItemStack> of(Item item) {
        return stack -> stack.is(item);
    }

    private static VatRecipe.Converter pickleConverter() {
        return VatRecipes::convertPickle;
    }

    private static VatRecipe.Converter spicyPickleConverter() {
        return stack -> {
            if (stack.is(ModTags.CROPS_NAPA_CABBAGE)) {
                return new ItemStack(spicyCabbage());
            }
            return convertPickle(stack);
        };
    }

    private static List<VatRecipe> buildRegistry() {
        List<VatRecipe> list = new ArrayList<>();
        VatRecipe.Seal press = VatRecipe.Seal.PRESS;
        VatRecipe.Seal cloth = VatRecipe.Seal.CLOTH;
        VatRecipe.Seal carpet = VatRecipe.Seal.CARPET;

        list.add(VatRecipe.of(Kind.PICKLE, "pickle")
                .priority(10).seal(press)
                .water(1, 3, VatRecipe.LiquidAfter.KEEP)
                .waterBecomesSourWater()
                .seconds(PICKLE_SECONDS_PER_LAYER).perLayerSeconds().biomeSeconds(FERMENT)
                .slot(VatRecipe.Slot.keep(VatRecipes::isPickleIngredient,
                        VatRecipe.BoundsRule.minWithPerLayerMax(1, VEGETABLES_PER_WATER),
                        pickleConverter()))
                .slot(VatRecipe.Slot.absorb(salt(),
                        VatRecipe.BoundsRule.perLayerExact(SALT_PER_WATER)).seasoning())
                .build());

        list.add(VatRecipe.of(Kind.SPICY_PICKLE, "spicy_pickle")
                .priority(20).seal(press)
                .water(1, 3, VatRecipe.LiquidAfter.KEEP)
                .waterBecomesSourWater()
                .seconds(SPICY_PICKLE_SECONDS_PER_LAYER).perLayerSeconds().biomeSeconds(FERMENT)
                .slot(VatRecipe.Slot.keep(VatRecipes::isPickleIngredient,
                        VatRecipe.BoundsRule.minWithPerLayerMax(1, VEGETABLES_PER_WATER),
                        spicyPickleConverter()))
                .slot(VatRecipe.Slot.absorb(salt(),
                        VatRecipe.BoundsRule.perLayerExact(SALT_PER_WATER)).seasoning())
                .slot(VatRecipe.Slot.absorb(of(chiliSauce()), VatRecipe.BoundsRule.exact(SPICY_SAUCE_MAX))
                        .refund(Items.BOWL).seasoning())
                .slot(VatRecipe.Slot.absorb(
                        stack -> stack.is(ModTags.FOODS_FISH_SAUCE) || stack.is(ModTags.FOODS_SHRIMP_PASTE),
                        VatRecipe.BoundsRule.minByWater(new int[] { 0, 1, 1, 2 }, SPICY_SEASONING_MAX))
                        .dosed().seasoning())
                .build());

        list.add(VatRecipe.of(Kind.MEAT, "salted_pork")
                .priority(10).seal(press)
                .dry()
                .seconds(MEAT_SECONDS).biomeSeconds(SALTED)
                .slot(VatRecipe.Slot.keep(VatRecipes::isMeatInput,
                        VatRecipe.BoundsRule.between(1, MAX_MEATS), VatRecipe.Converter.to(meatResult())))
                .slot(VatRecipe.Slot.absorb(salt(),
                        VatRecipe.BoundsRule.halfOf(0, SALTED_SALT_MAX)).seasoning())
                .build());

        list.add(VatRecipe.of(Kind.SALTED_FISH, "salted_fish")
                .priority(10).seal(press)
                .dry()
                .seconds(SALTED_FISH_SECONDS).biomeSeconds(SALTED)
                .slot(VatRecipe.Slot.keep(VatRecipes::isRawFish,
                        VatRecipe.BoundsRule.exact(SALTED_FISH_FISH),
                        VatRecipe.Converter.to(saltedFishResult())))
                .slot(VatRecipe.Slot.absorb(salt(),
                        VatRecipe.BoundsRule.exact(SALTED_FISH_SALT)).seasoning())
                .build());

        list.add(VatRecipe.of(Kind.PASTE, "soy_paste")
                .priority(10).seal(carpet)
                .water(3, 3, VatRecipe.LiquidAfter.CLEAR)
                .product(VatRecipe.Fluid.PASTE, PRODUCT_CAPACITY_MB, false)
                .seconds(PASTE_SECONDS).biomeSeconds(BREW)
                .slot(VatRecipe.Slot.convert(of(ModItems.SOY_PASTE_CHUNK.get()),
                        VatRecipe.BoundsRule.exact(PASTE_CHUNKS), residue()))
                .slot(VatRecipe.Slot.consume(salt(), VatRecipe.BoundsRule.exact(PASTE_SALT))
                        .seasoning())
                .build());

        list.add(VatRecipe.of(Kind.SOY_SAUCE, "soy_sauce")
                .priority(30).seal(carpet)
                .water(3, 3, VatRecipe.LiquidAfter.CLEAR)
                .product(VatRecipe.Fluid.SOY_SAUCE, PRODUCT_CAPACITY_MB, false)
                .seconds(SOY_SAUCE_SECONDS).biomeSeconds(BREW)
                .slot(VatRecipe.Slot.convert(of(ModItems.SOY_PASTE_CHUNK.get()),
                        VatRecipe.BoundsRule.exact(PASTE_CHUNKS), residue()))
                .slot(VatRecipe.Slot.consume(salt(), VatRecipe.BoundsRule.exact(PASTE_SALT))
                        .seasoning())
                .slot(VatRecipe.Slot.consume(VatRecipes::isWheat, VatRecipe.BoundsRule.exact(1))
                        .seasoning())
                .build());

        list.add(VatRecipe.of(Kind.VINEGAR, "vinegar")
                .priority(20).seal(cloth)
                .water(1, 3, VatRecipe.LiquidAfter.CLEAR)
                .product(VatRecipe.Fluid.VINEGAR, PRODUCT_CAPACITY_MB, false)
                .seconds(VINEGAR_SECONDS).biomeSeconds(BREW)
                .slot(VatRecipe.Slot.consume(of(residue()), VatRecipe.BoundsRule.exact(RESIDUE_COUNT)))
                .slot(VatRecipe.Slot.consume(VatRecipes::isVinegarGrain,
                        VatRecipe.BoundsRule.exact(VINEGAR_GRAIN_COUNT)).seasoning())
                .build());

        list.add(VatRecipe.of(Kind.WHITE_VINEGAR, "white_vinegar")
                .priority(20).seal(cloth)
                .dry()
                .sourWater(SOUR_WATER_MB, VatRecipe.LiquidAfter.DRAIN_REQUIRED)
                .product(VatRecipe.Fluid.WHITE_VINEGAR, PRODUCT_CAPACITY_MB, false)
                .seconds(WHITE_VINEGAR_SECONDS).biomeSeconds(BREW)
                .slot(VatRecipe.Slot.consume(VatRecipes::isVinegarGrain,
                        VatRecipe.BoundsRule.exact(VINEGAR_GRAIN_COUNT)).seasoning())
                .build());

        list.add(VatRecipe.of(Kind.FISH_SAUCE, "fish_sauce")
                .priority(40).seal(press)
                .dry()
                .product(VatRecipe.Fluid.FISH_SAUCE, FISH_SAUCE_SERVINGS * SERVING_MB, false)
                .seconds(FISH_SAUCE_SECONDS).biomeSeconds(BREW)
                .slot(VatRecipe.Slot.consume(VatRecipes::isRawFish,
                        VatRecipe.BoundsRule.exact(FISH_SAUCE_FISH)))
                .slot(VatRecipe.Slot.consume(salt(), VatRecipe.BoundsRule.exact(FISH_SAUCE_SALT))
                        .seasoning())
                .build());

        list.add(VatRecipe.of(Kind.SHRIMP_PASTE, "shrimp_paste")
                .priority(40).seal(press)
                .dry()
                .product(VatRecipe.Fluid.SHRIMP_PASTE, SHRIMP_PASTE_SERVINGS * SERVING_MB, false)
                .seconds(SHRIMP_PASTE_SECONDS).biomeSeconds(BREW)
                .slot(VatRecipe.Slot.consume(VatRecipes::isShrimp,
                        VatRecipe.BoundsRule.exact(SHRIMP_PASTE_SHRIMP)))
                .slot(VatRecipe.Slot.consume(salt(), VatRecipe.BoundsRule.exact(SHRIMP_PASTE_SALT))
                        .seasoning())
                .build());

        list.add(VatRecipe.of(Kind.BEAN_SPROUTS, "bean_sprouts")
                .priority(20).seal(cloth)
                .water(1, 1, VatRecipe.LiquidAfter.CLEAR)
                .seconds(BEAN_SPROUTS_SECONDS).biomeSeconds(SPROUT)
                .slot(VatRecipe.Slot.convert(VatRecipes::isSoybean,
                        VatRecipe.BoundsRule.between(1, SPROUT_SOYBEAN_MAX), beanSprouts()))
                .build());

        list.add(VatRecipe.of(Kind.SOUR_CORN, "sour_corn")
                .priority(20).seal(cloth)
                .water(1, 3, VatRecipe.LiquidAfter.CLEAR)
                .seconds(SOUR_CORN_SECONDS).biomeSeconds(FERMENT)
                .slot(VatRecipe.Slot.convert(VatRecipes::isCornKernels,
                        VatRecipe.BoundsRule.perLayerExact(SOUR_CORN_PER_WATER), sourCornKernels()))
                .build());

        list.add(VatRecipe.of(Kind.KVASS, "kvass")
                .priority(20).seal(cloth)
                .water(KVASS_WATER_LEVEL, KVASS_WATER_LEVEL, VatRecipe.LiquidAfter.CLEAR)
                .product(VatRecipe.Fluid.KVASS, KVASS_SERVINGS * SERVING_MB, false)
                .seconds(KVASS_SECONDS).biomeSeconds(YEAST)
                .slot(VatRecipe.Slot.consume(VatRecipes::isBread, VatRecipe.BoundsRule.exact(KVASS_BREAD)))
                .build());

        list.add(VatRecipe.of(Kind.FROZEN_PEAR, "frozen_pear")
                .priority(10).seal(press)
                .requiresSnowyBiome()
                .requiredSealItem(stack -> stack.is(Items.SNOW_BLOCK))
                .dry()
                .seconds(FROZEN_PEAR_SECONDS)
                .slot(VatRecipe.Slot.convert(VatRecipes::isPear,
                        VatRecipe.BoundsRule.between(1, 6), ModItems.FROZEN_PEAR.get()))
                .build());

        list.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
        List<VatRecipe> built = List.copyOf(list);
        selfCheck(built);
        return built;
    }

    /**
     * 注册表建好后跑一遍自检（规则见 {@link VatRecipeValidator}）。
     *
     * <p>以后加配方时踩坑（份数算不出来、JEI 一页画不下、两个格子重叠、超过大缸容量…）
     * 不用等到进游戏试，日志里会直接点名是哪一条配方、哪一格。
     * 自检本身出错也只是记一条日志 —— 它不该把游戏拦下来。
     */
    private static void selfCheck(List<VatRecipe> built) {
        List<String> problems;
        try {
            problems = VatRecipeValidator.check(built);
        } catch (RuntimeException e) {
            LOGGER.warn("[大缸配方] 自检没能跑完", e);
            return;
        }
        if (problems.isEmpty()) {
            LOGGER.info("[大缸配方] 自检通过：{} 条配方", built.size());
            return;
        }
        for (String problem : problems) {
            if (problem.startsWith("WARN")) {
                LOGGER.warn("[大缸配方] {}", problem);
            } else {
                LOGGER.error("[大缸配方] {}", problem);
            }
        }
    }

    public static Item vinegarResult() {
        return ModItems.VINEGAR.get();
    }

    public static Item sourWaterResult() {
        return ModItems.SOUR_WATER.get();
    }

    public static Item whiteVinegarResult() {
        return ModItems.WHITE_VINEGAR.get();
    }

    public static Item fishSauceResult() {
        return ModItems.FISH_SAUCE.get();
    }

    public static Item shrimpPasteResult() {
        return ModItems.SHRIMP_PASTE.get();
    }

    public static Item spicyCabbage() {
        return ModItems.SPICY_CABBAGE.get();
    }

    public static Item chiliSauce() {
        return ModItems.CHILI_SAUCE.get();
    }

    public static Item beanSprouts() {
        return ModItems.BEAN_SPROUTS.get();
    }

    public static Item sourCornKernels() {
        return ModItems.SOUR_CORN_KERNELS.get();
    }

    public static Item kvassBread() {
        return Items.BREAD;
    }

    public static boolean isShrimp(ItemStack stack) {
        return stack.is(ModTags.FOODS_SHRIMP);
    }

    public static boolean isSoybean(ItemStack stack) {
        return stack.is(ModTags.CROPS_SOYBEAN);
    }

    public static boolean isBread(ItemStack stack) {
        return stack.is(ModTags.FOODS_BREAD);
    }

    public static Item kvassResult() {
        return ModItems.KVASS.get();
    }

    @Nullable
    public static Item resultFor(Kind kind, Item input) {
        Item found = keepResult(recipeOf(kind), input);
        if (found != null) {
            return found;
        }
        for (VatRecipe other : all()) {
            found = keepResult(other, input);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Nullable
    private static Item keepResult(@Nullable VatRecipe recipe, Item input) {
        if (recipe == null) {
            return null;
        }
        ItemStack stack = new ItemStack(input);
        for (VatRecipe.Slot slot : recipe.keepSlots()) {
            if (!slot.matcher().test(stack)) {
                continue;
            }
            if (slot.converter() == null) {
                return null;
            }
            Item result = slot.converter().apply(stack.copyWithCount(1)).getItem();
            return result == input ? null : result;
        }
        return null;
    }

    /**
     * 用容器从缸里取液体：一个「一份」= {@link #SERVING_MB} mB，大酱用碗盛、其它都用玻璃瓶装。
     */
    public record Serving(Item container, Item item) {
    }

    @Nullable
    public static Serving servingOf(VatRecipe.Fluid fluid) {
        return switch (fluid) {
            case PASTE -> new Serving(Items.BOWL, ModItems.SOY_PASTE.get());
            case SOY_SAUCE -> new Serving(Items.GLASS_BOTTLE, ModItems.SOY_SAUCE.get());
            case VINEGAR -> new Serving(Items.GLASS_BOTTLE, ModItems.VINEGAR.get());
            case WHITE_VINEGAR -> new Serving(Items.GLASS_BOTTLE, ModItems.WHITE_VINEGAR.get());
            case FISH_SAUCE -> new Serving(Items.GLASS_BOTTLE, ModItems.FISH_SAUCE.get());
            case SHRIMP_PASTE -> new Serving(Items.GLASS_BOTTLE, ModItems.SHRIMP_PASTE.get());
            case KVASS -> new Serving(Items.GLASS_BOTTLE, ModItems.KVASS.get());
            case SOUR_WATER -> new Serving(Items.GLASS_BOTTLE, ModItems.SOUR_WATER.get());
            case WATER -> null;
        };
    }

    @Nullable
    public static VatRecipe.Fluid productOf(Kind kind) {
        VatRecipe recipe = recipeOf(kind);
        return recipe == null ? null : recipe.liquid().product();
    }

    @Nullable
    public static Item bottleFor(Kind kind) {
        VatRecipe.Fluid fluid = productOf(kind);
        Serving serving = fluid == null ? null : servingOf(fluid);
        return serving == null ? null : serving.item();
    }
}
