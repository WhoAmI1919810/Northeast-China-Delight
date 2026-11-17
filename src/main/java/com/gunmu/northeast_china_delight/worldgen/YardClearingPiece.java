package com.gunmu.northeast_china_delight.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * 小院周边清场：把树、灌木、花草这类「地物」从院子和它周围一圈清掉。
 *
 * <p>为什么需要它：结构放在 {@code top_layer_modification} 阶段（比种树晚），
 * 模板里的空气格不会覆盖世界（结构模板只写非空气方块），所以不主动清的话，
 * 树会长在院子里、挡住屋顶。清场分两块：
 * <ul>
 *   <li>院子轮廓外扩 radius 格（默认 3）、地坪以上 clearAbove 格（默认 60）：
 *       以范围内的树叶为种子；</li>
 *   <li>从种子出发，按六方向深度优先遍历相邻的树叶与 Y 轴原木，遍历完成后再统一清空。
 *       DFS 最多再向外扩 {@link #TREE_DFS_MARGIN} 格，保证不会只削掉树冠的一半。</li>
 * </ul>
 *
 * <p>它被排在院子方块之前生成，所以院子里那棵（模板自带的）大树不会被自己清掉。
 */
public class YardClearingPiece extends StructurePiece
{
    public static final int FOUNDATION_DEPTH = 32;
    public static final int FOUNDATION_MARGIN = 1;
    public static final int TREE_DFS_MARGIN = 16;
    public static final int GATE_PATH_LENGTH = 8;
    public static final int GATE_PATH_HALF_WIDTH = 5;

    private static final Direction[] TREE_NEIGHBORS = {
            Direction.DOWN, Direction.UP, Direction.NORTH,
            Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private final int groundY;
    private final int radius;
    private final int clearAbove;
    private final int yardMinX;
    private final int yardMaxX;
    private final int yardMinZ;
    private final int yardMaxZ;
    private final int gateDir;

    public YardClearingPiece(BlockPos yardOrigin, int sizeX, int sizeZ, int groundY,
                             int radius, int clearAbove, int gateDir)
    {
        super(ModWorldGen.CLEARING_PIECE.get(), 0,
                new BoundingBox(
                        yardOrigin.getX() - radius - TREE_DFS_MARGIN,
                        yardOrigin.getY() - 8,
                        yardOrigin.getZ() - radius - TREE_DFS_MARGIN,
                        yardOrigin.getX() + sizeX - 1 + radius + TREE_DFS_MARGIN,
                        groundY + clearAbove,
                        yardOrigin.getZ() + sizeZ - 1 + radius + TREE_DFS_MARGIN));
        this.boundingBox = new DetectionFreeBoundingBox(this.boundingBox);
        this.groundY = groundY;
        this.radius = radius;
        this.clearAbove = clearAbove;
        this.yardMinX = yardOrigin.getX();
        this.yardMaxX = yardOrigin.getX() + sizeX - 1;
        this.yardMinZ = yardOrigin.getZ();
        this.yardMaxZ = yardOrigin.getZ() + sizeZ - 1;
        this.gateDir = gateDir >= 0 ? 1 : -1;
    }

    public YardClearingPiece(CompoundTag tag)
    {
        super(ModWorldGen.CLEARING_PIECE.get(), tag);
        this.boundingBox = new DetectionFreeBoundingBox(this.boundingBox);
        this.groundY = tag.getInt("GroundY");
        this.radius = tag.getInt("Radius");
        this.clearAbove = tag.getInt("ClearAbove");
        this.yardMinX = tag.getInt("YardMinX");
        this.yardMaxX = tag.getInt("YardMaxX");
        this.yardMinZ = tag.getInt("YardMinZ");
        this.yardMaxZ = tag.getInt("YardMaxZ");
        this.gateDir = tag.getInt("GateDir") >= 0 ? 1 : -1;
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
        tag.putInt("GateDir", this.gateDir);
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
        Set<BlockPos> treeBlocks = collectTreeBlocks(level, this.treeSeedBox(maxY),
                this.treeTraversalBox(minY, maxY));
        clearTreeBlocks(level, treeBlocks, box);
        int gateCenterX = this.yardMinX + (this.yardMaxX - this.yardMinX + 1) / 2;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++)
        {
            for (int z = minZ; z <= maxZ; z++)
            {
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
                    if (isNonTreeVegetation(state))
                    {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                boolean inYard = x >= this.yardMinX - FOUNDATION_MARGIN
                        && x <= this.yardMaxX + FOUNDATION_MARGIN
                        && z >= this.yardMinZ - FOUNDATION_MARGIN
                        && z <= this.yardMaxZ + FOUNDATION_MARGIN;
                boolean inGatePath = x >= gateCenterX - GATE_PATH_HALF_WIDTH
                        && x <= gateCenterX + GATE_PATH_HALF_WIDTH
                        && (this.gateDir > 0
                        ? (z >= this.yardMaxZ && z <= this.yardMaxZ + GATE_PATH_LENGTH)
                        : (z <= this.yardMinZ && z >= this.yardMinZ - GATE_PATH_LENGTH));
                if (inYard || inGatePath)
                {
                    int top = inYard ? this.groundY
                            : Math.max(this.groundY - 4, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z));
                    int from = Math.max(level.getMinBuildHeight(), top - FOUNDATION_DEPTH);
                    // 以前填充到 y = groundY（含地面层），院墙外那一圈少垫一层，
                    // 墙外的地表比院子里低一格，墙根那排砖就露在外面了。
                    // 现在留矮一格：地面层由模板自己带（GROUND_LAYER_IN_TEMPLATE），
                    // 这里只负责把地面下面的坑填实。
                    int fillTo = inYard ? this.groundY - 1 : top - 1;
                    for (int y = from; y <= fillTo; y++)
                    {
                        cursor.set(x, y, z);
                        BlockState here = level.getBlockState(cursor);
                        if (shouldFillFoundation(here))
                        {
                            level.setBlock(cursor, Blocks.DIRT.defaultBlockState(), 2);
                        }
                    }
                    int coverTop = Math.min(yardTop, this.groundY + 4);
                    for (int y = this.groundY + 1; y <= coverTop; y++)
                    {
                        cursor.set(x, y, z);
                        BlockState cover = level.getBlockState(cursor);
                        if (cover.is(Blocks.SNOW) || cover.is(Blocks.SNOW_BLOCK) || isIce(cover)
                                || isNonTreeVegetation(cover))
                        {
                            level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }
    }

    private BoundingBox treeSeedBox(int maxY)
    {
        return new BoundingBox(
                this.yardMinX - this.radius,
                Math.min(maxY, this.groundY),
                this.yardMinZ - this.radius,
                this.yardMaxX + this.radius,
                Math.min(maxY, this.groundY + this.clearAbove),
                this.yardMaxZ + this.radius);
    }

    private BoundingBox treeTraversalBox(int minY, int maxY)
    {
        return new BoundingBox(
                this.boundingBox.minX(),
                Math.max(minY, this.boundingBox.minY()),
                this.boundingBox.minZ(),
                this.boundingBox.maxX(),
                Math.min(maxY, this.boundingBox.maxY()),
                this.boundingBox.maxZ());
    }

    static Set<BlockPos> collectTreeBlocks(BlockGetter level, BoundingBox seedBox,
                                           BoundingBox traversalBox)
    {
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> treeBlocks = new HashSet<>();
        ArrayDeque<BlockPos> stack = new ArrayDeque<>();
        for (int x = seedBox.minX(); x <= seedBox.maxX(); x++)
        {
            for (int y = seedBox.minY(); y <= seedBox.maxY(); y++)
            {
                for (int z = seedBox.minZ(); z <= seedBox.maxZ(); z++)
                {
                    BlockPos seed = new BlockPos(x, y, z);
                    if (level.getBlockState(seed).is(BlockTags.LEAVES)
                            && visited.add(seed))
                    {
                        stack.addLast(seed);
                    }
                }
            }
        }
        while (!stack.isEmpty())
        {
            BlockPos current = stack.removeLast();
            BlockState state = level.getBlockState(current);
            if (!isTreeBlock(state))
            {
                continue;
            }
            treeBlocks.add(current);
            for (Direction direction : TREE_NEIGHBORS)
            {
                BlockPos next = current.relative(direction);
                if (!traversalBox.isInside(next) || visited.contains(next))
                {
                    continue;
                }
                BlockState nextState = level.getBlockState(next);
                if (isTreeBlock(nextState))
                {
                    visited.add(next);
                    stack.addLast(next);
                }
            }
        }
        return treeBlocks;
    }

    static void clearTreeBlocks(WorldGenLevel level, Set<BlockPos> treeBlocks, BoundingBox box)
    {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (BlockPos treeBlock : treeBlocks)
        {
            if (box.isInside(treeBlock))
            {
                level.setBlock(treeBlock, air, 2);
            }
        }
    }

    private static boolean isTreeBlock(BlockState state)
    {
        return state.is(BlockTags.LEAVES) || isVerticalLog(state);
    }

    private static boolean isVerticalLog(BlockState state)
    {
        return state.is(BlockTags.LOGS)
                && state.hasProperty(BlockStateProperties.AXIS)
                && state.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y;
    }

    static boolean shouldFillFoundation(BlockState state)
    {
        return state.getFluidState().isEmpty() && !isIce(state)
                && (state.isAir() || isVegetation(state));
    }

    private static boolean isIce(BlockState state)
    {
        return state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.BLUE_ICE) || state.is(Blocks.FROSTED_ICE);
    }

    private static final class DetectionFreeBoundingBox extends BoundingBox
    {
        DetectionFreeBoundingBox(BoundingBox src)
        {
            super(src.minX(), src.minY(), src.minZ(), src.maxX(), src.maxY(), src.maxZ());
        }

        @Override
        public boolean isInside(net.minecraft.core.Vec3i v)
        {
            return false;
        }

        @Override
        public boolean isInside(int x, int y, int z)
        {
            return false;
        }
    }

    public static boolean isVegetation(BlockState state)
    {
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.FLOWERS) || state.is(BlockTags.SMALL_FLOWERS)
                || state.is(BlockTags.TALL_FLOWERS) || state.is(BlockTags.CROPS))
        {
            return true;
        }
        Block block = state.getBlock();
        return
                //? if <1.20.2 {
                block == Blocks.GRASS
                //?} else {
                /*block == Blocks.SHORT_GRASS
                *///?}
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

    static boolean isNonTreeVegetation(BlockState state)
    {
        return !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS) && isVegetation(state);
    }
}
