package com.gunmu.northeast_china_delight.compat.jei;

import com.gunmu.chinese_regional_culinary_ingredients.crafting.GrillRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 把 {@link GrillRecipes} 里的配方转成 JEI 展示用的形状
 */
public final class GrillJeiRecipes {

    private GrillJeiRecipes() {
    }

    public static List<GrillJeiRecipe> all(RecipeManager manager) {
        List<GrillJeiRecipe> list = new ArrayList<>();
        for (GrillRecipes.Recipe recipe : GrillRecipes.all(manager)) {
            List<ItemStack> ingredient = Arrays.stream(recipe.input().getItems())
                    .map(ItemStack::copy)
                    .toList();
            List<List<ItemStack>> seasonings = recipe.seasonings().stream()
                    .map(ing -> Arrays.stream(ing.getItems()).map(ItemStack::copy).toList())
                    .toList();
            list.add(new GrillJeiRecipe(recipe.id(), ingredient, seasonings, recipe.result().copy(), recipe.seconds()));
        }
        return List.copyOf(list);
    }
}
