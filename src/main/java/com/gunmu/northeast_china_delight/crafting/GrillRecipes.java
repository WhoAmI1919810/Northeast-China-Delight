package com.gunmu.northeast_china_delight.crafting;

import com.gunmu.northeast_china_delight.item.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 烧烤架的全部配方。
 *
 * 和大缸那边一样先写在代码里，结构已经和将来的数据包配方一一对应。
 * 一条配方 = 一份食材 + 若干种调料 → 一份成品。
 *
 * 加配方只要在 {@link #build()} 里加一行。
 */
public final class GrillRecipes {

    /**
     * 一条烧烤配方。
     *
     * @param input     食材（支持标签，所以「任意生鱼」这类写法也能用）
     * @param seasonings 需要的调料，按种类算，每样一份
     * @param result    成品
     * @param seconds   烤制秒数
     */
    public record Recipe(Ingredient input, List<Item> seasonings, ItemStack result, int seconds) {

        /** 换算成游戏刻 */
        public int ticks() {
            return Math.max(1, this.seconds * 20);
        }

        /**
         * 调料是否已经配齐。
         *
         * 已刷上去的调料按种类数一遍，
         * 所以同一道菜要求两份同样的调料也能正常工作。
         */
        public boolean seasoningsReady(List<Item> applied) {
            Map<Item, Integer> needed = new HashMap<>();
            for (Item seasoning : this.seasonings) {
                needed.merge(seasoning, 1, Integer::sum);
            }
            for (Item item : applied) {
                needed.computeIfPresent(item, (key, left) -> left - 1);
            }
            needed.values().removeIf(left -> left <= 0);
            return needed.isEmpty();
        }

        /** 还需要这份调料吗（用来判断玩家手里的东西能不能放上去） */
        public boolean needsSeasoning(List<Item> applied, ItemStack stack) {
            int required = 0;
            for (Item seasoning : this.seasonings) {
                if (seasoning == stack.getItem()) {
                    required++;
                }
            }
            if (required == 0) {
                return false;
            }
            int already = 0;
            for (Item item : applied) {
                if (item == stack.getItem()) {
                    already++;
                }
            }
            return already < required;
        }
    }

    private static List<Recipe> recipes;

    private GrillRecipes() {
    }

    public static List<Recipe> all() {
        if (recipes == null) {
            recipes = build();
        }
        return recipes;
    }

    /** 按食材找配方 */
    @Nullable
    public static Recipe byInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (Recipe recipe : all()) {
            if (recipe.input().test(stack)) {
                return recipe;
            }
        }
        return null;
    }

    private static List<Recipe> build() {
        List<Recipe> list = new ArrayList<>();

        // 烤鸡架：生鸡架刷辣椒油、撒糖
        list.add(new Recipe(
                Ingredient.of(ModItems.CHICKEN_FRAME.get()),
                List.of(ModItems.CHILI_OIL.get(), Items.SUGAR),
                new ItemStack(ModItems.GRILLED_CHICKEN_FRAME.get()),
                20));

        // 烤玉米：整根玉米撒糖
        list.add(new Recipe(
                Ingredient.of(ModItems.CORN.get()),
                List.of(Items.SUGAR),
                new ItemStack(ModItems.GRILLED_CORN.get()),
                20));

        // 烤油边：油边撒盐、刷辣椒油
        list.add(new Recipe(
                Ingredient.of(ModItems.OIL_EDGE.get()),
                List.of(ModItems.SALT.get(), ModItems.CHILI_OIL.get()),
                new ItemStack(ModItems.GRILLED_OIL_EDGE.get()),
                25));

        // 烤冷面：冷面片打鸡蛋、刷辣椒酱，再撒糖、淋醋（甜酸口才像街边那味儿）
        list.add(new Recipe(
                Ingredient.of(ModItems.COLD_NOODLE_SHEET.get()),
                List.of(ModItems.CHILI_SAUCE.get(), Items.EGG, Items.SUGAR, ModItems.VINEGAR.get()),
                new ItemStack(ModItems.GRILLED_COLD_NOODLES.get()),
                30));

        return List.copyOf(list);
    }
}
