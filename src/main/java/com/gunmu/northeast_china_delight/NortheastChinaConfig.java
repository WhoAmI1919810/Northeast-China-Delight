package com.gunmu.northeast_china_delight;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

//? if <1.20.2 {
import net.minecraftforge.common.ForgeConfigSpec;
//?} else {
/*import net.neoforged.neoforge.common.ModConfigSpec;
*///?}

/**
 * 模组配置。
 *
 * <p>控制菜肴摆放、世界生成和箱子战利品。</p>
 */
public final class NortheastChinaConfig
{
    public static final List<String> DEFAULT_VILLAGE_FARM_CROPS = List.of(
            "chinese_regional_culinary_ingredients:soybean",
            "northeast_china_delight:corn_seeds",
            "northeast_china_delight:napa_cabbage_seeds",
            "northeast_china_delight:snow_mustard_seeds",
            "northeast_china_delight:cucumber_seeds",
            "northeast_china_delight:green_onion_seeds",
            "northeast_china_delight:green_radish_seeds",
            "northeast_china_delight:eggplant_seeds",
            "northeast_china_delight:green_beans_seeds",
            "northeast_china_delight:buckwheat",
            "northeast_china_delight:red_bean",
            "northeast_china_delight:peanut",
            "northeast_china_delight:sweet_potato",
            "northeast_china_delight:green_pepper_seeds",
            "northeast_china_delight:red_chili_seeds",
            "northeast_china_delight:bellflower_seeds");

