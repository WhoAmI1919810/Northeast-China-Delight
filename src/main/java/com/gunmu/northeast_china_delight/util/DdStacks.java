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

    /**
     * 扣工具耐久（会顺带广播「工具用坏」）。
     *
     * <p>1.20.1 只能传一个"坏了怎么办"的回调；1.20.5 起改成直接传装备槽，
     * 于是有个 {@code LivingEntity.getSlotForHand}。</p>
     */
    public static void hurtAndBreak(ItemStack stack, int amount, Player player, InteractionHand hand) {
        //? if <1.20.5 {
        stack.hurtAndBreak(amount, player, broken -> broken.broadcastBreakEvent(hand));
        //?} else {
        /*stack.hurtAndBreak(amount, player, LivingEntity.getSlotForHand(hand));
        *///?}
    }

    /** 消耗掉 amount 个（1.21 的 {@code ItemStack#consume}：创造模式不扣）。 */
    public static void consume(ItemStack stack, int amount, Player player) {
        //? if <1.20.5 {
        if (!DdPlayers.hasInfiniteMaterials(player)) {
            stack.shrink(amount);
        }
        //?} else {
        /*stack.consume(amount, player);
        *///?}
    }

    /**
     * 拿走 amount 个并把这「一份」返回（1.21 的 {@code ItemStack#consumeAndReturn}），
     * 创造模式不扣原堆。
     */
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

    /** 是不是能吃的（1.20.1 的 {@code ItemStack#isEdible()} 在 1.20.5 起换成了食物组件） */
    public static boolean isEdible(ItemStack stack) {
        //? if <1.20.5 {
        return stack.isEdible();
        //?} else {
        /*return stack.has(DataComponents.FOOD);
        *///?}
    }
}
