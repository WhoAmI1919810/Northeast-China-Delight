package com.gunmu.northeast_china_delight.item;

import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
//? if <1.20.5 {
/*import net.minecraft.world.item.RecordItem;
*///?} else {
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;
//?}
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.ItemLike;
//? if <1.20.2 {
/*import net.minecraftforge.eventbus.api.IEventBus;
*///?} else {
import net.neoforged.bus.api.IEventBus;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.registries.DeferredRegister;
*///?} else {
import net.neoforged.neoforge.registries.DeferredRegister;
//?}

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
    /*public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.ITEMS, NortheastChinaDelight.MODID);
    *///?} else {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NortheastChinaDelight.MODID);
    //?}

    /** 油炸时一次烧完一整瓶的油：植物油、动物油（厨锅按这个标签判断） */
    public static final TagKey<Item> DEEP_FRY_OILS = TagKey.create(Registries.ITEM,
            DdIds.of(NortheastChinaDelight.MODID, "deep_fry_oils"));
    public static final TagKey<Item> DEEP_FRIED_DISHES = TagKey.create(Registries.ITEM,
            DdIds.of(NortheastChinaDelight.MODID, "deep_fried_dishes"));

    public static final Supplier<Item> SOYBEAN = selfSeeding("soybean", () -> ModBlocks.SOYBEAN_CROP.get());
    public static final Supplier<Item> EGGPLANT = food("eggplant", 2, 0.3F);
    public static final Supplier<Item> GREEN_PEPPER = food("green_pepper", 2, 0.3F);
    public static final Supplier<Item> CORN = food("corn", 3, 0.4F);
    public static final Supplier<Item> GREEN_BEANS = food("green_beans", 1, 0.3F);
    public static final Supplier<Item> BUCKWHEAT = selfSeeding("buckwheat", () -> ModBlocks.BUCKWHEAT_CROP.get());
    public static final Supplier<Item> NAPA_CABBAGE = food("napa_cabbage", 2, 0.4F);
    public static final Supplier<Item> SNOW_MUSTARD = food("snow_mustard", 2, 0.4F);
    public static final Supplier<Item> CUCUMBER = food("cucumber", 2, 0.4F);
    public static final Supplier<Item> SWEET_POTATO = selfSeedingFood("sweet_potato", () -> ModBlocks.SWEET_POTATO_CROP.get());
    public static final Supplier<Item> PEANUT = selfSeedingFood("peanut", () -> ModBlocks.PEANUT_CROP.get());
    public static final Supplier<Item> RED_BEAN = selfSeeding("red_bean", () -> ModBlocks.RED_BEAN_CROP.get());
    public static final Supplier<Item> RED_CHILI = food("red_chili", 1, 0.3F);
    public static final Supplier<Item> GREEN_ONION = food("green_onion", 1, 0.3F);
    public static final Supplier<Item> GREEN_RADISH = food("green_radish", 2, 0.4F);
    public static final Supplier<Item> BELLFLOWER_ROOT = food("bellflower_root", 2, 0.3F);
    public static final Supplier<Item> SHRIMP = food("shrimp", 2, 0.1F);
    public static final Supplier<Item> SPICY_CABBAGE = food("spicy_cabbage", 4, 0.5F);

    public static final Supplier<Item> EGGPLANT_SEEDS = seeds("eggplant_seeds", () -> ModBlocks.EGGPLANT_CROP.get());
    public static final Supplier<Item> GREEN_PEPPER_SEEDS = seeds("green_pepper_seeds", () -> ModBlocks.GREEN_PEPPER_CROP.get());
    public static final Supplier<Item> CORN_SEEDS = seeds("corn_seeds", () -> ModBlocks.CORN_CROP.get());
    public static final Supplier<Item> GREEN_BEANS_SEEDS = seeds("green_beans_seeds", () -> ModBlocks.GREEN_BEANS_CROP.get());
    public static final Supplier<Item> NAPA_CABBAGE_SEEDS = seeds("napa_cabbage_seeds", () -> ModBlocks.NAPA_CABBAGE_CROP.get());
    public static final Supplier<Item> SNOW_MUSTARD_SEEDS = seeds("snow_mustard_seeds", () -> ModBlocks.SNOW_MUSTARD_CROP.get());
    public static final Supplier<Item> CUCUMBER_SEEDS = seeds("cucumber_seeds", () -> ModBlocks.CUCUMBER_CROP.get());
    public static final Supplier<Item> RED_CHILI_SEEDS = seeds("red_chili_seeds", () -> ModBlocks.RED_CHILI_CROP.get());
    public static final Supplier<Item> GREEN_ONION_SEEDS = seeds("green_onion_seeds", () -> ModBlocks.GREEN_ONION_CROP.get());
    public static final Supplier<Item> GREEN_RADISH_SEEDS = seeds("green_radish_seeds", () -> ModBlocks.GREEN_RADISH_CROP.get());
    public static final Supplier<Item> GINSENG_SEEDS = seeds("ginseng_seeds", () -> ModBlocks.GINSENG_CROP.get());
    public static final Supplier<Item> BELLFLOWER_SEEDS = regItem(
            "bellflower_seeds",
            properties -> new BellflowerSeedItem(ModBlocks.BELLFLOWER_CROP.get(), properties),
            new Item.Properties());

    public static final Supplier<Item> SOUR_CABBAGE = food("sour_cabbage", 3, 0.4F);
    public static final Supplier<Item> PICKLED_SNOW_MUSTARD = food("pickled_snow_mustard", 3, 0.4F);
    public static final Supplier<Item> SOY_PASTE = plain("soy_paste");
    public static final Supplier<Item> SOY_PASTE_CHUNK = simple("soy_paste_chunk");
    public static final Supplier<Item> SOY_RESIDUE = simple("soy_residue");
    public static final Supplier<Item> CORN_STALK = simple("corn_stalk");
    public static final Supplier<Item> SALT = simple("salt");
    public static final Supplier<Item> SEA_WATER_BUCKET = regItem(
            "sea_water_bucket", SeaWaterBucketItem::new,
            new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET));
    public static final Supplier<Item> SALTED_PORK = food("salted_pork", 5, 0.5F);
    public static final Supplier<Item> SALTED_FISH = food("salted_fish", 4, 0.4F);
    public static final Supplier<Item> PICKLED_CUCUMBER = food("pickled_cucumber", 3, 0.4F);
    public static final Supplier<Item> PICKLED_CARROT = food("pickled_carrot", 3, 0.4F);
    public static final Supplier<Item> PICKLED_GREEN_RADISH = food("pickled_green_radish", 3, 0.4F);
    public static final Supplier<Item> WATER_DOUGH = simple("sour_dough");
    public static final Supplier<Item> BUCKWHEAT_NOODLES = plain("buckwheat_noodles");
    public static final Supplier<Item> DRIED_TOFU = food("dried_tofu", 3, 0.4F);
    public static final Supplier<Item> TOFU = food("tofu", 3, 0.4F);
    public static final Supplier<Item> FROZEN_TOFU = food("frozen_tofu", 3, 0.4F);
    public static final Supplier<Item> COOKING_OIL = bottled("cooking_oil");
    public static final Supplier<Item> CHILI_OIL = bottled("chili_oil");
    public static final Supplier<Item> ANIMAL_OIL = bottled("animal_oil");
    public static final Supplier<Item> KVASS = bottledDrink("kvass", 2, 0.25F);
    /**
     * 黄桃罐头：7 点饥饿值、实际 3 点饱和度（原版公式为 nutrition × modifier × 2，
     * 因此这里换算成 3/14 的饱和度系数）；吃完返还玻璃瓶。
     * 物品栏贴图与手持 / 掉落模型的分流见 {@code client/CannedYellowPeachItemRenderer}。
     */
    public static final Supplier<Item> CANNED_YELLOW_PEACH = regItem("canned_yellow_peach",
            properties -> new CannedYellowPeachItem(ModBlocks.CANNED_YELLOW_PEACH.get(), properties),
            new Item.Properties()
                    .craftRemainder(Items.GLASS_BOTTLE)
                    .food(foodReturning(statsWithSaturationPoints(7, 3.0F), Items.GLASS_BOTTLE)));
    public static final Supplier<Item> PEANUT_BUTTER = bottled("peanut_butter");
    public static final Supplier<Item> ROASTED_PEANUTS = food("roasted_peanuts", 4, 0.5F);
    public static final Supplier<Item> CHILI_SAUCE = seasoningBowl("chili_sauce");
    public static final Supplier<Item> BEAN_SPROUTS = food("bean_sprouts", 2, 0.3F);
    public static final Supplier<Item> SOUR_CORN_KERNELS = food("sour_corn_kernels", 3, 0.4F);
    public static final Supplier<Item> SWEET_POTATO_STARCH = simple("sweet_potato_starch");
    public static final Supplier<Item> VERMICELLI = plain("vermicelli");
    public static final Supplier<Item> CORN_FLOUR = plain("corn_flour");
    public static final Supplier<Item> LA_PI = plain("mung_bean_sheet");
    public static final Supplier<Item> CRACKLINGS = food("cracklings", 4, 0.6F);
    public static final Supplier<Item> COLD_NOODLE_SHEET = food("cold_noodle_sheet", 2, 0.3F);
    public static final Supplier<Item> BAKED_SWEET_POTATO = food("baked_sweet_potato", 5, 0.6F);
    public static final Supplier<Item> SOUL_CABBAGE = food("spirit_cabbage", 4, 0.5F);

    /**
     * 大脸盆：空盆能放在地上（方块见 {@link ModBlocks#LARGE_BASIN}），也能扣头上（见 {@link LargeBasinItem}）。
     * 物品图标仍旧是 2D 贴图 —— 2D/3D 的分流放在客户端渲染器里（{@code client/LargeBasinItemRenderer}）。
     */
    public static final Supplier<Item> LARGE_BASIN = regItem("large_basin",
            properties -> new LargeBasinItem(ModBlocks.LARGE_BASIN.get(), properties),
            new Item.Properties());
    public static final Supplier<Item> GRILL_RACK = simple("grill_rack");
    public static final Supplier<Item> BRASS_BOWL = simple("brass_bowl");

    /**
     * 木槌：木臼「舂捣」配方里的工具，本身也是一把钝器（耐久 118、攻击 5 / 攻速 1.0）。
     * 物品栏是 2D 贴图、拿在手里是 3D 模型，分流在 {@code client/WoodenMalletItemRenderer}。
     */
    public static final Supplier<Item> WOODEN_MALLET = regItem(
            "wooden_mallet",
            WoodenMalletItem::new,
            malletProperties()
    );

    public static final Supplier<Item> HAZELNUT = selfSeedingFood("hazelnut", () -> ModBlocks.HAZELNUT_BUSH.get());
    public static final Supplier<Item> HAZEL_MUSHROOM = selfSeeding("hazel_mushroom", () -> ModBlocks.HAZEL_MUSHROOM.get());
    public static final Supplier<Item> HAZEL_MUSHROOM_COLONY = regItem(
            "hazel_mushroom_colony",
            properties -> new HazelMushroomColonyItem(ModBlocks.HAZEL_MUSHROOM_COLONY.get(), properties),
            new Item.Properties());
    public static final Supplier<Item> WOOD_EAR = plain("wood_ear");
    public static final Supplier<Item> GINSENG = plain("ginseng");
    public static final Supplier<Item> FROZEN_PEAR = food("frozen_pear", 4, 0.4F);
    public static final Supplier<Item> HAIRTAIL = food("hairtail", 2, 0.1F);
    public static final Supplier<Item> OYSTER = food("oyster", 2, 0.2F);
    public static final Supplier<Item> SEA_CUCUMBER = food("sea_cucumber", 2, 0.2F);
    public static final Supplier<Item> CHICKEN_FRAME = food("chicken_frame", 2, 0.3F);

    public static final Supplier<Item> PORK_RIBS = food("pork_ribs", 3, 0.3F);
    public static final Supplier<Item> PORK_INTESTINE = food("pork_intestine", 2, 0.2F);
    public static final Supplier<Item> PORK_HOCK = food("pork_hock", 3, 0.3F);
    public static final Supplier<Item> PIG_BLOOD = food("pig_blood", 1, 0.1F);
    public static final Supplier<Item> PIG_LIVER = food("pig_liver", 2, 0.2F);
    public static final Supplier<Item> PORK_FAT = food("pork_fat", 2, 0.4F);
    public static final Supplier<Item> OIL_EDGE = food("oil_edge", 2, 0.3F);
    public static final Supplier<Item> RAW_SAUSAGE = food("raw_sausage", 3, 0.3F);
    public static final Supplier<Item> BLOOD_SAUSAGE = food("blood_sausage", 4, 0.5F);
    public static final Supplier<Item> RICE_SAUSAGE = food("rice_sausage", 4, 0.5F);
    public static final Supplier<Item> RED_SAUSAGE = food("red_sausage", 5, 0.6F);
    public static final Supplier<Item> SMOKED_PORK_HOCK = food("smoked_pork_hock", 6, 0.7F);
    public static final Supplier<Item> SMOKED_PIG_LIVER = food("smoked_pig_liver", 5, 0.5F);
    public static final Supplier<Item> SOY_SAUCE = bottled("soy_sauce");
    public static final Supplier<Item> BRAISED_PORK_HOCK = bowlFood("braised_pork_hock", 9, 0.8F);
    public static final Supplier<Item> BRAISED_PORK_STRIPS = bowlFood("braised_pork_strips", 8, 0.7F);
    public static final Supplier<Item> VINEGAR = bottled("vinegar");
    public static final Supplier<Item> SOUR_WATER = bottledLiquid("sour_water");
    public static final Supplier<Item> WHITE_VINEGAR = bottled("white_vinegar");
    public static final Supplier<Item> FISH_SAUCE = bottled("fish_sauce");
    public static final Supplier<Item> SHRIMP_PASTE = bottled("shrimp_paste");

    public static final Supplier<Item> SOY_MILK = bowlFood("soy_milk", 4, 0.4F);
    public static final Supplier<Item> OLD_STYLE_GUO_BAO_ROU =
            dishBowl("old_style_crispy_pork", () -> ModBlocks.OLD_STYLE_GUO_BAO_ROU.get(), 9, 0.7F);
    public static final Supplier<Item> NEW_STYLE_GUO_BAO_ROU = bowlFood("new_style_crispy_pork", 9, 0.7F);
    public static final Supplier<Item> DI_SAN_XIAN = bowlFood("three_fresh_veggies", 8, 0.6F);
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
    public static final Supplier<Item> LIANG_BAN_HUA_CAI = bowlFood("mixed_vegetable_salad", 8, 0.5F);
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
    public static final Supplier<Item> DA_GAO = food("pounded_rice_cake", 3, 0.5F);

    /**
     * 唱片「大东北是我的家乡」——播放时听范围内的玩家能看到歌词。
     *
     * <p>1.20.5+（1.21.1 / 1.21.4）走 jukebox_song 注册表 + jukeboxPlayable 数据组件；
     * 1.20.x 没有这套东西，退回 RecordItem。Forge / NeoForge 都给 RecordItem 加了收
     * Supplier&lt;SoundEvent&gt; 的构造器，正好避开「音效注册表比物品注册晚」的坑；
     * 第四参是 tick 数（不是秒），116.3 秒 → 2326，与原版唱片机停曲时机一致。</p>
     */
    //? if >=1.20.5 {
    public static final ResourceKey<JukeboxSong> SONG_NORTHEAST = ResourceKey.create(
            Registries.JUKEBOX_SONG, DdIds.of(NortheastChinaDelight.MODID, "northeast"));
    public static final Supplier<Item> MUSIC_DISC_NORTHEAST = regItem("music_disc_northeast",
            Item::new, new Item.Properties().stacksTo(1)
                    .jukeboxPlayable(SONG_NORTHEAST));
    //?} else {
    /*public static final Supplier<Item> MUSIC_DISC_NORTHEAST = regItem("music_disc_northeast",
            properties -> new RecordItem(15, ModSounds.MUSIC_DISC_NORTHEAST::get, properties, 2326),
            new Item.Properties().stacksTo(1));
    *///?}
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
    public static final Supplier<Item> DE_MO_LI_DUN_YU = bowlFood("demoli_fish_stew", 11, 0.8F);
    public static final Supplier<Item> SU_BO_TANG = bowlFood("borscht", 10, 0.7F);
    public static final Supplier<Item> SUAN_CAI_HAI_XIAN_GUO =
            feastItem("sauerkraut_seafood_stew", () -> ModBlocks.SUAN_CAI_HAI_XIAN_GUO_POT.get());
    public static final Supplier<Item> SUAN_CAI_HAI_XIAN_GUO_BOWL =
            bowlFood("sauerkraut_seafood_stew_bowl", 11, 0.8F);
    public static final Supplier<Item> SHEN_JI_TANG = brassBowlFood("ginseng_chicken_soup", 10, 0.7F);
    public static final Supplier<Item> XIAO_JI_DUN_MO_GU = feastItem("chicken_mushroom_stew", () -> ModBlocks.XIAO_JI_DUN_MO_GU_POT.get());
    public static final Supplier<Item> XIAO_JI_DUN_MO_GU_BOWL = bowlFood("chicken_mushroom_stew_bowl", 10, 0.7F);
    public static final Supplier<Item> BIBIMBAP = brassBowlFood("bibimbap", 10, 0.7F);
    public static final Supplier<Item> KIMCHI_PANCAKE = bowlFood("kimchi_pancake", 8, 0.6F);
    public static final Supplier<Item> GRILLED_CHICKEN_FRAME = food("grilled_chicken_frame", 6, 0.6F);
    public static final Supplier<Item> SNOWY_BEAN_PASTE = bowlFood("snowy_bean_paste", 7, 0.6F);
    public static final Supplier<Item> CANDIED_PEANUTS = bowlFood("candied_peanuts", 5, 0.5F);
    public static final Supplier<Item> TIGER_SALAD = bowlFood("tiger_salad", 6, 0.5F);
    public static final Supplier<Item> GRILLED_CORN = food("grilled_corn", 5, 0.5F);
    public static final Supplier<Item> GRILLED_OIL_EDGE = food("grilled_oil_edge", 6, 0.6F);
    public static final Supplier<Item> GRILLED_COLD_NOODLES = food("grilled_cold_noodles", 7, 0.6F);

    public static final Supplier<Item> UNFIRED_VAT_BLANK = regItem(
            "unfired_vat_blank",
            (properties) -> new DisplayOnlyBlockItem(ModBlocks.UNFIRED_VAT_BLANK.get(),
                    "message.northeast_china_delight.unfired_vat_blank.place_hint", properties),
            new Item.Properties().stacksTo(1)
    );

    public static final Supplier<Item> VAT = regItem(
            "vat",
            (properties) -> new BlockItem(ModBlocks.VAT.get(), properties),
            new Item.Properties().stacksTo(1)
    );

    public static final Supplier<Item> CREATIVE_VAT = regItem(
            "creative_vat",
            (properties) -> new BlockItem(ModBlocks.CREATIVE_VAT.get(), properties),
            new Item.Properties().stacksTo(1)
    );

    public static final Supplier<Item> WOODEN_MORTAR = regItem(
            "wooden_mortar",
            (properties) -> new BlockItem(ModBlocks.WOODEN_MORTAR.get(), properties),
            new Item.Properties().stacksTo(16)
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

    public static final List<Supplier<? extends Item>> INGREDIENT_TAB_ITEMS = List.of(
            EGGPLANT_SEEDS, GREEN_PEPPER_SEEDS, CORN_SEEDS, GREEN_BEANS_SEEDS,
            NAPA_CABBAGE_SEEDS, SNOW_MUSTARD_SEEDS, CUCUMBER_SEEDS, RED_CHILI_SEEDS, GREEN_ONION_SEEDS, GREEN_RADISH_SEEDS,
            GINSENG_SEEDS, BELLFLOWER_SEEDS,
            NAPA_CABBAGE, SNOW_MUSTARD, CUCUMBER, EGGPLANT, GREEN_PEPPER, GREEN_ONION, RED_CHILI,
            GREEN_BEANS, CORN, SWEET_POTATO, GREEN_RADISH, BELLFLOWER_ROOT,
            SOYBEAN, PEANUT, RED_BEAN, BUCKWHEAT,
            HAZELNUT, HAZEL_MUSHROOM, HAZEL_MUSHROOM_COLONY, WOOD_EAR, GINSENG,
            FROZEN_PEAR,
            PORK_RIBS, PORK_INTESTINE, PORK_HOCK, PIG_BLOOD, PIG_LIVER, PORK_FAT, OIL_EDGE, CHICKEN_FRAME,
            SHRIMP, HAIRTAIL, OYSTER, SEA_CUCUMBER,
            RAW_SAUSAGE, BLOOD_SAUSAGE, RICE_SAUSAGE, RED_SAUSAGE,
            SMOKED_PORK_HOCK, SMOKED_PIG_LIVER,
            SOUR_CABBAGE, PICKLED_SNOW_MUSTARD, SPICY_CABBAGE, PICKLED_CUCUMBER, PICKLED_CARROT, PICKLED_GREEN_RADISH,
            SALTED_PORK, SALTED_FISH,
            SALT, SEA_WATER_BUCKET, SOY_PASTE, CHILI_SAUCE, SOY_SAUCE, VINEGAR, SOUR_WATER, WHITE_VINEGAR,
            FISH_SAUCE, SHRIMP_PASTE, CHILI_OIL, PEANUT_BUTTER, COOKING_OIL, ANIMAL_OIL,
            SOY_PASTE_CHUNK, SOY_RESIDUE,
            WATER_DOUGH, BUCKWHEAT_NOODLES, DRIED_TOFU, TOFU, FROZEN_TOFU, SWEET_POTATO_STARCH,
            VERMICELLI, LA_PI, CORN_FLOUR, CRACKLINGS, COLD_NOODLE_SHEET, BEAN_SPROUTS, SOUR_CORN_KERNELS, ROASTED_PEANUTS,
            CORN_STALK,
            LARGE_BASIN, BRASS_BOWL, WOODEN_MALLET, ANIMAL_OIL_BLOCK_ITEM,
            UNFIRED_VAT_BLANK, MUSIC_DISC_NORTHEAST
    );

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

    /**
     * 木槌的物品属性：不可堆叠 + 118 点耐久。
     * 1.21 的数据组件还能顺手把武器属性（攻击 5 / 攻速 1.0）也写进去，
     * 1.20.1 没有数据组件，那部分由 {@link WoodenMalletItem#getDefaultAttributeModifiers} 给。
     */
    private static Item.Properties malletProperties() {
        //? if <1.20.5 {
        /*return new Item.Properties().stacksTo(1).durability(WoodenMalletItem.DURABILITY);
        *///?} else {
        return new Item.Properties().stacksTo(1).durability(WoodenMalletItem.DURABILITY)
                .attributes(WoodenMalletItem.createAttributes());
        //?}
    }

    public static final List<Supplier<? extends Item>> BLOCK_TAB_ITEMS = blockTabItems();

    private static List<Supplier<? extends Item>> blockTabItems() {
        List<Supplier<? extends Item>> list = new ArrayList<>();
        list.add(VAT);
        list.add(WOODEN_MORTAR);
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
            KVASS, CANNED_YELLOW_PEACH,
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
            BAKED_SWEET_POTATO, DA_FAN_BAO, NIAN_DOU_BAO, HONG_DOU_SONG_BING, HUA_SHENG_SONG_BING, HAZELNUT_SUGAR_FIRE_BUN, DA_GAO,
            GRILLED_CHICKEN_FRAME, GRILLED_CORN, GRILLED_OIL_EDGE, GRILLED_COLD_NOODLES,
            SUAN_CAI_JIAO_ZI, XIA_REN_ZHU_ROU_XIAN_SHUI_JIAO, SAN_XIAN_XIAN_SHUI_JIAO,
            JIANG_DA_GU, JIANG_NIU_ROU,
            SOY_MILK, SOUR_TANGZI, CHA_ZI_ZHOU, SWEET_POTATO_PORRIDGE,
            EGG_SOY_PASTE, XIA_JIANG_DUN_DOU_FU, HAI_XIAN_DOU_FU_TANG, SNOW_MUSTARD_TOFU_STEW,
            HAI_SHEN_DOU_FU_TANG, DE_MO_LI_DUN_YU, SU_BO_TANG,
            BAI_CAI_DOU_FU_DUN_FEN_TIAO,
            DI_GUO_JI_BOWL, DI_GUO_PAI_GU_BOWL, SHA_ZHU_CAI_BOWL,
            SUAN_CAI_DUN_GU_TOU_BOWL, ZHU_ROU_DUN_FEN_TIAO_BOWL,
            XIAO_JI_DUN_MO_GU_BOWL, SUAN_CAI_HAI_XIAN_GUO_BOWL, DA_FENG_SHOU_BOWL,
            LA_NIU_ROU_TANG_FAN, LA_BAI_CAI_CHAO_FAN, LA_BAI_CAI_TANG_FAN,
            DA_JIANG_TANG, SHEN_JI_TANG, BUCKWHEAT_COLD_NOODLES, DOG_BAO_PICKLES, BIBIMBAP,
            DI_GUO_JI, DI_GUO_PAI_GU, SHA_ZHU_CAI, DA_FENG_SHOU,
            SUAN_CAI_DUN_GU_TOU, ZHU_ROU_DUN_FEN_TIAO,
            XIAO_JI_DUN_MO_GU, SUAN_CAI_HAI_XIAN_GUO
    );

    private static <I extends Item> Supplier<I> regItem(String id,
                                                        Function<Item.Properties, ? extends I> factory,
                                                        Item.Properties properties) {
        //? if <1.20.2 {
        /*return ITEMS.register(id, () -> factory.apply(properties));
        *///?} else {
        return ITEMS.registerItem(id, factory, properties);
        //?}
    }

    private static Supplier<Item> simple(String id) {
        return regItem(id, Item::new, new Item.Properties());
    }

    private static Supplier<Item> bottled(String id) {
        return regItem(id, SeasoningBottleItem::new, new Item.Properties()
                .craftRemainder(Items.GLASS_BOTTLE)
                .durability(SeasoningBottleItem.CAPACITY_MB));
    }

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

    private static Supplier<Item> bottledDrink(String id) {
        return regItem(id, DdDrinkItem::new, new Item.Properties()
                .craftRemainder(Items.GLASS_BOTTLE)
                .food(foodReturning(steakFood(), Items.GLASS_BOTTLE)));
    }

    private static Supplier<Item> bottledDrink(String id, int nutrition, float saturation) {
        return regItem(id, DdDrinkItem::new, new Item.Properties()
                .craftRemainder(Items.GLASS_BOTTLE)
                .food(foodReturning(stats(nutrition, saturation), Items.GLASS_BOTTLE)));
    }

    private static Supplier<Item> bottledLiquid(String id) {
        return regItem(id, Item::new, new Item.Properties()
                .craftRemainder(Items.GLASS_BOTTLE));
    }

    private static Supplier<Item> seasoningBowl(String id) {
        return regItem(id, Item::new, new Item.Properties()
                .craftRemainder(Items.BOWL));
    }

    private static Supplier<Item> selfSeedingFood(String id, Supplier<? extends Block> crop) {
        return regItem(id,
                properties -> new ItemNameBlockItem(crop.get(), properties),
                new Item.Properties().food(steakFood().build()));
    }

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
        /*return builder.build();*/
        //?} else {
        return builder.usingConvertsTo(remainder).build();
        //?}
    }

    private static FoodProperties.Builder withSaturation(FoodProperties.Builder builder, float value) {
        //? if <1.20.5 {
        /*return builder.saturationMod(value);*/
        //?} else {
        return builder.saturationModifier(value);
        //?}
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
