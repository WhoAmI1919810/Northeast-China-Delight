package com.gunmu.northeast_china_delight.worldgen;

import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.block.WildWoodEarBlock;
import com.gunmu.northeast_china_delight.block.WoodEarRules;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 野生木耳：每个区块跑一次（在树都长完之后的 top_layer_modification 阶段），逐列从树冠往下看，
 * 碰到橡木 / 白桦 / 深色橡木的原木就按 {@link WoodEarRules#wildChance 配置的生成概率}掷一次，
 * 中了就在这根原木空着的一侧长一朵 {@link WildWoodEarBlock 野生木耳}。
 *
 * <p>只长在树上：掷中后再看头顶几格有没有树叶，村庄木屋的原木框架不会长出木耳。</p>
 */
public class WildWoodEarFeature extends Feature<NoneFeatureConfiguration>
{
    /** 每列从树冠往下最多看几格 */
    private static final int MAX_DEPTH = 40;
    /** 「是不是树」：原木上方这个范围里得有树叶 */
    private static final int LEAF_RADIUS = 3;
    private static final int LEAF_HEIGHT = 8;

    public WildWoodEarFeature(Codec<NoneFeatureConfiguration> codec)
    {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context)
    {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        int baseX = context.origin().getX() & ~15;
        int baseZ = context.origin().getZ() & ~15;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int placed = 0;
        for (int dx = 0; dx < 16; dx++)
        {
            for (int dz = 0; dz < 16; dz++)
            {
                int x = baseX + dx;
                int z = baseZ + dz;
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                for (int y = top; y > top - MAX_DEPTH && y >= level.getMinBuildHeight(); y--)
                {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.is(BlockTags.LEAVES))
                    {
                        continue;
                    }
                    if (!state.is(BlockTags.LOGS))
                    {
                        break;
                    }
                    float chance = WoodEarRules.wildChance(level, pos, state);
                    if (chance > 0.0F && random.nextFloat() < chance && grow(level, pos.immutable(), random))
                    {
                        placed++;
                    }
                }
            }
        }
        return placed > 0;
    }

    /** 在这根原木空着的一侧长一朵野生木耳（得是树上的原木）；长成了返回 true */
    public static boolean grow(WorldGenLevel level, BlockPos logPos, RandomSource random)
    {
        if (!isTreeLog(level, logPos))
        {
            return false;
        }
        int start = random.nextInt(4);
        for (int i = 0; i < 4; i++)
        {
            Direction side = Direction.from2DDataValue((start + i) & 3);
            BlockPos target = logPos.relative(side);
            if (!level.isEmptyBlock(target))
            {
                continue;
            }
            BlockState ear = ModBlocks.WILD_WOOD_EAR.get().defaultBlockState()
                    .setValue(WildWoodEarBlock.FACING, side.getOpposite());
            if (ear.canSurvive(level, target))
            {
                level.setBlock(target, ear, 2);
                return true;
            }
        }
        return false;
    }

    private static boolean isTreeLog(WorldGenLevel level, BlockPos logPos)
    {
        for (BlockPos around : BlockPos.betweenClosed(
                logPos.offset(-LEAF_RADIUS, 1, -LEAF_RADIUS),
                logPos.offset(LEAF_RADIUS, LEAF_HEIGHT, LEAF_RADIUS)))
        {
            if (level.getBlockState(around).is(BlockTags.LEAVES))
            {
                return true;
            }
        }
        return false;
    }
}
