package com.gunmu.northeast_china_delight.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 「大锅烹饪」配方的查询：列全部配方、某样东西能不能下锅、锅里的东西凑成哪条配方。 */
public final class BigPotRecipes {

    private BigPotRecipes() {
    }

    public static List<BigPotRecipe> all(RecipeManager manager) {
        List<BigPotRecipe> recipes = new ArrayList<>();
        //? if <1.20.5 {
        for (BigPotDataRecipe entry : manager.getAllRecipesFor(ModBigPotRecipes.TYPE.get())) {
            BigPotRecipe recipe = entry.rule(entry.getId());
            if (recipe != null) {
                recipes.add(recipe);
            }
        }
        //?} else {
        /*for (var entry : manager.getAllRecipesFor(ModBigPotRecipes.TYPE.get())) {
            BigPotRecipe recipe = entry.value().rule(entry.id());
            if (recipe != null) {
                recipes.add(recipe);
            }
        }
        *///?}
        recipes.sort(Comparator.comparing(recipe -> recipe.id().toString()));
        return List.copyOf(recipes);
    }

    /** 这样东西能不能放进锅里：有哪条配方的食材或调料认它 */
    public static boolean usable(RecipeManager manager, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (BigPotRecipe recipe : all(manager)) {
            for (BigPotRecipe.Entry entry : recipe.ingredients()) {
                if (entry.ingredient().test(stack)) {
                    return true;
                }
            }
            for (BigPotRecipe.Entry entry : recipe.seasonings()) {
                if (entry.ingredient().test(stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 这样东西下锅算不算调料：有哪条配方把它写在调料里（盐、酱油、豆油……）。调料倒进去拿不出来，水舀干就跟着没，拆灶也不掉 */
    public static boolean isSeasoning(RecipeManager manager, ItemStack stack) {
        for (BigPotRecipe recipe : all(manager)) {
            for (BigPotRecipe.Entry entry : recipe.seasonings()) {
                if (entry.ingredient().test(stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 锅里最多放几个食材：用食材最多的那条配方用几个（现在是 7）。再多放也凑不成哪道菜 */
    public static int maxIngredients(RecipeManager manager) {
        int max = 1;
        for (BigPotRecipe recipe : all(manager)) {
            max = Math.max(max, total(recipe.ingredients()));
        }
        return max;
    }

    /** 锅里最多倒几份调料：用调料最多的那条配方用几份（现在是 3） */
    public static int maxSeasonings(RecipeManager manager) {
        int max = 1;
        for (BigPotRecipe recipe : all(manager)) {
            max = Math.max(max, total(recipe.seasonings()));
        }
        return max;
    }

    private static int total(List<BigPotRecipe.Entry> entries) {
        int n = 0;
        for (BigPotRecipe.Entry entry : entries) {
            n += entry.count();
        }
        return n;
    }

    /** 锅里的液体和料正好凑成哪条配方（料要一样不多一样不少、个数对上；液体的量在配方的范围里）；凑不上返回 null */
    @Nullable
    public static BigPotRecipe match(RecipeManager manager, Fluid liquid, int liquidMb, List<ItemStack> contents) {
        for (BigPotRecipe recipe : all(manager)) {
            if (matches(recipe, liquid, liquidMb, contents)) {
                return recipe;
            }
        }
        return null;
    }

    public static boolean matches(BigPotRecipe recipe, Fluid liquid, int liquidMb, List<ItemStack> contents) {
        if (recipe.liquid() == null) {
            if (liquidMb > 0) {
                return false;
            }
        } else if (!recipe.liquid().accepts(liquidMb) || !recipe.liquid().test(liquid)) {
            return false;
        }
        List<BigPotRecipe.Entry> entries = new ArrayList<>(recipe.ingredients());
        entries.addAll(recipe.seasonings());
        if (entries.size() != contents.size()) {
            return false;
        }
        return assign(entries, contents, 0, new boolean[contents.size()]);
    }

    /** 每一格配方找一堆没用过、认得上、个数正好的料（最多 9 格，回溯就够） */
    private static boolean assign(List<BigPotRecipe.Entry> entries, List<ItemStack> contents, int index, boolean[] used) {
        if (index == entries.size()) {
            return true;
        }
        BigPotRecipe.Entry entry = entries.get(index);
        for (int i = 0; i < contents.size(); i++) {
            ItemStack stack = contents.get(i);
            if (used[i] || stack.getCount() != entry.count() || !entry.ingredient().test(stack)) {
                continue;
            }
            used[i] = true;
            if (assign(entries, contents, index + 1, used)) {
                return true;
            }
            used[i] = false;
        }
        return false;
    }
}
