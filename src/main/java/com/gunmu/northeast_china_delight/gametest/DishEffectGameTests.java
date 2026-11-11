package com.gunmu.northeast_china_delight.gametest;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.effect.DishEffects;
import com.gunmu.northeast_china_delight.effect.DishFlavors;
import com.gunmu.chinese_regional_culinary_ingredients.effect.CrciMobEffects;
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
/*import net.minecraftforge.gametest.GameTestHolder;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

import java.util.List;

/**
 * 油腻菜的缓慢：只有配方里食材多于 3 种（不算调料）的才给，其余油腻菜照常给生命提升、急迫和油腻。
 * 大盆菜碗装的那一份：90 秒，生命提升 II、急迫 I，再加油腻 II 或滋养 I，不缓慢；别的菜生命提升只给 I。
 */
@GameTestHolder(NortheastChinaDelight.MODID)
@PrefixGameTestTemplate(false)
public final class DishEffectGameTests {

    private DishEffectGameTests() {
    }

    private static boolean gives(String dish, Holder<MobEffect> effect) {
        return find(dish, effect) != null;
    }

    private static DishEffects.Applied find(String dish, Holder<MobEffect> effect) {
        Item item = BuiltInRegistries.ITEM.get(DdIds.of(NortheastChinaDelight.MODID, dish));
        DishFlavors.Info info = DishFlavors.of(item);
        if (info == null) {
            return null;
        }
        return DishEffects.of(info).stream()
                .filter(applied -> applied.effect().value() == effect.value())
                .findFirst().orElse(null);
    }

    @GameTest(template = "empty")
    public static void onlyGreasyDishesWithManyIngredientsSlow(GameTestHelper helper) {
        Holder<MobEffect> slowness = DdEffects.hold(MobEffects.MOVEMENT_SLOWDOWN);
        for (String dish : List.of("smoked_meat_platter")) {
            helper.assertTrue(gives(dish, slowness), "食材多于 3 种的油腻菜应保留缓慢：" + dish);
        }
        for (String dish : List.of("three_fresh_veggies", "old_style_crispy_pork", "braised_pork_ribs",
                "kimchi_pancake", "snowy_bean_paste", "grilled_chicken_frame", "braised_beef_sauce")) {
            helper.assertTrue(!gives(dish, slowness), "食材不多于 3 种的油腻菜不应缓慢：" + dish);
            helper.assertTrue(gives(dish, DdEffects.hold(MobEffects.HEALTH_BOOST))
                            && gives(dish, DdEffects.hold(CrciMobEffects.GREASY)),
                    "去掉缓慢后其余油腻效果应照常：" + dish);
            helper.assertTrue(find(dish, DdEffects.hold(MobEffects.HEALTH_BOOST)).amplifier() == 0,
                    "不是大盆菜的生命提升只给 I：" + dish);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void feastBowlsGiveHealthBoostTwoForNinetySeconds(GameTestHelper helper) {
        Holder<MobEffect> healthBoost = DdEffects.hold(MobEffects.HEALTH_BOOST);
        Holder<MobEffect> haste = DdEffects.hold(MobEffects.DIG_SPEED);
        Holder<MobEffect> greasy = DdEffects.hold(CrciMobEffects.GREASY);
        for (String dish : List.of("harvest_stew_bowl", "slaughter_feast_bowl", "ground_pot_chicken_bowl",
                "ground_pot_ribs_bowl", "pork_vermicelli_stew_bowl", "chicken_mushroom_stew_bowl",
                "sauerkraut_seafood_stew_bowl", "sauerkraut_bone_stew_bowl", "demoli_fish_stew_bowl")) {
            DishEffects.Applied boost = find(dish, healthBoost);
            helper.assertTrue(boost != null && boost.amplifier() == 1 && boost.durationTicks() == 1800,
                    "大盆菜应给 90 秒生命提升 II：" + dish);
            helper.assertTrue(gives(dish, haste), "大盆菜应给急迫：" + dish);
            helper.assertTrue(!gives(dish, DdEffects.hold(MobEffects.MOVEMENT_SLOWDOWN)), "大盆菜不应缓慢：" + dish);
            boolean balanced = dish.startsWith("sauerkraut_") || dish.equals("demoli_fish_stew_bowl");
            helper.assertTrue(gives(dish, greasy) != balanced,
                    "酸菜海鲜锅、酸菜炖大骨、得莫利炖鱼给滋养，其余给油腻：" + dish);
        }
        helper.succeed();
    }
}
