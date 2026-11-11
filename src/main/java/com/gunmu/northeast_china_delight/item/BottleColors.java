package com.gunmu.northeast_china_delight.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 瓶装物品（调料瓶 / 瓶装饮品）的「液面颜色」。瓶子的画法完全照原版药水：模型只有两层 —— layer0 =
 * {@code minecraft:item/potion_overlay}（灰白液面，会被染色）， layer1 = {@code minecraft:item/potion}
 * （玻璃瓶，不染色）。所以每种调料瓶不需要单独画贴图，只要在这里给一个颜色，游戏里就会像药水一样显示出对应颜色的液体。
 */
public final class BottleColors {

    private static final Map<Item, Integer> COLORS = new LinkedHashMap<>();

    static {
        put(ModItems.KVASS, 0xA05A1E);
    }

    private BottleColors() {
    }

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
        Integer color = COLORS.get(item);
        return color != null
                ? 0xFF000000 | color
                : com.gunmu.chinese_regional_culinary_ingredients.item.BottleColors.of(item);
    }

    private static void put(Supplier<? extends Item> item, int color) {
        COLORS.put(item.get(), color);
    }
}