    public static final List<String> DEFAULT_CHEST_LOOT_ITEMS = List.of(
            "northeast_china_delight:animal_oil",
            "northeast_china_delight:bellflower_root",
            "northeast_china_delight:bellflower_seeds",
            "northeast_china_delight:bibimbap",
            "northeast_china_delight:blood_sausage",
            "northeast_china_delight:borscht",
            "northeast_china_delight:braised_eggplant_potato",
            "northeast_china_delight:braised_hairtail",
            "northeast_china_delight:braised_pork_hock",
            "northeast_china_delight:braised_pork_ribs",
            "northeast_china_delight:braised_pork_strips",
            "northeast_china_delight:braised_sea_cucumber_scallion",
            "northeast_china_delight:buckwheat",
            "northeast_china_delight:buckwheat_cold_noodles",
            "northeast_china_delight:buckwheat_sack",
            "northeast_china_delight:cabbage_tofu_vermicelli_stew",
            "northeast_china_delight:candied_peanuts",
            "northeast_china_delight:candied_sweet_potato",
            "northeast_china_delight:canned_yellow_peach",
            "northeast_china_delight:chicken_frame",
            "northeast_china_delight:chicken_mushroom_stew",
            "northeast_china_delight:chicken_mushroom_stew_bowl",
            "northeast_china_delight:chili_oil",
            "northeast_china_delight:chili_sauce",
            "northeast_china_delight:cold_noodle_sheet",
            "northeast_china_delight:cooking_oil",
            "northeast_china_delight:corn_flour",
            "northeast_china_delight:corn_fritter",
            "northeast_china_delight:corn_seeds",
            "northeast_china_delight:corn_seeds_sack",
            "northeast_china_delight:crispy_pork_strips",
            "northeast_china_delight:crushed_corn_porridge",
            "northeast_china_delight:cucumber_seeds",
            "northeast_china_delight:cured_pork_bean_stew",
            "northeast_china_delight:demoli_fish_stew",
            "northeast_china_delight:demoli_fish_stew_bowl",
            "northeast_china_delight:dip_veggie_platter",
            "northeast_china_delight:dog_bao_pickles",
            "northeast_china_delight:dried_tofu",
            "northeast_china_delight:dry_fried_hairtail",
            "northeast_china_delight:eggplant_seeds",
            "northeast_china_delight:fish_sauce",
            "northeast_china_delight:ginseng_chicken_soup",
            "northeast_china_delight:ginseng_seeds",
            "northeast_china_delight:green_beans_seeds",
            "northeast_china_delight:green_onion_seeds",
            "northeast_china_delight:green_pepper_seeds",
            "northeast_china_delight:green_radish_seeds",
            "northeast_china_delight:grilled_chicken_frame",
            "northeast_china_delight:grilled_oil_edge",
            "northeast_china_delight:ground_pot_chicken",
            "northeast_china_delight:ground_pot_chicken_bowl",
            "northeast_china_delight:ground_pot_ribs",
            "northeast_china_delight:ground_pot_ribs_bowl",
            "northeast_china_delight:hairtail",
            "northeast_china_delight:harvest_stew",
            "northeast_china_delight:harvest_stew_bowl",
            "northeast_china_delight:hazelnut_sugar_fire_bun",
            "northeast_china_delight:kimchi_fried_rice",
            "northeast_china_delight:kimchi_pancake",
            "northeast_china_delight:kimchi_tofu_soup",
            "northeast_china_delight:kvass",
            "northeast_china_delight:mung_bean_sheet",
            "northeast_china_delight:mung_bean_sheet_salad",
            "northeast_china_delight:music_disc_northeast",
            "northeast_china_delight:napa_cabbage",
            "northeast_china_delight:napa_cabbage_seeds",
            "northeast_china_delight:new_style_crispy_pork",
            "northeast_china_delight:oil_edge",
            "northeast_china_delight:old_style_crispy_pork",
            "northeast_china_delight:orange_crispy_pork",
            "northeast_china_delight:oyster",
            "northeast_china_delight:peanut",
            "northeast_china_delight:peanut_butter",
            "northeast_china_delight:peanut_cake",
            "northeast_china_delight:peanut_potato_salad",
            "northeast_china_delight:peanut_songpyeon",
            "northeast_china_delight:pepper_dried_tofu",
            "northeast_china_delight:pickled_cucumber_pork_stirfry",
            "northeast_china_delight:pickled_snow_mustard",
            "northeast_china_delight:pickled_veggie_salad",
            "northeast_china_delight:pig_blood",
            "northeast_china_delight:pig_liver",
            "northeast_china_delight:pork_hock",
            "northeast_china_delight:pork_intestine",
            "northeast_china_delight:pork_ribs",
            "northeast_china_delight:pork_vermicelli_stew",
            "northeast_china_delight:pork_vermicelli_stew_bowl",
            "northeast_china_delight:potato_pancake",
            "northeast_china_delight:pounded_rice_cake",
            "northeast_china_delight:red_bean",
            "northeast_china_delight:red_bean_sack",
            "northeast_china_delight:red_bean_songpyeon",
            "northeast_china_delight:red_chili_seeds",
            "northeast_china_delight:red_sausage",
            "northeast_china_delight:rice_sausage",
            "northeast_china_delight:salt",
            "northeast_china_delight:salted_fish",
            "northeast_china_delight:salted_fish_flatbread",
            "northeast_china_delight:sauerkraut_bone_stew",
            "northeast_china_delight:sauerkraut_bone_stew_bowl",
            "northeast_china_delight:sauerkraut_fried_vermicelli",
            "northeast_china_delight:sauerkraut_seafood_stew",
            "northeast_china_delight:sauerkraut_seafood_stew_bowl",
            "northeast_china_delight:sea_cucumber",
            "northeast_china_delight:sea_cucumber_tofu_soup",
            "northeast_china_delight:seafood_tofu_soup",
            "northeast_china_delight:shredded_pollack",
            "northeast_china_delight:shrimp",
            "northeast_china_delight:shrimp_paste",
            "northeast_china_delight:shrimp_paste_scrambled_egg",
            "northeast_china_delight:shrimp_paste_tofu_stew",
            "northeast_china_delight:slaughter_feast",
            "northeast_china_delight:slaughter_feast_bowl",
            "northeast_china_delight:smoked_meat_platter",
            "northeast_china_delight:snow_mustard",
            "northeast_china_delight:snow_mustard_seeds",
            "northeast_china_delight:snow_mustard_tofu_stew",
            "northeast_china_delight:snowy_bean_paste",
            "northeast_china_delight:sour_dough",
            "northeast_china_delight:sour_noodle_soup",
            "northeast_china_delight:soy_milk",
            "northeast_china_delight:soy_paste",
            "northeast_china_delight:soy_paste_soup",
            "northeast_china_delight:soy_sauce",
            "chinese_regional_culinary_ingredients:soybean",
            "northeast_china_delight:soybean_sack",
            "northeast_china_delight:spicy_beef_soup",
            "northeast_china_delight:steamed_egg_soy_paste",
            "northeast_china_delight:sticky_bean_bun",
            "northeast_china_delight:stuffed_cucumber_pickle",
            "northeast_china_delight:sweet_potato",
            "northeast_china_delight:sweet_potato_porridge",
            "northeast_china_delight:sweet_potato_starch",
            "northeast_china_delight:three_fresh_veggies",
            "northeast_china_delight:tiger_salad",
            "northeast_china_delight:vermicelli",
            "northeast_china_delight:vinegar",
            "northeast_china_delight:white_vinegar");

