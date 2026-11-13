package com.gunmu.northeast_china_delight.item;


import com.gunmu.chinese_regional_culinary_ingredients.block.DdFeastBlock;
import com.gunmu.chinese_regional_culinary_ingredients.client.DdFeastItemRenderer;
import com.gunmu.chinese_regional_culinary_ingredients.client.LargeBasinItemRenderer;
import com.gunmu.chinese_regional_culinary_ingredients.client.WoodenMalletItemRenderer;
import com.gunmu.chinese_regional_culinary_ingredients.item.DdConsumableItem;
import com.gunmu.chinese_regional_culinary_ingredients.item.DdFeastItem;
import com.gunmu.chinese_regional_culinary_ingredients.item.DishBlockItem;
import com.gunmu.chinese_regional_culinary_ingredients.item.LargeBasinItem;
import com.gunmu.chinese_regional_culinary_ingredients.item.MysteryStewEffects;
import com.gunmu.chinese_regional_culinary_ingredients.item.WaterBasinItem;
import com.gunmu.chinese_regional_culinary_ingredients.item.WoodenMalletItem;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.chinese_regional_culinary_ingredients.item.CrciItems;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
//? if <1.20.5 {
import net.minecraft.world.item.RecordItem;
//?} else {
/*import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;
*///?}
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;
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
 * 本模组的全部物品。食物暂时统一使用牛排（熟牛肉）的数值：8 点饱食度、0.8 饱和度系数，待模组成型后再逐个调整。
 */
public class ModItems {

    //? if <1.20.2 {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.ITEMS, NortheastChinaDelight.MODID);
    //?} else {
    /*public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NortheastChinaDelight.MODID);
    *///?}

    public static final TagKey<Item> DEEP_FRY_OILS = TagKey.create(Registries.ITEM,
            DdIds.of(NortheastChinaDelight.MODID, "deep_fry_oils"));
    public static final TagKey<Item> DEEP_FRIED_DISHES = TagKey.create(Registries.ITEM,
            DdIds.of(NortheastChinaDelight.MODID, "deep_fried_dishes"));



