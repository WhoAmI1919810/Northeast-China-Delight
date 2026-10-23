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
 * <p>控制菜肴摆放、世界生成、箱子战利品和海洋取水模式。</p>
 */
public final class NortheastChinaConfig
{
    public static final List<String> DEFAULT_VILLAGE_FARM_CROPS = List.of(
            "northeast_china_delight:soybean",
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
            "northeast_china_delight:red_chili_seeds");

    public static final List<String> DEFAULT_CHEST_LOOT_ITEMS = List.of(
            "northeast_china_delight:animal_oil",
            "northeast_china_delight:bellflower_root",
            "northeast_china_delight:bellflower_seeds",
            "northeast_china_delight:blood_sausage",
            "northeast_china_delight:braised_pork_ribs",
            "northeast_china_delight:braised_sea_cucumber_scallion",
            "northeast_china_delight:buckwheat",
            "northeast_china_delight:buckwheat_sack",
            "northeast_china_delight:chicken_frame",
            "northeast_china_delight:chili_oil",
            "northeast_china_delight:chili_sauce",
            "northeast_china_delight:cold_noodle_sheet",
            "northeast_china_delight:cooking_oil",
            "northeast_china_delight:corn_flour",
            "northeast_china_delight:corn_seeds",
            "northeast_china_delight:corn_seeds_sack",
            "northeast_china_delight:crispy_pork_strips",
            "northeast_china_delight:cucumber_seeds",
            "northeast_china_delight:dried_tofu",
            "northeast_china_delight:eggplant_seeds",
            "northeast_china_delight:fish_sauce",
            "northeast_china_delight:ginseng_seeds",
            "northeast_china_delight:green_beans_seeds",
            "northeast_china_delight:green_onion_seeds",
            "northeast_china_delight:green_pepper_seeds",
            "northeast_china_delight:green_radish_seeds",
            "northeast_china_delight:grilled_chicken_frame",
            "northeast_china_delight:grilled_oil_edge",
            "northeast_china_delight:hairtail",
            "northeast_china_delight:mung_bean_sheet",
            "northeast_china_delight:napa_cabbage",
            "northeast_china_delight:napa_cabbage_seeds",
            "northeast_china_delight:oil_edge",
            "northeast_china_delight:old_style_crispy_pork",
            "northeast_china_delight:oyster",
            "northeast_china_delight:peanut",
            "northeast_china_delight:peanut_butter",
            "northeast_china_delight:pickled_snow_mustard",
            "northeast_china_delight:pig_blood",
            "northeast_china_delight:pig_liver",
            "northeast_china_delight:pork_hock",
            "northeast_china_delight:pork_intestine",
            "northeast_china_delight:pork_ribs",
            "northeast_china_delight:pork_vermicelli_stew_bowl",
            "northeast_china_delight:red_bean",
            "northeast_china_delight:red_bean_sack",
            "northeast_china_delight:red_chili_seeds",
            "northeast_china_delight:red_sausage",
            "northeast_china_delight:rice_sausage",
            "northeast_china_delight:salt",
            "northeast_china_delight:salted_fish",
            "northeast_china_delight:salted_fish_flatbread",
            "northeast_china_delight:sauerkraut_seafood_stew",
            "northeast_china_delight:sea_cucumber",
            "northeast_china_delight:shrimp",
            "northeast_china_delight:shrimp_paste",
            "northeast_china_delight:smoked_meat_platter",
            "northeast_china_delight:snow_mustard",
            "northeast_china_delight:snow_mustard_seeds",
            "northeast_china_delight:sour_dough",
            "northeast_china_delight:soy_paste",
            "northeast_china_delight:soy_sauce",
            "northeast_china_delight:soybean",
            "northeast_china_delight:soybean_sack",
            "northeast_china_delight:sweet_potato",
            "northeast_china_delight:vermicelli",
            "northeast_china_delight:vinegar",
            "northeast_china_delight:white_vinegar");

    //? if <1.20.2 {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue DISH_PLACEMENT_ENABLED;

    public static final ForgeConfigSpec.BooleanValue YARD_PARTS_ENABLED;
    public static final ForgeConfigSpec.IntValue SEA_WATER_BUCKET_MODE;
    public static final ForgeConfigSpec.BooleanValue VILLAGE_FARM_CROPS_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VILLAGE_FARM_ALLOWED_CROPS;
    public static final ForgeConfigSpec.BooleanValue CHEST_LOOT_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CHEST_LOOT_ALLOWED_ITEMS;
    //?} else {
    /*public static final ModConfigSpec SPEC;

    /^* 菜肴方块形态总开关 ^/
    public static final ModConfigSpec.BooleanValue DISH_PLACEMENT_ENABLED;

    /^* 东北小院里的小零件（灶棚、鸡架、水井、菜窖等）是否生成 ^/
    public static final ModConfigSpec.BooleanValue YARD_PARTS_ENABLED;
    public static final ModConfigSpec.IntValue SEA_WATER_BUCKET_MODE;
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
        builder.comment("菜肴方块形态").push("dishes");
        DISH_PLACEMENT_ENABLED = builder
                .comment("测试功能，尚未完善。",
                        "设为 true 时，允许玩家潜行右键放置菜肴的方块形态。")
                .define("dish_placement_enabled", false);
        builder.pop();

        builder.comment("世界生成").push("worldgen");
        YARD_PARTS_ENABLED = builder
                .comment("东北小院里的小建筑（灶棚、鸡架、水井、菜窖、柴垛等）是否生成。",
                        "设为 false 时只生成院子主体，不生成零件。")
                .define("yard_parts_enabled", false);
        VILLAGE_FARM_CROPS_ENABLED = builder
                .comment("允许在新生成的村庄农田中，把一部分原版作物替换为本模组作物。",
                        "设为 false 时不替换，不影响原版及其他模组的规则。")
                .define("village_farm_crops_enabled", true);
        VILLAGE_FARM_ALLOWED_CROPS = builder
                .comment("村庄农田允许生成的作物，填写播种物品的完整注册 ID。",
                        "默认包含全部 15 种农田作物，空列表表示不生成任何本模组农田作物。")
                .defineListAllowEmpty("village_farm_allowed_crops", DEFAULT_VILLAGE_FARM_CROPS,
                        //? if >=1.20.5 {
                        /*() -> "northeast_china_delight:soybean",*/
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
                        "默认包含全部 68 种现有候选，空列表表示不生成任何本模组箱子物品。")
                .defineListAllowEmpty("allowed_items", DEFAULT_CHEST_LOOT_ITEMS,
                        //? if >=1.20.5 {
                        /*() -> "northeast_china_delight:soybean",*/
                        //?}
                        NortheastChinaConfig::isItemId);
        builder.pop();

        builder.comment("海洋取水").push("sea_water");
        SEA_WATER_BUCKET_MODE = builder
                .comment("1：在海洋中取水获得海水桶。",
                        "2：在海洋中取水，获得带海水标记的普通水桶，功能同海水桶",
                        "3：不干预正常取水")
                .defineInRange("bucket_mode", 1, 1, 3);
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
        if (!NortheastChinaDelight.MODID.equals(itemId.getNamespace()))
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
