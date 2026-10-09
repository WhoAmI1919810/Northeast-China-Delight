package com.gunmu.northeast_china_delight.worldgen;

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
    /**
     * 院子底下要填多深：地下经常有洞穴/空腔，只填「地表到院子地面」那一小段不够，
     * 往下再垫一层实心泥土，站在院子里往下挖就不会突然漏到洞里。
     */
    public static final int FOUNDATION_DEPTH = 32;
    /** 墙外只垫一圈 */
    public static final int FOUNDATION_MARGIN = 1;
    /** 树冠比树干宽，清场半径外再补一圈专门清树叶/原木，免得留下悬空的树冠 */
    public static final int LEAF_MARGIN = 6;
    /** 门口正前方这一片也要填实（不然出门就是个洞） */
    public static final int GATE_PATH_LENGTH = 8;
    public static final int GATE_PATH_HALF_WIDTH = 5;

    private final int groundY;
    private final int radius;
    private final int clearAbove;
    /** 院子本体的水平范围（要在这里把地形垫平，免得院子悬空） */
    private final int yardMinX;
    private final int yardMaxX;
    private final int yardMinZ;
    private final int yardMaxZ;
    /** 大门的朝向：+1 = 朝南（+z），-1 = 朝北（-z） */
    private final int gateDir;

    public YardClearingPiece(BlockPos yardOrigin, int sizeX, int sizeZ, int groundY,
                             int radius, int clearAbove, int gateDir)
    {
        super(ModWorldGen.CLEARING_PIECE.get(), 0,
                new BoundingBox(yardOrigin.getX() - radius, yardOrigin.getY() - 8, yardOrigin.getZ() - radius,
                        yardOrigin.getX() + sizeX - 1 + radius, yardOrigin.getY() + clearAbove + 8,
                        yardOrigin.getZ() + sizeZ - 1 + radius));
        // 清场件的包围盒外扩了 radius 格，而 minecraft:location / getStructureWithPieceAt
        // 会把任意 piece 的包围盒当成「在结构里」。把它换成探测不到的盒子，
        // 「快乐老家」之类的进度判定就只认院子本体（CourtyardPiece）那一小块。
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
        // 同上：存档读回来的 boundingBox 是普通 BoundingBox，重新包一层
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
                boolean inYard = x >= this.yardMinX - FOUNDATION_MARGIN
                        && x <= this.yardMaxX + FOUNDATION_MARGIN
                        && z >= this.yardMinZ - FOUNDATION_MARGIN
                        && z <= this.yardMaxZ + FOUNDATION_MARGIN;
                // 院子范围 + 大门正前方一条：把地形表面到院子地面之间填实，别让院子悬空、别让门口是洞
                boolean inGatePath = x >= (this.yardMinX + this.yardMaxX) / 2 - GATE_PATH_HALF_WIDTH
                        && x <= (this.yardMinX + this.yardMaxX) / 2 + GATE_PATH_HALF_WIDTH
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
                        if (here.isAir() || isVegetation(here) || isIce(here)
                                || !here.getFluidState().isEmpty())
                        {
                            level.setBlock(cursor, Blocks.DIRT.defaultBlockState(), 2);
                        }
                    }
                    // 地面以上只清「盖在地表的雪/冰/草皮」：雪原里高度图会把薄雪片当成一层方块，
                    // 不把这些清掉，院外的雪就比院子里高，看着还是「内外不齐、墙根露砖」。
                    int coverTop = Math.min(yardTop, this.groundY + 4);
                    for (int y = this.groundY + 1; y <= coverTop; y++)
                    {
                        cursor.set(x, y, z);
                        BlockState cover = level.getBlockState(cursor);
                        if (cover.is(Blocks.SNOW) || cover.is(Blocks.SNOW_BLOCK) || isIce(cover)
                                || isVegetation(cover))
                        {
                            level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
                // 外圈再扫一遍树叶/原木：清场半径边缘被截断的树，树冠不会悬在半空
                if (!inYard && Math.abs(x - (this.yardMinX + this.yardMaxX) / 2) <= this.radius + LEAF_MARGIN
                        && Math.abs(z - (this.yardMinZ + this.yardMaxZ) / 2) <= this.radius + LEAF_MARGIN)
                {
                    for (int y = yFrom; y <= yardTop; y++)
                    {
                        cursor.set(x, y, z);
                        BlockState state = level.getBlockState(cursor);
                        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS))
                        {
                            level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }
    }

    private static boolean isIce(BlockState state)
    {
        return state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.BLUE_ICE) || state.is(Blocks.FROSTED_ICE);
    }

    /**
     * 探测无效的包围盒：isInside 永远 false。
     *
     * <p>{@code StructureManager.structureHasPieceAt} 只问每个 piece 的包围盒是否包住玩家，
     * 清场件外扩了 20 格就会被误认为「人在院里」。换成这个盒子后清场件照常按包围盒
     * 调度/写块（postProcess 用的是 intersects，不受影响），但进度的 location 判定
     * 不会再把院子的外围缓冲区算进去。</p>
     */
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
}
