package com.gunmu.dongbei_delight.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
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
 * 小院周边清场：把树、灌木、花草这类「地物」从院子和它周围一圈清掉。
 *
 * <p>为什么需要它：结构放在 {@code top_layer_modification} 阶段（比种树晚），
 * 模板里的空气格不会覆盖世界（结构模板只写非空气方块），所以不主动清的话，
 * 树会长在院子里、挡住屋顶。清场分两块：
 * <ul>
 *   <li>院子正上方 30 格：保证是空气；</li>
 *   <li>院子外扩 radius 格（默认 20）：清掉树、灌木、花草地物。</li>
 * </ul>
 *
 * <p>它被排在院子方块之前生成，所以院子里那棵（模板自带的）大树不会被自己清掉。
 */
public class YardClearingPiece extends StructurePiece
{
    private final int groundY;
    private final int radius;
    private final int clearAbove;
    /** 院子本体的水平范围（要在这里把地形垫平，免得院子悬空） */
    private final int yardMinX;
    private final int yardMaxX;
    private final int yardMinZ;
    private final int yardMaxZ;

    public YardClearingPiece(BlockPos yardOrigin, int sizeX, int sizeZ, int groundY,
                             int radius, int clearAbove)
    {
        super(ModWorldGen.CLEARING_PIECE.get(), 0,
                new BoundingBox(yardOrigin.getX() - radius, yardOrigin.getY() - 8, yardOrigin.getZ() - radius,
                        yardOrigin.getX() + sizeX - 1 + radius, yardOrigin.getY() + clearAbove + 8,
                        yardOrigin.getZ() + sizeZ - 1 + radius));
        this.groundY = groundY;
        this.radius = radius;
        this.clearAbove = clearAbove;
        this.yardMinX = yardOrigin.getX();
        this.yardMaxX = yardOrigin.getX() + sizeX - 1;
        this.yardMinZ = yardOrigin.getZ();
        this.yardMaxZ = yardOrigin.getZ() + sizeZ - 1;
    }

    public YardClearingPiece(CompoundTag tag)
    {
        super(ModWorldGen.CLEARING_PIECE.get(), tag);
        this.groundY = tag.getInt("GroundY");
        this.radius = tag.getInt("Radius");
        this.clearAbove = tag.getInt("ClearAbove");
        this.yardMinX = tag.getInt("YardMinX");
        this.yardMaxX = tag.getInt("YardMaxX");
        this.yardMinZ = tag.getInt("YardMinZ");
        this.yardMaxZ = tag.getInt("YardMaxZ");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag)
    {
        tag.putInt("GroundY", this.groundY);
        tag.putInt("Radius", this.radius);
        tag.putInt("ClearAbove", this.clearAbove);
        tag.putInt("YardMinX", this.yardMinX);
        tag.putInt("YardMaxX", this.yardMaxX);
        tag.putInt("YardMinZ", this.yardMinZ);
        tag.putInt("YardMaxZ", this.yardMaxZ);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos)
    {
        int minX = Math.max(box.minX(), this.boundingBox.minX());
        int maxX = Math.min(box.maxX(), this.boundingBox.maxX());
        int minZ = Math.max(box.minZ(), this.boundingBox.minZ());
        int maxZ = Math.min(box.maxZ(), this.boundingBox.maxZ());
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
        int yardTop = Math.min(maxY, this.groundY + this.clearAbove);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++)
        {
            for (int z = minZ; z <= maxZ; z++)
            {
                // 逐柱清理：下限按**这一柱自己的地表**算，不然长在低处的树
                // （地表比院子地面低）下半截会留在原地，看起来像一排树桩。
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                int yFrom = Math.max(minY, Math.min(surface, this.groundY) - 1);
                for (int y = yFrom; y <= yardTop; y++)
                {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir())
                    {
                        continue;
                    }
                    if (isVegetation(state))
                    {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                // 院子范围里：把地形表面到院子地面之间填实，别让院子悬空
                if (x >= this.yardMinX && x <= this.yardMaxX && z >= this.yardMinZ && z <= this.yardMaxZ)
                {
                    int from = Math.max(level.getMinBuildHeight(), Math.min(surface, this.groundY));
                    for (int y = from; y < this.groundY; y++)
                    {
                        cursor.set(x, y, z);
                        BlockState here = level.getBlockState(cursor);
                        if (here.isAir() || isVegetation(here))
                        {
                            level.setBlock(cursor, Blocks.DIRT.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }
    }

    /** 树、灌木、花草、作物这类「地物」，不是地形本身 */
    public static boolean isVegetation(BlockState state)
    {
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.FLOWERS) || state.is(BlockTags.SMALL_FLOWERS)
                || state.is(BlockTags.TALL_FLOWERS) || state.is(BlockTags.CROPS))
        {
            return true;
        }
        Block block = state.getBlock();
        return block == Blocks.SHORT_GRASS
                || block == Blocks.TALL_GRASS
                || block == Blocks.FERN
                || block == Blocks.LARGE_FERN
                || block == Blocks.DEAD_BUSH
                || block == Blocks.CACTUS
                || block == Blocks.BAMBOO
                || block == Blocks.VINE
                || block == Blocks.BROWN_MUSHROOM
                || block == Blocks.RED_MUSHROOM
                || block == Blocks.SWEET_BERRY_BUSH
                || block == Blocks.AZALEA
                || block == Blocks.FLOWERING_AZALEA
                || block == Blocks.LILY_PAD
                || block == Blocks.SUGAR_CANE
                || block == Blocks.PUMPKIN
                || block == Blocks.MELON
                || block == Blocks.CHORUS_FLOWER;
    }
}
