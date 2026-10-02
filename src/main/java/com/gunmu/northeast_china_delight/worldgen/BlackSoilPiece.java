package com.gunmu.northeast_china_delight.worldgen;

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

/**
 * 黑土地：在小院周围把表层的草方块 / 泥土 / 灰化土 / 积雪，按概率换成
 * 农夫乐事的「沃土」（rich_soil）。
 *
 * <p>概率、半径都在结构 JSON 里（black_soil_chance / black_soil_radius /
 * black_soil_density），改数字就能调；没装农夫乐事时整块直接跳过，不会报错。
 */
public class BlackSoilPiece extends StructurePiece
{
    /** 农夫乐事的沃土（软依赖：按 id 查，查不到就不长） */
    public static final ResourceLocation RICH_SOIL =
            ResourceLocation.fromNamespaceAndPath("farmersdelight", "rich_soil");

    private final int radius;
    private final float density;

    public BlackSoilPiece(BlockPos center, int radius, float density)
    {
        super(ModWorldGen.BLACK_SOIL_PIECE.get(), 0,
                new BoundingBox(center.getX() - radius, center.getY() - 96, center.getZ() - radius,
                        center.getX() + radius, center.getY() + 96, center.getZ() + radius));
        this.radius = radius;
        this.density = density;
    }

    public BlackSoilPiece(CompoundTag tag)
    {
        super(ModWorldGen.BLACK_SOIL_PIECE.get(), tag);
        this.radius = tag.getInt("Radius");
        this.density = tag.getFloat("Density");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag)
    {
        tag.putInt("Radius", this.radius);
        tag.putFloat("Density", this.density);
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
                    // 雪盖着的话先扫掉，不然黑土地看不见
                    level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                    cursor.setY(y - 1);
                    state = level.getBlockState(cursor);
                }
                if (isReplaceable(state))
                {
                    level.setBlock(cursor, soil, 2);
                }
            }
        }
    }

    private static boolean isReplaceable(BlockState state)
    {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.SNOW);
    }
}
