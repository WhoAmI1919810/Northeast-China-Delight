package com.gunmu.northeast_china_delight.gametest;

import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.block.VatBlockEntity;
import com.gunmu.northeast_china_delight.compat.jei.VatJeiRecipe;
import com.gunmu.northeast_china_delight.compat.jei.VatJeiRecipes;
import com.gunmu.northeast_china_delight.crafting.VatBrewing;
import com.gunmu.northeast_china_delight.crafting.ModVatRecipes;
import com.gunmu.northeast_china_delight.crafting.VatDataRecipe;
import com.gunmu.northeast_china_delight.crafting.VatRecipe;
import com.gunmu.northeast_china_delight.crafting.VatRecipes;
import com.gunmu.northeast_china_delight.crafting.VatRecipeValidator;
import com.gunmu.northeast_china_delight.fluid.ModFluids;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.gunmu.northeast_china_delight.block.Vat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
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
import java.util.ArrayList;

/**
 * 大缸的自动化回归（跑法：{@code gradlew runGameTestServer}）。
 */
@GameTestHolder(NortheastChinaDelight.MODID)
@PrefixGameTestTemplate(false)
public final class VatGameTests {

    private VatGameTests() {
    }

    private static Player mockPlayer(GameTestHelper helper, GameType gameType) {
        //? if <1.20.5 {
        /*return gameType == GameType.CREATIVE ? helper.makeMockPlayer() : helper.makeMockSurvivalPlayer();
        *///?} else {
        return helper.makeMockPlayer(gameType);
        //?}
    }

    private static final BlockPos AT = new BlockPos(1, 1, 1);

    @GameTest(template = "empty")
    public static void vatFinishedBeanSproutsCanBeTakenWithEmptyHand(GameTestHelper helper) {
        assertFinishedItemCanBeTaken(helper, VatRecipes.Kind.BEAN_SPROUTS, ModItems.BEAN_SPROUTS.get());
    }

    @GameTest(template = "empty")
    public static void vatFinishedSourCornCanBeTakenWithEmptyHand(GameTestHelper helper) {
        assertFinishedItemCanBeTaken(helper, VatRecipes.Kind.SOUR_CORN, ModItems.SOUR_CORN_KERNELS.get());
    }

    @GameTest(template = "empty")
    public static void vatFinishedSourCabbageCanBeTakenWithEmptyHand(GameTestHelper helper) {
        assertFinishedItemCanBeTaken(helper, VatRecipes.Kind.PICKLE, ModItems.SOUR_CABBAGE.get());
    }

