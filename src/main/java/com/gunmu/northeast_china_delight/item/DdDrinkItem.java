package com.gunmu.northeast_china_delight.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

/**
 * 瓶装饮品：用「喝」的动作和音效，不是「吃」的。
 */
public class DdDrinkItem extends DdConsumableItem {

    public DdDrinkItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }
}
