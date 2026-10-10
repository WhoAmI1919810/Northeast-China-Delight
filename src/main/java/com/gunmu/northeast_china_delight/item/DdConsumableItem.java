package com.gunmu.northeast_china_delight.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 吃完会返还容器（空碗 / 空瓶 / 大脸盆 / 黄铜碗）的食物。
 */
public class DdConsumableItem extends Item {

    public DdConsumableItem(Properties properties) {
        super(properties);
    }

    //? if <1.20.5 {
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
        ItemStack container = stack.getCraftingRemainingItem();
        super.finishUsingItem(stack, level, consumer);
        if (container.isEmpty()) {
            return stack;
        }
        if (stack.isEmpty()) {
            return container;
        }
        if (consumer instanceof Player player && !player.getAbilities().instabuild) {
            if (!player.getInventory().add(container)) {
                player.drop(container, false);
            }
        }
        return stack;
    }
    //?}
}
