package com.gunmu.northeast_china_delight.compat.jei;

import com.gunmu.northeast_china_delight.crafting.PoundingRecipes;
import com.gunmu.northeast_china_delight.item.SeasoningBottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 把 {@link PoundingRecipes} 的配方转成 JEI 展示形状。 */
public final class PoundJeiRecipes {

    private PoundJeiRecipes() {
    }

    public static List<PoundJeiRecipe> all(RecipeManager manager) {
        List<PoundJeiRecipe> list = new ArrayList<>();
        for (PoundingRecipes.Recipe recipe : PoundingRecipes.all(manager)) {
            List<PoundJeiRecipe.Input> inputs = new ArrayList<>();
            for (PoundingRecipes.Input input : recipe.inputs()) {
                List<ItemStack> items = Arrays.stream(input.ingredient().getItems())
                        .map(ItemStack::copy)
                        .peek(stack -> stack.setCount(input.count()))
                        .toList();
                inputs.add(new PoundJeiRecipe.Input(items, input.count()));
            }

            List<ItemStack> tool = Arrays.stream(recipe.tool().getItems())
                    .map(ItemStack::copy).toList();

            List<PoundJeiRecipe.Output> outputs = new ArrayList<>();
            Map<String, Integer> poolWeights = new HashMap<>();
            for (PoundingRecipes.Result result : recipe.results()) {
                if (result.pooled()) {
                    poolWeights.merge(result.pool(), result.weight(), Integer::sum);
                }
            }
            for (PoundingRecipes.Result result : recipe.results()) {
                ItemStack stack = result.stack().copy();
                stack.setCount(Math.min(result.maxCount(), Math.max(1, stack.getMaxStackSize())));
                if (recipe.resultMb() > 0) {
                    // 按毫升装的显示成只装了这么多的瓶子，耐久条一看就知道
                    stack.setDamageValue(SeasoningBottleItem.CAPACITY_MB - recipe.resultMb());
                }
                outputs.add(new PoundJeiRecipe.Output(stack, result.minCount(), result.maxCount(),
                        result.chance(), result.byproduct(), result.pool(), result.weight(),
                        result.pooled() ? poolWeights.getOrDefault(result.pool(), result.weight()) : 0,
                        result.poolChance()));
            }

            list.add(new PoundJeiRecipe(recipe.id(), List.copyOf(inputs), tool, recipe.poundTimes(),
                    recipe.chargeThreshold(), List.copyOf(outputs), recipe.stageModels(), recipe.stageModelMode(),
                    recipe.resultContainer(), recipe.resultMb()));
        }
        return List.copyOf(list);
    }
}
