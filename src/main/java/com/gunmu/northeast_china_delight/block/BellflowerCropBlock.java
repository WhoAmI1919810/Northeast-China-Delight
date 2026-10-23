package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * 桔梗的作物方块。
 *
 * <p>玩家播种仍然只认耕地；野生的桔梗则允许直接扎根在土类方块上，所以
 * {@link #mayPlaceOn(BlockState, BlockGetter, BlockPos)} 用的是单独的方块标签。</p>
 */
public class BellflowerCropBlock extends DdCropBlock
{
    public BellflowerCropBlock(BlockBehaviour.Properties properties, Supplier<? extends ItemLike> seed)
    {
        super(properties, seed);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos)
    {
        return state.is(ModBlockTags.BELLFLOWER_PLANTABLE_ON);
    }

    /** 种子只能点在普通耕地上；野生生成直接写方块，不经过这个判断。 */
    public static boolean isPlayerPlantableGround(BlockState state)
    {
        if (state.is(Blocks.FARMLAND))
        {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null
                && "farmersdelight".equals(id.getNamespace())
                && "rich_soil_farmland".equals(id.getPath());
    }

    /**
     * 世界生成专用：此时区块光照尚未点亮，不能复用 {@link net.minecraft.world.level.block.CropBlock#canSurvive}。
     * 野生桔梗只校验扎根方块，空间与遮挡由生成特性本身负责。
     */
    public static boolean canWildSurvive(BlockGetter level, BlockPos plantPos)
    {
        return level.getBlockState(plantPos.below()).is(ModBlockTags.BELLFLOWER_PLANTABLE_ON);
    }
}
