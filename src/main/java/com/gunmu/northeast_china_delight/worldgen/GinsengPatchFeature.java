package com.gunmu.northeast_china_delight.worldgen;

import com.gunmu.northeast_china_delight.block.GinsengCropBlock;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class GinsengPatchFeature extends Feature<NoneFeatureConfiguration>
{
    public static final float REGION_CHANCE = 0.30F;
    public static final int MAX_PER_REGION = 3;
    private static final int ATTEMPTS = 24;

    public GinsengPatchFeature(Codec<NoneFeatureConfiguration> codec)
    {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context)
    {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int chunkX = SectionPos.blockToSectionCoord(origin.getX());
        int chunkZ = SectionPos.blockToSectionCoord(origin.getZ());

        if ((chunkX & 1) != 0 || (chunkZ & 1) != 0)
        {
            return false;
        }

        RandomSource random = RandomSource.create(regionSeed(level.getSeed(), chunkX, chunkZ));
        if (random.nextFloat() >= REGION_CHANCE)
        {
            return false;
        }

        int wanted = 1 + random.nextInt(MAX_PER_REGION);
        int placed = 0;
        int baseX = SectionPos.sectionToBlockCoord(chunkX);
        int baseZ = SectionPos.sectionToBlockCoord(chunkZ);
        for (int attempt = 0; attempt < ATTEMPTS && placed < wanted; attempt++)
        {
            int x = baseX + random.nextInt(32);
            int z = baseZ + random.nextInt(32);
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos plantPos = new BlockPos(x, surfaceY, z);
            BlockState here = level.getBlockState(plantPos);
            if (!here.canBeReplaced() && !here.is(Blocks.SNOW))
            {
                continue;
            }
            BlockState ground = level.getBlockState(plantPos.below());
            BlockState soil = ground.is(Blocks.SNOW_BLOCK) ? level.getBlockState(plantPos.below(2)) : ground;
            if (!(soil.is(Blocks.GRASS_BLOCK) || soil.is(Blocks.DIRT) || soil.is(Blocks.PODZOL)))
            {
                continue;
            }
            BlockState ginseng = ModBlocks.GINSENG_CROP.get().defaultBlockState()
                    .setValue(GinsengCropBlock.AGE, GinsengCropBlock.MAX_AGE)
                    .setValue(GinsengCropBlock.SNOWY, GinsengCropBlock.isSnowyAt(level, plantPos));
            if (!ginseng.canSurvive(level, plantPos))
            {
                continue;
            }
            level.setBlock(plantPos, ginseng, 2);
            placed++;
        }
        return placed > 0;
    }

    private static long regionSeed(long worldSeed, int regionChunkX, int regionChunkZ)
    {
        return worldSeed ^ ((long) regionChunkX * 341873128712L) ^ ((long) regionChunkZ * 132897987541L);
    }
}
