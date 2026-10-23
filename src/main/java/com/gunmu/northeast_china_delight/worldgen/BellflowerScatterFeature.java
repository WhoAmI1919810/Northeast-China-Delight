package com.gunmu.northeast_china_delight.worldgen;

import com.gunmu.northeast_china_delight.block.BellflowerCropBlock;
import com.gunmu.northeast_china_delight.block.ModBlockTags;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Set;

/** 野生桔梗：每次最多种一株，只在指定群系的林缘/交界处零散生成，并要求头顶完全无遮挡。 */
public class BellflowerScatterFeature extends Feature<NoneFeatureConfiguration>
{
    private static final Set<String> EDGE_ONLY_BIOMES = Set.of(
            "forest", "birch_forest", "old_growth_birch_forest");
    private static final Set<String> OPEN_BIOMES = Set.of(
            "flower_forest", "taiga", "old_growth_pine_taiga", "old_growth_spruce_taiga");
    private static final Set<String> BOUNDARY_BIOMES = Set.of("meadow", "plains");

    private static final int[][] EDGE_SAMPLES = {
            {4, 0}, {-4, 0}, {0, 4}, {0, -4},
            {4, 4}, {4, -4}, {-4, 4}, {-4, -4},
            {7, 0}, {-7, 0}, {0, 7}, {0, -7}
    };

    public static final float EDGE_CHANCE = 0.30F;
    public static final float FLOWER_FOREST_CHANCE = 0.36F;
    public static final float TAIGA_CHANCE = 0.32F;
    public static final float BOUNDARY_CHANCE = 0.36F;
    public static final float HAZEL_BONUS = 0.20F;
    public static final float FAVORABLE_BONUS = 0.035F;
    public static final float MAX_CHANCE = 0.90F;

    public BellflowerScatterFeature(Codec<NoneFeatureConfiguration> codec)
    {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context)
    {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        BlockPos ground = findGround(level, origin);
        if (ground == null)
        {
            return false;
        }
        BlockPos plantPos = ground.above();
        BlockState plantHere = level.getBlockState(plantPos);
        if (!plantHere.canBeReplaced() || !plantHere.getFluidState().isEmpty())
        {
            return false;
        }
        if (hasSnowNearby(level, plantPos) || !hasClearOverhead(level, plantPos))
        {
            return false;
        }

        String biomePath = biomePath(level, plantPos);
        float chance = baseChance(level, plantPos, biomePath);
        if (chance < 0.0F)
        {
            return false;
        }

        if (hasBellflowerNearby(level, plantPos) && random.nextFloat() < 0.92F)
        {
            return false;
        }

        if (hasHazelnutNearby(level, plantPos))
        {
            chance += HAZEL_BONUS;
        }
        chance += favorableTypesNearby(level, plantPos) * FAVORABLE_BONUS;
        chance = Math.min(MAX_CHANCE, chance);
        if (random.nextFloat() >= chance)
        {
            return false;
        }

        BlockState bellflower = ModBlocks.BELLFLOWER_CROP.get().defaultBlockState()
                .setValue(BellflowerCropBlock.AGE, BellflowerCropBlock.MAX_AGE);
        if (!BellflowerCropBlock.canWildSurvive(level, plantPos))
        {
            return false;
        }
        level.setBlock(plantPos, bellflower, 2);
        return true;
    }

