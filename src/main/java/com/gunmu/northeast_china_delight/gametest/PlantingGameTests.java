package com.gunmu.northeast_china_delight.gametest;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.block.HazelnutBushBlock;
import com.gunmu.northeast_china_delight.block.GinsengCropBlock;
import com.gunmu.northeast_china_delight.block.ModBlockTags;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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

/**
 * 雪地种植的回归测试。
 *
 * <p>要的行为是「榛子丛**长在雪里**」：底下那格仍然是草方块，榛子丛和薄雪片**占同一格**
 * （把雪片顶掉但自带雪面），所以放下之后应当看到：</p>
 * <ul>
 *   <li>雪片那一格变成榛子丛（雪片本体没了，由模型里的雪面代替）；</li>
 *   <li>榛子丛的 {@code snowy=true}；</li>
 *   <li>它下面那格还是草方块。</li>
 * </ul>
 */
@GameTestHolder(NortheastChinaDelight.MODID)
@PrefixGameTestTemplate(false)
public final class PlantingGameTests
{
    private PlantingGameTests()
    {
    }

    private static Player mockPlayer(GameTestHelper helper, GameType gameType)
    {
        //? if <1.20.5 {
        /*return gameType == GameType.CREATIVE ? helper.makeMockPlayer() : helper.makeMockSurvivalPlayer();
        *///?} else {
        return helper.makeMockPlayer(gameType);
        //?}
    }