    @GameTest(template = "empty")
    public static void vatFinishedCoverComesOffBeforeCollecting(GameTestHelper helper) {
        Item rug = clothRug();
        if (rug == null) {
            helper.fail("Missing canvas rug for vat collection test");
        }
        VatBlockEntity vat = finishedVat(helper, VatRecipes.Kind.BEAN_SPROUTS);
        vat.addContent(new ItemStack(ModItems.BEAN_SPROUTS.get()));
        vat.setCover(new ItemStack(rug));
        Player player = mockPlayer(helper, GameType.SURVIVAL);
        click(helper, player, ItemStack.EMPTY);
        if (vat.isCovered() || player.getInventory().countItem(rug) != 1
                || vat.countOf(ModItems.BEAN_SPROUTS.get()) != 1) {
            helper.fail("First finished-vat click should return the rug before collecting food");
        }
        click(helper, player, ItemStack.EMPTY);
        if (vat.countOf(ModItems.BEAN_SPROUTS.get()) != 0
                || player.getInventory().countItem(ModItems.BEAN_SPROUTS.get()) != 1) {
            helper.fail("Finished food should be collectible after removing the rug");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vatFinishedSoyResidueCanBeTakenBeforeLiquid(GameTestHelper helper) {
        VatBlockEntity vat = finishedVat(helper, VatRecipes.Kind.PASTE);
        vat.addContent(new ItemStack(ModItems.SOY_RESIDUE.get()));
        vat.setProductMb(com.gunmu.northeast_china_delight.fluid.ModFluids.soyPasteSource(), VatRecipes.SERVING_MB);
        Player player = mockPlayer(helper, GameType.SURVIVAL);
        click(helper, player, ItemStack.EMPTY);
        if (vat.countOf(ModItems.SOY_RESIDUE.get()) != 0
                || player.getInventory().countItem(ModItems.SOY_RESIDUE.get()) != 1
                || vat.productMb(com.gunmu.northeast_china_delight.fluid.ModFluids.soyPasteSource()) != VatRecipes.SERVING_MB) {
            helper.fail("Solid residue should be collectible without draining the liquid first");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vatFinishedPicklesCanBeTakenBeforeSourWater(GameTestHelper helper) {
        Item cabbage = ModItems.NAPA_CABBAGE.get();
        VatBlockEntity vat = finishedVat(helper, VatRecipes.Kind.PICKLE);
        vat.addContent(new ItemStack(cabbage));
        vat.setProductMb(com.gunmu.northeast_china_delight.fluid.ModFluids.sourWaterSource(), VatRecipes.WATER_MB_PER_LEVEL);
        Player player = mockPlayer(helper, GameType.SURVIVAL);
        click(helper, player, ItemStack.EMPTY);
        if (vat.countOf(cabbage) != 0 || player.getInventory().countItem(ModItems.SOUR_CABBAGE.get()) != 1
                || vat.productMb(com.gunmu.northeast_china_delight.fluid.ModFluids.sourWaterSource()) != VatRecipes.WATER_MB_PER_LEVEL) {
            helper.fail("Finished cabbage should convert on collection without losing the sour water");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vatFinishedKvassRequiresBottle(GameTestHelper helper) {
        VatBlockEntity vat = finishedVat(helper, VatRecipes.Kind.KVASS);
        int amount = VatRecipes.KVASS_SERVINGS * VatRecipes.SERVING_MB;
        vat.setProductMb(com.gunmu.northeast_china_delight.fluid.ModFluids.kvassSource(), amount);
        Player player = mockPlayer(helper, GameType.SURVIVAL);
        click(helper, player, ItemStack.EMPTY);
        click(helper, player, new ItemStack(Items.BOWL));
        if (vat.productMb(com.gunmu.northeast_china_delight.fluid.ModFluids.kvassSource()) != amount) {
            helper.fail("An empty hand or bowl must not collect kvass");
        }
        click(helper, player, new ItemStack(Items.GLASS_BOTTLE));
        if (player.getInventory().countItem(ModItems.KVASS.get()) != 1
                || vat.productMb(com.gunmu.northeast_china_delight.fluid.ModFluids.kvassSource()) != amount - VatRecipes.SERVING_MB) {
            helper.fail("A glass bottle should collect exactly one serving of kvass");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vatKvassJeiUsesLiquidAndBottle(GameTestHelper helper) {
        VatRecipe recipe = VatRecipes.recipeOf(VatRecipes.Kind.KVASS, helper.getLevel());
        VatJeiRecipe.State state = VatJeiRecipes.pagesFor(recipe).get(0).first();
        if (!state.hasResultFluid() || state.resultFluid() != ModFluids.KVASS.get()
                || state.resultFluidMb() != recipe.productMbFor(VatRecipes.KVASS_WATER_LEVEL)
                || state.hasResultItem() || state.collectionContainers().size() != 1
                || !state.collectionContainers().get(0).is(Items.GLASS_BOTTLE)) {
            helper.fail("KVASS JEI must show the liquid output and a separate collection bottle");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vatJeiCollectionContainersFollowProducts(GameTestHelper helper) {
        for (VatRecipe recipe : VatRecipes.all(helper.getLevel())) {
            net.minecraft.world.level.material.Fluid product = recipe.liquid().product();
            VatRecipes.Serving serving = product == null ? null : VatRecipes.servingOf(product);
            for (VatJeiRecipe.State state : VatJeiRecipes.pagesFor(recipe).get(0).states()) {
                if (serving == null && !state.collectionContainers().isEmpty()
                        || serving != null && (state.collectionContainers().size() != 1
                        || !state.collectionContainers().get(0).is(serving.container()))) {
                    helper.fail("JEI collection container differs from gameplay: " + recipe.id());
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vatCustomLiteralVerbSurvivesNetworkAndJei(GameTestHelper helper) {
        for (String value : List.of("\"催芽\"", "{\"text\":\"催芽\"}")) {
            JsonObject config = verbTestConfig();
            config.add("verb", JsonParser.parseString(value));
            VatRecipe recipe = roundTripVerbRecipe(helper, config);
            if (recipe == null || !recipe.verb().getString().equals("催芽")
                    || !VatJeiRecipes.pagesFor(recipe).get(0).first().verb().getString().equals("催芽")) {
                helper.fail("Custom vat verb must survive network sync, ID binding and JEI generation");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vatCustomTranslatedVerbSurvivesNetworkAndJei(GameTestHelper helper) {
        JsonObject config = verbTestConfig();
        String key = "test.vat.custom_verb";
        JsonObject verb = new JsonObject();
        verb.addProperty("translate", key);
        config.add("verb", verb);
        VatRecipe recipe = roundTripVerbRecipe(helper, config);
        if (recipe == null || !(recipe.verb().getContents() instanceof TranslatableContents contents)
                || !contents.getKey().equals(key)
                || !(VatJeiRecipes.pagesFor(recipe).get(0).first().verb().getContents()
                instanceof TranslatableContents displayed) || !displayed.getKey().equals(key)) {
            helper.fail("Translated vat verbs must remain translatable after sync and JEI generation");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vatLegacyRecipesGetDefaultVerbs(GameTestHelper helper) {
        VatRecipe recipe = roundTripVerbRecipe(helper, verbTestConfig());
        if (recipe == null || !(recipe.verb().getContents() instanceof TranslatableContents contents)
                || !contents.getKey().equals("process.northeast_china_delight.vat.sprouting")) {
            helper.fail("Legacy vat recipes without a verb should keep a suitable default action");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vatInvalidVerbsAreRejected(GameTestHelper helper) {
        for (String value : List.of("\"\"", "\"   \"", "null", "5", "[]", "{}",
                "{\"text\":\"x\",\"translate\":\"y\"}", "{\"text\":\"\"}", "{\"translate\":\"\"}")) {
            JsonObject config = verbTestConfig();
            config.add("verb", JsonParser.parseString(value));
            try {
                com.gunmu.northeast_china_delight.crafting.VatRecipeJson.decode(null, config);
            } catch (JsonParseException expected) {
                continue;
            }
            helper.fail("Invalid vat verb should be rejected: " + value);
        }
        helper.succeed();
    }

    private static JsonObject verbTestConfig() {
        return JsonParser.parseString("""
                {"kind":"BEAN_SPROUTS","seal":"NONE","seconds":30,"liquid":{},
                 "slots":[{"ingredient":{"item":"minecraft:wheat"},"fate":"CONVERT",
                   "bounds":{"type":"exact","count":1},"result":"northeast_china_delight:bean_sprouts"}]}
                """).getAsJsonObject();
    }

    private static VatRecipe roundTripVerbRecipe(GameTestHelper helper, JsonObject config) {
        ResourceLocation id = DdIds.of(NortheastChinaDelight.MODID, "vat/verb_sync_test");
        JsonObject json = new JsonObject();
        json.addProperty("type", NortheastChinaDelight.MODID + ":vat");
        json.add("config", config);
        //? if <1.20.5 {
        /*VatDataRecipe source = ModVatRecipes.SERIALIZER.get().fromJson(id, json);
        net.minecraft.network.FriendlyByteBuf buffer = new net.minecraft.network.FriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer());
        try {
            ModVatRecipes.SERIALIZER.get().toNetwork(buffer, source);
            return ModVatRecipes.SERIALIZER.get().fromNetwork(id, buffer).rule(id);
        } finally {
            buffer.release();
        }
        *///?} else {
        VatDataRecipe source = ModVatRecipes.SERIALIZER.get().codec().codec()
                .parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow();
        net.minecraft.network.RegistryFriendlyByteBuf buffer = new net.minecraft.network.RegistryFriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            ModVatRecipes.SERIALIZER.get().streamCodec().encode(buffer, source);
            return ModVatRecipes.SERIALIZER.get().streamCodec().decode(buffer).rule(id);
        } finally {
            buffer.release();
        }
        //?}
    }

    private static VatBlockEntity finishedVat(GameTestHelper helper, VatRecipes.Kind kind) {
        VatRecipe recipe = VatRecipes.recipeOf(kind, helper.getLevel());
        if (recipe == null) {
            helper.fail("Missing vat recipe: " + kind);
        }
        VatBlockEntity vat = placeVat(helper);
        vat.setRecipe(recipe);
        helper.getLevel().setBlock(vat.getBlockPos(), vat.getBlockState().setValue(Vat.FERMENTED, true), 3);
        return vat;
    }

    private static void assertFinishedItemCanBeTaken(GameTestHelper helper, VatRecipes.Kind kind, Item product) {
        VatBlockEntity vat = finishedVat(helper, kind);
        vat.addContent(new ItemStack(product));
        Player player = mockPlayer(helper, GameType.SURVIVAL);
        click(helper, player, ItemStack.EMPTY);
        if (vat.countOf(product) != 0 || player.getInventory().countItem(product) != 1) {
            helper.fail("Empty-hand right-click must collect finished " + BuiltInRegistries.ITEM.getKey(product)
                    + " rather than report progress");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vatPicklesProduceThreeBucketsOfSourWater(GameTestHelper helper) {
        for (VatRecipes.Kind kind : List.of(VatRecipes.Kind.PICKLE, VatRecipes.Kind.SPICY_PICKLE)) {
            VatRecipe recipe = VatRecipes.recipeOf(kind, helper.getLevel());
            if (recipe == null || recipe.liquid().product() != com.gunmu.northeast_china_delight.fluid.ModFluids.sourWaterSource()
                    || recipe.productMbFor(3) != 3 * VatRecipes.WATER_MB_PER_LEVEL) {
                helper.fail("Missing or invalid sour water recipe: " + kind);
            }
            VatBlockEntity vat = placeVat(helper);
            vat.setRecipe(recipe);
            if (vat.getTankCapacity(0) != VatRecipes.SOUR_WATER_MB) {
                helper.fail("Sour water tank is smaller than its recipe output: " + kind);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vatPickleCanBeMade(GameTestHelper helper) {
        assertPickleCanBeMade(helper, false);
    }

    @GameTest(template = "empty")
    public static void vatSpicyPickleCanBeMade(GameTestHelper helper) {
        assertPickleCanBeMade(helper, true);
    }

    @GameTest(template = "empty")
    public static void vatSpicyPickleUsesOnlyItsFamilyTag(GameTestHelper helper) {
        VatRecipe recipe = VatRecipes.recipeOf(VatRecipes.Kind.SPICY_PICKLE, helper.getLevel());
        if (recipe == null || !recipe.accepts(0, new ItemStack(ModItems.NAPA_CABBAGE.get()))) {
            helper.fail("辣味泡菜族没有接受大白菜");
        }
        for (Item item : List.of(ModItems.CUCUMBER.get(), Items.CARROT, ModItems.GREEN_RADISH.get())) {
            if (recipe.accepts(0, new ItemStack(item))) {
                helper.fail("辣味泡菜族不应接受普通泡菜原料：" + BuiltInRegistries.ITEM.getKey(item));
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vatMappedConverterSurvivesNetwork(GameTestHelper helper) {
        JsonObject config = JsonParser.parseString("""
                {"kind":"SPICY_PICKLE","seal":"PRESS","seconds":1,"liquid":{},
                 "slots":[{"ingredient":{"tag":"northeast_china_delight:spicy_pickle_ingredients"},
                   "fate":"KEEP","bounds":{"type":"exact","count":1},
                   "converter":{"type":"mapped","entries":[
                     {"ingredient":{"tag":"c:crops/napa_cabbage"},
                      "result":"northeast_china_delight:spicy_cabbage"}]}}]}
                """).getAsJsonObject();
        VatRecipe recipe = roundTripVerbRecipe(helper, config);
        if (recipe == null || recipe.slots().get(0).converter() == null) {
            helper.fail("映射换算没有通过配方同步");
        }
        VatRecipe.Converter converter = recipe.slots().get(0).converter();
        if (!converter.apply(new ItemStack(ModItems.NAPA_CABBAGE.get()))
                .is(ModItems.SPICY_CABBAGE.get())) {
            helper.fail("大白菜没有按映射转成辣白菜");
        }
        ItemStack cucumber = new ItemStack(ModItems.CUCUMBER.get());
        if (!converter.apply(cucumber).is(cucumber.getItem())) {
            helper.fail("未命中映射的输入应原样返还");
        }
        helper.succeed();
    }

    private static void assertPickleCanBeMade(GameTestHelper helper, boolean spicy) {
        VatBlockEntity vat = placeVat(helper);
        vat.addWater(VatRecipes.WATER_MB_PER_LEVEL);
        for (int i = 0; i < 2; i++) {
            vat.addContent(new ItemStack(ModItems.NAPA_CABBAGE.get()));
        }
        vat.addContent(new ItemStack(ModItems.SALT.get()));
        if (spicy) {
            vat.addContent(new ItemStack(ModItems.CHILI_SAUCE.get()));
            vat.addContent(new ItemStack(ModItems.FISH_SAUCE.get()));
        }
        vat.setPress(new ItemStack(Items.STONE));

        VatRecipes.Kind expectedKind = spicy ? VatRecipes.Kind.SPICY_PICKLE : VatRecipes.Kind.PICKLE;
        VatRecipe recipe = VatBrewing.ready(vat);
        if (recipe == null || recipe.kind() != expectedKind) {
            helper.fail((spicy ? "辣白菜" : "酸菜") + "配方没有正确认出，实际 "
                    + (recipe == null ? "null" : recipe.kind()));
        }
        VatBrewing.complete(helper.getLevel(), helper.absolutePos(AT), vat, recipe);
        ItemStack result = VatBrewing.takeOne(vat, new ArrayList<>());
        Item expected = spicy ? ModItems.SPICY_CABBAGE.get() : ModItems.SOUR_CABBAGE.get();
        if (!result.is(expected) || vat.sourWaterMb() < VatRecipes.WATER_MB_PER_LEVEL) {
            helper.fail((spicy ? "辣白菜" : "酸菜") + "制作/取出失败，结果 "
                    + BuiltInRegistries.ITEM.getKey(result.getItem()));
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vatDisabledRecipeReturnsItsIngredients(GameTestHelper helper) {
        VatBlockEntity vat = placeVat(helper);
        vat.setRecipe(VatRecipe.of(VatRecipes.Kind.SPICY_PICKLE,
                "northeast_china_delight:vat/disabled_for_test").build());
        vat.addContent(new ItemStack(ModItems.CHILI_SAUCE.get()));
        List<ItemStack> refunds = new ArrayList<>();
        ItemStack taken = VatBrewing.takeOne(vat, refunds);
        if (!taken.is(ModItems.CHILI_SAUCE.get()) || !refunds.isEmpty()) {
            helper.fail("Disabled vat recipe should return raw inputs rather than absorb them");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vatDisabledRecipeCanBeEmptiedWithoutBreaking(GameTestHelper helper) {
        Player player = mockPlayer(helper, GameType.CREATIVE);
        VatBlockEntity vat = placeVat(helper);
        vat.setRecipe(VatRecipe.of(VatRecipes.Kind.SPICY_PICKLE,
                "northeast_china_delight:vat/disabled_for_test").build());
        vat.addContent(new ItemStack(ModItems.CHILI_SAUCE.get()));
        helper.getLevel().setBlock(vat.getBlockPos(), vat.getBlockState().setValue(Vat.PROGRESS, 1), 3);
        click(helper, player, ItemStack.EMPTY);
        if (vat.countOf(ModItems.CHILI_SAUCE.get()) != 0 || vat.recipeId() != null) {
            helper.fail("An unfinished disabled vat recipe must return its inputs on an empty-hand click");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vatDisabledRecipeStopsBeforeFirstTick(GameTestHelper helper) {
        VatBlockEntity vat = placeVat(helper);
        vat.addWater(3 * VatRecipes.WATER_MB_PER_LEVEL);
        for (int index = 0; index < VatRecipes.PASTE_CHUNKS; index++) {
            vat.addContent(new ItemStack(ModItems.SOY_PASTE_CHUNK.get()));
        }
        for (int index = 0; index < VatRecipes.PASTE_SALT; index++) {
            vat.addContent(new ItemStack(ModItems.SALT.get()));
        }
        vat.setCover(new ItemStack(Items.WHITE_CARPET));
        vat.setRecipe(VatRecipe.of(VatRecipes.Kind.PASTE,
                "northeast_china_delight:vat/disabled_for_test").build());
        if (vat.getBlockState().getValue(Vat.PROGRESS) != 0 || VatBrewing.ready(vat) != null
                || VatBrewing.current(vat) != null
                || VatBrewing.forInsert(vat, new ItemStack(Items.WHEAT)) != null
                || VatBrewing.sealRecipe(vat, new ItemStack(Items.WHITE_CARPET)) != null) {
            helper.fail("Disabled vat recipe must not switch to another recipe before the first tick");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vatDisabledRecipeRejectsNewLiquids(GameTestHelper helper) {
        VatBlockEntity vat = placeVat(helper);
        vat.setRecipe(VatRecipe.of(VatRecipes.Kind.PICKLE,
                "northeast_china_delight:vat/disabled_for_test").build());
        if (!vat.hasMissingUnfinishedRecipe()) {
            helper.fail("Disabled vat recipe should not accept water or sour water");
        }
        vat.setKind(VatRecipes.Kind.PICKLE);
        if (vat.hasMissingUnfinishedRecipe()) {
            helper.fail("Unbound vat should still accept water or sour water");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vatDisabledRecipeKeepsItsFinishedFluid(GameTestHelper helper) {
        VatBlockEntity vat = placeVat(helper);
        helper.getLevel().setBlock(vat.getBlockPos(), vat.getBlockState().setValue(Vat.FERMENTED, true), 3);
        vat.setRecipe(VatRecipe.of(VatRecipes.Kind.VINEGAR,
                "northeast_china_delight:vat/disabled_for_test")
                .product(com.gunmu.northeast_china_delight.fluid.ModFluids.whiteVinegarSource(), VatRecipes.SERVING_MB, false).build());
        vat.setProductMb(com.gunmu.northeast_china_delight.fluid.ModFluids.whiteVinegarSource(), VatRecipes.SERVING_MB);
        if (vat.getFluidInTank(0).getFluid() != ModFluids.whiteVinegarSource()) {
            helper.fail("Disabled vat recipe should keep its brewed fluid accessible");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vatRecipesPassSelfCheck(GameTestHelper helper) {
        List<String> problems = VatRecipeValidator.check(VatRecipes.all(helper.getLevel()), true);
        if (!problems.isEmpty()) {
            helper.fail("配方自检没过：" + problems);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vatDataPackIdsOverrideAndDisable(GameTestHelper helper) {
        ResourceLocation id = DdIds.of(NortheastChinaDelight.MODID, "vat/bean_sprouts");
        //? if <1.20.5 {
        /*VatDataRecipe source = (VatDataRecipe) helper.getLevel().getRecipeManager().byKey(id)
                .orElseThrow();
        *///?} else {
        VatDataRecipe source = (VatDataRecipe) helper.getLevel().getRecipeManager().byKey(id)
                .orElseThrow().value();
        //?}
        JsonObject override = JsonParser.parseString("""
                {"type":"northeast_china_delight:vat", "config":{
                  "kind":"BEAN_SPROUTS", "priority":20, "seal":"CLOTH", "seconds":7,
                  "liquid":{"water_min":1,"water_max":1,"water_after":"CLEAR"},
                  "slots":[{"ingredient":{"item":"northeast_china_delight:soybean"},
                    "bounds":{"type":"exact","count":1},"fate":"CONVERT",
                    "result":"northeast_china_delight:bean_sprouts"}]
                }}
                """).getAsJsonObject();
        //? if <1.20.5 {
        /*VatDataRecipe changed = ModVatRecipes.SERIALIZER.get().fromJson(id, override);
        *///?} else {
        VatDataRecipe changed = ModVatRecipes.SERIALIZER.get().codec()
                .codec().parse(com.mojang.serialization.JsonOps.INSTANCE, override).getOrThrow();
        //?}
        if (source.rule(id) == null || source.rule(id).seconds() != VatRecipes.BEAN_SPROUTS_SECONDS
                || !source.rule(id).id().equals(id.toString())
                || changed.rule(id) == null || changed.rule(id).seconds() != 7) {
            helper.fail("数据包大缸配方的路径应成为稳定 ID");
        }
        JsonObject disabled = override.deepCopy();
        disabled.getAsJsonObject("config").addProperty("disabled", true);
        //? if <1.20.5 {
        /*VatDataRecipe removed = ModVatRecipes.SERIALIZER.get().fromJson(id, disabled);
        *///?} else {
        VatDataRecipe removed = ModVatRecipes.SERIALIZER.get().codec()
                .codec().parse(com.mojang.serialization.JsonOps.INSTANCE, disabled).getOrThrow();
        //?}
        if (removed.rule(id) != null) {
            helper.fail("被数据包禁用的大缸配方不应继续提供规则");
        }
        helper.succeed();
    }

    /** 展示的份数必须满足配方本身：咸腊肉/咸鱼是"盐数 = 肉数"，不是 1 份盐 */
    @GameTest(template = "empty")
    public static void vatJeiCountsMatchRecipe(GameTestHelper helper) {
        checkSaltFollowsMainIngredient(helper, VatRecipes.Kind.MEAT, "咸腊肉");
        checkSaltFollowsMainIngredient(helper, VatRecipes.Kind.SALTED_FISH, "咸鱼");
        helper.succeed();
    }

    private static void checkSaltFollowsMainIngredient(GameTestHelper helper, VatRecipes.Kind kind, String name) {
        VatRecipe recipe = VatRecipes.recipeOf(kind, helper.getLevel());
        int[] counts = recipe.displayedCounts(0);
        if (counts.length != 2 || counts[0] != counts[1]) {
            helper.fail(name + "展示的份数不对（主料 " + counts[0] + " / 盐 " + counts[1] + "）");
        }
    }

    @GameTest(template = "empty")
    public static void vatJeiPagesAreGenerated(GameTestHelper helper) {
        for (VatRecipe recipe : VatRecipes.all(helper.getLevel())) {
            List<VatJeiRecipe> pages = VatJeiRecipes.pagesFor(recipe);
            if (pages.isEmpty()) {
                helper.fail("配方 " + recipe.id() + " 在 JEI 里一页都没有");
            }
            for (VatJeiRecipe page : pages) {
                for (VatJeiRecipe.State state : page.states()) {
                    if (state.results().isEmpty() && !state.hasResultFluid()) {
                        helper.fail("配方 " + recipe.id() + " 有一页什么都不产（投料画了、产物没画）");
                    }
                }
            }
            checkPageFollowsIngredient(helper, recipe, pages, VatRecipes.Kind.MEAT);
            checkPageFollowsIngredient(helper, recipe, pages, VatRecipes.Kind.SALTED_FISH);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vatJeiPicklePagesKeepAllIngredients(GameTestHelper helper) {
        VatRecipe pickle = VatRecipes.recipeOf(VatRecipes.Kind.PICKLE, helper.getLevel());
        VatRecipe spicyPickle = VatRecipes.recipeOf(VatRecipes.Kind.SPICY_PICKLE, helper.getLevel());
        VatJeiRecipe sourPage = pageProducing(VatJeiRecipes.pagesFor(pickle), ModItems.SOUR_CABBAGE.get());
        VatJeiRecipe spicyPage = pageProducing(VatJeiRecipes.pagesFor(spicyPickle), ModItems.SPICY_CABBAGE.get());
        if (sourPage == null || spicyPage == null) {
            helper.fail("JEI 缺少酸菜或辣白菜页面");
        }
        for (VatJeiRecipe.State state : sourPage.states()) {
            if (state.primary().size() != 1 || state.seasoning().size() != 1) {
                helper.fail("酸菜的 JEI 页面丢了盐格：主料 " + state.primary().size()
                        + " / 调味 " + state.seasoning().size());
            }
        }
        for (VatJeiRecipe.State state : spicyPage.states()) {
            if (state.primary().size() != 1 || state.seasoning().size() != 3) {
                helper.fail("辣白菜的 JEI 页面丢了盐、辣椒酱或鱼露/虾酱格：主料 "
                        + state.primary().size() + " / 调味 " + state.seasoning().size());
            }
        }
        if (sourPage.id().equals(spicyPage.id())) {
            helper.fail("酸菜和辣白菜的 JEI 页面 ID 不能相同");
        }
        helper.succeed();
    }

    @Nullable
    private static VatJeiRecipe pageProducing(List<VatJeiRecipe> pages, Item item) {
        for (VatJeiRecipe page : pages) {
            for (VatJeiRecipe.State state : page.states()) {
                if (state.results().stream().anyMatch(stack -> stack.is(item))) {
                    return page;
                }
            }
        }
        return null;
    }

    /** 抽查"盐数 = 主料数"：JEI 页面上主料格与盐格的份数必须相等 */
    private static void checkPageFollowsIngredient(GameTestHelper helper, VatRecipe recipe,
                                                   List<VatJeiRecipe> pages, VatRecipes.Kind kind) {
        if (recipe.kind() != kind) {
            return;
        }
        for (VatJeiRecipe page : pages) {
            for (VatJeiRecipe.State state : page.states()) {
                int main = shownCount(state.primary());
                int salt = shownCount(state.seasoning());
                if (main < 0 || salt != main) {
                    helper.fail("配方 " + recipe.id() + " 的 JEI 页面盐数(" + salt + ") 和主料(" + main + ") 不一样");
                }
            }
        }
    }

    private static int shownCount(List<List<ItemStack>> layer) {
        if (layer.isEmpty() || layer.get(0).isEmpty()) {
            return -1;
        }
        return layer.get(0).get(0).getCount();
    }

    @GameTest(template = "empty")
    public static void finishedPasteCanBrewVinegar(GameTestHelper helper) {
        Player player = mockPlayer(helper, GameType.CREATIVE);
        VatBlockEntity vat = placeVat(helper);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);

        vat.addWater(3 * VatRecipes.WATER_MB_PER_LEVEL);
        for (int i = 0; i < VatRecipes.PASTE_CHUNKS; i++) {
            vat.addContent(new ItemStack(ModItems.SOY_PASTE_CHUNK.get()));
        }
        for (int i = 0; i < VatRecipes.PASTE_SALT; i++) {
            vat.addContent(new ItemStack(ModItems.SALT.get()));
        }
        vat.setKind(VatRecipes.Kind.PASTE);
        vat.setCover(new ItemStack(Items.WHITE_CARPET));
        VatBrewing.complete(level, pos, vat, VatRecipes.recipeOf(VatRecipes.Kind.PASTE, level));
        if (vat.residueCount() != VatRecipes.RESIDUE_COUNT) {
            helper.fail("大酱酿完缸里应该有 " + VatRecipes.RESIDUE_COUNT + " 份酱渣，实际 " + vat.residueCount());
        }
        vat.setProductMb(com.gunmu.northeast_china_delight.fluid.ModFluids.soyPasteSource(), 0);
        vat.takeCover();

        if (VatBrewing.forInsert(vat, new ItemStack(ModItems.CORN_SEEDS.get())) == null) {
            helper.fail("酿完大酱的缸加不进谷物：二次发酵被 forInsert 挡掉了");
        }

        // 醋要一缸水：3 层，一桶一层
        for (int i = 0; i < 3; i++) {
            click(helper, player, new ItemStack(Items.WATER_BUCKET));
        }
        for (int i = 0; i < VatRecipes.VINEGAR_GRAIN_COUNT; i++) {
            click(helper, player, new ItemStack(ModItems.CORN_SEEDS.get()));
        }
        Item cloth = clothRug();
        if (cloth == null) {
            helper.succeed();
            return;
        }
        click(helper, player, new ItemStack(cloth));

        if (!vat.isCovered() || !vat.cover().is(cloth)) {
            helper.fail("酱渣缸没有接收粗布毯");
        }
        VatRecipe vinegarRecipe = VatBrewing.ready(vat);
        if (vinegarRecipe == null || vinegarRecipe.kind() != VatRecipes.Kind.VINEGAR
                || vat.kind() != VatRecipes.Kind.VINEGAR) {
            helper.fail("酱渣 + 谷物 + 水 + 粗布毯没有开工（配方也没认出来）");
        }
        assertScheduled(helper, level, pos);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void soySauceResidueRequiresGrainBeforeCloth(GameTestHelper helper) {
        Player player = mockPlayer(helper, GameType.CREATIVE);
        VatBlockEntity vat = placeVat(helper);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);

        vat.addWater(3 * VatRecipes.WATER_MB_PER_LEVEL);
        for (int i = 0; i < VatRecipes.PASTE_CHUNKS; i++) {
            vat.addContent(new ItemStack(ModItems.SOY_PASTE_CHUNK.get()));
        }
        for (int i = 0; i < VatRecipes.PASTE_SALT; i++) {
            vat.addContent(new ItemStack(ModItems.SALT.get()));
        }
        vat.addContent(new ItemStack(Items.WHEAT));
        vat.setKind(VatRecipes.Kind.SOY_SAUCE);
        vat.setCover(new ItemStack(Items.WHITE_CARPET));
        VatBrewing.complete(level, pos, vat, VatRecipes.recipeOf(VatRecipes.Kind.SOY_SAUCE, level));
        if (vat.residueCount() != VatRecipes.RESIDUE_COUNT) {
            helper.fail("酱油酿完酱渣数量不对");
        }
        vat.setProductMb(com.gunmu.northeast_china_delight.fluid.ModFluids.soySauceSource(), 0);
        vat.takeCover();
        for (int i = 0; i < 3; i++) {
            click(helper, player, new ItemStack(Items.WATER_BUCKET));
        }
        if (vat.waterLayers() != 3) {
            helper.fail("酱油酱渣缸未能续满三层水");
        }
        for (int i = 0; i < VatRecipes.VINEGAR_GRAIN_COUNT - 1; i++) {
            click(helper, player, new ItemStack(ModItems.CORN_SEEDS.get()));
        }
        Item cloth = clothRug();
        if (cloth == null) {
            helper.succeed();
            return;
        }
        click(helper, player, new ItemStack(cloth));
        if (vat.isCovered()) {
            helper.fail("谷物未齐时，不应盖上醋的粗布毯");
        }

        click(helper, player, new ItemStack(ModItems.CORN_SEEDS.get()));
        click(helper, player, new ItemStack(cloth));
        VatRecipe vinegarRecipe = VatBrewing.ready(vat);
        if (!vat.isCovered() || !vat.cover().is(cloth) || vat.kind() != VatRecipes.Kind.VINEGAR
                || vinegarRecipe == null || vinegarRecipe.kind() != VatRecipes.Kind.VINEGAR) {
            helper.fail("酱油酱渣补齐谷物后应盖粗布毯并开始酿醋");
        }
        assertScheduled(helper, level, pos);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void soySauceResidueStaysForNextRecipe(GameTestHelper helper) {
        Player player = mockPlayer(helper, GameType.CREATIVE);
        VatBlockEntity vat = placeVat(helper);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);
        finishSoySauce(helper, level, pos, vat);
        vat.takeCover();

        click(helper, player, new ItemStack(Items.WATER_BUCKET));
        if (vat.waterLayers() != 0 || vat.soySauceMb() == 0 || vat.residueCount() != VatRecipes.RESIDUE_COUNT) {
            helper.fail("酱油没取完时不应续水，也不应拿走酱渣");
        }
        vat.setProductMb(com.gunmu.northeast_china_delight.fluid.ModFluids.soySauceSource(), VatRecipes.SERVING_MB);
        click(helper, player, new ItemStack(Items.GLASS_BOTTLE));
        click(helper, player, new ItemStack(Items.GLASS_BOTTLE));
        click(helper, player, new ItemStack(Items.DIRT));
        if (vat.soySauceMb() != 0 || vat.residueCount() != VatRecipes.RESIDUE_COUNT) {
            helper.fail("酱油取完后，玻璃瓶或无关物品右键不应拿走酱渣");
        }

        click(helper, player, ItemStack.EMPTY);
        if (vat.residueCount() != VatRecipes.RESIDUE_COUNT - 1) {
            helper.fail("空手仍应能逐份取出酱渣");
        }
        vat.addContent(new ItemStack(ModItems.SOY_RESIDUE.get()));
        for (int i = 0; i < 3; i++) {
            click(helper, player, new ItemStack(Items.WATER_BUCKET));
        }
        for (int i = 0; i < VatRecipes.VINEGAR_GRAIN_COUNT; i++) {
            click(helper, player, new ItemStack(ModItems.CORN_SEEDS.get()));
        }
        Item cloth = clothRug();
        if (cloth == null) {
            helper.succeed();
            return;
        }
        click(helper, player, new ItemStack(cloth));
        VatRecipe vinegar = VatBrewing.ready(vat);
        if (vat.residueCount() != VatRecipes.RESIDUE_COUNT || !vat.isCovered()
                || vinegar == null || vinegar.kind() != VatRecipes.Kind.VINEGAR
                || vat.kind() != VatRecipes.Kind.VINEGAR) {
            helper.fail("酱渣留在缸里时，加水、加谷物并盖粗布毯后应开始酿醋");
        }
        assertScheduled(helper, level, pos);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void finishedProductBlocksFollowUpUntilDrained(GameTestHelper helper) {
        Player player = mockPlayer(helper, GameType.CREATIVE);
        VatBlockEntity vat = placeVat(helper);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);
        finishSoySauce(helper, level, pos, vat);
        vat.takeCover();

        if (vat.brewedMb() <= 0 || !vat.hasBottledProduct()) {
            helper.fail("酱油完成后应记录在新成品槽里");
        }
        if (VatBrewing.forInsert(vat, new ItemStack(ModItems.CORN_SEEDS.get())) != null
                || VatBrewing.ready(vat) != null) {
            helper.fail("成品液体没取完时不应接下一条配方");
        }
        click(helper, player, new ItemStack(Items.WATER_BUCKET));
        if (vat.waterLayers() != 0) {
            helper.fail("成品液体没取完时不应继续加水");
        }
        Item cloth = clothRug();
        if (cloth != null) {
            click(helper, player, new ItemStack(cloth));
            if (vat.isCovered()) {
                helper.fail("成品液体没取完时不应封口开始下一道工序");
            }
        }

        vat.setProductMb(ModFluids.soySauceSource(), 0);
        if (VatBrewing.forInsert(vat, new ItemStack(ModItems.CORN_SEEDS.get())) == null) {
            helper.fail("成品取完后酱渣应能接上醋的配方");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void processingVatRejectsWaterBeyondRecipeLimit(GameTestHelper helper) {
        Player player = mockPlayer(helper, GameType.CREATIVE);
        VatBlockEntity vat = placeVat(helper);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);
        Item cloth = clothRug();
        if (cloth == null) {
            helper.succeed();
            return;
        }

        vat.addContent(new ItemStack(ModItems.SOYBEAN.get()));
        click(helper, player, new ItemStack(Items.WATER_BUCKET));
        click(helper, player, new ItemStack(cloth));
        VatRecipe recipe = VatBrewing.ready(vat);
        if (recipe == null || vat.recipeId() == null || vat.waterLayers() != 1) {
            helper.fail("豆芽应在一层水 + 粗布毯后开工");
        }

        click(helper, player, new ItemStack(Items.WATER_BUCKET));
        if (vat.waterLayers() != 1 || VatBrewing.ready(vat) == null) {
            helper.fail("豆芽只允许一层水，开工后第二桶水必须被拒绝");
        }
        assertScheduled(helper, level, pos);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stalledProgressReturnsToZeroOnInteraction(GameTestHelper helper) {
        Player player = mockPlayer(helper, GameType.CREATIVE);
        VatBlockEntity vat = placeVat(helper);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);
        Item cloth = clothRug();
        if (cloth == null) {
            helper.succeed();
            return;
        }

        vat.addContent(new ItemStack(ModItems.SOYBEAN.get()));
        vat.setKind(VatRecipes.Kind.BEAN_SPROUTS);
        vat.setCover(new ItemStack(cloth));
        vat.addWater(2 * VatRecipes.WATER_MB_PER_LEVEL);
        level.setBlock(pos, level.getBlockState(pos).setValue(Vat.PROGRESS, 5), 3);
        if (VatBrewing.ready(vat) != null) {
            helper.fail("测试状态应超过豆芽的一层水上限");
        }

        click(helper, player, ItemStack.EMPTY);
        if (level.getBlockState(pos).getValue(Vat.PROGRESS) != 0 || vat.recipeId() != null) {
            helper.fail("配方失配后，空手右键应把进度退回 0 并解除旧配方绑定");
        }
        helper.succeed();
    }

    private static void finishSoySauce(GameTestHelper helper, ServerLevel level, BlockPos pos, VatBlockEntity vat) {
        vat.addWater(3 * VatRecipes.WATER_MB_PER_LEVEL);
        for (int i = 0; i < VatRecipes.PASTE_CHUNKS; i++) {
            vat.addContent(new ItemStack(ModItems.SOY_PASTE_CHUNK.get()));
        }
        for (int i = 0; i < VatRecipes.PASTE_SALT; i++) {
            vat.addContent(new ItemStack(ModItems.SALT.get()));
        }
        vat.addContent(new ItemStack(Items.WHEAT));
        vat.setKind(VatRecipes.Kind.SOY_SAUCE);
        vat.setCover(new ItemStack(Items.WHITE_CARPET));
        VatBrewing.complete(level, pos, vat, VatRecipes.recipeOf(VatRecipes.Kind.SOY_SAUCE, level));
        if (vat.residueCount() != VatRecipes.RESIDUE_COUNT || vat.soySauceMb() <= 0) {
            helper.fail("酱油完成状态不对");
        }
    }

    @GameTest(template = "empty")
    public static void pickleSourWaterCanBrewWhiteVinegar(GameTestHelper helper) {
        Player player = mockPlayer(helper, GameType.CREATIVE);
        VatBlockEntity vat = placeVat(helper);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);

        vat.addWater(3 * VatRecipes.WATER_MB_PER_LEVEL);
        for (int i = 0; i < 6; i++) {
            vat.addContent(new ItemStack(ModItems.NAPA_CABBAGE.get()));
        }
        for (int i = 0; i < 3; i++) {
            vat.addContent(new ItemStack(ModItems.SALT.get()));
        }
        vat.setKind(VatRecipes.Kind.PICKLE);
        VatBrewing.complete(level, pos, vat, VatRecipes.recipeOf(VatRecipes.Kind.PICKLE, level));
        if (vat.sourWaterMb() < VatRecipes.SOUR_WATER_MB) {
            helper.fail("泡菜腌完应该有酸引水，实际 " + vat.sourWaterMb() + " mB");
        }
        for (int i = 0; i < 6; i++) {
            VatBrewing.takeOne(vat, new java.util.ArrayList<>());
        }
        if (!vat.contents().isEmpty()) {
            helper.fail("菜没取干净，还剩 " + vat.contents().size() + " 份");
        }

        if (VatBrewing.forInsert(vat, new ItemStack(ModItems.CORN_SEEDS.get())) == null) {
            helper.fail("泡菜缸里的酸引水加不进谷物：二次发酵被挡掉了");
        }

        for (int i = 0; i < VatRecipes.VINEGAR_GRAIN_COUNT; i++) {
            click(helper, player, new ItemStack(ModItems.CORN_SEEDS.get()));
        }
        Item cloth = clothRug();
        if (cloth == null) {
            helper.succeed();
            return;
        }
        click(helper, player, new ItemStack(cloth));

        if (VatBrewing.ready(vat) == null) {
            helper.fail("酸引水 + 谷物 + 粗布毯没有开工（配方也没认出来）");
        }
        assertScheduled(helper, level, pos);
        helper.succeed();
    }

    /** 先盖地毯、后倒水：倒完水就该开工（以前加水分支不会触发开工） */
    @GameTest(template = "empty")
    public static void waterAfterSealingStartsBrew(GameTestHelper helper) {
        Player player = mockPlayer(helper, GameType.CREATIVE);
        VatBlockEntity vat = placeVat(helper);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);

        for (int i = 0; i < VatRecipes.PASTE_CHUNKS; i++) {
            vat.addContent(new ItemStack(ModItems.SOY_PASTE_CHUNK.get()));
        }
        for (int i = 0; i < VatRecipes.PASTE_SALT; i++) {
            vat.addContent(new ItemStack(ModItems.SALT.get()));
        }
        vat.setKind(VatRecipes.Kind.PASTE);
        click(helper, player, new ItemStack(Items.WHITE_CARPET));
        if (!vat.isCovered()) {
            helper.fail("羊毛地毯没能盖到缸上");
        }
        for (int i = 0; i < 3; i++) {
            click(helper, player, new ItemStack(Items.WATER_BUCKET));
        }
        if (vat.waterLayers() != 3) {
            helper.fail("三桶水应该正好三层，实际 " + vat.waterLayers() + " 层");
        }
        assertScheduled(helper, level, pos);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void coverComesBackBeforeFermented(GameTestHelper helper) {
        Player player = mockPlayer(helper, GameType.CREATIVE);
        VatBlockEntity vat = placeVat(helper);
        vat.addContent(new ItemStack(ModItems.SOY_PASTE_CHUNK.get()));
        vat.setKind(VatRecipes.Kind.PASTE);
        vat.setCover(new ItemStack(Items.WHITE_CARPET));

        click(helper, player, ItemStack.EMPTY);
        if (vat.isCovered()) {
            helper.fail("没发酵的缸取不回盖布");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vatCollisionStaysOpenUntilProductionStarts(GameTestHelper helper) {
        placeVat(helper);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(AT);
        BlockState idle = level.getBlockState(pos);
        if (idle.getCollisionShape(level, pos).max(Direction.Axis.Y, 0.5, 0.5) > 0.2) {
            helper.fail("没开工的大缸碰撞箱应该留出缸口");
        }
        BlockState brewing = idle.setValue(Vat.PROGRESS, 1);
        if (brewing.getCollisionShape(level, pos).max(Direction.Axis.Y, 0.5, 0.5) < 0.99) {
            helper.fail("制作开始后大缸碰撞箱应该封上");
        }
        BlockState done = idle.setValue(Vat.FERMENTED, true);
        if (done.getCollisionShape(level, pos).max(Direction.Axis.Y, 0.5, 0.5) < 0.99) {
            helper.fail("制作完成后大缸碰撞箱应该封上");
        }
        helper.succeed();
    }

    private static VatBlockEntity placeVat(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.VAT.get());
        return (VatBlockEntity) helper.getBlockEntity(AT);
    }

    private static void click(GameTestHelper helper, Player player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.useBlock(AT, player);
    }

    private static void assertScheduled(GameTestHelper helper, ServerLevel level, BlockPos pos) {
        if (!level.getBlockTicks().hasScheduledTick(pos, ModBlocks.VAT.get())) {
            helper.fail("条件齐了但没排上工序：这一缸不会自己往下走");
        }
    }

    private static Item clothRug() {
        Item rug = BuiltInRegistries.ITEM.get(
                DdIds.of("farmersdelight", "canvas_rug"));
        return rug == Items.AIR ? null : rug;
    }
}
