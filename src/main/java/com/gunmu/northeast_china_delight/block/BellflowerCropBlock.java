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

/** 桔梗作物：玩家播种只认耕地，野生植株按方块标签扎根土类方块。 */
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
