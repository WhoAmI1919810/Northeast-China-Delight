package com.gunmu.northeast_china_delight.compat.jei;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * JEI 里的一条「烧烤」配方：一份食材 + 若干种调料 → 一份成品。 @param ingredient 食材（支持标签时这里会有多种可选的物品） @param season
 * ings 需要的调料，一样一格 @param result 成品 @param seconds 烤制秒数，写在箭头下面
 */
public record GrillJeiRecipe(List<ItemStack> ingredient, List<ItemStack> seasonings, ItemStack result, int seconds) {
}
