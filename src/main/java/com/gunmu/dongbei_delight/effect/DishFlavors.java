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
 * 持续时长按配方里用了几种材料算：≤4 种给 3 分钟，>4 种给 5 分钟。
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

    public record Info(Flavor flavor, int ingredientCount) {
        /** 状态效果持续时长（tick）：≤4 种材料 3 分钟，>4 种 5 分钟 */
        public int durationTicks() {
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
        put(map, Flavor.GREASY, 2, "braised_pork_hock");
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
        put(map, Flavor.GREASY, 6, "sha_zhu_cai");
        put(map, Flavor.GREASY, 4, "di_guo_ji");
        put(map, Flavor.GREASY, 4, "di_guo_pai_gu");

        // ===== 爽口：凉拌 / 酸口 / 清汤 =====
        put(map, Flavor.REFRESHING, 3, "sweet_potato_porridge");
        put(map, Flavor.REFRESHING, 3, "cha_zi_zhou");
        put(map, Flavor.REFRESHING, 2, "sour_tangzi");
        put(map, Flavor.REFRESHING, 3, "jian_jiao_gan_dou_fu");
        put(map, Flavor.REFRESHING, 2, "suan_cai_chao_fen_tiao");
        put(map, Flavor.REFRESHING, 6, "buckwheat_cold_noodles");
        put(map, Flavor.REFRESHING, 5, "liang_ban_xian_cai");
        put(map, Flavor.REFRESHING, 7, "ming_tai_yu_si");
        put(map, Flavor.REFRESHING, 5, "zhan_jiang_cai");
        put(map, Flavor.REFRESHING, 4, "jia_xian_huang_gua_pao_cai");
        put(map, Flavor.REFRESHING, 7, "liang_ban_hua_cai");
        put(map, Flavor.REFRESHING, 3, "bai_cai_dou_fu_dun_fen_tiao");
        put(map, Flavor.REFRESHING, 6, "la_bai_cai_tang_fan");
        put(map, Flavor.REFRESHING, 2, "suan_huang_gua_chao_rou_si");

        // ===== 荤素搭配、不会腻：吃完给农夫乐事的滋养 =====
        put(map, Flavor.BALANCED, 3, "egg_soy_paste");
        put(map, Flavor.BALANCED, 4, "da_fan_bao");
        put(map, Flavor.BALANCED, 3, "suan_cai_dun_gu_tou");
        put(map, Flavor.BALANCED, 4, "zhu_rou_dun_fen_tiao");
        put(map, Flavor.BALANCED, 6, "da_feng_shou");
        put(map, Flavor.BALANCED, 5, "la_niu_rou_tang_fan");
        put(map, Flavor.BALANCED, 3, "la_bai_cai_chao_fan");
        put(map, Flavor.BALANCED, 4, "xia_ren_zhu_rou_xian_shui_jiao");
        put(map, Flavor.BALANCED, 3, "xia_jiang_chao_ji_dan");
        put(map, Flavor.BALANCED, 3, "xia_jiang_dun_dou_fu");
        put(map, Flavor.BALANCED, 4, "hai_xian_dou_fu_tang");
        put(map, Flavor.BALANCED, 4, "san_xian_xian_shui_jiao");
        put(map, Flavor.BALANCED, 3, "suan_cai_jiao_zi");
        put(map, Flavor.BALANCED, 2, "la_rou_dun_dou_jiao");
        put(map, Flavor.BALANCED, 5, "jiang_ban_la_pi");
        put(map, Flavor.BALANCED, 2, "cong_shao_hai_shen");
        put(map, Flavor.BALANCED, 2, "hai_shen_dou_fu_tang");

        return Map.copyOf(map);
    }

    private static void put(Map<Item, Info> map, Flavor flavor, int ingredientCount, String path) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(DongbeiDelight.MODID, path));
        map.put(item, new Info(flavor, ingredientCount));
    }
}
