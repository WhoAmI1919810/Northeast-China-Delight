package com.gunmu.northeast_china_delight.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DryingRecipes {

    public static final int MAX_RESULTS = 2;

    public record Recipe(ResourceLocation id, int priority, Ingredient input, List<ItemStack> results) {

        public Recipe {
            results = results.stream().map(ItemStack::copy).toList();
        }

        public Recipe withId(ResourceLocation id) {
            return new Recipe(id, this.priority, this.input, this.results);
        }

        public List<ItemStack> process(ItemStack stack) {
            List<ItemStack> outputs = new ArrayList<>();
            if (!stack.isEmpty() && this.input.test(stack)) {
                for (ItemStack result : this.results) {
                    outputs.addAll(multipliedOutput(stack, result));
                }
            }
            return outputs;
        }
    }

    /**
     * 一堆进去出多少：每个输入出一份 result，按最大堆叠数分成几堆（和 Create 的 ItemHelper.multipliedOutput 一样；
     * 自己写一份，这个类不再引用 Create 的类，机械动力是可选前置）
     */
    static List<ItemStack> multipliedOutput(ItemStack in, ItemStack out) {
        List<ItemStack> stacks = new ArrayList<>();
        int total = in.getCount() * out.getCount();
        int max = Math.max(1, out.getMaxStackSize());
        while (total > 0) {
            int n = Math.min(max, total);
            stacks.add(out.copyWithCount(n));
            total -= n;
        }
        return stacks;
    }

    private DryingRecipes() {
    }

    public static List<Recipe> all(RecipeManager manager) {
        List<Recipe> recipes = new ArrayList<>();
        //? if <1.20.5 {
        for (DryingDataRecipe entry : manager.getAllRecipesFor(ModDryingRecipes.TYPE.get())) {
            Recipe recipe = entry.rule(entry.getId());
        //?} else {
        /*for (var entry : manager.getAllRecipesFor(ModDryingRecipes.TYPE.get())) {
            Recipe recipe = entry.value().rule(entry.id());
        *///?}
            if (recipe != null) {
                recipes.add(recipe);
            }
        }
        recipes.sort(Comparator.comparingInt(Recipe::priority).reversed()
                .thenComparing(recipe -> recipe.id().toString()));
        return List.copyOf(recipes);
    }

    @Nullable
    public static Recipe byInput(RecipeManager manager, ItemStack stack) {
        if (!stack.isEmpty()) {
            for (Recipe recipe : all(manager)) {
                if (recipe.input().test(stack)) {
                    return recipe;
                }
            }
        }
        return null;
    }
}
