package com.gunmu.northeast_china_delight.item;

import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.ItemLike;
//? if <1.20.2 {
import net.minecraftforge.eventbus.api.IEventBus;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
*///?}
//? if <1.20.2 {
import net.minecraftforge.registries.DeferredRegister;
//?} else {
/*import net.neoforged.neoforge.registries.DeferredRegister;
*///?}

import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 本模组的全部物品。
 *
 * 食物暂时统一使用牛排（熟牛肉）的数值：8 点饱食度、0.8 饱和度系数，
 * 待模组成型后再逐个调整。
 */
public class ModItems {

    //? if <1.20.2 {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.ITEMS, NortheastChinaDelight.MODID);
    //?} else {
    /*public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NortheastChinaDelight.MODID);
    *///?}

    /** 油炸时一次烧完一整瓶的油：植物油、动物油（厨锅按这个标签判断） */
    public static final TagKey<Item> DEEP_FRY_OILS = TagKey.create(Registries.ITEM,
            DdIds.of(NortheastChinaDelight.MODID, "deep_fry_oils"));
    /** 油炸菜：用油时一次用光一整瓶（煎、炒、炖还是按 50 mB 扣） */
    public static final TagKey<Item> DEEP_FRIED_DISHES = TagKey.create(Registries.ITEM,
            DdIds.of(NortheastChinaDelight.MODID, "deep_fried_dishes"));

    // ===== 农作物 =====

    // 大豆与荞麦本身既是作物也是种子：拿着它右键就能种下，收获也用同一个物品
    public static final Supplier<Item> SOYBEAN = selfSeeding("soybean", () -> ModBlocks.SOYBEAN_CROP.get());
    public static final Supplier<Item> EGGPLANT = food("eggplant", 2, 0.3F);
    public static final Supplier<Item> GREEN_PEPPER = food("green_pepper", 2, 0.3F);
    public static final Supplier<Item> CORN = food("corn", 3, 0.4F);
    public static final Supplier<Item> GREEN_BEANS = food("green_beans", 1, 0.3F);
    public static final Supplier<Item> BUCKWHEAT = selfSeeding("buckwheat", () -> ModBlocks.BUCKWHEAT_CROP.get());
    public static final Supplier<Item> NAPA_CABBAGE = food("napa_cabbage", 2, 0.4F);
    public static final Supplier<Item> CUCUMBER = food("cucumber", 2, 0.4F);
    /** 红薯：自己就是种子（和黄豆、荞麦一样），右键直接种下 */
    public static final Supplier<Item> SWEET_POTATO = selfSeedingFood("sweet_potato", () -> ModBlocks.SWEET_POTATO_CROP.get());
    /** 花生、红豆：自己就是种子 */
    public static final Supplier<Item> PEANUT = selfSeedingFood("peanut", () -> ModBlocks.PEANUT_CROP.get());
    public static final Supplier<Item> RED_BEAN = selfSeeding("red_bean", () -> ModBlocks.RED_BEAN_CROP.get());
    public static final Supplier<Item> RED_CHILI = food("red_chili", 1, 0.3F);
    /** 大葱：葱烧海参、蘸酱菜一类用的辛香料作物 */
    public static final Supplier<Item> GREEN_ONION = food("green_onion", 1, 0.3F);
    /** 青萝卜：新作物，生吃、拌菜都行 */
    public static final Supplier<Item> GREEN_RADISH = food("green_radish", 2, 0.4F);
    /** 大虾：海产食材（获取途径待定） */
    public static final Supplier<Item> SHRIMP = food("shrimp", 2, 0.1F);
    /** 辣白菜：大缸用白菜 + 红辣椒 + 调味品腌成 */
    public static final Supplier<Item> SPICY_CABBAGE = food("spicy_cabbage", 4, 0.5F);

    // ===== 种子（右键种下对应作物）=====

    public static final Supplier<Item> EGGPLANT_SEEDS = seeds("eggplant_seeds", () -> ModBlocks.EGGPLANT_CROP.get());
    public static final Supplier<Item> GREEN_PEPPER_SEEDS = seeds("green_pepper_seeds", () -> ModBlocks.GREEN_PEPPER_CROP.get());
    public static final Supplier<Item> CORN_SEEDS = seeds("corn_seeds", () -> ModBlocks.CORN_CROP.get());
    public static final Supplier<Item> GREEN_BEANS_SEEDS = seeds("green_beans_seeds", () -> ModBlocks.GREEN_BEANS_CROP.get());
    public static final Supplier<Item> NAPA_CABBAGE_SEEDS = seeds("napa_cabbage_seeds", () -> ModBlocks.NAPA_CABBAGE_CROP.get());
    public static final Supplier<Item> CUCUMBER_SEEDS = seeds("cucumber_seeds", () -> ModBlocks.CUCUMBER_CROP.get());
    public static final Supplier<Item> RED_CHILI_SEEDS = seeds("red_chili_seeds", () -> ModBlocks.RED_CHILI_CROP.get());
    public static final Supplier<Item> GREEN_ONION_SEEDS = seeds("green_onion_seeds", () -> ModBlocks.GREEN_ONION_CROP.get());
    public static final Supplier<Item> GREEN_RADISH_SEEDS = seeds("green_radish_seeds", () -> ModBlocks.GREEN_RADISH_CROP.get());
    /** 参籽：人参的种子，种下去长成人参植株（种在雪片上时，参苗会和雪片共用一格） */
    public static final Supplier<Item> GINSENG_SEEDS = seeds("ginseng_seeds", () -> ModBlocks.GINSENG_CROP.get());

    // ===== 食材（含加工品）=====

