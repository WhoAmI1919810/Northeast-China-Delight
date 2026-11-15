package com.gunmu.northeast_china_delight.worldgen;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Rotation;
//? if <1.20.2 {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} else {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

import java.util.List;

@GameTestHolder(NortheastChinaDelight.MODID)
@PrefixGameTestTemplate(false)
public final class NortheastCourtyardStructureGameTests
{
    private NortheastCourtyardStructureGameTests()
    {
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
    public static void secondYardAcceptsDryLandButRejectsWaterAndWorldEdges(GameTestHelper helper)
    {
        int minY = -64;
        int maxY = 320;
        helper.assertTrue(NortheastCourtyardStructure.isSecondYardTerrainSuitable(64, 64, minY, maxY),
                "地表与海床高度一致时应视为干燥陆地");
        helper.assertTrue(!NortheastCourtyardStructure.isSecondYardTerrainSuitable(63, 64, minY, maxY),
                "地表高于海床说明水面上方，第二座不得落在水里");
        helper.assertTrue(NortheastCourtyardStructure.isSecondYardTerrainSuitable(minY + 12, minY + 12, minY, maxY),
                "刚好高于世界底部安全线的位置应允许生成");
        helper.assertTrue(!NortheastCourtyardStructure.isSecondYardTerrainSuitable(minY + 11, minY + 11, minY, maxY),
                "过低的位置应视为浮空或世界边缘并拒绝");
        helper.assertTrue(NortheastCourtyardStructure.isSecondYardTerrainSuitable(maxY - 32, maxY - 32, minY, maxY),
                "刚好低于世界顶部安全线的位置应允许生成");
        helper.assertTrue(!NortheastCourtyardStructure.isSecondYardTerrainSuitable(maxY - 31, maxY - 31, minY, maxY),
                "过高的位置应视为浮空或世界边缘并拒绝");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void secondYardFootprintRejectsBuriedAndFloatingSamples(GameTestHelper helper)
    {
        int anchor = 64;
        int maxSlope = 6;
        helper.assertTrue(NortheastCourtyardStructure.isSecondYardFootprintHeightSuitable(
                anchor, anchor - 6, maxSlope), "低于锚点 6 格以内应仍视为有地形支撑");
        helper.assertTrue(!NortheastCourtyardStructure.isSecondYardFootprintHeightSuitable(
                anchor, anchor - 7, maxSlope), "低于锚点过多应视为悬空");
        helper.assertTrue(NortheastCourtyardStructure.isSecondYardFootprintHeightSuitable(
                anchor, anchor + 3, maxSlope), "高于锚点 3 格以内应允许");
        helper.assertTrue(!NortheastCourtyardStructure.isSecondYardFootprintHeightSuitable(
                anchor, anchor + 4, maxSlope), "高于锚点过多应视为被地形埋住");
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
    public static void gradientStartsAtThirdYard(GameTestHelper helper)
    {
        assertChance(helper, 2, 0.65F, "第二座完成后，第三座开始按 65% 梯度");
        assertChance(helper, 3, 0.50F, "第四座应降到 50%");
        assertChance(helper, 4, 0.35F, "第五座应降到 35%");
        assertChance(helper, 5, 0.20F, "第六座应降到 20% 下限");
        assertChance(helper, 8, 0.20F, "超过下限后应保持 20%");
        helper.succeed();
    }

    private static void assertChance(GameTestHelper helper, int placedCount, float expected, String message)
    {
        float actual = NortheastCourtyardStructure.walkChanceFor(placedCount, 0.80F);
        helper.assertTrue(Math.abs(actual - expected) < 0.0001F, message + "，实际 " + actual);
    }
}
