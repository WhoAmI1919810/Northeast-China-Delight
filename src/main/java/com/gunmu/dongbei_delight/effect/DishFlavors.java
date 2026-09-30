package com.gunmu.dongbei_delight.effect;

import com.gunmu.dongbei_delight.DongbeiDelight;
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
            "da_jiang_tang",              // 大酱汤
            "hai_xian_dou_fu_tang",       // 大虾豆腐汤
            "hai_shen_dou_fu_tang",       // 海参豆腐汤
            "suan_cai_hai_xian_guo",      // 酸菜海鲜锅
            "la_niu_rou_tang_fan",        // 辣牛肉汤
            "la_bai_cai_tang_fan",        // 辣白菜豆腐汤
            "shen_ji_tang",               // 参鸡汤
            "bai_cai_dou_fu_dun_fen_tiao",// 白菜豆腐炖粉条
            "de_mo_li_dun_yu",            // 得莫利炖鱼
            "su_bo_tang",                 // 苏波汤
            "xia_jiang_dun_dou_fu",       // 虾酱炖豆腐
            "cha_zi_zhou",                // 碴子粥
            "sweet_potato_porridge",      // 地瓜粥
            "sour_tangzi"                 // 酸汤子
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
                        ResourceLocation.fromNamespaceAndPath(DongbeiDelight.MODID, path)));
            }
            soupTable = Set.copyOf(set);
        }
        return soupTable.contains(item);
    }

    private static Map<Item, Info> build() {
        Map<Item, Info> map = new HashMap<>();

        // ===== 吃了容易腻：油炸 / 重糖 / 肥肉 / 浓油赤酱 =====
        put(map, Flavor.GREASY, 5, "old_style_guo_bao_rou");
        put(map, Flavor.GREASY, 4, "new_style_guo_bao_rou");
        put(map, Flavor.GREASY, 5, "orange_guo_bao_rou");
        put(map, Flavor.GREASY, 4, "liu_rou_duan");
        put(map, Flavor.GREASY, 3, "braised_pork_hock");
        put(map, Flavor.GREASY, 2, "braised_pork_strips");
        put(map, Flavor.GREASY, 2, "jiang_da_gu");
        put(map, Flavor.GREASY, 3, "jiang_niu_rou");
        put(map, Flavor.GREASY, 6, "xun_jiang_pin_pan");
        put(map, Flavor.GREASY, 4, "hong_shao_pai_gu");
        put(map, Flavor.GREASY, 3, "ba_si_tu_dou");
        put(map, Flavor.GREASY, 5, "yu_mi_lao");
        put(map, Flavor.GREASY, 5, "tu_dou_bing");
        put(map, Flavor.GREASY, 3, "di_san_xian");
        put(map, Flavor.GREASY, 3, "nian_dou_bao");
        // 大盆菜：效果挂在**碗装的那一份**上（盆本身是方块物品，不能吃、没有效果）
        put(map, Flavor.GREASY, 6, "sha_zhu_cai_bowl");
        put(map, Flavor.GREASY, 4, "di_guo_ji_bowl");
        put(map, Flavor.GREASY, 4, "di_guo_pai_gu_bowl");
        put(map, Flavor.GREASY, 2, "xian_yu_bing_zi");
        put(map, Flavor.GREASY, 4, "kimchi_pancake");
        put(map, Flavor.GREASY, 5, "snowy_bean_paste");
        put(map, Flavor.GREASY, 2, "candied_peanuts");
        put(map, Flavor.GREASY, 3, "grilled_oil_edge");
        // 烤鸡架：口味是油腻，但只给 10 秒（200 tick）—— 一串鸡架不值当腻三分钟
        put(map, Flavor.GREASY, 3, 200, "grilled_chicken_frame");

        // ===== 爽口：凉拌 / 酸口 / 清汤 =====
        put(map, Flavor.REFRESHING, 3, "sweet_potato_porridge");
        put(map, Flavor.REFRESHING, 4, "cha_zi_zhou");
        put(map, Flavor.REFRESHING, 2, "sour_tangzi");
        put(map, Flavor.REFRESHING, 3, "jian_jiao_gan_dou_fu");
        put(map, Flavor.REFRESHING, 2, "suan_cai_chao_fen_tiao");
        put(map, Flavor.REFRESHING, 6, "buckwheat_cold_noodles");
        put(map, Flavor.REFRESHING, 5, "liang_ban_xian_cai");
        put(map, Flavor.REFRESHING, 7, "ming_tai_yu_si");
        put(map, Flavor.REFRESHING, 8, "zhan_jiang_cai");
        put(map, Flavor.REFRESHING, 4, "jia_xian_huang_gua_pao_cai");
        put(map, Flavor.REFRESHING, 8, "liang_ban_hua_cai");
        put(map, Flavor.REFRESHING, 3, "bai_cai_dou_fu_dun_fen_tiao");
        put(map, Flavor.REFRESHING, 6, "la_bai_cai_tang_fan");
        put(map, Flavor.REFRESHING, 2, "suan_huang_gua_chao_rou_si");
        put(map, Flavor.REFRESHING, 3, "da_jiang_tang");
        put(map, Flavor.REFRESHING, 5, "tiger_salad");

        // ===== 荤素搭配、不会腻：吃完给农夫乐事的滋养 =====
        put(map, Flavor.BALANCED, 3, "egg_soy_paste");
        put(map, Flavor.BALANCED, 4, "da_fan_bao");
        put(map, Flavor.BALANCED, 3, "suan_cai_dun_gu_tou_bowl");
        put(map, Flavor.BALANCED, 4, "zhu_rou_dun_fen_tiao_bowl");
        put(map, Flavor.BALANCED, 6, "da_feng_shou_bowl");
        put(map, Flavor.BALANCED, 5, "la_niu_rou_tang_fan");
        put(map, Flavor.BALANCED, 4, "la_bai_cai_chao_fan");
        put(map, Flavor.BALANCED, 4, "xia_ren_zhu_rou_xian_shui_jiao");
        put(map, Flavor.BALANCED, 3, "xia_jiang_chao_ji_dan");
        put(map, Flavor.BALANCED, 3, "xia_jiang_dun_dou_fu");
        put(map, Flavor.BALANCED, 4, "hai_xian_dou_fu_tang");
        put(map, Flavor.BALANCED, 5, "san_xian_xian_shui_jiao");
        put(map, Flavor.BALANCED, 3, "suan_cai_jiao_zi");
        put(map, Flavor.BALANCED, 2, "la_rou_dun_dou_jiao");
        put(map, Flavor.BALANCED, 5, "jiang_ban_la_pi");
        put(map, Flavor.BALANCED, 2, "cong_shao_hai_shen");
        put(map, Flavor.BALANCED, 2, "hai_shen_dou_fu_tang");
        put(map, Flavor.BALANCED, 6, "de_mo_li_dun_yu");
        put(map, Flavor.BALANCED, 6, "su_bo_tang");
        put(map, Flavor.BALANCED, 6, "suan_cai_hai_xian_guo");
        put(map, Flavor.BALANCED, 5, "shen_ji_tang");
        put(map, Flavor.BALANCED, 4, "xiao_ji_dun_mo_gu_bowl");
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
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(DongbeiDelight.MODID, path));
        map.put(item, new Info(flavor, ingredientCount, overrideTicks));
    }
}
