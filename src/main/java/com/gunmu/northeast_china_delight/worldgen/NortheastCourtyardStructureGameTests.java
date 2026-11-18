package com.gunmu.northeast_china_delight.worldgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.advancement.ModAdvancements;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
//? if <1.20.2 {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@GameTestHolder(NortheastChinaDelight.MODID)
@PrefixGameTestTemplate(false)
public final class NortheastCourtyardStructureGameTests
{
    private NortheastCourtyardStructureGameTests()
    {
    }

    @GameTest(template = "empty")
    public static void courtyardPlacementUsesKnownShapeToKeepCrops(GameTestHelper helper)
    {
        helper.assertTrue(CourtyardPiece.settings(Rotation.NONE).getKnownShape(),
                "小院模板必须按已知形状放置，避免世界生成阶段作物被光照形状更新删除");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void courtyardRotationSurvivesSerializationAndLegacyReload(GameTestHelper helper)
    {
        StructureTemplateManager templates = helper.getLevel().getStructureManager();
        BlockPos pos = new BlockPos(128, 64, -96);
        String template = NortheastCourtyardStructure.DEFAULT_TEMPLATES.get(0);
        CourtyardPiece original = new CourtyardPiece(templates, template, pos, Rotation.CLOCKWISE_180);
        CompoundTag tag = original.createTag(
                StructurePieceSerializationContext.fromLevel(helper.getLevel()));
        helper.assertTrue(tag.getString("Rotation").equals(Rotation.CLOCKWISE_180.name()),
                "存档必须写入小院旋转字段");
        CourtyardPiece restored = new CourtyardPiece(templates, tag);

        helper.assertTrue(restored.getRotation() == Rotation.CLOCKWISE_180,
                "小院旋转必须随结构件一起保存");
        helper.assertTrue(restored.getBoundingBox().equals(original.getBoundingBox()),
                "读档后的院子包围盒必须与生成时一致");

        tag.remove("Rotation");
        CourtyardPiece legacy = new CourtyardPiece(templates, tag);
        helper.assertTrue(legacy.getRotation() == Rotation.CLOCKWISE_180,
                "旧存档应能从包围盒恢复 180° 旋转");

        CourtyardPiece unrotated = new CourtyardPiece(templates, template, pos, Rotation.NONE);
        CompoundTag unrotatedTag = unrotated.createTag(
                StructurePieceSerializationContext.fromLevel(helper.getLevel()));
        unrotatedTag.remove("Rotation");
        helper.assertTrue(new CourtyardPiece(templates, unrotatedTag).getRotation() == Rotation.NONE,
                "未旋转的旧存档不能误判为 180°");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void secondYardAlwaysUsesADifferentTemplate(GameTestHelper helper)
    {
        for (String first : NortheastCourtyardStructure.DEFAULT_TEMPLATES)
        {
            List<String> pool = NortheastCourtyardStructure.secondTemplatePool(
                    NortheastCourtyardStructure.DEFAULT_TEMPLATES, first);
            helper.assertTrue(pool.size() == NortheastCourtyardStructure.DEFAULT_TEMPLATES.size() - 1,
                    "第二座必须保留除第一座外的全部模板：" + first);
            helper.assertTrue(!pool.contains(first), "第二座模板不得与第一座相同：" + first);
        }
        helper.assertTrue(NortheastCourtyardStructure.secondTemplatePool(List.of("only", "only"), "only").isEmpty(),
                "只有一个模板时无法满足第二座换模板的要求");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void laterYardsDrawFromTheCompleteTemplatePool(GameTestHelper helper)
    {
        List<String> templates = List.of("first", "second", "third", "fourth");
        WorldgenRandom random = new WorldgenRandom(RandomSource.create(20261116L));
        Set<String> seen = new HashSet<>();
        for (int attempt = 0; attempt < 128; attempt++)
        {
            seen.add(NortheastCourtyardStructure.pickTemplate(random, templates));
        }
        helper.assertTrue(seen.size() == templates.size(),
                "第三座起应能重复抽到前面的模板，候选池必须是完整模板列表");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void terrainBoundsKeepYardsAwayFromWorldEdges(GameTestHelper helper)
    {
        int minY = -64;
        int maxY = 320;
        helper.assertTrue(NortheastCourtyardStructure.isTerrainHeightWithinBounds(64, minY, maxY),
                "普通地表高度应允许生成");
        helper.assertTrue(NortheastCourtyardStructure.isTerrainHeightWithinBounds(minY + 12, minY, maxY),
                "刚好高于世界底部安全线的位置应允许生成");
        helper.assertTrue(!NortheastCourtyardStructure.isTerrainHeightWithinBounds(minY + 11, minY, maxY),
                "过低的位置应视为浮空或世界边缘并拒绝");
        helper.assertTrue(NortheastCourtyardStructure.isTerrainHeightWithinBounds(maxY - 32, minY, maxY),
                "刚好低于世界顶部安全线的位置应允许生成");
        helper.assertTrue(!NortheastCourtyardStructure.isTerrainHeightWithinBounds(maxY - 31, minY, maxY),
                "过高的位置应视为浮空或世界边缘并拒绝");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void terrainProbeIgnoresSnowAndTreesButRejectsFluidsAndIce(GameTestHelper helper)
    {
        int minY = -64;
        int maxY = 100;
        NoiseColumn snowyForest = solidColumn(minY, maxY, 60, Blocks.GRASS_BLOCK.defaultBlockState());
        snowyForest.setBlock(61, Blocks.SNOW.defaultBlockState());
        snowyForest.setBlock(62, Blocks.OAK_LEAVES.defaultBlockState());
        snowyForest.setBlock(63, Blocks.OAK_LOG.defaultBlockState());
        helper.assertTrue(NortheastCourtyardStructure.inspectTerrainColumn(snowyForest, minY, maxY)
                        .filter(probe -> probe.groundY() == 60 && !probe.wet()).isPresent(),
                "雪层与树冠不能抬高或污染干燥地表判定");

        NoiseColumn water = solidColumn(minY, maxY, 60, Blocks.GRAVEL.defaultBlockState());
        water.setBlock(61, Blocks.WATER.defaultBlockState());
        water.setBlock(62, Blocks.WATER.defaultBlockState());
        helper.assertTrue(NortheastCourtyardStructure.inspectTerrainColumn(water, minY, maxY)
                        .filter(NortheastCourtyardStructure.TerrainProbe::wet).isPresent(),
                "河流或湖面即使高度相同也必须判为湿区");

        for (BlockState ice : List.of(Blocks.ICE.defaultBlockState(), Blocks.PACKED_ICE.defaultBlockState(),
                Blocks.BLUE_ICE.defaultBlockState(), Blocks.FROSTED_ICE.defaultBlockState()))
        {
            NoiseColumn frozen = solidColumn(minY, maxY, 60, Blocks.GRAVEL.defaultBlockState());
            frozen.setBlock(61, Blocks.WATER.defaultBlockState());
            frozen.setBlock(62, ice);
            helper.assertTrue(NortheastCourtyardStructure.inspectTerrainColumn(frozen, minY, maxY)
                            .filter(NortheastCourtyardStructure.TerrainProbe::wet).isPresent(),
                    "结冰水面不能绕过湿区判定：" + ice);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void gatePathWaterCheckMatchesTheAreaClearingActuallyFills(GameTestHelper helper)
    {
        int minX = 100;
        int minZ = 200;
        int sizeX = 34;
        int sizeZ = 20;
        int centerX = 100 + 34 / 2;
        int southPathEnd = minZ + sizeZ - 1 + YardClearingPiece.GATE_PATH_LENGTH;
        int northPathEnd = minZ - YardClearingPiece.GATE_PATH_LENGTH;
        helper.assertTrue(!NortheastCourtyardStructure.gatePathPasses(minX, minZ, sizeX, sizeZ, 1,
                        (x, z) -> !(x == centerX && z == southPathEnd),
                        YardClearingPiece.GATE_PATH_LENGTH),
                "南门通道内出现水时必须否决候选");
        helper.assertTrue(NortheastCourtyardStructure.gatePathPasses(minX, minZ, sizeX, sizeZ, 1,
                        (x, z) -> !(x == centerX && z == southPathEnd + 1),
                        YardClearingPiece.GATE_PATH_LENGTH),
                "门前通道之外一格的水不属于清场填地范围，不得因此误拒整块院址");
        helper.assertTrue(!NortheastCourtyardStructure.gatePathPasses(minX, minZ, sizeX, sizeZ, -1,
                        (x, z) -> !(x == centerX && z == northPathEnd),
                        YardClearingPiece.GATE_PATH_LENGTH),
                "北门通道也必须按实际清场范围检查水");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void foundationWaterScanCoversEveryColumnIncludingInterior(GameTestHelper helper)
    {
        int sizeX = 34;
        int sizeZ = 20;
        int margin = YardClearingPiece.FOUNDATION_MARGIN;
        Set<Long> visited = new HashSet<>();
        helper.assertTrue(NortheastCourtyardStructure.footprintColumnsPass(sizeX, sizeZ, margin,
                        (dx, dz) -> visited.add(cellKey(dx, dz))),
                "干燥的完整 footprint 应通过逐格检查");
        helper.assertTrue(visited.size() == (sizeX + margin * 2) * (sizeZ + margin * 2),
                "临水检查必须覆盖 footprint 外扩后的每一格，不能再按 2 格采样");
        helper.assertTrue(visited.contains(cellKey(-margin, -margin))
                        && visited.contains(cellKey(sizeX - 1 + margin, sizeZ - 1 + margin)),
                "外扩后的四个边界都必须纳入检查");
        helper.assertTrue(!visited.contains(cellKey(-margin - 1, 0))
                        && !visited.contains(cellKey(sizeX + margin, 0)),
                "检查不得越过实际填地范围继续扩张");

        int holeX = 11;
        int holeZ = 7;
        helper.assertTrue(!NortheastCourtyardStructure.footprintColumnsPass(sizeX, sizeZ, margin,
                        (dx, dz) -> dx != holeX || dz != holeZ),
                "footprint 内部任意一格水都必须否决候选，包括旧采样步长会跳过的奇数坐标");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void foundationFillNeverReplacesWaterOrIce(GameTestHelper helper)
    {
        helper.assertTrue(YardClearingPiece.shouldFillFoundation(Blocks.AIR.defaultBlockState()),
                "地基应填补空气");
        helper.assertTrue(YardClearingPiece.shouldFillFoundation(Blocks.OAK_LEAVES.defaultBlockState()),
                "地基应清掉植被后填补");
        helper.assertTrue(!YardClearingPiece.shouldFillFoundation(Blocks.WATER.defaultBlockState()),
                "水绝不能在地基填充时被换成泥土");
        helper.assertTrue(!YardClearingPiece.shouldFillFoundation(Blocks.ICE.defaultBlockState()),
                "冰绝不能在地基填充时被换成泥土");
        helper.assertTrue(!YardClearingPiece.shouldFillFoundation(Blocks.BLUE_ICE.defaultBlockState()),
                "蓝冰同样不能被填成泥土");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void treeClearingFollowsLeavesBeforeRemovingWholeTrees(GameTestHelper helper)
    {
        BlockPos seedLeaf = new BlockPos(2, 3, 2);
        BlockPos connectedLeaf = new BlockPos(3, 3, 2);
        BlockPos trunkTop = new BlockPos(3, 2, 2);
        BlockPos trunkBottom = new BlockPos(3, 1, 2);
        BlockPos horizontalLog = new BlockPos(2, 4, 2);
        BlockPos isolatedLog = new BlockPos(6, 2, 2);
        BlockPos disconnectedLeaf = new BlockPos(8, 4, 8);
        BlockPos disconnectedLog = new BlockPos(8, 3, 8);
        helper.setBlock(seedLeaf, Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, true));
        helper.setBlock(connectedLeaf, Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, true));
        helper.setBlock(trunkTop, log(Direction.Axis.Y));
        helper.setBlock(trunkBottom, log(Direction.Axis.Y));
        helper.setBlock(horizontalLog, log(Direction.Axis.X));
        helper.setBlock(isolatedLog, log(Direction.Axis.Y));
        helper.setBlock(disconnectedLeaf, Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, true));
        helper.setBlock(disconnectedLog, log(Direction.Axis.Y));

        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        BoundingBox seedBox = new BoundingBox(
                origin.getX() + 1, origin.getY() + 3, origin.getZ() + 1,
                origin.getX() + 2, origin.getY() + 4, origin.getZ() + 2);
        BoundingBox traversalBox = new BoundingBox(
                origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + 10, origin.getY() + 8, origin.getZ() + 10);
        Set<BlockPos> treeBlocks = YardClearingPiece.collectTreeBlocks(
                helper.getLevel(), seedBox, traversalBox);

        helper.assertTrue(treeBlocks.contains(helper.absolutePos(seedLeaf)),
                "种子范围内的树叶必须入选");
        helper.assertTrue(treeBlocks.contains(helper.absolutePos(connectedLeaf)),
                "种子范围外、但与种子树叶相邻的树叶必须继续遍历");
        helper.assertTrue(treeBlocks.contains(helper.absolutePos(trunkTop))
                        && treeBlocks.contains(helper.absolutePos(trunkBottom)),
                "与树叶相邻的原木必须入选，并能沿树干继续向下遍历");
        helper.assertTrue(treeBlocks.contains(helper.absolutePos(horizontalLog)),
                "与树叶相邻的原木不再区分朝向，X/Z 轴原木也必须入选");
        helper.assertTrue(!treeBlocks.contains(helper.absolutePos(isolatedLog)),
                "没有树叶连接、也不是从树叶进入的原木不能直接按范围选中");
        helper.assertTrue(!treeBlocks.contains(helper.absolutePos(disconnectedLeaf))
                        && !treeBlocks.contains(helper.absolutePos(disconnectedLog)),
                "种子范围外且与种子不连通的树不能被扫描进来");
        helper.assertTrue(treeBlocks.size() == 5, "遍历结果不应混入其他方块："
                + treeBlocks.size());
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(trunkBottom))
                        .is(Blocks.OAK_LOG),
                "DFS 遍历阶段只能收集坐标，不能提前修改方块");

        YardClearingPiece.clearTreeBlocks(helper.getLevel(), treeBlocks, traversalBox);
        for (BlockPos pos : List.of(seedLeaf, connectedLeaf, trunkTop, trunkBottom, horizontalLog))
        {
            helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(pos)).isAir(),
                    "整棵树应在遍历完成后统一清空：" + pos);
        }
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(isolatedLog))
                        .is(Blocks.OAK_LOG),
                "未被树叶连到的原木必须保留");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(disconnectedLeaf))
                        .is(Blocks.OAK_LEAVES),
                "未与种子连通的树叶必须保留");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void footprintHeightAllowsGentleSlopeButRejectsBuriedWalls(GameTestHelper helper)
    {
        helper.assertTrue(NortheastCourtyardStructure.isFootprintHeightSuitable(
                        64, 61, 67,
                        NortheastCourtyardStructure.YARD_FOOTPRINT_DROP_LIMIT,
                        NortheastCourtyardStructure.YARD_FOOTPRINT_RISE_LIMIT),
                "中位落位后，低三格、高三格的缓坡不应否决整个院落");
        helper.assertTrue(!NortheastCourtyardStructure.isFootprintHeightSuitable(
                        64, 60, 64,
                        NortheastCourtyardStructure.YARD_FOOTPRINT_DROP_LIMIT,
                        NortheastCourtyardStructure.YARD_FOOTPRINT_RISE_LIMIT),
                "低处超过三格会在院前形成高台，必须拒绝");
        helper.assertTrue(!NortheastCourtyardStructure.isFootprintHeightSuitable(
                        64, 64, 68,
                        NortheastCourtyardStructure.YARD_FOOTPRINT_DROP_LIMIT,
                        NortheastCourtyardStructure.YARD_FOOTPRINT_RISE_LIMIT),
                "高处超过三格会埋住院墙，必须拒绝");
        helper.assertTrue(NortheastCourtyardStructure.isFootprintReliefSuitable(
                        60, 66, NortheastCourtyardStructure.SECOND_YARD_FOOTPRINT_RELIEF_LIMIT),
                "第二座允许六格以内的整体起伏");
        helper.assertTrue(!NortheastCourtyardStructure.isFootprintReliefSuitable(
                        60, 67, NortheastCourtyardStructure.SECOND_YARD_FOOTPRINT_RELIEF_LIMIT),
                "第二座整体起伏超过六格仍应另找落点");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rotatedYardFootprintUsesItsRealMinimumCorner(GameTestHelper helper)
    {
        helper.assertTrue(NortheastCourtyardStructure.footprintMinX(100, 34, Rotation.NONE) == 100,
                "未旋转院子的西边界就是原点");
        helper.assertTrue(NortheastCourtyardStructure.footprintMinZ(100, 34, Rotation.NONE) == 100,
                "未旋转院子的北边界就是原点");
        helper.assertTrue(NortheastCourtyardStructure.footprintMinX(100, 34, Rotation.CLOCKWISE_180) == 67,
                "旋转 180 度后院子向原点西侧展开，碰撞边界必须同步");
        helper.assertTrue(NortheastCourtyardStructure.footprintMinZ(100, 34, Rotation.CLOCKWISE_180) == 67,
                "旋转 180 度后院子向原点北侧展开，碰撞边界必须同步");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rotatedYardGapStepUsesItsRealFacingEdge(GameTestHelper helper)
    {
        int rotatedMin = NortheastCourtyardStructure.footprintMinX(100, 34, Rotation.CLOCKWISE_180);
        helper.assertTrue(NortheastCourtyardStructure.gapStepOrigin(100, 34, 20, 1, 20, 0) == 154,
                "未旋转院子向东步进后，下一座应从东边界外留出间距");
        helper.assertTrue(NortheastCourtyardStructure.gapStepOrigin(rotatedMin, 34, 20, 1, 20, 0) == 121,
                "180 度院子向东步进后，下一座应从真实东边界外留出间距");
        helper.assertTrue(NortheastCourtyardStructure.gapStepOrigin(rotatedMin, 34, 20, -1, 20, 0) == 27,
                "180 度院子向西步进后，下一座应从真实西边界外留出间距");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void clusterFacingKeepsFirstYardDirectionMostOfTheTime(GameTestHelper helper)
    {
        helper.assertTrue(NortheastCourtyardStructure.chooseRotation(
                        Rotation.NONE, true, true, true, true, 0.79F) == Rotation.NONE,
                "两侧都能通行时，南向主门向应在 80% 的掷骰中保持不变");
        helper.assertTrue(NortheastCourtyardStructure.chooseRotation(
                        Rotation.NONE, true, true, true, true, 0.81F) == Rotation.CLOCKWISE_180,
                "南向主门向应有 20% 概率尝试反向，避免院群过于机械");
        helper.assertTrue(NortheastCourtyardStructure.chooseRotation(
                        Rotation.CLOCKWISE_180, true, true, true, true, 0.79F) == Rotation.CLOCKWISE_180,
                "北向主门向同样应在 80% 的掷骰中保持不变");
        helper.assertTrue(NortheastCourtyardStructure.chooseRotation(
                        Rotation.CLOCKWISE_180, true, true, true, true, 0.81F) == Rotation.NONE,
                "北向主门向应有 20% 概率尝试反向");
        helper.assertTrue(NortheastCourtyardStructure.chooseRotation(
                        null, true, true, true, true, 0.49F) == Rotation.NONE,
                "第一座没有主门向时，两向都应可随机参与");
        helper.assertTrue(NortheastCourtyardStructure.chooseRotation(
                        null, true, true, true, true, 0.51F) == Rotation.CLOCKWISE_180,
                "第一座没有主门向时应保留两向各半的随机性");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void clusterFacingFallsBackWhenPreferredGateTerrainIsUnusable(GameTestHelper helper)
    {
        helper.assertTrue(NortheastCourtyardStructure.chooseRotation(
                        Rotation.NONE, false, true, true, true, 0.1F) == Rotation.CLOCKWISE_180,
                "主门向门口地形不可用时应自动反向");
        helper.assertTrue(NortheastCourtyardStructure.chooseRotation(
                        Rotation.CLOCKWISE_180, true, false, true, true, 0.1F) == Rotation.NONE,
                "北向主门不可用时应自动改走南门");
        helper.assertTrue(NortheastCourtyardStructure.chooseRotation(
                        Rotation.NONE, false, false, true, true, 0.1F) == null,
                "两个门口地形都不可用时应放弃候选点，而不是硬放一座院子");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void clusterFacingAvoidsExistingYardsBeforeKeepingPreference(GameTestHelper helper)
    {
        helper.assertTrue(NortheastCourtyardStructure.chooseRotation(
                        Rotation.NONE, true, true, false, true, 0.1F) == Rotation.CLOCKWISE_180,
                "主门向前方会撞已有院子时应优先反向");
        helper.assertTrue(NortheastCourtyardStructure.chooseRotation(
                        Rotation.CLOCKWISE_180, true, true, true, false, 0.1F) == Rotation.NONE,
                "北向门前会撞已有院子时应优先改走南门");
        helper.assertTrue(NortheastCourtyardStructure.chooseRotation(
                        Rotation.NONE, true, true, false, false, 0.1F) == null,
                "两个门前都撞已有院子时应放弃候选点");
        helper.assertTrue(NortheastCourtyardStructure.chooseRotation(
                        Rotation.NONE, false, true, true, false, 0.1F) == null,
                "主门向地形不通且反向门前被挡时，不得退回一个不安全朝向");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rotatedTemplateOriginUsesTheSameFootprint(GameTestHelper helper)
    {
        BlockPos footprintMin = new BlockPos(100, 64, 200);
        BlockPos southOrigin = NortheastCourtyardStructure.templateOrigin(
                footprintMin, 34, 20, Rotation.NONE);
        BlockPos northOrigin = NortheastCourtyardStructure.templateOrigin(
                footprintMin, 34, 20, Rotation.CLOCKWISE_180);
        helper.assertTrue(southOrigin.equals(footprintMin),
                "未旋转院子的模板原点就是包围盒最小角");
        helper.assertTrue(northOrigin.getX() == 133 && northOrigin.getZ() == 219,
                "180 度院子的模板原点应移到包围盒对角");
        helper.assertTrue(NortheastCourtyardStructure.footprintMinX(
                        northOrigin.getX(), 34, Rotation.CLOCKWISE_180) == footprintMin.getX()
                        && NortheastCourtyardStructure.footprintMinZ(
                        northOrigin.getZ(), 20, Rotation.CLOCKWISE_180) == footprintMin.getZ(),
                "模板原点与包围盒最小角必须能无损互转，避免放置时二次平移");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void gateFacingUsesSharedFootprintEdges(GameTestHelper helper)
    {
        BlockPos footprintMin = new BlockPos(100, 64, 200);
        helper.assertTrue(NortheastCourtyardStructure.gateCenterX(footprintMin, 34) == 117,
                "两个朝向共用同一条门中心线");
        helper.assertTrue(NortheastCourtyardStructure.gateEdgeZ(
                        footprintMin, 20, Rotation.NONE) == 219,
                "南向院门应位于包围盒南边");
        helper.assertTrue(NortheastCourtyardStructure.gateEdgeZ(
                        footprintMin, 20, Rotation.CLOCKWISE_180) == 200,
                "北向院门应位于包围盒北边");
        helper.assertTrue(NortheastCourtyardStructure.gateStep(Rotation.NONE) == 1
                        && NortheastCourtyardStructure.gateStep(Rotation.CLOCKWISE_180) == -1,
                "门前检查方向必须与院门所在边一致");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void gradientStartsAtThirdYard(GameTestHelper helper)
    {
        assertChance(helper, 2, 0.65F, "第二座完成后，第三座开始按 65% 梯度");
        assertChance(helper, 3, 0.50F, "第四座应降到 50%");
        assertChance(helper, 4, 0.35F, "第五座应降到 35%");
        assertChance(helper, 5, 0.20F, "第六座应降到 20% 下限");
        assertChance(helper, 8, 0.20F, "超过下限后应保持 20%");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyCourtyardTemplateHasAVisitCriterion(GameTestHelper helper)
    {
        JsonObject structure = resource(
                "northeast_china_delight/worldgen/structure/northeast_yard.json");
        JsonArray configured = structure.getAsJsonArray("templates");
        List<String> templates = configured.asList().stream().map(JsonElement::getAsString).toList();
        helper.assertTrue(templates.equals(NortheastCourtyardStructure.DEFAULT_TEMPLATES),
                "小院结构 JSON 与 Java 默认模板池必须一致");
        helper.assertTrue(templates.contains("northeast_china_delight:northeast_yard_korean"),
                "韩式小院必须进入世界生成模板池");
        for (String template : templates) {
            helper.assertTrue(helper.getLevel().getStructureManager().get(DdIds.parse(template)).isPresent(),
                    "小院模板 NBT 必须能从资源包加载：" + template);
        }
        StructureTemplate korean = helper.getLevel().getStructureManager()
                .get(DdIds.parse("northeast_china_delight:northeast_yard_korean")).orElseThrow();
        List<StructureTemplate.StructureBlockInfo> chests = korean.filterBlocks(
                BlockPos.ZERO, new StructurePlaceSettings(), Blocks.CHEST);
        Map<BlockPos, String> lootTables = new HashMap<>();
        for (StructureTemplate.StructureBlockInfo chest : chests) {
            lootTables.put(chest.pos(), chest.nbt() == null ? "" : chest.nbt().getString("LootTable"));
        }
        Map<BlockPos, String> expectedLoot = Map.of(
                new BlockPos(11, 4, 6), "northeast_china_delight:chests/courtyard_kitchen",
                new BlockPos(21, 4, 18), "northeast_china_delight:chests/courtyard_granary",
                new BlockPos(26, 6, 7), "northeast_china_delight:chests/courtyard_disc");
        helper.assertTrue(lootTables.equals(expectedLoot),
                "韩式小院的 3 个箱子必须全部挂上预期战利品表：" + lootTables);

        JsonObject advancement = resource(
                "northeast_china_delight/advancement/visit_all_yards.json");
        Set<String> criteria = advancement.getAsJsonObject("criteria").keySet();
        Set<String> required = new HashSet<>();
        for (JsonElement group : advancement.getAsJsonArray("requirements")) {
            for (JsonElement name : group.getAsJsonArray()) {
                required.add(name.getAsString());
            }
        }

        Set<String> expected = new HashSet<>();
        for (String template : templates) {
            expected.add(ModAdvancements.yardCriterion(template));
        }
        helper.assertTrue(criteria.equals(expected),
                "每种小院都必须有且只有一条对应的「挨户串门」判据：" + expected);
        helper.assertTrue(required.equals(expected),
                "每条「挨户串门」判据都必须进入 requirements：" + expected);
        helper.succeed();
    }

    private static void assertChance(GameTestHelper helper, int placedCount, float expected, String message)
    {
        float actual = NortheastCourtyardStructure.walkChanceFor(placedCount, 0.80F);
        helper.assertTrue(Math.abs(actual - expected) < 0.0001F, message + "，实际 " + actual);
    }

    private static long cellKey(int x, int z)
    {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static BlockState log(Direction.Axis axis)
    {
        return Blocks.OAK_LOG.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, axis);
    }

    private static NoiseColumn solidColumn(int minY, int maxY, int groundY, BlockState ground)
    {
        BlockState[] states = new BlockState[maxY - minY + 1];
        Arrays.fill(states, Blocks.AIR.defaultBlockState());
        for (int y = minY; y < groundY; y++)
        {
            states[y - minY] = Blocks.STONE.defaultBlockState();
        }
        states[groundY - minY] = ground;
        return new NoiseColumn(minY, states);
    }

    private static JsonObject resource(String path)
    {
        try (InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(
                NortheastCourtyardStructureGameTests.class.getResourceAsStream("/data/" + path)),
                StandardCharsets.UTF_8))
        {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("无法读取测试资源：" + path, exception);
        }
    }
}
