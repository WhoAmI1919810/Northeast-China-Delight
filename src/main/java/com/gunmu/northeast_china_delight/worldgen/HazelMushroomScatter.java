package com.gunmu.northeast_china_delight.worldgen;

import com.gunmu.northeast_china_delight.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

final class HazelMushroomScatter
{
    private static final int TREE_SCAN_UP = 14;
    private static final int TREE_SCAN_DOWN = 3;

    private HazelMushroomScatter()
    {
    }

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
