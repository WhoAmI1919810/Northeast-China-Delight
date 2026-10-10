package com.gunmu.northeast_china_delight.effect;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * 菜肴的"口味"表：决定吃完给什么状态效果、持续多久。持续时长按食材数算：每种食材 10 秒（不计调料）。个别菜可以单独指定时长（比如烤鸡架只给 10 秒）。不在表里的食物（比如
 * 豆浆、烤红薯）不给任何效果。
 */
public final class DishFlavors {

    public enum Flavor {
        GREASY,
        REFRESHING,
        BALANCED,
        WARM
    }

    public record Info(Flavor flavor, int ingredientCount, int overrideTicks) {
        public int durationTicks() {
            if (this.overrideTicks >= 0) {
                return this.overrideTicks;
            }
            return this.ingredientCount * 200;
        }
    }

    private static Map<Item, Info> table;

    private static final Map<String, Integer> WARMTH_PATHS = Map.ofEntries(
            Map.entry("soy_paste_soup", 0),
            Map.entry("seafood_tofu_soup", 0),
            Map.entry("sea_cucumber_tofu_soup", 0),
            Map.entry("sauerkraut_seafood_stew", 0),
            Map.entry("spicy_beef_soup", 0),
            Map.entry("kimchi_tofu_soup", 0),
            Map.entry("ginseng_chicken_soup", 0),
            Map.entry("cabbage_tofu_vermicelli_stew", 0),
            Map.entry("demoli_fish_stew", 0),
            Map.entry("borscht", 0),
            Map.entry("shrimp_paste_tofu_stew", 0),
            Map.entry("crushed_corn_porridge", 0),
            Map.entry("sweet_potato_porridge", 0),
            Map.entry("sour_noodle_soup", 0),
            Map.entry("soy_milk", 200),
            Map.entry("baked_sweet_potato", 200),
            Map.entry("grilled_corn", 200),
            Map.entry("shrimp_pork_dumpling", 200),
            Map.entry("three_delicacy_dumpling", 200),
            Map.entry("sauerkraut_crackling_dumpling", 200)
    );

    private static Map<Item, Integer> warmthTable;

    private DishFlavors() {
    }

    public static Info of(Item item) {
        if (table == null) {
            table = build();
        }
        return table.get(item);
    }

    public static Flavor flavorOf(Item item) {
        Info info = of(item);
        return info == null ? null : info.flavor();
    }

    public static int warmthTicks(Item item, Info info) {
        if (warmthTable == null) {
            Map<Item, Integer> map = new HashMap<>();
            for (Map.Entry<String, Integer> entry : WARMTH_PATHS.entrySet()) {
                map.put(BuiltInRegistries.ITEM.get(
                        DdIds.of(NortheastChinaDelight.MODID, entry.getKey())), entry.getValue());
            }
            warmthTable = Map.copyOf(map);
        }
        Integer ticks = warmthTable.get(item);
        if (ticks == null) {
            return 0;
        }
        return ticks > 0 ? ticks : info.durationTicks();
    }

