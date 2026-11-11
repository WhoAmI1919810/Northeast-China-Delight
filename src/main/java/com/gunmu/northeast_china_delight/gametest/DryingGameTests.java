package com.gunmu.northeast_china_delight.gametest;

import com.gunmu.chinese_regional_culinary_ingredients.item.CrciItems;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gunmu.chinese_regional_culinary_ingredients.CrciTags;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.compat.create.ModFanProcessingTypes;
import com.gunmu.northeast_china_delight.compat.jei.DryingJeiRecipes;
import com.gunmu.chinese_regional_culinary_ingredients.crafting.DryingDataRecipe;
import com.gunmu.chinese_regional_culinary_ingredients.crafting.DryingRecipes;
import com.gunmu.chinese_regional_culinary_ingredients.crafting.ModDryingRecipes;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessing;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
//? if <1.20.5 {
/*import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.crafting.Recipe;
*///?} else {
import com.mojang.serialization.JsonOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.crafting.RecipeHolder;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@GameTestHolder(NortheastChinaDelight.MODID)
@PrefixGameTestTemplate(false)
public final class DryingGameTests {

    private static final ResourceLocation SOURCE_ID = DdIds.of(NortheastChinaDelight.MODID, "drying/test_carrot");

    private DryingGameTests() {
    }

    private static JsonObject fixture() {
        return JsonParser.parseString("""
                {"type":"chinese_regional_culinary_ingredients:drying","config":{
                  "input":[{"item":"minecraft:carrot"},{"item":"minecraft:potato"}],
                  "results":[{"item":"minecraft:golden_carrot","count":2},{"item":"minecraft:bowl"}]
                }}
                """).getAsJsonObject();
    }

    private static DryingDataRecipe decode(ResourceLocation id, JsonObject json) {
        //? if <1.20.5 {
        /*return ModDryingRecipes.SERIALIZER.get().fromJson(id, json);
        *///?} else {
        return ModDryingRecipes.SERIALIZER.get().codec().codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        //?}
    }

    private static RecipeManager manager(GameTestHelper helper) {
        //? if <1.20.5 {
        /*return new RecipeManager();
        *///?} else {
        return new RecipeManager(helper.getLevel().registryAccess());
        //?}
    }

    private static void replace(RecipeManager manager, Map<ResourceLocation, DryingDataRecipe> recipes) {
        //? if <1.20.5 {
        /*List<Recipe<?>> entries = new ArrayList<>(recipes.values());
        *///?} else {
        List<RecipeHolder<?>> entries = new ArrayList<>();
        recipes.forEach((id, recipe) -> entries.add(new RecipeHolder<>(id, recipe)));
        //?}
        manager.replaceRecipes(entries);
    }

