package com.gunmu.northeast_china_delight.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.ModTags;
import com.gunmu.northeast_china_delight.block.GrillBlockEntity;
import com.gunmu.northeast_china_delight.block.GrillCampfireBlock;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.compat.jei.GrillJeiRecipe;
import com.gunmu.northeast_china_delight.compat.jei.GrillJeiRecipes;
import com.gunmu.northeast_china_delight.crafting.GrillDataRecipe;
import com.gunmu.northeast_china_delight.crafting.GrillRecipes;
import com.gunmu.northeast_china_delight.crafting.ModGrillRecipes;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.util.DdIds;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.phys.AABB;
//? if <1.20.5 {
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.crafting.Recipe;
//?} else {
/*import com.mojang.serialization.JsonOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.crafting.RecipeHolder;
*///?}
//? if <1.20.2 {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} else {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@GameTestHolder(NortheastChinaDelight.MODID)
@PrefixGameTestTemplate(false)
public final class GrillGameTests {

    private static final ResourceLocation SOURCE_ID = DdIds.of(NortheastChinaDelight.MODID, "grill/test_carrot");

    private GrillGameTests() {
    }

    private static JsonObject fixture() {
        return JsonParser.parseString("""
                {"type":"northeast_china_delight:grill","config":{
                  "input":[{"item":"minecraft:carrot"},{"item":"minecraft:potato"}],
                  "seasonings":[{"item":"minecraft:sugar"}],
                  "result":{"item":"minecraft:golden_carrot","count":2},"seconds":4
                }}
                """).getAsJsonObject();
    }

    private static GrillDataRecipe decode(ResourceLocation id, JsonObject json) {
        //? if <1.20.5 {
        return ModGrillRecipes.SERIALIZER.get().fromJson(id, json);
        //?} else {
        /*return ModGrillRecipes.SERIALIZER.get().codec().codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        *///?}
    }

    private static RecipeManager manager(GameTestHelper helper) {
        //? if <1.20.5 {
        return new RecipeManager();
        //?} else {
        /*return new RecipeManager(helper.getLevel().registryAccess());
        *///?}
    }

    private static void replace(RecipeManager manager, Map<ResourceLocation, GrillDataRecipe> recipes) {
        //? if <1.20.5 {
        List<Recipe<?>> entries = new ArrayList<>(recipes.values());
        //?} else {
        /*List<RecipeHolder<?>> entries = new ArrayList<>();
        recipes.forEach((id, recipe) -> entries.add(new RecipeHolder<>(id, recipe)));
        *///?}
        manager.replaceRecipes(entries);
    }

    private static GrillBlockEntity emptyGrill() {
        return new GrillBlockEntity(BlockPos.ZERO, ModBlocks.GRILL_CAMPFIRE.get().defaultBlockState());
    }

    private static CompoundTag save(GameTestHelper helper, GrillBlockEntity grill) {
        //? if <1.20.5 {
        return grill.saveWithoutMetadata();
        //?} else {
        /*return grill.saveWithoutMetadata(helper.getLevel().registryAccess());
        *///?}
    }

    private static GrillBlockEntity load(GameTestHelper helper, CompoundTag tag) {
        GrillBlockEntity grill = emptyGrill();
        //? if <1.20.5 {
        grill.load(tag);
        //?} else {
        /*grill.loadWithComponents(tag, helper.getLevel().registryAccess());
        *///?}
        return grill;
    }

    private static GrillDataRecipe networkCopy(GameTestHelper helper, ResourceLocation id, GrillDataRecipe source) {
        //? if <1.20.5 {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        //?} else {
        /*RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        *///?}
        try {
            //? if <1.20.5 {
            ModGrillRecipes.SERIALIZER.get().toNetwork(buffer, source);
            return ModGrillRecipes.SERIALIZER.get().fromNetwork(id, buffer);
            //?} else {
            /*ModGrillRecipes.SERIALIZER.get().streamCodec().encode(buffer, source);
            return ModGrillRecipes.SERIALIZER.get().streamCodec().decode(buffer);
            *///?}
        } finally {
            buffer.release();
        }
    }

