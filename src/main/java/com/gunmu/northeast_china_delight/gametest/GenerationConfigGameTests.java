package com.gunmu.northeast_china_delight.gametest;

import com.gunmu.chinese_regional_culinary_ingredients.item.CrciItems;
import com.gunmu.chinese_regional_culinary_ingredients.region.CrciAvailability;
import com.gunmu.chinese_regional_culinary_ingredients.region.CrciUseSite;
import com.gunmu.chinese_regional_culinary_ingredients.worldgen.CrciFarmRegistry;
import com.gunmu.chinese_regional_culinary_ingredients.worldgen.RegionalFarmCropProcessor;

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
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} else {
/*import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@GameTestHolder(NortheastChinaDelight.MODID)
@PrefixGameTestTemplate(false)
public final class GenerationConfigGameTests {
    private static final List<String> FARM_LISTS = List.of("minecraft:farm_plains", "minecraft:farm_taiga",
            "minecraft:farm_snowy", "minecraft:farm_savanna", "minecraft:farm_desert",
            "chinese_regional_culinary_ingredients:farm_taiga_small");
    private static final List<String> FARM_POOLS = List.of("plains", "taiga", "snowy", "savanna", "desert",
            "taiga_small");
    private static final List<String> COURTYARD_TABLES = List.of(
            "northeast_china_delight:chests/courtyard_kitchen",
            "northeast_china_delight:chests/courtyard_house",
            "northeast_china_delight:chests/courtyard_granary",
            "northeast_china_delight:chests/courtyard_disc",
            // 厨房表每格先按权重挑一张子表再抽（2026-11-06）：子表里的物品同样要在默认名单里
            "northeast_china_delight:chests/courtyard_kitchen_staples",
            "northeast_china_delight:chests/courtyard_kitchen_desserts",
            "northeast_china_delight:chests/courtyard_kitchen_dishes",
            "northeast_china_delight:chests/courtyard_kitchen_feasts");

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

    private static void withChestConfig(boolean enabled, List<String> items, Runnable checks) {
        NortheastChinaConfig.setChestLootTestOverride(enabled, items);
        try {
            checks.run();
        } finally {
            NortheastChinaConfig.setChestLootTestOverride(null, null);
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
        return builder.create(null);

        //?} else {
        /*return builder.create(Optional.empty());
        *///?}
    }

    private static LootTable table(GameTestHelper helper, String tableId) {
        //? if <1.20.5 {
        return helper.getLevel().getServer().getLootData().getLootTable(DdIds.parse(tableId));

        //?} else {
        /*return helper.getLevel().getServer().reloadableRegistries()
                .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, DdIds.parse(tableId)));
        *///?}
    }

    private static boolean ownItem(ItemStack stack) {
        String namespace = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
        return namespace.equals(NortheastChinaDelight.MODID)
                || namespace.equals("chinese_regional_culinary_ingredients");
    }

    private static ObjectArrayList<ItemStack> sampleLoot() {
        return new ObjectArrayList<>(List.of(new ItemStack(CrciItems.SOYBEAN.get()),
                new ItemStack(CrciItems.SALT.get()), new ItemStack(Items.WHEAT),
                new ItemStack(BuiltInRegistries.ITEM.get(DdIds.parse("farmersdelight:cabbage")))));
    }

    private static List<LootModifier> injectors() {
        return List.of(new StructureSeedsLootModifier(new LootItemCondition[0]),
                new VillageSeedsLootModifier(new LootItemCondition[0]),
                new ButcherLootModifier(new LootItemCondition[0]),
                new FishermanLootModifier(new LootItemCondition[0]));
    }

    @GameTest(template = "empty")
    public static void farmProcessorsKeepMigratedReplacementChances(GameTestHelper helper) {
        Map<String, Float> expected = Map.of(
                "plains", 0.3352F,
                "taiga", 0.367F,
                "snowy", 0.4596F,
                "savanna", 0.2262F,
                "desert", 0.2256F,
                "taiga_small", 0.4686F);
        for (String listId : FARM_LISTS) {
            var processors = helper.getLevel().registryAccess().registryOrThrow(Registries.PROCESSOR_LIST)
                    .get(DdIds.parse(listId));
            helper.assertTrue(processors != null, "农田处理器必须成功加载：" + listId);
            var regional = processors.list().stream()
                    .filter(processor -> processor instanceof RegionalFarmCropProcessor)
                    .map(processor -> (RegionalFarmCropProcessor) processor)
                    .findFirst().orElseThrow();
            String pool = regional.pool();
            helper.assertTrue(expected.containsKey(pool), "未知的农田气候池：" + pool);
            helper.assertTrue(Math.abs(regional.replacementChance() - expected.get(pool)) < 0.0001F,
                    "迁移后必须保持原有总替换概率：" + listId);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void farmDefaultsCoverEveryClimate(GameTestHelper helper) {
        for (String pool : FARM_POOLS) {
            var entries = CrciFarmRegistry.entries(pool);
            helper.assertTrue(!entries.isEmpty(), "每个气候池都必须有已启用的默认作物：" + pool);
            for (var entry : entries) {
                helper.assertTrue(BuiltInRegistries.ITEM.containsKey(entry.seed()),
                        "农田作物种子必须已注册：" + entry.seed());
                helper.assertTrue(!entry.state().isAir(), "农田作物状态不能是空气：" + entry.seed());
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void farmDisabledPreservesOtherProcessors(GameTestHelper helper) {
        for (String listId : FARM_LISTS) {
            var processors = helper.getLevel().registryAccess().registryOrThrow(Registries.PROCESSOR_LIST)
                    .get(DdIds.parse(listId));
            helper.assertTrue(processors != null && !processors.list().isEmpty(),
                    "农田处理器必须成功加载：" + listId);
            boolean hasRegional = processors.list().stream()
                    .anyMatch(processor -> processor instanceof RegionalFarmCropProcessor);
            helper.assertTrue(hasRegional, "处理器列表必须包含 CRCI 地区作物处理器：" + listId);
            if (!listId.endsWith("farm_taiga_small")) {
                boolean hasVanilla = processors.list().stream().anyMatch(processor -> processor instanceof RuleProcessor);
                helper.assertTrue(hasVanilla, "原版气候农田必须保留原有胡萝卜/马铃薯/甜菜规则：" + listId);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void farmEmptyListBlocksAllCrops(GameTestHelper helper) {
        var processor = new RegionalFarmCropProcessor("missing_pool", 1.0F);
        var settings = new StructurePlaceSettings();
        settings.setRandom(RandomSource.create(917));
        var info = new StructureTemplate.StructureBlockInfo(BlockPos.ZERO, Blocks.WHEAT.defaultBlockState(), null);
        var result = processor.processBlock(helper.getLevel(), BlockPos.ZERO, info.pos(), info, info, settings);
        helper.assertTrue(result != null && result.state().is(Blocks.WHEAT),
                "池为空时不得替换原版小麦，也不得抛错");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void farmWhitelistSelectsOnlyNamedCrops(GameTestHelper helper) {
        for (String pool : FARM_POOLS) {
            for (var entry : CrciFarmRegistry.entries(pool)) {
                helper.assertTrue(CrciAvailability.isIdAvailable(entry.seed(), CrciUseSite.FARM),
                        "农田池不得包含未启用种子：" + entry.seed());
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chestDefaultsCoverExistingChestTables(GameTestHelper helper) {
        var allowed = Set.copyOf(NortheastChinaConfig.DEFAULT_CHEST_LOOT_ITEMS);
        helper.assertTrue(allowed.size() == 139, "默认箱子列表应包含全部 139 种现有候选");
        for (String id : allowed) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(DdIds.parse(id)), "默认候选必须是已注册物品：" + id);
        }
        for (String tableId : COURTYARD_TABLES) {
            var id = DdIds.parse(tableId);
            //? if <1.20.5 {
            String folder = "loot_tables";

            //?} else {
            /*String folder = "loot_table";
            *///?}
            var json = resource(id.getNamespace() + "/" + folder + "/" + id.getPath() + ".json");
            for (var pool : json.getAsJsonArray("pools")) {
                for (var entry : pool.getAsJsonObject().getAsJsonArray("entries")) {
                    var value = entry.getAsJsonObject();
                    // 引用子表的条目（minecraft:loot_table）写的 name 是表名，不是物品，子表自己在 COURTYARD_TABLES 里查
                    boolean nested = value.has("type") && value.get("type").getAsString().equals("minecraft:loot_table");
                    if (!nested && value.has("name") && value.get("name").getAsString().startsWith(NortheastChinaDelight.MODID + ":")) {
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
        withChestConfig(true, List.of("chinese_regional_culinary_ingredients:soybean"), () -> {
            var loot = sampleLoot();
            new ChestLootConfigModifier(new LootItemCondition[0]).apply(loot, context(helper, "minecraft:chests/simple_dungeon", 3));
            helper.assertTrue(loot.size() == 3 && loot.get(0).is(CrciItems.SOYBEAN.get())
                    && loot.stream().noneMatch(stack -> stack.is(CrciItems.SALT.get())), "箱子必须按物品 ID 筛选");
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chestLimitsEachInjectionPool(GameTestHelper helper) {
        var modifiers = injectors();
        var targets = List.of("corn_seeds", "corn_seeds", "pork_ribs", "shrimp");
        for (int index = 0; index < modifiers.size(); index++) {
            var modifier = modifiers.get(index);
            String allowed = "chinese_regional_culinary_ingredients:" + targets.get(index);
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

}