    private static int count(List<ItemStack> stacks, Item item) {
        return stacks.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static DryingDataRecipe networkCopy(GameTestHelper helper, DryingDataRecipe source) {
        //? if <1.20.5 {
        /*FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        *///?} else {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        //?}
        try {
            //? if <1.20.5 {
            /*ModDryingRecipes.SERIALIZER.get().toNetwork(buffer, source);
            return ModDryingRecipes.SERIALIZER.get().fromNetwork(SOURCE_ID, buffer);
            *///?} else {
            ModDryingRecipes.SERIALIZER.get().streamCodec().encode(buffer, source);
            return ModDryingRecipes.SERIALIZER.get().streamCodec().decode(buffer);
            //?}
        } finally {
            buffer.release();
        }
    }

    @GameTest(template = "empty")
    public static void dryingDefaultRecipeUsesSoyMilkAndReturnsBowl(GameTestHelper helper) {
        DryingRecipes.Recipe recipe = DryingRecipes.byInput(helper.getLevel().getRecipeManager(),
                new ItemStack(ModItems.SOY_MILK.get()));
        helper.assertTrue(recipe != null && recipe.id().equals(
                DdIds.of("chinese_regional_culinary_ingredients", "drying/dried_tofu")), "豆浆应匹配自带数据包的干豆腐配方");
        List<ItemStack> output = recipe.process(new ItemStack(ModItems.SOY_MILK.get()));
        helper.assertTrue(count(output, CrciItems.DRIED_TOFU.get()) == 2 && count(output, Items.BOWL) == 1,
                "一份豆浆应产出两份干豆腐并返还一个碗");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dryingAlternativesProduceBothOutputsPerInput(GameTestHelper helper) {
        DryingRecipes.Recipe recipe = decode(SOURCE_ID, fixture()).rule(SOURCE_ID);
        helper.assertTrue(recipe.input().test(new ItemStack(Items.CARROT))
                && recipe.input().test(new ItemStack(Items.POTATO))
                && !recipe.input().test(new ItemStack(Items.SUGAR)), "任选数组应正确匹配食材");
        ItemStack input = new ItemStack(Items.CARROT, 4);
        List<ItemStack> outputs = recipe.process(input);
        helper.assertTrue(count(outputs, Items.GOLDEN_CARROT) == 8 && count(outputs, Items.BOWL) == 4
                && input.getCount() == 4, "两种产物都必须按输入数量扩展，不能修改原料堆叠");
        outputs.remove(0);
        helper.assertTrue(recipe.results().get(0).getCount() == 2, "返回结果应可供 Create 移除，不能修改配方定义");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dryingSplitsOversizedBatchOutputs(GameTestHelper helper) {
        List<ItemStack> outputs = decode(SOURCE_ID, fixture()).rule(SOURCE_ID)
                .process(new ItemStack(Items.CARROT, 40));
        helper.assertTrue(count(outputs, Items.GOLDEN_CARROT) == 80 && count(outputs, Items.BOWL) == 40
                && outputs.stream().allMatch(stack -> stack.getCount() <= stack.getMaxStackSize()),
                "大批量输出应保留全部产物，并遵守物品堆叠上限");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dryingReloadOverridesDisablesAndRemovesRules(GameTestHelper helper) {
        RecipeManager manager = manager(helper);
        replace(manager, Map.of(SOURCE_ID, decode(SOURCE_ID, fixture())));
        JsonObject changed = fixture();
        changed.getAsJsonObject("config").getAsJsonArray("results").get(0).getAsJsonObject().addProperty("count", 3);
        replace(manager, Map.of(SOURCE_ID, decode(SOURCE_ID, changed)));
        helper.assertTrue(DryingRecipes.byInput(manager, new ItemStack(Items.CARROT)).results().get(0).getCount() == 3,
                "同 ID 覆盖必须使用新产物");
        JsonObject disabled = JsonParser.parseString("""
                {"type":"chinese_regional_culinary_ingredients:drying","config":{"disabled":true}}
                """).getAsJsonObject();
        replace(manager, Map.of(SOURCE_ID, decode(SOURCE_ID, disabled)));
        helper.assertTrue(DryingRecipes.byInput(manager, new ItemStack(Items.CARROT)) == null
                && DryingJeiRecipes.all(manager).isEmpty(), "禁用规则不能继续加工或生成 JEI 页面");
        replace(manager, Map.of());
        helper.assertTrue(DryingRecipes.all(manager).isEmpty(), "删除所有配方后不能退回 Java 硬编码规则");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dryingPriorityThenIdDeterminesMatchingRule(GameTestHelper helper) {
        RecipeManager manager = manager(helper);
        ResourceLocation firstId = DdIds.of(NortheastChinaDelight.MODID, "drying/a_carrot");
        JsonObject prioritized = fixture();
        prioritized.getAsJsonObject("config").addProperty("priority", 10);
        replace(manager, Map.of(firstId, decode(firstId, fixture()), SOURCE_ID, decode(SOURCE_ID, prioritized)));
        helper.assertTrue(DryingRecipes.byInput(manager, new ItemStack(Items.CARROT)).id().equals(SOURCE_ID),
                "应优先使用 priority 较高的规则");
        replace(manager, Map.of(firstId, decode(firstId, fixture()), SOURCE_ID, decode(SOURCE_ID, fixture())));
        helper.assertTrue(DryingRecipes.byInput(manager, new ItemStack(Items.CARROT)).id().equals(firstId),
                "相同 priority 应按稳定 ID 排序");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dryingSerializersPreserveTaggedInputAndResults(GameTestHelper helper) {
        JsonObject tagged = fixture();
        JsonObject input = new JsonObject();
        input.addProperty("tag", CrciTags.FOODS_SOY_MILK.location().toString());
        tagged.getAsJsonObject("config").add("input", input);
        DryingDataRecipe source = decode(SOURCE_ID, tagged);
        DryingRecipes.Recipe synced = networkCopy(helper, source).rule(SOURCE_ID);
        helper.assertTrue(synced.input().test(new ItemStack(ModItems.SOY_MILK.get()))
                && synced.results().size() == 2 && synced.results().get(0).getCount() == 2,
                "网络同步必须保留标签食材和两个产物");
        //? if >=1.20.5 {
        var encoded = ModDryingRecipes.SERIALIZER.get().codec().codec()
                .encodeStart(JsonOps.INSTANCE, source).getOrThrow();
        helper.assertTrue(decode(SOURCE_ID, encoded.getAsJsonObject()).rule(SOURCE_ID).results().size() == 2,
                "MapCodec 应可往返序列化两个产物");
        //?}
        JsonObject disabled = JsonParser.parseString("""
                {"type":"chinese_regional_culinary_ingredients:drying","config":{"disabled":true}}
                """).getAsJsonObject();
        helper.assertTrue(networkCopy(helper, decode(SOURCE_ID, disabled)).rule(SOURCE_ID) == null,
                "禁用标记也必须完成网络同步");
        helper.succeed();
    }

    private static void assertRejected(GameTestHelper helper, JsonObject json) {
        boolean rejected = false;
        try {
            decode(SOURCE_ID, json);
        } catch (RuntimeException exception) {
            rejected = true;
        }
        helper.assertTrue(rejected, "应拒绝无效的风干配方：" + json);
    }

    @GameTest(template = "empty")
    public static void dryingRejectsInvalidIngredientsAndResults(GameTestHelper helper) {
        for (Number count : List.of(0, -1, 1.5, 65)) {
            JsonObject invalid = fixture();
            invalid.getAsJsonObject("config").getAsJsonArray("results").get(0).getAsJsonObject()
                    .addProperty("count", count);
            assertRejected(helper, invalid);
        }
        JsonObject empty = fixture();
        empty.getAsJsonObject("config").add("results", new JsonArray());
        assertRejected(helper, empty);
        JsonObject unknown = fixture();
        unknown.getAsJsonObject("config").getAsJsonArray("results").get(0).getAsJsonObject()
                .addProperty("item", "northeast_china_delight:missing_output");
        assertRejected(helper, unknown);
        JsonObject tooMany = fixture();
        tooMany.getAsJsonObject("config").getAsJsonArray("results").add(
                JsonParser.parseString("{\"item\":\"minecraft:sugar\"}"));
        assertRejected(helper, tooMany);
        JsonObject noInput = fixture();
        noInput.getAsJsonObject("config").remove("input");
        assertRejected(helper, noInput);
        JsonObject emptyInput = fixture();
        emptyInput.getAsJsonObject("config").add("input", new JsonArray());
        assertRejected(helper, emptyInput);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dryingJeiKeepsAlternativesOnOnePage(GameTestHelper helper) {
        RecipeManager manager = manager(helper);
        replace(manager, Map.of(SOURCE_ID, decode(SOURCE_ID, fixture())));
        var pages = DryingJeiRecipes.all(manager);
        helper.assertTrue(pages.size() == 1 && pages.get(0).id().equals(SOURCE_ID)
                && pages.get(0).ingredient().size() == 2 && pages.get(0).results().size() == 2,
                "一条数据包规则只应生成一页 JEI，候选食材在同一格轮换");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dryingFanProcessesEntityStackAndReturnsBowls(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        ItemEntity entity = new ItemEntity(helper.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5,
                pos.getZ() + 0.5, new ItemStack(ModItems.SOY_MILK.get(), 8));
        helper.getLevel().addFreshEntity(entity);
        CompoundTag processing = new CompoundTag();
        processing.putString("Type", "northeast_china_delight:drying");
        processing.putInt("Time", 1);
        CompoundTag createData = new CompoundTag();
        createData.put("Processing", processing);
        entity.getPersistentData().put("CreateData", createData);
        helper.assertTrue(FanProcessing.applyProcessing(entity, ModFanProcessingTypes.DRYING.get()),
                "Create 应通过真实风干处理入口完成加工");
        List<ItemEntity> bowls = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(pos).inflate(1), dropped -> dropped.getItem().is(Items.BOWL));
        helper.assertTrue(entity.getItem().is(CrciItems.DRIED_TOFU.get()) && entity.getItem().getCount() == 16
                && bowls.stream().mapToInt(dropped -> dropped.getItem().getCount()).sum() == 8,
                "八份豆浆应产出十六份干豆腐及八个碗，不能丢失堆叠数量或抛出不可变列表异常");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dryingDoesNotReplaceOtherFanProcessingTypes(GameTestHelper helper) {
        BlockPos fanPos = new BlockPos(1, 2, 1);
        BlockPos front = fanPos.east();
        helper.setBlock(fanPos, AllBlocks.ENCASED_FAN.getDefaultState()
                .setValue(EncasedFanBlock.FACING, Direction.EAST));
        helper.setBlock(front, Blocks.AIR);
        FanProcessingType drying = ModFanProcessingTypes.DRYING.get();
        helper.assertTrue(drying.isValidAt(helper.getLevel(), helper.absolutePos(front))
                && !drying.isValidAt(helper.getLevel(), helper.absolutePos(front.east())),
                "裸风处理只能从鼓风机正前方的空气格开始");
        helper.setBlock(front, Blocks.WATER);
        helper.assertTrue(FanProcessingType.getAt(helper.getLevel(), helper.absolutePos(front))
                == FanProcessingType.parse("create:splashing"), "水催化的喷溅配方不能被风干顶掉");
        helper.setBlock(front, Blocks.LAVA);
        helper.assertTrue(FanProcessingType.getAt(helper.getLevel(), helper.absolutePos(front))
                == FanProcessingType.parse("create:blasting"), "熔岩催化的鼓风配方不能被风干顶掉");
        helper.succeed();
    }
}
