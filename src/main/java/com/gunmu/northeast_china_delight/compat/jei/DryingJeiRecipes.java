package com.gunmu.northeast_china_delight.compat.jei;

import com.gunmu.northeast_china_delight.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** 「批量风干」JEI 配方的清单，和游戏里 {@code ModFanProcessingTypes.DryingType} 的逻辑一一对应。 */
public final class DryingJeiRecipes {

    private DryingJeiRecipes() {}

    public static List<DryingJeiRecipe> all() {
        // 碗装豆浆 → 干豆腐 + 空碗。200 tick ≈ 10 秒，参照 Create 默认 fanProcessingTime。
        return List.of(
                new DryingJeiRecipe(
                        List.of(new ItemStack(ModItems.SOY_MILK.get())),
                        List.of(new ItemStack(ModItems.DRIED_TOFU.get()), new ItemStack(Items.BOWL)),
                        200
                )
        );
    }
}
