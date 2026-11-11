package com.gunmu.northeast_china_delight.compat.jei;

import com.gunmu.chinese_regional_culinary_ingredients.crafting.DryingRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.Arrays;
import java.util.List;

/**
 * 「批量风干」JEI 配方的清单，和游戏里 {@code ModFanProcessingTypes.DryingType} 的逻辑一一对应。
 */
public final class DryingJeiRecipes {

    private DryingJeiRecipes() {}

    public static List<DryingJeiRecipe> all(RecipeManager manager) {
        if (manager == null) {
            return List.of();
        }
        return DryingRecipes.all(manager).stream()
                .map(recipe -> new DryingJeiRecipe(recipe.id(),
                        Arrays.stream(recipe.input().getItems()).map(ItemStack::copy).toList(),
                        recipe.results().stream().map(ItemStack::copy).toList()))
                .filter(recipe -> !recipe.ingredient().isEmpty())
                .toList();
    }
}
