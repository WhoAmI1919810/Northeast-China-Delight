package com.gunmu.dongbei_delight.item;

import com.gunmu.dongbei_delight.DongbeiDelight;
import com.gunmu.dongbei_delight.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

/**
 * 本模组的全部物品。
 *
 * 食物暂时统一使用牛排（熟牛肉）的数值：8 点饱食度、0.8 饱和度系数，
 * 待模组成型后再逐个调整。
 */
public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DongbeiDelight.MODID);

    // ===== 农作物 =====

    // 大豆与荞麦本身既是作物也是种子：拿着它右键就能种下，收获也用同一个物品
    public static final DeferredItem<Item> SOYBEAN = selfSeedingFood("soybean", () -> ModBlocks.SOYBEAN_CROP.get());
    public static final DeferredItem<Item> EGGPLANT = food("eggplant");
    public static final DeferredItem<Item> GREEN_PEPPER = food("green_pepper");
    public static final DeferredItem<Item> CORN = food("corn");
    public static final DeferredItem<Item> GREEN_BEANS = food("green_beans");
    public static final DeferredItem<Item> BUCKWHEAT = selfSeedingFood("buckwheat", () -> ModBlocks.BUCKWHEAT_CROP.get());
    public static final DeferredItem<Item> NAPA_CABBAGE = food("napa_cabbage");
    public static final DeferredItem<Item> CUCUMBER = food("cucumber");

    // ===== 种子（右键种下对应作物）=====

    public static final DeferredItem<Item> EGGPLANT_SEEDS = seeds("eggplant_seeds", () -> ModBlocks.EGGPLANT_CROP.get());
    public static final DeferredItem<Item> GREEN_PEPPER_SEEDS = seeds("green_pepper_seeds", () -> ModBlocks.GREEN_PEPPER_CROP.get());
    public static final DeferredItem<Item> CORN_SEEDS = seeds("corn_seeds", () -> ModBlocks.CORN_CROP.get());
    public static final DeferredItem<Item> GREEN_BEANS_SEEDS = seeds("green_beans_seeds", () -> ModBlocks.GREEN_BEANS_CROP.get());
    public static final DeferredItem<Item> NAPA_CABBAGE_SEEDS = seeds("napa_cabbage_seeds", () -> ModBlocks.NAPA_CABBAGE_CROP.get());
    public static final DeferredItem<Item> CUCUMBER_SEEDS = seeds("cucumber_seeds", () -> ModBlocks.CUCUMBER_CROP.get());

    // ===== 食材（含加工品）=====

    /** 大缸发酵大白菜得到 */
    public static final DeferredItem<Item> SOUR_CABBAGE = food("sour_cabbage");
    /** 大缸发酵大豆得到，需要用碗盛出 */
    public static final DeferredItem<Item> SOY_PASTE = bowlFood("soy_paste");
    /** 玉米茎秆：破坏玉米植株得到，可以当燃料 */
    public static final DeferredItem<Item> CORN_STALK = ITEMS.registerItem(
            "corn_stalk",
            properties -> new FuelItem(properties, 200),
            new Item.Properties()
    );
    /** 盐：目前只用于大缸腌制，来源待定 */
    public static final DeferredItem<Item> SALT = simple("salt");
    /** 盐渍猪肉 */
    public static final DeferredItem<Item> SALTED_PORK = food("salted_pork");
    /** 大缸发酵黄瓜得到 */
    public static final DeferredItem<Item> PICKLED_CUCUMBER = food("pickled_cucumber");
    /** 大缸发酵胡萝卜得到 */
    public static final DeferredItem<Item> PICKLED_CARROT = food("pickled_carrot");
    /** 玉米压碎而成，用来在大缸里发酵 */
    public static final DeferredItem<Item> CRUSHED_CORN = food("crushed_corn");
    /** 碎玉米粒在大缸里发酵得到，用来做酸汤子 */
    public static final DeferredItem<Item> WATER_DOUGH = simple("water_dough");
    public static final DeferredItem<Item> BUCKWHEAT_NOODLES = food("buckwheat_noodles");
    /** 豆浆压制而成 */
    public static final DeferredItem<Item> DRIED_TOFU = food("dried_tofu");
    /** 灵魂酸菜：彩蛋物品，不进创造模式物品栏 */
    public static final DeferredItem<Item> SOUL_CABBAGE = food("soul_cabbage");

    // ===== 掉落物 =====

    /** 猪掉落 */
    public static final DeferredItem<Item> PORK_RIBS = food("pork_ribs");

    // ===== 厨锅料理 =====

    // 碗装：喝掉返还空碗，作为食材使用时炖锅也会把空碗弹出
    public static final DeferredItem<Item> SOY_MILK = bowlFood("soy_milk");
    // 菜类与汤类都用碗盛装：吃完返还空碗
    public static final DeferredItem<Item> GUO_BAO_ROU = bowlFood("guo_bao_rou");
    public static final DeferredItem<Item> DI_SAN_XIAN = bowlFood("di_san_xian");
    public static final DeferredItem<Item> JIAN_JIAO_GAN_DOU_FU = bowlFood("jian_jiao_gan_dou_fu");
    public static final DeferredItem<Item> SUAN_CAI_DUN_GU_TOU = bowlFood("suan_cai_dun_gu_tou");
    public static final DeferredItem<Item> SUAN_CAI_JIAO_ZI = bowlFood("suan_cai_jiao_zi");
    public static final DeferredItem<Item> DI_GUO_JI = bowlFood("di_guo_ji");
    public static final DeferredItem<Item> SOUR_TANGZI = bowlFood("sour_tangzi");
    public static final DeferredItem<Item> EGG_SOY_PASTE = bowlFood("egg_soy_paste");

    // ===== 方块物品 =====

    public static final DeferredItem<Item> UNFIRED_VAT_BLANK = ITEMS.registerItem(
            "unfired_vat_blank",
            Item::new,
            new Item.Properties().stacksTo(1)
    );

    public static final DeferredItem<Item> VAT = ITEMS.registerItem(
            "vat",
            (properties) -> new BlockItem(ModBlocks.VAT.get(), properties),
            new Item.Properties().stacksTo(1)
    );

    /** 创造模式物品栏中的展示顺序 */
    public static final List<Supplier<? extends Item>> CREATIVE_TAB_ITEMS = List.of(
            // 农作物
            SOYBEAN, EGGPLANT, GREEN_PEPPER, CORN, GREEN_BEANS, BUCKWHEAT, NAPA_CABBAGE, CUCUMBER,
            // 种子
            EGGPLANT_SEEDS, GREEN_PEPPER_SEEDS, CORN_SEEDS, GREEN_BEANS_SEEDS,
            NAPA_CABBAGE_SEEDS, CUCUMBER_SEEDS,
            // 食材与加工品
            // 灵魂酸菜是彩蛋，不在此列出
            SOUR_CABBAGE, SOY_PASTE, PICKLED_CUCUMBER, PICKLED_CARROT,
            CRUSHED_CORN, CORN_STALK, WATER_DOUGH, BUCKWHEAT_NOODLES, DRIED_TOFU,
            PORK_RIBS, SALT, SALTED_PORK,
            // 料理
            SOY_MILK, GUO_BAO_ROU, DI_SAN_XIAN, JIAN_JIAO_GAN_DOU_FU, SUAN_CAI_DUN_GU_TOU,
            SUAN_CAI_JIAO_ZI, DI_GUO_JI, SOUR_TANGZI, EGG_SOY_PASTE,
            // 方块
            UNFIRED_VAT_BLANK, VAT
    );

    /** 登记一个只有默认属性的普通物品 */
    private static DeferredItem<Item> simple(String id) {
        return ITEMS.registerItem(id, Item::new, new Item.Properties());
    }

    /**
     * 「自身即种子」的食物：右键种下对应的作物，收获时也得到这个物品。
     * 用 ItemNameBlockItem 保证名字走物品的翻译键。
     */
    private static DeferredItem<Item> selfSeedingFood(String id, Supplier<? extends Block> crop) {
        return ITEMS.registerItem(id,
                properties -> new ItemNameBlockItem(crop.get(), properties),
                new Item.Properties().food(steakFood().build()));
    }

    /**
     * 种子物品：右键种下对应的作物方块。
     * 用 ItemNameBlockItem 而不是普通 BlockItem —— 否则物品名会用方块的翻译键，
     * 显示成「大豆」而不是「大豆种子」。
     */
    private static DeferredItem<Item> seeds(String id, Supplier<? extends Block> crop) {
        return ITEMS.registerItem(id, properties -> new ItemNameBlockItem(crop.get(), properties), new Item.Properties());
    }

    /** 登记一个食物，数值暂用牛排（熟牛肉）：8 点饱食度、0.8 饱和度系数 */
    private static DeferredItem<Item> food(String id) {
        return ITEMS.registerItem(id, Item::new, new Item.Properties().food(steakFood().build()));
    }

    /**
     * 用碗盛装的料理。
     * usingConvertsTo：吃完返还空碗；
     * craftRemainder：作为食材放进炖锅时，农夫乐事会把空碗弹出（比如豆浆做成干豆腐）。
     */
    private static DeferredItem<Item> bowlFood(String id) {
        return ITEMS.registerItem(id, Item::new, new Item.Properties()
                .craftRemainder(Items.BOWL)
                .food(steakFood().usingConvertsTo(Items.BOWL).build()));
    }

    private static FoodProperties.Builder steakFood() {
        return new FoodProperties.Builder()
                .nutrition(8)
                .saturationModifier(0.8F);
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