    @GameTest(template = "empty")
    public static void hazelnutGrowsInsideTheSnowLayer(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos ground = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos snowPos = ground.above();

        level.setBlockAndUpdate(ground, Blocks.GRASS_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(snowPos, Blocks.SNOW.defaultBlockState());

        Player player = mockPlayer(helper, GameType.SURVIVAL);
        placeOn(level, player, ModItems.HAZELNUT.get(), snowPos);

        helper.assertTrue(level.getBlockState(ground).is(Blocks.GRASS_BLOCK),
                "榛子丛下面那格应该还是草方块");
        helper.assertTrue(level.getBlockState(snowPos).is(ModBlocks.HAZELNUT_BUSH.get()),
                "榛子丛应该和雪片占同一格");
        helper.assertTrue(level.getBlockState(snowPos).getValue(HazelnutBushBlock.SNOWY),
                "这样种出来的榛子丛应该是覆雪形态（自带雪面）");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hazelnutOnBareSoilIsNotSnowy(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos ground = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos plant = ground.above();
        level.setBlockAndUpdate(ground, Blocks.GRASS_BLOCK.defaultBlockState());
        Player player = mockPlayer(helper, GameType.SURVIVAL);
        placeOn(level, player, ModItems.HAZELNUT.get(), plant);
        helper.assertTrue(level.getBlockState(plant).is(ModBlocks.HAZELNUT_BUSH.get()),
                "榛子丛应该能种在裸土上");
        helper.assertFalse(level.getBlockState(plant).getValue(HazelnutBushBlock.SNOWY),
                "裸土上的榛子丛不应该覆雪");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ginsengOnBareSoilIsNotSnowy(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        setBiome(helper, "minecraft:snowy_taiga");
        BlockPos ground = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos plant = ground.above();
        level.setBlockAndUpdate(ground, Blocks.GRASS_BLOCK.defaultBlockState());
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.abs(dx) > 1 || Math.abs(dz) > 1) {
                    level.setBlockAndUpdate(ground.offset(dx, 0, dz), Blocks.SNOW.defaultBlockState());
                }
            }
        }
        for (int y = 2; y <= 6; y++) {
            level.setBlockAndUpdate(plant.above(y), Blocks.OAK_LEAVES.defaultBlockState());
        }
        level.setBlockAndUpdate(ground.offset(4, 0, 0), Blocks.SNOW.defaultBlockState());
        Player player = mockPlayer(helper, GameType.SURVIVAL);
        placeOn(level, player, ModItems.GINSENG_SEEDS.get(), plant);
        helper.assertTrue(level.getBlockState(plant).is(ModBlocks.GINSENG_CROP.get()),
                "人参放下的瞬间就没有留下，当前=" + level.getBlockState(plant)
                        + "，下方=" + level.getBlockState(plant.below())
                        + "，下方在可种植标签=" + level.getBlockState(plant.below()).is(ModBlockTags.GINSENG_PLANTABLE_ON)
                        + "，群系温度=" + level.getBiome(plant).value().getBaseTemperature()
                        + "，能否存活=" + ModBlocks.GINSENG_CROP.get().defaultBlockState()
                        .canSurvive(level, plant));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(level.getBlockState(plant).is(ModBlocks.GINSENG_CROP.get()),
                    "人参应该能种下并在方块更新后继续存活");
            helper.assertFalse(level.getBlockState(plant).getValue(GinsengCropBlock.SNOWY),
                    "裸土上的人参不应该覆雪");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ginsengInSnowLayerSurvives(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        setBiome(helper, "minecraft:snowy_taiga");
        BlockPos ground = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos plant = ground.above();
        level.setBlockAndUpdate(ground, Blocks.GRASS_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(plant, Blocks.SNOW.defaultBlockState());
        addGinsengShade(level, plant);
        Player player = mockPlayer(helper, GameType.SURVIVAL);
        placeOn(level, player, ModItems.GINSENG_SEEDS.get(), plant);
        helper.assertTrue(level.getBlockState(plant).is(ModBlocks.GINSENG_CROP.get())
                        && level.getBlockState(plant).getValue(GinsengCropBlock.SNOWY),
                "参籽应该能把一层雪替换成覆雪人参");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(level.getBlockState(plant).is(ModBlocks.GINSENG_CROP.get()),
                    "雪层上的人参在方块更新后应该继续存活");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ginsengOnSnowBlockSurvives(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        setBiome(helper, "minecraft:snowy_taiga");
        BlockPos ground = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos snow = ground.above();
        BlockPos plant = snow.above();
        level.setBlockAndUpdate(ground, Blocks.GRASS_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(snow, Blocks.SNOW_BLOCK.defaultBlockState());
        addGinsengShade(level, plant);
        Player player = mockPlayer(helper, GameType.SURVIVAL);
        placeOn(level, player, ModItems.GINSENG_SEEDS.get(), plant);
        helper.assertTrue(level.getBlockState(plant).is(ModBlocks.GINSENG_CROP.get())
                        && level.getBlockState(plant).getValue(GinsengCropBlock.SNOWY),
                "参籽应该能种在雪块上并显示覆雪形态");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(level.getBlockState(plant).is(ModBlocks.GINSENG_CROP.get()),
                    "雪块上的人参在方块更新后应该继续存活");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void ginsengWithoutSnowDoesNotPlant(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        setBiome(helper, "minecraft:snowy_taiga");
        BlockPos ground = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos plant = ground.above();
        level.setBlockAndUpdate(ground, Blocks.GRASS_BLOCK.defaultBlockState());
        addGinsengShade(level, plant);
        Player player = mockPlayer(helper, GameType.SURVIVAL);
        placeOn(level, player, ModItems.GINSENG_SEEDS.get(), plant);
        helper.assertFalse(level.getBlockState(plant).is(ModBlocks.GINSENG_CROP.get()),
                "附近没有雪时不应种下参籽");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bellflowerSeedsPlantOnFarmland(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos ground = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos plant = ground.above();
        level.setBlockAndUpdate(ground, Blocks.FARMLAND.defaultBlockState());
        Player player = mockPlayer(helper, GameType.SURVIVAL);
        placeOn(level, player, ModItems.BELLFLOWER_SEEDS.get(), ground);
        helper.assertTrue(level.getBlockState(plant).is(ModBlocks.BELLFLOWER_CROP.get()),
                "桔梗种子应该能种在耕地上");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bellflowerSeedsRejectPlainDirt(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos ground = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos plant = ground.above();
        level.setBlockAndUpdate(ground, Blocks.DIRT.defaultBlockState());
        Player player = mockPlayer(helper, GameType.SURVIVAL);
        placeOn(level, player, ModItems.BELLFLOWER_SEEDS.get(), ground);
        helper.assertFalse(level.getBlockState(plant).is(ModBlocks.BELLFLOWER_CROP.get()),
                "桔梗种子不应绕过耕地直接种在泥土上；野生植株由世界生成放置");
        helper.succeed();
    }

    private static void placeOn(ServerLevel level, Player player, Item item, BlockPos clickedPos)
    {
        ItemStack stack = new ItemStack(item);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(clickedPos).add(0.0, 0.5, 0.0), Direction.UP, clickedPos, false);
        BlockPlaceContext context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack, hit);
        if (item instanceof BlockItem blockItem)
        {
            blockItem.place(context);
        }
    }

    private static void addGinsengShade(ServerLevel level, BlockPos plant)
    {
        for (int y = 2; y <= 6; y++) {
            level.setBlockAndUpdate(plant.above(y), Blocks.OAK_LEAVES.defaultBlockState());
        }
    }

    private static void setBiome(GameTestHelper helper, String biome)
    {
        BlockPos center = helper.absolutePos(new BlockPos(1, 1, 1));
        // fillbiome 会把坐标量化到 4 格；边界必须跨过至少一个 quart，才能覆盖中心。
        BlockPos min = center.offset(-4, -4, -4);
        BlockPos max = center.offset(4, 12, 4);
        String command = "fillbiome %d %d %d %d %d %d %s".formatted(
                min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ(), biome);
        helper.getLevel().getServer().getCommands().performPrefixedCommand(
                helper.getLevel().getServer().createCommandSourceStack().withLevel(helper.getLevel()), command);
        helper.assertTrue(helper.getLevel().getBiome(center)
                        .value().getBaseTemperature() < 0.15F,
                "测试群系应该已经切成寒冷群系");
    }
}