    private static Map<Item, Info> build() {
        Map<Item, Info> map = new HashMap<>();

        put(map, Flavor.GREASY, 5, "old_style_crispy_pork");
        put(map, Flavor.GREASY, 4, "new_style_crispy_pork");
        put(map, Flavor.GREASY, 5, "orange_crispy_pork");
        put(map, Flavor.GREASY, 4, "crispy_pork_strips");
        put(map, Flavor.GREASY, 3, "braised_pork_hock");
        put(map, Flavor.GREASY, 2, "braised_pork_strips");
        put(map, Flavor.GREASY, 2, "braised_pork_bone_sauce");
        put(map, Flavor.GREASY, 3, "braised_beef_sauce");
        put(map, Flavor.GREASY, 6, "smoked_meat_platter");
        put(map, Flavor.GREASY, 4, "braised_pork_ribs");
        put(map, Flavor.GREASY, 3, "candied_sweet_potato");
        put(map, Flavor.GREASY, 4, "dry_fried_hairtail");
        put(map, Flavor.GREASY, 4, "braised_hairtail");
        put(map, Flavor.GREASY, 5, "hazelnut_sugar_fire_bun");
        put(map, Flavor.GREASY, 5, "corn_fritter");
        put(map, Flavor.GREASY, 5, "potato_pancake");
        put(map, Flavor.GREASY, 3, "three_fresh_veggies");
        put(map, Flavor.GREASY, 3, "sticky_bean_bun");
        // 大盆菜：效果挂在碗装的那一份上（盆本身是方块物品，不能吃、没有效果）
        put(map, Flavor.GREASY, 6, "slaughter_feast_bowl");
        put(map, Flavor.GREASY, 4, "ground_pot_chicken_bowl");
        put(map, Flavor.GREASY, 4, "ground_pot_ribs_bowl");
        put(map, Flavor.GREASY, 2, "salted_fish_flatbread");
        put(map, Flavor.GREASY, 4, "kimchi_pancake");
        put(map, Flavor.GREASY, 5, "snowy_bean_paste");
        put(map, Flavor.GREASY, 2, "candied_peanuts");
        put(map, Flavor.GREASY, 3, "grilled_oil_edge");
        put(map, Flavor.GREASY, 3, 200, "grilled_chicken_frame");

        put(map, Flavor.REFRESHING, 3, "sweet_potato_porridge");
        put(map, Flavor.REFRESHING, 4, "crushed_corn_porridge");
        put(map, Flavor.REFRESHING, 2, "sour_noodle_soup");
        put(map, Flavor.REFRESHING, 3, "pepper_dried_tofu");
        put(map, Flavor.REFRESHING, 2, "sauerkraut_fried_vermicelli");
        put(map, Flavor.REFRESHING, 6, "buckwheat_cold_noodles");
        put(map, Flavor.REFRESHING, 5, "pickled_veggie_salad");
        put(map, Flavor.REFRESHING, 7, "shredded_pollack");
        put(map, Flavor.REFRESHING, 8, "dip_veggie_platter");
        put(map, Flavor.REFRESHING, 4, "stuffed_cucumber_pickle");
        put(map, Flavor.REFRESHING, 8, "mixed_vegetable_salad");
        put(map, Flavor.REFRESHING, 3, "cabbage_tofu_vermicelli_stew");
        put(map, Flavor.REFRESHING, 2, "pickled_cucumber_pork_stirfry");
        put(map, Flavor.REFRESHING, 5, "tiger_salad");
        put(map, Flavor.REFRESHING, 1, 200, "kvass");

        put(map, Flavor.WARM, 6, "kimchi_tofu_soup");
        put(map, Flavor.WARM, 3, "soy_paste_soup");
        put(map, Flavor.WARM, 1, "soy_milk");
        put(map, Flavor.WARM, 1, "baked_sweet_potato");
        put(map, Flavor.WARM, 1, "grilled_corn");

        put(map, Flavor.BALANCED, 3, "steamed_egg_soy_paste");
        put(map, Flavor.BALANCED, 4, "northeast_rice_wrap");
        put(map, Flavor.BALANCED, 3, "sauerkraut_bone_stew_bowl");
        put(map, Flavor.BALANCED, 4, "pork_vermicelli_stew_bowl");
        put(map, Flavor.BALANCED, 6, "harvest_stew_bowl");
        put(map, Flavor.BALANCED, 5, "spicy_beef_soup");
        put(map, Flavor.BALANCED, 4, "kimchi_fried_rice");
        put(map, Flavor.BALANCED, 4, "shrimp_pork_dumpling");
        put(map, Flavor.BALANCED, 3, "shrimp_paste_scrambled_egg");
        put(map, Flavor.BALANCED, 3, "shrimp_paste_tofu_stew");
        put(map, Flavor.BALANCED, 4, "seafood_tofu_soup");
        put(map, Flavor.BALANCED, 5, "three_delicacy_dumpling");
        put(map, Flavor.BALANCED, 3, "sauerkraut_crackling_dumpling");
        put(map, Flavor.BALANCED, 2, "cured_pork_bean_stew");
        put(map, Flavor.BALANCED, 5, "mung_bean_sheet_salad");
        put(map, Flavor.BALANCED, 2, "braised_sea_cucumber_scallion");
        put(map, Flavor.BALANCED, 2, "sea_cucumber_tofu_soup");
        put(map, Flavor.BALANCED, 6, "demoli_fish_stew");
        put(map, Flavor.BALANCED, 6, "borscht");
        put(map, Flavor.BALANCED, 6, "sauerkraut_seafood_stew_bowl");
        put(map, Flavor.BALANCED, 5, "ginseng_chicken_soup");
        put(map, Flavor.BALANCED, 4, "chicken_mushroom_stew_bowl");
        put(map, Flavor.BALANCED, 6, "bibimbap");
        put(map, Flavor.BALANCED, 5, "grilled_cold_noodles");

        return Map.copyOf(map);
    }

    private static void put(Map<Item, Info> map, Flavor flavor, int ingredientCount, String path) {
        put(map, flavor, ingredientCount, -1, path);
    }

    private static void put(Map<Item, Info> map, Flavor flavor, int ingredientCount, int overrideTicks, String path) {
        Item item = BuiltInRegistries.ITEM.get(DdIds.of(NortheastChinaDelight.MODID, path));
        map.put(item, new Info(flavor, ingredientCount, overrideTicks));
    }
}