    private static BlockPos findGround(WorldGenLevel level, BlockPos origin)
    {
        BlockPos.MutableBlockPos cursor = origin.mutable();
        for (int step = 0; step < 64; step++)
        {
            BlockPos pos = cursor.set(origin.getX(), origin.getY() - step, origin.getZ()).immutable();
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlockTags.BELLFLOWER_PLANTABLE_ON))
            {
                return pos;
            }
            if (!state.isAir() && !state.canBeReplaced())
            {
                return null;
            }
        }
        return null;
    }

    private static boolean isRichSoil(BlockState state)
    {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null
                && "farmersdelight".equals(id.getNamespace())
                && ("rich_soil".equals(id.getPath()) || "rich_soil_farmland".equals(id.getPath()));
    }

    private static boolean hasClearOverhead(WorldGenLevel level, BlockPos plantPos)
    {
        BlockPos.MutableBlockPos cursor = plantPos.mutable();
        for (int y = plantPos.getY() + 1; y < level.getMaxBuildHeight(); y++)
        {
            BlockState state = level.getBlockState(cursor.set(plantPos.getX(), y, plantPos.getZ()));
            if (!state.isAir() && !state.is(Blocks.CAVE_AIR) && !state.is(Blocks.VOID_AIR))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean hasSnowNearby(WorldGenLevel level, BlockPos plantPos)
    {
        for (BlockPos pos : BlockPos.betweenClosed(
                plantPos.offset(-2, -1, -2), plantPos.offset(2, 2, 2)))
        {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW))
            {
                return true;
            }
        }
        return false;
    }

    private static String biomePath(WorldGenLevel level, BlockPos pos)
    {
        return level.getBiome(pos).unwrapKey()
                .map(key -> key.location().getPath())
                .orElse("");
    }

    private static float baseChance(WorldGenLevel level, BlockPos pos, String biomePath)
    {
        if (EDGE_ONLY_BIOMES.contains(biomePath))
        {
            return isNearOtherBiome(level, pos) ? EDGE_CHANCE : -1.0F;
        }
        if (OPEN_BIOMES.contains(biomePath))
        {
            return "flower_forest".equals(biomePath) ? FLOWER_FOREST_CHANCE : TAIGA_CHANCE;
        }
        if (BOUNDARY_BIOMES.contains(biomePath))
        {
            return isNearForestBiome(level, pos) ? BOUNDARY_CHANCE : -1.0F;
        }
        return -1.0F;
    }

    private static boolean isNearOtherBiome(WorldGenLevel level, BlockPos pos)
    {
        for (int[] offset : EDGE_SAMPLES)
        {
            String path = biomePath(level, pos.offset(offset[0], 0, offset[1]));
            if (!path.isEmpty() && !EDGE_ONLY_BIOMES.contains(path))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isNearForestBiome(WorldGenLevel level, BlockPos pos)
    {
        for (int[] offset : EDGE_SAMPLES)
        {
            String path = biomePath(level, pos.offset(offset[0], 0, offset[1]));
            if (EDGE_ONLY_BIOMES.contains(path) || OPEN_BIOMES.contains(path))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBellflowerNearby(WorldGenLevel level, BlockPos plantPos)
    {
        for (BlockPos pos : BlockPos.betweenClosed(
                plantPos.offset(-4, -4, -4), plantPos.offset(4, 4, 4)))
        {
            if (level.getBlockState(pos).is(ModBlocks.BELLFLOWER_CROP.get()))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean hasHazelnutNearby(WorldGenLevel level, BlockPos plantPos)
    {
        for (BlockPos pos : BlockPos.betweenClosed(
                plantPos.offset(-8, -3, -8), plantPos.offset(8, 3, 8)))
        {
            if (level.getBlockState(pos).is(ModBlocks.HAZELNUT_BUSH.get()))
            {
                return true;
            }
        }
        return false;
    }

    private static int favorableTypesNearby(WorldGenLevel level, BlockPos plantPos)
    {
        boolean grass = false;
        boolean coarseDirt = false;
        boolean gravel = false;
        boolean water = false;
        boolean richSoil = false;
        for (BlockPos pos : BlockPos.betweenClosed(
                plantPos.offset(-3, -1, -3), plantPos.offset(3, 2, 3)))
        {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.GRASS_BLOCK))
            {
                grass = true;
            }
            else if (state.is(Blocks.COARSE_DIRT))
            {
                coarseDirt = true;
            }
            else if (state.is(Blocks.GRAVEL))
            {
                gravel = true;
            }
            else if (state.getFluidState().is(FluidTags.WATER))
            {
                water = true;
            }
            else if (isRichSoil(state))
            {
                richSoil = true;
            }
        }
        return (grass ? 1 : 0) + (coarseDirt ? 1 : 0) + (gravel ? 1 : 0)
                + (water ? 1 : 0) + (richSoil ? 1 : 0);
    }
}
