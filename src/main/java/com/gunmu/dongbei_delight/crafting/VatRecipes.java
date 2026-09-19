package com.gunmu.dongbei_delight.crafting;

import com.gunmu.dongbei_delight.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 大缸的全部规则集中在这里。
 *
 * 目前仍然写在代码里，但结构已经和将来的数据包配方一一对应，
 * 之后要做成自定义配方类型时把这张表换成 JSON 读取即可。
 */
public final class VatRecipes {

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
        /** 水面团：碎玉米粒泡水 */
        DOUGH
    }

    /** 每一份水能泡几份蔬菜（一份水配两份菜） */
    public static final int VEGETABLES_PER_WATER = 2;
    /** 每一份水配几份盐 */
    public static final int SALT_PER_WATER = 1;
    /** 一缸最多放几块肉 */
    public static final int MAX_MEATS = 5;
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

    /** 一个腌制单位 = 2 秒 = 40 tick；水量几层就是几个单位 */
    public static final int UNIT_TICKS = 40;

    private static Map<Item, Item> pickles;

    /** 泡菜：投入的蔬菜 → 腌制成品 */
    public static Map<Item, Item> pickles() {
        if (pickles == null) {
            Map<Item, Item> map = new HashMap<>();
            map.put(ModItems.NAPA_CABBAGE.get(), ModItems.SOUR_CABBAGE.get());
            map.put(ModItems.CUCUMBER.get(), ModItems.PICKLED_CUCUMBER.get());
            map.put(Items.CARROT, ModItems.PICKLED_CARROT.get());
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

    /** 水面团 */
    public static Item doughInput() {
        return ModItems.CORN_SEEDS.get();
    }

    public static Item doughResult() {
        return ModItems.WATER_DOUGH.get();
    }

    /** 酿醋用的谷物：玉米粒或荞麦 */
    public static boolean isVinegarGrain(ItemStack stack) {
        return stack.is(ModItems.CORN_SEEDS.get()) || stack.is(ModItems.BUCKWHEAT.get());
    }

    /** 任意生鱼：直接用 NeoForge 通用标签 c:foods/raw_fish（鳕鱼、鲑鱼以及模组生鱼片都算） */
    public static final TagKey<Item> RAW_FISH = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "foods/raw_fish"));

    public static boolean isRawFish(ItemStack stack) {
        return stack.is(RAW_FISH);
    }

    /** 大酱 / 酱油酿好后留在缸里的酱渣 */
    public static Item residue() {
        return ModItems.SOY_RESIDUE.get();
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

    /**
     * 腌制/发酵完成后，缸里应该显示成品的模型而不是原料的模型。
     * 没有对应成品的物品（比如盐）返回 null。
     */
    @Nullable
    public static Item resultFor(Item input) {
        Item pickle = pickles().get(input);
        if (pickle != null) {
            return pickle;
        }
        if (input == meatInput()) {
            return meatResult();
        }
        if (input == doughInput()) {
            return doughResult();
        }
        return null;
    }
}
