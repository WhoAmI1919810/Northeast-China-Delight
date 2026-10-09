package com.gunmu.northeast_china_delight.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 吃完会返还容器（空碗 / 空瓶 / 大脸盆 / 黄铜碗）的食物。
 *
 * <p>1.20.4 没有「食物组件」这一层：{@code Item.Properties#craftRemainder} 对吃东西毫无作用，
 * 想还容器只能自己覆写 {@code finishUsingItem}（写法同原版蜂蜜瓶、农夫乐事的 ConsumableItem）。
 * 1.21 起这件事写在食物组件里（{@code FoodProperties.Builder#usingConvertsTo}），物品本身什么都不用做。</p>
 *
 * <p>所以这个类在两个版本都用：1.21 分支里它就是个普通 {@code Item}。</p>
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
        // 整堆吃完了：直接把手里的东西换成容器
        if (stack.isEmpty()) {
            return container;
        }
        // 还有剩（可堆叠的瓶装饮品）：容器塞进背包，手里这堆照旧
        if (consumer instanceof Player player && !player.getAbilities().instabuild) {
            if (!player.getInventory().add(container)) {
                player.drop(container, false);
            }
        }
        return stack;
    }
    //?}
}
