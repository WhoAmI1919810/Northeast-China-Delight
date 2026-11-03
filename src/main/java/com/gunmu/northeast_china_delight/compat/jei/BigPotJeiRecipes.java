package com.gunmu.northeast_china_delight.compat.jei;

import com.gunmu.northeast_china_delight.crafting.BigPotRecipe;
import com.gunmu.northeast_china_delight.crafting.BigPotRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.Arrays;
import java.util.List;

/** 把 {@link BigPotRecipes} 的配方转成 JEI 展示形状。 */
public final class BigPotJeiRecipes {

    private BigPotJeiRecipes() {
    }

    public static List<BigPotJeiRecipe> all(RecipeManager manager) {
        return BigPotRecipes.all(manager).stream().map(BigPotJeiRecipes::of).toList();
    }

    private static BigPotJeiRecipe of(BigPotRecipe recipe) {
        return new BigPotJeiRecipe(recipe.id(),
                recipe.liquid() == null ? List.of() : recipe.liquid().fluids(),
                recipe.liquid() == null ? 0 : recipe.liquid().amount(),
                slots(recipe.ingredients()), slots(recipe.seasonings()),
                recipe.container().copy(), recipe.result().copy(), recipe.cookingTime());
    }

    private static List<List<ItemStack>> slots(List<BigPotRecipe.Entry> entries) {
        return entries.stream()
                .map(entry -> Arrays.stream(entry.ingredient().getItems())
                        .map(stack -> stack.copyWithCount(entry.count()))
                        .toList())
                .toList();
    }
}
