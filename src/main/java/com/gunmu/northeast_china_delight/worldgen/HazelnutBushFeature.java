package com.gunmu.northeast_china_delight.worldgen;

import com.gunmu.northeast_china_delight.block.HazelnutBushBlock;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class HazelnutBushFeature extends Feature<NoneFeatureConfiguration>
{
    public static final float BASE_CHANCE = 0.015F;
    public static final float DARK_OAK_CHANCE = 0.0075F;
    public static final float OAK_CHANCE = 0.005F;
    public static final float BIRCH_CHANCE = 0.003F;

    public static final int BUSH_RADIUS = 10;

    public HazelnutBushFeature(Codec<NoneFeatureConfiguration> codec)
    {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context)
    {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        BlockPos target = origin;
        BlockState belowOrigin = level.getBlockState(origin.below());
        if (belowOrigin.is(Blocks.SNOW) && belowOrigin.getValue(SnowLayerBlock.LAYERS) == 1)
        {
            target = origin.below();
        }
        BlockState targetState = level.getBlockState(target);
        boolean thinSnow = targetState.is(Blocks.SNOW) && targetState.getValue(SnowLayerBlock.LAYERS) == 1;
        if (!thinSnow && !targetState.canBeReplaced())
        {
            return false;
        }

        BlockState bush = ModBlocks.HAZELNUT_BUSH.get().defaultBlockState()
                .setValue(HazelnutBushBlock.AGE, HazelnutBushBlock.MAX_AGE)
                .setValue(HazelnutBushBlock.SNOWY, HazelnutBushBlock.isSnowyAt(level, target));
        if (!bush.canSurvive(level, target))
        {
            return false;
        }
        level.setBlock(target, bush, 2);

        float chance = BASE_CHANCE + HazelMushroomScatter.treeBonus(level, origin, BUSH_RADIUS);
        HazelMushroomScatter.scatter(level, origin, BUSH_RADIUS, chance, random);
        return true;
    }
}
