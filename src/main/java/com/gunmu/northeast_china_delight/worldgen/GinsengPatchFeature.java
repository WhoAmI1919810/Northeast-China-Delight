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

/**
 * 野生人参：只在「积雪山坡、雪林、积雪针叶林、冰封山峰」这类雪地里冒头。
 *
 * <p>密度按 2×2 区块（32×32 格）为一个单元来算：</p>
 * <ul>
 *   <li>每个单元有 <b>30%</b> 的概率长出人参；</li>
 *   <li>一个单元最多 <b>3 棵</b>；</li>
 *   <li>植株只长在「盖着一层雪的草方块／泥土／灰化土」上——把那一层雪顶掉，长出的是<b>成熟</b>的人参，
 *       并且直接是覆雪形态。</li>
 * </ul>
 *
 * <p>负责生成的是这个单元西南角的那个区块（x、z 都是偶数的区块），所以一片 2×2 区块只会被处理一次，
 * 概率和上限都是按单元算的，不会因为跨区块翻倍。</p>
 */
public class GinsengPatchFeature extends Feature<NoneFeatureConfiguration>
{
    /** 每个 2×2 区块单元长出人参的概率 */
    public static final float REGION_CHANCE = 0.30F;
    /** 每个单元最多几棵 */
    public static final int MAX_PER_REGION = 3;
    /** 每个单元最多试几次找地方 */
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

        // 只让 2×2 单元里西南角那个区块负责
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
            // 落雪那一步排在植被**之后**，所以这里地上通常还没有雪片：
            // 只判断「这块地将来会被雪盖住」（生物群系会落雪），成熟的人参就直接长在这一格，
            // 和将来那层雪片的位置重合（自带雪面的模型）。
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos plantPos = new BlockPos(x, surfaceY, z);
            BlockState here = level.getBlockState(plantPos);
            // 落雪在 vegetal_decoration 之后，这里地面多半还是空的（canBeReplaced）。
            // 接受空气/可替换格，人参直接长在将来会被雪盖住的格子上（自带雪面形态）。
            if (!here.canBeReplaced() && !here.is(Blocks.SNOW))
            {
                continue;
            }
            BlockState ground = level.getBlockState(plantPos.below());
            // 雪林、积雪山坡的地表是一整块雪，真正的土在它下面一层
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

    /** 用世界种子和单元坐标算一个稳定的随机种子，保证同一个单元的判定到处都一样 */
    private static long regionSeed(long worldSeed, int regionChunkX, int regionChunkZ)
    {
        return worldSeed ^ ((long) regionChunkX * 341873128712L) ^ ((long) regionChunkZ * 132897987541L);
    }
}
