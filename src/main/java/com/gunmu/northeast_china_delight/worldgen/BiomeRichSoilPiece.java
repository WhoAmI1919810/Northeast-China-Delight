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
 * 生物群系沃土：东北小院所在的**整片生物群系**里，土类方块按概率变成农夫乐事的「沃土」。
 *
 * <p>不是撒在院子周围那一小圈，而是**这一户落点的整片生物群系**——
 * 同一 chunk 里 biome 对得上的草方块 / 泥土 / 灰化土 / 积雪，按 {@code density} 换成 rich_soil。
 * 没装农夫乐事时整块直接跳过，不会报错。
 */
public class BiomeRichSoilPiece extends StructurePiece
{
    /** 农夫乐事的沃土（软依赖：按 id 查，查不到就不长） */
    public static final ResourceLocation RICH_SOIL =
            ResourceLocation.fromNamespaceAndPath("farmersdelight", "rich_soil");

    private final float density;

    public BiomeRichSoilPiece(BlockPos center, int radius, float density)
    {
        super(ModWorldGen.BIOME_RICH_SOIL_PIECE.get(), 0,
                new BoundingBox(center.getX() - radius, center.getY() - 96, center.getZ() - radius,
                        center.getX() + radius, center.getY() + 96, center.getZ() + radius));
        this.density = density;
    }

    public BiomeRichSoilPiece(CompoundTag tag)
    {
        super(ModWorldGen.BIOME_RICH_SOIL_PIECE.get(), tag);
        this.density = tag.getFloat("Density");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag)
    {
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
        int maxZ = Math.max(box.maxZ(), this.boundingBox.maxZ());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++)
        {
            for (int z = minZ; z <= maxZ; z++)
            {
                // 只处理正在生成的这个 chunk 里的格子，别伸手到隔壁
                if (!chunkContains(chunkPos, x, z))
                {
                    continue;
                }
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (y <= level.getMinBuildHeight())
                {
                    continue;
                }
                // 值噪声成片：大块大片一起变，不是满天星（密度越高斑块越大）
                double noise = valueNoise(x, z);
                if (noise < 1.0 - this.density)
                {
                    continue;
                }
                cursor.set(x, y, z);
                BlockState state = level.getBlockState(cursor);
                if (state.is(Blocks.SNOW))
                {
                    // 雪盖着的话先扫掉，不然沃土看不见
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

    private static boolean chunkContains(ChunkPos chunk, int x, int z)
    {
        return chunk.getMinBlockX() <= x && x <= chunk.getMaxBlockX()
                && chunk.getMinBlockZ() <= z && z <= chunk.getMaxBlockZ();
    }

    /** 值噪声的格子尺寸：12 格一格，沃土斑块大概是 5~8 格一团 */
    private static final double NOISE_CELL = 12.0;

    /**
     * 2D 值噪声：把 x/z 落在的格子四角哈希成 0~1 的值，再双线性插值平滑。
     * 纯按坐标算，不依赖 chunk 随机数 —— 跨 chunk 边界也能接上。
     */
    private static double valueNoise(int x, int z)
    {
        double fx = x / NOISE_CELL;
        double fz = z / NOISE_CELL;
        int x0 = (int) Math.floor(fx);
        int z0 = (int) Math.floor(fz);
        double tx = smoothstep(fx - x0);
        double tz = smoothstep(fz - z0);
        double v00 = hash(x0, z0);
        double v10 = hash(x0 + 1, z0);
        double v01 = hash(x0, z0 + 1);
        double v11 = hash(x0 + 1, z0 + 1);
        return lerp(lerp(v00, v10, tx), lerp(v01, v11, tx), tz);
    }

    private static double hash(int x, int z)
    {
        // 经典整数哈希：坐标混成分散到 0~1
        int h = x * 0x27d4eb2d ^ z * 0x165667b1;
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
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.SNOW);
    }
}
