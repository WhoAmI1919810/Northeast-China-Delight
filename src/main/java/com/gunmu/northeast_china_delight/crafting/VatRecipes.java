package com.gunmu.northeast_china_delight.crafting;

import com.mojang.logging.LogUtils;
import com.gunmu.northeast_china_delight.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
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
 * 大缸的全部规则集中在这里。
 *
 * 目前仍然写在代码里，但结构已经和将来的数据包配方一一对应，
 * 之后要做成自定义配方类型时把这张表换成 JSON 读取即可。
 */
public final class VatRecipes {

    private static final Logger LOGGER = LogUtils.getLogger();

    private VatRecipes() {
    }

    /** 大缸当前在处理什么 */
    public enum Kind {
        /** 空缸 */
        NONE,
        /** 泡菜：水 + 盐 + 蔬菜，压石头 */
        PICKLE,
        /** 腊肉：肉与盐交替层叠，压石头，不加水 */
        MEAT,
        /** 咸鱼：任意生鱼与盐交替层叠，压石头，不加水 */
        SALTED_FISH,
        /** 大酱：大酱块 + 盐 + 水，蒙地毯 */
        PASTE,
        /** 酱油：大酱块 + 小麦 + 盐 + 满水，蒙地毯 */
        SOY_SAUCE,
        /** 醋：大酱 / 酱油剩下的酱渣 + 玉米粒或荞麦，蒙粗布毯二次发酵 */
        VINEGAR,
        /** 白醋：泡菜剩下的酸引水 + 玉米粒或荞麦，蒙粗布毯二次发酵 */
        WHITE_VINEGAR,
        /** 鱼露：6 份任意生鱼 + 3 份盐，不加水，压石头发酵 */
        FISH_SAUCE,
        /** 虾酱：6 只大虾 + 3 份盐，不加水，压石头发酵 */
        SHRIMP_PASTE,
        /** 辣白菜：白菜 + 红辣椒 + 调味品，按泡菜的方式腌 */
        SPICY_PICKLE,
        /** 豆芽：1 层水 + 1~2 份黄豆，蒙粗布毯发芽 */
        BEAN_SPROUTS,
        /** 酸玉米粒：每 1 层水配 2 份玉米粒，蒙粗布毯发酵 */
        SOUR_CORN,
        /** 格瓦斯：6 个面包 + 3 份水（满水），蒙粗布毯发酵成瓶装饮料 */
        KVASS,
        /** 冻梨：一缸梨 + 顶面压雪块 + 寒冷群系，开缸就是冻梨 */
        FROZEN_PEAR
    }

