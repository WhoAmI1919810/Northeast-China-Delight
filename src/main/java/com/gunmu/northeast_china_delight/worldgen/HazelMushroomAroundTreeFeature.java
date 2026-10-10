package com.gunmu.northeast_china_delight.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class HazelMushroomAroundTreeFeature extends Feature<NoneFeatureConfiguration>
{
    private static final int TREE_SEARCH_RADIUS = 4;
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
