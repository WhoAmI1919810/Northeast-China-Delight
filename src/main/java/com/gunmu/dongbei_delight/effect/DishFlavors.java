package com.gunmu.dongbei_delight.effect;

import com.gunmu.dongbei_delight.DongbeiDelight;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

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
        put(map, Flavor.BALANCED, 3, "grilled_cold_noodles");

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