    /** 每一份水能泡几份蔬菜（一份水配两份菜） */
    public static final int VEGETABLES_PER_WATER = 2;
    /** 每一份水配几份盐 */
    public static final int SALT_PER_WATER = 1;
    /** 一缸最多腌几块猪肉：缸里一层放两块、一共三层，正好装满 */
    public static final int MAX_MEATS = 6;
    /** 一缸最多腌几条咸鱼：和腊肉一样，一层两条 × 三层 = 6 条 */
    public static final int MAX_SALTED_FISH = 6;
    /** 一缸大酱能装几碗 */
    public static final int PASTE_SERVINGS = 10;
    /** 一缸酱油能装几瓶 */
    public static final int SOY_SAUCE_SERVINGS = 10;
    /** 一缸醋能装几瓶 */
    public static final int VINEGAR_SERVINGS = 10;
    /** 一缸白醋能装几瓶 */
    public static final int WHITE_VINEGAR_SERVINGS = 10;
    /** 大缸一层水折算成多少 mB（水位 0~3 层，一层就是一桶） */
    public static final int WATER_MB_PER_LEVEL = 1000;
    /**
     * 一份成品折算成多少 mB 液体：一碗大酱 / 一瓶酱油都按 250 mB 算。
     * 流体管道按 mB 抽取，所以缸里其实按 mB 记账，取碗取瓶只是每次扣一份。
     */
    public static final int SERVING_MB = 250;
    /** 酿白醋需要的酸引水（3 瓶 = 750 mB） */
    public static final int SOUR_WATER_MB = 3 * SERVING_MB;
    /** 鱼露：6 条鱼 + 3 份盐，只出 1 瓶（鱼自身析出的液体不多） */
    public static final int FISH_SAUCE_FISH = 6;
    public static final int FISH_SAUCE_SALT = 3;
    public static final int FISH_SAUCE_SERVINGS = 1;
    /** 虾酱：6 只虾 + 3 份盐，出 4 瓶 */
    public static final int SHRIMP_PASTE_SHRIMP = 6;
    public static final int SHRIMP_PASTE_SALT = 3;
    public static final int SHRIMP_PASTE_SERVINGS = 1;
    /** 辣白菜：辣椒酱最多放几份（1 份是必需的） */
    public static final int SPICY_SAUCE_MAX = 1;
    /** 辣白菜的调味品上限（鱼露 + 虾酱，最多就是满水时需要的 2 份） */
    public static final int SPICY_SEASONING_MAX = 2;
    /** 生豆芽：一缸最多放 2 份黄豆 */
    public static final int SPROUT_SOYBEAN_MAX = 2;
    /** 酸玉米粒：每 1 层水配 2 份玉米粒 */
    public static final int SOUR_CORN_PER_WATER = 2;
    /** 辣白菜需要几份调味品：2 份 / 4 份（水位 1、2）要 1 份，6 份（满水）要 2 份 */
    public static int spicySeasoningNeed(int waterLevel) {
        return waterLevel >= 3 ? 2 : 1;
    }
    /** 格瓦斯：6 个面包 + 满水（3 层） */
    public static final int KVASS_BREAD = 6;
    public static final int KVASS_WATER_LEVEL = 3;
    /** 一缸格瓦斯能装几瓶 */
    public static final int KVASS_SERVINGS = 6;
    /** 一缸成品的总容量（mB） */
    public static final int PRODUCT_CAPACITY_MB = PASTE_SERVINGS * SERVING_MB;
    /** 大酱 / 酱油需要的酱块与盐的数量 */
    public static final int PASTE_CHUNKS = 3;
    public static final int PASTE_SALT = 3;
    /** 酿醋需要的酱渣与谷物数量 */
    public static final int RESIDUE_COUNT = 3;
    public static final int VINEGAR_GRAIN_COUNT = 3;

    /** 酱油额外需要的麦子 */
    public static Item wheatInput() {
        return Items.WHEAT;
    }

    /** 冻梨用的梨：果园乐事的梨子（物品 ID fruitsdelight:pear） */
    public static Item pearInput() {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM
                .get(ResourceLocation.fromNamespaceAndPath("fruitsdelight", "pear"));
    }

    // ===== 发酵时长 =====
    // 每个配方自己的时长都写在下面这张表里（单位：秒）。
    // 按水位配比的配方写「每层多少秒」，其余写一个固定值。
    // 改这里，游戏里的计时和 JEI 里显示的时长会一起变。

    // 时长定标（1 个 MC 日 = 1200 秒）：
    //   现实里泡菜/辣白菜 ≈ 1~3 天、腊肉咸鱼 ≈ 3~7 天、大酱酱油 ≈ 一个月、
    //   醋 ≈ 20 天、鱼露虾酱 ≈ 数月 —— 全部按比例压进 MC 的"几天"里，
    //   再用群系温度带做快慢：乳酸菌越冷越慢、越热越快；
    //   咸鱼腊肉在热带反而要捂出霉味 → 多给 50%。
    /** 泡菜：每层水位的发酵秒数（基准 = 1 个 MC 日） */
    public static final int PICKLE_SECONDS_PER_LAYER = 1200;
    /** 辣白菜：每层水位的发酵秒数 */
    public static final int SPICY_PICKLE_SECONDS_PER_LAYER = 1200;
    /** 咸腊肉：基准 2 天 */
    public static final int MEAT_SECONDS = 2400;
    /** 咸鱼：基准 2 天 */
    public static final int SALTED_FISH_SECONDS = 2400;
    /** 大酱：基准 5 天（现实一个月压缩到 5 天） */
    public static final int PASTE_SECONDS = 6000;
    /** 酱油：基准 5 天 */
    public static final int SOY_SAUCE_SECONDS = 6000;
    /** 醋：基准 4 天 */
    public static final int VINEGAR_SECONDS = 4800;
    /** 白醋：基准 3 天 */
    public static final int WHITE_VINEGAR_SECONDS = 3600;
    /** 鱼露：基准 4 天 */
    public static final int FISH_SAUCE_SECONDS = 4800;
    /** 虾酱：基准 4 天 */
    public static final int SHRIMP_PASTE_SECONDS = 4800;
    /** 豆芽：半天 */
    public static final int BEAN_SPROUTS_SECONDS = 600;
    /** 酸玉米粒：基准 1 天/层 */
    public static final int SOUR_CORN_SECONDS = 1200;
    /** 格瓦斯：基准 1 天 */
    public static final int KVASS_SECONDS = 1200;
    /** 冻梨：半天（雪地里冻一天就黑透了） */
    public static final int FROZEN_PEAR_SECONDS = 600;

