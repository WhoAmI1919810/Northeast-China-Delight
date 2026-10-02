package com.gunmu.northeast_china_delight.effect;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 菜肴的"口味"表：决定吃完给什么状态效果、持续多久。
 *
 * 持续时长按配方里用了几种材料算：≤4 种给 3 分钟，>4 种给 5 分钟；
 * 个别菜可以单独指定时长（比如烤鸡架只给 10 秒）。
 * 不在表里的食物（比如豆浆、烤红薯）不给任何效果。
 */
public final class DishFlavors {

    public enum Flavor {
        /** 吃了容易腻 */
        GREASY,
        /** 爽口 */
        REFRESHING,
        /** 荤素搭配、不腻 */
        BALANCED
    }

    public record Info(Flavor flavor, int ingredientCount, int overrideTicks) {
        /**
         * 状态效果持续时长（tick）：≤4 种材料 3 分钟，>4 种 5 分钟；
         * {@link #overrideTicks} 大于等于 0 时以它为准（个别菜的单独时长）。
         */
        public int durationTicks() {
            if (this.overrideTicks >= 0) {
                return this.overrideTicks;
            }
            return this.ingredientCount > 4 ? 6000 : 3600;
        }
    }

    private static Map<Item, Info> table;

    /**
     * 汤类料理：除了各自的口味效果，还会额外给「暖身」——
     * 寒冷覆雪的群系里跑得动（+5%×等级 移动速度），也不会陷进细雪。
     *
     * <p>想给某道汤加上/去掉，只要改这一份名单。
     */
    private static final Set<String> SOUP_PATHS = Set.of(
            "soy_paste_soup",              // 大酱汤
            "seafood_tofu_soup",       // 大虾豆腐汤
            "sea_cucumber_tofu_soup",       // 海参豆腐汤
            "sauerkraut_seafood_stew",      // 酸菜海鲜锅
            "spicy_beef_soup",        // 辣牛肉汤
            "kimchi_tofu_soup",        // 辣白菜豆腐汤
            "ginseng_chicken_soup",               // 参鸡汤
            "cabbage_tofu_vermicelli_stew",// 白菜豆腐炖粉条
            "demoli_fish_stew",            // 得莫利炖鱼
            "borscht",                 // 苏波汤
            "shrimp_paste_tofu_stew",       // 虾酱炖豆腐
            "crushed_corn_porridge",                // 碴子粥
            "sweet_potato_porridge",      // 地瓜粥
            "sour_noodle_soup"                 // 酸汤子
    );

    private static Set<Item> soupTable;

    private DishFlavors() {
    }

    /** 查一道菜的口味；返回 null 表示这道菜不触发口味效果 */
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

    /** 这道菜是不是汤类（决定要不要额外给「暖身」） */
    public static boolean isSoup(Item item) {
        if (soupTable == null) {
            Set<Item> set = new HashSet<>();
            for (String path : SOUP_PATHS) {
                set.add(BuiltInRegistries.ITEM.get(
                        ResourceLocation.fromNamespaceAndPath(NortheastChinaDelight.MODID, path)));
            }
            soupTable = Set.copyOf(set);
        }
        return soupTable.contains(item);
    }

    private static Map<Item, Info> build() {
        Map<Item, Info> map = new HashMap<>();

        // ===== 吃了容易腻：油炸 / 重糖 / 肥肉 / 浓油赤酱 =====
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
        put(map, Flavor.GREASY, 5, "corn_fritter");
        put(map, Flavor.GREASY, 5, "potato_pancake");
        put(map, Flavor.GREASY, 3, "three_fresh_veggies");
        put(map, Flavor.GREASY, 3, "sticky_bean_bun");
        // 大盆菜：效果挂在**碗装的那一份**上（盆本身是方块物品，不能吃、没有效果）
        put(map, Flavor.GREASY, 6, "slaughter_feast_bowl");
        put(map, Flavor.GREASY, 4, "ground_pot_chicken_bowl");
        put(map, Flavor.GREASY, 4, "ground_pot_ribs_bowl");
        put(map, Flavor.GREASY, 2, "salted_fish_flatbread");
        put(map, Flavor.GREASY, 4, "kimchi_pancake");
        put(map, Flavor.GREASY, 5, "snowy_bean_paste");
        put(map, Flavor.GREASY, 2, "candied_peanuts");
        put(map, Flavor.GREASY, 3, "grilled_oil_edge");
        // 烤鸡架：口味是油腻，但只给 10 秒（200 tick）—— 一串鸡架不值当腻三分钟
        put(map, Flavor.GREASY, 3, 200, "grilled_chicken_frame");

        // ===== 爽口：凉拌 / 酸口 / 清汤 =====
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
        put(map, Flavor.REFRESHING, 6, "kimchi_tofu_soup");
        put(map, Flavor.REFRESHING, 2, "pickled_cucumber_pork_stirfry");
        put(map, Flavor.REFRESHING, 3, "soy_paste_soup");
        put(map, Flavor.REFRESHING, 5, "tiger_salad");

        // ===== 荤素搭配、不会腻：吃完给农夫乐事的滋养 =====
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
        put(map, Flavor.BALANCED, 6, "sauerkraut_seafood_stew");
        put(map, Flavor.BALANCED, 5, "ginseng_chicken_soup");
        put(map, Flavor.BALANCED, 4, "chicken_mushroom_stew_bowl");
        put(map, Flavor.BALANCED, 6, "bibimbap");
        // 烤冷面：冷面片 + 鸡蛋 + 辣椒酱 + 糖 + 醋 = 5 种材料
        put(map, Flavor.BALANCED, 5, "grilled_cold_noodles");

        return Map.copyOf(map);
    }

    private static void put(Map<Item, Info> map, Flavor flavor, int ingredientCount, String path) {
        put(map, flavor, ingredientCount, -1, path);
    }

    /** 带单独时长（tick）的版本 */
    private static void put(Map<Item, Info> map, Flavor flavor, int ingredientCount, int overrideTicks, String path) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(NortheastChinaDelight.MODID, path));
        map.put(item, new Info(flavor, ingredientCount, overrideTicks));
    }
}
