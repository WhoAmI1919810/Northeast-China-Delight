package com.gunmu.northeast_china_delight.util;

//? if >=1.20.5 {
/*import net.minecraft.core.component.DataComponents;
*///?}
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 物品堆相关的小工具，用来抹平 1.20.1 与 1.21.1 的 API 差异。
 */
public final class DdStacks {

    private DdStacks() {
    }

    public static void hurtAndBreak(ItemStack stack, int amount, Player player, InteractionHand hand) {
        //? if <1.20.5 {
        stack.hurtAndBreak(amount, player, broken -> broken.broadcastBreakEvent(hand));
        //?} else {
        /*stack.hurtAndBreak(amount, player, LivingEntity.getSlotForHand(hand));
        *///?}
    }

    public static void consume(ItemStack stack, int amount, Player player) {
        //? if <1.20.5 {
        if (!DdPlayers.hasInfiniteMaterials(player)) {
            stack.shrink(amount);
        }
        //?} else {
        /*stack.consume(amount, player);
        *///?}
    }

    public static ItemStack consumeAndReturn(ItemStack stack, int amount, Player player) {
        //? if <1.20.5 {
        ItemStack taken = stack.copyWithCount(amount);
        if (!DdPlayers.hasInfiniteMaterials(player)) {
            stack.shrink(amount);
        }
        return taken;
        //?} else {
        /*return stack.consumeAndReturn(amount, player);
        *///?}
    }

    public static boolean isEdible(ItemStack stack) {
        //? if <1.20.5 {
        return stack.isEdible();
        //?} else {
        /*return stack.has(DataComponents.FOOD);
        *///?}
    }
}