    // ===== 各温度带的时长倍率 =====
    // 1.0 = 基准时长；越小越快。
    // 乳酸菌发酵（泡菜/辣白菜/酸玉米/大酱/酱油/醋/白醋/鱼露/虾酱/格瓦斯）：
    //   冷带慢一倍、温带基准、暖带快一半、热带再快一倍。
    // 豆芽要温度但怕烫伤：冷带慢一倍、热带略快。
    // 咸鱼腊肉不放倍率表（哪都能做、也不用更慢），
    //   只在热带加 50%（现实里大热天腊肉容易捂坏，咸货更吃工夫）。
    /** 乳酸菌发酵类（泡菜/辣白菜/酸玉米） */
    private static final java.util.function.Consumer<java.util.EnumMap<VatRecipe.BiomeBand, Float>> FERMENT =
            m -> { m.put(VatRecipe.BiomeBand.COLD, 2.0F); m.put(VatRecipe.BiomeBand.WARM, 0.5F); m.put(VatRecipe.BiomeBand.HOT, 0.25F); };
    /** 酱酵类（大酱/酱油/醋/白醋/鱼露/虾酱）：热带不减半那么多，防止过快 */
    private static final java.util.function.Consumer<java.util.EnumMap<VatRecipe.BiomeBand, Float>> BREW =
            m -> { m.put(VatRecipe.BiomeBand.COLD, 1.5F); m.put(VatRecipe.BiomeBand.WARM, 0.5F); m.put(VatRecipe.BiomeBand.HOT, 0.5F); };
    /** 豆芽：冷带慢一倍、暖带基准、热带稍快 */
    private static final java.util.function.Consumer<java.util.EnumMap<VatRecipe.BiomeBand, Float>> SPROUT =
            m -> { m.put(VatRecipe.BiomeBand.COLD, 2.0F); m.put(VatRecipe.BiomeBand.HOT, 0.75F); };
    /** 格瓦斯：冷带慢一半、暖热带稍快 */
    private static final java.util.function.Consumer<java.util.EnumMap<VatRecipe.BiomeBand, Float>> YEAST =
            m -> { m.put(VatRecipe.BiomeBand.COLD, 1.5F); m.put(VatRecipe.BiomeBand.WARM, 0.75F); m.put(VatRecipe.BiomeBand.HOT, 0.75F); };
    /** 咸货在热带更费劲 */
    private static final java.util.function.Consumer<java.util.EnumMap<VatRecipe.BiomeBand, Float>> SALTED =
            m -> { m.put(VatRecipe.BiomeBand.HOT, 1.5F); };
    /** 一秒钟多少游戏刻 */
    public static final int TICKS_PER_SECOND = 20;

    private static Map<Item, Item> pickles;

    /** 泡菜：投入的蔬菜 → 腌制成品 */
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

    public static boolean isPickleIngredient(ItemStack stack) {
        return pickles().containsKey(stack.getItem());
    }

    /** 腌制腊肉用的原料与成品 */
    public static Item meatInput() {
        return Items.PORKCHOP;
    }

