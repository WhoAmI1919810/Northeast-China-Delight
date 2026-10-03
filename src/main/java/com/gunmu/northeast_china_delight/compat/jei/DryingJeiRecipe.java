package com.gunmu.northeast_china_delight.compat.jei;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * JEI 里的一条「批量风干」配方：一份食材被鼓风机的裸风吹一段时间 → 成品。
 * 数据源写在 {@link DryingJeiRecipes} 里，目前只有"碗装豆浆 → 干豆腐 + 碗"。
 */
public record DryingJeiRecipe(List<ItemStack> ingredient, List<ItemStack> results, int ticks) {
}
