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
    /** 红薯：自己就是种子（和黄豆、荞麦一样），右键直接种下 */
    public static final DeferredItem<Item> SWEET_POTATO = selfSeedingFood("sweet_potato", () -> ModBlocks.SWEET_POTATO_CROP.get());
    /** 花生、红豆：自己就是种子 */
    public static final DeferredItem<Item> PEANUT = selfSeedingFood("peanut", () -> ModBlocks.PEANUT_CROP.get());
    public static final DeferredItem<Item> RED_BEAN = selfSeedingFood("red_bean", () -> ModBlocks.RED_BEAN_CROP.get());
    public static final DeferredItem<Item> RED_CHILI = food("red_chili");
    /** 大葱：葱烧海参、蘸酱菜一类用的辛香料作物 */
    public static final DeferredItem<Item> GREEN_ONION = food("green_onion");
    /** 大虾：海产食材（获取途径待定） */
    public static final DeferredItem<Item> SHRIMP = food("shrimp");
    /** 辣白菜：大缸用白菜 + 红辣椒 + 调味品腌成 */
    public static final DeferredItem<Item> SPICY_CABBAGE = food("spicy_cabbage");

    // ===== 种子（右键种下对应作物）=====

    public static final DeferredItem<Item> EGGPLANT_SEEDS = seeds("eggplant_seeds", () -> ModBlocks.EGGPLANT_CROP.get());
    public static final DeferredItem<Item> GREEN_PEPPER_SEEDS = seeds("green_pepper_seeds", () -> ModBlocks.GREEN_PEPPER_CROP.get());
    public static final DeferredItem<Item> CORN_SEEDS = seeds("corn_seeds", () -> ModBlocks.CORN_CROP.get());
    public static final DeferredItem<Item> GREEN_BEANS_SEEDS = seeds("green_beans_seeds", () -> ModBlocks.GREEN_BEANS_CROP.get());
    public static final DeferredItem<Item> NAPA_CABBAGE_SEEDS = seeds("napa_cabbage_seeds", () -> ModBlocks.NAPA_CABBAGE_CROP.get());
    public static final DeferredItem<Item> CUCUMBER_SEEDS = seeds("cucumber_seeds", () -> ModBlocks.CUCUMBER_CROP.get());
    public static final DeferredItem<Item> RED_CHILI_SEEDS = seeds("red_chili_seeds", () -> ModBlocks.RED_CHILI_CROP.get());
    public static final DeferredItem<Item> GREEN_ONION_SEEDS = seeds("green_onion_seeds", () -> ModBlocks.GREEN_ONION_CROP.get());

    // ===== 食材（含加工品）=====

    /** 大缸发酵大白菜得到 */
    public static final DeferredItem<Item> SOUR_CABBAGE = food("sour_cabbage");
    /** 大缸发酵大豆得到，需要用碗盛出 */
    public static final DeferredItem<Item> SOY_PASTE = bowlFood("soy_paste");
    /** 大酱块：6 份黄豆在厨锅里煮成，放进大缸酿大酱 */
    public static final DeferredItem<Item> SOY_PASTE_CHUNK = simple("soy_paste_chunk");
    /** 酱渣：大酱 / 酱油酿好后留在缸里的渣，可以继续酿醋 */
    public static final DeferredItem<Item> SOY_RESIDUE = simple("soy_residue");
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
    /** 碎玉米粒在大缸里发酵得到，用来做酸汤子 */
    public static final DeferredItem<Item> WATER_DOUGH = simple("water_dough");
    public static final DeferredItem<Item> BUCKWHEAT_NOODLES = food("buckwheat_noodles");
    /** 豆浆压制而成 */
    public static final DeferredItem<Item> DRIED_TOFU = food("dried_tofu");
    /** 豆腐：由豆浆压制（配方待定） */
    public static final DeferredItem<Item> TOFU = food("tofu");
    /** 食用油：炒菜用（来源与配方待定） */
    public static final DeferredItem<Item> COOKING_OIL = simple("cooking_oil");
    /** 辣椒油：一瓶食用油 + 两个红辣椒在厨锅里炸成，玻璃瓶装 */
    public static final DeferredItem<Item> CHILI_OIL = bottled("chili_oil");
    /** 花生酱：熟花生米磨出来，玻璃瓶装 */
    public static final DeferredItem<Item> PEANUT_BUTTER = bottledFood("peanut_butter");
    /** 熟花生米：花生在厨锅里炒熟 */
    public static final DeferredItem<Item> ROASTED_PEANUTS = food("roasted_peanuts");
    /** 辣椒酱：2 红辣椒 + 盐 + 碗，工作台合成 */
    public static final DeferredItem<Item> CHILI_SAUCE = bowlFood("chili_sauce");
    /** 豆芽：大缸里 1 层水 + 黄豆蒙粗布毯发出来 */
    public static final DeferredItem<Item> BEAN_SPROUTS = food("bean_sprouts");
    /** 酸玉米粒：大缸里玉米粒配水蒙粗布毯发酵而成，磨一磨就是水面团 */
    public static final DeferredItem<Item> SOUR_CORN_KERNELS = food("sour_corn_kernels");
    /** 红薯淀粉：红薯 + 水在动力搅拌器里加热搅出来，用来做粉条 */
    public static final DeferredItem<Item> SWEET_POTATO_STARCH = simple("sweet_potato_starch");
    /** 粉条：红薯淀粉 + 水 + 盐在厨锅里煮成 */
    public static final DeferredItem<Item> VERMICELLI = food("vermicelli");
    /** 拉皮：淀粉浆用动力冲压机压出来 */
    public static final DeferredItem<Item> LA_PI = food("la_pi");
    /** 烤红薯：熔炉 / 烟熏炉 / 篝火 */
    public static final DeferredItem<Item> BAKED_SWEET_POTATO = food("baked_sweet_potato");
    /** 灵魂酸菜：彩蛋物品，不进创造模式物品栏 */
    public static final DeferredItem<Item> SOUL_CABBAGE = food("soul_cabbage");

    // ===== 容器 =====

    /** 大脸盆：大份炖菜盛在里面的容器 */
    public static final DeferredItem<Item> LARGE_BASIN = simple("large_basin");

    // ===== 山珍与海味 =====

    /** 榛子：破坏大型蕨时有 12.5% 概率掉落 */
    public static final DeferredItem<Item> HAZELNUT = food("hazelnut");
    /** 榛蘑、木耳：山珍，获取方式待定 */
    public static final DeferredItem<Item> HAZEL_MUSHROOM = food("hazel_mushroom");
    public static final DeferredItem<Item> WOOD_EAR = food("wood_ear");
    /** 带鱼、生蚝：海味，获取方式待定 */
    public static final DeferredItem<Item> HAIRTAIL = food("hairtail");
    public static final DeferredItem<Item> OYSTER = food("oyster");
    /** 海参：海味，获取方式待定 */
    public static final DeferredItem<Item> SEA_CUCUMBER = food("sea_cucumber");
    /** 鸡架：用刀在砧板上切生鸡肉时得到 */
    public static final DeferredItem<Item> CHICKEN_FRAME = food("chicken_frame");

    // ===== 掉落物 =====

    /** 猪掉落 */
    public static final DeferredItem<Item> PORK_RIBS = food("pork_ribs");
    /** 猪副产物：只有用乐事的刀宰杀时才有概率掉落 */
    public static final DeferredItem<Item> PORK_INTESTINE = food("pork_intestine");
    public static final DeferredItem<Item> PORK_HOCK = food("pork_hock");
    public static final DeferredItem<Item> PIG_BLOOD = food("pig_blood");
    public static final DeferredItem<Item> PIG_LIVER = food("pig_liver");
    /** 灌肠类 */
    public static final DeferredItem<Item> RAW_SAUSAGE = food("raw_sausage");
    public static final DeferredItem<Item> BLOOD_SAUSAGE = food("blood_sausage");
    public static final DeferredItem<Item> RICE_SAUSAGE = food("rice_sausage");
    public static final DeferredItem<Item> RED_SAUSAGE = food("red_sausage");
    public static final DeferredItem<Item> SMOKED_PORK_HOCK = food("smoked_pork_hock");
    public static final DeferredItem<Item> SMOKED_PIG_LIVER = food("smoked_pig_liver");
    /** 酱油：大缸用酱块 + 小麦 + 盐 + 满水酿成 */
    public static final DeferredItem<Item> SOY_SAUCE = bottledFood("soy_sauce");
    /** 酱油烧的扒菜 */
    public static final DeferredItem<Item> BRAISED_PORK_HOCK = bowlFood("braised_pork_hock");
    public static final DeferredItem<Item> BRAISED_PORK_STRIPS = bowlFood("braised_pork_strips");
    /** 醋瓶：用玻璃瓶从大缸里装出 */
    public static final DeferredItem<Item> VINEGAR = bottledFood("vinegar");
    /** 酸引水瓶：泡菜腌好后缸里的那缸水，装瓶后可以酿白醋 */
    public static final DeferredItem<Item> SOUR_WATER = bottled("sour_water");
    /** 白醋瓶：酸引水 + 谷物二次发酵得到 */
    public static final DeferredItem<Item> WHITE_VINEGAR = bottledFood("white_vinegar");
    /** 鱼露瓶：大缸里 6 份生鱼 + 3 份盐发酵出的少量液体 */
    public static final DeferredItem<Item> FISH_SAUCE = bottledFood("fish_sauce");
    /** 虾酱瓶：6 只大虾 + 3 份盐发酵而成 */
    public static final DeferredItem<Item> SHRIMP_PASTE = bottledFood("shrimp_paste");

    // ===== 厨锅料理 =====

    // 碗装：喝掉返还空碗，作为食材使用时炖锅也会把空碗弹出
    public static final DeferredItem<Item> SOY_MILK = bowlFood("soy_milk");
    // 菜类与汤类都用碗盛装：吃完返还空碗
    /** 锅包肉拆成两派：老派用糖 + 白醋，新派用番茄酱 */
    public static final DeferredItem<Item> OLD_STYLE_GUO_BAO_ROU = bowlFood("old_style_guo_bao_rou");
    public static final DeferredItem<Item> NEW_STYLE_GUO_BAO_ROU = bowlFood("new_style_guo_bao_rou");
    public static final DeferredItem<Item> DI_SAN_XIAN = bowlFood("di_san_xian");
    public static final DeferredItem<Item> JIAN_JIAO_GAN_DOU_FU = bowlFood("jian_jiao_gan_dou_fu");
    public static final DeferredItem<Item> SUAN_CAI_DUN_GU_TOU = basinFood("suan_cai_dun_gu_tou");
    /** 饺子不用碗装：吃完不返还容器 */
    public static final DeferredItem<Item> SUAN_CAI_JIAO_ZI = food("suan_cai_jiao_zi");
    public static final DeferredItem<Item> DI_GUO_JI = basinFood("di_guo_ji");
    public static final DeferredItem<Item> SOUR_TANGZI = bowlFood("sour_tangzi");
    public static final DeferredItem<Item> EGG_SOY_PASTE = bowlFood("egg_soy_paste");
    public static final DeferredItem<Item> DI_GUO_PAI_GU = basinFood("di_guo_pai_gu");
    /** 酱大骨、酱牛肉：直接盛出来，不用碗 */
    public static final DeferredItem<Item> JIANG_DA_GU = food("jiang_da_gu");
    public static final DeferredItem<Item> SHA_ZHU_CAI = basinFood("sha_zhu_cai");
    public static final DeferredItem<Item> LA_ROU_DUN_DOU_JIAO = bowlFood("la_rou_dun_dou_jiao");
    public static final DeferredItem<Item> JIANG_NIU_ROU = food("jiang_niu_rou");
    public static final DeferredItem<Item> LIU_ROU_DUAN = bowlFood("liu_rou_duan");
    public static final DeferredItem<Item> HONG_SHAO_PAI_GU = bowlFood("hong_shao_pai_gu");
    /** 拔丝地瓜（物品 id 沿用早期的 ba_si_tu_dou，方便老存档继续用） */
    public static final DeferredItem<Item> BA_SI_TU_DOU = bowlFood("ba_si_tu_dou");
    public static final DeferredItem<Item> SUAN_HUANG_GUA_CHAO_ROU_SI = bowlFood("suan_huang_gua_chao_rou_si");
    public static final DeferredItem<Item> SWEET_POTATO_PORRIDGE = bowlFood("sweet_potato_porridge");
    public static final DeferredItem<Item> ZHU_ROU_DUN_FEN_TIAO = basinFood("zhu_rou_dun_fen_tiao");
    public static final DeferredItem<Item> SUAN_CAI_CHAO_FEN_TIAO = bowlFood("suan_cai_chao_fen_tiao");
    public static final DeferredItem<Item> BUCKWHEAT_COLD_NOODLES = bowlFood("buckwheat_cold_noodles");
    public static final DeferredItem<Item> LIANG_BAN_XIAN_CAI = bowlFood("liang_ban_xian_cai");
    public static final DeferredItem<Item> MING_TAI_YU_SI = bowlFood("ming_tai_yu_si");
    // 工作台凉菜 / 小吃
    public static final DeferredItem<Item> ZHAN_JIANG_CAI = bowlFood("zhan_jiang_cai");
    public static final DeferredItem<Item> DA_FAN_BAO = food("da_fan_bao");
    public static final DeferredItem<Item> JIA_XIAN_HUANG_GUA_PAO_CAI = bowlFood("jia_xian_huang_gua_pao_cai");
    public static final DeferredItem<Item> LIANG_BAN_HUA_CAI = bowlFood("liang_ban_hua_cai");
    // 厨锅料理（东北家常菜续）
    public static final DeferredItem<Item> DA_FENG_SHOU = basinFood("da_feng_shou");
    public static final DeferredItem<Item> BAI_CAI_DOU_FU_DUN_FEN_TIAO = basinFood("bai_cai_dou_fu_dun_fen_tiao");
    public static final DeferredItem<Item> LA_NIU_ROU_TANG_FAN = bowlFood("la_niu_rou_tang_fan");
    public static final DeferredItem<Item> LA_BAI_CAI_CHAO_FAN = bowlFood("la_bai_cai_chao_fan");
    public static final DeferredItem<Item> TU_DOU_BING = food("tu_dou_bing");
    public static final DeferredItem<Item> XIA_REN_ZHU_ROU_XIAN_SHUI_JIAO = food("xia_ren_zhu_rou_xian_shui_jiao");
    public static final DeferredItem<Item> XIA_JIANG_CHAO_JI_DAN = bowlFood("xia_jiang_chao_ji_dan");
    public static final DeferredItem<Item> XIA_JIANG_DUN_DOU_FU = bowlFood("xia_jiang_dun_dou_fu");
    public static final DeferredItem<Item> HAI_XIAN_DOU_FU_TANG = bowlFood("hai_xian_dou_fu_tang");
    public static final DeferredItem<Item> SAN_XIAN_XIAN_SHUI_JIAO = food("san_xian_xian_shui_jiao");
    public static final DeferredItem<Item> LA_BAI_CAI_TANG_FAN = bowlFood("la_bai_cai_tang_fan");
    public static final DeferredItem<Item> YU_MI_LAO = food("yu_mi_lao");
    public static final DeferredItem<Item> CHA_ZI_ZHOU = bowlFood("cha_zi_zhou");
    public static final DeferredItem<Item> NIAN_DOU_BAO = food("nian_dou_bao");
    public static final DeferredItem<Item> XUN_JIANG_PIN_PAN = bowlFood("xun_jiang_pin_pan");
    /** 果园乐事联动：橘子汁做的新派橙汁锅包肉 */
    public static final DeferredItem<Item> ORANGE_GUO_BAO_ROU = bowlFood("orange_guo_bao_rou");
    /** 酱拌拉皮：碗 + 拉皮 + 花生酱 + 黄瓜 + 番茄 */
    public static final DeferredItem<Item> JIANG_BAN_LA_PI = bowlFood("jiang_ban_la_pi");
    /** 葱烧海参：大葱 + 海参 */
    public static final DeferredItem<Item> CONG_SHAO_HAI_SHEN = bowlFood("cong_shao_hai_shen");
    /** 海参豆腐汤：海参 + 豆腐 */
    public static final DeferredItem<Item> HAI_SHEN_DOU_FU_TANG = bowlFood("hai_shen_dou_fu_tang");

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

    /**
     * 「食材与调料」物品栏的展示顺序。
     *
     * 按类别分组排列，方便在创造栏里找：
     * 种子 → 蔬菜 → 豆类与杂粮 → 山珍 → 肉类与水产 → 肉制品 → 腌制品
     * → 调料 → 酿造原料 → 主食与半成品 → 容器 → 方块。
     * （灵魂酸菜是彩蛋，不在此列出）
     */
    public static final List<Supplier<? extends Item>> INGREDIENT_TAB_ITEMS = List.of(
            // 种子
            EGGPLANT_SEEDS, GREEN_PEPPER_SEEDS, CORN_SEEDS, GREEN_BEANS_SEEDS,
            NAPA_CABBAGE_SEEDS, CUCUMBER_SEEDS, RED_CHILI_SEEDS, GREEN_ONION_SEEDS,
            // 蔬菜
            NAPA_CABBAGE, CUCUMBER, EGGPLANT, GREEN_PEPPER, GREEN_ONION, RED_CHILI,
            GREEN_BEANS, CORN, SWEET_POTATO,
            // 豆类与杂粮
            SOYBEAN, PEANUT, RED_BEAN, BUCKWHEAT,
            // 山珍
            HAZELNUT, HAZEL_MUSHROOM, WOOD_EAR,
            // 肉类与水产
            PORK_RIBS, PORK_INTESTINE, PORK_HOCK, PIG_BLOOD, PIG_LIVER, CHICKEN_FRAME,
            SHRIMP, HAIRTAIL, OYSTER, SEA_CUCUMBER,
            // 肉制品（灌肠与熏货）
            RAW_SAUSAGE, BLOOD_SAUSAGE, RICE_SAUSAGE, RED_SAUSAGE,
            SMOKED_PORK_HOCK, SMOKED_PIG_LIVER,
            // 腌制品
            SOUR_CABBAGE, SPICY_CABBAGE, PICKLED_CUCUMBER, PICKLED_CARROT, SALTED_PORK,
            // 调料
            SALT, SOY_PASTE, CHILI_SAUCE, SOY_SAUCE, VINEGAR, SOUR_WATER, WHITE_VINEGAR,
            FISH_SAUCE, SHRIMP_PASTE, CHILI_OIL, PEANUT_BUTTER, COOKING_OIL,
            // 酿造原料
            SOY_PASTE_CHUNK, SOY_RESIDUE,
            // 主食与半成品
            WATER_DOUGH, BUCKWHEAT_NOODLES, DRIED_TOFU, TOFU, SWEET_POTATO_STARCH,
            VERMICELLI, LA_PI, BEAN_SPROUTS, SOUR_CORN_KERNELS, ROASTED_PEANUTS,
            CORN_STALK,
            // 容器
            LARGE_BASIN,
            // 方块
            UNFIRED_VAT_BLANK, VAT
    );

    /**
     * 「菜肴」物品栏的展示顺序。
     *
     * 按「吃完给什么效果 / 要不要餐具」分组排列，方便在创造栏里找：
     * 饮品 → 油腻 → 清爽 → 荤素搭配 → 不需要带餐具（非碗装）→ 饺子。
     * 一道菜只出现在一组里：既是油腻又是非碗装的（比如土豆饼）就归到「不需要带餐具」那组。
     */
    public static final List<Supplier<? extends Item>> DISH_TAB_ITEMS = List.of(
            // 饮品
            SOY_MILK,
            // 油腻（吃多了腻，给油腻效果）
            OLD_STYLE_GUO_BAO_ROU, NEW_STYLE_GUO_BAO_ROU, ORANGE_GUO_BAO_ROU, LIU_ROU_DUAN,
            DI_SAN_XIAN, HONG_SHAO_PAI_GU, DI_GUO_JI, DI_GUO_PAI_GU, SHA_ZHU_CAI,
            JIANG_DA_GU, JIANG_NIU_ROU, BRAISED_PORK_HOCK, BRAISED_PORK_STRIPS,
            BA_SI_TU_DOU, XUN_JIANG_PIN_PAN,
            // 清爽（给爽口效果）
            SUAN_CAI_CHAO_FEN_TIAO, JIAN_JIAO_GAN_DOU_FU, BAI_CAI_DOU_FU_DUN_FEN_TIAO,
            LA_BAI_CAI_TANG_FAN, SUAN_HUANG_GUA_CHAO_ROU_SI, LIANG_BAN_XIAN_CAI, LIANG_BAN_HUA_CAI,
            JIA_XIAN_HUANG_GUA_PAO_CAI, ZHAN_JIANG_CAI, MING_TAI_YU_SI, BUCKWHEAT_COLD_NOODLES,
            SOUR_TANGZI, CHA_ZI_ZHOU, SWEET_POTATO_PORRIDGE,
            // 荤素搭配（给滋养效果）
            EGG_SOY_PASTE, SUAN_CAI_DUN_GU_TOU, ZHU_ROU_DUN_FEN_TIAO, LA_NIU_ROU_TANG_FAN,
            LA_BAI_CAI_CHAO_FAN, XIA_JIANG_CHAO_JI_DAN, XIA_JIANG_DUN_DOU_FU, HAI_XIAN_DOU_FU_TANG,
            JIANG_BAN_LA_PI, CONG_SHAO_HAI_SHEN, HAI_SHEN_DOU_FU_TANG, LA_ROU_DUN_DOU_JIAO, DA_FENG_SHOU,
            // 不需要带餐具（直接拿在手里吃）
            BAKED_SWEET_POTATO, DA_FAN_BAO, TU_DOU_BING, YU_MI_LAO, NIAN_DOU_BAO,
            // 饺子（一碟一碟的）
            SUAN_CAI_JIAO_ZI, XIA_REN_ZHU_ROU_XIAN_SHUI_JIAO, SAN_XIAN_XIAN_SHUI_JIAO
    );

    /** 登记一个只有默认属性的普通物品 */
    private static DeferredItem<Item> simple(String id) {
        return ITEMS.registerItem(id, Item::new, new Item.Properties());
    }

    /** 玻璃瓶装的东西：当材料用时返还空瓶（厨锅 / 工作台通用） */
    private static DeferredItem<Item> bottled(String id) {
        return ITEMS.registerItem(id, Item::new, new Item.Properties().craftRemainder(Items.GLASS_BOTTLE));
    }

    /** 玻璃瓶装的饮品：既是食物也返还空瓶 */
    private static DeferredItem<Item> bottledFood(String id) {
        return ITEMS.registerItem(id, Item::new,
                new Item.Properties().craftRemainder(Items.GLASS_BOTTLE).food(steakFood().build()));
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

    /**
     * 用大脸盆盛装的料理（分量大的炖菜）：吃完返还大脸盆，作为食材放进炖锅时也会把空盆弹出。
     *
     * 注意这里必须用 supplier 把属性拖到「物品注册事件」里现做：
     * 静态初始化阶段物品注册表还是冻结状态，既建不了 Item 实例，也取不到 LARGE_BASIN 的实例。
     */
    private static DeferredItem<Item> basinFood(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties()
                .craftRemainder(LARGE_BASIN.get())
                .food(steakFood().usingConvertsTo(LARGE_BASIN.get()).build())));
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
