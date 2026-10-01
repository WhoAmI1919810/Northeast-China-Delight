package com.gunmu.dongbei_delight.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 树脚下的榛蘑：在深色橡木、橡木、白桦的树干周围半径 3 格里，
 * 每一格「草方块／泥土」各自按树种的概率长一株榛蘑：
 * 深色橡木 1.5%、橡木 1%、白桦 0.6%，其它树种不长。
 *
 * <p>触发方式是「先在这片地上找一棵树，找到了再围着它撒蘑菇」——
 * 不去动原版的树木生成，也不会平白多长树出来。</p>
 */
public class HazelMushroomAroundTreeFeature extends Feature<NoneFeatureConfiguration>
{
    /** 找树的水平半径 */
    private static final int TREE_SEARCH_RADIUS = 4;
    /** 蘑菇离树干多远 */
    private static final int SCATTER_RADIUS = 3;

    public HazelMushroomAroundTreeFeature(Codec<NoneFeatureConfiguration> codec)
    {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context)
    {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        BlockPos logPos = HazelMushroomScatter.findNearbyLog(level, origin, TREE_SEARCH_RADIUS);
        if (logPos == null)
        {
            return false;
        }
        BlockState logState = level.getBlockState(logPos);
        float chance = HazelMushroomScatter.speciesChance(logState);
        if (chance <= 0.0F)
        {
            return false;
        }
        return HazelMushroomScatter.scatter(level, logPos, SCATTER_RADIUS, chance, context.random()) > 0;
    }
}
