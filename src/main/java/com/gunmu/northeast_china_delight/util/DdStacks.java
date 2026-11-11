package com.gunmu.northeast_china_delight.util;


import com.gunmu.chinese_regional_culinary_ingredients.item.PotLidItem;
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

    /**
     * 扣耐久，但碎了不发原版的「物品损坏」实体事件：客户端就不放 item.break 的声音、也不冒碎片，由调用的地方自己放声音
     * （木锅盖碎了放木头被破坏的声音，PotLidItem#wear）。创造模式不扣、耐久附魔照原版算，物品损坏统计照记
     * （1.20.1 在 hurtAndBreak 里记；1.21.1 原版放在发事件的 ServerPlayer#onEquippedItemBroken 里，这里不走那条，自己记）
     */
    public static void hurtAndBreakSilently(ItemStack stack, int amount, Player player) {
        //? if <1.20.5 {
        stack.hurtAndBreak(amount, player, broken -> { });
        //?} else {
        /*if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
            stack.hurtAndBreak(amount, level, player,
                    item -> player.awardStat(net.minecraft.stats.Stats.ITEM_BROKEN.get(item)));
        }
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