    //? if <1.20.2 {
    public static final ForgeConfigSpec SPEC;

    // 东北小院里的小零件（灶棚、鸡架、水井、菜窖等）是否生成
    public static final ForgeConfigSpec.BooleanValue YARD_PARTS_ENABLED;
    public static final ForgeConfigSpec.DoubleValue WILD_WOOD_EAR_OAK_CHANCE;
    public static final ForgeConfigSpec.DoubleValue WILD_WOOD_EAR_FOREST_CHANCE;
    public static final ForgeConfigSpec.BooleanValue VILLAGE_FARM_CROPS_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VILLAGE_FARM_ALLOWED_CROPS;
    public static final ForgeConfigSpec.BooleanValue CHEST_LOOT_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CHEST_LOOT_ALLOWED_ITEMS;
    //?} else {
    /*public static final ModConfigSpec SPEC;

    /^* 东北小院里的小零件（灶棚、鸡架、水井、菜窖等）是否生成 ^/
    public static final ModConfigSpec.BooleanValue YARD_PARTS_ENABLED;
    public static final ModConfigSpec.DoubleValue WILD_WOOD_EAR_OAK_CHANCE;
    public static final ModConfigSpec.DoubleValue WILD_WOOD_EAR_FOREST_CHANCE;
    public static final ModConfigSpec.BooleanValue VILLAGE_FARM_CROPS_ENABLED;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> VILLAGE_FARM_ALLOWED_CROPS;
    public static final ModConfigSpec.BooleanValue CHEST_LOOT_ENABLED;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CHEST_LOOT_ALLOWED_ITEMS;
    *///?}

