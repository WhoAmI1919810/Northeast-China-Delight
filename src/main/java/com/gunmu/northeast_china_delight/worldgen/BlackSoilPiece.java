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

public class BlackSoilPiece extends StructurePiece
{
    public static final ResourceLocation RICH_SOIL =
            DdIds.of("farmersdelight", "rich_soil");

    private final int radius;
    private final float density;
    /** 需要避开的院落水平包围盒，按 [minX, minZ, maxX, maxZ] 四元组平铺存储 */
    private final int[] excludedBoxes;

    public BlackSoilPiece(BlockPos center, int radius, float density)
    {
        this(center, radius, density, new int[0]);
    }

    public BlackSoilPiece(BlockPos center, int radius, float density, int[] excludedBoxes)
    {
        super(ModWorldGen.BLACK_SOIL_PIECE.get(), 0,
                new BoundingBox(center.getX() - radius, center.getY() - 96, center.getZ() - radius,
                        center.getX() + radius, center.getY() + 96, center.getZ() + radius));
        this.boundingBox = new YardClearingPiece.DetectionFreeBoundingBox(this.boundingBox);
        this.radius = radius;
        this.density = density;
        this.excludedBoxes = excludedBoxes == null ? new int[0] : excludedBoxes.clone();
    }

    public BlackSoilPiece(CompoundTag tag)
    {
        super(ModWorldGen.BLACK_SOIL_PIECE.get(), tag);
        this.boundingBox = new YardClearingPiece.DetectionFreeBoundingBox(this.boundingBox);
        this.radius = tag.getInt("Radius");
        this.density = tag.getFloat("Density");
        this.excludedBoxes = tag.getIntArray("ExcludedBoxes");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag)
    {
        tag.putInt("Radius", this.radius);
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
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++)
        {
            for (int z = minZ; z <= maxZ; z++)
            {
                if (!chunkContains(chunkPos, x, z) || isExcluded(this.excludedBoxes, x, z))
                {
                    continue;
                }
                if (random.nextFloat() > this.density)
                {
                    continue;
                }
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (y <= level.getMinBuildHeight())
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

    static boolean isExcluded(int[] excludedBoxes, int x, int z)
    {
        for (int i = 0; i + 3 < excludedBoxes.length; i += 4)
        {
            if (x >= excludedBoxes[i] && x <= excludedBoxes[i + 2]
                    && z >= excludedBoxes[i + 1] && z <= excludedBoxes[i + 3])
            {
                return true;
            }
        }
        return false;
    }

    private static boolean chunkContains(ChunkPos chunk, int x, int z)
    {
        return chunk.getMinBlockX() <= x && x <= chunk.getMaxBlockX()
                && chunk.getMinBlockZ() <= z && z <= chunk.getMaxBlockZ();
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
