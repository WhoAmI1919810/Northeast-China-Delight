package com.gunmu.dongbei_delight.worldgen;

import com.gunmu.dongbei_delight.block.HazelnutBushBlock;
import com.gunmu.dongbei_delight.block.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 榛子丛的自然生成：先种下一丛熟透的榛子丛，再按规则在周围撒榛蘑。
 *
 * <p>榛蘑的生成概率：</p>
 * <ul>
 *   <li>基础概率 <b>3%</b>，作用在榛子丛周围半径 10 格里每一格「草方块／泥土」上，各自独立判定；</li>
 *   <li>如果榛子丛周围 10 格里长着深色橡木／橡木／白桦，就把对应树种的概率加到这个 3% 上：
 *       深色橡木 +1.5%、橡木 +1%、白桦 +0.6%（同时有好几种就取最高的那一项）；</li>
 *   <li>自然生成的榛蘑永远是<b>单株</b>，不会出现榛蘑簇。</li>
 * </ul>
 */
public class HazelnutBushFeature extends Feature<NoneFeatureConfiguration>
{
    /** 榛蘑的基础概率 */
    public static final float BASE_CHANCE = 0.03F;
    /** 周围有深色橡木时额外加的概率 */
    public static final float DARK_OAK_CHANCE = 0.015F;
    /** 周围有橡木时额外加的概率 */
    public static final float OAK_CHANCE = 0.01F;
    /** 周围有白桦时额外加的概率 */
    public static final float BIRCH_CHANCE = 0.006F;

    /** 榛子丛影响榛蘑的水平半径 */
    public static final int BUSH_RADIUS = 10;

    public HazelnutBushFeature(Codec<NoneFeatureConfiguration> codec)
    {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context)
    {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        // 雪地里地表是一层薄雪片：榛子丛要**和雪片占同一格**（把雪片顶掉、自带雪面），
        // 而不是长在雪片上面那一层。
        BlockPos target = origin;
        BlockState belowOrigin = level.getBlockState(origin.below());
        if (belowOrigin.is(Blocks.SNOW) && belowOrigin.getValue(SnowLayerBlock.LAYERS) == 1)
        {
            target = origin.below();
        }
        BlockState targetState = level.getBlockState(target);
        boolean thinSnow = targetState.is(Blocks.SNOW) && targetState.getValue(SnowLayerBlock.LAYERS) == 1;
        if (!thinSnow && !targetState.canBeReplaced())
        {
            return false;
        }

        BlockState bush = ModBlocks.HAZELNUT_BUSH.get().defaultBlockState()
                .setValue(HazelnutBushBlock.AGE, HazelnutBushBlock.MAX_AGE)
                .setValue(HazelnutBushBlock.SNOWY, HazelnutBushBlock.isSnowyAt(level, target));
        if (!bush.canSurvive(level, target))
        {
            return false;
        }
        level.setBlock(target, bush, 2);

        float chance = BASE_CHANCE + HazelMushroomScatter.treeBonus(level, origin, BUSH_RADIUS);
        HazelMushroomScatter.scatter(level, origin, BUSH_RADIUS, chance, random);
        return true;
    }
}
