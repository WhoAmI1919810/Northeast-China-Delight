package com.gunmu.northeast_china_delight.worldgen;

import com.gunmu.northeast_china_delight.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** 榛蘑撒点用的公用工具：找树、算树种概率、在地表撒蘑菇。 */
final class HazelMushroomScatter
{
    /** 树干扫描时往上找多少格 */
    private static final int TREE_SCAN_UP = 14;
    /** 树干扫描时往下找多少格 */
    private static final int TREE_SCAN_DOWN = 3;

    private HazelMushroomScatter()
    {
    }

    /** 这一格「树干」对应哪种树的概率；不是那三种树就返回 0 */
    static float speciesChance(BlockState state)
    {
        if (state.is(Blocks.DARK_OAK_LOG) || state.is(Blocks.DARK_OAK_WOOD))
        {
            return HazelnutBushFeature.DARK_OAK_CHANCE;
        }
        if (state.is(Blocks.OAK_LOG) || state.is(Blocks.OAK_WOOD))
        {
            return HazelnutBushFeature.OAK_CHANCE;
        }
        if (state.is(Blocks.BIRCH_LOG) || state.is(Blocks.BIRCH_WOOD))
        {
            return HazelnutBushFeature.BIRCH_CHANCE;
        }
        return 0.0F;
    }

    /** 在中心周围 radius 格里找一圈树干，取「最显眼」的那种树的概率加成（深色橡木 > 橡木 > 白桦） */
    static float treeBonus(WorldGenLevel level, BlockPos center, int radius)
    {
        float best = 0.0F;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dz = -radius; dz <= radius; dz++)
            {
                for (int dy = -TREE_SCAN_DOWN; dy <= TREE_SCAN_UP; dy++)
                {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (!state.is(BlockTags.LOGS))
                    {
                        continue;
                    }
                    float chance = speciesChance(state);
                    if (chance > best)
                    {
                        best = chance;
                        if (best >= HazelnutBushFeature.DARK_OAK_CHANCE)
                        {
                            return best;
                        }
                    }
                }
            }
        }
        return best;
    }

    /** 找中心附近第一棵树的树干（返回树干方块坐标），找不到返回 null */
    static BlockPos findNearbyLog(WorldGenLevel level, BlockPos center, int radius)
    {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dz = -radius; dz <= radius; dz++)
            {
                for (int dy = -TREE_SCAN_DOWN; dy <= TREE_SCAN_UP; dy++)
                {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(BlockTags.LOGS) && speciesChance(state) > 0.0F)
                    {
                        return cursor.immutable();
                    }
                }
            }
        }
        return null;
    }

    /**
     * 以 center 为中心、半径 radius 的方形范围里，每一格「草方块／泥土」上按 chance 独立判定，
     * 长出单株榛蘑。
     */
    static int scatter(WorldGenLevel level, BlockPos center, int radius, float chance, RandomSource random)
    {
        if (chance <= 0.0F)
        {
            return 0;
        }
        BlockState mushroom = ModBlocks.HAZEL_MUSHROOM.get().defaultBlockState();
        int placed = 0;
        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dz = -radius; dz <= radius; dz++)
            {
                if (random.nextFloat() >= chance)
                {
                    continue;
                }
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
                BlockPos plantPos = new BlockPos(x, y, z);
                BlockState ground = level.getBlockState(plantPos.below());
                if (!ground.is(BlockTags.DIRT))
                {
                    continue;
                }
                if (!level.getBlockState(plantPos).canBeReplaced())
                {
                    continue;
                }
                if (!mushroom.canSurvive(level, plantPos))
                {
                    continue;
                }
                level.setBlock(plantPos, mushroom, 2);
                placed++;
            }
        }
        return placed;
    }
}
