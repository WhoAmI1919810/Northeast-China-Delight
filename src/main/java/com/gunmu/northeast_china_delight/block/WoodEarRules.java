package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 哪种树、长在哪儿会出木耳，出的概率多大。刮树皮（{@code ModGameplayEvents}）和野生木耳的生成
 * （{@code WildWoodEarFeature}）都按这一份算，所以「每根原木长出野生木耳的概率」就是「刮这根原木出木耳的概率」：
 * 黑森林的深色橡木、白桦林 / 原始桦木林的白桦 7%，任意群系的橡木 5%，其余树种不出。
 */
public final class WoodEarRules
{
    public static final float FOREST_CHANCE = 0.07F;
    public static final float OAK_CHANCE = 0.05F;

    private WoodEarRules()
    {
    }

    public static float chance(LevelReader level, BlockPos pos, BlockState logState)
    {
        if (logState.is(Blocks.DARK_OAK_LOG) || logState.is(Blocks.DARK_OAK_WOOD))
        {
            Holder<Biome> biome = level.getBiome(pos);
            return biome.is(Biomes.DARK_FOREST) ? FOREST_CHANCE : 0.0F;
        }
        if (logState.is(Blocks.BIRCH_LOG) || logState.is(Blocks.BIRCH_WOOD))
        {
            Holder<Biome> biome = level.getBiome(pos);
            return biome.is(Biomes.BIRCH_FOREST) || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST)
                    ? FOREST_CHANCE : 0.0F;
        }
        if (logState.is(Blocks.OAK_LOG) || logState.is(Blocks.OAK_WOOD))
        {
            return OAK_CHANCE;
        }
        return 0.0F;
    }
}
