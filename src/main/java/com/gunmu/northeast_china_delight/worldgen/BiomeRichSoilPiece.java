package com.gunmu.northeast_china_delight.worldgen;

import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class BiomeRichSoilPiece extends StructurePiece
{
    public static final ResourceLocation RICH_SOIL =
            DdIds.of("farmersdelight", "rich_soil");

    private final float density;
    private final int[] excludedBoxes;

    public BiomeRichSoilPiece(BlockPos center, int radius, float density)
    {
        this(center, radius, density, new int[0]);
    }

    public BiomeRichSoilPiece(BlockPos center, int radius, float density, int[] excludedBoxes)
    {
        super(ModWorldGen.BIOME_RICH_SOIL_PIECE.get(), 0,
                new BoundingBox(center.getX() - radius, center.getY() - 96, center.getZ() - radius,
                        center.getX() + radius, center.getY() + 96, center.getZ() + radius));
        this.boundingBox = new YardClearingPiece.DetectionFreeBoundingBox(this.boundingBox);
        this.density = density;
        this.excludedBoxes = excludedBoxes == null ? new int[0] : excludedBoxes.clone();
    }

    public BiomeRichSoilPiece(CompoundTag tag)
    {
        super(ModWorldGen.BIOME_RICH_SOIL_PIECE.get(), tag);
        this.boundingBox = new YardClearingPiece.DetectionFreeBoundingBox(this.boundingBox);
        this.density = tag.getFloat("Density");
        this.excludedBoxes = tag.getIntArray("ExcludedBoxes");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag)
    {
        tag.putFloat("Density", this.density);
        if (this.excludedBoxes.length > 0)
        {
            tag.putIntArray("ExcludedBoxes", this.excludedBoxes);
        }
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos)
    {
        Block richSoil = BuiltInRegistries.BLOCK.getOptional(RICH_SOIL).orElse(null);
        if (richSoil == null || this.density <= 0.0F)
        {
            return;
        }
        BlockState soil = richSoil.defaultBlockState();
        int minX = Math.max(box.minX(), this.boundingBox.minX());
        int maxX = Math.min(box.maxX(), this.boundingBox.maxX());
        int minZ = Math.max(box.minZ(), this.boundingBox.minZ());
        int maxZ = Math.min(box.maxZ(), this.boundingBox.maxZ());
        long worldSeed = level.getSeed();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++)
        {
            for (int z = minZ; z <= maxZ; z++)
            {
                if (!chunkContains(chunkPos, x, z) || BlackSoilPiece.isExcluded(this.excludedBoxes, x, z))
                {
                    continue;
                }
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (y <= level.getMinBuildHeight())
                {
                    continue;
                }
                double noise = valueNoise(x, z, worldSeed);
                if (noise < 1.0 - this.density)
                {
                    continue;
                }
                cursor.set(x, y, z);
                BlockState state = level.getBlockState(cursor);
                if (state.is(Blocks.SNOW))
                {
                    BlockPos belowPos = cursor.below();
                    BlockState below = level.getBlockState(belowPos);
                    if (isReplaceable(below))
                    {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                        level.setBlock(belowPos, soil, 2);
                    }
                    continue;
                }
                if (isReplaceable(state))
                {
                    level.setBlock(cursor, soil, 2);
                }
            }
        }
    }

    private static boolean chunkContains(ChunkPos chunk, int x, int z)
    {
        return chunk.getMinBlockX() <= x && x <= chunk.getMaxBlockX()
                && chunk.getMinBlockZ() <= z && z <= chunk.getMaxBlockZ();
    }

    private static final double NOISE_CELL = 12.0;

    private static double valueNoise(int x, int z, long seed)
    {
        double fx = x / NOISE_CELL;
        double fz = z / NOISE_CELL;
        int x0 = (int) Math.floor(fx);
        int z0 = (int) Math.floor(fz);
        double tx = smoothstep(fx - x0);
        double tz = smoothstep(fz - z0);
        double v00 = hash(x0, z0, seed);
        double v10 = hash(x0 + 1, z0, seed);
        double v01 = hash(x0, z0 + 1, seed);
        double v11 = hash(x0 + 1, z0 + 1, seed);
        return lerp(lerp(v00, v10, tx), lerp(v01, v11, tx), tz);
    }

    private static double hash(int x, int z, long seed)
    {
        int seedMix = (int) (seed ^ (seed >>> 32));
        int h = x * 0x27d4eb2d ^ z * 0x165667b1 ^ seedMix * 0x9e3779b9;
        h = (h ^ (h >>> 15)) * 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        h ^= h >>> 16;
        return (h & 0x7FFFFFFF) / (double) 0x7FFFFFFF;
    }

    private static double smoothstep(double t)
    {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double a, double b, double t)
    {
        return a + (b - a) * t;
    }

    private static boolean isReplaceable(BlockState state)
    {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM);
    }
}