    /** 大缸发酵大白菜得到 */
    public static final Supplier<Item> SOUR_CABBAGE = food("sour_cabbage", 3, 0.4F);
    /** 大缸发酵大豆得到，需要用碗盛出 */
    public static final Supplier<Item> SOY_PASTE = plain("soy_paste");
    /** 大酱块：6 份黄豆在厨锅里煮成，放进大缸酿大酱 */
    public static final Supplier<Item> SOY_PASTE_CHUNK = simple("soy_paste_chunk");
    /** 酱渣：大酱 / 酱油酿好后留在缸里的渣，可以继续酿醋 */
    public static final Supplier<Item> SOY_RESIDUE = simple("soy_residue");
    /**
     * 玉米茎秆：破坏玉米植株得到。
     *
     * <p>燃烧时长写在数据表里（{@code data/neoforge/data_maps/item/furnace_fuels.json}，
     * 和动物油、动物油块放在一起）—— 熔炉 / 烟熏炉 / 高炉都认，
     * 机械动力的烈焰燃烧器走的是同一个 {@code getBurnTime}，所以也认。
     */
    public static final Supplier<Item> CORN_STALK = simple("corn_stalk");
    /** 盐：目前只用于大缸腌制，来源待定 */
    public static final Supplier<Item> SALT = simple("salt");
    /** 海水桶：只在海洋中干净的水柱里取水，倒回海洋后才能继续使用。 */
    public static final Supplier<Item> SEA_WATER_BUCKET = regItem(
            "sea_water_bucket", SeaWaterBucketItem::new,
            new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET));
    /** 盐渍猪肉 */
    public static final Supplier<Item> SALTED_PORK = food("salted_pork", 5, 0.5F);
    /** 咸鱼：大缸里用任意生鱼 + 等量盐腌成（和咸腊肉一个路子，最多 5 条） */
    public static final Supplier<Item> SALTED_FISH = food("salted_fish", 4, 0.4F);
    /** 大缸发酵黄瓜得到 */
    public static final Supplier<Item> PICKLED_CUCUMBER = food("pickled_cucumber", 3, 0.4F);
    /** 大缸发酵胡萝卜得到 */
    public static final Supplier<Item> PICKLED_CARROT = food("pickled_carrot", 3, 0.4F);
    /** 大缸发酵青萝卜得到 */
    public static final Supplier<Item> PICKLED_GREEN_RADISH = food("pickled_green_radish", 3, 0.4F);
    /** 碎玉米粒在大缸里发酵得到，用来做酸汤子 */
    public static final Supplier<Item> WATER_DOUGH = simple("sour_dough");
    public static final Supplier<Item> BUCKWHEAT_NOODLES = plain("buckwheat_noodles");
    /** 豆浆压制而成 */
    public static final Supplier<Item> DRIED_TOFU = food("dried_tofu", 3, 0.4F);
    /** 豆腐：由豆浆压制（配方待定） */
    public static final Supplier<Item> TOFU = food("tofu", 3, 0.4F);
    /**
     * 植物油：动力冲压机压熟花生米挤出来，再用注液器灌进玻璃瓶。
     *
     * <p>和其它瓶装调料一样一瓶 250 mB、一次扣 50 mB（耐久条就是剩余量）；
     * 但**油炸菜**（见标签 {@code northeast_china_delight:deep_fried_dishes}）会一次用光一整瓶 ——
     * 一锅油下去了，剩下的油没法接着炒菜。标签 {@code northeast_china_delight:deep_fry_oils} 里
     * 列的就是"炸菜会烧完"的这几种油。
     */
    public static final Supplier<Item> COOKING_OIL = bottled("cooking_oil");
    /** 辣椒油：一瓶食用油 + 两个红辣椒在厨锅里炸成，玻璃瓶装 */
    public static final Supplier<Item> CHILI_OIL = bottled("chili_oil");
    /** 动物油：2 份肥肉在厨锅里熬出来，玻璃瓶装（白色偏微黄） */
    public static final Supplier<Item> ANIMAL_OIL = bottled("animal_oil");
    /** 格瓦斯：6 个面包 + 3 份水在大缸里蒙粗布毯发酵成的瓶装饮品（饮品不是调料，没有耐久条） */
    public static final Supplier<Item> KVASS = bottledDrink("kvass", 4, 0.5F);
    /** 花生酱：熟花生米磨出来，玻璃瓶装 */
    public static final Supplier<Item> PEANUT_BUTTER = bottled("peanut_butter");
    /** 熟花生米：花生在厨锅里炒熟 */
    public static final Supplier<Item> ROASTED_PEANUTS = food("roasted_peanuts", 4, 0.5F);
    /** 辣椒酱：2 红辣椒 + 盐 + 碗，工作台合成 */
    public static final Supplier<Item> CHILI_SAUCE = seasoningBowl("chili_sauce");
    /** 豆芽：大缸里 1 层水 + 黄豆蒙粗布毯发出来 */
    public static final Supplier<Item> BEAN_SPROUTS = food("bean_sprouts", 2, 0.3F);
    /** 酸玉米粒：大缸里玉米粒配水蒙粗布毯发酵而成，磨一磨就是水面团 */
    public static final Supplier<Item> SOUR_CORN_KERNELS = food("sour_corn_kernels", 3, 0.4F);
    /** 红薯淀粉：红薯 + 水在动力搅拌器里加热搅出来，用来做粉条 */
    public static final Supplier<Item> SWEET_POTATO_STARCH = simple("sweet_potato_starch");
    /** 粉条：红薯淀粉 + 水 + 盐在厨锅里煮成 */
    public static final Supplier<Item> VERMICELLI = plain("vermicelli");
    /** 玉米面：玉米粒用动力磨磨出来，做粘豆包/玉米面饼子用 */
    public static final Supplier<Item> CORN_FLOUR = plain("corn_flour");
    /** 拉皮：淀粉浆用动力冲压机压出来 */
    public static final Supplier<Item> LA_PI = plain("mung_bean_sheet");
    /** 油滋了：熬动物油时锅里留下的油渣，也能直接吃 */
    public static final Supplier<Item> CRACKLINGS = food("cracklings", 4, 0.6F);
    /** 冷面片：3 份面团在动力冲压机 + 工作盆里压出来（不用加热），烤冷面用 */
    public static final Supplier<Item> COLD_NOODLE_SHEET = food("cold_noodle_sheet", 2, 0.3F);
    /** 烤红薯：熔炉 / 烟熏炉 / 篝火 */
    public static final Supplier<Item> BAKED_SWEET_POTATO = food("baked_sweet_potato", 5, 0.6F);
    /** 灵魂酸菜：彩蛋物品，不进创造模式物品栏 */
    public static final Supplier<Item> SOUL_CABBAGE = food("spirit_cabbage", 4, 0.5F);

    // ===== 容器 =====

    /** 大脸盆：大份炖菜盛在里面的容器 */
    public static final Supplier<Item> LARGE_BASIN = simple("large_basin");
    /** 烧烤架：右键架在营火上，把营火变成可以烤东西的样子 */
    public static final Supplier<Item> GRILL_RACK = simple("grill_rack");
    /** 黄铜碗：机械动力的黄铜打的碗（具体用途与配方待定） */
    public static final Supplier<Item> BRASS_BOWL = simple("brass_bowl");

    // ===== 山珍与海味 =====

    /** 榛子：长在榛子丛上（右键采摘）；自己也是种子，种下去长成榛子丛 */
    public static final Supplier<Item> HAZELNUT = selfSeedingFood("hazelnut", () -> ModBlocks.HAZELNUT_BUSH.get());
    /** 榛蘑：长在榛子丛和阔叶树周围，自己也能种下去（右键地面就是种一株） */
    public static final Supplier<Item> HAZEL_MUSHROOM = selfSeeding("hazel_mushroom", () -> ModBlocks.HAZEL_MUSHROOM.get());
    /** 榛蘑簇：农夫乐事那套菌簇，撒骨粉能把单株榛蘑催成一丛 */
    public static final Supplier<Item> HAZEL_MUSHROOM_COLONY = regItem(
            "hazel_mushroom_colony",
            properties -> new HazelMushroomColonyItem(ModBlocks.HAZEL_MUSHROOM_COLONY.get(), properties),
            new Item.Properties());
    /** 木耳：给原木去皮时有概率掉 */
    public static final Supplier<Item> WOOD_EAR = plain("wood_ear");
    /** 人参：埋在雪地里的药材，空手刨开它脚下的土才能挖到 */
    public static final Supplier<Item> GINSENG = plain("ginseng");
    /** 冻梨：拿果园乐事的梨当兜底素材，暂时没有合成配方 */
    public static final Supplier<Item> FROZEN_PEAR = food("frozen_pear", 4, 0.4F);
    /** 带鱼、生蚝：海味，获取方式待定 */
    public static final Supplier<Item> HAIRTAIL = food("hairtail", 2, 0.1F);
    public static final Supplier<Item> OYSTER = food("oyster", 2, 0.2F);
    /** 海参：海味，获取方式待定 */
    public static final Supplier<Item> SEA_CUCUMBER = food("sea_cucumber", 2, 0.2F);
    /** 鸡架：用刀在砧板上切生鸡肉时得到 */
    public static final Supplier<Item> CHICKEN_FRAME = food("chicken_frame", 2, 0.3F);

    // ===== 掉落物 =====

    /** 猪掉落 */
    public static final Supplier<Item> PORK_RIBS = food("pork_ribs", 3, 0.3F);
    /** 猪副产物：只有用乐事的刀宰杀时才有概率掉落 */
    public static final Supplier<Item> PORK_INTESTINE = food("pork_intestine", 2, 0.2F);
    public static final Supplier<Item> PORK_HOCK = food("pork_hock", 3, 0.3F);
    public static final Supplier<Item> PIG_BLOOD = food("pig_blood", 1, 0.1F);
    public static final Supplier<Item> PIG_LIVER = food("pig_liver", 2, 0.2F);
    /** 肥肉：用刀宰杀猪时掉落（和上面几种副产物同一套概率规则），用来熬动物油 */
    public static final Supplier<Item> PORK_FAT = food("pork_fat", 2, 0.4F);
    /** 油边：猪排骨边上那条带油的肉，用刀宰杀猪时掉落，串起来烤最香 */
    public static final Supplier<Item> OIL_EDGE = food("oil_edge", 2, 0.3F);
    /** 灌肠类 */
    public static final Supplier<Item> RAW_SAUSAGE = food("raw_sausage", 3, 0.3F);
    public static final Supplier<Item> BLOOD_SAUSAGE = food("blood_sausage", 4, 0.5F);
    public static final Supplier<Item> RICE_SAUSAGE = food("rice_sausage", 4, 0.5F);
    public static final Supplier<Item> RED_SAUSAGE = food("red_sausage", 5, 0.6F);
    public static final Supplier<Item> SMOKED_PORK_HOCK = food("smoked_pork_hock", 6, 0.7F);
    public static final Supplier<Item> SMOKED_PIG_LIVER = food("smoked_pig_liver", 5, 0.5F);
    /** 酱油：大缸用酱块 + 小麦 + 盐 + 满水酿成 */
    public static final Supplier<Item> SOY_SAUCE = bottled("soy_sauce");
    /** 酱油烧的扒菜 */
    public static final Supplier<Item> BRAISED_PORK_HOCK = bowlFood("braised_pork_hock", 9, 0.8F);
    public static final Supplier<Item> BRAISED_PORK_STRIPS = bowlFood("braised_pork_strips", 8, 0.7F);
    /** 醋瓶：用玻璃瓶从大缸里装出 */
    public static final Supplier<Item> VINEGAR = bottled("vinegar");
    /**
     * 酸引水瓶：泡菜腌好后缸里的那缸水。
     *
     * <p>它是**酿白醋的原料**、也是往大缸里添酸引水用的，不是拿来炒菜的调料，
     * 所以不按 mB 记账、**没有耐久条** —— 一瓶就是一瓶，倒进缸里/酿成白醋就还一个空玻璃瓶。
     */
    public static final Supplier<Item> SOUR_WATER = bottledLiquid("sour_water");
    /** 白醋瓶：酸引水 + 谷物二次发酵得到 */
    public static final Supplier<Item> WHITE_VINEGAR = bottled("white_vinegar");
    /** 鱼露瓶：大缸里 6 份生鱼 + 3 份盐发酵出的少量液体 */
    public static final Supplier<Item> FISH_SAUCE = bottled("fish_sauce");
    /** 虾酱瓶：6 只大虾 + 3 份盐发酵而成 */
    public static final Supplier<Item> SHRIMP_PASTE = bottled("shrimp_paste");

    // ===== 厨锅料理 =====

    // 碗装：喝掉返还空碗，作为食材使用时炖锅也会把空碗弹出
    public static final Supplier<Item> SOY_MILK = bowlFood("soy_milk", 4, 0.4F);
    // 菜类与汤类都用碗盛装：吃完返还空碗
    /** 锅包肉拆成两派：老派用糖 + 白醋，新派用番茄酱 */
    /** 老派锅包肉：碗装菜，右键吃、潜行右键摆成方块（方块建模见 data/assets 里同名模型） */
    public static final Supplier<Item> OLD_STYLE_GUO_BAO_ROU =
            dishBowl("old_style_crispy_pork", () -> ModBlocks.OLD_STYLE_GUO_BAO_ROU.get(), 9, 0.7F);
    public static final Supplier<Item> NEW_STYLE_GUO_BAO_ROU = bowlFood("new_style_crispy_pork", 9, 0.7F);
    public static final Supplier<Item> DI_SAN_XIAN = bowlFood("three_fresh_veggies", 8, 0.6F);
    public static final Supplier<Item> JIAN_JIAO_GAN_DOU_FU = bowlFood("pepper_dried_tofu", 7, 0.5F);
    /** 大盆菜：物品栏里是**方块**（放下就是一整盆），取一份得到对应的「碗装XX」 */
    public static final Supplier<Item> SUAN_CAI_DUN_GU_TOU = feastItem("sauerkraut_bone_stew", () -> ModBlocks.SUAN_CAI_DUN_GU_TOU_POT.get());
    public static final Supplier<Item> SUAN_CAI_DUN_GU_TOU_BOWL = bowlFood("sauerkraut_bone_stew_bowl", 10, 0.7F);
    /** 饺子不用碗装：吃完不返还容器 */
    public static final Supplier<Item> SUAN_CAI_JIAO_ZI = food("sauerkraut_crackling_dumpling", 9, 0.7F);
    public static final Supplier<Item> DI_GUO_JI = feastItem("ground_pot_chicken", () -> ModBlocks.DI_GUO_JI_POT.get());
    public static final Supplier<Item> DI_GUO_JI_BOWL = bowlFood("ground_pot_chicken_bowl", 10, 0.7F);
    public static final Supplier<Item> SOUR_TANGZI = bowlFood("sour_noodle_soup", 5, 0.5F);
    public static final Supplier<Item> EGG_SOY_PASTE = bowlFood("steamed_egg_soy_paste", 6, 0.5F);
    public static final Supplier<Item> DI_GUO_PAI_GU = feastItem("ground_pot_ribs", () -> ModBlocks.DI_GUO_PAI_GU_POT.get());
    public static final Supplier<Item> DI_GUO_PAI_GU_BOWL = bowlFood("ground_pot_ribs_bowl", 10, 0.7F);
    /** 酱大骨、酱牛肉：直接盛出来，不用碗 */
    public static final Supplier<Item> JIANG_DA_GU = food("braised_pork_bone_sauce", 8, 0.7F);
    public static final Supplier<Item> SHA_ZHU_CAI = feastItem("slaughter_feast", () -> ModBlocks.SHA_ZHU_CAI_POT.get());
    public static final Supplier<Item> SHA_ZHU_CAI_BOWL = bowlFood("slaughter_feast_bowl", 12, 0.8F);
    public static final Supplier<Item> LA_ROU_DUN_DOU_JIAO = bowlFood("cured_pork_bean_stew", 8, 0.6F);
    public static final Supplier<Item> JIANG_NIU_ROU = food("braised_beef_sauce", 8, 0.7F);
    public static final Supplier<Item> LIU_ROU_DUAN = bowlFood("crispy_pork_strips", 9, 0.7F);
    public static final Supplier<Item> HONG_SHAO_PAI_GU = bowlFood("braised_pork_ribs", 9, 0.7F);
    /** 拔丝地瓜（物品 id 沿用早期的 ba_si_tu_dou，方便老存档继续用） */
    public static final Supplier<Item> BA_SI_TU_DOU = bowlFood("candied_sweet_potato", 7, 0.5F);
    public static final Supplier<Item> SUAN_HUANG_GUA_CHAO_ROU_SI = bowlFood("pickled_cucumber_pork_stirfry", 7, 0.6F);
    public static final Supplier<Item> SWEET_POTATO_PORRIDGE = bowlFood("sweet_potato_porridge", 7, 0.6F);
    public static final Supplier<Item> ZHU_ROU_DUN_FEN_TIAO = feastItem("pork_vermicelli_stew", () -> ModBlocks.ZHU_ROU_DUN_FEN_TIAO_POT.get());
    public static final Supplier<Item> ZHU_ROU_DUN_FEN_TIAO_BOWL = bowlFood("pork_vermicelli_stew_bowl", 10, 0.7F);
    public static final Supplier<Item> SUAN_CAI_CHAO_FEN_TIAO = bowlFood("sauerkraut_fried_vermicelli", 6, 0.5F);
    /** 朝鲜族菜：黄铜碗盛 */
    public static final Supplier<Item> BUCKWHEAT_COLD_NOODLES = brassBowlFood("buckwheat_cold_noodles", 9, 0.6F);
    public static final Supplier<Item> LIANG_BAN_XIAN_CAI = bowlFood("pickled_veggie_salad", 6, 0.5F);
    /** 朝鲜族菜：黄铜碗盛 */
    /** 明太鱼丝：普通碗装（不是朝鲜族那几道用黄铜碗的） */
    public static final Supplier<Item> MING_TAI_YU_SI = bowlFood("shredded_pollack", 6, 0.5F);
    // 工作台凉菜 / 小吃
    public static final Supplier<Item> ZHAN_JIANG_CAI = bowlFood("dip_veggie_platter", 7, 0.5F);
    public static final Supplier<Item> DA_FAN_BAO = food("northeast_rice_wrap", 10, 0.7F);
    public static final Supplier<Item> JIA_XIAN_HUANG_GUA_PAO_CAI = bowlFood("stuffed_cucumber_pickle", 6, 0.5F);
    public static final Supplier<Item> LIANG_BAN_HUA_CAI = bowlFood("mixed_vegetable_salad", 8, 0.5F);
    // 厨锅料理（东北家常菜续）
    public static final Supplier<Item> DA_FENG_SHOU = feastItem("harvest_stew", () -> ModBlocks.DA_FENG_SHOU_POT.get());
    public static final Supplier<Item> DA_FENG_SHOU_BOWL = bowlFood("harvest_stew_bowl", 12, 0.8F);
    public static final Supplier<Item> BAI_CAI_DOU_FU_DUN_FEN_TIAO = bowlFood("cabbage_tofu_vermicelli_stew", 8, 0.6F);
    /** 朝鲜族菜：黄铜碗盛 */
    public static final Supplier<Item> LA_NIU_ROU_TANG_FAN = brassBowlFood("spicy_beef_soup", 10, 0.7F);
    /** 朝鲜族菜：黄铜碗盛 */
    public static final Supplier<Item> LA_BAI_CAI_CHAO_FAN = brassBowlFood("kimchi_fried_rice", 10, 0.7F);
    /** 土豆饼：碗装（吃完返还空碗） */
    public static final Supplier<Item> TU_DOU_BING = bowlFood("potato_pancake", 7, 0.6F);
    public static final Supplier<Item> XIA_REN_ZHU_ROU_XIAN_SHUI_JIAO = food("shrimp_pork_dumpling", 10, 0.7F);
    public static final Supplier<Item> XIA_JIANG_CHAO_JI_DAN = bowlFood("shrimp_paste_scrambled_egg", 7, 0.6F);
    public static final Supplier<Item> XIA_JIANG_DUN_DOU_FU = bowlFood("shrimp_paste_tofu_stew", 7, 0.6F);
    public static final Supplier<Item> HAI_XIAN_DOU_FU_TANG = bowlFood("seafood_tofu_soup", 8, 0.6F);
    public static final Supplier<Item> SAN_XIAN_XIAN_SHUI_JIAO = food("three_delicacy_dumpling", 10, 0.7F);
    /** 朝鲜族菜：黄铜碗盛 */
    public static final Supplier<Item> LA_BAI_CAI_TANG_FAN = brassBowlFood("kimchi_tofu_soup", 9, 0.6F);
    /** 玉米烙：碗装 */
    public static final Supplier<Item> YU_MI_LAO = bowlFood("corn_fritter", 7, 0.6F);
    public static final Supplier<Item> CHA_ZI_ZHOU = bowlFood("crushed_corn_porridge", 6, 0.5F);
    public static final Supplier<Item> NIAN_DOU_BAO = food("sticky_bean_bun", 7, 0.6F);
    /** 榛子糖火烧：榛子、糖、面团和植物油做成的无容器点心 */
    public static final Supplier<Item> HAZELNUT_SUGAR_FIRE_BUN = food("hazelnut_sugar_fire_bun", 7, 0.6F);
    public static final Supplier<Item> XUN_JIANG_PIN_PAN = bowlFood("smoked_meat_platter", 10, 0.7F);
    /** 果园乐事联动：橘子汁做的新派橙汁锅包肉 */
    public static final Supplier<Item> ORANGE_GUO_BAO_ROU = bowlFood("orange_crispy_pork", 9, 0.7F);
    /** 酱拌拉皮：碗 + 拉皮 + 花生酱 + 黄瓜 + 番茄 */
    public static final Supplier<Item> JIANG_BAN_LA_PI = bowlFood("mung_bean_sheet_salad", 7, 0.5F);
    /** 葱烧海参：大葱 + 海参 */
    public static final Supplier<Item> CONG_SHAO_HAI_SHEN = bowlFood("braised_sea_cucumber_scallion", 8, 0.7F);
    /** 海参豆腐汤：海参 + 豆腐 */
    public static final Supplier<Item> HAI_SHEN_DOU_FU_TANG = bowlFood("sea_cucumber_tofu_soup", 7, 0.6F);
    /** 咸鱼饼子：2 咸鱼 + 2 面团，碗装 */
    public static final Supplier<Item> XIAN_YU_BING_ZI = bowlFood("salted_fish_flatbread", 8, 0.6F);
    /** 干炸带鱼：盘装菜 */
    public static final Supplier<Item> DRY_FRIED_HAIRTAIL = bowlFood("dry_fried_hairtail", 8, 0.6F);
    /** 红烧带鱼：盘装菜 */
    public static final Supplier<Item> BRAISED_HAIRTAIL = bowlFood("braised_hairtail", 8, 0.6F);
    /** 朝鲜族菜：黄铜碗盛 */
    public static final Supplier<Item> DA_JIANG_TANG = brassBowlFood("soy_paste_soup", 6, 0.5F);
    /** 得莫利炖鱼：分量大，用大脸盆盛 */
    public static final Supplier<Item> DE_MO_LI_DUN_YU = bowlFood("demoli_fish_stew", 11, 0.8F);
    public static final Supplier<Item> SU_BO_TANG = bowlFood("borscht", 10, 0.7F);
    /** 酸菜海鲜锅：分量大，和大盆菜一样是「盆装方块 + 碗装一份」 */
    public static final Supplier<Item> SUAN_CAI_HAI_XIAN_GUO =
            feastItem("sauerkraut_seafood_stew", () -> ModBlocks.SUAN_CAI_HAI_XIAN_GUO_POT.get());
    public static final Supplier<Item> SUAN_CAI_HAI_XIAN_GUO_BOWL =
            bowlFood("sauerkraut_seafood_stew_bowl", 11, 0.8F);
    /** 朝鲜族菜：黄铜碗盛 */
    public static final Supplier<Item> SHEN_JI_TANG = brassBowlFood("ginseng_chicken_soup", 10, 0.7F);
    public static final Supplier<Item> XIAO_JI_DUN_MO_GU = feastItem("chicken_mushroom_stew", () -> ModBlocks.XIAO_JI_DUN_MO_GU_POT.get());
    public static final Supplier<Item> XIAO_JI_DUN_MO_GU_BOWL = bowlFood("chicken_mushroom_stew_bowl", 10, 0.7F);
    /** 拌饭：黄铜碗 + 米饭 + 胡萝卜 + 鸡蛋 + 干海带 + 辣白菜 + 辣椒酱，工作台合成 */
    public static final Supplier<Item> BIBIMBAP = brassBowlFood("bibimbap", 10, 0.7F);
    /** 辣白菜饼：辣白菜 + 2 淀粉 + 植物油，厨锅煎成（饼类用普通碗盛） */
    public static final Supplier<Item> KIMCHI_PANCAKE = bowlFood("kimchi_pancake", 8, 0.6F);
    /** 烤鸡架：生鸡架刷辣椒油撒糖，放在烤架上烤出来 */
    public static final Supplier<Item> GRILLED_CHICKEN_FRAME = food("grilled_chicken_frame", 6, 0.6F);
    /** 雪绵豆沙：红豆 + 糖 + 鸡蛋 + 淀粉 + 动物油，厨锅做 */
    public static final Supplier<Item> SNOWY_BEAN_PASTE = bowlFood("snowy_bean_paste", 7, 0.6F);
    /** 糖花生：糖 + 熟花生米，厨锅做 */
    public static final Supplier<Item> CANDIED_PEANUTS = bowlFood("candied_peanuts", 5, 0.5F);
    /** 老虎菜：青椒 + 大葱 + 酱油 + 黄瓜 + 醋 */
    public static final Supplier<Item> TIGER_SALAD = bowlFood("tiger_salad", 6, 0.5F);
    /** 烧烤出品：烤玉米、烤油边、烤冷面 */
    public static final Supplier<Item> GRILLED_CORN = food("grilled_corn", 5, 0.5F);
    public static final Supplier<Item> GRILLED_OIL_EDGE = food("grilled_oil_edge", 6, 0.6F);
    public static final Supplier<Item> GRILLED_COLD_NOODLES = food("grilled_cold_noodles", 7, 0.6F);

    // ===== 方块物品 =====

    public static final Supplier<Item> UNFIRED_VAT_BLANK = regItem(
            "unfired_vat_blank",
            Item::new,
            new Item.Properties().stacksTo(1)
    );

    public static final Supplier<Item> VAT = regItem(
            "vat",
            (properties) -> new BlockItem(ModBlocks.VAT.get(), properties),
            new Item.Properties().stacksTo(1)
    );

    /** 创造大缸：同外观，没有合成配方，条件凑齐瞬间酿好 */
    public static final Supplier<Item> CREATIVE_VAT = regItem(
            "creative_vat",
            (properties) -> new BlockItem(ModBlocks.CREATIVE_VAT.get(), properties),
            new Item.Properties().stacksTo(1)
    );

    public static final Supplier<Item> ANIMAL_OIL_BLOCK_ITEM = regItem(
            "animal_oil_block",
            (properties) -> new BlockItem(ModBlocks.ANIMAL_OIL_BLOCK.get(), properties),
            new Item.Properties()
    );

    public static final Supplier<Item> CORN_SKEWER = regItem(
            "corn_skewer",
            properties -> new SkewerItem(ModBlocks.CORN_SKEWER.get(), properties),
            new Item.Properties()
    );
    public static final Supplier<Item> CHILI_SKEWER = regItem(
            "chili_skewer",
            properties -> new SkewerItem(ModBlocks.CHILI_SKEWER.get(), properties),
            new Item.Properties()
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
            NAPA_CABBAGE_SEEDS, CUCUMBER_SEEDS, RED_CHILI_SEEDS, GREEN_ONION_SEEDS, GREEN_RADISH_SEEDS,
            GINSENG_SEEDS,
            // 蔬菜
            NAPA_CABBAGE, CUCUMBER, EGGPLANT, GREEN_PEPPER, GREEN_ONION, RED_CHILI,
            GREEN_BEANS, CORN, SWEET_POTATO, GREEN_RADISH,
            // 豆类与杂粮
            SOYBEAN, PEANUT, RED_BEAN, BUCKWHEAT,
            // 山珍
            HAZELNUT, HAZEL_MUSHROOM, HAZEL_MUSHROOM_COLONY, WOOD_EAR, GINSENG,
            // 水果
            FROZEN_PEAR,
            // 肉类与水产
            PORK_RIBS, PORK_INTESTINE, PORK_HOCK, PIG_BLOOD, PIG_LIVER, PORK_FAT, OIL_EDGE, CHICKEN_FRAME,
            SHRIMP, HAIRTAIL, OYSTER, SEA_CUCUMBER,
            // 肉制品（灌肠与熏货）
            RAW_SAUSAGE, BLOOD_SAUSAGE, RICE_SAUSAGE, RED_SAUSAGE,
            SMOKED_PORK_HOCK, SMOKED_PIG_LIVER,
            // 腌制品
            SOUR_CABBAGE, SPICY_CABBAGE, PICKLED_CUCUMBER, PICKLED_CARROT, PICKLED_GREEN_RADISH,
            SALTED_PORK, SALTED_FISH,
            // 调料
            SALT, SEA_WATER_BUCKET, SOY_PASTE, CHILI_SAUCE, SOY_SAUCE, VINEGAR, SOUR_WATER, WHITE_VINEGAR,
            FISH_SAUCE, SHRIMP_PASTE, CHILI_OIL, PEANUT_BUTTER, COOKING_OIL, ANIMAL_OIL,
            // 酿造原料
            SOY_PASTE_CHUNK, SOY_RESIDUE,
            // 主食与半成品
            WATER_DOUGH, BUCKWHEAT_NOODLES, DRIED_TOFU, TOFU, SWEET_POTATO_STARCH,
            VERMICELLI, LA_PI, CORN_FLOUR, CRACKLINGS, COLD_NOODLE_SHEET, BEAN_SPROUTS, SOUR_CORN_KERNELS, ROASTED_PEANUTS,
            CORN_STALK,
            // 厨具与容器
            LARGE_BASIN, BRASS_BOWL, ANIMAL_OIL_BLOCK_ITEM,
            // 方块（大缸与烧烤架归到「方块」那一项里，见 BLOCK_TAB_ITEMS）
            UNFIRED_VAT_BLANK
    );

    /**
     * 「方块」物品栏：大缸、烧烤架，以及食物方块（箱装 / 袋装）。
     *
     * <p>顺序：大缸 → 烧烤架 → 箱装（{@link com.gunmu.northeast_china_delight.block.ModBlocks#CRATE_IDS}，
     * 蔬菜在前、腌菜在后）→ 袋装（{@code SACK_IDS}，谷物在前、山珍在后）。
     */
    public static final Map<String, Supplier<Item>> STORAGE_BLOCK_ITEMS = storageBlockItems();

    private static Map<String, Supplier<Item>> storageBlockItems() {
        Map<String, Supplier<Item>> map = new LinkedHashMap<>();
        for (var entry : ModBlocks.CRATES.entrySet()) {
            map.put(entry.getKey(), blockItem(entry.getKey(), entry.getValue()));
        }
        for (var entry : ModBlocks.SACKS.entrySet()) {
            map.put(entry.getKey(), blockItem(entry.getKey(), entry.getValue()));
        }
        return Collections.unmodifiableMap(map);
    }

    private static Supplier<Item> blockItem(String id, Supplier<? extends Block> block) {
        return regItem(id, properties -> new BlockItem(block.get(), properties), new Item.Properties());
    }

    /** 「方块」物品栏的展示顺序：大缸 → 烧烤架 → 箱装 → 袋装 */
    public static final List<Supplier<? extends Item>> BLOCK_TAB_ITEMS = blockTabItems();

    private static List<Supplier<? extends Item>> blockTabItems() {
        List<Supplier<? extends Item>> list = new ArrayList<>();
        list.add(VAT);
        list.add(GRILL_RACK);
        list.add(CORN_SKEWER);
        list.add(CHILI_SKEWER);
        list.addAll(STORAGE_BLOCK_ITEMS.values());
        return List.copyOf(list);
    }

    /**
     * 「菜肴」物品栏的展示顺序：格瓦斯 → 盘装 → 无容器 → 普通碗装 → 黄铜碗装 → 盆装。
     * 无容器菜单独列出，不能因为借用餐盘模型就误归入碗装。
     */
    public static final List<Supplier<? extends Item>> DISH_TAB_ITEMS = List.of(
            // ===== 唯一的瓶装菜肴 =====
            KVASS,
            // ===== 盘装 =====
            OLD_STYLE_GUO_BAO_ROU, NEW_STYLE_GUO_BAO_ROU, ORANGE_GUO_BAO_ROU,
            LIU_ROU_DUAN, HONG_SHAO_PAI_GU, DI_SAN_XIAN, JIAN_JIAO_GAN_DOU_FU,
            BRAISED_PORK_HOCK, BRAISED_PORK_STRIPS, BA_SI_TU_DOU,
            XUN_JIANG_PIN_PAN, SNOWY_BEAN_PASTE, CANDIED_PEANUTS,
            TU_DOU_BING, YU_MI_LAO, XIAN_YU_BING_ZI, KIMCHI_PANCAKE,
            SUAN_CAI_CHAO_FEN_TIAO, SUAN_HUANG_GUA_CHAO_ROU_SI,
            LIANG_BAN_XIAN_CAI, LIANG_BAN_HUA_CAI, JIA_XIAN_HUANG_GUA_PAO_CAI,
            ZHAN_JIANG_CAI, MING_TAI_YU_SI, TIGER_SALAD, XIA_JIANG_CHAO_JI_DAN,
            JIANG_BAN_LA_PI, CONG_SHAO_HAI_SHEN, LA_ROU_DUN_DOU_JIAO,
            DRY_FRIED_HAIRTAIL, BRAISED_HAIRTAIL,
            // ===== 无容器（单独列出，不归入碗装） =====
            BAKED_SWEET_POTATO, DA_FAN_BAO, NIAN_DOU_BAO, HAZELNUT_SUGAR_FIRE_BUN,
            GRILLED_CHICKEN_FRAME, GRILLED_CORN, GRILLED_OIL_EDGE, GRILLED_COLD_NOODLES,
            SUAN_CAI_JIAO_ZI, XIA_REN_ZHU_ROU_XIAN_SHUI_JIAO, SAN_XIAN_XIAN_SHUI_JIAO,
            JIANG_DA_GU, JIANG_NIU_ROU,
            // ===== 普通碗装 =====
            SOY_MILK, SOUR_TANGZI, CHA_ZI_ZHOU, SWEET_POTATO_PORRIDGE,
            EGG_SOY_PASTE, XIA_JIANG_DUN_DOU_FU, HAI_XIAN_DOU_FU_TANG,
            HAI_SHEN_DOU_FU_TANG, DE_MO_LI_DUN_YU, SU_BO_TANG,
            BAI_CAI_DOU_FU_DUN_FEN_TIAO,
            DI_GUO_JI_BOWL, DI_GUO_PAI_GU_BOWL, SHA_ZHU_CAI_BOWL,
            SUAN_CAI_DUN_GU_TOU_BOWL, ZHU_ROU_DUN_FEN_TIAO_BOWL,
            XIAO_JI_DUN_MO_GU_BOWL, SUAN_CAI_HAI_XIAN_GUO_BOWL, DA_FENG_SHOU_BOWL,
            // ===== 黄铜碗装 =====
            LA_NIU_ROU_TANG_FAN, LA_BAI_CAI_CHAO_FAN, LA_BAI_CAI_TANG_FAN,
            DA_JIANG_TANG, SHEN_JI_TANG, BUCKWHEAT_COLD_NOODLES, BIBIMBAP,
            // ===== 盆装 / 锅装 =====
            DI_GUO_JI, DI_GUO_PAI_GU, SHA_ZHU_CAI, DA_FENG_SHOU,
            SUAN_CAI_DUN_GU_TOU, ZHU_ROU_DUN_FEN_TIAO,
            XIAO_JI_DUN_MO_GU, SUAN_CAI_HAI_XIAN_GUO
    );

    /**
     * 按「属性 → 物品」的工厂注册一个物品。
     *
     * <p>1.20.1（Forge）的 DeferredRegister 没有 registerItem，属性得先套好再交出去；
     * 1.20.2 起 NeoForge 提供了 registerItem，用它才能正确继承物品的属性默认值。</p>
     */
    private static <I extends Item> Supplier<I> regItem(String id,
                                                        Function<Item.Properties, ? extends I> factory,
                                                        Item.Properties properties) {
        //? if <1.20.2 {
        return ITEMS.register(id, () -> factory.apply(properties));
        //?} else {
        /*return ITEMS.registerItem(id, factory, properties);
        *///?}
    }

    /** 登记一个只有默认属性的普通物品 */
    private static Supplier<Item> simple(String id) {
        return regItem(id, Item::new, new Item.Properties());
    }

    /**
     * 玻璃瓶装的调料：一瓶 250 mB，用一次扣 50 mB（见 {@link SeasoningBottleItem}）。
     *
     * 物品属性里仍然留着 {@code craftRemainder(空玻璃瓶)}：用空了的瓶子自然就是空瓶，
     * 而"还能用几次"是靠 {@link SeasoningBottleItem#getCraftingRemainingItem(ItemStack)} 返回带耐久的瓶子。
     */
    private static Supplier<Item> bottled(String id) {
        return regItem(id, SeasoningBottleItem::new, new Item.Properties()
                .craftRemainder(Items.GLASS_BOTTLE)
                .durability(SeasoningBottleItem.CAPACITY_MB));
    }

    /** 玻璃瓶装的饮品：既是食物也返还瓶子（同样带余量） */
    private static Supplier<Item> bottledFood(String id) {
        return regItem(id, SeasoningBottleItem::new,
                new Item.Properties()
                        .craftRemainder(Items.GLASS_BOTTLE)
                        .food(steakFood().build())
                        .durability(SeasoningBottleItem.CAPACITY_MB));
    }

    private static Supplier<Item> bottledFood(String id, int nutrition, float saturation) {
        return regItem(id, SeasoningBottleItem::new,
                new Item.Properties()
                        .craftRemainder(Items.GLASS_BOTTLE)
                        .food(stats(nutrition, saturation).build())
                        .durability(SeasoningBottleItem.CAPACITY_MB));
    }

    /**
     * 玻璃瓶装的**饮品**：喝完返还空玻璃瓶，但**不带耐久条** —— 它不像调料瓶那样按 mB 记账，
     * 也没有「还能用几次」的概念，一瓶就是一瓶。
     *
     * 液面颜色照样由 {@link BottleColors} 决定（模型用的是原版药水那两层贴图）。
     */
    private static Supplier<Item> bottledDrink(String id) {
        return regItem(id, DdConsumableItem::new, new Item.Properties()
                .craftRemainder(Items.GLASS_BOTTLE)
                .food(foodReturning(steakFood(), Items.GLASS_BOTTLE)));
    }

    private static Supplier<Item> bottledDrink(String id, int nutrition, float saturation) {
        return regItem(id, DdConsumableItem::new, new Item.Properties()
                .craftRemainder(Items.GLASS_BOTTLE)
                .food(foodReturning(stats(nutrition, saturation), Items.GLASS_BOTTLE)));
    }

    /**
     * 玻璃瓶装的**非调料液体**（酸引水这类"倒进大缸用的原料"）：
     * 一瓶就是一瓶，不按 mB 记账、**没有耐久条**，用掉之后返还空玻璃瓶。
     */
    private static Supplier<Item> bottledLiquid(String id) {
        return regItem(id, Item::new, new Item.Properties()
                .craftRemainder(Items.GLASS_BOTTLE));
    }

    /** 碗装的调料：用完返还空碗（跟 bowlFood 一样，但不可食用） */
    private static Supplier<Item> seasoningBowl(String id) {
        return regItem(id, Item::new, new Item.Properties()
                .craftRemainder(Items.BOWL));
    }

    /**
     * 「自身即种子」的食物：右键种下对应的作物，收获时也得到这个物品。
     * 用 ItemNameBlockItem 保证名字走物品的翻译键。
     */
    private static Supplier<Item> selfSeedingFood(String id, Supplier<? extends Block> crop) {
        return regItem(id,
                properties -> new ItemNameBlockItem(crop.get(), properties),
                new Item.Properties().food(steakFood().build()));
    }

    /** 「自身即种子」的**不可食用**原料：大豆、红豆、荞麦、榛蘑、榛子都得做熟了/加工了才能吃 */
    private static Supplier<Item> selfSeeding(String id, Supplier<? extends Block> crop) {
        return regItem(id,
                properties -> new ItemNameBlockItem(crop.get(), properties),
                new Item.Properties());
    }

    /** 单纯注册一个普通物品（不能吃）：调料、加工品原料用 */
    private static Supplier<Item> plain(String id) {
        return regItem(id, Item::new, new Item.Properties());
    }
    private static Supplier<Item> selfSeedingFood(String id, Supplier<? extends Block> crop,
                                                      int nutrition, float saturation) {
        return regItem(id,
                properties -> new ItemNameBlockItem(crop.get(), properties),
                new Item.Properties().food(stats(nutrition, saturation).build()));
    }

    /**
     * 种子物品：右键种下对应的作物方块。
     * 用 ItemNameBlockItem 而不是普通 BlockItem —— 否则物品名会用方块的翻译键，
     * 显示成「大豆」而不是「大豆种子」。
     */
    private static Supplier<Item> seeds(String id, Supplier<? extends Block> crop) {
        return regItem(id, properties -> new ItemNameBlockItem(crop.get(), properties), new Item.Properties());
    }

    /** 登记一个食物，数值暂用牛排（熟牛肉）：8 点饱食度、0.8 饱和度系数 */
    private static Supplier<Item> food(String id) {
        return regItem(id, Item::new, new Item.Properties().food(steakFood().build()));
    }

    private static Supplier<Item> food(String id, int nutrition, float saturation) {
        return regItem(id, Item::new,
                new Item.Properties().food(stats(nutrition, saturation).build()));
    }

    /**
     * 用碗盛装的料理。
     * usingConvertsTo：吃完返还空碗；
     * craftRemainder：作为食材放进炖锅时，农夫乐事会把空碗弹出（比如豆浆做成干豆腐）。
     */
    private static Supplier<Item> bowlFood(String id) {
        return regItem(id, DdConsumableItem::new, new Item.Properties()
                .craftRemainder(Items.BOWL)
                .food(foodReturning(steakFood(), Items.BOWL)));
    }

    private static Supplier<Item> bowlFood(String id, int nutrition, float saturation) {
        return regItem(id, DdConsumableItem::new, new Item.Properties()
                .craftRemainder(Items.BOWL)
                .food(foodReturning(stats(nutrition, saturation), Items.BOWL)));
    }

    /**
     * 碗装菜肴：物品形态既能吃、也能摆。
     * 右键 = 吃；潜行右键 = 摆成方块（方块形态由 {@code DishBlockItem} + 配置开关控制）。
     */
    private static Supplier<Item> dishBowl(String id, Supplier<? extends Block> block) {
        return regItem(id,
                properties -> new DishBlockItem(block.get(), properties),
                new Item.Properties()
                        .craftRemainder(Items.BOWL)
                        .food(foodReturning(steakFood(), Items.BOWL)));
    }

    private static Supplier<Item> dishBowl(String id, Supplier<? extends Block> block,
                                               int nutrition, float saturation) {
        return regItem(id,
                properties -> new DishBlockItem(block.get(), properties),
                new Item.Properties()
                        .craftRemainder(Items.BOWL)
                        .food(foodReturning(stats(nutrition, saturation), Items.BOWL)));
    }

    /**
     * 用大脸盆盛装的料理（分量大的炖菜）：吃完返还大脸盆，作为食材放进炖锅时也会把空盆弹出。
     *
     * 注意这里必须用 supplier 把属性拖到「物品注册事件」里现做：
     * 静态初始化阶段物品注册表还是冻结状态，既建不了 Item 实例，也取不到 LARGE_BASIN 的实例。
     */
    private static Supplier<Item> basinFood(String id) {
        return ITEMS.register(id, () -> new DdConsumableItem(new Item.Properties()
                .craftRemainder(LARGE_BASIN.get())
                .food(foodReturning(steakFood(), LARGE_BASIN.get()))));
    }

    private static Supplier<Item> basinFood(String id, int nutrition, float saturation) {
        return ITEMS.register(id, () -> new DdConsumableItem(new Item.Properties()
                .craftRemainder(LARGE_BASIN.get())
                .food(foodReturning(stats(nutrition, saturation), LARGE_BASIN.get()))));
    }

    /**
     * 大盆菜的**方块物品**：放下就是一大盆（见 {@code DdFeastBlock}），右键取一份得「碗装XX」。
     * 一盆就是一份菜，所以**不能堆叠**（最大堆叠 1）。
     */
    private static Supplier<Item> feastItem(String id, Supplier<? extends Block> block) {
        return regItem(id, properties -> new BlockItem(block.get(), properties),
                new Item.Properties().stacksTo(1));
    }

    /**
     * 用黄铜碗盛装的料理 —— 目前是**朝鲜族菜**的辨识方式：
     * 辣白菜豆腐汤、辣白菜炒饭、辣牛肉汤、大酱汤、参鸡汤、荞麦冷面、明太鱼丝、拌饭、辣白菜饼都用它。
     *
     * 和大脸盆一样：吃完返还黄铜碗，作为食材放进炖锅时也会把空碗弹出。
     * 同样要拖到物品注册事件里现做，静态初始化阶段取不到 BRASS_BOWL 的实例。
     */
    private static Supplier<Item> brassBowlFood(String id) {
        return ITEMS.register(id, () -> new DdConsumableItem(new Item.Properties()
                .craftRemainder(BRASS_BOWL.get())
                .food(foodReturning(steakFood(), BRASS_BOWL.get()))));
    }

    private static Supplier<Item> brassBowlFood(String id, int nutrition, float saturation) {
        return ITEMS.register(id, () -> new DdConsumableItem(new Item.Properties()
                .craftRemainder(BRASS_BOWL.get())
                .food(foodReturning(stats(nutrition, saturation), BRASS_BOWL.get()))));
    }

    /**
     * 造一份「吃完返还容器」的食物属性。
     *
     * <p>1.21 起写在食物组件里（{@code usingConvertsTo}）；1.20.1 没有这个 API，
     * 容器由 {@link DdConsumableItem} 按 {@code craftRemainder} 在吃完时还给玩家。</p>
     */
    private static FoodProperties foodReturning(FoodProperties.Builder builder, ItemLike remainder) {
        //? if <1.20.5 {
        return builder.build();
        //?} else {
        /*return builder.usingConvertsTo(remainder).build();
        *///?}
    }

    /** 饱和度倍率：1.20.1 叫 saturationMod，1.20.5 起改叫 saturationModifier */
    private static FoodProperties.Builder withSaturation(FoodProperties.Builder builder, float value) {
        //? if <1.20.5 {
        return builder.saturationMod(value);
        //?} else {
        /*return builder.saturationModifier(value);
        *///?}
    }

    private static FoodProperties.Builder steakFood() {
        return withSaturation(new FoodProperties.Builder().nutrition(8), 0.8F);
    }

    private static FoodProperties.Builder stats(int nutrition, float saturation) {
        return withSaturation(new FoodProperties.Builder().nutrition(nutrition), saturation);
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
