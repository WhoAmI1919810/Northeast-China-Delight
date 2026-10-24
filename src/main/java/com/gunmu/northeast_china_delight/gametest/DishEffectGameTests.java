package com.gunmu.northeast_china_delight.gametest;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.effect.DishEffects;
import com.gunmu.northeast_china_delight.effect.DishFlavors;
import com.gunmu.northeast_china_delight.effect.ModEffects;
import com.gunmu.northeast_china_delight.util.DdEffects;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
//? if <1.20.2 {
import net.minecraftforge.gametest.GameTestHolder;
//?} else {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
*///?}
//? if <1.20.2 {
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} else {
/*import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

import java.util.List;

/**
 * 油腻菜的缓慢：只有配方里食材多于 3 种（不算调料）的才给，其余油腻菜照常给生命提升、急迫和油腻。
 */
@GameTestHolder(NortheastChinaDelight.MODID)
@PrefixGameTestTemplate(false)
public final class DishEffectGameTests {

    private DishEffectGameTests() {
    }

    private static boolean gives(String dish, Holder<MobEffect> effect) {
        Item item = BuiltInRegistries.ITEM.get(DdIds.of(NortheastChinaDelight.MODID, dish));
        DishFlavors.Info info = DishFlavors.of(item);
        return info != null && DishEffects.of(info).stream()
                .anyMatch(applied -> applied.effect().value() == effect.value());
    }

    @GameTest(template = "empty")
    public static void onlyGreasyDishesWithManyIngredientsSlow(GameTestHelper helper) {
        Holder<MobEffect> slowness = DdEffects.hold(MobEffects.MOVEMENT_SLOWDOWN);
        for (String dish : List.of("smoked_meat_platter", "slaughter_feast_bowl",
                "ground_pot_chicken_bowl", "ground_pot_ribs_bowl")) {
            helper.assertTrue(gives(dish, slowness), "食材多于 3 种的油腻菜应保留缓慢：" + dish);
        }
        for (String dish : List.of("three_fresh_veggies", "old_style_crispy_pork", "braised_pork_ribs",
                "kimchi_pancake", "snowy_bean_paste", "grilled_chicken_frame", "braised_beef_sauce")) {
            helper.assertTrue(!gives(dish, slowness), "食材不多于 3 种的油腻菜不应缓慢：" + dish);
            helper.assertTrue(gives(dish, DdEffects.hold(MobEffects.HEALTH_BOOST))
                            && gives(dish, DdEffects.hold(ModEffects.GREASY)),
                    "去掉缓慢后其余油腻效果应照常：" + dish);
        }
        helper.succeed();
    }
}