    @GameTest(template = "empty")
    public static void grillDefaultsAndJeiUseDataPackIds(GameTestHelper helper) {
        RecipeManager manager = helper.getLevel().getRecipeManager();
        List<GrillRecipes.Recipe> rules = GrillRecipes.all(manager);
        Map<String, Item> inputs = Map.of(
                "grilled_chicken_frame", ModItems.CHICKEN_FRAME.get(),
                "grilled_corn", ModItems.CORN.get(),
                "grilled_oil_edge", ModItems.OIL_EDGE.get(),
                "grilled_cold_noodles", ModItems.COLD_NOODLE_SHEET.get());
        helper.assertTrue(rules.size() == 4, "默认烤架配方应全部从 JSON 加载");
        for (var expected : inputs.entrySet()) {
            ResourceLocation id = DdIds.of(NortheastChinaDelight.MODID, "grill/" + expected.getKey());
            GrillRecipes.Recipe rule = GrillRecipes.byId(manager, id);
            int seconds = switch (expected.getKey()) {
                case "grilled_oil_edge" -> 25;
                case "grilled_cold_noodles" -> 30;
                default -> 20;
            };
            int seasonings = switch (expected.getKey()) {
                case "grilled_corn" -> 1;
                case "grilled_cold_noodles" -> 4;
                default -> 2;
            };
            helper.assertTrue(rule != null && rule.id().equals(id)
                    && rule.input().test(new ItemStack(expected.getValue()))
                    && rule.seconds() == seconds && rule.ticks() == seconds * 20
                    && rule.seasonings().size() == seasonings && rule.result().getCount() == 1,
                    "默认烧烤材料、调料份数或时长被改变：" + id);
        }
        List<GrillJeiRecipe> pages = GrillJeiRecipes.all(manager);
        helper.assertTrue(pages.size() == 4 && pages.stream().map(GrillJeiRecipe::id).distinct().count() == 4,
                "JEI 应按稳定 ID 一条配方一页");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void grillColdNoodlesAcceptAllSeasonings(GameTestHelper helper) {
        RecipeManager manager = helper.getLevel().getRecipeManager();
        GrillRecipes.Recipe rule = GrillRecipes.byId(manager,
                DdIds.of(NortheastChinaDelight.MODID, "grill/grilled_cold_noodles"));
        GrillBlockEntity grill = emptyGrill();
        grill.setFood(0, new ItemStack(ModItems.COLD_NOODLE_SHEET.get()), rule);
        for (Item seasoning : List.of(ModItems.CHILI_SAUCE.get(), Items.EGG, Items.SUGAR, ModItems.VINEGAR.get())) {
            helper.assertTrue(grill.addSeasoning(0, new ItemStack(seasoning), manager),
                    "刷辣酱之后应仍能打鸡蛋、撒糖、加醋：" + seasoning);
        }
        helper.assertTrue(rule.seasoningsReady(grill.seasoningsOf(0)), "四份调料都应被正确计数");
        helper.assertTrue(!grill.addSeasoning(0, new ItemStack(Items.SUGAR), manager), "不得多加调料");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void grillRepeatedAndOverlappingSeasonings(GameTestHelper helper) {
        JsonObject repeated = fixture();
        repeated.getAsJsonObject("config").add("seasonings",
                JsonParser.parseString("[{\"item\":\"minecraft:sugar\"},{\"item\":\"minecraft:sugar\"}]"));
        GrillRecipes.Recipe rule = decode(SOURCE_ID, repeated).rule(SOURCE_ID);
        helper.assertTrue(rule.needsSeasoning(List.of(Items.SUGAR), new ItemStack(Items.SUGAR))
                && !rule.seasoningsReady(List.of(Items.SUGAR))
                && rule.seasoningsReady(List.of(Items.SUGAR, Items.SUGAR))
                && !rule.needsSeasoning(List.of(Items.SUGAR, Items.SUGAR), new ItemStack(Items.SUGAR)),
                "重复调料条目应逐份计数，不能提前就绪或多加");
        JsonObject overlapping = fixture();
        overlapping.getAsJsonObject("config").add("seasonings", JsonParser.parseString("""
                [[{"item":"minecraft:sugar"},{"item":"minecraft:egg"}],{"item":"minecraft:sugar"}]
                """));
        GrillRecipes.Recipe overlapRule = decode(SOURCE_ID, overlapping).rule(SOURCE_ID);
        helper.assertTrue(overlapRule.needsSeasoning(List.of(Items.SUGAR), new ItemStack(Items.EGG))
                && overlapRule.seasoningsReady(List.of(Items.SUGAR, Items.EGG)),
                "标签或任选材料重叠时，也应匹配不同的调料份数");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void grillDataPacksOverrideDisableAndPrioritize(GameTestHelper helper) {
        RecipeManager manager = manager(helper);
        ResourceLocation otherId = DdIds.of(NortheastChinaDelight.MODID, "grill/z_added_carrot");
        JsonObject preferred = fixture();
        preferred.getAsJsonObject("config").addProperty("priority", 10);
        Map<ResourceLocation, GrillDataRecipe> recipes = new LinkedHashMap<>();
        recipes.put(otherId, decode(otherId, fixture()));
        recipes.put(SOURCE_ID, decode(SOURCE_ID, preferred));
        replace(manager, recipes);
        helper.assertTrue(GrillRecipes.byInput(manager, new ItemStack(Items.CARROT)).id().equals(SOURCE_ID),
                "新增配方应按 priority 降序匹配，而非加载顺序");
        preferred.getAsJsonObject("config").addProperty("seconds", 8);
        preferred.getAsJsonObject("config").getAsJsonObject("result").addProperty("item", "minecraft:baked_potato");
        recipes.put(SOURCE_ID, decode(SOURCE_ID, preferred));
        replace(manager, recipes);
        helper.assertTrue(GrillRecipes.byId(manager, SOURCE_ID).seconds() == 8
                && GrillRecipes.byId(manager, SOURCE_ID).result().is(Items.BAKED_POTATO),
                "同 ID 覆盖应更换产物与时长");
        recipes.put(SOURCE_ID, decode(SOURCE_ID, JsonParser.parseString("""
                {"type":"northeast_china_delight:grill","config":{"disabled":true}}
                """).getAsJsonObject()));
        replace(manager, recipes);
        helper.assertTrue(GrillRecipes.byId(manager, SOURCE_ID) == null && GrillRecipes.all(manager).size() == 1
                && GrillRecipes.byInput(manager, new ItemStack(Items.CARROT)).id().equals(otherId),
                "disabled 应移除规则，且不能保留硬编码回退");
        List<GrillJeiRecipe> pages = GrillJeiRecipes.all(manager);
        helper.assertTrue(pages.size() == 1 && pages.get(0).id().equals(otherId)
                && pages.get(0).ingredient().size() == 2,
                "JEI 应移除被禁用的配方，并将任选食材保留在同一格");
        recipes.put(SOURCE_ID, decode(SOURCE_ID, fixture()));
        replace(manager, recipes);
        helper.assertTrue(GrillRecipes.byInput(manager, new ItemStack(Items.POTATO)).id().equals(SOURCE_ID),
                "同优先级应按 ID 排序，且支持输入任选数组");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void grillBoundRecipeReloadDoesNotSwitchIds(GameTestHelper helper) {
        RecipeManager manager = manager(helper);
        ResourceLocation fallbackId = DdIds.of(NortheastChinaDelight.MODID, "grill/z_fallback");
        Map<ResourceLocation, GrillDataRecipe> recipes = new LinkedHashMap<>();
        recipes.put(SOURCE_ID, decode(SOURCE_ID, fixture()));
        recipes.put(fallbackId, decode(fallbackId, fixture()));
        replace(manager, recipes);
        GrillBlockEntity grill = emptyGrill();
        grill.setFood(0, new ItemStack(Items.CARROT), GrillRecipes.byId(manager, SOURCE_ID));
        helper.assertTrue(grill.addSeasoning(0, new ItemStack(Items.SUGAR), manager), "原配方应能加糖");
        grill.setProgress(0, 2);
        JsonObject changed = fixture();
        changed.getAsJsonObject("config").addProperty("seconds", 8);
        changed.getAsJsonObject("config").add("seasonings", JsonParser.parseString("""
                [{"item":"minecraft:sugar"},{"item":"minecraft:egg"}]
                """));
        recipes.put(SOURCE_ID, decode(SOURCE_ID, changed));
        replace(manager, recipes);
        GrillRecipes.Recipe updated = grill.recipeOf(0, manager);
        helper.assertTrue(updated != null && updated.id().equals(SOURCE_ID) && updated.seconds() == 8
                && grill.progress(0) == 0 && grill.seasoningsOf(0).equals(List.of(Items.SUGAR)),
                "覆盖当前配方应重新计时，并保留仍有用的调料");
        helper.assertTrue(grill.addSeasoning(0, new ItemStack(Items.EGG), manager), "覆盖后应能加新增的调料");
        grill.setProgress(0, 2);
        JsonObject disabled = JsonParser.parseString("""
                {"type":"northeast_china_delight:grill","config":{"disabled":true}}
                """).getAsJsonObject();
        recipes.put(SOURCE_ID, decode(SOURCE_ID, disabled));
        replace(manager, recipes);
        helper.assertTrue(grill.recipeOf(0, manager) == null && grill.progress(0) == 0
                && !grill.addSeasoning(0, new ItemStack(Items.SUGAR), manager)
                && grill.getItems().get(0).is(Items.CARROT),
                "禁用已绑定配方必须停止烤制和投料，但保留可取回的原料");
        recipes.remove(SOURCE_ID);
        replace(manager, recipes);
        helper.assertTrue(grill.recipeOf(0, manager) == null
                && GrillRecipes.byInput(manager, new ItemStack(Items.CARROT)).id().equals(fallbackId),
                "删除已绑定配方不能偷偷转到另一条相同输入的配方");
        JsonObject incompatible = fixture();
        incompatible.getAsJsonObject("config").add("input",
                JsonParser.parseString("{\"item\":\"minecraft:beetroot\"}"));
        recipes.put(SOURCE_ID, decode(SOURCE_ID, incompatible));
        replace(manager, recipes);
        helper.assertTrue(grill.recipeOf(0, manager) == null, "覆盖后输入不匹配时不能继续烤制");
        recipes.put(SOURCE_ID, decode(SOURCE_ID, fixture()));
        replace(manager, recipes);
        helper.assertTrue(grill.recipeOf(0, manager).id().equals(SOURCE_ID)
                && grill.seasoningsOf(0).equals(List.of(Items.SUGAR)) && grill.progress(0) == 0,
                "恢复同 ID 后应重新开始，且移除不再需要的调料");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void grillZeroProgressBindingCannotRebind(GameTestHelper helper) {
        RecipeManager manager = manager(helper);
        GrillDataRecipe source = decode(SOURCE_ID, fixture());
        replace(manager, Map.of(SOURCE_ID, source));
        GrillBlockEntity grill = emptyGrill();
        grill.setFood(0, new ItemStack(Items.CARROT), GrillRecipes.byId(manager, SOURCE_ID));
        ResourceLocation alternateId = DdIds.of(NortheastChinaDelight.MODID, "grill/alternate");
        replace(manager, Map.of(alternateId, decode(alternateId, fixture())));
        helper.assertTrue(grill.progress(0) == 0 && grill.recipeOf(0, manager) == null
                && !grill.addSeasoning(0, new ItemStack(Items.SUGAR), manager),
                "尚未计时或添加调料的已绑定食物，也不能在重载后串配方");
        grill.clearSlot(0);
        grill.setFood(0, new ItemStack(Items.CARROT), GrillRecipes.byId(manager, alternateId));
        helper.assertTrue(grill.recipeOf(0, manager).id().equals(alternateId)
                && grill.seasoningsOf(0).isEmpty(), "取回原料后重新放入，才能选择新的配方");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void grillSavePreservesIdsAndMigratesLegacySlots(GameTestHelper helper) {
        RecipeManager manager = manager(helper);
        Map<ResourceLocation, GrillDataRecipe> recipes = new LinkedHashMap<>();
        recipes.put(SOURCE_ID, decode(SOURCE_ID, fixture()));
        replace(manager, recipes);
        GrillBlockEntity grill = emptyGrill();
        grill.setFood(0, new ItemStack(Items.CARROT), GrillRecipes.byId(manager, SOURCE_ID));
        grill.addSeasoning(0, new ItemStack(Items.SUGAR), manager);
        grill.setProgress(0, 2);
        CompoundTag tag = save(helper, grill);
        ResourceLocation priorityId = DdIds.of(NortheastChinaDelight.MODID, "grill/priority_carrot");
        JsonObject priority = fixture();
        priority.getAsJsonObject("config").addProperty("priority", 5);
        recipes.put(priorityId, decode(priorityId, priority));
        replace(manager, recipes);
        GrillBlockEntity restored = load(helper, tag);
        helper.assertTrue(restored.recipeOf(0, manager).id().equals(SOURCE_ID)
                && restored.progress(0) == 2 && restored.seasoningsOf(0).equals(List.of(Items.SUGAR)),
                "新存档必须保留配方 ID、进度和调料，而不能改用更高优先级的另一配方");
        CompoundTag legacy = tag.copy();
        legacy.remove("GrillRecipes");
        GrillBlockEntity migrated = load(helper, legacy);
        helper.assertTrue(migrated.recipeOf(0, manager).id().equals(priorityId)
                && migrated.progress(0) == 2 && migrated.seasoningsOf(0).equals(List.of(Items.SUGAR)),
                "旧存档无配方 ID 的槽位应按输入绑定，并保留兼容的进度与调料");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void grillSerializersSyncIngredientsResultsAndDisabledRules(GameTestHelper helper) {
        JsonObject tagged = fixture();
        JsonObject input = new JsonObject();
        input.addProperty("tag", ModTags.CROPS_CORN.location().toString());
        tagged.getAsJsonObject("config").add("input", input);
        JsonObject sugar = new JsonObject();
        sugar.addProperty("tag", ModTags.SUGAR.location().toString());
        JsonArray seasonings = new JsonArray();
        seasonings.add(sugar);
        tagged.getAsJsonObject("config").add("seasonings", seasonings);
        GrillDataRecipe source = decode(SOURCE_ID, tagged);
        GrillRecipes.Recipe copy = networkCopy(helper, SOURCE_ID, source).rule(SOURCE_ID);
        helper.assertTrue(copy != null && copy.id().equals(SOURCE_ID) && copy.seconds() == 4
                && copy.result().is(Items.GOLDEN_CARROT) && copy.result().getCount() == 2
                && copy.input().test(new ItemStack(ModItems.CORN.get()))
                && copy.needsSeasoning(List.of(), new ItemStack(Items.SUGAR)),
                "网络同步必须保留标签、稳定 ID、产物数量和时长");
        //? if >=1.20.5 {
        /*JsonObject encoded = ModGrillRecipes.SERIALIZER.get().codec().codec()
                .encodeStart(JsonOps.INSTANCE, source).getOrThrow().getAsJsonObject();
        helper.assertTrue(decode(SOURCE_ID, encoded).rule(SOURCE_ID).seconds() == 4,
                "MapCodec 应可往返序列化配方");
        *///?}
        GrillDataRecipe disabled = decode(SOURCE_ID, JsonParser.parseString("""
                {"type":"northeast_china_delight:grill","config":{"disabled":true}}
                """).getAsJsonObject());
        helper.assertTrue(networkCopy(helper, SOURCE_ID, disabled).rule(SOURCE_ID) == null,
                "禁用标记也应随配方同步");
        helper.succeed();
    }

    private static void assertRejected(GameTestHelper helper, JsonObject json) {
        //? if <1.20.5 {
        boolean rejected = false;
        try {
            decode(SOURCE_ID, json);
        } catch (RuntimeException exception) {
            rejected = true;
        }
        helper.assertTrue(rejected, "无效的烤架配方未被拒绝：" + json);
        //?} else {
        /*helper.assertTrue(ModGrillRecipes.SERIALIZER.get().codec().codec().parse(JsonOps.INSTANCE, json)
                .error().isPresent(), "无效的烤架配方应返回解码错误：" + json);
        *///?}
    }

    @GameTest(template = "empty")
    public static void grillRejectsInvalidDataPackRules(GameTestHelper helper) {
        for (String seconds : List.of("0", "-1", "0.5", "2147483647", "\"4\"")) {
            JsonObject invalid = fixture();
            invalid.getAsJsonObject("config").add("seconds", JsonParser.parseString(seconds));
            assertRejected(helper, invalid);
        }
        for (int count : List.of(0, -1, 65)) {
            JsonObject invalid = fixture();
            invalid.getAsJsonObject("config").getAsJsonObject("result").addProperty("count", count);
            assertRejected(helper, invalid);
        }
        for (String output : List.of("minecraft:air", "minecraft:missing_grill_item")) {
            JsonObject invalid = fixture();
            invalid.getAsJsonObject("config").getAsJsonObject("result").addProperty("item", output);
            assertRejected(helper, invalid);
        }
        JsonObject missingInput = fixture();
        missingInput.getAsJsonObject("config").remove("input");
        assertRejected(helper, missingInput);
        JsonObject emptyInput = fixture();
        emptyInput.getAsJsonObject("config").add("input", new JsonArray());
        assertRejected(helper, emptyInput);
        JsonObject tooManySeasonings = fixture();
        JsonArray excessive = new JsonArray();
        for (int index = 0; index <= GrillRecipes.MAX_SEASONINGS; index++) {
            excessive.add(JsonParser.parseString("{\"item\":\"minecraft:sugar\"}"));
        }
        tooManySeasonings.getAsJsonObject("config").add("seasonings", excessive);
        assertRejected(helper, tooManySeasonings);
        JsonObject oversized = fixture();
        oversized.getAsJsonObject("config").addProperty("extra", "x".repeat(32768));
        assertRejected(helper, oversized);
        JsonObject noSeasonings = fixture();
        noSeasonings.getAsJsonObject("config").add("seasonings", new JsonArray());
        helper.assertTrue(decode(SOURCE_ID, noSeasonings).rule(SOURCE_ID).seasoningsReady(List.of()),
                "数据包应允许无需调料的烧烤配方");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void grillSlotsResolveBoundRecipesIndependently(GameTestHelper helper) {
        RecipeManager manager = manager(helper);
        ResourceLocation otherId = DdIds.of(NortheastChinaDelight.MODID, "grill/other_carrot");
        GrillDataRecipe source = decode(SOURCE_ID, fixture());
        GrillDataRecipe other = decode(otherId, fixture());
        replace(manager, Map.of(SOURCE_ID, source, otherId, other));
        GrillBlockEntity grill = emptyGrill();
        grill.setFood(0, new ItemStack(Items.CARROT), source.rule(SOURCE_ID));
        grill.setFood(1, new ItemStack(Items.CARROT), other.rule(otherId));
        grill.addSeasoning(0, new ItemStack(Items.SUGAR), manager);
        grill.setProgress(0, 2);
        grill.setProgress(1, 2);
        replace(manager, Map.of(otherId, other));
        helper.assertTrue(grill.recipeOf(0, manager) == null && grill.progress(0) == 0
                && grill.recipeOf(1, manager).id().equals(otherId) && grill.progress(1) == 2
                && grill.seasoningsOf(1).isEmpty(), "一个槽位的配方或调料不能影响另一个槽位");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void grillCollisionMatchesRackHeight(GameTestHelper helper) {
        BlockPos localPos = new BlockPos(1, 2, 1);
        helper.setBlock(localPos, ModBlocks.GRILL_CAMPFIRE.get().defaultBlockState());
        BlockPos pos = helper.absolutePos(localPos);
        double top = helper.getLevel().getBlockState(pos)
                .getCollisionShape(helper.getLevel(), pos)
                .max(Direction.Axis.Y, 0.5, 0.5);
        helper.assertTrue(Math.abs(top - 15.0 / 16.0) < 1.0E-4,
                "带烤架的营火碰撞箱应为烤架的 15/16 高，而不是原版营火的 7/16");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void grillTickerProducesTheJsonResult(GameTestHelper helper) {
        BlockPos localPos = new BlockPos(1, 2, 1);
        helper.setBlock(localPos, ModBlocks.GRILL_CAMPFIRE.get().defaultBlockState()
                .setValue(GrillCampfireBlock.LIT, true));
        GrillBlockEntity grill = (GrillBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(localPos));
        RecipeManager manager = helper.getLevel().getRecipeManager();
        GrillRecipes.Recipe rule = GrillRecipes.byId(manager,
                DdIds.of(NortheastChinaDelight.MODID, "grill/grilled_corn"));
        grill.setFood(0, new ItemStack(ModItems.CORN.get()), rule);
        grill.addSeasoning(0, new ItemStack(Items.SUGAR), manager);
        grill.setProgress(0, GrillCampfireBlock.MAX_PROGRESS - 1);
        helper.succeedWhen(() -> {
            helper.assertTrue(grill.getItems().get(0).isEmpty(), "烤制完成后应清空原料");
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new AABB(helper.absolutePos(localPos)).inflate(2.0),
                    entity -> entity.getItem().is(ModItems.GRILLED_CORN.get()));
            helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().getCount() == rule.result().getCount(),
                    "实体 ticker 必须掉落 JSON 指定的烧烤产物");
        });
    }
}
