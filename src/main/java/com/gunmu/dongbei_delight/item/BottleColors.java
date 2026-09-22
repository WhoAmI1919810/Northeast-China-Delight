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
 *
 * 色号必须和原版药水拉开距离，判定标准见 docs/调料色号与药水色号比对.md：
 * 与原版任意药水的 CIEDE2000 色差 ΔE00 都要 ≥ 10。
 * 改了这里的色号，别忘记同步 textures/block 下同名的流体贴图
 * （*_still.png / *_flow.png，贴图的平均色要跟这里的色号一致）。
 */
public final class BottleColors {

    private static final Map<Item, Integer> COLORS = new LinkedHashMap<>();

    static {
        put(ModItems.SOY_SAUCE, 0x3E2410);      // 酱油：深褐        ΔE00 21.4
        put(ModItems.VINEGAR, 0x6B3A16);        // 醋：琥珀          ΔE00 18.7
        put(ModItems.SOUR_WATER, 0xC9C09A);     // 酸引水：发浑的米白 ΔE00 15.3
        put(ModItems.WHITE_VINEGAR, 0xE4DCC2);  // 白醋：近乎透明    ΔE00 12.0
        put(ModItems.FISH_SAUCE, 0x6B4416);     // 鱼露：琥珀棕      ΔE00 16.5
        put(ModItems.SHRIMP_PASTE, 0x8A4A34);   // 虾酱：紫褐        ΔE00 15.7
        put(ModItems.CHILI_OIL, 0xF26B0F);      // 辣椒油：鲜亮的橙红 ΔE00 14.8（原 0xE8451E 只有 5.3，撞生命恢复药水）
        put(ModItems.PEANUT_BUTTER, 0xC9A05A);  // 花生酱：棕黄      ΔE00 13.7
        put(ModItems.COOKING_OIL, 0xAA7E10);    // 植物油：琥珀金    ΔE00 18.2（原 0xE8C33C 只有 5.4，撞力量药水）
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