    static
    {
        //? if <1.20.2 {
        ForgeConfigSpec.Builder
        //?} else {
        /*ModConfigSpec.Builder
        *///?}
        builder = configBuilder();
        builder.comment("世界生成").push("worldgen");
        YARD_PARTS_ENABLED = builder
                .comment("东北小院里的小建筑（灶棚、鸡架、水井、菜窖、柴垛等）是否生成。",
                        "设为 false 时只生成院子主体，不生成零件。")
                .define("yard_parts_enabled", false);
        WILD_WOOD_EAR_OAK_CHANCE = builder
                .comment("野生木耳在任意群系橡木上的生成概率，取值 0~1；0 表示不生成。",
                        "只影响新生成区块；已生成区块不会回溯。")
                .defineInRange("wild_wood_ear_oak_chance", 0.05D, 0.0D, 1.0D);
        WILD_WOOD_EAR_FOREST_CHANCE = builder
                .comment("野生木耳在黑森林深色橡木、白桦林 / 原始桦木林白桦上的生成概率，取值 0~1；0 表示不生成。",
                        "只影响新生成区块；已生成区块不会回溯。")
                .defineInRange("wild_wood_ear_forest_chance", 0.07D, 0.0D, 1.0D);
        VILLAGE_FARM_CROPS_ENABLED = builder
                .comment("允许在新生成的村庄农田中，把一部分原版作物替换为本模组作物。",
                        "设为 false 时不替换，不影响原版及其他模组的规则。")
                .define("village_farm_crops_enabled", true);
        VILLAGE_FARM_ALLOWED_CROPS = builder
                .comment("村庄农田允许生成的作物，填写播种物品的完整注册 ID。",
                        "默认包含全部 16 种农田作物，空列表表示不生成任何本模组农田作物。")
                .defineListAllowEmpty("village_farm_allowed_crops", DEFAULT_VILLAGE_FARM_CROPS,
                        //? if >=1.20.5 {
                        /*() -> "chinese_regional_culinary_ingredients:soybean",*/
                        //?}
                        NortheastChinaConfig::isItemId);
        builder.pop();

        builder.comment("箱子战利品").push("chest_loot");
        CHEST_LOOT_ENABLED = builder
                .comment("允许本模组物品在箱子战利品中生成，包括结构、村庄和东北小院箱子。",
                        "设为 false 时保留原版及其他模组的物品，不影响钓鱼、实体或方块掉落。")
                .define("enabled", true);
        CHEST_LOOT_ALLOWED_ITEMS = builder
                .comment("箱子战利品允许生成的本模组物品，填写完整注册 ID。",
                        "默认包含全部 139 种现有候选，空列表表示不生成任何本模组箱子物品。")
                .defineListAllowEmpty("allowed_items", DEFAULT_CHEST_LOOT_ITEMS,
                        //? if >=1.20.5 {
                        /*() -> "chinese_regional_culinary_ingredients:soybean",*/
                        //?}
                        NortheastChinaConfig::isItemId);
        builder.pop();
        SPEC = builder.build();
    }

    /** 两个平台的 Builder 方法名完全一样，只有类型名不同，所以在这里分版本取。 */
    //? if <1.20.2 {
    private static ForgeConfigSpec.Builder configBuilder() {
        return new ForgeConfigSpec.Builder();
    }
    //?} else {
    /*private static ModConfigSpec.Builder configBuilder() {
        return new ModConfigSpec.Builder();
    }
    *///?}

    private static boolean isItemId(Object value)
    {
        return value instanceof String itemId && itemId.indexOf(':') > 0
                && ResourceLocation.tryParse(itemId) != null;
    }

    public static float wildWoodEarOakChance()
    {
        return SPEC.isLoaded() ? WILD_WOOD_EAR_OAK_CHANCE.get().floatValue() : 0.05F;
    }

    public static float wildWoodEarForestChance()
    {
        return SPEC.isLoaded() ? WILD_WOOD_EAR_FOREST_CHANCE.get().floatValue() : 0.07F;
    }

    public static boolean isVillageFarmCropAllowed(ResourceLocation cropItem)
    {
        return (!SPEC.isLoaded() || VILLAGE_FARM_CROPS_ENABLED.get())
                && (SPEC.isLoaded() ? VILLAGE_FARM_ALLOWED_CROPS.get() : DEFAULT_VILLAGE_FARM_CROPS)
                .contains(cropItem.toString());
    }

    public static boolean isChestLootEnabled()
    {
        return !SPEC.isLoaded() || CHEST_LOOT_ENABLED.get();
    }

    public static boolean isChestLootItemAllowed(ResourceLocation itemId)
    {
        if (!NortheastChinaDelight.MODID.equals(itemId.getNamespace())
                && !NortheastChinaDelight.CRCI_MODID.equals(itemId.getNamespace()))
        {
            return true;
        }
        return isChestLootEnabled()
                && (SPEC.isLoaded() ? CHEST_LOOT_ALLOWED_ITEMS.get() : DEFAULT_CHEST_LOOT_ITEMS)
                .contains(itemId.toString());
    }

    private NortheastChinaConfig()
    {
    }
}
