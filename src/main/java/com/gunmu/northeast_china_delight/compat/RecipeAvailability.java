package com.gunmu.northeast_china_delight.compat;

import com.gunmu.chinese_regional_culinary_ingredients.region.CrciAvailability;
import com.gunmu.chinese_regional_culinary_ingredients.region.CrciUseSite;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

/** Hides recipe-book entries that point at content this region pack did not enable. */
public final class RecipeAvailability {
    public static boolean visible(Recipe<?> recipe, RegistryAccess registries) {
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) {
                continue;
            }
            boolean any = false;
            for (ItemStack stack : ingredient.getItems()) {
                if (CrciAvailability.isAvailable(stack, CrciUseSite.RECIPE)) {
                    any = true;
                    break;
                }
            }
            if (!any) {
                return false;
            }
        }
        ItemStack result = recipe.getResultItem(registries);
        return result.isEmpty() || CrciAvailability.isAvailable(result, CrciUseSite.RECIPE);
    }

    private RecipeAvailability() {
    }
}
