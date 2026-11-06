package com.gunmu.northeast_china_delight.compat.jei;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * JEI 里的一条「大锅烹饪」配方。食材、调料每格是一组候选（数量已经写在物品上）；
 * 液体只在锅里画出来、另外挂成看不见的原料（JEI 里查「水」的用途能查到），锅盖加速 JEI 里不显示。
 * 配方的液体是个范围时 liquidMb 取最少的那个量（画液面用），liquidMaxMb 是最多的量（不限是 Integer.MAX_VALUE）；
 * 不要液体时两个都是 0。liquidTag 是配方写的流体标签（写的是具体流体时为 null）。鼠标移到锅上的框里写这些。
 */
public record BigPotJeiRecipe(ResourceLocation id, List<Fluid> liquids, int liquidMb, int liquidMaxMb,
                              @Nullable ResourceLocation liquidTag,
                              List<List<ItemStack>> ingredients, List<List<ItemStack>> seasonings,
                              ItemStack container, ItemStack result, int cookingTime, float experience) {
}
