package com.gunmu.northeast_china_delight.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.crafting.PoundDataRecipe;
import com.gunmu.northeast_china_delight.crafting.PoundingRecipes;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
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

import java.lang.reflect.Method;
import java.util.List;

/** 舂捣数据包解析与随机池的自动化回归。 */
@GameTestHolder(NortheastChinaDelight.MODID)
@PrefixGameTestTemplate(false)
public final class PoundGameTests {

    private PoundGameTests() {
    }

    @GameTest(template = "empty")
    public static void legacyRiceCakeJsonStillLoads(GameTestHelper helper) {
        PoundingRecipes.Recipe recipe = decode(
                "{\"input\":{\"item\":\"farmersdelight:cooked_rice\"},"
                        + "\"return_container\":{\"item\":\"minecraft:bowl\"},"
                        + "\"tool\":{\"item\":\"northeast_china_delight:wooden_mallet\"},"
                        + "\"pound_times\":4,"
                        + "\"stage_models\":[\"northeast_china_delight:block/wooden_mortar_1\"],"
                        + "\"result\":{\"item\":\"northeast_china_delight:pounded_rice_cake\",\"count\":2},"
                        + "\"transition\":\"sequential\"}",
                DdIds.of(NortheastChinaDelight.MODID, "pounding/test_legacy"));
        if (recipe == null) {
            helper.fail("Legacy pounding JSON should still decode");
            return;
        }
        if (recipe.inputs().size() != 1 || recipe.inputs().get(0).count() != 1
                || recipe.inputs().get(0).returnContainer() == null
                || !recipe.inputs().get(0).returnContainer().is(Items.BOWL)) {
            helper.fail("Legacy input / return_container should become one input entry");
        }
        if (recipe.poundTimes() != 4 || recipe.results().size() != 1
                || recipe.results().get(0).minCount() != 2
                || recipe.results().get(0).maxCount() != 2) {
            helper.fail("Legacy result should stay a fixed 2-item output");
        }
        if (Math.abs(recipe.chargeThreshold() - 0.9F) > 0.0001F) {
            helper.fail("Legacy recipe should keep the default 90% charge threshold");
        }
        if (recipe.stageModelMode() != PoundingRecipes.StageModelMode.FULL || recipe.finishedModel() != null
                || !recipe.stageModel(recipe.doneState()).equals(recipe.stageModels().get(0))) {
            helper.fail("Without stage_model_mode / finished_model the stage models stay whole mortars");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void overlayStageModelsAndFinishedModelDecode(GameTestHelper helper) {
        PoundingRecipes.Recipe recipe = decode(
                "{\"input\":{\"item\":\"farmersdelight:cooked_rice\"},"
                        + "\"tool\":{\"item\":\"northeast_china_delight:wooden_mallet\"},"
                        + "\"pound_times\":2,\"stage_model_mode\":\"overlay\","
                        + "\"stage_models\":[\"northeast_china_delight:block/wooden_mortar_rice_cake_0\","
                        + "\"northeast_china_delight:block/wooden_mortar_rice_cake_1\"],"
                        + "\"finished_model\":\"northeast_china_delight:block/wooden_mortar_rice_cake_done\","
                        + "\"result\":{\"item\":\"northeast_china_delight:pounded_rice_cake\",\"count\":2},"
                        + "\"transition\":\"sequential\"}",
                DdIds.of(NortheastChinaDelight.MODID, "pounding/test_overlay"));
        if (recipe == null) {
            helper.fail("Overlay pounding JSON should decode");
            return;
        }
        ResourceLocation first = DdIds.of(NortheastChinaDelight.MODID, "block/wooden_mortar_rice_cake_0");
        ResourceLocation second = DdIds.of(NortheastChinaDelight.MODID, "block/wooden_mortar_rice_cake_1");
        ResourceLocation done = DdIds.of(NortheastChinaDelight.MODID, "block/wooden_mortar_rice_cake_done");
        if (recipe.stageModelMode() != PoundingRecipes.StageModelMode.OVERLAY || recipe.doneState() != 3
                || !recipe.stageModel(1).equals(first) || !recipe.stageModel(2).equals(second)
                || !recipe.stageModel(3).equals(done)) {
            helper.fail("Overlay mode should keep stage order and use finished_model only for the done state");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void resultContainerDecodes(GameTestHelper helper) {
        PoundingRecipes.Recipe recipe = decode(
                "{\"input\":{\"item\":\"northeast_china_delight:roasted_peanuts\"},"
                        + "\"tool\":{\"item\":\"northeast_china_delight:wooden_mallet\"},"
                        + "\"pound_times\":3,"
                        + "\"result\":{\"item\":\"northeast_china_delight:cooking_oil\",\"count\":1},"
                        + "\"result_mb\":50,"
                        + "\"result_container\":{\"item\":\"minecraft:glass_bottle\"}}",
                DdIds.of(NortheastChinaDelight.MODID, "pounding/test_result_container"));
        if (recipe == null || recipe.resultContainer() == null
                || !recipe.resultContainer().is(Items.GLASS_BOTTLE) || recipe.resultContainer().getCount() != 1
                || recipe.resultMb() != 50 || !recipe.liquidProduct().is(ModItems.COOKING_OIL.get())) {
            helper.fail("result_container / result_mb should decode to 50 mB of cooking oil in a glass bottle");
            return;
        }
        boolean rejected = false;
        try {
            decode("{\"input\":{\"item\":\"northeast_china_delight:roasted_peanuts\"},"
                            + "\"result\":{\"item\":\"northeast_china_delight:cooking_oil\",\"count\":1},"
                            + "\"result_mb\":50}",
                    DdIds.of(NortheastChinaDelight.MODID, "pounding/test_mb_without_container"));
        } catch (RuntimeException expected) {
            rejected = true;
        }
        if (!rejected) {
            helper.fail("result_mb without result_container must be rejected");
            return;
        }
        PoundingRecipes.Recipe legacy = decode(
                "{\"input\":{\"item\":\"farmersdelight:cooked_rice\"},"
                        + "\"result\":{\"item\":\"northeast_china_delight:pounded_rice_cake\",\"count\":2}}",
                DdIds.of(NortheastChinaDelight.MODID, "pounding/test_no_container"));
        if (legacy == null || legacy.resultContainer() != null) {
            helper.fail("Recipes without result_container can be taken out bare-handed");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void extendedJsonParsesAllOutputForms(GameTestHelper helper) {
        PoundingRecipes.Recipe recipe = decode(
                "{\"inputs\":["
                        + "{\"ingredient\":{\"item\":\"minecraft:sugar\"},\"count\":2,"
                        + "\"return_container\":{\"item\":\"minecraft:glass_bottle\"}},"
                        + "{\"ingredient\":{\"item\":\"minecraft:egg\"},\"count\":1}],"
                        + "\"tool\":{\"item\":\"northeast_china_delight:wooden_mallet\"},"
                        + "\"pound_times\":2,\"charge_threshold\":0.5,"
                        + "\"stage_models\":[\"northeast_china_delight:block/wooden_mortar_1\","
                        + "\"northeast_china_delight:block/wooden_mortar_2\"],"
                        + "\"results\":["
                        + "{\"item\":\"northeast_china_delight:pounded_rice_cake\",\"count\":2},"
                        + "{\"item\":\"minecraft:sugar\",\"min_count\":1,\"max_count\":3,"
                        + "\"chance\":0.5,\"byproduct\":true}],"
                        + "\"random_results\":[{\"id\":\"bonus\",\"chance\":0.5,\"byproduct\":true,"
                        + "\"entries\":[{\"item\":\"minecraft:egg\",\"weight\":3,\"count\":1},"
                        + "{\"item\":\"minecraft:wheat\",\"weight\":1,\"min_count\":2,\"max_count\":4}]}],"
                        + "\"transition\":\"sequential\"}",
                DdIds.of(NortheastChinaDelight.MODID, "pounding/test_extended"));
        if (recipe == null) {
            helper.fail("Extended pounding JSON should decode");
            return;
        }
        if (recipe.inputs().size() != 2 || recipe.inputs().get(0).count() != 2
                || recipe.inputs().get(0).returnContainer() == null
                || !recipe.inputs().get(0).returnContainer().is(Items.GLASS_BOTTLE)) {
            helper.fail("Multi-input counts and per-input containers should survive decoding");
        }
        if (recipe.poundTimes() != 2 || Math.abs(recipe.chargeThreshold() - 0.5F) > 0.0001F
                || recipe.stageModels().size() != 2 || recipe.doneState() != 3) {
            helper.fail("Pound times / charge threshold / stage models should survive decoding");
        }
        if (recipe.results().size() != 4) {
            helper.fail("Two normal results plus two pool entries should be decoded");
        }
        PoundingRecipes.Result poolEgg = recipe.results().get(2);
        PoundingRecipes.Result poolWheat = recipe.results().get(3);
        if (!poolEgg.pooled() || !"bonus".equals(poolEgg.pool()) || poolEgg.weight() != 3
                || !poolEgg.byproduct() || Math.abs(poolEgg.poolChance() - 0.5F) > 0.0001F
                || poolWheat.weight() != 1 || poolWheat.minCount() != 2 || poolWheat.maxCount() != 4) {
            helper.fail("Random pool entries should keep id, weight, chance, range and byproduct flag");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void randomPoolPicksExactlyOneEntry(GameTestHelper helper) {
        PoundingRecipes.Recipe recipe = new PoundingRecipes.Recipe(
                DdIds.of(NortheastChinaDelight.MODID, "pounding/test_pool"),
                0,
                List.of(new PoundingRecipes.Input(Ingredient.of(Items.SUGAR), 1, null)),
                Ingredient.of(Items.STICK),
                1,
                0.9F,
                List.of(DdIds.of(NortheastChinaDelight.MODID, "block/wooden_mortar_1")),
                List.of(
                        new PoundingRecipes.Result(new ItemStack(Items.PAPER), 1, 1, 1.0F, false),
                        new PoundingRecipes.Result(new ItemStack(Items.SUGAR), 1, 1, 1.0F, false,
                                "bonus", 3, 1.0F),
                        new PoundingRecipes.Result(new ItemStack(Items.EGG), 2, 4, 1.0F, false,
                                "bonus", 1, 1.0F)),
                PoundingRecipes.Transition.SEQUENTIAL);
        RandomSource random = RandomSource.create(20261017L);
        int eggs = 0;
        for (int i = 0; i < 300; i++) {
            List<ItemStack> rolled = recipe.rollResults(random);
            if (rolled.size() != 2 || !rolled.get(0).is(Items.PAPER)) {
                helper.fail("Pool roll must produce the guaranteed item plus exactly one pool entry");
                return;
            }
            ItemStack bonus = rolled.get(1);
            if (bonus.is(Items.EGG)) {
                eggs++;
                if (bonus.getCount() < 2 || bonus.getCount() > 4) {
                    helper.fail("Random pool count must stay inside min_count..max_count");
                    return;
                }
            } else if (!bonus.is(Items.SUGAR)) {
                helper.fail("Pool roll produced an item outside the pool");
                return;
            }
        }
        if (eggs == 0 || eggs == 300) {
            helper.fail("Weighted pool should select both entries over 300 deterministic rolls");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void zeroChancePoolNeverDrops(GameTestHelper helper) {
        PoundingRecipes.Recipe recipe = new PoundingRecipes.Recipe(
                DdIds.of(NortheastChinaDelight.MODID, "pounding/test_pool_zero"),
                0,
                List.of(new PoundingRecipes.Input(Ingredient.of(Items.SUGAR), 1, null)),
                Ingredient.of(Items.STICK),
                1,
                0.9F,
                List.of(DdIds.of(NortheastChinaDelight.MODID, "block/wooden_mortar_1")),
                List.of(
                        new PoundingRecipes.Result(new ItemStack(Items.PAPER), 1, 1, 1.0F, false),
                        new PoundingRecipes.Result(new ItemStack(Items.EGG), 1, 1, 1.0F, false,
                                "bonus", 1, 0.0F)),
                PoundingRecipes.Transition.SEQUENTIAL);
        RandomSource random = RandomSource.create(7L);
        for (int i = 0; i < 50; i++) {
            List<ItemStack> rolled = recipe.rollResults(random);
            if (rolled.size() != 1 || !rolled.get(0).is(Items.PAPER)) {
                helper.fail("Pool with chance=0 must never produce an output");
                return;
            }
        }
        helper.succeed();
    }

    /**
     *「臼应该在玩家捶打时，才检查匹配哪一项配方」。两条配方共用熟花生：只放熟花生凑的是前一条，
     * 再放蜂蜜就改凑后一条，先放哪样都行；凑齐的那条再多放一样就放不进。
     * 后一条的 id 故意排在前面（同优先级按 id 排）：以前一放熟花生就会被它认死，前一条再也做不出来。
     */
    @GameTest(template = "empty")
    public static void depositChoosesRecipeByAllContents(GameTestHelper helper) {
        PoundingRecipes.Recipe withHoney = decode(sharedPeanutJson(true),
                DdIds.of(NortheastChinaDelight.MODID, "pounding/a_test_peanut_honey"));
        PoundingRecipes.Recipe peanutOnly = decode(sharedPeanutJson(false),
                DdIds.of(NortheastChinaDelight.MODID, "pounding/b_test_peanut_only"));
        if (withHoney == null || peanutOnly == null) {
            helper.fail("Test pounding JSON should decode");
            return;
        }
        List<PoundingRecipes.Recipe> recipes = List.of(withHoney, peanutOnly);
        ItemStack peanut = new ItemStack(ModItems.ROASTED_PEANUTS.get());
        ItemStack honey = new ItemStack(Items.HONEY_BOTTLE);

        PoundingRecipes.Fit onlyPeanut = PoundingRecipes.choose(recipes, List.of(peanut));
        helper.assertTrue(onlyPeanut != null && onlyPeanut.complete() && onlyPeanut.recipe() == peanutOnly,
                "只放熟花生应凑齐只要熟花生的那条");
        PoundingRecipes.Fit onlyHoney = PoundingRecipes.choose(recipes, List.of(honey));
        helper.assertTrue(onlyHoney != null && !onlyHoney.complete() && onlyHoney.recipe() == withHoney,
                "只放蜂蜜应算熟花生 + 蜂蜜那条、还没凑齐");
        for (List<ItemStack> both : List.of(List.of(peanut, honey), List.of(honey, peanut))) {
            PoundingRecipes.Fit fit = PoundingRecipes.choose(recipes, both);
            helper.assertTrue(fit != null && fit.complete() && fit.recipe() == withHoney,
                    "熟花生和蜂蜜都放了，不管先后都应凑齐熟花生 + 蜂蜜那条");
        }
        helper.assertTrue(PoundingRecipes.choose(recipes, List.of(peanut, new ItemStack(Items.COBBLESTONE))) == null,
                "放了哪条配方都不要的东西应一条也凑不上");

        helper.assertTrue(PoundingRecipes.room(recipes, List.of(), peanut.copyWithCount(5)) == 1,
                "两条都只要一个熟花生，一次只放一个");
        helper.assertTrue(PoundingRecipes.room(recipes, List.of(peanut), peanut.copyWithCount(5)) == 0,
                "已经有一个熟花生，两条都不收第二个");
        helper.assertTrue(PoundingRecipes.room(recipes, List.of(peanut), honey.copyWithCount(3)) == 1,
                "凑齐只要熟花生那条以后还能放蜂蜜");
        helper.succeed();
    }

    /**：配方写明哪样投料是液体（liquid），成品是液体写 finished_liquid；一份水不写 liquid 也算水 */
    @GameTest(template = "empty")
    public static void liquidFieldsDecode(GameTestHelper helper) {
        PoundingRecipes.Recipe recipe = decode(
                "{\"inputs\":["
                        + "{\"ingredient\":{\"item\":\"minecraft:honey_bottle\"},\"count\":1,"
                        + "\"liquid\":{\"fluid\":\"create:honey\",\"texture\":\"northeast_china_delight:block/honey_still\",\"color\":\"#80FF8800\"}},"
                        + "{\"water\":true,\"count\":1},"
                        + "{\"ingredient\":{\"item\":\"minecraft:sugar\"},\"count\":1,\"liquid\":\"minecraft:lava\"},"
                        + "{\"ingredient\":{\"item\":\"minecraft:wheat\"},\"count\":1}],"
                        + "\"tool\":{\"item\":\"northeast_china_delight:wooden_mallet\"},"
                        + "\"stage_models\":[\"northeast_china_delight:block/wooden_mortar_1\"],"
                        + "\"finished_liquid\":\"northeast_china_delight:vegetable_oil\","
                        + "\"result\":{\"item\":\"minecraft:paper\",\"count\":1}}",
                DdIds.of(NortheastChinaDelight.MODID, "pounding/test_liquid"));
        if (recipe == null) {
            helper.fail("带 liquid 的配方应能解析");
            return;
        }
        PoundingRecipes.Liquid honey = recipe.inputs().get(0).liquid();
        helper.assertTrue(honey != null && honey.fluid() != null && honey.fluid().getPath().equals("honey")
                        && honey.texture() != null && honey.color() == 0x80FF8800,
                "对象写法：fluid、texture、color 都要读到");
        PoundingRecipes.Liquid water = recipe.inputs().get(1).liquid();
        helper.assertTrue(water != null && water.fluid() != null && water.fluid().getPath().equals("water"),
                "一份水不写 liquid 也应算水");
        PoundingRecipes.Liquid lava = recipe.inputs().get(2).liquid();
        helper.assertTrue(lava != null && lava.fluid() != null && lava.fluid().getPath().equals("lava") && lava.color() == -1,
                "字符串写法就是流体 id，不染色");
        helper.assertTrue(recipe.inputs().get(3).liquid() == null, "没写 liquid 的不是液体");
        helper.assertTrue(recipe.finishedLiquid() != null
                        && recipe.finishedLiquid().fluid().getPath().equals("vegetable_oil"),
                "finished_liquid 应读到");
        helper.assertTrue(recipe.depositedLiquid(i -> i == 2 ? 1 : 0) == lava,
                "只放了第三样时画的应是第三样的液面");
        helper.succeed();
    }

    private static String sharedPeanutJson(boolean withHoney) {
        String honey = withHoney ? ",{\"ingredient\":{\"item\":\"minecraft:honey_bottle\"},\"count\":1}" : "";
        return "{\"inputs\":[{\"ingredient\":{\"item\":\"northeast_china_delight:roasted_peanuts\"},\"count\":1}"
                + honey + "],"
                + "\"tool\":{\"item\":\"northeast_china_delight:wooden_mallet\"},"
                + "\"pound_times\":3,"
                + "\"stage_models\":[\"northeast_china_delight:block/wooden_mortar_1\"],"
                + "\"result\":{\"item\":\"minecraft:paper\",\"count\":1},"
                + "\"transition\":\"sequential\"}";
    }

    private static PoundingRecipes.Recipe decode(String json, ResourceLocation id) {
        try {
            Method method = PoundDataRecipe.class.getDeclaredMethod("decode", JsonObject.class,
                    ResourceLocation.class);
            method.setAccessible(true);
            return (PoundingRecipes.Recipe) method.invoke(null,
                    JsonParser.parseString(json).getAsJsonObject(), id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to decode pounding JSON in game test", exception);
        }
    }
}
