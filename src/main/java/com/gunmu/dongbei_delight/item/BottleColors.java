package com.gunmu.dongbei_delight.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 调料瓶的「液面颜色」。
 *
 * 瓶子的画法完全照原版药水：模型只有两层 ——
 * layer0 = {@code minecraft:item/potion_overlay}（灰白液面，会被染色），
 * layer1 = {@code minecraft:item/potion}（玻璃瓶，不染色）。
 * 所以每种调料瓶不需要单独画贴图，只要在这里给一个颜色，
 * 游戏里就会像药水一样显示出对应颜色的液体。
 *
 * 改颜色直接改下面的数值（十六进制 ARGB，常用的都是 0xRRGGBB）。
 */
public final class BottleColors {

    private static final Map<Item, Integer> COLORS = new LinkedHashMap<>();

    static {
        put(ModItems.SOY_SAUCE, 0x3E2410);      // 酱油：深褐
        put(ModItems.VINEGAR, 0x6B3A16);        // 醋：琥珀
        put(ModItems.SOUR_WATER, 0xC9C09A);     // 酸引水：发浑的米白
        put(ModItems.WHITE_VINEGAR, 0xE4DCC2);  // 白醋：近乎透明
        put(ModItems.FISH_SAUCE, 0x6B4416);     // 鱼露：琥珀棕
        put(ModItems.SHRIMP_PASTE, 0x8A4A34);   // 虾酱：紫褐
        put(ModItems.CHILI_OIL, 0xE8451E);      // 辣椒油：鲜亮的红橙
        put(ModItems.PEANUT_BUTTER, 0xC9A05A);  // 花生酱：棕黄
        put(ModItems.COOKING_OIL, 0xE8C33C);    // 植物油：金黄
    }

    private BottleColors() {
    }

    /** 注册物品染色时用的清单 */
    public static ItemLike[] bottles() {
        return COLORS.keySet().toArray(ItemLike[]::new);
    }

    /**
     * 某个调料瓶的液面颜色；不在表里就返回白色（等于不染色）。
     *
     * 注意一定要带上不透明的 alpha（0xFF）：染色值如果 alpha 是 0，液面那层会被染成全透明，
     * 看起来就像空瓶子 —— 原版药水那边也是用 FastColor.ARGB32.opaque(...) 强制不透明的。
     */
    public static int of(Item item) {
        return 0xFF000000 | COLORS.getOrDefault(item, 0xFFFFFF);
    }

    private static void put(Supplier<? extends Item> item, int color) {
        COLORS.put(item.get(), color);
    }
}