    /**
     * 海水桶：3.2.2 起不再使用（制盐改用水盆），没有任何获取方式，只留着让老存档里的海水桶不丢。
     * 用起来就是一只普通水桶（倒水、倒完剩空桶）；不进创造物品栏，JEI 里也隐藏（见 NortheastJeiPlugin）。
     */
    public static final Supplier<Item> SEA_WATER_BUCKET = regItem("sea_water_bucket",
            properties -> new BucketItem(Fluids.WATER, properties),
            new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET));
    /** 格瓦斯：由 CRCI 注册，东北只保留别名供地区配方与交易引用。 */
    public static final Supplier<Item> KVASS = CrciItems.KVASS;
    /**
     * 黄桃罐头：7 点饥饿值、实际 3 点饱和度（原版公式为 nutrition × modifier × 2，
     * 因此这里换算成 3/14 的饱和度系数）；吃完返还玻璃瓶。
     * 物品栏贴图与手持 / 掉落模型的分流见 {@code models/item/canned_yellow_peach.json}。
     */
    public static final Supplier<Item> CANNED_YELLOW_PEACH = regItem("canned_yellow_peach",
            properties -> new CannedYellowPeachItem(ModBlocks.CANNED_YELLOW_PEACH.get(), properties),
            new Item.Properties()
                    .craftRemainder(Items.GLASS_BOTTLE)
                    .food(foodReturning(statsWithSaturationPoints(7, 3.0F), Items.GLASS_BOTTLE)));
    /** 豆沙：红豆 + 糖在木臼里舂 4 下出 2 份；能直接吃，比用它做的菜（红豆松饼 3 / 0.3 起）都低 */
    /** 湿粉团：木臼里 1 个红薯加一份水（一桶 / 一盆 / 一瓶）舂出 2 个，放烟熏炉熏干成淀粉（2026-11-05 用户：要批量烟熏，不要批量熔炼） */
    public static final Supplier<Item> BAKED_SWEET_POTATO = food("baked_sweet_potato", 5, 0.6F);

    /**
     * 大脸盆：空盆能放在地上（方块见 {@link ModBlocks#LARGE_BASIN}），也能扣头上（见 {@link LargeBasinItem}）。
     * 物品图标仍旧是 2D 贴图 —— 2D/3D 的分流放在客户端渲染器里（{@code client/LargeBasinItemRenderer}）。
     */
    public static final Supplier<Item> LARGE_BASIN = CrciItems.LARGE_BASIN;
    /**
     * 水盆：装了一盆水（1000 mB）的大脸盆。空盆对着水右键舀，或在机械动力注液器下灌；
     * 右键倒水、潜行右键放在地上（见 {@link WaterBasinItem}，方块见 {@link ModBlocks#WATER_BASIN}）；放进厨锅煮盐后返还空盆。
     * 返还物要拿 LARGE_BASIN 的实例，所以和 basinFood 一样在注册事件里现做属性。
     */
    public static final Supplier<Item> WATER_BASIN = CrciItems.WATER_BASIN;
    public static final Supplier<Item> GRILL_RACK = CrciItems.GRILL_RACK;
    public static final Supplier<Item> BRASS_BOWL = CrciItems.BRASS_BOWL;

    /**
     * 木槌：木臼「舂捣」配方里的工具，本身也是一把钝器（耐久 118、攻击 5 / 攻速 1.0）。
     * 物品栏是 2D 贴图、拿在手里是 3D 模型，分流在 {@code client/WoodenMalletItemRenderer}。
     */
    public static final Supplier<Item> WOODEN_MALLET = CrciItems.WOODEN_MALLET;

    /** 木耳本身就是「种子」：对着带皮的橡木 / 白桦 / 深色橡木侧面右键种下段木木耳 */

    public static final Supplier<Item> BRAISED_PORK_HOCK = bowlFood("braised_pork_hock", 9, 0.8F);
    public static final Supplier<Item> BRAISED_PORK_STRIPS = bowlFood("braised_pork_strips", 8, 0.7F);

    public static final Supplier<Item> SOY_MILK = CrciItems.SOY_MILK;
    public static final Supplier<Item> OLD_STYLE_GUO_BAO_ROU =
            dishBowl("old_style_crispy_pork", () -> ModBlocks.OLD_STYLE_GUO_BAO_ROU.get(), 9, 0.7F);
    public static final Supplier<Item> NEW_STYLE_GUO_BAO_ROU = bowlFood("new_style_crispy_pork", 9, 0.7F);
    public static final Supplier<Item> DI_SAN_XIAN = bowlFood("three_fresh_veggies", 8, 0.6F);
    /** 土豆焖茄子（2026-11-07 用户）：马铃薯 + 茄子 + 大葱 + 大酱，厨锅碗装；比地三鲜少过一道油，饱食度低一点 */
    public static final Supplier<Item> TU_DOU_MEN_QIE_ZI = bowlFood("braised_eggplant_potato", 7, 0.6F);
    public static final Supplier<Item> JIAN_JIAO_GAN_DOU_FU = bowlFood("pepper_dried_tofu", 7, 0.5F);
    public static final Supplier<Item> SUAN_CAI_DUN_GU_TOU = feastItem("sauerkraut_bone_stew", () -> ModBlocks.SUAN_CAI_DUN_GU_TOU_POT.get());
    public static final Supplier<Item> SUAN_CAI_DUN_GU_TOU_BOWL = bowlFood("sauerkraut_bone_stew_bowl", 10, 0.7F);
    public static final Supplier<Item> SUAN_CAI_JIAO_ZI = food("sauerkraut_crackling_dumpling", 9, 0.7F);
    public static final Supplier<Item> DI_GUO_JI = feastItem("ground_pot_chicken", () -> ModBlocks.DI_GUO_JI_POT.get());
    public static final Supplier<Item> DI_GUO_JI_BOWL = bowlFood("ground_pot_chicken_bowl", 10, 0.7F);
    public static final Supplier<Item> SOUR_TANGZI = bowlFood("sour_noodle_soup", 5, 0.5F);
    public static final Supplier<Item> EGG_SOY_PASTE = bowlFood("steamed_egg_soy_paste", 6, 0.5F);
    public static final Supplier<Item> DI_GUO_PAI_GU = feastItem("ground_pot_ribs", () -> ModBlocks.DI_GUO_PAI_GU_POT.get());
    public static final Supplier<Item> DI_GUO_PAI_GU_BOWL = bowlFood("ground_pot_ribs_bowl", 10, 0.7F);
    public static final Supplier<Item> JIANG_DA_GU = food("braised_pork_bone_sauce", 8, 0.7F);
    public static final Supplier<Item> SHA_ZHU_CAI = feastItem("slaughter_feast", () -> ModBlocks.SHA_ZHU_CAI_POT.get());
    public static final Supplier<Item> SHA_ZHU_CAI_BOWL = bowlFood("slaughter_feast_bowl", 12, 0.8F);
    public static final Supplier<Item> LA_ROU_DUN_DOU_JIAO = bowlFood("cured_pork_bean_stew", 8, 0.6F);
    public static final Supplier<Item> JIANG_NIU_ROU = food("braised_beef_sauce", 8, 0.7F);
    public static final Supplier<Item> LIU_ROU_DUAN = bowlFood("crispy_pork_strips", 9, 0.7F);
    public static final Supplier<Item> HONG_SHAO_PAI_GU = bowlFood("braised_pork_ribs", 9, 0.7F);
    public static final Supplier<Item> BA_SI_TU_DOU = bowlFood("candied_sweet_potato", 7, 0.5F);
    public static final Supplier<Item> SUAN_HUANG_GUA_CHAO_ROU_SI = bowlFood("pickled_cucumber_pork_stirfry", 7, 0.6F);
    public static final Supplier<Item> SWEET_POTATO_PORRIDGE = bowlFood("sweet_potato_porridge", 7, 0.6F);
    public static final Supplier<Item> ZHU_ROU_DUN_FEN_TIAO = feastItem("pork_vermicelli_stew", () -> ModBlocks.ZHU_ROU_DUN_FEN_TIAO_POT.get());
    public static final Supplier<Item> ZHU_ROU_DUN_FEN_TIAO_BOWL = bowlFood("pork_vermicelli_stew_bowl", 10, 0.7F);
    public static final Supplier<Item> SUAN_CAI_CHAO_FEN_TIAO = bowlFood("sauerkraut_fried_vermicelli", 6, 0.5F);
    public static final Supplier<Item> BUCKWHEAT_COLD_NOODLES = brassBowlFood("buckwheat_cold_noodles", 9, 0.6F);
    public static final Supplier<Item> DOG_BAO_PICKLES = brassBowlFood("dog_bao_pickles", 6, 0.6F);
    public static final Supplier<Item> LIANG_BAN_XIAN_CAI = bowlFood("pickled_veggie_salad", 6, 0.5F);
    public static final Supplier<Item> MING_TAI_YU_SI = bowlFood("shredded_pollack", 6, 0.5F);
    public static final Supplier<Item> ZHAN_JIANG_CAI = bowlFood("dip_veggie_platter", 7, 0.5F);
    public static final Supplier<Item> DA_FAN_BAO = food("northeast_rice_wrap", 10, 0.7F);
    public static final Supplier<Item> JIA_XIAN_HUANG_GUA_PAO_CAI = bowlFood("stuffed_cucumber_pickle", 6, 0.5F);
    public static final Supplier<Item> LIANG_BAN_HUA_CAI = bowlFood("peanut_potato_salad", 8, 0.5F);
    public static final Supplier<Item> DA_FENG_SHOU = feastItem("harvest_stew", () -> ModBlocks.DA_FENG_SHOU_POT.get());
    public static final Supplier<Item> DA_FENG_SHOU_BOWL = bowlFood("harvest_stew_bowl", 12, 0.8F);
    public static final Supplier<Item> BAI_CAI_DOU_FU_DUN_FEN_TIAO = bowlFood("cabbage_tofu_vermicelli_stew", 8, 0.6F);
    public static final Supplier<Item> LA_NIU_ROU_TANG_FAN = brassBowlFood("spicy_beef_soup", 10, 0.7F);
    public static final Supplier<Item> LA_BAI_CAI_CHAO_FAN = brassBowlFood("kimchi_fried_rice", 10, 0.7F);
    public static final Supplier<Item> TU_DOU_BING = bowlFood("potato_pancake", 7, 0.6F);
    public static final Supplier<Item> XIA_REN_ZHU_ROU_XIAN_SHUI_JIAO = food("shrimp_pork_dumpling", 10, 0.7F);
    public static final Supplier<Item> XIA_JIANG_CHAO_JI_DAN = bowlFood("shrimp_paste_scrambled_egg", 7, 0.6F);
    public static final Supplier<Item> XIA_JIANG_DUN_DOU_FU = bowlFood("shrimp_paste_tofu_stew", 7, 0.6F);
    public static final Supplier<Item> HAI_XIAN_DOU_FU_TANG = bowlFood("seafood_tofu_soup", 8, 0.6F);
    public static final Supplier<Item> SNOW_MUSTARD_TOFU_STEW = bowlFood("snow_mustard_tofu_stew", 8, 0.6F);
    public static final Supplier<Item> SAN_XIAN_XIAN_SHUI_JIAO = food("three_delicacy_dumpling", 10, 0.7F);
    public static final Supplier<Item> LA_BAI_CAI_TANG_FAN = brassBowlFood("kimchi_tofu_soup", 9, 0.6F);
    public static final Supplier<Item> YU_MI_LAO = bowlFood("corn_fritter", 7, 0.6F);
    public static final Supplier<Item> CHA_ZI_ZHOU = bowlFood("crushed_corn_porridge", 6, 0.5F);
    public static final Supplier<Item> NIAN_DOU_BAO = food("sticky_bean_bun", 7, 0.6F);
    public static final Supplier<Item> HONG_DOU_SONG_BING = food("red_bean_songpyeon", 3, 0.3F);
    public static final Supplier<Item> HUA_SHENG_SONG_BING = food("peanut_songpyeon", 3, 0.3F);
    /** 打糕：木臼里一盆米饭（4 碗）舂出 4 个，每个按一碗农夫乐事米饭算，饥饿值 6；饱和度系数照旧 0.5（2026-11-05 用户） */
    public static final Supplier<Item> DA_GAO = food("pounded_rice_cake", 6, 0.5F);
    /**
     * 花生糕（辽宁兴城特产）：熟花生 + 蜂蜜在木臼里舂 4 下出 2 块（2026-11-06 用户）。两样料的小点心，只管饱、不给效果
     * （用户：只有两种调料的点心给生命提升太夸张），所以不进 DishFlavors；无器具，摆展示乐事的小餐盘
     */
    public static final Supplier<Item> PEANUT_CAKE = food("peanut_cake", 4, 0.5F);
    /**
     * 唱片「大东北是我的家乡」——播放时听范围内的玩家能看到歌词。
     *
     * <p>1.20.5+（1.21.1 / 1.21.4）走 jukebox_song 注册表 + jukeboxPlayable 数据组件；
     * 1.20.x 没有这套东西，退回 RecordItem。Forge / NeoForge 都给 RecordItem 加了收
     * Supplier&lt;SoundEvent&gt; 的构造器，正好避开「音效注册表比物品注册晚」的坑；
     * 第四参是 tick 数（不是秒），116.3 秒 → 2326，与原版唱片机停曲时机一致。</p>
     */
    //? if >=1.20.5 {
    /*public static final ResourceKey<JukeboxSong> SONG_NORTHEAST = ResourceKey.create(
            Registries.JUKEBOX_SONG, DdIds.of(NortheastChinaDelight.MODID, "northeast"));
    public static final Supplier<Item> MUSIC_DISC_NORTHEAST = regItem("music_disc_northeast",
            Item::new, new Item.Properties().stacksTo(1)
                    .jukeboxPlayable(SONG_NORTHEAST));
    *///?} else {
    public static final Supplier<Item> MUSIC_DISC_NORTHEAST = regItem("music_disc_northeast",
            properties -> new RecordItem(15, ModSounds.MUSIC_DISC_NORTHEAST::get, properties, 2326),
            new Item.Properties().stacksTo(1));
    //?}
    public static final Supplier<Item> HAZELNUT_SUGAR_FIRE_BUN = food("hazelnut_sugar_fire_bun", 7, 0.6F);
    public static final Supplier<Item> XUN_JIANG_PIN_PAN = bowlFood("smoked_meat_platter", 10, 0.7F);
    public static final Supplier<Item> ORANGE_GUO_BAO_ROU = bowlFood("orange_crispy_pork", 9, 0.7F);
    public static final Supplier<Item> JIANG_BAN_LA_PI = bowlFood("mung_bean_sheet_salad", 7, 0.5F);
    public static final Supplier<Item> CONG_SHAO_HAI_SHEN = bowlFood("braised_sea_cucumber_scallion", 8, 0.7F);
    public static final Supplier<Item> HAI_SHEN_DOU_FU_TANG = bowlFood("sea_cucumber_tofu_soup", 7, 0.6F);
    public static final Supplier<Item> XIAN_YU_BING_ZI = bowlFood("salted_fish_flatbread", 8, 0.6F);
    public static final Supplier<Item> DRY_FRIED_HAIRTAIL = bowlFood("dry_fried_hairtail", 8, 0.6F);
    public static final Supplier<Item> BRAISED_HAIRTAIL = bowlFood("braised_hairtail", 8, 0.6F);
    public static final Supplier<Item> DA_JIANG_TANG = brassBowlFood("soy_paste_soup", 6, 0.5F);
    /**
     * 盆装得莫利炖鱼（2026-11-06 从碗装菜改成盆装菜）。名字同别的盆装菜：盆 = 菜名、碗 = 菜名_bowl
     * （用户：这道菜的用量就是大盆菜的，名字要统一）；原来那碗占的 demoli_fish_stew 给了盆，老存档里的碗读出来是一盆
     */
    public static final Supplier<Item> DE_MO_LI_DUN_YU = feastItem("demoli_fish_stew", () -> ModBlocks.DE_MO_LI_DUN_YU_POT.get());
    public static final Supplier<Item> DE_MO_LI_DUN_YU_BOWL = bowlFood("demoli_fish_stew_bowl", 11, 0.8F);
    public static final Supplier<Item> SU_BO_TANG = bowlFood("borscht", 10, 0.7F);
    public static final Supplier<Item> SUAN_CAI_HAI_XIAN_GUO =
            feastItem("sauerkraut_seafood_stew", () -> ModBlocks.SUAN_CAI_HAI_XIAN_GUO_POT.get());
    public static final Supplier<Item> SUAN_CAI_HAI_XIAN_GUO_BOWL =
            bowlFood("sauerkraut_seafood_stew_bowl", 11, 0.8F);
    public static final Supplier<Item> SHEN_JI_TANG = brassBowlFood("ginseng_chicken_soup", 10, 0.7F);
    public static final Supplier<Item> XIAO_JI_DUN_MO_GU = feastItem("chicken_mushroom_stew", () -> ModBlocks.XIAO_JI_DUN_MO_GU_POT.get());
    public static final Supplier<Item> XIAO_JI_DUN_MO_GU_BOWL = bowlFood("chicken_mushroom_stew_bowl", 10, 0.7F);
    /** 盆装米饭：大锅里 4 份大米煮好用大脸盆盛，拿碗盛出来是农夫乐事的米饭 */
    public static final Supplier<Item> PEN_MI_FAN = feastItem("steamed_rice", () -> ModBlocks.PEN_MI_FAN_POT.get());
    /**
     * 盆装谜之炖菜：大锅里凑不上配方时煮出来的（2026-11-05），效果存在物品上（MysteryStewEffects），拿碗盛出来是一碗原版谜之炖菜；
     * 创造模式的提示框里列效果（effect/DishEffectEvents#onItemTooltip）
     */
    public static final Supplier<Item> MYSTERY_STEW = CrciItems.MYSTERY_STEW;
    public static final Supplier<Item> BIBIMBAP = brassBowlFood("bibimbap", 10, 0.7F);
    public static final Supplier<Item> KIMCHI_PANCAKE = bowlFood("kimchi_pancake", 8, 0.6F);
    public static final Supplier<Item> GRILLED_CHICKEN_FRAME = food("grilled_chicken_frame", 6, 0.6F);
    public static final Supplier<Item> SNOWY_BEAN_PASTE = bowlFood("snowy_bean_paste", 7, 0.6F);
    public static final Supplier<Item> CANDIED_PEANUTS = bowlFood("candied_peanuts", 5, 0.5F);
    public static final Supplier<Item> TIGER_SALAD = bowlFood("tiger_salad", 6, 0.5F);
    public static final Supplier<Item> GRILLED_CORN = food("grilled_corn", 5, 0.5F);
    public static final Supplier<Item> GRILLED_OIL_EDGE = food("grilled_oil_edge", 6, 0.6F);
    public static final Supplier<Item> GRILLED_COLD_NOODLES = food("grilled_cold_noodles", 7, 0.6F);

    public static final Supplier<Item> UNFIRED_VAT_BLANK = CrciItems.UNFIRED_VAT_BLANK;
    public static final Supplier<Item> VAT = CrciItems.VAT;
    public static final Supplier<Item> CREATIVE_VAT = CrciItems.CREATIVE_VAT;
    public static final Supplier<Item> WOODEN_MORTAR = CrciItems.WOODEN_MORTAR;
    public static final Supplier<Item> ANIMAL_OIL_BLOCK_ITEM = CrciItems.ANIMAL_OIL_BLOCK_ITEM;

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

    /** 青砖（材料，像原版红砖）：4 块合成青砖块 */
    public static final Supplier<Item> BLUE_BRICK = simple("blue_brick");
    /** 窗花：贴在糊纸木格窗上（见 {@link PaperCutItem}）；一手红纸、一手剪刀右键剪出来（见 {@link RedPaperItem}） */
    public static final Supplier<Item> PAPER_CUT = regItem("paper_cut", PaperCutItem::new, new Item.Properties());
    /** 彩钢瓦（材料，不能放）：染料 + 3 铁锭做，再用它合成平屋顶 / 斜屋顶 */
    public static final Supplier<Item> RED_STEEL_ROOFING = simple("red_steel_roofing");
    public static final Supplier<Item> BLUE_STEEL_ROOFING = simple("blue_steel_roofing");
    public static final Supplier<Item> GREEN_STEEL_ROOFING = simple("green_steel_roofing");
    /** 大铁锅和锅盖均由 CRCI 注册。 */
    public static final Supplier<Item> BIG_IRON_POT = CrciItems.BIG_IRON_POT;
    public static final Supplier<Item> BIG_POT_LID = CrciItems.BIG_POT_LID;
    /** 大花袄：穿在胸甲槽，护甲 1、耐久照皮革胸甲（材料见 {@link ModArmorMaterials}）；四件套效果见 {@link NortheastOutfit} */
    public static final Supplier<Item> FLORAL_JACKET = regItem("floral_jacket",
            properties -> new ArmorItem(ModArmorMaterials.FLORAL_JACKET, ArmorItem.Type.CHESTPLATE, properties),
            jacketProperties());
    /** 红纸：两张纸 + 一份红染料做，剪窗花、糊红灯笼用（贴图暂时借原版的纸） */
    public static final Supplier<Item> RED_PAPER = regItem("red_paper", RedPaperItem::new, new Item.Properties());

    /** 东北建材，放在「方块」物品栏最后 */
    public static final List<Supplier<? extends Item>> BUILDING_BLOCK_ITEMS = List.of(
            BLUE_BRICK,
            blockItem("blue_bricks", ModBlocks.BLUE_BRICKS),
            blockItem("blue_brick_stairs", ModBlocks.BLUE_BRICK_STAIRS),
            blockItem("blue_brick_slab", ModBlocks.BLUE_BRICK_SLAB),
            blockItem("blue_brick_wall", ModBlocks.BLUE_BRICK_WALL),
            blockItem("paper_lattice_window", ModBlocks.PAPER_LATTICE_WINDOW),
            blockItem("glass_lattice_window", ModBlocks.GLASS_LATTICE_WINDOW),
            blockItem("sorghum_stalk_fence", ModBlocks.SORGHUM_STALK_FENCE),
            blockItem("board_fence", ModBlocks.BOARD_FENCE),
            blockItem("sorghum_stalk_gate", ModBlocks.SORGHUM_STALK_GATE),
            blockItem("board_fence_gate", ModBlocks.BOARD_FENCE_GATE),
            blockItem("plain_door_curtain", ModBlocks.PLAIN_DOOR_CURTAIN),
            blockItem("cotton_door_curtain", ModBlocks.COTTON_DOOR_CURTAIN),
            blockItem("floral_door_curtain", ModBlocks.FLORAL_DOOR_CURTAIN),
            blockItem("army_green_door_curtain", ModBlocks.ARMY_GREEN_DOOR_CURTAIN),
            RED_PAPER,
            PAPER_CUT,
            RED_STEEL_ROOFING,
            blockItem("red_steel_roof_slab", ModBlocks.RED_STEEL_ROOF_SLAB),
            blockItem("red_steel_roof_stairs", ModBlocks.RED_STEEL_ROOF_STAIRS),
            BLUE_STEEL_ROOFING,
            blockItem("blue_steel_roof_slab", ModBlocks.BLUE_STEEL_ROOF_SLAB),
            blockItem("blue_steel_roof_stairs", ModBlocks.BLUE_STEEL_ROOF_STAIRS),
            GREEN_STEEL_ROOFING,
            blockItem("green_steel_roof_slab", ModBlocks.GREEN_STEEL_ROOF_SLAB),
            blockItem("green_steel_roof_stairs", ModBlocks.GREEN_STEEL_ROOF_STAIRS),
            blockItem("white_tiles", ModBlocks.WHITE_TILES),
            blockItem("blue_tiles", ModBlocks.BLUE_TILES),
            regItem("red_iron_gate", p -> new DoubleHighBlockItem(ModBlocks.RED_IRON_GATE.get(), p), new Item.Properties()),
            regItem("korean_paper_door", p -> new DoubleHighBlockItem(ModBlocks.KOREAN_PAPER_DOOR.get(), p), new Item.Properties()),
            blockItem("folded_quilt", ModBlocks.FOLDED_QUILT),
            blockItem("pillow", ModBlocks.PILLOW),
            blockItem("small_stool", ModBlocks.SMALL_STOOL),
            blockItem("hand_pump", ModBlocks.HAND_PUMP),
            blockItem("red_lantern", ModBlocks.RED_LANTERN),
            blockItem("kang_wall_flower", ModBlocks.KANG_WALL_FLOWER),
            blockItem("kang_wall_landscape", ModBlocks.KANG_WALL_LANDSCAPE),
            blockItem("kang_wall_fret", ModBlocks.KANG_WALL_FRET),
            blockItem("radiator", ModBlocks.RADIATOR)
    );

    public static final List<Supplier<? extends Item>> INGREDIENT_TAB_ITEMS = List.of(
            FLORAL_JACKET, MUSIC_DISC_NORTHEAST
    );

    private static Supplier<Item> blockItem(String id, Supplier<? extends Block> block) {
        return regItem(id, properties -> new BlockItem(block.get(), properties), new Item.Properties());
    }

    /**
     * 木槌的物品属性：不可堆叠 + 118 点耐久。
     * 1.21 的数据组件还能顺手把武器属性（攻击 5 / 攻速 1.0）也写进去，
     * 1.20.1 没有数据组件，那部分由 {@link WoodenMalletItem#getDefaultAttributeModifiers} 给。
     */
    /** 1.20.1 的耐久由盔甲材料给（ArmorItem 构造时自己填）；1.21.1 要写在物品属性里 */
    private static Item.Properties jacketProperties() {
        //? if <1.20.5 {
        return new Item.Properties();
        //?} else {
        /*return new Item.Properties().durability(ModArmorMaterials.CHEST_DURABILITY);
        *///?}
    }

    private static Item.Properties malletProperties() {
        //? if <1.20.5 {
        return new Item.Properties().stacksTo(1).durability(WoodenMalletItem.DURABILITY);
        //?} else {
        /*return new Item.Properties().stacksTo(1).durability(WoodenMalletItem.DURABILITY)
                .attributes(WoodenMalletItem.createAttributes());
        *///?}
    }

    /** 格子桌布的物品，和 ModBlocks.GINGHAM_TABLECLOTHS 一一对应 */
    public static final Map<DyeColor, Supplier<Item>> GINGHAM_TABLECLOTH_ITEMS = ginghamTableclothItems();

    private static Map<DyeColor, Supplier<Item>> ginghamTableclothItems() {
        Map<DyeColor, Supplier<Item>> map = new LinkedHashMap<>();
        for (var entry : ModBlocks.GINGHAM_TABLECLOTHS.entrySet()) {
            map.put(entry.getKey(), blockItem(entry.getKey().getName() + "_gingham_tablecloth", entry.getValue()));
        }
        return Collections.unmodifiableMap(map);
    }

    public static final List<Supplier<? extends Item>> BLOCK_TAB_ITEMS = blockTabItems();

    private static List<Supplier<? extends Item>> blockTabItems() {
        List<Supplier<? extends Item>> list = new ArrayList<>();
        list.add(CORN_SKEWER);
        list.add(CHILI_SKEWER);
        list.addAll(BUILDING_BLOCK_ITEMS);
        list.addAll(GINGHAM_TABLECLOTH_ITEMS.values());
        return List.copyOf(list);
    }

    /**
     * 「菜肴」物品栏的展示顺序：格瓦斯 → 盘装 → 无容器 → 普通碗装 → 黄铜碗装 → 盆装。
     * 无容器菜单独列出，不能因为借用餐盘模型就误归入碗装。
     */
    public static final List<Supplier<? extends Item>> DISH_TAB_ITEMS = List.of(
            CANNED_YELLOW_PEACH,
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
            BAKED_SWEET_POTATO, DA_FAN_BAO, NIAN_DOU_BAO, HONG_DOU_SONG_BING, HUA_SHENG_SONG_BING, HAZELNUT_SUGAR_FIRE_BUN, DA_GAO, PEANUT_CAKE,
            GRILLED_CHICKEN_FRAME, GRILLED_CORN, GRILLED_OIL_EDGE, GRILLED_COLD_NOODLES,
            SUAN_CAI_JIAO_ZI, XIA_REN_ZHU_ROU_XIAN_SHUI_JIAO, SAN_XIAN_XIAN_SHUI_JIAO,
            JIANG_DA_GU, JIANG_NIU_ROU,
            SOUR_TANGZI, CHA_ZI_ZHOU, SWEET_POTATO_PORRIDGE,
            EGG_SOY_PASTE, XIA_JIANG_DUN_DOU_FU, HAI_XIAN_DOU_FU_TANG, SNOW_MUSTARD_TOFU_STEW, TU_DOU_MEN_QIE_ZI,
            HAI_SHEN_DOU_FU_TANG, SU_BO_TANG,
            BAI_CAI_DOU_FU_DUN_FEN_TIAO,
            DI_GUO_JI_BOWL, DI_GUO_PAI_GU_BOWL, SHA_ZHU_CAI_BOWL,
            SUAN_CAI_DUN_GU_TOU_BOWL, ZHU_ROU_DUN_FEN_TIAO_BOWL,
            XIAO_JI_DUN_MO_GU_BOWL, SUAN_CAI_HAI_XIAN_GUO_BOWL, DA_FENG_SHOU_BOWL, DE_MO_LI_DUN_YU_BOWL,
            LA_NIU_ROU_TANG_FAN, LA_BAI_CAI_CHAO_FAN, LA_BAI_CAI_TANG_FAN,
            DA_JIANG_TANG, SHEN_JI_TANG, BUCKWHEAT_COLD_NOODLES, DOG_BAO_PICKLES, BIBIMBAP,
            DI_GUO_JI, DI_GUO_PAI_GU, SHA_ZHU_CAI, DA_FENG_SHOU,
            SUAN_CAI_DUN_GU_TOU, ZHU_ROU_DUN_FEN_TIAO,
            XIAO_JI_DUN_MO_GU, SUAN_CAI_HAI_XIAN_GUO, DE_MO_LI_DUN_YU, PEN_MI_FAN
    );

    /** Items that intentionally stay out of the creative tab. */
    public static final List<Supplier<? extends Item>> HIDDEN_CREATIVE_ITEMS =
            List.of(SEA_WATER_BUCKET, MYSTERY_STEW);

    /** One creative tab for the whole regional mod. */
    public static final List<Supplier<? extends Item>> CREATIVE_TAB_ITEMS = buildCreativeTab();

    private static List<Supplier<? extends Item>> buildCreativeTab() {
        List<Supplier<? extends Item>> items = new ArrayList<>();
        items.addAll(INGREDIENT_TAB_ITEMS);
        items.addAll(DISH_TAB_ITEMS);
        items.addAll(BLOCK_TAB_ITEMS);
        items.removeAll(HIDDEN_CREATIVE_ITEMS);
        return List.copyOf(items);
    }

    private static <I extends Item> Supplier<I> regItem(String id,
                                                        Function<Item.Properties, ? extends I> factory,
                                                        Item.Properties properties) {
        //? if <1.20.2 {
        return ITEMS.register(id, () -> factory.apply(properties));
        //?} else {
        /*return ITEMS.registerItem(id, factory, properties);
        *///?}
    }

    private static Supplier<Item> simple(String id) {
        return regItem(id, Item::new, new Item.Properties());
    }









    private static Supplier<Item> food(String id) {
        return regItem(id, Item::new, new Item.Properties().food(steakFood().build()));
    }

    private static Supplier<Item> food(String id, int nutrition, float saturation) {
        return regItem(id, Item::new,
                new Item.Properties().food(stats(nutrition, saturation).build()));
    }

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
     * 大盆菜的方块物品：放下就是一大盆（见 {@code DdFeastBlock}），右键取一份得「碗装XX」。
     * 一盆就是一份菜，所以不能堆叠（最大堆叠 1）。
     * 物品栏是 2D 贴图、手 / 地面是方块模型，分流在客户端渲染器（{@code client/DdFeastItemRenderer}）。
     */
    private static Supplier<Item> feastItem(String id, Supplier<? extends Block> block) {
        return regItem(id, properties -> new DdFeastItem(block.get(), properties),
                new Item.Properties().stacksTo(1));
    }

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

    private static FoodProperties foodReturning(FoodProperties.Builder builder, ItemLike remainder) {
        //? if <1.20.5 {
        return builder.build();
        //?} else {
        /*return builder.usingConvertsTo(remainder).build();
        *///?}
    }

    private static FoodProperties.Builder withSaturation(FoodProperties.Builder builder, float value) {
        //? if <1.20.5 {
        return builder.saturationMod(value);
        //?} else {
        /*return builder.saturationModifier(value);
        *///?}
    }

    /** 不饿也能吃 / 喝（1.20.1 叫 alwaysEat） */
    private static FoodProperties.Builder alwaysEdible(FoodProperties.Builder builder) {
        //? if <1.20.5 {
        return builder.alwaysEat();
        //?} else {
        /*return builder.alwaysEdible();
        *///?}
    }

    private static FoodProperties.Builder steakFood() {
        return withSaturation(new FoodProperties.Builder().nutrition(8), 0.8F);
    }

    private static FoodProperties.Builder stats(int nutrition, float saturation) {
        return withSaturation(new FoodProperties.Builder().nutrition(nutrition), saturation);
    }

    /**
     * 按「实际饱和度点数」构造食物属性。原版实际饱和度 = 饥饿值 × 饱和度系数 × 2，
     * 所以 7 点饥饿值、3 点实际饱和度对应系数 3/14。
     */
    private static FoodProperties.Builder statsWithSaturationPoints(int nutrition, float saturationPoints) {
        return stats(nutrition, saturationPoints / (nutrition * 2.0F));
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
