package com.gunmu.northeast_china_delight.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gunmu.northeast_china_delight.NortheastChinaConfig;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.loot.ButcherLootModifier;
import com.gunmu.northeast_china_delight.loot.ChestLootConfigModifier;
import com.gunmu.northeast_china_delight.loot.FishermanLootModifier;
import com.gunmu.northeast_china_delight.loot.StructureSeedsLootModifier;
import com.gunmu.northeast_china_delight.loot.VillageSeedsLootModifier;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
//? if <1.20.2 {
/*import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@GameTestHolder(NortheastChinaDelight.MODID)
@PrefixGameTestTemplate(false)
public final class GenerationConfigGameTests {
    private static final List<String> FARM_LISTS = List.of("minecraft:farm_plains", "minecraft:farm_taiga",
            "minecraft:farm_snowy", "minecraft:farm_savanna", "minecraft:farm_desert",
            "northeast_china_delight:farm_taiga_small");
    private static final List<String> COURTYARD_TABLES = List.of(
            "northeast_china_delight:chests/courtyard_kitchen",
            "northeast_china_delight:chests/courtyard_house",
            "northeast_china_delight:chests/courtyard_granary",
            "northeast_china_delight:chests/courtyard_disc");

    private GenerationConfigGameTests() {
    }

    private static JsonObject resource(String path) {
        try (var reader = new InputStreamReader(Objects.requireNonNull(
                GenerationConfigGameTests.class.getResourceAsStream("/data/" + path)), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取测试资源：" + path, exception);
        }
    }

    private static List<JsonObject> farmRules() {
        List<JsonObject> rules = new ArrayList<>();
        for (String listId : FARM_LISTS) {
            var id = DdIds.parse(listId);
            var json = resource(id.getNamespace() + "/worldgen/processor_list/" + id.getPath() + ".json");
            for (var processor : json.getAsJsonArray("processors")) {
                for (var rule : processor.getAsJsonObject().getAsJsonArray("rules")) {
                    var value = rule.getAsJsonObject();
                    if (value.getAsJsonObject("output_state").get("Name").getAsString()
                            .startsWith(NortheastChinaDelight.MODID + ":")) {
                        rules.add(value);
                    }
                }
            }
        }
        return rules;
    }

    private static RuleTest predicate(JsonObject rule, boolean certain) {
        var input = rule.getAsJsonObject("input_predicate").deepCopy();
        if (certain) {
            input.addProperty("probability", 1.0F);
        }
        return RuleTest.CODEC.parse(JsonOps.INSTANCE, input).result()
                .orElseThrow(() -> new IllegalStateException("农田规则无法解码：" + input));
    }

    private static Block inputBlock(JsonObject rule) {
        return BuiltInRegistries.BLOCK.get(DdIds.parse(
                rule.getAsJsonObject("input_predicate").get("block").getAsString()));
    }

    private static void withFarmConfig(boolean enabled, List<String> crops, Runnable checks) {
        boolean previousEnabled = NortheastChinaConfig.VILLAGE_FARM_CROPS_ENABLED.get();
        var previousCrops = NortheastChinaConfig.VILLAGE_FARM_ALLOWED_CROPS.get();
        NortheastChinaConfig.VILLAGE_FARM_CROPS_ENABLED.set(enabled);
        NortheastChinaConfig.VILLAGE_FARM_ALLOWED_CROPS.set(crops);
        try {
            checks.run();
        } finally {
            NortheastChinaConfig.VILLAGE_FARM_CROPS_ENABLED.set(previousEnabled);
            NortheastChinaConfig.VILLAGE_FARM_ALLOWED_CROPS.set(previousCrops);
        }
    }

    private static void withChestConfig(boolean enabled, List<String> items, Runnable checks) {
        boolean previousEnabled = NortheastChinaConfig.CHEST_LOOT_ENABLED.get();
        var previousItems = NortheastChinaConfig.CHEST_LOOT_ALLOWED_ITEMS.get();
        NortheastChinaConfig.CHEST_LOOT_ENABLED.set(enabled);
        NortheastChinaConfig.CHEST_LOOT_ALLOWED_ITEMS.set(items);
        try {
            checks.run();
        } finally {
            NortheastChinaConfig.CHEST_LOOT_ENABLED.set(previousEnabled);
            NortheastChinaConfig.CHEST_LOOT_ALLOWED_ITEMS.set(previousItems);
        }
    }

    private static LootParams chestParams(GameTestHelper helper) {
        return new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .create(LootContextParamSets.CHEST);
    }

    private static LootContext context(GameTestHelper helper, String tableId, long seed) {
        var builder = new LootContext.Builder(chestParams(helper))
                .withQueriedLootTableId(DdIds.parse(tableId)).withOptionalRandomSeed(seed);
        //? if <1.20.5 {
        /*return builder.create(null);
        *///?} else {
        return builder.create(Optional.empty());
        //?}
    }

    private static LootTable table(GameTestHelper helper, String tableId) {
        //? if <1.20.5 {
        /*return helper.getLevel().getServer().getLootData().getLootTable(DdIds.parse(tableId));
        *///?} else {
        return helper.getLevel().getServer().reloadableRegistries()
                .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, DdIds.parse(tableId)));
        //?}
    }

    private static boolean ownItem(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(NortheastChinaDelight.MODID);
    }

    private static ObjectArrayList<ItemStack> sampleLoot() {
        return new ObjectArrayList<>(List.of(new ItemStack(ModItems.SOYBEAN.get()),
                new ItemStack(ModItems.SALT.get()), new ItemStack(Items.WHEAT),
                new ItemStack(BuiltInRegistries.ITEM.get(DdIds.parse("farmersdelight:cabbage")))));
    }

    private static List<LootModifier> injectors() {
        return List.of(new StructureSeedsLootModifier(new LootItemCondition[0]),
                new VillageSeedsLootModifier(new LootItemCondition[0]),
                new ButcherLootModifier(new LootItemCondition[0]),
                new FishermanLootModifier(new LootItemCondition[0]));
    }

    @GameTest(template = "empty")
    public static void farmDefaultRuleProbabilityUnchanged(GameTestHelper helper) {
        withFarmConfig(true, NortheastChinaConfig.DEFAULT_VILLAGE_FARM_CROPS, () -> {
            for (var rule : farmRules()) {
                var configured = predicate(rule, false);
                var original = new RandomBlockMatchTest(inputBlock(rule),
                        rule.getAsJsonObject("input_predicate").get("probability").getAsFloat());
                var configuredRandom = RandomSource.create(731);
                var originalRandom = RandomSource.create(731);
                for (int sample = 0; sample < 128; sample++) {
                    var state = sample % 3 == 0 ? Blocks.STONE.defaultBlockState() : inputBlock(rule).defaultBlockState();
                    helper.assertTrue(configured.test(state, configuredRandom) == original.test(state, originalRandom),
                            "默认配置不得改变农田原有概率或随机数消耗");
                }
            }
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void farmDefaultsCoverEveryClimate(GameTestHelper helper) {
        Set<String> seen = new HashSet<>();
        var rules = farmRules();
        helper.assertTrue(rules.size() == 30, "六份处理器应覆盖 30 条本模组作物规则");
        for (var rule : rules) {
            String crop = rule.getAsJsonObject("input_predicate").get("crop_item").getAsString();
            seen.add(crop);
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(DdIds.parse(crop)), "播种物品必须已注册：" + crop);
            var decoded = predicate(rule, false);
            var encoded = RuleTest.CODEC.encodeStart(JsonOps.INSTANCE, decoded).result().orElseThrow();
            helper.assertTrue(encoded.getAsJsonObject().get("crop_item").getAsString().equals(crop),
                    "农田自定义规则必须能通过注册表双向编解码");
        }
        helper.assertTrue(seen.equals(Set.copyOf(NortheastChinaConfig.DEFAULT_VILLAGE_FARM_CROPS)),
                "默认允许列表必须包含全部 15 种农田作物");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void farmDisabledPreservesOtherProcessors(GameTestHelper helper) {
        withFarmConfig(false, NortheastChinaConfig.DEFAULT_VILLAGE_FARM_CROPS, () -> {
            int retainedReplacements = 0;
            for (String listId : FARM_LISTS) {
                var processors = helper.getLevel().registryAccess().registryOrThrow(Registries.PROCESSOR_LIST)
                        .get(DdIds.parse(listId));
                helper.assertTrue(processors != null, "农田处理器必须成功加载：" + listId);
                Block source = listId.endsWith("farm_taiga_small") ? Blocks.PUMPKIN_STEM : Blocks.WHEAT;
                for (int sample = 0; sample < 128; sample++) {
                    var pos = helper.absolutePos(new BlockPos(sample % 16, 2, sample / 16));
                    var original = new StructureTemplate.StructureBlockInfo(BlockPos.ZERO, source.defaultBlockState(), null);
                    var result = new StructureTemplate.StructureBlockInfo(pos, source.defaultBlockState(), null);
                    for (var processor : processors.list()) {
                        result = processor.processBlock(helper.getLevel(), BlockPos.ZERO, pos, original, result,
                                new StructurePlaceSettings());
                    }
                    helper.assertTrue(result != null && !BuiltInRegistries.BLOCK.getKey(result.state().getBlock())
                            .getNamespace().equals(NortheastChinaDelight.MODID), "关闭后不得替换成本模组作物");
                    if (!result.state().is(source)) {
                        retainedReplacements++;
                    }
                }
            }
            helper.assertTrue(retainedReplacements > 0, "关闭本模组后原版/农夫乐事的替换应仍生效");
            Block foreignCrop = BuiltInRegistries.BLOCK.stream().filter(block -> block instanceof CropBlock
                    && BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("farmersdelight"))
                    .findFirst().orElseThrow();
            var configured = predicate(farmRules().get(0), true);
            var pipeline = new RuleProcessor(List.of(
                    new ProcessorRule(configured, AlwaysTrueTest.INSTANCE, Blocks.STONE.defaultBlockState()),
                    new ProcessorRule(new BlockMatchTest(Blocks.WHEAT), AlwaysTrueTest.INSTANCE, foreignCrop.defaultBlockState())));
            var info = new StructureTemplate.StructureBlockInfo(helper.absolutePos(BlockPos.ZERO), Blocks.WHEAT.defaultBlockState(), null);
            var result = pipeline.processBlock(helper.getLevel(), BlockPos.ZERO, info.pos(), info, info, new StructurePlaceSettings());
            helper.assertTrue(result != null && result.state().is(foreignCrop), "关闭时必须让后续其他模组规则继续匹配");
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void farmEmptyListBlocksAllCrops(GameTestHelper helper) {
        withFarmConfig(true, List.of(), () -> {
            for (var rule : farmRules()) {
                var random = RandomSource.create(917);
                helper.assertTrue(!predicate(rule, true).test(inputBlock(rule).defaultBlockState(), random),
                        "空列表不得生成本模组农田作物");
                helper.assertTrue(random.nextInt() == RandomSource.create(917).nextInt(),
                        "跳过的农田规则不得消耗其他模组使用的随机数");
            }
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void farmWhitelistSelectsOnlyNamedCrops(GameTestHelper helper) {
        String allowed = "northeast_china_delight:green_radish_seeds";
        withFarmConfig(true, List.of(allowed, "northeast_china_delight:not_registered"), () -> {
            for (var rule : farmRules()) {
                boolean expected = rule.getAsJsonObject("input_predicate").get("crop_item").getAsString().equals(allowed);
                helper.assertTrue(predicate(rule, true).test(inputBlock(rule).defaultBlockState(), RandomSource.create(11)) == expected,
                        "所有气候应只保留允许列表中的作物，未知 ID 安全无效");
            }
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chestDefaultsCoverExistingChestTables(GameTestHelper helper) {
        var allowed = Set.copyOf(NortheastChinaConfig.DEFAULT_CHEST_LOOT_ITEMS);
        helper.assertTrue(allowed.size() == 69, "默认箱子列表应包含全部 69 种现有候选");
        for (String id : allowed) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(DdIds.parse(id)), "默认候选必须是已注册物品：" + id);
        }
        for (String tableId : COURTYARD_TABLES) {
            var id = DdIds.parse(tableId);
            //? if <1.20.5 {
            /*String folder = "loot_tables";
            *///?} else {
            String folder = "loot_table";
            //?}
            var json = resource(id.getNamespace() + "/" + folder + "/" + id.getPath() + ".json");
            for (var pool : json.getAsJsonArray("pools")) {
                for (var entry : pool.getAsJsonObject().getAsJsonArray("entries")) {
                    var value = entry.getAsJsonObject();
                    if (value.has("name") && value.get("name").getAsString().startsWith(NortheastChinaDelight.MODID + ":")) {
                        helper.assertTrue(allowed.contains(value.get("name").getAsString()), "小院箱子候选不得遗漏");
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chestDisabledKeepsVanillaAndOtherMods(GameTestHelper helper) {
        withChestConfig(false, NortheastChinaConfig.DEFAULT_CHEST_LOOT_ITEMS, () -> {
            var loot = sampleLoot();
            helper.assertTrue(!loot.get(3).isEmpty(), "测试必须包含农夫乐事物品");
            new ChestLootConfigModifier(new LootItemCondition[0]).apply(loot, context(helper, "minecraft:chests/simple_dungeon", 1));
            helper.assertTrue(loot.size() == 2 && loot.get(0).is(Items.WHEAT)
                    && BuiltInRegistries.ITEM.getKey(loot.get(1).getItem()).getNamespace().equals("farmersdelight"),
                    "关闭箱子生成时只移除本模组物品");
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chestEmptyListBlocksOwnItems(GameTestHelper helper) {
        withChestConfig(true, List.of(), () -> {
            var loot = sampleLoot();
            new ChestLootConfigModifier(new LootItemCondition[0]).apply(loot, context(helper, "minecraft:chests/simple_dungeon", 2));
            helper.assertTrue(loot.size() == 2 && loot.stream().noneMatch(GenerationConfigGameTests::ownItem),
                    "开关开启但列表为空时也不得生成本模组箱子物品");
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chestWhitelistSelectsOnlyNamedItems(GameTestHelper helper) {
        withChestConfig(true, List.of("northeast_china_delight:soybean"), () -> {
            var loot = sampleLoot();
            new ChestLootConfigModifier(new LootItemCondition[0]).apply(loot, context(helper, "minecraft:chests/simple_dungeon", 3));
            helper.assertTrue(loot.size() == 3 && loot.get(0).is(ModItems.SOYBEAN.get())
                    && loot.stream().noneMatch(stack -> stack.is(ModItems.SALT.get())), "箱子必须按物品 ID 筛选");
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chestLimitsEachInjectionPool(GameTestHelper helper) {
        var modifiers = injectors();
        var targets = List.of("corn_seeds", "corn_seeds", "pork_ribs", "shrimp");
        for (int index = 0; index < modifiers.size(); index++) {
            var modifier = modifiers.get(index);
            String allowed = NortheastChinaDelight.MODID + ":" + targets.get(index);
            withChestConfig(true, List.of(allowed), () -> {
                int kept = 0;
                var seeds = RandomSource.create(591);
                for (int sample = 0; sample < 512; sample++) {
                    var loot = modifier.apply(new ObjectArrayList<>(), context(helper, "minecraft:chests/simple_dungeon", seeds.nextLong()));
                    for (var stack : loot) {
                        helper.assertTrue(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(allowed),
                                "四类箱子注入器都必须遵守允许列表");
                        kept++;
                    }
                }
                helper.assertTrue(kept > 0, "允许的候选仍应能从原有注入池生成：" + modifier.getClass().getSimpleName() + "/" + allowed);
            });
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chestDisabledDoesNotConsumeInjectionRandomness(GameTestHelper helper) {
        withChestConfig(false, NortheastChinaConfig.DEFAULT_CHEST_LOOT_ITEMS, () -> {
            for (var modifier : injectors()) {
                var context = context(helper, "minecraft:chests/simple_dungeon", 101);
                var loot = modifier.apply(new ObjectArrayList<>(), context);
                helper.assertTrue(loot.isEmpty() && context.getRandom().nextInt() == RandomSource.create(101).nextInt(),
                        "关闭箱子注入后不应生成物品或消耗其他模组的随机数");
            }
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chestConfigFiltersCourtyardTables(GameTestHelper helper) {
        for (String tableId : COURTYARD_TABLES) {
            var table = table(helper, tableId);
            helper.assertTrue(table.getParamSet() == LootContextParamSets.CHEST, "小院箱子表必须正确加载");
            var params = chestParams(helper);
            var seeds = RandomSource.create(731);
            int[] introduced = {0};
            for (int sample = 0; sample < 32; sample++) {
                long randomSeed = seeds.nextLong();
                List<String> vanilla = new ArrayList<>();
                withChestConfig(true, NortheastChinaConfig.DEFAULT_CHEST_LOOT_ITEMS, () -> {
                    var loot = table.getRandomItems(params, randomSeed);
                    introduced[0] += (int) loot.stream().filter(GenerationConfigGameTests::ownItem).count();
                    loot.stream().filter(stack -> !ownItem(stack))
                            .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()) + "=" + stack.getCount())
                            .sorted().forEach(vanilla::add);
                });
                withChestConfig(false, NortheastChinaConfig.DEFAULT_CHEST_LOOT_ITEMS, () -> {
                    var loot = table.getRandomItems(params, randomSeed);
                    helper.assertTrue(loot.stream().noneMatch(GenerationConfigGameTests::ownItem),
                            "最终全局过滤必须覆盖自有小院箱子：" + tableId);
                    var retained = loot.stream().map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()) + "=" + stack.getCount())
                            .sorted().toList();
                    helper.assertTrue(retained.equals(vanilla), "关闭后原版箱子物品及其数量不得改变");
                });
            }
            helper.assertTrue(introduced[0] > 0, "默认配置下应实际生成本模组小院箱子物品：" + tableId);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chestConfigDoesNotAffectOtherLootTypes(GameTestHelper helper) {
        withChestConfig(false, List.of(), () -> {
            var modifier = new ChestLootConfigModifier(new LootItemCondition[0]);
            for (String tableId : List.of("minecraft:entities/pig", "minecraft:blocks/wheat", "minecraft:gameplay/fishing")) {
                helper.assertTrue(table(helper, tableId) != LootTable.EMPTY
                        && table(helper, tableId).getParamSet() != LootContextParamSets.CHEST, "测试应使用真实的非箱子表");
                var loot = sampleLoot();
                modifier.apply(loot, context(helper, tableId, 13));
                helper.assertTrue(loot.size() == 4, "不得影响实体、方块或钓鱼掉落：" + tableId);
            }
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void generationSwitchesAreIndependent(GameTestHelper helper) {
        var soybean = DdIds.parse("northeast_china_delight:soybean");
        withFarmConfig(false, NortheastChinaConfig.DEFAULT_VILLAGE_FARM_CROPS, () ->
                withChestConfig(true, NortheastChinaConfig.DEFAULT_CHEST_LOOT_ITEMS, () -> {
                    helper.assertTrue(!NortheastChinaConfig.isVillageFarmCropAllowed(soybean)
                            && NortheastChinaConfig.isChestLootItemAllowed(soybean), "关闭农田不应关闭箱子");
                }));
        withFarmConfig(true, NortheastChinaConfig.DEFAULT_VILLAGE_FARM_CROPS, () ->
                withChestConfig(false, NortheastChinaConfig.DEFAULT_CHEST_LOOT_ITEMS, () -> {
                    helper.assertTrue(NortheastChinaConfig.isVillageFarmCropAllowed(soybean)
                            && !NortheastChinaConfig.isChestLootItemAllowed(soybean), "关闭箱子不应关闭农田");
                }));
        helper.succeed();
    }
}
