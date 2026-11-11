package com.gunmu.northeast_china_delight.compat.jei;

import com.gunmu.chinese_regional_culinary_ingredients.crafting.BigPotRecipe;
import com.gunmu.chinese_regional_culinary_ingredients.crafting.BigPotRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** 把 {@link BigPotRecipes} 的配方转成 JEI 展示形状。 */
public final class BigPotJeiRecipes {

    private BigPotJeiRecipes() {
    }

    public static List<BigPotJeiRecipe> all(RecipeManager manager) {
        return BigPotRecipes.all(manager).stream().map(BigPotJeiRecipes::of)
                .filter(Objects::nonNull).toList();
    }

    @Nullable
    private static BigPotJeiRecipe of(BigPotRecipe recipe) {
        List<List<ItemStack>> ingredients = slots(recipe.ingredients());
        List<List<ItemStack>> seasonings = slots(recipe.seasonings());
        if (!JeiAvailability.anyAvailable(ingredients) || !JeiAvailability.anyAvailable(seasonings)) {
            return null;
        }
        if (!recipe.result().isEmpty() && !JeiAvailability.available(recipe.result())) {
            return null;
        }
        if (!recipe.container().isEmpty() && !JeiAvailability.available(recipe.container())) {
            return null;
        }
        return new BigPotJeiRecipe(recipe.id(),
                recipe.liquid() == null ? List.of() : recipe.liquid().fluids(),
                recipe.liquid() == null ? 0 : recipe.liquid().min(),
                recipe.liquid() == null ? 0 : recipe.liquid().max(),
                recipe.liquid() == null || recipe.liquid().tag() == null ? null : recipe.liquid().tag().location(),
                ingredients, seasonings,
                recipe.container().copy(), recipe.result().copy(), recipe.cookingTime(), recipe.experience());
    }

    private static List<List<ItemStack>> slots(List<BigPotRecipe.Entry> entries) {
        return entries.stream()
                .map(entry -> Arrays.stream(entry.ingredient().getItems())
                        .map(stack -> stack.copyWithCount(entry.count()))
                        .filter(JeiAvailability::available)
                        .toList())
                .toList();
    }
}
