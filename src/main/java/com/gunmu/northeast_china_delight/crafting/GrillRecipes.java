package com.gunmu.northeast_china_delight.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class GrillRecipes {

    public static final int MAX_SEASONINGS = 4;

    /** {@code visuals} 与 {@code seasonings} 一一对应：第 i 份调料加上去之后的样子 */
    public record Recipe(ResourceLocation id, int priority, Ingredient input, List<Ingredient> seasonings,
                         ItemStack result, int seconds, List<GrillVisual> visuals) {

        public Recipe {
            seasonings = List.copyOf(seasonings);
            result = result.copy();
            visuals = List.copyOf(visuals);
            if (visuals.size() != seasonings.size()) {
                throw new IllegalArgumentException("烤架调料视觉必须和调料一一对应");
            }
        }

        public Recipe(ResourceLocation id, int priority, Ingredient input, List<Ingredient> seasonings,
                      ItemStack result, int seconds) {
            this(id, priority, input, seasonings, result, seconds,
                    Collections.nCopies(seasonings.size(), GrillVisual.AUTO));
        }

        public Recipe withId(ResourceLocation id) {
            return new Recipe(id, this.priority, this.input, this.seasonings, this.result, this.seconds, this.visuals);
        }

        public int ticks() {
            return this.seconds * 20;
        }

        private int matchedSeasonings(List<Item> applied) {
            int[] assigned = assign(applied);
            int matched = 0;
            for (int appliedIndex : assigned) {
                if (appliedIndex >= 0) {
                    matched++;
                }
            }
            return matched;
        }

        /** 每份调料条目分到了第几份已加的调料（-1 = 还没加） */
        private int[] assign(List<Item> applied) {
            int[] assigned = new int[this.seasonings.size()];
            Arrays.fill(assigned, -1);
            for (int appliedIndex = 0; appliedIndex < applied.size(); appliedIndex++) {
                assignSeasoning(appliedIndex, applied, assigned, new boolean[assigned.length]);
            }
            return assigned;
        }

        /** 已加的每份调料按所占的调料条目取视觉；多出来、没占到条目的按 AUTO */
        public List<GrillVisual> visualsFor(List<Item> applied) {
            GrillVisual[] visuals = new GrillVisual[applied.size()];
            Arrays.fill(visuals, GrillVisual.AUTO);
            int[] assigned = assign(applied);
            for (int requiredIndex = 0; requiredIndex < assigned.length; requiredIndex++) {
                if (assigned[requiredIndex] >= 0) {
                    visuals[assigned[requiredIndex]] = this.visuals.get(requiredIndex);
                }
            }
            return Arrays.asList(visuals);
        }

        private boolean assignSeasoning(int appliedIndex, List<Item> applied, int[] assigned, boolean[] visited) {
            ItemStack probe = new ItemStack(applied.get(appliedIndex));
            for (int requiredIndex = 0; requiredIndex < this.seasonings.size(); requiredIndex++) {
                if (!visited[requiredIndex] && this.seasonings.get(requiredIndex).test(probe)) {
                    visited[requiredIndex] = true;
                    if (assigned[requiredIndex] < 0
                            || assignSeasoning(assigned[requiredIndex], applied, assigned, visited)) {
                        assigned[requiredIndex] = appliedIndex;
                        return true;
                    }
                }
            }
            return false;
        }

        public boolean seasoningsReady(List<Item> applied) {
            return this.matchedSeasonings(applied) == this.seasonings.size();
        }

        public boolean needsSeasoning(List<Item> applied, ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            List<Item> candidate = new ArrayList<>(applied);
            candidate.add(stack.getItem());
            return this.matchedSeasonings(candidate) > this.matchedSeasonings(applied);
        }
    }

    private GrillRecipes() {
    }

    public static List<Recipe> all(RecipeManager manager) {
        List<Recipe> recipes = new ArrayList<>();
        //? if <1.20.5 {
        /*for (GrillDataRecipe entry : manager.getAllRecipesFor(ModGrillRecipes.TYPE.get())) {
            Recipe recipe = entry.rule(entry.getId());
        *///?} else {
        for (var entry : manager.getAllRecipesFor(ModGrillRecipes.TYPE.get())) {
            Recipe recipe = entry.value().rule(entry.id());
        //?}
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
        if (stack.isEmpty()) {
            return null;
        }
        for (Recipe recipe : all(manager)) {
            if (recipe.input().test(stack)) {
                return recipe;
            }
        }
        return null;
    }

    @Nullable
    public static Recipe byId(RecipeManager manager, ResourceLocation id) {
        //? if <1.20.5 {
        /*var loaded = manager.byKey(id).orElse(null);
        *///?} else {
        var loaded = manager.byKey(id).map(entry -> entry.value()).orElse(null);
        //?}
        return loaded instanceof GrillDataRecipe recipe ? recipe.rule(id) : null;
    }
}