    public static Item meatResult() {
        return ModItems.SALTED_PORK.get();
    }

    /** 咸鱼用的原料与成品：任意生鱼，数量与盐 1:1 */
    public static Item saltedFishResult() {
        return ModItems.SALTED_FISH.get();
    }

    /** 酸玉米粒用的玉米粒（旧的水面团配方已经删掉，这里只服务玉米粒这一条线） */
    public static Item cornKernels() {
        return ModItems.CORN_SEEDS.get();
    }

    /** 酿醋用的谷物：玉米粒或荞麦 */
    public static boolean isVinegarGrain(ItemStack stack) {
        return stack.is(ModItems.CORN_SEEDS.get()) || stack.is(ModItems.BUCKWHEAT.get());
    }

    /** 任意生鱼：直接用 NeoForge 通用标签 c:foods/raw_fish（鳕鱼、鲑鱼以及模组生鱼片都算） */
    public static final TagKey<Item> RAW_FISH = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "foods/raw_fish"));

    public static boolean isRawFish(ItemStack stack) {
        // 咸鱼只用整条的鱼：农夫乐事的生鳕鱼片 / 生鲑鱼片是切好的食材，不收
        return stack.is(RAW_FISH)
                && !stack.is(FarmersDelightItems.COD_SLICE)
                && !stack.is(FarmersDelightItems.SALMON_SLICE);
    }

    /** 农夫乐事的两种生鱼片（腌咸鱼不收，免得切好的鱼片拿去腌又变回整条） */
    private static final class FarmersDelightItems {
        private static final Item COD_SLICE = item("farmersdelight", "cod_slice");
        private static final Item SALMON_SLICE = item("farmersdelight", "salmon_slice");

        private static Item item(String namespace, String path) {
            return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
        }
    }

    /** 大酱 / 酱油酿好后留在缸里的酱渣 */
    public static Item residue() {
        return ModItems.SOY_RESIDUE.get();
    }

    // ==========================================================================
    //  配方注册表 —— **改配方 / 加配方只需要动这一张表**
    //  每条配方只说四件事：要什么液体、要哪些材料（数量规则）、要什么封口、完成后变什么。
    //  大缸的交互（VatBrewing）和 JEI 页面都从这张表生成，不会再出现"三处各写一遍"。
    // ==========================================================================

    private static List<VatRecipe> registry;
    private static Map<Kind, VatRecipe> byKind;

    /** 全部大缸配方（按 priority 从高到低排序，匹配时取第一条满足的） */
    public static List<VatRecipe> all() {
        if (registry == null) {
            registry = buildRegistry();
        }
        return registry;
    }

    /**
     * 大缸现在记着的 {@link Kind} 对应的那条配方 —— 取货时要用它来判断"缸里哪些东西能拿出来"。
     * 没有任何配方对应（比如 {@code NONE}）时返回 null。
     */
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
        return stack -> stack.is(ModItems.SALT.get());
    }

    private static Predicate<ItemStack> of(Item item) {
        return stack -> stack.is(item);
    }

    /** 泡菜：投入的蔬菜 → 腌制成品；认不出来的原样返回 */
    private static VatRecipe.Converter pickleConverter() {
        return stack -> {
            Item result = pickles().get(stack.getItem());
            return result == null ? stack : new ItemStack(result);
        };
    }

    /**
     * 辣白菜：大白菜 → 辣白菜，混进去的其它蔬菜还是按普通泡菜算。
     * 这就是"点大白菜给辣白菜、点黄瓜给酸黄瓜"的全部规则。
     */
    private static VatRecipe.Converter spicyPickleConverter() {
        return stack -> {
            if (stack.is(ModItems.NAPA_CABBAGE.get())) {
                return new ItemStack(spicyCabbage());
            }
            Item result = pickles().get(stack.getItem());
            return result == null ? stack : new ItemStack(result);
        };
    }

    private static List<VatRecipe> buildRegistry() {
        List<VatRecipe> list = new ArrayList<>();
        VatRecipe.Seal press = VatRecipe.Seal.PRESS;
        VatRecipe.Seal cloth = VatRecipe.Seal.CLOTH;
        VatRecipe.Seal carpet = VatRecipe.Seal.CARPET;

        // ----- 泡菜：水 1~3 层 + 每层 2 份菜 + 每层 1 份盐，压缸石；腌好后水等量变酸引水 -----
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

        // ----- 辣白菜：泡菜的做法 + 1 份辣椒酱 + 1~2 份鱼露/虾酱，压缸石 -----
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
                        stack -> stack.is(ModItems.FISH_SAUCE.get()) || stack.is(ModItems.SHRIMP_PASTE.get()),
                        VatRecipe.BoundsRule.minByWater(new int[] { 0, 1, 1, 2 }, SPICY_SEASONING_MAX))
                        .refund(Items.GLASS_BOTTLE).seasoning())
                .build());

        // ----- 咸腊肉：肉与盐一比一，最多 6 块，压缸石；取出即咸腊肉 -----
        list.add(VatRecipe.of(Kind.MEAT, "salted_pork")
                .priority(10).seal(press)
                .dry()
                .seconds(MEAT_SECONDS).biomeSeconds(SALTED)
                .slot(VatRecipe.Slot.keep(of(meatInput()),
                        VatRecipe.BoundsRule.between(1, MAX_MEATS), VatRecipe.Converter.to(meatResult())))
                // 第 0 格（肉）几块，盐就要几份 —— 按槽位号引用，JEI 那边也算得出来
                .slot(VatRecipe.Slot.absorb(salt(),
                        VatRecipe.BoundsRule.sameAs(0, 1)).seasoning())
                .build());

        // ----- 咸鱼：任意生鱼与盐一比一，最多 6 条，压缸石 -----
        list.add(VatRecipe.of(Kind.SALTED_FISH, "salted_fish")
                .priority(10).seal(press)
                .dry()
                .seconds(SALTED_FISH_SECONDS).biomeSeconds(SALTED)
                .slot(VatRecipe.Slot.keep(VatRecipes::isRawFish,
                        VatRecipe.BoundsRule.between(1, MAX_SALTED_FISH),
                        VatRecipe.Converter.to(saltedFishResult())))
                // 同上：第 0 格（鱼）几条，盐就要几份
                .slot(VatRecipe.Slot.absorb(salt(),
                        VatRecipe.BoundsRule.sameAs(0, 1)).seasoning())
                .build());

        // ----- 大酱：满水 + 3 酱块 + 3 盐，蒙羊毛地毯；酱块变酱渣、出 10 碗大酱 -----
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

        // ----- 酱油：大酱配方再加 1 份小麦（优先级更高，加了小麦就走这条） -----
        list.add(VatRecipe.of(Kind.SOY_SAUCE, "soy_sauce")
                .priority(30).seal(carpet)
                .water(3, 3, VatRecipe.LiquidAfter.CLEAR)
                .product(VatRecipe.Fluid.SOY_SAUCE, PRODUCT_CAPACITY_MB, false)
                .seconds(SOY_SAUCE_SECONDS).biomeSeconds(BREW)
                .slot(VatRecipe.Slot.convert(of(ModItems.SOY_PASTE_CHUNK.get()),
                        VatRecipe.BoundsRule.exact(PASTE_CHUNKS), residue()))
                .slot(VatRecipe.Slot.consume(salt(), VatRecipe.BoundsRule.exact(PASTE_SALT))
                        .seasoning())
                .slot(VatRecipe.Slot.consume(of(wheatInput()), VatRecipe.BoundsRule.exact(1))
                        .seasoning())
                .build());

        // ----- 醋：3 酱渣 + 3 谷物 + 1~3 层水，蒙粗布毯；材料全消耗，出 10 瓶醋 -----
        list.add(VatRecipe.of(Kind.VINEGAR, "vinegar")
                .priority(20).seal(cloth)
                .water(1, 3, VatRecipe.LiquidAfter.CLEAR)
                .product(VatRecipe.Fluid.VINEGAR, PRODUCT_CAPACITY_MB, false)
                .seconds(VINEGAR_SECONDS).biomeSeconds(BREW)
                .slot(VatRecipe.Slot.consume(of(residue()), VatRecipe.BoundsRule.exact(RESIDUE_COUNT)))
                .slot(VatRecipe.Slot.consume(VatRecipes::isVinegarGrain,
                        VatRecipe.BoundsRule.exact(VINEGAR_GRAIN_COUNT)).seasoning())
                .build());

        // ----- 白醋：3 份酸引水 + 3 谷物，蒙粗布毯；只喝掉要求的那 3 份酸引水 -----
        list.add(VatRecipe.of(Kind.WHITE_VINEGAR, "white_vinegar")
                .priority(20).seal(cloth)
                .dry()
                .sourWater(SOUR_WATER_MB, VatRecipe.LiquidAfter.DRAIN_REQUIRED)
                .product(VatRecipe.Fluid.WHITE_VINEGAR, PRODUCT_CAPACITY_MB, false)
                .seconds(WHITE_VINEGAR_SECONDS).biomeSeconds(BREW)
                .slot(VatRecipe.Slot.consume(VatRecipes::isVinegarGrain,
                        VatRecipe.BoundsRule.exact(VINEGAR_GRAIN_COUNT)).seasoning())
                .build());

        // ----- 鱼露：6 条任意生鱼 + 3 份盐，压缸石；全化掉，只出 1 瓶 -----
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

        // ----- 虾酱：6 只大虾 + 3 份盐，压缸石 -----
        list.add(VatRecipe.of(Kind.SHRIMP_PASTE, "shrimp_paste")
                .priority(40).seal(press)
                .dry()
                .product(VatRecipe.Fluid.SHRIMP_PASTE, SHRIMP_PASTE_SERVINGS * SERVING_MB, false)
                .seconds(SHRIMP_PASTE_SECONDS).biomeSeconds(BREW)
                .slot(VatRecipe.Slot.consume(of(ModItems.SHRIMP.get()),
                        VatRecipe.BoundsRule.exact(SHRIMP_PASTE_SHRIMP)))
                .slot(VatRecipe.Slot.consume(salt(), VatRecipe.BoundsRule.exact(SHRIMP_PASTE_SALT))
                        .seasoning())
                .build());

        // ----- 豆芽：1 层水 + 1~2 份黄豆，蒙粗布毯；一份黄豆出一份豆芽 -----
        list.add(VatRecipe.of(Kind.BEAN_SPROUTS, "bean_sprouts")
                .priority(20).seal(cloth)
                .water(1, 1, VatRecipe.LiquidAfter.CLEAR)
                .seconds(BEAN_SPROUTS_SECONDS).biomeSeconds(SPROUT)
                .slot(VatRecipe.Slot.convert(of(ModItems.SOYBEAN.get()),
                        VatRecipe.BoundsRule.between(1, SPROUT_SOYBEAN_MAX), beanSprouts()))
                .build());

        // ----- 酸玉米粒：每层水配 2 份玉米粒，蒙粗布毯；一份玉米粒出一份酸玉米粒 -----
        list.add(VatRecipe.of(Kind.SOUR_CORN, "sour_corn")
                .priority(20).seal(cloth)
                .water(1, 3, VatRecipe.LiquidAfter.CLEAR)
                .seconds(SOUR_CORN_SECONDS).biomeSeconds(FERMENT)
                .slot(VatRecipe.Slot.convert(of(cornKernels()),
                        VatRecipe.BoundsRule.perLayerExact(SOUR_CORN_PER_WATER), sourCornKernels()))
                .build());

        // ----- 格瓦斯：满水 + 6 个面包，蒙粗布毯；出 6 瓶 -----
        list.add(VatRecipe.of(Kind.KVASS, "kvass")
                .priority(20).seal(cloth)
                .water(KVASS_WATER_LEVEL, KVASS_WATER_LEVEL, VatRecipe.LiquidAfter.CLEAR)
                .product(VatRecipe.Fluid.KVASS, KVASS_SERVINGS * SERVING_MB, false)
                .seconds(KVASS_SECONDS).biomeSeconds(YEAST)
                .slot(VatRecipe.Slot.consume(of(kvassBread()), VatRecipe.BoundsRule.exact(KVASS_BREAD)))
                .build());

        // ----- 冻梨：一缸梨 + 缸顶压一块雪块 + 寒冷群系 -----
        // 借用「压缸石」这个槽位放雪块：seal=PRESS 表示"顶上要压东西"，
        // requiredSealItem 把"压什么"收窄到雪块（雪球和雪片都不行）。
        list.add(VatRecipe.of(Kind.FROZEN_PEAR, "frozen_pear")
                .priority(10).seal(press)
                .requiresSnowyBiome()
                .requiredSealItem(stack -> stack.is(Items.SNOW_BLOCK))
                .dry()
                .seconds(FROZEN_PEAR_SECONDS)
                .slot(VatRecipe.Slot.convert(of(pearInput()),
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

    /** 醋瓶 */
    public static Item vinegarResult() {
        return ModItems.VINEGAR.get();
    }

    /** 酸引水瓶 */
    public static Item sourWaterResult() {
        return ModItems.SOUR_WATER.get();
    }

    /** 白醋瓶 */
    public static Item whiteVinegarResult() {
        return ModItems.WHITE_VINEGAR.get();
    }

    /** 鱼露瓶 */
    public static Item fishSauceResult() {
        return ModItems.FISH_SAUCE.get();
    }

    /** 虾酱瓶 */
    public static Item shrimpPasteResult() {
        return ModItems.SHRIMP_PASTE.get();
    }

    /** 辣白菜 */
    public static Item spicyCabbage() {
        return ModItems.SPICY_CABBAGE.get();
    }

    /** 辣椒酱：辣白菜的必选调料 */
    public static Item chiliSauce() {
        return ModItems.CHILI_SAUCE.get();
    }

    /** 豆芽 */
    public static Item beanSprouts() {
        return ModItems.BEAN_SPROUTS.get();
    }

    /** 酸玉米粒 */
    public static Item sourCornKernels() {
        return ModItems.SOUR_CORN_KERNELS.get();
    }

    /** 格瓦斯用的面包（原版面包） */
    public static Item kvassBread() {
        return Items.BREAD;
    }

    /** 格瓦斯瓶 */
    public static Item kvassResult() {
        return ModItems.KVASS.get();
    }

    /**
     * 腌制/发酵完成后，缸里应该显示成品的模型而不是原料的模型。
     * 没有对应成品的物品（比如盐）返回 null。
     *
     * <p>不写死任何物品映射：直接问这一缸对应的那条配方里"留着等取货"的格子
     * （{@link VatRecipe.Fate#KEEP} + {@link VatRecipe.Slot#converter()}），
     * 所以以后新增"腌好后换个样子"的配方不需要再动渲染这边。
     * 缸里那份东西不属于当前配方时（比如腌好的泡菜缸接着酿白醋，缸里还剩白菜），
     * 再去别的配方里找一遍。
     */
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

    /** 这条配方里，缸里那一份 input 取出来会变成什么（没变化 / 不归它管时返回 null） */
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
     * 用容器从缸里取液体：一个「一份」= {@link #SERVING_MB} mB，
     * 大酱用碗盛、其它都用玻璃瓶装。
     */
    public record Serving(Item container, Item item) {
    }

    /** 某种液体用什么容器装、装出来是什么物品 */
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

    /**
     * 这一缸的成品液体是什么（泡菜缸的成品就是那缸酸引水）。
     * 存的是"种类"，具体剩多少要看 {@code VatBlockEntity#productMb}。
     */
    @Nullable
    public static VatRecipe.Fluid productOf(Kind kind) {
        VatRecipe recipe = recipeOf(kind);
        return recipe == null ? null : recipe.liquid().product();
    }

    /**
     * 大缸里能装瓶的成品液体 → 对应的瓶子物品。
     * 取货、以及"用过的调料瓶右键大缸续上"都用它。
     */
    @Nullable
    public static Item bottleFor(Kind kind) {
        VatRecipe.Fluid fluid = productOf(kind);
        Serving serving = fluid == null ? null : servingOf(fluid);
        return serving == null ? null : serving.item();
    }
}
