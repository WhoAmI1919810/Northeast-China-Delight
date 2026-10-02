package com.gunmu.northeast_china_delight.compat.jei;

import com.gunmu.northeast_china_delight.crafting.GrillRecipes;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 把 {@link GrillRecipes} 里的配方转成 JEI 展示用的形状 */
public final class GrillJeiRecipes {

    private GrillJeiRecipes() {
    }

    public static List<GrillJeiRecipe> all() {
        List<GrillJeiRecipe> list = new ArrayList<>();
        for (GrillRecipes.Recipe recipe : GrillRecipes.all()) {
            List<ItemStack> ingredient = Arrays.stream(recipe.input().getItems())
                    .map(ItemStack::copy)
                    .toList();
            List<ItemStack> seasonings = recipe.seasonings().stream()
                    .map(ItemStack::new)
                    .toList();
            list.add(new GrillJeiRecipe(ingredient, seasonings, recipe.result().copy(), recipe.seconds()));
        }
        return List.copyOf(list);
    }
}
