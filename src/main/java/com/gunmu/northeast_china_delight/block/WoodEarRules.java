package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.NortheastChinaConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 哪种树、长在哪儿会出野生木耳，出的概率多大。概率由 {@code [worldgen]} 中的两个配置项控制：
 * 黑森林的深色橡木、白桦林 / 原始桦木林的白桦默认 7%，任意群系的橡木默认 5%，其余树种不出。
 */
public final class WoodEarRules
{
    private WoodEarRules()
    {
    }

    /** 野生木耳生成概率；配置未加载时回退到默认值。 */
    public static float wildChance(LevelReader level, BlockPos pos, BlockState logState)
    {
        float forestChance = NortheastChinaConfig.wildWoodEarForestChance();
        float oakChance = NortheastChinaConfig.wildWoodEarOakChance();
        if (logState.is(Blocks.DARK_OAK_LOG) || logState.is(Blocks.DARK_OAK_WOOD))
        {
            Holder<Biome> biome = level.getBiome(pos);
            return biome.is(Biomes.DARK_FOREST) ? forestChance : 0.0F;
        }
        if (logState.is(Blocks.BIRCH_LOG) || logState.is(Blocks.BIRCH_WOOD))
        {
            Holder<Biome> biome = level.getBiome(pos);
            return biome.is(Biomes.BIRCH_FOREST) || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST)
                    ? forestChance : 0.0F;
        }
        if (logState.is(Blocks.OAK_LOG) || logState.is(Blocks.OAK_WOOD))
        {
            return oakChance;
        }
        return 0.0F;
    }
}
